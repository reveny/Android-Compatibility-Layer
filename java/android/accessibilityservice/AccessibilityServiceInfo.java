package android.accessibilityservice;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcel;
import android.os.Parcelable;

public class AccessibilityServiceInfo implements Parcelable
{
    public static final int CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT = 1;
    public static final int CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION = 2;
    public static final int CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY = 4;
    public static final int CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS = 8;
    public static final int CAPABILITY_CAN_CONTROL_MAGNIFICATION = 16;
    public static final int CAPABILITY_CAN_PERFORM_GESTURES = 32;
    public static final int CAPABILITY_CAN_REQUEST_FINGERPRINT_GESTURES = 64;
    public static final int CAPABILITY_CAN_TAKE_SCREENSHOT = 128;
    public static final int DEFAULT = 1;
    public static final int FEEDBACK_ALL_MASK = -1;
    public static final int FEEDBACK_SPOKEN = 1;
    public static final int FEEDBACK_HAPTIC = 2;
    public static final int FEEDBACK_AUDIBLE = 4;
    public static final int FEEDBACK_VISUAL = 8;
    public static final int FEEDBACK_GENERIC = 16;
    public static final int FEEDBACK_BRAILLE = 32;
    public static final int FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 2;
    public static final int FLAG_REQUEST_TOUCH_EXPLORATION_MODE = 4;
    public static final int FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY = 8;
    public static final int FLAG_REPORT_VIEW_IDS = 16;
    public static final int FLAG_REQUEST_FILTER_KEY_EVENTS = 32;
    public static final int FLAG_RETRIEVE_INTERACTIVE_WINDOWS = 64;
    public static final int FLAG_ENABLE_ACCESSIBILITY_VOLUME = 128;
    public static final int FLAG_REQUEST_ACCESSIBILITY_BUTTON = 256;
    public static final int FLAG_REQUEST_FINGERPRINT_GESTURES = 512;
    public static final int FLAG_REQUEST_SHORTCUT_WARNING_DIALOG_SPOKEN_FEEDBACK = 1024;
    public static final int FLAG_SERVICE_HANDLES_DOUBLE_TAP = 2048;
    public static final int FLAG_REQUEST_MULTI_FINGER_GESTURES = 4096;
    public static final int FLAG_REQUEST_2_FINGER_PASSTHROUGH = 8192;
    public static final int FLAG_SEND_MOTION_EVENTS = 16384;
    public static final int FLAG_INPUT_METHOD_EDITOR = 32768;

    public int eventTypes;
    public String[] packageNames;
    public int feedbackType;
    public long notificationTimeout;
    public int flags;

    private String mId;
    private ResolveInfo mResolveInfo;
    private String mSettingsActivityName;
    private String mTileServiceName;
    private int mCapabilities;
    private int mMotionEventSources;
    private String mDescription;
    private int mNonInteractiveUiTimeout;
    private int mInteractiveUiTimeout;
    private boolean mIsAccessibilityTool;

    public AccessibilityServiceInfo()
    {
    }

    public String getId()
    {
        return mId;
    }

    public ResolveInfo getResolveInfo()
    {
        return mResolveInfo;
    }

    public String getSettingsActivityName()
    {
        return mSettingsActivityName;
    }

    public String getTileServiceName()
    {
        return mTileServiceName;
    }

