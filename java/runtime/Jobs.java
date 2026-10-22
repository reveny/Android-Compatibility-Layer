package runtime;

import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Notification;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.app.job.JobServiceEngine;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Jobs extends JobScheduler
{
    private static final String TAG = "Jobs";

    private static final long EXECUTION_LIMIT_MILLIS = 10L * 60L * 1000L;
    private static final long EXPEDITED_EXECUTION_LIMIT_MILLIS = 3L * 60L * 1000L;
    private static final long DEFAULT_TRIGGER_UPDATE_DELAY_MILLIS = 10000L;
    private static final long DEFAULT_TRIGGER_MAX_DELAY_MILLIS = 2L * 60L * 1000L;
    private static final int MAX_JOBS_PER_APP = 150;

    private static final class Record
    {
        String namespace;
        JobInfo info;
        ServiceRecord host;
        JobParameters parameters;
        Runnable pending;
        Runnable timeout;
        boolean running;
        boolean settled;
        boolean waitingForContent;
        int failures;
        int nextWorkId = 1;
        int notificationId;
        int notificationPolicy = -1;
        final List<JobWorkItem> queuedWork = new ArrayList<>();
        final List<JobWorkItem> deliveredWork = new ArrayList<>();
        final LinkedHashSet<Uri> changedUris = new LinkedHashSet<>();
        long firstChangeUptime;
        ContentObserver observer;
    }

    private static final class ServiceRecord
    {
        String className;
        Service service;
        JobServiceEngine engine;
        int running;
    }

    private static final Map<String, Record> sJobs = new LinkedHashMap<>();
    private static final Map<String, ServiceRecord> sServices = new LinkedHashMap<>();
    private static final Map<String, Jobs> sSchedulers = new LinkedHashMap<>();
    private static final Jobs sInstance = new Jobs(null);

    private static Context sBaseContext;
    private static Application sApplication;
    private static AppComponentFactory sFactory;
    private static ClassLoader sLoader;
    private static boolean sSaidConstraintsAreAssumed;

    private final String mNamespace;

    private Jobs(String namespace)
    {
        mNamespace = namespace;
    }

    public static Jobs instance()
    {
        return sInstance;
    }

    public static void attachRuntime(Context base, Application application, AppComponentFactory factory, ClassLoader loader)
    {
        sBaseContext = base;
        sApplication = application;
        sFactory = factory;
        sLoader = loader;
    }

    @Override
    public JobScheduler forNamespace(String namespace)
    {
        if (namespace == null) throw new NullPointerException("namespace cannot be null");

        String trimmed = namespace.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("namespace cannot be empty");

        synchronized (sSchedulers)
        {
            Jobs scheduler = sSchedulers.get(trimmed);
            if (scheduler == null)
            {
                scheduler = new Jobs(trimmed);
                sSchedulers.put(trimmed, scheduler);
            }

            return scheduler;
        }
    }

    @Override
    public String getNamespace()
    {
        return mNamespace;
    }

    @Override
    public int schedule(JobInfo job)
    {
        return scheduleInternal(job, null);
    }

    @Override
    public int enqueue(JobInfo job, JobWorkItem work)
    {
        if (work == null) throw new NullPointerException("work");
        if (job != null && job.isPersisted()) throw new IllegalArgumentException("Can't enqueue work for persisted jobs");
        if (job != null && job.isPeriodic()) throw new IllegalArgumentException("Can't enqueue work for periodic jobs");

        return scheduleInternal(job, work);
    }

    private int scheduleInternal(JobInfo job, JobWorkItem work)
    {
        if (job == null || job.getService() == null) return RESULT_FAILURE;
        if (sBaseContext == null)
        {
            Log.w(TAG, "a job was scheduled before the runtime was attached");
            return RESULT_FAILURE;
        }

        Handler handler = handler();
        if (handler == null)
        {
            Log.w(TAG, "no main looper, so job " + job.getId() + " cannot be scheduled");
            return RESULT_FAILURE;
        }

        if (Components.resolve(Components.SERVICE, new Intent().setComponent(job.getService())) == null)
        {
            throw new IllegalArgumentException("No such service " + job.getService());
        }

        noteAssumedConstraints(job);

        final String key = keyFor(mNamespace, job.getId());
        Record previous;
        Record record;

        synchronized (sJobs)
        {
            previous = sJobs.get(key);

            if (work != null && previous != null && !previous.settled && previous.info.equals(job))
            {
                synchronized (previous)
                {
                    addWork(previous, work);
                }

                Log.i(TAG, "queued work on job " + describe(previous) + " (" + previous.queuedWork.size() + " waiting)");
                return RESULT_SUCCESS;
            }

            if (previous == null && sJobs.size() >= MAX_JOBS_PER_APP)
            {
                throw new IllegalStateException("Apps may not schedule more than " + MAX_JOBS_PER_APP + " distinct jobs");
            }

            record = new Record();
            record.namespace = mNamespace;
            record.info = job;
            record.pending = new Runnable()
            {
                @Override
                public void run()
                {
                    runJob(key);
                }
            };

            if (work != null) addWork(record, work);

            sJobs.put(key, record);
        }

        if (previous != null) retire(previous, JobParameters.STOP_REASON_CANCELLED_BY_APP);

        if (job.getTriggerContentUris() != null)
        {
            watchContent(record);
            Log.i(TAG, "job " + describe(record) + " for " + job.getService().getClassName()
                           + " waits for a change to " + job.getTriggerContentUris().length + " content uri(s)");
            return RESULT_SUCCESS;
        }

        long delay = Math.max(job.getMinLatencyMillis(), 0);
        handler.postDelayed(record.pending, delay);
        Log.i(TAG, "job " + describe(record) + " for " + job.getService().getClassName() + " runs in " + delay + "ms"
                       + (job.isPeriodic() ? ", then every " + job.getIntervalMillis() + "ms" : ""));

        return RESULT_SUCCESS;
    }

    @Override
    public void cancel(int jobId)
    {
        Record record;
        synchronized (sJobs)
        {
            record = sJobs.remove(keyFor(mNamespace, jobId));
        }

        if (record == null) return;

        retire(record, JobParameters.STOP_REASON_CANCELLED_BY_APP);
        Log.i(TAG, "cancelled job " + describe(record));
    }

    @Override
    public void cancelAll()
    {
        for (Record record : records(mNamespace, false)) cancel(record.info.getId());
    }

    @Override
    public void cancelInAllNamespaces()
    {
        for (Record record : records(null, true))
        {
            Record removed;
            synchronized (sJobs)
            {
                removed = sJobs.remove(keyFor(record.namespace, record.info.getId()));
            }

            if (removed != null) retire(removed, JobParameters.STOP_REASON_CANCELLED_BY_APP);
        }
    }

    @Override
    public List<JobInfo> getAllPendingJobs()
    {
        List<JobInfo> pending = new ArrayList<>();
        for (Record record : records(mNamespace, false)) pending.add(record.info);
        return pending;
    }

    @Override
    public Map<String, List<JobInfo>> getPendingJobsInAllNamespaces()
    {
        Map<String, List<JobInfo>> pending = new LinkedHashMap<>();

        for (Record record : records(null, true))
        {
            List<JobInfo> jobs = pending.get(record.namespace);
            if (jobs == null)
            {
                jobs = new ArrayList<>();
                pending.put(record.namespace, jobs);
            }

            jobs.add(record.info);
        }

        return pending;
    }

    @Override
    public JobInfo getPendingJob(int jobId)
    {
        synchronized (sJobs)
        {
            Record record = sJobs.get(keyFor(mNamespace, jobId));
            return record == null ? null : record.info;
        }
    }

    @Override
    public int getPendingJobReason(int jobId)
    {
        Record record;
        synchronized (sJobs)
        {
            record = sJobs.get(keyFor(mNamespace, jobId));
        }

        if (record == null) return PENDING_JOB_REASON_INVALID_JOB_ID;
        if (record.running) return PENDING_JOB_REASON_EXECUTING;
        if (record.waitingForContent) return PENDING_JOB_REASON_CONSTRAINT_CONTENT_TRIGGER;
        if (record.info.getMinLatencyMillis() > 0 || record.info.isPeriodic()) return PENDING_JOB_REASON_CONSTRAINT_MINIMUM_LATENCY;
        return PENDING_JOB_REASON_UNDEFINED;
    }

    @Override
    public boolean canRunUserInitiatedJobs()
    {
        return true;
    }

    public static JobWorkItem dequeueWork(JobParameters parameters)
    {
        Record record = recordFor(parameters);
        if (record == null) return null;

        JobWorkItem item = null;
        boolean finished = false;

        synchronized (record)
        {
            if (!record.queuedWork.isEmpty())
            {
                item = record.queuedWork.remove(0);
                item.bumpDeliveryCount();
                record.deliveredWork.add(item);
            }
            else if (record.deliveredWork.isEmpty())
            {
                finished = true;
            }
        }

        if (finished)
        {
            Log.i(TAG, "job " + describe(record) + " has no more work");
            finishOnMain(parameters, false);
        }

        return item;
    }

    public static boolean completeWork(JobParameters parameters, JobWorkItem work)
    {
        Record record = recordFor(parameters);
        if (record == null || work == null) return false;

        synchronized (record)
        {
            return record.deliveredWork.remove(work);
        }
    }

    public static void setNotification(JobParameters parameters, int notificationId, Notification notification, int policy)
    {
        Record record = recordFor(parameters);
        if (record == null) return;

        if (record.notificationPolicy >= 0 && record.notificationId != notificationId)
        {
            NotificationStore.cancel(null, record.notificationId);
        }

        record.notificationId = notificationId;
        record.notificationPolicy = policy;
        NotificationStore.post(null, notificationId, notification);
    }

    public static void finished(JobService service, JobParameters parameters, boolean wantsReschedule)
    {
        finished(parameters, wantsReschedule);
    }

    public static void finished(JobParameters parameters, boolean wantsReschedule)
    {
        if (parameters == null) return;

        finishOnMain(parameters, wantsReschedule);
    }

    private static void finishOnMain(final JobParameters parameters, final boolean wantsReschedule)
    {
        Handler handler = handler();
        if (handler == null) return;

        if (Looper.myLooper() == handler.getLooper())
        {
            settle(parameters, wantsReschedule);
            return;
        }

        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                settle(parameters, wantsReschedule);
            }
        });
    }

    private static void settle(JobParameters parameters, boolean wantsReschedule)
    {
        Record record = recordFor(parameters);
        if (record == null || record.settled) return;

        record.settled = true;
        record.running = false;

        Handler handler = handler();
        if (handler != null && record.timeout != null) handler.removeCallbacks(record.timeout);

        Log.i(TAG, "job " + describe(record) + " finished" + (wantsReschedule ? " and asked to run again" : ""));

        releaseHost(record);

        boolean moreWork;
        synchronized (record)
        {
            if (wantsReschedule)
            {
                record.queuedWork.addAll(0, record.deliveredWork);
            }

            record.deliveredWork.clear();
            moreWork = !record.queuedWork.isEmpty();
        }

        if (wantsReschedule)
        {
            record.failures++;
            long delay = backoffFor(record);
            Log.i(TAG, "job " + describe(record) + " backs off for " + delay + "ms");
            requeue(record, delay);
            return;
        }

        record.failures = 0;

        if (record.info.isPeriodic())
        {
            requeue(record, record.info.getIntervalMillis());
            return;
        }

        if (moreWork)
        {
            requeue(record, 0);
            return;
        }

        synchronized (sJobs)
        {
            String key = keyFor(record.namespace, record.info.getId());
            if (sJobs.get(key) == record) sJobs.remove(key);
        }

        unwatchContent(record);
        endNotification(record);
    }

    private static void requeue(Record record, long delay)
    {
        record.parameters = null;
        record.settled = false;

        Handler handler = handler();
        if (handler != null) handler.postDelayed(record.pending, Math.max(delay, 0));
    }

    private static long backoffFor(Record record)
    {
        long initial = record.info.getInitialBackoffMillis();
        long delay;

        if (record.info.getBackoffPolicy() == JobInfo.BACKOFF_POLICY_LINEAR)
        {
            delay = initial * record.failures;
        }
        else
        {
            int shift = Math.min(record.failures - 1, 30);
            delay = initial << shift;
        }

        if (delay < 0 || delay > JobInfo.MAX_BACKOFF_DELAY_MILLIS) delay = JobInfo.MAX_BACKOFF_DELAY_MILLIS;
        return delay;
    }

    private static void runJob(String key)
    {
        final Record record;
        synchronized (sJobs)
        {
            record = sJobs.get(key);
        }

        if (record == null || record.running) return;

        record.waitingForContent = false;

        ServiceRecord host = acquireHost(record.info.getService());
        if (host == null) return;

        record.host = host;

        Uri[] triggeredUris = null;
        String[] triggeredAuthorities = null;

        if (!record.changedUris.isEmpty())
        {
            triggeredUris = record.changedUris.toArray(new Uri[0]);
            LinkedHashSet<String> authorities = new LinkedHashSet<>();
            for (Uri uri : triggeredUris)
            {
                if (uri.getAuthority() != null) authorities.add(uri.getAuthority());
            }

            triggeredAuthorities = authorities.toArray(new String[0]);
            record.changedUris.clear();
            record.firstChangeUptime = 0;
        }

        JobInfo info = record.info;
        record.parameters = new JobParameters(info.getId(), record.namespace, info.getExtras(), info.getTransientExtras(),
                                              info.getClipData(), info.getClipGrantFlags(), triggeredUris, triggeredAuthorities,
                                              networkFor(info), false, info.isExpedited(), info.isUserInitiated());
        record.settled = false;
        record.running = true;

        final JobParameters parameters = record.parameters;

        record.timeout = new Runnable()
        {
            @Override
            public void run()
            {
                if (record.parameters != parameters || record.settled) return;

                Log.w(TAG, "job " + describe(record) + " ran past its execution limit and is being stopped");
                stopRunning(record, JobParameters.STOP_REASON_TIMEOUT, true);
            }
        };

        Handler handler = handler();
        if (handler != null)
        {
            handler.postDelayed(record.timeout, info.isExpedited() ? EXPEDITED_EXECUTION_LIMIT_MILLIS : EXECUTION_LIMIT_MILLIS);
        }

        try
        {
            Log.i(TAG, "job " + describe(record) + " started");

            boolean running = host.engine.onStartJob(parameters);
            if (!running) settle(parameters, false);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "onStartJob threw for job " + describe(record), cause);
            settle(parameters, false);
        }
    }

    private static void retire(Record record, int stopReason)
    {
        Handler handler = handler();
        if (handler != null)
        {
            handler.removeCallbacks(record.pending);
            if (record.timeout != null) handler.removeCallbacks(record.timeout);
        }

        unwatchContent(record);

        if (record.running && !record.settled) stopRunning(record, stopReason, false);

        endNotification(record);
    }

    private static void stopRunning(Record record, int stopReason, boolean mayReschedule)
    {
        JobParameters parameters = record.parameters;
        if (parameters == null || record.host == null) return;

        parameters.markStopped(stopReason);

        boolean reschedule = false;
        try
        {
            reschedule = record.host.engine.onStopJob(parameters);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "onStopJob threw for job " + describe(record), cause);
        }

        if (mayReschedule)
        {
            settle(parameters, reschedule);
            return;
        }

        record.settled = true;
        record.running = false;
        releaseHost(record);
    }

    private static void endNotification(Record record)
    {
        if (record.notificationPolicy == JobService.JOB_END_NOTIFICATION_POLICY_REMOVE)
        {
            NotificationStore.cancel(null, record.notificationId);
        }

        record.notificationPolicy = -1;
    }

    private static ServiceRecord acquireHost(ComponentName component)
    {
        String className = component.getClassName();
        ServiceRecord host = sServices.get(className);

        if (host == null)
        {
            host = new ServiceRecord();
            host.className = className;

            try
            {
                Intent intent = new Intent().setComponent(component);
                host.service = sFactory.instantiateService(sLoader, className, intent);
                host.service.attach(sBaseContext, sApplication);
                host.service.onCreate();

                IBinder binder = host.service.onBind(intent);
                host.engine = JobServiceEngine.fromBinder(binder);
            }
            catch (Throwable cause)
            {
                Log.e(TAG, "could not bring up job service " + className, cause);
                return null;
            }

            if (host.engine == null)
            {
                Log.e(TAG, className + " is not a JobService and returned no JobServiceEngine binder, so its jobs cannot run");
                destroyHost(host);
                return null;
            }

            sServices.put(className, host);
        }

        host.running++;
        return host;
    }

    private static void releaseHost(Record record)
    {
        ServiceRecord host = record.host;
        record.host = null;
        if (host == null) return;

        host.running--;
        if (host.running > 0) return;

        sServices.remove(host.className);

        try
        {
            host.service.onUnbind(new Intent().setComponent(record.info.getService()));
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "onUnbind threw in " + host.className, cause);
        }

        destroyHost(host);
    }

    private static void destroyHost(ServiceRecord host)
    {
        try
        {
            host.service.onDestroy();
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "onDestroy threw in " + host.className, cause);
        }
    }

    private static void watchContent(final Record record)
    {
        final Handler handler = handler();
        if (handler == null) return;

        record.waitingForContent = true;

        record.observer = new ContentObserver(handler)
        {
            @Override
            public void onChange(boolean selfChange, Uri uri)
            {
                contentChanged(record, uri);
            }
        };

        for (JobInfo.TriggerContentUri trigger : record.info.getTriggerContentUris())
        {
            boolean descendants = (trigger.getFlags() & JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS) != 0;
            sBaseContext.getContentResolver().registerContentObserver(trigger.getUri(), descendants, record.observer);
        }
    }

    private static void unwatchContent(Record record)
    {
        if (record.observer == null) return;

        sBaseContext.getContentResolver().unregisterContentObserver(record.observer);
        record.observer = null;
    }

    private static void contentChanged(Record record, Uri uri)
    {
        if (!record.waitingForContent) return;

        if (uri != null && record.changedUris.size() < 50) record.changedUris.add(uri);

        long now = android.os.SystemClock.uptimeMillis();
        if (record.firstChangeUptime == 0) record.firstChangeUptime = now;

        long update = record.info.getTriggerContentUpdateDelay();
        long maximum = record.info.getTriggerContentMaxDelay();
        if (update < 0) update = DEFAULT_TRIGGER_UPDATE_DELAY_MILLIS;
        if (maximum < 0) maximum = DEFAULT_TRIGGER_MAX_DELAY_MILLIS;

        long latest = record.firstChangeUptime + maximum;
        long due = Math.min(now + update, latest);
        due = Math.max(due, record.firstChangeUptime + Math.max(record.info.getMinLatencyMillis(), 0));

        Handler handler = handler();
        if (handler == null) return;

        handler.removeCallbacks(record.pending);
        handler.postDelayed(record.pending, Math.max(due - now, 0));
    }

    private static Network networkFor(JobInfo info)
    {
        if (info.getNetworkType() == JobInfo.NETWORK_TYPE_NONE && info.getRequiredNetwork() == null) return null;

        try
        {
            Object manager = sBaseContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            return manager instanceof ConnectivityManager ? ((ConnectivityManager)manager).getActiveNetwork() : null;
        }
        catch (Throwable cause)
        {
            return null;
        }
    }

    private static void addWork(Record record, JobWorkItem work)
    {
        work.setWorkId(record.nextWorkId++);
        record.queuedWork.add(work);
    }

    private static Record recordFor(JobParameters parameters)
    {
        if (parameters == null) return null;

        synchronized (sJobs)
        {
            Record record = sJobs.get(keyFor(parameters.getJobNamespace(), parameters.getJobId()));
            if (record != null && record.parameters == parameters) return record;

            for (Record candidate : sJobs.values())
            {
                if (candidate.parameters == parameters) return candidate;
            }
        }

        return null;
    }

    private static List<Record> records(String namespace, boolean everyNamespace)
    {
        List<Record> matched = new ArrayList<>();

        synchronized (sJobs)
        {
            for (Record record : sJobs.values())
            {
                if (everyNamespace || Objects.equals(record.namespace, namespace)) matched.add(record);
            }
        }

        return matched;
    }

    private static String keyFor(String namespace, int jobId)
    {
        return (namespace == null ? "" : namespace + "/") + jobId;
    }

    private static String describe(Record record)
    {
        return keyFor(record.namespace, record.info.getId());
    }

    private static void noteAssumedConstraints(JobInfo job)
    {
        boolean constrained = job.getNetworkType() != JobInfo.NETWORK_TYPE_NONE || job.getRequiredNetwork() != null
                              || job.isRequireCharging() || job.isRequireDeviceIdle() || job.isRequireBatteryNotLow()
                              || job.isRequireStorageNotLow();

        if (!constrained || sSaidConstraintsAreAssumed) return;

        sSaidConstraintsAreAssumed = true;
        Log.i(TAG, "this runtime has no network, charging, battery, storage or idle state to wait for, so those constraints are "
                       + "treated as met");
    }

    private static Handler handler()
    {
        Looper main = Looper.getMainLooper();
        return main == null ? null : new Handler(main);
    }
}
