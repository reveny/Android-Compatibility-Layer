package android.telecom;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public final class ConnectionRequest implements Parcelable
{
    public static final Parcelable.Creator<ConnectionRequest> CREATOR = new Parcelable.Creator<ConnectionRequest>()
    {
        @Override
        public ConnectionRequest createFromParcel(Parcel source)
        {
            PhoneAccountHandle account = source.readBoolean() ? PhoneAccountHandle.CREATOR.createFromParcel(source) : null;
            Uri address = source.readBoolean() ? Uri.CREATOR.createFromParcel(source) : null;
            Bundle extras = source.readBundle();
            int videoState = source.readInt();
            boolean requestingRtt = source.readBoolean();
            boolean adhocConference = source.readBoolean();
            List<Uri> participants = new ArrayList<>();
            source.readTypedList(participants, Uri.CREATOR);
            return new ConnectionRequest(account, address, extras, videoState, requestingRtt, adhocConference, participants);
        }

        @Override
        public ConnectionRequest[] newArray(int size)
        {
            return new ConnectionRequest[size];
        }
    };

    private final PhoneAccountHandle mAccountHandle;
    private final Uri mAddress;
    private final Bundle mExtras;
    private final int mVideoState;
    private final boolean mRequestingRtt;
    private final boolean mIsAdhocConference;
    private final List<Uri> mParticipants;

    public ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras)
    {
        this(accountHandle, handle, extras, VideoProfile.STATE_AUDIO_ONLY);
    }

    public ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras, int videoState)
    {
        this(accountHandle, handle, extras, videoState, false, false, null);
    }

    ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras, int videoState, boolean requestingRtt,
        boolean adhocConference, List<Uri> participants)
    {
        mAccountHandle = accountHandle;
        mAddress = handle;
        mExtras = extras;
        mVideoState = videoState;
        mRequestingRtt = requestingRtt;
        mIsAdhocConference = adhocConference;
        mParticipants = participants == null ? null : new ArrayList<>(participants);
    }

    public PhoneAccountHandle getAccountHandle()
    {
        return mAccountHandle;
    }

    public Uri getAddress()
    {
        return mAddress;
    }

    public List<Uri> getParticipants()
    {
        return mParticipants;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public int getVideoState()
    {
        return mVideoState;
    }

    public boolean isAdhocConferenceCall()
    {
        return mIsAdhocConference;
    }

    public boolean isRequestingRtt()
    {
        return mRequestingRtt;
    }

    public Connection.RttTextStream getRttTextStream()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return "ConnectionRequest " + (mAddress == null ? Uri.EMPTY : "xxxxxxx") + " " + (mExtras == null ? "" : mExtras)
            + " isAdhocConf: " + mIsAdhocConference;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeBoolean(mAccountHandle != null);
        if (mAccountHandle != null) mAccountHandle.writeToParcel(dest, flags);
        dest.writeBoolean(mAddress != null);
        if (mAddress != null) mAddress.writeToParcel(dest, flags);
        dest.writeBundle(mExtras);
        dest.writeInt(mVideoState);
        dest.writeBoolean(mRequestingRtt);
        dest.writeBoolean(mIsAdhocConference);
        dest.writeTypedList(mParticipants);
    }
}
