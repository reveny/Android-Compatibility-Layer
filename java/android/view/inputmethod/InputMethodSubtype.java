package android.view.inputmethod;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class InputMethodSubtype implements Parcelable
{
    private final int mNameResId;
    private final int mIconResId;
    private final String mLocale;
    private final String mMode;
    private final String mExtraValue;
    private final boolean mAuxiliary;
    private final boolean mOverridesImplicitlyEnabledSubtype;
    private final int mSubtypeId;

    public InputMethodSubtype(int nameId, int iconId, String locale, String mode, String extraValue, boolean isAuxiliary,
                              boolean overridesImplicitlyEnabledSubtype)
    {
        this(nameId, iconId, locale, mode, extraValue, isAuxiliary, overridesImplicitlyEnabledSubtype, 0);
    }

    public InputMethodSubtype(int nameId, int iconId, String locale, String mode, String extraValue, boolean isAuxiliary,
                              boolean overridesImplicitlyEnabledSubtype, int id)
    {
        mNameResId = nameId;
        mIconResId = iconId;
        mLocale = locale == null ? "" : locale;
        mMode = mode == null ? "" : mode;
        mExtraValue = extraValue == null ? "" : extraValue;
        mAuxiliary = isAuxiliary;
        mOverridesImplicitlyEnabledSubtype = overridesImplicitlyEnabledSubtype;
        mSubtypeId = id != 0 ? id : Objects.hash(mLocale, mMode, mExtraValue, mAuxiliary, mOverridesImplicitlyEnabledSubtype);
    }

    public int getNameResId()
    {
        return mNameResId;
    }

    public CharSequence getNameOverride()
    {
        return "";
    }

    public String getPhysicalKeyboardHintLayoutType()
    {
        return "";
    }

    public int getLayoutLabelResource()
    {
        return 0;
    }

    public CharSequence getLayoutLabelNonLocalized()
    {
        return "";
    }

    public CharSequence getLayoutDisplayName(Context context, ApplicationInfo imeAppInfo)
    {
        return getLayoutLabelNonLocalized();
    }

    public int getIconResId()
    {
        return mIconResId;
    }

    public String getLocale()
    {
        return mLocale;
    }

    public String getLanguageTag()
    {
        return mLocale.replace('_', '-');
    }

    public String getMode()
    {
        return mMode;
    }

    public String getExtraValue()
    {
        return mExtraValue;
    }

    public boolean isAuxiliary()
    {
        return mAuxiliary;
    }

    public boolean overridesImplicitlyEnabledSubtype()
    {
        return mOverridesImplicitlyEnabledSubtype;
    }

    public boolean isAsciiCapable()
    {
        return containsExtraValueKey("AsciiCapable");
    }

    public CharSequence getDisplayName(Context context, String packageName, ApplicationInfo appInfo)
    {
        String locale = getLanguageTag();
        String localeName = locale.isEmpty() ? "" : java.util.Locale.forLanguageTag(locale).getDisplayName();

        if (mNameResId == 0 || context == null) return localeName;

        CharSequence name;
        try
        {
            name = context.getResources().getText(mNameResId);
        }
        catch (RuntimeException exception)
        {
            return localeName;
        }

        if (name == null) return localeName;

        String text = name.toString();
        if (!text.contains("%s")) return text;

        return String.format(text, localeName);
    }

    public boolean containsExtraValueKey(String key)
    {
        return getExtraValueOf(key) != null || hasBareKey(key);
    }

    private boolean hasBareKey(String key)
    {
        for (String pair : mExtraValue.split(","))
        {
            if (pair.equals(key)) return true;
        }

        return false;
    }

    public String getExtraValueOf(String key)
    {
        for (String pair : mExtraValue.split(","))
        {
            int separator = pair.indexOf('=');
            if (separator > 0 && pair.substring(0, separator).equals(key)) return pair.substring(separator + 1);
        }

        return null;
    }

    @Override
    public int hashCode()
    {
        return mSubtypeId;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof InputMethodSubtype)) return false;

        InputMethodSubtype that = (InputMethodSubtype)other;
        return mSubtypeId == that.mSubtypeId && mLocale.equals(that.mLocale) && mMode.equals(that.mMode)
            && mExtraValue.equals(that.mExtraValue) && mAuxiliary == that.mAuxiliary;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mNameResId);
        dest.writeInt(mIconResId);
        dest.writeString(mLocale);
        dest.writeString(mMode);
        dest.writeString(mExtraValue);
        dest.writeInt(mAuxiliary ? 1 : 0);
        dest.writeInt(mOverridesImplicitlyEnabledSubtype ? 1 : 0);
        dest.writeInt(mSubtypeId);
    }

    public static final Creator<InputMethodSubtype> CREATOR = new Creator<InputMethodSubtype>()
    {
        @Override
        public InputMethodSubtype createFromParcel(Parcel source)
        {
            int nameId = source.readInt();
            int iconId = source.readInt();
            String locale = source.readString();
            String mode = source.readString();
            String extraValue = source.readString();
            boolean auxiliary = source.readInt() != 0;
            boolean overrides = source.readInt() != 0;
            int id = source.readInt();
            return new InputMethodSubtype(nameId, iconId, locale, mode, extraValue, auxiliary, overrides, id);
        }

        @Override
        public InputMethodSubtype[] newArray(int size)
        {
            return new InputMethodSubtype[size];
        }
    };
}
