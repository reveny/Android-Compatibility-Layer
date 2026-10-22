package android.media.audiofx;

public class AcousticEchoCanceler extends AudioEffect
{
    private AcousticEchoCanceler(int audioSession) throws IllegalArgumentException, UnsupportedOperationException, RuntimeException
    {
        super(EFFECT_TYPE_AEC, EFFECT_TYPE_NULL, 0, audioSession);
    }

    public static AcousticEchoCanceler create(int audioSession)
    {
        try
        {
            return new AcousticEchoCanceler(audioSession);
        }
        catch (RuntimeException exception)
        {
            return null;
        }
    }

    public static boolean isAvailable()
    {
        return isEffectTypeAvailable(EFFECT_TYPE_AEC);
    }
}
