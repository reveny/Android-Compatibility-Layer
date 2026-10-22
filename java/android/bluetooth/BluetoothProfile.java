package android.bluetooth;

import java.util.List;

public interface BluetoothProfile
{
    String EXTRA_STATE = "android.bluetooth.profile.extra.STATE";
    String EXTRA_PREVIOUS_STATE = "android.bluetooth.profile.extra.PREVIOUS_STATE";
    int STATE_DISCONNECTED = 0;
    int STATE_CONNECTING = 1;
    int STATE_CONNECTED = 2;
    int STATE_DISCONNECTING = 3;
    int HEADSET = 1;
    int A2DP = 2;
    int HEALTH = 3;
    int GATT = 7;
    int GATT_SERVER = 8;
    int SAP = 10;
    int HID_DEVICE = 19;
    int HEARING_AID = 21;
    int LE_AUDIO = 22;
    int CSIP_SET_COORDINATOR = 25;
    int HAP_CLIENT = 28;

    List<BluetoothDevice> getConnectedDevices();

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states);

    int getConnectionState(BluetoothDevice device);

    interface ServiceListener
    {
        void onServiceConnected(int profile, BluetoothProfile proxy);

        void onServiceDisconnected(int profile);
    }
}
