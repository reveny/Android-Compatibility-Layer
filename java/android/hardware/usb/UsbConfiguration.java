package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;

public class UsbConfiguration implements Parcelable
{
    private static final int ATTR_SELF_POWERED = 1 << 6;
    private static final int ATTR_REMOTE_WAKEUP = 1 << 5;

    public static final Parcelable.Creator<UsbConfiguration> CREATOR = new Parcelable.Creator<UsbConfiguration>()
    {
        @Override
        public UsbConfiguration createFromParcel(Parcel source)
        {
            int id = source.readInt();
            String name = source.readString();
            int attributes = source.readInt();
            int maxPower = source.readInt();
            int count = source.readInt();
            UsbInterface[] interfaces = new UsbInterface[count];
            for (int index = 0; index < count; index++) interfaces[index] = UsbInterface.CREATOR.createFromParcel(source);
            return new UsbConfiguration(id, name, attributes, maxPower, interfaces);
        }

        @Override
        public UsbConfiguration[] newArray(int size)
        {
            return new UsbConfiguration[size];
        }
    };

    private final int mId;
    private final String mName;
    private final int mAttributes;
    private final int mMaxPower;
    private final UsbInterface[] mInterfaces;

    UsbConfiguration(int id, String name, int attributes, int maxPower, UsbInterface[] interfaces)
    {
        mId = id;
        mName = name;
        mAttributes = attributes;
        mMaxPower = maxPower;
        mInterfaces = interfaces == null ? new UsbInterface[0] : interfaces;
    }

    public int getId()
    {
        return mId;
    }

    public String getName()
    {
        return mName;
    }

    public boolean isSelfPowered()
    {
        return (mAttributes & ATTR_SELF_POWERED) != 0;
    }

    public boolean isRemoteWakeup()
    {
        return (mAttributes & ATTR_REMOTE_WAKEUP) != 0;
    }

    public int getMaxPower()
    {
        return mMaxPower * 2;
    }

    public int getInterfaceCount()
    {
        return mInterfaces.length;
    }

    public UsbInterface getInterface(int index)
    {
        return mInterfaces[index];
    }

    @Override
    public String toString()
    {
        return "UsbConfiguration[mId=" + mId + ",mName=" + mName + ",mAttributes=" + mAttributes + ",mMaxPower=" + mMaxPower
            + ",mInterfaces=" + mInterfaces.length + "]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mId);
        dest.writeString(mName);
        dest.writeInt(mAttributes);
        dest.writeInt(mMaxPower);
        dest.writeInt(mInterfaces.length);
        for (UsbInterface usbInterface : mInterfaces) usbInterface.writeToParcel(dest, flags);
    }
}
