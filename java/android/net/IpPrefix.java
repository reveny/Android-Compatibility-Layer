package android.net;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public final class IpPrefix implements Parcelable
{
    private final byte[] mAddress;
    private final int mPrefixLength;

    public IpPrefix(InetAddress address, int prefixLength)
    {
        this(address.getAddress(), prefixLength);
    }

    IpPrefix(byte[] address, int prefixLength)
    {
        mAddress = address.clone();
        mPrefixLength = prefixLength;

        if (mAddress.length != 4 && mAddress.length != 16)
        {
            throw new IllegalArgumentException("IpPrefix has " + mAddress.length + " bytes which is neither 4 nor 16");
        }

        if (prefixLength < 0 || prefixLength > mAddress.length * 8)
        {
            throw new IllegalArgumentException("IpPrefix with " + mAddress.length + " bytes has invalid prefix length " + prefixLength);
        }

        maskRawAddress(mAddress, prefixLength);
    }

    private static void maskRawAddress(byte[] address, int prefixLength)
    {
        int offset = prefixLength / 8;
        int remainder = prefixLength % 8;
        if (offset >= address.length) return;

        byte mask = (byte)(0xFF << (8 - remainder));
        address[offset] = (byte)(address[offset] & mask);

        for (int index = offset + 1; index < address.length; index++)
        {
            address[index] = 0;
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof IpPrefix)) return false;

        IpPrefix prefix = (IpPrefix)other;
        return Arrays.equals(mAddress, prefix.mAddress) && mPrefixLength == prefix.mPrefixLength;
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(mAddress) + 11 * mPrefixLength;
    }

    public InetAddress getAddress()
    {
        try
        {
            return InetAddress.getByAddress(mAddress);
        }
        catch (UnknownHostException impossible)
        {
            return null;
        }
    }

    public byte[] getRawAddress()
    {
        return mAddress.clone();
    }

    public int getPrefixLength()
    {
        return mPrefixLength;
    }

    public boolean contains(InetAddress address)
    {
        byte[] raw = address == null ? null : address.getAddress();
        if (raw == null || raw.length != mAddress.length) return false;

        maskRawAddress(raw, mPrefixLength);
        return Arrays.equals(mAddress, raw);
    }

    @Override
    public String toString()
    {
        InetAddress address = getAddress();
        return (address == null ? "?" : address.getHostAddress()) + "/" + mPrefixLength;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mAddress.length);
        for (byte value : mAddress)
        {
            dest.writeInt(value);
        }
        dest.writeInt(mPrefixLength);
    }

    public static final Creator<IpPrefix> CREATOR = new Creator<IpPrefix>()
    {
        @Override
        public IpPrefix createFromParcel(Parcel source)
        {
            byte[] address = new byte[source.readInt()];
            for (int index = 0; index < address.length; index++)
            {
                address[index] = (byte)source.readInt();
            }
            return new IpPrefix(address, source.readInt());
        }

        @Override
        public IpPrefix[] newArray(int size)
        {
            return new IpPrefix[size];
        }
    };
}
