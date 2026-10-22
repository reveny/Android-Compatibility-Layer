package android.app.backup;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class BackupAgentHelper extends BackupAgent
{
    private static final String TAG = "BackupAgentHelper";

    private final Map<String, BackupHelper> mHelpers = new TreeMap<>();

    public BackupAgentHelper()
    {
        super();
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException
    {
        for (Map.Entry<String, BackupHelper> entry : mHelpers.entrySet())
        {
            data.setKeyPrefix(entry.getKey());
            try
            {
                entry.getValue().performBackup(oldState, data, newState);
            }
            finally
            {
                data.setKeyPrefix(null);
            }
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException
    {
        BackupDataInputStream stream = new BackupDataInputStream(data);

        while (data.readNextHeader())
        {
            String key = data.getKey();
            int separator = key.indexOf(':');
            BackupHelper helper = separator < 0 ? null : mHelpers.get(key.substring(0, separator));

            if (helper == null)
            {
                Log.w(TAG, "no helper restores the backup entity " + key);
                data.skipEntityData();
                continue;
            }

            stream.key = key.substring(separator + 1);
            stream.dataSize = data.getDataSize();
            helper.restoreEntity(stream);
            data.skipEntityData();
        }

        for (BackupHelper helper : mHelpers.values()) helper.writeNewStateDescription(newState);
    }

    public void addHelper(String keyPrefix, BackupHelper helper)
    {
        if (keyPrefix == null || helper == null) throw new NullPointerException();

        mHelpers.put(keyPrefix, helper);
    }
}
