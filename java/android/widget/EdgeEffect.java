package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class EdgeEffect
{
    public static final int TYPE_NONE = 0;
    public static final int TYPE_GLOW = 1;
    public static final int TYPE_STRETCH = 2;

    private static final int RECEDE_MILLIS = 600;
    private static final int PULL_MILLIS = 167;
    private static final float MAX_ALPHA = 0.15f;

    public static final android.graphics.BlendMode DEFAULT_BLEND_MODE = android.graphics.BlendMode.SRC_ATOP;

    private final Paint mPaint = new Paint();
    private android.graphics.BlendMode mBlendMode = DEFAULT_BLEND_MODE;
    private final Rect mBounds = new Rect();

    private float mDistance;
    private float mVelocity;
    private float mGlowAlpha;
    private long mStartTime;
    private boolean mFinished = true;
    private int mColor = 0xFF666666;

    public EdgeEffect(Context context)
    {
        mPaint.setAntiAlias(true);
    }

    public void setBlendMode(android.graphics.BlendMode blendMode)
    {
        mBlendMode = blendMode;
        mPaint.setBlendMode(blendMode);
    }

    public android.graphics.BlendMode getBlendMode()
    {
        return mBlendMode;
    }

    public EdgeEffect(Context context, android.util.AttributeSet attributes)
    {
        this(context);
    }

    public void setSize(int width, int height)
    {
        mBounds.set(0, 0, width, height);
    }

    public boolean isFinished()
    {
        return mFinished;
    }

    public void finish()
    {
        mFinished = true;
        mGlowAlpha = 0.0f;
        mDistance = 0.0f;
        mVelocity = 0.0f;
    }

    public void onPull(float deltaDistance)
    {
        onPull(deltaDistance, 0.5f);
    }

    public void onPull(float deltaDistance, float displacement)
    {
        mFinished = false;
        mStartTime = android.os.SystemClock.uptimeMillis();

        mDistance = Math.max(0.0f, mDistance + deltaDistance);
        mGlowAlpha = Math.min(MAX_ALPHA, mGlowAlpha + Math.abs(deltaDistance) * 0.8f);
    }

    public float onPullDistance(float deltaDistance, float displacement)
    {
        onPull(deltaDistance, displacement);

        return deltaDistance;
    }

    public float getDistance()
    {
        return mDistance;
    }

    public void onRelease()
    {
        if (mFinished) return;

        mStartTime = android.os.SystemClock.uptimeMillis();
        mDistance = 0.0f;
    }

    public void onAbsorb(int velocity)
    {
        mFinished = false;
        mVelocity = velocity;
        mStartTime = android.os.SystemClock.uptimeMillis();
        mGlowAlpha = MAX_ALPHA;
    }

    public void setColor(int color)
    {
        mColor = color;
    }

    public int getColor()
    {
        return mColor;
    }

    public int getMaxHeight()
    {
        return mBounds.height();
    }

    public boolean draw(Canvas canvas)
    {
        if (mFinished || canvas == null) return false;

        long elapsed = android.os.SystemClock.uptimeMillis() - mStartTime;

        if (elapsed >= RECEDE_MILLIS)
        {
            finish();
            return false;
        }

        float alpha = mGlowAlpha * (1.0f - (float)elapsed / RECEDE_MILLIS);

        if (alpha <= 0.0f)
        {
            finish();
            return false;
        }

        mPaint.setColor((Math.round(alpha * 255.0f) << 24) | (mColor & 0x00FFFFFF));
        canvas.drawRect(mBounds.left, mBounds.top, mBounds.right, mBounds.top + mBounds.height() * 0.1f, mPaint);

        return true;
    }
}
