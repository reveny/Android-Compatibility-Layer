package android.bluetooth;

import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.io.IOException;
import java.util.UUID;

public final class BluetoothDevice implements Parcelable
{
    public static final String ACTION_ACL_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED";
    public static final String ACTION_ACL_DISCONNECTED = "android.bluetooth.device.action.ACL_DISCONNECTED";
    public static final String ACTION_ACL_DISCONNECT_REQUESTED = "android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED";
    public static final String ACTION_ALIAS_CHANGED = "android.bluetooth.device.action.ALIAS_CHANGED";
    public static final String ACTION_BOND_STATE_CHANGED = "android.bluetooth.device.action.BOND_STATE_CHANGED";
    public static final String ACTION_CLASS_CHANGED = "android.bluetooth.device.action.CLASS_CHANGED";
    public static final String ACTION_FOUND = "android.bluetooth.device.action.FOUND";
    public static final String ACTION_NAME_CHANGED = "android.bluetooth.device.action.NAME_CHANGED";
    public static final String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    public static final String ACTION_UUID = "android.bluetooth.device.action.UUID";
    public static final int ADDRESS_TYPE_PUBLIC = 0;
    public static final int ADDRESS_TYPE_RANDOM = 1;
    public static final int ADDRESS_TYPE_UNKNOWN = 0xFFFF;
    public static final int BOND_NONE = 10;
    public static final int BOND_BONDING = 11;
    public static final int BOND_BONDED = 12;
    public static final int DEVICE_TYPE_UNKNOWN = 0;
    public static final int DEVICE_TYPE_CLASSIC = 1;
    public static final int DEVICE_TYPE_LE = 2;
    public static final int DEVICE_TYPE_DUAL = 3;
    public static final int ERROR = Integer.MIN_VALUE;
    public static final String EXTRA_BOND_STATE = "android.bluetooth.device.extra.BOND_STATE";
    public static final String EXTRA_CLASS = "android.bluetooth.device.extra.CLASS";
    public static final String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
    public static final String EXTRA_IS_COORDINATED_SET_MEMBER = "android.bluetooth.extra.IS_COORDINATED_SET_MEMBER";
    public static final String EXTRA_NAME = "android.bluetooth.device.extra.NAME";
    public static final String EXTRA_PAIRING_KEY = "android.bluetooth.device.extra.PAIRING_KEY";
    public static final String EXTRA_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";
    public static final String EXTRA_PREVIOUS_BOND_STATE = "android.bluetooth.device.extra.PREVIOUS_BOND_STATE";
    public static final String EXTRA_RSSI = "android.bluetooth.device.extra.RSSI";
    public static final String EXTRA_TRANSPORT = "android.bluetooth.device.extra.TRANSPORT";
    public static final String EXTRA_UUID = "android.bluetooth.device.extra.UUID";
    public static final int PAIRING_VARIANT_PIN = 0;
    public static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
    public static final int PHY_LE_1M = 1;
    public static final int PHY_LE_2M = 2;
    public static final int PHY_LE_CODED = 3;
    public static final int PHY_LE_1M_MASK = 1;
    public static final int PHY_LE_2M_MASK = 2;
    public static final int PHY_LE_CODED_MASK = 4;
    public static final int PHY_OPTION_NO_PREFERRED = 0;
    public static final int PHY_OPTION_S2 = 1;
    public static final int PHY_OPTION_S8 = 2;
    public static final int TRANSPORT_AUTO = 0;
    public static final int TRANSPORT_BREDR = 1;
    public static final int TRANSPORT_LE = 2;

    private final String mAddress;
    private final int mAddressType;

    BluetoothDevice(String address, int addressType)
    {
        if (!BluetoothAdapter.checkBluetoothAddress(address))
        {
            throw new IllegalArgumentException(address + " is not a valid Bluetooth address");
        }

        mAddress = address;
        mAddressType = addressType;
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof BluetoothDevice && mAddress.equals(((BluetoothDevice)other).getAddress());
    }

    @Override
    public int hashCode()
    {
        return mAddress.hashCode();
    }

    @Override
    public String toString()
    {
        return "XX:XX:XX:XX" + mAddress.substring(11);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(mAddress);
        out.writeInt(mAddressType);
    }

    public static final Creator<BluetoothDevice> CREATOR = new Creator<BluetoothDevice>()
    {
        @Override
        public BluetoothDevice createFromParcel(Parcel in)
        {
            return new BluetoothDevice(in.readString(), in.readInt());
        }

        @Override
        public BluetoothDevice[] newArray(int size)
        {
            return new BluetoothDevice[size];
        }
    };

    public String getAddress()
    {
        return mAddress;
    }

    public String getName()
    {
        return null;
    }

    public int getType()
    {
        return DEVICE_TYPE_UNKNOWN;
    }

    public String getAlias()
    {
        return null;
    }

    public int setAlias(String alias)
    {
        return 1;
    }

    public boolean createBond()
    {
        return false;
    }

    public int getBondState()
    {
        return BOND_NONE;
    }

    public BluetoothClass getBluetoothClass()
    {
        return null;
    }

    public ParcelUuid[] getUuids()
    {
        return null;
    }

    public boolean fetchUuidsWithSdp()
    {
        return false;
    }

    public boolean setPin(byte[] pin)
    {
        return false;
    }

    public boolean setPairingConfirmation(boolean confirm)
    {
        return false;
    }

    public BluetoothSocket createRfcommSocketToServiceRecord(UUID uuid) throws IOException
    {
        throw new IOException("Bluetooth is off");
    }

    public BluetoothSocket createInsecureRfcommSocketToServiceRecord(UUID uuid) throws IOException
    {
        throw new IOException("Bluetooth is off");
    }

    public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback)
    {
        return connectGatt(context, autoConnect, callback, TRANSPORT_AUTO);
    }

    public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport)
    {
        return connectGatt(context, autoConnect, callback, transport, PHY_LE_1M_MASK);
    }

    public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport, int phy)
    {
        return connectGatt(context, autoConnect, callback, transport, phy, null);
    }

    public BluetoothGatt connectGatt(Context context, boolean autoConnect, BluetoothGattCallback callback, int transport, int phy,
                                     Handler handler)
    {
        if (callback == null) throw new NullPointerException("callback is null");

        return null;
    }

    public BluetoothSocket createL2capChannel(int psm) throws IOException
    {
        throw new IOException("Bluetooth is off");
    }

    public BluetoothSocket createInsecureL2capChannel(int psm) throws IOException
    {
        throw new IOException("Bluetooth is off");
    }
}
