package android.animation;

public class PropertyValuesHolder
{
    private String mPropertyName;
    private float[] mFloatValues;
    private int[] mIntValues;
    private boolean mIntegral;

    private PropertyValuesHolder(String propertyName)
    {
        mPropertyName = propertyName;
    }

    public static PropertyValuesHolder ofFloat(String propertyName, float... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);

        holder.mFloatValues = values == null ? null : values.clone();
        holder.mIntegral = false;

        return holder;
    }

    public static PropertyValuesHolder ofInt(String propertyName, int... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);

        holder.mIntValues = values == null ? null : values.clone();
        holder.mIntegral = true;

        return holder;
    }

    public String getPropertyName()
    {
        return mPropertyName;
    }

    public void setPropertyName(String propertyName)
    {
        mPropertyName = propertyName;
    }

    public void setFloatValues(float... values)
    {
        mFloatValues = values == null ? null : values.clone();
        mIntegral = false;
    }

    public void setIntValues(int... values)
    {
        mIntValues = values == null ? null : values.clone();
        mIntegral = true;
    }

    void applyTo(ValueAnimator animator)
    {
        if (mIntegral)
        {
            if (mIntValues != null) animator.setIntValues(mIntValues);
            return;
        }

        if (mFloatValues != null) animator.setFloatValues(mFloatValues);
    }

    @Override
    public PropertyValuesHolder clone()
    {
        PropertyValuesHolder copy = new PropertyValuesHolder(mPropertyName);

        copy.mFloatValues = mFloatValues == null ? null : mFloatValues.clone();
        copy.mIntValues = mIntValues == null ? null : mIntValues.clone();
        copy.mIntegral = mIntegral;

        return copy;
    }
}