    @Deprecated
    public boolean getCanRetrieveWindowContent()
    {
        return (mCapabilities & CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0;
    }

    public int getCapabilities()
    {
        return mCapabilities;
    }

    public int getMotionEventSources()
    {
        return mMotionEventSources;
    }

    public void setMotionEventSources(int motionEventSources)
    {
        mMotionEventSources = motionEventSources;
    }

    public CharSequence loadSummary(PackageManager packageManager)
    {
        return null;
    }

    public CharSequence loadIntro(PackageManager packageManager)
    {
        return null;
    }

    @Deprecated
    public String getDescription()
    {
        return mDescription;
    }

    public String loadDescription(PackageManager packageManager)
    {
        return mDescription;
    }

    public void setNonInteractiveUiTimeoutMillis(int timeout)
    {
        mNonInteractiveUiTimeout = timeout;
    }

    public int getNonInteractiveUiTimeoutMillis()
    {
        return mNonInteractiveUiTimeout;
    }

    public void setInteractiveUiTimeoutMillis(int timeout)
    {
        mInteractiveUiTimeout = timeout;
    }

    public int getInteractiveUiTimeoutMillis()
    {
        return mInteractiveUiTimeout;
    }

    public boolean isAccessibilityTool()
    {
        return mIsAccessibilityTool;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int parcelFlags)
    {
        parcel.writeInt(eventTypes);
        writeStrings(parcel, packageNames);
        parcel.writeInt(feedbackType);
        parcel.writeLong(notificationTimeout);
        parcel.writeInt(flags);
        parcel.writeString(mId);
        parcel.writeString(mSettingsActivityName);
        parcel.writeString(mTileServiceName);
        parcel.writeInt(mCapabilities);
        parcel.writeInt(mMotionEventSources);
        parcel.writeString(mDescription);
        parcel.writeInt(mNonInteractiveUiTimeout);
        parcel.writeInt(mInteractiveUiTimeout);
        parcel.writeInt(mIsAccessibilityTool ? 1 : 0);
    }

    private static void writeStrings(Parcel parcel, String[] values)
    {
        if (values == null)
        {
            parcel.writeInt(-1);
            return;
        }

        parcel.writeInt(values.length);
        for (String value : values)
        {
            parcel.writeString(value);
        }
    }

    private static String[] readStrings(Parcel parcel)
    {
        int count = parcel.readInt();
        if (count < 0) return null;

        String[] values = new String[count];
        for (int index = 0; index < count; index++)
        {
            values[index] = parcel.readString();
        }
        return values;
    }

    @Override
    public int hashCode()
    {
        return 31 + (mId == null ? 0 : mId.hashCode());
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof AccessibilityServiceInfo)) return false;

