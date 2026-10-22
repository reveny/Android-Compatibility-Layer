package android.media;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;

public class RemoteControlClient
{
    public static final int FLAG_KEY_MEDIA_FAST_FORWARD = 64;
    public static final int FLAG_KEY_MEDIA_NEXT = 128;
    public static final int FLAG_KEY_MEDIA_PAUSE = 16;
    public static final int FLAG_KEY_MEDIA_PLAY = 4;
    public static final int FLAG_KEY_MEDIA_PLAY_PAUSE = 8;
    public static final int FLAG_KEY_MEDIA_POSITION_UPDATE = 256;
    public static final int FLAG_KEY_MEDIA_PREVIOUS = 1;
    public static final int FLAG_KEY_MEDIA_RATING = 512;
    public static final int FLAG_KEY_MEDIA_REWIND = 2;
    public static final int FLAG_KEY_MEDIA_STOP = 32;
    public static final int PLAYSTATE_BUFFERING = 8;
    public static final int PLAYSTATE_ERROR = 9;
    public static final int PLAYSTATE_FAST_FORWARDING = 4;
    public static final int PLAYSTATE_PAUSED = 2;
    public static final int PLAYSTATE_PLAYING = 3;
    public static final int PLAYSTATE_REWINDING = 5;
    public static final int PLAYSTATE_SKIPPING_BACKWARDS = 7;
    public static final int PLAYSTATE_SKIPPING_FORWARDS = 6;
    public static final int PLAYSTATE_STOPPED = 1;

    private static final long POSITION_INVALID = -1;
    private static final float PLAYBACK_SPEED_1X = 1.0f;

    private final Object mCacheLock = new Object();
    private final PendingIntent mRcMediaIntent;
    private final Looper mLooper;

    private int mPlaybackState = PLAYSTATE_STOPPED;
    private long mPlaybackStateChangeTimeMs;
    private long mPlaybackPositionMs = POSITION_INVALID;
    private float mPlaybackSpeed = PLAYBACK_SPEED_1X;
    private int mTransportControlFlags;
    private Bundle mMetadata = new Bundle();
    private Bitmap mOriginalArtwork;
    private MediaMetadata mMediaMetadata;
    private MediaSession mSession;
    private OnPlaybackPositionUpdateListener mPositionUpdateListener;
    private OnGetPlaybackPositionListener mPositionProvider;
    private OnMetadataUpdateListener mMetadataUpdateListener;

    public RemoteControlClient(PendingIntent mediaButtonIntent)
    {
        this(mediaButtonIntent, Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper());
    }

    public RemoteControlClient(PendingIntent mediaButtonIntent, Looper looper)
    {
        mRcMediaIntent = mediaButtonIntent;
        mLooper = looper;
    }

    public MetadataEditor editMetadata(boolean startEmpty)
    {
        MetadataEditor editor = new MetadataEditor();

        synchronized (mCacheLock)
        {
            if (!startEmpty)
            {
                editor.mEditorMetadata.putAll(mMetadata);
                editor.mEditorArtwork = mOriginalArtwork;
            }
        }

        editor.mMetadataChanged = startEmpty;
        editor.mArtworkChanged = startEmpty;
        return editor;
    }

    public void setPlaybackState(int state)
    {
        setPlaybackState(state, POSITION_INVALID, PLAYBACK_SPEED_1X);
    }

    public void setPlaybackState(int state, long timeInMs, float playbackSpeed)
    {
        synchronized (mCacheLock)
        {
            if (mPlaybackState != state || mPlaybackPositionMs != timeInMs || mPlaybackSpeed != playbackSpeed)
            {
                mPlaybackState = state;
                mPlaybackPositionMs = timeInMs;
                mPlaybackSpeed = playbackSpeed;
                mPlaybackStateChangeTimeMs = SystemClock.elapsedRealtime();
            }
        }
    }

    public void setTransportControlFlags(int transportControlFlags)
    {
        synchronized (mCacheLock)
        {
            mTransportControlFlags = transportControlFlags;
        }
    }

