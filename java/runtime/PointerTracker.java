package runtime;

import android.view.MotionEvent;

public final class PointerTracker
{
    public static final int MAX_POINTERS = 10;

    private final int[] mPointerIds = new int[MAX_POINTERS];
    private final float[] mPointerX = new float[MAX_POINTERS];
    private final float[] mPointerY = new float[MAX_POINTERS];

    private int mPointerCount;
    private long mDownTime;

    public interface Sink
    {
        void onMotion(MotionEvent event);
    }

    public int getPointerCount()
    {
        return mPointerCount;
    }

    public void reset()
    {
        mPointerCount = 0;
        mDownTime = 0;
    }

    public void track(int action, int pointerId, float x, float y, long eventTime, int metaState, Sink sink)
    {
        if (sink == null) return;

        if (isHover(action))
        {
            MotionEvent hover = MotionEvent.obtain(mDownTime, eventTime, action, x, y, metaState);
            sink.onMotion(hover);
            hover.recycle();

            return;
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)
        {
            press(pointerId, x, y, eventTime, metaState, sink);
            return;
        }

        int index = indexOf(pointerId);
        if (index < 0) return;

        mPointerX[index] = x;
        mPointerY[index] = y;

        if (action == MotionEvent.ACTION_MOVE)
        {
            send(MotionEvent.ACTION_MOVE, eventTime, metaState, sink);
            return;
        }

        if (action == MotionEvent.ACTION_CANCEL)
        {
            send(MotionEvent.ACTION_CANCEL, eventTime, metaState, sink);
            mPointerCount = 0;
            return;
        }

        if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_POINTER_UP) return;

        int reported = mPointerCount == 1 ? MotionEvent.ACTION_UP
                                          : MotionEvent.ACTION_POINTER_UP | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT);

        send(reported, eventTime, metaState, sink);
        forget(index);
    }

    private static boolean isHover(int action)
    {
        return action == MotionEvent.ACTION_HOVER_MOVE || action == MotionEvent.ACTION_HOVER_ENTER
               || action == MotionEvent.ACTION_HOVER_EXIT || action == MotionEvent.ACTION_SCROLL;
    }

    private void press(int pointerId, float x, float y, long eventTime, int metaState, Sink sink)
    {
        int index = indexOf(pointerId);

        if (index < 0)
        {
            if (mPointerCount >= MAX_POINTERS) return;

            index = mPointerCount;
            mPointerIds[index] = pointerId;
            mPointerCount++;
        }

        mPointerX[index] = x;
        mPointerY[index] = y;

        if (mPointerCount == 1) mDownTime = eventTime;

        int reported = mPointerCount == 1 ? MotionEvent.ACTION_DOWN
                                          : MotionEvent.ACTION_POINTER_DOWN
                                            | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT);

        send(reported, eventTime, metaState, sink);
    }

    private int indexOf(int pointerId)
    {
        for (int index = 0; index < mPointerCount; index++)
        {
            if (mPointerIds[index] == pointerId) return index;
        }

        return -1;
    }

    private void forget(int index)
    {
        for (int move = index; move + 1 < mPointerCount; move++)
        {
            mPointerIds[move] = mPointerIds[move + 1];
            mPointerX[move] = mPointerX[move + 1];
            mPointerY[move] = mPointerY[move + 1];
        }

        if (mPointerCount > 0) mPointerCount--;
    }

    private void send(int action, long eventTime, int metaState, Sink sink)
    {
        if (mPointerCount <= 0) return;

        MotionEvent event = MotionEvent.obtain(mDownTime, eventTime, action, mPointerCount, mPointerIds, mPointerX,
                                               mPointerY, metaState);

        sink.onMotion(event);
        event.recycle();
    }
}
