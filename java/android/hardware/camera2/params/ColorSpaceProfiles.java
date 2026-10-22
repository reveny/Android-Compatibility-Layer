package android.hardware.camera2.params;

import android.graphics.ColorSpace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ColorSpaceProfiles
{
    public static final int UNSPECIFIED = -1;

    private final Map<ColorSpace.Named, Map<Integer, Set<Long>>> mProfileMap = new HashMap<>();

    public ColorSpaceProfiles(long[] elements)
    {
        if ((elements.length % 3) != 0)
        {
            throw new IllegalArgumentException("Color space profile map length " + elements.length + " is not divisible by 3!");
        }

        ColorSpace.Named[] named = ColorSpace.Named.values();
        for (int index = 0; index < elements.length; index += 3)
        {
            int colorSpace = (int)elements[index];
            if (colorSpace < 0 || colorSpace >= named.length) throw new IllegalArgumentException("Unknown color space " + colorSpace);

            int imageFormat = (int)elements[index + 1];
            long dynamicRangeProfileBitmap = elements[index + 2];

            Set<Long> profiles = new HashSet<>();
            for (long profile = DynamicRangeProfiles.STANDARD; profile < DynamicRangeProfiles.PUBLIC_MAX; profile <<= 1)
            {
                if ((dynamicRangeProfileBitmap & profile) != 0) profiles.add(profile);
            }

            Map<Integer, Set<Long>> formats = mProfileMap.get(named[colorSpace]);
            if (formats == null)
            {
                formats = new HashMap<>();
                mProfileMap.put(named[colorSpace], formats);
            }
            Set<Long> existing = formats.get(imageFormat);
            if (existing == null)
            {
                formats.put(imageFormat, profiles);
            }
            else
            {
                existing.addAll(profiles);
            }
        }
    }

    public Set<ColorSpace.Named> getSupportedColorSpaces(int imageFormat)
    {
        Set<ColorSpace.Named> colorSpaces = new HashSet<>();
        for (Map.Entry<ColorSpace.Named, Map<Integer, Set<Long>>> entry : mProfileMap.entrySet())
        {
            if (imageFormat == UNSPECIFIED || entry.getValue().containsKey(imageFormat)) colorSpaces.add(entry.getKey());
        }
        return colorSpaces;
    }

    public Set<Integer> getSupportedImageFormatsForColorSpace(ColorSpace.Named colorSpace)
    {
        Map<Integer, Set<Long>> formats = mProfileMap.get(colorSpace);
        return formats == null ? new HashSet<Integer>() : new HashSet<>(formats.keySet());
    }

    public Set<Long> getSupportedDynamicRangeProfiles(ColorSpace.Named colorSpace, int imageFormat)
    {
        Set<Long> profiles = new HashSet<>();
        Map<Integer, Set<Long>> formats = mProfileMap.get(colorSpace);
        if (formats == null) return profiles;

        for (Map.Entry<Integer, Set<Long>> entry : formats.entrySet())
        {
            if (imageFormat == UNSPECIFIED || entry.getKey() == imageFormat) profiles.addAll(entry.getValue());
        }
        return profiles;
    }

    public Set<ColorSpace.Named> getSupportedColorSpacesForDynamicRange(int imageFormat, long dynamicRangeProfile)
    {
        Set<ColorSpace.Named> colorSpaces = new HashSet<>();
        for (Map.Entry<ColorSpace.Named, Map<Integer, Set<Long>>> entry : mProfileMap.entrySet())
        {
            for (Map.Entry<Integer, Set<Long>> format : entry.getValue().entrySet())
            {
                if ((imageFormat == UNSPECIFIED || format.getKey() == imageFormat) && format.getValue().contains(dynamicRangeProfile))
                {
                    colorSpaces.add(entry.getKey());
                }
            }
        }
        return colorSpaces;
    }
}
