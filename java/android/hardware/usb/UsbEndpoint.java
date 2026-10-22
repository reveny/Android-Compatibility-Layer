package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;

public class UsbEndpoint implements Parcelable
{
    public static final Parcelable.Creator<UsbEndpoint> CREATOR = new Parcelable.Creator<UsbEndpoint>()
    {
        @Override
        public UsbEndpoint createFromParcel(Parcel source)
        {
            int address = source.readInt();
            int attributes = source.readInt();
            int maxPacketSize = source.readInt();
            int interval = source.readInt();
            return new UsbEndpoint(address, attributes, maxPacketSize, interval);
        }

        @Override
        public UsbEndpoint[] newArray(int size)
        {
            return new UsbEndpoint[size];
        }
    };

    private final int mAddress;
    private final int mAttributes;
    private final int mMaxPacketSize;
    private final int mInterval;

    UsbEndpoint(int address, int attributes, int maxPacketSize, int interval)
    {
        mAddress = address;
        mAttributes = attributes;
        mMaxPacketSize = maxPacketSize;
        mInterval = interval;
    }

    public int getAddress()
    {
        return mAddress;
    }

    public int getEndpointNumber()
    {
        return mAddress & UsbConstants.USB_ENDPOINT_NUMBER_MASK;
    }

    public int getDirection()
    {
        return mAddress & UsbConstants.USB_ENDPOINT_DIR_MASK;
    }

    public int getAttributes()
    {
        return mAttributes;
    }

    public int getType()
    {
        return mAttributes & UsbConstants.USB_ENDPOINT_XFERTYPE_MASK;
    }

    public int getMaxPacketSize()
    {
        return mMaxPacketSize;
    }

    public int getInterval()
    {
        return mInterval;
    }

    @Override
    public String toString()
    {
        return "UsbEndpoint[mAddress=" + mAddress + ",mAttributes=" + mAttributes + ",mMaxPacketSize=" + mMaxPacketSize
            + ",mInterval=" + mInterval + "]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mAddress);
        dest.writeInt(mAttributes);
        dest.writeInt(mMaxPacketSize);
        dest.writeInt(mInterval);
    }
}
