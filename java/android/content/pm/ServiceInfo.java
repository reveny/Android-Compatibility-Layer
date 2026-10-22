package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceInfo extends ComponentInfo implements Parcelable
{
    public static final int FLAG_STOP_WITH_TASK = 0x0001;
    public static final int FLAG_ISOLATED_PROCESS = 0x0002;
    public static final int FLAG_EXTERNAL_SERVICE = 0x0004;
    public static final int FLAG_USE_APP_ZYGOTE = 0x0008;
    public static final int FLAG_ALLOW_SHARED_ISOLATED_PROCESS = 0x0010;
    public static final int FLAG_SINGLE_USER = 0x40000000;

    public static final int FOREGROUND_SERVICE_TYPE_MANIFEST = -1;
    public static final int FOREGROUND_SERVICE_TYPE_NONE = 0;
    public static final int FOREGROUND_SERVICE_TYPE_DATA_SYNC = 1;
    public static final int FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK = 2;
    public static final int FOREGROUND_SERVICE_TYPE_PHONE_CALL = 4;
    public static final int FOREGROUND_SERVICE_TYPE_LOCATION = 8;
    public static final int FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE = 16;
    public static final int FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION = 32;
    public static final int FOREGROUND_SERVICE_TYPE_CAMERA = 64;
    public static final int FOREGROUND_SERVICE_TYPE_MICROPHONE = 128;
    public static final int FOREGROUND_SERVICE_TYPE_HEALTH = 256;
    public static final int FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING = 512;
    public static final int FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED = 1024;
    public static final int FOREGROUND_SERVICE_TYPE_SHORT_SERVICE = 2048;
    public static final int FOREGROUND_SERVICE_TYPE_SPECIAL_USE = 0x40000000;

    public static final Parcelable.Creator<ServiceInfo> CREATOR = new Parcelable.Creator<ServiceInfo>()
    {
        @Override
        public ServiceInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ServiceInfo ? new ServiceInfo((ServiceInfo)value) : new ServiceInfo();
        }

        @Override
        public ServiceInfo[] newArray(int size)
        {
            return new ServiceInfo[size];
        }
    };

    public int flags;
    public String permission;
    private int mForegroundServiceType;

    public ServiceInfo()
    {
    }

    public ServiceInfo(ServiceInfo original)
    {
        super(original);
        if (original == null) return;

        flags = original.flags;
        permission = original.permission;
        mForegroundServiceType = original.mForegroundServiceType;
    }

    public int getForegroundServiceType()
    {
        return mForegroundServiceType;
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
        return "ServiceInfo{" + Integer.toHexString(System.identityHashCode(this)) + " " + name + "}";
    }
}
