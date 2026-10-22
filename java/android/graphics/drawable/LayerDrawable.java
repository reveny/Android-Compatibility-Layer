package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class LayerDrawable extends Drawable implements Drawable.Callback
{
    public static final int PADDING_MODE_NEST = 0;
    public static final int PADDING_MODE_STACK = 1;
    public static final int INSET_UNDEFINED = Integer.MIN_VALUE;

    private static final String[] PADDING_MODE_NAMES = {"nest", "stack"};

    private final ArrayList<Layer> mLayers = new ArrayList<>();
    private final Rect mContainer = new Rect();
    private final Rect mLayerBounds = new Rect();
    private final Rect mChildPadding = new Rect();

    private int mPaddingMode = PADDING_MODE_NEST;
    private int mPaddingLeft = -1;
    private int mPaddingTop = -1;
    private int mPaddingRight = -1;
    private int mPaddingBottom = -1;
    private int mPaddingStart = -1;
    private int mPaddingEnd = -1;
    private int mOpacityOverride = PixelFormat.UNKNOWN;
    private boolean mAutoMirrored;
    private boolean mMutated;
    private Rect mHotspotBounds;

    public LayerDrawable()
    {
    }

    public LayerDrawable(Drawable[] layers)
    {
        if (layers == null) throw new IllegalArgumentException("layers must be non-null");

        for (Drawable layer : layers)
        {
            if (layer != null) addLayer(layer);
        }
    }

    public int addLayer(Drawable drawable)
    {
        Layer layer = new Layer(drawable);
        attach(drawable);
        mLayers.add(layer);

        layoutLayers();
        return mLayers.size() - 1;
    }

    private void attach(Drawable drawable)
    {
        if (drawable == null) return;

        drawable.setCallback(this);
        drawable.setLayoutDirection(getLayoutDirection());
        drawable.setLevel(getLevel());
        drawable.setVisible(isVisible(), true);
        drawable.setState(getState());
        if (getAlpha() != 255) drawable.setAlpha(getAlpha());
        if (getColorFilter() != null) drawable.setColorFilter(getColorFilter());
    }

    public Drawable findDrawableByLayerId(int id)
    {
        for (int index = mLayers.size() - 1; index >= 0; index--)
        {
            Layer layer = mLayers.get(index);
            if (layer.id == id) return layer.drawable;
        }

        return null;
    }

    public void setId(int index, int id)
    {
        layer(index).id = id;
    }

    public int getId(int index)
    {
        return index < 0 || index >= mLayers.size() ? 0 : mLayers.get(index).id;
    }

    public int getNumberOfLayers()
    {
        return mLayers.size();
    }

    public boolean setDrawableByLayerId(int id, Drawable drawable)
    {
        int index = findIndexByLayerId(id);
        if (index < 0) return false;

        setDrawable(index, drawable);
        return true;
    }

    public int findIndexByLayerId(int id)
    {
        for (int index = 0; index < mLayers.size(); index++)
        {
            if (mLayers.get(index).id == id) return index;
        }

        return -1;
    }

    public void setDrawable(int index, Drawable drawable)
    {
        Layer layer = layer(index);

        if (layer.drawable != null) layer.drawable.setCallback(null);

        attach(drawable);
        layer.drawable = drawable;

        layoutLayers();
        invalidateSelf();
    }

    public Drawable getDrawable(int index)
    {
        return index < 0 || index >= mLayers.size() ? null : mLayers.get(index).drawable;
    }

    public void setLayerSize(int index, int width, int height)
    {
        Layer layer = layer(index);
        layer.width = width;
        layer.height = height;
    }

    public void setLayerWidth(int index, int width)
    {
        layer(index).width = width;
    }

    public int getLayerWidth(int index)
    {
        return layer(index).width;
    }

    public void setLayerHeight(int index, int height)
    {
        layer(index).height = height;
    }

    public int getLayerHeight(int index)
    {
        return layer(index).height;
    }

    public void setLayerGravity(int index, int gravity)
    {
        layer(index).gravity = gravity;
    }

    public int getLayerGravity(int index)
    {
        return layer(index).gravity;
    }

    public void setLayerInset(int index, int left, int top, int right, int bottom)
    {
        setInsets(index, left, top, right, bottom, INSET_UNDEFINED, INSET_UNDEFINED);
    }

    public void setLayerInsetRelative(int index, int start, int top, int end, int bottom)
    {
        setInsets(index, 0, top, 0, bottom, start, end);
    }

    private void setInsets(int index, int left, int top, int right, int bottom, int start, int end)
    {
        Layer layer = layer(index);
        layer.left = left;
        layer.top = top;
        layer.right = right;
        layer.bottom = bottom;
        layer.start = start;
        layer.end = end;
    }

    public void setLayerInsetLeft(int index, int left)
    {
        layer(index).left = left;
    }

    public int getLayerInsetLeft(int index)
    {
        return layer(index).left;
    }

    public void setLayerInsetRight(int index, int right)
    {
        layer(index).right = right;
    }

    public int getLayerInsetRight(int index)
    {
        return layer(index).right;
    }

    public void setLayerInsetTop(int index, int top)
    {
        layer(index).top = top;
    }

    public int getLayerInsetTop(int index)
    {
        return layer(index).top;
    }

    public void setLayerInsetBottom(int index, int bottom)
    {
        layer(index).bottom = bottom;
    }

    public int getLayerInsetBottom(int index)
    {
        return layer(index).bottom;
    }

    public void setLayerInsetStart(int index, int start)
    {
        layer(index).start = start;
    }

    public int getLayerInsetStart(int index)
    {
        return layer(index).start;
    }

    public void setLayerInsetEnd(int index, int end)
    {
        layer(index).end = end;
    }

    public int getLayerInsetEnd(int index)
    {
        return layer(index).end;
    }

    public void setPaddingMode(int mode)
    {
        if (mode != PADDING_MODE_NEST && mode != PADDING_MODE_STACK)
        {
            throw new IllegalArgumentException("Padding mode must be PADDING_MODE_NEST or PADDING_MODE_STACK");
        }

        mPaddingMode = mode;
    }

    public int getPaddingMode()
    {
        return mPaddingMode;
    }

    public void setPadding(int left, int top, int right, int bottom)
    {
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
        mPaddingStart = -1;
        mPaddingEnd = -1;
    }

    public void setPaddingRelative(int start, int top, int end, int bottom)
    {
        mPaddingStart = start;
        mPaddingTop = top;
        mPaddingEnd = end;
        mPaddingBottom = bottom;
        mPaddingLeft = -1;
        mPaddingRight = -1;
    }

    public int getLeftPadding()
    {
        return mPaddingLeft;
    }

    public int getRightPadding()
    {
        return mPaddingRight;
    }

    public int getStartPadding()
    {
        return mPaddingStart;
    }

    public int getEndPadding()
    {
        return mPaddingEnd;
    }

    public int getTopPadding()
    {
        return mPaddingTop;
    }

    public int getBottomPadding()
    {
        return mPaddingBottom;
    }

    public void setOpacity(int opacity)
    {
        mOpacityOverride = opacity;
    }

    @Override
    public int getOpacity()
    {
        if (mOpacityOverride != PixelFormat.UNKNOWN) return mOpacityOverride;

        int opacity = PixelFormat.TRANSPARENT;

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.isVisible()) opacity = resolveOpacity(opacity, layer.drawable.getOpacity());
        }

        return opacity;
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
    public void draw(Canvas canvas)
    {
        layoutLayers();

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.draw(canvas);
        }
    }

    @Override
    public boolean getPadding(Rect padding)
    {
        computePadding(padding, mLayers.size());

        boolean rightToLeft = getLayoutDirection() == 1;
        int start = rightToLeft ? mPaddingEnd : mPaddingStart;
        int end = rightToLeft ? mPaddingStart : mPaddingEnd;

        if (start >= 0) padding.left = start;
        else if (mPaddingLeft >= 0) padding.left = mPaddingLeft;

        if (end >= 0) padding.right = end;
        else if (mPaddingRight >= 0) padding.right = mPaddingRight;

        if (mPaddingTop >= 0) padding.top = mPaddingTop;
        if (mPaddingBottom >= 0) padding.bottom = mPaddingBottom;

        return padding.left != 0 || padding.top != 0 || padding.right != 0 || padding.bottom != 0;
    }

    private void computePadding(Rect padding, int count)
    {
        padding.set(0, 0, 0, 0);

        for (int index = 0; index < count && index < mLayers.size(); index++)
        {
            Drawable drawable = mLayers.get(index).drawable;
            if (drawable == null) continue;

            mChildPadding.set(0, 0, 0, 0);
            drawable.getPadding(mChildPadding);

            if (mPaddingMode == PADDING_MODE_NEST)
            {
                padding.left += mChildPadding.left;
                padding.top += mChildPadding.top;
                padding.right += mChildPadding.right;
                padding.bottom += mChildPadding.bottom;
            }
            else
            {
                padding.left = Math.max(padding.left, mChildPadding.left);
                padding.top = Math.max(padding.top, mChildPadding.top);
                padding.right = Math.max(padding.right, mChildPadding.right);
                padding.bottom = Math.max(padding.bottom, mChildPadding.bottom);
            }
        }
    }

    @Override
    public void getOutline(Outline outline)
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable == null) continue;

            layer.drawable.getOutline(outline);
            if (!outline.isEmpty()) return;
        }
    }

    @Override
    public void setHotspot(float x, float y)
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setHotspot(x, y);
        }
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom)
    {
        if (mHotspotBounds == null) mHotspotBounds = new Rect(left, top, right, bottom);
        else mHotspotBounds.set(left, top, right, bottom);

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setHotspotBounds(left, top, right, bottom);
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart)
    {
        boolean changed = super.setVisible(visible, restart);

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setVisible(visible, restart);
        }

        return changed;
    }

    @Override
    public void setDither(boolean dither)
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setDither(dither);
        }
    }

    @Override
    public void setAlpha(int alpha)
    {
        super.setAlpha(alpha);

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter filter)
    {
        super.setColorFilter(filter);

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setColorFilter(filter);
        }
    }

    @Override
    public void setTintList(android.content.res.ColorStateList tint)
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setTintList(tint);
        }
    }

    @Override
    public void setTintBlendMode(android.graphics.BlendMode blendMode)
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setTintBlendMode(blendMode);
        }
    }

    @Override
    public void setAutoMirrored(boolean mirrored)
    {
        mAutoMirrored = mirrored;

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.setAutoMirrored(mirrored);
        }
    }

    @Override
    public boolean isAutoMirrored()
    {
        return mAutoMirrored;
    }

    @Override
    public void jumpToCurrentState()
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) layer.drawable.jumpToCurrentState();
        }
    }

    @Override
    public boolean isStateful()
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.isStateful()) return true;
        }

        return false;
    }

    @Override
    public boolean hasFocusStateSpecified()
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.hasFocusStateSpecified()) return true;
        }

        return false;
    }

    @Override
    protected boolean onStateChange(int[] stateSet)
    {
        boolean changed = false;

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.isStateful() && layer.drawable.setState(stateSet)) changed = true;
        }

        if (changed) layoutLayers();
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        boolean changed = false;

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.setLevel(level)) changed = true;
        }

        if (changed) layoutLayers();
        return changed;
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection)
    {
        boolean changed = false;

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.setLayoutDirection(layoutDirection)) changed = true;
        }

        layoutLayers();
        return changed;
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        layoutLayers();
    }

    @Override
    public int getIntrinsicWidth()
    {
        int widest = -1;
        int paddingLeft = 0;
        int paddingRight = 0;
        boolean nest = mPaddingMode == PADDING_MODE_NEST;
        boolean rightToLeft = getLayoutDirection() == 1;

        for (Layer layer : mLayers)
        {
            if (layer.drawable == null) continue;

            int minimum = layer.width < 0 ? layer.drawable.getIntrinsicWidth() : layer.width;
            int width = minimum < 0 ? -1 : minimum + layer.resolvedLeft(rightToLeft) + layer.resolvedRight(rightToLeft) + paddingLeft
                                           + paddingRight;
            if (width > widest) widest = width;

            if (nest)
            {
                mChildPadding.set(0, 0, 0, 0);
                layer.drawable.getPadding(mChildPadding);
                paddingLeft += mChildPadding.left;
                paddingRight += mChildPadding.right;
            }
        }

        return widest;
    }

    @Override
    public int getIntrinsicHeight()
    {
        int tallest = -1;
        int paddingTop = 0;
        int paddingBottom = 0;
        boolean nest = mPaddingMode == PADDING_MODE_NEST;

        for (Layer layer : mLayers)
        {
            if (layer.drawable == null) continue;

            int minimum = layer.height < 0 ? layer.drawable.getIntrinsicHeight() : layer.height;
            int height = minimum < 0 ? -1 : minimum + layer.top + layer.bottom + paddingTop + paddingBottom;
            if (height > tallest) tallest = height;

            if (nest)
            {
                mChildPadding.set(0, 0, 0, 0);
                layer.drawable.getPadding(mChildPadding);
                paddingTop += mChildPadding.top;
                paddingBottom += mChildPadding.bottom;
            }
        }

        return tallest;
    }

    @Override
    public Drawable mutate()
    {
        if (!mMutated && super.mutate() == this)
        {
            for (Layer layer : mLayers)
            {
                if (layer.drawable != null) layer.drawable.mutate();
            }

            mMutated = true;
        }

        return this;
    }

    @Override
    public int getAlpha()
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) return layer.drawable.getAlpha();
        }

        return super.getAlpha();
    }

    @Override
    public int getChangingConfigurations()
    {
        int configurations = super.getChangingConfigurations();

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null) configurations |= layer.drawable.getChangingConfigurations();
        }

        return configurations;
    }

    @Override
    public void applyTheme(Resources.Theme theme)
    {
        super.applyTheme(theme);

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.canApplyTheme()) layer.drawable.applyTheme(theme);
        }

        layoutLayers();
    }

    @Override
    public boolean canApplyTheme()
    {
        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.canApplyTheme()) return true;
        }

        return super.canApplyTheme();
    }

    @Override
    public void getHotspotBounds(Rect outRect)
    {
        if (mHotspotBounds != null) outRect.set(mHotspotBounds);
        else super.getHotspotBounds(outRect);
    }

    @Override
    public boolean isProjected()
    {
        if (super.isProjected()) return true;

        for (Layer layer : mLayers)
        {
            if (layer.drawable != null && layer.drawable.isProjected()) return true;
        }

        return false;
    }

    LayerDrawable createCopy()
    {
        return new LayerDrawable();
    }

    void copyConfigurationTo(LayerDrawable copy)
    {
        copy.mPaddingMode = mPaddingMode;
        copy.mPaddingLeft = mPaddingLeft;
        copy.mPaddingTop = mPaddingTop;
        copy.mPaddingRight = mPaddingRight;
        copy.mPaddingBottom = mPaddingBottom;
        copy.mPaddingStart = mPaddingStart;
        copy.mPaddingEnd = mPaddingEnd;
        copy.mOpacityOverride = mOpacityOverride;
        copy.mAutoMirrored = mAutoMirrored;
    }

    @Override
    public ConstantState getConstantState()
    {
        ConstantState[] children = new ConstantState[mLayers.size()];

        for (int index = 0; index < children.length; index++)
        {
            Drawable drawable = mLayers.get(index).drawable;
            if (drawable == null) continue;

            children[index] = drawable.getConstantState();
            if (children[index] == null) return null;
        }

        LayerDrawable template = createCopy();
        copyConfigurationTo(template);

        Layer[] layers = new Layer[children.length];
        for (int index = 0; index < layers.length; index++) layers[index] = mLayers.get(index).copyWithout();

        return new LayerState(template, layers, children, getChangingConfigurations());
    }

    private static final class LayerState extends ConstantState
    {
        private final LayerDrawable mTemplate;
        private final Layer[] mLayers;
        private final ConstantState[] mChildren;
        private final int mChangingConfigurations;

        LayerState(LayerDrawable template, Layer[] layers, ConstantState[] children, int changingConfigurations)
        {
            mTemplate = template;
            mLayers = layers;
            mChildren = children;
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
            LayerDrawable copy = mTemplate.createCopy();
            mTemplate.copyConfigurationTo(copy);
            copy.setChangingConfigurations(mChangingConfigurations);

            for (int index = 0; index < mLayers.length; index++)
            {
                Layer layer = mLayers[index].copyWithout();
                layer.drawable = mChildren[index] == null ? null : mChildren[index].newDrawable(resources, theme);
                copy.attach(layer.drawable);
                copy.mLayers.add(layer);
            }

            copy.layoutLayers();
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
            for (ConstantState child : mChildren)
            {
                if (child != null && child.canApplyTheme()) return true;
            }

            return false;
        }
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        super.inflate(resources, parser, attributes, theme);

        int opacity = DrawableInflater.enumeration(attributes, "opacity", null, PixelFormat.UNKNOWN);
        if (opacity != PixelFormat.UNKNOWN) mOpacityOverride = opacity;

        mPaddingMode = DrawableInflater.enumeration(attributes, "paddingMode", PADDING_MODE_NAMES, mPaddingMode);
        mAutoMirrored = DrawableInflater.flag(attributes, "autoMirrored", false);
        mPaddingLeft = Math.round(DrawableInflater.dimension(resources, attributes, "paddingLeft", mPaddingLeft));
        mPaddingTop = Math.round(DrawableInflater.dimension(resources, attributes, "paddingTop", mPaddingTop));
        mPaddingRight = Math.round(DrawableInflater.dimension(resources, attributes, "paddingRight", mPaddingRight));
        mPaddingBottom = Math.round(DrawableInflater.dimension(resources, attributes, "paddingBottom", mPaddingBottom));
        mPaddingStart = Math.round(DrawableInflater.dimension(resources, attributes, "paddingStart", mPaddingStart));
        mPaddingEnd = Math.round(DrawableInflater.dimension(resources, attributes, "paddingEnd", mPaddingEnd));

        int outerDepth = parser.getDepth();
        int event;

        while ((event = parser.next()) != XmlPullParser.END_DOCUMENT)
        {
            if (event == XmlPullParser.END_TAG && parser.getDepth() <= outerDepth) break;
            if (event != XmlPullParser.START_TAG) continue;

            if (!"item".equals(parser.getName()))
            {
                DrawableInflater.skipElement(parser);
                continue;
            }

            Layer layer = new Layer(null);
            layer.id = DrawableInflater.reference(attributes, "id", 0);
            layer.left = Math.round(DrawableInflater.dimension(resources, attributes, "left", 0));
            layer.top = Math.round(DrawableInflater.dimension(resources, attributes, "top", 0));
            layer.right = Math.round(DrawableInflater.dimension(resources, attributes, "right", 0));
            layer.bottom = Math.round(DrawableInflater.dimension(resources, attributes, "bottom", 0));
            layer.start = Math.round(DrawableInflater.dimension(resources, attributes, "start", INSET_UNDEFINED));
            layer.end = Math.round(DrawableInflater.dimension(resources, attributes, "end", INSET_UNDEFINED));
            layer.width = Math.round(DrawableInflater.dimension(resources, attributes, "width", -1));
            layer.height = Math.round(DrawableInflater.dimension(resources, attributes, "height", -1));
            layer.gravity = DrawableInflater.gravity(attributes, "gravity", Gravity.NO_GRAVITY);

            layer.drawable = DrawableInflater.itemDrawable(resources, parser, attributes, theme);

            if (layer.drawable == null) continue;

            attach(layer.drawable);
            mLayers.add(layer);
        }

        layoutLayers();
    }

    void layoutLayers()
    {
        Rect bounds = getBounds();
        boolean nest = mPaddingMode == PADDING_MODE_NEST;
        boolean rightToLeft = getLayoutDirection() == 1;

        int paddingLeft = 0;
        int paddingTop = 0;
        int paddingRight = 0;
        int paddingBottom = 0;

        for (Layer layer : mLayers)
        {
            Drawable drawable = layer.drawable;
            if (drawable == null) continue;

            mContainer.set(bounds.left + layer.resolvedLeft(rightToLeft) + paddingLeft, bounds.top + layer.top + paddingTop,
                           bounds.right - layer.resolvedRight(rightToLeft) - paddingRight,
                           bounds.bottom - layer.bottom - paddingBottom);

            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();
            int gravity = resolveGravity(layer.gravity, layer.width, layer.height, intrinsicWidth, intrinsicHeight);
            int width = layer.width < 0 ? intrinsicWidth : layer.width;
            int height = layer.height < 0 ? intrinsicHeight : layer.height;

            Gravity.apply(gravity, width, height, mContainer, mLayerBounds, getLayoutDirection());

            Rect current = drawable.getBounds();

            if (!current.equals(mLayerBounds))
            {
                drawable.setBounds(mLayerBounds.left, mLayerBounds.top, mLayerBounds.right, mLayerBounds.bottom);
            }

            if (nest)
            {
                mChildPadding.set(0, 0, 0, 0);
                drawable.getPadding(mChildPadding);
                paddingLeft += mChildPadding.left;
                paddingTop += mChildPadding.top;
                paddingRight += mChildPadding.right;
                paddingBottom += mChildPadding.bottom;
            }
        }
    }

    private static int resolveGravity(int gravity, int width, int height, int intrinsicWidth, int intrinsicHeight)
    {
        int resolved = gravity;

        if (!Gravity.isHorizontal(resolved)) resolved |= width < 0 ? Gravity.FILL_HORIZONTAL : Gravity.LEFT;
        if (!Gravity.isVertical(resolved)) resolved |= height < 0 ? Gravity.FILL_VERTICAL : Gravity.TOP;

        if (width < 0 && intrinsicWidth < 0) resolved |= Gravity.FILL_HORIZONTAL;
        if (height < 0 && intrinsicHeight < 0) resolved |= Gravity.FILL_VERTICAL;

        return resolved;
    }

    private Layer layer(int index)
    {
        if (index < 0 || index >= mLayers.size()) throw new IndexOutOfBoundsException("layer " + index);

        return mLayers.get(index);
    }

    private static final class Layer
    {
        Drawable drawable;
        int id;
        int left;
        int top;
        int right;
        int bottom;
        int start = INSET_UNDEFINED;
        int end = INSET_UNDEFINED;
        int width = -1;
        int height = -1;
        int gravity = Gravity.NO_GRAVITY;

        Layer(Drawable drawable)
        {
            this.drawable = drawable;
        }

        Layer copyWithout()
        {
            Layer copy = new Layer(null);
            copy.id = id;
            copy.left = left;
            copy.top = top;
            copy.right = right;
            copy.bottom = bottom;
            copy.start = start;
            copy.end = end;
            copy.width = width;
            copy.height = height;
            copy.gravity = gravity;
            return copy;
        }

        int resolvedLeft(boolean rightToLeft)
        {
            int relative = rightToLeft ? end : start;
            return relative != INSET_UNDEFINED ? relative : left;
        }

        int resolvedRight(boolean rightToLeft)
        {
            int relative = rightToLeft ? start : end;
            return relative != INSET_UNDEFINED ? relative : right;
        }
    }
}