    public void setMetadataUpdateListener(OnMetadataUpdateListener listener)
    {
        synchronized (mCacheLock)
        {
            mMetadataUpdateListener = listener;
        }
    }

    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener)
    {
        synchronized (mCacheLock)
        {
            mPositionUpdateListener = listener;
        }
    }

    public void setOnGetPlaybackPositionListener(OnGetPlaybackPositionListener listener)
    {
        synchronized (mCacheLock)
        {
            mPositionProvider = listener;
        }
    }

    public MediaSession getMediaSession()
    {
        return mSession;
    }

    PendingIntent getRcMediaIntent()
    {
        return mRcMediaIntent;
    }

    Looper getLooper()
    {
        return mLooper;
    }

    int getPlaybackState()
    {
        synchronized (mCacheLock)
        {
            return mPlaybackState;
        }
    }

    long getPlaybackPosition()
    {
        OnGetPlaybackPositionListener provider;

        synchronized (mCacheLock)
        {
            provider = mPositionProvider;
        }

        if (provider != null) return provider.onGetPlaybackPosition();

        synchronized (mCacheLock)
        {
            if (mPlaybackPositionMs < 0 || mPlaybackState != PLAYSTATE_PLAYING) return mPlaybackPositionMs;

            long elapsed = SystemClock.elapsedRealtime() - mPlaybackStateChangeTimeMs;
            return mPlaybackPositionMs + (long)(elapsed * mPlaybackSpeed);
        }
    }

    int getTransportControlFlags()
    {
        synchronized (mCacheLock)
        {
            return mTransportControlFlags;
        }
    }

    MediaMetadata getMediaMetadata()
    {
        synchronized (mCacheLock)
        {
            return mMediaMetadata;
        }
    }

    void dispatchPlaybackPositionUpdate(long newPositionMs)
    {
        OnPlaybackPositionUpdateListener listener;

        synchronized (mCacheLock)
        {
            listener = mPositionUpdateListener;
        }

        if (listener != null) listener.onPlaybackPositionUpdate(newPositionMs);
    }

    void dispatchMetadataUpdate(int key, Object newValue)
    {
        OnMetadataUpdateListener listener;

        synchronized (mCacheLock)
        {
            listener = mMetadataUpdateListener;
        }

        if (listener != null) listener.onMetadataUpdate(key, newValue);
    }

    public interface OnPlaybackPositionUpdateListener
    {
        void onPlaybackPositionUpdate(long newPositionMs);
    }

    public interface OnMetadataUpdateListener
    {
        void onMetadataUpdate(int key, Object newValue);
    }

    public interface OnGetPlaybackPositionListener
    {
        long onGetPlaybackPosition();
    }

    public class MetadataEditor extends MediaMetadataEditor
    {
        public static final int BITMAP_KEY_ARTWORK = 100;

        private MetadataEditor()
        {
        }

        @Override
        public Object clone() throws CloneNotSupportedException
        {
            throw new CloneNotSupportedException();
        }

        @Override
        public synchronized MetadataEditor putString(int key, String value) throws IllegalArgumentException
        {
            super.putString(key, value);
            return this;
        }

        @Override
        public synchronized MetadataEditor putLong(int key, long value) throws IllegalArgumentException
        {
            super.putLong(key, value);
            return this;
        }

        @Override
        public synchronized MetadataEditor putBitmap(int key, Bitmap bitmap) throws IllegalArgumentException
        {
            super.putBitmap(key, bitmap);
            return this;
        }

        @Override
        public synchronized MetadataEditor putObject(int key, Object object) throws IllegalArgumentException
        {
            super.putObject(key, object);
            return this;
        }

        @Override
        public synchronized void clear()
        {
            super.clear();
        }

        @Override
        public synchronized void apply()
        {
            if (mApplied) return;

            synchronized (mCacheLock)
            {
                mMetadata = new Bundle(mEditorMetadata);
                mOriginalArtwork = mEditorArtwork;
                mMediaMetadata = toMediaMetadata();
            }

            if (mSession != null && (mMetadataChanged || mArtworkChanged)) mSession.setMetadata(mMediaMetadata);

            mApplied = true;
        }
    }
}
