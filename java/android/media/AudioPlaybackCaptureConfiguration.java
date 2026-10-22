package android.media;

import android.media.projection.MediaProjection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AudioPlaybackCaptureConfiguration
{
    private final MediaProjection mProjection;
    private final int[] mMatchingUsages;
    private final int[] mExcludeUsages;
    private final int[] mMatchingUids;
    private final int[] mExcludeUids;

    private AudioPlaybackCaptureConfiguration(MediaProjection projection, int[] matchingUsages, int[] excludeUsages,
        int[] matchingUids, int[] excludeUids)
    {
        mProjection = projection;
        mMatchingUsages = matchingUsages;
        mExcludeUsages = excludeUsages;
        mMatchingUids = matchingUids;
        mExcludeUids = excludeUids;
    }

    public int[] getExcludeUids()
    {
        return mExcludeUids.clone();
    }

    public int[] getExcludeUsages()
    {
        return mExcludeUsages.clone();
    }

    public int[] getMatchingUids()
    {
        return mMatchingUids.clone();
    }

    public int[] getMatchingUsages()
    {
        return mMatchingUsages.clone();
    }

    public MediaProjection getMediaProjection()
    {
        return mProjection;
    }

    public static final class Builder
    {
        private static final int RULE_NONE = 0;
        private static final int RULE_MATCH = 1;
        private static final int RULE_EXCLUDE = 2;

        private final MediaProjection mProjection;
        private final List<Integer> mMatchingUsages = new ArrayList<>();
        private final List<Integer> mExcludeUsages = new ArrayList<>();
        private final List<Integer> mMatchingUids = new ArrayList<>();
        private final List<Integer> mExcludeUids = new ArrayList<>();
        private int mUsageRule = RULE_NONE;
        private int mUidRule = RULE_NONE;

        public Builder(MediaProjection projection)
        {
            mProjection = Objects.requireNonNull(projection);
        }

        public Builder addMatchingUid(int uid)
        {
            if (mUidRule == RULE_EXCLUDE) throw new IllegalStateException("Inclusive and exclusive uid rules cannot be combined");

            mUidRule = RULE_MATCH;
            if (!mMatchingUids.contains(uid)) mMatchingUids.add(uid);
            return this;
        }

        public Builder addMatchingUsage(int usage)
        {
            if (mUsageRule == RULE_EXCLUDE)
            {
                throw new IllegalStateException("Inclusive and exclusive usage rules cannot be combined");
            }

            if (!AudioAttributes.isValidUsage(usage)) throw new IllegalArgumentException("Invalid usage " + usage);

            mUsageRule = RULE_MATCH;
            if (!mMatchingUsages.contains(usage)) mMatchingUsages.add(usage);
            return this;
        }

        public AudioPlaybackCaptureConfiguration build()
        {
            return new AudioPlaybackCaptureConfiguration(mProjection, toArray(mMatchingUsages), toArray(mExcludeUsages),
                toArray(mMatchingUids), toArray(mExcludeUids));
        }

        public Builder excludeUid(int uid)
        {
            if (mUidRule == RULE_MATCH) throw new IllegalStateException("Inclusive and exclusive uid rules cannot be combined");

            mUidRule = RULE_EXCLUDE;
            if (!mExcludeUids.contains(uid)) mExcludeUids.add(uid);
            return this;
        }

        public Builder excludeUsage(int usage)
        {
            if (mUsageRule == RULE_MATCH)
            {
                throw new IllegalStateException("Inclusive and exclusive usage rules cannot be combined");
            }

            if (!AudioAttributes.isValidUsage(usage)) throw new IllegalArgumentException("Invalid usage " + usage);

            mUsageRule = RULE_EXCLUDE;
            if (!mExcludeUsages.contains(usage)) mExcludeUsages.add(usage);
            return this;
        }

        private static int[] toArray(List<Integer> values)
        {
            int[] result = new int[values.size()];

            for (int index = 0; index < result.length; index++)
            {
                result[index] = values.get(index);
            }

            return result;
        }
    }
}
