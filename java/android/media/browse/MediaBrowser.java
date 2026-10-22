package android.media.browse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.service.media.MediaBrowserService;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class MediaBrowser
{
    public static final String EXTRA_PAGE = "android.media.browse.extra.PAGE";
    public static final String EXTRA_PAGE_SIZE = "android.media.browse.extra.PAGE_SIZE";

    private static final String TAG = "MediaBrowser";
    private static final String SERVICE_DESCRIPTOR = "android.service.media.IMediaBrowserService";

    private static final int CONNECT_STATE_DISCONNECTING = 0;
    private static final int CONNECT_STATE_DISCONNECTED = 1;
    private static final int CONNECT_STATE_CONNECTING = 2;
    private static final int CONNECT_STATE_CONNECTED = 3;
    private static final int CONNECT_STATE_SUSPENDED = 4;

    private final Context mContext;
    private final ComponentName mServiceComponent;
    private final ConnectionCallback mCallback;
    private final Bundle mRootHints;
    private final Handler mHandler;
    private final Map<String, List<SubscriptionRecord>> mSubscriptions = new HashMap<>();

    private volatile int mState = CONNECT_STATE_DISCONNECTED;
    private ServiceConnection mServiceConnection;
    private MediaBrowserService mService;
    private String mRootId;
    private MediaSession.Token mMediaSessionToken;
    private Bundle mExtras;

    public MediaBrowser(Context context, ComponentName serviceComponent, ConnectionCallback callback, Bundle rootHints)
    {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        if (serviceComponent == null) throw new IllegalArgumentException("service component must not be null");
        if (callback == null) throw new IllegalArgumentException("connection callback must not be null");

        mContext = context;
        mServiceComponent = serviceComponent;
        mCallback = callback;
        mRootHints = rootHints == null ? null : new Bundle(rootHints);

        Looper looper = Looper.myLooper();
        mHandler = new Handler(looper != null ? looper : Looper.getMainLooper());
    }

    public void connect()
    {
        if (mState != CONNECT_STATE_DISCONNECTING && mState != CONNECT_STATE_DISCONNECTED)
        {
            throw new IllegalStateException("connect() called while neither disconnecting nor disconnected (state="
                    + stateName(mState) + ")");
        }

        mState = CONNECT_STATE_CONNECTING;
        mHandler.post(this::bindToService);
    }

    public void disconnect()
    {
        mState = CONNECT_STATE_DISCONNECTING;

        mHandler.post(() ->
        {
            if (mService != null) mService.disconnectBrowser(mContext.getPackageName(), Process.myUid());

            int state = mState;
            forceCloseConnection();
            if (state != CONNECT_STATE_DISCONNECTING) mState = state;
        });
    }

    public boolean isConnected()
    {
        return mState == CONNECT_STATE_CONNECTED;
    }

    public ComponentName getServiceComponent()
    {
        if (!isConnected()) throw new IllegalStateException("getServiceComponent() called while not connected (state=" + mState + ")");

        return mServiceComponent;
    }

    public String getRoot()
    {
        if (!isConnected()) throw new IllegalStateException("getRoot() called while not connected (state=" + stateName(mState) + ")");

        return mRootId;
    }

    public Bundle getExtras()
    {
        if (!isConnected()) throw new IllegalStateException("getExtras() called while not connected (state=" + stateName(mState) + ")");

        return mExtras;
    }

    public MediaSession.Token getSessionToken()
    {
        if (!isConnected()) throw new IllegalStateException("getSessionToken() called while not connected (state=" + mState + ")");

        return mMediaSessionToken;
    }

    public void subscribe(String parentId, SubscriptionCallback callback)
    {
        subscribeInternal(parentId, null, callback);
    }

    public void subscribe(String parentId, Bundle options, SubscriptionCallback callback)
    {
        if (options == null) throw new IllegalArgumentException("options cannot be null");

        subscribeInternal(parentId, new Bundle(options), callback);
    }

    public void unsubscribe(String parentId)
    {
        unsubscribeInternal(parentId, null);
    }

    public void unsubscribe(String parentId, SubscriptionCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");

        unsubscribeInternal(parentId, callback);
    }

    public void getItem(String mediaId, ItemCallback cb)
    {
        if (TextUtils.isEmpty(mediaId)) throw new IllegalArgumentException("mediaId cannot be empty.");
        if (cb == null) throw new IllegalArgumentException("cb cannot be null.");

        MediaBrowserService service = mService;

        if (mState != CONNECT_STATE_CONNECTED || service == null)
        {
            Log.i(TAG, "Not connected, unable to retrieve the MediaItem.");
            mHandler.post(() -> cb.onError(mediaId));
            return;
        }

        try
        {
            service.loadItemForBrowser(mContext.getPackageName(), Process.myUid(), mediaId, item -> mHandler.post(() ->
            {
                if (item == null) cb.onError(mediaId);
                else cb.onItemLoaded(item);
            }));
        }
        catch (RuntimeException exception)
        {
            Log.i(TAG, "Remote error getting media item.", exception);
            mHandler.post(() -> cb.onError(mediaId));
        }
    }

    private void subscribeInternal(String parentId, Bundle options, SubscriptionCallback callback)
    {
        if (TextUtils.isEmpty(parentId)) throw new IllegalArgumentException("parentId cannot be empty.");
        if (callback == null) throw new IllegalArgumentException("callback cannot be null");

        SubscriptionRecord record = new SubscriptionRecord(parentId, options, callback);

        synchronized (mSubscriptions)
        {
            List<SubscriptionRecord> records = mSubscriptions.get(parentId);

            if (records == null)
            {
                records = new ArrayList<>();
                mSubscriptions.put(parentId, records);
            }

            for (int index = records.size() - 1; index >= 0; index--)
            {
                if (Objects.equals(bundleKey(records.get(index).mOptions), bundleKey(options))) records.remove(index);
            }

            records.add(record);
        }

        if (isConnected()) sendSubscription(record);
    }

    private void unsubscribeInternal(String parentId, SubscriptionCallback callback)
    {
        if (TextUtils.isEmpty(parentId)) throw new IllegalArgumentException("parentId cannot be empty.");

        List<SubscriptionRecord> remaining = new ArrayList<>();

        synchronized (mSubscriptions)
        {
            List<SubscriptionRecord> records = mSubscriptions.get(parentId);
            if (records == null) return;

            if (callback == null) records.clear();
            else records.removeIf(record -> record.mCallback == callback);

            remaining.addAll(records);
            if (records.isEmpty()) mSubscriptions.remove(parentId);
        }

        MediaBrowserService service = mService;

        if (isConnected() && service != null)
        {
            service.unsubscribeBrowser(mContext.getPackageName(), Process.myUid(), parentId);

            for (SubscriptionRecord record : remaining) sendSubscription(record);
        }
    }

    private void sendSubscription(SubscriptionRecord record)
    {
        MediaBrowserService service = mService;
        if (service == null) return;

        try
        {
            service.subscribeBrowser(mContext.getPackageName(), Process.myUid(), record.mParentId, record.mOptions,
                    items -> mHandler.post(() -> deliverChildren(record, items)));
        }
        catch (RuntimeException exception)
        {
            Log.i(TAG, "subscribe failed for id=" + record.mParentId, exception);
            mHandler.post(() -> deliverChildren(record, null));
        }
    }

    private void deliverChildren(SubscriptionRecord record, List<MediaItem> items)
    {
        synchronized (mSubscriptions)
        {
            List<SubscriptionRecord> records = mSubscriptions.get(record.mParentId);
            if (records == null || !records.contains(record)) return;
        }

        if (items == null)
        {
            if (record.mOptions == null) record.mCallback.onError(record.mParentId);
            else record.mCallback.onError(record.mParentId, record.mOptions);
            return;
        }

        List<MediaItem> copy = new ArrayList<>(items);

        if (record.mOptions == null) record.mCallback.onChildrenLoaded(record.mParentId, copy);
        else record.mCallback.onChildrenLoaded(record.mParentId, copy, record.mOptions);
    }

    private void bindToService()
    {
        if (mState != CONNECT_STATE_CONNECTING) return;

        Intent intent = new Intent(MediaBrowserService.SERVICE_INTERFACE);
        intent.setComponent(mServiceComponent);

        ServiceConnection connection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder)
            {
                if (mServiceConnection != this || mState != CONNECT_STATE_CONNECTING) return;

                MediaBrowserService service = resolveService(binder);

                if (service == null)
                {
                    failConnection();
                    return;
                }

                MediaBrowserService.BrowserRoot root;

                try
                {
                    root = service.connectBrowser(mContext.getPackageName(), Process.myPid(), Process.myUid(), mRootHints);
                }
                catch (RuntimeException exception)
                {
                    Log.w(TAG, "onGetRoot threw for " + mServiceComponent, exception);
                    root = null;
                }

                if (root == null)
                {
                    failConnection();
                    return;
                }

                mService = service;
                mRootId = root.getRootId();
                mExtras = root.getExtras();
                mMediaSessionToken = service.getSessionToken();
                mState = CONNECT_STATE_CONNECTED;
                mCallback.onConnected();

                List<SubscriptionRecord> records = new ArrayList<>();

                synchronized (mSubscriptions)
                {
                    for (List<SubscriptionRecord> list : mSubscriptions.values()) records.addAll(list);
                }

                for (SubscriptionRecord record : records) sendSubscription(record);
            }

            @Override
            public void onServiceDisconnected(ComponentName name)
            {
                if (mServiceConnection != this) return;

                mService = null;
                mState = CONNECT_STATE_SUSPENDED;
                mCallback.onConnectionSuspended();
            }

            @Override
            public void onNullBinding(ComponentName name)
            {
                if (mServiceConnection != this) return;

                failConnection();
            }
        };

        mServiceConnection = connection;
        boolean bound;

        try
        {
            bound = mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
        catch (RuntimeException exception)
        {
            Log.e(TAG, "Failed binding to service " + mServiceComponent, exception);
            bound = false;
        }

        if (!bound) failConnection();
    }

    private static MediaBrowserService resolveService(IBinder binder)
    {
        if (binder == null) return null;

        IInterface local = binder.queryLocalInterface(SERVICE_DESCRIPTOR);
        if (!(local instanceof Supplier)) return null;

        Object service = ((Supplier<?>)local).get();
        return service instanceof MediaBrowserService ? (MediaBrowserService)service : null;
    }

    private void failConnection()
    {
        forceCloseConnection();
        mCallback.onConnectionFailed();
    }

    private void forceCloseConnection()
    {
        ServiceConnection connection = mServiceConnection;
        mServiceConnection = null;

        if (connection != null)
        {
            try
            {
                mContext.unbindService(connection);
            }
            catch (RuntimeException exception)
            {
                Log.w(TAG, "unbindService failed", exception);
            }
        }

        mState = CONNECT_STATE_DISCONNECTED;
        mService = null;
        mRootId = null;
        mExtras = null;
        mMediaSessionToken = null;
    }

    private static String bundleKey(Bundle options)
    {
        if (options == null) return null;

        return options.getInt(EXTRA_PAGE, -1) + ":" + options.getInt(EXTRA_PAGE_SIZE, -1);
    }

    private static String stateName(int state)
    {
        switch (state)
        {
            case CONNECT_STATE_DISCONNECTING:
                return "CONNECT_STATE_DISCONNECTING";
            case CONNECT_STATE_DISCONNECTED:
                return "CONNECT_STATE_DISCONNECTED";
            case CONNECT_STATE_CONNECTING:
                return "CONNECT_STATE_CONNECTING";
            case CONNECT_STATE_CONNECTED:
                return "CONNECT_STATE_CONNECTED";
            case CONNECT_STATE_SUSPENDED:
                return "CONNECT_STATE_SUSPENDED";
            default:
                return "UNKNOWN/" + state;
        }
    }

    private static final class SubscriptionRecord
    {
        final String mParentId;
        final Bundle mOptions;
        final SubscriptionCallback mCallback;

        SubscriptionRecord(String parentId, Bundle options, SubscriptionCallback callback)
        {
            mParentId = parentId;
            mOptions = options;
            mCallback = callback;
        }
    }

    public static class MediaItem implements Parcelable
    {
        public static final int FLAG_BROWSABLE = 1;
        public static final int FLAG_PLAYABLE = 2;

        public static final Parcelable.Creator<MediaItem> CREATOR = new Parcelable.Creator<MediaItem>()
        {
            @Override
            public MediaItem createFromParcel(Parcel source)
            {
                int flags = source.readInt();
                MediaDescription description = MediaDescription.CREATOR.createFromParcel(source);
                return new MediaItem(description, flags);
            }

            @Override
            public MediaItem[] newArray(int size)
            {
                return new MediaItem[size];
            }
        };

        private final int mFlags;
        private final MediaDescription mDescription;

        public MediaItem(MediaDescription description, int flags)
        {
            if (description == null) throw new IllegalArgumentException("description cannot be null");
            if (TextUtils.isEmpty(description.getMediaId()))
            {
                throw new IllegalArgumentException("description must have a non-empty media id");
            }

            mFlags = flags;
            mDescription = description;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            out.writeInt(mFlags);
            mDescription.writeToParcel(out, flags);
        }

        @Override
        public String toString()
        {
            return "MediaItem{mFlags=" + mFlags + ", mDescription=" + mDescription + '}';
        }

        public int getFlags()
        {
            return mFlags;
        }

        public boolean isBrowsable()
        {
            return (mFlags & FLAG_BROWSABLE) != 0;
        }

        public boolean isPlayable()
        {
            return (mFlags & FLAG_PLAYABLE) != 0;
        }

        public MediaDescription getDescription()
        {
            return mDescription;
        }

        public String getMediaId()
        {
            return mDescription.getMediaId();
        }
    }

    public static class ConnectionCallback
    {
        public ConnectionCallback()
        {
        }

        public void onConnected()
        {
        }

        public void onConnectionSuspended()
        {
        }

        public void onConnectionFailed()
        {
        }
    }

    public abstract static class SubscriptionCallback
    {
        public SubscriptionCallback()
        {
        }

        public void onChildrenLoaded(String parentId, List<MediaItem> children)
        {
        }

        public void onChildrenLoaded(String parentId, List<MediaItem> children, Bundle options)
        {
        }

        public void onError(String parentId)
        {
        }

        public void onError(String parentId, Bundle options)
        {
        }
    }

    public abstract static class ItemCallback
    {
        public ItemCallback()
        {
        }

        public void onItemLoaded(MediaItem item)
        {
        }

        public void onError(String mediaId)
        {
        }
    }
}
