package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Insets;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Region;

public abstract class Drawable
{
    private static final int[] EMPTY_STATE = new int[0];

    private final Rect mBounds = new Rect();
    private boolean mVisible = true;
    private Callback mCallback;
    private int mAlpha = 255;
    private int[] mStateSet = EMPTY_STATE;
    private int mLevel;
    private int mLayoutDirection;
    private int mChangingConfigurations;
    private ColorFilter mColorFilter;
    private boolean mSettingTintMode;

    public Drawable()
    {
    }

    public abstract void draw(Canvas canvas);

    public void setBounds(int left, int top, int right, int bottom)
    {
        mBounds.set(left, top, right, bottom);
        onBoundsChange(mBounds);
    }

    public void setBounds(Rect bounds)
    {
        setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public final Rect getBounds()
    {
        return mBounds;
    }

    public final void copyBounds(Rect out)
    {
        out.set(mBounds);
    }

    public final Rect copyBounds()
    {
        return new Rect(mBounds);
    }

    public Rect getDirtyBounds()
    {
        return getBounds();
    }

    protected void onBoundsChange(Rect bounds)
    {
    }

    public void setChangingConfigurations(int configs)
    {
        mChangingConfigurations = configs;
    }

    public int getChangingConfigurations()
    {
        return mChangingConfigurations;
    }

    public void setDither(boolean dither)
    {
    }

    public void setFilterBitmap(boolean filter)
    {
    }

    public boolean isFilterBitmap()
    {
        return false;
    }

    public void setAlpha(int alpha)
    {
        mAlpha = alpha;
    }

    public int getAlpha()
    {
        return mAlpha;
    }

    public int getOpacity()
    {
        return PixelFormat.TRANSLUCENT;
    }

    public static int resolveOpacity(int first, int second)
    {
        if (first == second) return first;
        if (first == PixelFormat.UNKNOWN || second == PixelFormat.UNKNOWN) return PixelFormat.UNKNOWN;
        if (first == PixelFormat.TRANSLUCENT || second == PixelFormat.TRANSLUCENT) return PixelFormat.TRANSLUCENT;
        if (first == PixelFormat.TRANSPARENT || second == PixelFormat.TRANSPARENT) return PixelFormat.TRANSPARENT;

        return PixelFormat.OPAQUE;
    }

    public Region getTransparentRegion()
    {
        return null;
    }

    public boolean isStateful()
    {
        return false;
    }

    public boolean hasFocusStateSpecified()
    {
        return false;
    }

    public boolean setState(int[] stateSet)
    {
        int[] wanted = stateSet == null ? EMPTY_STATE : stateSet;
        if (java.util.Arrays.equals(mStateSet, wanted)) return false;

        mStateSet = wanted;
        return onStateChange(wanted);
    }

    public int[] getState()
    {
        return mStateSet;
    }

    protected boolean onStateChange(int[] stateSet)
    {
        return false;
    }

    public final boolean setLevel(int level)
    {
        if (mLevel == level) return false;

        mLevel = level;
        return onLevelChange(level);
    }

    public final int getLevel()
    {
        return mLevel;
    }

    protected boolean onLevelChange(int level)
    {
        return false;
    }

    public int getLayoutDirection()
    {
        return mLayoutDirection;
    }

    public final boolean setLayoutDirection(int layoutDirection)
    {
        if (mLayoutDirection == layoutDirection) return false;

        mLayoutDirection = layoutDirection;
        return onLayoutDirectionChanged(layoutDirection);
    }

    public boolean onLayoutDirectionChanged(int layoutDirection)
    {
        return false;
    }

    public void setAutoMirrored(boolean mirrored)
    {
    }

    public boolean isAutoMirrored()
    {
        return false;
    }

    public void applyTheme(android.content.res.Resources.Theme theme)
    {
    }

    public boolean canApplyTheme()
    {
        return false;
    }

    public int getIntrinsicWidth()
    {
        return -1;
    }

    public int getIntrinsicHeight()
    {
        return -1;
    }

    public int getMinimumWidth()
    {
        int intrinsic = getIntrinsicWidth();
        return intrinsic > 0 ? intrinsic : 0;
    }

    public int getMinimumHeight()
    {
        int intrinsic = getIntrinsicHeight();
        return intrinsic > 0 ? intrinsic : 0;
    }

    public boolean getPadding(Rect padding)
    {
        padding.set(0, 0, 0, 0);
        return false;
    }

    public Insets getOpticalInsets()
    {
        return Insets.NONE;
    }

    public void getOutline(Outline outline)
    {
        outline.setRect(getBounds());
        outline.setAlpha(0);
    }

    public void invalidateSelf()
    {
        if (mCallback != null) mCallback.invalidateDrawable(this);
    }

    public void scheduleSelf(Runnable what, long when)
    {
        if (mCallback != null) mCallback.scheduleDrawable(this, what, when);
    }

    public void unscheduleSelf(Runnable what)
    {
        if (mCallback != null) mCallback.unscheduleDrawable(this, what);
    }

    public Drawable mutate()
    {
        return this;
    }

    public ConstantState getConstantState()
    {
        return null;
    }

    public Drawable getCurrent()
    {
        return this;
    }

    public boolean setVisible(boolean visible, boolean restart)
    {
        boolean changed = mVisible != visible;
        mVisible = visible;

        return changed;
    }

    public final boolean isVisible()
    {
        return mVisible;
    }

    public final void setCallback(Callback callback)
    {
        mCallback = callback;
    }

    public Callback getCallback()
    {
        return mCallback;
    }

    public interface Callback
    {
        void invalidateDrawable(Drawable who);

        void scheduleDrawable(Drawable who, Runnable what, long when);

        void unscheduleDrawable(Drawable who, Runnable what);
    }

    public void jumpToCurrentState()
    {
    }

    public void setTint(int tintColor)
    {
        setTintList(android.content.res.ColorStateList.valueOf(tintColor));
    }

    public void setTintMode(PorterDuff.Mode mode)
    {
        if (mSettingTintMode) return;

        mSettingTintMode = true;

        try
        {
            android.graphics.BlendMode blendMode = mode == null ? null : android.graphics.BlendMode.fromPorterDuffMode(mode);
            setTintBlendMode(blendMode == null ? android.graphics.BlendMode.SRC_IN : blendMode);
        }
        finally
        {
            mSettingTintMode = false;
        }
    }

    static ColorFilter tintFilterFor(android.content.res.ColorStateList tint, android.graphics.BlendMode blendMode,
                                     int[] stateSet)
    {
        if (tint == null) return null;

        android.graphics.BlendMode mode = blendMode == null ? android.graphics.BlendMode.SRC_IN : blendMode;
        return new android.graphics.BlendModeColorFilter(tint.getColorForState(stateSet, tint.getDefaultColor()), mode);
    }

    static boolean hasFocusState(android.content.res.ColorStateList tint)
    {
        return tint != null && tint.hasFocusStateSpecified();
    }

    public void setTintBlendMode(android.graphics.BlendMode mode)
    {
    }

    public void setTintList(android.content.res.ColorStateList tintList)
    {
    }

    public void setColorFilter(ColorFilter filter)
    {
        mColorFilter = filter;
    }

    public void setColorFilter(int color, PorterDuff.Mode mode)
    {
        ColorFilter current = getColorFilter();

        if (current instanceof PorterDuffColorFilter)
        {
            PorterDuffColorFilter existing = (PorterDuffColorFilter) current;
            if (existing.getColor() == color && existing.getMode() == mode) return;
        }

        setColorFilter(new PorterDuffColorFilter(color, mode));
    }

    public ColorFilter getColorFilter()
    {
        return mColorFilter;
    }

    public void clearColorFilter()
    {
        setColorFilter(null);
    }

    public void setHotspot(float x, float y)
    {
    }

    public void setHotspotBounds(int left, int top, int right, int bottom)
    {
    }

    public void getHotspotBounds(Rect outRect)
    {
        outRect.set(getBounds());
    }

    public boolean isProjected()
    {
        return false;
    }

    public void inflate(android.content.res.Resources resources, org.xmlpull.v1.XmlPullParser parser,
                        android.util.AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        inflate(resources, parser, attributes, null);
    }

    public void inflate(android.content.res.Resources resources, org.xmlpull.v1.XmlPullParser parser,
                        android.util.AttributeSet attributes, android.content.res.Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        if (attributes != null) mVisible = DrawableInflater.flag(attributes, "visible", mVisible);
    }

    public static Drawable createFromXml(android.content.res.Resources resources, org.xmlpull.v1.XmlPullParser parser)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        return createFromXml(resources, parser, null);
    }

