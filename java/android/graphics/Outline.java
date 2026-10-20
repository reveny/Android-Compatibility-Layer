package android.graphics;

public final class Outline
{
    public final Rect mRect = new Rect();
    public float mRadius;
    public float mAlpha = 1.0f;

    private boolean mEmpty = true;

    public Outline()
    {
    }

    public Outline(Outline source)
    {
        set(source);
    }

    public void set(Outline source)
    {
        if (source == null) return;

        mRect.set(source.mRect);
        mRadius = source.mRadius;
        mAlpha = source.mAlpha;
        mEmpty = source.mEmpty;
    }

    public void setEmpty()
    {
        mEmpty = true;
        mRect.setEmpty();
        mRadius = 0;
    }

    public boolean isEmpty()
    {
        return mEmpty;
    }

    public void setRect(int left, int top, int right, int bottom)
    {
        setRoundRect(left, top, right, bottom, 0);
    }

    public void setRect(Rect rect)
    {
        if (rect == null) setEmpty();
        else setRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setRoundRect(int left, int top, int right, int bottom, float radius)
    {
        mRect.set(left, top, right, bottom);
        mRadius = radius;
        mEmpty = right <= left || bottom <= top;
    }

    public void setRoundRect(Rect rect, float radius)
    {
        if (rect == null) setEmpty();
        else setRoundRect(rect.left, rect.top, rect.right, rect.bottom, radius);
    }

    public void setOval(int left, int top, int right, int bottom)
    {
        setRoundRect(left, top, right, bottom, Math.min(right - left, bottom - top) / 2.0f);
    }

    public void setOval(Rect rect)
    {
        if (rect == null) setEmpty();
        else setOval(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setPath(Object path)
    {
        setEmpty();
    }

    public void setConvexPath(Object path)
    {
        setPath(path);
    }

    public void setAlpha(float alpha)
    {
        mAlpha = alpha;
    }

    public float getAlpha()
    {
        return mAlpha;
    }

    public float getRadius()
    {
        return mRadius;
    }

    public boolean getRect(Rect out)
    {
        if (mEmpty || out == null) return false;

        out.set(mRect);
        return true;
    }

    public boolean canClip()
    {
        return !mEmpty;
    }
}
