package android.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class AlarmManager
{
    public static final int RTC_WAKEUP = 0;
    public static final int RTC = 1;
    public static final int ELAPSED_REALTIME_WAKEUP = 2;
    public static final int ELAPSED_REALTIME = 3;

    public static final long INTERVAL_FIFTEEN_MINUTES = 15 * 60 * 1000L;
    public static final long INTERVAL_HALF_HOUR = 2 * INTERVAL_FIFTEEN_MINUTES;
    public static final long INTERVAL_HOUR = 2 * INTERVAL_HALF_HOUR;
    public static final long INTERVAL_HALF_DAY = 12 * INTERVAL_HOUR;
    public static final long INTERVAL_DAY = 2 * INTERVAL_HALF_DAY;

    public static final String ACTION_NEXT_ALARM_CLOCK_CHANGED = "android.app.action.NEXT_ALARM_CLOCK_CHANGED";
    public static final String ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED";

    private static final String TAG = "AlarmManager";

    public interface OnAlarmListener
    {
        void onAlarm();
    }

    public static final class AlarmClockInfo implements Parcelable
    {
        public static final Creator<AlarmClockInfo> CREATOR = new Creator<AlarmClockInfo>()
        {
            @Override
            public AlarmClockInfo createFromParcel(Parcel parcel)
            {
                long triggerTime = parcel.readLong();
                return new AlarmClockInfo(triggerTime, PendingIntent.readPendingIntentOrNullFromParcel(parcel));
            }

            @Override
            public AlarmClockInfo[] newArray(int size)
            {
                return new AlarmClockInfo[size];
            }
        };

        private final long mTriggerTime;
        private final PendingIntent mShowIntent;

        public AlarmClockInfo(long triggerTime, PendingIntent showIntent)
        {
            mTriggerTime = triggerTime;
            mShowIntent = showIntent;
        }

        public long getTriggerTime()
        {
            return mTriggerTime;
        }

        public PendingIntent getShowIntent()
        {
            return mShowIntent;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags)
        {
            destination.writeLong(mTriggerTime);
            PendingIntent.writePendingIntentOrNullToParcel(mShowIntent, destination);
        }

        @Override
        public String toString()
        {
            return "AlarmClockInfo{triggerTime=" + mTriggerTime + ", showIntent=" + mShowIntent + "}";
        }
    }

    private static final class Alarm
    {
        Runnable fire;
        Handler handler;
        AlarmClockInfo clock;
    }

    private static final Map<Object, Alarm> sScheduled = new LinkedHashMap<>();

    AlarmManager()
    {
    }

    private static Handler mainHandler()
    {
        Looper main = Looper.getMainLooper();
        return main == null ? null : new Handler(main);
    }

    public void set(int type, long triggerAtMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, 0, operation, null, null);
    }

    public void set(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler)
    {
        schedule(type, triggerAtMillis, 0, listener, targetHandler, null);
    }

    public void setExact(int type, long triggerAtMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, 0, operation, null, null);
    }

    public void setExact(int type, long triggerAtMillis, String tag, OnAlarmListener listener, Handler targetHandler)
    {
        schedule(type, triggerAtMillis, 0, listener, targetHandler, null);
    }

    public void setExactAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, 0, operation, null, null);
    }

    public void setAndAllowWhileIdle(int type, long triggerAtMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, 0, operation, null, null);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, PendingIntent operation)
    {
        schedule(type, windowStartMillis, 0, operation, null, null);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, OnAlarmListener listener,
                          Handler targetHandler)
    {
        schedule(type, windowStartMillis, 0, listener, targetHandler, null);
    }

    public void setWindow(int type, long windowStartMillis, long windowLengthMillis, String tag, final Executor executor,
                          final OnAlarmListener listener)
    {
        if (executor == null) throw new NullPointerException("executor");

        schedule(type, windowStartMillis, 0, listener, new Handler(mainLooper()), executor);
    }

    public void setRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, Math.max(intervalMillis, 60000L), operation, null, null);
    }

    public void setInexactRepeating(int type, long triggerAtMillis, long intervalMillis, PendingIntent operation)
    {
        schedule(type, triggerAtMillis, Math.max(intervalMillis, 60000L), operation, null, null);
    }

    public void setAlarmClock(AlarmClockInfo info, PendingIntent operation)
    {
        if (info == null) throw new NullPointerException("info");

        Alarm alarm = schedule(RTC_WAKEUP, info.getTriggerTime(), 0, operation, null, null);
        if (alarm != null) alarm.clock = info;
    }

    public AlarmClockInfo getNextAlarmClock()
    {
        AlarmClockInfo next = null;

        synchronized (sScheduled)
        {
            for (Alarm alarm : sScheduled.values())
            {
                if (alarm.clock == null) continue;
                if (next == null || alarm.clock.getTriggerTime() < next.getTriggerTime()) next = alarm.clock;
            }
        }

        return next;
    }

    public void cancel(PendingIntent operation)
    {
        if (operation == null) throw new NullPointerException("cancel() called with a null PendingIntent");

        remove(operation);
    }

    public void cancel(OnAlarmListener listener)
    {
        if (listener == null) throw new NullPointerException("cancel() called with a null OnAlarmListener");

        remove(listener);
    }

    public void cancelAll()
    {
        List<Object> keys;
        synchronized (sScheduled)
        {
            keys = new ArrayList<>(sScheduled.keySet());
        }

        for (Object key : keys) remove(key);
    }

    public boolean canScheduleExactAlarms()
    {
        return true;
    }

    public void setTime(long millis)
    {
        throw new SecurityException("setTime requires android.permission.SET_TIME, which this application does not hold");
    }

    public void setTimeZone(String timeZone)
    {
        throw new SecurityException("setTimeZone requires android.permission.SET_TIME_ZONE, which this application does not hold");
    }

    private static Looper mainLooper()
    {
        Looper main = Looper.getMainLooper();
        return main == null ? Looper.myLooper() : main;
    }

    private void remove(Object key)
    {
        Alarm alarm;
        synchronized (sScheduled)
        {
            alarm = sScheduled.remove(key);
        }

        if (alarm == null) return;

        alarm.handler.removeCallbacks(alarm.fire);
        Log.i(TAG, "cancelled the alarm for " + key);
    }

    private Alarm schedule(int type, long triggerAtMillis, final long intervalMillis, final Object target, Handler targetHandler,
                           final Executor executor)
    {
        if (target == null) throw new NullPointerException("an alarm needs a PendingIntent or an OnAlarmListener");

        final Handler handler = targetHandler != null ? targetHandler : mainHandler();
        if (handler == null)
        {
            Log.w(TAG, "no main looper, so the alarm for " + target + " cannot be scheduled");
            return null;
        }

        long delay = delayFor(type, triggerAtMillis);
        final Alarm alarm = new Alarm();
        alarm.handler = handler;

        alarm.fire = new Runnable()
        {
            @Override
            public void run()
            {
                Log.i(TAG, "the alarm for " + target + " fired");

                boolean repeating = intervalMillis > 0;
                if (!repeating)
                {
                    synchronized (sScheduled)
                    {
                        if (sScheduled.get(target) == alarm) sScheduled.remove(target);
                    }
                }

                deliver(target, executor);

                if (repeating) handler.postDelayed(this, intervalMillis);
            }
        };

        Alarm previous;
        synchronized (sScheduled)
        {
            previous = sScheduled.put(target, alarm);
        }

        if (previous != null) previous.handler.removeCallbacks(previous.fire);

        handler.postDelayed(alarm.fire, delay);
        Log.i(TAG, "an alarm for " + target + " will fire in " + delay + "ms"
                       + (intervalMillis > 0 ? ", then every " + intervalMillis + "ms" : ""));

        return alarm;
    }

    private static void deliver(Object target, Executor executor)
    {
        if (target instanceof PendingIntent)
        {
            ((PendingIntent)target).send();
            return;
        }

        final OnAlarmListener listener = (OnAlarmListener)target;
        if (executor == null)
        {
            listener.onAlarm();
            return;
        }

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onAlarm();
            }
        });
    }

    private static long delayFor(int type, long triggerAtMillis)
    {
        long now = type == RTC || type == RTC_WAKEUP ? System.currentTimeMillis() : SystemClock.elapsedRealtime();
        long delay = triggerAtMillis - now;

        return delay < 0 ? 0 : delay;
    }
}
