package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.MatrixCursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import runtime.Sqlite;

public final class SQLiteDatabase
{
    private static final String TAG = "SQLiteDatabase";

    public static final int OPEN_READWRITE = 0x00000000;
    public static final int OPEN_READONLY = 0x00000001;
    public static final int CREATE_IF_NECESSARY = 0x10000000;
    public static final int NO_LOCALIZED_COLLATORS = 0x00000010;
    public static final int ENABLE_WRITE_AHEAD_LOGGING = 0x20000000;

    public static final int CONFLICT_NONE = 0;
    public static final int CONFLICT_ROLLBACK = 1;
    public static final int CONFLICT_ABORT = 2;
    public static final int CONFLICT_FAIL = 3;
    public static final int CONFLICT_IGNORE = 4;
    public static final int CONFLICT_REPLACE = 5;

    private static final String[] CONFLICT_CLAUSE = {"", " OR ROLLBACK", " OR ABORT", " OR FAIL", " OR IGNORE", " OR REPLACE"};

    private final String mPath;
    private long mHandle;
    private int mTransactionDepth;
    private boolean mTransactionSucceeded;

    public interface CursorFactory
    {
        Cursor newCursor(SQLiteDatabase database, Object masterQuery, String editTable, Object query);
    }

    private SQLiteDatabase(String path, long handle)
    {
        mPath = path;
        mHandle = handle;
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags)
    {
        boolean readOnly = (flags & OPEN_READONLY) != 0;
        long handle = Sqlite.nativeOpen(path, readOnly);

        if (handle == 0) throw new SQLiteException("could not open " + path);

        SQLiteDatabase database = new SQLiteDatabase(path, handle);
        database.execSQL("PRAGMA foreign_keys=ON");

        return database;
    }

    public static SQLiteDatabase openDatabase(java.io.File path, OpenParams openParams)
    {
        int flags = openParams == null ? CREATE_IF_NECESSARY : openParams.getOpenFlags();

        return openDatabase(path.getPath(), openParams == null ? null : openParams.getCursorFactory(), flags);
    }

    public static final class OpenParams
    {
        private final int mOpenFlags;
        private final CursorFactory mCursorFactory;
        private final DatabaseErrorHandler mErrorHandler;
        private final int mLookasideSlotSize;
        private final int mLookasideSlotCount;

        private OpenParams(int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler,
                           int lookasideSlotSize, int lookasideSlotCount)
        {
            mOpenFlags = openFlags;
            mCursorFactory = cursorFactory;
            mErrorHandler = errorHandler;
            mLookasideSlotSize = lookasideSlotSize;
            mLookasideSlotCount = lookasideSlotCount;
        }

        public int getOpenFlags()
        {
            return mOpenFlags;
        }

        public CursorFactory getCursorFactory()
        {
            return mCursorFactory;
        }

        public DatabaseErrorHandler getErrorHandler()
        {
            return mErrorHandler;
        }

        public int getLookasideSlotSize()
        {
            return mLookasideSlotSize;
        }

        public int getLookasideSlotCount()
        {
            return mLookasideSlotCount;
        }

        public Builder toBuilder()
        {
            return new Builder(this);
        }

        public static final class Builder
        {
            private int mOpenFlags = CREATE_IF_NECESSARY;
            private CursorFactory mCursorFactory;
            private DatabaseErrorHandler mErrorHandler;
            private int mLookasideSlotSize = -1;
            private int mLookasideSlotCount = -1;

            public Builder()
            {
            }

            public Builder(OpenParams source)
            {
                if (source == null) return;

                mOpenFlags = source.mOpenFlags;
                mCursorFactory = source.mCursorFactory;
                mErrorHandler = source.mErrorHandler;
                mLookasideSlotSize = source.mLookasideSlotSize;
                mLookasideSlotCount = source.mLookasideSlotCount;
            }

            public Builder addOpenFlags(int flags)
            {
                mOpenFlags |= flags;
                return this;
            }

            public Builder removeOpenFlags(int flags)
            {
                mOpenFlags &= ~flags;
                return this;
            }

