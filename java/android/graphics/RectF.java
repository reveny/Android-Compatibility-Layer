package android.graphics;

public class RectF
{
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

    public boolean isEmpty()
    {
        return left >= right || top >= bottom;
    }

    public float width()
    {
        return right - left;
    }

    public float height()
    {
        return bottom - top;
    }

    public float centerX()
    {
        return (left + right) * 0.5f;
    }

    public float centerY()
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

    public void round(Rect out)
    {
        out.set(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
    }

    @Override
    public String toString()
    {
        return "RectF(" + left + ", " + top + " - " + right + ", " + bottom + ")";
    }
}
