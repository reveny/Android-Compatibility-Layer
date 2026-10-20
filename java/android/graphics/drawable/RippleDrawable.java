package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;

import org.xmlpull.v1.XmlPullParser;

public class RippleDrawable extends LayerDrawable
{
    private static final int PRESSED_STATE = 0x010100a7;
    private static final int MAXIMUM_RIPPLE_ALPHA = 80;

    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();

    private Drawable mMask;
    private int mRippleColor = 0x40FFFFFF;
    private float mHotspotX = Float.NaN;
    private float mHotspotY = Float.NaN;
    private int mRadius = -1;
    private boolean mPressed;

    public RippleDrawable()
    {
        mPaint.setAntiAlias(true);
    }

    public RippleDrawable(ColorStateList color, Drawable content, Drawable mask)
    {
        this();

        if (content != null) addLayer(content);
        mMask = mask;

        if (color != null) mRippleColor = color.getDefaultColor();
    }

    public void setColor(ColorStateList color)
    {
        if (color != null) mRippleColor = color.getDefaultColor();
    }

    public void setRippleColor(int color)
    {
        mRippleColor = color;
    }

    public boolean setDrawableByLayerId(int id, Drawable drawable)
    {
        return false;
    }

    @Override
    public void setHotspot(float x, float y)
    {
        mHotspotX = x;
        mHotspotY = y;
    }

    public void setRadius(int radius)
    {
        mRadius = radius;
    }

    public int getRadius()
    {
        return mRadius;
    }

    public void setHotspotBounds(int left, int top, int right, int bottom)
    {
    }

    @Override
    public boolean isStateful()
    {
        return true;
    }

    @Override
    public boolean setState(int[] stateSet)
    {
        boolean pressed = false;

        if (stateSet != null)
        {
            for (int state : stateSet)
            {
                if (state == PRESSED_STATE) pressed = true;
            }
        }

        boolean changed = pressed != mPressed;
        mPressed = pressed;

        return super.setState(stateSet) || changed;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes) throws java.io.IOException,
        org.xmlpull.v1.XmlPullParserException
    {
        int color = DrawableInflater.color(resources, attributes, "color", 0);
        if (color != 0) mRippleColor = color;

        super.inflate(resources, parser, attributes);
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);

        if (!mPressed) return;

        Rect bounds = getBounds();
        if (bounds.width() <= 0 || bounds.height() <= 0) return;

        float centreX = Float.isNaN(mHotspotX) ? bounds.centerX() : mHotspotX;
        float centreY = Float.isNaN(mHotspotY) ? bounds.centerY() : mHotspotY;

        float radius = mRadius > 0 ? mRadius : (float) Math.hypot(bounds.width(), bounds.height()) * 0.5f;

        int alpha = Math.min(MAXIMUM_RIPPLE_ALPHA, (mRippleColor >>> 24) & 0xFF) * getAlpha() / 255;

        int saveCount = canvas.save();

        mPath.reset();
        mPath.addRect(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
        canvas.clipPath(mPath);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor((alpha << 24) | (mRippleColor & 0x00FFFFFF));
        canvas.drawCircle(centreX, centreY, radius, mPaint);

        canvas.restoreToCount(saveCount);
    }
}
