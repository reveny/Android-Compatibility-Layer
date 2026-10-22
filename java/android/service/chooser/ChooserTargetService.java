package android.service.chooser;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

import java.util.List;

public abstract class ChooserTargetService extends Service
{
    public static final String META_DATA_NAME = "android.service.chooser.chooser_target_service";
    public static final String BIND_PERMISSION = "android.permission.BIND_CHOOSER_TARGET_SERVICE";
    public static final String SERVICE_INTERFACE = "android.service.chooser.ChooserTargetService";

    private IBinder mBinder;

    public ChooserTargetService()
    {
    }

    public abstract List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName, IntentFilter matchedFilter);

    @Override
    public IBinder onBind(Intent intent)
    {
        if (!SERVICE_INTERFACE.equals(intent == null ? null : intent.getAction())) return null;

        if (mBinder == null) mBinder = new Binder("android.service.chooser.IChooserTargetService");
        return mBinder;
    }
}
