package android.view.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Objects;

public final class AutofillValue implements Parcelable
{
    private static final int TYPE_TEXT = 1;
    private static final int TYPE_TOGGLE = 2;
    private static final int TYPE_LIST = 3;
    private static final int TYPE_DATE = 4;

    public static final Parcelable.Creator<AutofillValue> CREATOR = new Parcelable.Creator<AutofillValue>()
    {
        @Override
        public AutofillValue createFromParcel(Parcel source)
        {
            int type = source.readInt();

            switch (type)
            {
                case TYPE_TEXT:
                    return new AutofillValue(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source));
                case TYPE_TOGGLE:
                    return new AutofillValue(source.readInt() != 0);
                case TYPE_LIST:
                    return new AutofillValue(source.readInt());
                case TYPE_DATE:
                    return new AutofillValue(source.readLong());
                default:
                    throw new IllegalArgumentException("type=" + type + " not valid");
            }
        }

        @Override
        public AutofillValue[] newArray(int size)
        {
            return new AutofillValue[size];
        }
    };

    private final Object mValue;

    private AutofillValue(Object value)
    {
        mValue = value;
    }

    public static AutofillValue forText(CharSequence value)
    {
        return value == null ? null : new AutofillValue(value);
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
        if (!isText()) throw new IllegalStateException("value must be a text value, not type=" + typeOf());
        return (CharSequence)mValue;
    }

    public boolean getToggleValue()
    {
        if (!isToggle()) throw new IllegalStateException("value must be a toggle value, not type=" + typeOf());
        return (Boolean)mValue;
    }

    public int getListValue()
    {
        if (!isList()) throw new IllegalStateException("value must be a list value, not type=" + typeOf());
        return (Integer)mValue;
    }

    public long getDateValue()
    {
        if (!isDate()) throw new IllegalStateException("value must be a date value, not type=" + typeOf());
        return (Long)mValue;
    }

    private int typeOf()
    {
        if (isText()) return TYPE_TEXT;
        if (isToggle()) return TYPE_TOGGLE;
        if (isList()) return TYPE_LIST;
        return TYPE_DATE;
    }

    @Override
    public int hashCode()
    {
        return typeOf() + mValue.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        AutofillValue value = (AutofillValue)other;
        if (typeOf() != value.typeOf()) return false;
        if (isText()) return mValue.toString().equals(value.mValue.toString());

        return Objects.equals(mValue, value.mValue);
    }

    @Override
    public String toString()
    {
        return "[type=" + typeOf() + ", value=" + (isText() ? ((CharSequence)mValue).length() + "_chars" : mValue) + "]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        int type = typeOf();
        parcel.writeInt(type);

        switch (type)
        {
            case TYPE_TEXT:
                TextUtils.writeToParcel((CharSequence)mValue, parcel, flags);
                break;
            case TYPE_TOGGLE:
                parcel.writeInt((Boolean)mValue ? 1 : 0);
                break;
            case TYPE_LIST:
                parcel.writeInt((Integer)mValue);
                break;
            default:
                parcel.writeLong((Long)mValue);
                break;
        }
    }
}
