package android.media.metrics;

import android.os.PersistableBundle;

public final class BundleSession implements AutoCloseable
{
    public static final String KEY_STATSD_ATOM = "bundlesession-statsd-atom";

    private final String mId;
    private final LogSessionId mSessionId;

    BundleSession(String id)
    {
        mId = id;
        mSessionId = new LogSessionId(id);
    }

    @Override
    public void close()
    {
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof BundleSession)) return false;

        return mId.equals(((BundleSession)object).mId);
    }

    public LogSessionId getSessionId()
    {
        return mSessionId;
    }

    @Override
    public int hashCode()
    {
        return mId.hashCode();
    }

    public void reportBundleMetrics(PersistableBundle metrics)
    {
        if (metrics == null) throw new NullPointerException("metrics must not be null");
    }
}
