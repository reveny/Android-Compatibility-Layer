package android.media.midi;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public final class MidiDeviceInfo implements Parcelable
{
    public static final String PROPERTY_BLUETOOTH_DEVICE = "bluetooth_device";
    public static final String PROPERTY_MANUFACTURER = "manufacturer";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_PRODUCT = "product";
    public static final String PROPERTY_SERIAL_NUMBER = "serial_number";
    public static final String PROPERTY_USB_DEVICE = "usb_device";
    public static final String PROPERTY_VERSION = "version";
    public static final int PROTOCOL_UMP_MIDI_1_0_UP_TO_128_BITS = 3;
    public static final int PROTOCOL_UMP_MIDI_1_0_UP_TO_128_BITS_AND_JRTS = 4;
    public static final int PROTOCOL_UMP_MIDI_1_0_UP_TO_64_BITS = 1;
    public static final int PROTOCOL_UMP_MIDI_1_0_UP_TO_64_BITS_AND_JRTS = 2;
    public static final int PROTOCOL_UMP_MIDI_2_0 = 17;
    public static final int PROTOCOL_UMP_MIDI_2_0_AND_JRTS = 18;
    public static final int PROTOCOL_UMP_USE_MIDI_CI = 0;
    public static final int PROTOCOL_UNKNOWN = -1;
    public static final int TYPE_BLUETOOTH = 3;
    public static final int TYPE_USB = 1;
    public static final int TYPE_VIRTUAL = 2;

    public static final Parcelable.Creator<MidiDeviceInfo> CREATOR = new Parcelable.Creator<MidiDeviceInfo>()
    {
        @Override
        public MidiDeviceInfo createFromParcel(Parcel source)
        {
            int type = source.readInt();
            int id = source.readInt();
            int inputPortCount = source.readInt();
            int outputPortCount = source.readInt();
            String[] inputPortNames = source.createStringArray();
            String[] outputPortNames = source.createStringArray();
            boolean isPrivate = source.readInt() != 0;
            int defaultProtocol = source.readInt();
            Bundle properties = source.readBundle();
            return new MidiDeviceInfo(type, id, inputPortCount, outputPortCount, inputPortNames, outputPortNames, properties, isPrivate,
                defaultProtocol);
        }

        @Override
        public MidiDeviceInfo[] newArray(int size)
        {
            return new MidiDeviceInfo[size];
        }
    };

    private final int mType;
    private final int mId;
    private final int mInputPortCount;
    private final int mOutputPortCount;
    private final String[] mInputPortNames;
    private final String[] mOutputPortNames;
    private final Bundle mProperties;
    private final boolean mIsPrivate;
    private final int mDefaultProtocol;

    MidiDeviceInfo(int type, int id, int inputPortCount, int outputPortCount, String[] inputPortNames, String[] outputPortNames,
        Bundle properties, boolean isPrivate, int defaultProtocol)
    {
        mType = type;
        mId = id;
        mInputPortCount = inputPortCount;
        mOutputPortCount = outputPortCount;
        mInputPortNames = portNames(inputPortNames, inputPortCount);
        mOutputPortNames = portNames(outputPortNames, outputPortCount);
        mProperties = properties == null ? new Bundle() : properties;
        mIsPrivate = isPrivate;
        mDefaultProtocol = defaultProtocol;
    }

    private static String[] portNames(String[] names, int count)
    {
        String[] result = new String[count];

        for (int index = 0; index < count; index++)
        {
            result[index] = names != null && index < names.length && names[index] != null ? names[index] : "";
        }

        return result;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof MidiDeviceInfo && ((MidiDeviceInfo)object).mId == mId;
    }

    public int getDefaultProtocol()
    {
        return mDefaultProtocol;
    }

    public int getId()
    {
        return mId;
    }

    public int getInputPortCount()
    {
        return mInputPortCount;
    }

    public int getOutputPortCount()
    {
        return mOutputPortCount;
    }

    public PortInfo[] getPorts()
    {
        PortInfo[] ports = new PortInfo[mInputPortCount + mOutputPortCount];
        int index = 0;

        for (int port = 0; port < mInputPortCount; port++)
        {
            ports[index++] = new PortInfo(PortInfo.TYPE_INPUT, port, mInputPortNames[port]);
        }

        for (int port = 0; port < mOutputPortCount; port++)
        {
            ports[index++] = new PortInfo(PortInfo.TYPE_OUTPUT, port, mOutputPortNames[port]);
        }

        return ports;
    }

    public Bundle getProperties()
    {
        return mProperties;
    }

    public int getType()
    {
        return mType;
    }

    @Override
    public int hashCode()
    {
        return mId;
    }

    public boolean isPrivate()
    {
        return mIsPrivate;
    }

    @Override
    public String toString()
    {
        return "MidiDeviceInfo[mType=" + mType + ",mInputPortCount=" + mInputPortCount + ",mOutputPortCount=" + mOutputPortCount
            + ",mProperties=" + mProperties + ",mIsPrivate=" + mIsPrivate + ",mDefaultProtocol=" + mDefaultProtocol + "]";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeInt(mType);
        destination.writeInt(mId);
        destination.writeInt(mInputPortCount);
        destination.writeInt(mOutputPortCount);
        destination.writeStringArray(mInputPortNames);
        destination.writeStringArray(mOutputPortNames);
        destination.writeInt(mIsPrivate ? 1 : 0);
        destination.writeInt(mDefaultProtocol);
        destination.writeBundle(mProperties);
    }

    public static final class PortInfo
    {
        public static final int TYPE_INPUT = 1;
        public static final int TYPE_OUTPUT = 2;

        private final int mPortType;
        private final int mPortNumber;
        private final String mName;

        PortInfo(int type, int portNumber, String name)
        {
            mPortType = type;
            mPortNumber = portNumber;
            mName = name == null ? "" : name;
        }

        public String getName()
        {
            return mName;
        }

        public int getPortNumber()
        {
            return mPortNumber;
        }

        public int getType()
        {
            return mPortType;
        }
    }
}
