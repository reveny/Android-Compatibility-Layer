package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ServiceState implements Parcelable
{
    public static final int STATE_IN_SERVICE = 0;
    public static final int STATE_OUT_OF_SERVICE = 1;
    public static final int STATE_EMERGENCY_ONLY = 2;
    public static final int STATE_POWER_OFF = 3;

    public static final int DUPLEX_MODE_UNKNOWN = 0;
    public static final int DUPLEX_MODE_FDD = 1;
    public static final int DUPLEX_MODE_TDD = 2;

    public static final int UNKNOWN_ID = -1;

    public static final Parcelable.Creator<ServiceState> CREATOR = new Parcelable.Creator<ServiceState>()
    {
        @Override
        public ServiceState createFromParcel(Parcel source)
        {
            return new ServiceState(source);
        }

        @Override
        public ServiceState[] newArray(int size)
        {
            return new ServiceState[size];
        }
    };

    private int mState = STATE_OUT_OF_SERVICE;
    private boolean mRoaming;
    private String mOperatorAlphaLong;
    private String mOperatorAlphaShort;
    private String mOperatorNumeric;
    private boolean mIsManualSelection;
    private int mChannelNumber = CellInfo.UNAVAILABLE;
    private int[] mCellBandwidths = new int[0];
    private int mCdmaNetworkId = UNKNOWN_ID;
    private int mCdmaSystemId = UNKNOWN_ID;
    private final List<NetworkRegistrationInfo> mNetworkRegistrationInfos = new ArrayList<>();

    public ServiceState()
    {
        resetRegistrationInfos();
    }

    public ServiceState(ServiceState source)
    {
        copyFrom(source);
    }

    public ServiceState(Parcel source)
    {
        mState = source.readInt();
        mRoaming = source.readBoolean();
        mOperatorAlphaLong = source.readString();
        mOperatorAlphaShort = source.readString();
        mOperatorNumeric = source.readString();
        mIsManualSelection = source.readBoolean();
        mChannelNumber = source.readInt();
        mCellBandwidths = source.createIntArray();
        mCdmaNetworkId = source.readInt();
        mCdmaSystemId = source.readInt();
        source.readTypedList(mNetworkRegistrationInfos, NetworkRegistrationInfo.CREATOR);
    }

    private void copyFrom(ServiceState source)
    {
        mState = source.mState;
        mRoaming = source.mRoaming;
        mOperatorAlphaLong = source.mOperatorAlphaLong;
        mOperatorAlphaShort = source.mOperatorAlphaShort;
        mOperatorNumeric = source.mOperatorNumeric;
        mIsManualSelection = source.mIsManualSelection;
        mChannelNumber = source.mChannelNumber;
        mCellBandwidths = source.mCellBandwidths.clone();
        mCdmaNetworkId = source.mCdmaNetworkId;
        mCdmaSystemId = source.mCdmaSystemId;
        mNetworkRegistrationInfos.clear();
        mNetworkRegistrationInfos.addAll(source.mNetworkRegistrationInfos);
    }

    private void resetRegistrationInfos()
    {
        mNetworkRegistrationInfos.clear();
        mNetworkRegistrationInfos.add(NetworkRegistrationInfo.outOfService(NetworkRegistrationInfo.DOMAIN_CS));
        mNetworkRegistrationInfos.add(NetworkRegistrationInfo.outOfService(NetworkRegistrationInfo.DOMAIN_PS));
    }

    public int getState()
    {
        return mState;
    }

    public void setState(int state)
    {
        mState = state;
    }

    public boolean getRoaming()
    {
        return mRoaming;
    }

    public void setRoaming(boolean roaming)
    {
        mRoaming = roaming;
    }

    public int getDuplexMode()
    {
        return DUPLEX_MODE_UNKNOWN;
    }

    public int getChannelNumber()
    {
        return mChannelNumber;
    }

    public int[] getCellBandwidths()
    {
        return mCellBandwidths.clone();
    }

    public String getOperatorAlphaLong()
    {
        return mOperatorAlphaLong;
    }

    public String getOperatorAlphaShort()
    {
        return mOperatorAlphaShort;
    }

    public String getOperatorNumeric()
    {
        return mOperatorNumeric;
    }

    public boolean getIsManualSelection()
    {
        return mIsManualSelection;
    }

    public void setIsManualSelection(boolean isManual)
    {
        mIsManualSelection = isManual;
    }

    public void setOperatorName(String longName, String shortName, String numeric)
    {
        mOperatorAlphaLong = longName;
        mOperatorAlphaShort = shortName;
        mOperatorNumeric = numeric;
    }

    public int getCdmaNetworkId()
    {
        return mCdmaNetworkId;
    }

    public int getCdmaSystemId()
    {
        return mCdmaSystemId;
    }

    public boolean isSearching()
    {
        for (NetworkRegistrationInfo info : mNetworkRegistrationInfos)
        {
            if (info.isSearching()) return true;
        }
        return false;
    }

    public boolean isUsingNonTerrestrialNetwork()
    {
        for (NetworkRegistrationInfo info : mNetworkRegistrationInfos)
        {
            if (info.isNonTerrestrialNetwork()) return true;
        }
        return false;
    }

    public List<NetworkRegistrationInfo> getNetworkRegistrationInfoList()
    {
        return new ArrayList<>(mNetworkRegistrationInfos);
    }

    public void setStateOutOfService()
    {
        mState = STATE_OUT_OF_SERVICE;
        mRoaming = false;
        mOperatorAlphaLong = null;
        mOperatorAlphaShort = null;
        mOperatorNumeric = null;
        mIsManualSelection = false;
        mChannelNumber = CellInfo.UNAVAILABLE;
        mCellBandwidths = new int[0];
        mCdmaNetworkId = UNKNOWN_ID;
        mCdmaSystemId = UNKNOWN_ID;
        resetRegistrationInfos();
    }

    public void setStateOff()
    {
        setStateOutOfService();
        mState = STATE_POWER_OFF;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mState);
        dest.writeBoolean(mRoaming);
        dest.writeString(mOperatorAlphaLong);
        dest.writeString(mOperatorAlphaShort);
        dest.writeString(mOperatorNumeric);
        dest.writeBoolean(mIsManualSelection);
        dest.writeInt(mChannelNumber);
        dest.writeIntArray(mCellBandwidths);
        dest.writeInt(mCdmaNetworkId);
        dest.writeInt(mCdmaSystemId);
        dest.writeTypedList(mNetworkRegistrationInfos);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ServiceState)) return false;
        ServiceState state = (ServiceState)other;
        return mState == state.mState && mRoaming == state.mRoaming && Objects.equals(mOperatorAlphaLong, state.mOperatorAlphaLong)
            && Objects.equals(mOperatorAlphaShort, state.mOperatorAlphaShort) && Objects.equals(mOperatorNumeric, state.mOperatorNumeric)
            && mIsManualSelection == state.mIsManualSelection && mChannelNumber == state.mChannelNumber
            && Arrays.equals(mCellBandwidths, state.mCellBandwidths) && mCdmaNetworkId == state.mCdmaNetworkId
            && mCdmaSystemId == state.mCdmaSystemId && mNetworkRegistrationInfos.equals(state.mNetworkRegistrationInfos);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mState, mRoaming, mOperatorAlphaLong, mOperatorAlphaShort, mOperatorNumeric, mIsManualSelection, mChannelNumber,
            Arrays.hashCode(mCellBandwidths), mCdmaNetworkId, mCdmaSystemId, mNetworkRegistrationInfos);
    }

    @Override
    public String toString()
    {
        return "{mVoiceRegState=" + mState + ", mDataRegState=" + mState + ", mChannelNumber=" + mChannelNumber + ", mCellBandwidths="
            + Arrays.toString(mCellBandwidths) + ", mOperatorAlphaLong=" + mOperatorAlphaLong + ", mOperatorAlphaShort="
            + mOperatorAlphaShort + ", isManualNetworkSelection=" + mIsManualSelection + ", getRoaming=" + mRoaming
            + ", mCdmaNetworkId=" + mCdmaNetworkId + ", mCdmaSystemId=" + mCdmaSystemId + ", mNetworkRegistrationInfos="
            + mNetworkRegistrationInfos + "}";
    }
}
