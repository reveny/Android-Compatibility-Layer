package android.media;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

public interface AudioRouting
{
    void addOnRoutingChangedListener(OnRoutingChangedListener listener, Handler handler);

    AudioDeviceInfo getPreferredDevice();

    AudioDeviceInfo getRoutedDevice();

    default List<AudioDeviceInfo> getRoutedDevices()
    {
        List<AudioDeviceInfo> devices = new ArrayList<>();
        AudioDeviceInfo device = getRoutedDevice();

        if (device != null) devices.add(device);

        return devices;
    }

    void removeOnRoutingChangedListener(OnRoutingChangedListener listener);

    boolean setPreferredDevice(AudioDeviceInfo deviceInfo);

    public interface OnRoutingChangedListener
    {
        void onRoutingChanged(AudioRouting router);
    }
}
