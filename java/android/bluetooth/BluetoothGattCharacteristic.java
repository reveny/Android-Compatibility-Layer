package android.bluetooth;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothGattCharacteristic implements Parcelable
{
    public static final int PROPERTY_BROADCAST = 0x01;
    public static final int PROPERTY_READ = 0x02;
    public static final int PROPERTY_WRITE_NO_RESPONSE = 0x04;
    public static final int PROPERTY_WRITE = 0x08;
    public static final int PROPERTY_NOTIFY = 0x10;
    public static final int PROPERTY_INDICATE = 0x20;
    public static final int PROPERTY_SIGNED_WRITE = 0x40;
    public static final int PROPERTY_EXTENDED_PROPS = 0x80;
    public static final int PERMISSION_READ = 0x01;
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;
    public static final int PERMISSION_WRITE = 0x10;
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;
    public static final int PERMISSION_WRITE_SIGNED = 0x80;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 0x100;
    public static final int WRITE_TYPE_DEFAULT = 0x02;
    public static final int WRITE_TYPE_NO_RESPONSE = 0x01;
    public static final int WRITE_TYPE_SIGNED = 0x04;
    public static final int FORMAT_UINT8 = 0x11;
    public static final int FORMAT_UINT16 = 0x12;
    public static final int FORMAT_UINT32 = 0x14;
    public static final int FORMAT_SINT8 = 0x21;
    public static final int FORMAT_SINT16 = 0x22;
    public static final int FORMAT_SINT32 = 0x24;
    public static final int FORMAT_SFLOAT = 0x32;
    public static final int FORMAT_FLOAT = 0x34;

    protected List<BluetoothGattDescriptor> mDescriptors;

    private final UUID mUuid;
    private final int mInstance;
    private final int mProperties;
    private final int mPermissions;
    private int mWriteType;
    private BluetoothGattService mService;
    private byte[] mValue;

    public BluetoothGattCharacteristic(UUID uuid, int properties, int permissions)
    {
        this(null, uuid, 0, properties, permissions);
    }

    BluetoothGattCharacteristic(BluetoothGattService service, UUID uuid, int instanceId, int properties, int permissions)
    {
        mUuid = uuid;
        mInstance = instanceId;
        mProperties = properties;
        mPermissions = permissions;
        mService = service;
        mDescriptors = new ArrayList<>();

        if ((properties & PROPERTY_WRITE_NO_RESPONSE) != 0) mWriteType = WRITE_TYPE_NO_RESPONSE;
        else mWriteType = WRITE_TYPE_DEFAULT;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeParcelable(new ParcelUuid(mUuid), 0);
        out.writeInt(mInstance);
        out.writeInt(mProperties);
        out.writeInt(mPermissions);
        out.writeInt(mWriteType);
        out.writeInt(mDescriptors.size());

        for (BluetoothGattDescriptor descriptor : mDescriptors)
        {
            descriptor.writeToParcel(out, flags);
        }
    }

    public static final Creator<BluetoothGattCharacteristic> CREATOR = new Creator<BluetoothGattCharacteristic>()
    {
        @Override
        public BluetoothGattCharacteristic createFromParcel(Parcel in)
        {
            ParcelUuid uuid = in.readParcelable(ParcelUuid.class.getClassLoader());
            int instance = in.readInt();
            int properties = in.readInt();
            int permissions = in.readInt();
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(null, uuid.getUuid(), instance, properties,
                                                                                         permissions);
            characteristic.mWriteType = in.readInt();

            int count = in.readInt();
            for (int index = 0; index < count; index++)
            {
                BluetoothGattDescriptor descriptor = BluetoothGattDescriptor.CREATOR.createFromParcel(in);
                descriptor.setCharacteristic(characteristic);
                characteristic.mDescriptors.add(descriptor);
            }

            return characteristic;
        }

        @Override
        public BluetoothGattCharacteristic[] newArray(int size)
        {
            return new BluetoothGattCharacteristic[size];
        }
    };

    public boolean addDescriptor(BluetoothGattDescriptor descriptor)
    {
        mDescriptors.add(descriptor);
        descriptor.setCharacteristic(this);
        return true;
    }

    void setService(BluetoothGattService service)
    {
        mService = service;
    }

    public BluetoothGattService getService()
    {
        return mService;
    }

    public UUID getUuid()
    {
        return mUuid;
    }

    public int getInstanceId()
    {
        return mInstance;
    }

    public int getProperties()
    {
        return mProperties;
    }

    public int getPermissions()
    {
        return mPermissions;
    }

    public int getWriteType()
    {
        return mWriteType;
    }

    public void setWriteType(int writeType)
    {
        mWriteType = writeType;
    }

    public List<BluetoothGattDescriptor> getDescriptors()
    {
        return mDescriptors;
    }

    public BluetoothGattDescriptor getDescriptor(UUID uuid)
    {
        for (BluetoothGattDescriptor descriptor : mDescriptors)
        {
            if (descriptor.getUuid().equals(uuid)) return descriptor;
        }

        return null;
    }

    @Deprecated
    public byte[] getValue()
    {
        return mValue;
    }

    @Deprecated
    public Integer getIntValue(int formatType, int offset)
    {
        if (mValue == null || (offset + getTypeLen(formatType)) > mValue.length) return null;

        switch (formatType)
        {
            case FORMAT_UINT8:
                return unsignedByteToInt(mValue[offset]);
            case FORMAT_UINT16:
                return unsignedBytesToInt(mValue[offset], mValue[offset + 1]);
            case FORMAT_UINT32:
                return unsignedBytesToInt(mValue[offset], mValue[offset + 1], mValue[offset + 2], mValue[offset + 3]);
            case FORMAT_SINT8:
                return unsignedToSigned(unsignedByteToInt(mValue[offset]), 8);
            case FORMAT_SINT16:
                return unsignedToSigned(unsignedBytesToInt(mValue[offset], mValue[offset + 1]), 16);
            case FORMAT_SINT32:
                return unsignedToSigned(unsignedBytesToInt(mValue[offset], mValue[offset + 1], mValue[offset + 2], mValue[offset + 3]), 32);
            default:
                return null;
        }
    }

    @Deprecated
    public Float getFloatValue(int formatType, int offset)
    {
        if (mValue == null || (offset + getTypeLen(formatType)) > mValue.length) return null;

        switch (formatType)
        {
            case FORMAT_SFLOAT:
                return bytesToFloat(mValue[offset], mValue[offset + 1]);
            case FORMAT_FLOAT:
                return bytesToFloat(mValue[offset], mValue[offset + 1], mValue[offset + 2], mValue[offset + 3]);
            default:
                return null;
        }
    }

    @Deprecated
    public String getStringValue(int offset)
    {
        if (mValue == null || offset > mValue.length) return null;

        byte[] stringBytes = new byte[mValue.length - offset];
        System.arraycopy(mValue, offset, stringBytes, 0, stringBytes.length);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    @Deprecated
    public boolean setValue(byte[] value)
    {
        mValue = value;
        return true;
    }

    @Deprecated
    public boolean setValue(int value, int formatType, int offset)
    {
        int length = offset + getTypeLen(formatType);
        if (mValue == null) mValue = new byte[length];
        if (length > mValue.length) return false;

        switch (formatType)
        {
            case FORMAT_SINT8:
                value = intToSignedBits(value, 8);
                mValue[offset] = (byte)(value & 0xFF);
                break;
            case FORMAT_UINT8:
                mValue[offset] = (byte)(value & 0xFF);
                break;
            case FORMAT_SINT16:
                value = intToSignedBits(value, 16);
                mValue[offset++] = (byte)(value & 0xFF);
                mValue[offset] = (byte)((value >> 8) & 0xFF);
                break;
            case FORMAT_UINT16:
                mValue[offset++] = (byte)(value & 0xFF);
                mValue[offset] = (byte)((value >> 8) & 0xFF);
                break;
            case FORMAT_SINT32:
                value = intToSignedBits(value, 32);
                mValue[offset++] = (byte)(value & 0xFF);
                mValue[offset++] = (byte)((value >> 8) & 0xFF);
                mValue[offset++] = (byte)((value >> 16) & 0xFF);
                mValue[offset] = (byte)((value >> 24) & 0xFF);
                break;
            case FORMAT_UINT32:
                mValue[offset++] = (byte)(value & 0xFF);
                mValue[offset++] = (byte)((value >> 8) & 0xFF);
                mValue[offset++] = (byte)((value >> 16) & 0xFF);
                mValue[offset] = (byte)((value >> 24) & 0xFF);
                break;
            default:
                return false;
        }

        return true;
    }

    @Deprecated
    public boolean setValue(int mantissa, int exponent, int formatType, int offset)
    {
        int length = offset + getTypeLen(formatType);
        if (mValue == null) mValue = new byte[length];
        if (length > mValue.length) return false;

        switch (formatType)
        {
            case FORMAT_SFLOAT:
                mantissa = intToSignedBits(mantissa, 12);
                exponent = intToSignedBits(exponent, 4);
                mValue[offset++] = (byte)(mantissa & 0xFF);
                mValue[offset] = (byte)((mantissa >> 8) & 0x0F);
                mValue[offset] += (byte)((exponent & 0x0F) << 4);
                break;
            case FORMAT_FLOAT:
                mantissa = intToSignedBits(mantissa, 24);
                exponent = intToSignedBits(exponent, 8);
                mValue[offset++] = (byte)(mantissa & 0xFF);
                mValue[offset++] = (byte)((mantissa >> 8) & 0xFF);
                mValue[offset++] = (byte)((mantissa >> 16) & 0xFF);
                mValue[offset] += (byte)(exponent & 0xFF);
                break;
            default:
                return false;
        }

        return true;
    }

    @Deprecated
    public boolean setValue(String value)
    {
        mValue = value.getBytes(StandardCharsets.UTF_8);
        return true;
    }

    private static int getTypeLen(int formatType)
    {
        return formatType & 0xF;
    }

    private static int unsignedByteToInt(byte value)
    {
        return value & 0xFF;
    }

    private static int unsignedBytesToInt(byte first, byte second)
    {
        return unsignedByteToInt(first) + (unsignedByteToInt(second) << 8);
    }

    private static int unsignedBytesToInt(byte first, byte second, byte third, byte fourth)
    {
        return unsignedByteToInt(first) + (unsignedByteToInt(second) << 8) + (unsignedByteToInt(third) << 16)
            + (unsignedByteToInt(fourth) << 24);
    }

    private static float bytesToFloat(byte first, byte second)
    {
        int mantissa = unsignedToSigned(unsignedByteToInt(first) + ((unsignedByteToInt(second) & 0x0F) << 8), 12);
        int exponent = unsignedToSigned(unsignedByteToInt(second) >> 4, 4);
        return (float)(mantissa * Math.pow(10, exponent));
    }

    private static float bytesToFloat(byte first, byte second, byte third, byte fourth)
    {
        int mantissa = unsignedToSigned(unsignedByteToInt(first) + (unsignedByteToInt(second) << 8) + (unsignedByteToInt(third) << 16), 24);
        return (float)(mantissa * Math.pow(10, fourth));
    }

    private static int unsignedToSigned(int unsigned, int size)
    {
        if ((unsigned & (1 << (size - 1))) != 0) unsigned = -1 * ((1 << (size - 1)) - (unsigned & ((1 << (size - 1)) - 1)));

        return unsigned;
    }

    private static int intToSignedBits(int value, int size)
    {
        if (value < 0) value = (1 << (size - 1)) + (value & ((1 << (size - 1)) - 1));

        return value;
    }
}
