package android.app.backup;

public class FullBackupDataOutput
{
    private final long mQuota;
    private final int mTransportFlags;

    FullBackupDataOutput(long quota, int transportFlags)
    {
        mQuota = quota;
        mTransportFlags = transportFlags;
    }

    public long getQuota()
    {
        return mQuota;
    }

    public int getTransportFlags()
    {
        return mTransportFlags;
    }
}
