package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class ColorDrawable extends Drawable
{
    private final Paint mPaint = new Paint();

    private int mBaseColor;
    private int mUseColor;
    private ColorStateList mTint;
    private BlendMode mTintBlendMode = BlendMode.SRC_IN;
    private ColorFilter mTintFilter;

    public ColorDrawable()
    {
        this(0);
    }

    public ColorDrawable(int color)
    {
        mBaseColor = color;
        mUseColor = color;
    }

    public int getColor()
    {
        return mUseColor;
    }

    public void setColor(int color)
    {
        if (mBaseColor == color && mUseColor == color) return;

        mBaseColor = color;
        mUseColor = color;
        invalidateSelf();
    }

    @Override
    public int getAlpha()
    {
        return mUseColor >>> 24;
    }

    @Override
    public void setAlpha(int alpha)
    {
        super.setAlpha(alpha);

        int clamped = alpha < 0 ? 0 : (alpha > 255 ? 255 : alpha);
        int baseAlpha = mBaseColor >>> 24;
        int useAlpha = baseAlpha * (clamped + (clamped >> 7)) >> 8;
        int useColor = (mBaseColor << 8 >>> 8) | (useAlpha << 24);

        if (mUseColor == useColor) return;

        mUseColor = useColor;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter filter)
    {
        super.setColorFilter(filter);
        mPaint.setColorFilter(filter);
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return mPaint.getColorFilter();
    }

    @Override
    public void setTintList(ColorStateList tint)
    {
        mTint = tint;
        mTintFilter = tintFilter();
        invalidateSelf();
    }

    @Override
    public void setTintBlendMode(BlendMode blendMode)
    {
        mTintBlendMode = blendMode == null ? BlendMode.SRC_IN : blendMode;
        mTintFilter = tintFilter();
        invalidateSelf();
    }

    private ColorFilter tintFilter()
    {
        if (mTint == null) return null;

        return new BlendModeColorFilter(mTint.getColorForState(getState(), mTint.getDefaultColor()), mTintBlendMode);
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        if (mTint == null) return false;

        mTintFilter = tintFilter();
        return true;
    }

    @Override
    public boolean isStateful()
    {
        return mTint != null && mTint.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return false;
    }

    @Override
    public void draw(Canvas canvas)
    {
        ColorFilter filter = mPaint.getColorFilter();
        if ((mUseColor >>> 24) == 0 && filter == null && mTintFilter == null) return;

        if (filter == null) mPaint.setColorFilter(mTintFilter);

        mPaint.setColor(mUseColor);
        canvas.drawRect(getBounds(), mPaint);
        mPaint.setColorFilter(filter);
    }

    @Override
    public int getOpacity()
    {
        if (mTintFilter != null || mPaint.getColorFilter() != null) return PixelFormat.TRANSLUCENT;

        int alpha = mUseColor >>> 24;
        if (alpha == 255) return PixelFormat.OPAQUE;
        if (alpha == 0) return PixelFormat.TRANSPARENT;

        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void getOutline(Outline outline)
    {
        outline.setRect(getBounds());
        outline.setAlpha(getAlpha() / 255.0f);
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        mBaseColor = DrawableInflater.color(resources, attributes, "color", mBaseColor);
        mUseColor = mBaseColor;
    }

    @Override
    public boolean canApplyTheme()
    {
        return false;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations();
    }

    @Override
    public Drawable mutate()
    {
        return this;
    }

    @Override
    public ConstantState getConstantState()
    {
        return new ColorState(this);
    }

    static final class ColorState extends ConstantState
    {
        final int mBaseColor;
        final int mUseColor;
        final ColorStateList mTint;
        final BlendMode mTintBlendMode;
        final int mChangingConfigurations;

        ColorState(ColorDrawable source)
        {
            mBaseColor = source.mBaseColor;
            mUseColor = source.mUseColor;
            mTint = source.mTint;
            mTintBlendMode = source.mTintBlendMode;
            mChangingConfigurations = source.getChangingConfigurations();
        }

        @Override
        public Drawable newDrawable()
        {
            ColorDrawable drawable = new ColorDrawable(mBaseColor);
            drawable.mUseColor = mUseColor;
            drawable.mTint = mTint;
            drawable.mTintBlendMode = mTintBlendMode;
            drawable.mTintFilter = drawable.tintFilter();
            drawable.setChangingConfigurations(mChangingConfigurations);
            return drawable;
        }

        @Override
        public int getChangingConfigurations()
        {
            return mChangingConfigurations;
        }
    }
}
