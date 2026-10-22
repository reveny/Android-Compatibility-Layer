package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class ApplicationInfo extends PackageItemInfo implements Parcelable
{
    public static final int FLAG_SYSTEM = 1 << 0;
    public static final int FLAG_DEBUGGABLE = 1 << 1;
    public static final int FLAG_HAS_CODE = 1 << 2;
    public static final int FLAG_PERSISTENT = 1 << 3;
    public static final int FLAG_FACTORY_TEST = 1 << 4;
    public static final int FLAG_ALLOW_TASK_REPARENTING = 1 << 5;
    public static final int FLAG_ALLOW_CLEAR_USER_DATA = 1 << 6;
    public static final int FLAG_UPDATED_SYSTEM_APP = 1 << 7;
    public static final int FLAG_TEST_ONLY = 1 << 8;
    public static final int FLAG_SUPPORTS_SMALL_SCREENS = 1 << 9;
    public static final int FLAG_SUPPORTS_NORMAL_SCREENS = 1 << 10;
    public static final int FLAG_SUPPORTS_LARGE_SCREENS = 1 << 11;
    public static final int FLAG_RESIZEABLE_FOR_SCREENS = 1 << 12;
    public static final int FLAG_SUPPORTS_SCREEN_DENSITIES = 1 << 13;
    public static final int FLAG_VM_SAFE_MODE = 1 << 14;
    public static final int FLAG_ALLOW_BACKUP = 1 << 15;
    public static final int FLAG_KILL_AFTER_RESTORE = 1 << 16;
    public static final int FLAG_RESTORE_ANY_VERSION = 1 << 17;
    public static final int FLAG_EXTERNAL_STORAGE = 1 << 18;
    public static final int FLAG_SUPPORTS_XLARGE_SCREENS = 1 << 19;
    public static final int FLAG_LARGE_HEAP = 1 << 20;
    public static final int FLAG_STOPPED = 1 << 21;
    public static final int FLAG_SUPPORTS_RTL = 1 << 22;
    public static final int FLAG_INSTALLED = 1 << 23;
    public static final int FLAG_IS_DATA_ONLY = 1 << 24;
    public static final int FLAG_IS_GAME = 1 << 25;
    public static final int FLAG_FULL_BACKUP_ONLY = 1 << 26;
    public static final int FLAG_USES_CLEARTEXT_TRAFFIC = 1 << 27;
    public static final int FLAG_EXTRACT_NATIVE_LIBS = 1 << 28;
    public static final int FLAG_HARDWARE_ACCELERATED = 1 << 29;
    public static final int FLAG_SUSPENDED = 1 << 30;
    public static final int FLAG_MULTIARCH = 1 << 31;

    public static final int CATEGORY_UNDEFINED = -1;
    public static final int CATEGORY_GAME = 0;
    public static final int CATEGORY_AUDIO = 1;
    public static final int CATEGORY_VIDEO = 2;
    public static final int CATEGORY_IMAGE = 3;
    public static final int CATEGORY_SOCIAL = 4;
    public static final int CATEGORY_NEWS = 5;
    public static final int CATEGORY_MAPS = 6;
    public static final int CATEGORY_PRODUCTIVITY = 7;
    public static final int CATEGORY_ACCESSIBILITY = 8;

    public static final int GWP_ASAN_DEFAULT = -1;
    public static final int GWP_ASAN_NEVER = 0;
    public static final int GWP_ASAN_ALWAYS = 1;

    public static final int MEMTAG_DEFAULT = -1;
    public static final int MEMTAG_OFF = 0;
    public static final int MEMTAG_ASYNC = 1;
    public static final int MEMTAG_SYNC = 2;

    public static final int ZEROINIT_DEFAULT = -1;
    public static final int ZEROINIT_DISABLED = 0;
    public static final int ZEROINIT_ENABLED = 1;

    public static final int RAW_EXTERNAL_STORAGE_ACCESS_DEFAULT = 0;
    public static final int RAW_EXTERNAL_STORAGE_ACCESS_REQUESTED = 1;
    public static final int RAW_EXTERNAL_STORAGE_ACCESS_NOT_REQUESTED = 2;

    public static final Parcelable.Creator<ApplicationInfo> CREATOR = new Parcelable.Creator<ApplicationInfo>()
    {
        @Override
        public ApplicationInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ApplicationInfo ? new ApplicationInfo((ApplicationInfo)value) : new ApplicationInfo();
        }

        @Override
        public ApplicationInfo[] newArray(int size)
        {
            return new ApplicationInfo[size];
        }
    };

    public String className;
    public String appComponentFactory;
    public String backupAgentName;
    public String manageSpaceActivityName;
    public String permission;
    public String taskAffinity;
    public String sourceDir;
    public String publicSourceDir;
    public String dataDir;
    public String deviceProtectedDataDir;
    public String nativeLibraryDir;
    public String nativeLibraryRootDir;
    public boolean nativeLibraryRootRequiresIsa;
    public String secondaryNativeLibraryDir;
    public String primaryCpuAbi;
    public String secondaryCpuAbi;
    public String credentialProtectedDataDir;
    public String scanSourceDir;
    public String scanPublicSourceDir;
    public String seInfo;
    public String seInfoUser;
    public String[] resourceDirs;
    public String[] overlayPaths;
    public int privateFlags;
    public long longVersionCode;
    public int versionCode;
    public String[] splitSourceDirs;
    public String[] splitPublicSourceDirs;
    public String[] splitNames;
    public String[] sharedLibraryFiles;
    public int targetSdkVersion;
    public int minSdkVersion;
    public int compileSdkVersion;
    public String compileSdkVersionCodename;
    public int flags;
    public int uid;
    public int theme;
    public int uiOptions;
    public int descriptionRes;
    public int category = CATEGORY_UNDEFINED;
    public int requiresSmallestWidthDp;
    public int compatibleWidthLimitDp;
    public int largestWidthLimitDp;
    public java.util.UUID storageUuid;
    public String processName;
    public boolean enabled = true;

    public ApplicationInfo()
    {
    }

    public ApplicationInfo(ApplicationInfo original)
    {
        super(original);
        if (original == null) return;

        className = original.className;
        appComponentFactory = original.appComponentFactory;
        backupAgentName = original.backupAgentName;
        manageSpaceActivityName = original.manageSpaceActivityName;
        permission = original.permission;
        taskAffinity = original.taskAffinity;
        sourceDir = original.sourceDir;
        publicSourceDir = original.publicSourceDir;
        dataDir = original.dataDir;
        deviceProtectedDataDir = original.deviceProtectedDataDir;
        nativeLibraryDir = original.nativeLibraryDir;
        nativeLibraryRootDir = original.nativeLibraryRootDir;
        nativeLibraryRootRequiresIsa = original.nativeLibraryRootRequiresIsa;
        secondaryNativeLibraryDir = original.secondaryNativeLibraryDir;
        primaryCpuAbi = original.primaryCpuAbi;
        secondaryCpuAbi = original.secondaryCpuAbi;
        credentialProtectedDataDir = original.credentialProtectedDataDir;
        scanSourceDir = original.scanSourceDir;
        scanPublicSourceDir = original.scanPublicSourceDir;
        seInfo = original.seInfo;
        seInfoUser = original.seInfoUser;
        resourceDirs = original.resourceDirs;
        overlayPaths = original.overlayPaths;
        privateFlags = original.privateFlags;
        longVersionCode = original.longVersionCode;
        versionCode = original.versionCode;
        splitSourceDirs = original.splitSourceDirs;
        splitPublicSourceDirs = original.splitPublicSourceDirs;
        splitNames = original.splitNames;
        sharedLibraryFiles = original.sharedLibraryFiles;
        targetSdkVersion = original.targetSdkVersion;
        minSdkVersion = original.minSdkVersion;
        compileSdkVersion = original.compileSdkVersion;
        compileSdkVersionCodename = original.compileSdkVersionCodename;
        flags = original.flags;
        uid = original.uid;
        theme = original.theme;
        uiOptions = original.uiOptions;
        descriptionRes = original.descriptionRes;
        category = original.category;
        requiresSmallestWidthDp = original.requiresSmallestWidthDp;
        compatibleWidthLimitDp = original.compatibleWidthLimitDp;
        largestWidthLimitDp = original.largestWidthLimitDp;
        storageUuid = original.storageUuid;
        processName = original.processName;
        enabled = original.enabled;
    }

    @Override
    public CharSequence loadLabel(PackageManager packageManager)
    {
        if (nonLocalizedLabel != null) return nonLocalizedLabel;
        if (packageManager != null) return packageManager.getApplicationLabel(this);

        return name != null ? name : packageName;
    }

    @Override
    public android.graphics.drawable.Drawable loadIcon(PackageManager packageManager)
    {
        return packageManager == null ? null : packageManager.getApplicationIcon(this);
    }

    public CharSequence loadDescription(PackageManager packageManager)
    {
        if (descriptionRes == 0 || packageManager == null) return null;

        return packageManager.getText(packageName, descriptionRes, this);
    }

    public static CharSequence getCategoryTitle(android.content.Context context, int category)
    {
        switch (category)
        {
            case CATEGORY_GAME:
                return "Games";
            case CATEGORY_AUDIO:
                return "Music & audio";
            case CATEGORY_VIDEO:
                return "Movies & video";
            case CATEGORY_IMAGE:
                return "Photos & images";
            case CATEGORY_SOCIAL:
                return "Social & communication";
            case CATEGORY_NEWS:
                return "News & magazines";
            case CATEGORY_MAPS:
                return "Maps & navigation";
            case CATEGORY_PRODUCTIVITY:
                return "Productivity";
            case CATEGORY_ACCESSIBILITY:
                return "Accessibility";
            default:
                return null;
        }
    }

    public int getRequestRawExternalStorageAccess()
    {
        return RAW_EXTERNAL_STORAGE_ACCESS_DEFAULT;
    }

    public boolean isVirtualPreload()
    {
        return false;
    }

    public boolean isProfileableByShell()
    {
        return (flags & FLAG_DEBUGGABLE) != 0;
    }

    public boolean isProfileable()
    {
        return true;
    }

    public boolean areAttributionsUserVisible()
    {
        return false;
    }

    public boolean isResourceOverlay()
    {
        return false;
    }

    public int getGwpAsanMode()
    {
        return GWP_ASAN_DEFAULT;
    }

    public int getMemtagMode()
    {
        return MEMTAG_DEFAULT;
    }

    public int getNativeHeapZeroInitialized()
    {
        return ZEROINIT_DEFAULT;
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
        return "ApplicationInfo{" + Integer.toHexString(System.identityHashCode(this)) + " " + packageName + "}";
    }
}
