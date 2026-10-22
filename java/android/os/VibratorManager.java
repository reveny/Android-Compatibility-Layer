package android.os;

public class VibratorManager
{
    private final Vibrator mDefaultVibrator = new Vibrator();

    public VibratorManager(android.content.Context context)
    {
    }

    public int[] getVibratorIds()
    {
        return new int[0];
    }

    public Vibrator getVibrator(int vibratorId)
    {
        return mDefaultVibrator;
    }

    public Vibrator getDefaultVibrator()
    {
        return mDefaultVibrator;
    }

    public final void vibrate(CombinedVibration effect)
    {
        vibrate(effect, null);
    }

    public final void vibrate(CombinedVibration effect, VibrationAttributes attributes)
    {
        if (effect == null) throw new NullPointerException("effect");
    }

    public void cancel()
    {
    }
}
