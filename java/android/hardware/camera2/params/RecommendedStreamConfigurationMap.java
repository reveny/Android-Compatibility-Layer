package android.hardware.camera2.params;

import android.util.Range;
import android.util.Size;
import android.view.Surface;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class RecommendedStreamConfigurationMap
{
    public static final int USECASE_PREVIEW = 0;
    public static final int USECASE_RECORD = 1;
    public static final int USECASE_VIDEO_SNAPSHOT = 2;
    public static final int USECASE_SNAPSHOT = 3;
    public static final int USECASE_ZSL = 4;
    public static final int USECASE_RAW = 5;
    public static final int USECASE_LOW_LATENCY_SNAPSHOT = 6;
    public static final int USECASE_10BIT_OUTPUT = 8;

    private final StreamConfigurationMap mRecommendedMap;
    private final int mUsecase;

    RecommendedStreamConfigurationMap(StreamConfigurationMap recommendedMap, int usecase)
    {
        mRecommendedMap = recommendedMap;
        mUsecase = usecase;
    }

    public int getRecommendedUseCase()
    {
        return mUsecase;
    }

    private static Set<Integer> toFormatSet(int[] formats)
    {
        if (formats == null || formats.length == 0) return null;

        Set<Integer> set = new HashSet<>();
        for (int format : formats)
        {
            set.add(format);
        }
        return Collections.unmodifiableSet(set);
    }

    private static <T> Set<T> toSet(T[] values)
    {
        if (values == null || values.length == 0) return null;
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    public Set<Integer> getOutputFormats()
    {
        return toFormatSet(mRecommendedMap.getOutputFormats());
    }

    public Set<Integer> getValidOutputFormatsForInput(int inputFormat)
    {
        return toFormatSet(mRecommendedMap.getValidOutputFormatsForInput(inputFormat));
    }

    public Set<Integer> getInputFormats()
    {
        return toFormatSet(mRecommendedMap.getInputFormats());
    }

    public Set<Size> getInputSizes(int format)
    {
        return toSet(mRecommendedMap.getInputSizes(format));
    }

    public boolean isOutputSupportedFor(int format)
    {
        return mRecommendedMap.isOutputSupportedFor(format);
    }

    public Set<Size> getOutputSizes(int format)
    {
        return toSet(mRecommendedMap.getOutputSizes(format));
    }

    public Set<Size> getHighSpeedVideoSizes()
    {
        return toSet(mRecommendedMap.getHighSpeedVideoSizes());
    }

    public Set<Range<Integer>> getHighSpeedVideoFpsRangesFor(Size size)
    {
        return toSet(mRecommendedMap.getHighSpeedVideoFpsRangesFor(size));
    }

    public Set<Range<Integer>> getHighSpeedVideoFpsRanges()
    {
        return toSet(mRecommendedMap.getHighSpeedVideoFpsRanges());
    }

    public Set<Size> getHighSpeedVideoSizesFor(Range<Integer> fpsRange)
    {
        return toSet(mRecommendedMap.getHighSpeedVideoSizesFor(fpsRange));
    }

    public Set<Size> getHighResolutionOutputSizes(int format)
    {
        return toSet(mRecommendedMap.getHighResolutionOutputSizes(format));
    }

    public long getOutputMinFrameDuration(int format, Size size)
    {
        return mRecommendedMap.getOutputMinFrameDuration(format, size);
    }

    public long getOutputStallDuration(int format, Size size)
    {
        return mRecommendedMap.getOutputStallDuration(format, size);
    }

    public <T> Set<Size> getOutputSizes(Class<T> klass)
    {
        return toSet(mRecommendedMap.getOutputSizes(klass));
    }

    public <T> long getOutputMinFrameDuration(Class<T> klass, Size size)
    {
        return mRecommendedMap.getOutputMinFrameDuration(klass, size);
    }

    public <T> long getOutputStallDuration(Class<T> klass, Size size)
    {
        return mRecommendedMap.getOutputStallDuration(klass, size);
    }

    public boolean isOutputSupportedFor(Surface surface)
    {
        return mRecommendedMap.isOutputSupportedFor(surface);
    }
}
