package android.webkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GeolocationPermissions
{
    private static final GeolocationPermissions INSTANCE = new GeolocationPermissions();

    private final Map<String, Boolean> mPermissions = new HashMap<>();

    GeolocationPermissions()
    {
    }

    public static GeolocationPermissions getInstance()
    {
        return INSTANCE;
    }

    public void getOrigins(ValueCallback<Set<String>> callback)
    {
        Set<String> origins;
        synchronized (mPermissions)
        {
            origins = new HashSet<>(mPermissions.keySet());
        }
        if (callback != null) callback.onReceiveValue(origins);
    }

    public void getAllowed(String origin, ValueCallback<Boolean> callback)
    {
        Boolean allowed;
        synchronized (mPermissions)
        {
            allowed = origin == null ? null : mPermissions.get(origin);
        }
        if (callback != null) callback.onReceiveValue(allowed != null && allowed);
    }

    public void clear(String origin)
    {
        synchronized (mPermissions)
        {
            mPermissions.remove(origin);
        }
    }

    public void allow(String origin)
    {
        if (origin == null) return;
        synchronized (mPermissions)
        {
            mPermissions.put(origin, Boolean.TRUE);
        }
    }

    public void clearAll()
    {
        synchronized (mPermissions)
        {
            mPermissions.clear();
        }
    }

    public interface Callback
    {
        void invoke(String origin, boolean allow, boolean retain);
    }
}
