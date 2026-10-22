package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.StyleableSet;
import android.view.View;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalendarView extends FrameLayout
{
    private static final String DEFAULT_MIN_DATE = "01/01/1900";
    private static final String DEFAULT_MAX_DATE = "01/01/2100";

    private static StyleableSet sStyleable;

    private final Calendar mDate = Calendar.getInstance();
    private final Calendar mMinDate = Calendar.getInstance();
    private final Calendar mMaxDate = Calendar.getInstance();
    private final Calendar mShownMonth = Calendar.getInstance();
    private final MonthView mMonthView;
    private OnDateChangeListener mOnDateChangeListener;
    private int mFirstDayOfWeek;
    private boolean mShowWeekNumber;
    private int mShownWeekCount = 6;
    private int mSelectedWeekBackgroundColor;
    private int mFocusedMonthDateColor;
    private int mUnfocusedMonthDateColor;
    private int mWeekSeparatorLineColor;
    private int mWeekNumberColor;
    private int mDateTextAppearance;
    private int mWeekDayTextAppearance;
    private Drawable mSelectedDateVerticalBar;

    public CalendarView(Context context)
    {
        this(context, null);
    }

    public CalendarView(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "calendarViewStyle"));
    }

    public CalendarView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public CalendarView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mFirstDayOfWeek = mDate.getFirstDayOfWeek();
        mSelectedWeekBackgroundColor = ProgressBar.resolveThemeColor(context, 0xFF009688, "colorControlActivated", "colorAccent");
        mFocusedMonthDateColor = ProgressBar.resolveThemeColor(context, 0xDE000000, "textColorPrimary");
        mUnfocusedMonthDateColor = ProgressBar.resolveThemeColor(context, 0x8A000000, "textColorSecondary");
        mWeekSeparatorLineColor = 0x1F000000;
        mWeekNumberColor = mUnfocusedMonthDateColor;

        parseDate(DEFAULT_MIN_DATE, mMinDate);
        parseDate(DEFAULT_MAX_DATE, mMaxDate);

        if (attributes != null || defStyleAttr != 0 || defStyleRes != 0) readAttributes(context, attributes, defStyleAttr, defStyleRes);

        clamp(mDate);
        mShownMonth.setTimeInMillis(mDate.getTimeInMillis());

        mMonthView = new MonthView(context);
        addView(mMonthView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private void readAttributes(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "firstDayOfWeek", "minDate", "maxDate", "showWeekNumber", "shownWeekCount",
                                         "selectedWeekBackgroundColor", "focusedMonthDateColor", "unfocusedMonthDateColor",
                                         "weekNumberColor", "weekSeparatorLineColor", "selectedDateVerticalBar", "weekDayTextAppearance",
                                         "dateTextAppearance");
        }
        if (sStyleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

        int index = sStyleable.indexOf("firstDayOfWeek");
        if (index >= 0 && array.hasValue(index)) mFirstDayOfWeek = array.getInt(index, mFirstDayOfWeek);

        index = sStyleable.indexOf("minDate");
        if (index >= 0 && array.hasValue(index)) parseDate(array.getString(index), mMinDate);

        index = sStyleable.indexOf("maxDate");
        if (index >= 0 && array.hasValue(index)) parseDate(array.getString(index), mMaxDate);

        index = sStyleable.indexOf("showWeekNumber");
        if (index >= 0) mShowWeekNumber = array.getBoolean(index, mShowWeekNumber);

        index = sStyleable.indexOf("shownWeekCount");
        if (index >= 0) mShownWeekCount = array.getInt(index, mShownWeekCount);

        mSelectedWeekBackgroundColor = readColor(array, "selectedWeekBackgroundColor", mSelectedWeekBackgroundColor);
        mFocusedMonthDateColor = readColor(array, "focusedMonthDateColor", mFocusedMonthDateColor);
        mUnfocusedMonthDateColor = readColor(array, "unfocusedMonthDateColor", mUnfocusedMonthDateColor);
        mWeekNumberColor = readColor(array, "weekNumberColor", mWeekNumberColor);
        mWeekSeparatorLineColor = readColor(array, "weekSeparatorLineColor", mWeekSeparatorLineColor);

        index = sStyleable.indexOf("weekDayTextAppearance");
        if (index >= 0) mWeekDayTextAppearance = array.getResourceId(index, 0);

        index = sStyleable.indexOf("dateTextAppearance");
        if (index >= 0) mDateTextAppearance = array.getResourceId(index, 0);

        index = sStyleable.indexOf("selectedDateVerticalBar");
        if (index >= 0 && array.hasValue(index))
        {
            try
            {
                mSelectedDateVerticalBar = array.getDrawable(index);
            }
            catch (RuntimeException unsupported)
            {
                mSelectedDateVerticalBar = null;
            }
        }

        array.recycle();
    }

    private static int readColor(TypedArray array, String name, int fallback)
    {
        int index = sStyleable.indexOf(name);
        if (index < 0 || !array.hasValue(index)) return fallback;

        return array.getColor(index, fallback);
    }

    static boolean parseDate(String date, Calendar outDate)
    {
        if (date == null || date.isEmpty()) return false;

        try
        {
            outDate.setTime(new SimpleDateFormat("MM/dd/yyyy", Locale.US).parse(date));
            return true;
        }
        catch (ParseException malformed)
        {
            android.util.Log.w("CalendarView", "Date: " + date + " not in format: MM/dd/yyyy");
            return false;
        }
    }

    private void clamp(Calendar date)
    {
        if (date.before(mMinDate)) date.setTimeInMillis(mMinDate.getTimeInMillis());
        else if (date.after(mMaxDate)) date.setTimeInMillis(mMaxDate.getTimeInMillis());
    }

    public void setShownWeekCount(int count)
    {
        mShownWeekCount = count;
    }

    public int getShownWeekCount()
    {
        return mShownWeekCount;
    }

    public void setSelectedWeekBackgroundColor(int color)
    {
        mSelectedWeekBackgroundColor = color;
        mMonthView.invalidate();
    }

    public int getSelectedWeekBackgroundColor()
    {
        return mSelectedWeekBackgroundColor;
    }

    public void setFocusedMonthDateColor(int color)
    {
        mFocusedMonthDateColor = color;
        mMonthView.invalidate();
    }

    public int getFocusedMonthDateColor()
    {
        return mFocusedMonthDateColor;
    }

    public void setUnfocusedMonthDateColor(int color)
    {
        mUnfocusedMonthDateColor = color;
        mMonthView.invalidate();
    }

    public int getUnfocusedMonthDateColor()
    {
        return mUnfocusedMonthDateColor;
    }

    public void setWeekNumberColor(int color)
    {
        mWeekNumberColor = color;
        mMonthView.invalidate();
    }

    public int getWeekNumberColor()
    {
        return mWeekNumberColor;
    }

    public void setWeekSeparatorLineColor(int color)
    {
        mWeekSeparatorLineColor = color;
        mMonthView.invalidate();
    }

    public int getWeekSeparatorLineColor()
    {
        return mWeekSeparatorLineColor;
    }

    public void setSelectedDateVerticalBar(int resourceId)
    {
        setSelectedDateVerticalBar(resourceId == 0 ? null : getContext().getDrawable(resourceId));
    }

    public void setSelectedDateVerticalBar(Drawable drawable)
    {
        mSelectedDateVerticalBar = drawable;
        mMonthView.invalidate();
    }

    public Drawable getSelectedDateVerticalBar()
    {
        return mSelectedDateVerticalBar;
    }

    public void setWeekDayTextAppearance(int resourceId)
    {
        mWeekDayTextAppearance = resourceId;
    }

    public int getWeekDayTextAppearance()
    {
        return mWeekDayTextAppearance;
    }

    public void setDateTextAppearance(int resourceId)
    {
        mDateTextAppearance = resourceId;
    }

    public int getDateTextAppearance()
    {
        return mDateTextAppearance;
    }

    public void setMinDate(long minDate)
    {
        mMinDate.setTimeInMillis(minDate);
        if (mMaxDate.before(mMinDate)) mMaxDate.setTimeInMillis(minDate);
        clamp(mDate);
        clamp(mShownMonth);
        mMonthView.invalidate();
    }

    public long getMinDate()
    {
        return mMinDate.getTimeInMillis();
    }

    public void setMaxDate(long maxDate)
    {
        mMaxDate.setTimeInMillis(maxDate);
        if (mMinDate.after(mMaxDate)) mMinDate.setTimeInMillis(maxDate);
        clamp(mDate);
        clamp(mShownMonth);
        mMonthView.invalidate();
    }

    public long getMaxDate()
    {
        return mMaxDate.getTimeInMillis();
    }

    public void setShowWeekNumber(boolean showWeekNumber)
    {
        mShowWeekNumber = showWeekNumber;
        mMonthView.invalidate();
    }

    public boolean getShowWeekNumber()
    {
        return mShowWeekNumber;
    }

    public void setFirstDayOfWeek(int firstDayOfWeek)
    {
        mFirstDayOfWeek = firstDayOfWeek;
        mMonthView.invalidate();
    }

    public int getFirstDayOfWeek()
    {
        return mFirstDayOfWeek;
    }

    public void setOnDateChangeListener(OnDateChangeListener listener)
    {
        mOnDateChangeListener = listener;
    }

    public long getDate()
    {
        return mDate.getTimeInMillis();
    }

    public void setDate(long date)
    {
        setDate(date, false, false);
    }

    public void setDate(long date, boolean animate, boolean center)
    {
        mDate.setTimeInMillis(date);
        clamp(mDate);
        mShownMonth.setTimeInMillis(mDate.getTimeInMillis());
        mMonthView.invalidate();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        mMonthView.requestLayout();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return CalendarView.class.getName();
    }

    private void selectDay(int year, int month, int dayOfMonth)
    {
        Calendar candidate = Calendar.getInstance();
        candidate.setTimeInMillis(mDate.getTimeInMillis());
        candidate.set(year, month, dayOfMonth);

        if (candidate.before(startOfDay(mMinDate)) || candidate.after(mMaxDate)) return;

        mDate.setTimeInMillis(candidate.getTimeInMillis());
        mShownMonth.setTimeInMillis(candidate.getTimeInMillis());
        mMonthView.invalidate();

        if (mOnDateChangeListener != null) mOnDateChangeListener.onSelectedDayChange(this, year, month, dayOfMonth);
    }

    private static Calendar startOfDay(Calendar source)
    {
        Calendar result = Calendar.getInstance();
        result.setTimeInMillis(source.getTimeInMillis());
        result.set(Calendar.HOUR_OF_DAY, 0);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        return result;
    }

    private void moveMonth(int delta)
    {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(mShownMonth.getTimeInMillis());
        next.set(Calendar.DAY_OF_MONTH, 1);
        next.add(Calendar.MONTH, delta);

        Calendar firstAllowed = startOfDay(mMinDate);
        firstAllowed.set(Calendar.DAY_OF_MONTH, 1);
        if (next.before(firstAllowed) || next.after(mMaxDate)) return;

        mShownMonth.setTimeInMillis(next.getTimeInMillis());
        mMonthView.invalidate();
    }

    public interface OnDateChangeListener
    {
        void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth);
    }

    private class MonthView extends View
    {
        private static final int ROWS = 6;

        private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float mDensity;

        private MonthView(Context context)
        {
            super(context);

            mDensity = context.getResources().getDisplayMetrics().density;
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            setClickable(true);
        }

        private float headerHeight()
        {
            return 48 * mDensity;
        }

        private float weekdayHeight()
        {
            return 32 * mDensity;
        }

        private float rowHeight()
        {
            return 40 * mDensity;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            int width = (int)(7 * 44 * mDensity) + getPaddingLeft() + getPaddingRight();
            int height = (int)(headerHeight() + weekdayHeight() + ROWS * rowHeight()) + getPaddingTop() + getPaddingBottom();

            setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
        }

        private Calendar firstCell()
        {
            Calendar first = Calendar.getInstance();
            first.setTimeInMillis(mShownMonth.getTimeInMillis());
            first.set(Calendar.DAY_OF_MONTH, 1);

            int offset = (first.get(Calendar.DAY_OF_WEEK) - mFirstDayOfWeek + 7) % 7;
            first.add(Calendar.DAY_OF_MONTH, -offset);
            return first;
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);

            float left = getPaddingLeft();
            float width = getWidth() - getPaddingLeft() - getPaddingRight();
            float cellWidth = width / 7f;
            float top = getPaddingTop();

            mTextPaint.setColor(mFocusedMonthDateColor);
            mTextPaint.setTextSize(16 * mDensity);
            mTextPaint.setFakeBoldText(true);

            String title = new SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(mShownMonth.getTime());
            float headerBaseline = top + headerHeight() / 2 - (mTextPaint.ascent() + mTextPaint.descent()) / 2;
            canvas.drawText(title, left + width / 2, headerBaseline, mTextPaint);
            canvas.drawText("‹", left + cellWidth / 2, headerBaseline, mTextPaint);
            canvas.drawText("›", left + width - cellWidth / 2, headerBaseline, mTextPaint);

            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSize(12 * mDensity);
            mTextPaint.setColor(mUnfocusedMonthDateColor);

            String[] weekdays = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
            float weekdayTop = top + headerHeight();
            float weekdayBaseline = weekdayTop + weekdayHeight() / 2 - (mTextPaint.ascent() + mTextPaint.descent()) / 2;
            for (int column = 0; column < 7; column++)
            {
                int day = (mFirstDayOfWeek - 1 + column) % 7 + 1;
                String label = weekdays[day];
                if (label.length() > 2) label = label.substring(0, 2);
                canvas.drawText(label, left + cellWidth * column + cellWidth / 2, weekdayBaseline, mTextPaint);
            }

            mFillPaint.setColor(mWeekSeparatorLineColor);
            canvas.drawRect(left, weekdayTop + weekdayHeight() - mDensity, left + width, weekdayTop + weekdayHeight(), mFillPaint);

            Calendar cell = firstCell();
            Calendar minimum = startOfDay(mMinDate);
            int shownMonth = mShownMonth.get(Calendar.MONTH);
            float gridTop = weekdayTop + weekdayHeight();
            mTextPaint.setTextSize(14 * mDensity);

            for (int row = 0; row < ROWS; row++)
            {
                for (int column = 0; column < 7; column++)
                {
                    float centerX = left + cellWidth * column + cellWidth / 2;
                    float centerY = gridTop + rowHeight() * row + rowHeight() / 2;

                    boolean selected = cell.get(Calendar.YEAR) == mDate.get(Calendar.YEAR)
                                       && cell.get(Calendar.DAY_OF_YEAR) == mDate.get(Calendar.DAY_OF_YEAR);
                    boolean enabled = !cell.before(minimum) && !cell.after(mMaxDate);
                    boolean inMonth = cell.get(Calendar.MONTH) == shownMonth;

                    if (selected)
                    {
                        mFillPaint.setColor(mSelectedWeekBackgroundColor);
                        canvas.drawCircle(centerX, centerY, Math.min(cellWidth, rowHeight()) / 2 - 2 * mDensity, mFillPaint);
                    }

                    int colour = inMonth ? mFocusedMonthDateColor : mUnfocusedMonthDateColor;
                    if (selected) colour = 0xFFFFFFFF;
                    if (!enabled) colour = (colour & 0x00FFFFFF) | 0x42000000;
                    mTextPaint.setColor(colour);

                    float baseline = centerY - (mTextPaint.ascent() + mTextPaint.descent()) / 2;
                    canvas.drawText(Integer.toString(cell.get(Calendar.DAY_OF_MONTH)), centerX, baseline, mTextPaint);

                    cell.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            if (!isEnabled()) return false;
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;

            float x = event.getX() - getPaddingLeft();
            float y = event.getY() - getPaddingTop();
            float width = getWidth() - getPaddingLeft() - getPaddingRight();
            float cellWidth = width / 7f;

            if (y < headerHeight())
            {
                if (x < cellWidth) moveMonth(-1);
                else if (x > width - cellWidth) moveMonth(1);
                return true;
            }

            float gridY = y - headerHeight() - weekdayHeight();
            if (gridY < 0) return true;

            int row = (int)(gridY / rowHeight());
            int column = (int)(x / cellWidth);
            if (row < 0 || row >= ROWS || column < 0 || column >= 7) return true;

            Calendar cell = firstCell();
            cell.add(Calendar.DAY_OF_MONTH, row * 7 + column);
            selectDay(cell.get(Calendar.YEAR), cell.get(Calendar.MONTH), cell.get(Calendar.DAY_OF_MONTH));
            playSoundEffect(android.view.SoundEffectConstants.CLICK);
            return true;
        }
    }
}
