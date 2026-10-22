package android.media;

import android.media.metrics.LogSessionId;
import android.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MediaParser
{
    public static final String PARAMETER_ADTS_ENABLE_CBR_SEEKING = "android.media.mediaparser.adts.enableCbrSeeking";
    public static final String PARAMETER_AMR_ENABLE_CBR_SEEKING = "android.media.mediaparser.amr.enableCbrSeeking";
    public static final String PARAMETER_FLAC_DISABLE_ID3 = "android.media.mediaparser.flac.disableId3";
    public static final String PARAMETER_MATROSKA_DISABLE_CUES_SEEKING = "android.media.mediaparser.matroska.disableCuesSeeking";
    public static final String PARAMETER_MP3_DISABLE_ID3 = "android.media.mediaparser.mp3.disableId3";
    public static final String PARAMETER_MP3_ENABLE_CBR_SEEKING = "android.media.mediaparser.mp3.enableCbrSeeking";
    public static final String PARAMETER_MP3_ENABLE_INDEX_SEEKING = "android.media.mediaparser.mp3.enableIndexSeeking";
    public static final String PARAMETER_MP4_IGNORE_EDIT_LISTS = "android.media.mediaparser.mp4.ignoreEditLists";
    public static final String PARAMETER_MP4_IGNORE_TFDT_BOX = "android.media.mediaparser.mp4.ignoreTfdtBox";
    public static final String PARAMETER_MP4_TREAT_VIDEO_FRAMES_AS_KEYFRAMES = "android.media.mediaparser.mp4.treatVideoFramesAsKeyframes";
    public static final String PARAMETER_TS_ALLOW_NON_IDR_AVC_KEYFRAMES = "android.media.mediaparser.ts.allowNonIdrAvcKeyframes";
    public static final String PARAMETER_TS_DETECT_ACCESS_UNITS = "android.media.mediaparser.ts.ignoreDetectAccessUnits";
    public static final String PARAMETER_TS_ENABLE_HDMV_DTS_AUDIO_STREAMS = "android.media.mediaparser.ts.enableHdmvDtsAudioStreams";
    public static final String PARAMETER_TS_IGNORE_AAC_STREAM = "android.media.mediaparser.ts.ignoreAacStream";
    public static final String PARAMETER_TS_IGNORE_AVC_STREAM = "android.media.mediaparser.ts.ignoreAvcStream";
    public static final String PARAMETER_TS_IGNORE_SPLICE_INFO_STREAM = "android.media.mediaparser.ts.ignoreSpliceInfoStream";
    public static final String PARAMETER_TS_MODE = "android.media.mediaparser.ts.mode";
    public static final String PARSER_NAME_AC3 = "android.media.mediaparser.Ac3Parser";
    public static final String PARSER_NAME_AC4 = "android.media.mediaparser.Ac4Parser";
    public static final String PARSER_NAME_ADTS = "android.media.mediaparser.AdtsParser";
    public static final String PARSER_NAME_AMR = "android.media.mediaparser.AmrParser";
    public static final String PARSER_NAME_FLAC = "android.media.mediaparser.FlacParser";
    public static final String PARSER_NAME_FLV = "android.media.mediaparser.FlvParser";
    public static final String PARSER_NAME_FMP4 = "android.media.mediaparser.FragmentedMp4Parser";
    public static final String PARSER_NAME_MATROSKA = "android.media.mediaparser.MatroskaParser";
    public static final String PARSER_NAME_MP3 = "android.media.mediaparser.Mp3Parser";
    public static final String PARSER_NAME_MP4 = "android.media.mediaparser.Mp4Parser";
    public static final String PARSER_NAME_OGG = "android.media.mediaparser.OggParser";
    public static final String PARSER_NAME_PS = "android.media.mediaparser.PsParser";
    public static final String PARSER_NAME_TS = "android.media.mediaparser.TsParser";
    public static final String PARSER_NAME_UNKNOWN = "android.media.mediaparser.UNKNOWN";
    public static final String PARSER_NAME_WAV = "android.media.mediaparser.WavParser";
    public static final int SAMPLE_FLAG_DECODE_ONLY = -2147483648;
    public static final int SAMPLE_FLAG_ENCRYPTED = 1073741824;
    public static final int SAMPLE_FLAG_HAS_SUPPLEMENTAL_DATA = 268435456;
    public static final int SAMPLE_FLAG_KEY_FRAME = 1;
    public static final int SAMPLE_FLAG_LAST_SAMPLE = 536870912;

    private static final List<String> KNOWN_PARSER_NAMES = Collections.unmodifiableList(Arrays.asList(PARSER_NAME_MATROSKA,
        PARSER_NAME_FMP4, PARSER_NAME_MP4, PARSER_NAME_MP3, PARSER_NAME_ADTS, PARSER_NAME_AC3, PARSER_NAME_TS, PARSER_NAME_FLV,
        PARSER_NAME_OGG, PARSER_NAME_PS, PARSER_NAME_WAV, PARSER_NAME_AMR, PARSER_NAME_AC4, PARSER_NAME_FLAC));

    private static final List<String> KNOWN_PARAMETERS = Collections.unmodifiableList(Arrays.asList(
        PARAMETER_ADTS_ENABLE_CBR_SEEKING, PARAMETER_AMR_ENABLE_CBR_SEEKING, PARAMETER_FLAC_DISABLE_ID3,
        PARAMETER_MATROSKA_DISABLE_CUES_SEEKING, PARAMETER_MP3_DISABLE_ID3, PARAMETER_MP3_ENABLE_CBR_SEEKING,
        PARAMETER_MP3_ENABLE_INDEX_SEEKING, PARAMETER_MP4_IGNORE_EDIT_LISTS, PARAMETER_MP4_IGNORE_TFDT_BOX,
        PARAMETER_MP4_TREAT_VIDEO_FRAMES_AS_KEYFRAMES, PARAMETER_TS_ALLOW_NON_IDR_AVC_KEYFRAMES, PARAMETER_TS_DETECT_ACCESS_UNITS,
        PARAMETER_TS_ENABLE_HDMV_DTS_AUDIO_STREAMS, PARAMETER_TS_IGNORE_AAC_STREAM, PARAMETER_TS_IGNORE_AVC_STREAM,
        PARAMETER_TS_IGNORE_SPLICE_INFO_STREAM, PARAMETER_TS_MODE));

    private final OutputConsumer mOutputConsumer;
    private final String[] mParserNamesPool;
    private final Map<String, Object> mParameters = new HashMap<>();
    private LogSessionId mLogSessionId = LogSessionId.LOG_SESSION_ID_NONE;
    private boolean mReleased;

    private MediaParser(OutputConsumer outputConsumer, String[] parserNamesPool)
    {
        mOutputConsumer = outputConsumer;
        mParserNamesPool = parserNamesPool;
    }

    public boolean advance(MediaParser.SeekableInputReader seekableInputReader) throws IOException
    {
        if (seekableInputReader == null) throw new NullPointerException("seekableInputReader must not be null");
        if (mReleased) throw new IllegalStateException("MediaParser is released");

        if (seekableInputReader.getLength() == 0) return false;

        throw new UnrecognizedInputFormatException("None of the available parsers ( " + String.join(", ", mParserNamesPool)
            + ") could read the stream.");
    }

    public static MediaParser create(MediaParser.OutputConsumer outputConsumer, String... parserNames)
    {
        if (outputConsumer == null) throw new NullPointerException("outputConsumer must not be null");

        for (String name : parserNames)
        {
            if (!KNOWN_PARSER_NAMES.contains(name)) throw new IllegalArgumentException("Invalid parser name: " + name);
        }

        String[] pool = parserNames.length == 0 ? getParserNames(new MediaFormat()).toArray(new String[0]) : parserNames.clone();
        return new MediaParser(outputConsumer, pool);
    }

    public static MediaParser createByName(String name, MediaParser.OutputConsumer outputConsumer)
    {
        if (outputConsumer == null) throw new NullPointerException("outputConsumer must not be null");

        throw new IllegalArgumentException("Parser " + name + " is not available on this device");
    }

    public LogSessionId getLogSessionId()
    {
        return mLogSessionId;
    }

    public String getParserName()
    {
        return PARSER_NAME_UNKNOWN;
    }

    public static List<String> getParserNames(MediaFormat mediaFormat)
    {
        return new ArrayList<>();
    }

    public void release()
    {
        mReleased = true;
    }

    public void seek(MediaParser.SeekPoint seekPoint)
    {
        if (seekPoint == null) throw new NullPointerException("seekPoint must not be null");
    }

    public void setLogSessionId(LogSessionId logSessionId)
    {
        if (logSessionId == null) throw new NullPointerException("logSessionId must not be null");

        mLogSessionId = logSessionId;
    }

    public MediaParser setParameter(String parameterName, Object value)
    {
        if (!KNOWN_PARAMETERS.contains(parameterName)) return this;

        boolean stringParameter = PARAMETER_TS_MODE.equals(parameterName);
        if (stringParameter && !(value instanceof String)) throw new IllegalArgumentException(parameterName + " expects a String");
        if (!stringParameter && !(value instanceof Boolean)) throw new IllegalArgumentException(parameterName + " expects a Boolean");

        mParameters.put(parameterName, value);
        return this;
    }

    public boolean supportsParameter(String parameterName)
    {
        return KNOWN_PARAMETERS.contains(parameterName);
    }

    public static final class UnrecognizedInputFormatException extends IOException
    {
        UnrecognizedInputFormatException(String message)
        {
            super(message);
        }
    }

    public static final class TrackData
    {
        public final DrmInitData drmInitData;
        public final MediaFormat mediaFormat;

        TrackData(MediaFormat mediaFormat, DrmInitData drmInitData)
        {
            this.mediaFormat = mediaFormat;
            this.drmInitData = drmInitData;
        }
    }

    public interface SeekableInputReader extends MediaParser.InputReader
    {
        void seekToPosition(long position);
    }

    public static final class SeekPoint
    {
        public static final MediaParser.SeekPoint START = new SeekPoint(0, 0);
        public final long position;
        public final long timeMicros;

        SeekPoint(long timeMicros, long position)
        {
            this.timeMicros = timeMicros;
            this.position = position;
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) return true;
            if (!(object instanceof SeekPoint)) return false;

            SeekPoint other = (SeekPoint)object;
            return timeMicros == other.timeMicros && position == other.position;
        }

        @Override
        public int hashCode()
        {
            return 31 * (int)timeMicros + (int)position;
        }

        @Override
        public String toString()
        {
            return "SeekPoint{timeMicros=" + timeMicros + ", position=" + position + "}";
        }
    }

    public static final class SeekMap
    {
        public static final int UNKNOWN_DURATION = -2147483648;

        private final long mDurationMicros;

        SeekMap(long durationMicros)
        {
            mDurationMicros = durationMicros;
        }

        public long getDurationMicros()
        {
            return mDurationMicros;
        }

        public Pair<MediaParser.SeekPoint, MediaParser.SeekPoint> getSeekPoints(long timeMicros)
        {
            return new Pair<>(SeekPoint.START, SeekPoint.START);
        }

        public boolean isSeekable()
        {
            return false;
        }
    }

    public static final class ParsingException extends IOException
    {
        ParsingException(String message)
        {
            super(message);
        }
    }

    public interface OutputConsumer
    {
        void onSampleCompleted(int trackIndex, long timeMicros, int flags, int size, int offset, MediaCodec.CryptoInfo cryptoInfo);

        void onSampleDataFound(int trackIndex, MediaParser.InputReader inputReader) throws IOException;

        void onSeekMapFound(MediaParser.SeekMap seekMap);

        void onTrackCountFound(int numberOfTracks);

        void onTrackDataFound(int trackIndex, MediaParser.TrackData trackData);
    }

    public interface InputReader
    {
        long getLength();

        long getPosition();

        int read(byte[] buffer, int offset, int readLength) throws IOException;
    }
}
