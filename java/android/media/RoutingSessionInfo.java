package android.media;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RoutingSessionInfo implements Parcelable
{
    public static final int TRANSFER_REASON_APP = 2;
    public static final int TRANSFER_REASON_FALLBACK = 0;
    public static final int TRANSFER_REASON_SYSTEM_REQUEST = 1;

    public static final Parcelable.Creator<RoutingSessionInfo> CREATOR = new Parcelable.Creator<RoutingSessionInfo>()
    {
        @Override
        public RoutingSessionInfo createFromParcel(Parcel source)
        {
            return new RoutingSessionInfo(source);
        }

        @Override
        public RoutingSessionInfo[] newArray(int size)
        {
            return new RoutingSessionInfo[size];
        }
    };

    private final String mId;
    private final CharSequence mName;
    private final String mClientPackageName;
    private final List<String> mSelectedRoutes;
    private final List<String> mSelectableRoutes;
    private final List<String> mDeselectableRoutes;
    private final List<String> mTransferableRoutes;
    private final int mVolumeHandling;
    private final int mVolumeMax;
    private final int mVolume;
    private final Bundle mControlHints;
    private final int mTransferReason;
    private final UserHandle mTransferInitiatorUserHandle;
    private final String mTransferInitiatorPackageName;
    private final boolean mIsSystemSession;

    private RoutingSessionInfo(Builder builder)
    {
        mId = builder.mId;
        mName = builder.mName;
        mClientPackageName = builder.mClientPackageName;
        mSelectedRoutes = Collections.unmodifiableList(new ArrayList<>(builder.mSelectedRoutes));
        mSelectableRoutes = Collections.unmodifiableList(new ArrayList<>(builder.mSelectableRoutes));
        mDeselectableRoutes = Collections.unmodifiableList(new ArrayList<>(builder.mDeselectableRoutes));
        mTransferableRoutes = Collections.unmodifiableList(new ArrayList<>(builder.mTransferableRoutes));
        mVolumeHandling = builder.mVolumeHandling;
        mVolumeMax = builder.mVolumeMax;
        mVolume = builder.mVolume;
        mControlHints = builder.mControlHints;
        mTransferReason = builder.mTransferReason;
        mTransferInitiatorUserHandle = builder.mTransferInitiatorUserHandle;
        mTransferInitiatorPackageName = builder.mTransferInitiatorPackageName;
        mIsSystemSession = builder.mIsSystemSession;
    }

    private RoutingSessionInfo(Parcel source)
    {
        mId = source.readString();
        mName = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mClientPackageName = source.readString();
        mSelectedRoutes = Collections.unmodifiableList(source.createStringArrayList());
        mSelectableRoutes = Collections.unmodifiableList(source.createStringArrayList());
        mDeselectableRoutes = Collections.unmodifiableList(source.createStringArrayList());
        mTransferableRoutes = Collections.unmodifiableList(source.createStringArrayList());
        mVolumeHandling = source.readInt();
        mVolumeMax = source.readInt();
        mVolume = source.readInt();
        mControlHints = source.readBundle();
        mTransferReason = source.readInt();
        mTransferInitiatorUserHandle = source.readTypedObject(UserHandle.CREATOR);
        mTransferInitiatorPackageName = source.readString();
        mIsSystemSession = source.readBoolean();
    }

    public String getId()
    {
        return mId;
    }

    public CharSequence getName()
    {
        return mName;
    }

    public String getClientPackageName()
    {
        return mClientPackageName;
    }

    public List<String> getSelectedRoutes()
    {
        return mSelectedRoutes;
    }

    public List<String> getSelectableRoutes()
    {
        return mSelectableRoutes;
    }

    public List<String> getDeselectableRoutes()
    {
        return mDeselectableRoutes;
    }

    public List<String> getTransferableRoutes()
    {
        return mTransferableRoutes;
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

    public Bundle getControlHints()
    {
        return mControlHints;
    }

    public int getTransferReason()
    {
        return mTransferReason;
    }

    UserHandle getTransferInitiatorUserHandle()
    {
        return mTransferInitiatorUserHandle;
    }

    String getTransferInitiatorPackageName()
    {
        return mTransferInitiatorPackageName;
    }

    boolean isSystemSession()
    {
        return mIsSystemSession;
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
        dest.writeString(mClientPackageName);
        dest.writeStringList(mSelectedRoutes);
        dest.writeStringList(mSelectableRoutes);
        dest.writeStringList(mDeselectableRoutes);
        dest.writeStringList(mTransferableRoutes);
        dest.writeInt(mVolumeHandling);
        dest.writeInt(mVolumeMax);
        dest.writeInt(mVolume);
        dest.writeBundle(mControlHints);
        dest.writeInt(mTransferReason);
        dest.writeTypedObject(mTransferInitiatorUserHandle, flags);
        dest.writeString(mTransferInitiatorPackageName);
        dest.writeBoolean(mIsSystemSession);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) return true;
        if (!(object instanceof RoutingSessionInfo)) return false;

        RoutingSessionInfo other = (RoutingSessionInfo)object;

        return Objects.equals(mId, other.mId) && TextUtils.equals(mName, other.mName)
                && Objects.equals(mClientPackageName, other.mClientPackageName) && Objects.equals(mSelectedRoutes, other.mSelectedRoutes)
                && Objects.equals(mSelectableRoutes, other.mSelectableRoutes)
                && Objects.equals(mDeselectableRoutes, other.mDeselectableRoutes)
                && Objects.equals(mTransferableRoutes, other.mTransferableRoutes) && mVolumeHandling == other.mVolumeHandling
                && mVolumeMax == other.mVolumeMax && mVolume == other.mVolume && mTransferReason == other.mTransferReason
                && Objects.equals(mTransferInitiatorUserHandle, other.mTransferInitiatorUserHandle)
                && Objects.equals(mTransferInitiatorPackageName, other.mTransferInitiatorPackageName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mId, String.valueOf(mName), mClientPackageName, mSelectedRoutes, mSelectableRoutes, mDeselectableRoutes,
                mTransferableRoutes, mVolumeMax, mVolumeHandling, mVolume, mTransferReason);
    }

    @Override
    public String toString()
    {
        return "RoutingSessionInfo{ sessionId=" + mId + ", name=" + mName + ", clientPackageName=" + mClientPackageName
                + ", selectedRoutes=" + mSelectedRoutes + ", selectableRoutes=" + mSelectableRoutes + ", deselectableRoutes="
                + mDeselectableRoutes + ", transferableRoutes=" + mTransferableRoutes + ", volumeHandling=" + mVolumeHandling
                + ", volumeMax=" + mVolumeMax + ", volume=" + mVolume + ", transferReason=" + mTransferReason + " }";
    }

    public static final class Builder
    {
        private final String mId;
        private final List<String> mSelectedRoutes;
        private final List<String> mSelectableRoutes;
        private final List<String> mDeselectableRoutes;
        private final List<String> mTransferableRoutes;

        private CharSequence mName;
        private String mClientPackageName;
        private int mVolumeHandling = MediaRoute2Info.PLAYBACK_VOLUME_FIXED;
        private int mVolumeMax;
        private int mVolume;
        private Bundle mControlHints;
        private int mTransferReason = TRANSFER_REASON_FALLBACK;
        private UserHandle mTransferInitiatorUserHandle;
        private String mTransferInitiatorPackageName;
        private boolean mIsSystemSession;

        public Builder(String id, String clientPackageName)
        {
            if (TextUtils.isEmpty(id)) throw new IllegalArgumentException("id must not be empty");
            if (clientPackageName == null) throw new NullPointerException("clientPackageName must not be null");

            mId = id;
            mClientPackageName = clientPackageName;
            mSelectedRoutes = new ArrayList<>();
            mSelectableRoutes = new ArrayList<>();
            mDeselectableRoutes = new ArrayList<>();
            mTransferableRoutes = new ArrayList<>();
        }

        public Builder(RoutingSessionInfo sessionInfo)
        {
            if (sessionInfo == null) throw new NullPointerException("sessionInfo must not be null");

            mId = sessionInfo.mId;
            mName = sessionInfo.mName;
            mClientPackageName = sessionInfo.mClientPackageName;
            mSelectedRoutes = new ArrayList<>(sessionInfo.mSelectedRoutes);
            mSelectableRoutes = new ArrayList<>(sessionInfo.mSelectableRoutes);
            mDeselectableRoutes = new ArrayList<>(sessionInfo.mDeselectableRoutes);
            mTransferableRoutes = new ArrayList<>(sessionInfo.mTransferableRoutes);
            mVolumeHandling = sessionInfo.mVolumeHandling;
            mVolumeMax = sessionInfo.mVolumeMax;
            mVolume = sessionInfo.mVolume;
            mControlHints = sessionInfo.mControlHints;
            mTransferReason = sessionInfo.mTransferReason;
            mTransferInitiatorUserHandle = sessionInfo.mTransferInitiatorUserHandle;
            mTransferInitiatorPackageName = sessionInfo.mTransferInitiatorPackageName;
            mIsSystemSession = sessionInfo.mIsSystemSession;
        }

        Builder setSystemSession(boolean isSystemSession)
        {
            mIsSystemSession = isSystemSession;
            return this;
        }

        public Builder setName(CharSequence name)
        {
            mName = name;
            return this;
        }

        public Builder clearSelectedRoutes()
        {
            mSelectedRoutes.clear();
            return this;
        }

        public Builder addSelectedRoute(String routeId)
        {
            mSelectedRoutes.add(requireRouteId(routeId));
            return this;
        }

        public Builder removeSelectedRoute(String routeId)
        {
            mSelectedRoutes.remove(requireRouteId(routeId));
            return this;
        }

        public Builder clearSelectableRoutes()
        {
            mSelectableRoutes.clear();
            return this;
        }

        public Builder addSelectableRoute(String routeId)
        {
            mSelectableRoutes.add(requireRouteId(routeId));
            return this;
        }

        public Builder removeSelectableRoute(String routeId)
        {
            mSelectableRoutes.remove(requireRouteId(routeId));
            return this;
        }

        public Builder clearDeselectableRoutes()
        {
            mDeselectableRoutes.clear();
            return this;
        }

        public Builder addDeselectableRoute(String routeId)
        {
            mDeselectableRoutes.add(requireRouteId(routeId));
            return this;
        }

        public Builder removeDeselectableRoute(String routeId)
        {
            mDeselectableRoutes.remove(requireRouteId(routeId));
            return this;
        }

        public Builder clearTransferableRoutes()
        {
            mTransferableRoutes.clear();
            return this;
        }

        public Builder addTransferableRoute(String routeId)
        {
            mTransferableRoutes.add(requireRouteId(routeId));
            return this;
        }

        public Builder removeTransferableRoute(String routeId)
        {
            mTransferableRoutes.remove(requireRouteId(routeId));
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

        public Builder setControlHints(Bundle controlHints)
        {
            mControlHints = controlHints;
            return this;
        }

        public Builder setTransferReason(int transferReason)
        {
            mTransferReason = transferReason;
            return this;
        }

        public Builder setTransferInitiator(UserHandle transferInitiatorUserHandle, String transferInitiatorPackageName)
        {
            mTransferInitiatorUserHandle = transferInitiatorUserHandle;
            mTransferInitiatorPackageName = transferInitiatorPackageName;
            return this;
        }

        public RoutingSessionInfo build()
        {
            if (mSelectedRoutes.isEmpty()) throw new IllegalArgumentException("selectedRoutes must not be empty");

            return new RoutingSessionInfo(this);
        }

        private static String requireRouteId(String routeId)
        {
            if (TextUtils.isEmpty(routeId)) throw new IllegalArgumentException("routeId must not be empty");

            return routeId;
        }
    }
}
