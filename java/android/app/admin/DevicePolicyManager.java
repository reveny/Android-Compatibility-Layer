package android.app.admin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DevicePolicyManager
{
    public static final String ACTION_ADD_DEVICE_ADMIN = "android.app.action.ADD_DEVICE_ADMIN";
    public static final String ACTION_ADMIN_POLICY_COMPLIANCE = "android.app.action.ADMIN_POLICY_COMPLIANCE";
    public static final String ACTION_APPLICATION_DELEGATION_SCOPES_CHANGED = "android.app.action.APPLICATION_DELEGATION_SCOPES_CHANGED";
    public static final String ACTION_CHECK_POLICY_COMPLIANCE = "android.app.action.CHECK_POLICY_COMPLIANCE";
    public static final String ACTION_DEVICE_ADMIN_SERVICE = "android.app.action.DEVICE_ADMIN_SERVICE";
    public static final String ACTION_DEVICE_FINANCING_STATE_CHANGED = "android.app.admin.action.DEVICE_FINANCING_STATE_CHANGED";
    public static final String ACTION_DEVICE_OWNER_CHANGED = "android.app.action.DEVICE_OWNER_CHANGED";
    public static final String ACTION_DEVICE_POLICY_RESOURCE_UPDATED = "android.app.action.DEVICE_POLICY_RESOURCE_UPDATED";
    public static final String ACTION_GET_PROVISIONING_MODE = "android.app.action.GET_PROVISIONING_MODE";
    public static final String ACTION_MANAGED_PROFILE_PROVISIONED = "android.app.action.MANAGED_PROFILE_PROVISIONED";
    public static final String ACTION_PROFILE_OWNER_CHANGED = "android.app.action.PROFILE_OWNER_CHANGED";
    public static final String ACTION_PROVISIONING_SUCCESSFUL = "android.app.action.PROVISIONING_SUCCESSFUL";
    public static final String ACTION_PROVISION_MANAGED_DEVICE = "android.app.action.PROVISION_MANAGED_DEVICE";
    public static final String ACTION_PROVISION_MANAGED_PROFILE = "android.app.action.PROVISION_MANAGED_PROFILE";
    public static final String ACTION_SET_NEW_PARENT_PROFILE_PASSWORD = "android.app.action.SET_NEW_PARENT_PROFILE_PASSWORD";
    public static final String ACTION_SET_NEW_PASSWORD = "android.app.action.SET_NEW_PASSWORD";
    public static final String ACTION_START_ENCRYPTION = "android.app.action.START_ENCRYPTION";
    public static final String ACTION_SYSTEM_UPDATE_POLICY_CHANGED = "android.app.action.SYSTEM_UPDATE_POLICY_CHANGED";
    public static final String DELEGATION_APP_RESTRICTIONS = "delegation-app-restrictions";
    public static final String DELEGATION_BLOCK_UNINSTALL = "delegation-block-uninstall";
    public static final String DELEGATION_CERT_INSTALL = "delegation-cert-install";
    public static final String DELEGATION_CERT_SELECTION = "delegation-cert-selection";
    public static final String DELEGATION_ENABLE_SYSTEM_APP = "delegation-enable-system-app";
    public static final String DELEGATION_INSTALL_EXISTING_PACKAGE = "delegation-install-existing-package";
    public static final String DELEGATION_KEEP_UNINSTALLED_PACKAGES = "delegation-keep-uninstalled-packages";
    public static final String DELEGATION_NETWORK_LOGGING = "delegation-network-logging";
    public static final String DELEGATION_PACKAGE_ACCESS = "delegation-package-access";
    public static final String DELEGATION_PERMISSION_GRANT = "delegation-permission-grant";
    public static final String DELEGATION_SECURITY_LOGGING = "delegation-security-logging";
    public static final String EXTRA_ADD_EXPLANATION = "android.app.extra.ADD_EXPLANATION";
    public static final String EXTRA_DELEGATION_SCOPES = "android.app.extra.DELEGATION_SCOPES";
    public static final String EXTRA_DEVICE_ADMIN = "android.app.extra.DEVICE_ADMIN";
    public static final String EXTRA_DEVICE_PASSWORD_REQUIREMENT_ONLY = "android.app.extra.DEVICE_PASSWORD_REQUIREMENT_ONLY";
    public static final String EXTRA_PASSWORD_COMPLEXITY = "android.app.extra.PASSWORD_COMPLEXITY";
    public static final String EXTRA_PROVISIONING_ACCOUNT_TO_MIGRATE = "android.app.extra.PROVISIONING_ACCOUNT_TO_MIGRATE";
    public static final String EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE = "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE";
    public static final String EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES =
            "android.app.extra.PROVISIONING_ALLOWED_PROVISIONING_MODES";
    public static final String EXTRA_PROVISIONING_ALLOW_OFFLINE = "android.app.extra.PROVISIONING_ALLOW_OFFLINE";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_MINIMUM_VERSION_CODE";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_COOKIE_HEADER";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM =
            "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM";
    public static final String EXTRA_PROVISIONING_DISCLAIMERS = "android.app.extra.PROVISIONING_DISCLAIMERS";
    public static final String EXTRA_PROVISIONING_DISCLAIMER_CONTENT = "android.app.extra.PROVISIONING_DISCLAIMER_CONTENT";
    public static final String EXTRA_PROVISIONING_DISCLAIMER_HEADER = "android.app.extra.PROVISIONING_DISCLAIMER_HEADER";
    public static final String EXTRA_PROVISIONING_EMAIL_ADDRESS = "android.app.extra.PROVISIONING_EMAIL_ADDRESS";
    public static final String EXTRA_PROVISIONING_IMEI = "android.app.extra.PROVISIONING_IMEI";
    public static final String EXTRA_PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION = "android.app.extra.PROVISIONING_KEEP_ACCOUNT_ON_MIGRATION";
    public static final String EXTRA_PROVISIONING_KEEP_SCREEN_ON = "android.app.extra.PROVISIONING_KEEP_SCREEN_ON";
    public static final String EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED =
            "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED";
    public static final String EXTRA_PROVISIONING_LOCALE = "android.app.extra.PROVISIONING_LOCALE";
    public static final String EXTRA_PROVISIONING_LOCAL_TIME = "android.app.extra.PROVISIONING_LOCAL_TIME";
    public static final String EXTRA_PROVISIONING_LOGO_URI = "android.app.extra.PROVISIONING_LOGO_URI";
    public static final String EXTRA_PROVISIONING_MAIN_COLOR = "android.app.extra.PROVISIONING_MAIN_COLOR";
    public static final String EXTRA_PROVISIONING_MODE = "android.app.extra.PROVISIONING_MODE";
    public static final String EXTRA_PROVISIONING_SENSORS_PERMISSION_GRANT_OPT_OUT =
            "android.app.extra.PROVISIONING_SENSORS_PERMISSION_GRANT_OPT_OUT";
    public static final String EXTRA_PROVISIONING_SERIAL_NUMBER = "android.app.extra.PROVISIONING_SERIAL_NUMBER";
    public static final String EXTRA_PROVISIONING_SHOULD_LAUNCH_RESULT_INTENT =
            "android.app.extra.PROVISIONING_SHOULD_LAUNCH_RESULT_INTENT";
    public static final String EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS = "android.app.extra.PROVISIONING_SKIP_EDUCATION_SCREENS";
    public static final String EXTRA_PROVISIONING_SKIP_ENCRYPTION = "android.app.extra.PROVISIONING_SKIP_ENCRYPTION";
    public static final String EXTRA_PROVISIONING_SKIP_USER_CONSENT = "android.app.extra.PROVISIONING_SKIP_USER_CONSENT";
    public static final String EXTRA_PROVISIONING_TIME_ZONE = "android.app.extra.PROVISIONING_TIME_ZONE";
    public static final String EXTRA_PROVISIONING_USE_MOBILE_DATA = "android.app.extra.PROVISIONING_USE_MOBILE_DATA";
    public static final String EXTRA_PROVISIONING_WIFI_ANONYMOUS_IDENTITY = "android.app.extra.PROVISIONING_WIFI_ANONYMOUS_IDENTITY";
    public static final String EXTRA_PROVISIONING_WIFI_CA_CERTIFICATE = "android.app.extra.PROVISIONING_WIFI_CA_CERTIFICATE";
    public static final String EXTRA_PROVISIONING_WIFI_DOMAIN = "android.app.extra.PROVISIONING_WIFI_DOMAIN";
    public static final String EXTRA_PROVISIONING_WIFI_EAP_METHOD = "android.app.extra.PROVISIONING_WIFI_EAP_METHOD";
    public static final String EXTRA_PROVISIONING_WIFI_HIDDEN = "android.app.extra.PROVISIONING_WIFI_HIDDEN";
    public static final String EXTRA_PROVISIONING_WIFI_IDENTITY = "android.app.extra.PROVISIONING_WIFI_IDENTITY";
    public static final String EXTRA_PROVISIONING_WIFI_PAC_URL = "android.app.extra.PROVISIONING_WIFI_PAC_URL";
    public static final String EXTRA_PROVISIONING_WIFI_PASSWORD = "android.app.extra.PROVISIONING_WIFI_PASSWORD";
    public static final String EXTRA_PROVISIONING_WIFI_PHASE2_AUTH = "android.app.extra.PROVISIONING_WIFI_PHASE2_AUTH";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_BYPASS = "android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_HOST = "android.app.extra.PROVISIONING_WIFI_PROXY_HOST";
    public static final String EXTRA_PROVISIONING_WIFI_PROXY_PORT = "android.app.extra.PROVISIONING_WIFI_PROXY_PORT";
    public static final String EXTRA_PROVISIONING_WIFI_SECURITY_TYPE = "android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE";
    public static final String EXTRA_PROVISIONING_WIFI_SSID = "android.app.extra.PROVISIONING_WIFI_SSID";
    public static final String EXTRA_PROVISIONING_WIFI_USER_CERTIFICATE = "android.app.extra.PROVISIONING_WIFI_USER_CERTIFICATE";
    public static final String EXTRA_RESOURCE_IDS = "android.app.extra.RESOURCE_IDS";
    public static final String EXTRA_RESOURCE_TYPE = "android.app.extra.RESOURCE_TYPE";
    public static final String EXTRA_RESULT_LAUNCH_INTENT = "android.app.extra.RESULT_LAUNCH_INTENT";
    public static final String MIME_TYPE_PROVISIONING_NFC = "application/com.android.managedprovisioning";
    public static final String POLICY_DISABLE_CAMERA = "policy_disable_camera";
    public static final String POLICY_DISABLE_SCREEN_CAPTURE = "policy_disable_screen_capture";
    public static final int ENCRYPTION_STATUS_ACTIVATING = 2;
    public static final int ENCRYPTION_STATUS_ACTIVE = 3;
    public static final int ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY = 4;
    public static final int ENCRYPTION_STATUS_ACTIVE_PER_USER = 5;
    public static final int ENCRYPTION_STATUS_INACTIVE = 1;
    public static final int ENCRYPTION_STATUS_UNSUPPORTED = 0;
    public static final int EXTRA_RESOURCE_TYPE_DRAWABLE = 1;
    public static final int EXTRA_RESOURCE_TYPE_STRING = 2;
    public static final int FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY = 1;
    public static final int FLAG_MANAGED_CAN_ACCESS_PARENT = 2;
    public static final int FLAG_PARENT_CAN_ACCESS_MANAGED = 1;
    public static final int ID_TYPE_BASE_INFO = 1;
    public static final int ID_TYPE_IMEI = 4;
    public static final int ID_TYPE_INDIVIDUAL_ATTESTATION = 16;
    public static final int ID_TYPE_MEID = 8;
    public static final int ID_TYPE_SERIAL = 2;
    public static final int INSTALLKEY_REQUEST_CREDENTIALS_ACCESS = 1;
    public static final int INSTALLKEY_SET_USER_SELECTABLE = 2;
    public static final int KEYGUARD_DISABLE_BIOMETRICS = 416;
    public static final int KEYGUARD_DISABLE_FACE = 128;
    public static final int KEYGUARD_DISABLE_FEATURES_ALL = 2147483647;
    public static final int KEYGUARD_DISABLE_FEATURES_NONE = 0;
    public static final int KEYGUARD_DISABLE_FINGERPRINT = 32;
    public static final int KEYGUARD_DISABLE_IRIS = 256;
    public static final int KEYGUARD_DISABLE_REMOTE_INPUT = 64;
    public static final int KEYGUARD_DISABLE_SECURE_CAMERA = 2;
    public static final int KEYGUARD_DISABLE_SECURE_NOTIFICATIONS = 4;
    public static final int KEYGUARD_DISABLE_SHORTCUTS_ALL = 512;
    public static final int KEYGUARD_DISABLE_TRUST_AGENTS = 16;
    public static final int KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS = 8;
    public static final int KEYGUARD_DISABLE_WIDGETS_ALL = 1;
    public static final int LEAVE_ALL_SYSTEM_APPS_ENABLED = 16;
    public static final int LOCK_TASK_FEATURE_BLOCK_ACTIVITY_START_IN_TASK = 64;
    public static final int LOCK_TASK_FEATURE_GLOBAL_ACTIONS = 16;
    public static final int LOCK_TASK_FEATURE_HOME = 4;
    public static final int LOCK_TASK_FEATURE_KEYGUARD = 32;
    public static final int LOCK_TASK_FEATURE_NONE = 0;
    public static final int LOCK_TASK_FEATURE_NOTIFICATIONS = 2;
    public static final int LOCK_TASK_FEATURE_OVERVIEW = 8;
    public static final int LOCK_TASK_FEATURE_SYSTEM_INFO = 1;
    public static final int MAKE_USER_EPHEMERAL = 2;
    public static final int MTE_DISABLED = 2;
    public static final int MTE_ENABLED = 1;
    public static final int MTE_NOT_CONTROLLED_BY_POLICY = 0;
    public static final int NEARBY_STREAMING_DISABLED = 1;
    public static final int NEARBY_STREAMING_ENABLED = 2;
    public static final int NEARBY_STREAMING_NOT_CONTROLLED_BY_POLICY = 0;
    public static final int NEARBY_STREAMING_SAME_MANAGED_ACCOUNT_ONLY = 3;
    public static final int OPERATION_SAFETY_REASON_DRIVING_DISTRACTION = 1;
    public static final int PASSWORD_COMPLEXITY_HIGH = 327680;
    public static final int PASSWORD_COMPLEXITY_LOW = 65536;
    public static final int PASSWORD_COMPLEXITY_MEDIUM = 196608;
    public static final int PASSWORD_COMPLEXITY_NONE = 0;
    public static final int PASSWORD_QUALITY_ALPHABETIC = 262144;
    public static final int PASSWORD_QUALITY_ALPHANUMERIC = 327680;
    public static final int PASSWORD_QUALITY_BIOMETRIC_WEAK = 32768;
    public static final int PASSWORD_QUALITY_COMPLEX = 393216;
    public static final int PASSWORD_QUALITY_NUMERIC = 131072;
    public static final int PASSWORD_QUALITY_NUMERIC_COMPLEX = 196608;
    public static final int PASSWORD_QUALITY_SOMETHING = 65536;
    public static final int PASSWORD_QUALITY_UNSPECIFIED = 0;
    public static final int PERMISSION_GRANT_STATE_DEFAULT = 0;
    public static final int PERMISSION_GRANT_STATE_DENIED = 2;
    public static final int PERMISSION_GRANT_STATE_GRANTED = 1;
    public static final int PERMISSION_POLICY_AUTO_DENY = 2;
    public static final int PERMISSION_POLICY_AUTO_GRANT = 1;
    public static final int PERMISSION_POLICY_PROMPT = 0;
    public static final int PERSONAL_APPS_NOT_SUSPENDED = 0;
    public static final int PERSONAL_APPS_SUSPENDED_EXPLICITLY = 1;
    public static final int PERSONAL_APPS_SUSPENDED_PROFILE_TIMEOUT = 2;
    public static final int PRIVATE_DNS_MODE_OFF = 1;
    public static final int PRIVATE_DNS_MODE_OPPORTUNISTIC = 2;
    public static final int PRIVATE_DNS_MODE_PROVIDER_HOSTNAME = 3;
    public static final int PRIVATE_DNS_MODE_UNKNOWN = 0;
    public static final int PRIVATE_DNS_SET_ERROR_FAILURE_SETTING = 2;
    public static final int PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING = 1;
    public static final int PRIVATE_DNS_SET_NO_ERROR = 0;
    public static final int PROVISIONING_MODE_FULLY_MANAGED_DEVICE = 1;
    public static final int PROVISIONING_MODE_MANAGED_PROFILE = 2;
    public static final int PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE = 3;
    public static final int RESET_PASSWORD_DO_NOT_ASK_CREDENTIALS_ON_BOOT = 2;
    public static final int RESET_PASSWORD_REQUIRE_ENTRY = 1;
    public static final int SKIP_SETUP_WIZARD = 1;
    public static final int WIFI_SECURITY_ENTERPRISE_192 = 3;
    public static final int WIFI_SECURITY_ENTERPRISE_EAP = 2;
    public static final int WIFI_SECURITY_OPEN = 0;
    public static final int WIFI_SECURITY_PERSONAL = 1;
    public static final int WIPE_EUICC = 4;
    public static final int WIPE_EXTERNAL_STORAGE = 1;
    public static final int WIPE_RESET_PROTECTION_DATA = 2;
    public static final int WIPE_SILENTLY = 8;

    private final Context mContext;

    public DevicePolicyManager(Context context)
    {
        mContext = context;
    }

    private static SecurityException notAdmin(ComponentName admin)
    {
        return new SecurityException("No active admin " + admin + "; this device has no device or profile owner");
    }

    private static SecurityException notOwner(String operation)
    {
        return new SecurityException("Caller is not a device or profile owner, so " + operation + " is not allowed");
    }

    public boolean isAdminActive(ComponentName admin)
    {
        return false;
    }

    public List<ComponentName> getActiveAdmins()
    {
        return null;
    }

    public void removeActiveAdmin(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean hasGrantedPolicy(ComponentName admin, int usesPolicy)
    {
        throw notAdmin(admin);
    }

    public boolean isDeviceOwnerApp(String packageName)
    {
        return false;
    }

    public boolean isProfileOwnerApp(String packageName)
    {
        return false;
    }

    public boolean isManagedProfile(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean isOrganizationOwnedDeviceWithManagedProfile()
    {
        return false;
    }

    public boolean isAffiliatedUser()
    {
        return false;
    }

    public boolean isProvisioningAllowed(String action)
    {
        return false;
    }

    public boolean isDeviceFinanced()
    {
        return false;
    }

    public boolean isLogoutEnabled()
    {
        return false;
    }

    public boolean isComplianceAcknowledgementRequired()
    {
        return false;
    }

    public void acknowledgeDevicePolicyCompliance()
    {
    }

    public CharSequence getDeviceOwnerLockScreenInfo()
    {
        return null;
    }

    public Intent createAdminSupportIntent(String restriction)
    {
        return null;
    }

    public int getStorageEncryptionStatus()
    {
        return ENCRYPTION_STATUS_UNSUPPORTED;
    }

    public int setStorageEncryption(ComponentName admin, boolean encrypt)
    {
        throw notAdmin(admin);
    }

    public boolean getStorageEncryption(ComponentName admin)
    {
        return false;
    }

    public boolean getCameraDisabled(ComponentName admin)
    {
        return false;
    }

    public void setCameraDisabled(ComponentName admin, boolean disabled)
    {
        throw notAdmin(admin);
    }

    public boolean getScreenCaptureDisabled(ComponentName admin)
    {
        return false;
    }

    public void setScreenCaptureDisabled(ComponentName admin, boolean disabled)
    {
        throw notAdmin(admin);
    }

    public int getKeyguardDisabledFeatures(ComponentName admin)
    {
        return KEYGUARD_DISABLE_FEATURES_NONE;
    }

    public void setKeyguardDisabledFeatures(ComponentName admin, int which)
    {
        throw notAdmin(admin);
    }

    public int getPasswordQuality(ComponentName admin)
    {
        return PASSWORD_QUALITY_UNSPECIFIED;
    }

    public void setPasswordQuality(ComponentName admin, int quality)
    {
        throw notAdmin(admin);
    }

    public int getPasswordMinimumLength(ComponentName admin)
    {
        return 0;
    }

    public void setPasswordMinimumLength(ComponentName admin, int length)
    {
        throw notAdmin(admin);
    }

    public int getPasswordComplexity()
    {
        return PASSWORD_COMPLEXITY_NONE;
    }

    public int getRequiredPasswordComplexity()
    {
        throw notOwner("getRequiredPasswordComplexity");
    }

    public void setRequiredPasswordComplexity(int passwordComplexity)
    {
        throw notOwner("setRequiredPasswordComplexity");
    }

    public boolean isActivePasswordSufficient()
    {
        throw notOwner("isActivePasswordSufficient");
    }

    public boolean isActivePasswordSufficientForDeviceRequirement()
    {
        throw notOwner("isActivePasswordSufficientForDeviceRequirement");
    }

    public int getCurrentFailedPasswordAttempts()
    {
        throw notOwner("getCurrentFailedPasswordAttempts");
    }

    public int getMaximumFailedPasswordsForWipe(ComponentName admin)
    {
        return 0;
    }

    public void setMaximumFailedPasswordsForWipe(ComponentName admin, int num)
    {
        throw notAdmin(admin);
    }

    public long getMaximumTimeToLock(ComponentName admin)
    {
        return 0L;
    }

    public void setMaximumTimeToLock(ComponentName admin, long timeMs)
    {
        throw notAdmin(admin);
    }

    public long getPasswordExpiration(ComponentName admin)
    {
        return 0L;
    }

    public long getPasswordExpirationTimeout(ComponentName admin)
    {
        return 0L;
    }

    public boolean resetPassword(String password, int flags)
    {
        throw notOwner("resetPassword");
    }

    public void lockNow()
    {
        throw notOwner("lockNow");
    }

    public void lockNow(int flags)
    {
        throw notOwner("lockNow");
    }

    public void wipeData(int flags)
    {
        throw notOwner("wipeData");
    }

    public void wipeData(int flags, CharSequence reason)
    {
        throw notOwner("wipeData");
    }

    public void wipeDevice(int flags)
    {
        throw notOwner("wipeDevice");
    }

    public boolean getAutoTimeRequired()
    {
        return false;
    }

    public boolean getAutoTimeEnabled(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean getAutoTimeZoneEnabled(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean isLockTaskPermitted(String packageName)
    {
        return false;
    }

    public void setLockTaskPackages(ComponentName admin, String[] packages)
    {
        throw notAdmin(admin);
    }

    public String[] getLockTaskPackages(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public int getLockTaskFeatures(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public Bundle getApplicationRestrictions(ComponentName admin, String packageName)
    {
        if (admin == null) throw notOwner("getApplicationRestrictions");
        throw notAdmin(admin);
    }

    public void setApplicationRestrictions(ComponentName admin, String packageName, Bundle settings)
    {
        if (admin == null) throw notOwner("setApplicationRestrictions");
        throw notAdmin(admin);
    }

    public String getApplicationRestrictionsManagingPackage(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean isApplicationHidden(ComponentName admin, String packageName)
    {
        throw notAdmin(admin);
    }

    public boolean isUninstallBlocked(ComponentName admin, String packageName)
    {
        return false;
    }

    public List<String> getDelegatedScopes(ComponentName admin, String delegatePackage)
    {
        if (admin == null && mContext != null && mContext.getPackageName().equals(delegatePackage)) return Collections.emptyList();
        throw notAdmin(admin);
    }

    public Set<String> getCrossProfileCalendarPackages()
    {
        return Collections.emptySet();
    }

    public Set<String> getCrossProfilePackages(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public List<String> getPermittedAccessibilityServices(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public List<String> getPermittedInputMethods(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public int getPermissionGrantState(ComponentName admin, String packageName, String permission)
    {
        throw notAdmin(admin);
    }

    public int getPermissionPolicy(ComponentName admin)
    {
        return PERMISSION_POLICY_PROMPT;
    }

    public Bundle getUserRestrictions(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public void addUserRestriction(ComponentName admin, String key)
    {
        throw notAdmin(admin);
    }

    public void clearUserRestriction(ComponentName admin, String key)
    {
        throw notAdmin(admin);
    }

    public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent)
    {
        return null;
    }

    public boolean isDeviceIdAttestationSupported()
    {
        return false;
    }

    public boolean isUniqueDeviceAttestationSupported()
    {
        return false;
    }

    public boolean isUsbDataSignalingEnabled()
    {
        return true;
    }

    public boolean canUsbDataSignalingBeDisabled()
    {
        return false;
    }

    public boolean isBackupServiceEnabled(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean isSecurityLoggingEnabled(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean isNetworkLoggingEnabled(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean isEphemeralUser(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean isMasterVolumeMuted(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public CharSequence getOrganizationName(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public int getOrganizationColor(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public CharSequence getShortSupportMessage(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public CharSequence getLongSupportMessage(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public List<UserHandle> getSecondaryUsers(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public boolean canAdminGrantSensorsPermissions()
    {
        return false;
    }

    public int getNearbyNotificationStreamingPolicy()
    {
        return NEARBY_STREAMING_NOT_CONTROLLED_BY_POLICY;
    }

    public int getNearbyAppStreamingPolicy()
    {
        return NEARBY_STREAMING_NOT_CONTROLLED_BY_POLICY;
    }

    public int getMtePolicy()
    {
        throw notOwner("getMtePolicy");
    }

    public static boolean isMtePolicyEnforced()
    {
        return false;
    }

    public boolean isCommonCriteriaModeEnabled(ComponentName admin)
    {
        return false;
    }

    public int getPersonalAppsSuspendedReasons(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public long getManagedProfileMaximumTimeOff(ComponentName admin)
    {
        throw notAdmin(admin);
    }

    public String getEndUserSessionMessage(ComponentName admin)
    {
        return null;
    }

    public String getStartUserSessionMessage(ComponentName admin)
    {
        return null;
    }

    public String getDevicePolicyManagementRoleHolderPackage()
    {
        return null;
    }

    public UserHandle createAndManageUser(ComponentName admin, String name, ComponentName profileOwner, PersistableBundle adminExtras,
                                          int flags)
    {
        throw notAdmin(admin);
    }
}
