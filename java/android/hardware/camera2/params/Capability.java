package android.hardware.camera2.params;

import android.util.Range;
import android.util.Size;

import java.util.Objects;

public final class Capability
{
    private final int mMode;
    private final Size mMaxStreamingSize;
    private final Range<Float> mZoomRatioRange;

    public Capability(int mode, Size maxStreamingSize, Range<Float> zoomRatioRange)
    {
        if (maxStreamingSize.getWidth() < 0 || maxStreamingSize.getHeight() < 0)
        {
            throw new IllegalArgumentException("maxStreamingSize must be non-negative");
        }
        if (zoomRatioRange.getLower() > zoomRatioRange.getUpper() || zoomRatioRange.getLower() <= 0)
        {
            throw new IllegalArgumentException("zoomRatioRange must be valid");
        }
        mMode = mode;
        mMaxStreamingSize = maxStreamingSize;
        mZoomRatioRange = zoomRatioRange;
    }

    public int getMode()
    {
        return mMode;
    }

    public Size getMaxStreamingSize()
    {
        return mMaxStreamingSize;
    }

    public Range<Float> getZoomRatioRange()
    {
        return mZoomRatioRange;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Capability)) return false;

        Capability capability = (Capability)other;
        return mMode == capability.mMode && mMaxStreamingSize.equals(capability.mMaxStreamingSize)
                && mZoomRatioRange.equals(capability.mZoomRatioRange);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mMode, mMaxStreamingSize, mZoomRatioRange);
    }

    @Override
    public String toString()
    {
        return String.format("(mode:%d, maxStreamingSize:%d x %d, zoomRatio: %f-%f)", mMode, mMaxStreamingSize.getWidth(),
                mMaxStreamingSize.getHeight(), mZoomRatioRange.getLower(), mZoomRatioRange.getUpper());
    }
}
