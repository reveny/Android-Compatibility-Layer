package android.service.media;

import android.app.Service;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class MediaBrowserService extends Service
{
    public static final String SERVICE_INTERFACE = "android.media.browse.MediaBrowserService";

    private static final String TAG = "MediaBrowserService";

    private static final int RESULT_FLAG_OPTION_NOT_HANDLED = 1;

    private final List<Connection> mConnections = new ArrayList<>();
    private Connection mCurrentConnection;
    private MediaSession.Token mSession;
    private IBinder mBinder;
    private Handler mHandler;

    private static final class Connection
    {
        String packageName;
        int pid;
        int uid;
        Bundle rootHints;
        BrowserRoot root;
        final List<Subscription> subscriptions = new ArrayList<>();
    }

    private static final class Subscription
    {
        String parentId;
        Bundle options;
        Consumer<List<MediaBrowser.MediaItem>> listener;
    }

    public MediaBrowserService()
    {
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        Looper main = Looper.getMainLooper();
        mHandler = main == null ? null : new Handler(main);
        Binder binder = new Binder(BINDER_DESCRIPTOR);
        binder.attachInterface(new LocalInterface(this, binder), BINDER_DESCRIPTOR);
        mBinder = binder;
    }

    private static final String BINDER_DESCRIPTOR = "android.service.media.IMediaBrowserService";

    private static final class LocalInterface implements IInterface, Supplier<MediaBrowserService>
    {
        private final MediaBrowserService mService;
        private final IBinder mBinder;

        LocalInterface(MediaBrowserService service, IBinder binder)
        {
            mService = service;
            mBinder = binder;
        }

        @Override
        public IBinder asBinder()
        {
            return mBinder;
        }

        @Override
        public MediaBrowserService get()
        {
            return mService;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        if (SERVICE_INTERFACE.equals(intent == null ? null : intent.getAction())) return mBinder;
        return null;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args)
    {
        writer.println("MediaBrowserService: " + mConnections.size() + " connection(s), session " + mSession);
    }

    public abstract BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints);

    public abstract void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result);

    public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result, Bundle options)
    {
        result.setFlags(RESULT_FLAG_OPTION_NOT_HANDLED);
        onLoadChildren(parentId, result);
    }

    public void onLoadItem(String itemId, Result<MediaBrowser.MediaItem> result)
    {
        result.setFlags(RESULT_FLAG_OPTION_NOT_HANDLED);
        result.sendResult(null);
    }

    public void setSessionToken(final MediaSession.Token token)
    {
        if (token == null) throw new IllegalArgumentException("Session token may not be null.");
        if (mSession != null) throw new IllegalStateException("The session token has already been set.");

        mSession = token;
    }

    public MediaSession.Token getSessionToken()
    {
        return mSession;
    }

    public final Bundle getBrowserRootHints()
    {
        if (mCurrentConnection == null)
        {
            throw new IllegalStateException("This should be called inside of onGetRoot or onLoadChildren or onLoadItem methods");
        }

        return mCurrentConnection.rootHints == null ? null : new Bundle(mCurrentConnection.rootHints);
    }

    public final MediaSessionManager.RemoteUserInfo getCurrentBrowserInfo()
    {
        if (mCurrentConnection == null)
        {
            throw new IllegalStateException("This should be called inside of onGetRoot or onLoadChildren or onLoadItem methods");
        }

        return new MediaSessionManager.RemoteUserInfo(mCurrentConnection.packageName, mCurrentConnection.pid,
                                                      mCurrentConnection.uid);
    }

    public void notifyChildrenChanged(String parentId)
    {
        notifyChildrenChangedInternal(parentId, null);
    }

    public void notifyChildrenChanged(String parentId, Bundle options)
    {
        if (options == null) throw new IllegalArgumentException("options cannot be null in notifyChildrenChanged");

        notifyChildrenChangedInternal(parentId, options);
    }

    public final BrowserRoot connectBrowser(String packageName, int pid, int uid, Bundle rootHints)
    {
        Connection connection = new Connection();
        connection.packageName = packageName;
        connection.pid = pid;
        connection.uid = uid;
        connection.rootHints = rootHints;

        mCurrentConnection = connection;
        try
        {
            connection.root = onGetRoot(packageName, uid, rootHints);
        }
        finally
        {
            mCurrentConnection = null;
        }

        if (connection.root == null)
        {
            Log.i(TAG, "no root for client " + packageName + " from service " + getClass().getName());
            return null;
        }

        mConnections.add(connection);
        return connection.root;
    }

    public final void disconnectBrowser(String packageName, int uid)
    {
        for (int index = mConnections.size() - 1; index >= 0; index--)
        {
            Connection connection = mConnections.get(index);
            if (Objects.equals(connection.packageName, packageName) && connection.uid == uid) mConnections.remove(index);
        }
    }

    public final void subscribeBrowser(String packageName, int uid, String parentId, Bundle options,
                                       Consumer<List<MediaBrowser.MediaItem>> listener)
    {
        Connection connection = connectionFor(packageName, uid);
        if (connection == null) return;

        Subscription subscription = new Subscription();
        subscription.parentId = parentId;
        subscription.options = options;
        subscription.listener = listener;
        connection.subscriptions.add(subscription);

        performLoadChildren(connection, subscription);
    }

    public final void unsubscribeBrowser(String packageName, int uid, String parentId)
    {
        Connection connection = connectionFor(packageName, uid);
        if (connection == null) return;

        for (int index = connection.subscriptions.size() - 1; index >= 0; index--)
        {
            if (Objects.equals(connection.subscriptions.get(index).parentId, parentId)) connection.subscriptions.remove(index);
        }
    }

    public final void loadItemForBrowser(String packageName, int uid, String itemId,
                                         final Consumer<MediaBrowser.MediaItem> listener)
    {
        Connection connection = connectionFor(packageName, uid);
        if (connection == null) return;

        Result<MediaBrowser.MediaItem> result = new Result<MediaBrowser.MediaItem>(itemId)
        {
            @Override
            void onResultSent(MediaBrowser.MediaItem item, int flags)
            {
                if (listener != null) listener.accept(item);
            }
        };

        mCurrentConnection = connection;
        try
        {
            onLoadItem(itemId, result);
        }
        finally
        {
            mCurrentConnection = null;
        }

        if (!result.isDone())
        {
            throw new IllegalStateException("onLoadItem must call detach() or sendResult() before returning for id=" + itemId);
        }
    }

    private void notifyChildrenChangedInternal(final String parentId, final Bundle options)
    {
        if (parentId == null) throw new IllegalArgumentException("parentId cannot be null in notifyChildrenChanged");

        Runnable work = new Runnable()
        {
            @Override
            public void run()
            {
                for (Connection connection : new ArrayList<>(mConnections))
                {
                    for (Subscription subscription : new ArrayList<>(connection.subscriptions))
                    {
                        if (!parentId.equals(subscription.parentId)) continue;
                        if (options != null && subscription.options != null && !optionsOverlap(options, subscription.options))
                        {
                            continue;
                        }

                        performLoadChildren(connection, subscription);
                    }
                }
            }
        };

        if (mHandler != null) mHandler.post(work);
        else work.run();
    }

    private void performLoadChildren(Connection connection, final Subscription subscription)
    {
        Result<List<MediaBrowser.MediaItem>> result = new Result<List<MediaBrowser.MediaItem>>(subscription.parentId)
        {
            @Override
            void onResultSent(List<MediaBrowser.MediaItem> list, int flags)
            {
                List<MediaBrowser.MediaItem> delivered = list;
                if ((flags & RESULT_FLAG_OPTION_NOT_HANDLED) != 0 && list != null) delivered = page(list, subscription.options);
                if (subscription.listener != null) subscription.listener.accept(delivered);
            }
        };

        mCurrentConnection = connection;
        try
        {
            if (subscription.options == null) onLoadChildren(subscription.parentId, result);
            else onLoadChildren(subscription.parentId, result, subscription.options);
        }
        finally
        {
            mCurrentConnection = null;
        }

        if (!result.isDone())
        {
            throw new IllegalStateException("onLoadChildren must call detach() or sendResult() before returning for package="
                                            + connection.packageName + " id=" + subscription.parentId);
        }
    }

    private static List<MediaBrowser.MediaItem> page(List<MediaBrowser.MediaItem> list, Bundle options)
    {
        if (options == null) return list;

        int page = options.getInt(MediaBrowser.EXTRA_PAGE, -1);
        int pageSize = options.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1);
        if (page == -1 && pageSize == -1) return list;

        int fromIndex = pageSize * page;
        int toIndex = fromIndex + pageSize;
        if (page < 0 || pageSize < 1 || fromIndex >= list.size()) return new ArrayList<>();
        if (toIndex > list.size()) toIndex = list.size();
        return list.subList(fromIndex, toIndex);
    }

    private static boolean optionsOverlap(Bundle first, Bundle second)
    {
        int firstPage = first.getInt(MediaBrowser.EXTRA_PAGE, -1);
        int secondPage = second.getInt(MediaBrowser.EXTRA_PAGE, -1);
        int firstSize = first.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1);
        int secondSize = second.getInt(MediaBrowser.EXTRA_PAGE_SIZE, -1);

        long firstStart = firstPage == -1 || firstSize == -1 ? 0 : (long)firstSize * firstPage;
        long firstEnd = firstPage == -1 || firstSize == -1 ? Long.MAX_VALUE : firstStart + firstSize - 1;
        long secondStart = secondPage == -1 || secondSize == -1 ? 0 : (long)secondSize * secondPage;
        long secondEnd = secondPage == -1 || secondSize == -1 ? Long.MAX_VALUE : secondStart + secondSize - 1;

        return firstEnd >= secondStart && secondEnd >= firstStart;
    }

    private Connection connectionFor(String packageName, int uid)
    {
        for (Connection connection : mConnections)
        {
            if (Objects.equals(connection.packageName, packageName) && connection.uid == uid) return connection;
        }

        return null;
    }

    public class Result<T>
    {
        private final Object mDebug;
        private boolean mDetachCalled;
        private boolean mSendResultCalled;
        private int mFlags;

        Result(Object debug)
        {
            mDebug = debug;
        }

        public void sendResult(T result)
        {
            if (mSendResultCalled) throw new IllegalStateException("sendResult() called twice for: " + mDebug);

            mSendResultCalled = true;
            onResultSent(result, mFlags);
        }

        public void detach()
        {
            if (mDetachCalled)
            {
                throw new IllegalStateException("detach() called when detach() had already been called for: " + mDebug);
            }
            if (mSendResultCalled)
            {
                throw new IllegalStateException("detach() called when sendResult() had already been called for: " + mDebug);
            }

            mDetachCalled = true;
        }

        boolean isDone()
        {
            return mDetachCalled || mSendResultCalled;
        }

        void setFlags(int flags)
        {
            mFlags = flags;
        }

        void onResultSent(T result, int flags)
        {
        }
    }

    public static final class BrowserRoot
    {
        public static final String EXTRA_RECENT = "android.service.media.extra.RECENT";
        public static final String EXTRA_OFFLINE = "android.service.media.extra.OFFLINE";
        public static final String EXTRA_SUGGESTED = "android.service.media.extra.SUGGESTED";

        private final String mRootId;
        private final Bundle mExtras;

        public BrowserRoot(String rootId, Bundle extras)
        {
            if (rootId == null)
            {
                throw new IllegalArgumentException("The root id in BrowserRoot cannot be null. Use null for BrowserRoot instead.");
            }

            mRootId = rootId;
            mExtras = extras;
        }

        public String getRootId()
        {
            return mRootId;
        }

        public Bundle getExtras()
        {
            return mExtras;
        }
    }
}
