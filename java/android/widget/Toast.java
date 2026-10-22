package android.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class Toast
{
    private static final String TAG = "Toast";
    private static final long SHORT_DURATION_MILLISECONDS = 2000;
    private static final long LONG_DURATION_MILLISECONDS = 3500;

    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    public abstract static class Callback
    {
        public Callback()
        {
        }

        public void onToastShown()
        {
        }

        public void onToastHidden()
        {
        }
    }

    private final Context mContext;
    private final List<Callback> mCallbacks = new ArrayList<>();
    private final Handler mHandler;
    private final Runnable mHide = new Runnable()
    {
        @Override
        public void run()
        {
            hide();
        }
    };

    private CharSequence mText;
    private View mView;
    private int mDuration = LENGTH_SHORT;
    private int mGravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
    private int mXOffset;
    private int mYOffset;
    private float mHorizontalMargin;
    private float mVerticalMargin;
    private boolean mShowing;

    public Toast(Context context)
    {
        mContext = context;
        Looper looper = Looper.myLooper();
        mHandler = new Handler(looper != null ? looper : Looper.getMainLooper());
    }

    public static Toast makeText(Context context, CharSequence text, int duration)
    {
        Toast toast = new Toast(context);
        toast.mText = text;
        toast.mDuration = duration;
        return toast;
    }

    public static Toast makeText(Context context, int resId, int duration)
    {
        return makeText(context, context.getResources().getText(resId), duration);
    }

    public void show()
    {
        if (mText == null && mView == null) throw new RuntimeException("setText must have been called");

        Log.i(TAG, mText != null ? mText.toString() : "(custom view " + mView.getClass().getName() + ")");

        mShowing = true;
        for (Callback callback : new ArrayList<>(mCallbacks)) callback.onToastShown();

        mHandler.removeCallbacks(mHide);
        mHandler.postDelayed(mHide, mDuration == LENGTH_LONG ? LONG_DURATION_MILLISECONDS : SHORT_DURATION_MILLISECONDS);
    }

    public void cancel()
    {
        mHandler.removeCallbacks(mHide);
        hide();
    }

    private void hide()
    {
        if (!mShowing) return;

        mShowing = false;
        for (Callback callback : new ArrayList<>(mCallbacks)) callback.onToastHidden();
    }

    @Deprecated
    public void setView(View view)
    {
        mView = view;
    }

    @Deprecated
    public View getView()
    {
        return mView;
    }

    public void setDuration(int duration)
    {
        mDuration = duration;
    }

    public int getDuration()
    {
        return mDuration;
    }

    public void setMargin(float horizontalMargin, float verticalMargin)
    {
        mHorizontalMargin = horizontalMargin;
        mVerticalMargin = verticalMargin;
    }

    public float getHorizontalMargin()
    {
        return mHorizontalMargin;
    }

    public float getVerticalMargin()
    {
        return mVerticalMargin;
    }

    public void setGravity(int gravity, int xOffset, int yOffset)
    {
        mGravity = gravity;
        mXOffset = xOffset;
        mYOffset = yOffset;
    }

    public int getGravity()
    {
        return mGravity;
    }

    public int getXOffset()
    {
        return mXOffset;
    }

    public int getYOffset()
    {
        return mYOffset;
    }

    public void addCallback(Callback callback)
    {
        if (callback != null) mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback)
    {
        mCallbacks.remove(callback);
    }

    public void setText(int resId)
    {
        setText(mContext.getResources().getText(resId));
    }

    public void setText(CharSequence text)
    {
        mText = text;
    }
}
