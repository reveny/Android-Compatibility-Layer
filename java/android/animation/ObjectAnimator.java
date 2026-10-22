package android.animation;

import android.graphics.Path;
import android.graphics.PointF;
import android.util.Property;

import java.util.ArrayList;

public final class ObjectAnimator extends ValueAnimator
{
    private static final ArrayList<ObjectAnimator> sStartedAnimators = new ArrayList<>();

    private Object mTarget;
    private String mPropertyName;
    private Property mProperty;
    private boolean mAutoCancel;

    public ObjectAnimator()
    {
    }

    private ObjectAnimator(Object target, String propertyName)
    {
        setTarget(target);
        setPropertyName(propertyName);
    }

    private <T> ObjectAnimator(T target, Property<T, ?> property)
    {
        setTarget(target);
        setProperty(property);
    }

    public static ObjectAnimator ofInt(Object target, String propertyName, int... values)
    {
        ObjectAnimator animator = new ObjectAnimator(target, propertyName);
        animator.setIntValues(values);

        return animator;
    }

    public static ObjectAnimator ofInt(Object target, String xPropertyName, String yPropertyName, Path path)
    {
        PathKeyframes keyframes = KeyframeSet.ofPath(path);
        PropertyValuesHolder x = PropertyValuesHolder.ofXInt(xPropertyName, null, keyframes, true);
        PropertyValuesHolder y = PropertyValuesHolder.ofXInt(yPropertyName, null, keyframes, false);

        return ofPropertyValuesHolder(target, x, y);
    }

    public static <T> ObjectAnimator ofInt(T target, Property<T, Integer> property, int... values)
    {
        ObjectAnimator animator = new ObjectAnimator(target, property);
        animator.setIntValues(values);

        return animator;
    }

    public static <T> ObjectAnimator ofInt(T target, Property<T, Integer> xProperty, Property<T, Integer> yProperty, Path path)
    {
        PathKeyframes keyframes = KeyframeSet.ofPath(path);
        PropertyValuesHolder x = PropertyValuesHolder.ofXInt(null, xProperty, keyframes, true);
        PropertyValuesHolder y = PropertyValuesHolder.ofXInt(null, yProperty, keyframes, false);

        return ofPropertyValuesHolder(target, x, y);
    }

