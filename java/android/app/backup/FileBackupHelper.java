package android.app.backup;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import java.io.File;

public class FileBackupHelper implements BackupHelper
{
    private final Context mContext;
    private final String[] mFiles;

    public FileBackupHelper(Context context, String... files)
    {
        mContext = context;
        mFiles = files == null ? new String[0] : files.clone();
    }

    private File[] files()
    {
        File directory = mContext.getFilesDir();
        File[] files = new File[mFiles.length];
        for (int index = 0; index < mFiles.length; index++) files[index] = new File(directory, mFiles[index]);
        return files;
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState)
    {
        FileBackups.performBackup(oldState, data, newState, files(), mFiles);
    }

    @Override
    public void restoreEntity(BackupDataInputStream data)
    {
        String key = data.getKey();
        for (String name : mFiles)
        {
            if (name.equals(key))
            {
                FileBackups.restoreEntity(data, new File(mContext.getFilesDir(), name));
                return;
            }
        }
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState)
    {
        FileBackups.writeNewStateDescription(newState, files(), mFiles);
    }
}
