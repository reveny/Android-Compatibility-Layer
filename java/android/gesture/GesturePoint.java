package android.gesture;

public class GesturePoint
{
    public final float x;
    public final float y;
    public final long timestamp;

    public GesturePoint(float x, float y, long t)
    {
        this.x = x;
        this.y = y;
        timestamp = t;
    }

    @Override
    public Object clone()
    {
        return new GesturePoint(x, y, timestamp);
    }
}
