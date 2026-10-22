package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class PermissionInfo extends PackageItemInfo implements Parcelable
{
    public static final int PROTECTION_NORMAL = 0;
    public static final int PROTECTION_DANGEROUS = 1;
    public static final int PROTECTION_SIGNATURE = 2;
    public static final int PROTECTION_SIGNATURE_OR_SYSTEM = 3;
    public static final int PROTECTION_INTERNAL = 4;
    public static final int PROTECTION_FLAG_PRIVILEGED = 0x10;
    public static final int PROTECTION_FLAG_SYSTEM = 0x10;
    public static final int PROTECTION_FLAG_DEVELOPMENT = 0x20;
    public static final int PROTECTION_FLAG_APPOP = 0x40;
    public static final int PROTECTION_FLAG_PRE23 = 0x80;
    public static final int PROTECTION_FLAG_INSTALLER = 0x100;
    public static final int PROTECTION_FLAG_VERIFIER = 0x200;
    public static final int PROTECTION_FLAG_PREINSTALLED = 0x400;
    public static final int PROTECTION_FLAG_SETUP = 0x800;
    public static final int PROTECTION_FLAG_INSTANT = 0x1000;
    public static final int PROTECTION_FLAG_RUNTIME_ONLY = 0x2000;
    public static final int PROTECTION_MASK_BASE = 0xf;
    public static final int PROTECTION_MASK_FLAGS = 0xfff0;

    public static final int FLAG_COSTS_MONEY = 1 << 0;
    public static final int FLAG_HARD_RESTRICTED = 1 << 2;
    public static final int FLAG_SOFT_RESTRICTED = 1 << 3;
    public static final int FLAG_IMMUTABLY_RESTRICTED = 1 << 4;
    public static final int FLAG_INSTALLED = 1 << 30;

    public static final Parcelable.Creator<PermissionInfo> CREATOR = new Parcelable.Creator<PermissionInfo>()
    {
        @Override
        public PermissionInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof PermissionInfo ? new PermissionInfo((PermissionInfo)value) : new PermissionInfo();
        }

        @Override
        public PermissionInfo[] newArray(int size)
        {
            return new PermissionInfo[size];
        }
    };

    public int protectionLevel;
    public String group;
    public int flags;
    public int descriptionRes;
    public CharSequence nonLocalizedDescription;

    public PermissionInfo()
    {
    }

    public PermissionInfo(PermissionInfo original)
    {
        super(original);
        if (original == null) return;

        protectionLevel = original.protectionLevel;
        group = original.group;
        flags = original.flags;
        descriptionRes = original.descriptionRes;
        nonLocalizedDescription = original.nonLocalizedDescription;
    }

    public CharSequence loadDescription(PackageManager packageManager)
    {
        if (nonLocalizedDescription != null) return nonLocalizedDescription;
        if (descriptionRes == 0 || packageManager == null) return null;

        return packageManager.getText(packageName, descriptionRes, null);
    }

    public int getProtection()
    {
        return protectionLevel & PROTECTION_MASK_BASE;
    }

    public int getProtectionFlags()
    {
        return protectionLevel & ~PROTECTION_MASK_BASE;
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
        return "PermissionInfo{" + Integer.toHexString(System.identityHashCode(this)) + " " + name + "}";
    }
}
