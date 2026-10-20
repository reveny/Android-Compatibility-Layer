package runtime;

import android.app.AppComponentFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Broadcasts
{
    private static final String TAG = "Broadcasts";

    private static final class Registration
    {
        BroadcastReceiver receiver;
        IntentFilter filter;
        Context context;
    }

    private static final class Delivery
    {
        BroadcastReceiver receiver;
        int priority;
        String describedBy;
    }

    private static final List<Registration> sRegistered = new ArrayList<>();

    private static Context sBaseContext;
    private static AppComponentFactory sFactory;
    private static ClassLoader sLoader;
    private static String sPackageName;
    private static Handler sHandler;

    private Broadcasts()
    {
    }

    public static void attachRuntime(Context base, AppComponentFactory factory, ClassLoader loader, String packageName)
    {
        sBaseContext = base;
        sFactory = factory;
        sLoader = loader;
        sPackageName = packageName;

        Looper main = Looper.getMainLooper();
        sHandler = main == null ? null : new Handler(main);
    }

    public static Intent register(Context context, BroadcastReceiver receiver, IntentFilter filter)
    {
        if (receiver == null || filter == null) return null;

        Registration registration = new Registration();
        registration.receiver = receiver;
        registration.filter = filter;
        registration.context = context;

        synchronized (sRegistered)
        {
            sRegistered.add(registration);
        }

        return null;
    }

    public static void unregister(BroadcastReceiver receiver)
    {
        if (receiver == null) return;

        synchronized (sRegistered)
        {
            for (int index = sRegistered.size() - 1; index >= 0; index--)
            {
                if (sRegistered.get(index).receiver == receiver) sRegistered.remove(index);
            }
        }
    }

    public static void send(final Intent intent, final BroadcastReceiver resultReceiver, final boolean ordered)
    {
        if (intent == null) return;

        final List<Delivery> deliveries = collect(intent);

        if (deliveries.isEmpty())
        {
            Log.i(TAG, "nothing is listening for " + intent);
            return;
        }

        deliveries.sort(new Comparator<Delivery>()
        {
            @Override
            public int compare(Delivery first, Delivery second)
            {
                return Integer.compare(second.priority, first.priority);
            }
        });

        final Intent delivered = new Intent(intent);

        post(new Runnable()
        {
            @Override
            public void run()
            {
                for (Delivery delivery : deliveries)
                {
                    try
                    {
                        delivery.receiver.onReceive(sBaseContext, delivered);
                        if (ordered && delivery.receiver.isAborted())
                        {
                            Log.i(TAG, delivery.describedBy + " aborted the ordered broadcast " + delivered.getAction());
                            break;
                        }
                    }
                    catch (Throwable cause)
                    {
                        Log.e(TAG, "onReceive threw in " + delivery.describedBy, cause);
                    }
                }

                if (resultReceiver != null)
                {
                    try
                    {
                        resultReceiver.onReceive(sBaseContext, delivered);
                    }
                    catch (Throwable cause)
                    {
                        Log.e(TAG, "the result receiver threw", cause);
                    }
                }
            }
        });
    }

    private static List<Delivery> collect(Intent intent)
    {
        List<Delivery> deliveries = new ArrayList<>();

        synchronized (sRegistered)
        {
            for (Registration registration : sRegistered)
            {
                int outcome = registration.filter.match(intent.getAction(), intent.getType(), intent.getScheme(), intent.getData(),
                                                        intent.getCategories(), TAG);
                if (outcome < 0) continue;

                Delivery delivery = new Delivery();
                delivery.receiver = registration.receiver;
                delivery.priority = registration.filter.getPriority();
                delivery.describedBy = registration.receiver.getClass().getName();
                deliveries.add(delivery);
            }
        }

        if (sFactory == null) return deliveries;

        for (Components.Record record : Components.resolveAll(Components.RECEIVER, intent))
        {
            try
            {
                BroadcastReceiver receiver = sFactory.instantiateReceiver(sLoader, record.className, intent);

                Delivery delivery = new Delivery();
                delivery.receiver = receiver;
                delivery.priority = highestPriority(record);
                delivery.describedBy = record.className;
                deliveries.add(delivery);
            }
            catch (Throwable cause)
            {
                Log.e(TAG, "could not instantiate receiver " + record.className, cause);
            }
        }

        return deliveries;
    }

    private static int highestPriority(Components.Record record)
    {
        int best = 0;
        for (IntentFilter filter : record.filters)
        {
            if (filter.getPriority() > best) best = filter.getPriority();
        }

        return best;
    }

    private static void post(Runnable work)
    {
        if (sHandler != null)
        {
            sHandler.post(work);
            return;
        }

        work.run();
    }
}