            public Builder setOpenFlags(int flags)
            {
                mOpenFlags = flags;
                return this;
            }

            public Builder setCursorFactory(CursorFactory factory)
            {
                mCursorFactory = factory;
                return this;
            }

            public Builder setErrorHandler(DatabaseErrorHandler errorHandler)
            {
                mErrorHandler = errorHandler;
                return this;
            }

            public Builder setLookasideConfig(int slotSize, int slotCount)
            {
                mLookasideSlotSize = slotSize;
                mLookasideSlotCount = slotCount;
                return this;
            }

            public Builder setJournalMode(String mode)
            {
                return this;
            }

            public Builder setSynchronousMode(String mode)
            {
                return this;
            }

            public OpenParams build()
            {
                return new OpenParams(mOpenFlags, mCursorFactory, mErrorHandler, mLookasideSlotSize, mLookasideSlotCount);
            }
        }
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory)
    {
        return openDatabase(path, factory, CREATE_IF_NECESSARY);
    }

    public static SQLiteDatabase openOrCreateDatabase(java.io.File path, CursorFactory factory)
    {
        return openDatabase(path.getPath(), factory, CREATE_IF_NECESSARY);
    }

    public static int releaseMemory()
    {
        return 0;
    }

    public static SQLiteDatabase create(CursorFactory factory)
    {
        return openDatabase(":memory:", factory, CREATE_IF_NECESSARY);
    }

    public static boolean deleteDatabase(java.io.File path)
    {
        return path != null && path.delete();
    }

    public String getPath()
    {
        return mPath;
    }

    public boolean isOpen()
    {
        return mHandle != 0;
    }

    public boolean isReadOnly()
    {
        return false;
    }

    public void close()
    {
        if (mHandle == 0) return;

        Sqlite.nativeClose(mHandle);
        mHandle = 0;
    }

    public void execSQL(String sql)
    {
        checkOpen();

        int outcome = Sqlite.nativeExec(mHandle, sql);
        if (outcome != 0) throw new SQLiteException(describeFailure(sql));
    }

    public void execSQL(String sql, Object[] bindArgs)
    {
        if (bindArgs == null || bindArgs.length == 0)
        {
            execSQL(sql);
            return;
        }

        SQLiteStatement statement = compileStatement(sql);
        try
        {
            statement.bindAll(bindArgs);
            statement.execute();
        }
        finally
        {
            statement.close();
        }
    }

    public SQLiteStatement compileStatement(String sql)
    {
        checkOpen();

        long prepared = Sqlite.nativePrepare(mHandle, sql);
        if (prepared == 0) throw new SQLiteException(describeFailure(sql));

        return new SQLiteStatement(this, prepared);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs)
    {
        checkOpen();

        long prepared = Sqlite.nativePrepare(mHandle, sql);
        if (prepared == 0) throw new SQLiteException(describeFailure(sql));

        try
        {
            if (selectionArgs != null)
            {
                for (int index = 0; index < selectionArgs.length; index++)
                {
                    if (selectionArgs[index] == null) Sqlite.nativeBindNull(prepared, index + 1);
                    else Sqlite.nativeBindString(prepared, index + 1, selectionArgs[index]);
                }
            }

            return materialise(prepared);
        }
        finally
        {
            Sqlite.nativeFinalize(prepared);
        }
    }

    public Cursor rawQuery(String sql, String[] selectionArgs, Object cancellationSignal)
    {
        return rawQuery(sql, selectionArgs);
    }

    public Cursor rawQueryWithFactory(CursorFactory factory, String sql, String[] selectionArgs, String editTable)
    {
        return rawQuery(sql, selectionArgs);
    }

    public Cursor rawQueryWithFactory(CursorFactory factory, String sql, String[] selectionArgs, String editTable,
                                      Object cancellationSignal)
    {
        return rawQuery(sql, selectionArgs);
    }

