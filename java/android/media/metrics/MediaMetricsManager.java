package android.media.metrics;

import android.content.Context;

import java.security.SecureRandom;

public final class MediaMetricsManager
{
    public static final long INVALID_TIMESTAMP = -1L;

    private static final SecureRandom sRandom = new SecureRandom();

    private final Context mContext;

    public MediaMetricsManager(Context context)
    {
        mContext = context;
    }

    static String newSessionId()
    {
        byte[] bytes = new byte[12];

        synchronized (sRandom)
        {
            sRandom.nextBytes(bytes);
        }

        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (byte value : bytes)
        {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }

        return builder.toString();
    }

    public BundleSession createBundleSession()
    {
        return new BundleSession(newSessionId());
    }

    public EditingSession createEditingSession()
    {
        return new EditingSession(newSessionId());
    }

    public PlaybackSession createPlaybackSession()
    {
        return new PlaybackSession(newSessionId());
    }

    public RecordingSession createRecordingSession()
    {
        return new RecordingSession(newSessionId());
    }

    public TranscodingSession createTranscodingSession()
    {
        return new TranscodingSession(newSessionId());
    }

    public void releaseSessionId(String sessionId)
    {
    }
}
