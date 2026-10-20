package android.app.job;

import java.util.List;

public abstract class JobScheduler
{
    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;

    public abstract int schedule(JobInfo job);

    public abstract void cancel(int jobId);

    public abstract void cancelAll();

    public abstract List<JobInfo> getAllPendingJobs();

    public abstract JobInfo getPendingJob(int jobId);
}