    public Cursor queryWithFactory(CursorFactory factory, boolean distinct, String table, String[] columns, String selection,
                                   String[] selectionArgs, String groupBy, String having, String orderBy, String limit)
    {
        return query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit)
    {
        return query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit,
                        android.os.CancellationSignal cancellationSignal)
    {
        return query(distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having,
                        String orderBy)
    {
        return query(table, columns, selection, selectionArgs, groupBy, having, orderBy, null);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having,
                        String orderBy, String limit)
    {
        StringBuilder sql = new StringBuilder("SELECT ");

        if (columns == null || columns.length == 0)
        {
            sql.append('*');
        }
        else
        {
            for (int index = 0; index < columns.length; index++)
            {
                if (index != 0) sql.append(", ");
                sql.append(columns[index]);
            }
        }

        sql.append(" FROM ").append(table);
        if (selection != null && !selection.isEmpty()) sql.append(" WHERE ").append(selection);
        if (groupBy != null && !groupBy.isEmpty()) sql.append(" GROUP BY ").append(groupBy);
        if (having != null && !having.isEmpty()) sql.append(" HAVING ").append(having);
        if (orderBy != null && !orderBy.isEmpty()) sql.append(" ORDER BY ").append(orderBy);
        if (limit != null && !limit.isEmpty()) sql.append(" LIMIT ").append(limit);

        return rawQuery(sql.toString(), selectionArgs);
    }

    public long insert(String table, String nullColumnHack, ContentValues values)
    {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    public long insertOrThrow(String table, String nullColumnHack, ContentValues values)
    {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    public long replace(String table, String nullColumnHack, ContentValues values)
    {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_REPLACE);
    }

    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues values, int conflictAlgorithm)
    {
        StringBuilder sql = new StringBuilder("INSERT");
        sql.append(CONFLICT_CLAUSE[conflictAlgorithm]).append(" INTO ").append(table).append('(');

        List<Object> arguments = new ArrayList<>();
        StringBuilder placeholders = new StringBuilder();

        if (values == null || values.isEmpty())
        {
            sql.append(nullColumnHack == null ? "rowid" : nullColumnHack);
            placeholders.append("NULL");
        }
        else
        {
            boolean first = true;
            for (Map.Entry<String, Object> entry : values.valueSet())
            {
                if (!first)
                {
                    sql.append(',');
                    placeholders.append(',');
                }

                sql.append(entry.getKey());
                placeholders.append('?');
                arguments.add(entry.getValue());
                first = false;
            }
        }

        sql.append(") VALUES(").append(placeholders).append(')');

        SQLiteStatement statement = compileStatement(sql.toString());
        try
        {
            statement.bindAll(arguments.toArray());
            return statement.executeInsert();
        }
        finally
        {
            statement.close();
        }
    }

    public int update(String table, ContentValues values, String whereClause, String[] whereArgs)
    {
        return updateWithOnConflict(table, values, whereClause, whereArgs, CONFLICT_NONE);
    }

    public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs,
                                    int conflictAlgorithm)
    {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("nothing to update");

        StringBuilder sql = new StringBuilder("UPDATE");
        sql.append(CONFLICT_CLAUSE[conflictAlgorithm]).append(' ').append(table).append(" SET ");

        List<Object> arguments = new ArrayList<>();

        boolean first = true;
        for (Map.Entry<String, Object> entry : values.valueSet())
        {
            if (!first) sql.append(',');

            sql.append(entry.getKey()).append("=?");
            arguments.add(entry.getValue());
            first = false;
        }

        if (whereClause != null && !whereClause.isEmpty())
        {
            sql.append(" WHERE ").append(whereClause);
            if (whereArgs != null)
            {
                for (String argument : whereArgs) arguments.add(argument);
            }
        }

        SQLiteStatement statement = compileStatement(sql.toString());
        try
        {
            statement.bindAll(arguments.toArray());
            return statement.executeUpdateDelete();
        }
        finally
        {
            statement.close();
        }
    }

    public int delete(String table, String whereClause, String[] whereArgs)
    {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(table);
        if (whereClause != null && !whereClause.isEmpty()) sql.append(" WHERE ").append(whereClause);

        SQLiteStatement statement = compileStatement(sql.toString());
        try
        {
            if (whereArgs != null) statement.bindAll(whereArgs);
            return statement.executeUpdateDelete();
        }
        finally
        {
            statement.close();
        }
    }

