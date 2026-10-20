package android.app.job;

public class JobParameters
{
    private final int mJobId;
    private final android.os.Bundle mTransientExtras;
    private boolean mStopped;

    public JobParameters(int jobId, android.os.Bundle transientExtras)
    {
        mJobId = jobId;
        mTransientExtras = transientExtras;
    }

    public int getJobId()
    {
        return mJobId;
    }

    public android.os.Bundle getTransientExtras()
    {
        return mTransientExtras;
    }

    public boolean isOverrideDeadlineExpired()
    {
        return false;
    }

    public boolean isStopped()
    {
        return mStopped;
    }

    void markStopped()
    {
        mStopped = true;
    }
}
