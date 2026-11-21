package android.preference;

import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.XmlRes;
import android.app.Activity;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
public class PreferenceManager
{
    private static final String TAG = "PreferenceManager";
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    public static final String METADATA_KEY_PREFERENCES = "android.preference";
    public static final String KEY_HAS_SET_DEFAULT_VALUES = "_has_set_default_values";
    @Nullable
    private PreferenceFragment mFragment;

    private static final int STORAGE_DEFAULT = 0;
    private static final int STORAGE_DEVICE_PROTECTED = 1;

    private Context mContext;
    @Nullable
    private List<DialogInterface> mPreferencesScreens;
    private SharedPreferences mSharedPreferences;
    private PreferenceDataStore mPreferenceDataStore;
    private String mSharedPreferencesName;
    @Nullable
    private List<OnActivityResultListener> mActivityResultListeners;
    @Nullable
    @UnsupportedAppUsage
    private List<OnActivityDestroyListener> mActivityDestroyListeners;
    @UnsupportedAppUsage
    private OnPreferenceTreeClickListener mOnPreferenceTreeClickListener;
    private int mSharedPreferencesMode;
    private int mStorage = STORAGE_DEFAULT;
    @Nullable
    private SharedPreferences.Editor mEditor;
    @Nullable
    private List<OnActivityStopListener> mActivityStopListeners;
    @Nullable
    private PreferenceScreen mPreferenceScreen;
    private boolean mNoCommit;
    private long mNextId = 0;
    private int mNextRequestCode;
    @Nullable
    private Activity mActivity;

    PreferenceManager()
    {
        this(null);
    }

    PreferenceManager(Context context)
    {
        mContext = context;
        if (context != null) setSharedPreferencesName(getDefaultSharedPreferencesName(context));
    }
    private List<ResolveInfo> queryIntentActivities(Intent queryIntent) {
        return mContext.getPackageManager().queryIntentActivities(queryIntent,
                PackageManager.GET_META_DATA);
    }
    @UnsupportedAppUsage
    void registerOnActivityDestroyListener(OnActivityDestroyListener listener) {
            synchronized (this) {
                if (mActivityDestroyListeners == null) {
                    mActivityDestroyListeners = new ArrayList<OnActivityDestroyListener>();
                }

                if (!mActivityDestroyListeners.contains(listener)) {
                    mActivityDestroyListeners.add(listener);
                }
            }
        }
    @UnsupportedAppUsage
    void unregisterOnActivityDestroyListener(OnActivityDestroyListener listener) {
            synchronized (this) {
                if (mActivityDestroyListeners != null) {
                    mActivityDestroyListeners.remove(listener);
                }
            }
   }
    @Deprecated
    public interface OnActivityResultListener {

        /**
         * See Activity's onActivityResult.
         *
         * @return Whether the request code was handled (in which case
         *         subsequent listeners will not be called.
         */
        boolean onActivityResult(int requestCode, int resultCode, Intent data);
    }

    @UnsupportedAppUsage
    public PreferenceManager(Activity activity, int firstRequestCode) {
        mActivity = activity;
        mNextRequestCode = firstRequestCode;

        init(activity);
    }
    private void init(Context context) {
        mContext = context;

        setSharedPreferencesName(getDefaultSharedPreferencesName(context));
    }
    @Deprecated
    public interface OnActivityDestroyListener {

        /**
         * See Activity's onDestroy.
         */
        void onActivityDestroy();
    }

    public PreferenceDataStore getPreferenceDataStore()
    {
        return mPreferenceDataStore;
    }
    void setFragment(PreferenceFragment fragment) {
        mFragment = fragment;
    }
    @Nullable
    @UnsupportedAppUsage
    PreferenceFragment getFragment() {
        return mFragment;
    }
    void addPreferencesScreen(DialogInterface screen) {
        synchronized (this) {

            if (mPreferencesScreens == null) {
                mPreferencesScreens = new ArrayList<DialogInterface>();
            }

            mPreferencesScreens.add(screen);
        }
    }

