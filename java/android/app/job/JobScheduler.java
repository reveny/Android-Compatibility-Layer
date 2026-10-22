package android.app.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class JobScheduler
{
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;

    public static final int PENDING_JOB_REASON_EXECUTING = -1;
    public static final int PENDING_JOB_REASON_INVALID_JOB_ID = -2;
    public static final int PENDING_JOB_REASON_UNDEFINED = 0;
    public static final int PENDING_JOB_REASON_APP = 1;
    public static final int PENDING_JOB_REASON_APP_STANDBY = 2;
    public static final int PENDING_JOB_REASON_BACKGROUND_RESTRICTION = 3;
    public static final int PENDING_JOB_REASON_CONSTRAINT_BATTERY_NOT_LOW = 4;
    public static final int PENDING_JOB_REASON_CONSTRAINT_CHARGING = 5;
    public static final int PENDING_JOB_REASON_CONSTRAINT_CONNECTIVITY = 6;
    public static final int PENDING_JOB_REASON_CONSTRAINT_CONTENT_TRIGGER = 7;
    public static final int PENDING_JOB_REASON_CONSTRAINT_DEVICE_IDLE = 8;
    public static final int PENDING_JOB_REASON_CONSTRAINT_MINIMUM_LATENCY = 9;
    public static final int PENDING_JOB_REASON_CONSTRAINT_PREFETCH = 10;
    public static final int PENDING_JOB_REASON_CONSTRAINT_STORAGE_NOT_LOW = 11;
    public static final int PENDING_JOB_REASON_DEVICE_STATE = 12;
    public static final int PENDING_JOB_REASON_JOB_SCHEDULER_OPTIMIZATION = 13;
    public static final int PENDING_JOB_REASON_QUOTA = 14;
    public static final int PENDING_JOB_REASON_USER = 15;
    public static final int PENDING_JOB_REASON_CONSTRAINT_DEADLINE = 16;

    public JobScheduler()
    {
    }

    public JobScheduler forNamespace(String namespace)
    {
        throw new RuntimeException("Not implemented. Must override in a subclass.");
    }

    public String getNamespace()
    {
        throw new RuntimeException("Not implemented. Must override in a subclass.");
    }

    public abstract int schedule(JobInfo job);

    public abstract int enqueue(JobInfo job, JobWorkItem work);

    public abstract void cancel(int jobId);

    public abstract void cancelAll();

    public void cancelInAllNamespaces()
    {
        throw new RuntimeException("Not implemented. Must override in a subclass.");
    }

    public abstract List<JobInfo> getAllPendingJobs();

    public Map<String, List<JobInfo>> getPendingJobsInAllNamespaces()
    {
        Map<String, List<JobInfo>> pending = new HashMap<>();
        pending.put(getNamespace(), getAllPendingJobs());
        return pending;
    }

    public abstract JobInfo getPendingJob(int jobId);

    public int getPendingJobReason(int jobId)
    {
        return PENDING_JOB_REASON_UNDEFINED;
    }

    public int[] getPendingJobReasons(int jobId)
    {
        return new int[] {getPendingJobReason(jobId)};
    }

    public boolean canRunUserInitiatedJobs()
    {
        return false;
    }
}
