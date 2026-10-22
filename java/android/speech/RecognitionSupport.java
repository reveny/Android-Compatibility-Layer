package android.speech;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecognitionSupport implements Parcelable
{
    public static final Parcelable.Creator<RecognitionSupport> CREATOR = new Parcelable.Creator<RecognitionSupport>()
    {
        @Override
        public RecognitionSupport createFromParcel(Parcel source)
        {
            Builder builder = new Builder();
            builder.setInstalledOnDeviceLanguages(source.createStringArrayList());
            builder.setPendingOnDeviceLanguages(source.createStringArrayList());
            builder.setSupportedOnDeviceLanguages(source.createStringArrayList());
            builder.setOnlineLanguages(source.createStringArrayList());
            return builder.build();
        }

        @Override
        public RecognitionSupport[] newArray(int size)
        {
            return new RecognitionSupport[size];
        }
    };

    private final List<String> mInstalledOnDeviceLanguages;
    private final List<String> mPendingOnDeviceLanguages;
    private final List<String> mSupportedOnDeviceLanguages;
    private final List<String> mOnlineLanguages;

    private RecognitionSupport(Builder builder)
    {
        mInstalledOnDeviceLanguages = new ArrayList<>(builder.mInstalledOnDeviceLanguages);
        mPendingOnDeviceLanguages = new ArrayList<>(builder.mPendingOnDeviceLanguages);
        mSupportedOnDeviceLanguages = new ArrayList<>(builder.mSupportedOnDeviceLanguages);
        mOnlineLanguages = new ArrayList<>(builder.mOnlineLanguages);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof RecognitionSupport)) return false;

        RecognitionSupport other = (RecognitionSupport)object;
        return mInstalledOnDeviceLanguages.equals(other.mInstalledOnDeviceLanguages)
            && mPendingOnDeviceLanguages.equals(other.mPendingOnDeviceLanguages)
            && mSupportedOnDeviceLanguages.equals(other.mSupportedOnDeviceLanguages) && mOnlineLanguages.equals(other.mOnlineLanguages);
    }

    public List<String> getInstalledOnDeviceLanguages()
    {
        return mInstalledOnDeviceLanguages;
    }

    public List<String> getOnlineLanguages()
    {
        return mOnlineLanguages;
    }

    public List<String> getPendingOnDeviceLanguages()
    {
        return mPendingOnDeviceLanguages;
    }

    public List<String> getSupportedOnDeviceLanguages()
    {
        return mSupportedOnDeviceLanguages;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mInstalledOnDeviceLanguages, mPendingOnDeviceLanguages, mSupportedOnDeviceLanguages, mOnlineLanguages);
    }

    @Override
    public String toString()
    {
        return "RecognitionSupport { installedOnDeviceLanguages = " + mInstalledOnDeviceLanguages + ", pendingOnDeviceLanguages = "
            + mPendingOnDeviceLanguages + ", supportedOnDeviceLanguages = " + mSupportedOnDeviceLanguages + ", onlineLanguages = "
            + mOnlineLanguages + " }";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeStringList(mInstalledOnDeviceLanguages);
        destination.writeStringList(mPendingOnDeviceLanguages);
        destination.writeStringList(mSupportedOnDeviceLanguages);
        destination.writeStringList(mOnlineLanguages);
    }

    public static final class Builder
    {
        private List<String> mInstalledOnDeviceLanguages = new ArrayList<>();
        private List<String> mPendingOnDeviceLanguages = new ArrayList<>();
        private List<String> mSupportedOnDeviceLanguages = new ArrayList<>();
        private List<String> mOnlineLanguages = new ArrayList<>();

        public Builder()
        {
        }

        public Builder addInstalledOnDeviceLanguage(String language)
        {
            mInstalledOnDeviceLanguages.add(Objects.requireNonNull(language));
            return this;
        }

        public Builder addOnlineLanguage(String language)
        {
            mOnlineLanguages.add(Objects.requireNonNull(language));
            return this;
        }

        public Builder addPendingOnDeviceLanguage(String language)
        {
            mPendingOnDeviceLanguages.add(Objects.requireNonNull(language));
            return this;
        }

        public Builder addSupportedOnDeviceLanguage(String language)
        {
            mSupportedOnDeviceLanguages.add(Objects.requireNonNull(language));
            return this;
        }

        public RecognitionSupport build()
        {
            return new RecognitionSupport(this);
        }

        public Builder setInstalledOnDeviceLanguages(List<String> languages)
        {
            mInstalledOnDeviceLanguages = new ArrayList<>(Objects.requireNonNull(languages));
            return this;
        }

        public Builder setOnlineLanguages(List<String> languages)
        {
            mOnlineLanguages = new ArrayList<>(Objects.requireNonNull(languages));
            return this;
        }

        public Builder setPendingOnDeviceLanguages(List<String> languages)
        {
            mPendingOnDeviceLanguages = new ArrayList<>(Objects.requireNonNull(languages));
            return this;
        }

        public Builder setSupportedOnDeviceLanguages(List<String> languages)
        {
            mSupportedOnDeviceLanguages = new ArrayList<>(Objects.requireNonNull(languages));
            return this;
        }
    }
}
