package android.hardware.camera2.params;

import java.util.HashMap;
import java.util.Map;

public final class DeviceStateSensorOrientationMap
{
    public static final long NORMAL = 0;
    public static final long FOLDED = 1 << 2;

    private final Map<Long, Integer> mDeviceStateOrientationMap = new HashMap<>();

    DeviceStateSensorOrientationMap(long[] elements)
    {
        if (elements == null) throw new NullPointerException("elements must not be null");
        if ((elements.length % 2) != 0) throw new IllegalArgumentException("Device state sensor orientation map length must be even");

        for (int index = 0; index < elements.length; index += 2)
        {
            if ((elements[index + 1] % 90) != 0) throw new IllegalArgumentException("Sensor orientation not divisible by 90");
            mDeviceStateOrientationMap.put(elements[index], (int)elements[index + 1]);
        }
    }

    public int getSensorOrientation(long deviceState)
    {
        Integer orientation = mDeviceStateOrientationMap.get(deviceState);
        if (orientation == null) throw new IllegalArgumentException("Invalid device state: " + deviceState);
        return orientation;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof DeviceStateSensorOrientationMap
                && ((DeviceStateSensorOrientationMap)other).mDeviceStateOrientationMap.equals(mDeviceStateOrientationMap);
    }

    @Override
    public int hashCode()
    {
        return mDeviceStateOrientationMap.hashCode();
    }
}