        AccessibilityServiceInfo info = (AccessibilityServiceInfo)other;
        return mId == null ? info.mId == null : mId.equals(info.mId);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("eventTypes: ").append(eventTypes);
        builder.append(", packageNames: ").append(packageNames == null ? "null" : java.util.Arrays.toString(packageNames));
        builder.append(", feedbackTypes: ").append(feedbackTypeToString(feedbackType));
        builder.append(", notificationTimeout: ").append(notificationTimeout);
        builder.append(", flags: ").append(flags);
        builder.append(", id: ").append(mId);
        builder.append(", capabilities: ").append(mCapabilities);
        return builder.toString();
    }

    public static String feedbackTypeToString(int feedbackType)
    {
        StringBuilder builder = new StringBuilder();
        builder.append('[');

        while (feedbackType != 0)
        {
            int feedbackTypeFlag = 1 << Integer.numberOfTrailingZeros(feedbackType);
            feedbackType &= ~feedbackTypeFlag;

            if (builder.length() > 1) builder.append(", ");

            switch (feedbackTypeFlag)
            {
                case FEEDBACK_AUDIBLE:
                    builder.append("FEEDBACK_AUDIBLE");
                    break;
                case FEEDBACK_HAPTIC:
                    builder.append("FEEDBACK_HAPTIC");
                    break;
                case FEEDBACK_GENERIC:
                    builder.append("FEEDBACK_GENERIC");
                    break;
                case FEEDBACK_SPOKEN:
                    builder.append("FEEDBACK_SPOKEN");
                    break;
                case FEEDBACK_VISUAL:
                    builder.append("FEEDBACK_VISUAL");
                    break;
                case FEEDBACK_BRAILLE:
                    builder.append("FEEDBACK_BRAILLE");
                    break;
                default:
                    break;
            }
        }

        builder.append(']');
        return builder.toString();
    }

    public static String flagToString(int flag)
    {
        switch (flag)
        {
            case DEFAULT:
                return "DEFAULT";
            case FLAG_INCLUDE_NOT_IMPORTANT_VIEWS:
                return "FLAG_INCLUDE_NOT_IMPORTANT_VIEWS";
            case FLAG_REQUEST_TOUCH_EXPLORATION_MODE:
                return "FLAG_REQUEST_TOUCH_EXPLORATION_MODE";
            case FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY:
                return "FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY";
            case FLAG_REPORT_VIEW_IDS:
                return "FLAG_REPORT_VIEW_IDS";
            case FLAG_REQUEST_FILTER_KEY_EVENTS:
                return "FLAG_REQUEST_FILTER_KEY_EVENTS";
            case FLAG_RETRIEVE_INTERACTIVE_WINDOWS:
                return "FLAG_RETRIEVE_INTERACTIVE_WINDOWS";
            case FLAG_ENABLE_ACCESSIBILITY_VOLUME:
                return "FLAG_ENABLE_ACCESSIBILITY_VOLUME";
            case FLAG_REQUEST_ACCESSIBILITY_BUTTON:
                return "FLAG_REQUEST_ACCESSIBILITY_BUTTON";
            case FLAG_REQUEST_FINGERPRINT_GESTURES:
                return "FLAG_REQUEST_FINGERPRINT_GESTURES";
            case FLAG_REQUEST_SHORTCUT_WARNING_DIALOG_SPOKEN_FEEDBACK:
                return "FLAG_REQUEST_SHORTCUT_WARNING_DIALOG_SPOKEN_FEEDBACK";
            case FLAG_SERVICE_HANDLES_DOUBLE_TAP:
                return "FLAG_SERVICE_HANDLES_DOUBLE_TAP";
            case FLAG_REQUEST_MULTI_FINGER_GESTURES:
                return "FLAG_REQUEST_MULTI_FINGER_GESTURES";
            case FLAG_REQUEST_2_FINGER_PASSTHROUGH:
                return "FLAG_REQUEST_2_FINGER_PASSTHROUGH";
            case FLAG_SEND_MOTION_EVENTS:
                return "FLAG_SEND_MOTION_EVENTS";
            case FLAG_INPUT_METHOD_EDITOR:
                return "FLAG_INPUT_METHOD_EDITOR";
            default:
                return null;
        }
    }

    public static String capabilityToString(int capability)
    {
        switch (capability)
        {
            case CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT:
                return "CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT";
            case CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION:
                return "CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION";
            case CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY:
                return "CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY";
            case CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS:
                return "CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS";
            case CAPABILITY_CAN_CONTROL_MAGNIFICATION:
                return "CAPABILITY_CAN_CONTROL_MAGNIFICATION";
            case CAPABILITY_CAN_PERFORM_GESTURES:
                return "CAPABILITY_CAN_PERFORM_GESTURES";
            case CAPABILITY_CAN_REQUEST_FINGERPRINT_GESTURES:
                return "CAPABILITY_CAN_REQUEST_FINGERPRINT_GESTURES";
            case CAPABILITY_CAN_TAKE_SCREENSHOT:
                return "CAPABILITY_CAN_TAKE_SCREENSHOT";
            default:
                return "UNKNOWN";
        }
    }

    public static final Creator<AccessibilityServiceInfo> CREATOR = new Creator<AccessibilityServiceInfo>()
    {
        @Override
        public AccessibilityServiceInfo createFromParcel(Parcel parcel)
        {
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.eventTypes = parcel.readInt();
            info.packageNames = readStrings(parcel);
            info.feedbackType = parcel.readInt();
            info.notificationTimeout = parcel.readLong();
            info.flags = parcel.readInt();
            info.mId = parcel.readString();
            info.mSettingsActivityName = parcel.readString();
            info.mTileServiceName = parcel.readString();
            info.mCapabilities = parcel.readInt();
            info.mMotionEventSources = parcel.readInt();
            info.mDescription = parcel.readString();
            info.mNonInteractiveUiTimeout = parcel.readInt();
            info.mInteractiveUiTimeout = parcel.readInt();
            info.mIsAccessibilityTool = parcel.readInt() != 0;
            return info;
        }

        @Override
        public AccessibilityServiceInfo[] newArray(int size)
        {
            return new AccessibilityServiceInfo[size];
        }
    };
}
