package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class ActivityInfo extends ComponentInfo implements Parcelable
{
    public static final int SCREEN_ORIENTATION_UNSPECIFIED = -1;
    public static final int SCREEN_ORIENTATION_LANDSCAPE = 0;
    public static final int SCREEN_ORIENTATION_PORTRAIT = 1;
    public static final int SCREEN_ORIENTATION_USER = 2;
    public static final int SCREEN_ORIENTATION_BEHIND = 3;
    public static final int SCREEN_ORIENTATION_SENSOR = 4;
    public static final int SCREEN_ORIENTATION_NOSENSOR = 5;
    public static final int SCREEN_ORIENTATION_SENSOR_LANDSCAPE = 6;
    public static final int SCREEN_ORIENTATION_SENSOR_PORTRAIT = 7;
    public static final int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = 8;
    public static final int SCREEN_ORIENTATION_REVERSE_PORTRAIT = 9;
    public static final int SCREEN_ORIENTATION_FULL_SENSOR = 10;
    public static final int SCREEN_ORIENTATION_USER_LANDSCAPE = 11;
    public static final int SCREEN_ORIENTATION_USER_PORTRAIT = 12;
    public static final int SCREEN_ORIENTATION_FULL_USER = 13;
    public static final int SCREEN_ORIENTATION_LOCKED = 14;

    public static final int CONFIG_MCC = 0x0001;
    public static final int CONFIG_MNC = 0x0002;
    public static final int CONFIG_LOCALE = 0x0004;
    public static final int CONFIG_TOUCHSCREEN = 0x0008;
    public static final int CONFIG_KEYBOARD = 0x0010;
    public static final int CONFIG_KEYBOARD_HIDDEN = 0x0020;
    public static final int CONFIG_NAVIGATION = 0x0040;
    public static final int CONFIG_ORIENTATION = 0x0080;
    public static final int CONFIG_SCREEN_LAYOUT = 0x0100;
    public static final int CONFIG_UI_MODE = 0x0200;
    public static final int CONFIG_SCREEN_SIZE = 0x0400;
    public static final int CONFIG_SMALLEST_SCREEN_SIZE = 0x0800;
    public static final int CONFIG_DENSITY = 0x1000;
    public static final int CONFIG_LAYOUT_DIRECTION = 0x2000;
    public static final int CONFIG_COLOR_MODE = 0x4000;
    public static final int CONFIG_GRAMMATICAL_GENDER = 0x8000;
    public static final int CONFIG_FONT_SCALE = 0x40000000;
    public static final int CONFIG_FONT_WEIGHT_ADJUSTMENT = 0x10000000;

    public static final int COLOR_MODE_DEFAULT = 0;
    public static final int COLOR_MODE_WIDE_COLOR_GAMUT = 1;
    public static final int COLOR_MODE_HDR = 2;

    public static final int DOCUMENT_LAUNCH_NONE = 0;
    public static final int DOCUMENT_LAUNCH_INTO_EXISTING = 1;
    public static final int DOCUMENT_LAUNCH_ALWAYS = 2;
    public static final int DOCUMENT_LAUNCH_NEVER = 3;

    public static final int FLAG_MULTIPROCESS = 0x0001;
    public static final int FLAG_FINISH_ON_TASK_LAUNCH = 0x0002;
    public static final int FLAG_CLEAR_TASK_ON_LAUNCH = 0x0004;
    public static final int FLAG_ALWAYS_RETAIN_TASK_STATE = 0x0008;
    public static final int FLAG_STATE_NOT_NEEDED = 0x0010;
    public static final int FLAG_EXCLUDE_FROM_RECENTS = 0x0020;
    public static final int FLAG_ALLOW_TASK_REPARENTING = 0x0040;
    public static final int FLAG_NO_HISTORY = 0x0080;
    public static final int FLAG_FINISH_ON_CLOSE_SYSTEM_DIALOGS = 0x0100;
    public static final int FLAG_HARDWARE_ACCELERATED = 0x0200;
    public static final int FLAG_IMMERSIVE = 0x0800;
    public static final int FLAG_RELINQUISH_TASK_IDENTITY = 0x1000;
    public static final int FLAG_AUTO_REMOVE_FROM_RECENTS = 0x2000;
    public static final int FLAG_RESUME_WHILE_PAUSING = 0x4000;
    public static final int FLAG_ENABLE_VR_MODE = 0x8000;
    public static final int FLAG_PREFER_MINIMAL_POST_PROCESSING = 0x2000000;
    public static final int FLAG_ALLOW_UNTRUSTED_ACTIVITY_EMBEDDING = 0x10000000;
    public static final int FLAG_SINGLE_USER = 0x40000000;

    public static final int LAUNCH_MULTIPLE = 0;
    public static final int LAUNCH_SINGLE_TOP = 1;
    public static final int LAUNCH_SINGLE_TASK = 2;
    public static final int LAUNCH_SINGLE_INSTANCE = 3;
    public static final int LAUNCH_SINGLE_INSTANCE_PER_TASK = 4;

    public static final int PERSIST_ROOT_ONLY = 0;
    public static final int PERSIST_NEVER = 1;
    public static final int PERSIST_ACROSS_REBOOTS = 2;

    public static final int UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW = 1;

    public static final Parcelable.Creator<ActivityInfo> CREATOR = new Parcelable.Creator<ActivityInfo>()
    {
        @Override
        public ActivityInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ActivityInfo ? new ActivityInfo((ActivityInfo)value) : new ActivityInfo();
        }

        @Override
        public ActivityInfo[] newArray(int size)
        {
            return new ActivityInfo[size];
        }
    };

    public String parentActivityName;
    public String taskAffinity;
    public String permission;
    public String requiredDisplayCategory;
    public int theme;
    public int configChanges;
    public int launchMode;
    public int documentLaunchMode;
    public int persistableMode;
    public int maxRecents;
    public int flags;
    public int softInputMode;
    public int uiOptions;
    public int colorMode;
    public int screenOrientation = -1;
    public String targetActivity;

    public ActivityInfo()
    {
    }

    public ActivityInfo(ActivityInfo original)
    {
        super(original);
        if (original == null) return;

        parentActivityName = original.parentActivityName;
        taskAffinity = original.taskAffinity;
        permission = original.permission;
        requiredDisplayCategory = original.requiredDisplayCategory;
        theme = original.theme;
        configChanges = original.configChanges;
        launchMode = original.launchMode;
        documentLaunchMode = original.documentLaunchMode;
        persistableMode = original.persistableMode;
        maxRecents = original.maxRecents;
        flags = original.flags;
        softInputMode = original.softInputMode;
        uiOptions = original.uiOptions;
        colorMode = original.colorMode;
        screenOrientation = original.screenOrientation;
        targetActivity = original.targetActivity;
    }

    public final int getThemeResource()
    {
        if (theme != 0) return theme;

        return applicationInfo == null ? 0 : applicationInfo.theme;
    }

    public java.util.Set<String> getKnownActivityEmbeddingCerts()
    {
        return new java.util.HashSet<>();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags)
    {
        dest.writeValue(this);
    }

    @Override
    public String toString()
    {
        return "ActivityInfo{" + Integer.toHexString(System.identityHashCode(this)) + " " + name + "}";
    }
}
