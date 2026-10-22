package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class RippleDrawable extends LayerDrawable
{
    public static final int RADIUS_AUTO = -1;

    private static final int MASK_LAYER_ID = 0x0102002e;
    private static final int PRESSED_STATE = 0x010100a7;
    private static final int ENABLED_STATE = 0x0101009e;
    private static final int FOCUSED_STATE = 0x0101009c;
    private static final int HOVERED_STATE = 0x01010367;
    private static final int MAXIMUM_RIPPLE_ALPHA = 80;
    private static final int DEFAULT_EFFECT_COLOR = 0x8DFFFFFF;

    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();
    private final Rect mHotspotBounds = new Rect();

    private ColorStateList mColor = ColorStateList.valueOf(0x40FFFFFF);
    private ColorStateList mEffectColor = ColorStateList.valueOf(DEFAULT_EFFECT_COLOR);
    private float mHotspotX = Float.NaN;
    private float mHotspotY = Float.NaN;
    private boolean mHasHotspotBounds;
    private int mRadius = RADIUS_AUTO;
    private boolean mPressed;
    private boolean mFocused;
    private boolean mHovered;

    public RippleDrawable()
    {
        mPaint.setAntiAlias(true);
    }

    public RippleDrawable(ColorStateList color, Drawable content, Drawable mask)
    {
        this();

        if (color == null) throw new IllegalArgumentException("RippleDrawable requires a non-null color");

        if (content != null) addLayer(content);

        if (mask != null)
        {
            int index = addLayer(mask);
            setId(index, MASK_LAYER_ID);
        }

        setPaddingMode(PADDING_MODE_STACK);
        mColor = color;
    }

    public void setColor(ColorStateList color)
    {
        if (color == null) throw new IllegalArgumentException("color cannot be null");

        mColor = color;
        invalidateSelf();
    }

    public void setRippleColor(int color)
    {
        setColor(ColorStateList.valueOf(color));
    }

    public void setEffectColor(ColorStateList color)
    {
        if (color == null) throw new IllegalArgumentException("color cannot be null");

        mEffectColor = color;
        invalidateSelf();
    }

    public ColorStateList getEffectColor()
    {
        return mEffectColor;
    }

    public void setRadius(int radius)
    {
        mRadius = radius;
        invalidateSelf();
    }

    public int getRadius()
    {
        return mRadius;
    }

    @Override
    public boolean setDrawableByLayerId(int id, Drawable drawable)
    {
        if (super.setDrawableByLayerId(id, drawable)) return true;
        if (id != MASK_LAYER_ID || drawable == null) return false;

        int index = addLayer(drawable);
        setId(index, MASK_LAYER_ID);
        return true;
    }

    @Override
    public void setPaddingMode(int mode)
    {
        super.setPaddingMode(mode);
    }

    @Override
    public void jumpToCurrentState()
    {
        super.jumpToCurrentState();
        invalidateSelf();
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        boolean changed = super.onStateChange(stateSet);

        boolean enabled = false;
        boolean pressed = false;
        boolean focused = false;
        boolean hovered = false;

        if (stateSet != null)
        {
            for (int state : stateSet)
            {
                if (state == ENABLED_STATE) enabled = true;
                else if (state == PRESSED_STATE) pressed = true;
                else if (state == FOCUSED_STATE) focused = true;
                else if (state == HOVERED_STATE) hovered = true;
            }
        }

        boolean wantPressed = enabled && pressed;
        boolean wantFocused = enabled && focused;
        boolean wantHovered = enabled && hovered;

        if (wantPressed != mPressed || wantFocused != mFocused || wantHovered != mHovered)
        {
            mPressed = wantPressed;
            mFocused = wantFocused;
            mHovered = wantHovered;
            changed = true;
            invalidateSelf();
        }

        return changed;
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        super.onBoundsChange(bounds);
        if (!mHasHotspotBounds) mHotspotBounds.set(bounds);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart)
    {
        boolean changed = super.setVisible(visible, restart);

        if (!visible && mPressed)
        {
            mPressed = false;
            changed = true;
        }

        return changed;
    }

    @Override
    public boolean isProjected()
    {
        if (isBounded()) return false;

        Rect bounds = getBounds();
        Rect hotspot = mHasHotspotBounds ? mHotspotBounds : bounds;

        if (mRadius != RADIUS_AUTO && mRadius <= hotspot.width() / 2 && mRadius <= hotspot.height() / 2
            && (bounds.equals(hotspot) || bounds.contains(hotspot)))
        {
            return false;
        }

        return true;
    }

    private boolean isBounded()
    {
        return getNumberOfLayers() > 0;
    }

    @Override
    public boolean isStateful()
    {
        return true;
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        return true;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        ColorStateList color = DrawableInflater.colorStateList(resources, attributes, "color");
        if (color != null) mColor = color;

        ColorStateList effect = DrawableInflater.colorStateList(resources, attributes, "effectColor");
        if (effect != null) mEffectColor = effect;

        mRadius = Math.round(DrawableInflater.dimension(resources, attributes, "radius", mRadius));

        super.inflate(resources, parser, attributes, theme);
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);
    }

    @Override
    public boolean canApplyTheme()
    {
        return super.canApplyTheme();
    }

    @Override
    public void setHotspot(float x, float y)
    {
        mHotspotX = x;
        mHotspotY = y;
        super.setHotspot(x, y);
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom)
    {
        mHasHotspotBounds = true;
        mHotspotBounds.set(left, top, right, bottom);
        super.setHotspotBounds(left, top, right, bottom);
    }

    @Override
    public void getHotspotBounds(Rect outRect)
    {
        outRect.set(mHasHotspotBounds ? mHotspotBounds : getBounds());
    }

    @Override
    public void getOutline(Outline outline)
    {
        Drawable mask = findDrawableByLayerId(MASK_LAYER_ID);

        if (mask != null)
        {
            mask.getOutline(outline);
            return;
        }

        for (int index = 0; index < getNumberOfLayers(); index++)
        {
            Drawable layer = getDrawable(index);
            if (layer == null) continue;

            layer.getOutline(outline);
            if (!outline.isEmpty()) return;
        }
    }

    @Override
    public void draw(Canvas canvas)
    {
        layoutLayers();

        for (int index = 0; index < getNumberOfLayers(); index++)
        {
            if (getId(index) == MASK_LAYER_ID) continue;

            Drawable layer = getDrawable(index);
            if (layer != null) layer.draw(canvas);
        }

        if (!mPressed && !mFocused && !mHovered) return;

        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        int color = mColor.getColorForState(getState(), mColor.getDefaultColor());
        int baseAlpha = (color >>> 24) & 0xFF;
        int alpha = mPressed ? Math.min(MAXIMUM_RIPPLE_ALPHA, baseAlpha) : Math.min(MAXIMUM_RIPPLE_ALPHA, baseAlpha) / 2;
        alpha = alpha * getRootAlpha() / 255;
        if (alpha <= 0) return;

        int saveCount = canvas.save();

        Drawable mask = findDrawableByLayerId(MASK_LAYER_ID);
        mPath.reset();

        if (mask instanceof GradientDrawable || mask instanceof ShapeDrawable)
        {
            Outline outline = new Outline();
            mask.getOutline(outline);
            Rect rounded = new Rect();

            if (outline.getRect(rounded) && outline.getRadius() > 0)
            {
                float radius = outline.getRadius();
                mPath.addRoundRect(rounded.left, rounded.top, rounded.right, rounded.bottom, radius, radius, Path.Direction.CW);
            }
            else if (!outline.isEmpty() && outline.getRect(rounded))
            {
                mPath.addRect(rounded.left, rounded.top, rounded.right, rounded.bottom, Path.Direction.CW);
            }
        }

        if (mPath.isEmpty()) mPath.addRect(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
        if (isBounded()) canvas.clipPath(mPath);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor((alpha << 24) | (color & 0x00FFFFFF));

        if (mPressed)
        {
            float centreX = Float.isNaN(mHotspotX) ? bounds.centerX() : mHotspotX;
            float centreY = Float.isNaN(mHotspotY) ? bounds.centerY() : mHotspotY;
            float radius = mRadius > 0 ? mRadius : (float) Math.hypot(bounds.width(), bounds.height()) * 0.5f;

            canvas.drawCircle(centreX, centreY, radius, mPaint);
        }
        else if (isBounded())
        {
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);
        }
        else
        {
            float radius = mRadius > 0 ? mRadius : Math.min(bounds.width(), bounds.height()) * 0.5f;
            canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, mPaint);
        }

        canvas.restoreToCount(saveCount);
    }

    private int getRootAlpha()
    {
        return super.getAlpha();
    }

    @Override
    public void invalidateSelf()
    {
        super.invalidateSelf();
    }

    @Override
    public Rect getDirtyBounds()
    {
        if (isBounded()) return getBounds();

        Rect bounds = getBounds();
        int radius = mRadius > 0 ? mRadius : (int) Math.ceil(Math.hypot(bounds.width(), bounds.height()) * 0.5);
        int centreX = Float.isNaN(mHotspotX) ? bounds.centerX() : (int) mHotspotX;
        int centreY = Float.isNaN(mHotspotY) ? bounds.centerY() : (int) mHotspotY;

        Rect dirty = new Rect(bounds);
        dirty.union(centreX - radius, centreY - radius, centreX + radius, centreY + radius);
        return dirty;
    }

    @Override
    public ConstantState getConstantState()
    {
        return super.getConstantState();
    }

    @Override
    public Drawable mutate()
    {
        return super.mutate();
    }

    @Override
    LayerDrawable createCopy()
    {
        return new RippleDrawable();
    }

    @Override
    void copyConfigurationTo(LayerDrawable copy)
    {
        super.copyConfigurationTo(copy);

        if (!(copy instanceof RippleDrawable)) return;

        RippleDrawable ripple = (RippleDrawable) copy;
        ripple.mColor = mColor;
        ripple.mEffectColor = mEffectColor;
        ripple.mRadius = mRadius;
    }
}
