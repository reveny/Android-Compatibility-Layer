package android.os;

public class Vibrator
{
    public static final int VIBRATION_EFFECT_SUPPORT_UNKNOWN = 0;
    public static final int VIBRATION_EFFECT_SUPPORT_YES = 1;
    public static final int VIBRATION_EFFECT_SUPPORT_NO = 2;

    public boolean hasVibrator()
    {
        return false;
    }

    public boolean hasAmplitudeControl()
    {
        return false;
    }

    public float getResonantFrequency()
    {
        return Float.NaN;
    }

    public float getQFactor()
    {
        return Float.NaN;
    }

    public void vibrate(long milliseconds)
    {
    }

    public void vibrate(long[] pattern, int repeat)
    {
        if (pattern != null && (repeat < -1 || repeat >= pattern.length)) throw new ArrayIndexOutOfBoundsException();
    }

    public void vibrate(VibrationEffect effect)
    {
    }

    public void vibrate(VibrationEffect effect, VibrationAttributes attributes)
    {
    }

    public void vibrate(VibrationEffect effect, android.media.AudioAttributes attributes)
    {
        vibrate(effect);
    }

    public void vibrate(long milliseconds, android.media.AudioAttributes attributes)
    {
        vibrate(milliseconds);
    }

    public void vibrate(long[] pattern, int repeat, android.media.AudioAttributes attributes)
    {
        vibrate(pattern, repeat);
    }

    public void cancel()
    {
    }

    public final boolean areAllPrimitivesSupported(int... primitiveIds)
    {
        if (primitiveIds == null) return true;

        for (boolean supported : arePrimitivesSupported(primitiveIds))
        {
            if (!supported) return false;
        }
        return true;
    }

    public final int areAllEffectsSupported(int... effectIds)
    {
        if (effectIds == null || effectIds.length == 0) return VIBRATION_EFFECT_SUPPORT_YES;
        return VIBRATION_EFFECT_SUPPORT_NO;
    }

    public boolean[] arePrimitivesSupported(int... primitiveIds)
    {
        return new boolean[primitiveIds == null ? 0 : primitiveIds.length];
    }

    public int[] areEffectsSupported(int... effectIds)
    {
        int[] support = new int[effectIds == null ? 0 : effectIds.length];
        java.util.Arrays.fill(support, VIBRATION_EFFECT_SUPPORT_NO);
        return support;
    }

    public int[] getPrimitiveDurations(int... primitiveIds)
    {
        return new int[primitiveIds == null ? 0 : primitiveIds.length];
    }

    public int getId()
    {
        return 0;
    }
}
