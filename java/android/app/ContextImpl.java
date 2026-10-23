package android.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

public class ContextImpl extends Context
{
    private final ApplicationInfo mApplicationInfo;
    private final PackageManager mPackageManager;
    private final Resources mResources;
    private final ClassLoader mClassLoader;
    private final java.io.File mDataDir;
    private Context mApplicationContext;
    private Resources.Theme mTheme;
    private int mThemeResource;
    private android.view.LayoutInflater mInflater;
    private android.view.WindowManager mWindowManager;
    private LocaleManager mLocaleManager;
    private android.telephony.TelephonyManager mTelephonyManager;
    private NotificationManager mNotificationManager;
    private AlarmManager mAlarmManager;
    private android.view.autofill.AutofillManager mAutofillManager;
    private android.hardware.SensorManager mSensorManager;
    private android.content.ClipboardManager mClipboardManager;
    private android.media.AudioManager mAudioManager;
    private android.os.Vibrator mVibrator;
    private android.os.PowerManager mPowerManager;
    private android.content.ContentResolver mContentResolver;
    private ActivityManager mActivityManager;
    private android.net.ConnectivityManager mConnectivityManager;
    private android.hardware.display.DisplayManager mDisplayManager;
    private android.os.UserManager mUserManager;
    private android.content.pm.LauncherApps mLauncherApps;
    private android.content.pm.ShortcutManager mShortcutManager;
    private android.app.WallpaperManager mWallpaperManager;
    private android.appwidget.AppWidgetManager mAppWidgetManager;
    private android.app.SearchManager mSearchManager;
    private final java.util.Map<String, Object> mServices = new java.util.HashMap<>();

    public ContextImpl(ApplicationInfo applicationInfo, PackageManager packageManager, Resources resources, ClassLoader classLoader,
                       String dataDir)
    {
        mApplicationInfo = applicationInfo;
        mPackageManager = packageManager;
        mResources = resources;
        mClassLoader = classLoader;
        mDataDir = new java.io.File(dataDir);
        mApplicationContext = this;
    }

    public void setApplicationContext(Context applicationContext)
    {
        mApplicationContext = applicationContext;
    }

    @Override
    public Context getApplicationContext()
    {
        return mApplicationContext;
    }

    @Override
    public String getPackageName()
    {
        return mApplicationInfo.packageName;
    }

    @Override
    public PackageManager getPackageManager()
    {
        return mPackageManager;
    }

    @Override
    public ApplicationInfo getApplicationInfo()
    {
        return mApplicationInfo;
    }

    @Override
    public Resources getResources()
    {
        return mResources;
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return mClassLoader;
    }

    @Override
    public String getString(int resourceId)
    {
        return mResources.getString(resourceId);
    }

    @Override
    public java.io.File getFilesDir()
    {
        return ensureDir(new java.io.File(mDataDir, "files"));
    }

    @Override
    public java.io.File getCacheDir()
    {
        return ensureDir(new java.io.File(mDataDir, "cache"));
    }

    @Override
    public java.io.File getObbDir()
    {
        return ensureDir(new java.io.File(mDataDir, "obb"));
    }

    @Override
    public java.io.File getExternalFilesDir(String type)
    {
        java.io.File external = ensureDir(new java.io.File(mDataDir, "external"));
        if (type == null || type.isEmpty()) return external;

        return ensureDir(new java.io.File(external, type));
    }

    @Override
    public java.io.File getExternalCacheDir()
    {
        return ensureDir(new java.io.File(mDataDir, "external-cache"));
    }

    public void updateMetricsForWindow(int widthPixels, int heightPixels)
    {
        updateMetricsForWindow(widthPixels, heightPixels, mResources.getDisplayMetrics().densityDpi);
    }

    public void updateMetricsForWindow(int widthPixels, int heightPixels, int densityDpi)
    {
        mResources.updateMetrics(widthPixels, heightPixels, densityDpi);
    }

    @Override
    public Context createConfigurationContext(android.content.res.Configuration overrideConfiguration)
    {
        ContextImpl copy = new ContextImpl(mApplicationInfo, mPackageManager, mResources.withConfiguration(overrideConfiguration),
                                           mClassLoader, mDataDir.getPath());
        copy.mApplicationContext = mApplicationContext;
        copy.mThemeResource = mThemeResource;
        return copy;
    }

