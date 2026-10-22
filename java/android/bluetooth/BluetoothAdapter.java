package android.bluetooth;

import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BluetoothAdapter
{
    public static final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
    public static final String ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED";
    public static final String ACTION_DISCOVERY_STARTED = "android.bluetooth.adapter.action.DISCOVERY_STARTED";
    public static final String ACTION_LOCAL_NAME_CHANGED = "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED";
    public static final String ACTION_REQUEST_DISCOVERABLE = "android.bluetooth.adapter.action.REQUEST_DISCOVERABLE";
    public static final String ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE";
    public static final String ACTION_SCAN_MODE_CHANGED = "android.bluetooth.adapter.action.SCAN_MODE_CHANGED";
    public static final String ACTION_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";
    public static final int ERROR = Integer.MIN_VALUE;
    public static final String EXTRA_CONNECTION_STATE = "android.bluetooth.adapter.extra.CONNECTION_STATE";
    public static final String EXTRA_DISCOVERABLE_DURATION = "android.bluetooth.adapter.extra.DISCOVERABLE_DURATION";
    public static final String EXTRA_LOCAL_NAME = "android.bluetooth.adapter.extra.LOCAL_NAME";
    public static final String EXTRA_PREVIOUS_CONNECTION_STATE = "android.bluetooth.adapter.extra.PREVIOUS_CONNECTION_STATE";
    public static final String EXTRA_PREVIOUS_SCAN_MODE = "android.bluetooth.adapter.extra.PREVIOUS_SCAN_MODE";
    public static final String EXTRA_PREVIOUS_STATE = "android.bluetooth.adapter.extra.PREVIOUS_STATE";
    public static final String EXTRA_SCAN_MODE = "android.bluetooth.adapter.extra.SCAN_MODE";
    public static final String EXTRA_STATE = "android.bluetooth.adapter.extra.STATE";
    public static final int SCAN_MODE_NONE = 20;
    public static final int SCAN_MODE_CONNECTABLE = 21;
    public static final int SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 3;
    public static final int STATE_OFF = 10;
    public static final int STATE_TURNING_ON = 11;
    public static final int STATE_ON = 12;
    public static final int STATE_TURNING_OFF = 13;

    private static final int ADDRESS_LENGTH = 17;
    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

    private BluetoothAdapter()
    {
    }

    public interface LeScanCallback
    {
        void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    }

    public static synchronized BluetoothAdapter getDefaultAdapter()
    {
        return null;
    }

    public BluetoothDevice getRemoteDevice(String address)
    {
        return new BluetoothDevice(address, BluetoothDevice.ADDRESS_TYPE_PUBLIC);
    }

    public BluetoothDevice getRemoteLeDevice(String address, int addressType)
    {
        if (addressType != BluetoothDevice.ADDRESS_TYPE_PUBLIC && addressType != BluetoothDevice.ADDRESS_TYPE_RANDOM)
        {
            throw new IllegalArgumentException(addressType + " is not a Bluetooth LE address type");
        }

        return new BluetoothDevice(address, addressType);
    }

    public BluetoothDevice getRemoteDevice(byte[] address)
    {
        if (address == null || address.length != 6) throw new IllegalArgumentException("Bluetooth address must have 6 bytes");

        String text = String.format("%02X:%02X:%02X:%02X:%02X:%02X", address[0], address[1], address[2], address[3], address[4],
                                    address[5]);
        return new BluetoothDevice(text, BluetoothDevice.ADDRESS_TYPE_PUBLIC);
    }

    public BluetoothLeScanner getBluetoothLeScanner()
    {
        return null;
    }

    public boolean isEnabled()
    {
        return false;
    }

    public int getState()
    {
        return STATE_OFF;
    }

    @Deprecated
    public boolean enable()
    {
        return false;
    }

    @Deprecated
    public boolean disable()
    {
        return false;
    }

    public String getAddress()
    {
        return DEFAULT_MAC_ADDRESS;
    }

    public String getName()
    {
        return null;
    }

    public boolean setName(String name)
    {
        return false;
    }

    public int getScanMode()
    {
        return SCAN_MODE_NONE;
    }

    public Duration getDiscoverableTimeout()
    {
        return null;
    }

    public boolean startDiscovery()
    {
        return false;
    }

    public boolean cancelDiscovery()
    {
        return false;
    }

    public boolean isDiscovering()
    {
        return false;
    }

    public boolean isMultipleAdvertisementSupported()
    {
        return false;
    }

    public boolean isOffloadedFilteringSupported()
    {
        return false;
    }

    public boolean isOffloadedScanBatchingSupported()
    {
        return false;
    }

    public boolean isLe2MPhySupported()
    {
        return false;
    }

    public boolean isLeCodedPhySupported()
    {
        return false;
    }

    public boolean isLeExtendedAdvertisingSupported()
    {
        return false;
    }

    public boolean isLePeriodicAdvertisingSupported()
    {
        return false;
    }

    public int isLeAudioSupported()
    {
        return 11;
    }

    public int isLeAudioBroadcastSourceSupported()
    {
        return 11;
    }

    public int isLeAudioBroadcastAssistantSupported()
    {
        return 11;
    }

    public int getLeMaximumAdvertisingDataLength()
    {
        return 0;
    }

    public int getMaxConnectedAudioDevices()
    {
        return 1;
    }

    public Set<BluetoothDevice> getBondedDevices()
    {
        return new HashSet<>();
    }

    public int getProfileConnectionState(int profile)
    {
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    public BluetoothServerSocket listenUsingRfcommWithServiceRecord(String name, UUID uuid) throws IOException
    {
        throw new IOException("Bluetooth is off");
    }

    public BluetoothServerSocket listenUsingInsecureRfcommWithServiceRecord(String name, UUID uuid) throws IOException
    {
        throw new IOException("Bluetooth is off");
    }

    public boolean getProfileProxy(Context context, BluetoothProfile.ServiceListener listener, int profile)
    {
        return false;
    }

    public void closeProfileProxy(int profile, BluetoothProfile proxy)
    {
    }

    public static boolean checkBluetoothAddress(String address)
    {
        if (address == null || address.length() != ADDRESS_LENGTH) return false;

        for (int index = 0; index < ADDRESS_LENGTH; index++)
        {
            char character = address.charAt(index);

            switch (index % 3)
            {
                case 0:
                case 1:
                    if ((character >= '0' && character <= '9') || (character >= 'A' && character <= 'F')) break;

                    return false;
                case 2:
                    if (character == ':') break;

                    return false;
                default:
                    break;
            }
        }

        return true;
    }

    @Deprecated
    public boolean startLeScan(LeScanCallback callback)
    {
        return false;
    }

    @Deprecated
    public boolean startLeScan(UUID[] serviceUuids, LeScanCallback callback)
    {
        return false;
    }

    @Deprecated
    public void stopLeScan(LeScanCallback callback)
    {
    }

    public BluetoothServerSocket listenUsingL2capChannel() throws IOException
    {
        throw new IOException("Bluetooth is off");
    }

    public BluetoothServerSocket listenUsingInsecureL2capChannel() throws IOException
    {
        throw new IOException("Bluetooth is off");
    }
}