    void removePreferencesScreen(DialogInterface screen) {
        synchronized (this) {

            if (mPreferencesScreens == null) {
                return;
            }

            mPreferencesScreens.remove(screen);
        }
    }
    public void setPreferenceDataStore(PreferenceDataStore dataStore)
    {
        mPreferenceDataStore = dataStore;
    }
    @UnsupportedAppUsage
    void dispatchActivityStop() {
        List<OnActivityStopListener> list;

        synchronized (this) {
            if (mActivityStopListeners == null) return;
            list = new ArrayList<OnActivityStopListener>(mActivityStopListeners);
        }

        final int N = list.size();
        for (int i = 0; i < N; i++) {
            list.get(i).onActivityStop();
        }
    }
    @Deprecated
    public interface OnActivityStopListener {

        /**
         * See Activity's onStop.
         */
        void onActivityStop();
    }
    @UnsupportedAppUsage
    void dispatchActivityResult(int requestCode, int resultCode, Intent data) {
        List<OnActivityResultListener> list;

        synchronized (this) {
            if (mActivityResultListeners == null) return;
            list = new ArrayList<OnActivityResultListener>(mActivityResultListeners);
        }

        final int N = list.size();
        for (int i = 0; i < N; i++) {
            if (list.get(i).onActivityResult(requestCode, resultCode, data)) {
                break;
            }
        }
    }
    @UnsupportedAppUsage
    void dispatchActivityDestroy() {
        List<OnActivityDestroyListener> list = null;

        synchronized (this) {
            if (mActivityDestroyListeners != null) {
                list = new ArrayList<OnActivityDestroyListener>(mActivityDestroyListeners);
            }
        }

        if (list != null) {
            final int N = list.size();
            for (int i = 0; i < N; i++) {
                list.get(i).onActivityDestroy();
            }
        }

        // Dismiss any PreferenceScreens still showing
        dismissAllScreens();
    }
    private void dismissAllScreens() {
        // Remove any of the previously shown preferences screens
        ArrayList<DialogInterface> screensToDismiss;

        synchronized (this) {

            if (mPreferencesScreens == null) {
                return;
            }

            screensToDismiss = new ArrayList<DialogInterface>(mPreferencesScreens);
            mPreferencesScreens.clear();
        }

        for (int i = screensToDismiss.size() - 1; i >= 0; i--) {
            screensToDismiss.get(i).dismiss();
        }
    }
    public String getSharedPreferencesName()
    {
        return mSharedPreferencesName;
    }
     @Nullable
    @UnsupportedAppUsage
    PreferenceScreen getPreferenceScreen() {
        return mPreferenceScreen;
    }

    /**
     * Sets the root of the preference hierarchy.
     *
     * @param preferenceScreen The root {@link PreferenceScreen} of the preference hierarchy.
     * @return Whether the {@link PreferenceScreen} given is different than the previous.
     */
    @UnsupportedAppUsage
    boolean setPreferences(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != mPreferenceScreen) {
            mPreferenceScreen = preferenceScreen;
            return true;
        }

        return false;
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
     @UnsupportedAppUsage
    SharedPreferences.Editor getEditor() {
        if (mPreferenceDataStore != null) {
            return null;
        }

        if (mNoCommit) {
            if (mEditor == null) {
                mEditor = getSharedPreferences().edit();
            }

            return mEditor;
        } else {
            return getSharedPreferences().edit();
        }
    }

