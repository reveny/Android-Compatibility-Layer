package android.media.audiofx;

public class NoiseSuppressor extends AudioEffect
{
    private NoiseSuppressor(int audioSession) throws IllegalArgumentException, UnsupportedOperationException, RuntimeException
    {
        super(EFFECT_TYPE_NS, EFFECT_TYPE_NULL, 0, audioSession);
    }

    public static NoiseSuppressor create(int audioSession)
    {
        try
        {
            return new NoiseSuppressor(audioSession);
        }
        catch (RuntimeException exception)
        {
            return null;
        }
    }

    public static boolean isAvailable()
    {
        return isEffectTypeAvailable(EFFECT_TYPE_NS);
    }
}
