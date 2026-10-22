package android.view.animation;

import android.content.Context;
import android.graphics.Path;
import android.util.AttributeSet;

import java.util.ArrayList;

public class PathInterpolator extends BaseInterpolator
{
    private static final int SAMPLE_COUNT = 201;
    private static final int SEGMENT_SAMPLES = 64;

    private float[] mX = new float[SAMPLE_COUNT];
    private float[] mY = new float[SAMPLE_COUNT];

    public PathInterpolator(float controlX, float controlY)
    {
        sampleQuadratic(controlX, controlY);
    }

    public PathInterpolator(float firstX, float firstY, float secondX, float secondY)
    {
        sampleCubic(firstX, firstY, secondX, secondY);
    }

    public PathInterpolator(Path path)
    {
        sampleCubic(0.4f, 0.0f, 0.2f, 1.0f);
    }

    PathInterpolator(String pathData)
    {
        if (!samplePathData(pathData)) sampleCubic(0.4f, 0.0f, 0.2f, 1.0f);
    }

    public PathInterpolator(Context context, AttributeSet attributes)
    {
        AnimationAttributes values = new AnimationAttributes(context, attributes);

        if (values.has("pathData"))
        {
            String data = attributes.getAttributeValue(null, "pathData");
            if (data != null && samplePathData(data)) return;
        }

        float firstX = values.getFloat("controlX1", 0.0f);
        float firstY = values.getFloat("controlY1", 0.0f);

        if (values.has("controlX2") || values.has("controlY2"))
        {
            sampleCubic(firstX, firstY, values.getFloat("controlX2", 1.0f), values.getFloat("controlY2", 1.0f));
            return;
        }

        sampleQuadratic(firstX, firstY);
    }

    private boolean samplePathData(String data)
    {
        ArrayList<float[]> points = new ArrayList<>();
        float[] numbers = new float[6];

        float currentX = 0;
        float currentY = 0;
        char command = 0;
        int position = 0;
        int length = data.length();

        try
        {
            while (position < length)
            {
                char character = data.charAt(position);

                if (Character.isWhitespace(character) || character == ',')
                {
                    position++;
                    continue;
                }

                if (Character.isLetter(character) && character != 'e' && character != 'E')
                {
                    command = character;
                    position++;
                    continue;
                }

                int needed = argumentCount(command);
                if (needed == 0) return false;

                for (int index = 0; index < needed; index++)
                {
                    while (position < length && (Character.isWhitespace(data.charAt(position)) || data.charAt(position) == ',')) position++;

                    int start = position;
                    if (position < length && (data.charAt(position) == '-' || data.charAt(position) == '+')) position++;

                    while (position < length)
                    {
                        char digit = data.charAt(position);
                        boolean exponentSign = (digit == '-' || digit == '+') && position > start
                                               && (data.charAt(position - 1) == 'e' || data.charAt(position - 1) == 'E');

                        if (Character.isDigit(digit) || digit == '.' || digit == 'e' || digit == 'E' || exponentSign) position++;
                        else break;
                    }

                    numbers[index] = Float.parseFloat(data.substring(start, position));
                }

                boolean relative = Character.isLowerCase(command);
                float baseX = relative ? currentX : 0;
                float baseY = relative ? currentY : 0;
                char upper = Character.toUpperCase(command);

                if (upper == 'M')
                {
                    currentX = baseX + numbers[0];
                    currentY = baseY + numbers[1];
                    if (points.isEmpty()) points.add(new float[] {currentX, currentY});
                    command = relative ? 'l' : 'L';
                }
                else if (upper == 'L')
                {
                    float endX = baseX + numbers[0];
                    float endY = baseY + numbers[1];
                    if (points.isEmpty()) points.add(new float[] {currentX, currentY});
                    points.add(new float[] {endX, endY});
                    currentX = endX;
                    currentY = endY;
                }
                else if (upper == 'C')
                {
                    if (points.isEmpty()) points.add(new float[] {currentX, currentY});

                    float firstX = baseX + numbers[0];
                    float firstY = baseY + numbers[1];
                    float secondX = baseX + numbers[2];
                    float secondY = baseY + numbers[3];
                    float endX = baseX + numbers[4];
                    float endY = baseY + numbers[5];

                    for (int step = 1; step <= SEGMENT_SAMPLES; step++)
                    {
                        float t = step / (float)SEGMENT_SAMPLES;
                        points.add(new float[] {cubic(t, currentX, firstX, secondX, endX), cubic(t, currentY, firstY, secondY, endY)});
                    }

                    currentX = endX;
                    currentY = endY;
                }
                else if (upper == 'Q')
                {
                    if (points.isEmpty()) points.add(new float[] {currentX, currentY});

                    float controlX = baseX + numbers[0];
                    float controlY = baseY + numbers[1];
                    float endX = baseX + numbers[2];
                    float endY = baseY + numbers[3];

                    for (int step = 1; step <= SEGMENT_SAMPLES; step++)
                    {
                        float t = step / (float)SEGMENT_SAMPLES;
                        float inverse = 1 - t;
                        points.add(new float[] {inverse * inverse * currentX + 2 * inverse * t * controlX + t * t * endX,
                                                inverse * inverse * currentY + 2 * inverse * t * controlY + t * t * endY});
                    }

                    currentX = endX;
                    currentY = endY;
                }
                else
                {
                    return false;
                }
            }
        }
        catch (RuntimeException failure)
        {
            return false;
        }

        if (points.size() < 2) return false;

        float[] first = points.get(0);
        float[] last = points.get(points.size() - 1);
        if (Math.abs(first[0]) > 0.01f || Math.abs(first[1]) > 0.01f || Math.abs(last[0] - 1) > 0.01f || Math.abs(last[1] - 1) > 0.01f)
        {
            return false;
        }

        int count = points.size();
        mX = new float[count];
        mY = new float[count];

        float previousX = 0;
        for (int index = 0; index < count; index++)
        {
            float[] point = points.get(index);
            previousX = Math.max(previousX, point[0]);
            mX[index] = previousX;
            mY[index] = point[1];
        }

        return true;
    }

    private static int argumentCount(char command)
    {
        switch (Character.toUpperCase(command))
        {
            case 'M':
            case 'L':
                return 2;
            case 'Q':
                return 4;
            case 'C':
                return 6;
            default:
                return 0;
        }
    }

    private void sampleQuadratic(float controlX, float controlY)
    {
        for (int index = 0; index < SAMPLE_COUNT; index++)
        {
            float t = index / (float)(SAMPLE_COUNT - 1);
            float inverse = 1 - t;

            mX[index] = 2 * inverse * t * controlX + t * t;
            mY[index] = 2 * inverse * t * controlY + t * t;
        }
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
        int high = mX.length - 1;

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
