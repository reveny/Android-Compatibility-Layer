package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class PackageInfo implements Parcelable
{
    public static final int INSTALL_LOCATION_AUTO = 0;
    public static final int INSTALL_LOCATION_INTERNAL_ONLY = 1;
    public static final int INSTALL_LOCATION_PREFER_EXTERNAL = 2;

    public static final int REQUESTED_PERMISSION_GRANTED = 0x00000002;
    public static final int REQUESTED_PERMISSION_IMPLICIT = 0x00000004;
    public static final int REQUESTED_PERMISSION_NEVER_FOR_LOCATION = 0x00010000;

    public static final Parcelable.Creator<PackageInfo> CREATOR = new Parcelable.Creator<PackageInfo>()
    {
        @Override
        public PackageInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof PackageInfo ? (PackageInfo)value : new PackageInfo();
        }

        @Override
        public PackageInfo[] newArray(int size)
        {
            return new PackageInfo[size];
        }
    };

    public String packageName;
    public String versionName;
    public int versionCode;
    public long longVersionCode;
    public int baseRevisionCode;
    public long firstInstallTime;
    public long lastUpdateTime;
    public int installLocation = INSTALL_LOCATION_INTERNAL_ONLY;
    public boolean isApex;
    public ApplicationInfo applicationInfo;
    public ActivityInfo[] activities;
    public ActivityInfo[] receivers;
    public ServiceInfo[] services;
    public ProviderInfo[] providers;
    public PermissionInfo[] permissions;
    public FeatureInfo[] reqFeatures;
    public ConfigurationInfo[] configPreferences;
    public String[] requestedPermissions;
    public int[] requestedPermissionsFlags;
    public int[] gids;
    public Signature[] signatures;
    public SigningInfo signingInfo;
    public String sharedUserId;
    public int sharedUserLabel;
    public String[] splitNames;
    public int[] splitRevisionCodes;

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
        return "PackageInfo{" + Integer.toHexString(System.identityHashCode(this)) + " " + packageName + "}";
    }
}
