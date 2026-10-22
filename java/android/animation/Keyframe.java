package android.animation;

public abstract class Keyframe implements Cloneable
{
    float mFraction;
    Class mValueType;
    boolean mHasValue;
    boolean mValueWasSetOnStart;

    private TimeInterpolator mInterpolator;

    public static Keyframe ofInt(float fraction, int value)
    {
        return new IntKeyframe(fraction, value);
    }

    public static Keyframe ofInt(float fraction)
    {
        return new IntKeyframe(fraction);
    }

    public static Keyframe ofFloat(float fraction, float value)
    {
        return new FloatKeyframe(fraction, value);
    }

    public static Keyframe ofFloat(float fraction)
    {
        return new FloatKeyframe(fraction);
    }

    public static Keyframe ofObject(float fraction, Object value)
    {
        return new ObjectKeyframe(fraction, value);
    }

    public static Keyframe ofObject(float fraction)
    {
        return new ObjectKeyframe(fraction, null);
    }

    public boolean hasValue()
    {
        return mHasValue;
    }

    boolean valueWasSetOnStart()
    {
        return mValueWasSetOnStart;
    }

    void setValueWasSetOnStart(boolean valueWasSetOnStart)
    {
        mValueWasSetOnStart = valueWasSetOnStart;
    }

    public abstract Object getValue();

    public abstract void setValue(Object value);

    public float getFraction()
    {
        return mFraction;
    }

    public void setFraction(float fraction)
    {
        mFraction = fraction;
    }

    public TimeInterpolator getInterpolator()
    {
        return mInterpolator;
    }

    public void setInterpolator(TimeInterpolator interpolator)
    {
        mInterpolator = interpolator;
    }

    public Class getType()
    {
        return mValueType;
    }

    @Override
    public abstract Keyframe clone();

    static class ObjectKeyframe extends Keyframe
    {
        Object mValue;

        ObjectKeyframe(float fraction, Object value)
        {
            mFraction = fraction;
            mValue = value;
            mHasValue = value != null;
            mValueType = mHasValue ? value.getClass() : Object.class;
        }

        @Override
        public Object getValue()
        {
            return mValue;
        }

        @Override
        public void setValue(Object value)
        {
            mValue = value;
            mHasValue = value != null;
        }

        @Override
        public ObjectKeyframe clone()
        {
            ObjectKeyframe copy = new ObjectKeyframe(getFraction(), hasValue() ? mValue : null);
            copy.mValueWasSetOnStart = mValueWasSetOnStart;
            copy.setInterpolator(getInterpolator());

            return copy;
        }
    }

    static class IntKeyframe extends Keyframe
    {
        int mValue;

        IntKeyframe(float fraction, int value)
        {
            mFraction = fraction;
            mValue = value;
            mValueType = int.class;
            mHasValue = true;
        }

        IntKeyframe(float fraction)
        {
            mFraction = fraction;
            mValueType = int.class;
        }

        public int getIntValue()
        {
            return mValue;
        }

        @Override
        public Object getValue()
        {
            return mValue;
        }

        @Override
        public void setValue(Object value)
        {
            if (value != null && (value instanceof Number))
            {
                mValue = ((Number)value).intValue();
                mHasValue = true;
            }
        }

        @Override
        public IntKeyframe clone()
        {
            IntKeyframe copy = mHasValue ? new IntKeyframe(getFraction(), mValue) : new IntKeyframe(getFraction());
            copy.setInterpolator(getInterpolator());
            copy.mValueWasSetOnStart = mValueWasSetOnStart;

            return copy;
        }
    }

    static class FloatKeyframe extends Keyframe
    {
        float mValue;

        FloatKeyframe(float fraction, float value)
        {
            mFraction = fraction;
            mValue = value;
            mValueType = float.class;
            mHasValue = true;
        }

        FloatKeyframe(float fraction)
        {
            mFraction = fraction;
            mValueType = float.class;
        }

        public float getFloatValue()
        {
            return mValue;
        }

        @Override
        public Object getValue()
        {
            return mValue;
        }

        @Override
        public void setValue(Object value)
        {
            if (value != null && (value instanceof Number))
            {
                mValue = ((Number)value).floatValue();
                mHasValue = true;
            }
        }

        @Override
        public FloatKeyframe clone()
        {
            FloatKeyframe copy = mHasValue ? new FloatKeyframe(getFraction(), mValue) : new FloatKeyframe(getFraction());
            copy.setInterpolator(getInterpolator());
            copy.mValueWasSetOnStart = mValueWasSetOnStart;

            return copy;
        }
    }
}
