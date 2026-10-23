package android.credentials;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class CredentialOption implements Parcelable
{
    public static final String SUPPORTED_ELEMENT_KEYS = "android.credentials.GetCredentialOption.SUPPORTED_ELEMENT_KEYS";

    public static final Parcelable.Creator<CredentialOption> CREATOR = new Parcelable.Creator<CredentialOption>()
    {
        @Override
        public CredentialOption createFromParcel(Parcel source)
        {
            String type = source.readString();
            Bundle retrievalData = source.readBundle();
            Bundle candidateQueryData = source.readBundle();
            boolean systemProviderRequired = source.readBoolean();

            Set<ComponentName> allowedProviders = new HashSet<>();
            int count = source.readInt();
            for (int index = 0; index < count; index++) allowedProviders.add(ComponentName.readFromParcel(source));

            return new CredentialOption(type, retrievalData, candidateQueryData, systemProviderRequired, allowedProviders);
        }

        @Override
        public CredentialOption[] newArray(int size)
        {
            return new CredentialOption[size];
        }
    };

    private final String mType;
    private final Bundle mCredentialRetrievalData;
    private final Bundle mCandidateQueryData;
    private final boolean mSystemProviderRequired;
    private final Set<ComponentName> mAllowedProviders;

    private CredentialOption(String type, Bundle credentialRetrievalData, Bundle candidateQueryData, boolean systemProviderRequired,
                             Set<ComponentName> allowedProviders)
    {
        mType = CredentialChecks.requireType(type);
        mCredentialRetrievalData = Objects.requireNonNull(credentialRetrievalData, "credentialRetrievalData must not be null");
        mCandidateQueryData = Objects.requireNonNull(candidateQueryData, "candidateQueryData must not be null");
        mSystemProviderRequired = systemProviderRequired;
        mAllowedProviders = Collections.unmodifiableSet(new HashSet<>(allowedProviders));
    }

    public String getType()
    {
        return mType;
    }

    public Bundle getCredentialRetrievalData()
    {
        return mCredentialRetrievalData;
    }

    public Bundle getCandidateQueryData()
    {
        return mCandidateQueryData;
    }

    public boolean isSystemProviderRequired()
    {
        return mSystemProviderRequired;
    }

    public Set<ComponentName> getAllowedProviders()
    {
        return mAllowedProviders;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeString(mType);
        destination.writeBundle(mCredentialRetrievalData);
        destination.writeBundle(mCandidateQueryData);
        destination.writeBoolean(mSystemProviderRequired);
        destination.writeInt(mAllowedProviders.size());
        for (ComponentName provider : mAllowedProviders) ComponentName.writeToParcel(provider, destination);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "CredentialOption {type=" + mType + ", requestData=" + mCredentialRetrievalData + ", candidateQueryData="
               + mCandidateQueryData + ", isSystemProviderRequired=" + mSystemProviderRequired + ", allowedProviders="
               + mAllowedProviders + "}";
    }

    public static final class Builder
    {
        private final String mType;
        private final Bundle mCredentialRetrievalData;
        private final Bundle mCandidateQueryData;
        private boolean mSystemProviderRequired;
        private Set<ComponentName> mAllowedProviders = new HashSet<>();

        public Builder(String type, Bundle credentialRetrievalData, Bundle candidateQueryData)
        {
            mType = CredentialChecks.requireType(type);
            mCredentialRetrievalData = Objects.requireNonNull(credentialRetrievalData, "credentialRetrievalData must not be null");
            mCandidateQueryData = Objects.requireNonNull(candidateQueryData, "candidateQueryData must not be null");
        }

        public Builder setIsSystemProviderRequired(boolean isSystemProviderRequired)
        {
            mSystemProviderRequired = isSystemProviderRequired;
            return this;
        }

        public Builder addAllowedProvider(ComponentName allowedProvider)
        {
            mAllowedProviders.add(Objects.requireNonNull(allowedProvider, "allowedProvider must not be null"));
            return this;
        }

        public Builder setAllowedProviders(Set<ComponentName> allowedProviders)
        {
            for (ComponentName provider : Objects.requireNonNull(allowedProviders, "allowedProviders must not be null"))
            {
                Objects.requireNonNull(provider, "allowedProviders must not contain null");
            }

            mAllowedProviders = new HashSet<>(allowedProviders);
            return this;
        }

        public CredentialOption build()
        {
            return new CredentialOption(mType, mCredentialRetrievalData, mCandidateQueryData, mSystemProviderRequired, mAllowedProviders);
        }
    }
}
