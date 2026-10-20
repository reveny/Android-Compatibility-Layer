package android.app;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class AlarmManager
{
    public static final int RTC_WAKEUP = 0;
    public static final int RTC = 1;
    public static final int ELAPSED_REALTIME_WAKEUP = 2;
    public static final int ELAPSED_REALTIME = 3;

    private static final String TAG = "AlarmManager";

    private static final Map<PendingIntent, Runnable> sScheduled = new LinkedHashMap<>();

    private Handler handler()
    {
        Looper main = Looper.getMainLooper();
        return main == null ? null : new Handler(main);
    }

    public void set(int type, long triggerAtMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, 0, operation);
    }

    public void setExact(int type, long triggerAtMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, 0, operation);
    }

    public void setExactAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, 0, operation);
    }

    public void setAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, 0, operation);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation)
    {
        schedule(type, windowStartMillis, 0, operation);
    }

    public void setRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, intervalMillis, operation);
    }

    public void setInexactRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, intervalMillis, operation);
    }

    public void cancel(PendingIntent operation)
    {
        if (operation == null) return;

        Runnable pending;
        synchronized (sScheduled)
        {
            pending = sScheduled.remove(operation);
        }

        if (pending == null) return;

        Handler handler = handler();
        if (handler != null) handler.removeCallbacks(pending);

        Log.i(TAG, "cancelled the alarm for " + operation);
    }

    public boolean canScheduleExactAlarms()
    {
        return true;
    }

    private void schedule(final int type, long triggerAtMillis, final long intervalMillis, final PendingIntent operation)
    {
        if (operation == null) return;

        final Handler handler = handler();
        if (handler == null)
        {
            Log.w(TAG, "no main looper, so the alarm for " + operation + " cannot be scheduled");
            return;
        }

        long delay = delayFor(type, triggerAtMillis);

        Runnable fire = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i(TAG, "the alarm for " + operation + " fired");

                boolean repeating = intervalMillis > 0;
                if (!repeating)
                {
                    synchronized (sScheduled)
                    {
                        sScheduled.remove(operation);
                    }
                }

                operation.send();

                if (repeating) handler.postDelayed(this, intervalMillis);
            }
        };

        synchronized (sScheduled)
        {
            Runnable previous = sScheduled.put(operation, fire);
            if (previous != null) handler.removeCallbacks(previous);
        }

        handler.postDelayed(fire, delay);
        Log.i(TAG, "an alarm for " + operation + " will fire in " + delay + "ms"
                       + (intervalMillis > 0 ? ", then every " + intervalMillis + "ms" : ""));
    }

    private long delayFor(int type, long triggerAtMillis)
    {
        long now = type == RTC || type == RTC_WAKEUP ? System.currentTimeMillis() : SystemClock.elapsedRealtime();
        long delay = triggerAtMillis - now;

        return delay < 0 ? 0 : delay;
    }
}
