package android.media.metrics;

public final class RecordingSession implements AutoCloseable
{
    private final String mId;
    private final LogSessionId mSessionId;

    RecordingSession(String id)
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
        if (!(object instanceof RecordingSession)) return false;

        return mId.equals(((RecordingSession)object).mId);
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
}
