package android.hardware.camera2.params;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class InputConfiguration
{
    private final int mWidth;
    private final int mHeight;
    private final int mFormat;
    private final boolean mIsMultiResolution;
    private final List<MultiResolutionStreamInfo> mMultiResolutionInputs = new ArrayList<>();

    public InputConfiguration(int width, int height, int format)
    {
        mWidth = width;
        mHeight = height;
        mFormat = format;
        mIsMultiResolution = false;
    }

    public InputConfiguration(Collection<MultiResolutionStreamInfo> multiResolutionInputs, int format)
    {
        if (multiResolutionInputs == null || multiResolutionInputs.isEmpty())
        {
            throw new IllegalArgumentException("Input multi-resolution stream info must not be empty");
        }

        int maxWidth = 0;
        int maxHeight = 0;
        for (MultiResolutionStreamInfo info : multiResolutionInputs)
        {
            if (mMultiResolutionInputs.contains(info)) throw new IllegalArgumentException("Duplicate stream info");
            mMultiResolutionInputs.add(info);
            if ((long)info.getWidth() * info.getHeight() > (long)maxWidth * maxHeight)
            {
                maxWidth = info.getWidth();
                maxHeight = info.getHeight();
            }
        }
        mWidth = maxWidth;
        mHeight = maxHeight;
        mFormat = format;
        mIsMultiResolution = true;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public int getFormat()
    {
        return mFormat;
    }

    public boolean isMultiResolution()
    {
        return mIsMultiResolution;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof InputConfiguration)) return false;

        InputConfiguration configuration = (InputConfiguration)other;
        return configuration.mWidth == mWidth && configuration.mHeight == mHeight && configuration.mFormat == mFormat
                && configuration.mIsMultiResolution == mIsMultiResolution
                && configuration.mMultiResolutionInputs.equals(mMultiResolutionInputs);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mWidth, mHeight, mFormat, mIsMultiResolution);
    }

    @Override
    public String toString()
    {
        return String.format("InputConfiguration(w:%d, h:%d, format:%d, isMultiResolution %b)", mWidth, mHeight, mFormat,
                mIsMultiResolution);
    }
}
