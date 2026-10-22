package android.media;

import android.util.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class MediaCodecInfo
{
    public static final int SECURITY_MODEL_MEMORY_SAFE = 1;
    public static final int SECURITY_MODEL_SANDBOXED = 0;

    static final int FLAG_IS_ENCODER = 1;
    static final int FLAG_IS_VENDOR = 2;
    static final int FLAG_IS_SOFTWARE_ONLY = 4;
    static final int FLAG_IS_HARDWARE_ACCELERATED = 8;

    private final String mName;
    private final String mCanonicalName;
    private final int mFlags;
    private final int mSecurityModel;
    private final CodecCapabilities[] mCapabilities;

    MediaCodecInfo(String name, String canonicalName, int flags, int securityModel, CodecCapabilities[] capabilities)
    {
        mName = name;
        mCanonicalName = canonicalName;
        mFlags = flags;
        mSecurityModel = securityModel;
        mCapabilities = capabilities.clone();
    }

    public String getCanonicalName()
    {
        return mCanonicalName;
    }

    public MediaCodecInfo.CodecCapabilities getCapabilitiesForType(String type)
    {
        for (CodecCapabilities capabilities : mCapabilities)
        {
            if (capabilities.getMimeType().equalsIgnoreCase(type)) return capabilities;
        }

        throw new IllegalArgumentException("codec does not support type");
    }

    public String getName()
    {
        return mName;
    }

    public int getSecurityModel()
    {
        return mSecurityModel;
    }

    public String[] getSupportedTypes()
    {
        String[] types = new String[mCapabilities.length];

        for (int index = 0; index < types.length; index++)
        {
            types[index] = mCapabilities[index].getMimeType();
        }

        Arrays.sort(types);
        return types;
    }

    public boolean isAlias()
    {
        return !mName.equals(mCanonicalName);
    }

    public boolean isEncoder()
    {
        return (mFlags & FLAG_IS_ENCODER) != 0;
    }

    public boolean isHardwareAccelerated()
    {
        return (mFlags & FLAG_IS_HARDWARE_ACCELERATED) != 0;
    }

    public boolean isSoftwareOnly()
    {
        return (mFlags & FLAG_IS_SOFTWARE_ONLY) != 0;
    }

    public boolean isVendor()
    {
        return (mFlags & FLAG_IS_VENDOR) != 0;
    }

    private static int alignUp(int value, int alignment)
    {
        return (value + alignment - 1) / alignment * alignment;
    }

    public static final class VideoCapabilities
    {
        private final Range<Integer> mWidthRange;
        private final Range<Integer> mHeightRange;
        private final int mWidthAlignment;
        private final int mHeightAlignment;
        private final Range<Integer> mFrameRateRange;
        private final Range<Integer> mBitrateRange;
        private final long mMaxPixelsPerSecond;
        private final long mMaxPixels;
        private final List<PerformancePoint> mPerformancePoints;

        VideoCapabilities(Range<Integer> widthRange, Range<Integer> heightRange, int widthAlignment, int heightAlignment,
            Range<Integer> frameRateRange, Range<Integer> bitrateRange, long maxPixels, long maxPixelsPerSecond,
            List<PerformancePoint> performancePoints)
        {
            mWidthRange = widthRange;
            mHeightRange = heightRange;
            mWidthAlignment = widthAlignment;
            mHeightAlignment = heightAlignment;
            mFrameRateRange = frameRateRange;
            mBitrateRange = bitrateRange;
            mMaxPixels = maxPixels;
            mMaxPixelsPerSecond = maxPixelsPerSecond;
            mPerformancePoints = Collections.unmodifiableList(new ArrayList<>(performancePoints));
        }

        public boolean areSizeAndRateSupported(int width, int height, double frameRate)
        {
            if (!isSizeSupported(width, height)) return false;

            Range<Double> rates = getSupportedFrameRatesFor(width, height);
            return frameRate >= rates.getLower() && frameRate <= rates.getUpper();
        }

        public Range<Double> getAchievableFrameRatesFor(int width, int height)
        {
            if (!isSizeSupported(width, height)) throw new IllegalArgumentException("unsupported size");

            return null;
        }

        public Range<Integer> getBitrateRange()
        {
            return mBitrateRange;
        }

        public int getHeightAlignment()
        {
            return mHeightAlignment;
        }

        public Range<Integer> getSupportedFrameRates()
        {
            return mFrameRateRange;
        }

        public Range<Double> getSupportedFrameRatesFor(int width, int height)
        {
            if (!isSizeSupported(width, height)) throw new IllegalArgumentException("unsupported size");

            long pixels = (long)alignUp(width, mWidthAlignment) * alignUp(height, mHeightAlignment);
            double upper = Math.min(mFrameRateRange.getUpper(), (double)mMaxPixelsPerSecond / pixels);
            return Range.create(Math.min((double)mFrameRateRange.getLower(), upper), upper);
        }

        public Range<Integer> getSupportedHeights()
        {
            return mHeightRange;
        }

        public Range<Integer> getSupportedHeightsFor(int width)
        {
            if (!mWidthRange.contains(width) || width % mWidthAlignment != 0)
            {
                throw new IllegalArgumentException("unsupported width");
            }

            int upper = (int)Math.min(mHeightRange.getUpper(), mMaxPixels / alignUp(width, mWidthAlignment));
            upper = upper / mHeightAlignment * mHeightAlignment;
            return Range.create(mHeightRange.getLower(), Math.max(mHeightRange.getLower(), upper));
        }

        public List<MediaCodecInfo.VideoCapabilities.PerformancePoint> getSupportedPerformancePoints()
        {
            return mPerformancePoints;
        }

        public Range<Integer> getSupportedWidths()
        {
            return mWidthRange;
        }

        public Range<Integer> getSupportedWidthsFor(int height)
        {
            if (!mHeightRange.contains(height) || height % mHeightAlignment != 0)
            {
                throw new IllegalArgumentException("unsupported height");
            }

            int upper = (int)Math.min(mWidthRange.getUpper(), mMaxPixels / alignUp(height, mHeightAlignment));
            upper = upper / mWidthAlignment * mWidthAlignment;
            return Range.create(mWidthRange.getLower(), Math.max(mWidthRange.getLower(), upper));
        }

        public int getWidthAlignment()
        {
            return mWidthAlignment;
        }

        public boolean isSizeSupported(int width, int height)
        {
            if (!mWidthRange.contains(width) || !mHeightRange.contains(height)) return false;
            if (width % mWidthAlignment != 0 || height % mHeightAlignment != 0) return false;

            return (long)width * height <= mMaxPixels;
        }

        public static final class PerformancePoint
        {
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_100 = new PerformancePoint(1920, 1080, 100);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_120 = new PerformancePoint(1920, 1080, 120);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_200 = new PerformancePoint(1920, 1080, 200);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_24 = new PerformancePoint(1920, 1080, 24);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_240 = new PerformancePoint(1920, 1080, 240);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_25 = new PerformancePoint(1920, 1080, 25);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_30 = new PerformancePoint(1920, 1080, 30);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_50 = new PerformancePoint(1920, 1080, 50);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint FHD_60 = new PerformancePoint(1920, 1080, 60);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_100 = new PerformancePoint(1280, 720, 100);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_120 = new PerformancePoint(1280, 720, 120);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_200 = new PerformancePoint(1280, 720, 200);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_24 = new PerformancePoint(1280, 720, 24);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_240 = new PerformancePoint(1280, 720, 240);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_25 = new PerformancePoint(1280, 720, 25);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_30 = new PerformancePoint(1280, 720, 30);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_50 = new PerformancePoint(1280, 720, 50);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint HD_60 = new PerformancePoint(1280, 720, 60);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint SD_24 = new PerformancePoint(720, 480, 24);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint SD_25 = new PerformancePoint(720, 576, 25);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint SD_30 = new PerformancePoint(720, 480, 30);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint SD_48 = new PerformancePoint(720, 480, 48);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint SD_50 = new PerformancePoint(720, 576, 50);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint SD_60 = new PerformancePoint(720, 480, 60);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_100 = new PerformancePoint(3840, 2160, 100);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_120 = new PerformancePoint(3840, 2160, 120);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_200 = new PerformancePoint(3840, 2160, 200);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_24 = new PerformancePoint(3840, 2160, 24);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_240 = new PerformancePoint(3840, 2160, 240);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_25 = new PerformancePoint(3840, 2160, 25);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_30 = new PerformancePoint(3840, 2160, 30);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_50 = new PerformancePoint(3840, 2160, 50);
            public static final MediaCodecInfo.VideoCapabilities.PerformancePoint UHD_60 = new PerformancePoint(3840, 2160, 60);

            private static final int BLOCK_SIZE = 16;

            private final int mWidth;
            private final int mHeight;
            private final int mFrameRate;

            public PerformancePoint(int width, int height, int frameRate)
            {
                if (width <= 0 || height <= 0 || frameRate <= 0) throw new IllegalArgumentException("invalid performance point");

                mWidth = width;
                mHeight = height;
                mFrameRate = frameRate;
            }

            private long getMacroBlocks()
            {
                return (long)((mWidth + BLOCK_SIZE - 1) / BLOCK_SIZE) * ((mHeight + BLOCK_SIZE - 1) / BLOCK_SIZE);
            }

            private int getMaxDimensionBlocks()
            {
                return (Math.max(mWidth, mHeight) + BLOCK_SIZE - 1) / BLOCK_SIZE;
            }

            public boolean covers(MediaCodecInfo.VideoCapabilities.PerformancePoint other)
            {
                return getMacroBlocks() >= other.getMacroBlocks() && getMaxDimensionBlocks() >= other.getMaxDimensionBlocks()
                    && getMacroBlocks() * mFrameRate >= other.getMacroBlocks() * other.mFrameRate && mFrameRate >= other.mFrameRate;
            }

            public boolean covers(MediaFormat format)
            {
                Number rate = format.getNumber(MediaFormat.KEY_FRAME_RATE, 0);
                int frameRate = (int)Math.ceil(rate.doubleValue());
                int width = format.getInteger(MediaFormat.KEY_WIDTH, 0);
                int height = format.getInteger(MediaFormat.KEY_HEIGHT, 0);
                if (width <= 0 || height <= 0) return false;

                return covers(new PerformancePoint(width, height, Math.max(1, frameRate)));
            }

            @Override
            public boolean equals(Object object)
            {
                if (!(object instanceof PerformancePoint)) return false;

                PerformancePoint other = (PerformancePoint)object;
                return getMacroBlocks() == other.getMacroBlocks() && getMaxDimensionBlocks() == other.getMaxDimensionBlocks()
                    && mFrameRate == other.mFrameRate;
            }

            @Override
            public int hashCode()
            {
                return Objects.hash(getMacroBlocks(), getMaxDimensionBlocks(), mFrameRate);
            }

            @Override
            public String toString()
            {
                return "PerformancePoint(" + mWidth + "x" + mHeight + "@" + mFrameRate + ")";
            }
        }
    }

    public static final class EncoderCapabilities
    {
        public static final int BITRATE_MODE_CBR = 2;
        public static final int BITRATE_MODE_CBR_FD = 3;
        public static final int BITRATE_MODE_CQ = 0;
        public static final int BITRATE_MODE_VBR = 1;

        private final Range<Integer> mComplexityRange;
        private final Range<Integer> mQualityRange;
        private final int mBitrateModes;

        EncoderCapabilities(Range<Integer> complexityRange, Range<Integer> qualityRange, int bitrateModes)
        {
            mComplexityRange = complexityRange;
            mQualityRange = qualityRange;
            mBitrateModes = bitrateModes;
        }

        public Range<Integer> getComplexityRange()
        {
            return mComplexityRange;
        }

        public Range<Integer> getQualityRange()
        {
            return mQualityRange;
        }

        public boolean isBitrateModeSupported(int mode)
        {
            return mode >= 0 && mode < 32 && (mBitrateModes & (1 << mode)) != 0;
        }
    }

    public static final class CodecProfileLevel
    {
        public static final int AACObjectELD = 39;
        public static final int AACObjectERLC = 17;
        public static final int AACObjectERScalable = 20;
        public static final int AACObjectHE = 5;
        public static final int AACObjectHE_PS = 29;
        public static final int AACObjectLC = 2;
        public static final int AACObjectLD = 23;
        public static final int AACObjectLTP = 4;
        public static final int AACObjectMain = 1;
        public static final int AACObjectSSR = 3;
        public static final int AACObjectScalable = 6;
        public static final int AACObjectXHE = 42;
        public static final int AC4Level0 = 1;
        public static final int AC4Level1 = 2;
        public static final int AC4Level2 = 4;
        public static final int AC4Level3 = 8;
        public static final int AC4Level4 = 16;
        public static final int AC4Profile00 = 257;
        public static final int AC4Profile10 = 513;
        public static final int AC4Profile11 = 514;
        public static final int AC4Profile21 = 1026;
        public static final int AC4Profile22 = 1028;
        public static final int APVLevel11Band0 = 513;
        public static final int APVLevel11Band1 = 514;
        public static final int APVLevel11Band2 = 516;
        public static final int APVLevel11Band3 = 520;
        public static final int APVLevel1Band0 = 257;
        public static final int APVLevel1Band1 = 258;
        public static final int APVLevel1Band2 = 260;
        public static final int APVLevel1Band3 = 264;
        public static final int APVLevel21Band0 = 2049;
        public static final int APVLevel21Band1 = 2050;
        public static final int APVLevel21Band2 = 2052;
        public static final int APVLevel21Band3 = 2056;
        public static final int APVLevel2Band0 = 1025;
        public static final int APVLevel2Band1 = 1026;
        public static final int APVLevel2Band2 = 1028;
        public static final int APVLevel2Band3 = 1032;
        public static final int APVLevel31Band0 = 8193;
        public static final int APVLevel31Band1 = 8194;
        public static final int APVLevel31Band2 = 8196;
        public static final int APVLevel31Band3 = 8200;
        public static final int APVLevel3Band0 = 4097;
        public static final int APVLevel3Band1 = 4098;
        public static final int APVLevel3Band2 = 4100;
        public static final int APVLevel3Band3 = 4104;
        public static final int APVLevel41Band0 = 32769;
        public static final int APVLevel41Band1 = 32770;
        public static final int APVLevel41Band2 = 32772;
        public static final int APVLevel41Band3 = 32776;
        public static final int APVLevel4Band0 = 16385;
        public static final int APVLevel4Band1 = 16386;
        public static final int APVLevel4Band2 = 16388;
        public static final int APVLevel4Band3 = 16392;
        public static final int APVLevel51Band0 = 131073;
        public static final int APVLevel51Band1 = 131074;
        public static final int APVLevel51Band2 = 131076;
        public static final int APVLevel51Band3 = 131080;
        public static final int APVLevel5Band0 = 65537;
        public static final int APVLevel5Band1 = 65538;
        public static final int APVLevel5Band2 = 65540;
        public static final int APVLevel5Band3 = 65544;
        public static final int APVLevel61Band0 = 524289;
        public static final int APVLevel61Band1 = 524290;
        public static final int APVLevel61Band2 = 524292;
        public static final int APVLevel61Band3 = 524296;
        public static final int APVLevel6Band0 = 262145;
        public static final int APVLevel6Band1 = 262146;
        public static final int APVLevel6Band2 = 262148;
        public static final int APVLevel6Band3 = 262152;
        public static final int APVLevel71Band0 = 2097153;
        public static final int APVLevel71Band1 = 2097154;
        public static final int APVLevel71Band2 = 2097156;
        public static final int APVLevel71Band3 = 2097160;
        public static final int APVLevel7Band0 = 1048577;
        public static final int APVLevel7Band1 = 1048578;
        public static final int APVLevel7Band2 = 1048580;
        public static final int APVLevel7Band3 = 1048584;
        public static final int APVProfile422_10 = 1;
        public static final int APVProfile422_10HDR10 = 4096;
        public static final int APVProfile422_10HDR10Plus = 8192;
        public static final int AV1Level2 = 1;
        public static final int AV1Level21 = 2;
        public static final int AV1Level22 = 4;
        public static final int AV1Level23 = 8;
        public static final int AV1Level3 = 16;
        public static final int AV1Level31 = 32;
        public static final int AV1Level32 = 64;
        public static final int AV1Level33 = 128;
        public static final int AV1Level4 = 256;
        public static final int AV1Level41 = 512;
        public static final int AV1Level42 = 1024;
        public static final int AV1Level43 = 2048;
        public static final int AV1Level5 = 4096;
        public static final int AV1Level51 = 8192;
        public static final int AV1Level52 = 16384;
        public static final int AV1Level53 = 32768;
        public static final int AV1Level6 = 65536;
        public static final int AV1Level61 = 131072;
        public static final int AV1Level62 = 262144;
        public static final int AV1Level63 = 524288;
        public static final int AV1Level7 = 1048576;
        public static final int AV1Level71 = 2097152;
        public static final int AV1Level72 = 4194304;
        public static final int AV1Level73 = 8388608;
        public static final int AV1ProfileMain10 = 2;
        public static final int AV1ProfileMain10HDR10 = 4096;
        public static final int AV1ProfileMain10HDR10Plus = 8192;
        public static final int AV1ProfileMain8 = 1;
        public static final int AVCLevel1 = 1;
        public static final int AVCLevel11 = 4;
        public static final int AVCLevel12 = 8;
        public static final int AVCLevel13 = 16;
        public static final int AVCLevel1b = 2;
        public static final int AVCLevel2 = 32;
        public static final int AVCLevel21 = 64;
        public static final int AVCLevel22 = 128;
        public static final int AVCLevel3 = 256;
        public static final int AVCLevel31 = 512;
        public static final int AVCLevel32 = 1024;
        public static final int AVCLevel4 = 2048;
        public static final int AVCLevel41 = 4096;
        public static final int AVCLevel42 = 8192;
        public static final int AVCLevel5 = 16384;
        public static final int AVCLevel51 = 32768;
        public static final int AVCLevel52 = 65536;
        public static final int AVCLevel6 = 131072;
        public static final int AVCLevel61 = 262144;
        public static final int AVCLevel62 = 524288;
        public static final int AVCProfileBaseline = 1;
        public static final int AVCProfileConstrainedBaseline = 65536;
        public static final int AVCProfileConstrainedHigh = 524288;
        public static final int AVCProfileExtended = 4;
        public static final int AVCProfileHigh = 8;
        public static final int AVCProfileHigh10 = 16;
        public static final int AVCProfileHigh422 = 32;
        public static final int AVCProfileHigh444 = 64;
        public static final int AVCProfileMain = 2;
        public static final int DTS_HDProfileHRA = 1;
        public static final int DTS_HDProfileLBR = 2;
        public static final int DTS_HDProfileMA = 4;
        public static final int DTS_UHDProfileP1 = 1;
        public static final int DTS_UHDProfileP2 = 2;
        public static final int DolbyVisionLevel8k30 = 1024;
        public static final int DolbyVisionLevel8k60 = 2048;
        public static final int DolbyVisionLevelFhd24 = 4;
        public static final int DolbyVisionLevelFhd30 = 8;
        public static final int DolbyVisionLevelFhd60 = 16;
        public static final int DolbyVisionLevelHd24 = 1;
        public static final int DolbyVisionLevelHd30 = 2;
        public static final int DolbyVisionLevelUhd120 = 512;
        public static final int DolbyVisionLevelUhd24 = 32;
        public static final int DolbyVisionLevelUhd30 = 64;
        public static final int DolbyVisionLevelUhd48 = 128;
        public static final int DolbyVisionLevelUhd60 = 256;
        public static final int DolbyVisionProfileDvav110 = 1024;
        public static final int DolbyVisionProfileDvavPen = 2;
        public static final int DolbyVisionProfileDvavPer = 1;
        public static final int DolbyVisionProfileDvavSe = 512;
        public static final int DolbyVisionProfileDvheDen = 8;
        public static final int DolbyVisionProfileDvheDer = 4;
        public static final int DolbyVisionProfileDvheDtb = 128;
        public static final int DolbyVisionProfileDvheDth = 64;
        public static final int DolbyVisionProfileDvheDtr = 16;
        public static final int DolbyVisionProfileDvheSt = 256;
        public static final int DolbyVisionProfileDvheStn = 32;
        public static final int H263Level10 = 1;
        public static final int H263Level20 = 2;
        public static final int H263Level30 = 4;
        public static final int H263Level40 = 8;
        public static final int H263Level45 = 16;
        public static final int H263Level50 = 32;
        public static final int H263Level60 = 64;
        public static final int H263Level70 = 128;
        public static final int H263ProfileBackwardCompatible = 4;
        public static final int H263ProfileBaseline = 1;
        public static final int H263ProfileH320Coding = 2;
        public static final int H263ProfileHighCompression = 32;
        public static final int H263ProfileHighLatency = 256;
        public static final int H263ProfileISWV2 = 8;
        public static final int H263ProfileISWV3 = 16;
        public static final int H263ProfileInterlace = 128;
        public static final int H263ProfileInternet = 64;
        public static final int HEVCHighTierLevel1 = 2;
        public static final int HEVCHighTierLevel2 = 8;
        public static final int HEVCHighTierLevel21 = 32;
        public static final int HEVCHighTierLevel3 = 128;
        public static final int HEVCHighTierLevel31 = 512;
        public static final int HEVCHighTierLevel4 = 2048;
        public static final int HEVCHighTierLevel41 = 8192;
        public static final int HEVCHighTierLevel5 = 32768;
        public static final int HEVCHighTierLevel51 = 131072;
        public static final int HEVCHighTierLevel52 = 524288;
        public static final int HEVCHighTierLevel6 = 2097152;
        public static final int HEVCHighTierLevel61 = 8388608;
        public static final int HEVCHighTierLevel62 = 33554432;
        public static final int HEVCMainTierLevel1 = 1;
        public static final int HEVCMainTierLevel2 = 4;
        public static final int HEVCMainTierLevel21 = 16;
        public static final int HEVCMainTierLevel3 = 64;
        public static final int HEVCMainTierLevel31 = 256;
        public static final int HEVCMainTierLevel4 = 1024;
        public static final int HEVCMainTierLevel41 = 4096;
        public static final int HEVCMainTierLevel5 = 16384;
        public static final int HEVCMainTierLevel51 = 65536;
        public static final int HEVCMainTierLevel52 = 262144;
        public static final int HEVCMainTierLevel6 = 1048576;
        public static final int HEVCMainTierLevel61 = 4194304;
        public static final int HEVCMainTierLevel62 = 16777216;
        public static final int HEVCProfileMain = 1;
        public static final int HEVCProfileMain10 = 2;
        public static final int HEVCProfileMain10HDR10 = 4096;
        public static final int HEVCProfileMain10HDR10Plus = 8192;
        public static final int HEVCProfileMainStill = 4;
        public static final int IAMFProfileBaseAac = 16908290;
        public static final int IAMFProfileBaseEnhancedAac = 17039362;
        public static final int IAMFProfileBaseEnhancedFlac = 17039364;
        public static final int IAMFProfileBaseEnhancedOpus = 17039361;
        public static final int IAMFProfileBaseEnhancedPcm = 17039368;
        public static final int IAMFProfileBaseFlac = 16908292;
        public static final int IAMFProfileBaseOpus = 16908289;
        public static final int IAMFProfileBasePcm = 16908296;
        public static final int IAMFProfileSimpleAac = 16842754;
        public static final int IAMFProfileSimpleFlac = 16842756;
        public static final int IAMFProfileSimpleOpus = 16842753;
        public static final int IAMFProfileSimplePcm = 16842760;
        public static final int MPEG2LevelH14 = 2;
        public static final int MPEG2LevelHL = 3;
        public static final int MPEG2LevelHP = 4;
        public static final int MPEG2LevelLL = 0;
        public static final int MPEG2LevelML = 1;
        public static final int MPEG2Profile422 = 2;
        public static final int MPEG2ProfileHigh = 5;
        public static final int MPEG2ProfileMain = 1;
        public static final int MPEG2ProfileSNR = 3;
        public static final int MPEG2ProfileSimple = 0;
        public static final int MPEG2ProfileSpatial = 4;
        public static final int MPEG4Level0 = 1;
        public static final int MPEG4Level0b = 2;
        public static final int MPEG4Level1 = 4;
        public static final int MPEG4Level2 = 8;
        public static final int MPEG4Level3 = 16;
        public static final int MPEG4Level3b = 24;
        public static final int MPEG4Level4 = 32;
        public static final int MPEG4Level4a = 64;
        public static final int MPEG4Level5 = 128;
        public static final int MPEG4Level6 = 256;
        public static final int MPEG4ProfileAdvancedCoding = 4096;
        public static final int MPEG4ProfileAdvancedCore = 8192;
        public static final int MPEG4ProfileAdvancedRealTime = 1024;
        public static final int MPEG4ProfileAdvancedScalable = 16384;
        public static final int MPEG4ProfileAdvancedSimple = 32768;
        public static final int MPEG4ProfileBasicAnimated = 256;
        public static final int MPEG4ProfileCore = 4;
        public static final int MPEG4ProfileCoreScalable = 2048;
        public static final int MPEG4ProfileHybrid = 512;
        public static final int MPEG4ProfileMain = 8;
        public static final int MPEG4ProfileNbit = 16;
        public static final int MPEG4ProfileScalableTexture = 32;
        public static final int MPEG4ProfileSimple = 1;
        public static final int MPEG4ProfileSimpleFBA = 128;
        public static final int MPEG4ProfileSimpleFace = 64;
        public static final int MPEG4ProfileSimpleScalable = 2;
        public static final int VP8Level_Version0 = 1;
        public static final int VP8Level_Version1 = 2;
        public static final int VP8Level_Version2 = 4;
        public static final int VP8Level_Version3 = 8;
        public static final int VP8ProfileMain = 1;
        public static final int VP9Level1 = 1;
        public static final int VP9Level11 = 2;
        public static final int VP9Level2 = 4;
        public static final int VP9Level21 = 8;
        public static final int VP9Level3 = 16;
        public static final int VP9Level31 = 32;
        public static final int VP9Level4 = 64;
        public static final int VP9Level41 = 128;
        public static final int VP9Level5 = 256;
        public static final int VP9Level51 = 512;
        public static final int VP9Level52 = 1024;
        public static final int VP9Level6 = 2048;
        public static final int VP9Level61 = 4096;
        public static final int VP9Level62 = 8192;
        public static final int VP9Profile0 = 1;
        public static final int VP9Profile1 = 2;
        public static final int VP9Profile2 = 4;
        public static final int VP9Profile2HDR = 4096;
        public static final int VP9Profile2HDR10Plus = 16384;
        public static final int VP9Profile3 = 8;
        public static final int VP9Profile3HDR = 8192;
        public static final int VP9Profile3HDR10Plus = 32768;
        public int level;
        public int profile;

        public CodecProfileLevel()
        {
        }

        @Override
        public boolean equals(Object object)
        {
            if (!(object instanceof CodecProfileLevel)) return false;

            CodecProfileLevel other = (CodecProfileLevel)object;
            return other.profile == profile && other.level == level;
        }

        @Override
        public int hashCode()
        {
            return Long.hashCode(((long)profile << Integer.SIZE) | level);
        }
    }

    public static final class CodecCapabilities
    {
        public static final int COLOR_Format12bitRGB444 = 3;
        public static final int COLOR_Format16bitARGB1555 = 5;
        public static final int COLOR_Format16bitARGB4444 = 4;
        public static final int COLOR_Format16bitBGR565 = 7;
        public static final int COLOR_Format16bitRGB565 = 6;
        public static final int COLOR_Format18BitBGR666 = 41;
        public static final int COLOR_Format18bitARGB1665 = 9;
        public static final int COLOR_Format18bitRGB666 = 8;
        public static final int COLOR_Format19bitARGB1666 = 10;
        public static final int COLOR_Format24BitABGR6666 = 43;
        public static final int COLOR_Format24BitARGB6666 = 42;
        public static final int COLOR_Format24bitARGB1887 = 13;
        public static final int COLOR_Format24bitBGR888 = 12;
        public static final int COLOR_Format24bitRGB888 = 11;
        public static final int COLOR_Format25bitARGB1888 = 14;
        public static final int COLOR_Format32bitABGR2101010 = 2130750114;
        public static final int COLOR_Format32bitABGR8888 = 2130747392;
        public static final int COLOR_Format32bitARGB8888 = 16;
        public static final int COLOR_Format32bitBGRA8888 = 15;
        public static final int COLOR_Format64bitABGRFloat = 2130710294;
        public static final int COLOR_Format8bitRGB332 = 2;
        public static final int COLOR_FormatCbYCrY = 27;
        public static final int COLOR_FormatCrYCbY = 28;
        public static final int COLOR_FormatL16 = 36;
        public static final int COLOR_FormatL2 = 33;
        public static final int COLOR_FormatL24 = 37;
        public static final int COLOR_FormatL32 = 38;
        public static final int COLOR_FormatL4 = 34;
        public static final int COLOR_FormatL8 = 35;
        public static final int COLOR_FormatMonochrome = 1;
        public static final int COLOR_FormatRGBAFlexible = 2134288520;
        public static final int COLOR_FormatRGBFlexible = 2134292616;
        public static final int COLOR_FormatRawBayer10bit = 31;
        public static final int COLOR_FormatRawBayer8bit = 30;
        public static final int COLOR_FormatRawBayer8bitcompressed = 32;
        public static final int COLOR_FormatSurface = 2130708361;
        public static final int COLOR_FormatYCbYCr = 25;
        public static final int COLOR_FormatYCrYCb = 26;
        public static final int COLOR_FormatYUV411PackedPlanar = 18;
        public static final int COLOR_FormatYUV411Planar = 17;
        public static final int COLOR_FormatYUV420Flexible = 2135033992;
        public static final int COLOR_FormatYUV420PackedPlanar = 20;
        public static final int COLOR_FormatYUV420PackedSemiPlanar = 39;
        public static final int COLOR_FormatYUV420Planar = 19;
        public static final int COLOR_FormatYUV420SemiPlanar = 21;
        public static final int COLOR_FormatYUV422Flexible = 2135042184;
        public static final int COLOR_FormatYUV422PackedPlanar = 23;
        public static final int COLOR_FormatYUV422PackedSemiPlanar = 40;
        public static final int COLOR_FormatYUV422Planar = 22;
        public static final int COLOR_FormatYUV422SemiPlanar = 24;
        public static final int COLOR_FormatYUV444Flexible = 2135181448;
        public static final int COLOR_FormatYUV444Interleaved = 29;
        public static final int COLOR_FormatYUVP010 = 54;
        public static final int COLOR_FormatYUVP210 = 60;
        public static final int COLOR_QCOM_FormatYUV420SemiPlanar = 2141391872;
        public static final int COLOR_TI_FormatYUV420PackedSemiPlanar = 2130706688;
        public static final String FEATURE_AdaptivePlayback = "adaptive-playback";
        public static final String FEATURE_DetachedSurface = "detached-surface";
        public static final String FEATURE_DynamicColorAspects = "dynamic-color-aspects";
        public static final String FEATURE_DynamicTimestamp = "dynamic-timestamp";
        public static final String FEATURE_EncodingStatistics = "encoding-statistics";
        public static final String FEATURE_FrameParsing = "frame-parsing";
        public static final String FEATURE_HdrEditing = "hdr-editing";
        public static final String FEATURE_HlgEditing = "hlg-editing";
        public static final String FEATURE_IntraRefresh = "intra-refresh";
        public static final String FEATURE_LowLatency = "low-latency";
        public static final String FEATURE_MultipleFrames = "multiple-frames";
        public static final String FEATURE_PartialFrame = "partial-frame";
        public static final String FEATURE_QpBounds = "qp-bounds";
        public static final String FEATURE_Roi = "region-of-interest";
        public static final String FEATURE_SecurePlayback = "secure-playback";
        public static final String FEATURE_TunneledPlayback = "tunneled-playback";
        public int[] colorFormats;
        public MediaCodecInfo.CodecProfileLevel[] profileLevels;

        private String mMimeType;
        private int mMaxSupportedInstances;
        private final Set<String> mFeaturesSupported = new HashSet<>();
        private final Set<String> mFeaturesRequired = new HashSet<>();
        private MediaFormat mDefaultFormat = new MediaFormat();
        private AudioCapabilities mAudioCapabilities;
        private VideoCapabilities mVideoCapabilities;
        private EncoderCapabilities mEncoderCapabilities;

        public CodecCapabilities()
        {
            colorFormats = new int[0];
            profileLevels = new CodecProfileLevel[0];
        }

        CodecCapabilities(String mimeType, CodecProfileLevel[] levels, int[] formats, Set<String> supported, Set<String> required,
            MediaFormat defaultFormat, AudioCapabilities audio, VideoCapabilities video, EncoderCapabilities encoder, int maxInstances)
        {
            mMimeType = mimeType;
            profileLevels = levels.clone();
            colorFormats = formats.clone();
            mFeaturesSupported.addAll(supported);
            mFeaturesSupported.addAll(required);
            mFeaturesRequired.addAll(required);
            mDefaultFormat = new MediaFormat(defaultFormat);
            mAudioCapabilities = audio;
            mVideoCapabilities = video;
            mEncoderCapabilities = encoder;
            mMaxSupportedInstances = maxInstances;
        }

        public static MediaCodecInfo.CodecCapabilities createFromProfileLevel(String mime, int profile, int level)
        {
            if (mime == null) return null;

            CodecProfileLevel profileLevel = new CodecProfileLevel();
            profileLevel.profile = profile;
            profileLevel.level = level;

            MediaFormat defaultFormat = new MediaFormat();
            defaultFormat.setString(MediaFormat.KEY_MIME, mime);

            String lower = mime.toLowerCase(Locale.ROOT);
            AudioCapabilities audio = null;
            VideoCapabilities video = null;
            int[] formats = new int[0];

            if (lower.startsWith("audio/"))
            {
                audio = new AudioCapabilities(Range.create(1, 510000000), new Range[] {Range.create(8000, 96000)}, 8);
            }
            else if (lower.startsWith("video/") || lower.startsWith("image/"))
            {
                video = new VideoCapabilities(Range.create(2, 4096), Range.create(2, 4096), 2, 2, Range.create(0, 960),
                    Range.create(1, 500000000), 4096L * 2304L, 4096L * 2304L * 60L, new ArrayList<>());
                formats = new int[] {COLOR_FormatYUV420Flexible};
            }
            else
            {
                return null;
            }

            return new CodecCapabilities(mime, new CodecProfileLevel[] {profileLevel}, formats, new HashSet<>(), new HashSet<>(),
                defaultFormat, audio, video, null, 1);
        }

        public MediaCodecInfo.AudioCapabilities getAudioCapabilities()
        {
            return mAudioCapabilities;
        }

        public MediaFormat getDefaultFormat()
        {
            return mDefaultFormat;
        }

        public MediaCodecInfo.EncoderCapabilities getEncoderCapabilities()
        {
            return mEncoderCapabilities;
        }

        public int getMaxSupportedInstances()
        {
            return mMaxSupportedInstances;
        }

        public String getMimeType()
        {
            return mMimeType;
        }

        public MediaCodecInfo.VideoCapabilities getVideoCapabilities()
        {
            return mVideoCapabilities;
        }

        public boolean isFeatureRequired(String name)
        {
            return mFeaturesRequired.contains(name);
        }

        public boolean isFeatureSupported(String name)
        {
            return mFeaturesSupported.contains(name);
        }

        public boolean isFormatSupported(MediaFormat format)
        {
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mMimeType != null && !mMimeType.equalsIgnoreCase(mime)) return false;

            for (String feature : format.getFeatures())
            {
                boolean enabled = format.getFeatureEnabled(feature);
                if (enabled && !isFeatureSupported(feature)) return false;
                if (!enabled && isFeatureRequired(feature)) return false;
            }

            if (mAudioCapabilities != null)
            {
                int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 0);
                int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 0);
                if (sampleRate != 0 && !mAudioCapabilities.isSampleRateSupported(sampleRate)) return false;
                if (channelCount != 0 && (channelCount < mAudioCapabilities.getMinInputChannelCount()
                    || channelCount > mAudioCapabilities.getMaxInputChannelCount())) return false;
            }

            if (mVideoCapabilities != null)
            {
                int width = format.getInteger(MediaFormat.KEY_WIDTH, 0);
                int height = format.getInteger(MediaFormat.KEY_HEIGHT, 0);
                if (width != 0 && height != 0 && !mVideoCapabilities.isSizeSupported(width, height)) return false;

                Number frameRate = format.getNumber(MediaFormat.KEY_FRAME_RATE, null);
                if (frameRate != null && width != 0 && height != 0
                    && !mVideoCapabilities.areSizeAndRateSupported(width, height, frameRate.doubleValue())) return false;
            }

            if (format.containsKey(MediaFormat.KEY_PROFILE) && profileLevels.length > 0)
            {
                int profile = format.getInteger(MediaFormat.KEY_PROFILE);
                boolean found = false;

                for (CodecProfileLevel level : profileLevels)
                {
                    if (level.profile == profile) found = true;
                }

                if (!found) return false;
            }

            return true;
        }
    }

    public static final class AudioCapabilities
    {
        private final Range<Integer> mBitrateRange;
        private final Range<Integer>[] mSampleRateRanges;
        private final int mMaxInputChannelCount;

        AudioCapabilities(Range<Integer> bitrateRange, Range<Integer>[] sampleRateRanges, int maxInputChannelCount)
        {
            mBitrateRange = bitrateRange;
            mSampleRateRanges = sampleRateRanges.clone();
            mMaxInputChannelCount = maxInputChannelCount;
        }

        public Range<Integer> getBitrateRange()
        {
            return mBitrateRange;
        }

        @SuppressWarnings("unchecked")
        public Range<Integer>[] getInputChannelCountRanges()
        {
            return new Range[] {Range.create(1, mMaxInputChannelCount)};
        }

        public int getMaxInputChannelCount()
        {
            return mMaxInputChannelCount;
        }

        public int getMinInputChannelCount()
        {
            return 1;
        }

        public Range<Integer>[] getSupportedSampleRateRanges()
        {
            return mSampleRateRanges.clone();
        }

        public int[] getSupportedSampleRates()
        {
            for (Range<Integer> range : mSampleRateRanges)
            {
                if (!range.getLower().equals(range.getUpper())) return null;
            }

            int[] rates = new int[mSampleRateRanges.length];

            for (int index = 0; index < rates.length; index++)
            {
                rates[index] = mSampleRateRanges[index].getLower();
            }

            return rates;
        }

        public boolean isSampleRateSupported(int sampleRate)
        {
            for (Range<Integer> range : mSampleRateRanges)
            {
                if (range.contains(sampleRate)) return true;
            }

            return false;
        }
    }
}
