package android.transition;

import android.util.AttributeSet;

final class TransitionAttributes
{
    static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    private TransitionAttributes()
    {
    }

    static int getInt(AttributeSet attributes, String name, int defaultValue)
    {
        if (attributes == null) return defaultValue;

        try
        {
            return attributes.getAttributeIntValue(ANDROID_NAMESPACE, name, defaultValue);
        }
        catch (RuntimeException exception)
        {
            return defaultValue;
        }
    }

    static float getFloat(AttributeSet attributes, String name, float defaultValue)
    {
        if (attributes == null) return defaultValue;

        try
        {
            return attributes.getAttributeFloatValue(ANDROID_NAMESPACE, name, defaultValue);
        }
        catch (RuntimeException exception)
        {
            return defaultValue;
        }
    }

    static boolean getBoolean(AttributeSet attributes, String name, boolean defaultValue)
    {
        if (attributes == null) return defaultValue;

        try
        {
            return attributes.getAttributeBooleanValue(ANDROID_NAMESPACE, name, defaultValue);
        }
        catch (RuntimeException exception)
        {
            return defaultValue;
        }
    }

    static int getResource(AttributeSet attributes, String name, int defaultValue)
    {
        if (attributes == null) return defaultValue;

        try
        {
            return attributes.getAttributeResourceValue(ANDROID_NAMESPACE, name, defaultValue);
        }
        catch (RuntimeException exception)
        {
            return defaultValue;
        }
    }

    static String getString(AttributeSet attributes, String name)
    {
        if (attributes == null) return null;

        try
        {
            return attributes.getAttributeValue(ANDROID_NAMESPACE, name);
        }
        catch (RuntimeException exception)
        {
            return null;
        }
    }
}
