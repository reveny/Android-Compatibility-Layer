package android.app.job;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public abstract class JobService extends Service
{
    public static final String PERMISSION_BIND = "android.permission.BIND_JOB_SERVICE";

    public static final int JOB_END_NOTIFICATION_POLICY_DETACH = 0;
    public static final int JOB_END_NOTIFICATION_POLICY_REMOVE = 1;

    private JobServiceEngine mEngine;

    public JobService()
    {
    }

    @Override
    public final IBinder onBind(Intent intent)
    {
        if (mEngine == null)
        {
            mEngine = new JobServiceEngine(this)
            {
                @Override
                public boolean onStartJob(JobParameters params)
                {
                    return JobService.this.onStartJob(params);
                }

                @Override
                public boolean onStopJob(JobParameters params)
                {
                    return JobService.this.onStopJob(params);
                }

                @Override
                public void onNetworkChanged(JobParameters params)
                {
                    JobService.this.onNetworkChanged(params);
                }
            };
        }

        return mEngine.getBinder();
    }

    public final void jobFinished(JobParameters params, boolean wantsReschedule)
    {
        runtime.Jobs.finished(params, wantsReschedule);
    }

    public abstract boolean onStartJob(JobParameters params);

    public abstract boolean onStopJob(JobParameters params);

    public void onNetworkChanged(JobParameters params)
    {
    }

    public final void updateEstimatedNetworkBytes(JobParameters params, long downloadBytes, long uploadBytes)
    {
    }

    public final void updateEstimatedNetworkBytes(JobParameters params, JobWorkItem jobWorkItem, long downloadBytes, long uploadBytes)
    {
    }

    public final void updateTransferredNetworkBytes(JobParameters params, long transferredDownloadBytes, long transferredUploadBytes)
    {
    }

    public final void updateTransferredNetworkBytes(JobParameters params, JobWorkItem item, long transferredDownloadBytes,
                                                    long transferredUploadBytes)
    {
    }

    public final void setNotification(JobParameters params, int notificationId, Notification notification, int jobEndNotificationPolicy)
    {
        if (params == null) throw new NullPointerException("params");
        if (notification == null) throw new NullPointerException("notification");

        runtime.Jobs.setNotification(params, notificationId, notification, jobEndNotificationPolicy);
    }
}
