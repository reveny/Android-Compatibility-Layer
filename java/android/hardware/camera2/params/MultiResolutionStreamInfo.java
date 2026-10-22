package android.hardware.camera2.params;

import java.util.Objects;

public class MultiResolutionStreamInfo
{
    private final int mStreamWidth;
    private final int mStreamHeight;
    private final String mPhysicalCameraId;

    public MultiResolutionStreamInfo(int streamWidth, int streamHeight, String physicalCameraId)
    {
        if (streamWidth <= 0) throw new IllegalArgumentException("Invalid stream width " + streamWidth);
        if (streamHeight <= 0) throw new IllegalArgumentException("Invalid stream height " + streamHeight);
        mStreamWidth = streamWidth;
        mStreamHeight = streamHeight;
        mPhysicalCameraId = physicalCameraId;
    }

    public int getWidth()
    {
        return mStreamWidth;
    }

    public int getHeight()
    {
        return mStreamHeight;
    }

    public String getPhysicalCameraId()
    {
        return mPhysicalCameraId;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof MultiResolutionStreamInfo)) return false;

        MultiResolutionStreamInfo info = (MultiResolutionStreamInfo)other;
        return mStreamWidth == info.mStreamWidth && mStreamHeight == info.mStreamHeight
                && Objects.equals(mPhysicalCameraId, info.mPhysicalCameraId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mStreamWidth, mStreamHeight, mPhysicalCameraId);
    }
}
