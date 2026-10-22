package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.view.autofill.AutofillValue;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

public class TimePicker extends FrameLayout
{
    private static StyleableSet sStyleable;

    private final LinearLayout mContainer;
    private final NumberPicker mHourSpinner;
    private final NumberPicker mMinuteSpinner;
    private final NumberPicker mAmPmSpinner;
    private final TextView mDivider;
    private final String[] mAmPmStrings;
    private OnTimeChangedListener mOnTimeChangedListener;
    private OnTimeChangedListener mAutoFillChangeListener;
    private int mHour;
    private int mMinute;
    private boolean mIs24HourView;

    public TimePicker(Context context)
    {
        this(context, null);
    }

    public TimePicker(Context context, AttributeSet attributes)
    {
        this(context, attributes, ProgressBar.attributeIdentifier(context, "timePickerStyle"));
    }

    public TimePicker(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public TimePicker(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        Calendar now = Calendar.getInstance();
        mHour = now.get(Calendar.HOUR_OF_DAY);
        mMinute = now.get(Calendar.MINUTE);
        mIs24HourView = android.text.format.DateFormat.is24HourFormat(context);

        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "is24HourView");

        if (!sStyleable.isEmpty() && (attributes != null || defStyleAttr != 0 || defStyleRes != 0))
        {
            TypedArray array = context.obtainStyledAttributes(attributes, sStyleable.attributes(), defStyleAttr, defStyleRes);
            int index = sStyleable.indexOf("is24HourView");
            if (index >= 0 && array.hasValue(index)) mIs24HourView = array.getBoolean(index, mIs24HourView);
            array.recycle();
        }

        mAmPmStrings = new DateFormatSymbols(Locale.getDefault()).getAmPmStrings();

        mContainer = new LinearLayout(context);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setGravity(Gravity.CENTER);

        mHourSpinner = new NumberPicker(context);
        mHourSpinner.setOnLongPressUpdateInterval(100);
        mHourSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
        {
            public void onValueChange(NumberPicker picker, int oldValue, int newValue)
            {
                int hour = newValue;

                if (!mIs24HourView)
                {
                    boolean wasPm = mHour >= 12;
                    if ((oldValue == 11 && newValue == 12) || (oldValue == 12 && newValue == 11)) wasPm = !wasPm;
                    hour = newValue % 12 + (wasPm ? 12 : 0);
                }

                setHourInternal(hour, true);
            }
        });

        mDivider = new TextView(context);
        mDivider.setText(":");
        mDivider.setGravity(Gravity.CENTER);

