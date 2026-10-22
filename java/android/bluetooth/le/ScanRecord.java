package android.bluetooth.le;

import android.os.ParcelUuid;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScanRecord
{
    public static final int DATA_TYPE_NONE = -1;
    public static final int DATA_TYPE_FLAGS = 0x01;
    public static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    public static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    public static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    public static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    public static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    public static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    public static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    public static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    public static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    public static final int DATA_TYPE_CLASS_OF_DEVICE = 0x0D;
    public static final int DATA_TYPE_SIMPLE_PAIRING_HASH_C = 0x0E;
    public static final int DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R = 0x0F;
    public static final int DATA_TYPE_DEVICE_ID = 0x10;
    public static final int DATA_TYPE_SECURITY_MANAGER_OUT_OF_BAND_FLAGS = 0x11;
    public static final int DATA_TYPE_SLAVE_CONNECTION_INTERVAL_RANGE = 0x12;
    public static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_16_BIT = 0x14;
    public static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_128_BIT = 0x15;
    public static final int DATA_TYPE_SERVICE_DATA_16_BIT = 0x16;
    public static final int DATA_TYPE_PUBLIC_TARGET_ADDRESS = 0x17;
    public static final int DATA_TYPE_RANDOM_TARGET_ADDRESS = 0x18;
    public static final int DATA_TYPE_APPEARANCE = 0x19;
    public static final int DATA_TYPE_ADVERTISING_INTERVAL = 0x1A;
    public static final int DATA_TYPE_LE_BLUETOOTH_DEVICE_ADDRESS = 0x1B;
    public static final int DATA_TYPE_LE_ROLE = 0x1C;
    public static final int DATA_TYPE_SIMPLE_PAIRING_HASH_C_256 = 0x1D;
    public static final int DATA_TYPE_SIMPLE_PAIRING_RANDOMIZER_R_256 = 0x1E;
    public static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_32_BIT = 0x1F;
    public static final int DATA_TYPE_SERVICE_DATA_32_BIT = 0x20;
    public static final int DATA_TYPE_SERVICE_DATA_128_BIT = 0x21;
    public static final int DATA_TYPE_LE_SECURE_CONNECTIONS_CONFIRMATION_VALUE = 0x22;
    public static final int DATA_TYPE_LE_SECURE_CONNECTIONS_RANDOM_VALUE = 0x23;
    public static final int DATA_TYPE_URI = 0x24;
    public static final int DATA_TYPE_INDOOR_POSITIONING = 0x25;
    public static final int DATA_TYPE_TRANSPORT_DISCOVERY_DATA = 0x26;
    public static final int DATA_TYPE_LE_SUPPORTED_FEATURES = 0x27;
    public static final int DATA_TYPE_CHANNEL_MAP_UPDATE_INDICATION = 0x28;
    public static final int DATA_TYPE_PB_ADV = 0x29;
    public static final int DATA_TYPE_MESH_MESSAGE = 0x2A;
    public static final int DATA_TYPE_MESH_BEACON = 0x2B;
    public static final int DATA_TYPE_BIG_INFO = 0x2C;
    public static final int DATA_TYPE_BROADCAST_CODE = 0x2D;
    public static final int DATA_TYPE_RESOLVABLE_SET_IDENTIFIER = 0x2E;
    public static final int DATA_TYPE_ADVERTISING_INTERVAL_LONG = 0x2F;
    public static final int DATA_TYPE_3D_INFORMATION_DATA = 0x3D;
    public static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    private final int mAdvertiseFlags;
    private final List<ParcelUuid> mServiceUuids;
    private final List<ParcelUuid> mServiceSolicitationUuids;
    private final SparseArray<byte[]> mManufacturerSpecificData;
    private final Map<ParcelUuid, byte[]> mServiceData;
    private final int mTxPowerLevel;
    private final String mDeviceName;
    private final Map<Integer, byte[]> mAdvertisingDataMap;
    private final byte[] mBytes;

    ScanRecord(List<ParcelUuid> serviceUuids, List<ParcelUuid> serviceSolicitationUuids, SparseArray<byte[]> manufacturerData,
               Map<ParcelUuid, byte[]> serviceData, Map<Integer, byte[]> advertisingDataMap, int advertiseFlags, int txPowerLevel,
               String localName, byte[] bytes)
    {
        mServiceUuids = serviceUuids;
        mServiceSolicitationUuids = serviceSolicitationUuids == null ? new ArrayList<ParcelUuid>() : serviceSolicitationUuids;
        mManufacturerSpecificData = manufacturerData == null ? new SparseArray<byte[]>() : manufacturerData;
        mServiceData = serviceData == null ? new HashMap<ParcelUuid, byte[]>() : serviceData;
        mAdvertisingDataMap = advertisingDataMap == null ? new HashMap<Integer, byte[]>() : advertisingDataMap;
        mAdvertiseFlags = advertiseFlags;
        mTxPowerLevel = txPowerLevel;
        mDeviceName = localName;
        mBytes = bytes;
    }

    public int getAdvertiseFlags()
    {
        return mAdvertiseFlags;
    }

    public List<ParcelUuid> getServiceUuids()
    {
        return mServiceUuids;
    }

    public List<ParcelUuid> getServiceSolicitationUuids()
    {
        return mServiceSolicitationUuids;
    }

    public SparseArray<byte[]> getManufacturerSpecificData()
    {
        return mManufacturerSpecificData;
    }

    public byte[] getManufacturerSpecificData(int manufacturerId)
    {
        return mManufacturerSpecificData.get(manufacturerId);
    }

    public Map<ParcelUuid, byte[]> getServiceData()
    {
        return mServiceData;
    }

    public byte[] getServiceData(ParcelUuid serviceDataUuid)
    {
        if (serviceDataUuid == null) return null;

        return mServiceData.get(serviceDataUuid);
    }

    public int getTxPowerLevel()
    {
        return mTxPowerLevel;
    }

    public String getDeviceName()
    {
        return mDeviceName;
    }

    public Map<Integer, byte[]> getAdvertisingDataMap()
    {
        return mAdvertisingDataMap;
    }

    public byte[] getBytes()
    {
        return mBytes;
    }

    @Override
    public String toString()
    {
        return "ScanRecord [mAdvertiseFlags=" + mAdvertiseFlags + ", mServiceUuids=" + mServiceUuids + ", mTxPowerLevel=" + mTxPowerLevel
            + ", mDeviceName=" + mDeviceName + "]";
    }
}
