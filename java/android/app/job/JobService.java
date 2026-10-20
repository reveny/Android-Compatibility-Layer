package android.app.job;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public abstract class JobService extends Service
{
    public static final String PERMISSION_BIND = "android.permission.BIND_JOB_SERVICE";

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public abstract boolean onStartJob(JobParameters params);

    public abstract boolean onStopJob(JobParameters params);

    public final void jobFinished(JobParameters params, boolean wantsReschedule)
    {
        runtime.Jobs.finished(this, params, wantsReschedule);
    }
}
