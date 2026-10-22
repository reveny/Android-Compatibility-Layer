package android.media;

import android.content.Context;
import android.media.metrics.LogSessionId;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class AudioRecord implements AudioRecordingMonitor, AudioRouting, MicrophoneDirection
{
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -2;
    public static final int ERROR_DEAD_OBJECT = -6;
    public static final int ERROR_INVALID_OPERATION = -3;
    public static final int READ_BLOCKING = 0;
    public static final int READ_NON_BLOCKING = 1;
    public static final int RECORDSTATE_RECORDING = 3;
    public static final int RECORDSTATE_STOPPED = 1;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_UNINITIALIZED = 0;
    public static final int SUCCESS = 0;

    private static final int SAMPLE_RATE_MIN = 4000;
    private static final int SAMPLE_RATE_MAX = 192000;
    private static final int DEFAULT_SAMPLE_RATE = 48000;
    private static final int AUDIO_SOURCE_MAX = 10;
    private static final long NANOS_PER_SECOND = 1000000000L;

    private final Object mLock = new Object();
    private final int mAudioSource;
    private final int mSampleRate;
    private final int mChannelMask;
    private final int mChannelIndexMask;
    private final int mChannelCount;
    private final int mEncoding;
    private final int mFrameSize;
    private final int mBufferFrames;
    private final int mSessionId;
    private final boolean mPrivacySensitive;
    private final Looper mInitializationLooper;

    private int mState = STATE_UNINITIALIZED;
    private int mRecordingState = RECORDSTATE_STOPPED;
    private long mStartNanos;
    private long mFramesConsumed;
    private long mFramesCapturedBase;
    private int mMarkerPosition;
    private boolean mMarkerReached;
    private int mPeriodFrames;
    private long mNextPeriodFrame;
    private LogSessionId mLogSessionId = LogSessionId.LOG_SESSION_ID_NONE;
    private AudioDeviceInfo mPreferredDevice;
    private OnRecordPositionUpdateListener mPositionListener;
    private Handler mPositionHandler;
    private Thread mNotifierThread;
    private final Map<AudioRouting.OnRoutingChangedListener, Handler> mRoutingListeners = new HashMap<>();
    private final Map<AudioManager.AudioRecordingCallback, Executor> mRecordingCallbacks = new HashMap<>();

    public AudioRecord(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes)
        throws IllegalArgumentException
    {
        this(audioSource, legacyFormat(sampleRateInHz, channelConfig, audioFormat), bufferSizeInBytes, false);
    }

    private AudioRecord(int audioSource, AudioFormat format, int bufferSizeInBytes, boolean privacySensitive)
    {
        if (audioSource == MediaRecorder.AudioSource.DEFAULT) audioSource = MediaRecorder.AudioSource.MIC;
        if (audioSource < MediaRecorder.AudioSource.DEFAULT || audioSource > AUDIO_SOURCE_MAX)
        {
            throw new IllegalArgumentException("Invalid audio source " + audioSource);
        }

        int sampleRate = format.getSampleRate();
        if (sampleRate == AudioFormat.SAMPLE_RATE_UNSPECIFIED) sampleRate = DEFAULT_SAMPLE_RATE;
        if (sampleRate < SAMPLE_RATE_MIN || sampleRate > SAMPLE_RATE_MAX)
        {
            throw new IllegalArgumentException(sampleRate + "Hz is not a supported sample rate.");
        }

        int encoding = format.getEncoding();
        if (encoding == AudioFormat.ENCODING_DEFAULT || encoding == AudioFormat.ENCODING_INVALID)
        {
            encoding = AudioFormat.ENCODING_PCM_16BIT;
        }

        if (!AudioTrack.isPcm(encoding)) throw new IllegalArgumentException("Unsupported sample encoding " + encoding);

        int channelMask = format.getChannelMask();
        int channelIndexMask = format.getChannelIndexMask();
        int channelCount;

        if (channelIndexMask != 0)
        {
            channelCount = Integer.bitCount(channelIndexMask);
        }
        else
        {
            if (channelMask == AudioFormat.CHANNEL_INVALID || channelMask == AudioFormat.CHANNEL_IN_DEFAULT)
            {
                channelMask = AudioFormat.CHANNEL_IN_MONO;
            }

            channelCount = Integer.bitCount(channelMask);
        }

        if (channelCount <= 0 || channelCount > 30) throw new IllegalArgumentException("Unsupported channel configuration.");

        int frameSize = channelCount * AudioTrack.bytesPerSample(encoding);
        if (bufferSizeInBytes < 1 || bufferSizeInBytes % frameSize != 0)
        {
            throw new IllegalArgumentException("Invalid audio buffer size " + bufferSizeInBytes);
        }

        mAudioSource = audioSource;
        mSampleRate = sampleRate;
        mChannelMask = channelMask;
        mChannelIndexMask = channelIndexMask;
        mChannelCount = channelCount;
        mEncoding = encoding;
        mFrameSize = frameSize;
        mBufferFrames = bufferSizeInBytes / frameSize;
        mSessionId = AudioTrack.newAudioSessionId();
        mPrivacySensitive = privacySensitive;

        Looper looper = Looper.myLooper();
        mInitializationLooper = looper != null ? looper : Looper.getMainLooper();
        mState = STATE_INITIALIZED;
    }

    private static int inputChannelCount(int channelConfig)
    {
        switch (channelConfig)
        {
            case AudioFormat.CHANNEL_IN_DEFAULT:
            case AudioFormat.CHANNEL_IN_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                return 1;
            case AudioFormat.CHANNEL_IN_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
            case AudioFormat.CHANNEL_IN_FRONT | AudioFormat.CHANNEL_IN_BACK:
                return 2;
            default:
                return 0;
        }
    }

    private static AudioFormat legacyFormat(int sampleRate, int channelConfig, int encoding)
    {
        int channels = inputChannelCount(channelConfig);
        if (channels == 0) throw new IllegalArgumentException("Unsupported channel configuration.");

        int channelMask;
        if (channels == 1)
        {
            channelMask = AudioFormat.CHANNEL_IN_MONO;
        }
        else if (channelConfig == (AudioFormat.CHANNEL_IN_FRONT | AudioFormat.CHANNEL_IN_BACK))
        {
            channelMask = channelConfig;
        }
        else
        {
            channelMask = AudioFormat.CHANNEL_IN_STEREO;
        }

        if (encoding == AudioFormat.ENCODING_DEFAULT) encoding = AudioFormat.ENCODING_PCM_16BIT;
        if (!AudioTrack.isPcm(encoding)) throw new IllegalArgumentException("Unsupported sample encoding " + encoding);
        if (sampleRate < SAMPLE_RATE_MIN || sampleRate > SAMPLE_RATE_MAX)
        {
            throw new IllegalArgumentException(sampleRate + "Hz is not a supported sample rate.");
        }

        return new AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(channelMask).setEncoding(encoding).build();
    }

    @Deprecated
    public void addOnRoutingChangedListener(AudioRecord.OnRoutingChangedListener listener, Handler handler)
    {
        addOnRoutingChangedListener((AudioRouting.OnRoutingChangedListener)listener, handler);
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

    public List<MicrophoneInfo> getActiveMicrophones() throws IOException
    {
        return new ArrayList<>();
    }

    @Override
    public AudioRecordingConfiguration getActiveRecordingConfiguration()
    {
        return null;
    }

    public int getAudioFormat()
    {
        return mEncoding;
    }

    public int getAudioSessionId()
    {
        return mSessionId;
    }

    public int getAudioSource()
    {
        return mAudioSource;
    }

    public int getBufferSizeInFrames()
    {
        return mBufferFrames;
    }

    public int getChannelConfiguration()
    {
        return mChannelMask;
    }

    public int getChannelCount()
    {
        return mChannelCount;
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

    public PersistableBundle getMetrics()
    {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(MetricsConstants.CHANNELS, mChannelCount);
        bundle.putString(MetricsConstants.ENCODING, Integer.toString(mEncoding));
        bundle.putInt(MetricsConstants.LATENCY, (int)(mBufferFrames * 1000L / mSampleRate));
        bundle.putInt(MetricsConstants.SAMPLERATE, mSampleRate);
        bundle.putString(MetricsConstants.SOURCE, Integer.toString(mAudioSource));
        return bundle;
    }

    public static int getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat)
    {
        int channels = inputChannelCount(channelConfig);
        if (channels == 0) return ERROR_BAD_VALUE;
        if (!AudioTrack.isPcm(audioFormat)) return ERROR_BAD_VALUE;
        if (sampleRateInHz < SAMPLE_RATE_MIN || sampleRateInHz > SAMPLE_RATE_MAX) return ERROR_BAD_VALUE;

        return AudioTrack.minimumFrames(sampleRateInHz) * channels * AudioTrack.bytesPerSample(audioFormat);
    }

    public int getNotificationMarkerPosition()
    {
        synchronized (mLock)
        {
            return mMarkerPosition;
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

    public int getRecordingState()
    {
        synchronized (mLock)
        {
            return mRecordingState;
        }
    }

    @Override
    public AudioDeviceInfo getRoutedDevice()
    {
        synchronized (mLock)
        {
            return mPreferredDevice;
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

    public int getState()
    {
        synchronized (mLock)
        {
            return mState;
        }
    }

    public int getTimestamp(AudioTimestamp outTimestamp, int timebase)
    {
        if (outTimestamp == null || (timebase != AudioTimestamp.TIMEBASE_BOOTTIME && timebase != AudioTimestamp.TIMEBASE_MONOTONIC))
        {
            throw new IllegalArgumentException();
        }

        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED || mRecordingState != RECORDSTATE_RECORDING) return ERROR_INVALID_OPERATION;

            long now = System.nanoTime();
            outTimestamp.framePosition = capturedFramesLocked(now);
            outTimestamp.nanoTime = now;
            return SUCCESS;
        }
    }

    public boolean isPrivacySensitive()
    {
        return mPrivacySensitive;
    }

    public int read(byte[] audioData, int offsetInBytes, int sizeInBytes)
    {
        return read(audioData, offsetInBytes, sizeInBytes, READ_BLOCKING);
    }

    public int read(byte[] audioData, int offsetInBytes, int sizeInBytes, int readMode)
    {
        if (mState != STATE_INITIALIZED || mEncoding == AudioFormat.ENCODING_PCM_FLOAT) return ERROR_INVALID_OPERATION;
        if (readMode != READ_BLOCKING && readMode != READ_NON_BLOCKING) return ERROR_BAD_VALUE;
        if (audioData == null || offsetInBytes < 0 || sizeInBytes < 0 || offsetInBytes + sizeInBytes < 0
            || offsetInBytes + sizeInBytes > audioData.length)
        {
            return ERROR_BAD_VALUE;
        }

        int bytes = readFrames(sizeInBytes / mFrameSize, readMode == READ_BLOCKING) * mFrameSize;
        if (bytes < 0) return bytes;

        byte silence = mEncoding == AudioFormat.ENCODING_PCM_8BIT ? (byte)0x80 : 0;
        Arrays.fill(audioData, offsetInBytes, offsetInBytes + bytes, silence);
        return bytes;
    }

    public int read(float[] audioData, int offsetInFloats, int sizeInFloats, int readMode)
    {
        if (mState != STATE_INITIALIZED || mEncoding != AudioFormat.ENCODING_PCM_FLOAT) return ERROR_INVALID_OPERATION;
        if (readMode != READ_BLOCKING && readMode != READ_NON_BLOCKING) return ERROR_BAD_VALUE;
        if (audioData == null || offsetInFloats < 0 || sizeInFloats < 0 || offsetInFloats + sizeInFloats < 0
            || offsetInFloats + sizeInFloats > audioData.length)
        {
            return ERROR_BAD_VALUE;
        }

        int frames = readFrames(sizeInFloats / mChannelCount, readMode == READ_BLOCKING);
        if (frames < 0) return frames;

        int count = frames * mChannelCount;
        Arrays.fill(audioData, offsetInFloats, offsetInFloats + count, 0.0f);
        return count;
    }

    public int read(ByteBuffer audioBuffer, int sizeInBytes)
    {
        return read(audioBuffer, sizeInBytes, READ_BLOCKING);
    }

    public int read(ByteBuffer audioBuffer, int sizeInBytes, int readMode)
    {
        if (mState != STATE_INITIALIZED) return ERROR_INVALID_OPERATION;
        if (readMode != READ_BLOCKING && readMode != READ_NON_BLOCKING) return ERROR_BAD_VALUE;
        if (audioBuffer == null || sizeInBytes < 0) return ERROR_BAD_VALUE;

        int limit = Math.min(sizeInBytes, audioBuffer.capacity());
        int bytes = readFrames(limit / mFrameSize, readMode == READ_BLOCKING) * mFrameSize;
        if (bytes < 0) return bytes;

        byte silence = mEncoding == AudioFormat.ENCODING_PCM_8BIT ? (byte)0x80 : 0;
        for (int index = 0; index < bytes; index++)
        {
            audioBuffer.put(index, silence);
        }

        return bytes;
    }

    public int read(short[] audioData, int offsetInShorts, int sizeInShorts)
    {
        return read(audioData, offsetInShorts, sizeInShorts, READ_BLOCKING);
    }

    public int read(short[] audioData, int offsetInShorts, int sizeInShorts, int readMode)
    {
        if (mState != STATE_INITIALIZED || mEncoding == AudioFormat.ENCODING_PCM_FLOAT) return ERROR_INVALID_OPERATION;
        if (readMode != READ_BLOCKING && readMode != READ_NON_BLOCKING) return ERROR_BAD_VALUE;
        if (audioData == null || offsetInShorts < 0 || sizeInShorts < 0 || offsetInShorts + sizeInShorts < 0
            || offsetInShorts + sizeInShorts > audioData.length)
        {
            return ERROR_BAD_VALUE;
        }

        int bytes = readFrames(sizeInShorts * 2 / mFrameSize, readMode == READ_BLOCKING) * mFrameSize;
        if (bytes < 0) return bytes;

        int count = bytes / 2;
        Arrays.fill(audioData, offsetInShorts, offsetInShorts + count, (short)0);
        return count;
    }

    private int readFrames(int frames, boolean blocking)
    {
        synchronized (mLock)
        {
            int done = 0;

            while (done < frames)
            {
                if (mState != STATE_INITIALIZED) return done > 0 ? done : ERROR_DEAD_OBJECT;
                if (mRecordingState != RECORDSTATE_RECORDING) break;

                long now = System.nanoTime();
                long available = capturedFramesLocked(now) - mFramesConsumed;

                if (available > mBufferFrames)
                {
                    mFramesConsumed += available - mBufferFrames;
                    available = mBufferFrames;
                }

                if (available > 0)
                {
                    long chunk = Math.min(available, frames - done);
                    mFramesConsumed += chunk;
                    done += (int)chunk;
                    continue;
                }

                if (!blocking) break;

                long needed = Math.max(1, Math.min(frames - done, mBufferFrames / 4));
                long waitNanos = needed * NANOS_PER_SECOND / mSampleRate;

                try
                {
                    mLock.wait(Math.max(1, waitNanos / 1000000L), (int)(waitNanos % 1000000L));
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

    private long capturedFramesLocked(long now)
    {
        if (mRecordingState != RECORDSTATE_RECORDING) return mFramesCapturedBase;
        return mFramesCapturedBase + (now - mStartNanos) * mSampleRate / NANOS_PER_SECOND;
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
            if (mRecordingState == RECORDSTATE_RECORDING) mFramesCapturedBase = capturedFramesLocked(System.nanoTime());

            mRecordingState = RECORDSTATE_STOPPED;
            mState = STATE_UNINITIALIZED;
            mPositionListener = null;
            mLock.notifyAll();
        }
    }

    @Override
    protected void finalize()
    {
        release();
    }

    @Deprecated
    public void removeOnRoutingChangedListener(AudioRecord.OnRoutingChangedListener listener)
    {
        removeOnRoutingChangedListener((AudioRouting.OnRoutingChangedListener)listener);
    }

    @Override
    public void removeOnRoutingChangedListener(AudioRouting.OnRoutingChangedListener listener)
    {
        synchronized (mRoutingListeners)
        {
            mRoutingListeners.remove(listener);
        }
    }

    public void setLogSessionId(LogSessionId logSessionId)
    {
        if (logSessionId == null) throw new NullPointerException("logSessionId must not be null");

        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) throw new IllegalStateException("AudioRecord not initialized");
            mLogSessionId = logSessionId;
        }
    }

    public int setNotificationMarkerPosition(int markerInFrames)
    {
        synchronized (mLock)
        {
            if (mState == STATE_UNINITIALIZED) return ERROR_INVALID_OPERATION;

            mMarkerPosition = markerInFrames;
            mMarkerReached = markerInFrames <= 0 || capturedFramesLocked(System.nanoTime()) >= markerInFrames;
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
            long captured = capturedFramesLocked(System.nanoTime());
            mNextPeriodFrame = mPeriodFrames == 0 ? 0 : (captured / mPeriodFrames + 1) * mPeriodFrames;
            mLock.notifyAll();
            return SUCCESS;
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

    public void setRecordPositionUpdateListener(AudioRecord.OnRecordPositionUpdateListener listener)
    {
        setRecordPositionUpdateListener(listener, null);
    }

    public void setRecordPositionUpdateListener(AudioRecord.OnRecordPositionUpdateListener listener, Handler handler)
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

    public void startRecording() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("startRecording() called on an uninitialized AudioRecord.");
            if (mRecordingState == RECORDSTATE_RECORDING) return;

            mStartNanos = System.nanoTime();
            mFramesCapturedBase = 0;
            mFramesConsumed = 0;
            mMarkerReached = mMarkerPosition <= 0;
            mNextPeriodFrame = mPeriodFrames;
            mRecordingState = RECORDSTATE_RECORDING;
            mLock.notifyAll();
            ensureNotifierLocked();
        }
    }

    public void startRecording(MediaSyncEvent syncEvent) throws IllegalStateException
    {
        if (syncEvent == null) throw new IllegalArgumentException("Illegal null MediaSyncEvent");
        startRecording();
    }

    public void stop() throws IllegalStateException
    {
        synchronized (mLock)
        {
            if (mState != STATE_INITIALIZED) throw new IllegalStateException("stop() called on an uninitialized AudioRecord.");
            if (mRecordingState != RECORDSTATE_RECORDING) return;

            mFramesCapturedBase = capturedFramesLocked(System.nanoTime());
            mRecordingState = RECORDSTATE_STOPPED;
            mLock.notifyAll();
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

    private void ensureNotifierLocked()
    {
        if (mNotifierThread != null || mPositionListener == null || mState != STATE_INITIALIZED) return;

        mNotifierThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                runNotifier();
            }
        }, "AudioRecordPosition");
        mNotifierThread.setDaemon(true);
        mNotifierThread.start();
    }

    private void runNotifier()
    {
        synchronized (mLock)
        {
            try
            {
                while (mState == STATE_INITIALIZED && mPositionListener != null)
                {
                    if (mRecordingState != RECORDSTATE_RECORDING)
                    {
                        mLock.wait(100);
                        continue;
                    }

                    long captured = capturedFramesLocked(System.nanoTime());
                    final OnRecordPositionUpdateListener listener = mPositionListener;
                    Handler handler = mPositionHandler;

                    if (mMarkerPosition > 0 && !mMarkerReached && captured >= mMarkerPosition)
                    {
                        mMarkerReached = true;
                        dispatch(handler, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                listener.onMarkerReached(AudioRecord.this);
                            }
                        });
                    }

                    if (mPeriodFrames > 0 && captured >= mNextPeriodFrame)
                    {
                        mNextPeriodFrame = (captured / mPeriodFrames + 1) * mPeriodFrames;
                        dispatch(handler, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                listener.onPeriodicNotification(AudioRecord.this);
                            }
                        });
                    }

                    long target = Long.MAX_VALUE;
                    if (mMarkerPosition > 0 && !mMarkerReached) target = mMarkerPosition;
                    if (mPeriodFrames > 0) target = Math.min(target, mNextPeriodFrame);

                    long waitNanos = target == Long.MAX_VALUE ? 100000000L
                        : Math.max(1, target - captured) * NANOS_PER_SECOND / mSampleRate;
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

    @Deprecated
    public interface OnRoutingChangedListener extends AudioRouting.OnRoutingChangedListener
    {
        void onRoutingChanged(AudioRecord audioRecord);

        @Override
        default void onRoutingChanged(AudioRouting router)
        {
            if (router instanceof AudioRecord) onRoutingChanged((AudioRecord)router);
        }
    }

    public interface OnRecordPositionUpdateListener
    {
        void onMarkerReached(AudioRecord recorder);

        void onPeriodicNotification(AudioRecord recorder);
    }

    public static final class MetricsConstants
    {
        public static final String CHANNELS = "android.media.audiorecord.channels";
        public static final String ENCODING = "android.media.audiorecord.encoding";
        public static final String LATENCY = "android.media.audiorecord.latency";
        public static final String SAMPLERATE = "android.media.audiorecord.samplerate";
        public static final String SOURCE = "android.media.audiorecord.source";

        private MetricsConstants()
        {
        }
    }

    public static class Builder
    {
        private int mAudioSource = MediaRecorder.AudioSource.DEFAULT;
        private AudioFormat mFormat;
        private int mBufferSizeInBytes;
        private boolean mPrivacySensitive;
        private AudioPlaybackCaptureConfiguration mPlaybackCaptureConfiguration;

        public Builder()
        {
        }

        public AudioRecord build() throws UnsupportedOperationException
        {
            AudioFormat format = mFormat;
            if (format == null)
            {
                format = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();
            }

            int source = mPlaybackCaptureConfiguration != null ? MediaRecorder.AudioSource.REMOTE_SUBMIX : mAudioSource;
            int bufferSize = mBufferSizeInBytes;

            if (bufferSize == 0)
            {
                int sampleRate = format.getSampleRate() == AudioFormat.SAMPLE_RATE_UNSPECIFIED
                    ? DEFAULT_SAMPLE_RATE : format.getSampleRate();
                int encoding = format.getEncoding() == AudioFormat.ENCODING_DEFAULT ? AudioFormat.ENCODING_PCM_16BIT : format.getEncoding();
                int channels = format.getChannelCount() > 0 ? format.getChannelCount() : 1;
                bufferSize = AudioTrack.minimumFrames(sampleRate) * channels * AudioTrack.bytesPerSample(encoding);
            }

            try
            {
                return new AudioRecord(source, format, bufferSize, mPrivacySensitive);
            }
            catch (IllegalArgumentException exception)
            {
                throw new UnsupportedOperationException(exception.getMessage());
            }
        }

        public AudioRecord.Builder setAudioFormat(AudioFormat format) throws IllegalArgumentException
        {
            if (format == null) throw new IllegalArgumentException("Illegal null AudioFormat argument");
            mFormat = format;
            return this;
        }

        public AudioRecord.Builder setAudioPlaybackCaptureConfig(AudioPlaybackCaptureConfiguration config)
        {
            if (config == null) throw new NullPointerException("Illegal null AudioPlaybackCaptureConfiguration argument");
            mPlaybackCaptureConfiguration = config;
            return this;
        }

        public AudioRecord.Builder setAudioSource(int source) throws IllegalArgumentException
        {
            if (source < MediaRecorder.AudioSource.DEFAULT || source > AUDIO_SOURCE_MAX)
            {
                throw new IllegalArgumentException("Invalid audio source " + source);
            }

            mAudioSource = source;
            return this;
        }

        public AudioRecord.Builder setBufferSizeInBytes(int bufferSizeInBytes) throws IllegalArgumentException
        {
            if (bufferSizeInBytes <= 0) throw new IllegalArgumentException("Invalid buffer size " + bufferSizeInBytes);
            mBufferSizeInBytes = bufferSizeInBytes;
            return this;
        }

        public AudioRecord.Builder setContext(Context context)
        {
            if (context == null) throw new NullPointerException("context must not be null");
            return this;
        }

        public AudioRecord.Builder setPrivacySensitive(boolean privacySensitive)
        {
            mPrivacySensitive = privacySensitive;
            return this;
        }
    }
}
