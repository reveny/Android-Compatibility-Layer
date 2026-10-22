package android.animation;

public class IntArrayEvaluator implements TypeEvaluator<int[]>
{
    private int[] mArray;

    public IntArrayEvaluator()
    {
    }

    public IntArrayEvaluator(int[] reuseArray)
    {
        mArray = reuseArray;
    }

    @Override
    public int[] evaluate(float fraction, int[] startValue, int[] endValue)
    {
        int[] array = mArray;
        if (array == null) array = new int[startValue.length];

        for (int index = 0; index < array.length; index++)
        {
            int start = startValue[index];
            int end = endValue[index];
            array[index] = (int)(start + (fraction * (end - start)));
        }

        return array;
    }
}
