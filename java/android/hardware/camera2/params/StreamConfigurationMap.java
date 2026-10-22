package android.hardware.camera2.params;

import android.util.Range;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StreamConfigurationMap
{
    private static final String[] SUPPORTED_OUTPUT_CLASSES = {
        "android.media.ImageReader",
        "android.media.MediaRecorder",
        "android.media.MediaCodec",
        "android.renderscript.Allocation",
        "android.view.SurfaceHolder",
        "android.graphics.SurfaceTexture"
    };

    private final Map<Integer, List<Size>> mOutputSizes;
    private final Map<Integer, List<Size>> mInputSizes;
    private final Map<Integer, int[]> mInputOutputFormats;
    private final Map<Size, List<Range<Integer>>> mHighSpeedVideoConfigurations;

    StreamConfigurationMap()
    {
        mOutputSizes = new LinkedHashMap<>();
        mInputSizes = new LinkedHashMap<>();
        mInputOutputFormats = new LinkedHashMap<>();
        mHighSpeedVideoConfigurations = new LinkedHashMap<>();
    }

    public int[] getOutputFormats()
    {
        return toFormats(mOutputSizes);
    }

    public int[] getValidOutputFormatsForInput(int inputFormat)
    {
        int[] formats = mInputOutputFormats.get(inputFormat);
        return formats == null ? new int[0] : Arrays.copyOf(formats, formats.length);
    }

    public int[] getInputFormats()
    {
        return toFormats(mInputSizes);
    }

    public Size[] getInputSizes(int format)
    {
        List<Size> sizes = mInputSizes.get(format);
        return sizes == null ? null : sizes.toArray(new Size[0]);
    }

    public boolean isOutputSupportedFor(int format)
    {
        return mOutputSizes.containsKey(format);
    }

    public static <T> boolean isOutputSupportedFor(Class<T> klass)
    {
        if (klass == null) throw new NullPointerException("klass must not be null");

        for (Class<?> current = klass; current != null; current = current.getSuperclass())
        {
            if (isSupportedClassName(current.getName())) return true;
            for (Class<?> implemented : current.getInterfaces())
            {
                if (isSupportedClassName(implemented.getName())) return true;
            }
        }
        return false;
    }

    private static boolean isSupportedClassName(String name)
    {
        for (String supported : SUPPORTED_OUTPUT_CLASSES)
        {
            if (supported.equals(name)) return true;
        }
        return false;
    }

    public boolean isOutputSupportedFor(Surface surface)
    {
        if (surface == null) throw new NullPointerException("surface must not be null");
        return false;
    }

    public Size[] getHighSpeedVideoSizes()
    {
        return mHighSpeedVideoConfigurations.keySet().toArray(new Size[0]);
    }

    @SuppressWarnings("unchecked")
    public Range<Integer>[] getHighSpeedVideoFpsRangesFor(Size size)
    {
        List<Range<Integer>> ranges = mHighSpeedVideoConfigurations.get(size);
        if (ranges == null) throw new IllegalArgumentException(String.format("Size %s does not support high speed video recording", size));
        return ranges.toArray(new Range[0]);
    }

    @SuppressWarnings("unchecked")
    public Range<Integer>[] getHighSpeedVideoFpsRanges()
    {
        List<Range<Integer>> ranges = new ArrayList<>();
        for (List<Range<Integer>> sizeRanges : mHighSpeedVideoConfigurations.values())
        {
            for (Range<Integer> range : sizeRanges)
            {
                if (!ranges.contains(range)) ranges.add(range);
            }
        }
        return ranges.toArray(new Range[0]);
    }

    public Size[] getHighSpeedVideoSizesFor(Range<Integer> fpsRange)
    {
        List<Size> sizes = new ArrayList<>();
        for (Map.Entry<Size, List<Range<Integer>>> entry : mHighSpeedVideoConfigurations.entrySet())
        {
            if (entry.getValue().contains(fpsRange)) sizes.add(entry.getKey());
        }
        if (sizes.isEmpty())
        {
            throw new IllegalArgumentException(String.format("FpsRange %s does not support high speed video recording", fpsRange));
        }
        return sizes.toArray(new Size[0]);
    }

    public Size[] getHighResolutionOutputSizes(int format)
    {
        return mOutputSizes.containsKey(format) ? new Size[0] : null;
    }

    public long getOutputMinFrameDuration(int format, Size size)
    {
        checkOutput(format, size);
        return 0;
    }

    public <T> long getOutputMinFrameDuration(Class<T> klass, Size size)
    {
        if (!isOutputSupportedFor(klass)) throw new IllegalArgumentException("klass was not supported");
        throw new IllegalArgumentException("size was not supported");
    }

    public long getOutputStallDuration(int format, Size size)
    {
        checkOutput(format, size);
        return 0;
    }

    public <T> long getOutputStallDuration(Class<T> klass, Size size)
    {
        if (!isOutputSupportedFor(klass)) throw new IllegalArgumentException("klass was not supported");
        throw new IllegalArgumentException("size was not supported");
    }

    public <T> Size[] getOutputSizes(Class<T> klass)
    {
        if (!isOutputSupportedFor(klass)) return null;
        return new Size[0];
    }

    public Size[] getOutputSizes(int format)
    {
        List<Size> sizes = mOutputSizes.get(format);
        return sizes == null ? null : sizes.toArray(new Size[0]);
    }

    private void checkOutput(int format, Size size)
    {
        if (size == null) throw new NullPointerException("size must not be null");
        List<Size> sizes = mOutputSizes.get(format);
        if (sizes == null)
        {
            throw new IllegalArgumentException(String.format("format %x is not supported by this stream configuration map", format));
        }
        if (!sizes.contains(size)) throw new IllegalArgumentException("size was not supported");
    }

    private static int[] toFormats(Map<Integer, List<Size>> sizes)
    {
        int[] formats = new int[sizes.size()];
        int index = 0;
        for (Integer format : sizes.keySet())
        {
            formats[index++] = format;
        }
        return formats;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof StreamConfigurationMap)) return false;

        StreamConfigurationMap map = (StreamConfigurationMap)other;
        return mOutputSizes.equals(map.mOutputSizes) && mInputSizes.equals(map.mInputSizes)
                && mHighSpeedVideoConfigurations.equals(map.mHighSpeedVideoConfigurations);
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 * mOutputSizes.hashCode() + mInputSizes.hashCode()) + mHighSpeedVideoConfigurations.hashCode();
    }

    @Override
    public String toString()
    {
        return "StreamConfiguration(Outputs(" + mOutputSizes + "), Inputs(" + mInputSizes + "), ValidOutputFormatsForInput("
                + mInputOutputFormats.keySet() + "), HighSpeedVideoConfigurations(" + mHighSpeedVideoConfigurations + "))";
    }
}
