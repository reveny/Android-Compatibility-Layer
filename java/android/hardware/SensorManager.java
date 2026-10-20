package android.hardware;

import java.util.ArrayList;
import java.util.List;

public class SensorManager
{
    public static final int SENSOR_DELAY_FASTEST = 0;
    public static final int SENSOR_DELAY_GAME = 1;
    public static final int SENSOR_DELAY_UI = 2;
    public static final int SENSOR_DELAY_NORMAL = 3;

    public static final int SENSOR_STATUS_ACCURACY_HIGH = 3;

    public static final float GRAVITY_EARTH = 9.80665f;

    public List<Sensor> getSensorList(int type)
    {
        return new ArrayList<>();
    }

    public Sensor getDefaultSensor(int type)
    {
        return null;
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs)
    {
        return false;
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs)
    {
        return false;
    }

    public void unregisterListener(SensorEventListener listener)
    {
    }

    public void unregisterListener(SensorEventListener listener, Sensor sensor)
    {
    }

    public static boolean getRotationMatrix(float[] rotation, float[] inclination, float[] gravity, float[] geomagnetic)
    {
        return false;
    }

    public static float[] getOrientation(float[] rotation, float[] values)
    {
        return values;
    }
}