    @Override
    public java.io.File getDir(String name, int mode)
    {
        return ensureDir(new java.io.File(mDataDir, "app_" + name));
    }

    @Override
    public Resources.Theme getTheme()
    {
        if (mTheme == null)
        {
            mTheme = mResources.newTheme();
            if (mThemeResource != 0) mTheme.applyStyle(mThemeResource, true);
        }
        return mTheme;
    }

    @Override
    public void setTheme(int resourceId)
    {
        if (mThemeResource == resourceId) return;

        mThemeResource = resourceId;
        mTheme = null;
    }

    public int getThemeResource()
    {
        return mThemeResource;
    }

    @Override
    public synchronized android.content.ContentResolver getContentResolver()
    {
        if (mContentResolver == null) mContentResolver = new android.content.ContentResolver(this);
        return mContentResolver;
    }

    @Override
    public Object getSystemService(String name)
    {
        if (Context.LAYOUT_INFLATER_SERVICE.equals(name))
        {
            if (mInflater == null) mInflater = new android.view.RuntimeLayoutInflater(this);
            return mInflater;
        }

        if (Context.WINDOW_SERVICE.equals(name))
        {
            if (mWindowManager == null) mWindowManager = new android.view.WindowManagerImpl(this);
            return mWindowManager;
        }

        if (Context.LOCALE_SERVICE.equals(name))
        {
            if (mLocaleManager == null) mLocaleManager = new LocaleManager();
            return mLocaleManager;
        }

        if (Context.INPUT_METHOD_SERVICE.equals(name))
        {
            return android.view.inputmethod.InputMethodManager.getInstance();
        }

        if (Context.NOTIFICATION_SERVICE.equals(name))
        {
            if (mNotificationManager == null) mNotificationManager = new NotificationManager();
            return mNotificationManager;
        }

        if (Context.ALARM_SERVICE.equals(name))
        {
            if (mAlarmManager == null) mAlarmManager = new AlarmManager();
            return mAlarmManager;
        }

        if (Context.JOB_SCHEDULER_SERVICE.equals(name))
        {
            return runtime.Jobs.instance();
        }

        if (Context.AUTOFILL_MANAGER_SERVICE.equals(name))
        {
            if (mAutofillManager == null) mAutofillManager = new android.view.autofill.AutofillManager();
            return mAutofillManager;
        }

        if ("phone".equals(name))
        {
            if (mTelephonyManager == null) mTelephonyManager = new android.telephony.TelephonyManager();
            return mTelephonyManager;
        }

        if ("sensor".equals(name))
        {
            if (mSensorManager == null) mSensorManager = new android.hardware.SensorManager();
            return mSensorManager;
        }

        if ("clipboard".equals(name))
        {
            if (mClipboardManager == null) mClipboardManager = new android.content.ClipboardManager();
            return mClipboardManager;
        }

        if ("audio".equals(name))
        {
            if (mAudioManager == null) mAudioManager = new android.media.AudioManager();
            return mAudioManager;
        }

        if ("vibrator".equals(name))
        {
            if (mVibrator == null) mVibrator = new android.os.Vibrator();
            return mVibrator;
        }

        if ("power".equals(name))
        {
            if (mPowerManager == null) mPowerManager = new android.os.PowerManager();
            return mPowerManager;
        }

        if (Context.ACCESSIBILITY_SERVICE.equals(name))
        {
            return android.view.accessibility.AccessibilityManager.getInstance();
        }

        if ("captioning".equals(name))
        {
            return android.view.accessibility.CaptioningManager.getInstance();
        }

        if (Context.ACTIVITY_SERVICE.equals(name))
        {
            if (mActivityManager == null) mActivityManager = new ActivityManager(this);
            return mActivityManager;
        }

        if (Context.CONNECTIVITY_SERVICE.equals(name))
        {
            if (mConnectivityManager == null) mConnectivityManager = new android.net.ConnectivityManager();
            return mConnectivityManager;
        }

        if (Context.DISPLAY_SERVICE.equals(name))
        {
            if (mDisplayManager == null) mDisplayManager = new android.hardware.display.DisplayManager(this);
            return mDisplayManager;
        }

        if (Context.USER_SERVICE.equals(name))
        {
            if (mUserManager == null) mUserManager = new android.os.UserManager(this);
            return mUserManager;
        }

        if (Context.LAUNCHER_APPS_SERVICE.equals(name))
        {
            if (mLauncherApps == null) mLauncherApps = new android.content.pm.LauncherApps(this);
            return mLauncherApps;
        }

        if (Context.SHORTCUT_SERVICE.equals(name))
        {
            if (mShortcutManager == null) mShortcutManager = new android.content.pm.ShortcutManager(this);
            return mShortcutManager;
        }

        if (Context.WALLPAPER_SERVICE.equals(name))
        {
            if (mWallpaperManager == null) mWallpaperManager = android.app.WallpaperManager.getInstance(this);
            return mWallpaperManager;
        }

        if (Context.APPWIDGET_SERVICE.equals(name))
        {
            if (mAppWidgetManager == null) mAppWidgetManager = android.appwidget.AppWidgetManager.getInstance(this);
            return mAppWidgetManager;
        }

        if (Context.SEARCH_SERVICE.equals(name))
        {
            if (mSearchManager == null) mSearchManager = new android.app.SearchManager(this);
            return mSearchManager;
        }

        synchronized (mServices)
        {
            Object cached = mServices.get(name);
            if (cached != null) return cached;

            Object created = createService(name);
            if (created != null)
            {
                mServices.put(name, created);
                return created;
            }
        }

        runtime.Strict.unimplemented("android.content.Context.getSystemService(" + name + ")");
        return null;
    }

