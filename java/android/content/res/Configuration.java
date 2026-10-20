package android.content.res;

import android.content.pm.ActivityInfo;
import android.os.LocaleList;
import java.util.Locale;

public class Configuration
{
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;

    public static final int SCREENLAYOUT_SIZE_MASK = 0x0f;
    public static final int SCREENLAYOUT_SIZE_UNDEFINED = 0x00;
    public static final int SCREENLAYOUT_SIZE_SMALL = 0x01;
    public static final int SCREENLAYOUT_SIZE_NORMAL = 0x02;
    public static final int SCREENLAYOUT_SIZE_LARGE = 0x03;
    public static final int SCREENLAYOUT_SIZE_XLARGE = 0x04;
    public static final int SCREENLAYOUT_LAYOUTDIR_MASK = 0xc0;
    public static final int SCREENLAYOUT_LAYOUTDIR_LTR = 0x40;
    public static final int SCREENLAYOUT_LAYOUTDIR_RTL = 0x80;
    public static final int SCREENLAYOUT_UNDEFINED = 0;

    public static final int UI_MODE_TYPE_MASK = 0x0f;
    public static final int UI_MODE_TYPE_UNDEFINED = 0x00;
    public static final int UI_MODE_TYPE_NORMAL = 0x01;
    public static final int UI_MODE_NIGHT_MASK = 0x30;
    public static final int UI_MODE_NIGHT_UNDEFINED = 0x00;
    public static final int UI_MODE_NIGHT_NO = 0x10;
    public static final int UI_MODE_NIGHT_YES = 0x20;

    public static final int KEYBOARD_UNDEFINED = 0;
    public static final int KEYBOARD_NOKEYS = 1;
    public static final int KEYBOARDHIDDEN_UNDEFINED = 0;
    public static final int KEYBOARDHIDDEN_NO = 1;
    public static final int KEYBOARDHIDDEN_YES = 2;
    public static final int NAVIGATION_UNDEFINED = 0;
    public static final int NAVIGATION_NONAV = 1;
    public static final int NAVIGATIONHIDDEN_UNDEFINED = 0;
    public static final int TOUCHSCREEN_UNDEFINED = 0;
    public static final int TOUCHSCREEN_FINGER = 3;
    public static final int DENSITY_DPI_UNDEFINED = 0;
    public static final int SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int SCREEN_HEIGHT_DP_UNDEFINED = 0;
    public static final int SMALLEST_SCREEN_WIDTH_DP_UNDEFINED = 0;
    public static final int COLOR_MODE_UNDEFINED = 0;
    public static final int FONT_WEIGHT_ADJUSTMENT_UNDEFINED = Integer.MAX_VALUE;

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

    public LocaleList getLocales()
    {
        return mLocaleList;
    }

    public void setLocales(LocaleList locales)
    {
        if (locales == null || locales.isEmpty()) return;

        mLocaleList = locales;
        locale = locales.get(0);
    }

    public void setLocale(Locale value)
    {
        setLocales(new LocaleList(value));
    }

    public int getLayoutDirection()
    {
        return (screenLayout & SCREENLAYOUT_LAYOUTDIR_MASK) == SCREENLAYOUT_LAYOUTDIR_RTL ? 1 : 0;
    }

    public boolean isLayoutSizeAtLeast(int size)
    {
        return (screenLayout & SCREENLAYOUT_SIZE_MASK) >= size;
    }

    public boolean isNightModeActive()
    {
        return (uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES;
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
                && java.util.Objects.equals(locale, other.locale);
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
