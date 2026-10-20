package android.animation;

import java.lang.reflect.Method;

public final class ObjectAnimator extends ValueAnimator
{
    private Object mTarget;
    private String mPropertyName;
    private Method mSetter;
    private boolean mSetterResolved;

    public ObjectAnimator()
    {
    }

    public static ObjectAnimator ofFloat(Object target, String propertyName, float... values)
    {
        ObjectAnimator animator = new ObjectAnimator();

        animator.mTarget = target;
        animator.mPropertyName = propertyName;
        animator.setFloatValues(values);

        return animator;
    }

    public static ObjectAnimator ofInt(Object target, String propertyName, int... values)
    {
        ObjectAnimator animator = new ObjectAnimator();

        animator.mTarget = target;
        animator.mPropertyName = propertyName;
        animator.setIntValues(values);

        return animator;
    }

    public static ObjectAnimator ofArgb(Object target, String propertyName, int... values)
    {
        return ofInt(target, propertyName, values);
    }

    public static <T> ObjectAnimator ofFloat(T target, android.util.Property<T, Float> property, float... values)
    {
        ObjectAnimator animator = new ObjectAnimator();

        animator.mTarget = target;
        animator.setProperty(property);
        animator.setFloatValues(values);

        return animator;
    }

    public static <T> ObjectAnimator ofInt(T target, android.util.Property<T, Integer> property, int... values)
    {
        ObjectAnimator animator = new ObjectAnimator();

        animator.mTarget = target;
        animator.setProperty(property);
        animator.setIntValues(values);

        return animator;
    }

    public static <T> ObjectAnimator ofArgb(T target, android.util.Property<T, Integer> property, int... values)
    {
        return ofInt(target, property, values);
    }

    @SuppressWarnings("unchecked")
    public void setProperty(android.util.Property property)
    {
        mProperty = property;
        mPropertyName = property == null ? null : property.getName();
    }

    public android.util.Property getProperty()
    {
        return mProperty;
    }

    private android.util.Property mProperty;

    public static ObjectAnimator ofPropertyValuesHolder(Object target, PropertyValuesHolder... values)
    {
        ObjectAnimator animator = new ObjectAnimator();

        animator.mTarget = target;

        if (values != null && values.length > 0)
        {
            animator.mPropertyName = values[0].getPropertyName();
            values[0].applyTo(animator);
        }

        return animator;
    }

    public void setTarget(Object target)
    {
        if (mTarget == target) return;

        mTarget = target;
        mSetter = null;
        mSetterResolved = false;
    }

    public Object getTarget()
    {
        return mTarget;
    }

    public void setPropertyName(String propertyName)
    {
        mPropertyName = propertyName;
        mSetter = null;
        mSetterResolved = false;
    }

    public String getPropertyName()
    {
        return mPropertyName;
    }

    @Override
    public ObjectAnimator setDuration(long duration)
    {
        super.setDuration(duration);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onAnimatedValue(float value)
    {
        if (mTarget == null) return;

        if (mProperty != null)
        {
            if (mProperty instanceof android.util.FloatProperty)
            {
                ((android.util.FloatProperty)mProperty).setValue(mTarget, value);
                return;
            }

            if (mProperty instanceof android.util.IntProperty)
            {
                ((android.util.IntProperty)mProperty).setValue(mTarget, Math.round(value));
                return;
            }

            Class<?> type = mProperty.getType();

            if (type == Integer.class) mProperty.set(mTarget, Integer.valueOf(Math.round(value)));
            else mProperty.set(mTarget, Float.valueOf(value));

            return;
        }

        if (mPropertyName == null) return;

        if (mTarget instanceof android.view.View && applyToView((android.view.View)mTarget, value)) return;

        invokeSetter(value);
    }

    private boolean applyToView(android.view.View view, float value)
    {
        if ("alpha".equals(mPropertyName))
        {
            view.setAlpha(value);
            return true;
        }

        if ("translationX".equals(mPropertyName))
        {
            view.setTranslationX(value);
            return true;
        }

        if ("translationY".equals(mPropertyName))
        {
            view.setTranslationY(value);
            return true;
        }

        if ("scaleX".equals(mPropertyName))
        {
            view.setScaleX(value);
            return true;
        }

        if ("scaleY".equals(mPropertyName))
        {
            view.setScaleY(value);
            return true;
        }

        if ("rotation".equals(mPropertyName))
        {
            view.setRotation(value);
            return true;
        }

        if ("rotationX".equals(mPropertyName))
        {
            view.setRotationX(value);
            return true;
        }

        if ("rotationY".equals(mPropertyName))
        {
            view.setRotationY(value);
            return true;
        }

        if ("x".equals(mPropertyName))
        {
            view.setX(value);
            return true;
        }

        if ("y".equals(mPropertyName))
        {
            view.setY(value);
            return true;
        }

        if ("scrollX".equals(mPropertyName))
        {
            view.setScrollX(Math.round(value));
            return true;
        }

        if ("scrollY".equals(mPropertyName))
        {
            view.setScrollY(Math.round(value));
            return true;
        }

        return false;
    }

    private void invokeSetter(float value)
    {
        if (!mSetterResolved) resolveSetter();

        if (mSetter == null) return;

        try
        {
            Class<?> parameter = mSetter.getParameterTypes()[0];

            if (parameter == int.class) mSetter.invoke(mTarget, Math.round(value));
            else mSetter.invoke(mTarget, value);
        }
        catch (ReflectiveOperationException failure)
        {
            mSetter = null;
        }
    }

    private void resolveSetter()
    {
        mSetterResolved = true;

        String name = "set" + Character.toUpperCase(mPropertyName.charAt(0)) + mPropertyName.substring(1);

        try
        {
            mSetter = mTarget.getClass().getMethod(name, float.class);
            return;
        }
        catch (NoSuchMethodException ignored)
        {
        }

        try
        {
            mSetter = mTarget.getClass().getMethod(name, int.class);
        }
        catch (NoSuchMethodException ignored)
        {
            android.util.Log.w("ObjectAnimator", "no setter " + name + " on " + mTarget.getClass().getName());
        }
    }
}
