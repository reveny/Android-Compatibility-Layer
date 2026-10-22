package android.telecom;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Objects;

public final class CallEndpoint implements Parcelable
{
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_EARPIECE = 1;
    public static final int TYPE_BLUETOOTH = 2;
    public static final int TYPE_WIRED_HEADSET = 3;
    public static final int TYPE_SPEAKER = 4;
    public static final int TYPE_STREAMING = 5;

    public static final Parcelable.Creator<CallEndpoint> CREATOR = new Parcelable.Creator<CallEndpoint>()
    {
        @Override
        public CallEndpoint createFromParcel(Parcel source)
        {
            String name = source.readString();
            int type = source.readInt();
            ParcelUuid identifier = ParcelUuid.CREATOR.createFromParcel(source);
            return new CallEndpoint(name, type, identifier);
        }

        @Override
        public CallEndpoint[] newArray(int size)
        {
            return new CallEndpoint[size];
        }
    };

    private final CharSequence mName;
    private final int mType;
    private final ParcelUuid mIdentifier;

    public CallEndpoint(CharSequence name, int type, ParcelUuid id)
    {
        mName = name;
        mType = type;
        mIdentifier = id;
    }

    public CharSequence getEndpointName()
    {
        return mName;
    }

    public int getEndpointType()
    {
        return mType;
    }

    public ParcelUuid getIdentifier()
    {
        return mIdentifier;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mName == null ? null : mName.toString());
        dest.writeInt(mType);
        mIdentifier.writeToParcel(dest, flags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof CallEndpoint)) return false;
        CallEndpoint endpoint = (CallEndpoint)other;
        return TextUtils.equals(mName, endpoint.mName) && mType == endpoint.mType && Objects.equals(mIdentifier, endpoint.mIdentifier);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mName == null ? null : mName.toString(), mType, mIdentifier);
    }

    @Override
    public String toString()
    {
        String[] types = {"UNKNOWN", "EARPIECE", "BLUETOOTH", "WIRED_HEADSET", "SPEAKER", "STREAMING"};
        String type = mType >= TYPE_EARPIECE && mType <= TYPE_STREAMING ? types[mType] : types[0];
        return "[CallEndpoint Name: " + mName + ", Type: " + type + ", Identifier: " + mIdentifier + "]";
    }
}
