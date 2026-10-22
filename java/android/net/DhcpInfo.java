package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class DhcpInfo implements Parcelable
{
    public int ipAddress;
    public int gateway;
    public int netmask;
    public int dns1;
    public int dns2;
    public int serverAddress;
    public int leaseDuration;

    public DhcpInfo()
    {
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("ipaddr ");
        appendAddress(builder, ipAddress);
        builder.append(" gateway ");
        appendAddress(builder, gateway);
        builder.append(" netmask ");
        appendAddress(builder, netmask);
        builder.append(" dns1 ");
        appendAddress(builder, dns1);
        builder.append(" dns2 ");
        appendAddress(builder, dns2);
        builder.append(" DHCP server ");
        appendAddress(builder, serverAddress);
        builder.append(" lease ").append(leaseDuration).append(" seconds");
        return builder.toString();
    }

    private static void appendAddress(StringBuilder builder, int address)
    {
        builder.append(address & 0xff).append('.');
        builder.append((address >>> 8) & 0xff).append('.');
        builder.append((address >>> 16) & 0xff).append('.');
        builder.append((address >>> 24) & 0xff);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(ipAddress);
        dest.writeInt(gateway);
        dest.writeInt(netmask);
        dest.writeInt(dns1);
        dest.writeInt(dns2);
        dest.writeInt(serverAddress);
        dest.writeInt(leaseDuration);
    }

    public static final Creator<DhcpInfo> CREATOR = new Creator<DhcpInfo>()
    {
        @Override
        public DhcpInfo createFromParcel(Parcel source)
        {
            DhcpInfo info = new DhcpInfo();
            info.ipAddress = source.readInt();
            info.gateway = source.readInt();
            info.netmask = source.readInt();
            info.dns1 = source.readInt();
            info.dns2 = source.readInt();
            info.serverAddress = source.readInt();
            info.leaseDuration = source.readInt();
            return info;
        }

        @Override
        public DhcpInfo[] newArray(int size)
        {
            return new DhcpInfo[size];
        }
    };
}