    public static Drawable createFromXml(android.content.res.Resources resources, org.xmlpull.v1.XmlPullParser parser,
                                         android.content.res.Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int event;

        while ((event = parser.next()) != org.xmlpull.v1.XmlPullParser.START_TAG
               && event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT)
        {
        }

        if (event != org.xmlpull.v1.XmlPullParser.START_TAG) return null;

        return createFromXmlInner(resources, parser, (android.util.AttributeSet) parser, theme);
    }

    public static Drawable createFromXmlInner(android.content.res.Resources resources,
                                              org.xmlpull.v1.XmlPullParser parser, android.util.AttributeSet attributes)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        return createFromXmlInner(resources, parser, attributes, null);
    }

    public static Drawable createFromXmlInner(android.content.res.Resources resources,
                                              org.xmlpull.v1.XmlPullParser parser, android.util.AttributeSet attributes,
                                              android.content.res.Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        return DrawableInflater.inflate(resources, parser, attributes, theme);
    }

    public static Drawable createFromPath(String path)
    {
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(path);
        return bitmap == null ? null : new BitmapDrawable(bitmap);
    }

    public static Drawable createFromStream(java.io.InputStream stream, String name)
    {
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(stream);
        return bitmap == null ? null : new BitmapDrawable(bitmap);
    }

    public static Drawable createFromResourceStream(android.content.res.Resources resources, android.util.TypedValue value,
                                                    java.io.InputStream stream, String name)
    {
        return createFromResourceStream(resources, value, stream, name, null);
    }

    public static Drawable createFromResourceStream(android.content.res.Resources resources, android.util.TypedValue value,
                                                    java.io.InputStream stream, String name,
                                                    android.graphics.BitmapFactory.Options options)
    {
        if (stream == null) return null;

        byte[] data;
        try
        {
            java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int got;
            while ((got = stream.read(buffer)) > 0) collected.write(buffer, 0, got);
            data = collected.toByteArray();
        }
        catch (java.io.IOException failure)
        {
            return null;
        }

        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (bitmap == null) return null;

        byte[] chunk = NinePatchDrawable.extractChunk(data);
        if (chunk != null && android.graphics.NinePatch.isNinePatchChunk(chunk))
        {
            return new NinePatchDrawable(resources, bitmap, chunk, null, name);
        }

        return new BitmapDrawable(resources, bitmap);
    }

    public abstract static class ConstantState
    {
        public ConstantState()
        {
        }

        public abstract Drawable newDrawable();

        public Drawable newDrawable(android.content.res.Resources resources)
        {
            return newDrawable();
        }

        public Drawable newDrawable(android.content.res.Resources resources, android.content.res.Resources.Theme theme)
        {
            return newDrawable(resources);
        }

        public abstract int getChangingConfigurations();

        public boolean canApplyTheme()
        {
            return false;
        }
    }
}
