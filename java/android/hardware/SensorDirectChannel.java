package android.hardware;

import java.nio.channels.Channel;

public final class SensorDirectChannel implements Channel
{
    public static final int RATE_STOP = 0;
    public static final int RATE_NORMAL = 1;
    public static final int RATE_FAST = 2;
    public static final int RATE_VERY_FAST = 3;

    public static final int TYPE_MEMORY_FILE = 1;
    public static final int TYPE_HARDWARE_BUFFER = 2;

    private boolean mOpen = true;

    SensorDirectChannel()
    {
    }

    @Override
    public boolean isOpen()
    {
        return mOpen;
    }

    @Override
    public void close()
    {
        mOpen = false;
    }

    public int configure(Sensor sensor, int rateLevel)
    {
        if (!mOpen) throw new IllegalStateException("channel is closed");
        return 0;
    }
}
