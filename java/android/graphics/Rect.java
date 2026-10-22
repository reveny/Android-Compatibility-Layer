package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;

public final class Rect implements Parcelable
{
    public static final Parcelable.Creator<Rect> CREATOR = new Parcelable.Creator<Rect>()
    {
        @Override
        public Rect createFromParcel(Parcel source)
        {
            Rect rect = new Rect();
            rect.readFromParcel(source);
            return rect;
        }

        @Override
        public Rect[] newArray(int size)
        {
            return new Rect[size];
        }
    };

    public int left;
    public int top;
    public int right;
    public int bottom;

    public Rect()
    {
    }

    public Rect(int left, int top, int right, int bottom)
    {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public Rect(Rect source)
    {
        if (source != null) set(source);
    }

    public void set(int left, int top, int right, int bottom)
    {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
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

    public boolean isEmpty()
    {
        return left >= right || top >= bottom;
    }

    public int width()
    {
        return right - left;
    }

    public int height()
    {
        return bottom - top;
    }

    public int centerX()
    {
        return (left + right) >> 1;
    }

    public int centerY()
    {
        return (top + bottom) >> 1;
    }

    public float exactCenterX()
    {
        return (left + right) * 0.5f;
    }

    public float exactCenterY()
    {
        return (top + bottom) * 0.5f;
    }

    public void offset(int dx, int dy)
    {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }

    public void offsetTo(int newLeft, int newTop)
    {
        right += newLeft - left;
        bottom += newTop - top;
        left = newLeft;
        top = newTop;
    }

    public void inset(int dx, int dy)
    {
        left += dx;
        top += dy;
        right -= dx;
        bottom -= dy;
    }

    public void inset(Insets insets)
    {
        inset(insets.left, insets.top, insets.right, insets.bottom);
    }

    public void inset(int insetLeft, int insetTop, int insetRight, int insetBottom)
    {
        left += insetLeft;
        top += insetTop;
        right -= insetRight;
        bottom -= insetBottom;
    }

    public boolean contains(int x, int y)
    {
        return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom;
    }

    public boolean contains(int otherLeft, int otherTop, int otherRight, int otherBottom)
    {
        return left < right && top < bottom && left <= otherLeft && top <= otherTop && right >= otherRight && bottom >= otherBottom;
    }

    public boolean contains(Rect other)
    {
        return contains(other.left, other.top, other.right, other.bottom);
    }

    public boolean intersect(int otherLeft, int otherTop, int otherRight, int otherBottom)
    {
        if (left >= otherRight || otherLeft >= right || top >= otherBottom || otherTop >= bottom) return false;

        if (left < otherLeft) left = otherLeft;
        if (top < otherTop) top = otherTop;
        if (right > otherRight) right = otherRight;
        if (bottom > otherBottom) bottom = otherBottom;
        return true;
    }

    public boolean intersect(Rect other)
    {
        return intersect(other.left, other.top, other.right, other.bottom);
    }

    public boolean setIntersect(Rect first, Rect second)
    {
        if (first.left >= second.right || second.left >= first.right || first.top >= second.bottom || second.top >= first.bottom)
            return false;

        left = Math.max(first.left, second.left);
        top = Math.max(first.top, second.top);
        right = Math.min(first.right, second.right);
        bottom = Math.min(first.bottom, second.bottom);
        return true;
    }

    public boolean intersects(int otherLeft, int otherTop, int otherRight, int otherBottom)
    {
        return left < otherRight && otherLeft < right && top < otherBottom && otherTop < bottom;
    }

    public static boolean intersects(Rect first, Rect second)
    {
        return first.left < second.right && second.left < first.right && first.top < second.bottom && second.top < first.bottom;
    }

    public void union(int otherLeft, int otherTop, int otherRight, int otherBottom)
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

    public void union(Rect other)
    {
        union(other.left, other.top, other.right, other.bottom);
    }

    public void union(int x, int y)
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
            int swap = left;
            left = right;
            right = swap;
        }

        if (top > bottom)
        {
            int swap = top;
            top = bottom;
            bottom = swap;
        }
    }

    public String toShortString()
    {
        return "[" + left + "," + top + "][" + right + "," + bottom + "]";
    }

    public String flattenToString()
    {
        return left + " " + top + " " + right + " " + bottom;
    }

    public static Rect unflattenFromString(String text)
    {
        if (text == null) return null;

        String[] parts = text.trim().split(" ");
        if (parts.length != 4) return null;

        try
        {
            return new Rect(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        }
        catch (NumberFormatException malformed)
        {
            return null;
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(left);
        out.writeInt(top);
        out.writeInt(right);
        out.writeInt(bottom);
    }

    public void readFromParcel(Parcel in)
    {
        left = in.readInt();
        top = in.readInt();
        right = in.readInt();
        bottom = in.readInt();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Rect)) return false;

        Rect that = (Rect)other;
        return left == that.left && top == that.top && right == that.right && bottom == that.bottom;
    }

    @Override
    public int hashCode()
    {
        return ((left * 31 + top) * 31 + right) * 31 + bottom;
    }

    @Override
    public String toString()
    {
        return "Rect(" + left + ", " + top + " - " + right + ", " + bottom + ")";
    }
}
