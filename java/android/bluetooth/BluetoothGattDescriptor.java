package android.bluetooth;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.util.UUID;

public class BluetoothGattDescriptor implements Parcelable
{
    public static final byte[] ENABLE_NOTIFICATION_VALUE = {0x01, 0x00};
    public static final byte[] ENABLE_INDICATION_VALUE = {0x02, 0x00};
    public static final byte[] DISABLE_NOTIFICATION_VALUE = {0x00, 0x00};
    public static final int PERMISSION_READ = 0x01;
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;
    public static final int PERMISSION_WRITE = 0x10;
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;
    public static final int PERMISSION_WRITE_SIGNED = 0x80;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 0x100;

    private final UUID mUuid;
    private final int mInstance;
    private final int mPermissions;
    private BluetoothGattCharacteristic mCharacteristic;
    private byte[] mValue;

    public BluetoothGattDescriptor(UUID uuid, int permissions)
    {
        this(null, uuid, 0, permissions);
    }

    BluetoothGattDescriptor(BluetoothGattCharacteristic characteristic, UUID uuid, int instance, int permissions)
    {
        mCharacteristic = characteristic;
        mUuid = uuid;
        mInstance = instance;
        mPermissions = permissions;
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
        out.writeInt(mPermissions);
    }

    public static final Creator<BluetoothGattDescriptor> CREATOR = new Creator<BluetoothGattDescriptor>()
    {
        @Override
        public BluetoothGattDescriptor createFromParcel(Parcel in)
        {
            ParcelUuid uuid = in.readParcelable(ParcelUuid.class.getClassLoader());
            int instance = in.readInt();
            int permissions = in.readInt();
            return new BluetoothGattDescriptor(null, uuid.getUuid(), instance, permissions);
        }

        @Override
        public BluetoothGattDescriptor[] newArray(int size)
        {
            return new BluetoothGattDescriptor[size];
        }
    };

    void setCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mCharacteristic = characteristic;
    }

    public BluetoothGattCharacteristic getCharacteristic()
    {
        return mCharacteristic;
    }

    public UUID getUuid()
    {
        return mUuid;
    }

    public int getPermissions()
    {
        return mPermissions;
    }

    @Deprecated
    public byte[] getValue()
    {
        return mValue;
    }

    @Deprecated
    public boolean setValue(byte[] value)
    {
        mValue = value;
        return true;
    }
}
