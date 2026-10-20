package android.view.animation;

import android.graphics.Path;

public class PathInterpolator implements Interpolator
{
    private static final int SAMPLE_COUNT = 201;

    private final float[] mX = new float[SAMPLE_COUNT];
    private final float[] mY = new float[SAMPLE_COUNT];

    public PathInterpolator(float controlX, float controlY)
    {
        sampleCubic(controlX, controlY, 1.0f, 1.0f);
    }

    public PathInterpolator(float firstX, float firstY, float secondX, float secondY)
    {
        sampleCubic(firstX, firstY, secondX, secondY);
    }

    public PathInterpolator(Path path)
    {
        sampleCubic(0.25f, 0.1f, 0.25f, 1.0f);
    }

    private void sampleCubic(float firstX, float firstY, float secondX, float secondY)
    {
        for (int index = 0; index < SAMPLE_COUNT; index++)
        {
            float t = index / (float)(SAMPLE_COUNT - 1);

            mX[index] = cubic(t, 0, firstX, secondX, 1);
            mY[index] = cubic(t, 0, firstY, secondY, 1);
        }
    }

    private static float cubic(float t, float p0, float p1, float p2, float p3)
    {
        float inverse = 1 - t;

        return inverse * inverse * inverse * p0
                   + 3 * inverse * inverse * t * p1
                   + 3 * inverse * t * t * p2
                   + t * t * t * p3;
    }

    @Override
    public float getInterpolation(float input)
    {
        if (input <= 0) return 0;
        if (input >= 1) return 1;

        int low = 0;
        int high = SAMPLE_COUNT - 1;

        while (high - low > 1)
        {
            int middle = (low + high) / 2;

            if (mX[middle] <= input) low = middle;
            else high = middle;
        }

        float span = mX[high] - mX[low];
        if (span == 0) return mY[low];

        float fraction = (input - mX[low]) / span;

        return mY[low] + fraction * (mY[high] - mY[low]);
    }
}
