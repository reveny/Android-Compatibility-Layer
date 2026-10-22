package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class LinkProperties implements Parcelable
{
    private String mInterfaceName;
    private final List<LinkAddress> mLinkAddresses = new ArrayList<>();
    private final List<InetAddress> mDnsServers = new ArrayList<>();
    private final List<RouteInfo> mRoutes = new ArrayList<>();
    private String mDomains;
    private ProxyInfo mHttpProxy;
    private int mMtu;
    private IpPrefix mNat64Prefix;
    private Inet4Address mDhcpServerAddress;
    private String mPrivateDnsServerName;
    private boolean mUsePrivateDns;
    private boolean mWakeOnLanSupported;

    public LinkProperties()
    {
    }

    public void setInterfaceName(String interfaceName)
    {
        mInterfaceName = interfaceName;
    }

    public String getInterfaceName()
    {
        return mInterfaceName;
    }

    public List<LinkAddress> getLinkAddresses()
    {
        return Collections.unmodifiableList(new ArrayList<>(mLinkAddresses));
    }

    public void setLinkAddresses(Collection<LinkAddress> addresses)
    {
        mLinkAddresses.clear();
        for (LinkAddress address : addresses)
        {
            if (address != null && !mLinkAddresses.contains(address)) mLinkAddresses.add(address);
        }
    }

    public void setDnsServers(Collection<InetAddress> dnsServers)
    {
        mDnsServers.clear();
        for (InetAddress server : dnsServers)
        {
            if (server != null && !mDnsServers.contains(server)) mDnsServers.add(server);
        }
    }

    public List<InetAddress> getDnsServers()
    {
        return Collections.unmodifiableList(new ArrayList<>(mDnsServers));
    }

    public void setDomains(String domains)
    {
        mDomains = domains;
    }

    public String getDomains()
    {
        return mDomains;
    }

    public void setMtu(int mtu)
    {
        mMtu = mtu;
    }

    public int getMtu()
    {
        return mMtu;
    }

    public boolean addRoute(RouteInfo route)
    {
        if (route == null) throw new NullPointerException("route");

        if (mRoutes.contains(route)) return false;

        mRoutes.add(route);
        return true;
    }

    public List<RouteInfo> getRoutes()
    {
        return Collections.unmodifiableList(new ArrayList<>(mRoutes));
    }

    public void setHttpProxy(ProxyInfo proxy)
    {
        mHttpProxy = proxy;
    }

    public ProxyInfo getHttpProxy()
    {
        return mHttpProxy;
    }

    public IpPrefix getNat64Prefix()
    {
        return mNat64Prefix;
    }

    public void setNat64Prefix(IpPrefix prefix)
    {
        if (prefix != null && prefix.getPrefixLength() != 96)
        {
            throw new IllegalArgumentException("Only 96-bit prefixes are supported: " + prefix);
        }

        mNat64Prefix = prefix;
    }

    public Inet4Address getDhcpServerAddress()
    {
        return mDhcpServerAddress;
    }

    public void setDhcpServerAddress(Inet4Address serverAddress)
    {
        mDhcpServerAddress = serverAddress;
    }

    public boolean isPrivateDnsActive()
    {
        return mUsePrivateDns;
    }

    public String getPrivateDnsServerName()
    {
        return mPrivateDnsServerName;
    }

    public boolean isWakeOnLanSupported()
    {
        return mWakeOnLanSupported;
    }

    public void clear()
    {
        mInterfaceName = null;
        mLinkAddresses.clear();
        mDnsServers.clear();
        mRoutes.clear();
        mDomains = null;
        mHttpProxy = null;
        mMtu = 0;
        mNat64Prefix = null;
        mDhcpServerAddress = null;
        mPrivateDnsServerName = null;
        mUsePrivateDns = false;
        mWakeOnLanSupported = false;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof LinkProperties)) return false;

        LinkProperties that = (LinkProperties)other;
        return Objects.equals(mInterfaceName, that.mInterfaceName) && mLinkAddresses.equals(that.mLinkAddresses)
            && mDnsServers.equals(that.mDnsServers) && mRoutes.equals(that.mRoutes) && Objects.equals(mDomains, that.mDomains)
            && Objects.equals(mHttpProxy, that.mHttpProxy) && mMtu == that.mMtu && Objects.equals(mNat64Prefix, that.mNat64Prefix)
            && Objects.equals(mDhcpServerAddress, that.mDhcpServerAddress)
            && Objects.equals(mPrivateDnsServerName, that.mPrivateDnsServerName) && mUsePrivateDns == that.mUsePrivateDns
            && mWakeOnLanSupported == that.mWakeOnLanSupported;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mInterfaceName, mLinkAddresses, mDnsServers, mRoutes, mDomains, mHttpProxy, mMtu, mNat64Prefix,
            mDhcpServerAddress, mPrivateDnsServerName, mUsePrivateDns, mWakeOnLanSupported);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("{");

        if (mInterfaceName != null) builder.append(" InterfaceName: ").append(mInterfaceName);
        builder.append(" LinkAddresses: ").append(mLinkAddresses);
        builder.append(" DnsAddresses: ").append(mDnsServers);
        if (mUsePrivateDns) builder.append(" UsePrivateDns: true");
        if (mPrivateDnsServerName != null) builder.append(" PrivateDnsServerName: ").append(mPrivateDnsServerName);
        if (mDomains != null) builder.append(" Domains: ").append(mDomains);
        if (mMtu != 0) builder.append(" MTU: ").append(mMtu);
        if (mWakeOnLanSupported) builder.append(" WakeOnLanSupported: true");
        if (mDhcpServerAddress != null) builder.append(" ServerAddress: ").append(mDhcpServerAddress);
        builder.append(" Routes: ").append(mRoutes);
        if (mHttpProxy != null) builder.append(" HttpProxy: ").append(mHttpProxy);
        if (mNat64Prefix != null) builder.append(" Nat64Prefix: ").append(mNat64Prefix);

        return builder.append('}').toString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mInterfaceName);
        dest.writeTypedList(mLinkAddresses);

        dest.writeInt(mDnsServers.size());
        for (InetAddress server : mDnsServers) dest.writeByteArray(server.getAddress());

        dest.writeTypedList(mRoutes);
        dest.writeString(mDomains);
        dest.writeInt(mMtu);
        dest.writeParcelable(mHttpProxy, flags);
        dest.writeParcelable(mNat64Prefix, flags);
        dest.writeByteArray(mDhcpServerAddress == null ? null : mDhcpServerAddress.getAddress());
        dest.writeString(mPrivateDnsServerName);
        dest.writeBoolean(mUsePrivateDns);
        dest.writeBoolean(mWakeOnLanSupported);
    }

    public static final Creator<LinkProperties> CREATOR = new Creator<LinkProperties>()
    {
        @Override
        public LinkProperties createFromParcel(Parcel source)
        {
            LinkProperties properties = new LinkProperties();
            properties.mInterfaceName = source.readString();

            List<LinkAddress> addresses = source.createTypedArrayList(LinkAddress.CREATOR);
            if (addresses != null) properties.mLinkAddresses.addAll(addresses);

            int servers = source.readInt();
            for (int index = 0; index < servers; index++)
            {
                InetAddress server = addressOf(source.createByteArray());
                if (server != null) properties.mDnsServers.add(server);
            }

            List<RouteInfo> routes = source.createTypedArrayList(RouteInfo.CREATOR);
            if (routes != null) properties.mRoutes.addAll(routes);

            properties.mDomains = source.readString();
            properties.mMtu = source.readInt();
            properties.mHttpProxy = source.readParcelable(ProxyInfo.class.getClassLoader());
            properties.mNat64Prefix = source.readParcelable(IpPrefix.class.getClassLoader());

            InetAddress dhcpServer = addressOf(source.createByteArray());
            properties.mDhcpServerAddress = dhcpServer instanceof Inet4Address ? (Inet4Address)dhcpServer : null;

            properties.mPrivateDnsServerName = source.readString();
            properties.mUsePrivateDns = source.readBoolean();
            properties.mWakeOnLanSupported = source.readBoolean();

            return properties;
        }

        @Override
        public LinkProperties[] newArray(int size)
        {
            return new LinkProperties[size];
        }
    };

    private static InetAddress addressOf(byte[] raw)
    {
        if (raw == null) return null;

        try
        {
            return InetAddress.getByAddress(raw);
        }
        catch (java.net.UnknownHostException malformed)
        {
            return null;
        }
    }
}
