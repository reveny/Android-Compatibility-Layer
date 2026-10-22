package android.app.backup;

import android.content.Context;
import android.os.UserHandle;
import android.util.Log;

public class BackupManager
{
    private static final String TAG = "BackupManager";

    private static boolean sSaidNoTransport;

    private final Context mContext;

    public BackupManager(Context context)
    {
        mContext = context;
    }

    public void dataChanged()
    {
        dataChanged(mContext == null ? null : mContext.getPackageName());
    }

    public static void dataChanged(String packageName)
    {
        synchronized (BackupManager.class)
        {
            if (sSaidNoTransport) return;
            sSaidNoTransport = true;
        }

        Log.i(TAG, "backup data of " + packageName + " changed; this device has no backup transport, so no backup is scheduled");
    }

    public int requestRestore(RestoreObserver observer)
    {
        return -1;
    }

    public UserHandle getUserForAncestralSerialNumber(long ancestralSerialNumber)
    {
        return null;
    }
}
