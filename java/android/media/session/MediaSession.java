package android.media.session;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.VolumeProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MediaSession
{
    public static final int FLAG_HANDLES_MEDIA_BUTTONS = 1;
    public static final int FLAG_HANDLES_TRANSPORT_CONTROLS = 2;

    private static final Map<Integer, MediaSession> SESSIONS = new HashMap<>();
    private static final List<MediaSession> ACTIVE_SESSIONS = new ArrayList<>();
    private static int sNextSessionId = 1;

    private final Object mLock = new Object();
    private final Context mContext;
    private final String mTag;
    private final Bundle mSessionInfo;
    private final Token mSessionToken;
    private final MediaController mController;
    private final List<ControllerCallbackRecord> mControllerCallbacks = new ArrayList<>();

    private Callback mCallback;
    private Handler mCallbackHandler;
    private boolean mActive;
    private boolean mReleased;
    private int mFlags;
    private int mRatingType = Rating.RATING_NONE;
    private MediaMetadata mMetadata;
    private PlaybackState mPlaybackState;
    private List<QueueItem> mQueue;
    private CharSequence mQueueTitle;
    private Bundle mExtras;
    private PendingIntent mSessionActivity;
    private PendingIntent mMediaButtonReceiver;
    private ComponentName mMediaButtonBroadcastReceiver;
    private AudioAttributes mAudioAttributes;
    private VolumeProvider mVolumeProvider;
    private MediaSessionManager.RemoteUserInfo mCurrentControllerInfo;

    public MediaSession(Context context, String tag)
    {
        this(context, tag, null);
    }

    public MediaSession(Context context, String tag, Bundle sessionInfo)
    {
        if (context == null) throw new IllegalArgumentException("context cannot be null.");
        if (TextUtils.isEmpty(tag)) throw new IllegalArgumentException("tag cannot be null or empty");

        mContext = context;
        mTag = tag;
        mSessionInfo = sessionInfo == null ? new Bundle() : new Bundle(sessionInfo);
        mAudioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();

        int id;

        synchronized (SESSIONS)
        {
            id = sNextSessionId++;
            SESSIONS.put(id, this);
        }

        mSessionToken = new Token(id, context.getPackageName());
        mController = new MediaController(context, mSessionToken);
    }

    static MediaSession findSession(int id)
    {
        synchronized (SESSIONS)
        {
            return SESSIONS.get(id);
        }
    }

    static List<MediaSession> getActiveSessionsSnapshot()
    {
        synchronized (SESSIONS)
        {
            return new ArrayList<>(ACTIVE_SESSIONS);
        }
    }

    public void setCallback(Callback callback)
    {
        setCallback(callback, null);
    }

    public void setCallback(Callback callback, Handler handler)
    {
        synchronized (mLock)
        {
            if (mCallback != null) mCallback.mSession = null;

            if (callback == null)
            {
                mCallback = null;
                mCallbackHandler = null;
                return;
            }

            if (handler == null)
            {
                Looper looper = Looper.myLooper();
                handler = new Handler(looper != null ? looper : Looper.getMainLooper());
            }

            callback.mSession = this;
            mCallback = callback;
            mCallbackHandler = handler;
        }
    }

    public void setSessionActivity(PendingIntent pendingIntent)
    {
        mSessionActivity = pendingIntent;
    }

    public void setMediaButtonReceiver(PendingIntent mediaButtonIntent)
    {
        mMediaButtonReceiver = mediaButtonIntent;
    }

    public void setMediaButtonBroadcastReceiver(ComponentName broadcastReceiver)
    {
        mMediaButtonBroadcastReceiver = broadcastReceiver;
    }

    public void setFlags(int flags)
    {
        mFlags = flags;
    }

    public void setPlaybackToLocal(AudioAttributes attributes)
    {
        if (attributes == null) throw new IllegalArgumentException("Attributes cannot be null for local playback.");

        mAudioAttributes = attributes;
        mVolumeProvider = null;
        notifyAudioInfoChanged();
    }

    public void setPlaybackToRemote(VolumeProvider volumeProvider)
    {
        if (volumeProvider == null) throw new IllegalArgumentException("volumeProvider may not be null!");

        mVolumeProvider = volumeProvider;
        notifyAudioInfoChanged();
    }

    public void setActive(boolean active)
    {
        if (mActive == active) return;

        mActive = active;

        synchronized (SESSIONS)
        {
            ACTIVE_SESSIONS.remove(this);
            if (active && !mReleased) ACTIVE_SESSIONS.add(0, this);
        }

        MediaSessionManager.dispatchActiveSessionsChanged();
    }

    public boolean isActive()
    {
        return mActive;
    }

    public void sendSessionEvent(String event, Bundle extras)
    {
        if (TextUtils.isEmpty(event)) throw new IllegalArgumentException("event cannot be null or empty");

        for (ControllerCallbackRecord record : controllerCallbacks())
        {
            record.post(() -> record.mCallback.onSessionEvent(event, extras));
        }
    }

    public void release()
    {
        if (mReleased) return;

        mReleased = true;
        boolean wasActive = mActive;
        mActive = false;

        synchronized (SESSIONS)
        {
            SESSIONS.remove(mSessionToken.mId);
            ACTIVE_SESSIONS.remove(this);
        }

        for (ControllerCallbackRecord record : controllerCallbacks())
        {
            record.post(() -> record.mCallback.onSessionDestroyed());
        }

        synchronized (mLock)
        {
            mControllerCallbacks.clear();
            if (mCallback != null) mCallback.mSession = null;
            mCallback = null;
            mCallbackHandler = null;
        }

        if (wasActive) MediaSessionManager.dispatchActiveSessionsChanged();
    }

    public Token getSessionToken()
    {
        return mSessionToken;
    }

    public MediaController getController()
    {
        return mController;
    }

    public void setPlaybackState(PlaybackState state)
    {
        mPlaybackState = state;

        for (ControllerCallbackRecord record : controllerCallbacks())
        {
            record.post(() -> record.mCallback.onPlaybackStateChanged(state));
        }
    }

    public void setMetadata(MediaMetadata metadata)
    {
        mMetadata = metadata;

        for (ControllerCallbackRecord record : controllerCallbacks())
        {
            record.post(() -> record.mCallback.onMetadataChanged(metadata));
        }
    }

    public void setQueue(List<QueueItem> queue)
    {
        mQueue = queue == null ? null : new ArrayList<>(queue);
        List<QueueItem> snapshot = mQueue == null ? null : new ArrayList<>(mQueue);

        for (ControllerCallbackRecord record : controllerCallbacks())
        {
            record.post(() -> record.mCallback.onQueueChanged(snapshot));
        }
    }

    public void setQueueTitle(CharSequence title)
    {
        mQueueTitle = title;

        for (ControllerCallbackRecord record : controllerCallbacks())
        {
            record.post(() -> record.mCallback.onQueueTitleChanged(title));
        }
    }

    public void setRatingType(int type)
    {
        mRatingType = type;
    }

    public void setExtras(Bundle extras)
    {
        mExtras = extras;

        for (ControllerCallbackRecord record : controllerCallbacks())
        {
            record.post(() -> record.mCallback.onExtrasChanged(extras));
        }
    }

    public MediaSessionManager.RemoteUserInfo getCurrentControllerInfo()
    {
        MediaSessionManager.RemoteUserInfo info = mCurrentControllerInfo;

        if (info == null)
        {
            throw new IllegalStateException("This should be called inside of MediaSession.Callback methods");
        }

        return info;
    }

    Context getContext()
    {
        return mContext;
    }

    String getTag()
    {
        return mTag;
    }

    Bundle getSessionInfo()
    {
        return mSessionInfo;
    }

    int getFlags()
    {
        return mFlags;
    }

    int getRatingType()
    {
        return mRatingType;
    }

    MediaMetadata getMetadata()
    {
        return mMetadata;
    }

    PlaybackState getPlaybackState()
    {
        return mPlaybackState;
    }

    List<QueueItem> getQueue()
    {
        return mQueue == null ? null : new ArrayList<>(mQueue);
    }

    CharSequence getQueueTitle()
    {
        return mQueueTitle;
    }

    Bundle getExtras()
    {
        return mExtras;
    }

    PendingIntent getSessionActivity()
    {
        return mSessionActivity;
    }

    AudioAttributes getAudioAttributes()
    {
        return mAudioAttributes;
    }

    VolumeProvider getVolumeProvider()
    {
        return mVolumeProvider;
    }

    boolean isReleased()
    {
        return mReleased;
    }

    void addControllerCallback(MediaController.Callback callback, Handler handler)
    {
        synchronized (mLock)
        {
            for (ControllerCallbackRecord record : mControllerCallbacks)
            {
                if (record.mCallback == callback) return;
            }

            mControllerCallbacks.add(new ControllerCallbackRecord(callback, handler));
        }
    }

    boolean removeControllerCallback(MediaController.Callback callback)
    {
        synchronized (mLock)
        {
            for (int index = 0; index < mControllerCallbacks.size(); index++)
            {
                if (mControllerCallbacks.get(index).mCallback == callback)
                {
                    mControllerCallbacks.remove(index);
                    return true;
                }
            }

            return false;
        }
    }

    void notifyAudioInfoChanged()
    {
        MediaController.PlaybackInfo info = mController.getPlaybackInfo();

        for (ControllerCallbackRecord record : controllerCallbacks())
        {
            record.post(() -> record.mCallback.onAudioInfoChanged(info));
        }
    }

    boolean dispatchToCallback(String packageName, CallbackAction action)
    {
        Callback callback;
        Handler handler;

        synchronized (mLock)
        {
            callback = mCallback;
            handler = mCallbackHandler;
        }

        if (callback == null || handler == null || mReleased) return false;

        MediaSessionManager.RemoteUserInfo info = new MediaSessionManager.RemoteUserInfo(packageName, Process.myPid(),
                Process.myUid());

        handler.post(() ->
        {
            MediaSessionManager.RemoteUserInfo previous = mCurrentControllerInfo;
            mCurrentControllerInfo = info;

            try
            {
                action.run(callback);
            }
            finally
            {
                mCurrentControllerInfo = previous;
            }
        });

        return true;
    }

    private List<ControllerCallbackRecord> controllerCallbacks()
    {
        synchronized (mLock)
        {
            return new ArrayList<>(mControllerCallbacks);
        }
    }

    interface CallbackAction
    {
        void run(Callback callback);
    }

    private static final class ControllerCallbackRecord
    {
        final MediaController.Callback mCallback;
        final Handler mHandler;

        ControllerCallbackRecord(MediaController.Callback callback, Handler handler)
        {
            mCallback = callback;
            mHandler = handler;
        }

        void post(Runnable runnable)
        {
            mHandler.post(runnable);
        }
    }

    public static final class Token implements Parcelable
    {
        public static final Parcelable.Creator<Token> CREATOR = new Parcelable.Creator<Token>()
        {
            @Override
            public Token createFromParcel(Parcel source)
            {
                int id = source.readInt();
                String packageName = source.readString();
                return new Token(id, packageName);
            }

            @Override
            public Token[] newArray(int size)
            {
                return new Token[size];
            }
        };

        final int mId;
        final String mPackageName;

        Token(int id, String packageName)
        {
            mId = id;
            mPackageName = packageName;
        }

        MediaSession getSession()
        {
            return findSession(mId);
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(mId);
            dest.writeString(mPackageName);
        }

        @Override
        public boolean equals(Object object)
        {
            if (object == this) return true;
            if (!(object instanceof Token)) return false;

            Token other = (Token)object;

            return mId == other.mId && Objects.equals(mPackageName, other.mPackageName);
        }

        @Override
        public int hashCode()
        {
            return 31 * mId + Objects.hashCode(mPackageName);
        }
    }

    public static final class QueueItem implements Parcelable
    {
        public static final int UNKNOWN_ID = -1;

        public static final Parcelable.Creator<QueueItem> CREATOR = new Parcelable.Creator<QueueItem>()
        {
            @Override
            public QueueItem createFromParcel(Parcel source)
            {
                MediaDescription description = MediaDescription.CREATOR.createFromParcel(source);
                long id = source.readLong();
                return new QueueItem(description, id);
            }

            @Override
            public QueueItem[] newArray(int size)
            {
                return new QueueItem[size];
            }
        };

        private final MediaDescription mDescription;
        private final long mId;

        public QueueItem(MediaDescription description, long id)
        {
            if (description == null) throw new IllegalArgumentException("Description cannot be null.");
            if (id == UNKNOWN_ID) throw new IllegalArgumentException("Id cannot be QueueItem.UNKNOWN_ID");

            mDescription = description;
            mId = id;
        }

        public MediaDescription getDescription()
        {
            return mDescription;
        }

        public long getQueueId()
        {
            return mId;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            mDescription.writeToParcel(dest, flags);
            dest.writeLong(mId);
        }

        @Override
        public boolean equals(Object object)
        {
            if (object == this) return true;
            if (!(object instanceof QueueItem)) return false;

            QueueItem other = (QueueItem)object;

            return mId == other.mId && Objects.equals(mDescription, other.mDescription);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mDescription, mId);
        }

        @Override
        public String toString()
        {
            return "MediaSession.QueueItem {Description=" + mDescription + ", Id=" + mId + " }";
        }
    }

    public abstract static class Callback
    {
        MediaSession mSession;

        public Callback()
        {
        }

        public void onCommand(String command, Bundle args, ResultReceiver cb)
        {
        }

        public boolean onMediaButtonEvent(Intent mediaButtonIntent)
        {
            MediaSession session = mSession;

            if (session == null || mediaButtonIntent == null) return false;
            if (!Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) return false;

            KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (keyEvent == null || keyEvent.getAction() != KeyEvent.ACTION_DOWN) return false;

            PlaybackState state = session.getPlaybackState();
            long validActions = state == null ? 0 : state.getActions();

            switch (keyEvent.getKeyCode())
            {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    onPlay();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    onPause();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    if ((validActions & PlaybackState.ACTION_SKIP_TO_NEXT) == 0) return false;
                    onSkipToNext();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    if ((validActions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) == 0) return false;
                    onSkipToPrevious();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    if ((validActions & PlaybackState.ACTION_STOP) == 0) return false;
                    onStop();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    if ((validActions & PlaybackState.ACTION_FAST_FORWARD) == 0) return false;
                    onFastForward();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    if ((validActions & PlaybackState.ACTION_REWIND) == 0) return false;
                    onRewind();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_HEADSETHOOK:
                    if (keyEvent.getRepeatCount() > 0) return true;
                    if (state != null && state.isActive()) onPause();
                    else onPlay();
                    return true;
                default:
                    return false;
            }
        }

        public void onPrepare()
        {
        }

        public void onPrepareFromMediaId(String mediaId, Bundle extras)
        {
        }

        public void onPrepareFromSearch(String query, Bundle extras)
        {
        }

        public void onPrepareFromUri(Uri uri, Bundle extras)
        {
        }

        public void onPlay()
        {
        }

        public void onPlayFromSearch(String query, Bundle extras)
        {
        }

        public void onPlayFromMediaId(String mediaId, Bundle extras)
        {
        }

        public void onPlayFromUri(Uri uri, Bundle extras)
        {
        }

        public void onSkipToQueueItem(long id)
        {
        }

        public void onPause()
        {
        }

        public void onSkipToNext()
        {
        }

        public void onSkipToPrevious()
        {
        }

        public void onFastForward()
        {
        }

        public void onRewind()
        {
        }

        public void onStop()
        {
        }

        public void onSeekTo(long pos)
        {
        }

        public void onSetRating(Rating rating)
        {
        }

        public void onSetPlaybackSpeed(float speed)
        {
        }

        public void onCustomAction(String action, Bundle extras)
        {
        }
    }
}
