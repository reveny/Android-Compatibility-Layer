package android.content.res;

import android.content.pm.ActivityInfo;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Locale;

public final class Configuration implements Parcelable, Comparable<Configuration>
{
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_SQUARE = 3;

    public static final int SCREENLAYOUT_SIZE_MASK = 0x0f;
    public static final int SCREENLAYOUT_SIZE_UNDEFINED = 0x00;
    public static final int SCREENLAYOUT_SIZE_SMALL = 0x01;
    public static final int SCREENLAYOUT_SIZE_NORMAL = 0x02;
    public static final int SCREENLAYOUT_SIZE_LARGE = 0x03;
    public static final int SCREENLAYOUT_SIZE_XLARGE = 0x04;
    public static final int SCREENLAYOUT_LONG_MASK = 0x30;
    public static final int SCREENLAYOUT_LONG_UNDEFINED = 0x00;
    public static final int SCREENLAYOUT_LONG_NO = 0x10;
    public static final int SCREENLAYOUT_LONG_YES = 0x20;
    public static final int SCREENLAYOUT_LAYOUTDIR_MASK = 0xc0;
    public static final int SCREENLAYOUT_LAYOUTDIR_SHIFT = 6;
    public static final int SCREENLAYOUT_LAYOUTDIR_UNDEFINED = 0x00;
    public static final int SCREENLAYOUT_LAYOUTDIR_LTR = 0x40;
    public static final int SCREENLAYOUT_LAYOUTDIR_RTL = 0x80;
    public static final int SCREENLAYOUT_ROUND_MASK = 0x300;
    public static final int SCREENLAYOUT_ROUND_UNDEFINED = 0x000;
    public static final int SCREENLAYOUT_ROUND_NO = 0x100;
    public static final int SCREENLAYOUT_ROUND_YES = 0x200;
    public static final int SCREENLAYOUT_UNDEFINED = 0;

    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_MASK = 0x3;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_UNDEFINED = 0x0;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_NO = 0x1;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT_YES = 0x2;
    public static final int COLOR_MODE_HDR_MASK = 0xc;
    public static final int COLOR_MODE_HDR_SHIFT = 2;
    public static final int COLOR_MODE_HDR_UNDEFINED = 0x0;
    public static final int COLOR_MODE_HDR_NO = 0x4;
    public static final int COLOR_MODE_HDR_YES = 0x8;
    public static final int COLOR_MODE_UNDEFINED = 0;

    public static final int UI_MODE_TYPE_MASK = 0x0f;
    public static final int UI_MODE_TYPE_UNDEFINED = 0x00;
    public static final int UI_MODE_TYPE_NORMAL = 0x01;
    public static final int UI_MODE_TYPE_DESK = 0x02;
    public static final int UI_MODE_TYPE_CAR = 0x03;
    public static final int UI_MODE_TYPE_TELEVISION = 0x04;
    public static final int UI_MODE_TYPE_APPLIANCE = 0x05;
    public static final int UI_MODE_TYPE_WATCH = 0x06;
    public static final int UI_MODE_TYPE_VR_HEADSET = 0x07;
    public static final int UI_MODE_NIGHT_MASK = 0x30;
    public static final int UI_MODE_NIGHT_UNDEFINED = 0x00;
    public static final int UI_MODE_NIGHT_NO = 0x10;
    public static final int UI_MODE_NIGHT_YES = 0x20;

    public static final int MNC_ZERO = 0xffff;

