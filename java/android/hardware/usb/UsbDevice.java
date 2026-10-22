package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class UsbDevice implements Parcelable
{
    public static final Parcelable.Creator<UsbDevice> CREATOR = new Parcelable.Creator<UsbDevice>()
    {
        @Override
        public UsbDevice createFromParcel(Parcel source)
        {
            String name = source.readString();
            int vendorId = source.readInt();
            int productId = source.readInt();
            int deviceClass = source.readInt();
            int subclass = source.readInt();
            int protocol = source.readInt();
            String manufacturerName = source.readString();
            String productName = source.readString();
            String version = source.readString();
            String serialNumber = source.readString();
            int count = source.readInt();
            UsbConfiguration[] configurations = new UsbConfiguration[count];
            for (int index = 0; index < count; index++) configurations[index] = UsbConfiguration.CREATOR.createFromParcel(source);
            return new UsbDevice(name, vendorId, productId, deviceClass, subclass, protocol, manufacturerName, productName, version,
                serialNumber, configurations);
        }

        @Override
        public UsbDevice[] newArray(int size)
        {
            return new UsbDevice[size];
        }
    };

    private final String mName;
    private final int mVendorId;
    private final int mProductId;
    private final int mClass;
    private final int mSubclass;
    private final int mProtocol;
    private final String mManufacturerName;
    private final String mProductName;
    private final String mVersion;
    private final String mSerialNumber;
    private final UsbConfiguration[] mConfigurations;
    private final UsbInterface[] mInterfaces;

    UsbDevice(String name, int vendorId, int productId, int deviceClass, int subclass, int protocol, String manufacturerName,
        String productName, String version, String serialNumber, UsbConfiguration[] configurations)
    {
        mName = name;
        mVendorId = vendorId;
        mProductId = productId;
        mClass = deviceClass;
        mSubclass = subclass;
        mProtocol = protocol;
        mManufacturerName = manufacturerName;
        mProductName = productName;
        mVersion = version;
        mSerialNumber = serialNumber;
        mConfigurations = configurations == null ? new UsbConfiguration[0] : configurations;

        List<UsbInterface> interfaces = new ArrayList<>();
        for (UsbConfiguration configuration : mConfigurations)
        {
            for (int index = 0; index < configuration.getInterfaceCount(); index++) interfaces.add(configuration.getInterface(index));
        }
        mInterfaces = interfaces.toArray(new UsbInterface[0]);
    }

    public String getDeviceName()
    {
        return mName;
    }

    public String getManufacturerName()
    {
        return mManufacturerName;
    }

    public String getProductName()
    {
        return mProductName;
    }

    public String getVersion()
    {
        return mVersion;
    }

    public String getSerialNumber()
    {
        return mSerialNumber;
    }

    public int getDeviceId()
    {
        return getDeviceId(mName);
    }

    public int getVendorId()
    {
        return mVendorId;
    }

    public int getProductId()
    {
        return mProductId;
    }

    public int getDeviceClass()
    {
        return mClass;
    }

    public int getDeviceSubclass()
    {
        return mSubclass;
    }

    public int getDeviceProtocol()
    {
        return mProtocol;
    }

    public int getConfigurationCount()
    {
        return mConfigurations.length;
    }

    public UsbConfiguration getConfiguration(int index)
    {
        return mConfigurations[index];
    }

    public int getInterfaceCount()
    {
        return mInterfaces.length;
    }

    public UsbInterface getInterface(int index)
    {
        return mInterfaces[index];
    }

    public static int getDeviceId(String name)
    {
        if (name == null || !name.startsWith("/dev/bus/usb/")) return -1;

        String[] parts = name.substring("/dev/bus/usb/".length()).split("/");
        if (parts.length != 2) return -1;

        try
        {
            return Integer.parseInt(parts[0]) * 1000 + Integer.parseInt(parts[1]);
        }
        catch (NumberFormatException exception)
        {
            return -1;
        }
    }

    public static String getDeviceName(int id)
    {
        return String.format("/dev/bus/usb/%03d/%03d", id / 1000, id % 1000);
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof UsbDevice) return ((UsbDevice)other).mName.equals(mName);
        if (other instanceof String) return other.equals(mName);
        return false;
    }

    @Override
    public int hashCode()
    {
        return mName.hashCode();
    }

    @Override
    public String toString()
    {
        return "UsbDevice[mName=" + mName + ",mVendorId=" + mVendorId + ",mProductId=" + mProductId + ",mClass=" + mClass
            + ",mSubclass=" + mSubclass + ",mProtocol=" + mProtocol + ",mManufacturerName=" + mManufacturerName + ",mProductName="
            + mProductName + ",mVersion=" + mVersion + ",mSerialNumber=" + mSerialNumber + "]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mName);
        dest.writeInt(mVendorId);
        dest.writeInt(mProductId);
        dest.writeInt(mClass);
        dest.writeInt(mSubclass);
        dest.writeInt(mProtocol);
        dest.writeString(mManufacturerName);
        dest.writeString(mProductName);
        dest.writeString(mVersion);
        dest.writeString(mSerialNumber);
        dest.writeInt(mConfigurations.length);
        for (UsbConfiguration configuration : mConfigurations) configuration.writeToParcel(dest, flags);
    }
}
