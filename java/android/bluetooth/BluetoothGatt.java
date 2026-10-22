package android.bluetooth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BluetoothGatt implements BluetoothProfile
{
    public static final int GATT_SUCCESS = 0;
    public static final int GATT_READ_NOT_PERMITTED = 0x2;
    public static final int GATT_WRITE_NOT_PERMITTED = 0x3;
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;
    public static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;
    public static final int GATT_INVALID_OFFSET = 0x7;
    public static final int GATT_INSUFFICIENT_AUTHORIZATION = 0x8;
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;
    public static final int GATT_CONNECTION_CONGESTED = 0x8f;
    public static final int GATT_FAILURE = 0x101;
    public static final int CONNECTION_PRIORITY_BALANCED = 0;
    public static final int CONNECTION_PRIORITY_HIGH = 1;
    public static final int CONNECTION_PRIORITY_LOW_POWER = 2;
    public static final int CONNECTION_PRIORITY_DCK = 3;

    private static final int ERROR_PROFILE_SERVICE_NOT_BOUND = 9;

    private final BluetoothDevice mDevice;
    private final List<BluetoothGattService> mServices = new ArrayList<>();

    BluetoothGatt(BluetoothDevice device)
    {
        mDevice = device;
    }

    public void close()
    {
    }

    public void disconnect()
    {
    }

    public boolean connect()
    {
        return false;
    }

    public void setPreferredPhy(int txPhy, int rxPhy, int phyOptions)
    {
    }

    public void readPhy()
    {
    }

    public BluetoothDevice getDevice()
    {
        return mDevice;
    }

    public boolean discoverServices()
    {
        return false;
    }

    public List<BluetoothGattService> getServices()
    {
        return new ArrayList<>(mServices);
    }

    public BluetoothGattService getService(UUID uuid)
    {
        for (BluetoothGattService service : mServices)
        {
            if (service.getUuid().equals(uuid)) return service;
        }

        return null;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        return false;
    }

    @Deprecated
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        return false;
    }

    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value, int writeType)
    {
        return ERROR_PROFILE_SERVICE_NOT_BOUND;
    }

    public boolean readDescriptor(BluetoothGattDescriptor descriptor)
    {
        return false;
    }

    @Deprecated
    public boolean writeDescriptor(BluetoothGattDescriptor descriptor)
    {
        return false;
    }

    public int writeDescriptor(BluetoothGattDescriptor descriptor, byte[] value)
    {
        return ERROR_PROFILE_SERVICE_NOT_BOUND;
    }

    public boolean beginReliableWrite()
    {
        return false;
    }

    public boolean executeReliableWrite()
    {
        return false;
    }

    public void abortReliableWrite()
    {
    }

    @Deprecated
    public void abortReliableWrite(BluetoothDevice device)
    {
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable)
    {
        return false;
    }

    public boolean readRemoteRssi()
    {
        return false;
    }

    public boolean requestMtu(int mtu)
    {
        return false;
    }

    public boolean requestConnectionPriority(int connectionPriority)
    {
        if (connectionPriority < CONNECTION_PRIORITY_BALANCED || connectionPriority > CONNECTION_PRIORITY_DCK)
        {
            throw new IllegalArgumentException("connectionPriority not within valid range");
        }

        return false;
    }

    @Override
    public int getConnectionState(BluetoothDevice device)
    {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectionState instead.");
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices()
    {
        throw new UnsupportedOperationException("Use BluetoothManager#getConnectedDevices instead.");
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states)
    {
        throw new UnsupportedOperationException("Use BluetoothManager#getDevicesMatchingConnectionStates instead.");
    }
}
