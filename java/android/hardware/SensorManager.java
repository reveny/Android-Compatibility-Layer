package android.hardware;

import android.os.Handler;
import android.os.MemoryFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class SensorManager
{
    public static final int SENSOR_ORIENTATION = 1;
    public static final int SENSOR_ACCELEROMETER = 2;
    public static final int SENSOR_TEMPERATURE = 4;
    public static final int SENSOR_MAGNETIC_FIELD = 8;
    public static final int SENSOR_LIGHT = 16;
    public static final int SENSOR_PROXIMITY = 32;
    public static final int SENSOR_TRICORDER = 64;
    public static final int SENSOR_ORIENTATION_RAW = 128;
    public static final int SENSOR_ALL = 127;
    public static final int SENSOR_MIN = SENSOR_ORIENTATION;
    public static final int SENSOR_MAX = SENSOR_TRICORDER;

    public static final int DATA_X = 0;
    public static final int DATA_Y = 1;
    public static final int DATA_Z = 2;
    public static final int RAW_DATA_INDEX = 3;
    public static final int RAW_DATA_X = 3;
    public static final int RAW_DATA_Y = 4;
    public static final int RAW_DATA_Z = 5;

    public static final float STANDARD_GRAVITY = 9.80665f;
    public static final float GRAVITY_SUN = 275.0f;
    public static final float GRAVITY_MERCURY = 3.7f;
    public static final float GRAVITY_VENUS = 8.87f;
    public static final float GRAVITY_EARTH = 9.80665f;
    public static final float GRAVITY_MOON = 1.6f;
    public static final float GRAVITY_MARS = 3.71f;
    public static final float GRAVITY_JUPITER = 23.12f;
    public static final float GRAVITY_SATURN = 8.96f;
    public static final float GRAVITY_URANUS = 8.69f;
    public static final float GRAVITY_NEPTUNE = 11.0f;
    public static final float GRAVITY_PLUTO = 0.6f;
    public static final float GRAVITY_DEATH_STAR_I = 3.5303614E-7f;
    public static final float GRAVITY_THE_ISLAND = 4.815162f;

    public static final float MAGNETIC_FIELD_EARTH_MAX = 60.0f;
    public static final float MAGNETIC_FIELD_EARTH_MIN = 30.0f;

    public static final float PRESSURE_STANDARD_ATMOSPHERE = 1013.25f;

    public static final float LIGHT_SUNLIGHT_MAX = 120000.0f;
    public static final float LIGHT_SUNLIGHT = 110000.0f;
    public static final float LIGHT_SHADE = 20000.0f;
    public static final float LIGHT_OVERCAST = 10000.0f;
    public static final float LIGHT_SUNRISE = 400.0f;
    public static final float LIGHT_CLOUDY = 100.0f;
    public static final float LIGHT_FULLMOON = 0.25f;
    public static final float LIGHT_NO_MOON = 0.001f;

    public static final int SENSOR_DELAY_FASTEST = 0;
    public static final int SENSOR_DELAY_GAME = 1;
    public static final int SENSOR_DELAY_UI = 2;
    public static final int SENSOR_DELAY_NORMAL = 3;

    public static final int SENSOR_STATUS_NO_CONTACT = -1;
    public static final int SENSOR_STATUS_UNRELIABLE = 0;
    public static final int SENSOR_STATUS_ACCURACY_LOW = 1;
    public static final int SENSOR_STATUS_ACCURACY_MEDIUM = 2;
    public static final int SENSOR_STATUS_ACCURACY_HIGH = 3;

    public static final int AXIS_X = 1;
    public static final int AXIS_Y = 2;
    public static final int AXIS_Z = 3;
    public static final int AXIS_MINUS_X = AXIS_X | 0x80;
    public static final int AXIS_MINUS_Y = AXIS_Y | 0x80;
    public static final int AXIS_MINUS_Z = AXIS_Z | 0x80;

    public SensorManager()
    {
    }

    @Deprecated
    public int getSensors()
    {
        return 0;
    }

    public List<Sensor> getSensorList(int type)
    {
        return new ArrayList<>();
    }

    public List<Sensor> getDynamicSensorList(int type)
    {
        return new ArrayList<>();
    }

    public Sensor getDefaultSensor(int type)
    {
        return null;
    }

    public Sensor getDefaultSensor(int type, boolean wakeUp)
    {
        return null;
    }

    @Deprecated
    public boolean registerListener(SensorListener listener, int sensors)
    {
        return false;
    }

    @Deprecated
    public boolean registerListener(SensorListener listener, int sensors, int rate)
    {
        return false;
    }

    @Deprecated
    public void unregisterListener(SensorListener listener, int sensors)
    {
    }

    @Deprecated
    public void unregisterListener(SensorListener listener)
    {
    }

    public void unregisterListener(SensorEventListener listener, Sensor sensor)
    {
    }

    public void unregisterListener(SensorEventListener listener)
    {
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs)
    {
        return registerListener(listener, sensor, samplingPeriodUs, null);
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs)
    {
        return registerListener(listener, sensor, samplingPeriodUs, maxReportLatencyUs, null);
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, Handler handler)
    {
        return registerListener(listener, sensor, samplingPeriodUs, 0, handler);
    }

    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs, int maxReportLatencyUs,
        Handler handler)
    {
        return false;
    }

    public boolean flush(SensorEventListener listener)
    {
        return false;
    }

    public SensorDirectChannel createDirectChannel(MemoryFile memoryFile)
    {
        throw new UncheckedIOException(new IOException("create SensorDirectChannel failed"));
    }

    public SensorDirectChannel createDirectChannel(HardwareBuffer hardwareBuffer)
    {
        throw new UncheckedIOException(new IOException("create SensorDirectChannel failed"));
    }

    public void registerDynamicSensorCallback(DynamicSensorCallback callback)
    {
        registerDynamicSensorCallback(callback, null);
    }

    public void registerDynamicSensorCallback(DynamicSensorCallback callback, Handler handler)
    {
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");
    }

    public void unregisterDynamicSensorCallback(DynamicSensorCallback callback)
    {
    }

    public boolean isDynamicSensorDiscoverySupported()
    {
        return false;
    }

    public boolean requestTriggerSensor(TriggerEventListener listener, Sensor sensor)
    {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");
        if (sensor == null) throw new IllegalArgumentException("sensor cannot be null");
        return false;
    }

    public boolean cancelTriggerSensor(TriggerEventListener listener, Sensor sensor)
    {
        return false;
    }

    public static boolean getRotationMatrix(float[] rotation, float[] inclination, float[] gravity, float[] geomagnetic)
    {
        float ax = gravity[0];
        float ay = gravity[1];
        float az = gravity[2];

        final float normsqA = ax * ax + ay * ay + az * az;
        final float g = 9.81f;
        final float freeFallGravitySquared = 0.01f * g * g;
        if (normsqA < freeFallGravitySquared) return false;

        final float ex = geomagnetic[0];
        final float ey = geomagnetic[1];
        final float ez = geomagnetic[2];
        float hx = ey * az - ez * ay;
        float hy = ez * ax - ex * az;
        float hz = ex * ay - ey * ax;
        final float normH = (float)Math.sqrt(hx * hx + hy * hy + hz * hz);
        if (normH < 0.1f) return false;

        final float invH = 1.0f / normH;
        hx *= invH;
        hy *= invH;
        hz *= invH;
        final float invA = 1.0f / (float)Math.sqrt(normsqA);
        ax *= invA;
        ay *= invA;
        az *= invA;
        final float mx = ay * hz - az * hy;
        final float my = az * hx - ax * hz;
        final float mz = ax * hy - ay * hx;

        if (rotation != null)
        {
            if (rotation.length == 9)
            {
                rotation[0] = hx;
                rotation[1] = hy;
                rotation[2] = hz;
                rotation[3] = mx;
                rotation[4] = my;
                rotation[5] = mz;
                rotation[6] = ax;
                rotation[7] = ay;
                rotation[8] = az;
            }
            else if (rotation.length == 16)
            {
                rotation[0] = hx;
                rotation[1] = hy;
                rotation[2] = hz;
                rotation[3] = 0;
                rotation[4] = mx;
                rotation[5] = my;
                rotation[6] = mz;
                rotation[7] = 0;
                rotation[8] = ax;
                rotation[9] = ay;
                rotation[10] = az;
                rotation[11] = 0;
                rotation[12] = 0;
                rotation[13] = 0;
                rotation[14] = 0;
                rotation[15] = 1;
            }
        }

        if (inclination != null)
        {
            final float invE = 1.0f / (float)Math.sqrt(ex * ex + ey * ey + ez * ez);
            final float c = (ex * mx + ey * my + ez * mz) * invE;
            final float s = (ex * ax + ey * ay + ez * az) * invE;

            if (inclination.length == 9)
            {
                inclination[0] = 1;
                inclination[1] = 0;
                inclination[2] = 0;
                inclination[3] = 0;
                inclination[4] = c;
                inclination[5] = s;
                inclination[6] = 0;
                inclination[7] = -s;
                inclination[8] = c;
            }
            else if (inclination.length == 16)
            {
                inclination[0] = 1;
                inclination[1] = 0;
                inclination[2] = 0;
                inclination[4] = 0;
                inclination[5] = c;
                inclination[6] = s;
                inclination[8] = 0;
                inclination[9] = -s;
                inclination[10] = c;
                inclination[3] = 0;
                inclination[7] = 0;
                inclination[11] = 0;
                inclination[12] = 0;
                inclination[13] = 0;
                inclination[14] = 0;
                inclination[15] = 1;
            }
        }

        return true;
    }

    public static float getInclination(float[] inclination)
    {
        if (inclination.length == 9) return (float)Math.atan2(inclination[5], inclination[4]);
        return (float)Math.atan2(inclination[6], inclination[5]);
    }

    public static boolean remapCoordinateSystem(float[] inR, int x, int y, float[] outR)
    {
        if (inR == outR)
        {
            final float[] temp = new float[outR.length];
            if (remapCoordinateSystemImpl(inR, x, y, temp))
            {
                System.arraycopy(temp, 0, outR, 0, outR.length);
                return true;
            }
            return false;
        }

        return remapCoordinateSystemImpl(inR, x, y, outR);
    }

    private static boolean remapCoordinateSystemImpl(float[] inR, int axisX, int axisY, float[] outR)
    {
        final int length = outR.length;
        if (inR.length != length) return false;
        if ((axisX & 0x7C) != 0 || (axisY & 0x7C) != 0) return false;
        if ((axisX & 0x3) == 0 || (axisY & 0x3) == 0) return false;
        if ((axisX & 0x3) == (axisY & 0x3)) return false;

        int axisZ = axisX ^ axisY;
        final int x = (axisX & 0x3) - 1;
        final int y = (axisY & 0x3) - 1;
        final int z = (axisZ & 0x3) - 1;

        final int expectedY = (z + 1) % 3;
        final int expectedZ = (z + 2) % 3;
        if (((x ^ expectedY) | (y ^ expectedZ)) != 0) axisZ ^= 0x80;

        final boolean signX = axisX >= 0x80;
        final boolean signY = axisY >= 0x80;
        final boolean signZ = axisZ >= 0x80;

        final int rowLength = length == 16 ? 4 : 3;
        for (int row = 0; row < 3; row++)
        {
            final int offset = row * rowLength;
            for (int column = 0; column < 3; column++)
            {
                if (x == column) outR[offset + column] = signX ? -inR[offset + 0] : inR[offset + 0];
                if (y == column) outR[offset + column] = signY ? -inR[offset + 1] : inR[offset + 1];
                if (z == column) outR[offset + column] = signZ ? -inR[offset + 2] : inR[offset + 2];
            }
        }

        if (length == 16)
        {
            outR[3] = 0;
            outR[7] = 0;
            outR[11] = 0;
            outR[12] = 0;
            outR[13] = 0;
            outR[14] = 0;
            outR[15] = 1;
        }

        return true;
    }

    public static float[] getOrientation(float[] rotation, float[] values)
    {
        if (rotation.length == 9)
        {
            values[0] = (float)Math.atan2(rotation[1], rotation[4]);
            values[1] = (float)Math.asin(-rotation[7]);
            values[2] = (float)Math.atan2(-rotation[6], rotation[8]);
        }
        else
        {
            values[0] = (float)Math.atan2(rotation[1], rotation[5]);
            values[1] = (float)Math.asin(-rotation[9]);
            values[2] = (float)Math.atan2(-rotation[8], rotation[10]);
        }

        return values;
    }

    public static float getAltitude(float p0, float p)
    {
        final float coefficient = 1.0f / 5.255f;
        return 44330.0f * (1.0f - (float)Math.pow(p / p0, coefficient));
    }

    public static void getAngleChange(float[] angleChange, float[] rotation, float[] previousRotation)
    {
        float[] current = toRotation3x3(rotation);
        float[] previous = toRotation3x3(previousRotation);
        if (current == null || previous == null) return;

        float rd1 = previous[0] * current[1] + previous[3] * current[4] + previous[6] * current[7];
        float rd4 = previous[1] * current[1] + previous[4] * current[4] + previous[7] * current[7];
        float rd6 = previous[2] * current[0] + previous[5] * current[3] + previous[8] * current[6];
        float rd7 = previous[2] * current[1] + previous[5] * current[4] + previous[8] * current[7];
        float rd8 = previous[2] * current[2] + previous[5] * current[5] + previous[8] * current[8];

        angleChange[0] = (float)Math.atan2(rd1, rd4);
        angleChange[1] = (float)Math.asin(-rd7);
        angleChange[2] = (float)Math.atan2(-rd6, rd8);
    }

    private static float[] toRotation3x3(float[] matrix)
    {
        if (matrix.length == 9) return matrix;
        if (matrix.length != 16) return null;

        return new float[] {matrix[0], matrix[1], matrix[2], matrix[4], matrix[5], matrix[6], matrix[8], matrix[9], matrix[10]};
    }

    public static void getRotationMatrixFromVector(float[] rotation, float[] rotationVector)
    {
        float q0;
        float q1 = rotationVector[0];
        float q2 = rotationVector[1];
        float q3 = rotationVector[2];

        if (rotationVector.length >= 4)
        {
            q0 = rotationVector[3];
        }
        else
        {
            q0 = 1 - q1 * q1 - q2 * q2 - q3 * q3;
            q0 = q0 > 0 ? (float)Math.sqrt(q0) : 0;
        }

        float squareQ1 = 2 * q1 * q1;
        float squareQ2 = 2 * q2 * q2;
        float squareQ3 = 2 * q3 * q3;
        float q1q2 = 2 * q1 * q2;
        float q3q0 = 2 * q3 * q0;
        float q1q3 = 2 * q1 * q3;
        float q2q0 = 2 * q2 * q0;
        float q2q3 = 2 * q2 * q3;
        float q1q0 = 2 * q1 * q0;

        if (rotation.length == 9)
        {
            rotation[0] = 1 - squareQ2 - squareQ3;
            rotation[1] = q1q2 - q3q0;
            rotation[2] = q1q3 + q2q0;
            rotation[3] = q1q2 + q3q0;
            rotation[4] = 1 - squareQ1 - squareQ3;
            rotation[5] = q2q3 - q1q0;
            rotation[6] = q1q3 - q2q0;
            rotation[7] = q2q3 + q1q0;
            rotation[8] = 1 - squareQ1 - squareQ2;
        }
        else if (rotation.length == 16)
        {
            rotation[0] = 1 - squareQ2 - squareQ3;
            rotation[1] = q1q2 - q3q0;
            rotation[2] = q1q3 + q2q0;
            rotation[3] = 0.0f;
            rotation[4] = q1q2 + q3q0;
            rotation[5] = 1 - squareQ1 - squareQ3;
            rotation[6] = q2q3 - q1q0;
            rotation[7] = 0.0f;
            rotation[8] = q1q3 - q2q0;
            rotation[9] = q2q3 + q1q0;
            rotation[10] = 1 - squareQ1 - squareQ2;
            rotation[11] = 0.0f;
            rotation[12] = 0.0f;
            rotation[13] = 0.0f;
            rotation[14] = 0.0f;
            rotation[15] = 1.0f;
        }
    }

    public static void getQuaternionFromVector(float[] quaternion, float[] rotationVector)
    {
        if (rotationVector.length >= 4)
        {
            quaternion[0] = rotationVector[3];
        }
        else
        {
            quaternion[0] = 1 - rotationVector[0] * rotationVector[0] - rotationVector[1] * rotationVector[1]
                - rotationVector[2] * rotationVector[2];
            quaternion[0] = quaternion[0] > 0 ? (float)Math.sqrt(quaternion[0]) : 0;
        }

        quaternion[1] = rotationVector[0];
        quaternion[2] = rotationVector[1];
        quaternion[3] = rotationVector[2];
    }

    public abstract static class DynamicSensorCallback
    {
        public DynamicSensorCallback()
        {
        }

        public void onDynamicSensorConnected(Sensor sensor)
        {
        }

        public void onDynamicSensorDisconnected(Sensor sensor)
        {
        }
    }
}
