package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.EnumMap;

public class NetworkInfo implements Parcelable
{
    public enum State
    {
        CONNECTING,
        CONNECTED,
        SUSPENDED,
        DISCONNECTING,
        DISCONNECTED,
        UNKNOWN
    }

    public enum DetailedState
    {
        IDLE,
        SCANNING,
        CONNECTING,
        AUTHENTICATING,
        OBTAINING_IPADDR,
        CONNECTED,
        SUSPENDED,
        DISCONNECTING,
        DISCONNECTED,
        FAILED,
        BLOCKED,
        VERIFYING_POOR_LINK,
        CAPTIVE_PORTAL_CHECK
    }

    private static final EnumMap<DetailedState, State> STATE_MAP = new EnumMap<>(DetailedState.class);

    static
    {
        STATE_MAP.put(DetailedState.IDLE, State.DISCONNECTED);
        STATE_MAP.put(DetailedState.SCANNING, State.DISCONNECTED);
        STATE_MAP.put(DetailedState.CONNECTING, State.CONNECTING);
        STATE_MAP.put(DetailedState.AUTHENTICATING, State.CONNECTING);
        STATE_MAP.put(DetailedState.OBTAINING_IPADDR, State.CONNECTING);
        STATE_MAP.put(DetailedState.VERIFYING_POOR_LINK, State.CONNECTING);
        STATE_MAP.put(DetailedState.CAPTIVE_PORTAL_CHECK, State.CONNECTING);
        STATE_MAP.put(DetailedState.CONNECTED, State.CONNECTED);
        STATE_MAP.put(DetailedState.SUSPENDED, State.SUSPENDED);
        STATE_MAP.put(DetailedState.DISCONNECTING, State.DISCONNECTING);
        STATE_MAP.put(DetailedState.DISCONNECTED, State.DISCONNECTED);
        STATE_MAP.put(DetailedState.FAILED, State.DISCONNECTED);
        STATE_MAP.put(DetailedState.BLOCKED, State.DISCONNECTED);
    }

    private final int mNetworkType;
    private final int mSubtype;
    private final String mTypeName;
    private final String mSubtypeName;
    private State mState = State.UNKNOWN;
    private DetailedState mDetailedState = DetailedState.IDLE;
    private String mReason;
    private String mExtraInfo;
    private boolean mIsFailover;
    private boolean mIsAvailable;
    private boolean mIsRoaming;

    public NetworkInfo(int type, int subtype, String typeName, String subtypeName)
    {
        if (!ConnectivityManager.isNetworkTypeValid(type) && type != -1)
        {
            throw new IllegalArgumentException("Invalid network type: " + type);
        }

        mNetworkType = type;
        mSubtype = subtype;
        mTypeName = typeName;
        mSubtypeName = subtypeName;
    }

    public synchronized int getType()
    {
        return mNetworkType;
    }

    public synchronized int getSubtype()
    {
        return mSubtype;
    }

    public synchronized String getTypeName()
    {
        return mTypeName;
    }

    public synchronized String getSubtypeName()
    {
        return mSubtypeName;
    }

    public synchronized boolean isConnectedOrConnecting()
    {
        return mState == State.CONNECTED || mState == State.CONNECTING;
    }

    public synchronized boolean isConnected()
    {
        return mState == State.CONNECTED;
    }

    public synchronized boolean isAvailable()
    {
        return mIsAvailable;
    }

    public synchronized boolean isFailover()
    {
        return mIsFailover;
    }

    public synchronized boolean isRoaming()
    {
        return mIsRoaming;
    }

    public synchronized State getState()
    {
        return mState;
    }

    public synchronized DetailedState getDetailedState()
    {
        return mDetailedState;
    }

    public synchronized void setDetailedState(DetailedState detailedState, String reason, String extraInfo)
    {
        mDetailedState = detailedState;
        mState = STATE_MAP.get(detailedState);
        mReason = reason;
        mExtraInfo = extraInfo;
    }

    public synchronized String getReason()
    {
        return mReason;
    }

    public synchronized String getExtraInfo()
    {
        return mExtraInfo;
    }

    @Override
    public synchronized String toString()
    {
        return "[type: " + mTypeName + "[" + mSubtypeName + "], state: " + mState + "/" + mDetailedState + ", reason: "
            + (mReason == null ? "(unspecified)" : mReason) + ", extra: " + (mExtraInfo == null ? "(none)" : mExtraInfo)
            + ", failover: " + mIsFailover + ", available: " + mIsAvailable + ", roaming: " + mIsRoaming + "]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public synchronized void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mNetworkType);
        dest.writeInt(mSubtype);
        dest.writeString(mTypeName);
        dest.writeString(mSubtypeName);
        dest.writeString(mState.name());
        dest.writeString(mDetailedState.name());
        dest.writeInt(mIsFailover ? 1 : 0);
        dest.writeInt(mIsAvailable ? 1 : 0);
        dest.writeInt(mIsRoaming ? 1 : 0);
        dest.writeString(mReason);
        dest.writeString(mExtraInfo);
    }

    public static final Creator<NetworkInfo> CREATOR = new Creator<NetworkInfo>()
    {
        @Override
        public NetworkInfo createFromParcel(Parcel source)
        {
            NetworkInfo info = new NetworkInfo(source.readInt(), source.readInt(), source.readString(), source.readString());
            info.mState = State.valueOf(source.readString());
            info.mDetailedState = DetailedState.valueOf(source.readString());
            info.mIsFailover = source.readInt() != 0;
            info.mIsAvailable = source.readInt() != 0;
            info.mIsRoaming = source.readInt() != 0;
            info.mReason = source.readString();
            info.mExtraInfo = source.readString();

            return info;
        }

        @Override
        public NetworkInfo[] newArray(int size)
        {
            return new NetworkInfo[size];
        }
    };
}
