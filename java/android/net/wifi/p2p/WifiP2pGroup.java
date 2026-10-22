package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;

public class WifiP2pGroup implements Parcelable
{
    public static final int NETWORK_ID_TEMPORARY = -1;
    public static final int NETWORK_ID_PERSISTENT = -2;

    private String mNetworkName;
    private boolean mIsGroupOwner;
    private String mPassphrase;
    private String mInterface;
    private int mNetworkId = NETWORK_ID_TEMPORARY;
    private int mFrequency;

    public WifiP2pGroup()
    {
    }

    public WifiP2pGroup(WifiP2pGroup source)
    {
        if (source == null) return;

        mNetworkName = source.mNetworkName;
        mIsGroupOwner = source.mIsGroupOwner;
        mPassphrase = source.mPassphrase;
        mInterface = source.mInterface;
        mNetworkId = source.mNetworkId;
        mFrequency = source.mFrequency;
    }

    public String getNetworkName()
    {
        return mNetworkName;
    }

    public boolean isGroupOwner()
    {
        return mIsGroupOwner;
    }

    public String getPassphrase()
    {
        return mPassphrase;
    }

    public String getInterface()
    {
        return mInterface;
    }

    public int getNetworkId()
    {
        return mNetworkId;
    }

    public int getFrequency()
    {
        return mFrequency;
    }

    @Override
    public String toString()
    {
        return "network: " + mNetworkName + "\n isGO: " + mIsGroupOwner + "\n interface: " + mInterface + "\n networkId: " + mNetworkId
            + "\n frequency: " + mFrequency;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mNetworkName);
        dest.writeInt(mIsGroupOwner ? 1 : 0);
        dest.writeString(mPassphrase);
        dest.writeString(mInterface);
        dest.writeInt(mNetworkId);
        dest.writeInt(mFrequency);
    }

    public static final Creator<WifiP2pGroup> CREATOR = new Creator<WifiP2pGroup>()
    {
        @Override
        public WifiP2pGroup createFromParcel(Parcel source)
        {
            WifiP2pGroup group = new WifiP2pGroup();
            group.mNetworkName = source.readString();
            group.mIsGroupOwner = source.readInt() != 0;
            group.mPassphrase = source.readString();
            group.mInterface = source.readString();
            group.mNetworkId = source.readInt();
            group.mFrequency = source.readInt();
            return group;
        }

        @Override
        public WifiP2pGroup[] newArray(int size)
        {
            return new WifiP2pGroup[size];
        }
    };
}
