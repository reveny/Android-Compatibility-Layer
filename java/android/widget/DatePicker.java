package android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.view.autofill.AutofillValue;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class DatePicker extends FrameLayout
{
    private static final int MODE_SPINNER = 1;
    private static final int MODE_CALENDAR = 2;
    private static final String DEFAULT_MIN_DATE = "01/01/1900";
    private static final String DEFAULT_MAX_DATE = "12/31/2100";

    private static StyleableSet sStyleable;

    private final Calendar mCurrentDate = Calendar.getInstance();
    private final Calendar mMinDate = Calendar.getInstance();
    private final Calendar mMaxDate = Calendar.getInstance();
    private final int mMode;
    private final LinearLayout mContainer;
    private final LinearLayout mSpinners;
    private final NumberPicker mMonthSpinner;
    private final NumberPicker mDaySpinner;
    private final NumberPicker mYearSpinner;
    private CalendarView mCalendarView;
    private OnDateChangedListener mOnDateChangedListener;
    private OnDateChangedListener mAutoFillChangeListener;
    private int mFirstDayOfWeek;
    private boolean mSpinnersShown = true;
    private boolean mCalendarViewShown;
    private String[] mShortMonths;

    public DatePicker(Context context)
    {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "datePickerStyle"));
    }

    public DatePicker(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public DatePicker(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mFirstDayOfWeek = mCurrentDate.getFirstDayOfWeek();
        CalendarView.parseDate(DEFAULT_MIN_DATE, mMinDate);
        CalendarView.parseDate(DEFAULT_MAX_DATE, mMaxDate);

        int mode = MODE_CALENDAR;

        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "datePickerMode", "spinnersShown", "calendarViewShown", "minDate", "maxDate",
                                         "startYear", "endYear", "firstDayOfWeek");
        }

        if (!sStyleable.isEmpty() && (attributes != null || defStyleAttr != 0 || defStyleRes != 0))
        {
            TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);

            int index = sStyleable.indexOf("datePickerMode");
            if (index >= 0) mode = array.getInt(index, mode);

            index = sStyleable.indexOf("spinnersShown");
            if (index >= 0) mSpinnersShown = array.getBoolean(index, true);

            index = sStyleable.indexOf("calendarViewShown");
            if (index >= 0) mCalendarViewShown = array.getBoolean(index, false);

            index = sStyleable.indexOf("startYear");
            if (index >= 0 && array.hasValue(index)) mMinDate.set(array.getInt(index, 1900), Calendar.JANUARY, 1);

            index = sStyleable.indexOf("endYear");
            if (index >= 0 && array.hasValue(index)) mMaxDate.set(array.getInt(index, 2100), Calendar.DECEMBER, 31);

            index = sStyleable.indexOf("minDate");
            if (index >= 0 && array.hasValue(index)) CalendarView.parseDate(array.getString(index), mMinDate);

            index = sStyleable.indexOf("maxDate");
            if (index >= 0 && array.hasValue(index)) CalendarView.parseDate(array.getString(index), mMaxDate);

            index = sStyleable.indexOf("firstDayOfWeek");
            if (index >= 0 && array.hasValue(index)) mFirstDayOfWeek = array.getInt(index, mFirstDayOfWeek);

            array.recycle();
        }

        mMode = mode == MODE_SPINNER ? MODE_SPINNER : MODE_CALENDAR;
        if (mMaxDate.before(mMinDate)) mMaxDate.setTimeInMillis(mMinDate.getTimeInMillis());
        clampCurrent();

        mShortMonths = new DateFormatSymbols(Locale.getDefault()).getShortMonths();

        mContainer = new LinearLayout(context);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setGravity(Gravity.CENTER);

        mSpinners = new LinearLayout(context);
        mSpinners.setOrientation(LinearLayout.HORIZONTAL);
        mSpinners.setGravity(Gravity.CENTER);

        NumberPicker.OnValueChangeListener listener = new NumberPicker.OnValueChangeListener()
        {
            public void onValueChange(NumberPicker picker, int oldValue, int newValue)
            {
                onSpinnerChanged(picker, oldValue, newValue);
            }
        };

        mMonthSpinner = createSpinner(context, listener);
        mDaySpinner = createSpinner(context, listener);
        mYearSpinner = createSpinner(context, listener);
        mYearSpinner.setWrapSelectorWheel(false);

        mDaySpinner.setFormatter(new NumberPicker.Formatter()
        {
            public String format(int value)
            {
                return String.format(Locale.getDefault(), "%02d", value);
            }
        });

        addSpinnersInLocaleOrder();
        mContainer.addView(mSpinners, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                     ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(mContainer, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        if (mMode == MODE_CALENDAR)
        {
            mSpinnersShown = false;
            mCalendarViewShown = true;
        }

        updateSpinners();
        updateCalendarView();
        updateVisibility();
    }

    private static NumberPicker createSpinner(Context context, NumberPicker.OnValueChangeListener listener)
    {
        NumberPicker spinner = new NumberPicker(context);
        spinner.setOnValueChangedListener(listener);
        spinner.setOnLongPressUpdateInterval(100);
        return spinner;
    }

    private void addSpinnersInLocaleOrder()
    {
        String pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMMdd");
        boolean monthAdded = false;
        boolean dayAdded = false;
        boolean yearAdded = false;

        for (int index = 0; index < pattern.length(); index++)
        {
            char symbol = pattern.charAt(index);

            if ((symbol == 'M' || symbol == 'L') && !monthAdded)
            {
                addSpinner(mMonthSpinner);
                monthAdded = true;
            }
            else if (symbol == 'd' && !dayAdded)
            {
                addSpinner(mDaySpinner);
                dayAdded = true;
            }
            else if (symbol == 'y' && !yearAdded)
            {
                addSpinner(mYearSpinner);
                yearAdded = true;
            }
        }

        if (!monthAdded) addSpinner(mMonthSpinner);
        if (!dayAdded) addSpinner(mDaySpinner);
        if (!yearAdded) addSpinner(mYearSpinner);
    }

    private void addSpinner(NumberPicker spinner)
    {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int)(8 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, 0, margin, 0);
        mSpinners.addView(spinner, params);
    }

    private void onSpinnerChanged(NumberPicker picker, int oldValue, int newValue)
    {
        Calendar temp = Calendar.getInstance();
        temp.setTimeInMillis(mCurrentDate.getTimeInMillis());

        if (picker == mDaySpinner)
        {
            int maxDay = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (oldValue == maxDay && newValue == 1) temp.add(Calendar.DAY_OF_MONTH, 1);
            else if (oldValue == 1 && newValue == maxDay) temp.add(Calendar.DAY_OF_MONTH, -1);
            else temp.add(Calendar.DAY_OF_MONTH, newValue - oldValue);
        }
        else if (picker == mMonthSpinner)
        {
            if (oldValue == 11 && newValue == 0) temp.add(Calendar.MONTH, 1);
            else if (oldValue == 0 && newValue == 11) temp.add(Calendar.MONTH, -1);
            else temp.add(Calendar.MONTH, newValue - oldValue);
        }
        else
        {
            temp.set(Calendar.YEAR, newValue);
        }

        setDate(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH), temp.get(Calendar.DAY_OF_MONTH));
        updateSpinners();
        updateCalendarView();
        notifyDateChanged();
    }

    private void setDate(int year, int month, int dayOfMonth)
    {
        mCurrentDate.set(Calendar.YEAR, year);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, 1);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH)));
        clampCurrent();
    }

    private void clampCurrent()
    {
        if (mCurrentDate.before(mMinDate)) mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        else if (mCurrentDate.after(mMaxDate)) mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
    }

    private boolean sameMonth(Calendar first, Calendar second)
    {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) && first.get(Calendar.MONTH) == second.get(Calendar.MONTH);
    }

    private void updateSpinners()
    {
        int year = mCurrentDate.get(Calendar.YEAR);

        mDaySpinner.setDisplayedValues(null);
        if (sameMonth(mCurrentDate, mMinDate))
        {
            mDaySpinner.setMinValue(mMinDate.get(Calendar.DAY_OF_MONTH));
            mDaySpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            mDaySpinner.setWrapSelectorWheel(false);
        }
        else if (sameMonth(mCurrentDate, mMaxDate))
        {
            mDaySpinner.setMinValue(mCurrentDate.getActualMinimum(Calendar.DAY_OF_MONTH));
            mDaySpinner.setMaxValue(mMaxDate.get(Calendar.DAY_OF_MONTH));
            mDaySpinner.setWrapSelectorWheel(false);
        }
        else
        {
            mDaySpinner.setMinValue(1);
            mDaySpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            mDaySpinner.setWrapSelectorWheel(true);
        }

        mMonthSpinner.setDisplayedValues(null);
        int minMonth = year == mMinDate.get(Calendar.YEAR) ? mMinDate.get(Calendar.MONTH) : 0;
        int maxMonth = year == mMaxDate.get(Calendar.YEAR) ? mMaxDate.get(Calendar.MONTH) : 11;
        mMonthSpinner.setMinValue(minMonth);
        mMonthSpinner.setMaxValue(maxMonth);
        mMonthSpinner.setWrapSelectorWheel(minMonth == 0 && maxMonth == 11);

        String[] months = new String[maxMonth - minMonth + 1];
        for (int index = 0; index < months.length; index++) months[index] = mShortMonths[minMonth + index];
        mMonthSpinner.setDisplayedValues(months);

        mYearSpinner.setMinValue(mMinDate.get(Calendar.YEAR));
        mYearSpinner.setMaxValue(mMaxDate.get(Calendar.YEAR));

        mYearSpinner.setValue(year);
        mMonthSpinner.setValue(mCurrentDate.get(Calendar.MONTH));
        mDaySpinner.setValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
    }

    private CalendarView ensureCalendarView()
    {
        if (mCalendarView != null) return mCalendarView;

        mCalendarView = new CalendarView(getContext());
        mCalendarView.setMinDate(mMinDate.getTimeInMillis());
        mCalendarView.setMaxDate(mMaxDate.getTimeInMillis());
        mCalendarView.setFirstDayOfWeek(mFirstDayOfWeek);
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener()
        {
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth)
            {
                setDate(year, month, dayOfMonth);
                updateSpinners();
                notifyDateChanged();
            }
        });

        mContainer.addView(mCalendarView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         ViewGroup.LayoutParams.WRAP_CONTENT));
        return mCalendarView;
    }

    private void updateCalendarView()
    {
        if (mCalendarView != null) mCalendarView.setDate(mCurrentDate.getTimeInMillis(), false, false);
    }

    private void updateVisibility()
    {
        mSpinners.setVisibility(mSpinnersShown ? VISIBLE : GONE);

        if (mCalendarViewShown)
        {
            ensureCalendarView().setVisibility(VISIBLE);
            updateCalendarView();
        }
        else if (mCalendarView != null)
        {
            mCalendarView.setVisibility(GONE);
        }
    }

    private void notifyDateChanged()
    {
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);

        if (mOnDateChangedListener != null) mOnDateChangedListener.onDateChanged(this, getYear(), getMonth(), getDayOfMonth());
        if (mAutoFillChangeListener != null) mAutoFillChangeListener.onDateChanged(this, getYear(), getMonth(), getDayOfMonth());

        android.view.autofill.AutofillManager manager = getContext().getSystemService(android.view.autofill.AutofillManager.class);
        if (manager != null) manager.notifyValueChanged(this);
    }

    public void init(int year, int monthOfYear, int dayOfMonth, OnDateChangedListener onDateChangedListener)
    {
        setDate(year, monthOfYear, dayOfMonth);
        updateSpinners();
        updateCalendarView();
        mOnDateChangedListener = onDateChangedListener;
    }

    public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener)
    {
        mOnDateChangedListener = onDateChangedListener;
    }

    public void updateDate(int year, int month, int dayOfMonth)
    {
        if (getYear() == year && getMonth() == month && getDayOfMonth() == dayOfMonth) return;

        setDate(year, month, dayOfMonth);
        updateSpinners();
        updateCalendarView();
        notifyDateChanged();
    }

    public int getYear()
    {
        return mCurrentDate.get(Calendar.YEAR);
    }

    public int getMonth()
    {
        return mCurrentDate.get(Calendar.MONTH);
    }

    public int getDayOfMonth()
    {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    public void setMinDate(long minDate)
    {
        Calendar candidate = Calendar.getInstance();
        candidate.setTimeInMillis(minDate);
        if (candidate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
            && candidate.get(Calendar.DAY_OF_YEAR) == mMinDate.get(Calendar.DAY_OF_YEAR))
        {
            return;
        }

        mMinDate.setTimeInMillis(minDate);
        if (mMaxDate.before(mMinDate)) mMaxDate.setTimeInMillis(minDate);
        if (mCalendarView != null) mCalendarView.setMinDate(minDate);
        clampCurrent();
        updateSpinners();
        updateCalendarView();
    }

    public long getMinDate()
    {
        return mMinDate.getTimeInMillis();
    }

    public void setMaxDate(long maxDate)
    {
        Calendar candidate = Calendar.getInstance();
        candidate.setTimeInMillis(maxDate);
        if (candidate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
            && candidate.get(Calendar.DAY_OF_YEAR) == mMaxDate.get(Calendar.DAY_OF_YEAR))
        {
            return;
        }

        mMaxDate.setTimeInMillis(maxDate);
        if (mMinDate.after(mMaxDate)) mMinDate.setTimeInMillis(maxDate);
        if (mCalendarView != null) mCalendarView.setMaxDate(maxDate);
        clampCurrent();
        updateSpinners();
        updateCalendarView();
    }

    public long getMaxDate()
    {
        return mMaxDate.getTimeInMillis();
    }

    public void setFirstDayOfWeek(int firstDayOfWeek)
    {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY)
        {
            throw new IllegalArgumentException("firstDayOfWeek must be between 1 and 7");
        }

        mFirstDayOfWeek = firstDayOfWeek;
        if (mCalendarView != null) mCalendarView.setFirstDayOfWeek(firstDayOfWeek);
    }

    public int getFirstDayOfWeek()
    {
        return mFirstDayOfWeek;
    }

    public boolean getCalendarViewShown()
    {
        return mCalendarViewShown;
    }

    public CalendarView getCalendarView()
    {
        return ensureCalendarView();
    }

    public void setCalendarViewShown(boolean shown)
    {
        mCalendarViewShown = shown;
        updateVisibility();
    }

    public boolean getSpinnersShown()
    {
        return mSpinnersShown;
    }

    public void setSpinnersShown(boolean shown)
    {
        mSpinnersShown = shown;
        updateVisibility();
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        if (isEnabled() == enabled) return;

        super.setEnabled(enabled);
        if (mDaySpinner != null) mDaySpinner.setEnabled(enabled);
        if (mMonthSpinner != null) mMonthSpinner.setEnabled(enabled);
        if (mYearSpinner != null) mYearSpinner.setEnabled(enabled);
        if (mCalendarView != null) mCalendarView.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled()
    {
        return super.isEnabled();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        mShortMonths = new DateFormatSymbols(Locale.getDefault()).getShortMonths();
        updateSpinners();
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return DatePicker.class.getName();
    }

    @Override
    public void dispatchProvideAutofillStructure(ViewStructure structure, int flags)
    {
        structure.setAutofillId(getAutofillId());
        onProvideAutofillStructure(structure, flags);
    }

    @Override
    public void autofill(AutofillValue value)
    {
        if (!isEnabled() || value == null) return;

        if (!value.isDate())
        {
            android.util.Log.w("DatePicker", value + " could not be autofilled into " + this);
            return;
        }

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(value.getDateValue());
        updateDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public int getAutofillType()
    {
        return isEnabled() ? AUTOFILL_TYPE_DATE : AUTOFILL_TYPE_NONE;
    }

    @Override
    public AutofillValue getAutofillValue()
    {
        return isEnabled() ? AutofillValue.forDate(mCurrentDate.getTimeInMillis()) : null;
    }

    void setAutoFillChangeListener(OnDateChangedListener listener)
    {
        mAutoFillChangeListener = listener;
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container)
    {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getYear(), getMonth(), getDayOfMonth(), getMinDate(), getMaxDate());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (!(state instanceof SavedState))
        {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState saved = (SavedState)state;
        super.onRestoreInstanceState(saved.getSuperState());

        mMinDate.setTimeInMillis(saved.mMinDate);
        mMaxDate.setTimeInMillis(saved.mMaxDate);
        if (mCalendarView != null)
        {
            mCalendarView.setMinDate(saved.mMinDate);
            mCalendarView.setMaxDate(saved.mMaxDate);
        }

        setDate(saved.mYear, saved.mMonth, saved.mDay);
        updateSpinners();
        updateCalendarView();
    }

    public interface OnDateChangedListener
    {
        void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth);
    }

    private static class SavedState extends View.BaseSavedState
    {
        private final int mYear;
        private final int mMonth;
        private final int mDay;
        private final long mMinDate;
        private final long mMaxDate;

        SavedState(Parcelable superState, int year, int month, int day, long minDate, long maxDate)
        {
            super(superState == null ? android.view.AbsSavedState.EMPTY_STATE : superState);
            mYear = year;
            mMonth = month;
            mDay = day;
            mMinDate = minDate;
            mMaxDate = maxDate;
        }

        private SavedState(Parcel in)
        {
            super(in);
            mYear = in.readInt();
            mMonth = in.readInt();
            mDay = in.readInt();
            mMinDate = in.readLong();
            mMaxDate = in.readLong();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            super.writeToParcel(dest, flags);
            dest.writeInt(mYear);
            dest.writeInt(mMonth);
            dest.writeInt(mDay);
            dest.writeLong(mMinDate);
            dest.writeLong(mMaxDate);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }
}
