package android.media;

import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MediaCodec
{
    public static final int BUFFER_FLAG_CODEC_CONFIG = 2;
    public static final int BUFFER_FLAG_DECODE_ONLY = 32;
    public static final int BUFFER_FLAG_END_OF_STREAM = 4;
    public static final int BUFFER_FLAG_KEY_FRAME = 1;
    public static final int BUFFER_FLAG_PARTIAL_FRAME = 8;
    public static final int BUFFER_FLAG_SYNC_FRAME = 1;
    public static final int CONFIGURE_FLAG_DETACHED_SURFACE = 8;
    public static final int CONFIGURE_FLAG_ENCODE = 1;
    public static final int CONFIGURE_FLAG_USE_BLOCK_MODEL = 2;
    public static final int CONFIGURE_FLAG_USE_CRYPTO_ASYNC = 4;
    public static final int CRYPTO_MODE_AES_CBC = 2;
    public static final int CRYPTO_MODE_AES_CTR = 1;
    public static final int CRYPTO_MODE_UNENCRYPTED = 0;
    public static final int INFO_OUTPUT_BUFFERS_CHANGED = -3;
    public static final int INFO_OUTPUT_FORMAT_CHANGED = -2;
    public static final int INFO_TRY_AGAIN_LATER = -1;
    public static final String PARAMETER_KEY_HDR10_PLUS_INFO = "hdr10-plus-info";
    public static final String PARAMETER_KEY_LOW_LATENCY = "low-latency";
    public static final String PARAMETER_KEY_OFFSET_TIME = "time-offset-us";
    public static final String PARAMETER_KEY_QP_OFFSET_MAP = "qp-offset-map";
    public static final String PARAMETER_KEY_QP_OFFSET_RECTS = "qp-offset-rects";
    public static final String PARAMETER_KEY_REQUEST_SYNC_FRAME = "request-sync";
    public static final String PARAMETER_KEY_SUSPEND = "drop-input-frames";
    public static final String PARAMETER_KEY_SUSPEND_TIME = "drop-start-time-us";
    public static final String PARAMETER_KEY_TUNNEL_PEEK = "tunnel-peek";
    public static final String PARAMETER_KEY_VIDEO_BITRATE = "video-bitrate";
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT = 1;
    public static final int VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2;

    private final String mName;
    private boolean mReleased;

    private MediaCodec(String name)
    {
        mName = name;
    }

    private IllegalStateException unavailable()
    {
        return new IllegalStateException(mReleased ? "codec is released already" : "codec " + mName + " is not available");
    }

    public void configure(MediaFormat format, Surface surface, MediaCrypto crypto, int flags)
    {
        throw unavailable();
    }

    public void configure(MediaFormat format, Surface surface, int flags, MediaDescrambler descrambler)
    {
        throw unavailable();
    }

    public static MediaCodec createByCodecName(String name) throws IOException
    {
        if (name == null) throw new NullPointerException("name must not be null");

        for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos())
        {
            if (info.getName().equals(name)) throw new IOException("Failed to initialize " + name);
        }

        throw new IllegalArgumentException("Failed to initialize " + name + ", error 0xffffffff (NAME_NOT_FOUND)");
    }

    public static MediaCodec createDecoderByType(String type) throws IOException
    {
        return createByType(type, false);
    }

    public static MediaCodec createEncoderByType(String type) throws IOException
    {
        return createByType(type, true);
    }

    private static MediaCodec createByType(String type, boolean encoder) throws IOException
    {
        if (type == null) throw new NullPointerException("type must not be null");

        throw new IOException("Failed to initialize " + (encoder ? "encoder" : "decoder") + " for type " + type
            + ": no codec is available on this device");
    }

    public Surface createInputSurface()
    {
        throw unavailable();
    }

    public static Surface createPersistentInputSurface()
    {
        throw new IllegalStateException("persistent input surfaces are not supported without a codec");
    }

    public int dequeueInputBuffer(long timeoutUs)
    {
        throw unavailable();
    }

    public int dequeueOutputBuffer(MediaCodec.BufferInfo info, long timeoutUs)
    {
        throw unavailable();
    }

    public void detachOutputSurface()
    {
        throw unavailable();
    }

    public void flush()
    {
        throw unavailable();
    }

    public String getCanonicalName()
    {
        return mName;
    }

    public MediaCodecInfo getCodecInfo()
    {
        throw unavailable();
    }

    public ByteBuffer getInputBuffer(int index)
    {
        throw unavailable();
    }

    public ByteBuffer[] getInputBuffers()
    {
        throw unavailable();
    }

    public MediaFormat getInputFormat()
    {
        throw unavailable();
    }

    public Image getInputImage(int index)
    {
        throw unavailable();
    }

    public PersistableBundle getMetrics()
    {
        return new PersistableBundle();
    }

    public String getName()
    {
        return mName;
    }

    public ByteBuffer getOutputBuffer(int index)
    {
        throw unavailable();
    }

    public ByteBuffer[] getOutputBuffers()
    {
        throw unavailable();
    }

    public MediaFormat getOutputFormat()
    {
        throw unavailable();
    }

    public MediaFormat getOutputFormat(int index)
    {
        throw unavailable();
    }

    public MediaCodec.OutputFrame getOutputFrame(int index)
    {
        throw unavailable();
    }

    public Image getOutputImage(int index)
    {
        throw unavailable();
    }

    public MediaCodec.ParameterDescriptor getParameterDescriptor(String name)
    {
        return null;
    }

    public MediaCodec.QueueRequest getQueueRequest(int index)
    {
        throw unavailable();
    }

    public List<String> getSupportedVendorParameters()
    {
        return Collections.emptyList();
    }

    public void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags) throws MediaCodec.CryptoException
    {
        throw unavailable();
    }

    public void queueInputBuffers(int index, ArrayDeque<MediaCodec.BufferInfo> bufferInfos)
    {
        throw unavailable();
    }

    public void queueSecureInputBuffer(int index, int offset, MediaCodec.CryptoInfo info, long presentationTimeUs, int flags)
        throws MediaCodec.CryptoException
    {
        throw unavailable();
    }

    public void queueSecureInputBuffers(int index, ArrayDeque<MediaCodec.BufferInfo> bufferInfos,
        ArrayDeque<MediaCodec.CryptoInfo> cryptoInfos)
    {
        throw unavailable();
    }

    public void release()
    {
        mReleased = true;
    }

    public void releaseOutputBuffer(int index, boolean render)
    {
        throw unavailable();
    }

    public void releaseOutputBuffer(int index, long renderTimestampNs)
    {
        throw unavailable();
    }

    public void reset()
    {
        throw unavailable();
    }

    public void setAudioPresentation(AudioPresentation presentation)
    {
        if (presentation == null) throw new NullPointerException("audio presentation is null");
    }

    public void setCallback(MediaCodec.Callback callback)
    {
        setCallback(callback, null);
    }

    public void setCallback(MediaCodec.Callback callback, Handler handler)
    {
        throw unavailable();
    }

    public void setInputSurface(Surface surface)
    {
        throw unavailable();
    }

    public void setOnFirstTunnelFrameReadyListener(Handler handler, MediaCodec.OnFirstTunnelFrameReadyListener listener)
    {
        throw unavailable();
    }

    public void setOnFrameRenderedListener(MediaCodec.OnFrameRenderedListener listener, Handler handler)
    {
        throw unavailable();
    }

    public void setOutputSurface(Surface surface)
    {
        throw unavailable();
    }

    public void setParameters(Bundle params)
    {
        throw unavailable();
    }

    public void setVideoScalingMode(int mode)
    {
        throw unavailable();
    }

    public void signalEndOfInputStream()
    {
        throw unavailable();
    }

    public void start()
    {
        throw unavailable();
    }

    public void stop()
    {
        throw unavailable();
    }

    public void subscribeToVendorParameters(List<String> names)
    {
        throw unavailable();
    }

    public void unsubscribeFromVendorParameters(List<String> names)
    {
        throw unavailable();
    }

    public final class QueueRequest
    {
        QueueRequest()
        {
        }

        public void queue()
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setByteBufferParameter(String key, ByteBuffer value)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setEncryptedLinearBlock(MediaCodec.LinearBlock block, int offset, int size,
            MediaCodec.CryptoInfo cryptoInfo)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setFlags(int flags)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setFloatParameter(String key, float value)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setIntegerParameter(String key, int value)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setLinearBlock(MediaCodec.LinearBlock block, int offset, int size)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setLongParameter(String key, long value)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setMultiFrameEncryptedLinearBlock(MediaCodec.LinearBlock block,
            ArrayDeque<MediaCodec.BufferInfo> bufferInfos, ArrayDeque<MediaCodec.CryptoInfo> cryptoInfos)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setMultiFrameLinearBlock(MediaCodec.LinearBlock block, ArrayDeque<MediaCodec.BufferInfo> bufferInfos)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setPresentationTimeUs(long presentationTimeUs)
        {
            throw unavailable();
        }

        public MediaCodec.QueueRequest setStringParameter(String key, String value)
        {
            throw unavailable();
        }
    }

    public static class ParameterDescriptor
    {
        private final String mName;
        private final int mType;

        ParameterDescriptor(String name, int type)
        {
            mName = name;
            mType = type;
        }

        @Override
        public boolean equals(Object object)
        {
            if (!(object instanceof ParameterDescriptor)) return false;

            ParameterDescriptor other = (ParameterDescriptor)object;
            return mName.equals(other.mName) && mType == other.mType;
        }

        public String getName()
        {
            return mName;
        }

        public int getType()
        {
            return mType;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mName, mType);
        }
    }

    public static final class OutputFrame
    {
        private final ArrayDeque<BufferInfo> mBufferInfos = new ArrayDeque<>();
        private final Set<String> mChangedKeys = new HashSet<>();
        private int mFlags;
        private MediaFormat mFormat;
        private LinearBlock mLinearBlock;
        private long mPresentationTimeUs;

        OutputFrame()
        {
        }

        public ArrayDeque<MediaCodec.BufferInfo> getBufferInfos()
        {
            return mBufferInfos;
        }

        public Set<String> getChangedKeys()
        {
            return Collections.unmodifiableSet(mChangedKeys);
        }

        public int getFlags()
        {
            return mFlags;
        }

        public MediaFormat getFormat()
        {
            return mFormat;
        }

        public MediaCodec.LinearBlock getLinearBlock()
        {
            return mLinearBlock;
        }

        public long getPresentationTimeUs()
        {
            return mPresentationTimeUs;
        }
    }

    public interface OnFrameRenderedListener
    {
        void onFrameRendered(MediaCodec codec, long presentationTimeUs, long nanoTime);
    }

    public interface OnFirstTunnelFrameReadyListener
    {
        void onFirstTunnelFrameReady(MediaCodec codec);
    }

    public static final class MetricsConstants
    {
        public static final String CODEC = "android.media.mediacodec.codec";
        public static final String ENCODER = "android.media.mediacodec.encoder";
        public static final String HEIGHT = "android.media.mediacodec.height";
        public static final String MIME_TYPE = "android.media.mediacodec.mime";
        public static final String MODE = "android.media.mediacodec.mode";
        public static final String MODE_AUDIO = "audio";
        public static final String MODE_VIDEO = "video";
        public static final String ROTATION = "android.media.mediacodec.rotation";
        public static final String SECURE = "android.media.mediacodec.secure";
        public static final String WIDTH = "android.media.mediacodec.width";

        MetricsConstants()
        {
        }
    }

    public static final class LinearBlock
    {
        private ByteBuffer mBuffer;

        LinearBlock(int capacity)
        {
            mBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        }

        public static boolean isCodecCopyFreeCompatible(String[] codecNames)
        {
            for (String name : codecNames)
            {
                boolean known = false;

                for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos())
                {
                    if (info.getName().equals(name)) known = true;
                }

                if (!known) return false;
            }

            return true;
        }

        public boolean isMappable()
        {
            return mBuffer != null;
        }

        public ByteBuffer map()
        {
            if (mBuffer == null) throw new IllegalStateException("The linear block is not mappable");

            return mBuffer;
        }

        public static MediaCodec.LinearBlock obtain(int capacity, String[] codecNames)
        {
            if (!isCodecCopyFreeCompatible(codecNames)) return null;

            return new LinearBlock(capacity);
        }

        public void recycle()
        {
            if (mBuffer == null) throw new IllegalStateException("The linear block is recycled");

            mBuffer = null;
        }
    }

    public class InvalidBufferFlagsException extends RuntimeException
    {
        InvalidBufferFlagsException(String message)
        {
            super(message);
        }
    }

    public class IncompatibleWithBlockModelException extends RuntimeException
    {
        IncompatibleWithBlockModelException(String message)
        {
            super(message);
        }
    }

    public static final class CryptoInfo
    {
        public byte[] iv;
        public byte[] key;
        public int mode;
        public int[] numBytesOfClearData;
        public int[] numBytesOfEncryptedData;
        public int numSubSamples;

        private Pattern mPattern = new Pattern(0, 0);

        public CryptoInfo()
        {
        }

        public MediaCodec.CryptoInfo.Pattern getPattern()
        {
            return new Pattern(mPattern.getEncryptBlocks(), mPattern.getSkipBlocks());
        }

        public void set(int newNumSubSamples, int[] newNumBytesOfClearData, int[] newNumBytesOfEncryptedData, byte[] newKey,
            byte[] newIv, int newMode)
        {
            numSubSamples = newNumSubSamples;
            numBytesOfClearData = newNumBytesOfClearData;
            numBytesOfEncryptedData = newNumBytesOfEncryptedData;
            key = newKey;
            iv = newIv;
            mode = newMode;
            mPattern = new Pattern(0, 0);
        }

        public void setPattern(MediaCodec.CryptoInfo.Pattern newPattern)
        {
            mPattern = newPattern == null ? new Pattern(0, 0) : newPattern;
        }

        private static String hex(byte[] bytes)
        {
            if (bytes == null) return "null";

            StringBuilder builder = new StringBuilder();

            for (byte value : bytes)
            {
                builder.append(String.format("%02x", value & 0xff));
            }

            return builder.toString();
        }

        @Override
        public String toString()
        {
            return numSubSamples + " subsamples, key [" + hex(key) + "], iv [" + hex(iv) + "], mode " + mode + ", pattern (encrypt: "
                + mPattern.getEncryptBlocks() + ", skip: " + mPattern.getSkipBlocks() + "), clearData "
                + Arrays.toString(numBytesOfClearData) + ", encryptedData " + Arrays.toString(numBytesOfEncryptedData);
        }

        public static final class Pattern
        {
            private int mEncryptBlocks;
            private int mSkipBlocks;

            public Pattern(int blocksToEncrypt, int blocksToSkip)
            {
                set(blocksToEncrypt, blocksToSkip);
            }

            public int getEncryptBlocks()
            {
                return mEncryptBlocks;
            }

            public int getSkipBlocks()
            {
                return mSkipBlocks;
            }

            public void set(int blocksToEncrypt, int blocksToSkip)
            {
                mEncryptBlocks = blocksToEncrypt;
                mSkipBlocks = blocksToSkip;
            }
        }
    }

    public static final class CryptoException extends RuntimeException implements MediaDrmThrowable
    {
        public static final int ERROR_FRAME_TOO_LARGE = 8;
        public static final int ERROR_INSUFFICIENT_OUTPUT_PROTECTION = 4;
        public static final int ERROR_INSUFFICIENT_SECURITY = 7;
        public static final int ERROR_KEY_EXPIRED = 2;
        public static final int ERROR_LOST_STATE = 9;
        public static final int ERROR_NO_KEY = 1;
        public static final int ERROR_RESOURCE_BUSY = 3;
        public static final int ERROR_SESSION_NOT_OPENED = 5;
        public static final int ERROR_UNSUPPORTED_OPERATION = 6;

        private final int mErrorCode;

        public CryptoException(int errorCode, String detailMessage)
        {
            super(detailMessage);
            mErrorCode = errorCode;
        }

        public MediaCodec.CryptoInfo getCryptoInfo()
        {
            return null;
        }

        public int getErrorCode()
        {
            return mErrorCode;
        }

        public int getErrorContext()
        {
            return 0;
        }

        public int getOemError()
        {
            return 0;
        }

        public int getVendorError()
        {
            return 0;
        }
    }

    public static final class CodecException extends IllegalStateException
    {
        public static final int ERROR_INSUFFICIENT_RESOURCE = 1100;
        public static final int ERROR_RECLAIMED = 1101;

        private static final int ACTION_TRANSIENT = 1;
        private static final int ACTION_RECOVERABLE = 2;

        private final int mErrorCode;
        private final int mActionCode;
        private final String mDiagnosticInfo;

        CodecException(int errorCode, int actionCode, String detailMessage)
        {
            super(detailMessage);
            mErrorCode = errorCode;
            mActionCode = actionCode;
            mDiagnosticInfo = "android.media.MediaCodec.error_" + (errorCode < 0 ? "neg_" : "") + Math.abs(errorCode);
        }

        public String getDiagnosticInfo()
        {
            return mDiagnosticInfo;
        }

        public int getErrorCode()
        {
            return mErrorCode;
        }

        public boolean isRecoverable()
        {
            return (mActionCode & ACTION_RECOVERABLE) != 0;
        }

        public boolean isTransient()
        {
            return (mActionCode & ACTION_TRANSIENT) != 0;
        }
    }

    public static abstract class Callback
    {
        public Callback()
        {
        }

        public void onCryptoError(MediaCodec codec, MediaCodec.CryptoException exception)
        {
            throw new IllegalStateException("Client must override onCryptoError when the codec is configured with "
                + "CONFIGURE_FLAG_USE_CRYPTO_ASYNC.", exception);
        }

        public abstract void onError(MediaCodec codec, MediaCodec.CodecException exception);

        public abstract void onInputBufferAvailable(MediaCodec codec, int index);

        public void onMetricsFlushed(MediaCodec codec, PersistableBundle metrics)
        {
        }

        public abstract void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info);

        public void onOutputBuffersAvailable(MediaCodec codec, int index, ArrayDeque<MediaCodec.BufferInfo> infos)
        {
            throw new IllegalStateException("Client must override onOutputBuffersAvailable when codec is configured to "
                + "operate with multiple access units");
        }

        public abstract void onOutputFormatChanged(MediaCodec codec, MediaFormat format);
    }

    public static final class BufferInfo
    {
        public int flags;
        public int offset;
        public long presentationTimeUs;
        public int size;

        public BufferInfo()
        {
        }

        public void set(int newOffset, int newSize, long newTimeUs, int newFlags)
        {
            offset = newOffset;
            size = newSize;
            presentationTimeUs = newTimeUs;
            flags = newFlags;
        }
    }
}
