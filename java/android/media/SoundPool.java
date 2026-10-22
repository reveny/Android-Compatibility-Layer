package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SoundPool
{
    private static final int LOAD_STATUS_OK = 0;
    private static final int LOAD_STATUS_ERROR = 1;

    public interface OnLoadCompleteListener
    {
        void onLoadComplete(SoundPool soundPool, int sampleId, int status);
    }

    private static final class Stream
    {
        final int mStreamId;
        final int mSampleId;
        int mPriority;
        int mLoop;
        float mLeftVolume;
        float mRightVolume;
        float mRate;
        boolean mPaused;
        boolean mAutoPaused;

        Stream(int streamId, int sampleId)
        {
            mStreamId = streamId;
            mSampleId = sampleId;
        }
    }

    private final Object mLock = new Object();
    private final int mMaxStreams;
    private final AudioAttributes mAudioAttributes;
    private final Set<Integer> mSamples = new HashSet<>();
    private final Map<Integer, Stream> mStreams = new HashMap<>();
    private final Handler mHandler;
    private OnLoadCompleteListener mOnLoadCompleteListener;
    private int mNextSampleId = 1;
    private int mNextStreamId = 1;
    private boolean mReleased;

    @Deprecated
    public SoundPool(int maxStreams, int streamType, int sourceQuality)
    {
        this(maxStreams, new AudioAttributes.Builder().setLegacyStreamType(streamType).build());
    }

    private SoundPool(int maxStreams, AudioAttributes attributes)
    {
        mMaxStreams = maxStreams;
        mAudioAttributes = attributes;

        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        mHandler = looper == null ? null : new Handler(looper);
    }

    public final void autoPause()
    {
        synchronized (mLock)
        {
            for (Stream stream : mStreams.values())
            {
                if (!stream.mPaused)
                {
                    stream.mPaused = true;
                    stream.mAutoPaused = true;
                }
            }
        }
    }

    public final void autoResume()
    {
        synchronized (mLock)
        {
            for (Stream stream : mStreams.values())
            {
                if (stream.mAutoPaused)
                {
                    stream.mPaused = false;
                    stream.mAutoPaused = false;
                }
            }
        }
    }

    public int load(Context context, int resourceId, int priority)
    {
        boolean readable = false;
        InputStream input = context.getResources().openRawResource(resourceId);

        try
        {
            readable = input.read() >= 0;
        }
        catch (java.io.IOException exception)
        {
            readable = false;
        }
        finally
        {
            closeQuietly(input);
        }

        return enqueueLoad(readable);
    }

    public int load(AssetFileDescriptor descriptor, int priority)
    {
        if (descriptor == null) return 0;

        long length = descriptor.getLength();
        if (length < 0 && length != AssetFileDescriptor.UNKNOWN_LENGTH) throw new IllegalArgumentException("no length for fd");

        return enqueueLoad(descriptor.getFileDescriptor() != null && descriptor.getFileDescriptor().valid());
    }

    public int load(FileDescriptor descriptor, long offset, long length, int priority)
    {
        return enqueueLoad(descriptor != null && descriptor.valid() && offset >= 0 && length != 0);
    }

    public int load(String path, int priority)
    {
        if (path == null) return 0;

        File file = new File(path);
        if (!file.isFile() || !file.canRead()) return 0;

        return enqueueLoad(file.length() > 0);
    }

    public final void pause(int streamId)
    {
        synchronized (mLock)
        {
            Stream stream = mStreams.get(streamId);
            if (stream != null)
            {
                stream.mPaused = true;
                stream.mAutoPaused = false;
            }
        }
    }

    public final int play(int soundId, float leftVolume, float rightVolume, int priority, int loop, float rate)
    {
        synchronized (mLock)
        {
            if (mReleased || !mSamples.contains(soundId)) return 0;

            if (mMaxStreams > 0 && mStreams.size() >= mMaxStreams && !evictStreamBelow(priority)) return 0;

            Stream stream = new Stream(mNextStreamId++, soundId);
            stream.mPriority = priority;
            stream.mLoop = loop;
            stream.mLeftVolume = clampVolume(leftVolume);
            stream.mRightVolume = clampVolume(rightVolume);
            stream.mRate = clampRate(rate);
            mStreams.put(stream.mStreamId, stream);
            return stream.mStreamId;
        }
    }

    public final void release()
    {
        synchronized (mLock)
        {
            mReleased = true;
            mStreams.clear();
            mSamples.clear();
            mOnLoadCompleteListener = null;
        }
    }

    @Override
    protected void finalize()
    {
        release();
    }

    public final void resume(int streamId)
    {
        synchronized (mLock)
        {
            Stream stream = mStreams.get(streamId);
            if (stream != null)
            {
                stream.mPaused = false;
                stream.mAutoPaused = false;
            }
        }
    }

    public final void setLoop(int streamId, int loop)
    {
        synchronized (mLock)
        {
            Stream stream = mStreams.get(streamId);
            if (stream != null) stream.mLoop = loop;
        }
    }

    public void setOnLoadCompleteListener(SoundPool.OnLoadCompleteListener listener)
    {
        synchronized (mLock)
        {
            mOnLoadCompleteListener = listener;
        }
    }

    public final void setPriority(int streamId, int priority)
    {
        synchronized (mLock)
        {
            Stream stream = mStreams.get(streamId);
            if (stream != null) stream.mPriority = priority;
        }
    }

    public final void setRate(int streamId, float rate)
    {
        synchronized (mLock)
        {
            Stream stream = mStreams.get(streamId);
            if (stream != null) stream.mRate = clampRate(rate);
        }
    }

    public final void setVolume(int streamId, float leftVolume, float rightVolume)
    {
        synchronized (mLock)
        {
            Stream stream = mStreams.get(streamId);
            if (stream != null)
            {
                stream.mLeftVolume = clampVolume(leftVolume);
                stream.mRightVolume = clampVolume(rightVolume);
            }
        }
    }

    public final void stop(int streamId)
    {
        synchronized (mLock)
        {
            mStreams.remove(streamId);
        }
    }

    public final boolean unload(int soundId)
    {
        synchronized (mLock)
        {
            if (!mSamples.remove(soundId)) return false;

            Iterator<Stream> iterator = mStreams.values().iterator();
            while (iterator.hasNext())
            {
                if (iterator.next().mSampleId == soundId) iterator.remove();
            }

            return true;
        }
    }

    private int enqueueLoad(boolean readable)
    {
        final int sampleId;
        final int status = readable ? LOAD_STATUS_OK : LOAD_STATUS_ERROR;

        synchronized (mLock)
        {
            if (mReleased) return 0;

            sampleId = mNextSampleId++;
            if (readable) mSamples.add(sampleId);
        }

        Runnable notify = new Runnable()
        {
            @Override
            public void run()
            {
                OnLoadCompleteListener listener;

                synchronized (mLock)
                {
                    listener = mOnLoadCompleteListener;
                }

                if (listener != null) listener.onLoadComplete(SoundPool.this, sampleId, status);
            }
        };

        if (mHandler != null)
        {
            mHandler.post(notify);
        }
        else
        {
            Thread thread = new Thread(notify, "SoundPoolLoader");
            thread.setDaemon(true);
            thread.start();
        }

        return sampleId;
    }

    private boolean evictStreamBelow(int priority)
    {
        Stream victim = null;

        for (Stream stream : mStreams.values())
        {
            if (stream.mPriority > priority) continue;
            if (victim == null || stream.mPriority < victim.mPriority
                || (stream.mPriority == victim.mPriority && stream.mStreamId < victim.mStreamId))
            {
                victim = stream;
            }
        }

        if (victim == null) return false;

        mStreams.remove(victim.mStreamId);
        return true;
    }

    private static float clampVolume(float volume)
    {
        return Math.max(0.0f, Math.min(1.0f, volume));
    }

    private static float clampRate(float rate)
    {
        return Math.max(0.5f, Math.min(2.0f, rate));
    }

    private static void closeQuietly(InputStream input)
    {
        try
        {
            input.close();
        }
        catch (java.io.IOException exception)
        {
        }
    }

    public static class Builder
    {
        private int mMaxStreams = 1;
        private AudioAttributes mAudioAttributes;

        public Builder()
        {
        }

        public SoundPool build()
        {
            AudioAttributes attributes = mAudioAttributes;
            if (attributes == null) attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();

            return new SoundPool(mMaxStreams, attributes);
        }

        public SoundPool.Builder setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException
        {
            if (attributes == null) throw new IllegalArgumentException("Invalid null AudioAttributes");
            mAudioAttributes = attributes;
            return this;
        }

        public SoundPool.Builder setAudioSessionId(int sessionId)
        {
            if (sessionId != AudioManager.AUDIO_SESSION_ID_GENERATE && sessionId <= 0)
            {
                throw new IllegalArgumentException("Invalid audio session ID " + sessionId);
            }

            return this;
        }

        public SoundPool.Builder setContext(Context context)
        {
            return this;
        }

        public SoundPool.Builder setMaxStreams(int maxStreams) throws IllegalArgumentException
        {
            if (maxStreams <= 0) throw new IllegalArgumentException("Strictly positive value required for max streams");
            mMaxStreams = maxStreams;
            return this;
        }
    }
}
