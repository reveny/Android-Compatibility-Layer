package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.Rect;

public abstract class Drawable
{
    private final Rect mBounds = new Rect();
    private boolean mVisible = true;
    private Callback mCallback;
    private int mAlpha = 255;
    private int[] mStateSet = new int[0];

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

    public void copyBounds(Rect out)
    {
        out.set(mBounds);
    }

    protected void onBoundsChange(Rect bounds)
    {
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
        return mAlpha == 255 ? 0xFF : 0;
    }

    public boolean isStateful()
    {
        return false;
    }

    public boolean setState(int[] stateSet)
    {
        mStateSet = stateSet;
        return false;
    }

    public int[] getState()
    {
        return mStateSet;
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

    public boolean isVisible()
    {
        return mVisible;
    }

    public void setCallback(Callback callback)
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

    public void setTintMode(android.graphics.PorterDuff.Mode mode)
    {
    }

    public void setTintBlendMode(android.graphics.BlendMode mode)
    {
    }

    public void setTintList(android.content.res.ColorStateList tintList)
    {
    }

    public void setColorFilter(android.graphics.ColorFilter filter)
    {
    }

    public void setHotspot(float x, float y)
    {
    }

    public void inflate(android.content.res.Resources resources, org.xmlpull.v1.XmlPullParser parser,
                        android.util.AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
    }

    public static Drawable createFromXml(android.content.res.Resources resources, org.xmlpull.v1.XmlPullParser parser)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int event;

        while ((event = parser.next()) != org.xmlpull.v1.XmlPullParser.START_TAG
               && event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT)
        {
        }

        if (event != org.xmlpull.v1.XmlPullParser.START_TAG) return null;

        return createFromXmlInner(resources, parser, (android.util.AttributeSet) parser);
    }

    public static Drawable createFromXmlInner(android.content.res.Resources resources,
                                              org.xmlpull.v1.XmlPullParser parser, android.util.AttributeSet attributes)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        return DrawableInflater.inflate(resources, parser, attributes);
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

    public abstract static class ConstantState
    {
        public abstract Drawable newDrawable();

        public abstract int getChangingConfigurations();
    }
}
