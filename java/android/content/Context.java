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
    public static final String ACCOUNT_SERVICE = "account";
    public static final String APP_OPS_SERVICE = "appops";
    public static final String APP_SEARCH_SERVICE = "app_search";
    public static final String BATTERY_SERVICE = "batterymanager";
    public static final String BIOMETRIC_SERVICE = "biometric";
    public static final String BLOB_STORE_SERVICE = "blob_store";
    public static final String BLUETOOTH_SERVICE = "bluetooth";
    public static final String BUGREPORT_SERVICE = "bugreport";
    public static final String CAMERA_SERVICE = "camera";
    public static final String CARRIER_CONFIG_SERVICE = "carrier_config";
    public static final String COMPANION_DEVICE_SERVICE = "companiondevice";
    public static final String CONNECTIVITY_DIAGNOSTICS_SERVICE = "connectivity_diagnostics";
    public static final String CONSUMER_IR_SERVICE = "consumer_ir";
    public static final String CONTENT_CAPTURE_MANAGER_SERVICE = "content_capture";
    public static final String CREDENTIAL_SERVICE = "credential";
    public static final String CROSS_PROFILE_APPS_SERVICE = "crossprofileapps";
    public static final String DEVICE_LOCK_SERVICE = "device_lock";
    public static final String DEVICE_POLICY_SERVICE = "device_policy";
    public static final String DISPLAY_HASH_SERVICE = "display_hash";
    public static final String DOMAIN_VERIFICATION_SERVICE = "domain_verification";
    public static final String DOWNLOAD_SERVICE = "download";
    public static final String DROPBOX_SERVICE = "dropbox";
    public static final String EUICC_SERVICE = "euicc";
    public static final String FILE_INTEGRITY_SERVICE = "file_integrity";
    public static final String FINGERPRINT_SERVICE = "fingerprint";
    public static final String GRAMMATICAL_INFLECTION_SERVICE = "grammatical_inflection";
    public static final String HARDWARE_PROPERTIES_SERVICE = "hardware_properties";
    public static final String HEALTHCONNECT_SERVICE = "healthconnect";
    public static final String INPUT_SERVICE = "input";
    public static final String IPSEC_SERVICE = "ipsec";
    public static final String LOCATION_SERVICE = "location";
    public static final String MEDIA_COMMUNICATION_SERVICE = "media_communication";
    public static final String MEDIA_METRICS_SERVICE = "media_metrics";
    public static final String MEDIA_PROJECTION_SERVICE = "media_projection";
    public static final String MEDIA_SESSION_SERVICE = "media_session";
    public static final String MIDI_SERVICE = "midi";
    public static final String NETWORK_STATS_SERVICE = "netstats";
    public static final String NFC_SERVICE = "nfc";
    public static final String NSD_SERVICE = "servicediscovery";
    public static final String OVERLAY_SERVICE = "overlay";
    public static final String PEOPLE_SERVICE = "people";
    public static final String PERFORMANCE_HINT_SERVICE = "performance_hint";
    public static final String POWER_EXEMPTION_SERVICE = "power_exemption";
    public static final String POWER_WHITELIST_MANAGER = "power_whitelist";
    public static final String PRINT_SERVICE = "print";
    public static final String RESTRICTIONS_SERVICE = "restrictions";
    public static final String ROLE_SERVICE = "role";
    public static final String STATUS_BAR_SERVICE = "statusbar";
    public static final String STORAGE_STATS_SERVICE = "storagestats";
    public static final String SYSTEM_HEALTH_SERVICE = "systemhealth";
    public static final String TELECOM_SERVICE = "telecom";
    public static final String TELEPHONY_IMS_SERVICE = "telephony_ims";
    public static final String TELEPHONY_SUBSCRIPTION_SERVICE = "telephony_subscription_service";
    public static final String TEXT_CLASSIFICATION_SERVICE = "textclassification";
    public static final String TEXT_SERVICES_MANAGER_SERVICE = "textservices";
    public static final String TV_INPUT_SERVICE = "tv_input";
    public static final String TV_INTERACTIVE_APP_SERVICE = "tv_interactive_app";
    public static final String USB_SERVICE = "usb";
    public static final String VIBRATOR_MANAGER_SERVICE = "vibrator_manager";
    public static final String VIRTUAL_DEVICE_SERVICE = "virtualdevice";
    public static final String VPN_MANAGEMENT_SERVICE = "vpn_management";
    public static final String WIFI_AWARE_SERVICE = "wifiaware";
    public static final String WIFI_P2P_SERVICE = "wifip2p";
    public static final String WIFI_RTT_RANGING_SERVICE = "wifirtt";

    private static final java.util.Map<String, String> SERVICE_NAMES_BY_CLASS = new java.util.HashMap<>();

    static
    {
        SERVICE_NAMES_BY_CLASS.put("android.view.LayoutInflater", LAYOUT_INFLATER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.WindowManager", WINDOW_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.UiModeManager", UI_MODE_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.accessibility.AccessibilityManager", ACCESSIBILITY_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.accessibility.CaptioningManager", CAPTIONING_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.hardware.camera2.CameraManager", CAMERA_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.media.session.MediaSessionManager", MEDIA_SESSION_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.media.MediaRouter", MEDIA_ROUTER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.media.metrics.MediaMetricsManager", MEDIA_METRICS_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.media.midi.MidiManager", MIDI_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.media.projection.MediaProjectionManager", MEDIA_PROJECTION_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.media.tv.TvInputManager", TV_INPUT_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.telephony.SubscriptionManager", TELEPHONY_SUBSCRIPTION_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.telecom.TelecomManager", TELECOM_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.hardware.ConsumerIrManager", CONSUMER_IR_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.hardware.input.InputManager", INPUT_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.hardware.usb.UsbManager", USB_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.hardware.biometrics.BiometricManager", BIOMETRIC_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.hardware.fingerprint.FingerprintManager", FINGERPRINT_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.nfc.NfcManager", NFC_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.print.PrintManager", PRINT_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.contentcapture.ContentCaptureManager", CONTENT_CAPTURE_MANAGER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.view.textclassifier.TextClassificationManager", TEXT_CLASSIFICATION_SERVICE);
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
        SERVICE_NAMES_BY_CLASS.put("android.app.AppOpsManager", APP_OPS_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.KeyguardManager", KEYGUARD_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.DownloadManager", DOWNLOAD_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.GameManager", GAME_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.usage.UsageStatsManager", USAGE_STATS_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.usage.StorageStatsManager", STORAGE_STATS_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.usage.NetworkStatsManager", NETWORK_STATS_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.accounts.AccountManager", ACCOUNT_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.location.LocationManager", LOCATION_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.BatteryManager", BATTERY_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.storage.StorageManager", STORAGE_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.VibratorManager", VIBRATOR_MANAGER_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.PerformanceHintManager", PERFORMANCE_HINT_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.DropBoxManager", DROPBOX_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.net.wifi.WifiManager", WIFI_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.net.wifi.p2p.WifiP2pManager", WIFI_P2P_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.net.nsd.NsdManager", NSD_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.bluetooth.BluetoothManager", BLUETOOTH_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.content.RestrictionsManager", RESTRICTIONS_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.GrammaticalInflectionManager", GRAMMATICAL_INFLECTION_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.app.admin.DevicePolicyManager", DEVICE_POLICY_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.health.SystemHealthManager", SYSTEM_HEALTH_SERVICE);
        SERVICE_NAMES_BY_CLASS.put("android.os.PowerWhitelistManager", POWER_WHITELIST_MANAGER);
        SERVICE_NAMES_BY_CLASS.put("android.os.PowerExemptionManager", POWER_EXEMPTION_SERVICE);
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

    public AttributionSource getAttributionSource()
    {
        return new AttributionSource.Builder(android.os.Process.myUid()).setPackageName(getPackageName())
            .setAttributionTag(getAttributionTag())
            .build();
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
        return getResources().getText(resourceId);
    }

    public java.util.concurrent.Executor getMainExecutor()
    {
        final android.os.Handler handler = new android.os.Handler(getMainLooper());

        return new java.util.concurrent.Executor()
        {
            @Override
            public void execute(Runnable command)
            {
                if (!handler.post(command)) throw new java.util.concurrent.RejectedExecutionException(handler + " is shutting down");
            }
        };
    }

    public String getOpPackageName()
    {
        return getPackageName();
    }

    public String getBasePackageName()
    {
        return getPackageName();
    }

    public boolean moveSharedPreferencesFrom(Context sourceContext, String name)
    {
        return true;
    }

    public boolean deleteSharedPreferences(String name)
    {
        java.io.File file = new java.io.File(new java.io.File(getDataDir(), "shared_prefs"), name + ".xml");
        new java.io.File(file.getPath() + ".bak").delete();
        return !file.exists() || file.delete();
    }

    public java.io.File[] getExternalMediaDirs()
    {
        return getExternalFilesDirs(null);
    }

    public boolean deleteDatabase(String name)
    {
        java.io.File database = getDatabasePath(name);
        if (database == null) return false;

        boolean deleted = database.delete();
        for (String suffix : new String[] {"-journal", "-shm", "-wal"}) new java.io.File(database.getPath() + suffix).delete();

        return deleted;
    }

    public String[] databaseList()
    {
        java.io.File database = getDatabasePath("probe");
        java.io.File directory = database == null ? null : database.getParentFile();
        String[] names = directory == null ? null : directory.list();

        return names == null ? new String[0] : names;
    }

    public void startActivities(Intent[] intents)
    {
        startActivities(intents, null);
    }

    public void startActivities(Intent[] intents, android.os.Bundle options)
    {
        if (intents == null) return;

        for (Intent intent : intents) startActivity(intent, options);
    }

    public void sendBroadcast(Intent intent, String receiverPermission, android.os.Bundle options)
    {
        sendBroadcast(intent, receiverPermission);
    }

    public void sendOrderedBroadcast(Intent intent, String receiverPermission, BroadcastReceiver resultReceiver,
                                     android.os.Handler scheduler, int initialCode, String initialData, android.os.Bundle initialExtras)
    {
        runtime.Broadcasts.send(intent, resultReceiver, true);
    }

    public void sendOrderedBroadcast(Intent intent, String receiverPermission, android.os.Bundle options, BroadcastReceiver resultReceiver,
                                     android.os.Handler scheduler, int initialCode, String initialData, android.os.Bundle initialExtras)
    {
        runtime.Broadcasts.send(intent, resultReceiver, true);
    }

    public boolean bindService(Intent service, int flags, java.util.concurrent.Executor executor, ServiceConnection connection)
    {
        return bindService(service, connection, flags);
    }

    public boolean bindIsolatedService(Intent service, int flags, String instanceName, java.util.concurrent.Executor executor,
                                       ServiceConnection connection)
    {
        return bindService(service, connection, flags);
    }

    public void grantUriPermission(String toPackage, android.net.Uri uri, int modeFlags)
    {
    }

    public void revokeUriPermission(android.net.Uri uri, int modeFlags)
    {
    }

    public void revokeUriPermission(String toPackage, android.net.Uri uri, int modeFlags)
    {
    }

    public int checkCallingOrSelfUriPermission(android.net.Uri uri, int modeFlags)
    {
        return PackageManager.PERMISSION_GRANTED;
    }

    public int checkUriPermission(android.net.Uri uri, int pid, int uid, int modeFlags)
    {
        return PackageManager.PERMISSION_GRANTED;
    }

    public Context createContextForSplit(String splitName) throws PackageManager.NameNotFoundException
    {
        return this;
    }

    public Context createDeviceContext(int deviceId)
    {
        return this;
    }

    public android.view.Display getDisplay()
    {
        android.view.WindowManager manager = (android.view.WindowManager)getSystemService(WINDOW_SERVICE);
        return manager == null ? null : manager.getDefaultDisplay();
    }

    public int getDeviceId()
    {
        return 0;
    }

    public void registerDeviceIdChangeListener(java.util.concurrent.Executor executor, java.util.function.IntConsumer listener)
    {
    }

    public void unregisterDeviceIdChangeListener(java.util.function.IntConsumer listener)
    {
    }

    public boolean isUiContext()
    {
        return false;
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
