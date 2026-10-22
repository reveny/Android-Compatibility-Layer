package android.content;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

public class RestrictionEntry implements Parcelable
{
    public static final int TYPE_NULL = 0;
    public static final int TYPE_BOOLEAN = 1;
    public static final int TYPE_CHOICE = 2;
    public static final int TYPE_MULTI_SELECT = 4;
    public static final int TYPE_INTEGER = 5;
    public static final int TYPE_STRING = 6;
    public static final int TYPE_BUNDLE = 7;
    public static final int TYPE_BUNDLE_ARRAY = 8;

    public static final Parcelable.Creator<RestrictionEntry> CREATOR = new Parcelable.Creator<RestrictionEntry>()
    {
        @Override
        public RestrictionEntry createFromParcel(Parcel source)
        {
            return new RestrictionEntry(source);
        }

        @Override
        public RestrictionEntry[] newArray(int size)
        {
            return new RestrictionEntry[size];
        }
    };

    private int mType;
    private String mKey;
    private String mTitle;
    private String mDescription;
    private String[] mChoiceEntries;
    private String[] mChoiceValues;
    private String mCurrentValue;
    private String[] mCurrentValues;
    private RestrictionEntry[] mRestrictions;

    public RestrictionEntry(int type, String key)
    {
        mType = type;
        mKey = key;
    }

    public RestrictionEntry(String key, String selectedString)
    {
        mKey = key;
        mType = TYPE_CHOICE;
        mCurrentValue = selectedString;
    }

    public RestrictionEntry(String key, boolean selectedState)
    {
        mKey = key;
        mType = TYPE_BOOLEAN;
        setSelectedState(selectedState);
    }

    public RestrictionEntry(String key, String[] selectedStrings)
    {
        mKey = key;
        mType = TYPE_MULTI_SELECT;
        mCurrentValues = selectedStrings;
    }

    public RestrictionEntry(String key, int selectedInt)
    {
        mKey = key;
        mType = TYPE_INTEGER;
        setIntValue(selectedInt);
    }

    public RestrictionEntry(Parcel source)
    {
        mType = source.readInt();
        mKey = source.readString();
        mTitle = source.readString();
        mDescription = source.readString();
        mChoiceEntries = (String[])source.readValue(null);
        mChoiceValues = (String[])source.readValue(null);
        mCurrentValue = source.readString();
        mCurrentValues = (String[])source.readValue(null);
        mRestrictions = (RestrictionEntry[])source.readValue(null);
    }

    private RestrictionEntry(String key, RestrictionEntry[] restrictionEntries, boolean isBundle)
    {
        mKey = key;
        mType = isBundle ? TYPE_BUNDLE : TYPE_BUNDLE_ARRAY;
        setRestrictions(restrictionEntries);
    }

    public static RestrictionEntry createBundleEntry(String key, RestrictionEntry[] restrictionEntries)
    {
        return new RestrictionEntry(key, restrictionEntries, true);
    }

    public static RestrictionEntry createBundleArrayEntry(String key, RestrictionEntry[] restrictionEntries)
    {
        return new RestrictionEntry(key, restrictionEntries, false);
    }

    public void setType(int type)
    {
        mType = type;
    }

    public int getType()
    {
        return mType;
    }

    public String getSelectedString()
    {
        return mCurrentValue;
    }

    public String[] getAllSelectedStrings()
    {
        return mCurrentValues;
    }

    public boolean getSelectedState()
    {
        return Boolean.parseBoolean(mCurrentValue);
    }

    public int getIntValue()
    {
        return Integer.parseInt(mCurrentValue);
    }

    public void setIntValue(int value)
    {
        mCurrentValue = Integer.toString(value);
    }

    public void setSelectedString(String selectedString)
    {
        mCurrentValue = selectedString;
    }

    public void setSelectedState(boolean state)
    {
        mCurrentValue = Boolean.toString(state);
    }

    public void setAllSelectedStrings(String[] allSelectedStrings)
    {
        mCurrentValues = allSelectedStrings;
    }

    public void setChoiceValues(String[] choiceValues)
    {
        mChoiceValues = choiceValues;
    }

    public void setChoiceValues(Context context, int stringArrayResId)
    {
        mChoiceValues = context.getResources().getStringArray(stringArrayResId);
    }

    public RestrictionEntry[] getRestrictions()
    {
        return mRestrictions;
    }

    public void setRestrictions(RestrictionEntry[] restrictions)
    {
        mRestrictions = restrictions;
    }

    public String[] getChoiceValues()
    {
        return mChoiceValues;
    }

    public void setChoiceEntries(String[] choiceEntries)
    {
        mChoiceEntries = choiceEntries;
    }

    public void setChoiceEntries(Context context, int stringArrayResId)
    {
        mChoiceEntries = context.getResources().getStringArray(stringArrayResId);
    }

    public String[] getChoiceEntries()
    {
        return mChoiceEntries;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public void setDescription(String description)
    {
        mDescription = description;
    }

    public String getKey()
    {
        return mKey;
    }

    public String getTitle()
    {
        return mTitle;
    }

    public void setTitle(String title)
    {
        mTitle = title;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof RestrictionEntry)) return false;

        RestrictionEntry compared = (RestrictionEntry)other;
        if (mType != compared.mType || !Objects.equals(mKey, compared.mKey)) return false;
        if (mCurrentValues == null && compared.mCurrentValues == null && mRestrictions == null && compared.mRestrictions == null)
        {
            return Objects.equals(mCurrentValue, compared.mCurrentValue);
        }

        return Arrays.equals(mCurrentValues, compared.mCurrentValues) && Arrays.equals(mRestrictions, compared.mRestrictions);
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + (mKey == null ? 0 : mKey.hashCode());
        if (mCurrentValue != null) result = 31 * result + mCurrentValue.hashCode();
        else if (mCurrentValues != null) result = 31 * result + Arrays.hashCode(mCurrentValues);
        else if (mRestrictions != null) result = 31 * result + Arrays.hashCode(mRestrictions);

        return result;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mType);
        destination.writeString(mKey);
        destination.writeString(mTitle);
        destination.writeString(mDescription);
        destination.writeValue(mChoiceEntries);
        destination.writeValue(mChoiceValues);
        destination.writeString(mCurrentValue);
        destination.writeValue(mCurrentValues);
        destination.writeValue(mRestrictions);
    }

    @Override
    public String toString()
    {
        return "RestrictionEntry{mType=" + mType + ", mKey='" + mKey + "', mTitle='" + mTitle + "', mDescription='" + mDescription
            + "', mChoiceEntries=" + Arrays.toString(mChoiceEntries) + ", mChoiceValues=" + Arrays.toString(mChoiceValues)
            + ", mCurrentValue='" + mCurrentValue + "', mCurrentValues=" + Arrays.toString(mCurrentValues) + ", mRestrictions="
            + Arrays.toString(mRestrictions) + "}";
    }
}
