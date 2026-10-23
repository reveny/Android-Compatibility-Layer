package android.credentials;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class ClearCredentialStateRequest implements Parcelable
{
    public static final Parcelable.Creator<ClearCredentialStateRequest> CREATOR = new Parcelable.Creator<ClearCredentialStateRequest>()
    {
        @Override
        public ClearCredentialStateRequest createFromParcel(Parcel source)
        {
            return new ClearCredentialStateRequest(source.readBundle());
        }

        @Override
        public ClearCredentialStateRequest[] newArray(int size)
        {
            return new ClearCredentialStateRequest[size];
        }
    };

    private final Bundle mData;

    public ClearCredentialStateRequest(Bundle data)
    {
        mData = Objects.requireNonNull(data, "data must not be null");
    }

    public Bundle getData()
    {
        return mData;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
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
        return "ClearCredentialStateRequest {data=" + mData + "}";
    }
}
