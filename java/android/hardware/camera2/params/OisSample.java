package android.hardware.camera2.params;

import java.util.Objects;

public final class OisSample
{
    private final long mTimestampNs;
    private final float mXShift;
    private final float mYShift;

    public OisSample(long timestamp, float xShift, float yShift)
    {
        if (Float.isNaN(xShift) || Float.isInfinite(xShift)) throw new IllegalArgumentException("xShift must be finite");
        if (Float.isNaN(yShift) || Float.isInfinite(yShift)) throw new IllegalArgumentException("yShift must be finite");
        mTimestampNs = timestamp;
        mXShift = xShift;
        mYShift = yShift;
    }

    public long getTimestamp()
    {
        return mTimestampNs;
    }

    public float getXshift()
    {
        return mXShift;
    }

    public float getYshift()
    {
        return mYShift;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof OisSample)) return false;

        OisSample sample = (OisSample)other;
        return mTimestampNs == sample.mTimestampNs && mXShift == sample.mXShift && mYShift == sample.mYShift;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mTimestampNs, mXShift, mYShift);
    }

    @Override
    public String toString()
    {
        return String.format("OisSample{timestamp:%d, shift_x:%f, shift_y:%f}", mTimestampNs, mXShift, mYShift);
    }
}
