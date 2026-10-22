package android.os;

import android.content.Context;

import java.util.List;
import java.util.Set;

@Deprecated
public class PowerWhitelistManager
{
    public static final int TEMPORARY_ALLOWLIST_TYPE_FOREGROUND_SERVICE_ALLOWED =
            PowerExemptionManager.TEMPORARY_ALLOW_LIST_TYPE_FOREGROUND_SERVICE_ALLOWED;
    public static final int TEMPORARY_ALLOWLIST_TYPE_FOREGROUND_SERVICE_NOT_ALLOWED =
            PowerExemptionManager.TEMPORARY_ALLOW_LIST_TYPE_FOREGROUND_SERVICE_NOT_ALLOWED;

    public static final int EVENT_UNSPECIFIED = 0;
    public static final int EVENT_SMS = 1;
    public static final int EVENT_MMS = 2;

    public static final int REASON_UNKNOWN = PowerExemptionManager.REASON_UNKNOWN;
    public static final int REASON_OTHER = PowerExemptionManager.REASON_OTHER;
    public static final int REASON_ACTIVITY_RECOGNITION = PowerExemptionManager.REASON_ACTIVITY_RECOGNITION;
    public static final int REASON_GEOFENCING = PowerExemptionManager.REASON_GEOFENCING;
    public static final int REASON_PUSH_MESSAGING = PowerExemptionManager.REASON_PUSH_MESSAGING;
    public static final int REASON_PUSH_MESSAGING_OVER_QUOTA = PowerExemptionManager.REASON_PUSH_MESSAGING_OVER_QUOTA;
    public static final int REASON_ACCOUNT_TRANSFER = PowerExemptionManager.REASON_ACCOUNT_TRANSFER;
    public static final int REASON_BOOT_COMPLETED = PowerExemptionManager.REASON_BOOT_COMPLETED;
    public static final int REASON_LOCATION_PROVIDER = PowerExemptionManager.REASON_LOCATION_PROVIDER;
    public static final int REASON_MEDIA_BUTTON = PowerExemptionManager.REASON_MEDIA_BUTTON;
    public static final int REASON_NOTIFICATION_SERVICE = PowerExemptionManager.REASON_NOTIFICATION_SERVICE;
    public static final int REASON_SHELL = PowerExemptionManager.REASON_SHELL;

    public PowerWhitelistManager(Context context)
    {
    }

    public void addToWhitelist(String packageName)
    {
        PowerExemptionManager.addPermanent(packageName);
    }

    public void addToWhitelist(List<String> packageNames)
    {
        if (packageNames == null) return;

        for (String packageName : packageNames) PowerExemptionManager.addPermanent(packageName);
    }

    public void removeFromWhitelist(String packageName)
    {
        PowerExemptionManager.removePermanent(packageName);
    }

    public int[] getWhitelistedAppIds(boolean includingIdle)
    {
        return PowerExemptionManager.allowListedAppIds();
    }

    public boolean isWhitelisted(String packageName, boolean includingIdle)
    {
        return PowerExemptionManager.isExempt(packageName);
    }

    public boolean[] isWhitelisted(Set<String> packageNames, boolean includingIdle)
    {
        boolean[] result = new boolean[packageNames == null ? 0 : packageNames.size()];
        int index = 0;

        if (packageNames != null)
        {
            for (String packageName : packageNames) result[index++] = PowerExemptionManager.isExempt(packageName);
        }

        return result;
    }

    public void whitelistAppTemporarily(String packageName, long durationMs)
    {
        PowerExemptionManager.addTemporary(packageName, durationMs, REASON_UNKNOWN, null);
    }

    public void whitelistAppTemporarily(String packageName, long durationMs, int reasonCode, String reason)
    {
        PowerExemptionManager.addTemporary(packageName, durationMs, reasonCode, reason);
    }

    public long whitelistAppTemporarilyForEvent(String packageName, int event, String reason)
    {
        return whitelistAppTemporarilyForEvent(packageName, event, REASON_UNKNOWN, reason);
    }

    public long whitelistAppTemporarilyForEvent(String packageName, int event, int reasonCode, String reason)
    {
        long duration = event == EVENT_MMS ? 60000L : event == EVENT_SMS ? 20000L : 10000L;
        PowerExemptionManager.addTemporary(packageName, duration, reasonCode, reason);
        return duration;
    }
}
