package runtime;

import android.app.AppComponentFactory;
import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Jobs extends JobScheduler
{
    private static final String TAG = "Jobs";

    private static final class Record
    {
        JobInfo info;
        JobService service;
        JobParameters parameters;
        Runnable pending;
        boolean settled;
    }

    private static final Map<Integer, Record> sJobs = new LinkedHashMap<>();
    private static final Jobs sInstance = new Jobs();

    private static Context sBaseContext;
    private static Application sApplication;
    private static AppComponentFactory sFactory;
    private static ClassLoader sLoader;
    private static boolean sSaidConstraintsAreAssumed;

    private Jobs()
    {
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
    public int schedule(final JobInfo job)
    {
        if (job == null || job.getService() == null) return RESULT_FAILURE;
        if (sBaseContext == null)
        {
            Log.w(TAG, "a job was scheduled before the runtime was attached");
            return RESULT_FAILURE;
        }

        noteAssumedConstraints(job);

        Handler handler = handler();
        if (handler == null)
        {
            Log.w(TAG, "no main looper, so job " + job.getId() + " cannot be scheduled");
            return RESULT_FAILURE;
        }

        Record record = new Record();
        record.info = job;

        long delay = job.getMinLatencyMillis();
        if (delay <= 0 && job.isPeriodic()) delay = job.getIntervalMillis();
        if (delay < 0) delay = 0;

        final int jobId = job.getId();

        record.pending = new Runnable()
        {
            @Override
            public void run()
            {
                runJob(jobId);
            }
        };

        synchronized (sJobs)
        {
            Record previous = sJobs.put(jobId, record);
            if (previous != null && previous.pending != null) handler.removeCallbacks(previous.pending);
        }

        handler.postDelayed(record.pending, delay);
        Log.i(TAG, "job " + jobId + " for " + job.getService().getClassName() + " runs in " + delay + "ms"
                       + (job.isPeriodic() ? ", then every " + job.getIntervalMillis() + "ms" : ""));

        return RESULT_SUCCESS;
    }

    @Override
    public void cancel(int jobId)
    {
        Record record;
        synchronized (sJobs)
        {
            record = sJobs.remove(jobId);
        }

        if (record == null) return;

        Handler handler = handler();
        if (handler != null && record.pending != null) handler.removeCallbacks(record.pending);

        Log.i(TAG, "cancelled job " + jobId);
    }

    @Override
    public void cancelAll()
    {
        List<Integer> identifiers;
        synchronized (sJobs)
        {
            identifiers = new ArrayList<>(sJobs.keySet());
        }

        for (Integer jobId : identifiers) cancel(jobId);
    }

    @Override
    public List<JobInfo> getAllPendingJobs()
    {
        List<JobInfo> pending = new ArrayList<>();

        synchronized (sJobs)
        {
            for (Record record : sJobs.values()) pending.add(record.info);
        }

        return pending;
    }

    @Override
    public JobInfo getPendingJob(int jobId)
    {
        synchronized (sJobs)
        {
            Record record = sJobs.get(jobId);
            return record == null ? null : record.info;
        }
    }

    public static void finished(JobService service, JobParameters parameters, boolean wantsReschedule)
    {
        if (parameters == null) return;

        int jobId = parameters.getJobId();
        Record record;

        synchronized (sJobs)
        {
            record = sJobs.get(jobId);
        }

        if (record == null || record.settled) return;

        record.settled = true;
        Log.i(TAG, "job " + jobId + " finished" + (wantsReschedule ? " and asked to run again" : ""));

        boolean periodic = record.info.isPeriodic();

        if (!periodic && !wantsReschedule)
        {
            synchronized (sJobs)
            {
                sJobs.remove(jobId);
            }

            return;
        }

        Handler handler = handler();
        if (handler == null) return;

        long delay = periodic ? record.info.getIntervalMillis() : 0;
        handler.postDelayed(record.pending, delay);
    }

    private static void runJob(int jobId)
    {
        Record record;
        synchronized (sJobs)
        {
            record = sJobs.get(jobId);
        }

        if (record == null) return;

        String className = record.info.getService().getClassName();

        if (record.service == null)
        {
            try
            {
                android.app.Service created = sFactory.instantiateService(sLoader, className, new Intent());
                if (!(created instanceof JobService))
                {
                    Log.e(TAG, className + " is not a JobService, so job " + jobId + " cannot run");
                    return;
                }

                record.service = (JobService)created;
                record.service.attach(sBaseContext, sApplication);
                record.service.onCreate();
            }
            catch (Throwable cause)
            {
                Log.e(TAG, "could not instantiate " + className, cause);
                return;
            }
        }

        record.parameters = new JobParameters(jobId, record.info.getTransientExtras());
        record.settled = false;

        try
        {
            Log.i(TAG, "job " + jobId + " started");

            boolean running = record.service.onStartJob(record.parameters);
            if (!running) finished(record.service, record.parameters, false);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "onStartJob threw for job " + jobId, cause);
        }
    }

    private static void noteAssumedConstraints(JobInfo job)
    {
        boolean constrained = job.getNetworkType() != JobInfo.NETWORK_TYPE_NONE || job.isRequireCharging()
                              || job.isRequireDeviceIdle();

        if (!constrained || sSaidConstraintsAreAssumed) return;

        sSaidConstraintsAreAssumed = true;
        Log.i(TAG, "this runtime has no network, charging or idle state to wait for, so those constraints are treated as met");
    }

    private static Handler handler()
    {
        Looper main = Looper.getMainLooper();
        return main == null ? null : new Handler(main);
    }
}