    public static final int TOUCHSCREEN_UNDEFINED = 0;
    public static final int TOUCHSCREEN_NOTOUCH = 1;
    public static final int TOUCHSCREEN_STYLUS = 2;
    public static final int TOUCHSCREEN_FINGER = 3;
    public static final int KEYBOARD_UNDEFINED = 0;
    public static final int KEYBOARD_NOKEYS = 1;
    public static final int KEYBOARD_QWERTY = 2;
    public static final int KEYBOARD_12KEY = 3;
    public static final int KEYBOARDHIDDEN_UNDEFINED = 0;
    public static final int KEYBOARDHIDDEN_NO = 1;
    public static final int KEYBOARDHIDDEN_YES = 2;
    public static final int HARDKEYBOARDHIDDEN_UNDEFINED = 0;
    public static final int HARDKEYBOARDHIDDEN_NO = 1;
    public static final int HARDKEYBOARDHIDDEN_YES = 2;
    public static final int NAVIGATION_UNDEFINED = 0;
    public static final int NAVIGATION_NONAV = 1;
    public static final int NAVIGATION_DPAD = 2;
    public static final int NAVIGATION_TRACKBALL = 3;
    public static final int NAVIGATION_WHEEL = 4;
    public static final int NAVIGATIONHIDDEN_UNDEFINED = 0;
    public static final int NAVIGATIONHIDDEN_NO = 1;
    public static final int NAVIGATIONHIDDEN_YES = 2;
    public static final int DENSITY_DPI_UNDEFINED = 0;
    public static final int SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int SCREEN_HEIGHT_DP_UNDEFINED = 0;
    public static final int SMALLEST_SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int FONT_WEIGHT_ADJUSTMENT_UNDEFINED = Integer.MAX_VALUE;

    public static final int GRAMMATICAL_GENDER_NOT_SPECIFIED = 0;
    public static final int GRAMMATICAL_GENDER_NEUTRAL = 1;
    public static final int GRAMMATICAL_GENDER_FEMININE = 2;
    public static final int GRAMMATICAL_GENDER_MASCULINE = 3;

    public static final Parcelable.Creator<Configuration> CREATOR = new Parcelable.Creator<Configuration>()
    {
        @Override
        public Configuration createFromParcel(Parcel source)
        {
            Configuration configuration = new Configuration();
            configuration.readFromParcel(source);
            return configuration;
        }

        @Override
        public Configuration[] newArray(int size)
        {
            return new Configuration[size];
        }
    };

    public float fontScale = 1.0f;
    public int mcc;
    public int mnc;
    public Locale locale = Locale.US;
    public int densityDpi = 160;
    public int screenWidthDp = SCREEN_WIDTH_DP_UNDEFINED;
    public int screenHeightDp = SCREEN_HEIGHT_DP_UNDEFINED;
    public int smallestScreenWidthDp = SMALLEST_SCREEN_WIDTH_DP_UNDEFINED;
    public int screenLayout = SCREENLAYOUT_SIZE_NORMAL | SCREENLAYOUT_LAYOUTDIR_LTR;
    public int orientation = ORIENTATION_PORTRAIT;
    public int uiMode = UI_MODE_TYPE_NORMAL | UI_MODE_NIGHT_NO;
    public int colorMode = COLOR_MODE_UNDEFINED;
    public int touchscreen = TOUCHSCREEN_FINGER;
    public int keyboard = KEYBOARD_NOKEYS;
    public int keyboardHidden = KEYBOARDHIDDEN_YES;
    public int hardKeyboardHidden = KEYBOARDHIDDEN_YES;
    public int navigation = NAVIGATION_NONAV;
    public int navigationHidden = NAVIGATIONHIDDEN_UNDEFINED;
    public int fontWeightAdjustment = FONT_WEIGHT_ADJUSTMENT_UNDEFINED;

    private LocaleList mLocaleList = LocaleList.getDefault();
    private int mGrammaticalGender = GRAMMATICAL_GENDER_NOT_SPECIFIED;

    public Configuration()
    {
    }

    public Configuration(Configuration source)
    {
        setTo(source);
    }

    public void setTo(Configuration source)
    {
        fontScale = source.fontScale;
        mcc = source.mcc;
        mnc = source.mnc;
        locale = source.locale;
        mLocaleList = source.mLocaleList;
        mGrammaticalGender = source.mGrammaticalGender;
        densityDpi = source.densityDpi;
        screenWidthDp = source.screenWidthDp;
        screenHeightDp = source.screenHeightDp;
        smallestScreenWidthDp = source.smallestScreenWidthDp;
        screenLayout = source.screenLayout;
        orientation = source.orientation;
        uiMode = source.uiMode;
        colorMode = source.colorMode;
        touchscreen = source.touchscreen;
        keyboard = source.keyboard;
        keyboardHidden = source.keyboardHidden;
        hardKeyboardHidden = source.hardKeyboardHidden;
        navigation = source.navigation;
        navigationHidden = source.navigationHidden;
        fontWeightAdjustment = source.fontWeightAdjustment;
    }

