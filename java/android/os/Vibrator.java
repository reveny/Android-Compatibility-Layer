package android.os;

public class Vibrator
{
    public boolean hasVibrator()
    {
        return false;
    }

    public boolean hasAmplitudeControl()
    {
        return false;
    }

    public void vibrate(long milliseconds)
    {
    }

    public void vibrate(long[] pattern, int repeat)
    {
    }

    public void cancel()
    {
    }

    public boolean areAllPrimitivesSupported(int... primitiveIds)
    {
        return false;
    }

    public boolean areAllEffectsSupported(int... effectIds)
    {
        return false;
    }

    public int[] arePrimitivesSupported(int... primitiveIds)
    {
        return new int[primitiveIds == null ? 0 : primitiveIds.length];
    }

    public int[] areEffectsSupported(int... effectIds)
    {
        return new int[effectIds == null ? 0 : effectIds.length];
    }

    public int getId()
    {
        return 0;
    }
}
