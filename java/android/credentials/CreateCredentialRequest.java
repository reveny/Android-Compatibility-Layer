package android.credentials;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class CreateCredentialRequest implements Parcelable
{
    public static final Parcelable.Creator<CreateCredentialRequest> CREATOR = new Parcelable.Creator<CreateCredentialRequest>()
    {
        @Override
        public CreateCredentialRequest createFromParcel(Parcel source)
        {
            String type = source.readString();
            Bundle credentialData = source.readBundle();
            Bundle candidateQueryData = source.readBundle();
            boolean systemProviderRequired = source.readBoolean();
            boolean alwaysSendAppInfo = source.readBoolean();
            String origin = source.readString();
            return new CreateCredentialRequest(type, credentialData, candidateQueryData, systemProviderRequired, alwaysSendAppInfo, origin);
        }

        @Override
        public CreateCredentialRequest[] newArray(int size)
        {
            return new CreateCredentialRequest[size];
        }
    };

    private final String mType;
    private final Bundle mCredentialData;
    private final Bundle mCandidateQueryData;
    private final boolean mSystemProviderRequired;
    private final boolean mAlwaysSendAppInfoToProvider;
    private final String mOrigin;

    private CreateCredentialRequest(String type, Bundle credentialData, Bundle candidateQueryData, boolean systemProviderRequired,
                                    boolean alwaysSendAppInfoToProvider, String origin)
    {
        mType = CredentialChecks.requireType(type);
        mCredentialData = Objects.requireNonNull(credentialData, "credentialData must not be null");
        mCandidateQueryData = Objects.requireNonNull(candidateQueryData, "candidateQueryData must not be null");
        mSystemProviderRequired = systemProviderRequired;
        mAlwaysSendAppInfoToProvider = alwaysSendAppInfoToProvider;
        mOrigin = origin;
    }

    public String getType()
    {
        return mType;
    }

    public Bundle getCredentialData()
    {
        return mCredentialData;
    }

    public Bundle getCandidateQueryData()
    {
        return mCandidateQueryData;
    }

    public boolean isSystemProviderRequired()
    {
        return mSystemProviderRequired;
    }

    public boolean alwaysSendAppInfoToProvider()
    {
        return mAlwaysSendAppInfoToProvider;
    }

    public String getOrigin()
    {
        return mOrigin;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mType);
        destination.writeBundle(mCredentialData);
        destination.writeBundle(mCandidateQueryData);
        destination.writeBoolean(mSystemProviderRequired);
        destination.writeBoolean(mAlwaysSendAppInfoToProvider);
        destination.writeString(mOrigin);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "CreateCredentialRequest {type=" + mType + ", credentialData=" + mCredentialData + ", candidateQueryData="
               + mCandidateQueryData + ", isSystemProviderRequired=" + mSystemProviderRequired + ", alwaysSendAppInfoToProvider="
               + mAlwaysSendAppInfoToProvider + ", origin=" + mOrigin + "}";
    }

    public static final class Builder
    {
        private final String mType;
        private final Bundle mCredentialData;
        private final Bundle mCandidateQueryData;
        private boolean mSystemProviderRequired;
        private boolean mAlwaysSendAppInfoToProvider = true;
        private String mOrigin;

        public Builder(String type, Bundle credentialData, Bundle candidateQueryData)
        {
            mType = CredentialChecks.requireType(type);
            mCredentialData = Objects.requireNonNull(credentialData, "credentialData must not be null");
            mCandidateQueryData = Objects.requireNonNull(candidateQueryData, "candidateQueryData must not be null");
        }

        public Builder setAlwaysSendAppInfoToProvider(boolean value)
        {
            mAlwaysSendAppInfoToProvider = value;
            return this;
        }

        public Builder setIsSystemProviderRequired(boolean value)
        {
            mSystemProviderRequired = value;
            return this;
        }

        public Builder setOrigin(String origin)
        {
            mOrigin = origin;
            return this;
        }

        public CreateCredentialRequest build()
        {
            return new CreateCredentialRequest(mType, mCredentialData, mCandidateQueryData, mSystemProviderRequired,
                                               mAlwaysSendAppInfoToProvider, mOrigin);
        }
    }
}
