package android.view.inspector;

import android.graphics.Color;

public interface PropertyReader
{
    void readBoolean(int id, boolean value);

    void readByte(int id, byte value);

    void readChar(int id, char value);

    void readDouble(int id, double value);

    void readFloat(int id, float value);

    void readInt(int id, int value);

    void readLong(int id, long value);

    void readShort(int id, short value);

    void readObject(int id, Object value);

    void readColor(int id, int value);

    void readColor(int id, long value);

    void readColor(int id, Color value);

    void readGravity(int id, int value);

    void readIntEnum(int id, int value);

    void readIntFlag(int id, int value);

    void readResourceId(int id, int value);

    class PropertyTypeMismatchException extends RuntimeException
    {
        public PropertyTypeMismatchException(int id, String expectedPropertyType, String actualPropertyType, String propertyName)
        {
            super(String.format("Attempted to read property with ID 0x%08X as type %s, but the ID is of type %s (%s).", id,
                                expectedPropertyType, actualPropertyType, propertyName));
        }

        public PropertyTypeMismatchException(int id, String expectedPropertyType, String actualPropertyType)
        {
            super(String.format("Attempted to read property with ID 0x%08X as type %s, but the ID is of type %s.", id,
                                expectedPropertyType, actualPropertyType));
        }
    }
}
