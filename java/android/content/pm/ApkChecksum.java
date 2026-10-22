package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

public final class ApkChecksum implements Parcelable
{
    public static final Parcelable.Creator<ApkChecksum> CREATOR = new Parcelable.Creator<ApkChecksum>()
    {
        @Override
        public ApkChecksum createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ApkChecksum ? (ApkChecksum)value : new ApkChecksum(null, 0, new byte[0]);
        }

        @Override
        public ApkChecksum[] newArray(int size)
        {
            return new ApkChecksum[size];
        }
    };

    private final String mSplitName;
    private final int mType;
    private final byte[] mValue;

    ApkChecksum(String splitName, int type, byte[] value)
    {
        mSplitName = splitName;
        mType = type;
        mValue = value == null ? new byte[0] : value;
    }

    public int getType()
    {
        return mType;
    }

    public byte[] getValue()
    {
        return mValue.clone();
    }

    public java.security.cert.Certificate getInstallerCertificate() throws java.security.cert.CertificateException
    {
        return null;
    }

    public String getSplitName()
    {
        return mSplitName;
    }

    public String getInstallerPackageName()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return "ApkChecksum{" + mSplitName + " type=" + mType + "}";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(this);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }
}
