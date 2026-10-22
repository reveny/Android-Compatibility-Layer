package android.app.job;

import android.app.Notification;
import android.app.Service;
import android.os.Binder;
import android.os.IBinder;

public abstract class JobServiceEngine
{
    private static final String DESCRIPTOR = "android.app.job.IJobService";

    private final Service mService;
    private final EngineBinder mBinder;

    public JobServiceEngine(Service service)
    {
        mService = service;
        mBinder = new EngineBinder(this);
    }

    public final IBinder getBinder()
    {
        return mBinder;
    }

    public static JobServiceEngine fromBinder(IBinder binder)
    {
        return binder instanceof EngineBinder ? ((EngineBinder)binder).mEngine : null;
    }

    public Service getService()
    {
        return mService;
    }

    public abstract boolean onStartJob(JobParameters params);

    public abstract boolean onStopJob(JobParameters params);

    public void onNetworkChanged(JobParameters params)
    {
    }

    public void jobFinished(JobParameters params, boolean wantsReschedule)
    {
        if (params == null) throw new NullPointerException("params");

        runtime.Jobs.finished(params, wantsReschedule);
    }

    public void setNotification(JobParameters params, int notificationId, Notification notification, int jobEndNotificationPolicy)
    {
        if (params == null) throw new NullPointerException("params");
        if (notification == null) throw new NullPointerException("notification");

        runtime.Jobs.setNotification(params, notificationId, notification, jobEndNotificationPolicy);
    }

    public void updateEstimatedNetworkBytes(JobParameters params, JobWorkItem item, long downloadBytes, long uploadBytes)
    {
        if (params == null) throw new NullPointerException("params");
    }

    public void updateTransferredNetworkBytes(JobParameters params, JobWorkItem item, long downloadBytes, long uploadBytes)
    {
        if (params == null) throw new NullPointerException("params");
    }

    private static final class EngineBinder extends Binder
    {
        final JobServiceEngine mEngine;

        EngineBinder(JobServiceEngine engine)
        {
            super(DESCRIPTOR);
            mEngine = engine;
        }
    }
}
