package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;

public class ColorStateListDrawable extends Drawable implements Drawable.Callback
{
    private final ColorDrawable mColorDrawable = new ColorDrawable();
    private ColorStateList mColor;
    private ColorStateList mTint;
    private BlendMode mTintBlendMode;
    private int mAlpha = -1;
    private boolean mMutated;

    public ColorStateListDrawable()
    {
        this(ColorStateList.valueOf(0));
    }

    public ColorStateListDrawable(ColorStateList colorStateList)
    {
        mColorDrawable.setCallback(this);
        mColor = colorStateList == null ? ColorStateList.valueOf(0) : colorStateList;
        onStateChange(getState());
    }

    @Override
    public void draw(Canvas canvas)
    {
        mColorDrawable.draw(canvas);
    }

    @Override
    public int getAlpha()
    {
        return mColorDrawable.getAlpha();
    }

    @Override
    public boolean isStateful()
    {
        return mColor.isStateful() || (mTint != null && mTint.isStateful());
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return mColor.hasFocusStateSpecified() || (mTint != null && mTint.hasFocusStateSpecified());
    }

    @Override
    public Drawable getCurrent()
    {
        return mColorDrawable;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
        onStateChange(getState());
    }

    @Override
    public boolean canApplyTheme()
    {
        return super.canApplyTheme() || mColor.canApplyTheme();
    }

    @Override
    public void setAlpha(int alpha)
    {
        mAlpha = alpha;
        onStateChange(getState());
    }

    public void clearAlpha()
    {
        mAlpha = -1;
        onStateChange(getState());
    }

    @Override
    public void setTintList(ColorStateList tint)
    {
        mTint = tint;
        mColorDrawable.setTintList(tint);
        onStateChange(getState());
    }

    @Override
    public void setTintBlendMode(BlendMode blendMode)
    {
        mTintBlendMode = blendMode;
        mColorDrawable.setTintBlendMode(blendMode);
        onStateChange(getState());
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return mColorDrawable.getColorFilter();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter)
    {
        mColorDrawable.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity()
    {
        return mColorDrawable.getOpacity();
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        super.onBoundsChange(bounds);
        mColorDrawable.setBounds(bounds);
    }

    @Override
    protected boolean onStateChange(int[] state)
    {
        int color = mColor.getColorForState(state, mColor.getDefaultColor());
        if (mAlpha != -1) color = (color & 0xffffff) | (mAlpha << 24);

        mColorDrawable.setState(state);

        if (color == mColorDrawable.getColor()) return mTint != null && mTint.isStateful();

        mColorDrawable.setColor(color);
        return true;
    }

    @Override
    public void invalidateDrawable(Drawable who)
    {
        if (who == mColorDrawable && getCallback() != null) getCallback().invalidateDrawable(this);
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when)
    {
        if (who == mColorDrawable && getCallback() != null) getCallback().scheduleDrawable(this, what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what)
    {
        if (who == mColorDrawable && getCallback() != null) getCallback().unscheduleDrawable(this, what);
    }

    @Override
    public ConstantState getConstantState()
    {
        final ColorStateList color = mColor;
        final ColorStateList tint = mTint;
        final BlendMode blendMode = mTintBlendMode;
        final int alpha = mAlpha;
        final int changing = getChangingConfigurations();

        return new ConstantState()
        {
            @Override
            public Drawable newDrawable()
            {
                ColorStateListDrawable copy = new ColorStateListDrawable(color);
                if (tint != null) copy.setTintList(tint);
                if (blendMode != null) copy.setTintBlendMode(blendMode);
                if (alpha != -1) copy.setAlpha(alpha);
                return copy;
            }

            @Override
            public int getChangingConfigurations()
            {
                return changing;
            }
        };
    }

    public ColorStateList getColorStateList()
    {
        return mColor;
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations() | mColor.getChangingConfigurations();
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            mColorDrawable.mutate();
            mMutated = true;
        }
        return this;
    }

    public void setColorStateList(ColorStateList colorStateList)
    {
        mColor = colorStateList == null ? ColorStateList.valueOf(0) : colorStateList;
        onStateChange(getState());
        invalidateSelf();
    }
}