    private Object createService(String name)
    {
        if (name == null) return null;

        switch (name)
        {
            case Context.APP_OPS_SERVICE: return new AppOpsManager(this);
            case Context.CAMERA_SERVICE: return new android.hardware.camera2.CameraManager(this);
            case Context.MEDIA_SESSION_SERVICE: return new android.media.session.MediaSessionManager(this);
            case Context.MEDIA_ROUTER_SERVICE: return new android.media.MediaRouter(this);
            case Context.MEDIA_METRICS_SERVICE: return new android.media.metrics.MediaMetricsManager(this);
            case Context.MIDI_SERVICE: return new android.media.midi.MidiManager(this);
            case Context.MEDIA_PROJECTION_SERVICE: return new android.media.projection.MediaProjectionManager(this);
            case Context.TV_INPUT_SERVICE: return new android.media.tv.TvInputManager(this);
            case Context.TELEPHONY_SUBSCRIPTION_SERVICE: return new android.telephony.SubscriptionManager(this);
            case Context.TELECOM_SERVICE: return new android.telecom.TelecomManager(this);
            case Context.CONSUMER_IR_SERVICE: return new android.hardware.ConsumerIrManager(this);
            case Context.INPUT_SERVICE: return new android.hardware.input.InputManager(this);
            case Context.USB_SERVICE: return new android.hardware.usb.UsbManager(this);
            case Context.BIOMETRIC_SERVICE: return new android.hardware.biometrics.BiometricManager(this);
            case Context.FINGERPRINT_SERVICE: return new android.hardware.fingerprint.FingerprintManager(this);
            case Context.NFC_SERVICE: return new android.nfc.NfcManager(this);
            case Context.PRINT_SERVICE: return new android.print.PrintManager(this);
            case Context.CAPTIONING_SERVICE: return android.view.accessibility.CaptioningManager.getInstance();
            case Context.CONTENT_CAPTURE_MANAGER_SERVICE: return android.view.contentcapture.ContentCaptureManager.getInstance();
            case Context.TEXT_CLASSIFICATION_SERVICE: return android.view.textclassifier.TextClassificationManager.getInstance();
            case Context.UI_MODE_SERVICE: return new UiModeManager(this);
            case Context.KEYGUARD_SERVICE: return new KeyguardManager(this);
            case Context.DOWNLOAD_SERVICE: return new DownloadManager(this);
            case Context.GAME_SERVICE: return new GameManager(this);
            case Context.USAGE_STATS_SERVICE: return new android.app.usage.UsageStatsManager(this);
            case Context.STORAGE_STATS_SERVICE: return new android.app.usage.StorageStatsManager(this);
            case Context.NETWORK_STATS_SERVICE: return new android.app.usage.NetworkStatsManager(this);
            case Context.ACCOUNT_SERVICE: return new android.accounts.AccountManager(this);
            case Context.LOCATION_SERVICE: return new android.location.LocationManager(this);
            case Context.BATTERY_SERVICE: return new android.os.BatteryManager(this);
            case Context.STORAGE_SERVICE: return new android.os.storage.StorageManager(this);
            case Context.VIBRATOR_MANAGER_SERVICE: return new android.os.VibratorManager(this);
            case Context.PERFORMANCE_HINT_SERVICE: return new android.os.PerformanceHintManager(this);
            case Context.DROPBOX_SERVICE: return new android.os.DropBoxManager(this);
            case Context.WIFI_SERVICE: return new android.net.wifi.WifiManager(this);
            case Context.WIFI_P2P_SERVICE: return new android.net.wifi.p2p.WifiP2pManager(this);
            case Context.NSD_SERVICE: return new android.net.nsd.NsdManager(this);
            case Context.BLUETOOTH_SERVICE: return new android.bluetooth.BluetoothManager(this);
            case Context.RESTRICTIONS_SERVICE: return new android.content.RestrictionsManager(this);
            case Context.GRAMMATICAL_INFLECTION_SERVICE: return new GrammaticalInflectionManager(this);
            case Context.DEVICE_POLICY_SERVICE: return new android.app.admin.DevicePolicyManager(this);
            case Context.SYSTEM_HEALTH_SERVICE: return new android.os.health.SystemHealthManager();
            case Context.POWER_WHITELIST_MANAGER: return new android.os.PowerWhitelistManager(this);
            case Context.POWER_EXEMPTION_SERVICE: return new android.os.PowerExemptionManager(this);
            case Context.CREDENTIAL_SERVICE: return new android.credentials.CredentialManager(this);
            default: return null;
        }
    }

