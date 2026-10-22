package android.media.session;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.Rating;
import android.media.VolumeProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.view.KeyEvent;

import java.util.List;

public final class MediaController
{
    private final Context mContext;
    private final MediaSession.Token mToken;
    private final TransportControls mTransportControls;

    public MediaController(Context context, MediaSession.Token token)
    {
        if (context == null) throw new IllegalArgumentException("context shouldn't be null");
        if (token == null) throw new IllegalArgumentException("token shouldn't be null");

        mContext = context;
        mToken = token;
        mTransportControls = new TransportControls();
    }

    private MediaSession session()
    {
        MediaSession session = mToken.getSession();
        return session == null || session.isReleased() ? null : session;
    }

    private String callerPackageName()
    {
        return mContext.getPackageName();
    }

    public TransportControls getTransportControls()
    {
        return mTransportControls;
    }

    public boolean dispatchMediaButtonEvent(KeyEvent keyEvent)
    {
        if (keyEvent == null) throw new IllegalArgumentException("KeyEvent may not be null");

        switch (keyEvent.getKeyCode())
        {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_CLOSE:
            case KeyEvent.KEYCODE_MEDIA_EJECT:
            case KeyEvent.KEYCODE_MEDIA_RECORD:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                break;
            default:
                return false;
        }

        MediaSession session = session();
        if (session == null) return false;

        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);

