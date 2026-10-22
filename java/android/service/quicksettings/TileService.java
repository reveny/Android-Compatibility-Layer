package android.service.quicksettings;

import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class TileService extends Service
{
    public static final String ACTION_QS_TILE = "android.service.quicksettings.action.QS_TILE";
    public static final String ACTION_QS_TILE_PREFERENCES = "android.service.quicksettings.action.QS_TILE_PREFERENCES";
    public static final String META_DATA_ACTIVE_TILE = "android.service.quicksettings.ACTIVE_TILE";
    public static final String META_DATA_TOGGLEABLE_TILE = "android.service.quicksettings.TOGGLEABLE_TILE";

    private static final String TAG = "TileService";

    private final Tile mTile = new Tile();
    private IBinder mBinder;

    public TileService()
    {
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    public void onTileAdded()
    {
    }

    public void onTileRemoved()
    {
    }

    public void onStartListening()
    {
    }

    public void onStopListening()
    {
    }

    public void onClick()
    {
    }

    public final void showDialog(Dialog dialog)
    {
        if (dialog != null) dialog.show();
    }

    public final void unlockAndRun(Runnable runnable)
    {
        if (runnable != null) runnable.run();
    }

    public final boolean isSecure()
    {
        return false;
    }

    public final boolean isLocked()
    {
        return false;
    }

    public final void startActivityAndCollapse(Intent intent)
    {
        if (intent == null) return;

        Intent launched = new Intent(intent);
        launched.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launched);
    }

    public final void startActivityAndCollapse(PendingIntent pendingIntent)
    {
        if (pendingIntent == null) throw new NullPointerException("pendingIntent");

        try
        {
            pendingIntent.send();
        }
        catch (Exception cause)
        {
            Log.w(TAG, "the pending intent for the tile was cancelled", cause);
        }
    }

    public final Tile getQsTile()
    {
        return mTile;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        if (mBinder == null) mBinder = new Binder("android.service.quicksettings.ITileService");
        return mBinder;
    }

    public static final void requestListeningState(Context context, ComponentName component)
    {
        Log.i(TAG, "no quick settings panel exists here, so " + component + " is never asked to listen");
    }
}
