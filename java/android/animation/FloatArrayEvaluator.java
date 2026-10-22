package android.animation;

public class FloatArrayEvaluator implements TypeEvaluator<float[]>
{
    private float[] mArray;

    public FloatArrayEvaluator()
    {
    }

    public FloatArrayEvaluator(float[] reuseArray)
    {
        mArray = reuseArray;
    }

    @Override
    public float[] evaluate(float fraction, float[] startValue, float[] endValue)
    {
        float[] array = mArray;
        if (array == null) array = new float[startValue.length];

        for (int index = 0; index < array.length; index++)
        {
            float start = startValue[index];
            float end = endValue[index];
            array[index] = start + (fraction * (end - start));
        }

        return array;
    }
}
