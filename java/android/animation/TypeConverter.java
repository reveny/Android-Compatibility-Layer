package android.animation;

public abstract class TypeConverter<T, V>
{
    private final Class<T> mFromClass;
    private final Class<V> mToClass;

    public TypeConverter(Class<T> fromClass, Class<V> toClass)
    {
        mFromClass = fromClass;
        mToClass = toClass;
    }

    Class<V> getTargetType()
    {
        return mToClass;
    }

    Class<T> getSourceType()
    {
        return mFromClass;
    }

    public abstract V convert(T value);
}
