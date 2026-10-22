package android.app;

import android.content.ComponentCallbacks2;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.os.IBinder;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class Service extends ContextWrapper implements ComponentCallbacks2
{
    public static final int START_CONTINUATION_MASK = 0xf;
    public static final int START_STICKY_COMPATIBILITY = 0;
    public static final int START_STICKY = 1;
    public static final int START_NOT_STICKY = 2;
    public static final int START_REDELIVER_INTENT = 3;

    public static final int START_FLAG_REDELIVERY = 0x0001;
    public static final int START_FLAG_RETRY = 0x0002;

    public static final int STOP_FOREGROUND_LEGACY = 0;
    public static final int STOP_FOREGROUND_REMOVE = 1 << 0;
    public static final int STOP_FOREGROUND_DETACH = 1 << 1;

    private Application mApplication;
    private int mForegroundId;
    private Notification mForegroundNotification;
    private int mForegroundServiceType;

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

    public void onConfigurationChanged(Configuration newConfiguration)
    {
    }

    public void onTimeout(int startId)
    {
    }

    public void onTimeout(int startId, int foregroundServiceType)
    {
    }

    protected void dump(FileDescriptor descriptor, PrintWriter writer, String[] arguments)
    {
        writer.println("nothing to dump.");
    }

    public final void startForeground(int id, Notification notification)
    {
        startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
    }

    public final void startForeground(int id, Notification notification, int foregroundServiceType)
    {
        if (notification == null) throw new IllegalArgumentException("null notification");

        if (mForegroundId != 0 && mForegroundId != id) runtime.NotificationStore.cancel(null, mForegroundId);

        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;

        mForegroundId = id;
        mForegroundNotification = notification;
        mForegroundServiceType = foregroundServiceType == ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST ? 0
                                                                                                         : foregroundServiceType;

        runtime.Services.startForeground(this, id, notification);
    }

    @Deprecated
    public final void stopForeground(boolean removeNotification)
    {
        stopForeground(removeNotification ? STOP_FOREGROUND_REMOVE : STOP_FOREGROUND_LEGACY);
    }

    public final void stopForeground(int notificationBehavior)
    {
        int id = mForegroundId;
        Notification notification = mForegroundNotification;

        mForegroundId = 0;
        mForegroundNotification = null;
        mForegroundServiceType = 0;

        runtime.Services.stopForeground(this, (notificationBehavior & STOP_FOREGROUND_REMOVE) != 0);

        if (id == 0) return;

        if ((notificationBehavior & STOP_FOREGROUND_REMOVE) != 0)
        {
            runtime.NotificationStore.cancel(null, id);
            return;
        }

        if (notification != null)
        {
            notification.flags &= ~(Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR);
            if ((notificationBehavior & STOP_FOREGROUND_DETACH) == 0 && runtime.NotificationStore.find(null, id) != null)
            {
                runtime.NotificationStore.post(null, id, notification);
            }
        }
    }

    public final int getForegroundServiceType()
    {
        return mForegroundServiceType;
    }
}
