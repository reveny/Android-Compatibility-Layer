package android.app.backup;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import java.io.File;

public class SharedPreferencesBackupHelper implements BackupHelper
{
    private final Context mContext;
    private final String[] mPreferences;

    public SharedPreferencesBackupHelper(Context context, String... prefGroups)
    {
        mContext = context;
        mPreferences = prefGroups == null ? new String[0] : prefGroups.clone();
    }

    private File fileFor(String name)
    {
        return new File(new File(mContext.getDataDir(), "shared_prefs"), name + ".xml");
    }

    private File[] files()
    {
        File[] files = new File[mPreferences.length];
        for (int index = 0; index < mPreferences.length; index++) files[index] = fileFor(mPreferences[index]);
        return files;
    }

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState)
    {
        FileBackups.performBackup(oldState, data, newState, files(), mPreferences);
    }

    @Override
    public void restoreEntity(BackupDataInputStream data)
    {
        String key = data.getKey();
        for (String name : mPreferences)
        {
            if (name.equals(key))
            {
                FileBackups.restoreEntity(data, fileFor(name));
                return;
            }
        }
    }

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState)
    {
        FileBackups.writeNewStateDescription(newState, files(), mPreferences);
    }
}
