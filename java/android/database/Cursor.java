package android.database;

public interface Cursor extends java.io.Closeable
{
    int FIELD_TYPE_NULL = 0;
    int FIELD_TYPE_INTEGER = 1;
    int FIELD_TYPE_FLOAT = 2;
    int FIELD_TYPE_STRING = 3;
    int FIELD_TYPE_BLOB = 4;

    int getCount();

    int getPosition();

    boolean move(int offset);

    boolean moveToPosition(int position);

    boolean moveToFirst();

    boolean moveToLast();

    boolean moveToNext();

    boolean moveToPrevious();

    boolean isFirst();

    boolean isLast();

    boolean isBeforeFirst();

    boolean isAfterLast();

    int getColumnCount();

    int getColumnIndex(String columnName);

    int getColumnIndexOrThrow(String columnName);

    String getColumnName(int columnIndex);

    String[] getColumnNames();

    String getString(int columnIndex);

    short getShort(int columnIndex);

    int getInt(int columnIndex);

    long getLong(int columnIndex);

    float getFloat(int columnIndex);

    double getDouble(int columnIndex);

    byte[] getBlob(int columnIndex);

    int getType(int columnIndex);

    boolean isNull(int columnIndex);

    boolean isClosed();

    @Override
    void close();

    default void setExtras(android.os.Bundle extras)
    {
    }

    default android.os.Bundle getExtras()
    {
        return android.os.Bundle.EMPTY;
    }

    default android.os.Bundle respond(android.os.Bundle extras)
    {
        return android.os.Bundle.EMPTY;
    }

    default void setNotificationUri(android.content.ContentResolver resolver, android.net.Uri uri)
    {
    }

    default android.net.Uri getNotificationUri()
    {
        return null;
    }

    default void registerContentObserver(ContentObserver observer)
    {
    }

    default void unregisterContentObserver(ContentObserver observer)
    {
    }

    default void registerDataSetObserver(DataSetObserver observer)
    {
    }

    default void unregisterDataSetObserver(DataSetObserver observer)
    {
    }

    default boolean requery()
    {
        return true;
    }

    default void deactivate()
    {
    }
}
