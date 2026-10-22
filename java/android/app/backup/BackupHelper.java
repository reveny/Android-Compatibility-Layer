package android.app.backup;

import android.os.ParcelFileDescriptor;

public interface BackupHelper
{
    void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState);

    void restoreEntity(BackupDataInputStream data);

    void writeNewStateDescription(ParcelFileDescriptor newState);
}
