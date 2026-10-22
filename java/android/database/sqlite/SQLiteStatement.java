package android.database.sqlite;

import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import runtime.Sqlite;

public final class SQLiteStatement extends SQLiteProgram
{
    private final String mSql;

    SQLiteStatement(SQLiteDatabase database, long statement)
    {
        this(database, statement, null);
    }

    SQLiteStatement(SQLiteDatabase database, long statement, String sql)
    {
        super(database, statement);
        mSql = sql;
    }

    public void execute()
    {
        step();
    }

    public long executeInsert()
    {
        step();
        return Sqlite.nativeChanges(mDatabase.handle()) > 0 ? Sqlite.nativeLastInsertRowId(mDatabase.handle()) : -1;
    }

    public int executeUpdateDelete()
    {
        step();
        return Sqlite.nativeChanges(mDatabase.handle());
    }

    public long simpleQueryForLong()
    {
        stepForRow();

        long value = Sqlite.nativeColumnLong(mStatement, 0);
        Sqlite.nativeReset(mStatement);

        return value;
    }

    public String simpleQueryForString()
    {
        stepForRow();

        String value = Sqlite.nativeColumnType(mStatement, 0) == Sqlite.TYPE_NULL ? null : Sqlite.nativeColumnString(mStatement, 0);
        Sqlite.nativeReset(mStatement);

        return value;
    }

    public ParcelFileDescriptor simpleQueryForBlobFileDescriptor()
    {
        stepForRow();

        byte[] value = Sqlite.nativeColumnType(mStatement, 0) == Sqlite.TYPE_NULL ? null : Sqlite.nativeColumnBlob(mStatement, 0);
        Sqlite.nativeReset(mStatement);

        if (value == null) return null;

        try
        {
            File file = File.createTempFile("sqlite-blob", null);
            try (FileOutputStream output = new FileOutputStream(file))
            {
                output.write(value);
            }

            ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            file.delete();

            return descriptor;
        }
        catch (IOException failure)
        {
            throw new SQLiteException("unable to expose the blob as a file descriptor", failure);
        }
    }

    @Override
    public String toString()
    {
        return "SQLiteProgram: " + mSql;
    }

    private void stepForRow()
    {
        rebind();

        int outcome = Sqlite.nativeStep(mStatement);
        if (outcome == Sqlite.STEP_ERROR)
        {
            SQLiteException failure = mDatabase.failure(null);
            Sqlite.nativeReset(mStatement);
            throw failure;
        }

        if (outcome != Sqlite.STEP_ROW)
        {
            Sqlite.nativeReset(mStatement);
            throw new SQLiteDoneException("the statement returned no rows");
        }
    }

    private void step()
    {
        rebind();

        int outcome = Sqlite.nativeStep(mStatement);
        if (outcome == Sqlite.STEP_ERROR)
        {
            SQLiteException failure = mDatabase.failure(null);
            Sqlite.nativeReset(mStatement);
            throw failure;
        }

        Sqlite.nativeReset(mStatement);
    }
}
