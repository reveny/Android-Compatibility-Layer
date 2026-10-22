package android.net.nsd;

import android.net.Network;
import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NsdServiceInfo implements Parcelable
{
    private String mServiceName;
    private String mServiceType;
    private final LinkedHashMap<String, byte[]> mAttributes = new LinkedHashMap<>();
    private final List<InetAddress> mHostAddresses = new ArrayList<>();
    private int mPort;
    private Network mNetwork;

    public NsdServiceInfo()
    {
    }

    public String getServiceName()
    {
        return mServiceName;
    }

    public void setServiceName(String serviceName)
    {
        mServiceName = serviceName;
    }

    public String getServiceType()
    {
        return mServiceType;
    }

    public void setServiceType(String serviceType)
    {
        mServiceType = serviceType;
    }

    @Deprecated
    public InetAddress getHost()
    {
        return mHostAddresses.isEmpty() ? null : mHostAddresses.get(0);
    }

    @Deprecated
    public void setHost(InetAddress host)
    {
        mHostAddresses.clear();
        if (host != null) mHostAddresses.add(host);
    }

    public int getPort()
    {
        return mPort;
    }

    public void setPort(int port)
    {
        mPort = port;
    }

    public List<InetAddress> getHostAddresses()
    {
        return new ArrayList<>(mHostAddresses);
    }

    public void setHostAddresses(List<InetAddress> addresses)
    {
        mHostAddresses.clear();
        mHostAddresses.addAll(addresses);
    }

    public void setAttribute(String key, String value)
    {
        if (key == null || key.isEmpty() || key.indexOf('=') >= 0) throw new IllegalArgumentException("Invalid attribute key " + key);
        if (key.length() > 9) throw new IllegalArgumentException("Key lengths > 9 are discouraged: " + key);

        byte[] bytes = value == null ? null : value.getBytes(StandardCharsets.UTF_8);
        mAttributes.put(key, bytes);
    }

    public void removeAttribute(String key)
    {
        mAttributes.remove(key);
    }

    public Map<String, byte[]> getAttributes()
    {
        return Collections.unmodifiableMap(mAttributes);
    }

    public Network getNetwork()
    {
        return mNetwork;
    }

    public void setNetwork(Network network)
    {
        mNetwork = network;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("name: ").append(mServiceName);
        builder.append(", type: ").append(mServiceType);
        builder.append(", hostAddresses: ").append(mHostAddresses);
        builder.append(", port: ").append(mPort);
        builder.append(", network: ").append(mNetwork);
        return builder.toString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mServiceName);
        dest.writeString(mServiceType);
        dest.writeInt(mPort);
        dest.writeInt(mHostAddresses.size());

        for (InetAddress address : mHostAddresses)
        {
            dest.writeString(address.getHostAddress());
        }

        dest.writeInt(mAttributes.size());

        for (Map.Entry<String, byte[]> entry : mAttributes.entrySet())
        {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue() == null ? null : new String(entry.getValue(), StandardCharsets.UTF_8));
        }
    }

    public static final Creator<NsdServiceInfo> CREATOR = new Creator<NsdServiceInfo>()
    {
        @Override
        public NsdServiceInfo createFromParcel(Parcel source)
        {
            NsdServiceInfo info = new NsdServiceInfo();
            info.mServiceName = source.readString();
            info.mServiceType = source.readString();
            info.mPort = source.readInt();

            int addressCount = source.readInt();
            for (int index = 0; index < addressCount; index++)
            {
                String address = source.readString();

                try
                {
                    info.mHostAddresses.add(InetAddress.getByName(address));
                }
                catch (UnknownHostException malformed)
                {
                    android.util.Log.w("NsdServiceInfo", "dropped the unparsable address " + address);
                }
            }

            int attributeCount = source.readInt();
            for (int index = 0; index < attributeCount; index++)
            {
                String key = source.readString();
                String value = source.readString();
                info.mAttributes.put(key, value == null ? null : value.getBytes(StandardCharsets.UTF_8));
            }

            return info;
        }

        @Override
        public NsdServiceInfo[] newArray(int size)
        {
            return new NsdServiceInfo[size];
        }
    };
}
