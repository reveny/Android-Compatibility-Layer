package android.print;

public final class PrintJob
{
    private final PrintManager mPrintManager;
    private final PrintJobInfo mInfo;

    PrintJob(PrintJobInfo info, PrintManager printManager)
    {
        mInfo = info;
        mPrintManager = printManager;
    }

    public PrintJobId getId()
    {
        return mInfo.getId();
    }

    public PrintJobInfo getInfo()
    {
        synchronized (mInfo)
        {
            return new PrintJobInfo(mInfo);
        }
    }

    public void cancel()
    {
        if (isInImmutableState()) return;
        mPrintManager.cancelPrintJob(this);
    }

    public void restart()
    {
        if (isFailed()) mPrintManager.restartPrintJob(this);
    }

    public boolean isQueued()
    {
        return getState() == PrintJobInfo.STATE_QUEUED;
    }

    public boolean isStarted()
    {
        return getState() == PrintJobInfo.STATE_STARTED;
    }

    public boolean isBlocked()
    {
        return getState() == PrintJobInfo.STATE_BLOCKED;
    }

    public boolean isCompleted()
    {
        return getState() == PrintJobInfo.STATE_COMPLETED;
    }

    public boolean isFailed()
    {
        return getState() == PrintJobInfo.STATE_FAILED;
    }

    public boolean isCancelled()
    {
        return getState() == PrintJobInfo.STATE_CANCELED;
    }

    PrintJobInfo getMutableInfo()
    {
        return mInfo;
    }

    void setState(int state)
    {
        synchronized (mInfo)
        {
            mInfo.setState(state);
        }
    }

    private int getState()
    {
        synchronized (mInfo)
        {
            return mInfo.getState();
        }
    }

    private boolean isInImmutableState()
    {
        int state = getState();
        return state == PrintJobInfo.STATE_CANCELED || state == PrintJobInfo.STATE_COMPLETED || state == PrintJobInfo.STATE_FAILED;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PrintJob)) return false;

        PrintJobId id = mInfo.getId();
        return id != null && id.equals(((PrintJob)other).getId());
    }

    @Override
    public int hashCode()
    {
        PrintJobId id = mInfo.getId();
        return id != null ? id.hashCode() : 0;
    }
}
