package android.view;

import android.graphics.Rect;

public class TouchDelegate
{
    public static final int ABOVE = 1;
    public static final int BELOW = 2;
    public static final int TO_LEFT = 4;
    public static final int TO_RIGHT = 8;

    private final View mDelegateView;
    private final Rect mBounds;
    private final Rect mSlopBounds;
    private final int mSlop;
    private boolean mDelegateTargeted;

    public TouchDelegate(Rect bounds, View delegateView)
    {
        mBounds = bounds;
        mDelegateView = delegateView;
        mSlop = ViewConfiguration.get(delegateView.getContext()).getScaledTouchSlop();
        mSlopBounds = new Rect(bounds);
        mSlopBounds.inset(-mSlop, -mSlop);
    }

    public boolean onTouchEvent(MotionEvent event)
    {
        int x = (int)event.getX();
        int y = (int)event.getY();
        boolean sendToDelegate = false;
        boolean hit = true;
        boolean handled = false;

        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                mDelegateTargeted = mBounds.contains(x, y);
                sendToDelegate = mDelegateTargeted;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE:
                sendToDelegate = mDelegateTargeted;
                if (sendToDelegate && !mSlopBounds.contains(x, y)) hit = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                sendToDelegate = mDelegateTargeted;
                mDelegateTargeted = false;
                break;
            default:
                break;
        }

        if (!sendToDelegate) return false;

        float offsetX;
        float offsetY;

        if (hit)
        {
            offsetX = mDelegateView.getWidth() / 2.0f - x;
            offsetY = mDelegateView.getHeight() / 2.0f - y;
        }
        else
        {
            offsetX = -(mSlop * 2) - x;
            offsetY = -(mSlop * 2) - y;
        }

        event.offsetLocation(offsetX, offsetY);

        try
        {
            handled = mDelegateView.dispatchTouchEvent(event);
        }
        finally
        {
            event.offsetLocation(-offsetX, -offsetY);
        }

        return handled;
    }

    public boolean onTouchExplorationHoverEvent(MotionEvent event)
    {
        if (mBounds == null) return false;

        int x = (int)event.getX();
        int y = (int)event.getY();
        if (!mBounds.contains(x, y)) return false;

        float offsetX = mDelegateView.getWidth() / 2.0f - x;
        float offsetY = mDelegateView.getHeight() / 2.0f - y;

        event.offsetLocation(offsetX, offsetY);

        try
        {
            return mDelegateView.dispatchHoverEvent(event);
        }
        finally
        {
            event.offsetLocation(-offsetX, -offsetY);
        }
    }

    public android.view.accessibility.AccessibilityNodeInfo.TouchDelegateInfo getTouchDelegateInfo()
    {
        android.util.ArrayMap<android.graphics.Region, View> targetMap = new android.util.ArrayMap<>(1);
        targetMap.put(new android.graphics.Region(mBounds), mDelegateView);
        return new android.view.accessibility.AccessibilityNodeInfo.TouchDelegateInfo(targetMap);
    }
}
