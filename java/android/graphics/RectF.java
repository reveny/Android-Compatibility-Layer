package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public class RectF implements Parcelable
{
    public static final Parcelable.Creator<RectF> CREATOR = new Parcelable.Creator<RectF>()
    {
        @Override
        public RectF createFromParcel(Parcel source)
        {
            RectF rect = new RectF();
            rect.readFromParcel(source);
            return rect;
        }

        @Override
        public RectF[] newArray(int size)
        {
            return new RectF[size];
        }
    };

    public float left;
    public float top;
    public float right;
    public float bottom;

    public RectF()
    {
    }

    public RectF(float left, float top, float right, float bottom)
    {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public RectF(RectF source)
    {
        if (source != null) set(source);
    }

    public RectF(Rect source)
    {
        if (source != null) set(source);
    }

    public void set(float left, float top, float right, float bottom)
    {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void set(RectF source)
    {
        set(source.left, source.top, source.right, source.bottom);
    }

    public void set(Rect source)
    {
        set(source.left, source.top, source.right, source.bottom);
    }

    public void setEmpty()
    {
        left = 0;
        top = 0;
        right = 0;
        bottom = 0;
    }

    public final boolean isEmpty()
    {
        return left >= right || top >= bottom;
    }

    public final float width()
    {
        return right - left;
    }

    public final float height()
    {
        return bottom - top;
    }

    public final float centerX()
    {
        return (left + right) * 0.5f;
    }

    public final float centerY()
    {
        return (top + bottom) * 0.5f;
    }

    public void offset(float dx, float dy)
    {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }

    public void offsetTo(float newLeft, float newTop)
    {
        right += newLeft - left;
        bottom += newTop - top;
        left = newLeft;
        top = newTop;
    }

    public void inset(float dx, float dy)
    {
        left += dx;
        top += dy;
        right -= dx;
        bottom -= dy;
    }

    public boolean contains(float x, float y)
    {
        return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom;
    }

    public boolean contains(float otherLeft, float otherTop, float otherRight, float otherBottom)
    {
        return left < right && top < bottom && left <= otherLeft && top <= otherTop && right >= otherRight && bottom >= otherBottom;
    }

    public boolean contains(RectF other)
    {
        return contains(other.left, other.top, other.right, other.bottom);
    }

    public boolean intersect(float otherLeft, float otherTop, float otherRight, float otherBottom)
    {
        if (left >= otherRight || otherLeft >= right || top >= otherBottom || otherTop >= bottom) return false;

        if (left < otherLeft) left = otherLeft;
        if (top < otherTop) top = otherTop;
        if (right > otherRight) right = otherRight;
        if (bottom > otherBottom) bottom = otherBottom;
        return true;
    }

    public boolean intersect(RectF other)
    {
        return intersect(other.left, other.top, other.right, other.bottom);
    }

    public boolean setIntersect(RectF first, RectF second)
    {
        if (first.left >= second.right || second.left >= first.right || first.top >= second.bottom || second.top >= first.bottom)
            return false;

        left = Math.max(first.left, second.left);
        top = Math.max(first.top, second.top);
        right = Math.min(first.right, second.right);
        bottom = Math.min(first.bottom, second.bottom);
        return true;
    }

    public boolean intersects(float otherLeft, float otherTop, float otherRight, float otherBottom)
    {
        return left < otherRight && otherLeft < right && top < otherBottom && otherTop < bottom;
    }

    public static boolean intersects(RectF first, RectF second)
    {
        return first.left < second.right && second.left < first.right && first.top < second.bottom && second.top < first.bottom;
    }

    public void round(Rect out)
    {
        out.set((int)Math.floor(left + 0.5f), (int)Math.floor(top + 0.5f), (int)Math.floor(right + 0.5f), (int)Math.floor(bottom + 0.5f));
    }

    public void roundOut(Rect out)
    {
        out.set((int)Math.floor(left), (int)Math.floor(top), (int)Math.ceil(right), (int)Math.ceil(bottom));
    }

    public void union(float otherLeft, float otherTop, float otherRight, float otherBottom)
    {
        if (otherLeft >= otherRight || otherTop >= otherBottom) return;

        if (left < right && top < bottom)
        {
            if (left > otherLeft) left = otherLeft;
            if (top > otherTop) top = otherTop;
            if (right < otherRight) right = otherRight;
            if (bottom < otherBottom) bottom = otherBottom;
            return;
        }

        set(otherLeft, otherTop, otherRight, otherBottom);
    }

    public void union(RectF other)
    {
        union(other.left, other.top, other.right, other.bottom);
    }

    public void union(float x, float y)
    {
        if (x < left) left = x;
        else if (x > right) right = x;

        if (y < top) top = y;
        else if (y > bottom) bottom = y;
    }

    public void sort()
    {
        if (left > right)
        {
            float swap = left;
            left = right;
            right = swap;
        }

        if (top > bottom)
        {
            float swap = top;
            top = bottom;
            bottom = swap;
        }
    }

    public String toShortString()
    {
        return "[" + left + "," + top + "][" + right + "," + bottom + "]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeFloat(left);
        out.writeFloat(top);
        out.writeFloat(right);
        out.writeFloat(bottom);
    }

    public void readFromParcel(Parcel in)
    {
        left = in.readFloat();
        top = in.readFloat();
        right = in.readFloat();
        bottom = in.readFloat();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof RectF)) return false;

        RectF that = (RectF)other;
        return left == that.left && top == that.top && right == that.right && bottom == that.bottom;
    }

    @Override
    public int hashCode()
    {
        int result = Float.floatToIntBits(left);
        result = 31 * result + Float.floatToIntBits(top);
        result = 31 * result + Float.floatToIntBits(right);
        return 31 * result + Float.floatToIntBits(bottom);
    }

    @Override
    public String toString()
    {
        return "RectF(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}
