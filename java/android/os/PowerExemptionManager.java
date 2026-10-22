package android.os;

import android.content.Context;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PowerExemptionManager
{
    private static final String TAG = "PowerExemptionManager";

    public static final int ALLOW_LIST_TYPE_NONE = 0;
    public static final int TEMPORARY_ALLOW_LIST_TYPE_NONE = -1;
    public static final int TEMPORARY_ALLOW_LIST_TYPE_FOREGROUND_SERVICE_ALLOWED = 0;
    public static final int TEMPORARY_ALLOW_LIST_TYPE_FOREGROUND_SERVICE_NOT_ALLOWED = 1;

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_OTHER = 1;
    public static final int REASON_PROC_STATE_PERSISTENT = 10;
    public static final int REASON_PROC_STATE_PERSISTENT_UI = 11;
    public static final int REASON_PROC_STATE_TOP = 12;
    public static final int REASON_PROC_STATE_BTOP = 13;
    public static final int REASON_PROC_STATE_FGS = 14;
    public static final int REASON_PROC_STATE_BFGS = 15;
    public static final int REASON_UID_VISIBLE = 50;
    public static final int REASON_SYSTEM_UID = 51;
    public static final int REASON_ACTIVITY_STARTER = 52;
    public static final int REASON_START_ACTIVITY_FLAG = 53;
    public static final int REASON_FGS_BINDING = 54;
    public static final int REASON_DEVICE_OWNER = 55;
    public static final int REASON_PROFILE_OWNER = 56;
    public static final int REASON_COMPANION_DEVICE_MANAGER = 57;
    public static final int REASON_BACKGROUND_ACTIVITY_PERMISSION = 58;
    public static final int REASON_BACKGROUND_FGS_PERMISSION = 59;
    public static final int REASON_INSTR_BACKGROUND_ACTIVITY_PERMISSION = 60;
    public static final int REASON_INSTR_BACKGROUND_FGS_PERMISSION = 61;
    public static final int REASON_SYSTEM_ALERT_WINDOW_PERMISSION = 62;
    public static final int REASON_DEVICE_DEMO_MODE = 63;
    public static final int REASON_ALLOWLISTED_PACKAGE = 65;
    public static final int REASON_APPOP = 66;
    public static final int REASON_ACTIVITY_VISIBILITY_GRACE_PERIOD = 67;
    public static final int REASON_OP_ACTIVATE_VPN = 68;
    public static final int REASON_OP_ACTIVATE_PLATFORM_VPN = 69;
    public static final int REASON_TEMP_ALLOWED_WHILE_IN_USE = 70;
    public static final int REASON_CURRENT_INPUT_METHOD = 71;
    public static final int REASON_GEOFENCING = 100;
    public static final int REASON_PUSH_MESSAGING = 101;
    public static final int REASON_PUSH_MESSAGING_OVER_QUOTA = 102;
    public static final int REASON_ACTIVITY_RECOGNITION = 103;
    public static final int REASON_ACCOUNT_TRANSFER = 104;
    public static final int REASON_BOOT_COMPLETED = 200;
    public static final int REASON_PRE_BOOT_COMPLETED = 201;
    public static final int REASON_LOCKED_BOOT_COMPLETED = 202;
    public static final int REASON_BLUETOOTH_BROADCAST = 203;
    public static final int REASON_TIMEZONE_CHANGED = 204;
    public static final int REASON_TIME_CHANGED = 205;
    public static final int REASON_LOCALE_CHANGED = 206;
    public static final int REASON_SYNC_MANAGER = 207;
    public static final int REASON_DOMAIN_VERIFICATION_V1 = 208;
    public static final int REASON_DOMAIN_VERIFICATION_V2 = 209;
    public static final int REASON_VPN = 210;
    public static final int REASON_NOTIFICATION_SERVICE = 211;
    public static final int REASON_PACKAGE_REPLACED = 212;
    public static final int REASON_LOCATION_PROVIDER = 213;
    public static final int REASON_MEDIA_BUTTON = 214;
    public static final int REASON_EVENT_SMS = 215;
    public static final int REASON_EVENT_MMS = 216;
    public static final int REASON_SHELL = 217;
    public static final int REASON_MEDIA_SESSION_CALLBACK = 218;
    public static final int REASON_ROLE_DIALER = 219;
    public static final int REASON_ROLE_EMERGENCY = 220;
    public static final int REASON_SYSTEM_MODULE = 221;
    public static final int REASON_CARRIER_PRIVILEGED_APP = 222;
    public static final int REASON_DPO_PROTECTED_APP = 223;
    public static final int REASON_DISALLOW_APPS_CONTROL = 224;
    public static final int REASON_ACTIVE_DEVICE_ADMIN = 324;
    public static final int REASON_MEDIA_NOTIFICATION_TRANSFER = 325;
    public static final int REASON_PACKAGE_INSTALLER = 326;

    private static final Set<String> sPermanent = new LinkedHashSet<>();
    private static final Map<String, Long> sTemporary = new LinkedHashMap<>();

    public PowerExemptionManager(Context context)
    {
    }

    public void addToPermanentAllowList(String packageName)
    {
        addPermanent(packageName);
    }

    public void addToPermanentAllowList(List<String> packageNames)
    {
        if (packageNames == null) return;

        for (String packageName : packageNames) addPermanent(packageName);
    }

    public void removeFromPermanentAllowList(String packageName)
    {
        removePermanent(packageName);
    }

    public int[] getAllowListedAppIds(boolean includingIdle)
    {
        return allowListedAppIds();
    }

    public boolean isAllowListed(String packageName, boolean includingIdle)
    {
        return isExempt(packageName);
    }

    public boolean[] isAllowListed(Set<String> packageNames, boolean includingIdle)
    {
        boolean[] result = new boolean[packageNames == null ? 0 : packageNames.size()];
        int index = 0;

        if (packageNames != null)
        {
            for (String packageName : packageNames) result[index++] = isExempt(packageName);
        }

        return result;
    }

    public void addToTemporaryAllowList(String packageName, int reasonCode, String reason, long durationMs)
    {
        addTemporary(packageName, durationMs, reasonCode, reason);
    }

    public long addToTemporaryAllowListForEvent(String packageName, int reasonCode, String reason, int event)
    {
        long duration = 10000L;
        addTemporary(packageName, duration, reasonCode, reason);
        return duration;
    }

    static void addPermanent(String packageName)
    {
        if (packageName == null) return;

        synchronized (sPermanent)
        {
            sPermanent.add(packageName);
        }
    }

    static void removePermanent(String packageName)
    {
        synchronized (sPermanent)
        {
            sPermanent.remove(packageName);
        }
    }

    static String[] permanent()
    {
        synchronized (sPermanent)
        {
            return sPermanent.toArray(new String[0]);
        }
    }

    static void addTemporary(String packageName, long durationMs, int reasonCode, String reason)
    {
        if (packageName == null) return;

        synchronized (sTemporary)
        {
            sTemporary.put(packageName, SystemClock.elapsedRealtime() + Math.max(0L, durationMs));
        }

        Log.i(TAG, packageName + " is temporarily allow-listed for " + durationMs + "ms (reason " + reasonCode + ": " + reason
                       + "); this device never enters idle, so nothing is deferred either way");
    }

    static boolean isTemporarilyAllowed(String packageName)
    {
        synchronized (sTemporary)
        {
            Long until = sTemporary.get(packageName);
            if (until == null) return false;
            if (until > SystemClock.elapsedRealtime()) return true;

            sTemporary.remove(packageName);
            return false;
        }
    }

    static boolean isExempt(String packageName)
    {
        if (packageName == null) return false;

        synchronized (sPermanent)
        {
            if (sPermanent.contains(packageName)) return true;
        }

        return isTemporarilyAllowed(packageName);
    }

    static int[] allowListedAppIds()
    {
        String own = android.app.ActivityThread.currentPackageName();
        return own != null && isExempt(own) ? new int[] {Process.myUid() % 100000} : new int[0];
    }

    static int[] temporaryAppIds()
    {
        String own = android.app.ActivityThread.currentPackageName();
        return own != null && isTemporarilyAllowed(own) ? new int[] {Process.myUid() % 100000} : new int[0];
    }

    static final class DeviceIdleController extends IDeviceIdleController.Stub
    {
        @Override
        public void addPowerSaveWhitelistApp(String packageName)
        {
            addPermanent(packageName);
        }

        @Override
        public int addPowerSaveWhitelistApps(List<String> packageNames)
        {
            int added = 0;
            if (packageNames == null) return added;

            for (String packageName : packageNames)
            {
                if (packageName == null) continue;

                addPermanent(packageName);
                added++;
            }

            return added;
        }

        @Override
        public void removePowerSaveWhitelistApp(String packageName)
        {
            removePermanent(packageName);
        }

        @Override
        public void removeSystemPowerWhitelistApp(String packageName)
        {
        }

        @Override
        public void restoreSystemPowerWhitelistApp(String packageName)
        {
        }

        @Override
        public String[] getRemovedSystemPowerWhitelistApps()
        {
            return new String[0];
        }

        @Override
        public String[] getSystemPowerWhitelistExceptIdle()
        {
            return new String[0];
        }

        @Override
        public String[] getSystemPowerWhitelist()
        {
            return new String[0];
        }

        @Override
        public String[] getUserPowerWhitelist()
        {
            return permanent();
        }

        @Override
        public String[] getFullPowerWhitelistExceptIdle()
        {
            return permanent();
        }

        @Override
        public String[] getFullPowerWhitelist()
        {
            return permanent();
        }

        @Override
        public int[] getAppIdWhitelistExceptIdle()
        {
            return allowListedAppIds();
        }

        @Override
        public int[] getAppIdWhitelist()
        {
            return allowListedAppIds();
        }

        @Override
        public int[] getAppIdUserWhitelist()
        {
            return allowListedAppIds();
        }

        @Override
        public int[] getAppIdTempWhitelist()
        {
            return temporaryAppIds();
        }

        @Override
        public boolean isPowerSaveWhitelistExceptIdleApp(String packageName)
        {
            return isExempt(packageName);
        }

        @Override
        public boolean isPowerSaveWhitelistApp(String packageName)
        {
            return isExempt(packageName);
        }

        @Override
        public void addPowerSaveTempWhitelistApp(String packageName, long duration, int userId, String reason)
        {
            addTemporary(packageName, duration, REASON_UNKNOWN, reason);
        }

        @Override
        public void addPowerSaveTempWhitelistApp(String packageName, long duration, int userId, int reasonCode, String reason)
        {
            addTemporary(packageName, duration, reasonCode, reason);
        }

        @Override
        public long addPowerSaveTempWhitelistAppForMms(String packageName, int userId, int reasonCode, String reason)
        {
            addTemporary(packageName, 60000L, reasonCode, reason);
            return 60000L;
        }

        @Override
        public long addPowerSaveTempWhitelistAppForSms(String packageName, int userId, int reasonCode, String reason)
        {
            addTemporary(packageName, 20000L, reasonCode, reason);
            return 20000L;
        }

        @Override
        public long whitelistAppTemporarily(String packageName, int userId, int reasonCode, String reason)
        {
            addTemporary(packageName, 10000L, reasonCode, reason);
            return 10000L;
        }

        @Override
        public void exitIdle(String reason)
        {
        }

        @Override
        public int setPreIdleTimeoutMode(int mode)
        {
            return 0;
        }

        @Override
        public void resetPreIdleTimeoutMode()
        {
        }
    }
}
