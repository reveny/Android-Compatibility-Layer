package runtime;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.util.Log;

public final class StartupProbe
{
    private static final String TAG = "StartupProbe";
    private static final String APP_INITIALIZER = "androidx.startup.AppInitializer";

    private StartupProbe()
    {
    }

    public static void reportInitializers(Context context, ProviderInfo[] providers)
    {
        Object appInitializer = instance(context);
        if (appInitializer == null) return;

        for (ProviderInfo provider : providers)
        {
            Bundle metaData = provider.metaData;
            if (metaData == null) continue;

            for (String className : metaData.keySet())
            {
                report(appInitializer, className);
            }
        }
    }

    private static Object instance(Context context)
    {
        try
        {
            Class<?> type = Class.forName(APP_INITIALIZER);
            return type.getMethod("getInstance", Context.class).invoke(null, context);
        }
        catch (Throwable cause)
        {
            Log.i(TAG, "androidx.startup not present in this package");
            return null;
        }
    }

    private static void report(Object appInitializer, String className)
    {
        Class<?> initializer;
        try
        {
            initializer = Class.forName(className);
        }
        catch (Throwable cause)
        {
            Log.i(TAG, "initializer " + className + " not loadable");
            return;
        }

        try
        {
            Object initialized = appInitializer.getClass()
                    .getMethod("isEagerlyInitialized", Class.class)
                    .invoke(appInitializer, initializer);
            Log.i(TAG, "initializer " + className + " initialized=" + initialized);
        }
        catch (Throwable cause)
        {
            Log.i(TAG, "initializer " + className + " state unavailable: " + cause);
        }
    }
}
