package android.telephony;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.OutcomeReceiver;
import android.os.ParcelUuid;
import android.os.PersistableBundle;
import android.telecom.PhoneAccountHandle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class TelephonyManager
{
    public static final String ACTION_CARRIER_MESSAGING_CLIENT_SERVICE = "android.telephony.action.CARRIER_MESSAGING_CLIENT_SERVICE";
    public static final String ACTION_CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE =
        "android.telephony.action.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE";
    public static final String ACTION_CARRIER_SIGNAL_PCO_VALUE = "android.telephony.action.CARRIER_SIGNAL_PCO_VALUE";
    public static final String ACTION_CARRIER_SIGNAL_REDIRECTED = "android.telephony.action.CARRIER_SIGNAL_REDIRECTED";
    public static final String ACTION_CARRIER_SIGNAL_REQUEST_NETWORK_FAILED = "android.telephony.action.CARRIER_SIGNAL_REQUEST_NETWORK_FAILED";
    public static final String ACTION_CARRIER_SIGNAL_RESET = "android.telephony.action.CARRIER_SIGNAL_RESET";
    public static final String ACTION_CONFIGURE_VOICEMAIL = "android.telephony.action.CONFIGURE_VOICEMAIL";
    public static final String ACTION_MULTI_SIM_CONFIG_CHANGED = "android.telephony.action.MULTI_SIM_CONFIG_CHANGED";
    public static final String ACTION_NETWORK_COUNTRY_CHANGED = "android.telephony.action.NETWORK_COUNTRY_CHANGED";
    public static final String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
    public static final String ACTION_RESET_MOBILE_NETWORK_SETTINGS = "android.telephony.action.RESET_MOBILE_NETWORK_SETTINGS";
    public static final String ACTION_RESPOND_VIA_MESSAGE = "android.intent.action.RESPOND_VIA_MESSAGE";
    public static final String ACTION_SECRET_CODE = "android.telephony.action.SECRET_CODE";
    public static final String ACTION_SHOW_VOICEMAIL_NOTIFICATION = "android.telephony.action.SHOW_VOICEMAIL_NOTIFICATION";
    public static final String ACTION_SUBSCRIPTION_CARRIER_IDENTITY_CHANGED = "android.telephony.action.SUBSCRIPTION_CARRIER_IDENTITY_CHANGED";
    public static final String ACTION_SUBSCRIPTION_SPECIFIC_CARRIER_IDENTITY_CHANGED =
        "android.telephony.action.SUBSCRIPTION_SPECIFIC_CARRIER_IDENTITY_CHANGED";

    public static final int ALLOWED_NETWORK_TYPES_REASON_USER = 0;
    public static final int ALLOWED_NETWORK_TYPES_REASON_CARRIER = 2;

    public static final int APPTYPE_UNKNOWN = 0;
    public static final int APPTYPE_SIM = 1;
    public static final int APPTYPE_USIM = 2;
    public static final int APPTYPE_RUIM = 3;
    public static final int APPTYPE_CSIM = 4;
    public static final int APPTYPE_ISIM = 5;

    public static final int AUTHTYPE_EAP_SIM = 128;
    public static final int AUTHTYPE_EAP_AKA = 129;
    public static final int AUTHTYPE_GBA_BOOTSTRAP = 132;
    public static final int AUTHTYPE_GBA_NAF_KEY_EXTERNAL = 133;

    public static final int CALL_COMPOSER_STATUS_OFF = 0;
    public static final int CALL_COMPOSER_STATUS_ON = 1;
    public static final int CALL_COMPOSER_STATUS_BUSINESS_ONLY = 2;

    public static final int CALL_STATE_IDLE = 0;
    public static final int CALL_STATE_RINGING = 1;
    public static final int CALL_STATE_OFFHOOK = 2;

    public static final String CAPABILITY_SLICING_CONFIG_SUPPORTED = "CAPABILITY_SLICING_CONFIG_SUPPORTED";

    public static final int CARRIER_RESTRICTION_STATUS_UNKNOWN = 0;
    public static final int CARRIER_RESTRICTION_STATUS_NOT_RESTRICTED = 1;
    public static final int CARRIER_RESTRICTION_STATUS_RESTRICTED = 2;
    public static final int CARRIER_RESTRICTION_STATUS_RESTRICTED_TO_CALLER = 3;

    public static final int CDMA_ROAMING_MODE_RADIO_DEFAULT = -1;
    public static final int CDMA_ROAMING_MODE_HOME = 0;
    public static final int CDMA_ROAMING_MODE_AFFILIATED = 1;
    public static final int CDMA_ROAMING_MODE_ANY = 2;

    public static final int DATA_ACTIVITY_NONE = 0;
    public static final int DATA_ACTIVITY_IN = 1;
    public static final int DATA_ACTIVITY_OUT = 2;
    public static final int DATA_ACTIVITY_INOUT = 3;
    public static final int DATA_ACTIVITY_DORMANT = 4;

    public static final int DATA_UNKNOWN = -1;
    public static final int DATA_DISCONNECTED = 0;
    public static final int DATA_CONNECTING = 1;
    public static final int DATA_CONNECTED = 2;
    public static final int DATA_SUSPENDED = 3;
    public static final int DATA_DISCONNECTING = 4;
    public static final int DATA_HANDOVER_IN_PROGRESS = 5;

    public static final int DATA_ENABLED_REASON_UNKNOWN = -1;
    public static final int DATA_ENABLED_REASON_USER = 0;
    public static final int DATA_ENABLED_REASON_POLICY = 1;
    public static final int DATA_ENABLED_REASON_CARRIER = 2;
    public static final int DATA_ENABLED_REASON_THERMAL = 3;
    public static final int DATA_ENABLED_REASON_OVERRIDE = 4;

    public static final int DEFAULT_PORT_INDEX = 0;

    public static final int ERI_ON = 0;
    public static final int ERI_OFF = 1;
    public static final int ERI_FLASH = 2;

    public static final String EVENT_DISPLAY_EMERGENCY_MESSAGE = "android.telephony.event.DISPLAY_EMERGENCY_MESSAGE";

    public static final String EXTRA_ACTIVE_SIM_SUPPORTED_COUNT = "android.telephony.extra.ACTIVE_SIM_SUPPORTED_COUNT";
    public static final String EXTRA_APN_PROTOCOL = "android.telephony.extra.APN_PROTOCOL";
    public static final String EXTRA_APN_TYPE = "android.telephony.extra.APN_TYPE";
    public static final String EXTRA_CALL_VOICEMAIL_INTENT = "android.telephony.extra.CALL_VOICEMAIL_INTENT";
    public static final String EXTRA_CARRIER_ID = "android.telephony.extra.CARRIER_ID";
    public static final String EXTRA_CARRIER_NAME = "android.telephony.extra.CARRIER_NAME";
    public static final String EXTRA_DATA_FAIL_CAUSE = "android.telephony.extra.DATA_FAIL_CAUSE";
    public static final String EXTRA_DEFAULT_NETWORK_AVAILABLE = "android.telephony.extra.DEFAULT_NETWORK_AVAILABLE";
    public static final String EXTRA_EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE =
        "android.telephony.extra.EMERGENCY_CALL_TO_SATELLITE_HANDOVER_TYPE";
    public static final String EXTRA_EMERGENCY_CALL_TO_SATELLITE_LAUNCH_INTENT =
        "android.telephony.extra.EMERGENCY_CALL_TO_SATELLITE_LAUNCH_INTENT";
    public static final String EXTRA_HIDE_PUBLIC_SETTINGS = "android.telephony.extra.HIDE_PUBLIC_SETTINGS";
    public static final String EXTRA_INCOMING_NUMBER = "incoming_number";
    public static final String EXTRA_IS_REFRESH = "android.telephony.extra.IS_REFRESH";
    public static final String EXTRA_LAST_KNOWN_NETWORK_COUNTRY = "android.telephony.extra.LAST_KNOWN_NETWORK_COUNTRY";
    public static final String EXTRA_LAUNCH_VOICEMAIL_SETTINGS_INTENT = "android.telephony.extra.LAUNCH_VOICEMAIL_SETTINGS_INTENT";
    public static final String EXTRA_NETWORK_COUNTRY = "android.telephony.extra.NETWORK_COUNTRY";
    public static final String EXTRA_NOTIFICATION_COUNT = "android.telephony.extra.NOTIFICATION_COUNT";
    public static final String EXTRA_PCO_ID = "android.telephony.extra.PCO_ID";
    public static final String EXTRA_PCO_VALUE = "android.telephony.extra.PCO_VALUE";
    public static final String EXTRA_PHONE_ACCOUNT_HANDLE = "android.telephony.extra.PHONE_ACCOUNT_HANDLE";
    public static final String EXTRA_REDIRECTION_URL = "android.telephony.extra.REDIRECTION_URL";
    public static final String EXTRA_SPECIFIC_CARRIER_ID = "android.telephony.extra.SPECIFIC_CARRIER_ID";
    public static final String EXTRA_SPECIFIC_CARRIER_NAME = "android.telephony.extra.SPECIFIC_CARRIER_NAME";
    public static final String EXTRA_STATE = "state";
    public static final String EXTRA_STATE_IDLE = stateName("IDLE");
    public static final String EXTRA_STATE_RINGING = stateName("RINGING");
    public static final String EXTRA_STATE_OFFHOOK = stateName("OFFHOOK");
    public static final String EXTRA_SUBSCRIPTION_ID = "android.telephony.extra.SUBSCRIPTION_ID";
    public static final String EXTRA_VOICEMAIL_NUMBER = "android.telephony.extra.VOICEMAIL_NUMBER";

    public static final int INCLUDE_LOCATION_DATA_NONE = 0;
    public static final int INCLUDE_LOCATION_DATA_COARSE = 1;
    public static final int INCLUDE_LOCATION_DATA_FINE = 2;

    public static final String METADATA_HIDE_VOICEMAIL_SETTINGS_MENU = "android.telephony.HIDE_VOICEMAIL_SETTINGS_MENU";

    public static final int MULTISIM_ALLOWED = 0;
    public static final int MULTISIM_NOT_SUPPORTED_BY_HARDWARE = 1;
    public static final int MULTISIM_NOT_SUPPORTED_BY_CARRIER = 2;

    public static final int NETWORK_SELECTION_MODE_UNKNOWN = 0;
    public static final int NETWORK_SELECTION_MODE_AUTO = 1;
    public static final int NETWORK_SELECTION_MODE_MANUAL = 2;

    public static final int NETWORK_TYPE_UNKNOWN = 0;
    public static final int NETWORK_TYPE_GPRS = 1;
    public static final int NETWORK_TYPE_EDGE = 2;
    public static final int NETWORK_TYPE_UMTS = 3;
    public static final int NETWORK_TYPE_CDMA = 4;
    public static final int NETWORK_TYPE_EVDO_0 = 5;
    public static final int NETWORK_TYPE_EVDO_A = 6;
    public static final int NETWORK_TYPE_1xRTT = 7;
    public static final int NETWORK_TYPE_HSDPA = 8;
    public static final int NETWORK_TYPE_HSUPA = 9;
    public static final int NETWORK_TYPE_HSPA = 10;
    public static final int NETWORK_TYPE_IDEN = 11;
    public static final int NETWORK_TYPE_EVDO_B = 12;
    public static final int NETWORK_TYPE_LTE = 13;
    public static final int NETWORK_TYPE_EHRPD = 14;
    public static final int NETWORK_TYPE_HSPAP = 15;
    public static final int NETWORK_TYPE_GSM = 16;
    public static final int NETWORK_TYPE_TD_SCDMA = 17;
    public static final int NETWORK_TYPE_IWLAN = 18;
    public static final int NETWORK_TYPE_NR = 20;

    public static final long NETWORK_TYPE_BITMASK_UNKNOWN = 0L;
    public static final long NETWORK_TYPE_BITMASK_GPRS = 1L;
    public static final long NETWORK_TYPE_BITMASK_EDGE = 2L;
    public static final long NETWORK_TYPE_BITMASK_UMTS = 4L;
    public static final long NETWORK_TYPE_BITMASK_CDMA = 8L;
    public static final long NETWORK_TYPE_BITMASK_EVDO_0 = 16L;
    public static final long NETWORK_TYPE_BITMASK_EVDO_A = 32L;
    public static final long NETWORK_TYPE_BITMASK_1xRTT = 64L;
    public static final long NETWORK_TYPE_BITMASK_HSDPA = 128L;
    public static final long NETWORK_TYPE_BITMASK_HSUPA = 256L;
    public static final long NETWORK_TYPE_BITMASK_HSPA = 512L;
    public static final long NETWORK_TYPE_BITMASK_EVDO_B = 2048L;
    public static final long NETWORK_TYPE_BITMASK_LTE = 4096L;
    public static final long NETWORK_TYPE_BITMASK_EHRPD = 8192L;
    public static final long NETWORK_TYPE_BITMASK_HSPAP = 16384L;
    public static final long NETWORK_TYPE_BITMASK_GSM = 32768L;
    public static final long NETWORK_TYPE_BITMASK_TD_SCDMA = 65536L;
    public static final long NETWORK_TYPE_BITMASK_IWLAN = 131072L;
    public static final long NETWORK_TYPE_BITMASK_LTE_CA = 262144L;
    public static final long NETWORK_TYPE_BITMASK_NR = 524288L;

    public static final int PHONE_TYPE_NONE = 0;
    public static final int PHONE_TYPE_GSM = 1;
    public static final int PHONE_TYPE_CDMA = 2;
    public static final int PHONE_TYPE_SIP = 3;

    public static final int PREMIUM_CAPABILITY_PRIORITIZE_LATENCY = 34;

    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_SUCCESS = 1;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_THROTTLED = 2;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_ALREADY_PURCHASED = 3;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_ALREADY_IN_PROGRESS = 4;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_NOT_FOREGROUND = 5;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_USER_CANCELED = 6;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_CARRIER_DISABLED = 7;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_CARRIER_ERROR = 8;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_TIMEOUT = 9;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_FEATURE_NOT_SUPPORTED = 10;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_REQUEST_FAILED = 11;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_NETWORK_NOT_AVAILABLE = 12;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_ENTITLEMENT_CHECK_FAILED = 13;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_NOT_DEFAULT_DATA_SUBSCRIPTION = 14;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_PENDING_NETWORK_SETUP = 15;
    public static final int PURCHASE_PREMIUM_CAPABILITY_RESULT_USER_DISABLED = 16;

    public static final int SET_OPPORTUNISTIC_SUB_SUCCESS = 0;
    public static final int SET_OPPORTUNISTIC_SUB_VALIDATION_FAILED = 1;
    public static final int SET_OPPORTUNISTIC_SUB_INACTIVE_SUBSCRIPTION = 2;
    public static final int SET_OPPORTUNISTIC_SUB_NO_OPPORTUNISTIC_SUB_AVAILABLE = 3;
    public static final int SET_OPPORTUNISTIC_SUB_REMOTE_SERVICE_EXCEPTION = 4;

    public static final int SIM_STATE_UNKNOWN = 0;
    public static final int SIM_STATE_ABSENT = 1;
    public static final int SIM_STATE_PIN_REQUIRED = 2;
    public static final int SIM_STATE_PUK_REQUIRED = 3;
    public static final int SIM_STATE_NETWORK_LOCKED = 4;
    public static final int SIM_STATE_READY = 5;
    public static final int SIM_STATE_NOT_READY = 6;
    public static final int SIM_STATE_PERM_DISABLED = 7;
    public static final int SIM_STATE_CARD_IO_ERROR = 8;
    public static final int SIM_STATE_CARD_RESTRICTED = 9;

    public static final int UNINITIALIZED_CARD_ID = -2;
    public static final int UNKNOWN_CARRIER_ID = -1;
    public static final int UNSUPPORTED_CARD_ID = -1;

    public static final int UPDATE_AVAILABLE_NETWORKS_SUCCESS = 0;
    public static final int UPDATE_AVAILABLE_NETWORKS_UNKNOWN_FAILURE = 1;
    public static final int UPDATE_AVAILABLE_NETWORKS_ABORTED = 2;
    public static final int UPDATE_AVAILABLE_NETWORKS_INVALID_ARGUMENTS = 3;
    public static final int UPDATE_AVAILABLE_NETWORKS_NO_CARRIER_PRIVILEGE = 4;
    public static final int UPDATE_AVAILABLE_NETWORKS_DISABLE_MODEM_FAIL = 5;
    public static final int UPDATE_AVAILABLE_NETWORKS_ENABLE_MODEM_FAIL = 6;
    public static final int UPDATE_AVAILABLE_NETWORKS_MULTIPLE_NETWORKS_NOT_SUPPORTED = 7;
    public static final int UPDATE_AVAILABLE_NETWORKS_NO_OPPORTUNISTIC_SUB_AVAILABLE = 8;
    public static final int UPDATE_AVAILABLE_NETWORKS_REMOTE_SERVICE_EXCEPTION = 9;
    public static final int UPDATE_AVAILABLE_NETWORKS_SERVICE_IS_DISABLED = 10;

    public static final int USSD_RETURN_FAILURE = -1;
    public static final int USSD_ERROR_SERVICE_UNAVAIL = -2;

    public static final String VVM_TYPE_OMTP = "vvm_type_omtp";
    public static final String VVM_TYPE_CVVM = "vvm_type_cvvm";

    private static final Map<TelephonyCallback, Executor> sCallbacks = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<PhoneStateListener, Integer> sListeners = Collections.synchronizedMap(new LinkedHashMap<>());

    private final Context mContext;
    private final int mSubscriptionId;

    public TelephonyManager()
    {
        this(null, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    public TelephonyManager(Context context)
    {
        this(context, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    public TelephonyManager(Context context, int subscriptionId)
    {
        mContext = context;
        mSubscriptionId = subscriptionId;
    }

    private static String stateName(String name)
    {
        return name;
    }

    public TelephonyManager createForSubscriptionId(int subId)
    {
        return new TelephonyManager(mContext, subId);
    }

    public TelephonyManager createForPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle)
    {
        return null;
    }

    public int getSubscriptionId()
    {
        return mSubscriptionId;
    }

    public int getSubscriptionId(PhoneAccountHandle phoneAccountHandle)
    {
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    public PhoneAccountHandle getPhoneAccountHandle()
    {
        return null;
    }

    public String getNetworkSpecifier()
    {
        return String.valueOf(mSubscriptionId);
    }

    public PersistableBundle getCarrierConfig()
    {
        return new PersistableBundle();
    }

    public int getPhoneType()
    {
        return PHONE_TYPE_NONE;
    }

    @Deprecated
    public int getPhoneCount()
    {
        return getActiveModemCount();
    }

    public int getActiveModemCount()
    {
        return 0;
    }

    public int getSupportedModemCount()
    {
        return 0;
    }

    public int isMultiSimSupported()
    {
        return MULTISIM_NOT_SUPPORTED_BY_HARDWARE;
    }

    public void switchMultiSimConfig(int numOfSims)
    {
    }

    public boolean doesSwitchMultiSimConfigTriggerReboot()
    {
        return false;
    }

    public boolean isModemEnabledForSlot(int slotIndex)
    {
        return false;
    }

    public void rebootModem()
    {
    }

    @Deprecated
    public String getDeviceId()
    {
        return null;
    }

    @Deprecated
    public String getDeviceId(int slotIndex)
    {
        return null;
    }

    public String getImei()
    {
        return null;
    }

    public String getImei(int slotIndex)
    {
        return null;
    }

    public String getPrimaryImei()
    {
        return null;
    }

    public String getMeid()
    {
        return null;
    }

    public String getMeid(int slotIndex)
    {
        return null;
    }

    public String getTypeAllocationCode()
    {
        return null;
    }

    public String getTypeAllocationCode(int slotIndex)
    {
        return null;
    }

    public String getManufacturerCode()
    {
        return null;
    }

    public String getManufacturerCode(int slotIndex)
    {
        return null;
    }

    public String getDeviceSoftwareVersion()
    {
        return null;
    }

    public String getNai()
    {
        return null;
    }

    @Deprecated
    public CellLocation getCellLocation()
    {
        return null;
    }

    public List<CellInfo> getAllCellInfo()
    {
        return new ArrayList<>();
    }

    public void requestCellInfoUpdate(Executor executor, CellInfoCallback callback)
    {
        if (executor == null || callback == null) return;
        executor.execute(() -> callback.onCellInfo(new ArrayList<>()));
    }

    public String getNetworkOperatorName()
    {
        return "";
    }

    public String getNetworkOperator()
    {
        return "";
    }

    public String getNetworkCountryIso()
    {
        return "";
    }

    public String getNetworkCountryIso(int slotIndex)
    {
        return "";
    }

    public boolean isNetworkRoaming()
    {
        return false;
    }

    public int getNetworkSelectionMode()
    {
        return NETWORK_SELECTION_MODE_UNKNOWN;
    }

    public String getManualNetworkSelectionPlmn()
    {
        return "";
    }

    public boolean isManualNetworkSelectionAllowed()
    {
        return false;
    }

    public void setNetworkSelectionModeAutomatic()
    {
    }

    public boolean setNetworkSelectionModeManual(String operatorNumeric, boolean persistSelection)
    {
        return false;
    }

    public boolean setNetworkSelectionModeManual(String operatorNumeric, boolean persistSelection, int ran)
    {
        return false;
    }

    public String[] getForbiddenPlmns()
    {
        return null;
    }

    public int setForbiddenPlmns(List<String> fplmns)
    {
        return -1;
    }

    public List<String> getEquivalentHomePlmns()
    {
        return new ArrayList<>();
    }

    @Deprecated
    public int getNetworkType()
    {
        return NETWORK_TYPE_UNKNOWN;
    }

    public int getDataNetworkType()
    {
        return NETWORK_TYPE_UNKNOWN;
    }

    public int getVoiceNetworkType()
    {
        return NETWORK_TYPE_UNKNOWN;
    }

    public long getSupportedRadioAccessFamily()
    {
        return NETWORK_TYPE_BITMASK_UNKNOWN;
    }

    public long getAllowedNetworkTypesForReason(int reason)
    {
        return NETWORK_TYPE_BITMASK_UNKNOWN;
    }

    public void setAllowedNetworkTypesForReason(int reason, long allowedNetworkTypes)
    {
    }

    public boolean setPreferredNetworkTypeToGlobal()
    {
        return false;
    }

    public boolean isWorldPhone()
    {
        return false;
    }

    public boolean hasIccCard()
    {
        return false;
    }

    public int getSimState()
    {
        return SIM_STATE_ABSENT;
    }

    public int getSimState(int slotIndex)
    {
        return SIM_STATE_ABSENT;
    }

    public String getSimOperator()
    {
        return "";
    }

    public String getSimOperatorName()
    {
        return "";
    }

    public String getSimCountryIso()
    {
        return "";
    }

    public String getSimSerialNumber()
    {
        return null;
    }

    public int getSimCarrierId()
    {
        return UNKNOWN_CARRIER_ID;
    }

    public CharSequence getSimCarrierIdName()
    {
        return null;
    }

    public int getSimSpecificCarrierId()
    {
        return UNKNOWN_CARRIER_ID;
    }

    public CharSequence getSimSpecificCarrierIdName()
    {
        return null;
    }

    public int getCarrierIdFromSimMccMnc()
    {
        return UNKNOWN_CARRIER_ID;
    }

    public int getCardIdForDefaultEuicc()
    {
        return UNSUPPORTED_CARD_ID;
    }

    public String getSubscriberId()
    {
        return null;
    }

    public String getGroupIdLevel1()
    {
        return null;
    }

    public String getLine1Number()
    {
        return null;
    }

    public boolean setLine1NumberForDisplay(String alphaTag, String number)
    {
        return false;
    }

    public String getVoiceMailNumber()
    {
        return null;
    }

    public String getVoiceMailAlphaTag()
    {
        return null;
    }

    public boolean setVoiceMailNumber(String alphaTag, String number)
    {
        return false;
    }

    public String getVisualVoicemailPackageName()
    {
        return null;
    }

    public void sendVisualVoicemailSms(String number, int port, String text, PendingIntent sentIntent)
    {
    }

    public Uri getVoicemailRingtoneUri(PhoneAccountHandle accountHandle)
    {
        return null;
    }

    public void setVoicemailRingtoneUri(PhoneAccountHandle phoneAccountHandle, Uri uri)
    {
    }

    public boolean isVoicemailVibrationEnabled(PhoneAccountHandle accountHandle)
    {
        return false;
    }

    public void setVoicemailVibrationEnabled(PhoneAccountHandle phoneAccountHandle, boolean enabled)
    {
    }

    public String getMmsUserAgent()
    {
        return null;
    }

    public String getMmsUAProfUrl()
    {
        return null;
    }

    public String getIccAuthentication(int appType, int authType, String data)
    {
        return null;
    }

    public boolean iccCloseLogicalChannel(int channel)
    {
        return false;
    }

    public String iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2, int p3, String data)
    {
        return "";
    }

    public String iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3, String data)
    {
        return "";
    }

    public byte[] iccExchangeSimIO(int fileId, int command, int p1, int p2, int p3, String filePath)
    {
        return null;
    }

    public String sendEnvelopeWithStatus(String content)
    {
        return "";
    }

    public boolean hasCarrierPrivileges()
    {
        return false;
    }

    public boolean setOperatorBrandOverride(String brand)
    {
        return false;
    }

    @Deprecated
    public int getCallState()
    {
        return CALL_STATE_IDLE;
    }

    public int getCallStateForSubscription()
    {
        return CALL_STATE_IDLE;
    }

    public int getDataActivity()
    {
        return DATA_ACTIVITY_NONE;
    }

    public int getDataState()
    {
        return DATA_DISCONNECTED;
    }

    public boolean isDataEnabled()
    {
        return false;
    }

    public boolean isDataEnabledForReason(int reason)
    {
        return false;
    }

    public void setDataEnabledForReason(int reason, boolean enabled)
    {
    }

    @Deprecated
    public void setDataEnabled(boolean enable)
    {
    }

    public boolean isDataRoamingEnabled()
    {
        return false;
    }

    public boolean isDataConnectionAllowed()
    {
        return false;
    }

    public boolean isDataCapable()
    {
        return false;
    }

    public boolean isConcurrentVoiceAndDataSupported()
    {
        return false;
    }

    public boolean isVoiceCapable()
    {
        return false;
    }

    public boolean isDeviceVoiceCapable()
    {
        return false;
    }

    public boolean isSmsCapable()
    {
        return false;
    }

    public boolean isDeviceSmsCapable()
    {
        return false;
    }

    public boolean isTtyModeSupported()
    {
        return false;
    }

    public boolean isRttSupported()
    {
        return false;
    }

    public boolean isHearingAidCompatibilitySupported()
    {
        return false;
    }

    public boolean canChangeDtmfToneLength()
    {
        return false;
    }

    public boolean isRadioInterfaceCapabilitySupported(String capability)
    {
        return false;
    }

    public boolean isEmergencyNumber(String number)
    {
        return PhoneNumberUtils.isEmergencyNumber(number);
    }

    public ServiceState getServiceState()
    {
        ServiceState state = new ServiceState();
        state.setStateOff();
        return state;
    }

    public ServiceState getServiceState(int includeLocationData)
    {
        return getServiceState();
    }

    public SignalStrength getSignalStrength()
    {
        return new SignalStrength();
    }

    public int getCallComposerStatus()
    {
        return CALL_COMPOSER_STATUS_OFF;
    }

    public void setCallComposerStatus(int status)
    {
    }

    public static long getMaximumCallComposerPictureSize()
    {
        return 80000L;
    }

    public void uploadCallComposerPicture(Path pictureToUpload, String contentType, Executor executor,
        OutcomeReceiver<ParcelUuid, CallComposerException> callback)
    {
        failCallComposerUpload(executor, callback);
    }

    public void uploadCallComposerPicture(InputStream pictureToUpload, String contentType, Executor executor,
        OutcomeReceiver<ParcelUuid, CallComposerException> callback)
    {
        failCallComposerUpload(executor, callback);
    }

    private static void failCallComposerUpload(Executor executor, OutcomeReceiver<ParcelUuid, CallComposerException> callback)
    {
        if (executor == null || callback == null) return;
        executor.execute(() -> callback.onError(new CallComposerException(CallComposerException.ERROR_NETWORK_UNAVAILABLE, null)));
    }

    public void getCarrierRestrictionStatus(Executor executor, Consumer<Integer> resultListener)
    {
        if (executor == null || resultListener == null) return;
        executor.execute(() -> resultListener.accept(CARRIER_RESTRICTION_STATUS_UNKNOWN));
    }

    public boolean isPremiumCapabilityAvailableForPurchase(int capability)
    {
        return false;
    }

    public void purchasePremiumCapability(int capability, Executor executor, Consumer<Integer> callback)
    {
        if (executor == null || callback == null) return;
        executor.execute(() -> callback.accept(PURCHASE_PREMIUM_CAPABILITY_RESULT_FEATURE_NOT_SUPPORTED));
    }

    public int getPreferredOpportunisticDataSubscription()
    {
        return SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
    }

    public void setPreferredOpportunisticDataSubscription(int subId, boolean needValidation, Executor executor, Consumer<Integer> callback)
    {
        if (executor == null || callback == null) return;
        executor.execute(() -> callback.accept(SET_OPPORTUNISTIC_SUB_REMOTE_SERVICE_EXCEPTION));
    }

    public void sendDialerSpecialCode(String inputCode)
    {
    }

    public void sendUssdRequest(String ussdRequest, UssdResponseCallback callback, Handler handler)
    {
        if (callback == null) return;
        Handler target = handler != null ? handler : new Handler(Looper.getMainLooper());
        target.post(() -> callback.onReceiveUssdResponseFailed(this, ussdRequest, USSD_ERROR_SERVICE_UNAVAIL));
    }

    @Deprecated
    public void listen(PhoneStateListener listener, int events)
    {
        if (listener == null) return;
        if (events == PhoneStateListener.LISTEN_NONE)
        {
            sListeners.remove(listener);
            return;
        }
        sListeners.put(listener, events);
        listener.mExecutor.execute(() -> dispatchInitialState(listener, events));
    }

    private void dispatchInitialState(PhoneStateListener listener, int events)
    {
        if ((events & PhoneStateListener.LISTEN_SERVICE_STATE) != 0) listener.onServiceStateChanged(getServiceState());
        if ((events & PhoneStateListener.LISTEN_SIGNAL_STRENGTH) != 0) listener.onSignalStrengthChanged(99);
        if ((events & PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR) != 0) listener.onMessageWaitingIndicatorChanged(false);
        if ((events & PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR) != 0) listener.onCallForwardingIndicatorChanged(false);
        if ((events & PhoneStateListener.LISTEN_CALL_STATE) != 0) listener.onCallStateChanged(CALL_STATE_IDLE, "");
        if ((events & PhoneStateListener.LISTEN_DATA_CONNECTION_STATE) != 0)
        {
            listener.onDataConnectionStateChanged(DATA_DISCONNECTED, NETWORK_TYPE_UNKNOWN);
            listener.onDataConnectionStateChanged(DATA_DISCONNECTED);
        }
        if ((events & PhoneStateListener.LISTEN_DATA_ACTIVITY) != 0) listener.onDataActivity(DATA_ACTIVITY_NONE);
        if ((events & PhoneStateListener.LISTEN_SIGNAL_STRENGTHS) != 0) listener.onSignalStrengthsChanged(getSignalStrength());
        if ((events & PhoneStateListener.LISTEN_CELL_INFO) != 0) listener.onCellInfoChanged(getAllCellInfo());
        if ((events & PhoneStateListener.LISTEN_USER_MOBILE_DATA_STATE) != 0) listener.onUserMobileDataStateChanged(false);
        if ((events & PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED) != 0) listener.onDisplayInfoChanged(defaultDisplayInfo());
        if ((events & PhoneStateListener.LISTEN_ACTIVE_DATA_SUBSCRIPTION_ID_CHANGE) != 0)
        {
            listener.onActiveDataSubscriptionIdChanged(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
    }

    public void registerTelephonyCallback(Executor executor, TelephonyCallback callback)
    {
        registerTelephonyCallback(INCLUDE_LOCATION_DATA_FINE, executor, callback);
    }

    public void registerTelephonyCallback(int includeLocationData, Executor executor, TelephonyCallback callback)
    {
        if (executor == null) throw new IllegalStateException("telephony service is null.");
        if (callback == null) throw new IllegalStateException("callback is null.");
        sCallbacks.put(callback, executor);
        executor.execute(() -> dispatchInitialState(callback));
    }

    public void unregisterTelephonyCallback(TelephonyCallback callback)
    {
        if (callback != null) sCallbacks.remove(callback);
    }

    private void dispatchInitialState(TelephonyCallback callback)
    {
        if (!sCallbacks.containsKey(callback)) return;
        if (callback instanceof TelephonyCallback.ServiceStateListener)
        {
            ((TelephonyCallback.ServiceStateListener)callback).onServiceStateChanged(getServiceState());
        }
        if (callback instanceof TelephonyCallback.MessageWaitingIndicatorListener)
        {
            ((TelephonyCallback.MessageWaitingIndicatorListener)callback).onMessageWaitingIndicatorChanged(false);
        }
        if (callback instanceof TelephonyCallback.CallForwardingIndicatorListener)
        {
            ((TelephonyCallback.CallForwardingIndicatorListener)callback).onCallForwardingIndicatorChanged(false);
        }
        if (callback instanceof TelephonyCallback.CallStateListener)
        {
            ((TelephonyCallback.CallStateListener)callback).onCallStateChanged(CALL_STATE_IDLE);
        }
        if (callback instanceof TelephonyCallback.DataConnectionStateListener)
        {
            ((TelephonyCallback.DataConnectionStateListener)callback).onDataConnectionStateChanged(DATA_DISCONNECTED, NETWORK_TYPE_UNKNOWN);
        }
        if (callback instanceof TelephonyCallback.DataActivityListener)
        {
            ((TelephonyCallback.DataActivityListener)callback).onDataActivity(DATA_ACTIVITY_NONE);
        }
        if (callback instanceof TelephonyCallback.SignalStrengthsListener)
        {
            ((TelephonyCallback.SignalStrengthsListener)callback).onSignalStrengthsChanged(getSignalStrength());
        }
        if (callback instanceof TelephonyCallback.CellInfoListener)
        {
            ((TelephonyCallback.CellInfoListener)callback).onCellInfoChanged(getAllCellInfo());
        }
        if (callback instanceof TelephonyCallback.UserMobileDataStateListener)
        {
            ((TelephonyCallback.UserMobileDataStateListener)callback).onUserMobileDataStateChanged(false);
        }
        if (callback instanceof TelephonyCallback.DisplayInfoListener)
        {
            ((TelephonyCallback.DisplayInfoListener)callback).onDisplayInfoChanged(defaultDisplayInfo());
        }
        if (callback instanceof TelephonyCallback.ActiveDataSubscriptionIdListener)
        {
            ((TelephonyCallback.ActiveDataSubscriptionIdListener)callback).onActiveDataSubscriptionIdChanged(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
        if (callback instanceof TelephonyCallback.CarrierNetworkListener)
        {
            ((TelephonyCallback.CarrierNetworkListener)callback).onCarrierNetworkChange(false);
        }
    }

    private static TelephonyDisplayInfo defaultDisplayInfo()
    {
        return new TelephonyDisplayInfo(NETWORK_TYPE_UNKNOWN, TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE, false);
    }

    public abstract static class CellInfoCallback
    {
        public static final int ERROR_TIMEOUT = 1;
        public static final int ERROR_MODEM_ERROR = 2;

        public CellInfoCallback()
        {
        }

        public abstract void onCellInfo(List<CellInfo> cellInfo);

        public void onError(int errorCode, Throwable detail)
        {
            onCellInfo(new ArrayList<>());
        }
    }

    public abstract static class UssdResponseCallback
    {
        public UssdResponseCallback()
        {
        }

        public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response)
        {
        }

        public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode)
        {
        }
    }

    public static class CallComposerException extends Exception
    {
        public static final int ERROR_UNKNOWN = 0;
        public static final int ERROR_REMOTE_END_CLOSED = 1;
        public static final int ERROR_FILE_TOO_LARGE = 2;
        public static final int ERROR_AUTHENTICATION_FAILED = 3;
        public static final int ERROR_INPUT_CLOSED = 4;
        public static final int ERROR_IO_EXCEPTION = 5;
        public static final int ERROR_NETWORK_UNAVAILABLE = 6;

        private final int mErrorCode;
        private final IOException mIOException;

        public CallComposerException(int errorCode, IOException ioException)
        {
            mErrorCode = errorCode;
            mIOException = ioException;
        }

        public int getErrorCode()
        {
            return mErrorCode;
        }

        public IOException getIOException()
        {
            return mIOException;
        }
    }
}
