package android.media;

import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class MediaRouter2
{
    private static final String SYSTEM_SESSION_ID = "SYSTEM_SESSION";
    private static final Object INSTANCE_LOCK = new Object();
    private static MediaRouter2 sInstance;

    private final Object mLock = new Object();
    private final Context mContext;
    private final String mPackageName;
    private final RoutingController mSystemController;
    private final List<RouteCallbackRecord> mRouteCallbacks = new ArrayList<>();
    private final List<TransferCallbackRecord> mTransferCallbacks = new ArrayList<>();
    private final List<ControllerCallbackRecord> mControllerCallbacks = new ArrayList<>();
    private final List<PreferenceCallbackRecord> mPreferenceCallbacks = new ArrayList<>();
    private final List<ScanToken> mScanTokens = new ArrayList<>();

    private RouteListingPreference mRouteListingPreference;
    private OnGetControllerHintsListener mOnGetControllerHintsListener;
    private int mNextScanTokenId = 1;

    private MediaRouter2(Context context)
    {
        mContext = context;
        mPackageName = context.getPackageName();
        mSystemController = new RoutingController();
    }

    public static MediaRouter2 getInstance(Context context)
    {
        Objects.requireNonNull(context, "context must not be null");

        synchronized (INSTANCE_LOCK)
        {
            if (sInstance == null)
            {
                Context applicationContext = context.getApplicationContext();
                sInstance = new MediaRouter2(applicationContext != null ? applicationContext : context);
            }

            return sInstance;
        }
    }

    public static MediaRouter2 getInstance(Context context, String clientPackageName, Executor executor,
                                           Runnable onInstanceInvalidatedListener)
    {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(clientPackageName, "clientPackageName must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(onInstanceInvalidatedListener, "onInstanceInvalidatedListener must not be null");

        if (clientPackageName.equals(context.getPackageName())) return getInstance(context);

        throw new SecurityException("Requires MEDIA_ROUTING_CONTROL or MEDIA_CONTENT_CONTROL to control other packages' routing");
    }

    private AudioManager audioManager()
    {
        Object service = mContext.getSystemService(Context.AUDIO_SERVICE);
        return service instanceof AudioManager ? (AudioManager)service : null;
    }

    private MediaRoute2Info deviceRoute()
    {
        AudioManager audioManager = audioManager();
        int volume = audioManager == null ? 0 : audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int volumeMax = audioManager == null ? 15 : audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return MediaRoute2Info.createDeviceSpeakerRoute(volume, volumeMax);
    }

    private boolean isDeviceRoute(MediaRoute2Info route)
    {
        return route != null && route.isSystemRoute() && "DEVICE_ROUTE".equals(route.getId());
    }

    public void registerRouteCallback(Executor executor, RouteCallback routeCallback, RouteDiscoveryPreference preference)
    {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(routeCallback, "callback must not be null");
        Objects.requireNonNull(preference, "preference must not be null");

        synchronized (mLock)
        {
            mRouteCallbacks.removeIf(record -> record.mCallback == routeCallback);
            mRouteCallbacks.add(new RouteCallbackRecord(executor, routeCallback, preference));
        }
    }

    public void unregisterRouteCallback(RouteCallback routeCallback)
    {
        Objects.requireNonNull(routeCallback, "callback must not be null");

        synchronized (mLock)
        {
            mRouteCallbacks.removeIf(record -> record.mCallback == routeCallback);
        }
    }

    public void registerRouteListingPreferenceUpdatedCallback(Executor executor,
                                                             Consumer<RouteListingPreference> routeListingPreferenceCallback)
    {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(routeListingPreferenceCallback, "callback must not be null");

        synchronized (mLock)
        {
            mPreferenceCallbacks.removeIf(record -> record.mCallback == routeListingPreferenceCallback);
            mPreferenceCallbacks.add(new PreferenceCallbackRecord(executor, routeListingPreferenceCallback));
        }
    }

    public void unregisterRouteListingPreferenceUpdatedCallback(Consumer<RouteListingPreference> callback)
    {
        Objects.requireNonNull(callback, "callback must not be null");

        synchronized (mLock)
        {
            mPreferenceCallbacks.removeIf(record -> record.mCallback == callback);
        }
    }

    public boolean showSystemOutputSwitcher()
    {
        return false;
    }

    public void setRouteListingPreference(RouteListingPreference routeListingPreference)
    {
        List<PreferenceCallbackRecord> records;

        synchronized (mLock)
        {
            if (Objects.equals(mRouteListingPreference, routeListingPreference)) return;

            mRouteListingPreference = routeListingPreference;
            records = new ArrayList<>(mPreferenceCallbacks);
        }

        for (PreferenceCallbackRecord record : records)
        {
            record.mExecutor.execute(() -> record.mCallback.accept(routeListingPreference));
        }
    }

    public RouteListingPreference getRouteListingPreference()
    {
        synchronized (mLock)
        {
            return mRouteListingPreference;
        }
    }

    public ScanToken requestScan(ScanRequest scanRequest)
    {
        Objects.requireNonNull(scanRequest, "scanRequest must not be null");

        synchronized (mLock)
        {
            ScanToken token = new ScanToken(mNextScanTokenId++);
            mScanTokens.add(token);
            return token;
        }
    }

    public void cancelScanRequest(ScanToken token)
    {
        Objects.requireNonNull(token, "token must not be null");

        synchronized (mLock)
        {
            if (!mScanTokens.remove(token)) throw new IllegalArgumentException("The token does not match any active scan request");
        }
    }

    public void setOnGetControllerHintsListener(OnGetControllerHintsListener listener)
    {
        mOnGetControllerHintsListener = listener;
    }

    public List<MediaRoute2Info> getRoutes()
    {
        List<MediaRoute2Info> routes = new ArrayList<>();
        routes.add(deviceRoute());
        return routes;
    }

    public void registerTransferCallback(Executor executor, TransferCallback callback)
    {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        synchronized (mLock)
        {
            for (TransferCallbackRecord record : mTransferCallbacks)
            {
                if (record.mCallback == callback) return;
            }

            mTransferCallbacks.add(new TransferCallbackRecord(executor, callback));
        }
    }

    public void unregisterTransferCallback(TransferCallback callback)
    {
        Objects.requireNonNull(callback, "callback must not be null");

        synchronized (mLock)
        {
            mTransferCallbacks.removeIf(record -> record.mCallback == callback);
        }
    }

    public void registerControllerCallback(Executor executor, ControllerCallback callback)
    {
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        synchronized (mLock)
        {
            for (ControllerCallbackRecord record : mControllerCallbacks)
            {
                if (record.mCallback == callback) return;
            }

            mControllerCallbacks.add(new ControllerCallbackRecord(executor, callback));
        }
    }

    public void unregisterControllerCallback(ControllerCallback callback)
    {
        Objects.requireNonNull(callback, "callback must not be null");

        synchronized (mLock)
        {
            mControllerCallbacks.removeIf(record -> record.mCallback == callback);
        }
    }

    public void transferTo(MediaRoute2Info route)
    {
        Objects.requireNonNull(route, "route must not be null");

        if (isDeviceRoute(route)) return;

        List<TransferCallbackRecord> records;

        synchronized (mLock)
        {
            records = new ArrayList<>(mTransferCallbacks);
        }

        for (TransferCallbackRecord record : records)
        {
            record.mExecutor.execute(() -> record.mCallback.onTransferFailure(route));
        }
    }

    public void stop()
    {
    }

    public RoutingController getSystemController()
    {
        return mSystemController;
    }

    public RoutingController getController(String id)
    {
        Objects.requireNonNull(id, "id must not be null");

        return SYSTEM_SESSION_ID.equals(id) ? mSystemController : null;
    }

    public List<RoutingController> getControllers()
    {
        List<RoutingController> controllers = new ArrayList<>();
        controllers.add(mSystemController);
        return controllers;
    }

    public void setRouteVolume(MediaRoute2Info route, int volume)
    {
        Objects.requireNonNull(route, "route must not be null");

        if (!isDeviceRoute(route)) return;

        AudioManager audioManager = audioManager();
        if (audioManager != null) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);

        notifyControllerUpdated();
    }

    private void notifyControllerUpdated()
    {
        List<ControllerCallbackRecord> records;

        synchronized (mLock)
        {
            records = new ArrayList<>(mControllerCallbacks);
        }

        for (ControllerCallbackRecord record : records)
        {
            record.mExecutor.execute(() -> record.mCallback.onControllerUpdated(mSystemController));
        }
    }

    private static final class RouteCallbackRecord
    {
        final Executor mExecutor;
        final RouteCallback mCallback;
        final RouteDiscoveryPreference mPreference;

        RouteCallbackRecord(Executor executor, RouteCallback callback, RouteDiscoveryPreference preference)
        {
            mExecutor = executor;
            mCallback = callback;
            mPreference = preference;
        }
    }

    private static final class TransferCallbackRecord
    {
        final Executor mExecutor;
        final TransferCallback mCallback;

        TransferCallbackRecord(Executor executor, TransferCallback callback)
        {
            mExecutor = executor;
            mCallback = callback;
        }
    }

    private static final class ControllerCallbackRecord
    {
        final Executor mExecutor;
        final ControllerCallback mCallback;

        ControllerCallbackRecord(Executor executor, ControllerCallback callback)
        {
            mExecutor = executor;
            mCallback = callback;
        }
    }

    private static final class PreferenceCallbackRecord
    {
        final Executor mExecutor;
        final Consumer<RouteListingPreference> mCallback;

        PreferenceCallbackRecord(Executor executor, Consumer<RouteListingPreference> callback)
        {
            mExecutor = executor;
            mCallback = callback;
        }
    }

    public abstract static class RouteCallback
    {
        public RouteCallback()
        {
        }

        public void onRoutesAdded(List<MediaRoute2Info> routes)
        {
        }

        public void onRoutesRemoved(List<MediaRoute2Info> routes)
        {
        }

        public void onRoutesChanged(List<MediaRoute2Info> routes)
        {
        }

        public void onRoutesUpdated(List<MediaRoute2Info> routes)
        {
        }
    }

    public abstract static class TransferCallback
    {
        public TransferCallback()
        {
        }

        public void onTransfer(RoutingController oldController, RoutingController newController)
        {
        }

        public void onTransferFailure(MediaRoute2Info requestedRoute)
        {
        }

        public void onStop(RoutingController controller)
        {
        }
    }

    public interface OnGetControllerHintsListener
    {
        Bundle onGetControllerHints(MediaRoute2Info route);
    }

    public abstract static class ControllerCallback
    {
        public ControllerCallback()
        {
        }

        public void onControllerUpdated(RoutingController controller)
        {
        }
    }

    public static final class ScanToken
    {
        private final int mId;

        private ScanToken(int id)
        {
            mId = id;
        }
    }

    public static final class ScanRequest
    {
        private final boolean mIsScreenOffScan;

        private ScanRequest(boolean isScreenOffScan)
        {
            mIsScreenOffScan = isScreenOffScan;
        }

        public boolean isScreenOffScan()
        {
            return mIsScreenOffScan;
        }

        public static final class Builder
        {
            private boolean mIsScreenOffScan;

            public Builder()
            {
            }

            public Builder setScreenOffScan(boolean isScreenOffScan)
            {
                mIsScreenOffScan = isScreenOffScan;
                return this;
            }

            public ScanRequest build()
            {
                return new ScanRequest(mIsScreenOffScan);
            }
        }
    }

    public class RoutingController
    {
        RoutingController()
        {
        }

        public String getId()
        {
            return SYSTEM_SESSION_ID;
        }

        public Bundle getControlHints()
        {
            return null;
        }

        public List<MediaRoute2Info> getSelectedRoutes()
        {
            List<MediaRoute2Info> routes = new ArrayList<>();
            routes.add(deviceRoute());
            return routes;
        }

        public List<MediaRoute2Info> getSelectableRoutes()
        {
            return Collections.emptyList();
        }

        public List<MediaRoute2Info> getDeselectableRoutes()
        {
            return Collections.emptyList();
        }

        public List<MediaRoute2Info> getTransferableRoutes()
        {
            return Collections.emptyList();
        }

        public boolean wasTransferInitiatedBySelf()
        {
            return false;
        }

        public RoutingSessionInfo getRoutingSessionInfo()
        {
            MediaRoute2Info route = deviceRoute();
            RoutingSessionInfo.Builder builder = new RoutingSessionInfo.Builder(SYSTEM_SESSION_ID, mPackageName);
            builder.setName("System session");
            builder.addSelectedRoute(route.getId());
            builder.setVolumeHandling(route.getVolumeHandling());
            builder.setVolumeMax(route.getVolumeMax());
            builder.setVolume(route.getVolume());
            builder.setSystemSession(true);
            return builder.build();
        }

        public int getVolumeHandling()
        {
            return MediaRoute2Info.PLAYBACK_VOLUME_VARIABLE;
        }

        public int getVolumeMax()
        {
            return deviceRoute().getVolumeMax();
        }

        public int getVolume()
        {
            return deviceRoute().getVolume();
        }

        public boolean isReleased()
        {
            return false;
        }

        public void selectRoute(MediaRoute2Info route)
        {
            Objects.requireNonNull(route, "route must not be null");
        }

        public void deselectRoute(MediaRoute2Info route)
        {
            Objects.requireNonNull(route, "route must not be null");
        }

        public void setVolume(int volume)
        {
            setRouteVolume(deviceRoute(), volume);
        }

        public void release()
        {
        }

        @Override
        public String toString()
        {
            return "RoutingController{ id=" + getId() + ", selectedRoutes=" + getSelectedRoutes() + ", volume=" + getVolume()
                    + ", volumeMax=" + getVolumeMax() + " }";
        }
    }
}
