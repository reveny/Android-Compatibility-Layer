package android.os;

import android.util.ArrayMap;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class Parcel
{
    public static final Parcelable.Creator<String> STRING_CREATOR = new Parcelable.Creator<String>()
    {
        @Override
        public String createFromParcel(Parcel source)
        {
            return source.readString();
        }

        @Override
        public String[] newArray(int size)
        {
            return new String[size];
        }
    };

    private static final String TAG = "Parcel";

    private static final int EX_SECURITY = -1;
    private static final int EX_BAD_PARCELABLE = -2;
    private static final int EX_ILLEGAL_ARGUMENT = -3;
    private static final int EX_NULL_POINTER = -4;
    private static final int EX_ILLEGAL_STATE = -5;
    private static final int EX_NETWORK_MAIN_THREAD = -6;
    private static final int EX_UNSUPPORTED_OPERATION = -7;
    private static final int EX_PARCELABLE = -9;
    private static final int EX_HAS_NOTED_APPOPS_REPLY_HEADER = -127;
    private static final int EX_HAS_STRICTMODE_REPLY_HEADER = -128;

    private static final int VAL_NULL = -1;
    private static final int VAL_STRING = 0;
    private static final int VAL_INTEGER = 1;
    private static final int VAL_MAP = 2;
    private static final int VAL_BUNDLE = 3;
    private static final int VAL_PARCELABLE = 4;
    private static final int VAL_SHORT = 5;
    private static final int VAL_LONG = 6;
    private static final int VAL_FLOAT = 7;
    private static final int VAL_DOUBLE = 8;
    private static final int VAL_BOOLEAN = 9;
    private static final int VAL_CHARSEQUENCE = 10;
    private static final int VAL_LIST = 11;
    private static final int VAL_SPARSEARRAY = 12;
    private static final int VAL_BYTEARRAY = 13;
    private static final int VAL_STRINGARRAY = 14;
    private static final int VAL_IBINDER = 15;
    private static final int VAL_PARCELABLEARRAY = 16;
    private static final int VAL_OBJECTARRAY = 17;
    private static final int VAL_INTARRAY = 18;
    private static final int VAL_LONGARRAY = 19;
    private static final int VAL_BYTE = 20;
    private static final int VAL_SERIALIZABLE = 21;
    private static final int VAL_SPARSEBOOLEANARRAY = 22;
    private static final int VAL_BOOLEANARRAY = 23;
    private static final int VAL_CHARSEQUENCEARRAY = 24;
    private static final int VAL_PERSISTABLEBUNDLE = 25;
    private static final int VAL_SIZE = 26;
    private static final int VAL_SIZEF = 27;
    private static final int VAL_DOUBLEARRAY = 28;
    private static final int VAL_CHAR = 29;
    private static final int VAL_SHORTARRAY = 30;
    private static final int VAL_CHARARRAY = 31;
    private static final int VAL_FLOATARRAY = 32;
    private static final int VAL_LOCAL_OBJECT = 0x41434c00;

    private static final int LOCAL_OBJECT = 0xac1a0b1e;
    private static final int BUNDLE_MAGIC = 0x4c444e42;
    private static final int INTERFACE_HEADER = 0x53595354;

    private byte[] mData = new byte[64];
    private int mSize;
    private int mPosition;
    private final ArrayList<Object> mObjects = new ArrayList<>();
    private final ArrayList<Integer> mObjectOffsets = new ArrayList<>();
    private final ArrayList<Object> mFlattening = new ArrayList<>();
    private ClassLoader mClassLoader;
    private boolean mSaidObjectsAreLocal;

    private Parcel()
    {
    }

    public static Parcel obtain()
    {
        return new Parcel();
    }

    public static Parcel obtain(IBinder binder)
    {
        return new Parcel();
    }

    public void recycle()
    {
        mData = new byte[64];
        mSize = 0;
        mPosition = 0;
        mObjects.clear();
        mObjectOffsets.clear();
        mFlattening.clear();
        mClassLoader = null;
    }

    public void setPropagateAllowBlocking()
    {
    }

    public int dataPosition()
    {
        return mPosition;
    }

    public void setDataPosition(int position)
    {
        if (position < 0) throw new IllegalArgumentException("the data position cannot be negative: " + position);
        mPosition = position;
    }

    public int dataSize()
    {
        return mSize;
    }

    public int dataAvail()
    {
        int available = mSize - mPosition;
        return available < 0 ? 0 : available;
    }

    public int dataCapacity()
    {
        return mData.length;
    }

    public void setDataSize(int size)
    {
        int target = size < 0 ? 0 : size;
        ensureCapacity(target);

        if (target > mSize) Arrays.fill(mData, mSize, target, (byte)0);
        mSize = target;
        if (mPosition > target) mPosition = target;

        for (int index = mObjectOffsets.size() - 1; index >= 0; index--)
        {
            if (mObjectOffsets.get(index) + 4 > target) mObjectOffsets.remove(index);
        }
    }

    public void setDataCapacity(int size)
    {
        if (size > mData.length) ensureCapacity(size);
    }

    public boolean hasFileDescriptors()
    {
        return hasFileDescriptors(0, mSize);
    }

    public boolean hasFileDescriptors(int offset, int length)
    {
        for (int offsetIndex = 0; offsetIndex < mObjectOffsets.size(); offsetIndex++)
        {
            int at = mObjectOffsets.get(offsetIndex);
            if (at < offset || at >= offset + length) continue;

            Object value = objectAt(at);
            if (value instanceof FileDescriptor || value instanceof ParcelFileDescriptor) return true;
        }

        return false;
    }

    public void appendFrom(Parcel parcel, int offset, int length)
    {
        if (parcel == null) return;
        if (offset < 0 || length < 0 || offset + length > parcel.mSize)
        {
            throw new IllegalArgumentException("appendFrom range " + offset + "+" + length + " exceeds " + parcel.mSize);
        }

        int destination = mPosition;
        ensureCapacity(destination + length);
        System.arraycopy(parcel.mData, offset, mData, destination, length);
        mPosition = destination + length;
        if (mPosition > mSize) mSize = mPosition;

        for (int offsetIndex = 0; offsetIndex < parcel.mObjectOffsets.size(); offsetIndex++)
        {
            int at = parcel.mObjectOffsets.get(offsetIndex);
            if (at < offset || at + 4 > offset + length) continue;

            int handle = parcel.peekIntAt(at);
            if (handle < 0 || handle >= parcel.mObjects.size()) continue;

            int moved = destination + (at - offset);
            int copy = mObjects.size();
            mObjects.add(parcel.mObjects.get(handle));
            putIntAt(moved, copy);
            mObjectOffsets.add(moved);
        }
    }

    public void writeInterfaceToken(String interfaceName)
    {
        writeInt(0);
        writeInt(-1);
        writeInt(INTERFACE_HEADER);
        writeString(interfaceName);
    }

    public void enforceInterface(String interfaceName)
    {
        readInt();
        readInt();
        int header = readInt();
        String actual = readString();

        if (header != INTERFACE_HEADER || (interfaceName == null ? actual != null : !interfaceName.equals(actual)))
        {
            throw new SecurityException("Binder invocation to an incorrect interface");
        }
    }

    public void enforceNoDataAvail()
    {
        int available = dataAvail();
        if (available > 0) throw new BadParcelableException("Parcel data not fully consumed, unread size: " + available);
    }

    public void writeInt(int value)
    {
        int at = reserve(4);
        putIntAt(at, value);
    }

    public int readInt()
    {
        if (mPosition < 0 || mPosition + 4 > mSize) return 0;

        int value = peekIntAt(mPosition);
        mPosition += 4;
        return value;
    }

    public void writeLong(long value)
    {
        int at = reserve(8);
        putIntAt(at, (int)value);
        putIntAt(at + 4, (int)(value >>> 32));
    }

    public long readLong()
    {
        if (mPosition < 0 || mPosition + 8 > mSize) return 0L;

        long low = peekIntAt(mPosition) & 0xffffffffL;
        long high = peekIntAt(mPosition + 4) & 0xffffffffL;
        mPosition += 8;
        return (high << 32) | low;
    }

    public void writeFloat(float value)
    {
        writeInt(Float.floatToRawIntBits(value));
    }

    public float readFloat()
    {
        return Float.intBitsToFloat(readInt());
    }

    public void writeDouble(double value)
    {
        writeLong(Double.doubleToRawLongBits(value));
    }

    public double readDouble()
    {
        return Double.longBitsToDouble(readLong());
    }

    public void writeBoolean(boolean value)
    {
        writeInt(value ? 1 : 0);
    }

    public boolean readBoolean()
    {
        return readInt() != 0;
    }

    public void writeByte(byte value)
    {
        writeInt(value);
    }

    public byte readByte()
    {
        return (byte)(readInt() & 0xff);
    }

    public void writeString(String value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        int length = value.length();
        writeInt(length);

        int at = reserve(padded((length + 1) * 2));
        for (int index = 0; index < length; index++)
        {
            char character = value.charAt(index);
            mData[at + index * 2] = (byte)character;
            mData[at + index * 2 + 1] = (byte)(character >>> 8);
        }
    }

    public String readString()
    {
        int start = mPosition;
        int length = readInt();
        if (length < 0) return null;

        int bytes = padded((length + 1) * 2);
        if (length > mSize || mPosition + bytes > mSize)
        {
            mPosition = start;
            return null;
        }

        char[] characters = new char[length];
        for (int index = 0; index < length; index++)
        {
            int low = mData[mPosition + index * 2] & 0xff;
            int high = mData[mPosition + index * 2 + 1] & 0xff;
            characters[index] = (char)(low | (high << 8));
        }

        mPosition += bytes;
        return new String(characters);
    }

    public void writeStrongBinder(IBinder value)
    {
        writeLocal(value);
    }

    public void writeStrongInterface(IInterface value)
    {
        writeStrongBinder(value == null ? null : value.asBinder());
    }

    public IBinder readStrongBinder()
    {
        Object value = readLocal();
        return value instanceof IBinder ? (IBinder)value : null;
    }

    public void writeFileDescriptor(FileDescriptor value)
    {
        writeLocal(value);
    }

    public ParcelFileDescriptor readFileDescriptor()
    {
        Object value = readLocal();
        if (value instanceof ParcelFileDescriptor) return (ParcelFileDescriptor)value;
        if (!(value instanceof FileDescriptor)) return null;

        try
        {
            return ParcelFileDescriptor.dup((FileDescriptor)value);
        }
        catch (IOException failure)
        {
            android.util.Log.w(TAG, "could not duplicate a file descriptor read from a parcel", failure);
            return null;
        }
    }

    public void writeByteArray(byte[] value)
    {
        writeByteArray(value, 0, value == null ? 0 : value.length);
    }

    public void writeByteArray(byte[] value, int offset, int length)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        if (offset < 0 || length < 0 || offset + length > value.length) throw new ArrayIndexOutOfBoundsException();

        writeInt(length);
        int at = reserve(padded(length));
        System.arraycopy(value, offset, mData, at, length);
    }

    public void writeBlob(byte[] value)
    {
        writeByteArray(value);
    }

    public void writeBlob(byte[] value, int offset, int length)
    {
        writeByteArray(value, offset, length);
    }

    public byte[] createByteArray()
    {
        int start = mPosition;
        int length = readInt();
        if (length < 0) return null;

        if (mPosition + padded(length) > mSize)
        {
            mPosition = start;
            return null;
        }

        byte[] value = Arrays.copyOfRange(mData, mPosition, mPosition + length);
        mPosition += padded(length);
        return value;
    }

    public byte[] readBlob()
    {
        return createByteArray();
    }

    public void readByteArray(byte[] destination)
    {
        copyInto(createByteArray(), destination);
    }

    public void writeBooleanArray(boolean[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (boolean item : value) writeInt(item ? 1 : 0);
    }

    public boolean[] createBooleanArray()
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        boolean[] value = new boolean[length];
        for (int index = 0; index < length; index++) value[index] = readInt() != 0;
        return value;
    }

    public void readBooleanArray(boolean[] destination)
    {
        copyInto(createBooleanArray(), destination);
    }

    public void writeCharArray(char[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (char item : value) writeInt(item);
    }

    public char[] createCharArray()
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        char[] value = new char[length];
        for (int index = 0; index < length; index++) value[index] = (char)readInt();
        return value;
    }

    public void readCharArray(char[] destination)
    {
        copyInto(createCharArray(), destination);
    }

    public void writeIntArray(int[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (int item : value) writeInt(item);
    }

    public int[] createIntArray()
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        int[] value = new int[length];
        for (int index = 0; index < length; index++) value[index] = readInt();
        return value;
    }

    public void readIntArray(int[] destination)
    {
        copyInto(createIntArray(), destination);
    }

    public void writeLongArray(long[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (long item : value) writeLong(item);
    }

    public long[] createLongArray()
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        long[] value = new long[length];
        for (int index = 0; index < length; index++) value[index] = readLong();
        return value;
    }

    public void readLongArray(long[] destination)
    {
        copyInto(createLongArray(), destination);
    }

    public void writeFloatArray(float[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (float item : value) writeFloat(item);
    }

    public float[] createFloatArray()
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        float[] value = new float[length];
        for (int index = 0; index < length; index++) value[index] = readFloat();
        return value;
    }

    public void readFloatArray(float[] destination)
    {
        copyInto(createFloatArray(), destination);
    }

    public void writeDoubleArray(double[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (double item : value) writeDouble(item);
    }

    public double[] createDoubleArray()
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        double[] value = new double[length];
        for (int index = 0; index < length; index++) value[index] = readDouble();
        return value;
    }

    public void readDoubleArray(double[] destination)
    {
        copyInto(createDoubleArray(), destination);
    }

    public void writeStringArray(String[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (String item : value) writeString(item);
    }

    public String[] createStringArray()
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        String[] value = new String[length];
        for (int index = 0; index < length; index++) value[index] = readString();
        return value;
    }

    public void readStringArray(String[] destination)
    {
        copyInto(createStringArray(), destination);
    }

    public void writeBinderArray(IBinder[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (IBinder item : value) writeStrongBinder(item);
    }

    public IBinder[] createBinderArray()
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        IBinder[] value = new IBinder[length];
        for (int index = 0; index < length; index++) value[index] = readStrongBinder();
        return value;
    }

    public void readBinderArray(IBinder[] destination)
    {
        copyInto(createBinderArray(), destination);
    }

    public <T extends IInterface> void writeInterfaceArray(T[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (T item : value) writeStrongInterface(item);
    }

    public <T extends IInterface> T[] createInterfaceArray(IntFunction<T[]> newArray, Function<IBinder, T> asInterface)
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        T[] value = newArray.apply(length);
        for (int index = 0; index < length; index++) value[index] = asInterface.apply(readStrongBinder());
        return value;
    }

    public <T extends IInterface> void readInterfaceArray(T[] destination, Function<IBinder, T> asInterface)
    {
        int length = readInt();
        if (destination == null || length != destination.length) throw new BadParcelableException("bad array lengths");

        for (int index = 0; index < length; index++) destination[index] = asInterface.apply(readStrongBinder());
    }

    public <T extends IInterface> void writeInterfaceList(List<T> value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (T item : value) writeStrongInterface(item);
    }

    public <T extends IInterface> ArrayList<T> createInterfaceArrayList(Function<IBinder, T> asInterface)
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        ArrayList<T> value = new ArrayList<>(length);
        for (int index = 0; index < length; index++) value.add(asInterface.apply(readStrongBinder()));
        return value;
    }

    public <T extends IInterface> void readInterfaceList(List<T> destination, Function<IBinder, T> asInterface)
    {
        replaceContents(destination, createInterfaceArrayList(asInterface));
    }

    public void writeBinderList(List<IBinder> value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (IBinder item : value) writeStrongBinder(item);
    }

    public ArrayList<IBinder> createBinderArrayList()
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        ArrayList<IBinder> value = new ArrayList<>(length);
        for (int index = 0; index < length; index++) value.add(readStrongBinder());
        return value;
    }

    public void readBinderList(List<IBinder> destination)
    {
        replaceContents(destination, createBinderArrayList());
    }

    public void writeStringList(List<String> value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (String item : value) writeString(item);
    }

    public ArrayList<String> createStringArrayList()
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        ArrayList<String> value = new ArrayList<>(length);
        for (int index = 0; index < length; index++) value.add(readString());
        return value;
    }

    public void readStringList(List<String> destination)
    {
        replaceContents(destination, createStringArrayList());
    }

    public <T extends Parcelable> void writeTypedList(List<T> value)
    {
        writeTypedList(value, 0);
    }

    public <T extends Parcelable> void writeTypedList(List<T> value, int flags)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (T item : value) writeTypedObject(item, flags);
    }

    public <T extends Parcelable> void writeParcelableList(List<T> value, int flags)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (T item : value) writeParcelable(item, flags);
    }

    public <T> ArrayList<T> createTypedArrayList(Parcelable.Creator<T> creator)
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        ArrayList<T> value = new ArrayList<>(length);
        for (int index = 0; index < length; index++) value.add(readTypedObject(creator));
        return value;
    }

    public <T> void readTypedList(List<T> destination, Parcelable.Creator<T> creator)
    {
        replaceContents(destination, createTypedArrayList(creator));
    }

    public <T extends Parcelable> List<T> readParcelableList(List<T> destination, ClassLoader loader)
    {
        List<T> target = destination == null ? new ArrayList<T>() : destination;
        target.clear();

        int length = readArrayLength(4);
        for (int index = 0; index < length; index++) target.add(this.<T>readParcelable(loader));
        return target;
    }

    public <T> List<T> readParcelableList(List<T> destination, ClassLoader loader, Class<? extends T> type)
    {
        List<T> target = destination == null ? new ArrayList<T>() : destination;
        target.clear();

        int length = readArrayLength(4);
        for (int index = 0; index < length; index++) target.add(this.<T>cast(readParcelableValue(loader)));
        return target;
    }

    public <T extends Parcelable> void writeTypedArray(T[] value, int flags)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (T item : value) writeTypedObject(item, flags);
    }

    public <T> T[] createTypedArray(Parcelable.Creator<T> creator)
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        T[] value = creator.newArray(length);
        for (int index = 0; index < length; index++) value[index] = readTypedObject(creator);
        return value;
    }

    public <T> void readTypedArray(T[] destination, Parcelable.Creator<T> creator)
    {
        int length = readInt();
        if (destination == null || length != destination.length) throw new RuntimeException("bad array lengths");

        for (int index = 0; index < length; index++) destination[index] = readTypedObject(creator);
    }

    public <T extends Parcelable> void writeTypedObject(T value, int flags)
    {
        if (value == null)
        {
            writeInt(0);
            return;
        }

        if (isFlattening(value))
        {
            writeLocal(value);
            return;
        }

        writeInt(1);
        flatten(value, flags);
    }

    public <T> T readTypedObject(Parcelable.Creator<T> creator)
    {
        int marker = peekInt();
        if (marker == LOCAL_OBJECT) return cast(readLocal());

        if (readInt() == 0) return null;
        return creator == null ? null : this.<T>cast(create(creator, mClassLoader));
    }

    public <T extends Parcelable> void writeTypedArrayMap(ArrayMap<String, T> value, int flags)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (int index = 0; index < value.size(); index++)
        {
            writeString(value.keyAt(index));
            writeTypedObject(value.valueAt(index), flags);
        }
    }

    public <T extends Parcelable> ArrayMap<String, T> createTypedArrayMap(Parcelable.Creator<T> creator)
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        ArrayMap<String, T> value = new ArrayMap<>(length);
        for (int index = 0; index < length; index++)
        {
            String key = readString();
            value.put(key, readTypedObject(creator));
        }
        return value;
    }

    public <T extends Parcelable> void writeTypedSparseArray(SparseArray<T> value, int flags)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (int index = 0; index < value.size(); index++)
        {
            writeInt(value.keyAt(index));
            writeTypedObject(value.valueAt(index), flags);
        }
    }

    public <T extends Parcelable> SparseArray<T> createTypedSparseArray(Parcelable.Creator<T> creator)
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        SparseArray<T> value = new SparseArray<>(length);
        for (int index = 0; index < length; index++)
        {
            int key = readInt();
            value.put(key, readTypedObject(creator));
        }
        return value;
    }

    public <T> void writeFixedArray(T value, int flags, int... dimensions)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeFixedArray(value, flags, 0, dimensions);
    }

    public <T> T createFixedArray(Class<T> type, int... dimensions)
    {
        return createFixedArray(type, null, null, dimensions);
    }

    public <T, S extends Parcelable> T createFixedArray(Class<T> type, Parcelable.Creator<S> creator, int... dimensions)
    {
        return createFixedArray(type, creator, null, dimensions);
    }

    public <T, S extends IInterface> T createFixedArray(Class<T> type, Function<IBinder, S> asInterface, int... dimensions)
    {
        return createFixedArray(type, null, asInterface, dimensions);
    }

    public <T> void readFixedArray(T destination)
    {
        readFixedArray(destination, null, null);
    }

    public <T, S extends Parcelable> void readFixedArray(T destination, Parcelable.Creator<S> creator)
    {
        readFixedArray(destination, creator, null);
    }

    public <T, S extends IInterface> void readFixedArray(T destination, Function<IBinder, S> asInterface)
    {
        readFixedArray(destination, null, asInterface);
    }

    public void writeBundle(Bundle value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeBundleBody(value);
    }

    public Bundle readBundle()
    {
        return readBundle(null);
    }

    public Bundle readBundle(ClassLoader loader)
    {
        if (peekInt() < 0)
        {
            readInt();
            return null;
        }

        Bundle bundle = new Bundle();
        if (loader != null) bundle.setClassLoader(loader);
        readBundleBody(bundle.mMap, loader);
        return bundle;
    }

    public void writePersistableBundle(PersistableBundle value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeBundleBody(value);
    }

    public PersistableBundle readPersistableBundle()
    {
        return readPersistableBundle(null);
    }

    public PersistableBundle readPersistableBundle(ClassLoader loader)
    {
        if (peekInt() < 0)
        {
            readInt();
            return null;
        }

        PersistableBundle bundle = new PersistableBundle();
        readBundleBody(bundle.mMap, loader);
        return bundle;
    }

    public void writeSize(android.util.Size value)
    {
        writeInt(value.getWidth());
        writeInt(value.getHeight());
    }

    public android.util.Size readSize()
    {
        int width = readInt();
        return new android.util.Size(width, readInt());
    }

    public void writeSizeF(android.util.SizeF value)
    {
        writeFloat(value.getWidth());
        writeFloat(value.getHeight());
    }

    public android.util.SizeF readSizeF()
    {
        float width = readFloat();
        return new android.util.SizeF(width, readFloat());
    }

    public void writeMap(Map value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (Object entry : value.entrySet())
        {
            writeValue(((Map.Entry<?, ?>)entry).getKey());
            writeValue(((Map.Entry<?, ?>)entry).getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public void readMap(Map outVal, ClassLoader loader)
    {
        int length = readArrayLength(8);
        for (int index = 0; index < length; index++)
        {
            Object key = readValue(loader);
            Object value = readValue(loader);
            if (outVal != null) outVal.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <K, V> void readMap(Map<? super K, ? super V> outVal, ClassLoader loader, Class<K> keyType, Class<V> valueType)
    {
        readMap((Map)outVal, loader);
    }

    @SuppressWarnings("unchecked")
    public HashMap readHashMap(ClassLoader loader)
    {
        if (peekInt() < 0)
        {
            readInt();
            return null;
        }

        HashMap<Object, Object> value = new HashMap<>();
        readMap(value, loader);
        return value;
    }

    @SuppressWarnings("unchecked")
    public <K, V> HashMap<K, V> readHashMap(ClassLoader loader, Class<? extends K> keyType, Class<? extends V> valueType)
    {
        return (HashMap<K, V>)readHashMap(loader);
    }

    public void writeList(List value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (Object item : value) writeValue(item);
    }

    @SuppressWarnings("unchecked")
    public void readList(List outVal, ClassLoader loader)
    {
        int length = readArrayLength(4);
        for (int index = 0; index < length; index++)
        {
            Object item = readValue(loader);
            if (outVal != null) outVal.add(item);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void readList(List<? super T> outVal, ClassLoader loader, Class<T> type)
    {
        readList((List)outVal, loader);
    }

    public ArrayList readArrayList(ClassLoader loader)
    {
        if (peekInt() < 0)
        {
            readInt();
            return null;
        }

        ArrayList<Object> value = new ArrayList<>();
        readList(value, loader);
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> readArrayList(ClassLoader loader, Class<? extends T> type)
    {
        return (ArrayList<T>)readArrayList(loader);
    }

    public void writeArray(Object[] value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (Object item : value) writeValue(item);
    }

    public Object[] readArray(ClassLoader loader)
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        Object[] value = new Object[length];
        for (int index = 0; index < length; index++) value[index] = readValue(loader);
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] readArray(ClassLoader loader, Class<T> type)
    {
        Object[] source = readArray(loader);
        if (source == null) return null;

        T[] value = (T[])Array.newInstance(type, source.length);
        for (int index = 0; index < source.length; index++) value[index] = (T)source[index];
        return value;
    }

    public <T> void writeSparseArray(SparseArray<T> value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (int index = 0; index < value.size(); index++)
        {
            writeInt(value.keyAt(index));
            writeValue(value.valueAt(index));
        }
    }

    public <T> SparseArray<T> readSparseArray(ClassLoader loader)
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        SparseArray<T> value = new SparseArray<>(length);
        for (int index = 0; index < length; index++)
        {
            int key = readInt();
            value.put(key, this.<T>cast(readValue(loader)));
        }
        return value;
    }

    public <T> SparseArray<T> readSparseArray(ClassLoader loader, Class<? extends T> type)
    {
        return readSparseArray(loader);
    }

    public void writeSparseBooleanArray(SparseBooleanArray value)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.size());
        for (int index = 0; index < value.size(); index++)
        {
            writeInt(value.keyAt(index));
            writeInt(value.valueAt(index) ? 1 : 0);
        }
    }

    public SparseBooleanArray readSparseBooleanArray()
    {
        int length = readArrayLength(8);
        if (length < 0) return null;

        SparseBooleanArray value = new SparseBooleanArray(length);
        for (int index = 0; index < length; index++)
        {
            int key = readInt();
            value.put(key, readInt() != 0);
        }
        return value;
    }

    public void writeParcelable(Parcelable value, int flags)
    {
        if (value == null)
        {
            writeString(null);
            return;
        }

        if (isFlattening(value))
        {
            writeLocal(value);
            return;
        }

        writeString(value.getClass().getName());
        flatten(value, flags);
    }

    public void writeParcelableCreator(Parcelable value)
    {
        writeString(value.getClass().getName());
    }

    public <T extends Parcelable> T readParcelable(ClassLoader loader)
    {
        Object value = readParcelableValue(loader);
        return value instanceof Parcelable ? this.<T>cast(value) : null;
    }

    public <T> T readParcelable(ClassLoader loader, Class<T> type)
    {
        Object value = readParcelableValue(loader);
        if (value == null) return null;
        if (type != null && !type.isInstance(value))
        {
            throw new BadParcelableException("Parcelable creator for " + value.getClass().getName() + " is not a subclass of " + type);
        }

        return cast(value);
    }

    public Parcelable.Creator<?> readParcelableCreator(ClassLoader loader)
    {
        if (peekInt() == LOCAL_OBJECT)
        {
            final Object instance = readLocal();
            if (instance == null) return null;

            return new Parcelable.Creator<Object>()
            {
                @Override
                public Object createFromParcel(Parcel source)
                {
                    return instance;
                }

                @Override
                public Object[] newArray(int size)
                {
                    return (Object[])Array.newInstance(instance.getClass(), size);
                }
            };
        }

        String className = readString();
        if (className == null) return null;

        return creatorFor(className, loader == null ? mClassLoader : loader);
    }

    @SuppressWarnings("unchecked")
    public <T> Parcelable.Creator<T> readParcelableCreator(ClassLoader loader, Class<T> type)
    {
        return (Parcelable.Creator<T>)readParcelableCreator(loader);
    }

    public <T extends Parcelable> void writeParcelableArray(T[] value, int flags)
    {
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        writeInt(value.length);
        for (T item : value) writeParcelable(item, flags);
    }

    public Parcelable[] readParcelableArray(ClassLoader loader)
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        Parcelable[] value = new Parcelable[length];
        for (int index = 0; index < length; index++) value[index] = readParcelable(loader);
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] readParcelableArray(ClassLoader loader, Class<T> type)
    {
        int length = readArrayLength(4);
        if (length < 0) return null;

        T[] value = (T[])Array.newInstance(type, length);
        for (int index = 0; index < length; index++) value[index] = readParcelable(loader, type);
        return value;
    }

    public void writeSerializable(Serializable value)
    {
        if (value == null)
        {
            writeString(null);
            return;
        }

        byte[] payload;
        try
        {
            ByteArrayOutputStream serialized = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(serialized);
            stream.writeObject(value);
            stream.flush();
            payload = serialized.toByteArray();
        }
        catch (IOException failure)
        {
            android.util.Log.w(TAG, "a " + value.getClass().getName() + " could not be serialized and is kept by reference", failure);
            writeLocal(value);
            return;
        }

        writeString(value.getClass().getName());
        writeByteArray(payload);
    }

    public Serializable readSerializable()
    {
        Object value = readSerializableValue(mClassLoader);
        return value instanceof Serializable ? (Serializable)value : null;
    }

    public <T> T readSerializable(ClassLoader loader, Class<T> type)
    {
        Object value = readSerializableValue(loader);
        return type != null && type.isInstance(value) ? type.cast(value) : null;
    }

    public void writeValue(Object value)
    {
        if (value == null)
        {
            writeInt(VAL_NULL);
        }
        else if (value instanceof String)
        {
            writeInt(VAL_STRING);
            writeString((String)value);
        }
        else if (value instanceof Integer)
        {
            writeInt(VAL_INTEGER);
            writeInt((Integer)value);
        }
        else if (value instanceof Map)
        {
            writeInt(VAL_MAP);
            writeMap((Map)value);
        }
        else if (value instanceof Bundle)
        {
            writeInt(VAL_BUNDLE);
            writeBundle((Bundle)value);
        }
        else if (value instanceof PersistableBundle)
        {
            writeInt(VAL_PERSISTABLEBUNDLE);
            writePersistableBundle((PersistableBundle)value);
        }
        else if (value instanceof ParcelFileDescriptor || value instanceof FileDescriptor || isFlattening(value))
        {
            writeInt(VAL_LOCAL_OBJECT);
            writeLocal(value);
        }
        else if (value instanceof Parcelable)
        {
            writeInt(VAL_PARCELABLE);
            writeParcelable((Parcelable)value, 0);
        }
        else if (value instanceof Short)
        {
            writeInt(VAL_SHORT);
            writeInt((Short)value);
        }
        else if (value instanceof Long)
        {
            writeInt(VAL_LONG);
            writeLong((Long)value);
        }
        else if (value instanceof Float)
        {
            writeInt(VAL_FLOAT);
            writeFloat((Float)value);
        }
        else if (value instanceof Double)
        {
            writeInt(VAL_DOUBLE);
            writeDouble((Double)value);
        }
        else if (value instanceof Boolean)
        {
            writeInt(VAL_BOOLEAN);
            writeInt((Boolean)value ? 1 : 0);
        }
        else if (value instanceof CharSequence)
        {
            writeInt(VAL_CHARSEQUENCE);
            writeCharSequence((CharSequence)value);
        }
        else if (value instanceof List)
        {
            writeInt(VAL_LIST);
            writeList((List)value);
        }
        else if (value instanceof SparseArray)
        {
            writeInt(VAL_SPARSEARRAY);
            writeSparseArray((SparseArray<?>)value);
        }
        else if (value instanceof boolean[])
        {
            writeInt(VAL_BOOLEANARRAY);
            writeBooleanArray((boolean[])value);
        }
        else if (value instanceof byte[])
        {
            writeInt(VAL_BYTEARRAY);
            writeByteArray((byte[])value);
        }
        else if (value instanceof String[])
        {
            writeInt(VAL_STRINGARRAY);
            writeStringArray((String[])value);
        }
        else if (value instanceof CharSequence[])
        {
            CharSequence[] array = (CharSequence[])value;
            writeInt(VAL_CHARSEQUENCEARRAY);
            writeInt(array.length);
            for (CharSequence item : array) writeCharSequence(item);
        }
        else if (value instanceof IBinder)
        {
            writeInt(VAL_IBINDER);
            writeStrongBinder((IBinder)value);
        }
        else if (value instanceof Parcelable[])
        {
            writeInt(VAL_PARCELABLEARRAY);
            writeParcelableArray((Parcelable[])value, 0);
        }
        else if (value instanceof int[])
        {
            writeInt(VAL_INTARRAY);
            writeIntArray((int[])value);
        }
        else if (value instanceof long[])
        {
            writeInt(VAL_LONGARRAY);
            writeLongArray((long[])value);
        }
        else if (value instanceof Byte)
        {
            writeInt(VAL_BYTE);
            writeInt((Byte)value);
        }
        else if (value instanceof android.util.Size)
        {
            writeInt(VAL_SIZE);
            writeSize((android.util.Size)value);
        }
        else if (value instanceof android.util.SizeF)
        {
            writeInt(VAL_SIZEF);
            writeSizeF((android.util.SizeF)value);
        }
        else if (value instanceof double[])
        {
            writeInt(VAL_DOUBLEARRAY);
            writeDoubleArray((double[])value);
        }
        else if (value instanceof Character)
        {
            writeInt(VAL_CHAR);
            writeInt((Character)value);
        }
        else if (value instanceof short[])
        {
            short[] array = (short[])value;
            writeInt(VAL_SHORTARRAY);
            writeInt(array.length);
            for (short item : array) writeInt(item);
        }
        else if (value instanceof char[])
        {
            writeInt(VAL_CHARARRAY);
            writeCharArray((char[])value);
        }
        else if (value instanceof float[])
        {
            writeInt(VAL_FLOATARRAY);
            writeFloatArray((float[])value);
        }
        else if (value.getClass() == Object[].class)
        {
            writeInt(VAL_OBJECTARRAY);
            writeArray((Object[])value);
        }
        else if (value instanceof SparseBooleanArray)
        {
            writeInt(VAL_SPARSEBOOLEANARRAY);
            writeSparseBooleanArray((SparseBooleanArray)value);
        }
        else if (value instanceof Serializable)
        {
            writeInt(VAL_SERIALIZABLE);
            writeSerializable((Serializable)value);
        }
        else
        {
            writeInt(VAL_LOCAL_OBJECT);
            writeLocal(value);
        }
    }

    public Object readValue(ClassLoader loader)
    {
        ClassLoader effective = loader == null ? mClassLoader : loader;
        int type = readInt();

        switch (type)
        {
            case VAL_NULL:
                return null;
            case VAL_STRING:
                return readString();
            case VAL_INTEGER:
                return readInt();
            case VAL_MAP:
                return readHashMap(effective);
            case VAL_BUNDLE:
                return readBundle(effective);
            case VAL_PERSISTABLEBUNDLE:
                return readPersistableBundle(effective);
            case VAL_PARCELABLE:
                return readParcelableValue(effective);
            case VAL_SHORT:
                return (short)readInt();
            case VAL_LONG:
                return readLong();
            case VAL_FLOAT:
                return readFloat();
            case VAL_DOUBLE:
                return readDouble();
            case VAL_BOOLEAN:
                return readInt() != 0;
            case VAL_CHARSEQUENCE:
                return readCharSequence();
            case VAL_LIST:
                return readArrayList(effective);
            case VAL_SPARSEARRAY:
                return readSparseArray(effective);
            case VAL_BOOLEANARRAY:
                return createBooleanArray();
            case VAL_BYTEARRAY:
                return createByteArray();
            case VAL_STRINGARRAY:
                return createStringArray();
            case VAL_CHARSEQUENCEARRAY:
            {
                int length = readArrayLength(4);
                if (length < 0) return null;

                CharSequence[] array = new CharSequence[length];
                for (int index = 0; index < length; index++) array[index] = readCharSequence();
                return array;
            }
            case VAL_IBINDER:
                return readStrongBinder();
            case VAL_PARCELABLEARRAY:
                return readParcelableArray(effective);
            case VAL_INTARRAY:
                return createIntArray();
            case VAL_LONGARRAY:
                return createLongArray();
            case VAL_BYTE:
                return (byte)readInt();
            case VAL_SIZE:
                return readSize();
            case VAL_SIZEF:
                return readSizeF();
            case VAL_DOUBLEARRAY:
                return createDoubleArray();
            case VAL_CHAR:
                return (char)readInt();
            case VAL_SHORTARRAY:
            {
                int length = readArrayLength(4);
                if (length < 0) return null;

                short[] array = new short[length];
                for (int index = 0; index < length; index++) array[index] = (short)readInt();
                return array;
            }
            case VAL_CHARARRAY:
                return createCharArray();
            case VAL_FLOATARRAY:
                return createFloatArray();
            case VAL_OBJECTARRAY:
                return readArray(effective);
            case VAL_SPARSEBOOLEANARRAY:
                return readSparseBooleanArray();
            case VAL_SERIALIZABLE:
                return readSerializableValue(effective);
            case VAL_LOCAL_OBJECT:
                return readLocal();
            default:
                throw new BadParcelableException("Parcel " + this + ": Unmarshalling unknown type code " + type + " at offset "
                                                 + (mPosition - 4));
        }
    }

    public void writeException(Exception exception)
    {
        int code = 0;
        if (exception instanceof Parcelable && exception.getClass().getClassLoader() != Parcel.class.getClassLoader())
        {
            code = EX_PARCELABLE;
        }
        else if (exception instanceof SecurityException) code = EX_SECURITY;
        else if (exception instanceof BadParcelableException) code = EX_BAD_PARCELABLE;
        else if (exception instanceof IllegalArgumentException) code = EX_ILLEGAL_ARGUMENT;
        else if (exception instanceof NullPointerException) code = EX_NULL_POINTER;
        else if (exception instanceof IllegalStateException) code = EX_ILLEGAL_STATE;
        else if (exception instanceof NetworkOnMainThreadException) code = EX_NETWORK_MAIN_THREAD;
        else if (exception instanceof UnsupportedOperationException) code = EX_UNSUPPORTED_OPERATION;

        if (code == 0)
        {
            if (exception instanceof RuntimeException) throw (RuntimeException)exception;
            throw new RuntimeException(exception);
        }

        writeInt(code);
        writeString(exception.getMessage());
        writeInt(0);

        if (code == EX_PARCELABLE)
        {
            int sizeAt = dataPosition();
            writeInt(0);
            writeParcelable((Parcelable)exception, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
            int end = dataPosition();
            setDataPosition(sizeAt);
            writeInt(end - sizeAt - 4);
            setDataPosition(end);
        }
    }

    public void writeNoException()
    {
        writeInt(0);
    }

    public void readException()
    {
        int code = readExceptionCode();
        if (code == 0) return;

        String message = readString();
        int header = readInt();
        if (header > 0) setDataPosition(dataPosition() + header);

        readException(code, message);
    }

    public void readException(int code, String message)
    {
        switch (code)
        {
            case EX_PARCELABLE:
            {
                if (readInt() > 0)
                {
                    Object thrown = readParcelable(Parcelable.class.getClassLoader());
                    if (thrown instanceof RuntimeException) throw (RuntimeException)thrown;
                    if (thrown instanceof Throwable) throw new RuntimeException((Throwable)thrown);
                }

                throw new RuntimeException(message);
            }
            case EX_SECURITY:
                throw new SecurityException(message);
            case EX_BAD_PARCELABLE:
                throw new BadParcelableException(message);
            case EX_ILLEGAL_ARGUMENT:
                throw new IllegalArgumentException(message);
            case EX_NULL_POINTER:
                throw new NullPointerException(message);
            case EX_ILLEGAL_STATE:
                throw new IllegalStateException(message);
            case EX_NETWORK_MAIN_THREAD:
                throw new NetworkOnMainThreadException();
            case EX_UNSUPPORTED_OPERATION:
                throw new UnsupportedOperationException(message);
            default:
                throw new RuntimeException("Unknown exception code: " + code + " msg " + message);
        }
    }

    public byte[] marshall()
    {
        if (!mObjects.isEmpty() && !mSaidObjectsAreLocal)
        {
            mSaidObjectsAreLocal = true;
            android.util.Log.w(TAG, "marshalling a parcel that holds " + mObjects.size()
                                        + " binder, descriptor or unflattened object(s); they read back as null elsewhere");
        }

        return Arrays.copyOf(mData, mSize);
    }

    public void unmarshall(byte[] data, int offset, int length)
    {
        mObjects.clear();
        mObjectOffsets.clear();

        int count = data == null || length < 0 ? 0 : length;
        mData = new byte[Math.max(64, padded(count))];
        if (count > 0) System.arraycopy(data, offset, mData, 0, count);
        mSize = count;
        mPosition = count;
    }

    @Override
    public String toString()
    {
        return "android.os.Parcel@" + Integer.toHexString(hashCode()) + "{size=" + mSize + ", position=" + mPosition + ", objects="
               + mObjects.size() + "}";
    }

    void writeBundleBody(BaseBundle bundle)
    {
        Map<String, Object> map = bundle.mMap;
        if (map.isEmpty())
        {
            writeInt(0);
            return;
        }

        int lengthAt = dataPosition();
        writeInt(0);
        writeInt(BUNDLE_MAGIC);

        int start = dataPosition();
        writeInt(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            writeString(entry.getKey());
            writeValue(entry.getValue());
        }

        int end = dataPosition();
        setDataPosition(lengthAt);
        writeInt(end - start);
        setDataPosition(end);
    }

    void readBundleBody(Map<String, Object> into, ClassLoader loader)
    {
        int length = readInt();
        if (length <= 0) return;

        int magic = readInt();
        if (magic != BUNDLE_MAGIC)
        {
            throw new IllegalStateException("Bad magic number for Bundle: 0x" + Integer.toHexString(magic));
        }

        int start = dataPosition();
        int count = readInt();
        for (int index = 0; index < count && dataAvail() > 0; index++)
        {
            String key = readString();
            into.put(key, readValue(loader));
        }

        setDataPosition(start + length);
    }

    private void writeCharSequence(CharSequence value)
    {
        if (value == null)
        {
            writeInt(1);
            writeString(null);
            return;
        }

        if (value instanceof android.text.Spanned)
        {
            writeLocal(value);
            return;
        }

        writeInt(1);
        writeString(value.toString());
    }

    private CharSequence readCharSequence()
    {
        if (peekInt() == LOCAL_OBJECT)
        {
            Object value = readLocal();
            return value instanceof CharSequence ? (CharSequence)value : null;
        }

        readInt();
        return readString();
    }

    private Object readParcelableValue(ClassLoader loader)
    {
        ClassLoader effective = loader == null ? mClassLoader : loader;

        if (peekInt() == LOCAL_OBJECT) return readLocal();

        String className = readString();
        if (className == null) return null;

        return create(creatorFor(className, effective), effective);
    }

    private Object readSerializableValue(ClassLoader loader)
    {
        if (peekInt() == LOCAL_OBJECT) return readLocal();

        String className = readString();
        if (className == null) return null;

        byte[] payload = createByteArray();
        if (payload == null) return null;

        final ClassLoader effective = loader(loader == null ? mClassLoader : loader);
        try
        {
            ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(payload))
            {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass description) throws IOException, ClassNotFoundException
                {
                    try
                    {
                        return Class.forName(description.getName(), false, effective);
                    }
                    catch (ClassNotFoundException missing)
                    {
                        return super.resolveClass(description);
                    }
                }
            };

            return stream.readObject();
        }
        catch (IOException | ClassNotFoundException failure)
        {
            throw new BadParcelableException("Parcelable encountered " + failure.getClass().getSimpleName()
                                             + " reading a Serializable object (name = " + className + ")");
        }
    }

    private Object create(Parcelable.Creator<?> creator, ClassLoader loader)
    {
        if (creator instanceof Parcelable.ClassLoaderCreator)
        {
            return ((Parcelable.ClassLoaderCreator<?>)creator).createFromParcel(this, loader(loader));
        }

        return creator.createFromParcel(this);
    }

    private void flatten(Parcelable value, int flags)
    {
        mFlattening.add(value);
        try
        {
            value.writeToParcel(this, flags);
        }
        finally
        {
            mFlattening.remove(mFlattening.size() - 1);
        }
    }

    private boolean isFlattening(Object value)
    {
        for (int index = mFlattening.size() - 1; index >= 0; index--)
        {
            if (mFlattening.get(index) == value) return true;
        }

        return false;
    }

    private void writeLocal(Object value)
    {
        writeInt(LOCAL_OBJECT);
        if (value == null)
        {
            writeInt(-1);
            return;
        }

        int handle = mObjects.size();
        mObjects.add(value);
        mObjectOffsets.add(dataPosition());
        writeInt(handle);
    }

    private Object readLocal()
    {
        int marker = readInt();
        int handle = readInt();
        if (marker != LOCAL_OBJECT || handle < 0 || handle >= mObjects.size()) return null;

        return mObjects.get(handle);
    }

    private Object objectAt(int offset)
    {
        int handle = peekIntAt(offset);
        return handle >= 0 && handle < mObjects.size() ? mObjects.get(handle) : null;
    }

    private <T> void writeFixedArray(T value, int flags, int depth, int[] dimensions)
    {
        if (depth >= dimensions.length) throw new BadParcelableException("Array has more dimensions than expected: " + dimensions.length);
        if (value == null) throw new BadParcelableException("Non-null array shouldn't have a null array.");

        int length = Array.getLength(value);
        if (length != dimensions[depth])
        {
            throw new BadParcelableException("bad length: expected " + dimensions[depth] + ", but got " + length);
        }

        Class<?> component = value.getClass().getComponentType();
        if (component.isArray() == (depth + 1 == dimensions.length))
        {
            throw new BadParcelableException("Array dimensions mismatch: expected " + dimensions.length);
        }

        if (component == boolean.class) writeBooleanArray((boolean[])value);
        else if (component == byte.class) writeByteArray((byte[])value);
        else if (component == char.class) writeCharArray((char[])value);
        else if (component == int.class) writeIntArray((int[])value);
        else if (component == long.class) writeLongArray((long[])value);
        else if (component == float.class) writeFloatArray((float[])value);
        else if (component == double.class) writeDoubleArray((double[])value);
        else if (component == String.class) writeStringArray((String[])value);
        else if (component == IBinder.class) writeBinderArray((IBinder[])value);
        else if (IInterface.class.isAssignableFrom(component)) writeInterfaceArray((IInterface[])value);
        else if (Parcelable.class.isAssignableFrom(component)) writeTypedArray((Parcelable[])value, flags);
        else if (component.isArray())
        {
            writeInt(length);
            for (int index = 0; index < length; index++) writeFixedArray(Array.get(value, index), flags, depth + 1, dimensions);
        }
        else
        {
            throw new BadParcelableException("Fixed-size array of " + component + " is not supported");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T createFixedArray(Class<T> type, Parcelable.Creator<?> creator, Function<IBinder, ?> asInterface, int[] dimensions)
    {
        if (peekInt() < 0)
        {
            readInt();
            return null;
        }

        return (T)createFixedArray(type, creator, asInterface, 0, dimensions);
    }

    @SuppressWarnings("unchecked")
    private Object createFixedArray(Class<?> type, Parcelable.Creator<?> creator, Function<IBinder, ?> asInterface, int depth,
                                    int[] dimensions)
    {
        Class<?> component = type.getComponentType();
        Object value;

        if (component == boolean.class) value = createBooleanArray();
        else if (component == byte.class) value = createByteArray();
        else if (component == char.class) value = createCharArray();
        else if (component == int.class) value = createIntArray();
        else if (component == long.class) value = createLongArray();
        else if (component == float.class) value = createFloatArray();
        else if (component == double.class) value = createDoubleArray();
        else if (component == String.class) value = createStringArray();
        else if (component == IBinder.class) value = createBinderArray();
        else if (component.isArray())
        {
            int length = readInt();
            value = Array.newInstance(component, Math.max(length, 0));
            for (int index = 0; index < length; index++)
            {
                Array.set(value, index, createFixedArray(component, creator, asInterface, depth + 1, dimensions));
            }
        }
        else if (IInterface.class.isAssignableFrom(component) && asInterface != null)
        {
            int length = readInt();
            value = Array.newInstance(component, Math.max(length, 0));
            for (int index = 0; index < length; index++) Array.set(value, index, asInterface.apply(readStrongBinder()));
        }
        else if (Parcelable.class.isAssignableFrom(component) && creator != null)
        {
            int length = readInt();
            value = Array.newInstance(component, Math.max(length, 0));
            for (int index = 0; index < length; index++) Array.set(value, index, readTypedObject(creator));
        }
        else
        {
            throw new BadParcelableException("Fixed-size array of " + component + " cannot be created");
        }

        int length = value == null ? -1 : Array.getLength(value);
        if (depth < dimensions.length && length != dimensions[depth])
        {
            throw new BadParcelableException("Bad length: expected " + dimensions[depth] + ", but got " + length);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    private void readFixedArray(Object destination, Parcelable.Creator<?> creator, Function<IBinder, ?> asInterface)
    {
        Object value = createFixedArray(destination.getClass(), creator, asInterface, 0, new int[] {Array.getLength(destination)});
        copyFixed(value, destination);
    }

    private static void copyFixed(Object source, Object destination)
    {
        int length = Array.getLength(destination);
        if (source == null || Array.getLength(source) != length) throw new BadParcelableException("bad array lengths");

        if (!destination.getClass().getComponentType().isArray())
        {
            System.arraycopy(source, 0, destination, 0, length);
            return;
        }

        for (int index = 0; index < length; index++) copyFixed(Array.get(source, index), Array.get(destination, index));
    }

    private static Parcelable.Creator<?> creatorFor(String className, ClassLoader loader)
    {
        Class<?> type = findClass(className, loader);
        if (type == null) throw new BadParcelableException("ClassNotFoundException when unmarshalling: " + className);

        try
        {
            java.lang.reflect.Field field = type.getField("CREATOR");
            Object creator = field.get(null);
            if (creator instanceof Parcelable.Creator) return (Parcelable.Creator<?>)creator;
        }
        catch (NoSuchFieldException | IllegalAccessException missing)
        {
            throw new BadParcelableException("Parcelable protocol requires a Parcelable.Creator object called CREATOR on class "
                                             + className);
        }

        throw new BadParcelableException("Parcelable protocol requires a Parcelable.Creator object called CREATOR on class "
                                         + className);
    }

    private static Class<?> findClass(String className, ClassLoader loader)
    {
        try
        {
            return Class.forName(className, false, loader(loader));
        }
        catch (ClassNotFoundException missing)
        {
            try
            {
                return Class.forName(className);
            }
            catch (ClassNotFoundException absent)
            {
                return null;
            }
        }
    }

    private static ClassLoader loader(ClassLoader preferred)
    {
        if (preferred != null) return preferred;

        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context != null ? context : Parcel.class.getClassLoader();
    }

    private int readExceptionCode()
    {
        int code = readInt();

        if (code == EX_HAS_NOTED_APPOPS_REPLY_HEADER)
        {
            int header = readInt();
            if (header > 0) setDataPosition(dataPosition() + header);
            code = readInt();
        }

        if (code == EX_HAS_STRICTMODE_REPLY_HEADER)
        {
            int header = readInt();
            if (header > 0) setDataPosition(dataPosition() + header);
            code = 0;
        }

        return code;
    }

    private int readArrayLength(int elementBytes)
    {
        int start = mPosition;
        int length = readInt();
        if (length < 0) return -1;

        if ((long)length * elementBytes > dataAvail() + 4L)
        {
            mPosition = start + 4;
            throw new BadParcelableException("array length " + length + " exceeds the " + dataAvail() + " bytes left in the parcel");
        }

        return length;
    }

    private int peekInt()
    {
        if (mPosition < 0 || mPosition + 4 > mSize) return 0;
        return peekIntAt(mPosition);
    }

    private int peekIntAt(int offset)
    {
        return (mData[offset] & 0xff) | ((mData[offset + 1] & 0xff) << 8) | ((mData[offset + 2] & 0xff) << 16)
               | ((mData[offset + 3] & 0xff) << 24);
    }

    private void putIntAt(int offset, int value)
    {
        mData[offset] = (byte)value;
        mData[offset + 1] = (byte)(value >>> 8);
        mData[offset + 2] = (byte)(value >>> 16);
        mData[offset + 3] = (byte)(value >>> 24);
    }

    private int reserve(int bytes)
    {
        int at = mPosition;
        int end = at + bytes;
        ensureCapacity(end);

        if (at > mSize) Arrays.fill(mData, mSize, at, (byte)0);
        Arrays.fill(mData, at, end, (byte)0);

        mPosition = end;
        if (end > mSize) mSize = end;
        return at;
    }

    private void ensureCapacity(int size)
    {
        if (size <= mData.length) return;

        int capacity = Math.max(size, mData.length * 3 / 2 + 16);
        mData = Arrays.copyOf(mData, capacity);
    }

    private static int padded(int length)
    {
        return (length + 3) & ~3;
    }

    private static void copyInto(Object source, Object destination)
    {
        if (destination == null) return;

        int length = source == null ? -1 : Array.getLength(source);
        if (length != Array.getLength(destination)) throw new RuntimeException("bad array lengths");

        System.arraycopy(source, 0, destination, 0, length);
    }

    private static <T> void replaceContents(List<T> destination, List<T> source)
    {
        if (destination == null) return;

        destination.clear();
        if (source != null) destination.addAll(source);
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value)
    {
        return (T)value;
    }
}
