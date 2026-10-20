package runtime;

import android.os.Bundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public final class SavedState
{
    private static final String TAG = "SavedState";

    public static final int MAGIC = 0x41475253;
    public static final int VERSION = 1;
    public static final String LEAF = "saved-state";

    private static final int TYPE_END = 0;
    private static final int TYPE_STRING = 1;
    private static final int TYPE_INT = 2;
    private static final int TYPE_LONG = 3;
    private static final int TYPE_FLOAT = 4;
    private static final int TYPE_DOUBLE = 5;
    private static final int TYPE_BOOLEAN = 6;
    private static final int TYPE_BUNDLE = 7;
    private static final int TYPE_INT_ARRAY = 8;
    private static final int TYPE_STRING_ARRAY = 9;
    private static final int TYPE_STRING_LIST = 10;
    private static final int TYPE_BYTE_ARRAY = 11;
    private static final int TYPE_BYTE = 12;
    private static final int TYPE_SHORT = 13;
    private static final int TYPE_CHAR = 14;
    private static final int TYPE_LONG_ARRAY = 15;
    private static final int TYPE_INTEGER_LIST = 16;

    private SavedState()
    {
    }

    public static File fileFor(String dataDir, String activityClassName)
    {
        return new File(new File(dataDir), LEAF + "-" + activityClassName + ".bin");
    }

    public static boolean write(File target, Bundle bundle)
    {
        if (target == null || bundle == null) return false;

        DataOutputStream out = null;

        try
        {
            out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(target)));
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            writeBundle(out, bundle);
            out.flush();

            return true;
        }
        catch (IOException failure)
        {
            Log.w(TAG, "saved state could not be written: " + failure);
            return false;
        }
        finally
        {
            if (out != null) try { out.close(); } catch (IOException ignored) { }
        }
    }

    public static Bundle read(File source)
    {
        if (source == null || !source.exists()) return null;

        DataInputStream in = null;

        try
        {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(source)));

            if (in.readInt() != MAGIC) return null;
            if (in.readInt() != VERSION) return null;

            return readBundle(in);
        }
        catch (IOException failure)
        {
            Log.w(TAG, "saved state could not be read: " + failure);
            return null;
        }
        finally
        {
            if (in != null) try { in.close(); } catch (IOException ignored) { }
        }
    }

    private static void writeBundle(DataOutputStream out, Bundle bundle) throws IOException
    {
        for (String key : bundle.keySet())
        {
            Object value = bundle.get(key);
            int type = typeOf(value);

            if (type == TYPE_END)
            {
                Log.w(TAG, "dropping " + key + ", whose type does not survive being written down");
                continue;
            }

            out.writeByte(type);
            out.writeUTF(key);
            writeValue(out, type, value);
        }

        out.writeByte(TYPE_END);
    }

    private static int typeOf(Object value)
    {
        if (value instanceof String) return TYPE_STRING;
        if (value instanceof Integer) return TYPE_INT;
        if (value instanceof Long) return TYPE_LONG;
        if (value instanceof Float) return TYPE_FLOAT;
        if (value instanceof Double) return TYPE_DOUBLE;
        if (value instanceof Boolean) return TYPE_BOOLEAN;
        if (value instanceof Byte) return TYPE_BYTE;
        if (value instanceof Short) return TYPE_SHORT;
        if (value instanceof Character) return TYPE_CHAR;
        if (value instanceof Bundle) return TYPE_BUNDLE;
        if (value instanceof int[]) return TYPE_INT_ARRAY;
        if (value instanceof long[]) return TYPE_LONG_ARRAY;
        if (value instanceof byte[]) return TYPE_BYTE_ARRAY;
        if (value instanceof String[]) return TYPE_STRING_ARRAY;

        if (value instanceof ArrayList)
        {
            ArrayList<?> list = (ArrayList<?>)value;
            if (list.isEmpty()) return TYPE_STRING_LIST;
            if (list.get(0) instanceof String) return TYPE_STRING_LIST;
            if (list.get(0) instanceof Integer) return TYPE_INTEGER_LIST;
        }

        return TYPE_END;
    }

    private static void writeValue(DataOutputStream out, int type, Object value) throws IOException
    {
        switch (type)
        {
            case TYPE_STRING: out.writeUTF((String)value); break;
            case TYPE_INT: out.writeInt((Integer)value); break;
            case TYPE_LONG: out.writeLong((Long)value); break;
            case TYPE_FLOAT: out.writeFloat((Float)value); break;
            case TYPE_DOUBLE: out.writeDouble((Double)value); break;
            case TYPE_BOOLEAN: out.writeBoolean((Boolean)value); break;
            case TYPE_BYTE: out.writeByte((Byte)value); break;
            case TYPE_SHORT: out.writeShort((Short)value); break;
            case TYPE_CHAR: out.writeChar((Character)value); break;
            case TYPE_BUNDLE: writeBundle(out, (Bundle)value); break;

            case TYPE_INT_ARRAY:
            {
                int[] array = (int[])value;
                out.writeInt(array.length);
                for (int element : array) out.writeInt(element);
                break;
            }

            case TYPE_LONG_ARRAY:
            {
                long[] array = (long[])value;
                out.writeInt(array.length);
                for (long element : array) out.writeLong(element);
                break;
            }

            case TYPE_BYTE_ARRAY:
            {
                byte[] array = (byte[])value;
                out.writeInt(array.length);
                out.write(array);
                break;
            }

            case TYPE_STRING_ARRAY:
            {
                String[] array = (String[])value;
                out.writeInt(array.length);
                for (String element : array) out.writeUTF(element == null ? "" : element);
                break;
            }

            case TYPE_STRING_LIST:
            {
                ArrayList<?> list = (ArrayList<?>)value;
                out.writeInt(list.size());
                for (Object element : list) out.writeUTF(element == null ? "" : element.toString());
                break;
            }

            case TYPE_INTEGER_LIST:
            {
                ArrayList<?> list = (ArrayList<?>)value;
                out.writeInt(list.size());
                for (Object element : list) out.writeInt(((Integer)element).intValue());
                break;
            }

            default: break;
        }
    }

    private static Bundle readBundle(DataInputStream in) throws IOException
    {
        Bundle bundle = new Bundle();

        for (;;)
        {
            int type = in.readByte();
            if (type == TYPE_END) return bundle;

            String key = in.readUTF();

            switch (type)
            {
                case TYPE_STRING: bundle.putString(key, in.readUTF()); break;
                case TYPE_INT: bundle.putInt(key, in.readInt()); break;
                case TYPE_LONG: bundle.putLong(key, in.readLong()); break;
                case TYPE_FLOAT: bundle.putFloat(key, in.readFloat()); break;
                case TYPE_DOUBLE: bundle.putDouble(key, in.readDouble()); break;
                case TYPE_BOOLEAN: bundle.putBoolean(key, in.readBoolean()); break;
                case TYPE_BYTE: bundle.putByte(key, in.readByte()); break;
                case TYPE_SHORT: bundle.putShort(key, in.readShort()); break;
                case TYPE_CHAR: bundle.putChar(key, in.readChar()); break;
                case TYPE_BUNDLE: bundle.putBundle(key, readBundle(in)); break;

                case TYPE_INT_ARRAY:
                {
                    int[] array = new int[in.readInt()];
                    for (int index = 0; index < array.length; index++) array[index] = in.readInt();
                    bundle.putIntArray(key, array);
                    break;
                }

                case TYPE_LONG_ARRAY:
                {
                    long[] array = new long[in.readInt()];
                    for (int index = 0; index < array.length; index++) array[index] = in.readLong();
                    bundle.putLongArray(key, array);
                    break;
                }

                case TYPE_BYTE_ARRAY:
                {
                    byte[] array = new byte[in.readInt()];
                    in.readFully(array);
                    bundle.putByteArray(key, array);
                    break;
                }

                case TYPE_STRING_ARRAY:
                {
                    String[] array = new String[in.readInt()];
                    for (int index = 0; index < array.length; index++) array[index] = in.readUTF();
                    bundle.putStringArray(key, array);
                    break;
                }

                case TYPE_STRING_LIST:
                {
                    int size = in.readInt();
                    ArrayList<String> list = new ArrayList<>(size);
                    for (int index = 0; index < size; index++) list.add(in.readUTF());
                    bundle.putStringArrayList(key, list);
                    break;
                }

                case TYPE_INTEGER_LIST:
                {
                    int size = in.readInt();
                    ArrayList<Integer> list = new ArrayList<>(size);
                    for (int index = 0; index < size; index++) list.add(Integer.valueOf(in.readInt()));
                    bundle.putIntegerArrayList(key, list);
                    break;
                }

                default: throw new IOException("unknown saved-state type " + type);
            }
        }
    }
}
