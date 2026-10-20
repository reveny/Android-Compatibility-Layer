package android.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteProgram;
import android.database.sqlite.SQLiteStatement;

public class DatabaseUtils
{
    private DatabaseUtils()
    {
    }

    public static void appendEscapedSQLString(StringBuilder into, String value)
    {
        if (into == null) return;

        into.append('\'');

        if (value != null)
        {
            for (int index = 0; index < value.length(); index++)
            {
                char letter = value.charAt(index);

                if (letter == '\'') into.append('\'');

                into.append(letter);
            }
        }

        into.append('\'');
    }

    public static String sqlEscapeString(String value)
    {
        StringBuilder escaped = new StringBuilder();
        appendEscapedSQLString(escaped, value);

        return escaped.toString();
    }

    public static void appendValueToSql(StringBuilder into, Object value)
    {
        if (into == null) return;

        if (value == null) into.append("NULL");
        else if (value instanceof Boolean) into.append(((Boolean)value).booleanValue() ? '1' : '0');
        else if (value instanceof Number) into.append(value.toString());
        else appendEscapedSQLString(into, value.toString());
    }

    public static long queryNumEntries(SQLiteDatabase database, String table)
    {
        return queryNumEntries(database, table, null, null);
    }

    public static long queryNumEntries(SQLiteDatabase database, String table, String selection)
    {
        return queryNumEntries(database, table, selection, null);
    }

    public static long queryNumEntries(SQLiteDatabase database, String table, String selection, String[] selectionArgs)
    {
        if (database == null || table == null) return 0;

        String where = selection == null || selection.isEmpty() ? "" : " where " + selection;
        Cursor cursor = database.rawQuery("select count(*) from " + table + where, selectionArgs);

        if (cursor == null) return 0;

        try
        {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
        finally
        {
            cursor.close();
        }
    }

    public static long longForQuery(SQLiteDatabase database, String query, String[] selectionArgs)
    {
        if (database == null) return 0;

        Cursor cursor = database.rawQuery(query, selectionArgs);

        if (cursor == null) return 0;

        try
        {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        }
        finally
        {
            cursor.close();
        }
    }

    public static long longForQuery(SQLiteStatement statement, String[] selectionArgs)
    {
        if (statement == null) return 0;

        bindAllArgsAsStrings(statement, selectionArgs);

        return statement.simpleQueryForLong();
    }

    public static String stringForQuery(SQLiteStatement statement, String[] selectionArgs)
    {
        if (statement == null) return null;

        bindAllArgsAsStrings(statement, selectionArgs);

        return statement.simpleQueryForString();
    }

    public static void bindAllArgsAsStrings(SQLiteProgram program, String[] selectionArgs)
    {
        if (program == null || selectionArgs == null) return;

        for (int index = selectionArgs.length; index != 0; index--)
        {
            program.bindString(index, selectionArgs[index - 1]);
        }
    }

    public static String stringForQuery(SQLiteDatabase database, String query, String[] selectionArgs)
    {
        if (database == null) return null;

        Cursor cursor = database.rawQuery(query, selectionArgs);

        if (cursor == null) return null;

        try
        {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
        finally
        {
            cursor.close();
        }
    }

    public static int getSqlStatementType(String sql)
    {
        if (sql == null) return STATEMENT_OTHER;

        String trimmed = sql.trim();

        if (trimmed.length() < 3) return STATEMENT_OTHER;

        String prefix = trimmed.substring(0, 3).toUpperCase(java.util.Locale.US);

        if (prefix.equals("SEL")) return STATEMENT_SELECT;
        if (prefix.equals("INS") || prefix.equals("UPD") || prefix.equals("REP") || prefix.equals("DEL"))
        {
            return STATEMENT_UPDATE;
        }
        if (prefix.equals("ATT")) return STATEMENT_ATTACH;
        if (prefix.equals("COM") || prefix.equals("END")) return STATEMENT_COMMIT;
        if (prefix.equals("ROL")) return STATEMENT_ABORT;
        if (prefix.equals("BEG")) return STATEMENT_BEGIN;
        if (prefix.equals("PRA")) return STATEMENT_PRAGMA;
        if (prefix.equals("CRE") || prefix.equals("DRO") || prefix.equals("ALT")) return STATEMENT_DDL;

        return STATEMENT_OTHER;
    }

    public static final int STATEMENT_SELECT = 1;
    public static final int STATEMENT_UPDATE = 2;
    public static final int STATEMENT_ATTACH = 3;
    public static final int STATEMENT_BEGIN = 4;
    public static final int STATEMENT_COMMIT = 5;
    public static final int STATEMENT_ABORT = 6;
    public static final int STATEMENT_PRAGMA = 7;
    public static final int STATEMENT_DDL = 8;
    public static final int STATEMENT_UNPREPARED = 9;
    public static final int STATEMENT_OTHER = 99;

    public static void bindObjectToProgram(SQLiteProgram program, int index, Object value)
    {
        if (program == null) return;

        if (value == null) program.bindNull(index);
        else if (value instanceof Double || value instanceof Float) program.bindDouble(index, ((Number)value).doubleValue());
        else if (value instanceof Number) program.bindLong(index, ((Number)value).longValue());
        else if (value instanceof byte[]) program.bindBlob(index, (byte[])value);
        else if (value instanceof Boolean) program.bindLong(index, ((Boolean)value).booleanValue() ? 1 : 0);
        else program.bindString(index, value.toString());
    }

    public static void cursorRowToContentValues(Cursor cursor, ContentValues values)
    {
        if (cursor == null || values == null) return;

        String[] columns = cursor.getColumnNames();

        for (int index = 0; index < columns.length; index++)
        {
            values.put(columns[index], cursor.getString(index));
        }
    }

    public static void cursorStringToContentValues(Cursor cursor, String column, ContentValues values)
    {
        if (cursor == null || values == null) return;

        values.put(column, cursor.getString(cursor.getColumnIndexOrThrow(column)));
    }

    public static void cursorIntToContentValues(Cursor cursor, String column, ContentValues values)
    {
        if (cursor == null || values == null) return;

        int index = cursor.getColumnIndexOrThrow(column);

        if (cursor.isNull(index)) values.put(column, (Integer)null);
        else values.put(column, Integer.valueOf(cursor.getInt(index)));
    }

    public static void cursorLongToContentValues(Cursor cursor, String column, ContentValues values)
    {
        if (cursor == null || values == null) return;

        int index = cursor.getColumnIndexOrThrow(column);

        if (cursor.isNull(index)) values.put(column, (Long)null);
        else values.put(column, Long.valueOf(cursor.getLong(index)));
    }
}
