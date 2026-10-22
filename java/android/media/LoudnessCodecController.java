package android.media;

import android.os.Bundle;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

public class LoudnessCodecController implements AutoCloseable
{
    private final int mSessionId;
    private final Executor mExecutor;
    private final OnLoudnessCodecUpdateListener mListener;
    private final Set<MediaCodec> mCodecs = Collections.newSetFromMap(new IdentityHashMap<>());

    LoudnessCodecController(int sessionId, Executor executor, OnLoudnessCodecUpdateListener listener)
    {
        mSessionId = sessionId;
        mExecutor = executor;
        mListener = listener;
    }

    public static LoudnessCodecController create(int sessionId)
    {
        return new LoudnessCodecController(sessionId, null, null);
    }

    public static LoudnessCodecController create(int sessionId, Executor executor, OnLoudnessCodecUpdateListener listener)
    {
        Objects.requireNonNull(executor, "Executor cannot be null");
        Objects.requireNonNull(listener, "OnLoudnessCodecUpdateListener cannot be null");

        return new LoudnessCodecController(sessionId, executor, listener);
    }

    public boolean addMediaCodec(MediaCodec mediaCodec)
    {
        Objects.requireNonNull(mediaCodec, "MediaCodec for addMediaCodec cannot be null");

        synchronized (mCodecs)
        {
            return mCodecs.add(mediaCodec);
        }
    }

    @Override
    public void close()
    {
        synchronized (mCodecs)
        {
            mCodecs.clear();
        }
    }

    public Bundle getLoudnessCodecParams(MediaCodec mediaCodec)
    {
        Objects.requireNonNull(mediaCodec, "MediaCodec for getLoudnessCodecParams cannot be null");

        synchronized (mCodecs)
        {
            if (!mCodecs.contains(mediaCodec))
            {
                throw new IllegalArgumentException("MediaCodec was not added for loudness annotation");
            }
        }

        return new Bundle();
    }

    public void removeMediaCodec(MediaCodec mediaCodec)
    {
        Objects.requireNonNull(mediaCodec, "MediaCodec for removeMediaCodec cannot be null");

        synchronized (mCodecs)
        {
            if (!mCodecs.remove(mediaCodec))
            {
                throw new IllegalArgumentException("Could not find matching MediaCodec to remove");
            }
        }
    }

    public interface OnLoudnessCodecUpdateListener
    {
        default Bundle onLoudnessCodecUpdate(MediaCodec mediaCodec, Bundle codecValues)
        {
            return codecValues;
        }
    }
}
