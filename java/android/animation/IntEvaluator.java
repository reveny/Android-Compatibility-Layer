package android.animation;

public class IntEvaluator implements TypeEvaluator<Integer>
{
    @Override
    public Integer evaluate(float fraction, Integer startValue, Integer endValue)
    {
        int start = startValue == null ? 0 : startValue.intValue();
        int end = endValue == null ? 0 : endValue.intValue();

        return Integer.valueOf(start + (int)(fraction * (end - start)));
    }
}
