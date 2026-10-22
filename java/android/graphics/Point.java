package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public class Point implements Parcelable
{
    public static final Parcelable.Creator<Point> CREATOR = new Parcelable.Creator<Point>()
    {
        @Override
        public Point createFromParcel(Parcel source)
        {
            Point point = new Point();
            point.readFromParcel(source);
            return point;
        }

        @Override
        public Point[] newArray(int size)
        {
            return new Point[size];
        }
    };

    public int x;
    public int y;

    public Point()
    {
    }

    public Point(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Point(Point source)
    {
        x = source.x;
        y = source.y;
    }

    public void set(int newX, int newY)
    {
        x = newX;
        y = newY;
    }

    public final void negate()
    {
        x = -x;
        y = -y;
    }

    public final void offset(int deltaX, int deltaY)
    {
        x += deltaX;
        y += deltaY;
    }

    public final boolean equals(int otherX, int otherY)
    {
        return x == otherX && y == otherY;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Point)) return false;

        Point point = (Point)other;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode()
    {
        return 31 * x + y;
    }

    @Override
    public String toString()
    {
        return "Point(" + x + ", " + y + ")";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(x);
        out.writeInt(y);
    }

    public void readFromParcel(Parcel in)
    {
        x = in.readInt();
        y = in.readInt();
    }
}
