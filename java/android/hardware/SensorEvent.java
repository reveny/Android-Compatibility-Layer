package android.hardware;

public class SensorEvent
{
    public final float[] values;
    public Sensor sensor;
    public int accuracy;
    public long timestamp;
    public boolean firstEventAfterDiscontinuity;

    public SensorEvent(int valueCount)
    {
        values = new float[valueCount];
    }
}
