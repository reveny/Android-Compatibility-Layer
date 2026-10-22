package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class ProviderInfo extends ComponentInfo implements Parcelable
{
    public static final int FLAG_SINGLE_USER = 0x40000000;

    public static final Parcelable.Creator<ProviderInfo> CREATOR = new Parcelable.Creator<ProviderInfo>()
    {
        @Override
        public ProviderInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ProviderInfo ? new ProviderInfo((ProviderInfo)value) : new ProviderInfo();
        }

        @Override
        public ProviderInfo[] newArray(int size)
        {
            return new ProviderInfo[size];
        }
    };

    public String authority;
    public String readPermission;
    public String writePermission;
    public boolean multiprocess;
    public boolean grantUriPermissions;
    public boolean forceUriPermissions;
    public boolean isSyncable;
    public int initOrder;
    public int flags;

    public ProviderInfo()
    {
    }

    public ProviderInfo(ProviderInfo original)
    {
        super(original);
        if (original == null) return;

        authority = original.authority;
        readPermission = original.readPermission;
        writePermission = original.writePermission;
        multiprocess = original.multiprocess;
        grantUriPermissions = original.grantUriPermissions;
        forceUriPermissions = original.forceUriPermissions;
        isSyncable = original.isSyncable;
        initOrder = original.initOrder;
        flags = original.flags;
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
        return "ContentProviderInfo{name=" + authority + " className=" + name + "}";
    }
}