    public void beginTransaction()
    {
        if (mTransactionDepth == 0)
        {
            execSQL("BEGIN IMMEDIATE");
            mTransactionSucceeded = false;
        }

        mTransactionDepth++;
    }

    public void beginTransactionNonExclusive()
    {
        beginTransaction();
    }

    public void beginTransactionWithListener(Object listener)
    {
        beginTransaction();
    }

    public void setTransactionSuccessful()
    {
        if (mTransactionDepth == 0) throw new IllegalStateException("no transaction is open");

        mTransactionSucceeded = true;
    }

    public void endTransaction()
    {
        if (mTransactionDepth == 0) return;

        mTransactionDepth--;
        if (mTransactionDepth != 0) return;

        execSQL(mTransactionSucceeded ? "COMMIT" : "ROLLBACK");
    }

    public boolean inTransaction()
    {
        return mTransactionDepth != 0;
    }

    public boolean isDbLockedByCurrentThread()
    {
        return mTransactionDepth != 0;
    }

    public int getVersion()
    {
        Cursor cursor = rawQuery("PRAGMA user_version", null);
        try
        {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
        finally
        {
            cursor.close();
        }
    }

    public void setVersion(int version)
    {
        execSQL("PRAGMA user_version = " + version);
    }

    public long getMaximumSize()
    {
        return Long.MAX_VALUE;
    }

    public void setForeignKeyConstraintsEnabled(boolean enabled)
    {
        execSQL("PRAGMA foreign_keys=" + (enabled ? "ON" : "OFF"));
    }

    public boolean enableWriteAheadLogging()
    {
        execSQL("PRAGMA journal_mode=WAL");
        return true;
    }

    public void disableWriteAheadLogging()
    {
        execSQL("PRAGMA journal_mode=DELETE");
    }

    public boolean isWriteAheadLoggingEnabled()
    {
        return false;
    }

    public void setLocale(java.util.Locale locale)
    {
    }

    public void setMaxSqlCacheSize(int cacheSize)
    {
    }

    public List<android.util.Pair<String, String>> getAttachedDbs()
    {
        List<android.util.Pair<String, String>> attached = new ArrayList<>();
        attached.add(new android.util.Pair<>("main", mPath));

        return attached;
    }

    public boolean isDatabaseIntegrityOk()
    {
        return true;
    }

    long handle()
    {
        return mHandle;
    }

    String describeFailure(String sql)
    {
        return Sqlite.nativeErrorMessage(mHandle) + " (while running: " + sql + ")";
    }

    private void checkOpen()
    {
        if (mHandle == 0) throw new IllegalStateException("the database " + mPath + " is closed");
    }

    private Cursor materialise(long prepared)
    {
        int columns = Sqlite.nativeColumnCount(prepared);
        String[] names = new String[columns];

        for (int index = 0; index < columns; index++) names[index] = Sqlite.nativeColumnName(prepared, index);

        MatrixCursor cursor = new MatrixCursor(names);

        for (;;)
        {
            int outcome = Sqlite.nativeStep(prepared);
            if (outcome == Sqlite.STEP_DONE) break;

            if (outcome == Sqlite.STEP_ERROR)
            {
                throw new SQLiteException(Sqlite.nativeErrorMessage(mHandle));
            }

            Object[] row = new Object[columns];
            for (int index = 0; index < columns; index++) row[index] = valueOf(prepared, index);

            cursor.addRow(row);
        }

        return cursor;
    }

    private Object valueOf(long prepared, int index)
    {
        switch (Sqlite.nativeColumnType(prepared, index))
        {
            case Sqlite.TYPE_INTEGER:
                return Sqlite.nativeColumnLong(prepared, index);
            case Sqlite.TYPE_FLOAT:
                return Sqlite.nativeColumnDouble(prepared, index);
            case Sqlite.TYPE_BLOB:
                return Sqlite.nativeColumnBlob(prepared, index);
            case Sqlite.TYPE_NULL:
                return null;
            default:
                return Sqlite.nativeColumnString(prepared, index);
        }
    }
}
