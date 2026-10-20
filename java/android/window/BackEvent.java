package android.window;

public final class BackEvent
{
    public static final int EDGE_LEFT = 0;
    public static final int EDGE_RIGHT = 1;

    private final float mTouchX;
    private final float mTouchY;
    private final float mProgress;
    private final int mSwipeEdge;

    public BackEvent(float touchX, float touchY, float progress, int swipeEdge)
    {
        mTouchX = touchX;
        mTouchY = touchY;
        mProgress = progress;
        mSwipeEdge = swipeEdge;
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
}
