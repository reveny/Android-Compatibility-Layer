package android.credentials;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class GetCredentialResponse implements Parcelable
{
    public static final Parcelable.Creator<GetCredentialResponse> CREATOR = new Parcelable.Creator<GetCredentialResponse>()
    {
        @Override
        public GetCredentialResponse createFromParcel(Parcel source)
        {
            return new GetCredentialResponse(Credential.CREATOR.createFromParcel(source));
        }

        @Override
        public GetCredentialResponse[] newArray(int size)
        {
            return new GetCredentialResponse[size];
        }
    };

    private final Credential mCredential;

    public GetCredentialResponse(Credential credential)
    {
        mCredential = Objects.requireNonNull(credential, "credential must not be null");
    }

    public Credential getCredential()
    {
        return mCredential;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        mCredential.writeToParcel(destination, flags);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "GetCredentialResponse {credential=" + mCredential + "}";
    }
}