    @Override
    public java.io.File getCodeCacheDir()
    {
        return ensureDir(new java.io.File(mDataDir, "code_cache"));
    }

    @Override
    public java.io.File getNoBackupFilesDir()
    {
        return ensureDir(new java.io.File(mDataDir, "no_backup"));
    }

    @Override
    public java.io.File getDataDir()
    {
        return ensureDir(mDataDir);
    }

    @Override
    public android.content.res.AssetManager getAssets()
    {
        return getResources().getAssets();
    }

    @Override
    public android.content.SharedPreferences getSharedPreferences(String name, int mode)
    {
        if (name == null) throw new IllegalArgumentException("preferences name must not be null");
        return RuntimeSharedPreferences.forName(ensureDir(new java.io.File(mDataDir, "shared_prefs")), name);
    }

    @Override
    public java.io.FileInputStream openFileInput(String name) throws java.io.FileNotFoundException
    {
        return new java.io.FileInputStream(getFileStreamPath(name));
    }

    @Override
    public java.io.FileOutputStream openFileOutput(String name, int mode) throws java.io.FileNotFoundException
    {
        boolean append = (mode & Context.MODE_APPEND) != 0;
        return new java.io.FileOutputStream(getFileStreamPath(name), append);
    }

    @Override
    public boolean deleteFile(String name)
    {
        return getFileStreamPath(name).delete();
    }

    @Override
    public java.io.File getFileStreamPath(String name)
    {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("file name must not be empty");
        if (name.indexOf(java.io.File.separatorChar) >= 0)
        {
            throw new IllegalArgumentException("File " + name + " contains a path separator");
        }

        return new java.io.File(getFilesDir(), name);
    }

    @Override
    public String[] fileList()
    {
        String[] names = getFilesDir().list();
        return names == null ? new String[0] : names;
    }

    @Override
    public java.io.File getDatabasePath(String name)
    {
        return new java.io.File(ensureDir(new java.io.File(mDataDir, "databases")), name);
    }

    private static java.io.File ensureDir(java.io.File dir)
    {
        if (!dir.exists() && !dir.mkdirs())
        {
            android.util.Log.w("ContextImpl", "could not create " + dir);
        }
        return dir;
    }
}
