package android.net.nsd;

import android.content.Context;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public final class NsdManager
{
    public static final String ACTION_NSD_STATE_CHANGED = "android.net.nsd.STATE_CHANGED";
    public static final String EXTRA_NSD_STATE = "nsd_state";
    public static final int FAILURE_INTERNAL_ERROR = 0;
    public static final int FAILURE_ALREADY_ACTIVE = 3;
    public static final int FAILURE_MAX_LIMIT = 4;
    public static final int FAILURE_OPERATION_NOT_RUNNING = 5;
    public static final int FAILURE_BAD_PARAMETERS = 6;
    public static final int NSD_STATE_DISABLED = 1;
    public static final int NSD_STATE_ENABLED = 2;
    public static final int PROTOCOL_DNS_SD = 1;

    private final Context mContext;
    private final Map<DiscoveryListener, String> mActiveListeners = new HashMap<>();

    public NsdManager(Context context)
    {
        mContext = context;
    }

    public interface RegistrationListener
    {
        void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode);

        void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode);

        void onServiceRegistered(NsdServiceInfo serviceInfo);

        void onServiceUnregistered(NsdServiceInfo serviceInfo);
    }

    public interface DiscoveryListener
    {
        void onStartDiscoveryFailed(String serviceType, int errorCode);

        void onStopDiscoveryFailed(String serviceType, int errorCode);

        void onDiscoveryStarted(String serviceType);

        void onDiscoveryStopped(String serviceType);

        void onServiceFound(NsdServiceInfo serviceInfo);

        void onServiceLost(NsdServiceInfo serviceInfo);
    }

    public interface ResolveListener
    {
        void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode);

        void onServiceResolved(NsdServiceInfo serviceInfo);

        default void onResolutionStopped(NsdServiceInfo serviceInfo)
        {
        }

        default void onStopResolutionFailed(NsdServiceInfo serviceInfo, int errorCode)
        {
        }
    }

    public void registerService(NsdServiceInfo serviceInfo, int protocolType, RegistrationListener listener)
    {
        registerService(serviceInfo, protocolType, mainExecutor(), listener);
    }

    public void registerService(final NsdServiceInfo serviceInfo, int protocolType, Executor executor,
                                final RegistrationListener listener)
    {
        checkProtocol(protocolType);
        if (serviceInfo == null || listener == null || executor == null) throw new IllegalArgumentException("null argument");

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onRegistrationFailed(serviceInfo, FAILURE_INTERNAL_ERROR);
            }
        });
    }

    public void unregisterService(RegistrationListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");

        throw new IllegalArgumentException("listener not registered");
    }

    public void discoverServices(String serviceType, int protocolType, DiscoveryListener listener)
    {
        discoverServices(serviceType, protocolType, (Network)null, mainExecutor(), listener);
    }

    public void discoverServices(final String serviceType, int protocolType, Network network, Executor executor,
                                 final DiscoveryListener listener)
    {
        if (serviceType == null || serviceType.isEmpty()) throw new IllegalArgumentException("Service type cannot be empty");
        if (listener == null || executor == null) throw new IllegalArgumentException("null argument");

        checkProtocol(protocolType);

        synchronized (mActiveListeners)
        {
            if (mActiveListeners.containsKey(listener)) throw new IllegalArgumentException("listener already in use");

            mActiveListeners.put(listener, serviceType);
        }

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onDiscoveryStarted(serviceType);
            }
        });
    }

    public void discoverServices(String serviceType, int protocolType, NetworkRequest networkRequest, Executor executor,
                                 DiscoveryListener listener)
    {
        discoverServices(serviceType, protocolType, (Network)null, executor, listener);
    }

    public void stopServiceDiscovery(final DiscoveryListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");

        final String serviceType;
        synchronized (mActiveListeners)
        {
            if (!mActiveListeners.containsKey(listener)) throw new IllegalArgumentException("listener not registered");

            serviceType = mActiveListeners.remove(listener);
        }

        mainExecutor().execute(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onDiscoveryStopped(serviceType);
            }
        });
    }

    @Deprecated
    public void resolveService(NsdServiceInfo serviceInfo, ResolveListener listener)
    {
        resolveService(serviceInfo, mainExecutor(), listener);
    }

    @Deprecated
    public void resolveService(final NsdServiceInfo serviceInfo, Executor executor, final ResolveListener listener)
    {
        if (serviceInfo == null || listener == null || executor == null) throw new IllegalArgumentException("null argument");

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                listener.onResolveFailed(serviceInfo, FAILURE_INTERNAL_ERROR);
            }
        });
    }

    public void stopServiceResolution(ResolveListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null");

        throw new IllegalArgumentException("listener not registered");
    }

    private static void checkProtocol(int protocolType)
    {
        if (protocolType != PROTOCOL_DNS_SD) throw new IllegalArgumentException("Unsupported protocol");
    }

    private static Executor mainExecutor()
    {
        Looper main = Looper.getMainLooper();

        if (main == null)
        {
            return new Executor()
            {
                @Override
                public void execute(Runnable command)
                {
                    command.run();
                }
            };
        }

        final Handler handler = new Handler(main);
        return new Executor()
        {
            @Override
            public void execute(Runnable command)
            {
                handler.post(command);
            }
        };
    }
}