    public void setToDefaults()
    {
        setTo(new Configuration());
    }

    public int updateFrom(Configuration source)
    {
        if (source == null) return 0;

        int changes = diff(source);

        if (source.fontScale > 0) fontScale = source.fontScale;
        if (source.mcc != 0) mcc = source.mcc;
        if (source.mnc != 0) mnc = source.mnc;
        if (source.mLocaleList != null && !source.mLocaleList.isEmpty())
        {
            mLocaleList = source.mLocaleList;
            locale = source.locale;
        }
        if (source.mGrammaticalGender != GRAMMATICAL_GENDER_NOT_SPECIFIED) mGrammaticalGender = source.mGrammaticalGender;
        if (source.touchscreen != TOUCHSCREEN_UNDEFINED) touchscreen = source.touchscreen;
        if (source.keyboard != KEYBOARD_UNDEFINED) keyboard = source.keyboard;
        if (source.keyboardHidden != KEYBOARDHIDDEN_UNDEFINED) keyboardHidden = source.keyboardHidden;
        if (source.hardKeyboardHidden != KEYBOARDHIDDEN_UNDEFINED) hardKeyboardHidden = source.hardKeyboardHidden;
        if (source.navigation != NAVIGATION_UNDEFINED) navigation = source.navigation;
        if (source.navigationHidden != NAVIGATIONHIDDEN_UNDEFINED) navigationHidden = source.navigationHidden;
        if (source.orientation != ORIENTATION_UNDEFINED) orientation = source.orientation;
        if (source.screenLayout != SCREENLAYOUT_UNDEFINED) screenLayout = source.screenLayout;
        if (source.colorMode != COLOR_MODE_UNDEFINED) colorMode = source.colorMode;
        if (source.uiMode != UI_MODE_TYPE_UNDEFINED) uiMode = source.uiMode;
        if (source.screenWidthDp != SCREEN_WIDTH_DP_UNDEFINED) screenWidthDp = source.screenWidthDp;
        if (source.screenHeightDp != SCREEN_HEIGHT_DP_UNDEFINED) screenHeightDp = source.screenHeightDp;
        if (source.smallestScreenWidthDp != SMALLEST_SCREEN_WIDTH_DP_UNDEFINED) smallestScreenWidthDp = source.smallestScreenWidthDp;
        if (source.densityDpi != DENSITY_DPI_UNDEFINED) densityDpi = source.densityDpi;
        if (source.fontWeightAdjustment != FONT_WEIGHT_ADJUSTMENT_UNDEFINED) fontWeightAdjustment = source.fontWeightAdjustment;

        return changes;
    }

