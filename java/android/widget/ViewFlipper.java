package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.StyleableSet;

public class ViewFlipper extends ViewAnimator
{
    private static final int DEFAULT_INTERVAL = 3000;

    private static StyleableSet sStyleable;

    private int mFlipInterval = DEFAULT_INTERVAL;
    private boolean mAutoStart;
    private boolean mRunning;
    private boolean mStarted;
    private boolean mVisible;

    private final Runnable mFlipRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (!mRunning) return;

            showNext();
            postDelayed(this, mFlipInterval);
        }
    };

    public ViewFlipper(Context context)
    {
        super(context);
    }

    public ViewFlipper(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        if (attributes == null) return;

        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "flipInterval", "autoStart");
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), 0, 0);

        int index = sStyleable.indexOf("flipInterval");
        if (index >= 0) mFlipInterval = array.getInt(index, DEFAULT_INTERVAL);

        index = sStyleable.indexOf("autoStart");
        if (index >= 0) mAutoStart = array.getBoolean(index, false);

        array.recycle();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        mVisible = getWindowVisibility() == VISIBLE;

        if (mAutoStart) startFlipping();
        else updateRunning();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        mVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility)
    {
        super.onWindowVisibilityChanged(visibility);

        mVisible = visibility == VISIBLE;
        updateRunning(false);
    }

    public void setFlipInterval(int milliseconds)
    {
        mFlipInterval = milliseconds;
    }

    public int getFlipInterval()
    {
        return mFlipInterval;
    }

    public void startFlipping()
    {
        mStarted = true;
        updateRunning();
    }

    public void stopFlipping()
    {
        mStarted = false;
        updateRunning();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return ViewFlipper.class.getName();
    }

    private void updateRunning()
    {
        updateRunning(true);
    }

    private void updateRunning(boolean flipNow)
    {
        boolean running = mVisible && mStarted;
        if (running == mRunning) return;

        if (running)
        {
            showOnly(mWhichChild, flipNow);
            postDelayed(mFlipRunnable, mFlipInterval);
        }
        else
        {
            removeCallbacks(mFlipRunnable);
        }

        mRunning = running;
    }

    public boolean isFlipping()
    {
        return mStarted;
    }

    public void setAutoStart(boolean autoStart)
    {
        mAutoStart = autoStart;
    }

    public boolean isAutoStart()
    {
        return mAutoStart;
    }
}