    /**
     * Whether it is the client's responsibility to commit on the
     * {@link #getEditor()}. This will return false in cases where the writes
     * should be batched, for example when inflating preferences from XML.
     *
     * <p>If preferences are using {@link PreferenceDataStore} this value is irrelevant.
     *
     * @return Whether the client should commit.
     */
    @UnsupportedAppUsage
    boolean shouldCommit() {
        return !mNoCommit;
    }
    public void setStorageDeviceProtected()
    {
        mStorage = STORAGE_DEVICE_PROTECTED;
        mSharedPreferences = null;
    }
    void setOnPreferenceTreeClickListener(OnPreferenceTreeClickListener listener) {
        mOnPreferenceTreeClickListener = listener;
    }
    long getNextId() {
        synchronized (this) {
            return mNextId++;
        }
    }
    public boolean isStorageDefault()
    {
        return mStorage == STORAGE_DEFAULT;
    }

    public boolean isStorageDeviceProtected()
    {
        return mStorage == STORAGE_DEVICE_PROTECTED;
    }
    @UnsupportedAppUsage
    private void setNoCommit(boolean noCommit) {
        if (!noCommit && mEditor != null) {
            try {
                mEditor.apply();
            } catch (AbstractMethodError unused) {
                // The app injected its own pre-Gingerbread
                // SharedPreferences.Editor implementation without
                // an apply method.
                mEditor.commit();
            }
        }
        mNoCommit = noCommit;
    }
    @UnsupportedAppUsage
    public PreferenceScreen inflateFromResource(Context context, @XmlRes int resId,
            PreferenceScreen rootPreferences) {
        // Block commits
        setNoCommit(true);

        final PreferenceInflater inflater = new PreferenceInflater(context, this);
        rootPreferences = (PreferenceScreen) inflater.inflate(resId, rootPreferences, true);
        rootPreferences.onAttachedToHierarchy(this);

        // Unblock commits
        setNoCommit(false);

        return rootPreferences;
    }
    @UnsupportedAppUsage
    PreferenceScreen inflateFromIntent(Intent queryIntent, PreferenceScreen rootPreferences) {
        final List<ResolveInfo> activities = queryIntentActivities(queryIntent);
        final HashSet<String> inflatedRes = new HashSet<String>();

        for (int i = activities.size() - 1; i >= 0; i--) {
            final ActivityInfo activityInfo = activities.get(i).activityInfo;
            final Bundle metaData = activityInfo.metaData;

            if ((metaData == null) || !metaData.containsKey(METADATA_KEY_PREFERENCES)) {
                continue;
            }

            // Need to concat the package with res ID since the same res ID
            // can be re-used across contexts
            final String uniqueResId = activityInfo.packageName + ":"
                    + activityInfo.metaData.getInt(METADATA_KEY_PREFERENCES);

            if (!inflatedRes.contains(uniqueResId)) {
                inflatedRes.add(uniqueResId);

                final Context context;
                try {
                    context = mContext.createPackageContext(activityInfo.packageName, 0);
                } catch (Exception e) {
                    Log.w(TAG, "Could not create context for " + activityInfo.packageName + ": "
                            + Log.getStackTraceString(e));
                    continue;
                }

                final PreferenceInflater inflater = new PreferenceInflater(context, this);
                final XmlResourceParser parser = activityInfo.loadXmlMetaData(context
                        .getPackageManager(), METADATA_KEY_PREFERENCES);
                rootPreferences = (PreferenceScreen) inflater
                        .inflate(parser, rootPreferences, true);
                parser.close();
            }
        }

        rootPreferences.onAttachedToHierarchy(this);

        return rootPreferences;
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
    @Deprecated
    public interface OnPreferenceTreeClickListener {
        /**
         * Called when a preference in the tree rooted at this
         * {@link PreferenceScreen} has been clicked.
         *
         * @param preferenceScreen The {@link PreferenceScreen} that the
         *        preference is located in.
         * @param preference The preference that was clicked.
         * @return Whether the click was handled.
         */
        boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference);
    }
    @Nullable
    public Preference findPreference(CharSequence key) {
        if (mPreferenceScreen == null) {
            return null;
        }

        return mPreferenceScreen.findPreference(key);
    }
    @Nullable
    OnPreferenceTreeClickListener getOnPreferenceTreeClickListener() {
        return mOnPreferenceTreeClickListener;
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
