package android.util;

import java.lang.reflect.Method;

public abstract class Property<T, V>
{
    private final Class<V> mType;
    private final String mName;

    public Property(Class<V> type, String name)
    {
        mType = type;
        mName = name;
    }

    public static <T, V> Property<T, V> of(Class<T> hostType, Class<V> valueType, String name)
    {
        return new ReflectiveProperty<T, V>(hostType, valueType, name);
    }

    public boolean isReadOnly()
    {
        return false;
    }

    public void set(T object, V value)
    {
        throw new UnsupportedOperationException("this property is read only: " + mName);
    }

    public abstract V get(T object);

    public String getName()
    {
        return mName;
    }

    public Class<V> getType()
    {
        return mType;
    }

    private static final class ReflectiveProperty<T, V> extends Property<T, V>
    {
        private Method mGetter;
        private Method mSetter;

        ReflectiveProperty(Class<T> hostType, Class<V> valueType, String name)
        {
            super(valueType, name);

            String capitalised = Character.toUpperCase(name.charAt(0)) + name.substring(1);

            try
            {
                mGetter = hostType.getMethod("get" + capitalised);
            }
            catch (NoSuchMethodException ignored)
            {
                try
                {
                    mGetter = hostType.getMethod("is" + capitalised);
                }
                catch (NoSuchMethodException alsoIgnored)
                {
                }
            }

            try
            {
                mSetter = hostType.getMethod("set" + capitalised, valueType);
            }
            catch (NoSuchMethodException ignored)
            {
            }
        }

        @Override
        public boolean isReadOnly()
        {
            return mSetter == null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V get(T object)
        {
            if (mGetter == null) return null;

            try
            {
                return (V)mGetter.invoke(object);
            }
            catch (ReflectiveOperationException failure)
            {
                return null;
            }
        }

        @Override
        public void set(T object, V value)
        {
            if (mSetter == null) throw new UnsupportedOperationException("this property is read only: " + getName());

            try
            {
                mSetter.invoke(object, value);
            }
            catch (ReflectiveOperationException failure)
            {
                throw new UnsupportedOperationException("could not set " + getName(), failure);
            }
        }
    }
}
