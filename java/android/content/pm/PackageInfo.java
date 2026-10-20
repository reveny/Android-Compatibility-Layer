package android.content.pm;

public class PackageInfo
{
    public String packageName;
    public String versionName;
    public int versionCode;
    public long longVersionCode;
    public long firstInstallTime;
    public long lastUpdateTime;
    public ApplicationInfo applicationInfo;
    public ActivityInfo[] activities;
    public ProviderInfo[] providers;
    public String[] requestedPermissions;
    public int[] requestedPermissionsFlags;
    public Signature[] signatures;
    public SigningInfo signingInfo;
    public String sharedUserId;
    public String[] splitNames;
    public long[] splitRevisionCodes;

    public static final int REQUESTED_PERMISSION_GRANTED = 0x00000002;

    public PackageInfo()
    {
    }

    public long getLongVersionCode()
    {
        return longVersionCode;
    }

    public void setLongVersionCode(long value)
    {
        longVersionCode = value;
        versionCode = (int)value;
    }
}
