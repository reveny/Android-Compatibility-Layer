package android.content.pm;

import android.content.res.Configuration;

public class ConfigurationInfo
{
    public static final int GL_ES_VERSION_UNDEFINED = 0;

    public static final int INPUT_FEATURE_FIVE_WAY_NAV = 0x00000002;
    public static final int INPUT_FEATURE_HARD_KEYBOARD = 0x00000001;

    public int reqTouchScreen = Configuration.TOUCHSCREEN_FINGER;
    public int reqKeyboardType = Configuration.KEYBOARD_NOKEYS;
    public int reqNavigation = Configuration.NAVIGATION_NONAV;
    public int reqInputFeatures;
    public int reqGlEsVersion = GL_ES_VERSION_UNDEFINED;

    public ConfigurationInfo()
    {
    }

    public ConfigurationInfo(ConfigurationInfo source)
    {
        if (source == null) return;

        reqTouchScreen = source.reqTouchScreen;
        reqKeyboardType = source.reqKeyboardType;
        reqNavigation = source.reqNavigation;
        reqInputFeatures = source.reqInputFeatures;
        reqGlEsVersion = source.reqGlEsVersion;
    }

    public String getGlEsVersion()
    {
        return ((reqGlEsVersion & 0xffff0000) >> 16) + "." + (reqGlEsVersion & 0xffff);
    }

    @Override
    public String toString()
    {
        return "ConfigurationInfo{glEsVersion=" + getGlEsVersion() + "}";
    }
}
