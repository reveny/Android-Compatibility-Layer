package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class InsetDrawable extends DrawableWrapper
{
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;
    private float mLeftFraction;
    private float mTopFraction;
    private float mRightFraction;
    private float mBottomFraction;

    public InsetDrawable()
    {
    }

    public InsetDrawable(Drawable inner, int inset)
    {
        this(inner, inset, inset, inset, inset);
    }

    public InsetDrawable(Drawable inner, float inset)
    {
        this(inner, inset, inset, inset, inset);
    }

    public InsetDrawable(Drawable inner, int left, int top, int right, int bottom)
    {
        super(inner);
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
    }

    public InsetDrawable(Drawable inner, float left, float top, float right, float bottom)
    {
        super(inner);
        mLeftFraction = left;
        mTopFraction = top;
        mRightFraction = right;
        mBottomFraction = bottom;
    }

    private int left(Rect bounds)
    {
        return mLeft + Math.round(bounds.width() * mLeftFraction);
    }

    private int top(Rect bounds)
    {
        return mTop + Math.round(bounds.height() * mTopFraction);
    }

    private int right(Rect bounds)
    {
        return mRight + Math.round(bounds.width() * mRightFraction);
    }

    private int bottom(Rect bounds)
    {
        return mBottom + Math.round(bounds.height() * mBottomFraction);
    }

    @Override
    public int getIntrinsicWidth()
    {
        Drawable inner = getDrawable();
        if (inner == null) return -1;

        int width = inner.getIntrinsicWidth();
        if (width < 0) return -1;

        float fractions = mLeftFraction + mRightFraction;
        return fractions >= 1f ? -1 : Math.round((width + mLeft + mRight) / (1f - fractions));
    }

    @Override
    public int getIntrinsicHeight()
    {
        Drawable inner = getDrawable();
        if (inner == null) return -1;

        int height = inner.getIntrinsicHeight();
        if (height < 0) return -1;

        float fractions = mTopFraction + mBottomFraction;
        return fractions >= 1f ? -1 : Math.round((height + mTop + mBottom) / (1f - fractions));
    }

    @Override
    public boolean getPadding(Rect padding)
    {
        if (padding == null) return false;

        Rect bounds = getBounds();
        Rect inner = new Rect();
        boolean innerPadded = getDrawable() != null && getDrawable().getPadding(inner);

        padding.set(left(bounds) + inner.left, top(bounds) + inner.top, right(bounds) + inner.right, bottom(bounds) + inner.bottom);
        return innerPadded || padding.left != 0 || padding.top != 0 || padding.right != 0 || padding.bottom != 0;
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        Drawable inner = getDrawable();
        if (inner == null) return;

        inner.setBounds(bounds.left + left(bounds), bounds.top + top(bounds), bounds.right - right(bounds),
                        bounds.bottom - bottom(bounds));
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        boolean allFraction = DrawableInflater.isFraction(attributes, "inset");
        int all = allFraction ? 0 : Math.round(DrawableInflater.dimension(resources, attributes, "inset", 0));
        float allPart = allFraction ? DrawableInflater.fraction(attributes, "inset", 0) : 0;

        mLeft = Math.round(DrawableInflater.dimension(resources, attributes, "insetLeft", all));
        mTop = Math.round(DrawableInflater.dimension(resources, attributes, "insetTop", all));
        mRight = Math.round(DrawableInflater.dimension(resources, attributes, "insetRight", all));
        mBottom = Math.round(DrawableInflater.dimension(resources, attributes, "insetBottom", all));
        mLeftFraction = side(attributes, "insetLeft", allPart);
        mTopFraction = side(attributes, "insetTop", allPart);
        mRightFraction = side(attributes, "insetRight", allPart);
        mBottomFraction = side(attributes, "insetBottom", allPart);

        if (DrawableInflater.isFraction(attributes, "insetLeft")) mLeft = 0;
        if (DrawableInflater.isFraction(attributes, "insetTop")) mTop = 0;
        if (DrawableInflater.isFraction(attributes, "insetRight")) mRight = 0;
        if (DrawableInflater.isFraction(attributes, "insetBottom")) mBottom = 0;

        inflateChild(resources, parser, attributes, theme);
    }

    private static float side(AttributeSet attributes, String name, float fallback)
    {
        if (DrawableInflater.isFraction(attributes, name)) return DrawableInflater.fraction(attributes, name, fallback);
        if (DrawableInflater.text(attributes, name) != null) return 0;

        return fallback;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    public android.graphics.Insets getOpticalInsets()
    {
        android.graphics.Insets inner = super.getOpticalInsets();
        Rect bounds = getBounds();

        return android.graphics.Insets.of(inner.left + left(bounds), inner.top + top(bounds), inner.right + right(bounds),
                                          inner.bottom + bottom(bounds));
    }

    @Override
    public int getOpacity()
    {
        Drawable inner = getDrawable();
        if (inner == null) return android.graphics.PixelFormat.TRANSPARENT;

        int opacity = inner.getOpacity();
        Rect bounds = getBounds();
        boolean inset = left(bounds) > 0 || top(bounds) > 0 || right(bounds) > 0 || bottom(bounds) > 0;

        if (opacity == android.graphics.PixelFormat.OPAQUE && inset) return android.graphics.PixelFormat.TRANSLUCENT;

        return opacity;
    }

    @Override
    public void getOutline(android.graphics.Outline outline)
    {
        super.getOutline(outline);
    }

    @Override
    DrawableWrapper copyWrapper()
    {
        InsetDrawable copy = new InsetDrawable();
        copy.mLeft = mLeft;
        copy.mTop = mTop;
        copy.mRight = mRight;
        copy.mBottom = mBottom;
        copy.mLeftFraction = mLeftFraction;
        copy.mTopFraction = mTopFraction;
        copy.mRightFraction = mRightFraction;
        copy.mBottomFraction = mBottomFraction;
        return copy;
    }

    @Override
    public void draw(Canvas canvas)
    {
        Drawable inner = getDrawable();
        if (inner == null) return;

        onBoundsChange(getBounds());
        inner.setAlpha(getAlpha());
        inner.draw(canvas);
    }
}
