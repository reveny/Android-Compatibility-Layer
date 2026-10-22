package android.media;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AudioMetadata
{
    private AudioMetadata()
    {
    }

    public static AudioMetadataMap createMap()
    {
        return new BaseMap();
    }

    public static <T> Key<T> createKey(String name, Class<T> valueClass)
    {
        return new BaseKey<>(name, valueClass);
    }

    public interface Key<T>
    {
        String getName();

        Class<T> getValueClass();
    }

    public static class Format
    {
        public static final Key<Boolean> KEY_ATMOS_PRESENT = createKey("atmos-present", Boolean.class);
        public static final Key<Integer> KEY_AUDIO_ENCODING = createKey("audio-encoding", Integer.class);
        public static final Key<Integer> KEY_BIT_RATE = createKey("bitrate", Integer.class);
        public static final Key<Integer> KEY_BIT_WIDTH = createKey("bit-width", Integer.class);
        public static final Key<Integer> KEY_CHANNEL_MASK = createKey("channel-mask", Integer.class);
        public static final Key<String> KEY_MIME = createKey("mime", String.class);
        public static final Key<Integer> KEY_PRESENTATION_CONTENT_CLASSIFIER =
            createKey("presentation-content-classifier", Integer.class);
        public static final Key<Integer> KEY_PRESENTATION_ID = createKey("presentation-id", Integer.class);
        public static final Key<String> KEY_PRESENTATION_LANGUAGE = createKey("presentation-language", String.class);
        public static final Key<Integer> KEY_PROGRAM_ID = createKey("program-id", Integer.class);
        public static final Key<Integer> KEY_SAMPLE_RATE = createKey("sample-rate", Integer.class);

        private Format()
        {
        }
    }

    private static final class BaseKey<T> implements Key<T>
    {
        private final String mName;
        private final Class<T> mValueClass;

        BaseKey(String name, Class<T> valueClass)
        {
            mName = Objects.requireNonNull(name);
            mValueClass = Objects.requireNonNull(valueClass);
        }

        @Override
        public String getName()
        {
            return mName;
        }

        @Override
        public Class<T> getValueClass()
        {
            return mValueClass;
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) return true;
            if (!(object instanceof BaseKey)) return false;

            BaseKey<?> other = (BaseKey<?>)object;
            return mName.equals(other.mName) && mValueClass.equals(other.mValueClass);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mName, mValueClass);
        }

        @Override
        public String toString()
        {
            return "Key{" + mName + ", " + mValueClass.getName() + "}";
        }
    }

    private static final class BaseMap implements AudioMetadataMap
    {
        private final Map<String, Object> mValues = new HashMap<>();
        private final Map<String, Key<?>> mKeys = new HashMap<>();

        @Override
        public <T> boolean containsKey(Key<T> key)
        {
            Key<?> stored = mKeys.get(key.getName());
            return stored != null && stored.getValueClass() == key.getValueClass();
        }

        @Override
        public AudioMetadataMap dup()
        {
            BaseMap copy = new BaseMap();
            copy.mValues.putAll(mValues);
            copy.mKeys.putAll(mKeys);
            return copy;
        }

        @Override
        public <T> T get(Key<T> key)
        {
            if (!containsKey(key)) return null;

            return key.getValueClass().cast(mValues.get(key.getName()));
        }

        @Override
        public int size()
        {
            return mValues.size();
        }

        @Override
        public <T> T remove(Key<T> key)
        {
            if (!containsKey(key)) return null;

            mKeys.remove(key.getName());
            return key.getValueClass().cast(mValues.remove(key.getName()));
        }

        @Override
        public <T> T set(Key<T> key, T value)
        {
            Objects.requireNonNull(value);

            if (!key.getValueClass().isInstance(value))
            {
                throw new IllegalArgumentException("Value class " + value.getClass() + " does not match " + key);
            }

            T previous = get(key);
            mKeys.put(key.getName(), key);
            mValues.put(key.getName(), value);
            return previous;
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) return true;
            if (!(object instanceof BaseMap)) return false;

            return mValues.equals(((BaseMap)object).mValues);
        }

        @Override
        public int hashCode()
        {
            return mValues.hashCode();
        }

        @Override
        public String toString()
        {
            return mValues.toString();
        }
    }
}
