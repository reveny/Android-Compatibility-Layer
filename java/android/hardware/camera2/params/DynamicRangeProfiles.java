package android.hardware.camera2.params;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DynamicRangeProfiles
{
    public static final long STANDARD = 0x1;
    public static final long HLG10 = 0x2;
    public static final long HDR10 = 0x4;
    public static final long HDR10_PLUS = 0x8;
    public static final long DOLBY_VISION_10B_HDR_REF = 0x10;
    public static final long DOLBY_VISION_10B_HDR_REF_PO = 0x20;
    public static final long DOLBY_VISION_10B_HDR_OEM = 0x40;
    public static final long DOLBY_VISION_10B_HDR_OEM_PO = 0x80;
    public static final long DOLBY_VISION_8B_HDR_REF = 0x100;
    public static final long DOLBY_VISION_8B_HDR_REF_PO = 0x200;
    public static final long DOLBY_VISION_8B_HDR_OEM = 0x400;
    public static final long DOLBY_VISION_8B_HDR_OEM_PO = 0x800;
    public static final long PUBLIC_MAX = 0x1000;

    private final Map<Long, Set<Long>> mProfileMap = new HashMap<>();
    private final Map<Long, Boolean> mLookaheadLatencyMap = new HashMap<>();

    public DynamicRangeProfiles(long[] elements)
    {
        if ((elements.length % 3) != 0)
        {
            throw new IllegalArgumentException("Dynamic range profile map length " + elements.length + " is not a multiple of 3!");
        }

        for (int index = 0; index < elements.length; index += 3)
        {
            checkProfileValue(elements[index]);
            if (elements[index] == STANDARD)
            {
                throw new IllegalArgumentException("Dynamic range profile map must not include a STANDARD profile entry!");
            }

            Set<Long> profiles = new HashSet<>();
            if (elements[index + 1] == 0)
            {
                profiles.add(elements[index]);
            }
            else
            {
                for (long profile = STANDARD; profile < PUBLIC_MAX; profile <<= 1)
                {
                    if ((elements[index + 1] & profile) != 0) profiles.add(profile);
                }
            }

            mProfileMap.put(elements[index], profiles);
            mLookaheadLatencyMap.put(elements[index], elements[index + 2] != 0);
        }

        Set<Long> standardConstraints = new HashSet<>();
        standardConstraints.add(STANDARD);
        for (Long profile : mProfileMap.keySet())
        {
            if (mProfileMap.get(profile).isEmpty() || mProfileMap.get(profile).contains(STANDARD)) standardConstraints.add(profile);
        }
        mProfileMap.put(STANDARD, standardConstraints);
        mLookaheadLatencyMap.put(STANDARD, false);
    }

    private static void checkProfileValue(long profile)
    {
        if (profile == STANDARD || profile == HLG10 || profile == HDR10 || profile == HDR10_PLUS || profile == DOLBY_VISION_10B_HDR_REF
                || profile == DOLBY_VISION_10B_HDR_REF_PO || profile == DOLBY_VISION_10B_HDR_OEM || profile == DOLBY_VISION_10B_HDR_OEM_PO
                || profile == DOLBY_VISION_8B_HDR_REF || profile == DOLBY_VISION_8B_HDR_REF_PO || profile == DOLBY_VISION_8B_HDR_OEM
                || profile == DOLBY_VISION_8B_HDR_OEM_PO)
        {
            return;
        }
        throw new IllegalArgumentException("Unknown profile " + profile);
    }

    public Set<Long> getSupportedProfiles()
    {
        return Collections.unmodifiableSet(mProfileMap.keySet());
    }

    public Set<Long> getProfileCaptureRequestConstraints(long profile)
    {
        Set<Long> constraints = mProfileMap.get(profile);
        if (constraints == null) throw new IllegalArgumentException("Unsupported profile!");
        return Collections.unmodifiableSet(constraints);
    }

    public boolean isExtraLatencyPresent(long profile)
    {
        Boolean latency = mLookaheadLatencyMap.get(profile);
        if (latency == null) throw new IllegalArgumentException("Unsupported profile!");
        return latency;
    }
}
