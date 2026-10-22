package android.media.metrics;

public final class LogSessionId
{
    public static final LogSessionId LOG_SESSION_ID_NONE = new LogSessionId("");

    private final String mSessionId;

    LogSessionId(String sessionId)
    {
        mSessionId = sessionId == null ? "" : sessionId;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof LogSessionId)) return false;

        return mSessionId.equals(((LogSessionId)object).mSessionId);
    }

    public String getStringId()
    {
        return mSessionId;
    }

    @Override
    public int hashCode()
    {
        return mSessionId.hashCode();
    }

    @Override
    public String toString()
    {
        return mSessionId;
    }
}
