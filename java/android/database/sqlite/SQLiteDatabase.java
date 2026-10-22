package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DefaultDatabaseErrorHandler;
import android.database.SQLException;
import android.os.CancellationSignal;
import android.util.Log;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import runtime.Sqlite;

public final class SQLiteDatabase extends SQLiteClosable
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

    public static final int MAX_SQL_CACHE_SIZE = 100;
    public static final int SQLITE_MAX_LIKE_PATTERN_LENGTH = 50000;

    public static final String JOURNAL_MODE_DELETE = "DELETE";
    public static final String JOURNAL_MODE_MEMORY = "MEMORY";
    public static final String JOURNAL_MODE_OFF = "OFF";
    public static final String JOURNAL_MODE_PERSIST = "PERSIST";
    public static final String JOURNAL_MODE_TRUNCATE = "TRUNCATE";
    public static final String JOURNAL_MODE_WAL = "WAL";

    public static final String SYNC_MODE_EXTRA = "EXTRA";
    public static final String SYNC_MODE_FULL = "FULL";
    public static final String SYNC_MODE_NORMAL = "NORMAL";
    public static final String SYNC_MODE_OFF = "OFF";

    private static final String MEMORY_DB_PATH = ":memory:";

    private static final String[] CONFLICT_CLAUSE = {"", " OR ROLLBACK", " OR ABORT", " OR FAIL", " OR IGNORE", " OR REPLACE"};

    private final String mPath;
    private final CursorFactory mCursorFactory;
    private final DatabaseErrorHandler mErrorHandler;
    private final boolean mReadOnly;
    private long mHandle;
    private boolean mWriteAheadLogging;
    private final ArrayDeque<Transaction> mTransactions = new ArrayDeque<>();

    public interface CursorFactory
    {
        Cursor newCursor(SQLiteDatabase database, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query);
    }

    private static final class Transaction
    {
        final SQLiteTransactionListener mListener;
        boolean mMarkedSuccessful;
        boolean mChildFailed;

        Transaction(SQLiteTransactionListener listener)
        {
            mListener = listener;
        }
    }

    private SQLiteDatabase(String path, long handle, CursorFactory factory, DatabaseErrorHandler errorHandler, boolean readOnly)
    {
        mPath = path;
        mHandle = handle;
        mCursorFactory = factory;
        mErrorHandler = errorHandler == null ? new DefaultDatabaseErrorHandler() : errorHandler;
        mReadOnly = readOnly;
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags)
    {
        return openDatabase(path, factory, flags, null);
    }

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags, DatabaseErrorHandler errorHandler)
    {
        boolean readOnly = (flags & OPEN_READONLY) != 0;
        boolean memory = MEMORY_DB_PATH.equalsIgnoreCase(path);

        if (!readOnly && !memory && (flags & CREATE_IF_NECESSARY) == 0 && !new File(path).exists())
        {
            throw new SQLiteCantOpenDatabaseException("unable to open database file: " + path);
        }

        long handle = Sqlite.nativeOpen(path, readOnly);

        if (handle == 0) throw new SQLiteCantOpenDatabaseException("unable to open database file: " + path);

        SQLiteDatabase database = new SQLiteDatabase(path, handle, factory, errorHandler, readOnly);
        if (!readOnly && !memory && (flags & ENABLE_WRITE_AHEAD_LOGGING) != 0) database.enableWriteAheadLogging();

        return database;
    }

    public static SQLiteDatabase openDatabase(File path, OpenParams openParams)
    {
        if (openParams == null) return openDatabase(path.getPath(), null, CREATE_IF_NECESSARY, null);

        SQLiteDatabase database = openDatabase(path.getPath(), openParams.getCursorFactory(), openParams.getOpenFlags(),
            openParams.getErrorHandler());
        database.applyOpenParams(openParams);

        return database;
    }

    public static SQLiteDatabase createInMemory(OpenParams openParams)
    {
        if (openParams == null) return create(null);

        SQLiteDatabase database = openDatabase(MEMORY_DB_PATH, openParams.getCursorFactory(),
            openParams.getOpenFlags() | CREATE_IF_NECESSARY, openParams.getErrorHandler());
        database.applyOpenParams(openParams);

        return database;
    }

    private void applyOpenParams(OpenParams openParams)
    {
        if (mReadOnly) return;

        if (openParams.mJournalMode != null) execSQL("PRAGMA journal_mode=" + openParams.mJournalMode);
        if (openParams.mSynchronousMode != null) execSQL("PRAGMA synchronous=" + openParams.mSynchronousMode);
    }

    public static final class OpenParams
    {
        private final int mOpenFlags;
        private final CursorFactory mCursorFactory;
        private final DatabaseErrorHandler mErrorHandler;
        private final int mLookasideSlotSize;
        private final int mLookasideSlotCount;
        private final long mIdleConnectionTimeout;
        private final String mJournalMode;
        private final String mSynchronousMode;

        private OpenParams(int openFlags, CursorFactory cursorFactory, DatabaseErrorHandler errorHandler,
                           int lookasideSlotSize, int lookasideSlotCount, long idleConnectionTimeout, String journalMode,
                           String synchronousMode)
        {
            mOpenFlags = openFlags;
            mCursorFactory = cursorFactory;
            mErrorHandler = errorHandler;
            mLookasideSlotSize = lookasideSlotSize;
            mLookasideSlotCount = lookasideSlotCount;
            mIdleConnectionTimeout = idleConnectionTimeout;
            mJournalMode = journalMode;
            mSynchronousMode = synchronousMode;
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

        public long getIdleConnectionTimeout()
        {
            return mIdleConnectionTimeout;
        }

        public String getJournalMode()
        {
            return mJournalMode;
        }

        public String getSynchronousMode()
        {
            return mSynchronousMode;
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
            private long mIdleConnectionTimeout = -1;
            private String mJournalMode;
            private String mSynchronousMode;

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
                mIdleConnectionTimeout = source.mIdleConnectionTimeout;
                mJournalMode = source.mJournalMode;
                mSynchronousMode = source.mSynchronousMode;
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

            @Deprecated
            public Builder setIdleConnectionTimeout(long idleConnectionTimeoutMs)
            {
                mIdleConnectionTimeout = idleConnectionTimeoutMs;
                return this;
            }

            public Builder setJournalMode(String journalMode)
            {
                mJournalMode = journalMode;
                return this;
            }

            public Builder setSynchronousMode(String syncMode)
            {
                mSynchronousMode = syncMode;
                return this;
            }

            public OpenParams build()
            {
                return new OpenParams(mOpenFlags, mCursorFactory, mErrorHandler, mLookasideSlotSize, mLookasideSlotCount,
                    mIdleConnectionTimeout, mJournalMode, mSynchronousMode);
            }
        }
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory)
    {
        return openDatabase(path, factory, CREATE_IF_NECESSARY, null);
    }

    public static SQLiteDatabase openOrCreateDatabase(String path, CursorFactory factory, DatabaseErrorHandler errorHandler)
    {
        return openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler);
    }

    public static SQLiteDatabase openOrCreateDatabase(File path, CursorFactory factory)
    {
        return openDatabase(path.getPath(), factory, CREATE_IF_NECESSARY, null);
    }

    public static int releaseMemory()
    {
        return 0;
    }

    public static SQLiteDatabase create(CursorFactory factory)
    {
        return openDatabase(MEMORY_DB_PATH, factory, CREATE_IF_NECESSARY, null);
    }

    public static boolean deleteDatabase(File file)
    {
        if (file == null) throw new IllegalArgumentException("file must not be null");

        boolean deleted = file.delete();
        deleted |= new File(file.getPath() + "-journal").delete();
        deleted |= new File(file.getPath() + "-shm").delete();
        deleted |= new File(file.getPath() + "-wal").delete();

        File directory = file.getParentFile();
        if (directory != null)
        {
            final String prefix = file.getName() + "-mj";
            File[] files = directory.listFiles((candidate) -> candidate.getName().startsWith(prefix));
            if (files != null)
            {
                for (File masterJournal : files) deleted |= masterJournal.delete();
            }
        }

        return deleted;
    }

    public static String findEditTable(String tables)
    {
        if (tables == null || tables.isEmpty()) throw new IllegalStateException("Invalid tables");

        int spacePosition = tables.indexOf(' ');
        int commaPosition = tables.indexOf(',');

        if (spacePosition > 0 && (spacePosition < commaPosition || commaPosition < 0)) return tables.substring(0, spacePosition);
        if (commaPosition > 0 && (commaPosition < spacePosition || spacePosition < 0)) return tables.substring(0, commaPosition);

        return tables;
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
        return mReadOnly;
    }

    public boolean isInMemoryDatabase()
    {
        return MEMORY_DB_PATH.equalsIgnoreCase(mPath);
    }

    @Override
    protected void onAllReferencesReleased()
    {
        if (mHandle == 0) return;

        Sqlite.nativeClose(mHandle);
        mHandle = 0;
    }

    public void execSQL(String sql) throws SQLException
    {
        checkOpen();

        int outcome = Sqlite.nativeExec(mHandle, sql);
        if (outcome != 0) throw failure(sql);
    }

    public void execSQL(String sql, Object[] bindArgs) throws SQLException
    {
        if (bindArgs == null) throw new IllegalArgumentException("Empty bindArgs");

        if (bindArgs.length == 0)
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

    public void execPerConnectionSQL(String sql, Object[] bindArgs) throws SQLException
    {
        if (sql == null) throw new IllegalArgumentException("sql must not be null");

        if (bindArgs == null || bindArgs.length == 0) execSQL(sql);
        else execSQL(sql, bindArgs);
    }

    public SQLiteStatement compileStatement(String sql) throws SQLException
    {
        return new SQLiteStatement(this, prepare(sql), sql);
    }

    SQLiteQuery compileQuery(String sql, CancellationSignal cancellationSignal)
    {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();

        return new SQLiteQuery(this, sql, prepare(sql), cancellationSignal);
    }

    public void validateSql(String sql, CancellationSignal cancellationSignal)
    {
        if (cancellationSignal != null) cancellationSignal.throwIfCanceled();

        Sqlite.nativeFinalize(prepare(sql));
    }

    private long prepare(String sql)
    {
        checkOpen();

        long prepared = Sqlite.nativePrepare(mHandle, sql);
        if (prepared == 0) throw failure(sql);

        return prepared;
    }

    public Cursor rawQuery(String sql, String[] selectionArgs)
    {
        return rawQueryWithFactory(null, sql, selectionArgs, null, null);
    }

    public Cursor rawQuery(String sql, String[] selectionArgs, CancellationSignal cancellationSignal)
    {
        return rawQueryWithFactory(null, sql, selectionArgs, null, cancellationSignal);
    }

    public Cursor rawQueryWithFactory(CursorFactory factory, String sql, String[] selectionArgs, String editTable)
    {
        return rawQueryWithFactory(factory, sql, selectionArgs, editTable, null);
    }

    public Cursor rawQueryWithFactory(CursorFactory factory, String sql, String[] selectionArgs, String editTable,
                                      CancellationSignal cancellationSignal)
    {
        checkOpen();

        SQLiteCursorDriver driver = new SQLiteDirectCursorDriver(this, sql, editTable, cancellationSignal);
        return driver.query(factory != null ? factory : mCursorFactory, selectionArgs);
    }

    public Cursor queryWithFactory(CursorFactory factory, boolean distinct, String table, String[] columns, String selection,
                                   String[] selectionArgs, String groupBy, String having, String orderBy, String limit)
    {
        return queryWithFactory(factory, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public Cursor queryWithFactory(CursorFactory factory, boolean distinct, String table, String[] columns, String selection,
                                   String[] selectionArgs, String groupBy, String having, String orderBy, String limit,
                                   CancellationSignal cancellationSignal)
    {
        String sql = SQLiteQueryBuilder.buildQueryString(distinct, table, columns, selection, groupBy, having, orderBy, limit);

        return rawQueryWithFactory(factory, sql, selectionArgs, findEditTable(table), cancellationSignal);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit)
    {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public Cursor query(boolean distinct, String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit, CancellationSignal cancellationSignal)
    {
        return queryWithFactory(null, distinct, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit,
            cancellationSignal);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having,
                        String orderBy)
    {
        return queryWithFactory(null, false, table, columns, selection, selectionArgs, groupBy, having, orderBy, null, null);
    }

    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having,
                        String orderBy, String limit)
    {
        return queryWithFactory(null, false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, null);
    }

    public long insert(String table, String nullColumnHack, ContentValues values)
    {
        try
        {
            return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
        }
        catch (SQLException failure)
        {
            Log.e(TAG, "Error inserting " + values, failure);
            return -1;
        }
    }

    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) throws SQLException
    {
        return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_NONE);
    }

    public long replace(String table, String nullColumnHack, ContentValues values)
    {
        try
        {
            return insertWithOnConflict(table, nullColumnHack, values, CONFLICT_REPLACE);
        }
        catch (SQLException failure)
        {
            Log.e(TAG, "Error inserting " + values, failure);
            return -1;
        }
    }

    public long replaceOrThrow(String table, String nullColumnHack, ContentValues values) throws SQLException
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
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("Empty values");

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

        if (whereArgs != null)
        {
            for (String argument : whereArgs) arguments.add(argument);
        }

        if (whereClause != null && !whereClause.isEmpty()) sql.append(" WHERE ").append(whereClause);

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
            statement.bindAll(whereArgs);
            return statement.executeUpdateDelete();
        }
        finally
        {
            statement.close();
        }
    }

    public void beginTransaction()
    {
        beginTransaction(null, "BEGIN IMMEDIATE");
    }

    public void beginTransactionNonExclusive()
    {
        beginTransaction(null, "BEGIN IMMEDIATE");
    }

    public void beginTransactionReadOnly()
    {
        beginTransaction(null, "BEGIN DEFERRED");
    }

    public void beginTransactionWithListener(SQLiteTransactionListener transactionListener)
    {
        beginTransaction(transactionListener, "BEGIN IMMEDIATE");
    }

    public void beginTransactionWithListenerNonExclusive(SQLiteTransactionListener transactionListener)
    {
        beginTransaction(transactionListener, "BEGIN IMMEDIATE");
    }

    public void beginTransactionWithListenerReadOnly(SQLiteTransactionListener transactionListener)
    {
        beginTransaction(transactionListener, "BEGIN DEFERRED");
    }

    private void beginTransaction(SQLiteTransactionListener listener, String beginSql)
    {
        checkOpen();

        boolean outermost = mTransactions.isEmpty();
        if (outermost) execSQL(beginSql);

        try
        {
            if (listener != null) listener.onBegin();
        }
        catch (RuntimeException failure)
        {
            if (outermost) execSQL("ROLLBACK");
            throw failure;
        }

        mTransactions.push(new Transaction(listener));
    }

    public void setTransactionSuccessful()
    {
        Transaction transaction = mTransactions.peek();
        if (transaction == null) throw new IllegalStateException("Cannot perform this operation because there is no current transaction.");
        if (transaction.mMarkedSuccessful)
        {
            throw new IllegalStateException("Cannot perform this operation because the transaction has already been marked successful.");
        }

        transaction.mMarkedSuccessful = true;
    }

    public void endTransaction()
    {
        Transaction transaction = mTransactions.peek();
        if (transaction == null) throw new IllegalStateException("Cannot perform this operation because there is no current transaction.");

        boolean successful = transaction.mMarkedSuccessful && !transaction.mChildFailed;
        RuntimeException listenerFailure = null;

        if (transaction.mListener != null)
        {
            try
            {
                if (successful) transaction.mListener.onCommit();
                else transaction.mListener.onRollback();
            }
            catch (RuntimeException failure)
            {
                listenerFailure = failure;
                successful = false;
            }
        }

        mTransactions.pop();

        Transaction parent = mTransactions.peek();
        if (parent != null)
        {
            if (!successful) parent.mChildFailed = true;
        }
        else if (isOpen())
        {
            execSQL(successful ? "COMMIT" : "ROLLBACK");
        }

        if (listenerFailure != null) throw listenerFailure;
    }

    public boolean inTransaction()
    {
        return !mTransactions.isEmpty();
    }

    public boolean isDbLockedByCurrentThread()
    {
        return !mTransactions.isEmpty();
    }

    @Deprecated
    public boolean isDbLockedByOtherThreads()
    {
        return false;
    }

    @Deprecated
    public boolean yieldIfContended()
    {
        return false;
    }

    public boolean yieldIfContendedSafely()
    {
        return false;
    }

    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay)
    {
        return false;
    }

    public int getVersion()
    {
        return (int)longForQuery("PRAGMA user_version");
    }

    public void setVersion(int version)
    {
        execSQL("PRAGMA user_version = " + version);
    }

    public boolean needUpgrade(int newVersion)
    {
        return newVersion > getVersion();
    }

    public long getMaximumSize()
    {
        return longForQuery("PRAGMA max_page_count") * getPageSize();
    }

    public long setMaximumSize(long numBytes)
    {
        long pageSize = getPageSize();
        long numPages = numBytes / pageSize;
        if ((numBytes % pageSize) != 0) numPages++;

        long newPageCount = longForQuery("PRAGMA max_page_count = " + numPages);
        return newPageCount * pageSize;
    }

    public long getPageSize()
    {
        return longForQuery("PRAGMA page_size");
    }

    public void setPageSize(long numBytes)
    {
        execSQL("PRAGMA page_size = " + numBytes);
    }

    public long getLastInsertRowId()
    {
        checkOpen();
        return Sqlite.nativeLastInsertRowId(mHandle);
    }

    public long getLastChangedRowCount()
    {
        checkOpen();
        return Sqlite.nativeChanges(mHandle);
    }

    public long getTotalChangedRowCount()
    {
        return longForQuery("SELECT total_changes()");
    }

    @Deprecated
    public void setLockingEnabled(boolean lockingEnabled)
    {
    }

    @Deprecated
    public void markTableSyncable(String table, String deletedTable)
    {
    }

    @Deprecated
    public void markTableSyncable(String table, String foreignKey, String updateTable)
    {
    }

    @Deprecated
    public Map<String, String> getSyncedTables()
    {
        return new HashMap<>(0);
    }

    public void setForeignKeyConstraintsEnabled(boolean enable)
    {
        if (inTransaction()) throw new IllegalStateException("Foreign key constraints may not be changed while in a transaction");

        execSQL("PRAGMA foreign_keys=" + (enable ? "ON" : "OFF"));
    }

    public boolean enableWriteAheadLogging()
    {
        if (mReadOnly || isInMemoryDatabase()) return false;
        if (inTransaction()) throw new IllegalStateException("Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.");

        String mode = stringForQuery("PRAGMA journal_mode=WAL");
        mWriteAheadLogging = "wal".equalsIgnoreCase(mode);

        return mWriteAheadLogging;
    }

    public void disableWriteAheadLogging()
    {
        if (!mWriteAheadLogging) return;
        if (inTransaction()) throw new IllegalStateException("Write Ahead Logging (WAL) mode cannot be enabled or disabled while there are transactions in progress.");

        stringForQuery("PRAGMA journal_mode=DELETE");
        mWriteAheadLogging = false;
    }

    public boolean isWriteAheadLoggingEnabled()
    {
        return mWriteAheadLogging;
    }

    public void setLocale(java.util.Locale locale)
    {
        if (locale == null) throw new IllegalArgumentException("locale must not be null.");
    }

    public void setMaxSqlCacheSize(int cacheSize)
    {
        if (cacheSize > MAX_SQL_CACHE_SIZE || cacheSize < 0)
        {
            throw new IllegalStateException("expected value between 0 and " + MAX_SQL_CACHE_SIZE);
        }
    }

    public List<android.util.Pair<String, String>> getAttachedDbs()
    {
        if (!isOpen()) return null;

        List<android.util.Pair<String, String>> attached = new ArrayList<>();

        Cursor cursor = rawQuery("PRAGMA database_list", null);
        try
        {
            while (cursor.moveToNext()) attached.add(new android.util.Pair<>(cursor.getString(1), cursor.getString(2)));
        }
        finally
        {
            cursor.close();
        }

        if (attached.isEmpty()) attached.add(new android.util.Pair<>("main", mPath));

        return attached;
    }

    public boolean isDatabaseIntegrityOk()
    {
        return "ok".equalsIgnoreCase(stringForQuery("PRAGMA integrity_check(1)"));
    }

    @Override
    public String toString()
    {
        return "SQLiteDatabase: " + mPath;
    }

    long handle()
    {
        return mHandle;
    }

    String describeFailure(String sql)
    {
        String message = Sqlite.nativeErrorMessage(mHandle);
        return sql == null ? message : message + " (while running: " + sql + ")";
    }

    SQLiteException failure(String sql)
    {
        int code = mHandle == 0 ? 21 : Sqlite.nativeErrorCode(mHandle);
        String message = mHandle == 0 ? "the database is closed" : describeFailure(sql);
        message = message + " (code " + code + ")";

        switch (code & 0xff)
        {
            case 3: return new SQLiteAccessPermException(message);
            case 4: return new SQLiteAbortException(message);
            case 5: return new SQLiteDatabaseLockedException(message);
            case 6: return new SQLiteTableLockedException(message);
            case 8: return new SQLiteReadOnlyDatabaseException(message);
            case 10: return new SQLiteDiskIOException(message);
            case 11:
            case 26:
                return corruption(message);
            case 13: return new SQLiteFullException(message);
            case 14: return new SQLiteCantOpenDatabaseException(message);
            case 18: return new SQLiteBlobTooBigException(message);
            case 19: return new SQLiteConstraintException(message);
            case 101: return new SQLiteDoneException(message);
            default: return new SQLiteException(message);
        }
    }

    private SQLiteException corruption(String message)
    {
        SQLiteDatabaseCorruptException exception = new SQLiteDatabaseCorruptException(message);

        try
        {
            mErrorHandler.onCorruption(this);
        }
        catch (RuntimeException failure)
        {
            Log.w(TAG, "the corruption handler failed: " + failure.getMessage());
        }

        return exception;
    }

    private void checkOpen()
    {
        if (mHandle == 0) throw new IllegalStateException("attempt to re-open an already-closed object: " + this);
    }

    private long longForQuery(String sql)
    {
        SQLiteStatement statement = compileStatement(sql);
        try
        {
            return statement.simpleQueryForLong();
        }
        finally
        {
            statement.close();
        }
    }

    private String stringForQuery(String sql)
    {
        SQLiteStatement statement = compileStatement(sql);
        try
        {
            return statement.simpleQueryForString();
        }
        finally
        {
            statement.close();
        }
    }
}
