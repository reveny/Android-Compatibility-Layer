package android.animation;

import android.graphics.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class KeyframeSet implements Keyframes
{
    int mNumKeyframes;
    Keyframe mFirstKeyframe;
    Keyframe mLastKeyframe;
    TimeInterpolator mInterpolator;
    List<Keyframe> mKeyframes;
    TypeEvaluator mEvaluator;

    KeyframeSet(Keyframe... keyframes)
    {
        mNumKeyframes = keyframes.length;
        mKeyframes = new ArrayList<>(Arrays.asList(keyframes));
        mFirstKeyframe = keyframes[0];
        mLastKeyframe = keyframes[mNumKeyframes - 1];
        mInterpolator = mLastKeyframe.getInterpolator();
    }

    @Override
    public List<Keyframe> getKeyframes()
    {
        return mKeyframes;
    }

    static KeyframeSet ofInt(int... values)
    {
        int count = values.length;
        Keyframe[] keyframes = new Keyframe[Math.max(count, 2)];

        if (count == 1)
        {
            keyframes[0] = Keyframe.ofInt(0.0f);
            keyframes[1] = Keyframe.ofInt(1.0f, values[0]);
        }
        else
        {
            keyframes[0] = Keyframe.ofInt(0.0f, values[0]);
            for (int index = 1; index < count; index++) keyframes[index] = Keyframe.ofInt((float)index / (count - 1), values[index]);
        }

        return new KeyframeSet(keyframes);
    }

    static KeyframeSet ofFloat(float... values)
    {
        int count = values.length;
        Keyframe[] keyframes = new Keyframe[Math.max(count, 2)];

        if (count == 1)
        {
            keyframes[0] = Keyframe.ofFloat(0.0f);
            keyframes[1] = Keyframe.ofFloat(1.0f, values[0]);

            if (Float.isNaN(values[0])) android.util.Log.w("Animator", "Bad value (NaN) in float animator");
        }
        else
        {
            keyframes[0] = Keyframe.ofFloat(0.0f, values[0]);

            for (int index = 1; index < count; index++)
            {
                keyframes[index] = Keyframe.ofFloat((float)index / (count - 1), values[index]);
                if (Float.isNaN(values[index])) android.util.Log.w("Animator", "Bad value (NaN) in float animator");
            }
        }

        return new KeyframeSet(keyframes);
    }

    static KeyframeSet ofKeyframe(Keyframe... keyframes)
    {
        int count = keyframes.length;
        boolean hasFloat = false;
        boolean hasInt = false;
        boolean hasOther = false;

        for (int index = 0; index < count; index++)
        {
            if (keyframes[index] instanceof Keyframe.FloatKeyframe) hasFloat = true;
            else if (keyframes[index] instanceof Keyframe.IntKeyframe) hasInt = true;
            else hasOther = true;
        }

        if (hasFloat && !hasInt && !hasOther) return new KeyframeSet(keyframes);
        if (hasInt && !hasFloat && !hasOther) return new KeyframeSet(keyframes);

        if (hasFloat && hasInt && !hasOther)
        {
            Keyframe[] floats = new Keyframe[count];

            for (int index = 0; index < count; index++)
            {
                Keyframe keyframe = keyframes[index];

                if (keyframe instanceof Keyframe.IntKeyframe && keyframe.hasValue())
                {
                    Keyframe converted = Keyframe.ofFloat(keyframe.getFraction(), ((Keyframe.IntKeyframe)keyframe).getIntValue());
                    converted.setInterpolator(keyframe.getInterpolator());
                    floats[index] = converted;
                }
                else if (keyframe instanceof Keyframe.IntKeyframe)
                {
                    Keyframe converted = Keyframe.ofFloat(keyframe.getFraction());
                    converted.setInterpolator(keyframe.getInterpolator());
                    floats[index] = converted;
                }
                else
                {
                    floats[index] = keyframe;
                }
            }

            return new KeyframeSet(floats);
        }

        return new KeyframeSet(keyframes);
    }

    static KeyframeSet ofObject(Object... values)
    {
        int count = values.length;
        Keyframe[] keyframes = new Keyframe[Math.max(count, 2)];

        if (count == 1)
        {
            keyframes[0] = Keyframe.ofObject(0.0f);
            keyframes[1] = Keyframe.ofObject(1.0f, values[0]);
        }
        else
        {
            keyframes[0] = Keyframe.ofObject(0.0f, values[0]);
            for (int index = 1; index < count; index++) keyframes[index] = Keyframe.ofObject((float)index / (count - 1), values[index]);
        }

        return new KeyframeSet(keyframes);
    }

    static PathKeyframes ofPath(Path path)
    {
        return new PathKeyframes(path);
    }

    static PathKeyframes ofPath(Path path, float error)
    {
        return new PathKeyframes(path, error);
    }

    @Override
    public void setEvaluator(TypeEvaluator evaluator)
    {
        mEvaluator = evaluator;
    }

    @Override
    public Class getType()
    {
        return mFirstKeyframe.getType();
    }

    @Override
    public KeyframeSet clone()
    {
        int count = mKeyframes.size();
        Keyframe[] copies = new Keyframe[count];

        for (int index = 0; index < count; index++) copies[index] = mKeyframes.get(index).clone();

        KeyframeSet copy = new KeyframeSet(copies);
        copy.mEvaluator = mEvaluator;

        return copy;
    }

    @Override
    public Object getValue(float fraction)
    {
        if (mNumKeyframes == 2)
        {
            if (mInterpolator != null) fraction = mInterpolator.getInterpolation(fraction);

            return evaluate(fraction, mFirstKeyframe, mLastKeyframe);
        }

        if (fraction <= 0.0f)
        {
            Keyframe next = mKeyframes.get(1);
            TimeInterpolator interpolator = next.getInterpolator();
            if (interpolator != null) fraction = interpolator.getInterpolation(fraction);

            float previousFraction = mFirstKeyframe.getFraction();
            float intervalFraction = (fraction - previousFraction) / (next.getFraction() - previousFraction);

            return evaluate(intervalFraction, mFirstKeyframe, next);
        }

        if (fraction >= 1.0f)
        {
            Keyframe previous = mKeyframes.get(mNumKeyframes - 2);
            TimeInterpolator interpolator = mLastKeyframe.getInterpolator();
            if (interpolator != null) fraction = interpolator.getInterpolation(fraction);

            float previousFraction = previous.getFraction();
            float intervalFraction = (fraction - previousFraction) / (mLastKeyframe.getFraction() - previousFraction);

            return evaluate(intervalFraction, previous, mLastKeyframe);
        }

        Keyframe previous = mFirstKeyframe;

        for (int index = 1; index < mNumKeyframes; index++)
        {
            Keyframe next = mKeyframes.get(index);

            if (fraction < next.getFraction())
            {
                TimeInterpolator interpolator = next.getInterpolator();
                float previousFraction = previous.getFraction();
                float intervalFraction = (fraction - previousFraction) / (next.getFraction() - previousFraction);

                if (interpolator != null) intervalFraction = interpolator.getInterpolation(intervalFraction);

                return evaluate(intervalFraction, previous, next);
            }

            previous = next;
        }

        return mLastKeyframe.getValue();
    }

    @SuppressWarnings("unchecked")
    private Object evaluate(float fraction, Keyframe start, Keyframe end)
    {
        if (mEvaluator != null) return mEvaluator.evaluate(fraction, start.getValue(), end.getValue());

        if (start instanceof Keyframe.FloatKeyframe && end instanceof Keyframe.FloatKeyframe)
        {
            float from = ((Keyframe.FloatKeyframe)start).getFloatValue();
            float to = ((Keyframe.FloatKeyframe)end).getFloatValue();

            return from + fraction * (to - from);
        }

        if (start instanceof Keyframe.IntKeyframe && end instanceof Keyframe.IntKeyframe)
        {
            int from = ((Keyframe.IntKeyframe)start).getIntValue();
            int to = ((Keyframe.IntKeyframe)end).getIntValue();

            return from + (int)(fraction * (to - from));
        }

        return fraction < 1.0f ? start.getValue() : end.getValue();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(" ");

        for (int index = 0; index < mNumKeyframes; index++) builder.append(mKeyframes.get(index).getValue()).append("  ");

        return builder.toString();
    }
}
