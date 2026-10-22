package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;

public class UsbInterface implements Parcelable
{
    public static final Parcelable.Creator<UsbInterface> CREATOR = new Parcelable.Creator<UsbInterface>()
    {
        @Override
        public UsbInterface createFromParcel(Parcel source)
        {
            int id = source.readInt();
            int alternateSetting = source.readInt();
            String name = source.readString();
            int interfaceClass = source.readInt();
            int subclass = source.readInt();
            int protocol = source.readInt();
            int count = source.readInt();
            UsbEndpoint[] endpoints = new UsbEndpoint[count];
            for (int index = 0; index < count; index++) endpoints[index] = UsbEndpoint.CREATOR.createFromParcel(source);
            return new UsbInterface(id, alternateSetting, name, interfaceClass, subclass, protocol, endpoints);
        }

        @Override
        public UsbInterface[] newArray(int size)
        {
            return new UsbInterface[size];
        }
    };

    private final int mId;
    private final int mAlternateSetting;
    private final String mName;
    private final int mClass;
    private final int mSubclass;
    private final int mProtocol;
    private final UsbEndpoint[] mEndpoints;

    UsbInterface(int id, int alternateSetting, String name, int interfaceClass, int subclass, int protocol, UsbEndpoint[] endpoints)
    {
        mId = id;
        mAlternateSetting = alternateSetting;
        mName = name;
        mClass = interfaceClass;
        mSubclass = subclass;
        mProtocol = protocol;
        mEndpoints = endpoints == null ? new UsbEndpoint[0] : endpoints;
    }

    public int getId()
    {
        return mId;
    }

    public int getAlternateSetting()
    {
        return mAlternateSetting;
    }

    public String getName()
    {
        return mName;
    }

    public int getInterfaceClass()
    {
        return mClass;
    }

    public int getInterfaceSubclass()
    {
        return mSubclass;
    }

    public int getInterfaceProtocol()
    {
        return mProtocol;
    }

    public int getEndpointCount()
    {
        return mEndpoints.length;
    }

    public UsbEndpoint getEndpoint(int index)
    {
        return mEndpoints[index];
    }

    @Override
    public String toString()
    {
        return "UsbInterface[mId=" + mId + ",mAlternateSetting=" + mAlternateSetting + ",mName=" + mName + ",mClass=" + mClass
            + ",mSubclass=" + mSubclass + ",mProtocol=" + mProtocol + ",mEndpoints=" + mEndpoints.length + "]";
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
        dest.writeInt(mAlternateSetting);
        dest.writeString(mName);
        dest.writeInt(mClass);
        dest.writeInt(mSubclass);
        dest.writeInt(mProtocol);
        dest.writeInt(mEndpoints.length);
        for (UsbEndpoint endpoint : mEndpoints) endpoint.writeToParcel(dest, flags);
    }
}
