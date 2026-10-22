package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public class FeatureInfo implements Parcelable
{
    public static final int FLAG_REQUIRED = 0x0001;
    public static final int GL_ES_VERSION_UNDEFINED = 0;

    public static final Parcelable.Creator<FeatureInfo> CREATOR = new Parcelable.Creator<FeatureInfo>()
    {
        @Override
        public FeatureInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof FeatureInfo ? new FeatureInfo((FeatureInfo)value) : new FeatureInfo();
        }

        @Override
        public FeatureInfo[] newArray(int size)
        {
            return new FeatureInfo[size];
        }
    };

    public String name;
    public int version;
    public int reqGlEsVersion;
    public int flags;

    public FeatureInfo()
    {
    }

    public FeatureInfo(FeatureInfo original)
    {
        if (original == null) return;

        name = original.name;
        version = original.version;
        reqGlEsVersion = original.reqGlEsVersion;
        flags = original.flags;
    }

    public String getGlEsVersion()
    {
        int major = (reqGlEsVersion & 0xffff0000) >> 16;
        int minor = reqGlEsVersion & 0x0000ffff;
        return major + "." + minor;
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
        String identity = Integer.toHexString(System.identityHashCode(this));
        String described = name != null ? name + " v=" + version : "glEsVers=" + getGlEsVersion();

        return "FeatureInfo{" + identity + " " + described + " fl=0x" + Integer.toHexString(flags) + "}";
    }
}
