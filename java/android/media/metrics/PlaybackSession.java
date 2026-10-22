package android.media.metrics;

public final class PlaybackSession implements AutoCloseable
{
    private final String mId;
    private final LogSessionId mSessionId;
    private boolean mClosed;

    PlaybackSession(String id)
    {
        mId = id;
        mSessionId = new LogSessionId(id);
    }

    @Override
    public void close()
    {
        mClosed = true;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof PlaybackSession)) return false;

        return mId.equals(((PlaybackSession)object).mId);
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

    public void reportNetworkEvent(NetworkEvent networkEvent)
    {
        requireEvent(networkEvent);
    }

    public void reportPlaybackErrorEvent(PlaybackErrorEvent playbackErrorEvent)
    {
        requireEvent(playbackErrorEvent);
    }

    public void reportPlaybackMetrics(PlaybackMetrics playbackMetrics)
    {
        requireEvent(playbackMetrics);
    }

    public void reportPlaybackStateEvent(PlaybackStateEvent playbackStateEvent)
    {
        requireEvent(playbackStateEvent);
    }

    public void reportTrackChangeEvent(TrackChangeEvent trackChangeEvent)
    {
        requireEvent(trackChangeEvent);
    }

    private static void requireEvent(Object event)
    {
        if (event == null) throw new NullPointerException("event must not be null");
    }
}
