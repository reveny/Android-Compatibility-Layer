package android.telecom;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CallAudioState implements Parcelable
{
    public static final int ROUTE_EARPIECE = 0x00000001;
    public static final int ROUTE_BLUETOOTH = 0x00000002;
    public static final int ROUTE_WIRED_HEADSET = 0x00000004;
    public static final int ROUTE_SPEAKER = 0x00000008;
    public static final int ROUTE_STREAMING = 0x00000010;
    public static final int ROUTE_WIRED_OR_EARPIECE = ROUTE_EARPIECE | ROUTE_WIRED_HEADSET;

    public static final Parcelable.Creator<CallAudioState> CREATOR = new Parcelable.Creator<CallAudioState>()
    {
        @Override
        public CallAudioState createFromParcel(Parcel source)
        {
            boolean muted = source.readBoolean();
            int route = source.readInt();
            int supportedRouteMask = source.readInt();
            BluetoothDevice active = source.readBoolean() ? BluetoothDevice.CREATOR.createFromParcel(source) : null;
            List<BluetoothDevice> devices = new ArrayList<>();
            source.readTypedList(devices, BluetoothDevice.CREATOR);
            return new CallAudioState(muted, route, supportedRouteMask, active, devices);
        }

        @Override
        public CallAudioState[] newArray(int size)
        {
            return new CallAudioState[size];
        }
    };

    private final boolean mIsMuted;
    private final int mRoute;
    private final int mSupportedRouteMask;
    private final BluetoothDevice mActiveBluetoothDevice;
    private final List<BluetoothDevice> mSupportedBluetoothDevices;

    public CallAudioState(boolean muted, int route, int supportedRouteMask)
    {
        this(muted, route, supportedRouteMask, null, null);
    }

    CallAudioState(boolean muted, int route, int supportedRouteMask, BluetoothDevice activeBluetoothDevice,
        Collection<BluetoothDevice> supportedBluetoothDevices)
    {
        mIsMuted = muted;
        mRoute = route;
        mSupportedRouteMask = supportedRouteMask;
        mActiveBluetoothDevice = activeBluetoothDevice;
        mSupportedBluetoothDevices = supportedBluetoothDevices == null ? new ArrayList<>() : new ArrayList<>(supportedBluetoothDevices);
    }

    public boolean isMuted()
    {
        return mIsMuted;
    }

    public int getRoute()
    {
        return mRoute;
    }

    public int getSupportedRouteMask()
    {
        return mSupportedRouteMask;
    }

    public BluetoothDevice getActiveBluetoothDevice()
    {
        return mActiveBluetoothDevice;
    }

    public Collection<BluetoothDevice> getSupportedBluetoothDevices()
    {
        return Collections.unmodifiableList(mSupportedBluetoothDevices);
    }

    public static String audioRouteToString(int route)
    {
        if (route == 0 || (route & ~(ROUTE_EARPIECE | ROUTE_BLUETOOTH | ROUTE_WIRED_HEADSET | ROUTE_SPEAKER | ROUTE_STREAMING)) != 0)
        {
            return "UNKNOWN";
        }
        StringBuilder buffer = new StringBuilder();
        appendRoute(buffer, route, ROUTE_EARPIECE, "EARPIECE");
        appendRoute(buffer, route, ROUTE_BLUETOOTH, "BLUETOOTH");
        appendRoute(buffer, route, ROUTE_WIRED_HEADSET, "WIRED_HEADSET");
        appendRoute(buffer, route, ROUTE_SPEAKER, "SPEAKER");
        appendRoute(buffer, route, ROUTE_STREAMING, "STREAMING");
        return buffer.toString();
    }

    private static void appendRoute(StringBuilder buffer, int route, int flag, String name)
    {
        if ((route & flag) != flag) return;
        if (buffer.length() > 0) buffer.append(", ");
        buffer.append(name);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeBoolean(mIsMuted);
        dest.writeInt(mRoute);
        dest.writeInt(mSupportedRouteMask);
        dest.writeBoolean(mActiveBluetoothDevice != null);
        if (mActiveBluetoothDevice != null) mActiveBluetoothDevice.writeToParcel(dest, flags);
        dest.writeTypedList(mSupportedBluetoothDevices);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CallAudioState)) return false;
        CallAudioState state = (CallAudioState)other;
        return mIsMuted == state.mIsMuted && mRoute == state.mRoute && mSupportedRouteMask == state.mSupportedRouteMask
            && Objects.equals(mActiveBluetoothDevice, state.mActiveBluetoothDevice)
            && mSupportedBluetoothDevices.equals(state.mSupportedBluetoothDevices);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mIsMuted, mRoute, mSupportedRouteMask, mActiveBluetoothDevice, mSupportedBluetoothDevices);
    }

    @Override
    public String toString()
    {
        return "[AudioState isMuted: " + mIsMuted + ", route: " + audioRouteToString(mRoute) + ", supportedRouteMask: "
            + audioRouteToString(mSupportedRouteMask) + ", activeBluetoothDevice: [" + mActiveBluetoothDevice
            + "], supportedBluetoothDevices: [" + mSupportedBluetoothDevices + "]]";
    }
}