        return session.dispatchToCallback(callerPackageName(), callback -> callback.onMediaButtonEvent(intent));
    }

    public PlaybackState getPlaybackState()
    {
        MediaSession session = session();
        return session == null ? null : session.getPlaybackState();
    }

    public MediaMetadata getMetadata()
    {
        MediaSession session = session();
        return session == null ? null : session.getMetadata();
    }

    public List<MediaSession.QueueItem> getQueue()
    {
        MediaSession session = session();
        return session == null ? null : session.getQueue();
    }

    public CharSequence getQueueTitle()
    {
        MediaSession session = session();
        return session == null ? null : session.getQueueTitle();
    }

    public Bundle getExtras()
    {
        MediaSession session = session();
        return session == null ? null : session.getExtras();
    }

    public int getRatingType()
    {
        MediaSession session = session();
        return session == null ? Rating.RATING_NONE : session.getRatingType();
    }

    public long getFlags()
    {
        MediaSession session = session();
        return session == null ? 0 : session.getFlags();
    }

    public PlaybackInfo getPlaybackInfo()
    {
        MediaSession session = session();
        if (session == null) return null;

        VolumeProvider provider = session.getVolumeProvider();

        if (provider != null)
        {
            return new PlaybackInfo(PlaybackInfo.PLAYBACK_TYPE_REMOTE, provider.getVolumeControl(), provider.getMaxVolume(),
                    provider.getCurrentVolume(), session.getAudioAttributes(), provider.getVolumeControlId());
        }

        AudioManager audioManager = audioManager();
        int maxVolume = audioManager == null ? 15 : audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager == null ? 0 : audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        return new PlaybackInfo(PlaybackInfo.PLAYBACK_TYPE_LOCAL, VolumeProvider.VOLUME_CONTROL_ABSOLUTE, maxVolume,
                currentVolume, session.getAudioAttributes(), null);
    }

    public PendingIntent getSessionActivity()
    {
        MediaSession session = session();
        return session == null ? null : session.getSessionActivity();
    }

    public MediaSession.Token getSessionToken()
    {
        return mToken;
    }

    public void setVolumeTo(int value, int flags)
    {
        MediaSession session = session();
        if (session == null) return;

        VolumeProvider provider = session.getVolumeProvider();

        if (provider != null)
        {
            if (provider.getVolumeControl() == VolumeProvider.VOLUME_CONTROL_ABSOLUTE) provider.onSetVolumeTo(value);
            return;
        }

        AudioManager audioManager = audioManager();
        if (audioManager != null) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, flags);
        session.notifyAudioInfoChanged();
    }

    public void adjustVolume(int direction, int flags)
    {
        MediaSession session = session();
        if (session == null) return;

        VolumeProvider provider = session.getVolumeProvider();

        if (provider != null)
        {
            if (provider.getVolumeControl() != VolumeProvider.VOLUME_CONTROL_FIXED) provider.onAdjustVolume(direction);
            return;
        }

        AudioManager audioManager = audioManager();
        if (audioManager != null) audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, flags);
        session.notifyAudioInfoChanged();
    }

    public void registerCallback(Callback callback)
    {
        registerCallback(callback, null);
    }

    public void registerCallback(Callback callback, Handler handler)
    {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");

        if (handler == null)
        {
            Looper looper = Looper.myLooper();
            handler = new Handler(looper != null ? looper : Looper.getMainLooper());
        }

        MediaSession session = session();
        if (session != null) session.addControllerCallback(callback, handler);
    }

    public void unregisterCallback(Callback callback)
    {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");

        MediaSession session = session();
        if (session != null) session.removeControllerCallback(callback);
    }

    public void sendCommand(String command, Bundle args, ResultReceiver cb)
    {
        if (TextUtils.isEmpty(command)) throw new IllegalArgumentException("command cannot be null or empty");

        MediaSession session = session();
        if (session != null) session.dispatchToCallback(callerPackageName(), callback -> callback.onCommand(command, args, cb));
    }

    public String getPackageName()
    {
        MediaSession session = session();
        return session == null ? mToken.mPackageName : session.getContext().getPackageName();
    }

    public Bundle getSessionInfo()
    {
        MediaSession session = session();
        return session == null ? new Bundle() : new Bundle(session.getSessionInfo());
    }

    public String getTag()
    {
        MediaSession session = session();
        return session == null ? null : session.getTag();
    }

    private AudioManager audioManager()
    {
        Object service = mContext.getSystemService(Context.AUDIO_SERVICE);
        return service instanceof AudioManager ? (AudioManager)service : null;
    }

    private void dispatch(MediaSession.CallbackAction action)
    {
        MediaSession session = session();
        if (session != null) session.dispatchToCallback(callerPackageName(), action);
    }

    public final class TransportControls
    {
        private TransportControls()
        {
        }

        public void prepare()
        {
            dispatch(callback -> callback.onPrepare());
        }

        public void prepareFromMediaId(String mediaId, Bundle extras)
        {
            if (TextUtils.isEmpty(mediaId))
            {
                throw new IllegalArgumentException("You must specify a non-empty String for prepareFromMediaId.");
            }

            dispatch(callback -> callback.onPrepareFromMediaId(mediaId, extras));
        }

        public void prepareFromSearch(String query, Bundle extras)
        {
            String safeQuery = query == null ? "" : query;
            dispatch(callback -> callback.onPrepareFromSearch(safeQuery, extras));
        }

        public void prepareFromUri(Uri uri, Bundle extras)
        {
            if (uri == null || Uri.EMPTY.equals(uri))
            {
                throw new IllegalArgumentException("You must specify a non-empty Uri for prepareFromUri.");
            }

            dispatch(callback -> callback.onPrepareFromUri(uri, extras));
        }

        public void play()
        {
            dispatch(callback -> callback.onPlay());
        }

        public void playFromMediaId(String mediaId, Bundle extras)
        {
            if (TextUtils.isEmpty(mediaId))
            {
                throw new IllegalArgumentException("You must specify a non-empty String for playFromMediaId.");
            }

            dispatch(callback -> callback.onPlayFromMediaId(mediaId, extras));
        }

        public void playFromSearch(String query, Bundle extras)
        {
            String safeQuery = query == null ? "" : query;
            dispatch(callback -> callback.onPlayFromSearch(safeQuery, extras));
        }

        public void playFromUri(Uri uri, Bundle extras)
        {
            if (uri == null || Uri.EMPTY.equals(uri))
            {
                throw new IllegalArgumentException("You must specify a non-empty Uri for playFromUri.");
            }

            dispatch(callback -> callback.onPlayFromUri(uri, extras));
        }

        public void skipToQueueItem(long id)
        {
            dispatch(callback -> callback.onSkipToQueueItem(id));
        }

        public void pause()
        {
            dispatch(callback -> callback.onPause());
        }

        public void stop()
        {
            dispatch(callback -> callback.onStop());
        }

        public void seekTo(long pos)
        {
            dispatch(callback -> callback.onSeekTo(pos));
        }

        public void fastForward()
        {
            dispatch(callback -> callback.onFastForward());
        }

        public void skipToNext()
        {
            dispatch(callback -> callback.onSkipToNext());
        }

        public void rewind()
        {
            dispatch(callback -> callback.onRewind());
        }

        public void skipToPrevious()
        {
            dispatch(callback -> callback.onSkipToPrevious());
        }

        public void setRating(Rating rating)
        {
            dispatch(callback -> callback.onSetRating(rating));
        }

        public void setPlaybackSpeed(float speed)
        {
            if (speed == 0.0f) throw new IllegalArgumentException("speed must not be zero");

            dispatch(callback -> callback.onSetPlaybackSpeed(speed));
        }

        public void sendCustomAction(PlaybackState.CustomAction customAction, Bundle args)
        {
            if (customAction == null) throw new IllegalArgumentException("CustomAction cannot be null.");

            sendCustomAction(customAction.getAction(), args);
        }

        public void sendCustomAction(String action, Bundle args)
        {
            if (TextUtils.isEmpty(action)) throw new IllegalArgumentException("CustomAction cannot be null.");

            dispatch(callback -> callback.onCustomAction(action, args));
        }
    }

    public static final class PlaybackInfo implements Parcelable
    {
        public static final int PLAYBACK_TYPE_LOCAL = 1;
        public static final int PLAYBACK_TYPE_REMOTE = 2;

        public static final Parcelable.Creator<PlaybackInfo> CREATOR = new Parcelable.Creator<PlaybackInfo>()
        {
            @Override
            public PlaybackInfo createFromParcel(Parcel source)
            {
                int playbackType = source.readInt();
                int volumeControl = source.readInt();
                int maxVolume = source.readInt();
                int currentVolume = source.readInt();
                AudioAttributes audioAttributes = source.readParcelable(null, AudioAttributes.class);
                String volumeControlId = source.readString();
                return new PlaybackInfo(playbackType, volumeControl, maxVolume, currentVolume, audioAttributes, volumeControlId);
            }

            @Override
            public PlaybackInfo[] newArray(int size)
            {
                return new PlaybackInfo[size];
            }
        };

        private final int mPlaybackType;
        private final int mVolumeControl;
        private final int mMaxVolume;
        private final int mCurrentVolume;
        private final AudioAttributes mAudioAttributes;
        private final String mVolumeControlId;

        PlaybackInfo(int playbackType, int volumeControl, int maxVolume, int currentVolume, AudioAttributes audioAttributes,
                String volumeControlId)
        {
            mPlaybackType = playbackType;
            mVolumeControl = volumeControl;
            mMaxVolume = maxVolume;
            mCurrentVolume = currentVolume;
            mAudioAttributes = audioAttributes;
            mVolumeControlId = volumeControlId;
        }

        public int getPlaybackType()
        {
            return mPlaybackType;
        }

        public AudioAttributes getAudioAttributes()
        {
            return mAudioAttributes;
        }

        public int getVolumeControl()
        {
            return mVolumeControl;
        }

        public int getMaxVolume()
        {
            return mMaxVolume;
        }

        public int getCurrentVolume()
        {
            return mCurrentVolume;
        }

        public String getVolumeControlId()
        {
            return mVolumeControlId;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(mPlaybackType);
            dest.writeInt(mVolumeControl);
            dest.writeInt(mMaxVolume);
            dest.writeInt(mCurrentVolume);
            dest.writeParcelable(mAudioAttributes, flags);
            dest.writeString(mVolumeControlId);
        }

        @Override
        public String toString()
        {
            return "playbackType=" + mPlaybackType + ", volumeControlType=" + mVolumeControl + ", maxVolume=" + mMaxVolume
                    + ", currentVolume=" + mCurrentVolume + ", audioAttrs=" + mAudioAttributes + ", volumeControlId="
                    + mVolumeControlId;
        }
    }

    public abstract static class Callback
    {
        public Callback()
        {
        }

        public void onSessionDestroyed()
        {
        }

        public void onSessionEvent(String event, Bundle extras)
        {
        }

        public void onPlaybackStateChanged(PlaybackState state)
        {
        }

        public void onMetadataChanged(MediaMetadata metadata)
        {
        }

        public void onQueueChanged(List<MediaSession.QueueItem> queue)
        {
        }

        public void onQueueTitleChanged(CharSequence title)
        {
        }

        public void onExtrasChanged(Bundle extras)
        {
        }

        public void onAudioInfoChanged(PlaybackInfo info)
        {
        }
    }
}
