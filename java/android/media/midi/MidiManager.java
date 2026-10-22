package android.media.midi;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class MidiManager
{
    public static final int TRANSPORT_MIDI_BYTE_STREAM = 1;
    public static final int TRANSPORT_UNIVERSAL_MIDI_PACKETS = 2;

    private final Context mContext;
    private final Map<DeviceCallback, Executor> mDeviceCallbacks = new ConcurrentHashMap<>();

    public MidiManager(Context context)
    {
        mContext = context;
    }

    public MidiDeviceInfo[] getDevices()
    {
        return new MidiDeviceInfo[0];
    }

    public Set<MidiDeviceInfo> getDevicesForTransport(int transport)
    {
        return Collections.unmodifiableSet(new HashSet<MidiDeviceInfo>());
    }

    public void openBluetoothDevice(BluetoothDevice bluetoothDevice, OnDeviceOpenedListener listener, Handler handler)
    {
        reportOpenFailure(listener, handler);
    }

    public void openDevice(MidiDeviceInfo deviceInfo, OnDeviceOpenedListener listener, Handler handler)
    {
        reportOpenFailure(listener, handler);
    }

    private static void reportOpenFailure(final OnDeviceOpenedListener listener, Handler handler)
    {
        if (listener == null) return;

        Handler target = handler != null ? handler : new Handler(Looper.getMainLooper());
        target.post(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onDeviceOpened(null);
            }
        });
    }

    public void registerDeviceCallback(DeviceCallback callback, Handler handler)
    {
        if (callback == null) return;

        final Handler target = handler != null ? handler : new Handler(Looper.getMainLooper());
        mDeviceCallbacks.put(callback, new Executor()
        {
            @Override
            public void execute(Runnable command)
            {
                target.post(command);
            }
        });
    }

    public void registerDeviceCallback(int transport, Executor executor, DeviceCallback callback)
    {
        if (executor == null) throw new NullPointerException("executor must not be null");
        if (callback == null) throw new NullPointerException("callback must not be null");

        mDeviceCallbacks.put(callback, executor);
    }

    public void unregisterDeviceCallback(DeviceCallback callback)
    {
        if (callback != null) mDeviceCallbacks.remove(callback);
    }

    public interface OnDeviceOpenedListener
    {
        void onDeviceOpened(MidiDevice device);
    }

    public static class DeviceCallback
    {
        public DeviceCallback()
        {
        }

        public void onDeviceAdded(MidiDeviceInfo device)
        {
        }

        public void onDeviceRemoved(MidiDeviceInfo device)
        {
        }

        public void onDeviceStatusChanged(MidiDeviceStatus status)
        {
        }
    }
}
