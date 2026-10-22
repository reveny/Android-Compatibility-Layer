package android.os;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PersistableBundle extends BaseBundle implements Cloneable, Parcelable
{
    public static final PersistableBundle EMPTY =
        new PersistableBundle(Collections.<String, Object>unmodifiableMap(new HashMap<String, Object>()));

    public static final Parcelable.Creator<PersistableBundle> CREATOR = new Parcelable.Creator<PersistableBundle>()
    {
        @Override
        public PersistableBundle createFromParcel(Parcel source)
        {
            PersistableBundle bundle = new PersistableBundle();
            if (source != null) source.readBundleBody(bundle.mMap, null);
            return bundle;
        }

        @Override
        public PersistableBundle[] newArray(int size)
        {
            return new PersistableBundle[size];
        }
    };

    public PersistableBundle()
    {
        super();
    }

    public PersistableBundle(int capacity)
    {
        super(capacity);
    }

    public PersistableBundle(PersistableBundle source)
    {
        super();
        if (source != null) mMap.putAll(source.mMap);
    }

    private PersistableBundle(Map<String, Object> map)
    {
        super(map);
    }

    public void putPersistableBundle(String key, PersistableBundle value)
    {
        mMap.put(key, value);
    }

    public PersistableBundle getPersistableBundle(String key)
    {
        Object value = mMap.get(key);
        return value instanceof PersistableBundle ? (PersistableBundle)value : null;
    }

    public PersistableBundle deepCopy()
    {
        PersistableBundle copy = new PersistableBundle();

        for (Map.Entry<String, Object> entry : mMap.entrySet())
        {
            Object value = entry.getValue();
            copy.mMap.put(entry.getKey(), value instanceof PersistableBundle ? ((PersistableBundle)value).deepCopy() : value);
        }

        return copy;
    }

    @Override
    public Object clone()
    {
        return new PersistableBundle(this);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeBundleBody(this);
    }

    public void writeToStream(OutputStream outputStream) throws IOException
    {
        ObjectOutputStream output = new ObjectOutputStream(outputStream);
        output.writeObject(toPlainMap());
        output.flush();
    }

    public static PersistableBundle readFromStream(InputStream inputStream) throws IOException
    {
        try
        {
            Object read = new ObjectInputStream(inputStream).readObject();
            return read instanceof HashMap ? fromPlainMap((HashMap<?, ?>)read) : new PersistableBundle();
        }
        catch (ClassNotFoundException missing)
        {
            throw new IOException(missing);
        }
    }

    private HashMap<String, Object> toPlainMap()
    {
        HashMap<String, Object> plain = new HashMap<>();

        for (Map.Entry<String, Object> entry : mMap.entrySet())
        {
            Object value = entry.getValue();
            plain.put(entry.getKey(), value instanceof PersistableBundle ? ((PersistableBundle)value).toPlainMap() : value);
        }

        return plain;
    }

    private static PersistableBundle fromPlainMap(HashMap<?, ?> plain)
    {
        PersistableBundle bundle = new PersistableBundle();

        for (Map.Entry<?, ?> entry : plain.entrySet())
        {
            Object value = entry.getValue();
            bundle.mMap.put(String.valueOf(entry.getKey()), value instanceof HashMap ? fromPlainMap((HashMap<?, ?>)value) : value);
        }

        return bundle;
    }

    @Override
    public synchronized String toString()
    {
        return "PersistableBundle[" + mMap + "]";
    }
}
