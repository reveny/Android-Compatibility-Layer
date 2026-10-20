package android.content;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;

public abstract class Context
{
    public static final int MODE_PRIVATE = 0x0000;
    public static final int MODE_WORLD_READABLE = 0x0001;
    public static final int MODE_WORLD_WRITEABLE = 0x0002;
    public static final int MODE_APPEND = 0x8000;

    public static final String LAYOUT_INFLATER_SERVICE = "layout_inflater";
    public static final String WINDOW_SERVICE = "window";
    public static final String UI_MODE_SERVICE = "uimode";
    public static final String ACCESSIBILITY_SERVICE = "accessibility";
    public static final String INPUT_METHOD_SERVICE = "input_method";
    public static final String AUDIO_SERVICE = "audio";
    public static final String MEDIA_ROUTER_SERVICE = "media_router";
    public static final String LOCALE_SERVICE = "locale";
    public static final String ACTIVITY_SERVICE = "activity";
    public static final String POWER_SERVICE = "power";
    public static final String CLIPBOARD_SERVICE = "clipboard";
    public static final String NOTIFICATION_SERVICE = "notification";
    public static final String CONNECTIVITY_SERVICE = "connectivity";
    public static final String SENSOR_SERVICE = "sensor";
    public static final String VIBRATOR_SERVICE = "vibrator";
    public static final String STORAGE_SERVICE = "storage";
    public static final String CAPTIONING_SERVICE = "captioning";
    public static final String TELEPHONY_SERVICE = "phone";
    public static final String DISPLAY_SERVICE = "display";
    public static final String USER_SERVICE = "user";
    public static final String SEARCH_SERVICE = "search";
    public static final String ALARM_SERVICE = "alarm";
    public static final String JOB_SCHEDULER_SERVICE = "jobscheduler";
    public static final String AUTOFILL_MANAGER_SERVICE = "autofill";
    public static final String KEYGUARD_SERVICE = "keyguard";
    public static final String WIFI_SERVICE = "wifi";
    public static final String GAME_SERVICE = "game";
    public static final String LAUNCHER_APPS_SERVICE = "launcherapps";
    public static final String SHORTCUT_SERVICE = "shortcut";
    public static final String WALLPAPER_SERVICE = "wallpaper";
    public static final String APPWIDGET_SERVICE = "appwidget";
    public static final String USAGE_STATS_SERVICE = "usagestats";

    private static final java.util.Map<String, String> SERVICE_NAMES_BY_CLASS = new java.util.HashMap<>();