    public int diff(Configuration other)
    {
        if (other == null) return 0;

        int changes = 0;

        if (other.fontScale > 0 && fontScale != other.fontScale) changes |= ActivityInfo.CONFIG_FONT_SCALE;
        if (other.mcc != 0 && mcc != other.mcc) changes |= ActivityInfo.CONFIG_MCC;
        if (other.mnc != 0 && mnc != other.mnc) changes |= ActivityInfo.CONFIG_MNC;
        if (other.mLocaleList != null && !other.mLocaleList.isEmpty() && !mLocaleList.equals(other.mLocaleList))
        {
            changes |= ActivityInfo.CONFIG_LOCALE | ActivityInfo.CONFIG_LAYOUT_DIRECTION;
        }
        if (other.touchscreen != TOUCHSCREEN_UNDEFINED && touchscreen != other.touchscreen) changes |= ActivityInfo.CONFIG_TOUCHSCREEN;
        if (other.keyboard != KEYBOARD_UNDEFINED && keyboard != other.keyboard) changes |= ActivityInfo.CONFIG_KEYBOARD;
        if (other.keyboardHidden != KEYBOARDHIDDEN_UNDEFINED && keyboardHidden != other.keyboardHidden)
        {
            changes |= ActivityInfo.CONFIG_KEYBOARD_HIDDEN;
        }
        if (other.hardKeyboardHidden != KEYBOARDHIDDEN_UNDEFINED && hardKeyboardHidden != other.hardKeyboardHidden)
        {
            changes |= ActivityInfo.CONFIG_KEYBOARD_HIDDEN;
        }
        if (other.navigation != NAVIGATION_UNDEFINED && navigation != other.navigation) changes |= ActivityInfo.CONFIG_NAVIGATION;
        if (other.navigationHidden != NAVIGATIONHIDDEN_UNDEFINED && navigationHidden != other.navigationHidden)
        {
            changes |= ActivityInfo.CONFIG_KEYBOARD_HIDDEN;
        }
        if (other.orientation != ORIENTATION_UNDEFINED && orientation != other.orientation) changes |= ActivityInfo.CONFIG_ORIENTATION;
        if (other.screenLayout != SCREENLAYOUT_UNDEFINED && screenLayout != other.screenLayout)
        {
            changes |= ActivityInfo.CONFIG_SCREEN_LAYOUT;
            if ((screenLayout & SCREENLAYOUT_LAYOUTDIR_MASK) != (other.screenLayout & SCREENLAYOUT_LAYOUTDIR_MASK))
            {
                changes |= ActivityInfo.CONFIG_LAYOUT_DIRECTION;
            }
        }
        if (other.colorMode != COLOR_MODE_UNDEFINED && colorMode != other.colorMode) changes |= ActivityInfo.CONFIG_COLOR_MODE;
        if (other.uiMode != UI_MODE_TYPE_UNDEFINED && uiMode != other.uiMode) changes |= ActivityInfo.CONFIG_UI_MODE;
        if (other.screenWidthDp != SCREEN_WIDTH_DP_UNDEFINED && screenWidthDp != other.screenWidthDp) changes |= ActivityInfo.CONFIG_SCREEN_SIZE;
        if (other.screenHeightDp != SCREEN_HEIGHT_DP_UNDEFINED && screenHeightDp != other.screenHeightDp) changes |= ActivityInfo.CONFIG_SCREEN_SIZE;
        if (other.smallestScreenWidthDp != SMALLEST_SCREEN_WIDTH_DP_UNDEFINED && smallestScreenWidthDp != other.smallestScreenWidthDp)
        {
            changes |= ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE;
        }
        if (other.densityDpi != DENSITY_DPI_UNDEFINED && densityDpi != other.densityDpi) changes |= ActivityInfo.CONFIG_DENSITY;
        if (other.fontWeightAdjustment != FONT_WEIGHT_ADJUSTMENT_UNDEFINED && fontWeightAdjustment != other.fontWeightAdjustment)
        {
            changes |= ActivityInfo.CONFIG_FONT_WEIGHT_ADJUSTMENT;
        }

        return changes;
    }

    public static boolean needNewResources(int configChanges, int interestingChanges)
    {
        interestingChanges = interestingChanges | ActivityInfo.CONFIG_FONT_SCALE | ActivityInfo.CONFIG_FONT_WEIGHT_ADJUSTMENT;
        return (configChanges & interestingChanges) != 0;
    }

