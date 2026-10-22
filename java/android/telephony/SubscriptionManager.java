package android.telephony;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

public class SubscriptionManager
{
    public static final String ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED = "android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED";
    public static final String ACTION_DEFAULT_SUBSCRIPTION_CHANGED = "android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED";
    public static final String ACTION_MANAGE_SUBSCRIPTION_PLANS = "android.telephony.action.MANAGE_SUBSCRIPTION_PLANS";
    public static final String ACTION_REFRESH_SUBSCRIPTION_PLANS = "android.telephony.action.REFRESH_SUBSCRIPTION_PLANS";

    public static final int D2D_SHARING_DISABLED = 0;
    public static final int D2D_SHARING_ALL_CONTACTS = 1;
    public static final int D2D_SHARING_SELECTED_CONTACTS = 2;
    public static final int D2D_SHARING_ALL = 3;
    public static final String D2D_STATUS_SHARING = "d2d_sharing_status";
    public static final String D2D_STATUS_SHARING_SELECTED_CONTACTS = "d2d_sharing_contacts";

    public static final int DATA_ROAMING_DISABLE = 0;
    public static final int DATA_ROAMING_ENABLE = 1;

    public static final int DEFAULT_SUBSCRIPTION_ID = Integer.MAX_VALUE;

    public static final String EXTRA_SLOT_INDEX = "android.telephony.extra.SLOT_INDEX";
    public static final String EXTRA_SUBSCRIPTION_INDEX = "android.telephony.extra.SUBSCRIPTION_INDEX";

    public static final int INVALID_SIM_SLOT_INDEX = -1;
    public static final int INVALID_SUBSCRIPTION_ID = -1;

    public static final int PHONE_NUMBER_SOURCE_UICC = 1;
    public static final int PHONE_NUMBER_SOURCE_CARRIER = 2;
    public static final int PHONE_NUMBER_SOURCE_IMS = 3;

    public static final int SERVICE_CAPABILITY_VOICE = 1;
    public static final int SERVICE_CAPABILITY_SMS = 2;
    public static final int SERVICE_CAPABILITY_DATA = 3;

    public static final int SUBSCRIPTION_TYPE_LOCAL_SIM = 0;
    public static final int SUBSCRIPTION_TYPE_REMOTE_SIM = 1;

    public static final int USAGE_SETTING_UNKNOWN = -1;
    public static final int USAGE_SETTING_DEFAULT = 0;
    public static final int USAGE_SETTING_VOICE_CENTRIC = 1;
    public static final int USAGE_SETTING_DATA_CENTRIC = 2;

    private static final Map<OnSubscriptionsChangedListener, Executor> sSubscriptionListeners =
        Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<OnOpportunisticSubscriptionsChangedListener, Executor> sOpportunisticListeners =
        Collections.synchronizedMap(new LinkedHashMap<>());

    private final Context mContext;

    public SubscriptionManager(Context context)
    {
        mContext = context;
    }

    public static SubscriptionManager from(Context context)
    {
        Object service = context == null ? null : context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        return service instanceof SubscriptionManager ? (SubscriptionManager)service : new SubscriptionManager(context);
    }

    private static Executor mainExecutor()
    {
        Handler handler = new Handler(Looper.getMainLooper());
        return command -> handler.post(command);
    }

