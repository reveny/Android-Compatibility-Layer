package android.hardware.camera2.params;

import java.util.Arrays;
import java.util.Objects;

public final class LensIntrinsicsSample
{
    private final long mTimestampNs;
    private final float[] mLensIntrinsics;

    public LensIntrinsicsSample(long timestampNs, float[] lensIntrinsics)
    {
        if (lensIntrinsics == null) throw new NullPointerException("lensIntrinsics must not be null");
        if (lensIntrinsics.length != 5) throw new IllegalArgumentException("lensIntrinsics must contain 5 elements");
        mTimestampNs = timestampNs;
        mLensIntrinsics = Arrays.copyOf(lensIntrinsics, lensIntrinsics.length);
    }

    public long getTimestampNanos()
    {
        return mTimestampNs;
    }

    public float[] getLensIntrinsics()
    {
        return mLensIntrinsics;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof LensIntrinsicsSample)) return false;

        LensIntrinsicsSample sample = (LensIntrinsicsSample)other;
        return mTimestampNs == sample.mTimestampNs && Arrays.equals(mLensIntrinsics, sample.mLensIntrinsics);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mTimestampNs, Arrays.hashCode(mLensIntrinsics));
    }

    @Override
    public String toString()
    {
        return "LensIntrinsicsSample{timestamp:" + mTimestampNs + ", sample:" + Arrays.toString(mLensIntrinsics) + "}";
    }
}