    public static Configuration generateDelta(Configuration base, Configuration change)
    {
        Configuration delta = new Configuration();
        delta.fontScale = 0;
        delta.mLocaleList = LocaleList.getEmptyLocaleList();
        delta.locale = null;
        delta.densityDpi = DENSITY_DPI_UNDEFINED;
        delta.screenLayout = SCREENLAYOUT_UNDEFINED;
        delta.orientation = ORIENTATION_UNDEFINED;
        delta.uiMode = UI_MODE_TYPE_UNDEFINED;
        delta.touchscreen = TOUCHSCREEN_UNDEFINED;
        delta.keyboard = KEYBOARD_UNDEFINED;
        delta.keyboardHidden = KEYBOARDHIDDEN_UNDEFINED;
        delta.hardKeyboardHidden = HARDKEYBOARDHIDDEN_UNDEFINED;
        delta.navigation = NAVIGATION_UNDEFINED;

        if (base.fontScale != change.fontScale) delta.fontScale = change.fontScale;
        if (base.mcc != change.mcc) delta.mcc = change.mcc;
        if (base.mnc != change.mnc) delta.mnc = change.mnc;
        if (!base.mLocaleList.equals(change.mLocaleList))
        {
            delta.mLocaleList = change.mLocaleList;
            delta.locale = change.locale;
        }
        if (base.mGrammaticalGender != change.mGrammaticalGender) delta.mGrammaticalGender = change.mGrammaticalGender;
        if (base.touchscreen != change.touchscreen) delta.touchscreen = change.touchscreen;
        if (base.keyboard != change.keyboard) delta.keyboard = change.keyboard;
        if (base.keyboardHidden != change.keyboardHidden) delta.keyboardHidden = change.keyboardHidden;
        if (base.hardKeyboardHidden != change.hardKeyboardHidden) delta.hardKeyboardHidden = change.hardKeyboardHidden;
        if (base.navigation != change.navigation) delta.navigation = change.navigation;
        if (base.navigationHidden != change.navigationHidden) delta.navigationHidden = change.navigationHidden;
        if (base.orientation != change.orientation) delta.orientation = change.orientation;
        if (base.screenLayout != change.screenLayout) delta.screenLayout = change.screenLayout;
        if (base.colorMode != change.colorMode) delta.colorMode = change.colorMode;
        if (base.uiMode != change.uiMode) delta.uiMode = change.uiMode;
        if (base.screenWidthDp != change.screenWidthDp) delta.screenWidthDp = change.screenWidthDp;
        if (base.screenHeightDp != change.screenHeightDp) delta.screenHeightDp = change.screenHeightDp;
        if (base.smallestScreenWidthDp != change.smallestScreenWidthDp) delta.smallestScreenWidthDp = change.smallestScreenWidthDp;
        if (base.densityDpi != change.densityDpi) delta.densityDpi = change.densityDpi;
        if (base.fontWeightAdjustment != change.fontWeightAdjustment) delta.fontWeightAdjustment = change.fontWeightAdjustment;

        return delta;
    }

    public LocaleList getLocales()
    {
        return mLocaleList;
    }

    public void setLocales(LocaleList locales)
    {
        if (locales == null || locales.isEmpty()) return;

        mLocaleList = locales;
        locale = locales.get(0);
        setLayoutDirection(locale);
    }

    public void setLocale(Locale value)
    {
        setLocales(value == null ? LocaleList.getEmptyLocaleList() : new LocaleList(value));
    }

    public int getGrammaticalGender()
    {
        return mGrammaticalGender;
    }

    public int getLayoutDirection()
    {
        return (screenLayout & SCREENLAYOUT_LAYOUTDIR_MASK) == SCREENLAYOUT_LAYOUTDIR_RTL ? 1 : 0;
    }

    public void setLayoutDirection(Locale value)
    {
        int layoutDirection = 1 + android.text.TextUtils.getLayoutDirectionFromLocale(value);
        screenLayout = (screenLayout & ~SCREENLAYOUT_LAYOUTDIR_MASK) | (layoutDirection << SCREENLAYOUT_LAYOUTDIR_SHIFT);
    }

    public boolean isLayoutSizeAtLeast(int size)
    {
        int current = screenLayout & SCREENLAYOUT_SIZE_MASK;
        if (current == SCREENLAYOUT_SIZE_UNDEFINED) return false;
        return current >= size;
    }

