package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.StyleableSet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TextClock extends TextView
{
    public static final CharSequence DEFAULT_FORMAT_12_HOUR = "h:mm a";
    public static final CharSequence DEFAULT_FORMAT_24_HOUR = "H:mm";

    private static StyleableSet sStyleable;

    private CharSequence mFormat12 = DEFAULT_FORMAT_12_HOUR;
    private CharSequence mFormat24 = DEFAULT_FORMAT_24_HOUR;
    private CharSequence mFormat;
    private boolean mHasSeconds;
    private String mTimeZone;
    private Calendar mTime;
    private boolean mRegistered;
    private boolean mShouldRunTicker;

    private final Runnable mTicker = new Runnable()
    {
        @Override
        public void run()
        {
            removeCallbacks(this);
            if (!mShouldRunTicker) return;

            onTimeChanged();

            long now = System.currentTimeMillis();
            long interval = mHasSeconds ? 1000 : 60000;
            long next = interval - (now % interval);

            postDelayed(this, next);
        }
    };

    public TextClock(Context context)
    {
        this(context, null);
    }

    public TextClock(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public TextClock(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public TextClock(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        if (attributes != null)
        {
            if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "format12Hour", "format24Hour", "timeZone");

            if (!sStyleable.isEmpty())
            {
                TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

                int index = sStyleable.indexOf("format12Hour");
                if (index >= 0)
                {
                    CharSequence format = array.getText(index);
                    if (format != null) mFormat12 = format;
                }

                index = sStyleable.indexOf("format24Hour");
                if (index >= 0)
                {
                    CharSequence format = array.getText(index);
                    if (format != null) mFormat24 = format;
                }

                index = sStyleable.indexOf("timeZone");
                if (index >= 0) mTimeZone = array.getString(index);

                array.recycle();
            }
        }

        createTime(mTimeZone);
        chooseFormat();
        onTimeChanged();
    }

    private void createTime(String timeZone)
    {
        mTime = timeZone != null ? Calendar.getInstance(TimeZone.getTimeZone(timeZone)) : Calendar.getInstance();
    }

    public CharSequence getFormat12Hour()
    {
        return mFormat12;
    }

    public void setFormat12Hour(CharSequence format)
    {
        mFormat12 = format;
        chooseFormat();
        onTimeChanged();
    }

    public CharSequence getFormat24Hour()
    {
        return mFormat24;
    }

    public void setFormat24Hour(CharSequence format)
    {
        mFormat24 = format;
        chooseFormat();
        onTimeChanged();
    }

    public void refreshTime()
    {
        onTimeChanged();
        invalidate();
    }

    public boolean is24HourModeEnabled()
    {
        try
        {
            String value = Settings.System.getString(getContext().getContentResolver(), Settings.System.TIME_12_24);
            if ("24".equals(value)) return true;
            if ("12".equals(value)) return false;
        }
        catch (RuntimeException unavailable)
        {
            android.util.Log.w("TextClock", "time format setting unavailable", unavailable);
        }

        java.text.DateFormat natural = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault());
        if (natural instanceof SimpleDateFormat) return ((SimpleDateFormat)natural).toPattern().indexOf('H') >= 0;

        return false;
    }

    public String getTimeZone()
    {
        return mTimeZone;
    }

    public void setTimeZone(String timeZone)
    {
        mTimeZone = timeZone;
        createTime(timeZone);
        onTimeChanged();
    }

    private void chooseFormat()
    {
        boolean format24 = is24HourModeEnabled();

        CharSequence chosen = format24 ? firstNonEmpty(mFormat24, mFormat12, DEFAULT_FORMAT_24_HOUR)
                                       : firstNonEmpty(mFormat12, mFormat24, DEFAULT_FORMAT_12_HOUR);

        boolean hadSeconds = mHasSeconds;
        mFormat = chosen;
        mHasSeconds = hasSeconds(chosen);

        if (mShouldRunTicker && hadSeconds != mHasSeconds) mTicker.run();
    }

    private static CharSequence firstNonEmpty(CharSequence first, CharSequence second, CharSequence third)
    {
        if (first != null && first.length() > 0) return first;
        if (second != null && second.length() > 0) return second;

        return third;
    }

    private static boolean hasSeconds(CharSequence format)
    {
        if (format == null) return false;

        boolean quoted = false;

        for (int index = 0; index < format.length(); index++)
        {
            char character = format.charAt(index);

            if (character == '\'') quoted = !quoted;
            else if (!quoted && character == 's') return true;
        }

        return false;
    }

    private void onTimeChanged()
    {
        if (mTime == null || mFormat == null) return;

        mTime.setTimeInMillis(System.currentTimeMillis());

        try
        {
            SimpleDateFormat formatter = new SimpleDateFormat(mFormat.toString(), Locale.getDefault());
            formatter.setTimeZone(mTime.getTimeZone());
            setText(formatter.format(new Date(mTime.getTimeInMillis())));
        }
        catch (IllegalArgumentException malformed)
        {
            setText(mFormat);
        }
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (!mRegistered)
        {
            mRegistered = true;
            createTime(mTimeZone);
            chooseFormat();
        }

        if (!mShouldRunTicker && isShown())
        {
            mShouldRunTicker = true;
            mTicker.run();
        }
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible)
    {
        super.onVisibilityAggregated(isVisible);

        if (!mShouldRunTicker && isVisible)
        {
            mShouldRunTicker = true;
            mTicker.run();
        }
        else if (mShouldRunTicker && !isVisible)
        {
            mShouldRunTicker = false;
            removeCallbacks(mTicker);
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        mRegistered = false;
        mShouldRunTicker = false;
        removeCallbacks(mTicker);
    }
}
