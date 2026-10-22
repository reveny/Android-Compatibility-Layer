package android.hardware.input;

import android.os.Parcel;
import android.os.Parcelable;

public final class HostUsiVersion implements Parcelable
{
    public static final Parcelable.Creator<HostUsiVersion> CREATOR = new Parcelable.Creator<HostUsiVersion>()
    {
        @Override
        public HostUsiVersion createFromParcel(Parcel source)
        {
            int major = source.readInt();
            int minor = source.readInt();
            return new HostUsiVersion(major, minor);
        }

        @Override
        public HostUsiVersion[] newArray(int size)
        {
            return new HostUsiVersion[size];
        }
    };

    private final int mMajorVersion;
    private final int mMinorVersion;

    HostUsiVersion(int majorVersion, int minorVersion)
    {
        mMajorVersion = majorVersion;
        mMinorVersion = minorVersion;
    }

    public int getMajorVersion()
    {
        return mMajorVersion;
    }

    public int getMinorVersion()
    {
        return mMinorVersion;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof HostUsiVersion)) return false;

        HostUsiVersion version = (HostUsiVersion)other;
        return mMajorVersion == version.mMajorVersion && mMinorVersion == version.mMinorVersion;
    }

    @Override
    public int hashCode()
    {
        return 31 * mMajorVersion + mMinorVersion;
    }

    @Override
    public String toString()
    {
        return "HostUsiVersion { majorVersion = " + mMajorVersion + ", minorVersion = " + mMinorVersion + " }";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mMajorVersion);
        dest.writeInt(mMinorVersion);
    }
}
