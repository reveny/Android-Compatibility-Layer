package android.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.util.List;

public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler
{
    private static final String TAG = "DefaultDatabaseErrorHandler";

    public DefaultDatabaseErrorHandler()
    {
    }

    @Override
    public void onCorruption(SQLiteDatabase database)
    {
        Log.e(TAG, "Corruption reported by sqlite on database: " + database.getPath());

        if (!database.isOpen())
        {
            deleteDatabaseFile(database.getPath());
            return;
        }

        List<Pair<String, String>> attachedDbs = null;
        try
        {
            try
            {
                attachedDbs = database.getAttachedDbs();
            }
            catch (RuntimeException ignored)
            {
            }

            try
            {
                database.close();
            }
            catch (RuntimeException ignored)
            {
            }
        }
        finally
        {
            if (attachedDbs != null)
            {
                for (Pair<String, String> attached : attachedDbs) deleteDatabaseFile(attached.second);
            }
            else
            {
                deleteDatabaseFile(database.getPath());
            }
        }
    }

    private void deleteDatabaseFile(String fileName)
    {
        if (fileName == null || ":memory:".equalsIgnoreCase(fileName) || fileName.trim().isEmpty()) return;

        Log.e(TAG, "deleting the database file: " + fileName);

        try
        {
            SQLiteDatabase.deleteDatabase(new File(fileName));
        }
        catch (RuntimeException failure)
        {
            Log.w(TAG, "delete failed: " + failure.getMessage());
        }
    }
}
