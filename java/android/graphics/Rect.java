package android.graphics;

public class Rect
{
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
        if (source == null) return;
        set(source);
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

    public boolean contains(int x, int y)
    {
        return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom;
    }

    public void offset(int dx, int dy)
    {
        left += dx;
        top += dy;
        right += dx;
        bottom += dy;
    }

    public void inset(int dx, int dy)
    {
        left += dx;
        top += dy;
        right -= dx;
        bottom -= dy;
    }

    public boolean intersect(Rect other)
    {
        int newLeft = Math.max(left, other.left);
        int newTop = Math.max(top, other.top);
        int newRight = Math.min(right, other.right);
        int newBottom = Math.min(bottom, other.bottom);

        if (newLeft >= newRight || newTop >= newBottom) return false;

        set(newLeft, newTop, newRight, newBottom);
        return true;
    }

    @Override
    public boolean equals(Object other)
    {
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
