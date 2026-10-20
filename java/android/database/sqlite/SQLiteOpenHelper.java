package android.database.sqlite;

import android.content.Context;
import android.util.Log;

public abstract class SQLiteOpenHelper
{
    private static final String TAG = "SQLiteOpenHelper";

    private final Context mContext;
    private final String mName;
    private final SQLiteDatabase.CursorFactory mFactory;
    private final int mVersion;

    private SQLiteDatabase mDatabase;
    private boolean mOpening;

    public SQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version)
    {
        mContext = context;
        mName = name;
        mFactory = factory;
        mVersion = version;
    }

    public SQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version,
                            android.database.DatabaseErrorHandler errorHandler)
    {
        this(context, name, factory, version);
    }

    public String getDatabaseName()
    {
        return mName;
    }

    private SQLiteDatabase.OpenParams mOpenParams;

    public void setOpenParams(SQLiteDatabase.OpenParams openParams)
    {
        mOpenParams = openParams;
    }

    public SQLiteDatabase.OpenParams getOpenParams()
    {
        return mOpenParams;
    }

    public void setLookasideConfig(int slotSize, int slotCount)
    {
    }

    public void setWriteAheadLoggingEnabled(boolean enabled)
    {
    }

    public synchronized SQLiteDatabase getWritableDatabase()
    {
        if (mDatabase != null && mDatabase.isOpen()) return mDatabase;
        if (mOpening) throw new IllegalStateException("getWritableDatabase called recursively");

        mOpening = true;
        try
        {
            mDatabase = open();
            return mDatabase;
        }
        finally
        {
            mOpening = false;
        }
    }

    public synchronized SQLiteDatabase getReadableDatabase()
    {
        return getWritableDatabase();
    }

    public synchronized void close()
    {
        if (mDatabase == null) return;

        mDatabase.close();
        mDatabase = null;
    }

    public void onConfigure(SQLiteDatabase database)
    {
    }

    public abstract void onCreate(SQLiteDatabase database);

    public abstract void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion);

    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion)
    {
        throw new SQLiteException("cannot downgrade " + mName + " from " + oldVersion + " to " + newVersion);
    }

    public void onOpen(SQLiteDatabase database)
    {
    }

    private SQLiteDatabase open()
    {
        String path = mName == null ? ":memory:" : databasePath();

        SQLiteDatabase database = SQLiteDatabase.openDatabase(path, mFactory, SQLiteDatabase.CREATE_IF_NECESSARY);

        onConfigure(database);

        int version = database.getVersion();

        if (version != mVersion)
        {
            database.beginTransaction();
            try
            {
                if (version == 0)
                {
                    Log.i(TAG, "creating " + mName + " at version " + mVersion);
                    onCreate(database);
                }
                else if (version > mVersion)
                {
                    onDowngrade(database, version, mVersion);
                }
                else
                {
                    Log.i(TAG, "upgrading " + mName + " from " + version + " to " + mVersion);
                    onUpgrade(database, version, mVersion);
                }

                database.setVersion(mVersion);
                database.setTransactionSuccessful();
            }
            finally
            {
                database.endTransaction();
            }
        }

        onOpen(database);
        return database;
    }

    private String databasePath()
    {
        java.io.File file = mContext == null ? new java.io.File(mName) : mContext.getDatabasePath(mName);
        java.io.File parent = file.getParentFile();

        if (parent != null && !parent.exists()) parent.mkdirs();

        return file.getPath();
    }
}
