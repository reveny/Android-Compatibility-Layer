package android.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class PreferenceManager
{
    private static final String TAG = "PreferenceManager";
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    public static final String METADATA_KEY_PREFERENCES = "android.preference";
    public static final String KEY_HAS_SET_DEFAULT_VALUES = "_has_set_default_values";

    private static final int STORAGE_DEFAULT = 0;
    private static final int STORAGE_DEVICE_PROTECTED = 1;

    private final Context mContext;
    private SharedPreferences mSharedPreferences;
    private PreferenceDataStore mPreferenceDataStore;
    private String mSharedPreferencesName;
    private int mSharedPreferencesMode;
    private int mStorage = STORAGE_DEFAULT;

    PreferenceManager()
    {
        this(null);
    }

    PreferenceManager(Context context)
    {
        mContext = context;
        if (context != null) setSharedPreferencesName(getDefaultSharedPreferencesName(context));
    }

    public PreferenceDataStore getPreferenceDataStore()
    {
        return mPreferenceDataStore;
    }

    public void setPreferenceDataStore(PreferenceDataStore dataStore)
    {
        mPreferenceDataStore = dataStore;
    }

    public String getSharedPreferencesName()
    {
        return mSharedPreferencesName;
    }

    public void setSharedPreferencesName(String sharedPreferencesName)
    {
        mSharedPreferencesName = sharedPreferencesName;
        mSharedPreferences = null;
    }

    public int getSharedPreferencesMode()
    {
        return mSharedPreferencesMode;
    }

    public void setSharedPreferencesMode(int sharedPreferencesMode)
    {
        mSharedPreferencesMode = sharedPreferencesMode;
        mSharedPreferences = null;
    }

    public void setStorageDefault()
    {
        mStorage = STORAGE_DEFAULT;
        mSharedPreferences = null;
    }

    public void setStorageDeviceProtected()
    {
        mStorage = STORAGE_DEVICE_PROTECTED;
        mSharedPreferences = null;
    }

    public boolean isStorageDefault()
    {
        return mStorage == STORAGE_DEFAULT;
    }

    public boolean isStorageDeviceProtected()
    {
        return mStorage == STORAGE_DEVICE_PROTECTED;
    }

    public SharedPreferences getSharedPreferences()
    {
        if (mPreferenceDataStore != null) return null;
        if (mContext == null) return null;

        if (mSharedPreferences == null)
        {
            Context storageContext = mStorage == STORAGE_DEVICE_PROTECTED ? mContext.createDeviceProtectedStorageContext() : mContext;
            mSharedPreferences = storageContext.getSharedPreferences(mSharedPreferencesName, mSharedPreferencesMode);
        }

        return mSharedPreferences;
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context)
    {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE);
    }

    public static String getDefaultSharedPreferencesName(Context context)
    {
        return context.getPackageName() + "_preferences";
    }

    public static void setDefaultValues(Context context, int resId, boolean readAgain)
    {
        setDefaultValues(context, getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE, resId, readAgain);
    }

    public static void setDefaultValues(Context context, String sharedPreferencesName, int sharedPreferencesMode, int resId, boolean readAgain)
    {
        SharedPreferences marker = context.getSharedPreferences(KEY_HAS_SET_DEFAULT_VALUES, Context.MODE_PRIVATE);

        if (!readAgain && marker.getBoolean(KEY_HAS_SET_DEFAULT_VALUES, false)) return;

        SharedPreferences preferences = context.getSharedPreferences(sharedPreferencesName, sharedPreferencesMode);
        SharedPreferences.Editor editor = preferences.edit();

        try
        {
            applyDefaults(context.getResources(), resId, preferences, editor);
        }
        catch (Exception failure)
        {
            Log.w(TAG, "could not read default preference values from resource 0x" + Integer.toHexString(resId), failure);
        }

        editor.apply();
        marker.edit().putBoolean(KEY_HAS_SET_DEFAULT_VALUES, true).apply();
    }

    private static void applyDefaults(Resources resources, int resId, SharedPreferences preferences, SharedPreferences.Editor editor)
        throws Exception
    {
        XmlResourceParser parser = resources.getXml(resId);

        try
        {
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (type != XmlPullParser.START_TAG) continue;

                String key = parser.getAttributeValue(ANDROID_NAMESPACE, "key");
                if (key == null || preferences.contains(key)) continue;

                int valueResource = parser.getAttributeResourceValue(ANDROID_NAMESPACE, "defaultValue", 0);
                String value = parser.getAttributeValue(ANDROID_NAMESPACE, "defaultValue");
                if (valueResource == 0 && value == null) continue;

                applyDefault(resources, simpleName(parser.getName()), key, valueResource, value, editor);
            }
        }
        finally
        {
            parser.close();
        }
    }

    private static String simpleName(String tag)
    {
        int dot = tag.lastIndexOf('.');
        return dot < 0 ? tag : tag.substring(dot + 1);
    }

    private static void applyDefault(Resources resources, String tag, String key, int valueResource, String value,
                                     SharedPreferences.Editor editor)
    {
        switch (tag)
        {
            case "CheckBoxPreference":
            case "SwitchPreference":
            case "TwoStatePreference":
            case "SwitchPreferenceCompat":
                editor.putBoolean(key, valueResource != 0 ? resources.getBoolean(valueResource) : Boolean.parseBoolean(value));
                return;

            case "MultiSelectListPreference":
                if (valueResource != 0) editor.putStringSet(key, new LinkedHashSet<>(Arrays.asList(resources.getStringArray(valueResource))));
                return;

            case "SeekBarPreference":
                editor.putInt(key, valueResource != 0 ? resources.getInteger(valueResource) : Integer.parseInt(value.trim()));
                return;

            case "EditTextPreference":
            case "ListPreference":
            case "RingtonePreference":
            case "DropDownPreference":
                editor.putString(key, valueResource != 0 ? resources.getString(valueResource) : value);
                return;

            default:
                Log.i(TAG, "the default of " + key + " belongs to " + tag + ", whose value type is not known here, so it is not stored");
        }
    }
}
