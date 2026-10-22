package android.app;

import android.content.Context;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public class AppOpsManager
{
    public static final int MODE_ALLOWED = 0;
    public static final int MODE_IGNORED = 1;
    public static final int MODE_ERRORED = 2;
    public static final int MODE_DEFAULT = 3;
    public static final int MODE_FOREGROUND = 4;

    public static final int WATCH_FOREGROUND_CHANGES = 1;

    public static final String OPSTR_ADD_VOICEMAIL = "android:add_voicemail";
    public static final String OPSTR_ANSWER_PHONE_CALLS = "android:answer_phone_calls";
    public static final String OPSTR_BODY_SENSORS = "android:body_sensors";
    public static final String OPSTR_CALL_PHONE = "android:call_phone";
    public static final String OPSTR_CAMERA = "android:camera";
    public static final String OPSTR_COARSE_LOCATION = "android:coarse_location";
    public static final String OPSTR_FINE_LOCATION = "android:fine_location";
    public static final String OPSTR_GET_USAGE_STATS = "android:get_usage_stats";
    public static final String OPSTR_MOCK_LOCATION = "android:mock_location";
    public static final String OPSTR_MONITOR_HIGH_POWER_LOCATION = "android:monitor_location_high_power";
    public static final String OPSTR_MONITOR_LOCATION = "android:monitor_location";
    public static final String OPSTR_PICTURE_IN_PICTURE = "android:picture_in_picture";
    public static final String OPSTR_PROCESS_OUTGOING_CALLS = "android:process_outgoing_calls";
    public static final String OPSTR_READ_CALENDAR = "android:read_calendar";
    public static final String OPSTR_READ_CALL_LOG = "android:read_call_log";
    public static final String OPSTR_READ_CELL_BROADCASTS = "android:read_cell_broadcasts";
    public static final String OPSTR_READ_CONTACTS = "android:read_contacts";
    public static final String OPSTR_READ_EXTERNAL_STORAGE = "android:read_external_storage";
    public static final String OPSTR_READ_PHONE_NUMBERS = "android:read_phone_numbers";
    public static final String OPSTR_READ_PHONE_STATE = "android:read_phone_state";
    public static final String OPSTR_READ_SMS = "android:read_sms";
    public static final String OPSTR_RECEIVE_MMS = "android:receive_mms";
    public static final String OPSTR_RECEIVE_SMS = "android:receive_sms";
    public static final String OPSTR_RECEIVE_WAP_PUSH = "android:receive_wap_push";
    public static final String OPSTR_RECORD_AUDIO = "android:record_audio";
    public static final String OPSTR_SEND_SMS = "android:send_sms";
    public static final String OPSTR_SYSTEM_ALERT_WINDOW = "android:system_alert_window";
    public static final String OPSTR_USE_FINGERPRINT = "android:use_fingerprint";
    public static final String OPSTR_USE_SIP = "android:use_sip";
    public static final String OPSTR_WRITE_CALENDAR = "android:write_calendar";
    public static final String OPSTR_WRITE_CALL_LOG = "android:write_call_log";
    public static final String OPSTR_WRITE_CONTACTS = "android:write_contacts";
    public static final String OPSTR_WRITE_EXTERNAL_STORAGE = "android:write_external_storage";
    public static final String OPSTR_WRITE_SETTINGS = "android:write_settings";

    private static final String PERMISSION_PREFIX = "android.permission.";

    private static final String[][] OP_PERMISSIONS = {
        {OPSTR_COARSE_LOCATION, "ACCESS_COARSE_LOCATION"},
        {OPSTR_FINE_LOCATION, "ACCESS_FINE_LOCATION"},
        {OPSTR_CAMERA, "CAMERA"},
        {OPSTR_RECORD_AUDIO, "RECORD_AUDIO"},
        {OPSTR_READ_CONTACTS, "READ_CONTACTS"},
        {OPSTR_WRITE_CONTACTS, "WRITE_CONTACTS"},
        {OPSTR_READ_CALL_LOG, "READ_CALL_LOG"},
        {OPSTR_WRITE_CALL_LOG, "WRITE_CALL_LOG"},
        {OPSTR_READ_CALENDAR, "READ_CALENDAR"},
        {OPSTR_WRITE_CALENDAR, "WRITE_CALENDAR"},
        {OPSTR_CALL_PHONE, "CALL_PHONE"},
        {OPSTR_READ_PHONE_STATE, "READ_PHONE_STATE"},
        {OPSTR_READ_PHONE_NUMBERS, "READ_PHONE_NUMBERS"},
        {OPSTR_READ_SMS, "READ_SMS"},
        {OPSTR_RECEIVE_SMS, "RECEIVE_SMS"},
        {OPSTR_SEND_SMS, "SEND_SMS"},
        {OPSTR_RECEIVE_MMS, "RECEIVE_MMS"},
        {OPSTR_RECEIVE_WAP_PUSH, "RECEIVE_WAP_PUSH"},
        {OPSTR_BODY_SENSORS, "BODY_SENSORS"},
        {OPSTR_ADD_VOICEMAIL, "com.android.voicemail.permission.ADD_VOICEMAIL"},
        {OPSTR_USE_SIP, "USE_SIP"},
        {OPSTR_PROCESS_OUTGOING_CALLS, "PROCESS_OUTGOING_CALLS"},
        {OPSTR_ANSWER_PHONE_CALLS, "ANSWER_PHONE_CALLS"},
        {OPSTR_READ_EXTERNAL_STORAGE, "READ_EXTERNAL_STORAGE"},
        {OPSTR_WRITE_EXTERNAL_STORAGE, "WRITE_EXTERNAL_STORAGE"},
        {OPSTR_USE_FINGERPRINT, "USE_FINGERPRINT"},
        {OPSTR_SYSTEM_ALERT_WINDOW, "SYSTEM_ALERT_WINDOW"},
        {OPSTR_WRITE_SETTINGS, "WRITE_SETTINGS"},
        {OPSTR_GET_USAGE_STATS, "PACKAGE_USAGE_STATS"},
        {OPSTR_READ_CELL_BROADCASTS, "READ_CELL_BROADCASTS"},
        {"android:activity_recognition", "ACTIVITY_RECOGNITION"},
        {"android:read_media_images", "READ_MEDIA_IMAGES"},
        {"android:read_media_video", "READ_MEDIA_VIDEO"},
        {"android:read_media_audio", "READ_MEDIA_AUDIO"},
        {"android:post_notification", "POST_NOTIFICATIONS"},
        {"android:bluetooth_scan", "BLUETOOTH_SCAN"},
        {"android:bluetooth_connect", "BLUETOOTH_CONNECT"},
        {"android:bluetooth_advertise", "BLUETOOTH_ADVERTISE"},
        {"android:nearby_wifi_devices", "NEARBY_WIFI_DEVICES"},
        {"android:request_install_packages", "REQUEST_INSTALL_PACKAGES"},
        {"android:schedule_exact_alarm", "SCHEDULE_EXACT_ALARM"},
        {"android:manage_external_storage", "MANAGE_EXTERNAL_STORAGE"},
        {"android:access_media_location", "ACCESS_MEDIA_LOCATION"},
    };

    private static final String[] SPECIAL_ACCESS_OPS = {
        OPSTR_SYSTEM_ALERT_WINDOW, OPSTR_WRITE_SETTINGS, OPSTR_GET_USAGE_STATS, "android:request_install_packages",
        "android:manage_external_storage",
    };

    private static final Map<String, String> PERMISSION_BY_OP = new HashMap<>();
    private static final Map<String, String> OP_BY_PERMISSION = new HashMap<>();

    static
    {
        for (String[] entry : OP_PERMISSIONS)
        {
            String permission = entry[1].indexOf('.') >= 0 ? entry[1] : PERMISSION_PREFIX + entry[1];
            PERMISSION_BY_OP.put(entry[0], permission);
            OP_BY_PERMISSION.put(permission, entry[0]);
        }
    }

    private final Context mContext;
    private final List<OnOpChangedListener> mModeWatchers = new ArrayList<>();
    private final Map<OnOpActiveChangedListener, ActiveWatcher> mActiveWatchers = new HashMap<>();
    private final Set<String> mActiveOps = new HashSet<>();
    private Executor mNotedExecutor;
    private OnOpNotedCallback mNotedCallback;

    public AppOpsManager(Context context)
    {
        mContext = context;
    }

    public static String permissionToOp(String permission)
    {
        return permission == null ? null : OP_BY_PERMISSION.get(permission);
    }

    public void startWatchingMode(String op, String packageName, OnOpChangedListener callback)
    {
        startWatchingMode(op, packageName, 0, callback);
    }

    public void startWatchingMode(String op, String packageName, int flags, OnOpChangedListener callback)
    {
        if (callback == null) return;

        synchronized (mModeWatchers)
        {
            if (!mModeWatchers.contains(callback)) mModeWatchers.add(callback);
        }
    }

    public void stopWatchingMode(OnOpChangedListener callback)
    {
        synchronized (mModeWatchers)
        {
            mModeWatchers.remove(callback);
        }
    }

    public void startWatchingActive(String[] ops, Executor executor, OnOpActiveChangedListener callback)
    {
        if (ops == null || executor == null || callback == null) throw new NullPointerException("ops, executor and callback are required");

        synchronized (mActiveWatchers)
        {
            mActiveWatchers.put(callback, new ActiveWatcher(ops, executor));
        }
    }

    public void stopWatchingActive(OnOpActiveChangedListener callback)
    {
        synchronized (mActiveWatchers)
        {
            mActiveWatchers.remove(callback);
        }
    }

    public int unsafeCheckOp(String op, int uid, String packageName)
    {
        return checkOp(op, uid, packageName);
    }

    public int checkOp(String op, int uid, String packageName)
    {
        return enforce(op, modeFor(op, packageName));
    }

    public int unsafeCheckOpNoThrow(String op, int uid, String packageName)
    {
        return modeFor(op, packageName);
    }

    public int checkOpNoThrow(String op, int uid, String packageName)
    {
        return modeFor(op, packageName);
    }

    public int unsafeCheckOpRaw(String op, int uid, String packageName)
    {
        return modeFor(op, packageName);
    }

    public int unsafeCheckOpRawNoThrow(String op, int uid, String packageName)
    {
        return modeFor(op, packageName);
    }

    public int noteOp(String op, int uid, String packageName)
    {
        return noteOp(op, uid, packageName, null, null);
    }

    public int noteOp(String op, int uid, String packageName, String attributionTag, String message)
    {
        return enforce(op, noteOpNoThrow(op, uid, packageName, attributionTag, message));
    }

    public int noteOpNoThrow(String op, int uid, String packageName)
    {
        return noteOpNoThrow(op, uid, packageName, null, null);
    }

    public int noteOpNoThrow(String op, int uid, String packageName, String attributionTag, String message)
    {
        int mode = modeFor(op, packageName);
        if (mode == MODE_ALLOWED) dispatchSelfNoted(op, attributionTag);

        return mode;
    }

    public int noteProxyOp(String op, String proxiedPackageName)
    {
        return noteOp(op, android.os.Process.myUid(), proxiedPackageName);
    }

    public int noteProxyOp(String op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message)
    {
        return noteOp(op, proxiedUid, proxiedPackageName, proxiedAttributionTag, message);
    }

    public int noteProxyOpNoThrow(String op, String proxiedPackageName)
    {
        return noteOpNoThrow(op, android.os.Process.myUid(), proxiedPackageName);
    }

    public int noteProxyOpNoThrow(String op, String proxiedPackageName, int proxiedUid)
    {
        return noteOpNoThrow(op, proxiedUid, proxiedPackageName);
    }

    public int noteProxyOpNoThrow(String op, String proxiedPackageName, int proxiedUid, String proxiedAttributionTag, String message)
    {
        return noteOpNoThrow(op, proxiedUid, proxiedPackageName, proxiedAttributionTag, message);
    }

    public void checkPackage(int uid, String packageName)
    {
        if (!isOwnPackage(packageName)) throw new SecurityException("Package " + packageName + " does not belong to " + uid);
    }

    public int startOp(String op, int uid, String packageName)
    {
        return startOp(op, uid, packageName, null, null);
    }

    public int startOp(String op, int uid, String packageName, String attributionTag, String message)
    {
        return enforce(op, startOpNoThrow(op, uid, packageName, attributionTag, message));
    }

    public int startOpNoThrow(String op, int uid, String packageName)
    {
        return startOpNoThrow(op, uid, packageName, null, null);
    }

    public int startOpNoThrow(String op, int uid, String packageName, String attributionTag, String message)
    {
        int mode = noteOpNoThrow(op, uid, packageName, attributionTag, message);
        if (mode == MODE_ALLOWED) setActive(op, uid, packageName, attributionTag, true);

        return mode;
    }

    public int startProxyOp(String op, int proxiedUid, String proxiedPackageName, String proxiedAttributionTag, String message)
    {
        return startOp(op, proxiedUid, proxiedPackageName, proxiedAttributionTag, message);
    }

    public int startProxyOpNoThrow(String op, int proxiedUid, String proxiedPackageName, String proxiedAttributionTag, String message)
    {
        return startOpNoThrow(op, proxiedUid, proxiedPackageName, proxiedAttributionTag, message);
    }

    public void finishOp(String op, int uid, String packageName)
    {
        finishOp(op, uid, packageName, null);
    }

    public void finishOp(String op, int uid, String packageName, String attributionTag)
    {
        setActive(op, uid, packageName, attributionTag, false);
    }

    public void finishProxyOp(String op, int proxiedUid, String proxiedPackageName, String proxiedAttributionTag)
    {
        finishOp(op, proxiedUid, proxiedPackageName, proxiedAttributionTag);
    }

    public boolean isOpActive(String op, int uid, String packageName)
    {
        String prefix = op + "|" + uid + "|" + packageName + "|";

        synchronized (mActiveOps)
        {
            for (String active : mActiveOps)
            {
                if (active.startsWith(prefix)) return true;
            }
        }

        return false;
    }

    public void setOnOpNotedCallback(Executor asyncExecutor, OnOpNotedCallback callback)
    {
        synchronized (this)
        {
            if (callback != null && mNotedCallback != null) throw new IllegalStateException("A callback is already registered");

            mNotedExecutor = asyncExecutor;
            mNotedCallback = callback;
        }
    }

    private boolean isOwnPackage(String packageName)
    {
        return packageName == null || mContext == null || packageName.equals(mContext.getPackageName());
    }

    private int modeFor(String op, String packageName)
    {
        if (op == null) throw new IllegalArgumentException("Unknown operation string: null");
        if (!isOwnPackage(packageName)) return MODE_ERRORED;
        if (OPSTR_MOCK_LOCATION.equals(op)) return MODE_ERRORED;

        String permission = PERMISSION_BY_OP.get(op);
        if (permission == null) return MODE_ALLOWED;
        if (runtime.Permissions.check(permission) == PackageManager.PERMISSION_GRANTED) return MODE_ALLOWED;

        for (String special : SPECIAL_ACCESS_OPS)
        {
            if (special.equals(op)) return MODE_DEFAULT;
        }

        return MODE_IGNORED;
    }

    private static int enforce(String op, int mode)
    {
        if (mode == MODE_ERRORED) throw new SecurityException("Operation " + op + " is not allowed for this package");

        return mode;
    }

    private void dispatchSelfNoted(String op, String attributionTag)
    {
        final OnOpNotedCallback callback;
        final Executor executor;

        synchronized (this)
        {
            callback = mNotedCallback;
            executor = mNotedExecutor;
        }

        if (callback == null || !PERMISSION_BY_OP.containsKey(op)) return;

        final SyncNotedAppOp noted = new SyncNotedAppOp(op, attributionTag);
        if (executor == null)
        {
            callback.onSelfNoted(noted);
            return;
        }

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onSelfNoted(noted);
            }
        });
    }

    private void setActive(final String op, final int uid, final String packageName, String attributionTag, final boolean active)
    {
        if (op == null) return;

        String key = op + "|" + uid + "|" + packageName + "|" + attributionTag;
        boolean changed;

        synchronized (mActiveOps)
        {
            changed = active ? mActiveOps.add(key) : mActiveOps.remove(key);
        }

        if (!changed) return;

        List<Map.Entry<OnOpActiveChangedListener, ActiveWatcher>> watchers;
        synchronized (mActiveWatchers)
        {
            watchers = new ArrayList<>(mActiveWatchers.entrySet());
        }

        for (Map.Entry<OnOpActiveChangedListener, ActiveWatcher> entry : watchers)
        {
            if (!entry.getValue().watches(op)) continue;

            final OnOpActiveChangedListener listener = entry.getKey();
            entry.getValue().mExecutor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    listener.onOpActiveChanged(op, uid, packageName, active);
                }
            });
        }
    }

    private static final class ActiveWatcher
    {
        final String[] mOps;
        final Executor mExecutor;

        ActiveWatcher(String[] ops, Executor executor)
        {
            mOps = ops.clone();
            mExecutor = executor;
        }

        boolean watches(String op)
        {
            for (String watched : mOps)
            {
                if (op.equals(watched)) return true;
            }

            return false;
        }
    }

    public interface OnOpChangedListener
    {
        void onOpChanged(String op, String packageName);
    }

    public interface OnOpActiveChangedListener
    {
        void onOpActiveChanged(String op, int uid, String packageName, boolean active);
    }

    public abstract static class OnOpNotedCallback
    {
        public OnOpNotedCallback()
        {
        }

        public abstract void onNoted(SyncNotedAppOp op);

        public abstract void onSelfNoted(SyncNotedAppOp op);

        public abstract void onAsyncNoted(AsyncNotedAppOp asyncOp);
    }
}
