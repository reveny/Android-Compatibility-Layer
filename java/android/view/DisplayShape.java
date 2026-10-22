package android.view;

import android.graphics.Path;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class DisplayShape implements Parcelable
{
    public static final Parcelable.Creator<DisplayShape> CREATOR = new Parcelable.Creator<DisplayShape>()
    {
        @Override
        public DisplayShape createFromParcel(Parcel in)
        {
            int width = in.readInt();
            int height = in.readInt();
            float scale = in.readFloat();
            int rotation = in.readInt();
            int offsetX = in.readInt();
            int offsetY = in.readInt();
            return new DisplayShape(width, height, scale, rotation, offsetX, offsetY);
        }

        @Override
        public DisplayShape[] newArray(int size)
        {
            return new DisplayShape[size];
        }
    };

    private final int mDisplayWidth;
    private final int mDisplayHeight;
    private final float mPhysicalPixelDisplaySizeRatio;
    private final int mRotation;
    private final int mOffsetX;
    private final int mOffsetY;

    private DisplayShape(int displayWidth, int displayHeight, float physicalPixelDisplaySizeRatio, int rotation, int offsetX,
                         int offsetY)
    {
        mDisplayWidth = displayWidth;
        mDisplayHeight = displayHeight;
        mPhysicalPixelDisplaySizeRatio = physicalPixelDisplaySizeRatio;
        mRotation = rotation;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
    }

    static DisplayShape rectangle(int width, int height)
    {
        return new DisplayShape(width, height, 1.0f, Surface.ROTATION_0, 0, 0);
    }

    DisplayShape offset(int dx, int dy)
    {
        return new DisplayShape(mDisplayWidth, mDisplayHeight, mPhysicalPixelDisplaySizeRatio, mRotation, mOffsetX + dx, mOffsetY + dy);
    }

    public Path getPath()
    {
        Path path = new Path();
        path.addRect(mOffsetX, mOffsetY, mOffsetX + mDisplayWidth * mPhysicalPixelDisplaySizeRatio,
                     mOffsetY + mDisplayHeight * mPhysicalPixelDisplaySizeRatio, Path.Direction.CW);
        return path;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof DisplayShape)) return false;

        DisplayShape shape = (DisplayShape)other;
        return mDisplayWidth == shape.mDisplayWidth && mDisplayHeight == shape.mDisplayHeight
               && mPhysicalPixelDisplaySizeRatio == shape.mPhysicalPixelDisplaySizeRatio && mRotation == shape.mRotation
               && mOffsetX == shape.mOffsetX && mOffsetY == shape.mOffsetY;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mDisplayWidth, mDisplayHeight, mPhysicalPixelDisplaySizeRatio, mRotation, mOffsetX, mOffsetY);
    }

    @Override
    public String toString()
    {
        return "DisplayShape{ spec=rect displayWidth=" + mDisplayWidth + " displayHeight=" + mDisplayHeight
               + " physicalPixelDisplaySizeRatio=" + mPhysicalPixelDisplaySizeRatio + " rotation=" + mRotation + " offsetX=" + mOffsetX
               + " offsetY=" + mOffsetY + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mDisplayWidth);
        dest.writeInt(mDisplayHeight);
        dest.writeFloat(mPhysicalPixelDisplaySizeRatio);
        dest.writeInt(mRotation);
        dest.writeInt(mOffsetX);
        dest.writeInt(mOffsetY);
    }
}
