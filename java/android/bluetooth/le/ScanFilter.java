package android.bluetooth.le;

import android.bluetooth.BluetoothAdapter;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ScanFilter implements Parcelable
{
    private final String mDeviceName;
    private final String mDeviceAddress;
    private final ParcelUuid mServiceUuid;
    private final ParcelUuid mServiceUuidMask;
    private final ParcelUuid mServiceSolicitationUuid;
    private final ParcelUuid mServiceSolicitationUuidMask;
    private final ParcelUuid mServiceDataUuid;
    private final byte[] mServiceData;
    private final byte[] mServiceDataMask;
    private final int mManufacturerId;
    private final byte[] mManufacturerData;
    private final byte[] mManufacturerDataMask;
    private final int mAdvertisingDataType;
    private final byte[] mAdvertisingData;
    private final byte[] mAdvertisingDataMask;

    private ScanFilter(Builder builder)
    {
        mDeviceName = builder.mDeviceName;
        mDeviceAddress = builder.mDeviceAddress;
        mServiceUuid = builder.mServiceUuid;
        mServiceUuidMask = builder.mServiceUuidMask;
        mServiceSolicitationUuid = builder.mServiceSolicitationUuid;
        mServiceSolicitationUuidMask = builder.mServiceSolicitationUuidMask;
        mServiceDataUuid = builder.mServiceDataUuid;
        mServiceData = builder.mServiceData;
        mServiceDataMask = builder.mServiceDataMask;
        mManufacturerId = builder.mManufacturerId;
        mManufacturerData = builder.mManufacturerData;
        mManufacturerDataMask = builder.mManufacturerDataMask;
        mAdvertisingDataType = builder.mAdvertisingDataType;
        mAdvertisingData = builder.mAdvertisingData;
        mAdvertisingDataMask = builder.mAdvertisingDataMask;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mDeviceName);
        dest.writeString(mDeviceAddress);
        dest.writeString(mServiceUuid == null ? null : mServiceUuid.toString());
        dest.writeString(mServiceUuidMask == null ? null : mServiceUuidMask.toString());
        dest.writeInt(mManufacturerId);
    }

    public static final Creator<ScanFilter> CREATOR = new Creator<ScanFilter>()
    {
        @Override
        public ScanFilter createFromParcel(Parcel in)
        {
            Builder builder = new Builder();
            builder.setDeviceName(in.readString());

            String address = in.readString();
            if (address != null) builder.setDeviceAddress(address);

            String uuid = in.readString();
            String mask = in.readString();
            if (uuid != null) builder.setServiceUuid(ParcelUuid.fromString(uuid), mask == null ? null : ParcelUuid.fromString(mask));

            builder.mManufacturerId = in.readInt();
            return builder.build();
        }

        @Override
        public ScanFilter[] newArray(int size)
        {
            return new ScanFilter[size];
        }
    };

    public String getDeviceName()
    {
        return mDeviceName;
    }

    public ParcelUuid getServiceUuid()
    {
        return mServiceUuid;
    }

    public ParcelUuid getServiceUuidMask()
    {
        return mServiceUuidMask;
    }

    public ParcelUuid getServiceSolicitationUuid()
    {
        return mServiceSolicitationUuid;
    }

    public ParcelUuid getServiceSolicitationUuidMask()
    {
        return mServiceSolicitationUuidMask;
    }

    public String getDeviceAddress()
    {
        return mDeviceAddress;
    }

    public byte[] getServiceData()
    {
        return mServiceData;
    }

    public byte[] getServiceDataMask()
    {
        return mServiceDataMask;
    }

    public ParcelUuid getServiceDataUuid()
    {
        return mServiceDataUuid;
    }

    public int getManufacturerId()
    {
        return mManufacturerId;
    }

    public byte[] getManufacturerData()
    {
        return mManufacturerData;
    }

    public byte[] getManufacturerDataMask()
    {
        return mManufacturerDataMask;
    }

    public int getAdvertisingDataType()
    {
        return mAdvertisingDataType;
    }

    public byte[] getAdvertisingData()
    {
        return mAdvertisingData;
    }

    public byte[] getAdvertisingDataMask()
    {
        return mAdvertisingDataMask;
    }

    public boolean matches(ScanResult scanResult)
    {
        if (scanResult == null) return false;

        if (mDeviceAddress != null
            && (scanResult.getDevice() == null || !mDeviceAddress.equals(scanResult.getDevice().getAddress())))
        {
            return false;
        }

        ScanRecord scanRecord = scanResult.getScanRecord();

        if (scanRecord == null && (mDeviceName != null || mServiceUuid != null || mManufacturerData != null || mServiceData != null))
        {
            return false;
        }

        if (mDeviceName != null && !mDeviceName.equals(scanRecord.getDeviceName())) return false;

        if (mServiceUuid != null && !matchesServiceUuids(mServiceUuid, mServiceUuidMask, scanRecord.getServiceUuids())) return false;

        if (mServiceDataUuid != null && !matchesPartialData(mServiceData, mServiceDataMask, scanRecord.getServiceData(mServiceDataUuid)))
        {
            return false;
        }

        if (mManufacturerId >= 0 && mManufacturerData != null
            && !matchesPartialData(mManufacturerData, mManufacturerDataMask, scanRecord.getManufacturerSpecificData(mManufacturerId)))
        {
            return false;
        }

        return true;
    }

    private static boolean matchesServiceUuids(ParcelUuid uuid, ParcelUuid parcelUuidMask, List<ParcelUuid> uuids)
    {
        if (uuid == null) return true;
        if (uuids == null) return false;

        for (ParcelUuid candidate : uuids)
        {
            UUID mask = parcelUuidMask == null ? null : parcelUuidMask.getUuid();
            if (matchesServiceUuid(uuid.getUuid(), mask, candidate.getUuid())) return true;
        }

        return false;
    }

    private static boolean matchesServiceUuid(UUID uuid, UUID mask, UUID data)
    {
        if (mask == null) return uuid.equals(data);

        if ((uuid.getLeastSignificantBits() & mask.getLeastSignificantBits())
            != (data.getLeastSignificantBits() & mask.getLeastSignificantBits()))
        {
            return false;
        }

        return (uuid.getMostSignificantBits() & mask.getMostSignificantBits())
            == (data.getMostSignificantBits() & mask.getMostSignificantBits());
    }

    private static boolean matchesPartialData(byte[] data, byte[] dataMask, byte[] parsedData)
    {
        if (parsedData == null || parsedData.length < data.length) return false;

        for (int index = 0; index < data.length; index++)
        {
            byte mask = dataMask == null ? (byte)0xFF : dataMask[index];
            if ((mask & parsedData[index]) != (mask & data[index])) return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "BluetoothLeScanFilter [mDeviceName=" + mDeviceName + ", mDeviceAddress=" + mDeviceAddress + ", mUuid=" + mServiceUuid
            + ", mUuidMask=" + mServiceUuidMask + ", mManufacturerId=" + mManufacturerId + "]";
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mDeviceName, mDeviceAddress, mManufacturerId, Arrays.hashCode(mManufacturerData), mServiceDataUuid,
                            Arrays.hashCode(mServiceData), mServiceUuid, mServiceUuidMask);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ScanFilter)) return false;

        ScanFilter filter = (ScanFilter)other;
        return Objects.equals(mDeviceName, filter.mDeviceName) && Objects.equals(mDeviceAddress, filter.mDeviceAddress)
            && mManufacturerId == filter.mManufacturerId && Arrays.equals(mManufacturerData, filter.mManufacturerData)
            && Arrays.equals(mManufacturerDataMask, filter.mManufacturerDataMask)
            && Objects.equals(mServiceDataUuid, filter.mServiceDataUuid)
            && Arrays.equals(mServiceData, filter.mServiceData) && Arrays.equals(mServiceDataMask, filter.mServiceDataMask)
            && Objects.equals(mServiceUuid, filter.mServiceUuid) && Objects.equals(mServiceUuidMask, filter.mServiceUuidMask);
    }

    public static final class Builder
    {
        private String mDeviceName;
        private String mDeviceAddress;
        private ParcelUuid mServiceUuid;
        private ParcelUuid mServiceUuidMask;
        private ParcelUuid mServiceSolicitationUuid;
        private ParcelUuid mServiceSolicitationUuidMask;
        private ParcelUuid mServiceDataUuid;
        private byte[] mServiceData;
        private byte[] mServiceDataMask;
        private int mManufacturerId = -1;
        private byte[] mManufacturerData;
        private byte[] mManufacturerDataMask;
        private int mAdvertisingDataType = ScanRecord.DATA_TYPE_NONE;
        private byte[] mAdvertisingData;
        private byte[] mAdvertisingDataMask;

        public Builder()
        {
        }

        public Builder setDeviceName(String deviceName)
        {
            mDeviceName = deviceName;
            return this;
        }

        public Builder setDeviceAddress(String deviceAddress)
        {
            if (deviceAddress != null && !BluetoothAdapter.checkBluetoothAddress(deviceAddress))
            {
                throw new IllegalArgumentException("invalid device address " + deviceAddress);
            }

            mDeviceAddress = deviceAddress;
            return this;
        }

        public Builder setServiceUuid(ParcelUuid serviceUuid)
        {
            mServiceUuid = serviceUuid;
            mServiceUuidMask = null;
            return this;
        }

        public Builder setServiceUuid(ParcelUuid serviceUuid, ParcelUuid uuidMask)
        {
            if (uuidMask != null && serviceUuid == null) throw new IllegalArgumentException("uuid is null while uuidMask is not null!");

            mServiceUuid = serviceUuid;
            mServiceUuidMask = uuidMask;
            return this;
        }

        public Builder setServiceSolicitationUuid(ParcelUuid serviceSolicitationUuid)
        {
            mServiceSolicitationUuid = serviceSolicitationUuid;
            if (serviceSolicitationUuid == null) mServiceSolicitationUuidMask = null;
            return this;
        }

        public Builder setServiceSolicitationUuid(ParcelUuid serviceSolicitationUuid, ParcelUuid solicitationUuidMask)
        {
            if (solicitationUuidMask != null && serviceSolicitationUuid == null)
            {
                throw new IllegalArgumentException("SolicitationUuid is null while SolicitationUuidMask is not null!");
            }

            mServiceSolicitationUuid = serviceSolicitationUuid;
            mServiceSolicitationUuidMask = solicitationUuidMask;
            return this;
        }

        public Builder setServiceData(ParcelUuid serviceDataUuid, byte[] serviceData)
        {
            if (serviceDataUuid == null) throw new IllegalArgumentException("serviceDataUuid is null");

            mServiceDataUuid = serviceDataUuid;
            mServiceData = serviceData;
            mServiceDataMask = null;
            return this;
        }

        public Builder setServiceData(ParcelUuid serviceDataUuid, byte[] serviceData, byte[] serviceDataMask)
        {
            if (serviceDataUuid == null) throw new IllegalArgumentException("serviceDataUuid is null");

            if (serviceDataMask != null && (serviceData == null || serviceData.length != serviceDataMask.length))
            {
                throw new IllegalArgumentException("size mismatch for service data and service data mask");
            }

            mServiceDataUuid = serviceDataUuid;
            mServiceData = serviceData;
            mServiceDataMask = serviceDataMask;
            return this;
        }

        public Builder setManufacturerData(int manufacturerId, byte[] manufacturerData)
        {
            if (manufacturerData != null && manufacturerId < 0) throw new IllegalArgumentException("invalid manufacture id");

            mManufacturerId = manufacturerId;
            mManufacturerData = manufacturerData;
            mManufacturerDataMask = null;
            return this;
        }

        public Builder setManufacturerData(int manufacturerId, byte[] manufacturerData, byte[] manufacturerDataMask)
        {
            if (manufacturerData != null && manufacturerId < 0) throw new IllegalArgumentException("invalid manufacture id");

            if (manufacturerDataMask != null && (manufacturerData == null || manufacturerData.length != manufacturerDataMask.length))
            {
                throw new IllegalArgumentException("size mismatch for manufacturerData and manufacturerDataMask");
            }

            mManufacturerId = manufacturerId;
            mManufacturerData = manufacturerData;
            mManufacturerDataMask = manufacturerDataMask;
            return this;
        }

        public Builder setAdvertisingDataType(int advertisingDataType)
        {
            if (advertisingDataType < 0) throw new IllegalArgumentException("invalid advertising data type");

            mAdvertisingDataType = advertisingDataType;
            return this;
        }

        public Builder setAdvertisingDataTypeWithData(int advertisingDataType, byte[] advertisingData, byte[] advertisingDataMask)
        {
            if (advertisingDataType < 0) throw new IllegalArgumentException("invalid advertising data type");
            if (advertisingData == null || advertisingDataMask == null) throw new NullPointerException("advertising data is null");

            if (advertisingData.length != advertisingDataMask.length)
            {
                throw new IllegalArgumentException("size mismatch for advertisingData and advertisingDataMask");
            }

            mAdvertisingDataType = advertisingDataType;
            mAdvertisingData = advertisingData;
            mAdvertisingDataMask = advertisingDataMask;
            return this;
        }

        public ScanFilter build()
        {
            return new ScanFilter(this);
        }
    }
}
