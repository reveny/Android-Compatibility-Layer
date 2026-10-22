package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public final class RouteInfo implements Parcelable
{
    public static final int RTN_UNICAST = 1;
    public static final int RTN_UNREACHABLE = 7;
    public static final int RTN_THROW = 9;

    private final IpPrefix mDestination;
    private final InetAddress mGateway;
    private final String mInterface;
    private final int mType;

    RouteInfo(IpPrefix destination, InetAddress gateway, String interfaceName, int type)
    {
        mDestination = destination;
        mGateway = gateway;
        mInterface = interfaceName;
        mType = type;
    }

    public IpPrefix getDestination()
    {
        return mDestination;
    }

    public InetAddress getGateway()
    {
        return mGateway;
    }

    public String getInterface()
    {
        return mInterface;
    }

    public int getType()
    {
        return mType;
    }

    public boolean isDefaultRoute()
    {
        return mType == RTN_UNICAST && mDestination.getPrefixLength() == 0;
    }

    public boolean hasGateway()
    {
        return mGateway != null && !mGateway.isAnyLocalAddress();
    }

    public boolean matches(InetAddress destination)
    {
        return mDestination.contains(destination);
    }

    @Override
    public String toString()
    {
        String value = mDestination == null ? "" : mDestination.toString();

        if (mType == RTN_UNREACHABLE) value += " unreachable";
        else if (mType == RTN_THROW) value += " throw";
        else
        {
            value += " ->";
            if (mGateway != null) value += " " + mGateway.getHostAddress();
            if (mInterface != null) value += " " + mInterface;
        }

        return value;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof RouteInfo)) return false;

        RouteInfo route = (RouteInfo)other;
        return Objects.equals(mDestination, route.mDestination) && Objects.equals(mGateway, route.mGateway)
            && Objects.equals(mInterface, route.mInterface) && mType == route.mType;
    }

    @Override
    public int hashCode()
    {
        return (mDestination == null ? 0 : mDestination.hashCode() * 41) + (mGateway == null ? 0 : mGateway.hashCode() * 47)
            + (mInterface == null ? 0 : mInterface.hashCode() * 67) + mType * 71;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(mDestination, flags);
        dest.writeString(mGateway == null ? null : mGateway.getHostAddress());
        dest.writeString(mInterface);
        dest.writeInt(mType);
    }

    public static final Creator<RouteInfo> CREATOR = new Creator<RouteInfo>()
    {
        @Override
        public RouteInfo createFromParcel(Parcel source)
        {
            IpPrefix destination = source.readParcelable(IpPrefix.class.getClassLoader());
            String gatewayText = source.readString();
            InetAddress gateway = null;

            try
            {
                if (gatewayText != null) gateway = InetAddress.getByName(gatewayText);
            }
            catch (UnknownHostException malformed)
            {
                gateway = null;
            }

            return new RouteInfo(destination, gateway, source.readString(), source.readInt());
        }

        @Override
        public RouteInfo[] newArray(int size)
        {
            return new RouteInfo[size];
        }
    };
}
