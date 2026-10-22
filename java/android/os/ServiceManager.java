package android.os;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ServiceManager
{
    private static final Map<String, IBinder> sServices = new LinkedHashMap<>();

    static
    {
        sServices.put("deviceidle", new PowerExemptionManager.DeviceIdleController());
    }

    private ServiceManager()
    {
    }

    public static void addService(String name, IBinder service)
    {
        if (name == null || service == null) return;

        synchronized (sServices)
        {
            sServices.put(name, service);
        }
    }

    public static IBinder getService(String name)
    {
        synchronized (sServices)
        {
            return sServices.get(name);
        }
    }

    public static IBinder checkService(String name)
    {
        return getService(name);
    }

    public static String[] listServices()
    {
        synchronized (sServices)
        {
            return sServices.keySet().toArray(new String[0]);
        }
    }
}
