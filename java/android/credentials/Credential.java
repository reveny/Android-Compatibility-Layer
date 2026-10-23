package android.credentials;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class Credential implements Parcelable
{
    public static final String TYPE_PASSWORD_CREDENTIAL = "android.credentials.TYPE_PASSWORD_CREDENTIAL";

    public static final Parcelable.Creator<Credential> CREATOR = new Parcelable.Creator<Credential>()
    {
        @Override
        public Credential createFromParcel(Parcel source)
        {
            return new Credential(source.readString(), source.readBundle());
        }

        @Override
        public Credential[] newArray(int size)
        {
            return new Credential[size];
        }
    };

    private final String mType;
    private final Bundle mData;

    public Credential(String type, Bundle data)
    {
        mType = CredentialChecks.requireType(type);
        mData = Objects.requireNonNull(data, "data must not be null");
    }

    public String getType()
    {
        return mType;
    }

    public Bundle getData()
    {
        return mData;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mType);
        destination.writeBundle(mData);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "Credential {type=" + mType + ", data=" + mData + "}";
    }
}
