package android.media;

import android.content.Context;
import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class AudioTrack implements AudioRouting, VolumeAutomation
{
    public static final int DUAL_MONO_MODE_LL = 2;
    public static final int DUAL_MONO_MODE_LR = 1;
    public static final int DUAL_MONO_MODE_OFF = 0;
    public static final int DUAL_MONO_MODE_RR = 3;
    public static final int ENCAPSULATION_METADATA_TYPE_DVB_AD_DESCRIPTOR = 2;
    public static final int ENCAPSULATION_METADATA_TYPE_FRAMEWORK_TUNER = 1;
    public static final int ENCAPSULATION_METADATA_TYPE_SUPPLEMENTARY_AUDIO_PLACEMENT = 3;
    public static final int ENCAPSULATION_MODE_ELEMENTARY_STREAM = 1;
    public static final int ENCAPSULATION_MODE_NONE = 0;
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_DEAD_OBJECT = -6;
    public static final int ERROR_INVALID_OPERATION = -3;
    public static final int MODE_STATIC = 0;
    public static final int MODE_STREAM = 1;
    public static final int PERFORMANCE_MODE_LOW_LATENCY = 1;
    public static final int PERFORMANCE_MODE_NONE = 0;
    public static final int PERFORMANCE_MODE_POWER_SAVING = 2;
    public static final int PLAYSTATE_PAUSED = 2;
    public static final int PLAYSTATE_PLAYING = 3;
    public static final int PLAYSTATE_STOPPED = 1;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_NO_STATIC_DATA = 2;
    public static final int STATE_UNINITIALIZED = 0;
    public static final int SUCCESS = 0;
    public static final int SUPPLEMENTARY_AUDIO_PLACEMENT_LEFT = 1;
    public static final int SUPPLEMENTARY_AUDIO_PLACEMENT_NORMAL = 0;
    public static final int SUPPLEMENTARY_AUDIO_PLACEMENT_RIGHT = 2;
    public static final int WRITE_BLOCKING = 0;
    public static final int WRITE_NON_BLOCKING = 1;

    static final int NATIVE_OUTPUT_SAMPLE_RATE = 48000;

    private static final float GAIN_MIN = 0.0f;
    private static final float GAIN_MAX = 1.0f;
    private static final int SAMPLE_RATE_MIN = 4000;
    private static final int SAMPLE_RATE_MAX = 192000;
    private static final int LOW_LATENCY_FRAMES = 192;
    private static final long NANOS_PER_SECOND = 1000000000L;

    private final Object mLock = new Object();
    private final int mSampleRate;
    private final int mChannelMask;
    private final int mChannelIndexMask;
    private final int mChannelCount;
    private final int mEncoding;
    private final int mFrameSize;
    private final int mMode;
    private final int mSessionId;
    private final int mPerformanceMode;
    private final int mOffloadSupported;
    private final AudioAttributes mAttributes;
    private final Looper mInitializationLooper;

    private int mState = STATE_UNINITIALIZED;
    private int mPlayState = PLAYSTATE_STOPPED;
    private int mBufferCapacityFrames;
    private int mBufferSizeFrames;
    private int mStartThresholdFrames;
    private byte[] mStaticData;
    private int mStaticBytes;

    private long mBytesWritten;
    private long mPlayedFramesBase;
    private long mClockStartNanos;
    private boolean mClockRunning;
    private boolean mDraining;
    private boolean mResetOnPlay;
    private int mUnderrunCount;
    private boolean mStarved;

    private int mPlaybackRate;
    private PlaybackParams mPlaybackParams = new PlaybackParams().allowDefaults();
    private float mLeftVolume = GAIN_MAX;
    private float mRightVolume = GAIN_MAX;
    private float mAuxSendLevel;
    private int mAuxEffectId;
    private int mDualMonoMode = DUAL_MONO_MODE_OFF;
    private float mAudioDescriptionMixLeveldB = Float.NEGATIVE_INFINITY;
    private int mOffloadDelay;
    private int mOffloadPadding;
    private int mLoopStart;
    private int mLoopEnd;
    private int mLoopCount;
    private int mMarkerPosition;
    private int mPeriodFrames;
    private long mNextPeriodFrame;
    private boolean mMarkerReached;
    private LogSessionId mLogSessionId = LogSessionId.LOG_SESSION_ID_NONE;
    private AudioDeviceInfo mPreferredDevice;

    private OnPlaybackPositionUpdateListener mPositionListener;
    private Handler mPositionHandler;
    private Thread mNotifierThread;
    private final Map<AudioRouting.OnRoutingChangedListener, Handler> mRoutingListeners = new HashMap<>();
    private final Map<OnCodecFormatChangedListener, Executor> mCodecFormatListeners = new HashMap<>();
    private final Map<StreamEventCallback, Executor> mStreamEventCallbacks = new HashMap<>();

    public AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int mode, int sessionId)
        throws IllegalArgumentException
    {
        this(attributes, format, bufferSizeInBytes, mode, sessionId, PERFORMANCE_MODE_NONE, false);
    }

    @Deprecated
    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode)
        throws IllegalArgumentException
    {
        this(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, mode, AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    @Deprecated
    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes, int mode,
        int sessionId) throws IllegalArgumentException
    {
        this(new AudioAttributes.Builder().setLegacyStreamType(streamType).build(),
            legacyFormat(sampleRateInHz, channelConfig, audioFormat), bufferSizeInBytes, mode, sessionId);
    }

    private AudioTrack(AudioAttributes attributes, AudioFormat format, int bufferSizeInBytes, int mode, int sessionId,
        int performanceMode, boolean offload) throws IllegalArgumentException
    {
        if (attributes == null) throw new IllegalArgumentException("Illegal null AudioAttributes");
        if (format == null) throw new IllegalArgumentException("Illegal null AudioFormat");
        if (mode != MODE_STATIC && mode != MODE_STREAM) throw new IllegalArgumentException("Invalid mode.");
        if (sessionId < 0) throw new IllegalArgumentException("Invalid audio session ID: " + sessionId);

        int sampleRate = format.getSampleRate();
        if (sampleRate == AudioFormat.SAMPLE_RATE_UNSPECIFIED) sampleRate = NATIVE_OUTPUT_SAMPLE_RATE;
        if (sampleRate < SAMPLE_RATE_MIN || sampleRate > SAMPLE_RATE_MAX)
        {
            throw new IllegalArgumentException(sampleRate + "Hz is not a supported sample rate.");
        }

        int encoding = format.getEncoding();
        if (encoding == AudioFormat.ENCODING_DEFAULT || encoding == AudioFormat.ENCODING_INVALID)
        {
            encoding = AudioFormat.ENCODING_PCM_16BIT;
        }

        if (!isKnownEncoding(encoding)) throw new IllegalArgumentException("Unsupported audio encoding.");

        int channelMask = format.getChannelMask();
        int channelIndexMask = format.getChannelIndexMask();
        int channelCount;

        if (channelIndexMask != 0)
        {
            channelCount = Integer.bitCount(channelIndexMask);
        }
        else
        {
            if (channelMask == AudioFormat.CHANNEL_INVALID || channelMask == AudioFormat.CHANNEL_OUT_DEFAULT)
            {
                channelMask = AudioFormat.CHANNEL_OUT_STEREO;
            }

            channelCount = Integer.bitCount(channelMask);
        }

        if (channelCount <= 0 || channelCount > 30) throw new IllegalArgumentException("Unsupported channel mask");

        boolean pcm = isPcm(encoding);
        int frameSize = pcm ? channelCount * bytesPerSample(encoding) : 1;

        if (bufferSizeInBytes < 1 || bufferSizeInBytes % frameSize != 0)
        {
            throw new IllegalArgumentException("Invalid audio buffer size.");
        }

        mAttributes = attributes;
        mSampleRate = sampleRate;
        mChannelMask = channelMask;
        mChannelIndexMask = channelIndexMask;
        mChannelCount = channelCount;
        mEncoding = encoding;
        mFrameSize = frameSize;
        mMode = mode;
        mPerformanceMode = performanceMode;
        mOffloadSupported = offload ? 1 : 0;
        mSessionId = sessionId == AudioManager.AUDIO_SESSION_ID_GENERATE ? newAudioSessionId() : sessionId;
        mPlaybackRate = sampleRate;
        mBufferCapacityFrames = bufferSizeInBytes / frameSize;
        mBufferSizeFrames = mBufferCapacityFrames;
        mStartThresholdFrames = mBufferCapacityFrames;

        Looper looper = Looper.myLooper();
        mInitializationLooper = looper != null ? looper : Looper.getMainLooper();

        if (!pcm) return;

        if (mode == MODE_STATIC)
        {
            mStaticData = new byte[bufferSizeInBytes];
            mState = STATE_NO_STATIC_DATA;
        }
        else
        {
            mState = STATE_INITIALIZED;
        }
    }

    static int newAudioSessionId()
    {
        return AudioManager.newAudioSessionId();
    }

    private static AudioFormat legacyFormat(int sampleRate, int channelConfig, int encoding)
    {
        int channelMask;

        switch (channelConfig)
        {
            case AudioFormat.CHANNEL_OUT_DEFAULT:
            case AudioFormat.CHANNEL_OUT_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                channelMask = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case AudioFormat.CHANNEL_OUT_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
                channelMask = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            default:
                if (!isValidOutputChannelMask(channelConfig)) throw new IllegalArgumentException("Unsupported channel configuration.");
                channelMask = channelConfig;
                break;
        }

        if (encoding == AudioFormat.ENCODING_DEFAULT) encoding = AudioFormat.ENCODING_PCM_16BIT;
        if (!isKnownEncoding(encoding)) throw new IllegalArgumentException("Unsupported audio encoding.");
        if (sampleRate < SAMPLE_RATE_MIN || sampleRate > SAMPLE_RATE_MAX)
        {
            throw new IllegalArgumentException(sampleRate + "Hz is not a supported sample rate.");
        }

        return new AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(channelMask).setEncoding(encoding).build();
    }

    private static boolean isValidOutputChannelMask(int channelMask)
    {
        int frontPair = AudioFormat.CHANNEL_OUT_FRONT_LEFT | AudioFormat.CHANNEL_OUT_FRONT_RIGHT;
        int count = Integer.bitCount(channelMask);

        if (channelMask == 0 || (channelMask & 3) != 0 || count > 30) return false;
        if (count > 1 && (channelMask & frontPair) != frontPair) return false;

        return true;
    }

    static boolean isPcm(int encoding)
    {
        switch (encoding)
        {
            case AudioFormat.ENCODING_PCM_8BIT:
            case AudioFormat.ENCODING_PCM_16BIT:
            case AudioFormat.ENCODING_PCM_FLOAT:
            case AudioFormat.ENCODING_PCM_24BIT_PACKED:
            case AudioFormat.ENCODING_PCM_32BIT:
                return true;
            default:
                return false;
        }
    }

    private static boolean isKnownEncoding(int encoding)
    {
        return encoding > AudioFormat.ENCODING_INVALID && encoding <= AudioFormat.ENCODING_IAMF_BASE_ENHANCED_PROFILE_PCM;
    }

    static int bytesPerSample(int encoding)
    {
        switch (encoding)
        {
            case AudioFormat.ENCODING_PCM_8BIT:
                return 1;
            case AudioFormat.ENCODING_PCM_24BIT_PACKED:
                return 3;
            case AudioFormat.ENCODING_PCM_FLOAT:
            case AudioFormat.ENCODING_PCM_32BIT:
                return 4;
            default:
                return 2;
        }
    }

    static int minimumFrames(int sampleRate)
    {
        int frames = (sampleRate + 49) / 50;
        return Math.max(256, (frames + 15) & ~15);
    }

    public void addOnCodecFormatChangedListener(Executor executor, AudioTrack.OnCodecFormatChangedListener listener)
    {
        if (executor == null) throw new NullPointerException("executor must not be null");
        if (listener == null) throw new NullPointerException("listener must not be null");

        synchronized (mCodecFormatListeners)
        {
            if (mCodecFormatListeners.containsKey(listener)) throw new IllegalArgumentException("listener already added");
            mCodecFormatListeners.put(listener, executor);
        }
    }

    @Override
    public void addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener, Handler handler)
    {
        if (listener == null) return;

        synchronized (mRoutingListeners)
        {
            mRoutingListeners.put(listener, handler != null ? handler : new Handler(mInitializationLooper));
        }
    }

    @Deprecated
    public void addOnRoutingChangedListener(AudioTrack.OnRoutingChangedListener listener, Handler handler)
    {
        addOnRoutingChangedListener((AudioRouting.OnRoutingChangedListener)listener, handler);
    }

    public int attachAuxEffect(int effectId)
    {
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;
            if (effectId != 0) return ERROR_BAD_VALUE;
            mAuxEffectId = effectId;
            return SUCCESS;
        }
    }

    @Override
    public VolumeShaper createVolumeShaper(VolumeShaper.Configuration configuration)
    {
        if (configuration == null) throw new IllegalArgumentException("configuration must not be null");
        if (mState == STATE_UNINITIALIZED) throw new IllegalStateException("track not initialized");
        return new VolumeShaper(configuration);
    }

    public void flush()
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED || mMode != MODE_STREAM) return;
            if (mPlayState == PLAYSTATE_PLAYING) return;

            long position = positionLocked(System.nanoTime());
            mBytesWritten = position * mFrameSize;
            mDraining = false;
            mResetOnPlay = true;
            mLock.notifyAll();
        }
    }

    public AudioAttributes getAudioAttributes()
    {
        if (mState == STATE_UNINITIALIZED) throw new IllegalStateException("track not initialized");
        return mAttributes;
    }

    public float getAudioDescriptionMixLeveldB()
    {
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return Float.NEGATIVE_INFINITY;
            return mAudioDescriptionMixLeveldB;
        }
    }

    public int getAudioFormat()
    {
        return mEncoding;
    }

    public int getAudioSessionId()
    {
        return mSessionId;
    }

    public int getBufferCapacityInFrames()
    {
        return mBufferCapacityFrames;
    }

    public int getBufferSizeInFrames()
    {
        synchronized (mLock)
        {
            return mBufferSizeFrames;
        }
    }

    public int getChannelConfiguration()
    {
        return mChannelMask;
    }

    public int getChannelCount()
    {
        return mChannelCount;
    }

    public int getDualMonoMode()
    {
        synchronized (mLock)
        {
            return mDualMonoMode;
        }
    }

    public AudioFormat getFormat()
    {
        AudioFormat.Builder builder = new AudioFormat.Builder().setSampleRate(mSampleRate).setEncoding(mEncoding);

        if (mChannelIndexMask != 0)
        {
            builder.setChannelIndexMask(mChannelIndexMask);
        }
        else
        {
            builder.setChannelMask(mChannelMask);
        }

        return builder.build();
    }

    public LogSessionId getLogSessionId()
    {
        synchronized (mLock)
        {
            return mLogSessionId;
        }
    }

    public static float getMaxVolume()
    {
        return GAIN_MAX;
    }

    public PersistableBundle getMetrics()
    {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(MetricsConstants.STREAMTYPE, Integer.toString(mAttributes.getVolumeControlStream()));
        bundle.putString(MetricsConstants.CONTENTTYPE, Integer.toString(mAttributes.getContentType()));
        bundle.putString(MetricsConstants.USAGE, Integer.toString(mAttributes.getUsage()));
        bundle.putInt(MetricsConstants.SAMPLERATE, mSampleRate);
        bundle.putLong(MetricsConstants.CHANNELMASK, mChannelMask);
        return bundle;
    }

    public static int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat)
    {
        int channelCount;

        switch (channelConfig)
        {
            case AudioFormat.CHANNEL_OUT_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                channelCount = 1;
                break;
            case AudioFormat.CHANNEL_OUT_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
                channelCount = 2;
                break;
            default:
                if (!isValidOutputChannelMask(channelConfig)) return ERROR_BAD_VALUE;
                channelCount = Integer.bitCount(channelConfig);
                break;
        }

        if (!isPcm(audioFormat)) return ERROR_BAD_VALUE;
        if (sampleRateInHz < SAMPLE_RATE_MIN || sampleRateInHz > SAMPLE_RATE_MAX) return ERROR_BAD_VALUE;

        return minimumFrames(sampleRateInHz) * channelCount * bytesPerSample(audioFormat);
    }

    public static float getMinVolume()
    {
        return GAIN_MIN;
    }

    public static int getNativeOutputSampleRate(int streamType)
    {
        return NATIVE_OUTPUT_SAMPLE_RATE;
    }

    public int getNotificationMarkerPosition()
    {
        synchronized (mLock)
        {
            return mMarkerPosition;
        }
    }

    public int getOffloadDelay()
    {
        if (mOffloadSupported == 0) throw new IllegalStateException("Illegal query of delay on non-offloaded track");
        synchronized (mLock)
        {
            return mOffloadDelay;
        }
    }

    public int getOffloadPadding()
    {
        if (mOffloadSupported == 0) throw new IllegalStateException("Illegal query of padding on non-offloaded track");
        synchronized (mLock)
        {
            return mOffloadPadding;
        }
    }

    public int getPerformanceMode()
    {
        return mPerformanceMode;
    }

    public int getPlayState()
    {
        synchronized (mLock)
        {
            return mPlayState;
        }
    }

    public int getPlaybackHeadPosition()
    {
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return 0;
            return (int)headPositionLocked(System.nanoTime());
        }
    }

    public PlaybackParams getPlaybackParams()
    {
        synchronized (mLock)
        {
            return new PlaybackParams().setSpeed(mPlaybackParams.getSpeed()).setPitch(mPlaybackParams.getPitch())
                .setAudioFallbackMode(mPlaybackParams.getAudioFallbackMode());
        }
    }

    public int getPlaybackRate()
    {
        synchronized (mLock)
        {
            return mPlaybackRate;
        }
    }

    public int getPositionNotificationPeriod()
    {
        synchronized (mLock)
        {
            return mPeriodFrames;
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
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED || mPlayState != PLAYSTATE_PLAYING) return null;
            return mPreferredDevice != null ? mPreferredDevice : new AudioDeviceInfo(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
        }
    }

    @Override
    public List<AudioDeviceInfo> getRoutedDevices()
    {
        AudioDeviceInfo device = getRoutedDevice();
        if (device == null) return Collections.emptyList();

        List<AudioDeviceInfo> devices = new ArrayList<>();
        devices.add(device);
        return devices;
    }

    public int getSampleRate()
    {
        return mSampleRate;
    }

    public int getStartThresholdInFrames()
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("AudioTrack is not initialized");
            if (mMode != MODE_STREAM) throw new IllegalStateException("AudioTrack must be a streaming track");
            return mStartThresholdFrames;
        }
    }

    public int getState()
    {
        synchronized (mLock)
        {
            return mState;
        }
    }

    public int getStreamType()
    {
        return mAttributes.getVolumeControlStream();
    }

    public boolean getTimestamp(AudioTimestamp timestamp)
    {
        if (timestamp == null) throw new IllegalArgumentException();

        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED || !mClockRunning) return false;

            long now = System.nanoTime();
            long position = positionLocked(now);
            if (position <= 0) return false;

            timestamp.framePosition = position;
            timestamp.nanoTime = now;
            return true;
        }
    }

    public int getUnderrunCount()
    {
        synchronized (mLock)
        {
            positionLocked(System.nanoTime());
            return mUnderrunCount;
        }
    }

    public static boolean isDirectPlaybackSupported(AudioFormat format, AudioAttributes attributes)
    {
        if (format == null) throw new IllegalArgumentException("Illegal null AudioFormat argument");
        if (attributes == null) throw new IllegalArgumentException("Illegal null AudioAttributes argument");

        return false;
    }

    public boolean isOffloadedPlayback()
    {
        return false;
    }

    public void pause() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("pause() called on uninitialized AudioTrack.");
            if (mPlayState != PLAYSTATE_PLAYING) return;

            freezeClockLocked(System.nanoTime());
            mPlayState = PLAYSTATE_PAUSED;
            mLock.notifyAll();
        }

        AudioManager.unregisterActivePlayer(this);
    }

    public void play() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("play() called on uninitialized AudioTrack.");
            if (mPlayState == PLAYSTATE_PLAYING) return;

            long now = System.nanoTime();

            if (mPlayState == PLAYSTATE_STOPPED)
            {
                if (mDraining) freezeClockLocked(now);

                if (mMode == MODE_STREAM && (mResetOnPlay || mDraining || mPlayedFramesBase * mFrameSize >= mBytesWritten))
                {
                    long pending = Math.max(0, mBytesWritten - mPlayedFramesBase * mFrameSize);
                    if (mDraining) pending = 0;
                    mBytesWritten = pending;
                    mPlayedFramesBase = 0;
                    mMarkerReached = false;
                    mNextPeriodFrame = mPeriodFrames;
                }
                else if (mMode == MODE_STATIC && mStaticBytes / mFrameSize <= mPlayedFramesBase)
                {
                    mPlayedFramesBase = 0;
                }
            }

            mResetOnPlay = false;
            mDraining = false;
            mStarved = false;
            mPlayState = PLAYSTATE_PLAYING;
            mClockStartNanos = now;
            mClockRunning = true;
            mLock.notifyAll();
            ensureNotifierLocked();
        }

        AudioManager.registerActivePlayer(this, mAttributes, mSessionId);
    }

    public void registerStreamEventCallback(Executor executor, AudioTrack.StreamEventCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("Illegal null StreamEventCallback");
        if (executor == null) throw new IllegalArgumentException("Illegal null Executor for the StreamEventCallback");
        if (mOffloadSupported == 0) throw new IllegalStateException("Cannot register StreamEventCallback on non-offloaded track");

        synchronized (mStreamEventCallbacks)
        {
            mStreamEventCallbacks.put(callback, executor);
        }
    }

    public void release()
    {
        synchronized (mLock)
        {
            if (mState != STATE_UNINITIALIZED && mClockRunning) freezeClockLocked(System.nanoTime());

            mState = STATE_UNINITIALIZED;
            mPlayState = PLAYSTATE_STOPPED;
            mPositionListener = null;
            mStaticData = null;
            mLock.notifyAll();
        }

        AudioManager.unregisterActivePlayer(this);
    }

    public int reloadStaticData()
    {
        synchronized (mLock)
        {
            if (mMode == MODE_STREAM || mState != STATE_INITIALIZED) return ERROR_INVALID_OPERATION;

            if (mClockRunning) freezeClockLocked(System.nanoTime());
            mPlayedFramesBase = 0;
            mMarkerReached = false;
            mNextPeriodFrame = mPeriodFrames;
            if (mPlayState == PLAYSTATE_PLAYING)
            {
                mClockStartNanos = System.nanoTime();
                mClockRunning = true;
            }

            return SUCCESS;
        }
    }

    @Override
    protected void finalize()
    {
        release();
    }

    @Deprecated
    protected int getNativeFrameCount()
    {
        return mBufferCapacityFrames;
    }

    @Deprecated
    protected void setState(int state)
    {
        synchronized (mLock)
        {
            mState = state;
        }
    }

    public void removeOnCodecFormatChangedListener(AudioTrack.OnCodecFormatChangedListener listener)
    {
        if (listener == null) throw new NullPointerException("listener must not be null");

        synchronized (mCodecFormatListeners)
        {
            mCodecFormatListeners.remove(listener);
        }
    }

    @Override
    public void removeOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener)
    {
        synchronized (mRoutingListeners)
        {
            mRoutingListeners.remove(listener);
        }
    }

    @Deprecated
    public void removeOnRoutingChangedListener(AudioTrack.OnRoutingChangedListener listener)
    {
        removeOnRoutingChangedListener((AudioRouting.OnRoutingChangedListener)listener);
    }

    public boolean setAudioDescriptionMixLeveldB(float level)
    {
        if (level > 48.0f || Float.isNaN(level)) throw new IllegalArgumentException("level is out of range" + level);

        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return false;
            mAudioDescriptionMixLeveldB = level;
            return true;
        }
    }

    public int setAuxEffectSendLevel(float level)
    {
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;
            mAuxSendLevel = clampGain(level);
            return SUCCESS;
        }
    }

    public int setBufferSizeInFrames(int bufferSizeInFrames)
    {
        synchronized (mLock)
        {
            if (mMode == MODE_STATIC || mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;
            if (bufferSizeInFrames < 0) return ERROR_BAD_VALUE;

            int minimum = mPerformanceMode == PERFORMANCE_MODE_LOW_LATENCY ? LOW_LATENCY_FRAMES : 16;
            mBufferSizeFrames = Math.max(minimum, Math.min(mBufferCapacityFrames, bufferSizeInFrames));
            mLock.notifyAll();
            return mBufferSizeFrames;
        }
    }

    public boolean setDualMonoMode(int dualMonoMode)
    {
        if (dualMonoMode < DUAL_MONO_MODE_OFF || dualMonoMode > DUAL_MONO_MODE_RR)
        {
            throw new IllegalArgumentException("Invalid Dual Mono mode " + dualMonoMode);
        }

        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return false;
            mDualMonoMode = dualMonoMode;
            return true;
        }
    }

    public void setLogSessionId(LogSessionId logSessionId)
    {
        if (logSessionId == null) throw new NullPointerException("logSessionId must not be null");

        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) throw new IllegalStateException("track not initialized");
            mLogSessionId = logSessionId;
        }
    }

    public int setLoopPoints(int startInFrames, int endInFrames, int loopCount)
    {
        synchronized (mLock)
        {
            if (mMode == MODE_STREAM || mState == STATE_UNINITIALIZED || mPlayState == PLAYSTATE_PLAYING)
            {
                return ERROR_INVALID_OPERATION;
            }

            if (loopCount != 0)
            {
                int capacity = mStaticData == null ? 0 : mStaticData.length / mFrameSize;
                if (startInFrames < 0 || startInFrames >= capacity || startInFrames >= endInFrames || endInFrames > capacity)
                {
                    return ERROR_BAD_VALUE;
                }
            }

            mLoopStart = startInFrames;
            mLoopEnd = endInFrames;
            mLoopCount = loopCount;
            return SUCCESS;
        }
    }

    public int setNotificationMarkerPosition(int markerInFrames)
    {
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;

            mMarkerPosition = markerInFrames;
            mMarkerReached = markerInFrames <= 0 || headPositionLocked(System.nanoTime()) >= markerInFrames;
            mLock.notifyAll();
            return SUCCESS;
        }
    }

    public void setOffloadDelayPadding(int delayInFrames, int paddingInFrames)
    {
        if (paddingInFrames < 0) throw new IllegalArgumentException("Illegal negative padding");
        if (delayInFrames < 0) throw new IllegalArgumentException("Illegal negative delay");
        if (mOffloadSupported == 0) throw new IllegalStateException("Illegal use of delay/padding on non-offloaded track");
        if (mState == STATE_UNINITIALIZED) throw new IllegalStateException("Uninitialized track");

        synchronized (mLock)
        {
            mOffloadDelay = delayInFrames;
            mOffloadPadding = paddingInFrames;
        }
    }

    public void setOffloadEndOfStream()
    {
        if (mOffloadSupported == 0) throw new IllegalStateException("EOS not supported on non-offloaded track");
        if (mState == STATE_UNINITIALIZED) throw new IllegalStateException("Uninitialized track");
        if (mPlayState != PLAYSTATE_PLAYING) throw new IllegalStateException("EOS not supported if not playing");
    }

    public int setPlaybackHeadPosition(int positionInFrames)
    {
        synchronized (mLock)
        {
            if (mMode == MODE_STREAM || mState == STATE_UNINITIALIZED || mPlayState == PLAYSTATE_PLAYING)
            {
                return ERROR_INVALID_OPERATION;
            }

            if (positionInFrames < 0 || positionInFrames > mStaticBytes / mFrameSize) return ERROR_BAD_VALUE;

            mPlayedFramesBase = positionInFrames;
            mClockRunning = false;
            mDraining = false;
            return SUCCESS;
        }
    }

    public void setPlaybackParams(PlaybackParams params)
    {
        if (params == null) throw new IllegalArgumentException("params is null");

        float speed = params.speedOrDefault();
        if (speed < 0.1f || speed > 8.0f) throw new IllegalArgumentException("speed is out of range: " + speed);

        synchronized (mLock)
        {
            rebaseClockLocked(System.nanoTime());

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
            mLock.notifyAll();
        }
    }

    public void setPlaybackPositionUpdateListener(AudioTrack.OnPlaybackPositionUpdateListener listener)
    {
        setPlaybackPositionUpdateListener(listener, null);
    }

    public void setPlaybackPositionUpdateListener(AudioTrack.OnPlaybackPositionUpdateListener listener, Handler handler)
    {
        synchronized (mLock)
        {
            mPositionListener = listener;

            if (listener == null)
            {
                mPositionHandler = null;
            }
            else if (handler != null)
            {
                mPositionHandler = handler;
            }
            else
            {
                mPositionHandler = mInitializationLooper == null ? null : new Handler(mInitializationLooper);
            }

            mLock.notifyAll();
            ensureNotifierLocked();
        }
    }

    @Deprecated
    public int setPlaybackRate(int sampleRateInHz)
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) return ERROR_INVALID_OPERATION;
            if (sampleRateInHz <= 0 || sampleRateInHz > mSampleRate * 8) return ERROR_BAD_VALUE;

            rebaseClockLocked(System.nanoTime());
            mPlaybackRate = sampleRateInHz;
            mLock.notifyAll();
            return SUCCESS;
        }
    }

    public int setPositionNotificationPeriod(int periodInFrames)
    {
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;

            mPeriodFrames = Math.max(0, periodInFrames);
            long head = headPositionLocked(System.nanoTime());
            mNextPeriodFrame = mPeriodFrames == 0 ? 0 : (head / mPeriodFrames + 1) * mPeriodFrames;
            mLock.notifyAll();
            return SUCCESS;
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

    public int setPresentation(AudioPresentation presentation)
    {
        if (presentation == null) throw new IllegalArgumentException("audio presentation is null");
        return ERROR_INVALID_OPERATION;
    }

    public int setStartThresholdInFrames(int startThresholdInFrames)
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("AudioTrack is not initialized");
            if (mMode != MODE_STREAM) throw new IllegalStateException("AudioTrack must be a streaming track");
            if (startThresholdInFrames < 1) throw new IllegalArgumentException("startThresholdInFrames must be positive");

            mStartThresholdFrames = Math.min(startThresholdInFrames, mBufferCapacityFrames);
            return mStartThresholdFrames;
        }
    }

    @Deprecated
    public int setStereoVolume(float leftGain, float rightGain)
    {
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;

            mLeftVolume = clampGain(leftGain);
            mRightVolume = clampGain(rightGain);
            return SUCCESS;
        }
    }

    public int setVolume(float gain)
    {
        return setStereoVolume(gain, gain);
    }

    public void stop() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("stop() called on uninitialized AudioTrack.");

            long now = System.nanoTime();

            if (mMode == MODE_STREAM && mPlayState == PLAYSTATE_PLAYING)
            {
                rebaseClockLocked(now);
                mDraining = true;
            }
            else
            {
                if (mClockRunning) freezeClockLocked(now);
                if (mMode == MODE_STATIC) mPlayedFramesBase = 0;
                mResetOnPlay = true;
            }

            mPlayState = PLAYSTATE_STOPPED;
            mLock.notifyAll();
        }

        AudioManager.unregisterActivePlayer(this);
    }

    public void unregisterStreamEventCallback(AudioTrack.StreamEventCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("Illegal null StreamEventCallback");

        synchronized (mStreamEventCallbacks)
        {
            if (mStreamEventCallbacks.remove(callback) == null)
            {
                throw new IllegalArgumentException("StreamEventCallback was not registered");
            }
        }
    }

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes)
    {
        return write(audioData, offsetInBytes, sizeInBytes, WRITE_BLOCKING);
    }

    public int write(byte[] audioData, int offsetInBytes, int sizeInBytes, int writeMode)
    {
        if (mState == STATE_UNINITIALIZED || mEncoding == AudioFormat.ENCODING_PCM_FLOAT) return ERROR_INVALID_OPERATION;
        if (writeMode != WRITE_BLOCKING && writeMode != WRITE_NON_BLOCKING) return ERROR_BAD_VALUE;
        if (audioData == null || offsetInBytes < 0 || sizeInBytes < 0 || offsetInBytes + sizeInBytes < 0
            || offsetInBytes + sizeInBytes > audioData.length)
        {
            return ERROR_BAD_VALUE;
        }

        return writeBytes(audioData, offsetInBytes, sizeInBytes, writeMode == WRITE_BLOCKING);
    }

    public int write(float[] audioData, int offsetInFloats, int sizeInFloats, int writeMode)
    {
        if (mState == STATE_UNINITIALIZED || mEncoding != AudioFormat.ENCODING_PCM_FLOAT) return ERROR_INVALID_OPERATION;
        if (writeMode != WRITE_BLOCKING && writeMode != WRITE_NON_BLOCKING) return ERROR_BAD_VALUE;
        if (audioData == null || offsetInFloats < 0 || sizeInFloats < 0 || offsetInFloats + sizeInFloats < 0
            || offsetInFloats + sizeInFloats > audioData.length)
        {
            return ERROR_BAD_VALUE;
        }

        int written = writeBytes(null, 0, sizeInFloats * 4, writeMode == WRITE_BLOCKING);
        return written < 0 ? written : written / 4;
    }

    public int write(ByteBuffer audioData, int sizeInBytes, int writeMode)
    {
        if (mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;
        if (writeMode != WRITE_BLOCKING && writeMode != WRITE_NON_BLOCKING) return ERROR_BAD_VALUE;
        if (audioData == null || sizeInBytes < 0 || sizeInBytes > audioData.remaining()) return ERROR_BAD_VALUE;

        byte[] staticCopy = null;
        if (mMode == MODE_STATIC)
        {
            staticCopy = new byte[sizeInBytes];
            audioData.duplicate().get(staticCopy);
        }

        int written = writeBytes(staticCopy, 0, sizeInBytes, writeMode == WRITE_BLOCKING);
        if (written > 0) audioData.position(audioData.position() + written);
        return written;
    }

    public int write(ByteBuffer audioData, int sizeInBytes, int writeMode, long timestamp)
    {
        if (mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;
        if (mMode == MODE_STATIC) return ERROR_INVALID_OPERATION;
        if ((mAttributes.getFlags() & AudioAttributes.FLAG_HW_AV_SYNC) == 0) return write(audioData, sizeInBytes, writeMode);
        if (timestamp < 0) return ERROR_BAD_VALUE;

        return write(audioData, sizeInBytes, writeMode);
    }

    public int write(short[] audioData, int offsetInShorts, int sizeInShorts)
    {
        return write(audioData, offsetInShorts, sizeInShorts, WRITE_BLOCKING);
    }

    public int write(short[] audioData, int offsetInShorts, int sizeInShorts, int writeMode)
    {
        if (mState == STATE_UNINITIALIZED || mEncoding == AudioFormat.ENCODING_PCM_FLOAT) return ERROR_INVALID_OPERATION;
        if (writeMode != WRITE_BLOCKING && writeMode != WRITE_NON_BLOCKING) return ERROR_BAD_VALUE;
        if (audioData == null || offsetInShorts < 0 || sizeInShorts < 0 || offsetInShorts + sizeInShorts < 0
            || offsetInShorts + sizeInShorts > audioData.length)
        {
            return ERROR_BAD_VALUE;
        }

        int written = writeBytes(null, 0, sizeInShorts * 2, writeMode == WRITE_BLOCKING);
        return written < 0 ? written : written / 2;
    }

    private int writeBytes(byte[] data, int offset, int size, boolean blocking)
    {
        synchronized (mLock)
        {
            if (mMode == MODE_STATIC) return writeStaticLocked(data, offset, size);

            int done = 0;

            while (done < size)
            {
                if (mState != STATE_INITIALIZED) return done > 0 ? done : ERROR_DEAD_OBJECT;

                long now = System.nanoTime();
                long position = positionLocked(now);
                long space = (long)mBufferSizeFrames * mFrameSize - (mBytesWritten - position * mFrameSize);

                if (space > 0)
                {
                    long chunk = Math.min(space, size - done);
                    mBytesWritten += chunk;
                    done += (int)chunk;
                    mStarved = false;
                    continue;
                }

                if (!blocking || mPlayState == PLAYSTATE_STOPPED) break;

                try
                {
                    if (mPlayState == PLAYSTATE_PLAYING)
                    {
                        long frames = Math.max(1, Math.min(mBufferSizeFrames / 4, (size - done) / mFrameSize));
                        long waitNanos = framesToNanos(frames);
                        mLock.wait(Math.max(1, waitNanos / 1000000L), (int)(waitNanos % 1000000L));
                    }
                    else
                    {
                        mLock.wait();
                    }
                }
                catch (InterruptedException exception)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return done;
        }
    }

    private int writeStaticLocked(byte[] data, int offset, int size)
    {
        if (mStaticData == null) return ERROR_DEAD_OBJECT;

        int count = Math.min(size, mStaticData.length - mStaticBytes);
        if (count <= 0) return 0;

        if (data != null) System.arraycopy(data, offset, mStaticData, mStaticBytes, count);
        mStaticBytes += count;
        mBytesWritten = mStaticBytes;
        if (mState == STATE_NO_STATIC_DATA) mState = STATE_INITIALIZED;
        return count;
    }

    private double framesPerNanoLocked()
    {
        return mPlaybackRate * (double)mPlaybackParams.speedOrDefault() / NANOS_PER_SECOND;
    }

    private long framesToNanos(long frames)
    {
        double rate = framesPerNanoLocked();
        return rate <= 0 ? NANOS_PER_SECOND : (long)Math.ceil(frames / rate);
    }

    private long positionLocked(long now)
    {
        long elapsed = 0;
        if (mClockRunning) elapsed = (long)((now - mClockStartNanos) * framesPerNanoLocked());

        if (mMode == MODE_STATIC) return mPlayedFramesBase + elapsed;

        long available = mBytesWritten / mFrameSize;
        long position = mPlayedFramesBase + elapsed;

        if (position >= available)
        {
            if (mClockRunning && !mDraining && !mStarved && mPlayState == PLAYSTATE_PLAYING && elapsed > 0)
            {
                mUnderrunCount++;
                mStarved = true;
            }

            if (mClockRunning)
            {
                mPlayedFramesBase = available;
                mClockStartNanos = now;
                if (mDraining)
                {
                    mClockRunning = false;
                    mDraining = false;
                    mResetOnPlay = true;
                }
            }

            return available;
        }

        return position;
    }

    private long headPositionLocked(long now)
    {
        long position = positionLocked(now);
        if (mMode != MODE_STATIC) return position & 0xffffffffL;

        long frames = mStaticBytes / mFrameSize;
        long loopLength = mLoopEnd - mLoopStart;

        if (mLoopCount != 0 && loopLength > 0 && position >= mLoopEnd)
        {
            long laps = (position - mLoopEnd) / loopLength + 1;

            if (mLoopCount < 0 || laps <= mLoopCount)
            {
                return mLoopStart + (position - mLoopStart) % loopLength;
            }

            position -= mLoopCount * loopLength;
        }

        if (position >= frames && mClockRunning && mPlayState == PLAYSTATE_PLAYING)
        {
            mPlayedFramesBase = position;
            mClockRunning = false;
        }

        return Math.min(position, frames);
    }

    private void rebaseClockLocked(long now)
    {
        if (!mClockRunning) return;

        long position = positionLocked(now);
        if (!mClockRunning) return;

        mPlayedFramesBase = position;
        mClockStartNanos = now;
    }

    private void freezeClockLocked(long now)
    {
        if (!mClockRunning) return;

        long position = positionLocked(now);
        mPlayedFramesBase = position;
        mClockRunning = false;
        mDraining = false;
    }

    private static float clampGain(float gain)
    {
        if (Float.isNaN(gain)) return GAIN_MIN;
        return Math.max(GAIN_MIN, Math.min(GAIN_MAX, gain));
    }

    private void ensureNotifierLocked()
    {
        if (mNotifierThread != null || mPositionListener == null || mState == STATE_UNINITIALIZED) return;

        mNotifierThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                runNotifier();
            }
        }, "AudioTrackPosition");
        mNotifierThread.setDaemon(true);
        mNotifierThread.start();
    }

    private void runNotifier()
    {
        synchronized (mLock)
        {
            try
            {
                while (mState != STATE_UNINITIALIZED && mPositionListener != null)
                {
                    long now = System.nanoTime();
                    long head = headPositionLocked(now);
                    final OnPlaybackPositionUpdateListener listener = mPositionListener;
                    Handler handler = mPositionHandler;

                    if (mMarkerPosition > 0 && !mMarkerReached && head >= mMarkerPosition)
                    {
                        mMarkerReached = true;
                        dispatch(handler, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                listener.onMarkerReached(AudioTrack.this);
                            }
                        });
                    }

                    if (mPeriodFrames > 0 && head >= mNextPeriodFrame && mClockRunning)
                    {
                        mNextPeriodFrame = (head / mPeriodFrames + 1) * mPeriodFrames;
                        dispatch(handler, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                listener.onPeriodicNotification(AudioTrack.this);
                            }
                        });
                    }

                    if (!mClockRunning)
                    {
                        mLock.wait(100);
                        continue;
                    }

                    long target = Long.MAX_VALUE;
                    if (mMarkerPosition > 0 && !mMarkerReached) target = mMarkerPosition;
                    if (mPeriodFrames > 0) target = Math.min(target, Math.max(mNextPeriodFrame, head + 1));

                    long waitNanos = target == Long.MAX_VALUE ? 100000000L : framesToNanos(Math.max(1, target - head));
                    waitNanos = Math.max(1000000L, Math.min(waitNanos, 100000000L));
                    mLock.wait(waitNanos / 1000000L, (int)(waitNanos % 1000000L));
                }
            }
            catch (InterruptedException exception)
            {
                Thread.currentThread().interrupt();
            }
            finally
            {
                mNotifierThread = null;
            }
        }
    }

    private static void dispatch(Handler handler, Runnable runnable)
    {
        if (handler != null)
        {
            handler.post(runnable);
        }
        else
        {
            runnable.run();
        }
    }

    public static abstract class StreamEventCallback
    {
        public StreamEventCallback()
        {
        }

        public void onDataRequest(AudioTrack track, int sizeInFrames)
        {
        }

        public void onPresentationEnded(AudioTrack track)
        {
        }

        public void onTearDown(AudioTrack track)
        {
        }
    }

    @Deprecated
    public interface OnRoutingChangedListener extends AudioRouting.OnRoutingChangedListener
    {
        @Override
        default void onRoutingChanged(AudioRouting router)
        {
            if (router instanceof AudioTrack) onRoutingChanged((AudioTrack)router);
        }

        void onRoutingChanged(AudioTrack audioTrack);
    }

    public interface OnPlaybackPositionUpdateListener
    {
        void onMarkerReached(AudioTrack track);

        void onPeriodicNotification(AudioTrack track);
    }

    public interface OnCodecFormatChangedListener
    {
        void onCodecFormatChanged(AudioTrack audioTrack, AudioMetadataReadMap info);
    }

    public static final class MetricsConstants
    {
        public static final String CHANNELMASK = "android.media.audiorecord.channelmask";
        public static final String CONTENTTYPE = "android.media.audiotrack.type";
        public static final String SAMPLERATE = "android.media.audiorecord.samplerate";
        public static final String STREAMTYPE = "android.media.audiotrack.streamtype";
        public static final String USAGE = "android.media.audiotrack.usage";

        private MetricsConstants()
        {
        }
    }

    public static class Builder
    {
        private AudioAttributes mAttributes;
        private AudioFormat mFormat;
        private int mBufferSizeInBytes;
        private int mSessionId = AudioManager.AUDIO_SESSION_ID_GENERATE;
        private int mMode = MODE_STREAM;
        private int mPerformanceMode = PERFORMANCE_MODE_NONE;
        private boolean mOffload;

        public Builder()
        {
        }

        public AudioTrack build() throws UnsupportedOperationException
        {
            AudioAttributes attributes = mAttributes;
            if (attributes == null) attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();

            AudioFormat format = mFormat;
            if (format == null)
            {
                format = new AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_DEFAULT).build();
            }

            if (mOffload && !isPcm(format.getEncoding()))
            {
                throw new UnsupportedOperationException("Offload configuration not supported");
            }

            int bufferSize = mBufferSizeInBytes;
            if (bufferSize == 0)
            {
                if (mMode == MODE_STATIC) throw new UnsupportedOperationException("Static mode requires a buffer size");
                bufferSize = defaultBufferSize(format);
            }

            AudioTrack track;
            try
            {
                track = new AudioTrack(attributes, format, bufferSize, mMode, mSessionId, mPerformanceMode, false);
            }
            catch (IllegalArgumentException exception)
            {
                throw new UnsupportedOperationException(exception.getMessage());
            }

            if (track.getState() == STATE_UNINITIALIZED)
            {
                throw new UnsupportedOperationException("Cannot create AudioTrack");
            }

            return track;
        }

        private int defaultBufferSize(AudioFormat format)
        {
            int sampleRate = format.getSampleRate() == AudioFormat.SAMPLE_RATE_UNSPECIFIED
                ? NATIVE_OUTPUT_SAMPLE_RATE : format.getSampleRate();
            int encoding = format.getEncoding() == AudioFormat.ENCODING_DEFAULT ? AudioFormat.ENCODING_PCM_16BIT : format.getEncoding();
            int channels = format.getChannelCount() > 0 ? format.getChannelCount() : 2;

            if (!isPcm(encoding)) return Math.max(1, minimumFrames(sampleRate) * 4);

            int frames = mPerformanceMode == PERFORMANCE_MODE_LOW_LATENCY ? LOW_LATENCY_FRAMES * 2 : minimumFrames(sampleRate);
            return frames * channels * bytesPerSample(encoding);
        }

        public AudioTrack.Builder setAudioAttributes(AudioAttributes attributes) throws IllegalArgumentException
        {
            if (attributes == null) throw new IllegalArgumentException("Illegal null AudioAttributes argument");
            mAttributes = attributes;
            return this;
        }

        public AudioTrack.Builder setAudioFormat(AudioFormat format) throws IllegalArgumentException
        {
            if (format == null) throw new IllegalArgumentException("Illegal null AudioFormat argument");
            mFormat = format;
            return this;
        }

        public AudioTrack.Builder setBufferSizeInBytes(int bufferSizeInBytes) throws IllegalArgumentException
        {
            if (bufferSizeInBytes <= 0) throw new IllegalArgumentException("Invalid buffer size " + bufferSizeInBytes);
            mBufferSizeInBytes = bufferSizeInBytes;
            return this;
        }

        public AudioTrack.Builder setContext(Context context)
        {
            if (context == null) throw new NullPointerException("context must not be null");
            return this;
        }

        public AudioTrack.Builder setEncapsulationMode(int encapsulationMode)
        {
            if (encapsulationMode != ENCAPSULATION_MODE_NONE && encapsulationMode != ENCAPSULATION_MODE_ELEMENTARY_STREAM)
            {
                throw new IllegalArgumentException("Invalid encapsulation mode " + encapsulationMode);
            }

            return this;
        }

        public AudioTrack.Builder setOffloadedPlayback(boolean offload)
        {
            mOffload = offload;
            return this;
        }

        public AudioTrack.Builder setPerformanceMode(int performanceMode)
        {
            switch (performanceMode)
            {
                case PERFORMANCE_MODE_NONE:
                case PERFORMANCE_MODE_LOW_LATENCY:
                case PERFORMANCE_MODE_POWER_SAVING:
                    mPerformanceMode = performanceMode;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid performance mode " + performanceMode);
            }

            return this;
        }

        public AudioTrack.Builder setSessionId(int sessionId) throws IllegalArgumentException
        {
            if (sessionId != AudioManager.AUDIO_SESSION_ID_GENERATE && sessionId < 1)
            {
                throw new IllegalArgumentException("Invalid audio session ID " + sessionId);
            }

            mSessionId = sessionId;
            return this;
        }

        public AudioTrack.Builder setTransferMode(int mode) throws IllegalArgumentException
        {
            if (mode != MODE_STREAM && mode != MODE_STATIC) throw new IllegalArgumentException("Invalid transfer mode " + mode);
            mMode = mode;
            return this;
        }
    }
}
