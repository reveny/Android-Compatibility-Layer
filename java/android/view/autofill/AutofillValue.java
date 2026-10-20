package android.view.autofill;

public final class AutofillValue
{
    private final Object mValue;

    private AutofillValue(Object value)
    {
        mValue = value;
    }

    public static AutofillValue forText(CharSequence value)
    {
        return new AutofillValue(value);
    }

    public static AutofillValue forToggle(boolean value)
    {
        return new AutofillValue(value);
    }

    public static AutofillValue forList(int value)
    {
        return new AutofillValue(value);
    }

    public static AutofillValue forDate(long value)
    {
        return new AutofillValue(value);
    }

    public boolean isText()
    {
        return mValue instanceof CharSequence;
    }

    public boolean isToggle()
    {
        return mValue instanceof Boolean;
    }

    public boolean isList()
    {
        return mValue instanceof Integer;
    }

    public boolean isDate()
    {
        return mValue instanceof Long;
    }

    public CharSequence getTextValue()
    {
        return (CharSequence)mValue;
    }

    public boolean getToggleValue()
    {
        return mValue instanceof Boolean && (Boolean)mValue;
    }

    public int getListValue()
    {
        return mValue instanceof Integer ? (Integer)mValue : 0;
    }

    public long getDateValue()
    {
        return mValue instanceof Long ? (Long)mValue : 0L;
    }
}
