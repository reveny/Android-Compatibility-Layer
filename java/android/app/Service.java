package android.app;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;

public abstract class Service extends ContextWrapper
{
    public static final int START_STICKY_COMPATIBILITY = 0;
    public static final int START_STICKY = 1;
    public static final int START_NOT_STICKY = 2;
    public static final int START_REDELIVER_INTENT = 3;

    public static final int START_FLAG_REDELIVERY = 0x0001;
    public static final int START_FLAG_RETRY = 0x0002;

    private Application mApplication;

    public Service()
    {
        super(null);
    }

    public final void attach(android.content.Context context, Application application)
    {
        attachBaseContext(context);
        mApplication = application;
    }

    public final Application getApplication()
    {
        return mApplication;
    }

    public void onCreate()
    {
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        onStart(intent, startId);
        return START_STICKY;
    }

    public void onStart(Intent intent, int startId)
    {
    }

    public abstract IBinder onBind(Intent intent);

    public boolean onUnbind(Intent intent)
    {
        return false;
    }

    public void onRebind(Intent intent)
    {
    }

    public void onTaskRemoved(Intent rootIntent)
    {
    }

    public void onLowMemory()
    {
    }

    public void onTrimMemory(int level)
    {
    }

    public void onDestroy()
    {
    }

    public final void stopSelf()
    {
        runtime.Services.stopSelf(this, -1);
    }

    public final void stopSelf(int startId)
    {
        runtime.Services.stopSelf(this, startId);
    }

    public final boolean stopSelfResult(int startId)
    {
        return runtime.Services.stopSelf(this, startId);
    }

    public final void startForeground(int id, Notification notification)
    {
        runtime.Services.startForeground(this, id, notification);
    }

    public final void stopForeground(boolean removeNotification)
    {
        runtime.Services.stopForeground(this, removeNotification);
    }
}