    public static ObjectAnimator ofMultiInt(Object target, String propertyName, int[][] values)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofMultiInt(propertyName, values);
        return ofPropertyValuesHolder(target, holder);
    }

    public static ObjectAnimator ofMultiInt(Object target, String propertyName, Path path)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofMultiInt(propertyName, path);
        return ofPropertyValuesHolder(target, holder);
    }

    @SafeVarargs
    public static <T> ObjectAnimator ofMultiInt(Object target, String propertyName, TypeConverter<T, int[]> converter,
                                                TypeEvaluator<T> evaluator, T... values)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofMultiInt(propertyName, converter, evaluator, values);
        return ObjectAnimator.ofPropertyValuesHolder(target, holder);
    }

    public static ObjectAnimator ofArgb(Object target, String propertyName, int... values)
    {
        ObjectAnimator animator = ofInt(target, propertyName, values);
        animator.setEvaluator(ArgbEvaluator.getInstance());

        return animator;
    }

    public static <T> ObjectAnimator ofArgb(T target, Property<T, Integer> property, int... values)
    {
        ObjectAnimator animator = ofInt(target, property, values);
        animator.setEvaluator(ArgbEvaluator.getInstance());

        return animator;
    }

    public static ObjectAnimator ofFloat(Object target, String propertyName, float... values)
    {
        ObjectAnimator animator = new ObjectAnimator(target, propertyName);
        animator.setFloatValues(values);

        return animator;
    }

    public static ObjectAnimator ofFloat(Object target, String xPropertyName, String yPropertyName, Path path)
    {
        PathKeyframes keyframes = KeyframeSet.ofPath(path);
        PropertyValuesHolder x = PropertyValuesHolder.ofXFloat(xPropertyName, null, keyframes, true);
        PropertyValuesHolder y = PropertyValuesHolder.ofXFloat(yPropertyName, null, keyframes, false);

        return ofPropertyValuesHolder(target, x, y);
    }

    public static <T> ObjectAnimator ofFloat(T target, Property<T, Float> property, float... values)
    {
        ObjectAnimator animator = new ObjectAnimator(target, property);
        animator.setFloatValues(values);

        return animator;
    }

    public static <T> ObjectAnimator ofFloat(T target, Property<T, Float> xProperty, Property<T, Float> yProperty, Path path)
    {
        PathKeyframes keyframes = KeyframeSet.ofPath(path);
        PropertyValuesHolder x = PropertyValuesHolder.ofXFloat(null, xProperty, keyframes, true);
        PropertyValuesHolder y = PropertyValuesHolder.ofXFloat(null, yProperty, keyframes, false);

        return ofPropertyValuesHolder(target, x, y);
    }

    public static ObjectAnimator ofMultiFloat(Object target, String propertyName, float[][] values)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofMultiFloat(propertyName, values);
        return ofPropertyValuesHolder(target, holder);
    }

    public static ObjectAnimator ofMultiFloat(Object target, String propertyName, Path path)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofMultiFloat(propertyName, path);
        return ofPropertyValuesHolder(target, holder);
    }

    @SafeVarargs
    public static <T> ObjectAnimator ofMultiFloat(Object target, String propertyName, TypeConverter<T, float[]> converter,
                                                  TypeEvaluator<T> evaluator, T... values)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofMultiFloat(propertyName, converter, evaluator, values);
        return ObjectAnimator.ofPropertyValuesHolder(target, holder);
    }

    public static ObjectAnimator ofObject(Object target, String propertyName, TypeEvaluator evaluator, Object... values)
    {
        ObjectAnimator animator = new ObjectAnimator(target, propertyName);
        animator.setObjectValues(values);
        animator.setEvaluator(evaluator);

        return animator;
    }

    public static ObjectAnimator ofObject(Object target, String propertyName, TypeConverter<PointF, ?> converter, Path path)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofObject(propertyName, converter, path);
        return ofPropertyValuesHolder(target, holder);
    }

    @SafeVarargs
    public static <T, V> ObjectAnimator ofObject(T target, Property<T, V> property, TypeEvaluator<V> evaluator, V... values)
    {
        ObjectAnimator animator = new ObjectAnimator(target, property);
        animator.setObjectValues(values);
        animator.setEvaluator(evaluator);

        return animator;
    }

    @SafeVarargs
    public static <T, V, P> ObjectAnimator ofObject(T target, Property<T, P> property, TypeConverter<V, P> converter,
                                                    TypeEvaluator<V> evaluator, V... values)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofObject(property, converter, evaluator, values);
        return ofPropertyValuesHolder(target, holder);
    }

    public static <T, V> ObjectAnimator ofObject(T target, Property<T, V> property, TypeConverter<PointF, V> converter, Path path)
    {
        PropertyValuesHolder holder = PropertyValuesHolder.ofObject(property, converter, path);
        return ofPropertyValuesHolder(target, holder);
    }

    public static ObjectAnimator ofPropertyValuesHolder(Object target, PropertyValuesHolder... values)
    {
        ObjectAnimator animator = new ObjectAnimator();
        animator.setTarget(target);
        animator.setValues(values);

        return animator;
    }

    public void setPropertyName(String propertyName)
    {
        if (mValues != null)
        {
            PropertyValuesHolder holder = mValues[0];
            String oldName = holder.getPropertyName();
            holder.setPropertyName(propertyName);
            mValuesMap.remove(oldName);
            mValuesMap.put(propertyName, holder);
        }

        mPropertyName = propertyName;
        mInitialized = false;
    }

    public void setProperty(Property property)
    {
        if (mValues != null)
        {
            PropertyValuesHolder holder = mValues[0];
            String oldName = holder.getPropertyName();
            holder.setProperty(property);
            mValuesMap.remove(oldName);
            mValuesMap.put(mPropertyName, holder);
        }

        if (mProperty != null) mPropertyName = property.getName();

        mProperty = property;
        mInitialized = false;
    }

    public Property getProperty()
    {
        return mProperty;
    }

    public String getPropertyName()
    {
        if (mPropertyName != null) return mPropertyName;
        if (mProperty != null) return mProperty.getName();

        if (mValues != null && mValues.length > 0)
        {
            StringBuilder builder = new StringBuilder();

            for (int index = 0; index < mValues.length; index++)
            {
                if (index > 0) builder.append(',');
                builder.append(mValues[index].getPropertyName());
            }

            return builder.toString();
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setIntValues(int... values)
    {
        if (mValues == null || mValues.length == 0)
        {
            if (mProperty != null) setValues(PropertyValuesHolder.ofInt(mProperty, values));
            else setValues(PropertyValuesHolder.ofInt(mPropertyName, values));

            return;
        }

        super.setIntValues(values);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setFloatValues(float... values)
    {
        if (mValues == null || mValues.length == 0)
        {
            if (mProperty != null) setValues(PropertyValuesHolder.ofFloat(mProperty, values));
            else setValues(PropertyValuesHolder.ofFloat(mPropertyName, values));

            return;
        }

        super.setFloatValues(values);
    }

    @Override
    public void setObjectValues(Object... values)
    {
        if (mValues == null || mValues.length == 0)
        {
            if (mProperty != null) setValues(PropertyValuesHolder.ofObject(mProperty, (TypeEvaluator)null, values));
            else setValues(PropertyValuesHolder.ofObject(mPropertyName, (TypeEvaluator)null, values));

            return;
        }

        super.setObjectValues(values);
    }

    public void setAutoCancel(boolean cancel)
    {
        mAutoCancel = cancel;
    }

    private boolean hasSameTargetAndProperties(ObjectAnimator other)
    {
        PropertyValuesHolder[] theirValues = other.getValues();

        if (other.getTarget() != getTarget() || mValues == null || theirValues == null) return false;
        if (mValues.length != theirValues.length) return false;

        for (int index = 0; index < mValues.length; index++)
        {
            PropertyValuesHolder mine = mValues[index];
            PropertyValuesHolder theirs = theirValues[index];

            if (mine.getPropertyName() == null || !mine.getPropertyName().equals(theirs.getPropertyName())) return false;
        }

        return true;
    }

    @Override
    public void start()
    {
        ArrayList<ObjectAnimator> started;

        synchronized (sStartedAnimators)
        {
            for (int index = sStartedAnimators.size() - 1; index >= 0; index--)
            {
                if (!sStartedAnimators.get(index).isStarted()) sStartedAnimators.remove(index);
            }

            started = new ArrayList<>(sStartedAnimators);
        }

        if (mAutoCancel)
        {
            for (int index = 0; index < started.size(); index++)
            {
                ObjectAnimator other = started.get(index);
                if (other != this && other.isStarted() && hasSameTargetAndProperties(other)) other.cancel();
            }
        }

        super.start();

        if (isStarted())
        {
            synchronized (sStartedAnimators)
            {
                if (!sStartedAnimators.contains(this)) sStartedAnimators.add(this);
            }
        }
    }

    @Override
    void initAnimation()
    {
        if (!mInitialized)
        {
            Object target = getTarget();

            if (target != null && mValues != null)
            {
                for (int index = 0; index < mValues.length; index++) mValues[index].setupSetterAndGetter(target);
            }

            super.initAnimation();
        }
    }

    @Override
    public ObjectAnimator setDuration(long duration)
    {
        super.setDuration(duration);
        return this;
    }

    public Object getTarget()
    {
        return mTarget;
    }

    @Override
    public void setTarget(Object target)
    {
        if (mTarget == target) return;

        if (isStarted()) cancel();

        mTarget = target;
        mInitialized = false;
    }

    @Override
    public void setupStartValues()
    {
        initAnimation();

        Object target = getTarget();
        if (target == null || mValues == null) return;

        for (int index = 0; index < mValues.length; index++) mValues[index].setupStartValue(target);
    }

    @Override
    public void setupEndValues()
    {
        initAnimation();

        Object target = getTarget();
        if (target == null || mValues == null) return;

        for (int index = 0; index < mValues.length; index++) mValues[index].setupEndValue(target);
    }

    @Override
    void animateValue(float fraction)
    {
        Object target = getTarget();

        super.animateValue(fraction);

        if (target == null || mValues == null) return;

        for (int index = 0; index < mValues.length; index++) mValues[index].setAnimatedValue(target);
    }

    @Override
    public ObjectAnimator clone()
    {
        return (ObjectAnimator)super.clone();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("ObjectAnimator@" + Integer.toHexString(hashCode()) + ", target " + getTarget());

        if (mValues != null)
        {
            for (int index = 0; index < mValues.length; index++) builder.append("\n    ").append(mValues[index].toString());
        }

        return builder.toString();
    }
}
