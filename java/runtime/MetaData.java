package runtime;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;

public final class MetaData
{
    public static final int ACTIVITY = 0;
    public static final int SERVICE = 1;
    public static final int RECEIVER = 2;
    public static final int PROVIDER = 3;
    public static final int APPLICATION = 4;

    private static final int NO_VALUE = -1;
    private static final int RESOURCE = -2;

    private MetaData()
    {
    }

    public static Bundle read(Resources resources, int kind, int index)
    {
        int count = Bootstrap.nativeComponentMetaDataCount(kind, index);
        if (count <= 0) return null;

        Bundle bundle = new Bundle();

        for (int entry = 0; entry < count; entry++)
        {
            String name = Bootstrap.nativeComponentMetaDataName(kind, index, entry);
            if (name == null) continue;

            int type = Bootstrap.nativeComponentMetaDataType(kind, index, entry);
            int data = Bootstrap.nativeComponentMetaDataData(kind, index, entry);
            String text = Bootstrap.nativeComponentMetaDataValue(kind, index, entry);

            put(bundle, resources, name, type, data, text);
        }

        return bundle;
    }

    private static void put(Bundle bundle, Resources resources, String name, int type, int data, String text)
    {
        if (type == RESOURCE)
        {
            bundle.putInt(name, data);
            return;
        }

        if (type == TypedValue.TYPE_REFERENCE && data != 0 && resources != null)
        {
            TypedValue resolved = new TypedValue();

            try
            {
                resources.getValue(data, resolved, true);
                put(bundle, null, name, resolved.type, resolved.data, resolved.type == TypedValue.TYPE_STRING
                                                                          ? String.valueOf(resolved.coerceToString())
                                                                          : text);
                return;
            }
            catch (Resources.NotFoundException missing)
            {
                bundle.putInt(name, data);
                return;
            }
        }

        if (type == TypedValue.TYPE_INT_BOOLEAN)
        {
            bundle.putBoolean(name, data != 0);
            return;
        }

        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT)
        {
            bundle.putInt(name, data);
            return;
        }

        if (type == TypedValue.TYPE_FLOAT)
        {
            bundle.putFloat(name, Float.intBitsToFloat(data));
            return;
        }

        if (type == TypedValue.TYPE_REFERENCE)
        {
            bundle.putInt(name, data);
            return;
        }

        if (type == NO_VALUE && text == null) return;

        bundle.putString(name, text);
    }
}