    static
    {
        SERVICE_NAMES_BY_CLASS.put("android.view.LayoutInflater", LAYOUT_INFLATER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.WindowManager", WINDOW_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.UiModeManager", UI_MODE_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.accessibility.AccessibilityManager", ACCESSIBILITY_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.accessibility.CaptioningManager", CAPTIONING_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.inputmethod.InputMethodManager", INPUT_METHOD_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.media.AudioManager", AUDIO_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.LocaleManager", LOCALE_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.ActivityManager", ACTIVITY_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.PowerManager", POWER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.content.ClipboardManager", CLIPBOARD_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.net.ConnectivityManager", CONNECTIVITY_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.hardware.SensorManager", SENSOR_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.Vibrator", VIBRATOR_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.NotificationManager", NOTIFICATION_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.AlarmManager", ALARM_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.job.JobScheduler", JOB_SCHEDULER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.autofill.AutofillManager", AUTOFILL_MANAGER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.telephony.TelephonyManager", TELEPHONY_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.hardware.display.DisplayManager", DISPLAY_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.UserManager", USER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.content.pm.LauncherApps", LAUNCHER_APPS_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.content.pm.ShortcutManager", SHORTCUT_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.WallpaperManager", WALLPAPER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.appwidget.AppWidgetManager", APPWIDGET_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.SearchManager", SEARCH_SERVICE);
    }

    public abstract Resources.Theme getTheme();

    public abstract void setTheme(int resourceId);

    public abstract Object getSystemService(String name);

    public abstract ContentResolver getContentResolver();

    public abstract java.io.File getDir(String name, int mode);

    public TypedArray obtainStyledAttributes(int[] attributes)
    {
        return getTheme().obtainStyledAttributes(null, attributes, 0, 0);
    }

    public TypedArray obtainStyledAttributes(int resourceId, int[] attributes)
    {
        return getTheme().obtainStyledAttributes(null, attributes, 0, resourceId);
    }

    public TypedArray obtainStyledAttributes(AttributeSet set, int[] attributes)
    {
        return getTheme().obtainStyledAttributes(set, attributes, 0, 0);
    }

    public TypedArray obtainStyledAttributes(AttributeSet set, int[] attributes, int defStyleAttr, int defStyleRes)
    {
        return getTheme().obtainStyledAttributes(set, attributes, defStyleAttr, defStyleRes);
    }

    public android.graphics.drawable.Drawable getDrawable(int resourceId)
    {
        return getResources().getDrawable(resourceId, getTheme());
    }

    public int getColor(int resourceId)
    {
        return getResources().getColor(resourceId);
    }

    public android.content.res.ColorStateList getColorStateList(int resourceId)
    {
        return getResources().getColorStateList(resourceId);
    }

    public Context createConfigurationContext(android.content.res.Configuration overrideConfiguration)
    {
        return this;
    }

    public Context createDeviceProtectedStorageContext()
    {
        return this;
    }

    public Context createCredentialProtectedStorageContext()
    {
        return this;
    }

    public Context createDisplayContext(android.view.Display display)
    {
        return this;
    }

    public Context createWindowContext(int type, android.os.Bundle options)
    {
        return this;
    }

    public Context createWindowContext(android.view.Display display, int type, android.os.Bundle options)
    {
        return createDisplayContext(display);
    }

    public Context createPackageContext(String packageName, int flags)
    {
        return this;
    }

    public Context createAttributionContext(String attributionTag)
    {
        return this;
    }

    public String getAttributionTag()
    {
        return null;
    }

    public boolean isRestricted()
    {
        return false;
    }

    public boolean isDeviceProtectedStorage()
    {
        return false;
    }

    public String getSystemServiceName(Class<?> serviceClass)
    {
        if (serviceClass == null) return null;

        return SERVICE_NAMES_BY_CLASS.get(serviceClass.getName());
    }

    public <T> T getSystemService(Class<T> serviceClass)
    {
        String name = getSystemServiceName(serviceClass);
        return name == null ? null : serviceClass.cast(getSystemService(name));
    }
    public abstract Context getApplicationContext();

    public abstract String getPackageName();

    public abstract PackageManager getPackageManager();

    public abstract ApplicationInfo getApplicationInfo();

    public abstract Resources getResources();

    public abstract ClassLoader getClassLoader();

    public abstract String getString(int resourceId);

    public abstract java.io.File getFilesDir();

    public abstract java.io.File getCacheDir();

    public abstract java.io.File getObbDir();

    public abstract java.io.File getExternalFilesDir(String type);

    public abstract java.io.File getExternalCacheDir();

    public abstract java.io.File getCodeCacheDir();

    public abstract java.io.File getNoBackupFilesDir();

    public abstract java.io.File getDataDir();

    public abstract android.content.res.AssetManager getAssets();

    public abstract SharedPreferences getSharedPreferences(String name, int mode);

    public abstract java.io.FileInputStream openFileInput(String name) throws java.io.FileNotFoundException;

    public abstract java.io.FileOutputStream openFileOutput(String name, int mode) throws java.io.FileNotFoundException;

    public abstract boolean deleteFile(String name);

    public abstract java.io.File getFileStreamPath(String name);

    public abstract String[] fileList();

    public abstract java.io.File getDatabasePath(String name);

    public String getPackageCodePath()
    {
        ApplicationInfo info = getApplicationInfo();
        return info == null ? null : info.sourceDir;
    }

    public String getPackageResourcePath()
    {
        ApplicationInfo info = getApplicationInfo();
        return info == null ? null : info.publicSourceDir;
    }

    public android.os.Looper getMainLooper()
    {
        return android.os.Looper.getMainLooper();
    }

    public int checkPermission(String permission, int pid, int uid)
    {
        return runtime.Permissions.check(permission);
    }

    public int checkCallingOrSelfPermission(String permission)
    {
        return runtime.Permissions.check(permission);
    }

    public int checkCallingPermission(String permission)
    {
        return runtime.Permissions.check(permission);
    }

    public int checkSelfPermission(String permission)
    {
        return runtime.Permissions.check(permission);
    }

    public void enforceCallingOrSelfPermission(String permission, String message)
    {
        if (runtime.Permissions.check(permission) == PackageManager.PERMISSION_GRANTED) return;

        throw new SecurityException(message != null ? message : "denied: " + permission);
    }

    public void enforcePermission(String permission, int pid, int uid, String message)
    {
        enforceCallingOrSelfPermission(permission, message);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter)
    {
        return runtime.Broadcasts.register(this, receiver, filter);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission,
                                   android.os.Handler scheduler)
    {
        return registerReceiver(receiver, filter);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, String broadcastPermission,
                                   android.os.Handler scheduler, int flags)
    {
        return registerReceiver(receiver, filter);
    }

    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, int flags)
    {
        return runtime.Broadcasts.register(this, receiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver receiver)
    {
        runtime.Broadcasts.unregister(receiver);
    }

    public static final int RECEIVER_EXPORTED = 0x2;
    public static final int RECEIVER_NOT_EXPORTED = 0x4;

    public void sendBroadcast(Intent intent)
    {
        runtime.Broadcasts.send(intent, null, false);
    }

    public void sendBroadcast(Intent intent, String receiverPermission)
    {
        runtime.Broadcasts.send(intent, null, false);
    }

    public void sendOrderedBroadcast(Intent intent, String receiverPermission)
    {
        runtime.Broadcasts.send(intent, null, true);
    }

    public ComponentName startService(Intent service)
    {
        return runtime.Services.start(service);
    }

    public ComponentName startForegroundService(Intent service)
    {
        return runtime.Services.start(service);
    }

    public boolean stopService(Intent service)
    {
        return runtime.Services.stop(service);
    }

    public boolean bindService(Intent service, ServiceConnection connection, int flags)
    {
        return runtime.Services.bind(service, connection, flags);
    }

    public void unbindService(ServiceConnection connection)
    {
        runtime.Services.unbind(connection);
    }

    public static final int BIND_AUTO_CREATE = 0x0001;
    public static final int BIND_DEBUG_UNBIND = 0x0002;
    public static final int BIND_NOT_FOREGROUND = 0x0004;
    public static final int BIND_ABOVE_CLIENT = 0x0008;
    public static final int BIND_ALLOW_OOM_MANAGEMENT = 0x0010;
    public static final int BIND_WAIVE_PRIORITY = 0x0020;
    public static final int BIND_IMPORTANT = 0x0040;
    public static final int BIND_ADJUST_WITH_ACTIVITY = 0x0080;

    public void startActivity(Intent intent)
    {
        runtime.Activities.start(intent, -1);
    }

    public void startActivity(Intent intent, android.os.Bundle options)
    {
        runtime.Activities.start(intent, -1);
    }

    public java.io.File[] getExternalFilesDirs(String type)
    {
        return new java.io.File[] {getExternalFilesDir(type)};
    }

    public java.io.File[] getExternalCacheDirs()
    {
        return new java.io.File[] {getExternalCacheDir()};
    }

    public java.io.File[] getObbDirs()
    {
        return new java.io.File[] {getObbDir()};
    }

    public String getString(int resourceId, Object... formatArgs)
    {
        return String.format(getString(resourceId), formatArgs);
    }

    public CharSequence getText(int resourceId)
    {
        return getString(resourceId);
    }

    public void registerComponentCallbacks(ComponentCallbacks callback)
    {
        if (callback != null) sComponentCallbacks.add(callback);
    }

    public void unregisterComponentCallbacks(ComponentCallbacks callback)
    {
        sComponentCallbacks.remove(callback);
    }

    public static void dispatchConfigurationChanged(android.content.res.Configuration configuration)
    {
        for (ComponentCallbacks callback : new java.util.ArrayList<>(sComponentCallbacks))
        {
            callback.onConfigurationChanged(configuration);
        }
    }

    public static void dispatchLowMemory()
    {
        for (ComponentCallbacks callback : new java.util.ArrayList<>(sComponentCallbacks))
        {
            callback.onLowMemory();
        }
    }

    private static final java.util.ArrayList<ComponentCallbacks> sComponentCallbacks = new java.util.ArrayList<>();
}
