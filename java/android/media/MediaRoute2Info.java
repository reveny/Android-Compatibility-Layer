package android.media;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MediaRoute2Info implements Parcelable
{
    public static final int CONNECTION_STATE_CONNECTED = 2;
    public static final int CONNECTION_STATE_CONNECTING = 1;
    public static final int CONNECTION_STATE_DISCONNECTED = 0;
    public static final String FEATURE_LIVE_AUDIO = "android.media.route.feature.LIVE_AUDIO";
    public static final String FEATURE_LIVE_VIDEO = "android.media.route.feature.LIVE_VIDEO";
    public static final String FEATURE_REMOTE_AUDIO_PLAYBACK = "android.media.route.feature.REMOTE_AUDIO_PLAYBACK";
    public static final String FEATURE_REMOTE_PLAYBACK = "android.media.route.feature.REMOTE_PLAYBACK";
    public static final String FEATURE_REMOTE_VIDEO_PLAYBACK = "android.media.route.feature.REMOTE_VIDEO_PLAYBACK";
    public static final int PLAYBACK_VOLUME_FIXED = 0;
    public static final int PLAYBACK_VOLUME_VARIABLE = 1;
    public static final int SUITABILITY_STATUS_NOT_SUITABLE_FOR_TRANSFER = 2;
    public static final int SUITABILITY_STATUS_SUITABLE_FOR_DEFAULT_TRANSFER = 0;
    public static final int SUITABILITY_STATUS_SUITABLE_FOR_MANUAL_TRANSFER = 1;
    public static final int TYPE_AUX_LINE = 19;
    public static final int TYPE_BLE_HEADSET = 26;
    public static final int TYPE_BLUETOOTH_A2DP = 8;
    public static final int TYPE_BUILTIN_SPEAKER = 2;
    public static final int TYPE_DOCK = 13;
    public static final int TYPE_GROUP = 2000;
    public static final int TYPE_HDMI = 9;
    public static final int TYPE_HDMI_ARC = 10;
    public static final int TYPE_HDMI_EARC = 29;
    public static final int TYPE_HEARING_AID = 23;
    public static final int TYPE_LINE_ANALOG = 5;
    public static final int TYPE_LINE_DIGITAL = 6;
    public static final int TYPE_MULTICHANNEL_SPEAKER_GROUP = 32;
    public static final int TYPE_REMOTE_AUDIO_VIDEO_RECEIVER = 1003;
    public static final int TYPE_REMOTE_CAR = 1008;
    public static final int TYPE_REMOTE_COMPUTER = 1006;
    public static final int TYPE_REMOTE_GAME_CONSOLE = 1007;
    public static final int TYPE_REMOTE_SMARTPHONE = 1010;
    public static final int TYPE_REMOTE_SMARTWATCH = 1009;
    public static final int TYPE_REMOTE_SPEAKER = 1002;
    public static final int TYPE_REMOTE_TABLET = 1004;
    public static final int TYPE_REMOTE_TABLET_DOCKED = 1005;
    public static final int TYPE_REMOTE_TV = 1001;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_USB_ACCESSORY = 12;
    public static final int TYPE_USB_DEVICE = 11;
    public static final int TYPE_USB_HEADSET = 22;
    public static final int TYPE_WIRED_HEADPHONES = 4;
    public static final int TYPE_WIRED_HEADSET = 3;

    public static final Parcelable.Creator<MediaRoute2Info> CREATOR = new Parcelable.Creator<MediaRoute2Info>()
    {
        @Override
        public MediaRoute2Info createFromParcel(Parcel source)
        {
            return new MediaRoute2Info(source);
        }

        @Override
        public MediaRoute2Info[] newArray(int size)
        {
            return new MediaRoute2Info[size];
        }
    };

    private final String mId;
    private final CharSequence mName;
    private final List<String> mFeatures;
    private final int mType;
    private final boolean mIsSystem;
    private final Uri mIconUri;
    private final CharSequence mDescription;
    private final int mConnectionState;
    private final String mClientPackageName;
    private final int mVolumeHandling;
    private final int mVolumeMax;
    private final int mVolume;
    private final Bundle mExtras;
    private final Set<String> mDeduplicationIds;
    private final int mSuitabilityStatus;
    private final boolean mIsVisibilityRestricted;
    private final Set<String> mAllowedPackages;

    private MediaRoute2Info(Builder builder)
    {
        mId = builder.mId;
        mName = builder.mName;
        mFeatures = Collections.unmodifiableList(new ArrayList<>(builder.mFeatures));
        mType = builder.mType;
        mIsSystem = builder.mIsSystem;
        mIconUri = builder.mIconUri;
        mDescription = builder.mDescription;
        mConnectionState = builder.mConnectionState;
        mClientPackageName = builder.mClientPackageName;
        mVolumeHandling = builder.mVolumeHandling;
        mVolumeMax = builder.mVolumeMax;
        mVolume = builder.mVolume;
        mExtras = builder.mExtras;
        mDeduplicationIds = Collections.unmodifiableSet(new LinkedHashSet<>(builder.mDeduplicationIds));
        mSuitabilityStatus = builder.mSuitabilityStatus;
        mIsVisibilityRestricted = builder.mIsVisibilityRestricted;
        mAllowedPackages = Collections.unmodifiableSet(new LinkedHashSet<>(builder.mAllowedPackages));
    }

    private MediaRoute2Info(Parcel source)
    {
        mId = source.readString();
        mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mFeatures = Collections.unmodifiableList(source.createStringArrayList());
        mType = source.readInt();
        mIsSystem = source.readBoolean();
        mIconUri = source.readTypedObject(Uri.CREATOR);
        mDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mConnectionState = source.readInt();
        mClientPackageName = source.readString();
        mVolumeHandling = source.readInt();
        mVolumeMax = source.readInt();
        mVolume = source.readInt();
        mExtras = source.readBundle();
        mDeduplicationIds = Collections.unmodifiableSet(new LinkedHashSet<>(source.createStringArrayList()));
        mSuitabilityStatus = source.readInt();
        mIsVisibilityRestricted = source.readBoolean();
        mAllowedPackages = Collections.unmodifiableSet(new LinkedHashSet<>(source.createStringArrayList()));
    }

    static MediaRoute2Info createDeviceSpeakerRoute(int volume, int volumeMax)
    {
        Builder builder = new Builder("DEVICE_ROUTE", "Device speaker");
        builder.setType(TYPE_BUILTIN_SPEAKER);
        builder.addFeature(FEATURE_LIVE_AUDIO);
        builder.addFeature(FEATURE_LIVE_VIDEO);
        builder.setVolumeHandling(PLAYBACK_VOLUME_VARIABLE);
        builder.setVolumeMax(volumeMax);
        builder.setVolume(volume);
        builder.setConnectionState(CONNECTION_STATE_CONNECTED);
        builder.mIsSystem = true;
        return builder.build();
    }

    public String getId()
    {
        return mId;
    }

    public CharSequence getName()
    {
        return mName;
    }

    public CharSequence getDescription()
    {
        return mDescription;
    }

    public int getConnectionState()
    {
        return mConnectionState;
    }

    public Uri getIconUri()
    {
        return mIconUri;
    }

    public String getClientPackageName()
    {
        return mClientPackageName;
    }

    public List<String> getFeatures()
    {
        return mFeatures;
    }

    public int getType()
    {
        return mType;
    }

    public boolean isSystemRoute()
    {
        return mIsSystem;
    }

    public int getVolumeHandling()
    {
        return mVolumeHandling;
    }

    public int getVolumeMax()
    {
        return mVolumeMax;
    }

    public int getVolume()
    {
        return mVolume;
    }

    public Bundle getExtras()
    {
        return mExtras == null ? null : new Bundle(mExtras);
    }

    public Set<String> getDeduplicationIds()
    {
        return mDeduplicationIds;
    }

    public int getSuitabilityStatus()
    {
        return mSuitabilityStatus;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mId);
        TextUtils.writeToParcel(mName, dest, flags);
        dest.writeStringList(mFeatures);
        dest.writeInt(mType);
        dest.writeBoolean(mIsSystem);
        dest.writeTypedObject(mIconUri, flags);
        TextUtils.writeToParcel(mDescription, dest, flags);
        dest.writeInt(mConnectionState);
        dest.writeString(mClientPackageName);
        dest.writeInt(mVolumeHandling);
        dest.writeInt(mVolumeMax);
        dest.writeInt(mVolume);
        dest.writeBundle(mExtras);
        dest.writeStringList(new ArrayList<>(mDeduplicationIds));
        dest.writeInt(mSuitabilityStatus);
        dest.writeBoolean(mIsVisibilityRestricted);
        dest.writeStringList(new ArrayList<>(mAllowedPackages));
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) return true;
        if (!(object instanceof MediaRoute2Info)) return false;

        MediaRoute2Info other = (MediaRoute2Info)object;

        return Objects.equals(mId, other.mId) && TextUtils.equals(mName, other.mName) && Objects.equals(mFeatures, other.mFeatures)
                && mType == other.mType && mIsSystem == other.mIsSystem && Objects.equals(mIconUri, other.mIconUri)
                && TextUtils.equals(mDescription, other.mDescription) && mConnectionState == other.mConnectionState
                && Objects.equals(mClientPackageName, other.mClientPackageName) && mVolumeHandling == other.mVolumeHandling
                && mVolumeMax == other.mVolumeMax && mVolume == other.mVolume
                && Objects.equals(mDeduplicationIds, other.mDeduplicationIds) && mSuitabilityStatus == other.mSuitabilityStatus
                && mIsVisibilityRestricted == other.mIsVisibilityRestricted && Objects.equals(mAllowedPackages, other.mAllowedPackages);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mId, String.valueOf(mName), mFeatures, mType, mIsSystem, mIconUri, String.valueOf(mDescription),
                mConnectionState, mClientPackageName, mVolumeHandling, mVolumeMax, mVolume, mDeduplicationIds, mSuitabilityStatus);
    }

    @Override
    public String toString()
    {
        return "MediaRoute2Info{ id=" + mId + ", name=" + mName + ", type=" + mType + ", features=" + mFeatures + ", isSystem="
                + mIsSystem + ", connectionState=" + mConnectionState + ", volumeHandling=" + mVolumeHandling + ", volume="
                + mVolume + ", volumeMax=" + mVolumeMax + ", suitabilityStatus=" + mSuitabilityStatus + " }";
    }

    public static final class Builder
    {
        private final String mId;
        private final CharSequence mName;
        private final List<String> mFeatures = new ArrayList<>();

        private int mType = TYPE_UNKNOWN;
        private boolean mIsSystem;
        private Uri mIconUri;
        private CharSequence mDescription;
        private int mConnectionState = CONNECTION_STATE_DISCONNECTED;
        private String mClientPackageName;
        private int mVolumeHandling = PLAYBACK_VOLUME_FIXED;
        private int mVolumeMax;
        private int mVolume;
        private Bundle mExtras;
        private Set<String> mDeduplicationIds = new LinkedHashSet<>();
        private int mSuitabilityStatus = SUITABILITY_STATUS_SUITABLE_FOR_DEFAULT_TRANSFER;
        private boolean mIsVisibilityRestricted;
        private Set<String> mAllowedPackages = new LinkedHashSet<>();

        public Builder(String id, CharSequence name)
        {
            if (TextUtils.isEmpty(id)) throw new IllegalArgumentException("id must not be empty");
            if (TextUtils.isEmpty(name)) throw new IllegalArgumentException("name must not be empty");

            mId = id;
            mName = name;
        }

        public Builder(MediaRoute2Info routeInfo)
        {
            if (routeInfo == null) throw new NullPointerException("routeInfo must not be null");

            mId = routeInfo.mId;
            mName = routeInfo.mName;
            mFeatures.addAll(routeInfo.mFeatures);
            mType = routeInfo.mType;
            mIsSystem = routeInfo.mIsSystem;
            mIconUri = routeInfo.mIconUri;
            mDescription = routeInfo.mDescription;
            mConnectionState = routeInfo.mConnectionState;
            mClientPackageName = routeInfo.mClientPackageName;
            mVolumeHandling = routeInfo.mVolumeHandling;
            mVolumeMax = routeInfo.mVolumeMax;
            mVolume = routeInfo.mVolume;
            mExtras = routeInfo.mExtras == null ? null : new Bundle(routeInfo.mExtras);
            mDeduplicationIds = new LinkedHashSet<>(routeInfo.mDeduplicationIds);
            mSuitabilityStatus = routeInfo.mSuitabilityStatus;
            mIsVisibilityRestricted = routeInfo.mIsVisibilityRestricted;
            mAllowedPackages = new LinkedHashSet<>(routeInfo.mAllowedPackages);
        }

        public Builder addFeature(String feature)
        {
            if (TextUtils.isEmpty(feature)) throw new IllegalArgumentException("feature must not be null or empty");

            mFeatures.add(feature);
            return this;
        }

        public Builder addFeatures(Collection<String> features)
        {
            if (features == null) throw new NullPointerException("features must not be null");

            for (String feature : features) addFeature(feature);

            return this;
        }

        public Builder clearFeatures()
        {
            mFeatures.clear();
            return this;
        }

        public Builder setType(int type)
        {
            mType = type;
            return this;
        }

        public Builder setIconUri(Uri iconUri)
        {
            mIconUri = iconUri;
            return this;
        }

        public Builder setDescription(CharSequence description)
        {
            mDescription = description;
            return this;
        }

        public Builder setConnectionState(int connectionState)
        {
            mConnectionState = connectionState;
            return this;
        }

        public Builder setClientPackageName(String packageName)
        {
            mClientPackageName = packageName;
            return this;
        }

        public Builder setVolumeHandling(int volumeHandling)
        {
            mVolumeHandling = volumeHandling;
            return this;
        }

        public Builder setVolumeMax(int volumeMax)
        {
            mVolumeMax = volumeMax;
            return this;
        }

        public Builder setVolume(int volume)
        {
            mVolume = volume;
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras == null ? null : new Bundle(extras);
            return this;
        }

        public Builder setDeduplicationIds(Set<String> id)
        {
            mDeduplicationIds = new LinkedHashSet<>(id);
            return this;
        }

        public Builder setSuitabilityStatus(int suitabilityStatus)
        {
            mSuitabilityStatus = suitabilityStatus;
            return this;
        }

        public Builder setVisibilityPublic()
        {
            mIsVisibilityRestricted = false;
            mAllowedPackages = new LinkedHashSet<>();
            return this;
        }

        public Builder setVisibilityRestricted(Set<String> allowedPackages)
        {
            mIsVisibilityRestricted = true;
            mAllowedPackages = new LinkedHashSet<>(allowedPackages);
            return this;
        }

        public MediaRoute2Info build()
        {
            if (mFeatures.isEmpty()) throw new IllegalArgumentException("features must not be empty!");

            return new MediaRoute2Info(this);
        }
    }
}
