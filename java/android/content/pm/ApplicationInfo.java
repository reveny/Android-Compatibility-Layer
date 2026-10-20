package android.content.pm;

public class ApplicationInfo extends PackageItemInfo
{
    public String className;
    public String appComponentFactory;
    public String sourceDir;
    public String publicSourceDir;
    public String dataDir;
    public String deviceProtectedDataDir;
    public String nativeLibraryDir;
    public String[] splitSourceDirs;
    public String[] splitPublicSourceDirs;
    public String[] splitNames;
    public int targetSdkVersion;
    public int minSdkVersion;
    public int flags;
    public int uid;
    public String storageUuid;
    public String processName;
    public boolean enabled = true;

    public static final int FLAG_SYSTEM = 1 << 0;
    public static final int FLAG_DEBUGGABLE = 1 << 1;
    public static final int FLAG_INSTALLED = 1 << 23;
    public static final int FLAG_EXTRACT_NATIVE_LIBS = 1 << 28;

    public CharSequence loadLabel(PackageManager packageManager)
    {
        if (packageManager != null) return packageManager.getApplicationLabel(this);

        return name != null ? name : packageName;
    }

    public android.graphics.drawable.Drawable loadIcon(PackageManager packageManager)
    {
        return packageManager == null ? null : packageManager.getApplicationIcon(this);
    }

    public ApplicationInfo()
    {
    }
}
