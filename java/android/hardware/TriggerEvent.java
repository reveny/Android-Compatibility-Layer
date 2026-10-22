package android.hardware;

public final class TriggerEvent
{
    public final float[] values;
    public Sensor sensor;
    public long timestamp;

    TriggerEvent(int valueCount)
    {
        values = new float[valueCount];
    }
}
