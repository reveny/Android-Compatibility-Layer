package android.graphics;

public class BlurMaskFilter extends MaskFilter
{
    private final float mRadius;
    private final Blur mStyle;

    public BlurMaskFilter(float radius, Blur style)
    {
        mRadius = radius;
        mStyle = style == null ? Blur.NORMAL : style;
    }

    public float getRadius()
    {
        return mRadius;
    }

    public Blur getStyle()
    {
        return mStyle;
    }

    public enum Blur
    {
        NORMAL,
        SOLID,
        OUTER,
        INNER
    }
}
