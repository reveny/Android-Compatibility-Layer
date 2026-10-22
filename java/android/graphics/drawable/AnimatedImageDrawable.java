package android.graphics.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

public class AnimatedImageDrawable extends Drawable implements Animatable2
{
    public static final int REPEAT_INFINITE = -1;

    private final ArrayList<AnimationCallback> mCallbacks = new ArrayList<>();
    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private Bitmap mFrame;
    private int mRepeatCount = REPEAT_INFINITE;
    private boolean mRunning;
    private boolean mAutoMirrored;

    public AnimatedImageDrawable()
    {
    }

    public AnimatedImageDrawable(Resources resources, Bitmap frame)
    {
        mFrame = frame;
    }

    public void setRepeatCount(int repeatCount)
    {
        if (repeatCount < REPEAT_INFINITE) throw new IllegalArgumentException("invalid value passed to setRepeatCount" + repeatCount);

        mRepeatCount = repeatCount;
    }

    public int getRepeatCount()
    {
        return mRepeatCount;
    }

    @Override
    public void inflate(Resources resources, XmlPullParser parser, AttributeSet attributes, Resources.Theme theme)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int source = DrawableInflater.reference(attributes, "src", 0);
        if (source != 0) mFrame = resources.getBitmap(source);

        mRepeatCount = DrawableInflater.integer(attributes, "repeatCount", mRepeatCount);
        mAutoMirrored = DrawableInflater.flag(attributes, "autoMirrored", false);

        boolean autoStart = DrawableInflater.flag(attributes, "autoStart", false);
        if (autoStart) start();
    }

    @Override
    public int getIntrinsicWidth()
    {
        return mFrame == null ? -1 : mFrame.getWidth();
    }

    @Override
    public int getIntrinsicHeight()
    {
        return mFrame == null ? -1 : mFrame.getHeight();
    }

    @Override
    public void draw(Canvas canvas)
    {
        if (mFrame == null) return;

        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;

        mPaint.setAlpha(getAlpha());
        mPaint.setColorFilter(getColorFilter());
        canvas.drawBitmap(mFrame, null, new RectF(bounds), mPaint);
    }

    @Override
    public void setAlpha(int alpha)
    {
        if (alpha < 0 || alpha > 255) throw new IllegalArgumentException("Alpha must be between 0 and 255! provided " + alpha);

        super.setAlpha(alpha);
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
        invalidateSelf();
    }

    @Override
    public ColorFilter getColorFilter()
    {
        return super.getColorFilter();
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAutoMirrored(boolean mirrored)
    {
        mAutoMirrored = mirrored;
    }

    @Override
    public final boolean isAutoMirrored()
    {
        return mAutoMirrored;
    }

    @Override
    public boolean onLayoutDirectionChanged(int layoutDirection)
    {
        return mAutoMirrored;
    }

    @Override
    public boolean isRunning()
    {
        return mRunning;
    }

    @Override
    public void start()
    {
        if (mRunning) return;

        mRunning = true;
        for (AnimationCallback callback : new ArrayList<>(mCallbacks)) callback.onAnimationStart(this);

        if (mRepeatCount == REPEAT_INFINITE) return;

        android.os.Looper looper = android.os.Looper.myLooper();
        new android.os.Handler(looper != null ? looper : android.os.Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                stop();
            }
        });
    }

    @Override
    public void stop()
    {
        if (!mRunning) return;

        mRunning = false;
        for (AnimationCallback callback : new ArrayList<>(mCallbacks)) callback.onAnimationEnd(this);
    }

    @Override
    public void registerAnimationCallback(AnimationCallback callback)
    {
        if (callback == null) throw new NullPointerException("callback");
        if (!mCallbacks.contains(callback)) mCallbacks.add(callback);
    }

    @Override
    public boolean unregisterAnimationCallback(AnimationCallback callback)
    {
        return mCallbacks.remove(callback);
    }

    @Override
    public void clearAnimationCallbacks()
    {
        mCallbacks.clear();
    }

    @Override
    protected void onBoundsChange(Rect bounds)
    {
        invalidateSelf();
    }
}
