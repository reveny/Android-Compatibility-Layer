package android.bluetooth;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothGattService implements Parcelable
{
    public static final int SERVICE_TYPE_PRIMARY = 0;
    public static final int SERVICE_TYPE_SECONDARY = 1;

    protected List<BluetoothGattCharacteristic> mCharacteristics;
    protected List<BluetoothGattService> mIncludedServices;

    private final UUID mUuid;
    private final int mInstanceId;
    private final int mServiceType;

    public BluetoothGattService(UUID uuid, int serviceType)
    {
        this(uuid, 0, serviceType);
    }

    BluetoothGattService(UUID uuid, int instanceId, int serviceType)
    {
        mUuid = uuid;
        mInstanceId = instanceId;
        mServiceType = serviceType;
        mCharacteristics = new ArrayList<>();
        mIncludedServices = new ArrayList<>();
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
        out.writeInt(mInstanceId);
        out.writeInt(mServiceType);
        out.writeInt(mCharacteristics.size());

        for (BluetoothGattCharacteristic characteristic : mCharacteristics)
        {
            characteristic.writeToParcel(out, flags);
        }
    }

    public static final Creator<BluetoothGattService> CREATOR = new Creator<BluetoothGattService>()
    {
        @Override
        public BluetoothGattService createFromParcel(Parcel in)
        {
            ParcelUuid uuid = in.readParcelable(ParcelUuid.class.getClassLoader());
            int instanceId = in.readInt();
            int serviceType = in.readInt();
            BluetoothGattService service = new BluetoothGattService(uuid.getUuid(), instanceId, serviceType);

            int count = in.readInt();
            for (int index = 0; index < count; index++)
            {
                service.addCharacteristic(BluetoothGattCharacteristic.CREATOR.createFromParcel(in));
            }

            return service;
        }

        @Override
        public BluetoothGattService[] newArray(int size)
        {
            return new BluetoothGattService[size];
        }
    };

    public boolean addService(BluetoothGattService service)
    {
        mIncludedServices.add(service);
        return true;
    }

    public boolean addCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        mCharacteristics.add(characteristic);
        characteristic.setService(this);
        return true;
    }

    public UUID getUuid()
    {
        return mUuid;
    }

    public int getInstanceId()
    {
        return mInstanceId;
    }

    public int getType()
    {
        return mServiceType;
    }

    public List<BluetoothGattService> getIncludedServices()
    {
        return mIncludedServices;
    }

    public List<BluetoothGattCharacteristic> getCharacteristics()
    {
        return mCharacteristics;
    }

    public BluetoothGattCharacteristic getCharacteristic(UUID uuid)
    {
        for (BluetoothGattCharacteristic characteristic : mCharacteristics)
        {
            if (uuid.equals(characteristic.getUuid())) return characteristic;
        }

        return null;
    }
}
