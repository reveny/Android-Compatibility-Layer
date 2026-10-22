package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public final class InstallSourceInfo implements Parcelable
{
    public static final Parcelable.Creator<InstallSourceInfo> CREATOR = new Parcelable.Creator<InstallSourceInfo>()
    {
        @Override
        public InstallSourceInfo createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof InstallSourceInfo ? (InstallSourceInfo)value : new InstallSourceInfo(null, null, null, null, 0);
        }

        @Override
        public InstallSourceInfo[] newArray(int size)
        {
            return new InstallSourceInfo[size];
        }
    };

    private final String mInitiatingPackageName;
    private final String mOriginatingPackageName;
    private final String mInstallingPackageName;
    private final String mUpdateOwnerPackageName;
    private final int mPackageSource;

    InstallSourceInfo(String initiatingPackageName, String originatingPackageName, String installingPackageName,
                      String updateOwnerPackageName, int packageSource)
    {
        mInitiatingPackageName = initiatingPackageName;
        mOriginatingPackageName = originatingPackageName;
        mInstallingPackageName = installingPackageName;
        mUpdateOwnerPackageName = updateOwnerPackageName;
        mPackageSource = packageSource;
    }

    public String getInitiatingPackageName()
    {
        return mInitiatingPackageName;
    }

    public SigningInfo getInitiatingPackageSigningInfo()
    {
        return null;
    }

    public String getOriginatingPackageName()
    {
        return mOriginatingPackageName;
    }

    public String getInstallingPackageName()
    {
        return mInstallingPackageName;
    }

    public String getUpdateOwnerPackageName()
    {
        return mUpdateOwnerPackageName;
    }

    public int getPackageSource()
    {
        return mPackageSource;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(this);
    }
}
