package android.credentials;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class GetCredentialRequest implements Parcelable
{
    public static final Parcelable.Creator<GetCredentialRequest> CREATOR = new Parcelable.Creator<GetCredentialRequest>()
    {
        @Override
        public GetCredentialRequest createFromParcel(Parcel source)
        {
            List<CredentialOption> options = source.createTypedArrayList(CredentialOption.CREATOR);
            Bundle data = source.readBundle();
            String origin = source.readString();
            boolean alwaysSendAppInfo = source.readBoolean();
            return new GetCredentialRequest(options, data, origin, alwaysSendAppInfo);
        }

        @Override
        public GetCredentialRequest[] newArray(int size)
        {
            return new GetCredentialRequest[size];
        }
    };

    private final List<CredentialOption> mCredentialOptions;
    private final Bundle mData;
    private final String mOrigin;
    private final boolean mAlwaysSendAppInfoToProvider;

    private GetCredentialRequest(List<CredentialOption> credentialOptions, Bundle data, String origin, boolean alwaysSendAppInfoToProvider)
    {
        if (credentialOptions == null || credentialOptions.isEmpty())
        {
            throw new IllegalArgumentException("credentialOptions should not be empty");
        }

        mCredentialOptions = Collections.unmodifiableList(new ArrayList<>(credentialOptions));
        mData = Objects.requireNonNull(data, "data must not be null");
        mOrigin = origin;
        mAlwaysSendAppInfoToProvider = alwaysSendAppInfoToProvider;
    }

    public List<CredentialOption> getCredentialOptions()
    {
        return mCredentialOptions;
    }

    public Bundle getData()
    {
        return mData;
    }

    public String getOrigin()
    {
        return mOrigin;
    }

    public boolean alwaysSendAppInfoToProvider()
    {
        return mAlwaysSendAppInfoToProvider;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeTypedList(mCredentialOptions, flags);
        destination.writeBundle(mData);
        destination.writeString(mOrigin);
        destination.writeBoolean(mAlwaysSendAppInfoToProvider);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "GetCredentialRequest {credentialOption=" + mCredentialOptions + ", data=" + mData + ", origin=" + mOrigin
               + ", alwaysSendAppInfoToProvider=" + mAlwaysSendAppInfoToProvider + "}";
    }

    public static final class Builder
    {
        private final Bundle mData;
        private List<CredentialOption> mCredentialOptions = new ArrayList<>();
        private String mOrigin;
        private boolean mAlwaysSendAppInfoToProvider = true;

        public Builder(Bundle data)
        {
            mData = Objects.requireNonNull(data, "data must not be null");
        }

        public Builder addCredentialOption(CredentialOption credentialOption)
        {
            mCredentialOptions.add(Objects.requireNonNull(credentialOption, "credentialOption must not be null"));
            return this;
        }

        public Builder setAlwaysSendAppInfoToProvider(boolean value)
        {
            mAlwaysSendAppInfoToProvider = value;
            return this;
        }

        public Builder setCredentialOptions(List<CredentialOption> credentialOptions)
        {
            for (CredentialOption option : Objects.requireNonNull(credentialOptions, "credentialOptions must not be null"))
            {
                Objects.requireNonNull(option, "credentialOptions must not contain null");
            }

            mCredentialOptions = new ArrayList<>(credentialOptions);
            return this;
        }

        public Builder setOrigin(String origin)
        {
            mOrigin = origin;
            return this;
        }

        public GetCredentialRequest build()
        {
            return new GetCredentialRequest(mCredentialOptions, mData, mOrigin, mAlwaysSendAppInfoToProvider);
        }
    }
}