    public boolean isNightModeActive()
    {
        return (uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES;
    }

    public boolean isScreenRound()
    {
        return (screenLayout & SCREENLAYOUT_ROUND_MASK) == SCREENLAYOUT_ROUND_YES;
    }

    public boolean isScreenWideColorGamut()
    {
        return (colorMode & COLOR_MODE_WIDE_COLOR_GAMUT_MASK) == COLOR_MODE_WIDE_COLOR_GAMUT_YES;
    }

    public boolean isScreenHdr()
    {
        return (colorMode & COLOR_MODE_HDR_MASK) == COLOR_MODE_HDR_YES;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeFloat(fontScale);
        destination.writeInt(mcc);
        destination.writeInt(mnc);
        destination.writeString(mLocaleList == null ? "" : mLocaleList.toLanguageTags());
        destination.writeInt(mGrammaticalGender);
        destination.writeInt(touchscreen);
        destination.writeInt(keyboard);
        destination.writeInt(keyboardHidden);
        destination.writeInt(hardKeyboardHidden);
        destination.writeInt(navigation);
        destination.writeInt(navigationHidden);
        destination.writeInt(orientation);
        destination.writeInt(screenLayout);
        destination.writeInt(colorMode);
        destination.writeInt(uiMode);
        destination.writeInt(screenWidthDp);
        destination.writeInt(screenHeightDp);
        destination.writeInt(smallestScreenWidthDp);
        destination.writeInt(densityDpi);
        destination.writeInt(fontWeightAdjustment);
    }

    public void readFromParcel(Parcel source)
    {
        fontScale = source.readFloat();
        mcc = source.readInt();
        mnc = source.readInt();
        String tags = source.readString();
        mLocaleList = tags == null || tags.isEmpty() ? LocaleList.getEmptyLocaleList() : LocaleList.forLanguageTags(tags);
        locale = mLocaleList.isEmpty() ? null : mLocaleList.get(0);
        mGrammaticalGender = source.readInt();
        touchscreen = source.readInt();
        keyboard = source.readInt();
        keyboardHidden = source.readInt();
        hardKeyboardHidden = source.readInt();
        navigation = source.readInt();
        navigationHidden = source.readInt();
        orientation = source.readInt();
        screenLayout = source.readInt();
        colorMode = source.readInt();
        uiMode = source.readInt();
        screenWidthDp = source.readInt();
        screenHeightDp = source.readInt();
        smallestScreenWidthDp = source.readInt();
        densityDpi = source.readInt();
        fontWeightAdjustment = source.readInt();
    }

    @Override
    public int compareTo(Configuration that)
    {
        float scaleDifference = fontScale - that.fontScale;
        if (scaleDifference != 0) return scaleDifference < 0 ? -1 : 1;

        int[] mine = comparableFields();
        int[] theirs = that.comparableFields();

        for (int index = 0; index < mine.length; index++)
        {
            if (mine[index] != theirs[index]) return mine[index] < theirs[index] ? -1 : 1;
        }

        String myTags = mLocaleList == null ? "" : mLocaleList.toLanguageTags();
        String theirTags = that.mLocaleList == null ? "" : that.mLocaleList.toLanguageTags();
        return myTags.compareTo(theirTags);
    }

    private int[] comparableFields()
    {
        return new int[] {mcc, mnc, mGrammaticalGender, touchscreen, keyboard, keyboardHidden, hardKeyboardHidden, navigation,
                          navigationHidden, orientation, screenLayout, colorMode, uiMode, screenWidthDp, screenHeightDp,
                          smallestScreenWidthDp, densityDpi, fontWeightAdjustment};
    }

    public boolean equals(Configuration other)
    {
        if (other == null) return false;
        if (other == this) return true;

        return fontScale == other.fontScale && mcc == other.mcc && mnc == other.mnc && densityDpi == other.densityDpi
                && screenWidthDp == other.screenWidthDp && screenHeightDp == other.screenHeightDp
                && smallestScreenWidthDp == other.smallestScreenWidthDp && screenLayout == other.screenLayout
                && orientation == other.orientation && uiMode == other.uiMode && colorMode == other.colorMode
                && touchscreen == other.touchscreen && keyboard == other.keyboard && keyboardHidden == other.keyboardHidden
                && hardKeyboardHidden == other.hardKeyboardHidden && navigation == other.navigation
                && navigationHidden == other.navigationHidden && fontWeightAdjustment == other.fontWeightAdjustment
                && mGrammaticalGender == other.mGrammaticalGender && java.util.Objects.equals(locale, other.locale);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof Configuration && equals((Configuration)other);
    }

    @Override
    public int hashCode()
    {
        int result = Float.floatToIntBits(fontScale);
        result = result * 31 + densityDpi;
        result = result * 31 + screenWidthDp;
        result = result * 31 + screenHeightDp;
        result = result * 31 + screenLayout;
        result = result * 31 + orientation;
        result = result * 31 + uiMode;
        return result;
    }

    @Override
    public String toString()
    {
        return "Configuration{" + screenWidthDp + "x" + screenHeightDp + "dp density=" + densityDpi + " locale=" + locale + "}";
    }
}
