package android.media.audiofx;

public class LoudnessEnhancer extends AudioEffect
{
    public static final int PARAM_TARGET_GAIN_MB = 0;

    private int mTargetGainMillibels;

    public LoudnessEnhancer(int audioSession)
        throws IllegalArgumentException, IllegalStateException, RuntimeException, UnsupportedOperationException
    {
        super(EFFECT_TYPE_LOUDNESS_ENHANCER, EFFECT_TYPE_NULL, 0, audioSession);
    }

    public float getTargetGain() throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException
    {
        checkState("getTargetGain()");
        return mTargetGainMillibels;
    }

    public void setTargetGain(int gainmB) throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException
    {
        checkState("setTargetGain()");
        mTargetGainMillibels = gainmB;
    }
}
