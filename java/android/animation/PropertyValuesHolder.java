package android.animation;

import android.graphics.Path;
import android.graphics.PointF;
import android.util.Log;
import android.util.Property;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class PropertyValuesHolder implements Cloneable
{
    private static final String TAG = "PropertyValuesHolder";

    private static final int MULTI_NONE = 0;
    private static final int MULTI_FLOAT = 1;
    private static final int MULTI_INT = 2;

    private static final Class[] FLOAT_VARIANTS = {float.class, Float.class, double.class, int.class, Double.class, Integer.class};
    private static final Class[] INTEGER_VARIANTS = {int.class, Integer.class, float.class, double.class, Float.class, Double.class};
    private static final Class[] DOUBLE_VARIANTS = {double.class, Double.class, float.class, int.class, Float.class, Integer.class};

    private static final TypeEvaluator sIntEvaluator = new IntEvaluator();
    private static final TypeEvaluator sFloatEvaluator = new FloatEvaluator();

    String mPropertyName;
    Property mProperty;
    Method mSetter;
    Method mGetter;
    Class mValueType;
    Keyframes mKeyframes;

    private TypeEvaluator mEvaluator;
    private TypeConverter mConverter;
    private Object mAnimatedValue;
    private int mMultiDimension = MULTI_NONE;
    private boolean mMultiSetterResolved;

    PropertyValuesHolder()
    {
    }

    private PropertyValuesHolder(String propertyName)
    {
        mPropertyName = propertyName;
    }

    private PropertyValuesHolder(Property property)
    {
        mProperty = property;
        if (property != null) mPropertyName = property.getName();
    }

    public static PropertyValuesHolder ofInt(String propertyName, int... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.setIntValues(values);

        return holder;
    }

    public static PropertyValuesHolder ofInt(Property<?, Integer> property, int... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(property);
        holder.setIntValues(values);

        return holder;
    }

    public static PropertyValuesHolder ofMultiInt(String propertyName, int[][] values)
    {
        if (values.length < 2) throw new IllegalArgumentException("At least 2 values must be supplied");

        int numParameters = 0;

        for (int index = 0; index < values.length; index++)
        {
            if (values[index] == null) throw new IllegalArgumentException("values must not be null");

            int length = values[index].length;
            if (index == 0) numParameters = length;
            else if (length != numParameters) throw new IllegalArgumentException("Values must all have the same length");
        }

        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mMultiDimension = MULTI_INT;
        holder.setEvaluator(new IntArrayEvaluator(new int[numParameters]));
        holder.setObjectValues((Object[])values);

        return holder;
    }

    public static PropertyValuesHolder ofMultiInt(String propertyName, Path path)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mMultiDimension = MULTI_INT;
        holder.mKeyframes = KeyframeSet.ofPath(path);
        holder.mValueType = PointF.class;
        holder.setConverter(new PointToIntArrayConverter());

        return holder;
    }

    @SafeVarargs
    public static <V> PropertyValuesHolder ofMultiInt(String propertyName, TypeConverter<V, int[]> converter, TypeEvaluator<V> evaluator,
                                                      V... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mMultiDimension = MULTI_INT;
        holder.setConverter(converter);
        holder.setObjectValues(values);
        holder.setEvaluator(evaluator);

        return holder;
    }

    public static <T> PropertyValuesHolder ofMultiInt(String propertyName, TypeConverter<T, int[]> converter, TypeEvaluator<T> evaluator,
                                                      Keyframe... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mMultiDimension = MULTI_INT;
        holder.setKeyframes(values);
        holder.setConverter(converter);
        holder.setEvaluator(evaluator);

        return holder;
    }

    public static PropertyValuesHolder ofFloat(String propertyName, float... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.setFloatValues(values);

        return holder;
    }

    public static PropertyValuesHolder ofFloat(Property<?, Float> property, float... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(property);
        holder.setFloatValues(values);

        return holder;
    }

    public static PropertyValuesHolder ofMultiFloat(String propertyName, float[][] values)
    {
        if (values.length < 2) throw new IllegalArgumentException("At least 2 values must be supplied");

        int numParameters = 0;

        for (int index = 0; index < values.length; index++)
        {
            if (values[index] == null) throw new IllegalArgumentException("values must not be null");

            int length = values[index].length;
            if (index == 0) numParameters = length;
            else if (length != numParameters) throw new IllegalArgumentException("Values must all have the same length");
        }

        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mMultiDimension = MULTI_FLOAT;
        holder.setEvaluator(new FloatArrayEvaluator(new float[numParameters]));
        holder.setObjectValues((Object[])values);

        return holder;
    }

    public static PropertyValuesHolder ofMultiFloat(String propertyName, Path path)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mMultiDimension = MULTI_FLOAT;
        holder.mKeyframes = KeyframeSet.ofPath(path);
        holder.mValueType = PointF.class;
        holder.setConverter(new PointToFloatArrayConverter());

        return holder;
    }

    @SafeVarargs
    public static <V> PropertyValuesHolder ofMultiFloat(String propertyName, TypeConverter<V, float[]> converter,
                                                        TypeEvaluator<V> evaluator, V... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mMultiDimension = MULTI_FLOAT;
        holder.setConverter(converter);
        holder.setObjectValues(values);
        holder.setEvaluator(evaluator);

        return holder;
    }

    public static <T> PropertyValuesHolder ofMultiFloat(String propertyName, TypeConverter<T, float[]> converter,
                                                        TypeEvaluator<T> evaluator, Keyframe... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mMultiDimension = MULTI_FLOAT;
        holder.setKeyframes(values);
        holder.setConverter(converter);
        holder.setEvaluator(evaluator);

        return holder;
    }

    public static PropertyValuesHolder ofObject(String propertyName, TypeEvaluator evaluator, Object... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.setObjectValues(values);
        holder.setEvaluator(evaluator);

        return holder;
    }

    public static PropertyValuesHolder ofObject(String propertyName, TypeConverter<PointF, ?> converter, Path path)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.mKeyframes = KeyframeSet.ofPath(path);
        holder.mValueType = PointF.class;
        holder.setConverter(converter);

        return holder;
    }

    @SafeVarargs
    public static <V> PropertyValuesHolder ofObject(Property property, TypeEvaluator<V> evaluator, V... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(property);
        holder.setObjectValues(values);
        holder.setEvaluator(evaluator);

        return holder;
    }

    @SafeVarargs
    public static <T, V> PropertyValuesHolder ofObject(Property<?, V> property, TypeConverter<T, V> converter, TypeEvaluator<T> evaluator,
                                                       T... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(property);
        holder.setConverter(converter);
        holder.setObjectValues(values);
        holder.setEvaluator(evaluator);

        return holder;
    }

    public static <V> PropertyValuesHolder ofObject(Property<?, V> property, TypeConverter<PointF, V> converter, Path path)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(property);
        holder.mKeyframes = KeyframeSet.ofPath(path);
        holder.mValueType = PointF.class;
        holder.setConverter(converter);

        return holder;
    }

    public static PropertyValuesHolder ofKeyframe(String propertyName, Keyframe... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(propertyName);
        holder.setKeyframes(values);

        return holder;
    }

    public static PropertyValuesHolder ofKeyframe(Property property, Keyframe... values)
    {
        PropertyValuesHolder holder = new PropertyValuesHolder(property);
        holder.setKeyframes(values);

        return holder;
    }

    static PropertyValuesHolder ofXFloat(String propertyName, Property property, PathKeyframes keyframes, boolean horizontal)
    {
        PropertyValuesHolder holder = property != null ? new PropertyValuesHolder(property) : new PropertyValuesHolder(propertyName);
        holder.mKeyframes = horizontal ? keyframes.createXFloatKeyframes() : keyframes.createYFloatKeyframes();
        holder.mValueType = float.class;

        return holder;
    }

    static PropertyValuesHolder ofXInt(String propertyName, Property property, PathKeyframes keyframes, boolean horizontal)
    {
        PropertyValuesHolder holder = property != null ? new PropertyValuesHolder(property) : new PropertyValuesHolder(propertyName);
        holder.mKeyframes = horizontal ? keyframes.createXIntKeyframes() : keyframes.createYIntKeyframes();
        holder.mValueType = int.class;

        return holder;
    }

    public void setIntValues(int... values)
    {
        mValueType = int.class;
        mKeyframes = KeyframeSet.ofInt(values);
    }

    public void setFloatValues(float... values)
    {
        mValueType = float.class;
        mKeyframes = KeyframeSet.ofFloat(values);
    }

    public void setKeyframes(Keyframe... values)
    {
        int count = values.length;
        Keyframe[] keyframes = new Keyframe[Math.max(count, 2)];
        mValueType = values[0].getType();

        for (int index = 0; index < count; index++) keyframes[index] = values[index];

        if (count < 2) keyframes[1] = values[0];

        KeyframeSet set = KeyframeSet.ofKeyframe(keyframes);
        mKeyframes = set;
        mValueType = set.getType();

        if (mEvaluator != null) mKeyframes.setEvaluator(mEvaluator);
    }

    public void setObjectValues(Object... values)
    {
        mValueType = values[0] == null ? Object.class : values[0].getClass();
        mKeyframes = KeyframeSet.ofObject(values);

        if (mEvaluator != null) mKeyframes.setEvaluator(mEvaluator);
    }

    public void setConverter(TypeConverter converter)
    {
        mConverter = converter;
    }

    public void setEvaluator(TypeEvaluator evaluator)
    {
        mEvaluator = evaluator;
        if (mKeyframes != null) mKeyframes.setEvaluator(evaluator);
    }

    public void setPropertyName(String propertyName)
    {
        mPropertyName = propertyName;
        mSetter = null;
        mGetter = null;
        mMultiSetterResolved = false;
    }

    public void setProperty(Property property)
    {
        mProperty = property;
    }

    public String getPropertyName()
    {
        return mPropertyName;
    }

    Property getProperty()
    {
        return mProperty;
    }

    Object getAnimatedValue()
    {
        return mAnimatedValue;
    }

    Class getValueType()
    {
        return mValueType;
    }

    void init()
    {
        if (mEvaluator == null)
        {
            if (mValueType == Integer.class) mEvaluator = sIntEvaluator;
            else if (mValueType == Float.class) mEvaluator = sFloatEvaluator;
        }

        if (mEvaluator != null && mKeyframes != null) mKeyframes.setEvaluator(mEvaluator);
    }

    void calculateValue(float fraction)
    {
        if (mKeyframes == null) return;

        Object value = mKeyframes.getValue(fraction);
        mAnimatedValue = mConverter == null ? value : convert(value);
    }

    @SuppressWarnings("unchecked")
    private Object convert(Object value)
    {
        return mConverter.convert(value);
    }

    @SuppressWarnings("unchecked")
    private Object convertBack(Object value)
    {
        if (mConverter == null) return value;

        if (!(mConverter instanceof BidirectionalTypeConverter))
        {
            throw new IllegalArgumentException("Converter " + mConverter.getClass().getName() + " must be a BidirectionalTypeConverter");
        }

        return ((BidirectionalTypeConverter)mConverter).convertBack(value);
    }

    @SuppressWarnings("unchecked")
    void setupSetterAndGetter(Object target)
    {
        if (target == null || mKeyframes == null) return;

        if (mProperty != null)
        {
            try
            {
                Object testValue = null;
                List<Keyframe> keyframes = mKeyframes.getKeyframes();

                for (int index = 0; index < keyframes.size(); index++)
                {
                    Keyframe keyframe = keyframes.get(index);

                    if (!keyframe.hasValue() || keyframe.valueWasSetOnStart())
                    {
                        if (testValue == null) testValue = convertBack(mProperty.get(target));

                        keyframe.setValue(testValue);
                        keyframe.setValueWasSetOnStart(true);
                    }
                }

                return;
            }
            catch (ClassCastException failure)
            {
                Log.w(TAG, "No such property (" + mProperty.getName() + ") on target object " + target + ". Trying reflection instead");
                mProperty = null;
            }
        }

        if (mPropertyName == null) return;

        Class targetClass = target.getClass();

        if (mMultiDimension == MULTI_NONE && mSetter == null) setupSetter(targetClass);

        List<Keyframe> keyframes = mKeyframes.getKeyframes();

        for (int index = 0; index < keyframes.size(); index++)
        {
            Keyframe keyframe = keyframes.get(index);
            if (keyframe.hasValue() && !keyframe.valueWasSetOnStart()) continue;

            if (mGetter == null) setupGetter(targetClass);
            if (mGetter == null) return;

            try
            {
                keyframe.setValue(convertBack(mGetter.invoke(target)));
                keyframe.setValueWasSetOnStart(true);
            }
            catch (IllegalAccessException | InvocationTargetException failure)
            {
                Log.e(TAG, failure.toString());
            }
        }
    }

    void setupStartValue(Object target)
    {
        List<Keyframe> keyframes = mKeyframes == null ? null : mKeyframes.getKeyframes();
        if (keyframes != null && !keyframes.isEmpty()) setupValue(target, keyframes.get(0));
    }

    void setupEndValue(Object target)
    {
        List<Keyframe> keyframes = mKeyframes == null ? null : mKeyframes.getKeyframes();
        if (keyframes != null && !keyframes.isEmpty()) setupValue(target, keyframes.get(keyframes.size() - 1));
    }

    @SuppressWarnings("unchecked")
    private void setupValue(Object target, Keyframe keyframe)
    {
        if (target == null) return;

        if (mProperty != null)
        {
            keyframe.setValue(convertBack(mProperty.get(target)));
            return;
        }

        if (mPropertyName == null) return;

        try
        {
            if (mGetter == null) setupGetter(target.getClass());
            if (mGetter == null) return;

            keyframe.setValue(convertBack(mGetter.invoke(target)));
        }
        catch (IllegalAccessException | InvocationTargetException failure)
        {
            Log.e(TAG, failure.toString());
        }
    }

    @SuppressWarnings("unchecked")
    void setAnimatedValue(Object target)
    {
        if (target == null) return;

        if (mProperty != null)
        {
            mProperty.set(target, getAnimatedValue());
            return;
        }

        if (mMultiDimension != MULTI_NONE)
        {
            setMultiValue(target);
            return;
        }

        if (mSetter == null) return;

        try
        {
            mSetter.invoke(target, coerce(mSetter.getParameterTypes()[0], getAnimatedValue()));
        }
        catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException failure)
        {
            Log.e(TAG, failure.toString());
        }
    }

    private void setMultiValue(Object target)
    {
        Object value = getAnimatedValue();
        if (value == null || mPropertyName == null) return;

        int length = value instanceof float[] ? ((float[])value).length : value instanceof int[] ? ((int[])value).length : 0;
        if (length == 0) return;

        if (!mMultiSetterResolved)
        {
            mMultiSetterResolved = true;
            mSetter = findMultiSetter(target.getClass(), length);
        }

        if (mSetter == null) return;

        Class[] parameters = mSetter.getParameterTypes();
        Object[] arguments = new Object[parameters.length];

        for (int index = 0; index < parameters.length; index++)
        {
            Number component = value instanceof float[] ? (Number)((float[])value)[index] : (Number)((int[])value)[index];
            arguments[index] = coerce(parameters[index], component);
        }

        try
        {
            mSetter.invoke(target, arguments);
        }
        catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException failure)
        {
            Log.e(TAG, failure.toString());
        }
    }

    private Method findMultiSetter(Class targetClass, int parameterCount)
    {
        String name = getMethodName("set", mPropertyName);
        Class wanted = mMultiDimension == MULTI_INT ? int.class : float.class;
        Method fallback = null;

        for (Method method : targetClass.getMethods())
        {
            if (!method.getName().equals(name)) continue;

            Class[] parameters = method.getParameterTypes();
            if (parameters.length != parameterCount) continue;

            boolean exact = true;
            boolean numeric = true;

            for (Class parameter : parameters)
            {
                if (parameter != wanted) exact = false;
                if (parameter != int.class && parameter != float.class && parameter != double.class) numeric = false;
            }

            if (exact) return method;
            if (numeric && fallback == null) fallback = method;
        }

        if (fallback == null) Log.w(TAG, "Method " + name + " with " + parameterCount + " parameters not found on target class " + targetClass);

        return fallback;
    }

    private static Object coerce(Class type, Object value)
    {
        if (!(value instanceof Number)) return value;

        Number number = (Number)value;

        if (type == float.class || type == Float.class) return number.floatValue();
        if (type == int.class || type == Integer.class) return number.intValue();
        if (type == double.class || type == Double.class) return number.doubleValue();
        if (type == long.class || type == Long.class) return number.longValue();
        if (type == short.class || type == Short.class) return number.shortValue();
        if (type == byte.class || type == Byte.class) return number.byteValue();

        return value;
    }

    static String getMethodName(String prefix, String propertyName)
    {
        if (propertyName == null || propertyName.length() == 0) return prefix;

        char firstLetter = Character.toUpperCase(propertyName.charAt(0));
        return prefix + firstLetter + propertyName.substring(1);
    }

    private void setupSetter(Class targetClass)
    {
        Class propertyType = mConverter == null ? mValueType : mConverter.getTargetType();
        mSetter = getPropertyFunction(targetClass, "set", propertyType);
    }

    private void setupGetter(Class targetClass)
    {
        mGetter = getPropertyFunction(targetClass, "get", null);
    }

    @SuppressWarnings("unchecked")
    private Method getPropertyFunction(Class targetClass, String prefix, Class valueType)
    {
        String methodName = getMethodName(prefix, mPropertyName);

        if (valueType == null)
        {
            try
            {
                return targetClass.getMethod(methodName);
            }
            catch (NoSuchMethodException failure)
            {
                Log.w(TAG, "Method " + methodName + "() not found on target class " + targetClass);
                return null;
            }
        }

        Class[] variants;

        if (valueType == Float.class || valueType == float.class) variants = FLOAT_VARIANTS;
        else if (valueType == Integer.class || valueType == int.class) variants = INTEGER_VARIANTS;
        else if (valueType == Double.class || valueType == double.class) variants = DOUBLE_VARIANTS;
        else variants = new Class[] {valueType};

        for (Class variant : variants)
        {
            try
            {
                Method method = targetClass.getMethod(methodName, variant);
                if (mConverter == null) mValueType = variant;

                return method;
            }
            catch (NoSuchMethodException failure)
            {
                continue;
            }
        }

        if (!variants[0].isPrimitive())
        {
            for (Method method : targetClass.getMethods())
            {
                if (!method.getName().equals(methodName) || method.getParameterTypes().length != 1) continue;
                if (method.getParameterTypes()[0].isAssignableFrom(valueType)) return method;
            }
        }

        Log.w(TAG, "Method " + methodName + "() with type " + valueType + " not found on target class " + targetClass);
        return null;
    }

    @Override
    public PropertyValuesHolder clone()
    {
        try
        {
            PropertyValuesHolder copy = (PropertyValuesHolder)super.clone();
            copy.mPropertyName = mPropertyName;
            copy.mProperty = mProperty;
            copy.mKeyframes = mKeyframes == null ? null : mKeyframes.clone();
            copy.mEvaluator = mEvaluator;

            return copy;
        }
        catch (CloneNotSupportedException failure)
        {
            return null;
        }
    }

    @Override
    public String toString()
    {
        return mPropertyName + ": " + mKeyframes;
    }

    private static final class PointToFloatArrayConverter extends TypeConverter<PointF, float[]>
    {
        PointToFloatArrayConverter()
        {
            super(PointF.class, float[].class);
        }

        @Override
        public float[] convert(PointF value)
        {
            return new float[] {value.x, value.y};
        }
    }

    private static final class PointToIntArrayConverter extends TypeConverter<PointF, int[]>
    {
        PointToIntArrayConverter()
        {
            super(PointF.class, int[].class);
        }

        @Override
        public int[] convert(PointF value)
        {
            return new int[] {Math.round(value.x), Math.round(value.y)};
        }
    }
}
