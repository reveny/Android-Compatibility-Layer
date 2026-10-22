package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;

import org.xmlpull.v1.XmlPullParser;

public class ScaleDrawable extends DrawableWrapper
{
    private static final int MAX_LEVEL = 10000;

    private final Rect mScaledRect = new Rect();

    private float mScaleWidth = -1;
    private float mScaleHeight = -1;
    private int mGravity = Gravity.LEFT;
    private boolean mUseIntrinsicSizeAsMinimum;

    ScaleDrawable()
    {
    }

    public ScaleDrawable(Drawable drawable, int gravity, float scaleWidth, float scaleHeight)
    {
        super(drawable);
        mGravity = gravity;
        mScaleWidth = scaleWidth;
        mScaleHeight = scaleHeight;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    DrawableWrapper copyWrapper()
    {
        ScaleDrawable copy = new ScaleDrawable(null, mGravity, mScaleWidth, mScaleHeight);
        copy.mUseIntrinsicSizeAsMinimum = mUseIntrinsicSizeAsMinimum;
        return copy;
    }

    @Override
    public void draw(Canvas canvas)
    {
        Drawable inner = getDrawable();
        if (inner == null || inner.getLevel() == 0) return;

        onBoundsChange(getBounds());
        inner.setAlpha(getAlpha());
        inner.draw(canvas);
    }

    @Override
    public int getOpacity()
    {
        Drawable inner = getDrawable();
        if (inner == null || inner.getLevel() == 0) return PixelFormat.TRANSPARENT;

        int opacity = inner.getOpacity();
        if (opacity == PixelFormat.OPAQUE && inner.getLevel() < MAX_LEVEL) return PixelFormat.TRANSLUCENT;

        return opacity;
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        Drawable inner = getDrawable();
        if (inner != null) inner.setLevel(level);

        onBoundsChange(getBounds());
        invalidateSelf();
        return true;
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        Drawable inner = getDrawable();
        if (inner == null) return;

        int level = getLevel();
        int width = bounds.width();
        int height = bounds.height();

        if (mScaleWidth > 0)
        {
            int intrinsic = mUseIntrinsicSizeAsMinimum ? Math.max(0, inner.getIntrinsicWidth()) : 0;
            width -= (int)((width - intrinsic) * (MAX_LEVEL - level) * mScaleWidth / MAX_LEVEL);
        }

        if (mScaleHeight > 0)
        {
            int intrinsic = mUseIntrinsicSizeAsMinimum ? Math.max(0, inner.getIntrinsicHeight()) : 0;
            height -= (int)((height - intrinsic) * (MAX_LEVEL - level) * mScaleHeight / MAX_LEVEL);
        }

        Gravity.apply(mGravity, width, height, bounds, mScaledRect, getLayoutDirection());

        if (width > 0 && height > 0) inner.setBounds(mScaledRect.left, mScaledRect.top, mScaledRect.right, mScaledRect.bottom);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        mScaleWidth = percent(attributes, "scaleWidth", mScaleWidth);
        mScaleHeight = percent(attributes, "scaleHeight", mScaleHeight);
        mGravity = DrawableInflater.gravity(attributes, "scaleGravity", mGravity);
        mUseIntrinsicSizeAsMinimum = DrawableInflater.flag(attributes, "useIntrinsicSizeAsMinimum", false);

        int initialLevel = DrawableInflater.integer(attributes, "level", -1);

        int reference = DrawableInflater.reference(attributes, "drawable", 0);
        if (reference != 0) setDrawable(resources.getDrawable(reference));

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            Drawable child = DrawableInflater.inflate(resources, parser, attributes, theme);
            if (child != null) setDrawable(child);
        }

        if (initialLevel >= 0) setLevel(initialLevel);
        else if (getDrawable() != null) getDrawable().setLevel(getLevel());
    }

    private static float percent(AttributeSet attributes, String name, float fallback)
    {
        String text = DrawableInflater.text(attributes, name);
        if (text == null) return fallback;

        if (!text.endsWith("%")) return DrawableInflater.fraction(attributes, name, fallback);

        try
        {
            return Float.parseFloat(text.substring(0, text.length() - 1)) / 100;
        }
        catch (NumberFormatException failure)
        {
            return fallback;
        }
    }
}
