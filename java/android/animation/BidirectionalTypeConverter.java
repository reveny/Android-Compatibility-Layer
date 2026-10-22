package android.animation;

public abstract class BidirectionalTypeConverter<T, V> extends TypeConverter<T, V>
{
    private BidirectionalTypeConverter<V, T> mInvertedConverter;

    public BidirectionalTypeConverter(Class<T> fromClass, Class<V> toClass)
    {
        super(fromClass, toClass);
    }

    public abstract T convertBack(V value);

    public BidirectionalTypeConverter<V, T> invert()
    {
        if (mInvertedConverter == null) mInvertedConverter = new InvertedConverter<>(this);

        return mInvertedConverter;
    }

    private static final class InvertedConverter<From, To> extends BidirectionalTypeConverter<From, To>
    {
        private final BidirectionalTypeConverter<To, From> mConverter;

        InvertedConverter(BidirectionalTypeConverter<To, From> converter)
        {
            super(converter.getTargetType(), converter.getSourceType());
            mConverter = converter;
        }

        @Override
        public From convertBack(To value)
        {
            return mConverter.convert(value);
        }

        @Override
        public To convert(From value)
        {
            return mConverter.convertBack(value);
        }
    }
}
