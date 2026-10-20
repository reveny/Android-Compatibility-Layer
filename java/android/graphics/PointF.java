package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public class PointF implements Parcelable
{
    public float x;
    public float y;

    public PointF()
    {
    }

    public PointF(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    public PointF(Point point)
    {
        if (point == null) return;

        x = point.x;
        y = point.y;
    }

    public PointF(PointF point)
    {
        if (point == null) return;

        x = point.x;
        y = point.y;
    }

    public final void set(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    public final void set(PointF point)
    {
        if (point == null) return;

        x = point.x;
        y = point.y;
    }

    public final void negate()
    {
        x = -x;
        y = -y;
    }

    public final void offset(float dx, float dy)
    {
        x += dx;
        y += dy;
    }

    public final boolean equals(float x, float y)
    {
        return this.x == x && this.y == y;
    }

    public final float length()
    {
        return length(x, y);
    }

    public static float length(float x, float y)
    {
        return (float)Math.hypot(x, y);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PointF)) return false;

        PointF point = (PointF)other;

        return Float.compare(x, point.x) == 0 && Float.compare(y, point.y) == 0;
    }

    @Override
    public int hashCode()
    {
        return Float.floatToIntBits(x) * 31 + Float.floatToIntBits(y);
    }

    @Override
    public String toString()
    {
        return "PointF(" + x + ", " + y + ")";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeValue(Float.valueOf(x));
        parcel.writeValue(Float.valueOf(y));
    }

    public void readFromParcel(Parcel parcel)
    {
        Object first = parcel.readValue(null);
        Object second = parcel.readValue(null);

        if (first instanceof Float) x = ((Float)first).floatValue();
        if (second instanceof Float) y = ((Float)second).floatValue();
    }

    public static final Creator<PointF> CREATOR = new Creator<PointF>()
    {
        @Override
        public PointF createFromParcel(Parcel parcel)
        {
            PointF point = new PointF();
            point.readFromParcel(parcel);

            return point;
        }

        @Override
        public PointF[] newArray(int size)
        {
            return new PointF[size];
        }
    };
}