        mMinuteSpinner = new NumberPicker(context);
        mMinuteSpinner.setMinValue(0);
        mMinuteSpinner.setMaxValue(59);
        mMinuteSpinner.setOnLongPressUpdateInterval(100);
        mMinuteSpinner.setFormatter(new NumberPicker.Formatter()
        {
            public String format(int value)
            {
                return String.format(Locale.getDefault(), "%02d", value);
            }
        });
        mMinuteSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
        {
            public void onValueChange(NumberPicker picker, int oldValue, int newValue)
            {
                int hour = mHour;
                if (oldValue == 59 && newValue == 0) hour = (hour + 1) % 24;
                else if (oldValue == 0 && newValue == 59) hour = (hour + 23) % 24;

                mMinute = newValue;
                if (hour != mHour) setHourInternal(hour, false);
                updateSpinners();
                onTimeChanged();
            }
        });

        mAmPmSpinner = new NumberPicker(context);
        mAmPmSpinner.setMinValue(0);
        mAmPmSpinner.setMaxValue(1);
        mAmPmSpinner.setDisplayedValues(mAmPmStrings);
        mAmPmSpinner.setOnValueChangedListener(new NumberPicker.OnValueChangeListener()
        {
            public void onValueChange(NumberPicker picker, int oldValue, int newValue)
            {
                setHourInternal(mHour % 12 + (newValue == 1 ? 12 : 0), true);
            }
        });

        int margin = (int)(8 * getResources().getDisplayMetrics().density);
        mContainer.addView(mHourSpinner, spinnerParams(margin));
        mContainer.addView(mDivider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                    ViewGroup.LayoutParams.WRAP_CONTENT));
        mContainer.addView(mMinuteSpinner, spinnerParams(margin));

        String pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "hm");
        boolean amPmAtStart = pattern.startsWith("a");
        if (amPmAtStart) mContainer.addView(mAmPmSpinner, 0, spinnerParams(margin));
        else mContainer.addView(mAmPmSpinner, spinnerParams(margin));

        addView(mContainer, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        updateHourRange();
        updateSpinners();
    }

    private static LinearLayout.LayoutParams spinnerParams(int margin)
    {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                         ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(margin, 0, margin, 0);
        return params;
    }

    private void updateHourRange()
    {
        mHourSpinner.setDisplayedValues(null);

        if (mIs24HourView)
        {
            mHourSpinner.setMinValue(0);
            mHourSpinner.setMaxValue(23);
            mHourSpinner.setFormatter(new NumberPicker.Formatter()
            {
                public String format(int value)
                {
                    return String.format(Locale.getDefault(), "%02d", value);
                }
            });
            mAmPmSpinner.setVisibility(GONE);
        }
        else
        {
            mHourSpinner.setMinValue(1);
            mHourSpinner.setMaxValue(12);
            mHourSpinner.setFormatter(null);
            mAmPmSpinner.setVisibility(VISIBLE);
        }

        mHourSpinner.setWrapSelectorWheel(true);
        mMinuteSpinner.setWrapSelectorWheel(true);
    }

    private void updateSpinners()
    {
        if (mIs24HourView)
        {
            mHourSpinner.setValue(mHour);
        }
        else
        {
            int hour = mHour % 12;
            mHourSpinner.setValue(hour == 0 ? 12 : hour);
            mAmPmSpinner.setValue(mHour >= 12 ? 1 : 0);
        }

        mMinuteSpinner.setValue(mMinute);
    }

    private void setHourInternal(int hour, boolean notify)
    {
        if (hour == mHour)
        {
            updateSpinners();
            return;
        }

        mHour = hour;
        updateSpinners();
        if (notify) onTimeChanged();
    }

    private void onTimeChanged()
    {
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);

        if (mOnTimeChangedListener != null) mOnTimeChangedListener.onTimeChanged(this, mHour, mMinute);
        if (mAutoFillChangeListener != null) mAutoFillChangeListener.onTimeChanged(this, mHour, mMinute);

        android.view.autofill.AutofillManager manager = getContext().getSystemService(android.view.autofill.AutofillManager.class);
        if (manager != null) manager.notifyValueChanged(this);
    }

    public void setHour(int hour)
    {
        hour = Math.max(0, Math.min(hour, 23));
        if (hour == mHour) return;

        mHour = hour;
        updateSpinners();
        onTimeChanged();
    }

    public int getHour()
    {
        return mHour;
    }

    public void setMinute(int minute)
    {
        minute = Math.max(0, Math.min(minute, 59));
        if (minute == mMinute) return;

        mMinute = minute;
        updateSpinners();
        onTimeChanged();
    }

    public int getMinute()
    {
        return mMinute;
    }

    @Deprecated
    public void setCurrentHour(Integer currentHour)
    {
        setHour(currentHour);
    }

    @Deprecated
    public Integer getCurrentHour()
    {
        return getHour();
    }

    @Deprecated
    public void setCurrentMinute(Integer currentMinute)
    {
        setMinute(currentMinute);
    }

    @Deprecated
    public Integer getCurrentMinute()
    {
        return getMinute();
    }

    public void setIs24HourView(Boolean is24HourView)
    {
        if (is24HourView == null || is24HourView == mIs24HourView) return;

        mIs24HourView = is24HourView;
        updateHourRange();
        updateSpinners();
    }

    public boolean is24HourView()
    {
        return mIs24HourView;
    }

    public void setOnTimeChangedListener(OnTimeChangedListener onTimeChangedListener)
    {
        mOnTimeChangedListener = onTimeChangedListener;
    }

    void setAutoFillChangeListener(OnTimeChangedListener listener)
    {
        mAutoFillChangeListener = listener;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);

        if (mHourSpinner != null) mHourSpinner.setEnabled(enabled);
        if (mMinuteSpinner != null) mMinuteSpinner.setEnabled(enabled);
        if (mAmPmSpinner != null) mAmPmSpinner.setEnabled(enabled);
        if (mDivider != null) mDivider.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled()
    {
        return super.isEnabled();
    }

    @Override
    public int getBaseline()
    {
        int baseline = mHourSpinner.getBaseline();
        if (baseline < 0) return super.getBaseline();

        return mContainer.getTop() + mHourSpinner.getTop() + baseline;
    }

    public boolean validateInput()
    {
        return true;
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return TimePicker.class.getName();
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
            android.util.Log.w("TimePicker", value + " could not be autofilled into " + this);
            return;
        }

        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(value.getDateValue());
        mHour = time.get(Calendar.HOUR_OF_DAY);
        mMinute = time.get(Calendar.MINUTE);
        updateSpinners();
        onTimeChanged();
    }

    @Override
    public int getAutofillType()
    {
        return isEnabled() ? AUTOFILL_TYPE_DATE : AUTOFILL_TYPE_NONE;
    }

    @Override
    public AutofillValue getAutofillValue()
    {
        if (!isEnabled()) return null;

        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, mHour);
        time.set(Calendar.MINUTE, mMinute);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return AutofillValue.forDate(time.getTimeInMillis());
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mHour, mMinute, mIs24HourView);
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

        mIs24HourView = saved.mIs24HourView;
        mHour = saved.mHour;
        mMinute = saved.mMinute;
        updateHourRange();
        updateSpinners();
    }

    public interface OnTimeChangedListener
    {
        void onTimeChanged(TimePicker view, int hourOfDay, int minute);
    }

    private static class SavedState extends View.BaseSavedState
    {
        private final int mHour;
        private final int mMinute;
        private final boolean mIs24HourView;

        SavedState(Parcelable superState, int hour, int minute, boolean is24HourView)
        {
            super(superState == null ? android.view.AbsSavedState.EMPTY_STATE : superState);
            mHour = hour;
            mMinute = minute;
            mIs24HourView = is24HourView;
        }

        private SavedState(Parcel in)
        {
            super(in);
            mHour = in.readInt();
            mMinute = in.readInt();
            mIs24HourView = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            super.writeToParcel(dest, flags);
            dest.writeInt(mHour);
            dest.writeInt(mMinute);
            dest.writeInt(mIs24HourView ? 1 : 0);
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
