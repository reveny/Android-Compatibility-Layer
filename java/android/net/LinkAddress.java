package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class LinkAddress implements Parcelable
{
    private static final int RT_SCOPE_UNIVERSE = 0;
    private static final int RT_SCOPE_LINK = 253;
    private static final int RT_SCOPE_HOST = 254;

    private final InetAddress mAddress;
    private final int mPrefixLength;
    private final int mFlags;
    private final int mScope;

    LinkAddress(InetAddress address, int prefixLength, int flags, int scope)
    {
        if (address == null || prefixLength < 0 || (address instanceof Inet4Address && prefixLength > 32) || prefixLength > 128)
        {
            throw new IllegalArgumentException("bad LinkAddress params " + address + "/" + prefixLength);
        }

        mAddress = address;
        mPrefixLength = prefixLength;
        mFlags = flags;
        mScope = scope;
    }

    LinkAddress(InetAddress address, int prefixLength)
    {
        this(address, prefixLength, 0, scopeForUnicastAddress(address));
    }

    private static int scopeForUnicastAddress(InetAddress address)
    {
        if (address.isAnyLocalAddress()) return RT_SCOPE_HOST;
        if (address.isLoopbackAddress() || address.isLinkLocalAddress()) return RT_SCOPE_LINK;

        return RT_SCOPE_UNIVERSE;
    }

    @Override
    public String toString()
    {
        return mAddress.getHostAddress() + "/" + mPrefixLength;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof LinkAddress)) return false;

        LinkAddress address = (LinkAddress)other;
        return mAddress.equals(address.mAddress) && mPrefixLength == address.mPrefixLength && mFlags == address.mFlags
            && mScope == address.mScope;
    }

    @Override
    public int hashCode()
    {
        return mAddress.hashCode() + 11 * mPrefixLength + 19 * mFlags + 43 * mScope;
    }

    public InetAddress getAddress()
    {
        return mAddress;
    }

    public int getPrefixLength()
    {
        return mPrefixLength;
    }

    public int getFlags()
    {
        return mFlags;
    }

    public int getScope()
    {
        return mScope;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        byte[] raw = mAddress.getAddress();
        dest.writeInt(raw.length);
        for (byte value : raw)
        {
            dest.writeInt(value);
        }
        dest.writeInt(mPrefixLength);
        dest.writeInt(mFlags);
        dest.writeInt(mScope);
    }

    public static final Creator<LinkAddress> CREATOR = new Creator<LinkAddress>()
    {
        @Override
        public LinkAddress createFromParcel(Parcel source)
        {
            byte[] raw = new byte[source.readInt()];
            for (int index = 0; index < raw.length; index++)
            {
                raw[index] = (byte)source.readInt();
            }

            InetAddress address;
            try
            {
                address = InetAddress.getByAddress(raw);
            }
            catch (UnknownHostException malformed)
            {
                throw new IllegalArgumentException(malformed);
            }

            int prefixLength = source.readInt();
            int flags = source.readInt();
            int scope = source.readInt();
            return new LinkAddress(address, prefixLength, flags, scope);
        }

        @Override
        public LinkAddress[] newArray(int size)
        {
            return new LinkAddress[size];
        }
    };
}
