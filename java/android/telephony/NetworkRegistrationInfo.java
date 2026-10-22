package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class NetworkRegistrationInfo implements Parcelable
{
    public static final int DOMAIN_UNKNOWN = 0;
    public static final int DOMAIN_CS = 1;
    public static final int DOMAIN_PS = 2;
    public static final int DOMAIN_CS_PS = DOMAIN_CS | DOMAIN_PS;

    public static final int NR_STATE_NONE = 0;
    public static final int NR_STATE_RESTRICTED = 1;
    public static final int NR_STATE_NOT_RESTRICTED = 2;
    public static final int NR_STATE_CONNECTED = 3;

    public static final int SERVICE_TYPE_UNKNOWN = 0;
    public static final int SERVICE_TYPE_VOICE = 1;
    public static final int SERVICE_TYPE_DATA = 2;
    public static final int SERVICE_TYPE_SMS = 3;
    public static final int SERVICE_TYPE_VIDEO = 4;
    public static final int SERVICE_TYPE_EMERGENCY = 5;
    public static final int SERVICE_TYPE_MMS = 6;

    static final int REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING = 0;
    static final int REGISTRATION_STATE_HOME = 1;
    static final int REGISTRATION_STATE_NOT_REGISTERED_SEARCHING = 2;
    static final int REGISTRATION_STATE_DENIED = 3;
    static final int REGISTRATION_STATE_UNKNOWN = 4;
    static final int REGISTRATION_STATE_ROAMING = 5;

    static final int TRANSPORT_TYPE_WWAN = 1;

    public static final Parcelable.Creator<NetworkRegistrationInfo> CREATOR = new Parcelable.Creator<NetworkRegistrationInfo>()
    {
        @Override
        public NetworkRegistrationInfo createFromParcel(Parcel source)
        {
            int domain = source.readInt();
            int transportType = source.readInt();
            int registrationState = source.readInt();
            int accessNetworkTechnology = source.readInt();
            int rejectCause = source.readInt();
            int[] services = source.createIntArray();
            List<Integer> availableServices = new ArrayList<>();
            for (int service : services) availableServices.add(service);
            CellIdentity cellIdentity = source.readBoolean() ? CellIdentity.CREATOR.createFromParcel(source) : null;
            String registeredPlmn = source.readString();
            boolean nonTerrestrial = source.readBoolean();
            return new NetworkRegistrationInfo(domain, transportType, registrationState, accessNetworkTechnology, rejectCause,
                availableServices, cellIdentity, registeredPlmn, nonTerrestrial);
        }

        @Override
        public NetworkRegistrationInfo[] newArray(int size)
        {
            return new NetworkRegistrationInfo[size];
        }
    };

    private final int mDomain;
    private final int mTransportType;
    private final int mRegistrationState;
    private final int mAccessNetworkTechnology;
    private final int mRejectCause;
    private final List<Integer> mAvailableServices;
    private final CellIdentity mCellIdentity;
    private final String mRegisteredPlmn;
    private final boolean mNonTerrestrial;

    NetworkRegistrationInfo(int domain, int transportType, int registrationState, int accessNetworkTechnology, int rejectCause,
        List<Integer> availableServices, CellIdentity cellIdentity, String registeredPlmn, boolean nonTerrestrial)
    {
        mDomain = domain;
        mTransportType = transportType;
        mRegistrationState = registrationState;
        mAccessNetworkTechnology = accessNetworkTechnology;
        mRejectCause = rejectCause;
        mAvailableServices = availableServices == null ? new ArrayList<>() : new ArrayList<>(availableServices);
        mCellIdentity = cellIdentity;
        mRegisteredPlmn = registeredPlmn == null ? "" : registeredPlmn;
        mNonTerrestrial = nonTerrestrial;
    }

    static NetworkRegistrationInfo outOfService(int domain)
    {
        return new NetworkRegistrationInfo(domain, TRANSPORT_TYPE_WWAN, REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING,
            TelephonyManager.NETWORK_TYPE_UNKNOWN, 0, null, null, null, false);
    }

    public int getTransportType()
    {
        return mTransportType;
    }

    public int getDomain()
    {
        return mDomain;
    }

    public String getRegisteredPlmn()
    {
        return mRegisteredPlmn;
    }

    public boolean isRegistered()
    {
        return mRegistrationState == REGISTRATION_STATE_HOME || mRegistrationState == REGISTRATION_STATE_ROAMING;
    }

    public boolean isNetworkRegistered()
    {
        return isRegistered();
    }

    public boolean isSearching()
    {
        return mRegistrationState == REGISTRATION_STATE_NOT_REGISTERED_SEARCHING;
    }

    public boolean isNetworkSearching()
    {
        return isSearching();
    }

    public boolean isRoaming()
    {
        return mRegistrationState == REGISTRATION_STATE_ROAMING;
    }

    public boolean isNetworkRoaming()
    {
        return isRoaming();
    }

    public boolean isNonTerrestrialNetwork()
    {
        return mNonTerrestrial;
    }

    public int getRejectCause()
    {
        return mRejectCause;
    }

    public List<Integer> getAvailableServices()
    {
        return Collections.unmodifiableList(mAvailableServices);
    }

    public int getAccessNetworkTechnology()
    {
        return mAccessNetworkTechnology;
    }

    public CellIdentity getCellIdentity()
    {
        return mCellIdentity;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mDomain);
        dest.writeInt(mTransportType);
        dest.writeInt(mRegistrationState);
        dest.writeInt(mAccessNetworkTechnology);
        dest.writeInt(mRejectCause);
        int[] services = new int[mAvailableServices.size()];
        for (int index = 0; index < services.length; index++) services[index] = mAvailableServices.get(index);
        dest.writeIntArray(services);
        dest.writeBoolean(mCellIdentity != null);
        if (mCellIdentity != null) mCellIdentity.writeToParcel(dest, flags);
        dest.writeString(mRegisteredPlmn);
        dest.writeBoolean(mNonTerrestrial);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof NetworkRegistrationInfo)) return false;
        NetworkRegistrationInfo info = (NetworkRegistrationInfo)other;
        return mDomain == info.mDomain && mTransportType == info.mTransportType && mRegistrationState == info.mRegistrationState
            && mAccessNetworkTechnology == info.mAccessNetworkTechnology && mRejectCause == info.mRejectCause
            && mAvailableServices.equals(info.mAvailableServices) && Objects.equals(mCellIdentity, info.mCellIdentity)
            && mRegisteredPlmn.equals(info.mRegisteredPlmn) && mNonTerrestrial == info.mNonTerrestrial;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mDomain, mTransportType, mRegistrationState, mAccessNetworkTechnology, mRejectCause, mAvailableServices,
            mCellIdentity, mRegisteredPlmn, mNonTerrestrial);
    }

    @Override
    public String toString()
    {
        return "NetworkRegistrationInfo{ domain=" + (mDomain == DOMAIN_CS ? "CS" : mDomain == DOMAIN_PS ? "PS" : "UNKNOWN")
            + " transportType=WWAN registrationState=" + mRegistrationState + " rejectCause=" + mRejectCause + " accessNetworkTechnology="
            + mAccessNetworkTechnology + " availableServices=" + mAvailableServices + " cellIdentity=" + mCellIdentity
            + " registeredPlmn=" + mRegisteredPlmn + " isNonTerrestrialNetwork=" + mNonTerrestrial + "}";
    }
}
