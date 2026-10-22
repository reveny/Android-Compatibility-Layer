package android.window;

public final class BackEvent
{
    public static final int EDGE_LEFT = 0;
    public static final int EDGE_RIGHT = 1;
    public static final int EDGE_NONE = 2;

    private final float mTouchX;
    private final float mTouchY;
    private final float mProgress;
    private final int mSwipeEdge;
    private final long mFrameTimeMillis;

    public BackEvent(float touchX, float touchY, float progress, int swipeEdge)
    {
        this(touchX, touchY, progress, swipeEdge, android.os.SystemClock.uptimeMillis());
    }

    public BackEvent(float touchX, float touchY, float progress, int swipeEdge, long frameTimeMillis)
    {
        mTouchX = touchX;
        mTouchY = touchY;
        mProgress = progress;
        mSwipeEdge = swipeEdge;
        mFrameTimeMillis = frameTimeMillis;
    }

    public float getTouchX()
    {
        return mTouchX;
    }

    public float getTouchY()
    {
        return mTouchY;
    }

    public float getProgress()
    {
        return mProgress;
    }

    public int getSwipeEdge()
    {
        return mSwipeEdge;
    }

    public long getFrameTimeMillis()
    {
        return mFrameTimeMillis;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof BackEvent)) return false;

        BackEvent that = (BackEvent)other;
        return mTouchX == that.mTouchX && mTouchY == that.mTouchY && mProgress == that.mProgress && mSwipeEdge == that.mSwipeEdge
               && mFrameTimeMillis == that.mFrameTimeMillis;
    }

    @Override
    public int hashCode()
    {
        int result = Float.floatToIntBits(mTouchX);
        result = 31 * result + Float.floatToIntBits(mTouchY);
        result = 31 * result + Float.floatToIntBits(mProgress);
        result = 31 * result + mSwipeEdge;
        return 31 * result + Long.hashCode(mFrameTimeMillis);
    }

    @Override
    public String toString()
    {
        return "BackEvent{mTouchX=" + mTouchX + ", mTouchY=" + mTouchY + ", mProgress=" + mProgress + ", mSwipeEdge=" + mSwipeEdge
               + ", mFrameTimeMillis=" + mFrameTimeMillis + "}";
    }
}
