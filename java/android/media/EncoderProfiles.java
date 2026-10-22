package android.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EncoderProfiles
{
    private final int mDurationSeconds;
    private final int mFileFormat;
    private final List<VideoProfile> mVideoProfiles;
    private final List<AudioProfile> mAudioProfiles;

    EncoderProfiles(int durationSeconds, int fileFormat, List<VideoProfile> videoProfiles, List<AudioProfile> audioProfiles)
    {
        mDurationSeconds = durationSeconds;
        mFileFormat = fileFormat;
        mVideoProfiles = Collections.unmodifiableList(new ArrayList<>(videoProfiles));
        mAudioProfiles = Collections.unmodifiableList(new ArrayList<>(audioProfiles));
    }

    public List<EncoderProfiles.AudioProfile> getAudioProfiles()
    {
        return mAudioProfiles;
    }

    public int getDefaultDurationSeconds()
    {
        return mDurationSeconds;
    }

    public int getRecommendedFileFormat()
    {
        return mFileFormat;
    }

    public List<EncoderProfiles.VideoProfile> getVideoProfiles()
    {
        return mVideoProfiles;
    }

    public static final class VideoProfile
    {
        public static final int HDR_DOLBY_VISION = 4;
        public static final int HDR_HDR10 = 2;
        public static final int HDR_HDR10PLUS = 3;
        public static final int HDR_HLG = 1;
        public static final int HDR_NONE = 0;
        public static final int YUV_420 = 0;
        public static final int YUV_422 = 1;
        public static final int YUV_444 = 2;

        private final int mCodec;
        private final int mWidth;
        private final int mHeight;
        private final int mFrameRate;
        private final int mBitrate;
        private final int mProfile;
        private final int mChromaSubsampling;
        private final int mBitDepth;
        private final int mHdrFormat;

        VideoProfile(int codec, int width, int height, int frameRate, int bitrate, int profile, int chromaSubsampling,
            int bitDepth, int hdrFormat)
        {
            mCodec = codec;
            mWidth = width;
            mHeight = height;
            mFrameRate = frameRate;
            mBitrate = bitrate;
            mProfile = profile;
            mChromaSubsampling = chromaSubsampling;
            mBitDepth = bitDepth;
            mHdrFormat = hdrFormat;
        }

        public int getBitDepth()
        {
            return mBitDepth;
        }

        public int getBitrate()
        {
            return mBitrate;
        }

        public int getChromaSubsampling()
        {
            return mChromaSubsampling;
        }

        public int getCodec()
        {
            return mCodec;
        }

        public int getFrameRate()
        {
            return mFrameRate;
        }

        public int getHdrFormat()
        {
            return mHdrFormat;
        }

        public int getHeight()
        {
            return mHeight;
        }

        public String getMediaType()
        {
            switch (mCodec)
            {
                case MediaRecorder.VideoEncoder.H263:
                    return "video/3gpp";
                case MediaRecorder.VideoEncoder.H264:
                    return "video/avc";
                case MediaRecorder.VideoEncoder.MPEG_4_SP:
                    return "video/mp4v-es";
                case MediaRecorder.VideoEncoder.VP8:
                    return "video/x-vnd.on2.vp8";
                case MediaRecorder.VideoEncoder.HEVC:
                    return "video/hevc";
                case MediaRecorder.VideoEncoder.VP9:
                    return "video/x-vnd.on2.vp9";
                case MediaRecorder.VideoEncoder.DOLBY_VISION:
                    return "video/dolby-vision";
                case MediaRecorder.VideoEncoder.AV1:
                    return "video/av01";
                default:
                    throw new IllegalArgumentException("Unknown codec");
            }
        }

        public int getProfile()
        {
            return mProfile;
        }

        public int getWidth()
        {
            return mWidth;
        }
    }

    public static final class AudioProfile
    {
        private final int mCodec;
        private final int mChannels;
        private final int mSampleRate;
        private final int mBitrate;
        private final int mProfile;

        AudioProfile(int codec, int channels, int sampleRate, int bitrate, int profile)
        {
            mCodec = codec;
            mChannels = channels;
            mSampleRate = sampleRate;
            mBitrate = bitrate;
            mProfile = profile;
        }

        public int getBitrate()
        {
            return mBitrate;
        }

        public int getChannels()
        {
            return mChannels;
        }

        public int getCodec()
        {
            return mCodec;
        }

        public String getMediaType()
        {
            switch (mCodec)
            {
                case MediaRecorder.AudioEncoder.AMR_NB:
                    return "audio/3gpp";
                case MediaRecorder.AudioEncoder.AMR_WB:
                    return "audio/amr-wb";
                case MediaRecorder.AudioEncoder.AAC:
                case MediaRecorder.AudioEncoder.HE_AAC:
                case MediaRecorder.AudioEncoder.AAC_ELD:
                    return "audio/mp4a-latm";
                case MediaRecorder.AudioEncoder.VORBIS:
                    return "audio/vorbis";
                case MediaRecorder.AudioEncoder.OPUS:
                    return "audio/opus";
                default:
                    throw new IllegalArgumentException("Unknown codec");
            }
        }

        public int getProfile()
        {
            return mProfile;
        }

        public int getSampleRate()
        {
            return mSampleRate;
        }
    }
}
