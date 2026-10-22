package android.database.sqlite;

import android.database.Cursor;

public interface SQLiteCursorDriver
{
    Cursor query(SQLiteDatabase.CursorFactory factory, String[] bindArgs);

    void cursorDeactivated();

    void cursorRequeried(Cursor cursor);

    void cursorClosed();

    void setBindArguments(String[] bindArgs);
}
