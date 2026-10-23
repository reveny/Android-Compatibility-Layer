package android.credentials;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CreateCredentialResponse implements Parcelable
{
    public static final Parcelable.Creator<CreateCredentialResponse> CREATOR = new Parcelable.Creator<CreateCredentialResponse>()
    {
        @Override
        public CreateCredentialResponse createFromParcel(Parcel source)
        {
            return new CreateCredentialResponse(source.readBundle());
        }

        @Override
        public CreateCredentialResponse[] newArray(int size)
        {
            return new CreateCredentialResponse[size];
        }
    };

    private final Bundle mData;

    public CreateCredentialResponse(Bundle data)
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
        return "CreateCredentialResponse {data=" + mData + "}";
    }
}
