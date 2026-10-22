package android.view.inspector;

import java.util.Set;
import java.util.function.IntFunction;

public interface PropertyMapper
{
    int mapBoolean(String name, int attributeId);

    int mapByte(String name, int attributeId);

    int mapChar(String name, int attributeId);

    int mapDouble(String name, int attributeId);

    int mapFloat(String name, int attributeId);

    int mapInt(String name, int attributeId);

    int mapLong(String name, int attributeId);

    int mapShort(String name, int attributeId);

    int mapObject(String name, int attributeId);

    int mapColor(String name, int attributeId);

    int mapGravity(String name, int attributeId);

    int mapIntEnum(String name, int attributeId, IntFunction<String> mapping);

    int mapResourceId(String name, int attributeId);

    int mapIntFlag(String name, int attributeId, IntFunction<Set<String>> mapping);

    class PropertyConflictException extends RuntimeException
    {
        public PropertyConflictException(String name, String newPropertyType, String existingPropertyType)
        {
            super(String.format("Attempted to map property \"%s\" as type %s, but it is already mapped as %s.", name, newPropertyType,
                                existingPropertyType));
        }
    }
}
