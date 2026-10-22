package android.view;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

public final class RoundedCorner implements Parcelable
{
    public static final int POSITION_TOP_LEFT = 0;
    public static final int POSITION_TOP_RIGHT = 1;
    public static final int POSITION_BOTTOM_RIGHT = 2;
    public static final int POSITION_BOTTOM_LEFT = 3;

    public static final Parcelable.Creator<RoundedCorner> CREATOR = new Parcelable.Creator<RoundedCorner>()
    {
        @Override
        public RoundedCorner createFromParcel(Parcel source)
        {
            return new RoundedCorner(source.readInt(), source.readInt(), source.readInt(), source.readInt());
        }

        @Override
        public RoundedCorner[] newArray(int size)
        {
            return new RoundedCorner[size];
        }
    };

    private final int mPosition;
    private final int mRadius;
    private final Point mCenter;

    public RoundedCorner(int position, int radius, int centerX, int centerY)
    {
        mPosition = position;
        mRadius = radius > 0 ? radius : 0;
        mCenter = new Point(radius > 0 ? centerX : 0, radius > 0 ? centerY : 0);
    }

    public int getPosition()
    {
        return mPosition;
    }

    public int getRadius()
    {
        return mRadius;
    }

    public Point getCenter()
    {
        return new Point(mCenter.x, mCenter.y);
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof RoundedCorner)) return false;

        RoundedCorner corner = (RoundedCorner)other;
        return mPosition == corner.mPosition && mRadius == corner.mRadius && mCenter.x == corner.mCenter.x && mCenter.y == corner.mCenter.y;
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        result = 31 * result + mPosition;
        result = 31 * result + mRadius;
        result = 31 * result + mCenter.x;
        result = 31 * result + mCenter.y;
        return result;
    }

    @Override
    public String toString()
    {
        return "RoundedCorner{position=" + mPosition + ", radius=" + mRadius + ", center=" + mCenter + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mPosition);
        dest.writeInt(mRadius);
        dest.writeInt(mCenter.x);
        dest.writeInt(mCenter.y);
    }
}
