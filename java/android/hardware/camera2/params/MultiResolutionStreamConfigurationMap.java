package android.hardware.camera2.params;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MultiResolutionStreamConfigurationMap
{
    private final Map<Integer, List<MultiResolutionStreamInfo>> mMultiResolutionOutputConfigs;
    private final Map<Integer, List<MultiResolutionStreamInfo>> mMultiResolutionInputConfigs;

    MultiResolutionStreamConfigurationMap(Map<Integer, List<MultiResolutionStreamInfo>> outputs,
            Map<Integer, List<MultiResolutionStreamInfo>> inputs)
    {
        mMultiResolutionOutputConfigs = outputs == null ? new HashMap<>() : new HashMap<>(outputs);
        mMultiResolutionInputConfigs = inputs == null ? new HashMap<>() : new HashMap<>(inputs);
    }

    public int[] getOutputFormats()
    {
        return toArray(mMultiResolutionOutputConfigs);
    }

    public int[] getInputFormats()
    {
        return toArray(mMultiResolutionInputConfigs);
    }

    public Collection<MultiResolutionStreamInfo> getOutputInfo(int format)
    {
        List<MultiResolutionStreamInfo> infos = mMultiResolutionOutputConfigs.get(format);
        if (infos == null) throw new IllegalArgumentException("format " + format + " is not supported");
        return Collections.unmodifiableCollection(infos);
    }

    public Collection<MultiResolutionStreamInfo> getInputInfo(int format)
    {
        List<MultiResolutionStreamInfo> infos = mMultiResolutionInputConfigs.get(format);
        if (infos == null) throw new IllegalArgumentException("format " + format + " is not supported");
        return Collections.unmodifiableCollection(infos);
    }

    private static int[] toArray(Map<Integer, List<MultiResolutionStreamInfo>> configs)
    {
        int[] formats = new int[configs.size()];
        int index = 0;
        for (Integer format : configs.keySet())
        {
            formats[index++] = format;
        }
        return formats;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof MultiResolutionStreamConfigurationMap)) return false;

        MultiResolutionStreamConfigurationMap map = (MultiResolutionStreamConfigurationMap)other;
        return mMultiResolutionOutputConfigs.equals(map.mMultiResolutionOutputConfigs)
                && mMultiResolutionInputConfigs.equals(map.mMultiResolutionInputConfigs);
    }

    @Override
    public int hashCode()
    {
        return 31 * mMultiResolutionOutputConfigs.hashCode() + mMultiResolutionInputConfigs.hashCode();
    }

    @Override
    public String toString()
    {
        return "MultiResolutionStreamConfigurationMap(Outputs(" + mMultiResolutionOutputConfigs + "), Inputs("
                + mMultiResolutionInputConfigs + "))";
    }
}