    public void addOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener)
    {
        addOnSubscriptionsChangedListener(mainExecutor(), listener);
    }

    public void addOnSubscriptionsChangedListener(Executor executor, OnSubscriptionsChangedListener listener)
    {
        if (executor == null || listener == null) return;
        sSubscriptionListeners.put(listener, executor);
        executor.execute(() ->
        {
            if (sSubscriptionListeners.containsKey(listener)) listener.onSubscriptionsChanged();
        });
    }

    public void removeOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener)
    {
        if (listener != null) sSubscriptionListeners.remove(listener);
    }

    public void addOnOpportunisticSubscriptionsChangedListener(Executor executor, OnOpportunisticSubscriptionsChangedListener listener)
    {
        if (executor == null || listener == null) return;
        sOpportunisticListeners.put(listener, executor);
        executor.execute(() ->
        {
            if (sOpportunisticListeners.containsKey(listener)) listener.onOpportunisticSubscriptionsChanged();
        });
    }

    public void removeOnOpportunisticSubscriptionsChangedListener(OnOpportunisticSubscriptionsChangedListener listener)
    {
        if (listener != null) sOpportunisticListeners.remove(listener);
    }

    public SubscriptionInfo getActiveSubscriptionInfo(int subId)
    {
        return null;
    }

    public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIndex)
    {
        return null;
    }

    public List<SubscriptionInfo> getActiveSubscriptionInfoList()
    {
        return new ArrayList<>();
    }

    public List<SubscriptionInfo> getCompleteActiveSubscriptionInfoList()
    {
        return new ArrayList<>();
    }

    public List<SubscriptionInfo> getAllSubscriptionInfoList()
    {
        return new ArrayList<>();
    }

    public List<SubscriptionInfo> getAccessibleSubscriptionInfoList()
    {
        return null;
    }

    public List<SubscriptionInfo> getOpportunisticSubscriptions()
    {
        return new ArrayList<>();
    }

    public List<SubscriptionInfo> getSubscriptionsInGroup(ParcelUuid groupUuid)
    {
        return new ArrayList<>();
    }

    public int getActiveSubscriptionInfoCount()
    {
        return 0;
    }

    public int getActiveSubscriptionInfoCountMax()
    {
        return 0;
    }

    public boolean isActiveSubscriptionId(int subscriptionId)
    {
        return false;
    }

    public boolean isNetworkRoaming(int subId)
    {
        return false;
    }

    public boolean canManageSubscription(SubscriptionInfo info)
    {
        return false;
    }

    public boolean setOpportunistic(boolean opportunistic, int subId)
    {
        return false;
    }

    public ParcelUuid createSubscriptionGroup(List<Integer> subIdList)
    {
        return new ParcelUuid(UUID.randomUUID());
    }

    public void addSubscriptionsIntoGroup(List<Integer> subIdList, ParcelUuid groupUuid)
    {
    }

    public void removeSubscriptionsFromGroup(List<Integer> subIdList, ParcelUuid groupUuid)
    {
    }

    public int[] getSubscriptionIds(int slotIndex)
    {
        return null;
    }

    public String getPhoneNumber(int subscriptionId)
    {
        return "";
    }

    public String getPhoneNumber(int subscriptionId, int source)
    {
        return "";
    }

    public void setCarrierPhoneNumber(int subscriptionId, String number)
    {
    }

    public void setDeviceToDeviceStatusSharingPreference(int subscriptionId, int sharing)
    {
    }

    public int getDeviceToDeviceStatusSharingPreference(int subscriptionId)
    {
        return D2D_SHARING_DISABLED;
    }

    public void setDeviceToDeviceStatusSharingContacts(int subscriptionId, List<Uri> contacts)
    {
    }

    public List<Uri> getDeviceToDeviceStatusSharingContacts(int subscriptionId)
    {
        return new ArrayList<>();
    }

    public void setSubscriptionOverrideUnmetered(int subId, boolean overrideUnmetered, long timeoutMillis)
    {
    }

    public void setSubscriptionOverrideUnmetered(int subId, boolean overrideUnmetered, int[] networkTypes, long timeoutMillis)
    {
    }

    public void setSubscriptionOverrideCongested(int subId, boolean overrideCongested, long timeoutMillis)
    {
    }

    public void setSubscriptionOverrideCongested(int subId, boolean overrideCongested, int[] networkTypes, long timeoutMillis)
    {
    }

    public void switchToSubscription(int subId, PendingIntent callbackIntent)
    {
    }

    public static int getSlotIndex(int subscriptionId)
    {
        return INVALID_SIM_SLOT_INDEX;
    }

    public static int getSubscriptionId(int slotIndex)
    {
        return INVALID_SUBSCRIPTION_ID;
    }

    public static int getDefaultSubscriptionId()
    {
        return INVALID_SUBSCRIPTION_ID;
    }

    public static int getDefaultVoiceSubscriptionId()
    {
        return INVALID_SUBSCRIPTION_ID;
    }

    public static int getDefaultSmsSubscriptionId()
    {
        return INVALID_SUBSCRIPTION_ID;
    }

    public static int getDefaultDataSubscriptionId()
    {
        return INVALID_SUBSCRIPTION_ID;
    }

    public static int getActiveDataSubscriptionId()
    {
        return INVALID_SUBSCRIPTION_ID;
    }

    public static boolean isValidSubscriptionId(int subscriptionId)
    {
        return subscriptionId > INVALID_SUBSCRIPTION_ID;
    }

    public static boolean isUsableSubscriptionId(int subscriptionId)
    {
        return subscriptionId >= 0 && subscriptionId != DEFAULT_SUBSCRIPTION_ID;
    }

    public static class OnSubscriptionsChangedListener
    {
        public OnSubscriptionsChangedListener()
        {
        }

        public void onSubscriptionsChanged()
        {
        }
    }

    public static class OnOpportunisticSubscriptionsChangedListener
    {
        public OnOpportunisticSubscriptionsChangedListener()
        {
        }

        public void onOpportunisticSubscriptionsChanged()
        {
        }
    }
}
