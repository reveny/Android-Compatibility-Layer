package android.view.inspector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class IntFlagMapping
{
    private final ArrayList<Flag> mFlags = new ArrayList<>();

    public IntFlagMapping()
    {
    }

    public Set<String> get(int value)
    {
        HashSet<String> enabledFlagNames = new HashSet<>(mFlags.size());

        for (Flag flag : mFlags)
        {
            if ((value & flag.mMask) == flag.mTarget) enabledFlagNames.add(flag.mName);
        }

        return enabledFlagNames;
    }

    public void add(int mask, int target, String name)
    {
        mFlags.add(new Flag(mask, target, name));
    }

    private static final class Flag
    {
        final int mMask;
        final int mTarget;
        final String mName;

        Flag(int mask, int target, String name)
        {
            mTarget = target;
            mMask = mask;
            mName = name;
        }
    }
}
