package android.animation;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

class PathKeyframes implements Keyframes
{
    private static final int FRACTION_OFFSET = 0;
    private static final int X_OFFSET = 1;
    private static final int Y_OFFSET = 2;
    private static final int NUM_COMPONENTS = 3;
    private static final float DEFAULT_ERROR = 0.5f;

    private static final ArrayList<Keyframe> EMPTY_KEYFRAMES = new ArrayList<>();

    private final PointF mTempPointF = new PointF();
    private final float[] mKeyframeData;

    PathKeyframes(Path path)
    {
        this(path, DEFAULT_ERROR);
    }

    PathKeyframes(Path path, float error)
    {
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("The path must not be null or empty");

        mKeyframeData = path.approximate(error);
    }

    @Override
    public List<Keyframe> getKeyframes()
    {
        return EMPTY_KEYFRAMES;
    }

    @Override
    public Object getValue(float fraction)
    {
        int numPoints = mKeyframeData.length / NUM_COMPONENTS;

        if (fraction < 0.0f) return interpolateInRange(fraction, 0, 1);
        if (fraction > 1.0f) return interpolateInRange(fraction, numPoints - 2, numPoints - 1);
        if (fraction == 0.0f) return pointForIndex(0);
        if (fraction == 1.0f) return pointForIndex(numPoints - 1);

        int low = 0;
        int high = numPoints - 1;

        while (low <= high)
        {
            int middle = (low + high) / 2;
            float middleFraction = mKeyframeData[(middle * NUM_COMPONENTS) + FRACTION_OFFSET];

            if (fraction < middleFraction) high = middle - 1;
            else if (fraction > middleFraction) low = middle + 1;
            else return pointForIndex(middle);
        }

        return interpolateInRange(fraction, high, low);
    }

    private PointF interpolateInRange(float fraction, int startIndex, int endIndex)
    {
        int startBase = startIndex * NUM_COMPONENTS;
        int endBase = endIndex * NUM_COMPONENTS;

        float startFraction = mKeyframeData[startBase + FRACTION_OFFSET];
        float endFraction = mKeyframeData[endBase + FRACTION_OFFSET];

        float span = endFraction - startFraction;
        float intervalFraction = span == 0.0f ? 0.0f : (fraction - startFraction) / span;

        float startX = mKeyframeData[startBase + X_OFFSET];
        float endX = mKeyframeData[endBase + X_OFFSET];
        float startY = mKeyframeData[startBase + Y_OFFSET];
        float endY = mKeyframeData[endBase + Y_OFFSET];

        mTempPointF.set(startX + intervalFraction * (endX - startX), startY + intervalFraction * (endY - startY));
        return mTempPointF;
    }

    private PointF pointForIndex(int index)
    {
        int base = index * NUM_COMPONENTS;
        mTempPointF.set(mKeyframeData[base + X_OFFSET], mKeyframeData[base + Y_OFFSET]);

        return mTempPointF;
    }

    @Override
    public void setEvaluator(TypeEvaluator evaluator)
    {
    }

    @Override
    public Class getType()
    {
        return PointF.class;
    }

    @Override
    public PathKeyframes clone()
    {
        return this;
    }

    Keyframes createXFloatKeyframes()
    {
        return new FloatKeyframesBase()
        {
            @Override
            public float getFloatValue(float fraction)
            {
                PointF point = (PointF)PathKeyframes.this.getValue(fraction);
                return point.x;
            }
        };
    }

    Keyframes createYFloatKeyframes()
    {
        return new FloatKeyframesBase()
        {
            @Override
            public float getFloatValue(float fraction)
            {
                PointF point = (PointF)PathKeyframes.this.getValue(fraction);
                return point.y;
            }
        };
    }

    Keyframes createXIntKeyframes()
    {
        return new IntKeyframesBase()
        {
            @Override
            public int getIntValue(float fraction)
            {
                PointF point = (PointF)PathKeyframes.this.getValue(fraction);
                return Math.round(point.x);
            }
        };
    }

    Keyframes createYIntKeyframes()
    {
        return new IntKeyframesBase()
        {
            @Override
            public int getIntValue(float fraction)
            {
                PointF point = (PointF)PathKeyframes.this.getValue(fraction);
                return Math.round(point.y);
            }
        };
    }

    private abstract static class SimpleKeyframes implements Keyframes
    {
        @Override
        public void setEvaluator(TypeEvaluator evaluator)
        {
        }

        @Override
        public List<Keyframe> getKeyframes()
        {
            return EMPTY_KEYFRAMES;
        }

        @Override
        public Keyframes clone()
        {
            return this;
        }
    }

    abstract static class IntKeyframesBase extends SimpleKeyframes
    {
        abstract int getIntValue(float fraction);

        @Override
        public Class getType()
        {
            return Integer.class;
        }

        @Override
        public Object getValue(float fraction)
        {
            return getIntValue(fraction);
        }
    }

    abstract static class FloatKeyframesBase extends SimpleKeyframes
    {
        abstract float getFloatValue(float fraction);

        @Override
        public Class getType()
        {
            return Float.class;
        }

        @Override
        public Object getValue(float fraction)
        {
            return getFloatValue(fraction);
        }
    }
}
