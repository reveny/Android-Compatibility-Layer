package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.StyleableSet;
import android.view.View;

import java.util.Locale;

public class Chronometer extends TextView
{
    public interface OnChronometerTickListener
    {
        void onChronometerTick(Chronometer chronometer);
    }

    private static final int TICK_INTERVAL = 1000;

    private static StyleableSet sStyleable;

    private long mBase;
    private long mNow;
    private boolean mVisible;
    private boolean mStarted;
    private boolean mRunning;
    private boolean mCountDown;
    private String mFormat;
    private OnChronometerTickListener mOnChronometerTickListener;

    private final Runnable mTickRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (!mRunning) return;

            updateText(SystemClock.elapsedRealtime());
            dispatchChronometerTick();
            postDelayed(this, TICK_INTERVAL);
        }
    };

    public Chronometer(Context context)
    {
        this(context, null, 0);
    }

    public Chronometer(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public Chronometer(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public Chronometer(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes != null)
        {
            if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "format", "countDown");

            if (!sStyleable.isEmpty())
            {
                TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

                int index = sStyleable.indexOf("format");
                if (index >= 0) setFormat(array.getString(index));

                index = sStyleable.indexOf("countDown");
                if (index >= 0) mCountDown = array.getBoolean(index, false);

                array.recycle();
            }
        }

        mBase = SystemClock.elapsedRealtime();
        updateText(mBase);
    }

    public void setCountDown(boolean countDown)
    {
        mCountDown = countDown;
        updateText(SystemClock.elapsedRealtime());
    }

    public boolean isCountDown()
    {
        return mCountDown;
    }

    public boolean isTheFinalCountDown()
    {
        return false;
    }

    public void setBase(long base)
    {
        mBase = base;
        dispatchChronometerTick();
        updateText(SystemClock.elapsedRealtime());
    }

    public long getBase()
    {
        return mBase;
    }

    public void setFormat(String format)
    {
        mFormat = format;
    }

    public String getFormat()
    {
        return mFormat;
    }

    public void setOnChronometerTickListener(OnChronometerTickListener listener)
    {
        mOnChronometerTickListener = listener;
    }

    public OnChronometerTickListener getOnChronometerTickListener()
    {
        return mOnChronometerTickListener;
    }

    public void start()
    {
        mStarted = true;
        updateRunning();
    }

    public void stop()
    {
        mStarted = false;
        updateRunning();
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
        updateRunning();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility)
    {
        super.onVisibilityChanged(changedView, visibility);

        updateRunning();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        mVisible = getWindowVisibility() == VISIBLE;
        updateRunning();
    }

    private synchronized void updateText(long now)
    {
        mNow = now;

        long seconds = mCountDown ? mBase - now : now - mBase;
        seconds /= 1000;

        boolean negative = false;

        if (seconds < 0)
        {
            seconds = -seconds;
            negative = true;
        }

        String text = formatElapsedTime(seconds);
        if (negative) text = "−" + text;

        if (mFormat != null)
        {
            try
            {
                text = String.format(Locale.getDefault(), mFormat, text);
            }
            catch (RuntimeException malformed)
            {
                android.util.Log.w("Chronometer", "Illegal format string: " + mFormat);
            }
        }

        setText(text);
    }

    static String formatElapsedTime(long elapsedSeconds)
    {
        long hours = elapsedSeconds / 3600;
        long minutes = (elapsedSeconds % 3600) / 60;
        long seconds = elapsedSeconds % 60;

        if (hours > 0) return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);

        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void updateRunning()
    {
        boolean running = mVisible && mStarted && isShown();
        if (running == mRunning) return;

        if (running)
        {
            updateText(SystemClock.elapsedRealtime());
            dispatchChronometerTick();
            postDelayed(mTickRunnable, TICK_INTERVAL);
        }
        else
        {
            removeCallbacks(mTickRunnable);
        }

        mRunning = running;
    }

    void dispatchChronometerTick()
    {
        if (mOnChronometerTickListener != null) mOnChronometerTickListener.onChronometerTick(this);
    }

    @Override
    public CharSequence getContentDescription()
    {
        CharSequence description = super.getContentDescription();
        if (description != null) return description;

        long elapsed = Math.abs(mCountDown ? mBase - mNow : mNow - mBase) / 1000;
        long hours = elapsed / 3600;
        long minutes = (elapsed % 3600) / 60;
        long seconds = elapsed % 60;

        StringBuilder builder = new StringBuilder();
        if (hours > 0) builder.append(hours).append(hours == 1 ? " hour, " : " hours, ");
        if (hours > 0 || minutes > 0) builder.append(minutes).append(minutes == 1 ? " minute, " : " minutes, ");
        builder.append(seconds).append(seconds == 1 ? " second" : " seconds");

        return builder.toString();
    }

    public CharSequence getAccessibilityClassName()
    {
        return Chronometer.class.getName();
    }
}
