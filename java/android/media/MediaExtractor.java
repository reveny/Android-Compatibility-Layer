package android.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.metrics.LogSessionId;
import android.net.Uri;
import android.os.PersistableBundle;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MediaExtractor
{
    public static final int SAMPLE_FLAG_ENCRYPTED = 2;
    public static final int SAMPLE_FLAG_PARTIAL_FRAME = 4;
    public static final int SAMPLE_FLAG_SYNC = 1;
    public static final int SEEK_TO_CLOSEST_SYNC = 2;
    public static final int SEEK_TO_NEXT_SYNC = 1;
    public static final int SEEK_TO_PREVIOUS_SYNC = 0;

    private final List<MediaFormat> mTracks = new ArrayList<>();
    private LogSessionId mLogSessionId = LogSessionId.LOG_SESSION_ID_NONE;
    private boolean mReleased;

    public MediaExtractor()
    {
    }

    private void checkNotReleased()
    {
        if (mReleased) throw new IllegalStateException("MediaExtractor is released");
    }

    private void checkTrack(int index)
    {
        checkNotReleased();

        if (index < 0 || index >= mTracks.size()) throw new IllegalArgumentException("track index " + index + " is out of range");
    }

    private IOException noExtractor()
    {
        checkNotReleased();
        return new IOException("Failed to instantiate extractor.");
    }

    public boolean advance()
    {
        checkNotReleased();
        return false;
    }

    public List<AudioPresentation> getAudioPresentations(int trackIndex)
    {
        checkTrack(trackIndex);
        return new ArrayList<>();
    }

    public long getCachedDuration()
    {
        checkNotReleased();
        return -1;
    }

    public MediaExtractor.CasInfo getCasInfo(int index)
    {
        checkTrack(index);
        return null;
    }

    public DrmInitData getDrmInitData()
    {
        checkNotReleased();
        return null;
    }

    public LogSessionId getLogSessionId()
    {
        return mLogSessionId;
    }

    public PersistableBundle getMetrics()
    {
        PersistableBundle metrics = new PersistableBundle();
        metrics.putInt(MetricsConstants.TRACKS, mTracks.size());
        return metrics;
    }

    public Map<UUID, byte[]> getPsshInfo()
    {
        checkNotReleased();
        return null;
    }

    public boolean getSampleCryptoInfo(MediaCodec.CryptoInfo info)
    {
        checkNotReleased();
        return false;
    }

    public int getSampleFlags()
    {
        checkNotReleased();
        return -1;
    }

    public long getSampleSize()
    {
        checkNotReleased();
        return -1;
    }

    public long getSampleTime()
    {
        checkNotReleased();
        return -1;
    }

    public int getSampleTrackIndex()
    {
        checkNotReleased();
        return -1;
    }

    public int getTrackCount()
    {
        checkNotReleased();
        return mTracks.size();
    }

    public MediaFormat getTrackFormat(int index)
    {
        checkTrack(index);
        return new MediaFormat(mTracks.get(index));
    }

    public boolean hasCacheReachedEndOfStream()
    {
        checkNotReleased();
        return true;
    }

    public int readSampleData(ByteBuffer byteBuf, int offset)
    {
        checkNotReleased();
        return -1;
    }

    public void release()
    {
        mReleased = true;
        mTracks.clear();
    }

    public void seekTo(long timeUs, int mode)
    {
        checkNotReleased();
    }

    public void selectTrack(int index)
    {
        checkTrack(index);
    }

    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException
    {
        if (context == null || uri == null) throw new IllegalArgumentException("context and uri must not be null");

        throw noExtractor();
    }

    public void setDataSource(AssetFileDescriptor afd) throws IOException, IllegalArgumentException, IllegalStateException
    {
        if (afd == null) throw new IllegalArgumentException("afd must not be null");

        throw noExtractor();
    }

    public void setDataSource(MediaDataSource dataSource) throws IOException
    {
        if (dataSource == null) throw new IllegalArgumentException("dataSource must not be null");

        throw noExtractor();
    }

    public void setDataSource(FileDescriptor fd) throws IOException
    {
        setDataSource(fd, 0, 0x7ffffffffffffffL);
    }

    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException
    {
        if (fd == null || !fd.valid()) throw new IllegalArgumentException("invalid file descriptor");

        throw noExtractor();
    }

    public void setDataSource(String path) throws IOException
    {
        setDataSource(path, (Map<String, String>)null);
    }

    public void setDataSource(String path, Map<String, String> headers) throws IOException
    {
        if (path == null) throw new IllegalArgumentException("path must not be null");

        throw noExtractor();
    }

    public void setLogSessionId(LogSessionId logSessionId)
    {
        if (logSessionId == null) throw new NullPointerException("logSessionId must not be null");

        mLogSessionId = logSessionId;
    }

    public void setMediaCas(MediaCas mediaCas)
    {
        checkNotReleased();
    }

    public void unselectTrack(int index)
    {
        checkTrack(index);
    }

    public static final class MetricsConstants
    {
        public static final String FORMAT = "android.media.mediaextractor.fmt";
        public static final String MIME_TYPE = "android.media.mediaextractor.mime";
        public static final String TRACKS = "android.media.mediaextractor.ntrk";

        MetricsConstants()
        {
        }
    }

    public static final class CasInfo
    {
        private final int mSystemId;
        private final MediaCas.Session mSession;
        private final byte[] mPrivateData;

        CasInfo(int systemId, MediaCas.Session session, byte[] privateData)
        {
            mSystemId = systemId;
            mSession = session;
            mPrivateData = privateData;
        }

        public byte[] getPrivateData()
        {
            return mPrivateData;
        }

        public MediaCas.Session getSession()
        {
            return mSession;
        }

        public int getSystemId()
        {
            return mSystemId;
        }
    }
}
