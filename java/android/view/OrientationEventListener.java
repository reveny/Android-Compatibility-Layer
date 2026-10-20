package android.view;

import android.content.Context;

public abstract class OrientationEventListener
{
    public static final int ORIENTATION_UNKNOWN = -1;

    private final Context mContext;
    private boolean mEnabled;

    public OrientationEventListener(Context context)
    {
        this(context, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
    }

    public OrientationEventListener(Context context, int rate)
    {
        mContext = context;
    }

    public void enable()
    {
        mEnabled = true;
    }

    public void disable()
    {
        mEnabled = false;
    }

    public boolean canDetectOrientation()
    {
        return false;
    }

    public abstract void onOrientationChanged(int orientation);
}
