package android.bluetooth;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class BluetoothManager
{
    private final Context mContext;

    public BluetoothManager(Context context)
    {
        mContext = context;
    }

    public BluetoothAdapter getAdapter()
    {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public int getConnectionState(BluetoothDevice device, int profile)
    {
        if (profile != BluetoothProfile.GATT && profile != BluetoothProfile.GATT_SERVER)
        {
            throw new IllegalArgumentException("Profile not supported: " + profile);
        }

        return BluetoothProfile.STATE_DISCONNECTED;
    }

    public List<BluetoothDevice> getConnectedDevices(int profile)
    {
        if (profile != BluetoothProfile.GATT && profile != BluetoothProfile.GATT_SERVER)
        {
            throw new IllegalArgumentException("Profile not supported: " + profile);
        }

        return new ArrayList<>();
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int profile, int[] states)
    {
        if (profile != BluetoothProfile.GATT && profile != BluetoothProfile.GATT_SERVER)
        {
            throw new IllegalArgumentException("Profile not supported: " + profile);
        }

        return new ArrayList<>();
    }
}
