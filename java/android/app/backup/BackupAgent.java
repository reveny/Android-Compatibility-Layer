package android.app.backup;

import android.content.ContextWrapper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public abstract class BackupAgent extends ContextWrapper
{
    private static final String TAG = "BackupAgent";

    public static final int TYPE_FILE = 1;
    public static final int TYPE_DIRECTORY = 2;

    public static final int FLAG_CLIENT_SIDE_ENCRYPTION_ENABLED = 1;
    public static final int FLAG_DEVICE_TO_DEVICE_TRANSFER = 2;

    public BackupAgent()
    {
        super(null);
    }

    public void onCreate()
    {
    }

    public void onDestroy()
    {
    }

    public abstract void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState)
            throws IOException;

    public abstract void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException;

    public void onRestore(BackupDataInput data, long appVersionCode, ParcelFileDescriptor newState) throws IOException
    {
        onRestore(data, (int)appVersionCode, newState);
    }

    public void onFullBackup(FullBackupDataOutput data) throws IOException
    {
    }

    public void onQuotaExceeded(long backupDataBytes, long quotaBytes)
    {
    }

    public final void fullBackupFile(File file, FullBackupDataOutput output)
    {
        Log.i(TAG, "no backup transport exists on this device, so " + file + " is not written to a full backup");
    }

    public void onRestoreFile(ParcelFileDescriptor data, long size, File destination, int type, long mode, long mtime)
            throws IOException
    {
    }

    public void onRestoreFinished()
    {
    }
}
