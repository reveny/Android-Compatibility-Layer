package android.media;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class MediaMuxer
{
    private static final int MUXER_STATE_INITIALIZED = 0;
    private static final int MUXER_STATE_STARTED = 1;
    private static final int MUXER_STATE_STOPPED = 2;
    private static final int MUXER_STATE_RELEASED = 3;

    private final int mFormat;
    private final List<MediaFormat> mTracks = new ArrayList<>();
    private RandomAccessFile mFile;
    private int mState = MUXER_STATE_INITIALIZED;
    private int mOrientationHint;
    private float mLatitude;
    private float mLongitude;

    public MediaMuxer(FileDescriptor fd, int format) throws IOException
    {
        if (fd == null) throw new IllegalArgumentException("fd must not be null");
        if (!fd.valid()) throw new IOException("Muxer output file descriptor is not valid");

        checkFormat(format);
        mFormat = format;
    }

    public MediaMuxer(String path, int format) throws IOException
    {
        if (path == null) throw new IllegalArgumentException("path must not be null");

        checkFormat(format);
        mFormat = format;
        mFile = new RandomAccessFile(path, "rw");
        mFile.setLength(0);
    }

    private static void checkFormat(int format)
    {
        if (format < OutputFormat.MUXER_OUTPUT_MPEG_4 || format > OutputFormat.MUXER_OUTPUT_OGG)
        {
            throw new IllegalArgumentException("format: " + format + " is invalid");
        }
    }

    public int addTrack(MediaFormat format)
    {
        if (format == null) throw new IllegalArgumentException("format must not be null.");
        if (mState != MUXER_STATE_INITIALIZED) throw new IllegalStateException("Muxer is not initialized.");

        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime == null) throw new IllegalArgumentException("Track format must contain a mime type");

        mTracks.add(new MediaFormat(format));
        return mTracks.size() - 1;
    }

    public void release()
    {
        if (mFile != null)
        {
            try
            {
                mFile.close();
            }
            catch (IOException ignored)
            {
            }

            mFile = null;
        }

        mState = MUXER_STATE_RELEASED;
    }

    public void setLocation(float latitude, float longitude)
    {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
        {
            throw new IllegalArgumentException("Location is out of range");
        }

        if (mState != MUXER_STATE_INITIALIZED) throw new IllegalStateException("Can't set location due to wrong state.");

        mLatitude = latitude;
        mLongitude = longitude;
    }

    public void setOrientationHint(int degrees)
    {
        if (degrees != 0 && degrees != 90 && degrees != 180 && degrees != 270)
        {
            throw new IllegalArgumentException("Unsupported angle: " + degrees);
        }

        if (mState != MUXER_STATE_INITIALIZED) throw new IllegalStateException("Can't set rotation degrees due to wrong state.");

        mOrientationHint = degrees;
    }

    public void start()
    {
        if (mState != MUXER_STATE_INITIALIZED) throw new IllegalStateException("Can't start due to wrong state.");
        if (mTracks.isEmpty()) throw new IllegalStateException("Failed to start the muxer: no tracks were added");

        throw new IllegalStateException("Failed to start the muxer: no container writer is available for output format " + mFormat);
    }

    public void stop()
    {
        if (mState != MUXER_STATE_STARTED) throw new IllegalStateException("Can't stop due to wrong state.");

        mState = MUXER_STATE_STOPPED;
    }

    public void writeSampleData(int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo)
    {
        if (trackIndex < 0 || trackIndex >= mTracks.size()) throw new IllegalArgumentException("trackIndex is invalid");
        if (byteBuf == null) throw new IllegalArgumentException("byteBuffer must not be null");
        if (bufferInfo == null) throw new IllegalArgumentException("bufferInfo must not be null");
        if (bufferInfo.size < 0 || bufferInfo.offset < 0 || bufferInfo.offset + bufferInfo.size > byteBuf.capacity())
        {
            throw new IllegalArgumentException("bufferInfo must specify a valid buffer offset and size");
        }

        if (mState != MUXER_STATE_STARTED) throw new IllegalStateException("Can't write, muxer is not started");
    }

    public static final class OutputFormat
    {
        public static final int MUXER_OUTPUT_3GPP = 2;
        public static final int MUXER_OUTPUT_HEIF = 3;
        public static final int MUXER_OUTPUT_MPEG_4 = 0;
        public static final int MUXER_OUTPUT_OGG = 4;
        public static final int MUXER_OUTPUT_WEBM = 1;

        OutputFormat()
        {
        }
    }
}
