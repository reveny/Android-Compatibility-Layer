package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class AdaptiveIconDrawable extends Drawable implements Drawable.Callback
{
    private static final float EXTRA_INSET_PERCENTAGE = 1 / 4f;
    private static final float DEFAULT_VIEW_PORT_SCALE = 1f / (1 + 2 * EXTRA_INSET_PERCENTAGE);
    private static final float MASK_CORNER_FRACTION = 0.25f;

    private final Path mMask = new Path();
    private final Rect mLayerBounds = new Rect();
    private final Rect mHotspotBounds = new Rect();

    private Drawable mBackground;
    private Drawable mForeground;
    private Drawable mMonochrome;
    private int mOpacityOverride = PixelFormat.UNKNOWN;
    private boolean mAutoMirrored;
    private boolean mHasHotspotBounds;
    private boolean mMutated;

    AdaptiveIconDrawable()
    {
    }

    public AdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable)
    {
        this(backgroundDrawable, foregroundDrawable, null);
    }

    public AdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable, Drawable monochromeDrawable)
    {
        mBackground = attach(backgroundDrawable);
        mForeground = attach(foregroundDrawable);
        mMonochrome = attach(monochromeDrawable);
    }

    private Drawable attach(Drawable drawable)
    {
        if (drawable == null) return null;

        drawable.setCallback(this);
        drawable.setVisible(isVisible(), true);
        drawable.setState(getState());
        drawable.setLevel(getLevel());
        drawable.setLayoutDirection(getLayoutDirection());
        if (!mLayerBounds.isEmpty()) drawable.setBounds(mLayerBounds);
        return drawable;
    }

    private void forEach(java.util.function.Consumer<Drawable> action)
    {
        if (mBackground != null) action.accept(mBackground);
        if (mForeground != null) action.accept(mForeground);
        if (mMonochrome != null) action.accept(mMonochrome);
    }

    public static float getExtraInsetFraction()
    {
        return EXTRA_INSET_PERCENTAGE;
    }

    public Path getIconMask()
    {
        buildMask(getBounds());
        return mMask;
    }

    public Drawable getForeground()
    {
        return mForeground;
    }

    public Drawable getBackground()
    {
        return mBackground;
    }

    public Drawable getMonochrome()
    {
        return mMonochrome;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            String name = parser.getName();

            if (!"background".equals(name) && !"foreground".equals(name) && !"monochrome".equals(name))
            {
                DrawableInflater.skipElement(parser);
                continue;
            }

            Drawable layer = DrawableInflater.itemDrawable(resources, parser, attributes, theme);

            if ("background".equals(name)) mBackground = attach(layer);
            else if ("foreground".equals(name)) mForeground = attach(layer);
            else mMonochrome = attach(layer);
        }

        updateLayerBounds(getBounds());
    }

    private void buildMask(Rect bounds)
    {
        mMask.reset();

        float radius = Math.min(bounds.width(), bounds.height()) * MASK_CORNER_FRACTION;
        mMask.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, radius, radius, Path.Direction.CW);
    }

    private void updateLayerBounds(Rect bounds)
    {
        if (bounds.isEmpty()) return;

        int centreX = bounds.centerX();
        int centreY = bounds.centerY();
        int halfWidth = (int) (bounds.width() / (DEFAULT_VIEW_PORT_SCALE * 2));
        int halfHeight = (int) (bounds.height() / (DEFAULT_VIEW_PORT_SCALE * 2));

        mLayerBounds.set(centreX - halfWidth, centreY - halfHeight, centreX + halfWidth, centreY + halfHeight);

        forEach(layer -> layer.setBounds(mLayerBounds));
        buildMask(bounds);
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        if (bounds.isEmpty()) return;

        updateLayerBounds(bounds);
    }

    @Override
    public void draw(Canvas canvas)
    {
        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        if (mLayerBounds.isEmpty()) updateLayerBounds(bounds);
        buildMask(bounds);

        int saveCount = canvas.save();
        canvas.clipPath(mMask);

        if (mBackground != null) mBackground.draw(canvas);
        if (mForeground != null) mForeground.draw(canvas);

        canvas.restoreToCount(saveCount);
    }

    @Override
    public void invalidateSelf()
    {
        super.invalidateSelf();
    }

    @Override
    public void getOutline(Outline outline)
    {
        buildMask(getBounds());
        outline.setPath(mMask);
    }

    @Override
    public Region getTransparentRegion()
    {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return null;

        buildMask(bounds);

        Region transparent = new Region(bounds);
        Region opaque = new Region();
        opaque.setPath(mMask, new Region(bounds));
        transparent.op(opaque, Region.Op.DIFFERENCE);
        return transparent;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);

        forEach(layer ->
        {
            if (layer.canApplyTheme()) layer.applyTheme(theme);
        });
    }

    @Override
    public boolean canApplyTheme()
    {
        return mBackground != null && mBackground.canApplyTheme() || mForeground != null && mForeground.canApplyTheme()
               || mMonochrome != null && mMonochrome.canApplyTheme() || super.canApplyTheme();
    }

    @Override
    public boolean isProjected()
    {
        if (super.isProjected()) return true;

        return mBackground != null && mBackground.isProjected() || mForeground != null && mForeground.isProjected();
    }

    @Override
    public void invalidateDrawable(Drawable who)
    {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when)
    {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what)
    {
        unscheduleSelf(what);
    }

    @Override
    public int getChangingConfigurations()
    {
        int configurations = super.getChangingConfigurations();
        if (mBackground != null) configurations |= mBackground.getChangingConfigurations();
        if (mForeground != null) configurations |= mForeground.getChangingConfigurations();
        if (mMonochrome != null) configurations |= mMonochrome.getChangingConfigurations();
        return configurations;
    }

    @Override
    public void setHotspot(float x, float y)
    {
        forEach(layer -> layer.setHotspot(x, y));
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom)
    {
        mHotspotBounds.set(left, top, right, bottom);
        mHasHotspotBounds = true;
        forEach(layer -> layer.setHotspotBounds(left, top, right, bottom));
    }

    @Override
    public void getHotspotBounds(Rect outRect)
    {
        if (mHasHotspotBounds) outRect.set(mHotspotBounds);
        else super.getHotspotBounds(outRect);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart)
    {
        boolean changed = super.setVisible(visible, restart);
        forEach(layer -> layer.setVisible(visible, restart));
        return changed;
    }

    @Override
    public void setDither(boolean dither)
    {
        forEach(layer -> layer.setDither(dither));
    }

    @Override
    public void setAlpha(int alpha)
    {
        super.setAlpha(alpha);
        forEach(layer -> layer.setAlpha(alpha));
        invalidateSelf();
    }

    @Override
    public int getAlpha()
    {
        return super.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter filter)
    {
        super.setColorFilter(filter);
        forEach(layer -> layer.setColorFilter(filter));
    }

    @Override
    public void setTintList(ColorStateList tint)
    {
        forEach(layer -> layer.setTintList(tint));
    }

    @Override
    public void setTintBlendMode(BlendMode blendMode)
    {
        forEach(layer -> layer.setTintBlendMode(blendMode));
    }

    public void setOpacity(int opacity)
    {
        mOpacityOverride = opacity;
    }

    @Override
    public int getOpacity()
    {
        return mOpacityOverride != PixelFormat.UNKNOWN ? mOpacityOverride : PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAutoMirrored(boolean mirrored)
    {
        mAutoMirrored = mirrored;
        forEach(layer -> layer.setAutoMirrored(mirrored));
    }

    @Override
    public boolean isAutoMirrored()
    {
        return mAutoMirrored;
    }

    @Override
    public void jumpToCurrentState()
    {
        forEach(Drawable::jumpToCurrentState);
    }

    @Override
    public boolean isStateful()
    {
        return mBackground != null && mBackground.isStateful() || mForeground != null && mForeground.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return mBackground != null && mBackground.hasFocusStateSpecified()
               || mForeground != null && mForeground.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        boolean changed = false;

        if (mBackground != null && mBackground.isStateful() && mBackground.setState(stateSet)) changed = true;
        if (mForeground != null && mForeground.isStateful() && mForeground.setState(stateSet)) changed = true;
        if (mMonochrome != null && mMonochrome.isStateful() && mMonochrome.setState(stateSet)) changed = true;

        if (changed) invalidateSelf();
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        boolean changed = false;

        if (mBackground != null && mBackground.setLevel(level)) changed = true;
        if (mForeground != null && mForeground.setLevel(level)) changed = true;
        if (mMonochrome != null && mMonochrome.setLevel(level)) changed = true;

        if (changed) invalidateSelf();
        return changed;
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection)
    {
        boolean changed = false;

        if (mBackground != null && mBackground.setLayoutDirection(layoutDirection)) changed = true;
        if (mForeground != null && mForeground.setLayoutDirection(layoutDirection)) changed = true;
        if (mMonochrome != null && mMonochrome.setLayoutDirection(layoutDirection)) changed = true;

        return changed;
    }

    @Override
    public int getIntrinsicWidth()
    {
        int widest = Math.max(mBackground == null ? -1 : mBackground.getIntrinsicWidth(),
                              mForeground == null ? -1 : mForeground.getIntrinsicWidth());
        return widest < 0 ? -1 : (int) (widest * DEFAULT_VIEW_PORT_SCALE);
    }

    @Override
    public int getIntrinsicHeight()
    {
        int tallest = Math.max(mBackground == null ? -1 : mBackground.getIntrinsicHeight(),
                               mForeground == null ? -1 : mForeground.getIntrinsicHeight());
        return tallest < 0 ? -1 : (int) (tallest * DEFAULT_VIEW_PORT_SCALE);
    }

    @Override
    public ConstantState getConstantState()
    {
        ConstantState background = stateOf(mBackground);
        ConstantState foreground = stateOf(mForeground);
        ConstantState monochrome = stateOf(mMonochrome);

        if (mBackground != null && background == null) return null;
        if (mForeground != null && foreground == null) return null;
        if (mMonochrome != null && monochrome == null) return null;

        return new LayerState(background, foreground, monochrome, mOpacityOverride, mAutoMirrored, getChangingConfigurations());
    }

    private static ConstantState stateOf(Drawable drawable)
    {
        return drawable == null ? null : drawable.getConstantState();
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            forEach(Drawable::mutate);
            mMutated = true;
        }

        return this;
    }

    private static final class LayerState extends ConstantState
    {
        private final ConstantState mBackground;
        private final ConstantState mForeground;
        private final ConstantState mMonochrome;
        private final int mOpacityOverride;
        private final boolean mAutoMirrored;
        private final int mChangingConfigurations;

        LayerState(ConstantState background, ConstantState foreground, ConstantState monochrome, int opacityOverride,
                   boolean autoMirrored, int changingConfigurations)
        {
            mBackground = background;
            mForeground = foreground;
            mMonochrome = monochrome;
            mOpacityOverride = opacityOverride;
            mAutoMirrored = autoMirrored;
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
            AdaptiveIconDrawable drawable = new AdaptiveIconDrawable(create(mBackground, resources, theme),
                                                                     create(mForeground, resources, theme),
                                                                     create(mMonochrome, resources, theme));
            drawable.mOpacityOverride = mOpacityOverride;
            drawable.mAutoMirrored = mAutoMirrored;
            drawable.setChangingConfigurations(mChangingConfigurations);
            return drawable;
        }

        private static Drawable create(ConstantState state, Resources resources, Resources.Theme theme)
        {
            return state == null ? null : state.newDrawable(resources, theme);
        }

        @Override
        public int getChangingConfigurations()
        {
            return mChangingConfigurations;
        }

        @Override
        public boolean canApplyTheme()
        {
            return mBackground != null && mBackground.canApplyTheme() || mForeground != null && mForeground.canApplyTheme()
                   || mMonochrome != null && mMonochrome.canApplyTheme();
        }
    }
}
