package android.media.metrics;

public final class EditingSession implements AutoCloseable
{
    private final String mId;
    private final LogSessionId mSessionId;

    EditingSession(String id)
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
        if (!(object instanceof EditingSession)) return false;

        return mId.equals(((EditingSession)object).mId);
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

    public void reportEditingEndedEvent(EditingEndedEvent editingEndedEvent)
    {
        if (editingEndedEvent == null) throw new NullPointerException("editingEndedEvent must not be null");
    }
}
