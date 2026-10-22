package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Outline;
import android.graphics.Rect;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public abstract class DrawableWrapper extends Drawable implements Drawable.Callback
{
    private Drawable mDrawable;
    private boolean mMutated;

    DrawableWrapper()
    {
    }

    public DrawableWrapper(Drawable drawable)
    {
        setDrawable(drawable);
    }

    public void setDrawable(Drawable drawable)
    {
        if (mDrawable != null) mDrawable.setCallback(null);

        mDrawable = drawable;

        if (drawable == null) return;

        drawable.setCallback(this);
        drawable.setVisible(isVisible(), true);
        drawable.setState(getState());
        drawable.setLevel(getLevel());
        drawable.setBounds(getBounds());
        drawable.setLayoutDirection(getLayoutDirection());
    }

    public Drawable getDrawable()
    {
        return mDrawable;
    }

    DrawableWrapper copyWrapper()
    {
        return null;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);
        inflateChild(resources, parser, attributes, theme);
    }

    void inflateChild(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int reference = DrawableInflater.reference(attributes, "drawable", 0);
        if (reference != 0 && resources != null) setDrawable(resources.getDrawable(reference, theme));

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            Drawable child = DrawableInflater.inflate(resources, parser, attributes, theme);
            if (child != null) setDrawable(child);
        }
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
        if (mDrawable != null && mDrawable.canApplyTheme()) mDrawable.applyTheme(theme);
    }

    @Override
    public boolean canApplyTheme()
    {
        return mDrawable != null && mDrawable.canApplyTheme() || super.canApplyTheme();
    }

    @Override
    public void invalidateDrawable(Drawable who)
    {
        Callback callback = getCallback();
        if (callback != null) callback.invalidateDrawable(this);
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when)
    {
        Callback callback = getCallback();
        if (callback != null) callback.scheduleDrawable(this, what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what)
    {
        Callback callback = getCallback();
        if (callback != null) callback.unscheduleDrawable(this, what);
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mDrawable != null) mDrawable.draw(canvas);
    }

    @Override
    public int getChangingConfigurations()
    {
        return super.getChangingConfigurations() | (mDrawable != null ? mDrawable.getChangingConfigurations() : 0);
    }

    @Override
    public boolean getPadding(Rect padding)
    {
        return mDrawable != null ? mDrawable.getPadding(padding) : super.getPadding(padding);
    }

    @Override
    public Insets getOpticalInsets()
    {
        return mDrawable != null ? mDrawable.getOpticalInsets() : Insets.NONE;
    }

    @Override
    public void setHotspot(float x, float y)
    {
        if (mDrawable != null) mDrawable.setHotspot(x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom)
    {
        if (mDrawable != null) mDrawable.setHotspotBounds(left, top, right, bottom);
    }

    @Override
    public void getHotspotBounds(Rect outRect)
    {
        if (mDrawable != null) mDrawable.getHotspotBounds(outRect);
        else outRect.set(getBounds());
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart)
    {
        boolean changed = super.setVisible(visible, restart);
        if (mDrawable != null && mDrawable.setVisible(visible, restart)) changed = true;

        return changed;
    }

    @Override
    public void setAlpha(int alpha)
    {
        super.setAlpha(alpha);
        if (mDrawable != null) mDrawable.setAlpha(alpha);
    }

    @Override
    public int getAlpha()
    {
        return mDrawable != null ? mDrawable.getAlpha() : super.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter filter)
    {
        super.setColorFilter(filter);
        if (mDrawable != null) mDrawable.setColorFilter(filter);
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return mDrawable != null ? mDrawable.getColorFilter() : super.getColorFilter();
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        return mDrawable != null && mDrawable.setLevel(level);
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection)
    {
        return mDrawable != null && mDrawable.setLayoutDirection(layoutDirection);
    }

    @Override
    public void setTintList(android.content.res.ColorStateList tint)
    {
        if (mDrawable != null) mDrawable.setTintList(tint);
    }

    @Override
    public void setTintBlendMode(android.graphics.BlendMode blendMode)
    {
        if (mDrawable != null) mDrawable.setTintBlendMode(blendMode);
    }

    @Override
    public int getOpacity()
    {
        return mDrawable != null ? mDrawable.getOpacity() : super.getOpacity();
    }

    @Override
    public boolean isStateful()
    {
        return mDrawable != null && mDrawable.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return mDrawable != null && mDrawable.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        if (mDrawable == null || !mDrawable.isStateful()) return false;

        boolean changed = mDrawable.setState(stateSet);
        if (changed) onBoundsChange(getBounds());

        return changed;
    }

    @Override
    public void jumpToCurrentState()
    {
        if (mDrawable != null) mDrawable.jumpToCurrentState();
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        if (mDrawable != null) mDrawable.setBounds(bounds);
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mDrawable != null ? mDrawable.getIntrinsicWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mDrawable != null ? mDrawable.getIntrinsicHeight() : -1;
    }

    @Override
    public void getOutline(Outline outline)
    {
        if (mDrawable != null) mDrawable.getOutline(outline);
        else super.getOutline(outline);
    }

    @Override
    public ConstantState getConstantState()
    {
        ConstantState child = null;

        if (mDrawable != null)
        {
            child = mDrawable.getConstantState();
            if (child == null) return null;
        }

        DrawableWrapper template = copyWrapper();
        if (template == null) return null;

        return new WrapperState(template, child, getChangingConfigurations());
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            if (mDrawable != null) mDrawable.mutate();
            mMutated = true;
        }

        return this;
    }

    private static final class WrapperState extends ConstantState
    {
        private final DrawableWrapper mTemplate;
        private final ConstantState mChild;
        private final int mChangingConfigurations;

        WrapperState(DrawableWrapper template, ConstantState child, int changingConfigurations)
        {
            mTemplate = template;
            mChild = child;
            mChangingConfigurations = changingConfigurations;
        }

        @Override
        public Drawable newDrawable()
        {
            return newDrawable(null, null);
        }

        @Override
        public Drawable newDrawable(Resources resources)
        {
            return newDrawable(resources, null);
        }

        @Override
        public Drawable newDrawable(Resources resources, Resources.Theme theme)
        {
            DrawableWrapper copy = mTemplate.copyWrapper();
            copy.setChangingConfigurations(mChangingConfigurations);
            if (mChild != null) copy.setDrawable(mChild.newDrawable(resources, theme));
            return copy;
        }

        @Override
        public int getChangingConfigurations()
        {
            return mChangingConfigurations;
        }

        @Override
        public boolean canApplyTheme()
        {
            return mChild != null && mChild.canApplyTheme();
        }
    }
}
