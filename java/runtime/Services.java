package runtime;

import android.app.AppComponentFactory;
import android.app.Application;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Services
{
    private static final String TAG = "Services";

    private static final class Record
    {
        Service service;
        String className;
        IBinder binding;
        boolean started;
        int nextStartId = 1;
        int lastStartId;
        boolean foreground;
        final List<ServiceConnection> connections = new ArrayList<>();
    }

    private static final Map<String, Record> sRecords = new LinkedHashMap<>();

    private static Context sBaseContext;
    private static Application sApplication;
    private static AppComponentFactory sFactory;
    private static ClassLoader sLoader;
    private static String sPackageName;
    private static Handler sHandler;

    private Services()
    {
    }

    public static void attachRuntime(Context base, Application application, AppComponentFactory factory, ClassLoader loader,
                                     String packageName)
    {
        sBaseContext = base;
        sApplication = application;
        sFactory = factory;
        sLoader = loader;
        sPackageName = packageName;

        Looper main = Looper.getMainLooper();
        sHandler = main == null ? null : new Handler(main);
    }

    public static ComponentName start(Intent intent)
    {
        Record record = obtain(intent);
        if (record == null) return null;

        record.started = true;
        final int startId = record.nextStartId++;
        record.lastStartId = startId;

        final Record target = record;
        final Intent delivered = intent == null ? null : new Intent(intent);

        post(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    int outcome = target.service.onStartCommand(delivered, 0, startId);
                    Log.i(TAG, "started " + target.className + " with id " + startId + ", it answered " + outcome);
                }
                catch (Throwable cause)
                {
                    Log.e(TAG, "onStartCommand threw in " + target.className, cause);
                }
            }
        });

        return new ComponentName(sPackageName, record.className);
    }

    public static boolean stop(Intent intent)
    {
        Components.Record declared = Components.resolve(Components.SERVICE, intent);
        if (declared == null) return false;

        Record record = sRecords.get(declared.className);
        if (record == null) return false;

        record.started = false;
        return teardownIfIdle(record);
    }

    public static boolean stopSelf(Service service, int startId)
    {
        Record record = recordFor(service);
        if (record == null) return false;
        if (startId >= 0 && startId != record.lastStartId) return false;

        record.started = false;
        teardownIfIdle(record);
        return true;
    }

    public static boolean bind(Intent intent, final ServiceConnection connection, int flags)
    {
        if (connection == null) return false;

        Record record = obtain(intent);
        if (record == null) return false;

        final Record target = record;
        final Intent delivered = intent == null ? null : new Intent(intent);

        if (!target.connections.contains(connection)) target.connections.add(connection);

        post(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (target.binding == null) target.binding = target.service.onBind(delivered);

                    ComponentName name = new ComponentName(sPackageName, target.className);

                    if (target.binding == null)
                    {
                        Log.i(TAG, target.className + " bound with no interface");
                        connection.onNullBinding(name);
                        return;
                    }

                    Log.i(TAG, "bound " + target.className);
                    connection.onServiceConnected(name, target.binding);
                }
                catch (Throwable cause)
                {
                    Log.e(TAG, "onBind threw in " + target.className, cause);
                }
            }
        });

        return true;
    }

    public static void unbind(ServiceConnection connection)
    {
        if (connection == null) return;

        for (Record record : new ArrayList<>(sRecords.values()))
        {
            if (!record.connections.remove(connection)) continue;

            try
            {
                connection.onServiceDisconnected(new ComponentName(sPackageName, record.className));
                if (record.connections.isEmpty() && record.binding != null)
                {
                    record.service.onUnbind(null);
                    record.binding = null;
                }
            }
            catch (Throwable cause)
            {
                Log.e(TAG, "onUnbind threw in " + record.className, cause);
            }

            teardownIfIdle(record);
        }
    }

    public static void startForeground(Service service, int id, Notification notification)
    {
        Record record = recordFor(service);
        if (record == null) return;

        record.foreground = true;
        Log.i(TAG, record.className + " went to the foreground with notification " + id);

        if (notification != null) NotificationStore.post(null, id, notification);
    }

    public static void stopForeground(Service service, boolean removeNotification)
    {
        Record record = recordFor(service);
        if (record == null) return;

        record.foreground = false;
        Log.i(TAG, record.className + " left the foreground");
    }

    private static Record obtain(Intent intent)
    {
        if (sBaseContext == null)
        {
            Log.w(TAG, "a service was asked for before the runtime was attached: " + intent);
            return null;
        }

        Components.Record declared = Components.resolve(Components.SERVICE, intent);
        if (declared == null) return null;

        Record record = sRecords.get(declared.className);
        if (record != null) return record;

        record = new Record();
        record.className = declared.className;

        try
        {
            record.service = sFactory.instantiateService(sLoader, declared.className, intent);
            record.service.attach(sBaseContext, sApplication);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "could not instantiate service " + declared.className, cause);
            return null;
        }

        sRecords.put(declared.className, record);

        try
        {
            record.service.onCreate();
            Log.i(TAG, "created " + declared.className);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "onCreate threw in " + declared.className, cause);
        }

        return record;
    }

    private static boolean teardownIfIdle(Record record)
    {
        if (record.started || !record.connections.isEmpty()) return false;

        sRecords.remove(record.className);

        try
        {
            record.service.onDestroy();
            Log.i(TAG, "destroyed " + record.className);
        }
        catch (Throwable cause)
        {
            Log.e(TAG, "onDestroy threw in " + record.className, cause);
        }

        return true;
    }

    private static Record recordFor(Service service)
    {
        for (Record record : sRecords.values())
        {
            if (record.service == service) return record;
        }

        return null;
    }

    private static void post(Runnable work)
    {
        if (sHandler != null && sHandler.getLooper() != Looper.myLooper())
        {
            sHandler.post(work);
            return;
        }

        if (sHandler != null)
        {
            sHandler.post(work);
            return;
        }

        work.run();
    }
}
