package android.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MediaPlayer implements AudioRouting, VolumeAutomation
{
    public static final int MEDIA_ERROR_IO = -1004;
    public static final int MEDIA_ERROR_MALFORMED = -1007;
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_ERROR_TIMED_OUT = -110;
    public static final int MEDIA_ERROR_UNKNOWN = 1;
    public static final int MEDIA_ERROR_UNSUPPORTED = -1010;
    public static final int MEDIA_INFO_AUDIO_NOT_PLAYING = 804;
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;
    public static final int MEDIA_INFO_BUFFERING_END = 702;
    public static final int MEDIA_INFO_BUFFERING_START = 701;
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
    public static final int MEDIA_INFO_STARTED_AS_NEXT = 2;
    public static final int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;
    public static final int MEDIA_INFO_UNKNOWN = 1;
    public static final int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    public static final int MEDIA_INFO_VIDEO_NOT_PLAYING = 805;
    public static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    public static final String MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip";
    public static final int PREPARE_DRM_STATUS_PREPARATION_ERROR = 3;
    public static final int PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR = 1;
    public static final int PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR = 2;
    public static final int PREPARE_DRM_STATUS_SUCCESS = 0;
    public static final int SEEK_CLOSEST = 3;
    public static final int SEEK_CLOSEST_SYNC = 2;
    public static final int SEEK_NEXT_SYNC = 1;
    public static final int SEEK_PREVIOUS_SYNC = 0;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;

    private static final int INVALID_OPERATION = -38;

    private static final int STATE_IDLE = 0;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_PREPARING = 2;
    private static final int STATE_ERROR = 3;
    private static final int STATE_END = 4;

    private final Object mLock = new Object();
    private final Handler mEventHandler;
    private final Map<AudioRouting.OnRoutingChangedListener, Handler> mRoutingListeners = new HashMap<>();

    private int mState = STATE_IDLE;
    private int mAudioSessionId;
    private AudioAttributes mAudioAttributes;
    private boolean mLooping;
    private float mLeftVolume = 1.0f;
    private float mRightVolume = 1.0f;
    private float mAuxSendLevel;
    private int mVideoScalingMode = VIDEO_SCALING_MODE_SCALE_TO_FIT;
    private PlaybackParams mPlaybackParams = new PlaybackParams().allowDefaults();
    private SyncParams mSyncParams = new SyncParams().allowDefaults();
    private AudioDeviceInfo mPreferredDevice;
    private Surface mSurface;
    private SurfaceHolder mSurfaceHolder;
    private boolean mScreenOnWhilePlaying;
    private MediaPlayer mNextMediaPlayer;

    private OnPreparedListener mOnPreparedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private OnTimedTextListener mOnTimedTextListener;
    private OnTimedMetaDataAvailableListener mOnTimedMetaDataAvailableListener;
    private OnSubtitleDataListener mOnSubtitleDataListener;
    private OnMediaTimeDiscontinuityListener mOnMediaTimeDiscontinuityListener;
    private OnDrmConfigHelper mOnDrmConfigHelper;
    private OnDrmInfoListener mOnDrmInfoListener;
    private OnDrmPreparedListener mOnDrmPreparedListener;

    public MediaPlayer()
    {
        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();

        mEventHandler = looper == null ? null : new Handler(looper);
        mAudioSessionId = AudioTrack.newAudioSessionId();
        mAudioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();
    }

    public MediaPlayer(Context context)
    {
        this();
        if (context == null) throw new IllegalArgumentException("context cannot be null");
    }

    @Override
    public void addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener, Handler handler)
    {
        if (listener == null) return;

        synchronized (mRoutingListeners)
        {
            mRoutingListeners.put(listener, handler != null ? handler : mEventHandler);
        }
    }

    public void addTimedTextSource(Context context, Uri uri, String mimeType)
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        checkTimedTextMime(mimeType);
        if (context == null || uri == null) throw new IllegalArgumentException("context and uri must not be null");
        checkTimedTextState();
    }

    public void addTimedTextSource(FileDescriptor descriptor, String mimeType) throws IllegalArgumentException, IllegalStateException
    {
        addTimedTextSource(descriptor, 0, 0x7ffffffffffffffL, mimeType);
    }

    public void addTimedTextSource(FileDescriptor descriptor, long offset, long length, String mimeType)
        throws IllegalArgumentException, IllegalStateException
    {
        checkTimedTextMime(mimeType);
        if (descriptor == null) throw new IllegalArgumentException("fd must not be null");
        checkTimedTextState();
    }

    public void addTimedTextSource(String path, String mimeType) throws IOException, IllegalArgumentException, IllegalStateException
    {
        checkTimedTextMime(mimeType);

        File file = new File(path);
        if (!file.exists()) throw new IOException(path);
        checkTimedTextState();
    }

    private static void checkTimedTextMime(String mimeType)
    {
        if (!MEDIA_MIMETYPE_TEXT_SUBRIP.equals(mimeType))
        {
            throw new IllegalArgumentException("Illegal mimeType for timed text source: " + mimeType);
        }
    }

    private void checkTimedTextState()
    {
        synchronized (mLock)
        {
            if (mState != STATE_IDLE && mState != STATE_INITIALIZED)
            {
                throw new IllegalStateException("addTimedTextSource called in state " + mState);
            }
        }
    }

    public void attachAuxEffect(int effectId)
    {
        synchronized (mLock)
        {
            if (mState == STATE_IDLE || mState == STATE_ERROR || mState == STATE_END) postErrorLocked(INVALID_OPERATION, 0);
        }
    }

    public void clearOnMediaTimeDiscontinuityListener()
    {
        setOnMediaTimeDiscontinuityListener(null, null);
    }

    public void clearOnSubtitleDataListener()
    {
        setOnSubtitleDataListener(null, null);
    }

    public static MediaPlayer create(Context context, Uri uri)
    {
        return create(context, uri, null);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder)
    {
        return create(context, uri, holder, null, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    public static MediaPlayer create(Context context, Uri uri, SurfaceHolder holder, AudioAttributes audioAttributes, int audioSessionId)
    {
        MediaPlayer player = new MediaPlayer();

        try
        {
            if (audioAttributes != null) player.setAudioAttributes(audioAttributes);
            if (audioSessionId != AudioManager.AUDIO_SESSION_ID_GENERATE) player.setAudioSessionId(audioSessionId);
            player.setDataSource(context, uri);
            if (holder != null) player.setDisplay(holder);
            player.prepare();
            return player;
        }
        catch (IOException | IllegalArgumentException | SecurityException exception)
        {
            player.release();
            return null;
        }
    }

    public static MediaPlayer create(Context context, int resid)
    {
        return create(context, resid, null, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    public static MediaPlayer create(Context context, int resid, AudioAttributes audioAttributes, int audioSessionId)
    {
        AssetFileDescriptor descriptor;

        try
        {
            descriptor = context.getResources().openRawResourceFd(resid);
        }
        catch (RuntimeException exception)
        {
            return null;
        }

        if (descriptor == null) return null;

        MediaPlayer player = new MediaPlayer();

        try
        {
            if (audioAttributes != null) player.setAudioAttributes(audioAttributes);
            if (audioSessionId != AudioManager.AUDIO_SESSION_ID_GENERATE) player.setAudioSessionId(audioSessionId);
            player.setDataSource(descriptor);
            player.prepare();
            return player;
        }
        catch (IOException | IllegalArgumentException | SecurityException exception)
        {
            player.release();
            return null;
        }
        finally
        {
            try
            {
                descriptor.close();
            }
            catch (IOException exception)
            {
            }
        }
    }

    @Override
    public VolumeShaper createVolumeShaper(VolumeShaper.Configuration configuration)
    {
        if (configuration == null) throw new IllegalArgumentException("configuration must not be null");
        return new VolumeShaper(configuration);
    }

    public void deselectTrack(int index) throws IllegalStateException
    {
        selectOrDeselectTrack(index);
    }

    public int getAudioSessionId()
    {
        synchronized (mLock)
        {
            return mAudioSessionId;
        }
    }

    public int getCurrentPosition()
    {
        synchronized (mLock)
        {
            if (mState == STATE_ERROR || mState == STATE_END) postErrorLocked(INVALID_OPERATION, 0);
            return 0;
        }
    }

    public MediaPlayer.DrmInfo getDrmInfo()
    {
        return null;
    }

    public String getDrmPropertyString(String propertyName) throws MediaPlayer.NoDrmSchemeException
    {
        throw new NoDrmSchemeException("getDrmPropertyString NoDrmSchemeException");
    }

    public int getDuration()
    {
        synchronized (mLock)
        {
            if (mState != STATE_PREPARING) postErrorLocked(INVALID_OPERATION, 0);
            return -1;
        }
    }

    public MediaDrm.KeyRequest getKeyRequest(byte[] keySetId, byte[] initData, String mimeType, int keyType,
        Map<String, String> optionalParameters) throws MediaPlayer.NoDrmSchemeException
    {
        throw new NoDrmSchemeException("getKeyRequest NoDrmSchemeException");
    }

    public PersistableBundle getMetrics()
    {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(MetricsConstants.ERRORS, mState == STATE_ERROR ? 1 : 0);
        bundle.putInt(MetricsConstants.ERROR_CODE, mState == STATE_ERROR ? MEDIA_ERROR_UNSUPPORTED : 0);
        bundle.putLong(MetricsConstants.DURATION, 0);
        bundle.putLong(MetricsConstants.PLAYING, 0);
        return bundle;
    }

    public PlaybackParams getPlaybackParams()
    {
        synchronized (mLock)
        {
            return new PlaybackParams().setSpeed(mPlaybackParams.getSpeed()).setPitch(mPlaybackParams.getPitch())
                .setAudioFallbackMode(mPlaybackParams.getAudioFallbackMode());
        }
    }

    @Override
    public AudioDeviceInfo getPreferredDevice()
    {
        synchronized (mLock)
        {
            return mPreferredDevice;
        }
    }

    @Override
    public AudioDeviceInfo getRoutedDevice()
    {
        return null;
    }

    @Override
    public List<AudioDeviceInfo> getRoutedDevices()
    {
        return Collections.emptyList();
    }

    public int getSelectedTrack(int trackType) throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_PREPARING) throw new IllegalStateException("getSelectedTrack called in an invalid state");
            return -1;
        }
    }

    public SyncParams getSyncParams()
    {
        synchronized (mLock)
        {
            return new SyncParams().setSyncSource(mSyncParams.getSyncSource()).setAudioAdjustMode(mSyncParams.getAudioAdjustMode())
                .setTolerance(mSyncParams.getTolerance());
        }
    }

    public MediaTimestamp getTimestamp()
    {
        return null;
    }

    public MediaPlayer.TrackInfo[] getTrackInfo() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_PREPARING) throw new IllegalStateException("getTrackInfo called in an invalid state");
            return new TrackInfo[0];
        }
    }

    public int getVideoHeight()
    {
        return 0;
    }

    public int getVideoWidth()
    {
        return 0;
    }

    public boolean isLooping()
    {
        synchronized (mLock)
        {
            return mLooping;
        }
    }

    public boolean isPlaying()
    {
        synchronized (mLock)
        {
            if (mState == STATE_END) throw new IllegalStateException("isPlaying called after release");
            return false;
        }
    }

    public void pause() throws IllegalStateException
    {
        invalidOperation();
    }

    public void prepare() throws IOException, IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("prepare called in state " + mState);
            mState = STATE_ERROR;
        }

        throw new IOException("Prepare failed.: status=0x1");
    }

    public void prepareAsync() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("prepareAsync called in state " + mState);
            mState = STATE_PREPARING;
            postErrorLocked(MEDIA_ERROR_UNKNOWN, MEDIA_ERROR_UNSUPPORTED);
        }
    }

    public void prepareDrm(UUID uuid) throws MediaPlayer.ProvisioningNetworkErrorException,
        MediaPlayer.ProvisioningServerErrorException, ResourceBusyException, UnsupportedSchemeException
    {
        if (uuid == null) throw new IllegalArgumentException("Null UUID");
        throw new IllegalStateException("prepareDrm(): Wrong usage: The player must be prepared and DRM info be retrieved "
            + "before this call.");
    }

    public byte[] provideKeyResponse(byte[] keySetId, byte[] response) throws DeniedByServerException, MediaPlayer.NoDrmSchemeException
    {
        throw new NoDrmSchemeException("provideKeyResponse NoDrmSchemeException");
    }

    public void release()
    {
        synchronized (mLock)
        {
            mState = STATE_END;
            mOnPreparedListener = null;
            mOnCompletionListener = null;
            mOnBufferingUpdateListener = null;
            mOnSeekCompleteListener = null;
            mOnVideoSizeChangedListener = null;
            mOnErrorListener = null;
            mOnInfoListener = null;
            mOnTimedTextListener = null;
            mOnTimedMetaDataAvailableListener = null;
            mOnSubtitleDataListener = null;
            mOnMediaTimeDiscontinuityListener = null;
            mOnDrmConfigHelper = null;
            mOnDrmInfoListener = null;
            mOnDrmPreparedListener = null;
            mSurface = null;
            mSurfaceHolder = null;
            mNextMediaPlayer = null;
        }
    }

    @Override
    protected void finalize()
    {
        release();
    }

    public void releaseDrm() throws MediaPlayer.NoDrmSchemeException
    {
        throw new NoDrmSchemeException("releaseDrm: No active DRM scheme to release.");
    }

    @Override
    public void removeOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener)
    {
        synchronized (mRoutingListeners)
        {
            mRoutingListeners.remove(listener);
        }
    }

    public void reset()
    {
        synchronized (mLock)
        {
            if (mState != STATE_END) mState = STATE_IDLE;
            mLooping = false;
            mNextMediaPlayer = null;
        }
    }

    public void restoreKeys(byte[] keySetId) throws MediaPlayer.NoDrmSchemeException
    {
        throw new NoDrmSchemeException("restoreKeys NoDrmSchemeException");
    }

    public void seekTo(int msec) throws IllegalStateException
    {
        seekTo(msec, SEEK_PREVIOUS_SYNC);
    }

    public void seekTo(long msec, int mode)
    {
        if (mode < SEEK_PREVIOUS_SYNC || mode > SEEK_CLOSEST) throw new IllegalArgumentException("Illegal seek mode: " + mode);
        invalidOperation();
    }

    public void selectTrack(int index) throws IllegalStateException
    {
        selectOrDeselectTrack(index);
    }

    private void selectOrDeselectTrack(int index)
    {
        synchronized (mLock)
        {
            if (mState != STATE_PREPARING) throw new IllegalStateException("selectTrack called in an invalid state");
        }

        throw new IllegalArgumentException("Invalid track index: " + index);
    }

    public void setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException
    {
        if (attributes == null) throw new IllegalArgumentException("Cannot set AudioAttributes to null");

        synchronized (mLock)
        {
            mAudioAttributes = attributes;
        }
    }

    public void setAudioSessionId(int sessionId) throws IllegalArgumentException, IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_IDLE) throw new IllegalStateException("setAudioSessionId called in state " + mState);
            if (sessionId <= 0) throw new IllegalArgumentException("Invalid audio session ID " + sessionId);
            mAudioSessionId = sessionId;
        }
    }

    @Deprecated
    public void setAudioStreamType(int streamtype)
    {
        setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(streamtype).build());
    }

    public void setAuxEffectSendLevel(float level)
    {
        synchronized (mLock)
        {
            mAuxSendLevel = Math.max(0.0f, Math.min(1.0f, level));
        }
    }

    public void setDataSource(Context context, Uri uri)
        throws IOException, IllegalArgumentException, IllegalStateException, SecurityException
    {
        setDataSource(context, uri, null, null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers)
        throws IOException, IllegalArgumentException, IllegalStateException, SecurityException
    {
        setDataSource(context, uri, headers, null);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers, List<HttpCookie> cookies) throws IOException
    {
        if (context == null) throw new NullPointerException("context param can not be null.");
        if (uri == null) throw new NullPointerException("uri param can not be null.");

        String scheme = uri.getScheme();

        if (scheme == null || ContentResolver.SCHEME_FILE.equals(scheme))
        {
            setDataSource(uri.getPath());
            return;
        }

        if (ContentResolver.SCHEME_CONTENT.equals(scheme) || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme))
        {
            AssetFileDescriptor descriptor = null;

            try
            {
                descriptor = context.getContentResolver().openAssetFileDescriptor(uri, "r");
            }
            catch (IOException | RuntimeException exception)
            {
                descriptor = null;
            }

            if (descriptor != null)
            {
                try
                {
                    setDataSource(descriptor);
                    return;
                }
                finally
                {
                    descriptor.close();
                }
            }
        }

        setInitializedSource();
    }

    public void setDataSource(AssetFileDescriptor descriptor) throws IOException, IllegalArgumentException, IllegalStateException
    {
        if (descriptor == null) throw new IllegalArgumentException("afd must not be null");

        setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
            descriptor.getDeclaredLength() < 0 ? 0x7ffffffffffffffL : descriptor.getDeclaredLength());
    }

    public void setDataSource(MediaDataSource dataSource) throws IllegalArgumentException, IllegalStateException
    {
        if (dataSource == null) throw new IllegalArgumentException("dataSource must not be null");
        setInitializedSource();
    }

    public void setDataSource(FileDescriptor descriptor) throws IOException, IllegalArgumentException, IllegalStateException
    {
        setDataSource(descriptor, 0, 0x7ffffffffffffffL);
    }

    public void setDataSource(FileDescriptor descriptor, long offset, long length)
        throws IOException, IllegalArgumentException, IllegalStateException
    {
        if (descriptor == null || !descriptor.valid()) throw new IllegalArgumentException("Invalid file descriptor");
        if (offset < 0 || length < 0) throw new IllegalArgumentException("Invalid offset or length");
        setInitializedSource();
    }

    public void setDataSource(String path) throws IOException, IllegalArgumentException, IllegalStateException, SecurityException
    {
        if (path == null) throw new IllegalArgumentException("path must not be null");

        Uri uri = Uri.parse(path);
        String scheme = uri.getScheme();

        if (scheme == null || ContentResolver.SCHEME_FILE.equals(scheme))
        {
            File file = new File(scheme == null ? path : uri.getPath());
            if (!file.exists()) throw new IOException("setDataSource failed.");
            if (!file.canRead()) throw new IOException("setDataSource failed.: status=0x80000000");
        }

        setInitializedSource();
    }

    private void setInitializedSource()
    {
        synchronized (mLock)
        {
            if (mState != STATE_IDLE) throw new IllegalStateException("setDataSource called in state " + mState);
            mState = STATE_INITIALIZED;
        }
    }

    public void setDisplay(SurfaceHolder holder)
    {
        synchronized (mLock)
        {
            mSurfaceHolder = holder;
            mSurface = holder == null ? null : holder.getSurface();
        }
    }

    public void setDrmPropertyString(String propertyName, String value) throws MediaPlayer.NoDrmSchemeException
    {
        throw new NoDrmSchemeException("setDrmPropertyString NoDrmSchemeException");
    }

    public void setLooping(boolean looping)
    {
        synchronized (mLock)
        {
            mLooping = looping;
        }
    }

    public void setNextMediaPlayer(MediaPlayer next)
    {
        if (next == this) throw new IllegalArgumentException("next player can't be this player");

        synchronized (mLock)
        {
            mNextMediaPlayer = next;
        }
    }

    public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener listener)
    {
        synchronized (mLock)
        {
            mOnBufferingUpdateListener = listener;
        }
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener)
    {
        synchronized (mLock)
        {
            mOnCompletionListener = listener;
        }
    }

    public void setOnDrmConfigHelper(MediaPlayer.OnDrmConfigHelper listener)
    {
        synchronized (mLock)
        {
            mOnDrmConfigHelper = listener;
        }
    }

    public void setOnDrmInfoListener(MediaPlayer.OnDrmInfoListener listener)
    {
        setOnDrmInfoListener(listener, null);
    }

    public void setOnDrmInfoListener(MediaPlayer.OnDrmInfoListener listener, Handler handler)
    {
        synchronized (mLock)
        {
            mOnDrmInfoListener = listener;
        }
    }

    public void setOnDrmPreparedListener(MediaPlayer.OnDrmPreparedListener listener)
    {
        setOnDrmPreparedListener(listener, null);
    }

    public void setOnDrmPreparedListener(MediaPlayer.OnDrmPreparedListener listener, Handler handler)
    {
        synchronized (mLock)
        {
            mOnDrmPreparedListener = listener;
        }
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener listener)
    {
        synchronized (mLock)
        {
            mOnErrorListener = listener;
        }
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener listener)
    {
        synchronized (mLock)
        {
            mOnInfoListener = listener;
        }
    }

    public void setOnMediaTimeDiscontinuityListener(MediaPlayer.OnMediaTimeDiscontinuityListener listener)
    {
        setOnMediaTimeDiscontinuityListener(listener, null);
    }

    public void setOnMediaTimeDiscontinuityListener(MediaPlayer.OnMediaTimeDiscontinuityListener listener, Handler handler)
    {
        synchronized (mLock)
        {
            mOnMediaTimeDiscontinuityListener = listener;
        }
    }

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener)
    {
        synchronized (mLock)
        {
            mOnPreparedListener = listener;
        }
    }

    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener listener)
    {
        synchronized (mLock)
        {
            mOnSeekCompleteListener = listener;
        }
    }

    public void setOnSubtitleDataListener(MediaPlayer.OnSubtitleDataListener listener)
    {
        setOnSubtitleDataListener(listener, null);
    }

    public void setOnSubtitleDataListener(MediaPlayer.OnSubtitleDataListener listener, Handler handler)
    {
        synchronized (mLock)
        {
            mOnSubtitleDataListener = listener;
        }
    }

    public void setOnTimedMetaDataAvailableListener(MediaPlayer.OnTimedMetaDataAvailableListener listener)
    {
        synchronized (mLock)
        {
            mOnTimedMetaDataAvailableListener = listener;
        }
    }

    public void setOnTimedTextListener(MediaPlayer.OnTimedTextListener listener)
    {
        synchronized (mLock)
        {
            mOnTimedTextListener = listener;
        }
    }

    public void setOnVideoSizeChangedListener(MediaPlayer.OnVideoSizeChangedListener listener)
    {
        synchronized (mLock)
        {
            mOnVideoSizeChangedListener = listener;
        }
    }

    public void setPlaybackParams(PlaybackParams params)
    {
        if (params == null) throw new IllegalArgumentException("params must not be null");

        synchronized (mLock)
        {
            if (mState == STATE_IDLE || mState == STATE_END) throw new IllegalStateException("setPlaybackParams called in state " + mState);

            float speed = params.speedOrDefault();
            if (speed < 0.0f) throw new IllegalArgumentException("speed must not be negative");

            PlaybackParams merged = new PlaybackParams().setSpeed(speed);
            try
            {
                merged.setPitch(params.getPitch());
            }
            catch (IllegalStateException exception)
            {
                merged.setPitch(mPlaybackParams.getPitch());
            }

            try
            {
                merged.setAudioFallbackMode(params.getAudioFallbackMode());
            }
            catch (IllegalStateException exception)
            {
                merged.setAudioFallbackMode(mPlaybackParams.getAudioFallbackMode());
            }

            mPlaybackParams = merged;
        }
    }

    @Override
    public boolean setPreferredDevice(AudioDeviceInfo deviceInfo)
    {
        if (deviceInfo != null && !deviceInfo.isSink()) return false;

        synchronized (mLock)
        {
            mPreferredDevice = deviceInfo;
            return true;
        }
    }

    public void setScreenOnWhilePlaying(boolean screenOn)
    {
        synchronized (mLock)
        {
            mScreenOnWhilePlaying = screenOn;
        }
    }

    public void setSurface(Surface surface)
    {
        synchronized (mLock)
        {
            mSurface = surface;
            mSurfaceHolder = null;
        }
    }

    public void setSyncParams(SyncParams params)
    {
        if (params == null) throw new IllegalArgumentException("params must not be null");

        synchronized (mLock)
        {
            SyncParams merged = new SyncParams();

            try
            {
                merged.setSyncSource(params.getSyncSource());
            }
            catch (IllegalStateException exception)
            {
                merged.setSyncSource(mSyncParams.getSyncSource());
            }

            try
            {
                merged.setAudioAdjustMode(params.getAudioAdjustMode());
            }
            catch (IllegalStateException exception)
            {
                merged.setAudioAdjustMode(mSyncParams.getAudioAdjustMode());
            }

            try
            {
                merged.setTolerance(params.getTolerance());
            }
            catch (IllegalStateException exception)
            {
                merged.setTolerance(mSyncParams.getTolerance());
            }

            mSyncParams = merged;
        }
    }

    public void setVideoScalingMode(int mode)
    {
        if (mode != VIDEO_SCALING_MODE_SCALE_TO_FIT && mode != VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
        {
            throw new IllegalArgumentException("Scaling mode " + mode + " is not supported");
        }

        synchronized (mLock)
        {
            mVideoScalingMode = mode;
        }
    }

    public void setVolume(float leftVolume, float rightVolume)
    {
        synchronized (mLock)
        {
            mLeftVolume = Math.max(0.0f, Math.min(1.0f, leftVolume));
            mRightVolume = Math.max(0.0f, Math.min(1.0f, rightVolume));
        }
    }

    public void setWakeMode(Context context, int mode)
    {
    }

    public void start() throws IllegalStateException
    {
        invalidOperation();
    }

    public void stop() throws IllegalStateException
    {
        invalidOperation();
    }

    private void invalidOperation()
    {
        synchronized (mLock)
        {
            if (mState == STATE_END) throw new IllegalStateException("MediaPlayer has been released");
            postErrorLocked(INVALID_OPERATION, 0);
        }
    }

    private void postErrorLocked(final int what, final int extra)
    {
        if (mState == STATE_END) return;

        mState = STATE_ERROR;

        Runnable notify = new Runnable()
        {
            @Override
            public void run()
            {
                OnErrorListener errorListener;
                OnCompletionListener completionListener;

                synchronized (mLock)
                {
                    if (mState != STATE_ERROR) return;
                    errorListener = mOnErrorListener;
                    completionListener = mOnCompletionListener;
                }

                boolean handled = errorListener != null && errorListener.onError(MediaPlayer.this, what, extra);
                if (!handled && completionListener != null) completionListener.onCompletion(MediaPlayer.this);
            }
        };

        if (mEventHandler != null)
        {
            mEventHandler.post(notify);
        }
        else
        {
            Thread thread = new Thread(notify, "MediaPlayerEvent");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public static class TrackInfo implements Parcelable
    {
        public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
        public static final int MEDIA_TRACK_TYPE_METADATA = 5;
        public static final int MEDIA_TRACK_TYPE_SUBTITLE = 4;
        public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
        public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
        public static final int MEDIA_TRACK_TYPE_VIDEO = 1;

        static final Parcelable.Creator<TrackInfo> CREATOR = new Parcelable.Creator<TrackInfo>()
        {
            @Override
            public TrackInfo createFromParcel(Parcel source)
            {
                int trackType = source.readInt();
                String mime = source.readString();
                String language = source.readString();
                return new TrackInfo(trackType, mime, language);
            }

            @Override
            public TrackInfo[] newArray(int size)
            {
                return new TrackInfo[size];
            }
        };

        private final int mTrackType;
        private final String mMime;
        private final String mLanguage;

        TrackInfo(int trackType, String mime, String language)
        {
            mTrackType = trackType;
            mMime = mime;
            mLanguage = language == null ? "und" : language;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        public MediaFormat getFormat()
        {
            if (mTrackType != MEDIA_TRACK_TYPE_TIMEDTEXT && mTrackType != MEDIA_TRACK_TYPE_SUBTITLE) return null;
            return MediaFormat.createSubtitleFormat(mMime, mLanguage);
        }

        public String getLanguage()
        {
            return mLanguage;
        }

        public int getTrackType()
        {
            return mTrackType;
        }

        @Override
        public String toString()
        {
            String type;

            switch (mTrackType)
            {
                case MEDIA_TRACK_TYPE_VIDEO:
                    type = "VIDEO";
                    break;
                case MEDIA_TRACK_TYPE_AUDIO:
                    type = "AUDIO";
                    break;
                case MEDIA_TRACK_TYPE_TIMEDTEXT:
                    type = "TIMEDTEXT";
                    break;
                case MEDIA_TRACK_TYPE_SUBTITLE:
                    type = "SUBTITLE";
                    break;
                case MEDIA_TRACK_TYPE_METADATA:
                    type = "METADATA";
                    break;
                default:
                    type = "UNKNOWN";
                    break;
            }

            return getClass().getName() + '{' + type + ", " + mMime + ", " + mLanguage + "}";
        }

        @Override
        public void writeToParcel(Parcel destination, int flags)
        {
            destination.writeInt(mTrackType);
            destination.writeString(mMime);
            destination.writeString(mLanguage);
        }
    }

    public static final class ProvisioningServerErrorException extends MediaDrmException
    {
        public ProvisioningServerErrorException(String detailMessage)
        {
            super(detailMessage);
        }
    }

    public static final class ProvisioningNetworkErrorException extends MediaDrmException
    {
        public ProvisioningNetworkErrorException(String detailMessage)
        {
            super(detailMessage);
        }
    }

    public interface OnVideoSizeChangedListener
    {
        void onVideoSizeChanged(MediaPlayer mp, int width, int height);
    }

    public interface OnTimedTextListener
    {
        void onTimedText(MediaPlayer mp, TimedText text);
    }

    public interface OnTimedMetaDataAvailableListener
    {
        void onTimedMetaDataAvailable(MediaPlayer mp, TimedMetaData data);
    }

    public interface OnSubtitleDataListener
    {
        void onSubtitleData(MediaPlayer mp, SubtitleData data);
    }

    public interface OnSeekCompleteListener
    {
        void onSeekComplete(MediaPlayer mp);
    }

    public interface OnPreparedListener
    {
        void onPrepared(MediaPlayer mp);
    }

    public interface OnMediaTimeDiscontinuityListener
    {
        void onMediaTimeDiscontinuity(MediaPlayer mp, MediaTimestamp mts);
    }

    public interface OnInfoListener
    {
        boolean onInfo(MediaPlayer mp, int what, int extra);
    }

    public interface OnErrorListener
    {
        boolean onError(MediaPlayer mp, int what, int extra);
    }

    public interface OnDrmPreparedListener
    {
        void onDrmPrepared(MediaPlayer mp, int status);
    }

    public interface OnDrmInfoListener
    {
        void onDrmInfo(MediaPlayer mp, MediaPlayer.DrmInfo drmInfo);
    }

    public interface OnDrmConfigHelper
    {
        void onDrmConfig(MediaPlayer mp);
    }

    public interface OnCompletionListener
    {
        void onCompletion(MediaPlayer mp);
    }

    public interface OnBufferingUpdateListener
    {
        void onBufferingUpdate(MediaPlayer mp, int percent);
    }

    public static final class NoDrmSchemeException extends MediaDrmException
    {
        public NoDrmSchemeException(String detailMessage)
        {
            super(detailMessage);
        }
    }

    public static final class MetricsConstants
    {
        public static final String CODEC_AUDIO = "android.media.mediaplayer.audio.codec";
        public static final String CODEC_VIDEO = "android.media.mediaplayer.video.codec";
        public static final String DURATION = "android.media.mediaplayer.durationMs";
        public static final String ERRORS = "android.media.mediaplayer.err";
        public static final String ERROR_CODE = "android.media.mediaplayer.errcode";
        public static final String FRAMES = "android.media.mediaplayer.frames";
        public static final String FRAMES_DROPPED = "android.media.mediaplayer.dropped";
        public static final String HEIGHT = "android.media.mediaplayer.height";
        public static final String MIME_TYPE_AUDIO = "android.media.mediaplayer.audio.mime";
        public static final String MIME_TYPE_VIDEO = "android.media.mediaplayer.video.mime";
        public static final String PLAYING = "android.media.mediaplayer.playingMs";
        public static final String WIDTH = "android.media.mediaplayer.width";

        private MetricsConstants()
        {
        }
    }

    public static final class DrmInfo
    {
        private final Map<UUID, byte[]> mPssh;
        private final UUID[] mSupportedSchemes;

        DrmInfo(Map<UUID, byte[]> pssh, UUID[] supportedSchemes)
        {
            mPssh = pssh;
            mSupportedSchemes = supportedSchemes;
        }

        public Map<UUID, byte[]> getPssh()
        {
            return mPssh;
        }

        public UUID[] getSupportedSchemes()
        {
            return mSupportedSchemes;
        }
    }
}
