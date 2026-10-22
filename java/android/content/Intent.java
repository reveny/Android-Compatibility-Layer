package android.content;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class Intent implements Parcelable, Cloneable
{
    public static final String ACTION_AIRPLANE_MODE_CHANGED = "android.intent.action.AIRPLANE_MODE";
    public static final String ACTION_ALL_APPS = "android.intent.action.ALL_APPS";
    public static final String ACTION_ANSWER = "android.intent.action.ANSWER";
    public static final String ACTION_APPLICATION_LOCALE_CHANGED = "android.intent.action.APPLICATION_LOCALE_CHANGED";
    public static final String ACTION_APPLICATION_PREFERENCES = "android.intent.action.APPLICATION_PREFERENCES";
    public static final String ACTION_APPLICATION_RESTRICTIONS_CHANGED = "android.intent.action.APPLICATION_RESTRICTIONS_CHANGED";
    public static final String ACTION_APP_ERROR = "android.intent.action.APP_ERROR";
    public static final String ACTION_ASSIST = "android.intent.action.ASSIST";
    public static final String ACTION_ATTACH_DATA = "android.intent.action.ATTACH_DATA";
    public static final String ACTION_AUTO_REVOKE_PERMISSIONS = "android.intent.action.AUTO_REVOKE_PERMISSIONS";
    public static final String ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";
    public static final String ACTION_BATTERY_LOW = "android.intent.action.BATTERY_LOW";
    public static final String ACTION_BATTERY_OKAY = "android.intent.action.BATTERY_OKAY";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_BUG_REPORT = "android.intent.action.BUG_REPORT";
    public static final String ACTION_CALL = "android.intent.action.CALL";
    public static final String ACTION_CALL_BUTTON = "android.intent.action.CALL_BUTTON";
    public static final String ACTION_CAMERA_BUTTON = "android.intent.action.CAMERA_BUTTON";
    public static final String ACTION_CARRIER_SETUP = "android.intent.action.CARRIER_SETUP";
    public static final String ACTION_CHOOSER = "android.intent.action.CHOOSER";
    public static final String ACTION_CLOSE_SYSTEM_DIALOGS = "android.intent.action.CLOSE_SYSTEM_DIALOGS";
    public static final String ACTION_CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    public static final String ACTION_CREATE_DOCUMENT = "android.intent.action.CREATE_DOCUMENT";
    public static final String ACTION_CREATE_NOTE = "android.intent.action.CREATE_NOTE";
    public static final String ACTION_CREATE_REMINDER = "android.intent.action.CREATE_REMINDER";
    public static final String ACTION_CREATE_SHORTCUT = "android.intent.action.CREATE_SHORTCUT";
    public static final String ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED";
    public static final String ACTION_DEFAULT = "android.intent.action.VIEW";
    public static final String ACTION_DEFINE = "android.intent.action.DEFINE";
    public static final String ACTION_DELETE = "android.intent.action.DELETE";
    public static final String ACTION_DEVICE_STORAGE_LOW = "android.intent.action.DEVICE_STORAGE_LOW";
    public static final String ACTION_DEVICE_STORAGE_OK = "android.intent.action.DEVICE_STORAGE_OK";
    public static final String ACTION_DIAL = "android.intent.action.DIAL";
    public static final String ACTION_DOCK_EVENT = "android.intent.action.DOCK_EVENT";
    public static final String ACTION_DREAMING_STARTED = "android.intent.action.DREAMING_STARTED";
    public static final String ACTION_DREAMING_STOPPED = "android.intent.action.DREAMING_STOPPED";
    public static final String ACTION_EDIT = "android.intent.action.EDIT";
    public static final String ACTION_EXTERNAL_APPLICATIONS_AVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
    public static final String ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
    public static final String ACTION_FACTORY_TEST = "android.intent.action.FACTORY_TEST";
    public static final String ACTION_GET_CONTENT = "android.intent.action.GET_CONTENT";
    public static final String ACTION_GET_RESTRICTION_ENTRIES = "android.intent.action.GET_RESTRICTION_ENTRIES";
    public static final String ACTION_GTALK_SERVICE_CONNECTED = "android.intent.action.GTALK_CONNECTED";
    public static final String ACTION_GTALK_SERVICE_DISCONNECTED = "android.intent.action.GTALK_DISCONNECTED";
    public static final String ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    public static final String ACTION_INPUT_METHOD_CHANGED = "android.intent.action.INPUT_METHOD_CHANGED";
    public static final String ACTION_INSERT = "android.intent.action.INSERT";
    public static final String ACTION_INSERT_OR_EDIT = "android.intent.action.INSERT_OR_EDIT";
    public static final String ACTION_INSTALL_FAILURE = "android.intent.action.INSTALL_FAILURE";
    public static final String ACTION_INSTALL_PACKAGE = "android.intent.action.INSTALL_PACKAGE";
    public static final String ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE =
        "android.intent.action.LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE";
    public static final String ACTION_LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED";
    public static final String ACTION_LOCKED_BOOT_COMPLETED = "android.intent.action.LOCKED_BOOT_COMPLETED";
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_MANAGED_PROFILE_ADDED = "android.intent.action.MANAGED_PROFILE_ADDED";
    public static final String ACTION_MANAGED_PROFILE_AVAILABLE = "android.intent.action.MANAGED_PROFILE_AVAILABLE";
    public static final String ACTION_MANAGED_PROFILE_REMOVED = "android.intent.action.MANAGED_PROFILE_REMOVED";
    public static final String ACTION_MANAGED_PROFILE_UNAVAILABLE = "android.intent.action.MANAGED_PROFILE_UNAVAILABLE";
    public static final String ACTION_MANAGED_PROFILE_UNLOCKED = "android.intent.action.MANAGED_PROFILE_UNLOCKED";
    public static final String ACTION_MANAGE_NETWORK_USAGE = "android.intent.action.MANAGE_NETWORK_USAGE";
    public static final String ACTION_MANAGE_PACKAGE_STORAGE = "android.intent.action.MANAGE_PACKAGE_STORAGE";
    public static final String ACTION_MANAGE_UNUSED_APPS = "android.intent.action.MANAGE_UNUSED_APPS";
    public static final String ACTION_MEDIA_BAD_REMOVAL = "android.intent.action.MEDIA_BAD_REMOVAL";
    public static final String ACTION_MEDIA_BUTTON = "android.intent.action.MEDIA_BUTTON";
    public static final String ACTION_MEDIA_CHECKING = "android.intent.action.MEDIA_CHECKING";
    public static final String ACTION_MEDIA_EJECT = "android.intent.action.MEDIA_EJECT";
    public static final String ACTION_MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
    public static final String ACTION_MEDIA_NOFS = "android.intent.action.MEDIA_NOFS";
    public static final String ACTION_MEDIA_REMOVED = "android.intent.action.MEDIA_REMOVED";
    public static final String ACTION_MEDIA_SCANNER_FINISHED = "android.intent.action.MEDIA_SCANNER_FINISHED";
    public static final String ACTION_MEDIA_SCANNER_SCAN_FILE = "android.intent.action.MEDIA_SCANNER_SCAN_FILE";
    public static final String ACTION_MEDIA_SCANNER_STARTED = "android.intent.action.MEDIA_SCANNER_STARTED";
    public static final String ACTION_MEDIA_SHARED = "android.intent.action.MEDIA_SHARED";
    public static final String ACTION_MEDIA_UNMOUNTABLE = "android.intent.action.MEDIA_UNMOUNTABLE";
    public static final String ACTION_MEDIA_UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED";
    public static final String ACTION_MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED";
    public static final String ACTION_MY_PACKAGE_SUSPENDED = "android.intent.action.MY_PACKAGE_SUSPENDED";
    public static final String ACTION_MY_PACKAGE_UNSUSPENDED = "android.intent.action.MY_PACKAGE_UNSUSPENDED";
    public static final String ACTION_NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL";
    public static final String ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
    public static final String ACTION_OPEN_DOCUMENT_TREE = "android.intent.action.OPEN_DOCUMENT_TREE";
    public static final String ACTION_PACKAGES_SUSPENDED = "android.intent.action.PACKAGES_SUSPENDED";
    public static final String ACTION_PACKAGES_UNSUSPENDED = "android.intent.action.PACKAGES_UNSUSPENDED";
    public static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    public static final String ACTION_PACKAGE_CHANGED = "android.intent.action.PACKAGE_CHANGED";
    public static final String ACTION_PACKAGE_DATA_CLEARED = "android.intent.action.PACKAGE_DATA_CLEARED";
    public static final String ACTION_PACKAGE_FIRST_LAUNCH = "android.intent.action.PACKAGE_FIRST_LAUNCH";
    public static final String ACTION_PACKAGE_FULLY_REMOVED = "android.intent.action.PACKAGE_FULLY_REMOVED";
    public static final String ACTION_PACKAGE_INSTALL = "android.intent.action.PACKAGE_INSTALL";
    public static final String ACTION_PACKAGE_NEEDS_VERIFICATION = "android.intent.action.PACKAGE_NEEDS_VERIFICATION";
    public static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
    public static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
    public static final String ACTION_PACKAGE_RESTARTED = "android.intent.action.PACKAGE_RESTARTED";
    public static final String ACTION_PACKAGE_VERIFIED = "android.intent.action.PACKAGE_VERIFIED";
    public static final String ACTION_PASTE = "android.intent.action.PASTE";
    public static final String ACTION_PICK = "android.intent.action.PICK";
    public static final String ACTION_PICK_ACTIVITY = "android.intent.action.PICK_ACTIVITY";
    public static final String ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED";
    public static final String ACTION_POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED";
    public static final String ACTION_POWER_USAGE_SUMMARY = "android.intent.action.POWER_USAGE_SUMMARY";
    public static final String ACTION_PROCESS_TEXT = "android.intent.action.PROCESS_TEXT";
    public static final String ACTION_PROFILE_ACCESSIBLE = "android.intent.action.PROFILE_ACCESSIBLE";
    public static final String ACTION_PROFILE_ADDED = "android.intent.action.PROFILE_ADDED";
    public static final String ACTION_PROFILE_INACCESSIBLE = "android.intent.action.PROFILE_INACCESSIBLE";
    public static final String ACTION_PROFILE_REMOVED = "android.intent.action.PROFILE_REMOVED";
    public static final String ACTION_PROVIDER_CHANGED = "android.intent.action.PROVIDER_CHANGED";
    public static final String ACTION_QUICK_CLOCK = "android.intent.action.QUICK_CLOCK";
    public static final String ACTION_QUICK_VIEW = "android.intent.action.QUICK_VIEW";
    public static final String ACTION_REBOOT = "android.intent.action.REBOOT";
    public static final String ACTION_RUN = "android.intent.action.RUN";
    public static final String ACTION_SAFETY_CENTER = "android.intent.action.SAFETY_CENTER";
    public static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    public static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    public static final String ACTION_SEARCH = "android.intent.action.SEARCH";
    public static final String ACTION_SEARCH_LONG_PRESS = "android.intent.action.SEARCH_LONG_PRESS";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_SENDTO = "android.intent.action.SENDTO";
    public static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";
    public static final String ACTION_SET_WALLPAPER = "android.intent.action.SET_WALLPAPER";
    public static final String ACTION_SHOW_APP_INFO = "android.intent.action.SHOW_APP_INFO";
    public static final String ACTION_SHOW_WORK_APPS = "android.intent.action.SHOW_WORK_APPS";
    public static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";
    public static final String ACTION_SYNC = "android.intent.action.SYNC";
    public static final String ACTION_SYSTEM_TUTORIAL = "android.intent.action.SYSTEM_TUTORIAL";
    public static final String ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
    public static final String ACTION_TIME_CHANGED = "android.intent.action.TIME_SET";
    public static final String ACTION_TIME_TICK = "android.intent.action.TIME_TICK";
    public static final String ACTION_TRANSLATE = "android.intent.action.TRANSLATE";
    public static final String ACTION_UID_REMOVED = "android.intent.action.UID_REMOVED";
    public static final String ACTION_UMS_CONNECTED = "android.intent.action.UMS_CONNECTED";
    public static final String ACTION_UMS_DISCONNECTED = "android.intent.action.UMS_DISCONNECTED";
    public static final String ACTION_UNINSTALL_PACKAGE = "android.intent.action.UNINSTALL_PACKAGE";
    public static final String ACTION_USER_BACKGROUND = "android.intent.action.USER_BACKGROUND";
    public static final String ACTION_USER_FOREGROUND = "android.intent.action.USER_FOREGROUND";
    public static final String ACTION_USER_INITIALIZE = "android.intent.action.USER_INITIALIZE";
    public static final String ACTION_USER_PRESENT = "android.intent.action.USER_PRESENT";
    public static final String ACTION_USER_UNLOCKED = "android.intent.action.USER_UNLOCKED";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String ACTION_VIEW_LOCUS = "android.intent.action.VIEW_LOCUS";
    public static final String ACTION_VIEW_PERMISSION_USAGE = "android.intent.action.VIEW_PERMISSION_USAGE";
    public static final String ACTION_VIEW_PERMISSION_USAGE_FOR_PERIOD = "android.intent.action.VIEW_PERMISSION_USAGE_FOR_PERIOD";
    public static final String ACTION_VOICE_COMMAND = "android.intent.action.VOICE_COMMAND";
    public static final String ACTION_WALLPAPER_CHANGED = "android.intent.action.WALLPAPER_CHANGED";
    public static final String ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH";

    public static final String CATEGORY_ACCESSIBILITY_SHORTCUT_TARGET = "android.intent.category.ACCESSIBILITY_SHORTCUT_TARGET";
    public static final String CATEGORY_ALTERNATIVE = "android.intent.category.ALTERNATIVE";
    public static final String CATEGORY_APP_BROWSER = "android.intent.category.APP_BROWSER";
    public static final String CATEGORY_APP_CALCULATOR = "android.intent.category.APP_CALCULATOR";
    public static final String CATEGORY_APP_CALENDAR = "android.intent.category.APP_CALENDAR";
    public static final String CATEGORY_APP_CONTACTS = "android.intent.category.APP_CONTACTS";
    public static final String CATEGORY_APP_EMAIL = "android.intent.category.APP_EMAIL";
    public static final String CATEGORY_APP_FILES = "android.intent.category.APP_FILES";
    public static final String CATEGORY_APP_FITNESS = "android.intent.category.APP_FITNESS";
    public static final String CATEGORY_APP_GALLERY = "android.intent.category.APP_GALLERY";
    public static final String CATEGORY_APP_MAPS = "android.intent.category.APP_MAPS";
    public static final String CATEGORY_APP_MARKET = "android.intent.category.APP_MARKET";
    public static final String CATEGORY_APP_MESSAGING = "android.intent.category.APP_MESSAGING";
    public static final String CATEGORY_APP_MUSIC = "android.intent.category.APP_MUSIC";
    public static final String CATEGORY_APP_WEATHER = "android.intent.category.APP_WEATHER";
    public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";
    public static final String CATEGORY_CAR_DOCK = "android.intent.category.CAR_DOCK";
    public static final String CATEGORY_CAR_MODE = "android.intent.category.CAR_MODE";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_DESK_DOCK = "android.intent.category.DESK_DOCK";
    public static final String CATEGORY_DEVELOPMENT_PREFERENCE = "android.intent.category.DEVELOPMENT_PREFERENCE";
    public static final String CATEGORY_EMBED = "android.intent.category.EMBED";
    public static final String CATEGORY_FRAMEWORK_INSTRUMENTATION_TEST = "android.intent.category.FRAMEWORK_INSTRUMENTATION_TEST";
    public static final String CATEGORY_HE_DESK_DOCK = "android.intent.category.HE_DESK_DOCK";
    public static final String CATEGORY_HOME = "android.intent.category.HOME";
    public static final String CATEGORY_INFO = "android.intent.category.INFO";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_LEANBACK_LAUNCHER = "android.intent.category.LEANBACK_LAUNCHER";
    public static final String CATEGORY_LE_DESK_DOCK = "android.intent.category.LE_DESK_DOCK";
    public static final String CATEGORY_MONKEY = "android.intent.category.MONKEY";
    public static final String CATEGORY_OPENABLE = "android.intent.category.OPENABLE";
    public static final String CATEGORY_PREFERENCE = "android.intent.category.PREFERENCE";
    public static final String CATEGORY_SAMPLE_CODE = "android.intent.category.SAMPLE_CODE";
    public static final String CATEGORY_SECONDARY_HOME = "android.intent.category.SECONDARY_HOME";
    public static final String CATEGORY_SELECTED_ALTERNATIVE = "android.intent.category.SELECTED_ALTERNATIVE";
    public static final String CATEGORY_TAB = "android.intent.category.TAB";
    public static final String CATEGORY_TEST = "android.intent.category.TEST";
    public static final String CATEGORY_TYPED_OPENABLE = "android.intent.category.TYPED_OPENABLE";
    public static final String CATEGORY_UNIT_TEST = "android.intent.category.UNIT_TEST";
    public static final String CATEGORY_VOICE = "android.intent.category.VOICE";
    public static final String CATEGORY_VR_HOME = "android.intent.category.VR_HOME";

    public static final String EXTRA_ALARM_COUNT = "android.intent.extra.ALARM_COUNT";
    public static final String EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE";
    public static final String EXTRA_ALLOW_REPLACE = "android.intent.extra.ALLOW_REPLACE";
    public static final String EXTRA_ALTERNATE_INTENTS = "android.intent.extra.ALTERNATE_INTENTS";
    public static final String EXTRA_ASSIST_CONTEXT = "android.intent.extra.ASSIST_CONTEXT";
    public static final String EXTRA_ASSIST_INPUT_DEVICE_ID = "android.intent.extra.ASSIST_INPUT_DEVICE_ID";
    public static final String EXTRA_ASSIST_INPUT_HINT_KEYBOARD = "android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD";
    public static final String EXTRA_ASSIST_PACKAGE = "android.intent.extra.ASSIST_PACKAGE";
    public static final String EXTRA_ASSIST_UID = "android.intent.extra.ASSIST_UID";
    public static final String EXTRA_ATTRIBUTION_TAGS = "android.intent.extra.ATTRIBUTION_TAGS";
    public static final String EXTRA_AUTO_LAUNCH_SINGLE_CHOICE = "android.intent.extra.AUTO_LAUNCH_SINGLE_CHOICE";
    public static final String EXTRA_BCC = "android.intent.extra.BCC";
    public static final String EXTRA_BUG_REPORT = "android.intent.extra.BUG_REPORT";
    public static final String EXTRA_CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE = "android.intent.extra.CAPTURE_CONTENT_FOR_NOTE_STATUS_CODE";
    public static final String EXTRA_CC = "android.intent.extra.CC";
    public static final String EXTRA_CHANGED_COMPONENT_NAME = "android.intent.extra.changed_component_name";
    public static final String EXTRA_CHANGED_COMPONENT_NAME_LIST = "android.intent.extra.changed_component_name_list";
    public static final String EXTRA_CHANGED_PACKAGE_LIST = "android.intent.extra.changed_package_list";
    public static final String EXTRA_CHANGED_UID_LIST = "android.intent.extra.changed_uid_list";
    public static final String EXTRA_CHOOSER_CUSTOM_ACTIONS = "android.intent.extra.CHOOSER_CUSTOM_ACTIONS";
    public static final String EXTRA_CHOOSER_MODIFY_SHARE_ACTION = "android.intent.extra.CHOOSER_MODIFY_SHARE_ACTION";
    public static final String EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER = "android.intent.extra.CHOOSER_REFINEMENT_INTENT_SENDER";
    public static final String EXTRA_CHOOSER_TARGETS = "android.intent.extra.CHOOSER_TARGETS";
    public static final String EXTRA_CHOSEN_COMPONENT = "android.intent.extra.CHOSEN_COMPONENT";
    public static final String EXTRA_CHOSEN_COMPONENT_INTENT_SENDER = "android.intent.extra.CHOSEN_COMPONENT_INTENT_SENDER";
    public static final String EXTRA_COMPONENT_NAME = "android.intent.extra.COMPONENT_NAME";
    public static final String EXTRA_CONTENT_ANNOTATIONS = "android.intent.extra.CONTENT_ANNOTATIONS";
    public static final String EXTRA_CONTENT_QUERY = "android.intent.extra.CONTENT_QUERY";
    public static final String EXTRA_DATA_REMOVED = "android.intent.extra.DATA_REMOVED";
    public static final String EXTRA_DOCK_STATE = "android.intent.extra.DOCK_STATE";
    public static final String EXTRA_DONT_KILL_APP = "android.intent.extra.DONT_KILL_APP";
    public static final String EXTRA_DURATION_MILLIS = "android.intent.extra.DURATION_MILLIS";
    public static final String EXTRA_EMAIL = "android.intent.extra.EMAIL";
    public static final String EXTRA_END_TIME = "android.intent.extra.END_TIME";
    public static final String EXTRA_EXCLUDE_COMPONENTS = "android.intent.extra.EXCLUDE_COMPONENTS";
    public static final String EXTRA_FROM_STORAGE = "android.intent.extra.FROM_STORAGE";
    public static final String EXTRA_HTML_TEXT = "android.intent.extra.HTML_TEXT";
    public static final String EXTRA_INDEX = "android.intent.extra.INDEX";
    public static final String EXTRA_INITIAL_INTENTS = "android.intent.extra.INITIAL_INTENTS";
    public static final String EXTRA_INSTALLER_PACKAGE_NAME = "android.intent.extra.INSTALLER_PACKAGE_NAME";
    public static final String EXTRA_INTENT = "android.intent.extra.INTENT";
    public static final String EXTRA_KEY_EVENT = "android.intent.extra.KEY_EVENT";
    public static final String EXTRA_LOCALE_LIST = "android.intent.extra.LOCALE_LIST";
    public static final String EXTRA_LOCAL_ONLY = "android.intent.extra.LOCAL_ONLY";
    public static final String EXTRA_LOCUS_ID = "android.intent.extra.LOCUS_ID";
    public static final String EXTRA_MIME_TYPES = "android.intent.extra.MIME_TYPES";
    public static final String EXTRA_NOT_UNKNOWN_SOURCE = "android.intent.extra.NOT_UNKNOWN_SOURCE";
    public static final String EXTRA_ORIGINATING_URI = "android.intent.extra.ORIGINATING_URI";
    public static final String EXTRA_PACKAGES = "android.intent.extra.PACKAGES";
    public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";
    public static final String EXTRA_PERMISSION_GROUP_NAME = "android.intent.extra.PERMISSION_GROUP_NAME";
    public static final String EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER";
    public static final String EXTRA_PROCESS_TEXT = "android.intent.extra.PROCESS_TEXT";
    public static final String EXTRA_PROCESS_TEXT_READONLY = "android.intent.extra.PROCESS_TEXT_READONLY";
    public static final String EXTRA_QUICK_VIEW_FEATURES = "android.intent.extra.QUICK_VIEW_FEATURES";
    public static final String EXTRA_QUIET_MODE = "android.intent.extra.QUIET_MODE";
    public static final String EXTRA_REFERRER = "android.intent.extra.REFERRER";
    public static final String EXTRA_REFERRER_NAME = "android.intent.extra.REFERRER_NAME";
    public static final String EXTRA_REMOTE_INTENT_TOKEN = "android.intent.extra.remote_intent_token";
    public static final String EXTRA_REPLACEMENT_EXTRAS = "android.intent.extra.REPLACEMENT_EXTRAS";
    public static final String EXTRA_REPLACING = "android.intent.extra.REPLACING";
    public static final String EXTRA_RESTRICTIONS_BUNDLE = "android.intent.extra.restrictions_bundle";
    public static final String EXTRA_RESTRICTIONS_INTENT = "android.intent.extra.restrictions_intent";
    public static final String EXTRA_RESTRICTIONS_LIST = "android.intent.extra.restrictions_list";
    public static final String EXTRA_RESULT_RECEIVER = "android.intent.extra.RESULT_RECEIVER";
    public static final String EXTRA_RETURN_RESULT = "android.intent.extra.RETURN_RESULT";
    public static final String EXTRA_SHORTCUT_ICON = "android.intent.extra.shortcut.ICON";
    public static final String EXTRA_SHORTCUT_ICON_RESOURCE = "android.intent.extra.shortcut.ICON_RESOURCE";
    public static final String EXTRA_SHORTCUT_ID = "android.intent.extra.shortcut.ID";
    public static final String EXTRA_SHORTCUT_INTENT = "android.intent.extra.shortcut.INTENT";
    public static final String EXTRA_SHORTCUT_NAME = "android.intent.extra.shortcut.NAME";
    public static final String EXTRA_SHUTDOWN_USERSPACE_ONLY = "android.intent.extra.SHUTDOWN_USERSPACE_ONLY";
    public static final String EXTRA_SPLIT_NAME = "android.intent.extra.SPLIT_NAME";
    public static final String EXTRA_START_TIME = "android.intent.extra.START_TIME";
    public static final String EXTRA_STREAM = "android.intent.extra.STREAM";
    public static final String EXTRA_SUBJECT = "android.intent.extra.SUBJECT";
    public static final String EXTRA_SUSPENDED_PACKAGE_EXTRAS = "android.intent.extra.SUSPENDED_PACKAGE_EXTRAS";
    public static final String EXTRA_TEMPLATE = "android.intent.extra.TEMPLATE";
    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final String EXTRA_TIME = "android.intent.extra.TIME";
    public static final String EXTRA_TIMEZONE = "time-zone";
    public static final String EXTRA_TITLE = "android.intent.extra.TITLE";
    public static final String EXTRA_UID = "android.intent.extra.UID";
    public static final String EXTRA_USER = "android.intent.extra.USER";
    public static final String EXTRA_USER_INITIATED = "android.intent.extra.USER_INITIATED";
    public static final String EXTRA_USE_STYLUS_MODE = "android.intent.extra.USE_STYLUS_MODE";
    public static final String METADATA_DOCK_HOME = "android.dock_home";

    public static final int CAPTURE_CONTENT_FOR_NOTE_SUCCESS = 0;
    public static final int CAPTURE_CONTENT_FOR_NOTE_FAILED = 1;
    public static final int CAPTURE_CONTENT_FOR_NOTE_USER_CANCELED = 2;
    public static final int CAPTURE_CONTENT_FOR_NOTE_WINDOW_MODE_UNSUPPORTED = 3;
    public static final int CAPTURE_CONTENT_FOR_NOTE_BLOCKED_BY_ADMIN = 4;

    public static final int EXTRA_DOCK_STATE_UNDOCKED = 0;
    public static final int EXTRA_DOCK_STATE_DESK = 1;
    public static final int EXTRA_DOCK_STATE_CAR = 2;
    public static final int EXTRA_DOCK_STATE_LE_DESK = 3;
    public static final int EXTRA_DOCK_STATE_HE_DESK = 4;

    public static final int FILL_IN_ACTION = 1;
    public static final int FILL_IN_DATA = 2;
    public static final int FILL_IN_CATEGORIES = 4;
    public static final int FILL_IN_COMPONENT = 8;
    public static final int FILL_IN_PACKAGE = 16;
    public static final int FILL_IN_SOURCE_BOUNDS = 32;
    public static final int FILL_IN_SELECTOR = 64;
    public static final int FILL_IN_CLIP_DATA = 128;
    public static final int FILL_IN_IDENTIFIER = 256;

    public static final int FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
    public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;
    public static final int FLAG_FROM_BACKGROUND = 0x00000004;
    public static final int FLAG_DEBUG_LOG_RESOLUTION = 0x00000008;
    public static final int FLAG_EXCLUDE_STOPPED_PACKAGES = 0x00000010;
    public static final int FLAG_INCLUDE_STOPPED_PACKAGES = 0x00000020;
    public static final int FLAG_GRANT_PERSISTABLE_URI_PERMISSION = 0x00000040;
    public static final int FLAG_GRANT_PREFIX_URI_PERMISSION = 0x00000080;
    public static final int FLAG_DIRECT_BOOT_AUTO = 0x00000100;
    public static final int FLAG_ACTIVITY_REQUIRE_DEFAULT = 0x00000200;
    public static final int FLAG_ACTIVITY_REQUIRE_NON_BROWSER = 0x00000400;
    public static final int FLAG_ACTIVITY_MATCH_EXTERNAL = 0x00000800;
    public static final int FLAG_ACTIVITY_LAUNCH_ADJACENT = 0x00001000;
    public static final int FLAG_ACTIVITY_RETAIN_IN_RECENTS = 0x00002000;
    public static final int FLAG_ACTIVITY_TASK_ON_HOME = 0x00004000;
    public static final int FLAG_ACTIVITY_CLEAR_TASK = 0x00008000;
    public static final int FLAG_ACTIVITY_NO_ANIMATION = 0x00010000;
    public static final int FLAG_ACTIVITY_REORDER_TO_FRONT = 0x00020000;
    public static final int FLAG_ACTIVITY_NO_USER_ACTION = 0x00040000;
    public static final int FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET = 0x00080000;
    public static final int FLAG_ACTIVITY_NEW_DOCUMENT = 0x00080000;
    public static final int FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000;
    public static final int FLAG_ACTIVITY_RESET_TASK_IF_NEEDED = 0x00200000;
    public static final int FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS = 0x00200000;
    public static final int FLAG_ACTIVITY_BROUGHT_TO_FRONT = 0x00400000;
    public static final int FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS = 0x00800000;
    public static final int FLAG_ACTIVITY_PREVIOUS_IS_TOP = 0x01000000;
    public static final int FLAG_ACTIVITY_FORWARD_RESULT = 0x02000000;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    public static final int FLAG_ACTIVITY_MULTIPLE_TASK = 0x08000000;
    public static final int FLAG_RECEIVER_NO_ABORT = 0x08000000;
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_RECEIVER_FOREGROUND = 0x10000000;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
    public static final int FLAG_RECEIVER_REPLACE_PENDING = 0x20000000;
    public static final int FLAG_ACTIVITY_NO_HISTORY = 0x40000000;
    public static final int FLAG_RECEIVER_REGISTERED_ONLY = 0x40000000;

    public static final int URI_INTENT_SCHEME = 1;
    public static final int URI_ANDROID_APP_SCHEME = 2;
    public static final int URI_ALLOW_UNSAFE = 4;

    private static final int IMMUTABLE_FLAGS = FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION
                                               | FLAG_GRANT_PERSISTABLE_URI_PERMISSION | FLAG_GRANT_PREFIX_URI_PERMISSION;

    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    public static final Parcelable.Creator<Intent> CREATOR = new Parcelable.Creator<Intent>()
    {
        @Override
        public Intent createFromParcel(Parcel source)
        {
            Intent intent = new Intent();
            intent.readFromParcel(source);
            return intent;
        }

        @Override
        public Intent[] newArray(int size)
        {
            return new Intent[size];
        }
    };

    private String mAction;
    private Uri mData;
    private String mType;
    private String mIdentifier;
    private String mPackage;
    private ComponentName mComponent;
    private int mFlags;
    private LinkedHashSet<String> mCategories;
    private Bundle mExtras;
    private android.graphics.Rect mSourceBounds;
    private Intent mSelector;
    private ClipData mClipData;

    public Intent()
    {
    }

    public Intent(Intent other)
    {
        mAction = other.mAction;
        mData = other.mData;
        mType = other.mType;
        mIdentifier = other.mIdentifier;
        mPackage = other.mPackage;
        mComponent = other.mComponent;
        mFlags = other.mFlags;
        if (other.mCategories != null) mCategories = new LinkedHashSet<>(other.mCategories);
        if (other.mExtras != null) mExtras = new Bundle(other.mExtras);
        if (other.mSourceBounds != null) mSourceBounds = new android.graphics.Rect(other.mSourceBounds);
        if (other.mSelector != null) mSelector = new Intent(other.mSelector);
        mClipData = other.mClipData;
    }

    public Intent(String action)
    {
        setAction(action);
    }

    public Intent(String action, Uri uri)
    {
        setAction(action);
        mData = uri;
    }

    public Intent(Context packageContext, Class<?> target)
    {
        mComponent = new ComponentName(packageContext == null ? "" : packageContext.getPackageName(), target.getName());
    }

    public Intent(String action, Uri uri, Context packageContext, Class<?> target)
    {
        setAction(action);
        mData = uri;
        mComponent = new ComponentName(packageContext == null ? "" : packageContext.getPackageName(), target.getName());
    }

    public static Intent createChooser(Intent target, CharSequence title)
    {
        return createChooser(target, title, null);
    }

    public static Intent createChooser(Intent target, CharSequence title, IntentSender sender)
    {
        Intent intent = new Intent(ACTION_CHOOSER);
        intent.putExtra(EXTRA_INTENT, target);
        if (title != null) intent.putExtra(EXTRA_TITLE, title);
        if (sender != null) intent.putExtra(EXTRA_CHOSEN_COMPONENT_INTENT_SENDER, sender);

        if (target != null)
        {
            int permissionGrants = target.getFlags() & IMMUTABLE_FLAGS;
            if (target.getClipData() != null) intent.setClipData(target.getClipData());
            if (permissionGrants != 0) intent.addFlags(permissionGrants);
        }

        return intent;
    }

    @Override
    public Object clone()
    {
        return new Intent(this);
    }

    public Intent cloneFilter()
    {
        Intent filter = new Intent();
        filter.mAction = mAction;
        filter.mData = mData;
        filter.mType = mType;
        filter.mIdentifier = mIdentifier;
        filter.mPackage = mPackage;
        filter.mComponent = mComponent;
        if (mCategories != null) filter.mCategories = new LinkedHashSet<>(mCategories);
        return filter;
    }

    public static Intent makeMainActivity(ComponentName mainActivity)
    {
        Intent intent = new Intent(ACTION_MAIN);
        intent.setComponent(mainActivity);
        intent.addCategory(CATEGORY_LAUNCHER);
        return intent;
    }

    public static Intent makeMainSelectorActivity(String selectorAction, String selectorCategory)
    {
        Intent intent = new Intent(ACTION_MAIN);
        intent.addCategory(CATEGORY_LAUNCHER);

        Intent selector = new Intent();
        selector.setAction(selectorAction);
        selector.addCategory(selectorCategory);
        intent.setSelector(selector);
        return intent;
    }

    public static Intent makeRestartActivityTask(ComponentName mainActivity)
    {
        Intent intent = makeMainActivity(mainActivity);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    public static Intent getIntent(String uri) throws URISyntaxException
    {
        return parseUri(uri, 0);
    }

    public static Intent getIntentOld(String uri) throws URISyntaxException
    {
        return new Intent(ACTION_VIEW, Uri.parse(uri));
    }

    public static Intent parseUri(String uri, int flags) throws URISyntaxException
    {
        int index = 0;

        try
        {
            boolean androidApp = uri.startsWith("android-app:");

            if ((flags & (URI_INTENT_SCHEME | URI_ANDROID_APP_SCHEME)) != 0)
            {
                if (!uri.startsWith("intent:") && !androidApp)
                {
                    Intent intent = new Intent(ACTION_VIEW);
                    intent.setData(Uri.parse(uri));
                    return intent;
                }
            }

            index = uri.lastIndexOf("#");
            if (index == -1)
            {
                if (!androidApp) return new Intent(ACTION_VIEW, Uri.parse(uri));
            }
            else if (!uri.startsWith("#Intent;", index))
            {
                if (!androidApp) return getIntentOld(uri);
                index = -1;
            }

            Intent intent = new Intent(ACTION_VIEW);
            Intent baseIntent = intent;
            boolean explicitAction = false;
            boolean inSelector = false;
            String scheme = null;
            String data;

            if (index >= 0)
            {
                data = uri.substring(0, index);
                index += 8;
            }
            else
            {
                data = uri;
            }

            while (index >= 0 && !uri.startsWith("end", index))
            {
                int equals = uri.indexOf('=', index);
                if (equals < 0) equals = index - 1;
                int semicolon = uri.indexOf(';', index);
                String value = equals < semicolon ? Uri.decode(uri.substring(equals + 1, semicolon)) : "";

                if (uri.startsWith("action=", index))
                {
                    intent.setAction(value);
                    if (!inSelector) explicitAction = true;
                }
                else if (uri.startsWith("category=", index))
                {
                    intent.addCategory(value);
                }
                else if (uri.startsWith("type=", index))
                {
                    intent.mType = value;
                }
                else if (uri.startsWith("identifier=", index))
                {
                    intent.mIdentifier = value;
                }
                else if (uri.startsWith("launchFlags=", index))
                {
                    intent.mFlags = Integer.decode(value);
                    if ((flags & URI_ALLOW_UNSAFE) == 0) intent.mFlags &= ~IMMUTABLE_FLAGS;
                }
                else if (uri.startsWith("package=", index))
                {
                    intent.mPackage = value;
                }
                else if (uri.startsWith("component=", index))
                {
                    intent.mComponent = ComponentName.unflattenFromString(value);
                }
                else if (uri.startsWith("scheme=", index))
                {
                    if (inSelector) intent.mData = Uri.parse(value + ":");
                    else scheme = value;
                }
                else if (uri.startsWith("sourceBounds=", index))
                {
                    intent.mSourceBounds = android.graphics.Rect.unflattenFromString(value);
                }
                else if (semicolon == (index + 3) && uri.startsWith("SEL", index))
                {
                    intent = new Intent();
                    inSelector = true;
                }
                else
                {
                    String key = Uri.decode(uri.substring(index + 2, equals));
                    if (intent.mExtras == null) intent.mExtras = new Bundle();
                    Bundle extras = intent.mExtras;

                    if (uri.startsWith("S.", index)) extras.putString(key, value);
                    else if (uri.startsWith("B.", index)) extras.putBoolean(key, Boolean.parseBoolean(value));
                    else if (uri.startsWith("b.", index)) extras.putByte(key, Byte.parseByte(value));
                    else if (uri.startsWith("c.", index)) extras.putChar(key, value.charAt(0));
                    else if (uri.startsWith("d.", index)) extras.putDouble(key, Double.parseDouble(value));
                    else if (uri.startsWith("f.", index)) extras.putFloat(key, Float.parseFloat(value));
                    else if (uri.startsWith("i.", index)) extras.putInt(key, Integer.parseInt(value));
                    else if (uri.startsWith("l.", index)) extras.putLong(key, Long.parseLong(value));
                    else if (uri.startsWith("s.", index)) extras.putShort(key, Short.parseShort(value));
                    else throw new URISyntaxException(uri, "unknown EXTRA type", index);
                }

                index = semicolon + 1;
            }

            if (inSelector)
            {
                if (baseIntent.mPackage == null) baseIntent.setSelector(intent);
                intent = baseIntent;
            }

            if (data != null)
            {
                if (data.startsWith("intent:"))
                {
                    data = data.substring(7);
                    if (scheme != null) data = scheme + ':' + data;
                }
                else if (data.startsWith("android-app:"))
                {
                    data = parseAndroidAppData(intent, data, explicitAction);
                }

                if (data.length() > 0) intent.mData = Uri.parse(data);
            }

            return intent;
        }
        catch (IndexOutOfBoundsException malformed)
        {
            throw new URISyntaxException(uri, "illegal Intent URI format", index);
        }
        catch (NumberFormatException malformed)
        {
            throw new URISyntaxException(uri, "illegal Intent URI number", index);
        }
    }

    private static String parseAndroidAppData(Intent intent, String data, boolean explicitAction)
    {
        if (data.length() < 14 || data.charAt(12) != '/' || data.charAt(13) != '/') return "";

        int end = data.indexOf('/', 14);
        if (end < 0)
        {
            intent.mPackage = data.substring(14);
            if (!explicitAction) intent.setAction(ACTION_MAIN);
            return "";
        }

        String scheme = null;
        String authority = null;
        intent.mPackage = data.substring(14, end);

        if ((end + 1) < data.length())
        {
            int newEnd = data.indexOf('/', end + 1);
            if (newEnd >= 0)
            {
                scheme = data.substring(end + 1, newEnd);
                end = newEnd;

                if (end < data.length() && (newEnd = data.indexOf('/', end + 1)) >= 0)
                {
                    authority = data.substring(end + 1, newEnd);
                    end = newEnd;
                }
            }
            else
            {
                scheme = data.substring(end + 1);
            }
        }

        if (scheme == null)
        {
            if (!explicitAction) intent.setAction(ACTION_MAIN);
            return "";
        }

        if (authority == null) return scheme + ":";

        return scheme + "://" + authority + data.substring(end);
    }

    public static Intent parseIntent(android.content.res.Resources resources, org.xmlpull.v1.XmlPullParser parser,
                                     android.util.AttributeSet attrs)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        Intent intent = new Intent();
        intent.setAction(attrs.getAttributeValue(ANDROID_NAMESPACE, "action"));

        String data = attrs.getAttributeValue(ANDROID_NAMESPACE, "data");
        String mimeType = attrs.getAttributeValue(ANDROID_NAMESPACE, "mimeType");
        intent.setDataAndType(data != null ? Uri.parse(data) : null, mimeType);
        intent.setIdentifier(attrs.getAttributeValue(ANDROID_NAMESPACE, "identifier"));

        String packageName = attrs.getAttributeValue(ANDROID_NAMESPACE, "targetPackage");
        String className = attrs.getAttributeValue(ANDROID_NAMESPACE, "targetClass");
        if (packageName != null && className != null) intent.setComponent(new ComponentName(packageName, className));

        int outerDepth = parser.getDepth();
        int type;

        while ((type = parser.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT
               && (type != org.xmlpull.v1.XmlPullParser.END_TAG || parser.getDepth() > outerDepth))
        {
            if (type != org.xmlpull.v1.XmlPullParser.START_TAG) continue;

            String nodeName = parser.getName();
            if ("categories".equals(nodeName))
            {
                String category = attrs.getAttributeValue(ANDROID_NAMESPACE, "name");
                if (category != null) intent.addCategory(category);
            }
            else if ("extra".equals(nodeName))
            {
                String name = attrs.getAttributeValue(ANDROID_NAMESPACE, "name");
                String value = attrs.getAttributeValue(ANDROID_NAMESPACE, "value");
                if (name != null) intent.putExtra(name, value);
            }

            skipCurrentTag(parser);
        }

        return intent;
    }

    private static void skipCurrentTag(org.xmlpull.v1.XmlPullParser parser)
        throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        int outerDepth = parser.getDepth();
        int type;

        while ((type = parser.next()) != org.xmlpull.v1.XmlPullParser.END_DOCUMENT
               && (type != org.xmlpull.v1.XmlPullParser.END_TAG || parser.getDepth() > outerDepth))
        {
        }
    }

    public static String normalizeMimeType(String type)
    {
        if (type == null) return null;

        type = type.trim().toLowerCase(Locale.ROOT);

        int semicolon = type.indexOf(';');
        if (semicolon != -1) type = type.substring(0, semicolon);

        return type;
    }

    public String getAction()
    {
        return mAction;
    }

    public Uri getData()
    {
        return mData;
    }

    public String getDataString()
    {
        return mData == null ? null : mData.toString();
    }

    public String getScheme()
    {
        return mData == null ? null : mData.getScheme();
    }

    public String getType()
    {
        return mType;
    }

    public String resolveType(Context context)
    {
        return resolveType(context.getContentResolver());
    }

    public String resolveType(ContentResolver resolver)
    {
        if (mType != null) return mType;
        if (mData != null && ContentResolver.SCHEME_CONTENT.equals(mData.getScheme())) return resolver.getType(mData);

        return null;
    }

    public String resolveTypeIfNeeded(ContentResolver resolver)
    {
        if (mComponent != null) return mType;

        return resolveType(resolver);
    }

    public String getIdentifier()
    {
        return mIdentifier;
    }

    public boolean hasCategory(String category)
    {
        return mCategories != null && mCategories.contains(category);
    }

    public Set<String> getCategories()
    {
        return mCategories;
    }

    public Intent getSelector()
    {
        return mSelector;
    }

    public ClipData getClipData()
    {
        return mClipData;
    }

    public void setExtrasClassLoader(ClassLoader loader)
    {
        if (mExtras != null) mExtras.setClassLoader(loader);
    }

    public boolean hasExtra(String name)
    {
        return mExtras != null && mExtras.containsKey(name);
    }

    public boolean hasFileDescriptors()
    {
        return mExtras != null && mExtras.hasFileDescriptors();
    }

    public boolean getBooleanExtra(String name, boolean defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getBoolean(name, defaultValue);
    }

    public byte getByteExtra(String name, byte defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getByte(name, defaultValue);
    }

    public short getShortExtra(String name, short defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getShort(name, defaultValue);
    }

    public char getCharExtra(String name, char defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getChar(name, defaultValue);
    }

    public int getIntExtra(String name, int defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getInt(name, defaultValue);
    }

    public long getLongExtra(String name, long defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getLong(name, defaultValue);
    }

    public float getFloatExtra(String name, float defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getFloat(name, defaultValue);
    }

    public double getDoubleExtra(String name, double defaultValue)
    {
        return mExtras == null ? defaultValue : mExtras.getDouble(name, defaultValue);
    }

    public String getStringExtra(String name)
    {
        return mExtras == null ? null : mExtras.getString(name);
    }

    public CharSequence getCharSequenceExtra(String name)
    {
        return mExtras == null ? null : mExtras.getCharSequence(name);
    }

    public <T extends Parcelable> T getParcelableExtra(String name)
    {
        return mExtras == null ? null : mExtras.<T>getParcelable(name);
    }

    public <T> T getParcelableExtra(String name, Class<T> type)
    {
        return mExtras == null ? null : mExtras.getParcelable(name, type);
    }

    public Parcelable[] getParcelableArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getParcelableArray(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getParcelableArrayExtra(String name, Class<T> type)
    {
        Parcelable[] array = getParcelableArrayExtra(name);
        if (array == null) return null;

        T[] typed = (T[])java.lang.reflect.Array.newInstance(type, array.length);
        for (int index = 0; index < array.length; index++)
        {
            if (array[index] != null && !type.isInstance(array[index])) return null;
            typed[index] = type.cast(array[index]);
        }

        return typed;
    }

    public <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(String name)
    {
        return mExtras == null ? null : mExtras.<T>getParcelableArrayList(name);
    }

    public <T> ArrayList<T> getParcelableArrayListExtra(String name, Class<? extends T> type)
    {
        return mExtras == null ? null : mExtras.<T>getParcelableArrayList(name, type);
    }

    public java.io.Serializable getSerializableExtra(String name)
    {
        return mExtras == null ? null : mExtras.getSerializable(name);
    }

    public <T extends java.io.Serializable> T getSerializableExtra(String name, Class<T> type)
    {
        return mExtras == null ? null : mExtras.getSerializable(name, type);
    }

    public ArrayList<Integer> getIntegerArrayListExtra(String name)
    {
        return mExtras == null ? null : mExtras.getIntegerArrayList(name);
    }

    public ArrayList<String> getStringArrayListExtra(String name)
    {
        return mExtras == null ? null : mExtras.getStringArrayList(name);
    }

    public ArrayList<CharSequence> getCharSequenceArrayListExtra(String name)
    {
        return mExtras == null ? null : mExtras.getCharSequenceArrayList(name);
    }

    public boolean[] getBooleanArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getBooleanArray(name);
    }

    public byte[] getByteArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getByteArray(name);
    }

    public short[] getShortArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getShortArray(name);
    }

    public char[] getCharArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getCharArray(name);
    }

    public int[] getIntArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getIntArray(name);
    }

    public long[] getLongArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getLongArray(name);
    }

    public float[] getFloatArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getFloatArray(name);
    }

    public double[] getDoubleArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getDoubleArray(name);
    }

    public String[] getStringArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getStringArray(name);
    }

    public CharSequence[] getCharSequenceArrayExtra(String name)
    {
        return mExtras == null ? null : mExtras.getCharSequenceArray(name);
    }

    public Bundle getBundleExtra(String name)
    {
        return mExtras == null ? null : mExtras.getBundle(name);
    }

    public Bundle getExtras()
    {
        return mExtras == null ? null : new Bundle(mExtras);
    }

    public int getFlags()
    {
        return mFlags;
    }

    public String getPackage()
    {
        return mPackage;
    }

    public ComponentName getComponent()
    {
        return mComponent;
    }

    public android.graphics.Rect getSourceBounds()
    {
        return mSourceBounds;
    }

    public ComponentName resolveActivity(android.content.pm.PackageManager packageManager)
    {
        if (mComponent != null) return mComponent;

        android.content.pm.ResolveInfo info = packageManager.resolveActivity(this, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null) return null;

        String packageName = info.activityInfo.applicationInfo != null ? info.activityInfo.applicationInfo.packageName
                                                                      : info.activityInfo.packageName;
        return new ComponentName(packageName, info.activityInfo.name);
    }

    public android.content.pm.ActivityInfo resolveActivityInfo(android.content.pm.PackageManager packageManager, int flags)
    {
        if (mComponent != null)
        {
            try
            {
                return packageManager.getActivityInfo(mComponent, flags);
            }
            catch (android.content.pm.PackageManager.NameNotFoundException missing)
            {
                return null;
            }
        }

        android.content.pm.ResolveInfo info =
            packageManager.resolveActivity(this, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY | flags);
        return info == null ? null : info.activityInfo;
    }

    public Intent setAction(String action)
    {
        mAction = action != null ? action.intern() : null;
        return this;
    }

    public Intent setData(Uri data)
    {
        mData = data;
        mType = null;
        return this;
    }

    public Intent setDataAndNormalize(Uri data)
    {
        return setData(data.normalizeScheme());
    }

    public Intent setType(String type)
    {
        mData = null;
        mType = type;
        return this;
    }

    public Intent setTypeAndNormalize(String type)
    {
        return setType(normalizeMimeType(type));
    }

    public Intent setDataAndType(Uri data, String type)
    {
        mData = data;
        mType = type;
        return this;
    }

    public Intent setDataAndTypeAndNormalize(Uri data, String type)
    {
        return setDataAndType(data.normalizeScheme(), normalizeMimeType(type));
    }

    public Intent setIdentifier(String identifier)
    {
        mIdentifier = identifier;
        return this;
    }

    public Intent addCategory(String category)
    {
        if (category == null) return this;

        if (mCategories == null) mCategories = new LinkedHashSet<>();
        mCategories.add(category.intern());
        return this;
    }

    public void removeCategory(String category)
    {
        if (mCategories == null) return;

        mCategories.remove(category);
        if (mCategories.isEmpty()) mCategories = null;
    }

    public void setSelector(Intent selector)
    {
        if (selector == this) throw new IllegalArgumentException("Intent being set as a selector of itself");
        if (selector != null && mPackage != null)
        {
            throw new IllegalArgumentException("Can't set selector when package name is already set");
        }

        mSelector = selector;
    }

    public void setClipData(ClipData clip)
    {
        mClipData = clip;
    }

    public Intent putExtra(String name, boolean value)
    {
        ensureExtras().putBoolean(name, value);
        return this;
    }

    public Intent putExtra(String name, byte value)
    {
        ensureExtras().putByte(name, value);
        return this;
    }

    public Intent putExtra(String name, char value)
    {
        ensureExtras().putChar(name, value);
        return this;
    }

    public Intent putExtra(String name, short value)
    {
        ensureExtras().putShort(name, value);
        return this;
    }

    public Intent putExtra(String name, int value)
    {
        ensureExtras().putInt(name, value);
        return this;
    }

    public Intent putExtra(String name, long value)
    {
        ensureExtras().putLong(name, value);
        return this;
    }

    public Intent putExtra(String name, float value)
    {
        ensureExtras().putFloat(name, value);
        return this;
    }

    public Intent putExtra(String name, double value)
    {
        ensureExtras().putDouble(name, value);
        return this;
    }

    public Intent putExtra(String name, String value)
    {
        ensureExtras().putString(name, value);
        return this;
    }

    public Intent putExtra(String name, CharSequence value)
    {
        ensureExtras().putCharSequence(name, value);
        return this;
    }

    public Intent putExtra(String name, Parcelable value)
    {
        ensureExtras().putParcelable(name, value);
        return this;
    }

    public Intent putExtra(String name, Parcelable[] value)
    {
        ensureExtras().putParcelableArray(name, value);
        return this;
    }

    public Intent putParcelableArrayListExtra(String name, ArrayList<? extends Parcelable> value)
    {
        ensureExtras().putParcelableArrayList(name, value);
        return this;
    }

    public Intent putIntegerArrayListExtra(String name, ArrayList<Integer> value)
    {
        ensureExtras().putIntegerArrayList(name, value);
        return this;
    }

    public Intent putStringArrayListExtra(String name, ArrayList<String> value)
    {
        ensureExtras().putStringArrayList(name, value);
        return this;
    }

    public Intent putCharSequenceArrayListExtra(String name, ArrayList<CharSequence> value)
    {
        ensureExtras().putCharSequenceArrayList(name, value);
        return this;
    }

    public Intent putExtra(String name, java.io.Serializable value)
    {
        ensureExtras().putSerializable(name, value);
        return this;
    }

    public Intent putExtra(String name, boolean[] value)
    {
        ensureExtras().putBooleanArray(name, value);
        return this;
    }

    public Intent putExtra(String name, byte[] value)
    {
        ensureExtras().putByteArray(name, value);
        return this;
    }

    public Intent putExtra(String name, short[] value)
    {
        ensureExtras().putShortArray(name, value);
        return this;
    }

    public Intent putExtra(String name, char[] value)
    {
        ensureExtras().putCharArray(name, value);
        return this;
    }

    public Intent putExtra(String name, int[] value)
    {
        ensureExtras().putIntArray(name, value);
        return this;
    }

    public Intent putExtra(String name, long[] value)
    {
        ensureExtras().putLongArray(name, value);
        return this;
    }

    public Intent putExtra(String name, float[] value)
    {
        ensureExtras().putFloatArray(name, value);
        return this;
    }

    public Intent putExtra(String name, double[] value)
    {
        ensureExtras().putDoubleArray(name, value);
        return this;
    }

    public Intent putExtra(String name, String[] value)
    {
        ensureExtras().putStringArray(name, value);
        return this;
    }

    public Intent putExtra(String name, CharSequence[] value)
    {
        ensureExtras().putCharSequenceArray(name, value);
        return this;
    }

    public Intent putExtra(String name, Bundle value)
    {
        ensureExtras().putBundle(name, value);
        return this;
    }

    public Intent putExtras(Intent source)
    {
        if (source.mExtras == null) return this;

        if (mExtras == null) mExtras = new Bundle(source.mExtras);
        else mExtras.putAll(source.mExtras);

        return this;
    }

    public Intent putExtras(Bundle extras)
    {
        if (extras != null) ensureExtras().putAll(extras);
        return this;
    }

    public Intent replaceExtras(Intent source)
    {
        mExtras = source.mExtras != null ? new Bundle(source.mExtras) : null;
        return this;
    }

    public Intent replaceExtras(Bundle extras)
    {
        mExtras = extras != null ? new Bundle(extras) : null;
        return this;
    }

    public void removeExtra(String name)
    {
        if (mExtras == null) return;

        mExtras.remove(name);
        if (mExtras.size() == 0) mExtras = null;
    }

    public Intent setFlags(int flags)
    {
        mFlags = flags;
        return this;
    }

    public Intent addFlags(int flags)
    {
        mFlags |= flags;
        return this;
    }

    public void removeFlags(int flags)
    {
        mFlags &= ~flags;
    }

    public Intent setPackage(String packageName)
    {
        if (packageName != null && mSelector != null)
        {
            throw new IllegalArgumentException("Can't set package name when selector is already set");
        }

        mPackage = packageName;
        return this;
    }

    public Intent setComponent(ComponentName component)
    {
        mComponent = component;
        return this;
    }

    public Intent setClassName(Context packageContext, String className)
    {
        mComponent = new ComponentName(packageContext, className);
        return this;
    }

    public Intent setClassName(String packageName, String className)
    {
        mComponent = new ComponentName(packageName, className);
        return this;
    }

    public Intent setClass(Context packageContext, Class<?> target)
    {
        mComponent = new ComponentName(packageContext == null ? "" : packageContext.getPackageName(), target.getName());
        return this;
    }

    public void setSourceBounds(android.graphics.Rect bounds)
    {
        mSourceBounds = bounds != null ? new android.graphics.Rect(bounds) : null;
    }

    public int fillIn(Intent other, int flags)
    {
        int changes = 0;

        if (other.mAction != null && (mAction == null || (flags & FILL_IN_ACTION) != 0))
        {
            mAction = other.mAction;
            changes |= FILL_IN_ACTION;
        }

        if ((other.mData != null || other.mType != null)
            && ((mData == null && mType == null) || (flags & FILL_IN_DATA) != 0))
        {
            mData = other.mData;
            mType = other.mType;
            changes |= FILL_IN_DATA;
        }

        if (other.mIdentifier != null && (mIdentifier == null || (flags & FILL_IN_IDENTIFIER) != 0))
        {
            mIdentifier = other.mIdentifier;
            changes |= FILL_IN_IDENTIFIER;
        }

        if (other.mCategories != null && (mCategories == null || (flags & FILL_IN_CATEGORIES) != 0))
        {
            mCategories = new LinkedHashSet<>(other.mCategories);
            changes |= FILL_IN_CATEGORIES;
        }

        if (other.mPackage != null && (mPackage == null || (flags & FILL_IN_PACKAGE) != 0))
        {
            if (mSelector == null)
            {
                mPackage = other.mPackage;
                changes |= FILL_IN_PACKAGE;
            }
        }

        if (other.mSelector != null && (flags & FILL_IN_SELECTOR) != 0)
        {
            if (mPackage == null)
            {
                mSelector = new Intent(other.mSelector);
                mPackage = null;
                changes |= FILL_IN_SELECTOR;
            }
        }

        if (other.mClipData != null && (mClipData == null || (flags & FILL_IN_CLIP_DATA) != 0))
        {
            mClipData = other.mClipData;
            changes |= FILL_IN_CLIP_DATA;
        }

        if (other.mComponent != null && (flags & FILL_IN_COMPONENT) != 0)
        {
            mComponent = other.mComponent;
            changes |= FILL_IN_COMPONENT;
        }

        mFlags |= other.mFlags;

        if (other.mSourceBounds != null && (mSourceBounds == null || (flags & FILL_IN_SOURCE_BOUNDS) != 0))
        {
            mSourceBounds = new android.graphics.Rect(other.mSourceBounds);
            changes |= FILL_IN_SOURCE_BOUNDS;
        }

        if (mExtras == null)
        {
            if (other.mExtras != null) mExtras = new Bundle(other.mExtras);
        }
        else if (other.mExtras != null)
        {
            Bundle merged = new Bundle(other.mExtras);
            merged.putAll(mExtras);
            mExtras = merged;
        }

        return changes;
    }

    public boolean filterEquals(Intent other)
    {
        if (other == null) return false;
        if (!Objects.equals(mAction, other.mAction)) return false;
        if (!Objects.equals(mData, other.mData)) return false;
        if (!Objects.equals(mType, other.mType)) return false;
        if (!Objects.equals(mIdentifier, other.mIdentifier)) return false;
        if (!Objects.equals(mPackage, other.mPackage)) return false;
        if (!Objects.equals(mComponent, other.mComponent)) return false;

        return Objects.equals(mCategories, other.mCategories);
    }

    public int filterHashCode()
    {
        int code = 0;
        if (mAction != null) code += mAction.hashCode();
        if (mData != null) code += mData.hashCode();
        if (mType != null) code += mType.hashCode();
        if (mIdentifier != null) code += mIdentifier.hashCode();
        if (mPackage != null) code += mPackage.hashCode();
        if (mComponent != null) code += mComponent.hashCode();
        if (mCategories != null) code += mCategories.hashCode();
        return code;
    }

    @Override
    public String toString()
    {
        StringBuilder described = new StringBuilder(128);
        described.append("Intent { ");
        toShortString(described, true, true, true, false);
        described.append(" }");
        return described.toString();
    }

    public String toShortString(boolean secure, boolean component, boolean extras, boolean clip)
    {
        StringBuilder described = new StringBuilder(128);
        toShortString(described, secure, component, extras, clip);
        return described.toString();
    }

    private void toShortString(StringBuilder described, boolean secure, boolean component, boolean extras, boolean clip)
    {
        boolean first = true;

        if (mAction != null)
        {
            described.append("act=").append(mAction);
            first = false;
        }

        if (mCategories != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("cat=[");

            boolean firstCategory = true;
            for (String category : mCategories)
            {
                if (!firstCategory) described.append(',');
                described.append(category);
                firstCategory = false;
            }

            described.append(']');
        }

        if (mData != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("dat=").append(secure ? safeData() : mData.toString());
        }

        if (mType != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("typ=").append(mType);
        }

        if (mIdentifier != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("id=").append(mIdentifier);
        }

        if (mFlags != 0)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("flg=0x").append(Integer.toHexString(mFlags));
        }

        if (mPackage != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("pkg=").append(mPackage);
        }

        if (component && mComponent != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("cmp=").append(mComponent.flattenToShortString());
        }

        if (mSourceBounds != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("bnds=").append(mSourceBounds.toShortString());
        }

        if (mClipData != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append(clip ? "clip={" + mClipData + "}" : "(has clip)");
        }

        if (extras && mExtras != null)
        {
            if (!first) described.append(' ');
            first = false;
            described.append("(has extras)");
        }

        if (mSelector != null)
        {
            if (!first) described.append(' ');
            described.append("sel=");
            mSelector.toShortString(described, secure, component, extras, clip);
        }
    }

    private String safeData()
    {
        String scheme = mData.getScheme();
        if (scheme == null) return "";
        if ("http".equals(scheme) || "https".equals(scheme) || "content".equals(scheme) || "file".equals(scheme))
        {
            String host = mData.getHost();
            return scheme + "://" + (host == null ? "" : host) + "/...";
        }

        return scheme + ":...";
    }

    @Deprecated
    public String toURI()
    {
        return toUri(0);
    }

    public String toUri(int flags)
    {
        StringBuilder uri = new StringBuilder(128);

        if ((flags & URI_ANDROID_APP_SCHEME) != 0)
        {
            if (mPackage == null)
            {
                throw new IllegalArgumentException("Intent must include an explicit package name to build an android-app: " + this);
            }

            uri.append("android-app://").append(mPackage);
            String scheme = null;

            if (mData != null)
            {
                scheme = mData.getScheme();
                if (scheme != null)
                {
                    uri.append('/').append(scheme);
                    String authority = mData.getEncodedAuthority();

                    if (authority != null)
                    {
                        uri.append('/').append(authority);
                        String path = mData.getEncodedPath();
                        if (path != null) uri.append(path);
                        String query = mData.getEncodedQuery();
                        if (query != null) uri.append('?').append(query);
                        String fragment = mData.getEncodedFragment();
                        if (fragment != null) uri.append('#').append(fragment);
                    }
                }
            }

            toUriFragment(uri, null, scheme == null ? ACTION_MAIN : ACTION_VIEW, mPackage, flags);
            return uri.toString();
        }

        String scheme = null;

        if (mData != null)
        {
            String data = mData.toString();

            if ((flags & URI_INTENT_SCHEME) != 0)
            {
                for (int index = 0; index < data.length(); index++)
                {
                    char character = data.charAt(index);
                    if ((character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
                        || (character >= '0' && character <= '9') || character == '.' || character == '-' || character == '+')
                    {
                        continue;
                    }

                    if (character == ':' && index > 0)
                    {
                        scheme = data.substring(0, index);
                        uri.append("intent:");
                        data = data.substring(index + 1);
                    }

                    break;
                }
            }

            uri.append(data);
        }
        else if ((flags & URI_INTENT_SCHEME) != 0)
        {
            uri.append("intent:");
        }

        toUriFragment(uri, scheme, ACTION_VIEW, null, flags);
        return uri.toString();
    }

    private void toUriFragment(StringBuilder uri, String scheme, String defaultAction, String defaultPackage, int flags)
    {
        StringBuilder fragment = new StringBuilder(128);
        toUriInner(fragment, scheme, defaultAction, defaultPackage, flags);

        if (mSelector != null)
        {
            fragment.append("SEL;");
            mSelector.toUriInner(fragment, mSelector.mData != null ? mSelector.mData.getScheme() : null, null, null, flags);
        }

        if (fragment.length() > 0)
        {
            uri.append("#Intent;");
            uri.append(fragment);
            uri.append("end");
        }
    }

    private void toUriInner(StringBuilder uri, String scheme, String defaultAction, String defaultPackage, int flags)
    {
        if (scheme != null) uri.append("scheme=").append(Uri.encode(scheme)).append(';');
        if (mAction != null && !mAction.equals(defaultAction)) uri.append("action=").append(Uri.encode(mAction)).append(';');

        if (mCategories != null)
        {
            for (String category : mCategories)
            {
                uri.append("category=").append(Uri.encode(category)).append(';');
            }
        }

        if (mType != null) uri.append("type=").append(Uri.encode(mType, "/")).append(';');
        if (mIdentifier != null) uri.append("identifier=").append(Uri.encode(mIdentifier, "/")).append(';');
        if (mFlags != 0) uri.append("launchFlags=0x").append(Integer.toHexString(mFlags)).append(';');
        if (mPackage != null && !mPackage.equals(defaultPackage)) uri.append("package=").append(Uri.encode(mPackage)).append(';');
        if (mComponent != null) uri.append("component=").append(Uri.encode(mComponent.flattenToShortString(), "/")).append(';');
        if (mSourceBounds != null) uri.append("sourceBounds=").append(Uri.encode(mSourceBounds.flattenToString())).append(';');

        if (mExtras == null) return;

        for (String key : mExtras.keySet())
        {
            Object value = mExtras.get(key);
            char entryType = value instanceof String ? 'S'
                           : value instanceof Boolean ? 'B'
                           : value instanceof Byte ? 'b'
                           : value instanceof Character ? 'c'
                           : value instanceof Double ? 'd'
                           : value instanceof Float ? 'f'
                           : value instanceof Integer ? 'i'
                           : value instanceof Long ? 'l'
                           : value instanceof Short ? 's'
                           : '\0';

            if (entryType == '\0') continue;

            uri.append(entryType).append('.').append(Uri.encode(key)).append('=').append(Uri.encode(value.toString())).append(';');
        }
    }

    @Override
    public int describeContents()
    {
        return mExtras != null && mExtras.hasFileDescriptors() ? Parcelable.CONTENTS_FILE_DESCRIPTOR : 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mAction);
        destination.writeValue(mData);
        destination.writeString(mType);
        destination.writeString(mIdentifier);
        destination.writeInt(mFlags);
        destination.writeString(mPackage);
        destination.writeValue(mComponent);
        destination.writeValue(mSourceBounds == null ? null : new android.graphics.Rect(mSourceBounds));
        destination.writeValue(mCategories == null ? null : new ArrayList<>(mCategories));
        destination.writeValue(mSelector == null ? null : new Intent(mSelector));
        destination.writeValue(mClipData);
        destination.writeBundle(mExtras == null ? null : new Bundle(mExtras));
    }

    @SuppressWarnings("unchecked")
    public void readFromParcel(Parcel source)
    {
        setAction(source.readString());
        mData = (Uri)source.readValue(null);
        mType = source.readString();
        mIdentifier = source.readString();
        mFlags = source.readInt();
        mPackage = source.readString();
        mComponent = (ComponentName)source.readValue(null);
        mSourceBounds = (android.graphics.Rect)source.readValue(null);

        Object categories = source.readValue(null);
        mCategories = categories == null ? null : new LinkedHashSet<>((ArrayList<String>)categories);

        mSelector = (Intent)source.readValue(null);
        mClipData = (ClipData)source.readValue(null);
        mExtras = source.readBundle();
    }

    private Bundle ensureExtras()
    {
        if (mExtras == null) mExtras = new Bundle();

        return mExtras;
    }

    public static final class FilterComparison
    {
        private final Intent mIntent;
        private final int mHashCode;

        public FilterComparison(Intent intent)
        {
            mIntent = intent;
            mHashCode = intent.filterHashCode();
        }

        public Intent getIntent()
        {
            return mIntent;
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof FilterComparison)) return false;

            return mIntent.filterEquals(((FilterComparison)other).mIntent);
        }

        @Override
        public int hashCode()
        {
            return mHashCode;
        }
    }

    public static class ShortcutIconResource implements Parcelable
    {
        public static final Parcelable.Creator<ShortcutIconResource> CREATOR = new Parcelable.Creator<ShortcutIconResource>()
        {
            @Override
            public ShortcutIconResource createFromParcel(Parcel source)
            {
                ShortcutIconResource icon = new ShortcutIconResource();
                icon.packageName = source.readString();
                icon.resourceName = source.readString();
                return icon;
            }

            @Override
            public ShortcutIconResource[] newArray(int size)
            {
                return new ShortcutIconResource[size];
            }
        };

        public String packageName;
        public String resourceName;

        public ShortcutIconResource()
        {
        }

        public static ShortcutIconResource fromContext(Context context, int resourceId)
        {
            ShortcutIconResource icon = new ShortcutIconResource();
            icon.packageName = context.getPackageName();
            icon.resourceName = context.getResources().getResourceName(resourceId);
            return icon;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags)
        {
            destination.writeString(packageName);
            destination.writeString(resourceName);
        }

        @Override
        public String toString()
        {
            return resourceName;
        }
    }
}
