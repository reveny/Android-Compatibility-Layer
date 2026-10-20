package android.animation;

public class FloatEvaluator implements TypeEvaluator<Number>
{
    @Override
    public Float evaluate(float fraction, Number startValue, Number endValue)
    {
        float start = startValue == null ? 0.0f : startValue.floatValue();
        float end = endValue == null ? 0.0f : endValue.floatValue();

        return Float.valueOf(start + fraction * (end - start));
    }
}
