package android.media;

import android.content.Context;
import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.view.Surface;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class MediaRecorder implements AudioRecordingMonitor, AudioRouting, MicrophoneDirection
{
    public static final int MEDIA_ERROR_SERVER_DIED = 100;
    public static final int MEDIA_RECORDER_ERROR_UNKNOWN = 1;
    public static final int MEDIA_RECORDER_INFO_MAX_DURATION_REACHED = 800;
    public static final int MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING = 802;
    public static final int MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED = 801;
    public static final int MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED = 803;
    public static final int MEDIA_RECORDER_INFO_UNKNOWN = 1;

    private static final int STATE_INITIAL = 0;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CONFIGURED = 2;
    private static final int STATE_PREPARED = 3;
    private static final int STATE_RECORDING = 4;
    private static final int STATE_PAUSED = 5;
    private static final int STATE_RELEASED = 6;
    private static final int STATE_ERROR = 7;

    private final Object mLock = new Object();
    private final Map<AudioRouting.OnRoutingChangedListener, Handler> mRoutingListeners = new HashMap<>();
    private final Map<AudioManager.AudioRecordingCallback, Executor> mRecordingCallbacks = new HashMap<>();

    private int mState = STATE_INITIAL;
    private int mAudioSource = -1;
    private int mVideoSource = -1;
    private int mOutputFormat = -1;
    private int mAudioEncoder = -1;
    private int mVideoEncoder = -1;
    private int mAudioChannels;
    private int mAudioSamplingRate;
    private int mAudioBitRate;
    private int mVideoBitRate;
    private int mVideoFrameRate;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mOrientationHint;
    private double mCaptureRate;
    private boolean mPrivacySensitive;
    private Object mOutput;
    private Surface mInputSurface;
    private LogSessionId mLogSessionId = LogSessionId.LOG_SESSION_ID_NONE;
    private AudioDeviceInfo mPreferredDevice;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;

    public MediaRecorder()
    {
    }

    public MediaRecorder(Context context)
    {
        if (context == null) throw new NullPointerException("context must not be null");
    }

    @Override
    public void addOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener, Handler handler)
    {
        if (listener == null) return;

        Looper looper = Looper.myLooper() != null ? Looper.myLooper() : Looper.getMainLooper();
        synchronized (mRoutingListeners)
        {
            mRoutingListeners.put(listener, handler != null ? handler : new Handler(looper));
        }
    }

    public List<MicrophoneInfo> getActiveMicrophones() throws IOException
    {
        return new ArrayList<>();
    }

    @Override
    public AudioRecordingConfiguration getActiveRecordingConfiguration()
    {
        return null;
    }

    public static final int getAudioSourceMax()
    {
        return AudioSource.VOICE_PERFORMANCE;
    }

    public LogSessionId getLogSessionId()
    {
        synchronized (mLock)
        {
            return mLogSessionId;
        }
    }

    public int getMaxAmplitude() throws IllegalStateException
    {
        synchronized (mLock)
        {
            checkRecordingLocked("getMaxAmplitude");
            return 0;
        }
    }

    public PersistableBundle getMetrics()
    {
        synchronized (mLock)
        {
            PersistableBundle bundle = new PersistableBundle();
            bundle.putInt(MetricsConstants.AUDIO_BITRATE, mAudioBitRate);
            bundle.putInt(MetricsConstants.AUDIO_CHANNELS, mAudioChannels);
            bundle.putInt(MetricsConstants.AUDIO_SAMPLERATE, mAudioSamplingRate);
            bundle.putInt(MetricsConstants.VIDEO_BITRATE, mVideoBitRate);
            bundle.putInt(MetricsConstants.FRAMERATE, mVideoFrameRate);
            bundle.putInt(MetricsConstants.WIDTH, mVideoWidth);
            bundle.putInt(MetricsConstants.HEIGHT, mVideoHeight);
            bundle.putInt(MetricsConstants.ROTATION, mOrientationHint);
            bundle.putDouble(MetricsConstants.CAPTURE_FPS, mCaptureRate);
            bundle.putInt(MetricsConstants.CAPTURE_FPS_ENABLE, mCaptureRate > 0 ? 1 : 0);
            return bundle;
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

    public Surface getSurface()
    {
        synchronized (mLock)
        {
            if (mState != STATE_PREPARED && mState != STATE_RECORDING && mState != STATE_PAUSED)
            {
                throw new IllegalStateException("getSurface called in an invalid state");
            }

            throw new IllegalStateException("No surface is available for the configured video source");
        }
    }

    public boolean isPrivacySensitive()
    {
        synchronized (mLock)
        {
            return mPrivacySensitive;
        }
    }

    public void pause() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_RECORDING) throw new IllegalStateException("pause called in an invalid state");
            mState = STATE_PAUSED;
        }
    }

    public void prepare() throws IOException, IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_CONFIGURED) throw new IllegalStateException("prepare called in an invalid state");
            if (mOutput == null) throw new IllegalStateException("No output file specified");
            if (mAudioSource >= 0 && mAudioEncoder < 0) throw new IllegalStateException("Audio encoder not set");
            if (mVideoSource >= 0 && mVideoEncoder < 0) throw new IllegalStateException("Video encoder not set");

            mState = STATE_ERROR;
            throw new IOException("prepare failed: no media encoder is available");
        }
    }

    @Override
    public void registerAudioRecordingCallback(Executor executor, AudioManager.AudioRecordingCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("Illegal null AudioRecordingCallback");
        if (executor == null) throw new IllegalArgumentException("Illegal null Executor");

        synchronized (mRecordingCallbacks)
        {
            if (mRecordingCallbacks.containsKey(callback)) throw new IllegalArgumentException("AudioRecordingCallback already registered");
            mRecordingCallbacks.put(callback, executor);
        }
    }

    public void release()
    {
        synchronized (mLock)
        {
            clearConfigurationLocked();
            mState = STATE_RELEASED;
            mOnErrorListener = null;
            mOnInfoListener = null;
        }
    }

    @Override
    protected void finalize()
    {
        release();
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
            clearConfigurationLocked();
            mState = STATE_INITIAL;
        }
    }

    public void resume() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_PAUSED) throw new IllegalStateException("resume called in an invalid state");
            mState = STATE_RECORDING;
        }
    }

    public void setAudioChannels(int numChannels)
    {
        if (numChannels <= 0) throw new IllegalArgumentException("Number of channels is not positive");

        synchronized (mLock)
        {
            checkConfigurableLocked("setAudioChannels");
            mAudioChannels = numChannels;
        }
    }

    public void setAudioEncoder(int audioEncoder) throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_CONFIGURED) throw new IllegalStateException("setAudioEncoder called in an invalid state");
            if (mAudioSource < 0) throw new IllegalStateException("setAudioEncoder called before setAudioSource");
            mAudioEncoder = audioEncoder;
        }
    }

    public void setAudioEncodingBitRate(int bitRate)
    {
        if (bitRate <= 0) throw new IllegalArgumentException("Audio encoding bit rate is not positive");

        synchronized (mLock)
        {
            checkConfigurableLocked("setAudioEncodingBitRate");
            mAudioBitRate = bitRate;
        }
    }

    public void setAudioProfile(EncoderProfiles.AudioProfile profile)
    {
        setAudioEncodingBitRate(profile.getBitrate());
        setAudioChannels(profile.getChannels());
        setAudioSamplingRate(profile.getSampleRate());
        setAudioEncoder(profile.getCodec());
    }

    public void setAudioSamplingRate(int samplingRate)
    {
        if (samplingRate <= 0) throw new IllegalArgumentException("Audio sampling rate is not positive");

        synchronized (mLock)
        {
            checkConfigurableLocked("setAudioSamplingRate");
            mAudioSamplingRate = samplingRate;
        }
    }

    public void setAudioSource(int audioSource) throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIAL && mState != STATE_INITIALIZED)
            {
                throw new IllegalStateException("setAudioSource called in an invalid state");
            }

            if (audioSource < AudioSource.DEFAULT || audioSource > getAudioSourceMax())
            {
                throw new IllegalArgumentException("Invalid audio source " + audioSource);
            }

            mAudioSource = audioSource;
            mState = STATE_INITIALIZED;
        }
    }

    public void setCaptureRate(double fps)
    {
        synchronized (mLock)
        {
            checkConfigurableLocked("setCaptureRate");
            mCaptureRate = fps;
        }
    }

    public void setInputSurface(Surface surface)
    {
        if (surface == null) throw new IllegalArgumentException("surface must not be null");

        synchronized (mLock)
        {
            if (mVideoSource != VideoSource.SURFACE) throw new IllegalStateException("Video source is not SURFACE");
            mInputSurface = surface;
        }
    }

    public void setLocation(float latitude, float longitude)
    {
        if (latitude < -90 || latitude > 90) throw new IllegalArgumentException("Latitude: " + latitude + " out of range.");
        if (longitude < -180 || longitude > 180) throw new IllegalArgumentException("Longitude: " + longitude + " out of range");
    }

    public void setLogSessionId(LogSessionId logSessionId)
    {
        if (logSessionId == null) throw new NullPointerException("logSessionId must not be null");

        synchronized (mLock)
        {
            mLogSessionId = logSessionId;
        }
    }

    public void setMaxDuration(int maxDurationMs) throws IllegalArgumentException
    {
        synchronized (mLock)
        {
            checkConfigurableLocked("setMaxDuration");
        }
    }

    public void setMaxFileSize(long maxFileSizeBytes) throws IllegalArgumentException
    {
        synchronized (mLock)
        {
            checkConfigurableLocked("setMaxFileSize");
        }
    }

    public void setNextOutputFile(File file) throws IOException
    {
        synchronized (mLock)
        {
            checkRecordingLocked("setNextOutputFile");
        }

        throw new IOException("No recording is in progress");
    }

    public void setNextOutputFile(FileDescriptor descriptor) throws IOException
    {
        synchronized (mLock)
        {
            checkRecordingLocked("setNextOutputFile");
        }

        throw new IOException("No recording is in progress");
    }

    public void setOnErrorListener(MediaRecorder.OnErrorListener listener)
    {
        synchronized (mLock)
        {
            mOnErrorListener = listener;
        }
    }

    public void setOnInfoListener(MediaRecorder.OnInfoListener listener)
    {
        synchronized (mLock)
        {
            mOnInfoListener = listener;
        }
    }

    public void setOrientationHint(int degrees)
    {
        if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270)
        {
            throw new IllegalArgumentException("Unsupported angle: " + degrees);
        }

        synchronized (mLock)
        {
            checkConfigurableLocked("setOrientationHint");
            mOrientationHint = degrees;
        }
    }

    public void setOutputFile(File file)
    {
        setOutputTarget(file);
    }

    public void setOutputFile(FileDescriptor descriptor) throws IllegalStateException
    {
        setOutputTarget(descriptor);
    }

    public void setOutputFile(String path) throws IllegalStateException
    {
        setOutputTarget(path);
    }

    private void setOutputTarget(Object target)
    {
        synchronized (mLock)
        {
            if (mState != STATE_CONFIGURED) throw new IllegalStateException("setOutputFile called in an invalid state");
            mOutput = target;
        }
    }

    public void setOutputFormat(int outputFormat) throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("setOutputFormat called in an invalid state");
            mOutputFormat = outputFormat;
            mState = STATE_CONFIGURED;
        }
    }

    @Override
    public boolean setPreferredDevice(AudioDeviceInfo deviceInfo)
    {
        if (deviceInfo != null && !deviceInfo.isSource()) return false;

        synchronized (mLock)
        {
            mPreferredDevice = deviceInfo;
            return true;
        }
    }

    @Override
    public boolean setPreferredMicrophoneDirection(int direction)
    {
        return false;
    }

    @Override
    public boolean setPreferredMicrophoneFieldDimension(float zoom)
    {
        if (zoom < -1.0f || zoom > 1.0f) throw new IllegalArgumentException("Argument must fall between -1 & 1 (inclusive)");
        return false;
    }

    public void setPreviewDisplay(Surface surface)
    {
    }

    public void setPrivacySensitive(boolean privacySensitive)
    {
        synchronized (mLock)
        {
            if (mAudioSource == AudioSource.VOICE_CALL || mAudioSource == AudioSource.VOICE_UPLINK
                || mAudioSource == AudioSource.VOICE_DOWNLINK || mAudioSource == AudioSource.REMOTE_SUBMIX)
            {
                throw new IllegalArgumentException("Invalid audio source for privacy sensitive capture");
            }

            mPrivacySensitive = privacySensitive;
        }
    }

    public void setProfile(CamcorderProfile profile)
    {
        setOutputFormat(profile.fileFormat);
        setVideoFrameRate(profile.videoFrameRate);
        setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        setVideoEncodingBitRate(profile.videoBitRate);
        setVideoEncoder(profile.videoCodec);

        if (mAudioSource >= 0)
        {
            setAudioEncodingBitRate(profile.audioBitRate);
            setAudioChannels(profile.audioChannels);
            setAudioSamplingRate(profile.audioSampleRate);
            setAudioEncoder(profile.audioCodec);
        }
    }

    public void setVideoEncoder(int videoEncoder) throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_CONFIGURED) throw new IllegalStateException("setVideoEncoder called in an invalid state");
            if (mVideoSource < 0) throw new IllegalStateException("setVideoEncoder called before setVideoSource");
            mVideoEncoder = videoEncoder;
        }
    }

    public void setVideoEncodingBitRate(int bitRate)
    {
        if (bitRate <= 0) throw new IllegalArgumentException("Video encoding bit rate is not positive");

        synchronized (mLock)
        {
            checkConfigurableLocked("setVideoEncodingBitRate");
            mVideoBitRate = bitRate;
        }
    }

    public void setVideoEncodingProfileLevel(int profile, int level)
    {
        if (profile <= 0) throw new IllegalArgumentException("Video encoding profile is not positive");
        if (level <= 0) throw new IllegalArgumentException("Video encoding level is not positive");

        synchronized (mLock)
        {
            checkConfigurableLocked("setVideoEncodingProfileLevel");
        }
    }

    public void setVideoFrameRate(int rate) throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_CONFIGURED) throw new IllegalStateException("setVideoFrameRate called in an invalid state");
            mVideoFrameRate = rate;
        }
    }

    public void setVideoProfile(EncoderProfiles.VideoProfile profile)
    {
        setVideoFrameRate(profile.getFrameRate());
        setVideoSize(profile.getWidth(), profile.getHeight());
        setVideoEncodingBitRate(profile.getBitrate());
        setVideoEncoder(profile.getCodec());
        if (profile.getProfile() > 0) setVideoEncodingProfileLevel(profile.getProfile(), 1);
    }

    public void setVideoSize(int width, int height) throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_CONFIGURED) throw new IllegalStateException("setVideoSize called in an invalid state");
            mVideoWidth = width;
            mVideoHeight = height;
        }
    }

    public void setVideoSource(int videoSource) throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIAL && mState != STATE_INITIALIZED)
            {
                throw new IllegalStateException("setVideoSource called in an invalid state");
            }

            if (videoSource < VideoSource.DEFAULT || videoSource > VideoSource.SURFACE)
            {
                throw new IllegalArgumentException("Invalid video source " + videoSource);
            }

            mVideoSource = videoSource;
            mState = STATE_INITIALIZED;
        }
    }

    public void start() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_PREPARED) throw new IllegalStateException("start called in an invalid state: " + mState);
            mState = STATE_RECORDING;
        }
    }

    public void stop() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_RECORDING && mState != STATE_PAUSED) throw new IllegalStateException("stop called in an invalid state");
            mState = STATE_INITIAL;
            clearConfigurationLocked();
        }
    }

    @Override
    public void unregisterAudioRecordingCallback(AudioManager.AudioRecordingCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");

        synchronized (mRecordingCallbacks)
        {
            if (mRecordingCallbacks.remove(callback) == null)
            {
                throw new IllegalArgumentException("AudioRecordingCallback was not registered");
            }
        }
    }

    private void checkRecordingLocked(String operation)
    {
        if (mState != STATE_RECORDING && mState != STATE_PAUSED)
        {
            throw new IllegalStateException(operation + " called in an invalid state");
        }
    }

    private void checkConfigurableLocked(String operation)
    {
        if (mState != STATE_CONFIGURED && mState != STATE_INITIALIZED)
        {
            throw new IllegalStateException(operation + " called in an invalid state");
        }
    }

    private void clearConfigurationLocked()
    {
        mAudioSource = -1;
        mVideoSource = -1;
        mOutputFormat = -1;
        mAudioEncoder = -1;
        mVideoEncoder = -1;
        mAudioChannels = 0;
        mAudioSamplingRate = 0;
        mAudioBitRate = 0;
        mVideoBitRate = 0;
        mVideoFrameRate = 0;
        mVideoWidth = 0;
        mVideoHeight = 0;
        mOrientationHint = 0;
        mCaptureRate = 0;
        mOutput = null;
        mInputSurface = null;
    }

    public final class VideoSource
    {
        public static final int CAMERA = 1;
        public static final int DEFAULT = 0;
        public static final int SURFACE = 2;

        private VideoSource()
        {
        }
    }

    public final class VideoEncoder
    {
        public static final int AV1 = 8;
        public static final int DEFAULT = 0;
        public static final int DOLBY_VISION = 7;
        public static final int H263 = 1;
        public static final int H264 = 2;
        public static final int HEVC = 5;
        public static final int MPEG_4_SP = 3;
        public static final int VP8 = 4;
        public static final int VP9 = 6;

        private VideoEncoder()
        {
        }
    }

    public final class OutputFormat
    {
        public static final int AAC_ADTS = 6;
        public static final int AMR_NB = 3;
        public static final int AMR_WB = 4;
        public static final int DEFAULT = 0;
        public static final int MPEG_2_TS = 8;
        public static final int MPEG_4 = 2;
        public static final int OGG = 11;
        public static final int RAW_AMR = 3;
        public static final int THREE_GPP = 1;
        public static final int WEBM = 9;

        private OutputFormat()
        {
        }
    }

    public interface OnInfoListener
    {
        void onInfo(MediaRecorder recorder, int what, int extra);
    }

    public interface OnErrorListener
    {
        void onError(MediaRecorder recorder, int what, int extra);
    }

    public static final class MetricsConstants
    {
        public static final String AUDIO_BITRATE = "android.media.mediarecorder.audio-bitrate";
        public static final String AUDIO_CHANNELS = "android.media.mediarecorder.audio-channels";
        public static final String AUDIO_SAMPLERATE = "android.media.mediarecorder.audio-samplerate";
        public static final String AUDIO_TIMESCALE = "android.media.mediarecorder.audio-timescale";
        public static final String CAPTURE_FPS = "android.media.mediarecorder.capture-fps";
        public static final String CAPTURE_FPS_ENABLE = "android.media.mediarecorder.capture-fpsenable";
        public static final String FRAMERATE = "android.media.mediarecorder.frame-rate";
        public static final String HEIGHT = "android.media.mediarecorder.height";
        public static final String MOVIE_TIMESCALE = "android.media.mediarecorder.movie-timescale";
        public static final String ROTATION = "android.media.mediarecorder.rotation";
        public static final String VIDEO_BITRATE = "android.media.mediarecorder.video-bitrate";
        public static final String VIDEO_IFRAME_INTERVAL = "android.media.mediarecorder.video-iframe-interval";
        public static final String VIDEO_LEVEL = "android.media.mediarecorder.video-encoder-level";
        public static final String VIDEO_PROFILE = "android.media.mediarecorder.video-encoder-profile";
        public static final String VIDEO_TIMESCALE = "android.media.mediarecorder.video-timescale";
        public static final String WIDTH = "android.media.mediarecorder.width";

        private MetricsConstants()
        {
        }
    }

    public final class AudioSource
    {
        public static final int CAMCORDER = 5;
        public static final int DEFAULT = 0;
        public static final int MIC = 1;
        public static final int REMOTE_SUBMIX = 8;
        public static final int UNPROCESSED = 9;
        public static final int VOICE_CALL = 4;
        public static final int VOICE_COMMUNICATION = 7;
        public static final int VOICE_DOWNLINK = 3;
        public static final int VOICE_PERFORMANCE = 10;
        public static final int VOICE_RECOGNITION = 6;
        public static final int VOICE_UPLINK = 2;

        private AudioSource()
        {
        }
    }

    public final class AudioEncoder
    {
        public static final int AAC = 3;
        public static final int AAC_ELD = 5;
        public static final int AMR_NB = 1;
        public static final int AMR_WB = 2;
        public static final int DEFAULT = 0;
        public static final int HE_AAC = 4;
        public static final int OPUS = 7;
        public static final int VORBIS = 6;

        private AudioEncoder()
        {
        }
    }
}
