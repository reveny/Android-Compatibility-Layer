package android.content;

import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

public abstract class AsyncQueryHandler extends Handler
{
    private static final String TAG = "AsyncQuery";

    private static final int EVENT_ARG_QUERY = 1;
    private static final int EVENT_ARG_INSERT = 2;
    private static final int EVENT_ARG_UPDATE = 3;
    private static final int EVENT_ARG_DELETE = 4;

    private static Looper sLooper = null;

    final WeakReference<ContentResolver> mResolver;

    private final Handler mWorkerThreadHandler;

    protected static final class WorkerArgs
    {
        public Uri uri;
        public Handler handler;
        public String[] projection;
        public String selection;
        public String[] selectionArgs;
        public String orderBy;
        public Object result;
        public Object cookie;
        public ContentValues values;
    }

    protected class WorkerHandler extends Handler
    {
        public WorkerHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message message)
        {
            ContentResolver resolver = mResolver.get();
            if (resolver == null) return;

            WorkerArgs args = (WorkerArgs)message.obj;
            int token = message.what;
            int event = message.arg1;

            switch (event)
            {
                case EVENT_ARG_QUERY:
                    Cursor cursor;
                    try
                    {
                        cursor = resolver.query(args.uri, args.projection, args.selection, args.selectionArgs, args.orderBy);
                        if (cursor != null) cursor.getCount();
                    }
                    catch (Exception failure)
                    {
                        android.util.Log.w(TAG, "the query failed", failure);
                        cursor = null;
                    }
                    args.result = cursor;
                    break;
                case EVENT_ARG_INSERT:
                    args.result = resolver.insert(args.uri, args.values);
                    break;
                case EVENT_ARG_UPDATE:
                    args.result = resolver.update(args.uri, args.values, args.selection, args.selectionArgs);
                    break;
                case EVENT_ARG_DELETE:
                    args.result = resolver.delete(args.uri, args.selection, args.selectionArgs);
                    break;
                default:
                    break;
            }

            Message reply = args.handler.obtainMessage(token);
            reply.obj = args;
            reply.arg1 = message.arg1;
            reply.sendToTarget();
        }
    }

    public AsyncQueryHandler(ContentResolver resolver)
    {
        super();
        mResolver = new WeakReference<>(resolver);

        synchronized (AsyncQueryHandler.class)
        {
            if (sLooper == null)
            {
                HandlerThread thread = new HandlerThread("AsyncQueryWorker");
                thread.start();
                sLooper = thread.getLooper();
            }
        }

        mWorkerThreadHandler = createHandler(sLooper);
    }

    protected Handler createHandler(Looper looper)
    {
        return new WorkerHandler(looper);
    }

    public void startQuery(int token, Object cookie, Uri uri, String[] projection, String selection, String[] selectionArgs,
                           String orderBy)
    {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.uri = uri;
        args.projection = projection;
        args.selection = selection;
        args.selectionArgs = selectionArgs;
        args.orderBy = orderBy;
        args.cookie = cookie;

        send(token, EVENT_ARG_QUERY, args);
    }

    public final void cancelOperation(int token)
    {
        mWorkerThreadHandler.removeMessages(token);
    }

    public final void startInsert(int token, Object cookie, Uri uri, ContentValues initialValues)
    {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.uri = uri;
        args.cookie = cookie;
        args.values = initialValues;

        send(token, EVENT_ARG_INSERT, args);
    }

    public final void startUpdate(int token, Object cookie, Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.uri = uri;
        args.cookie = cookie;
        args.values = values;
        args.selection = selection;
        args.selectionArgs = selectionArgs;

        send(token, EVENT_ARG_UPDATE, args);
    }

    public final void startDelete(int token, Object cookie, Uri uri, String selection, String[] selectionArgs)
    {
        WorkerArgs args = new WorkerArgs();
        args.handler = this;
        args.uri = uri;
        args.cookie = cookie;
        args.selection = selection;
        args.selectionArgs = selectionArgs;

        send(token, EVENT_ARG_DELETE, args);
    }

    private void send(int token, int event, WorkerArgs args)
    {
        Message message = mWorkerThreadHandler.obtainMessage(token);
        message.arg1 = event;
        message.obj = args;
        mWorkerThreadHandler.sendMessage(message);
    }

    protected void onQueryComplete(int token, Object cookie, Cursor cursor)
    {
    }

    protected void onInsertComplete(int token, Object cookie, Uri uri)
    {
    }

    protected void onUpdateComplete(int token, Object cookie, int result)
    {
    }

    protected void onDeleteComplete(int token, Object cookie, int result)
    {
    }

    @Override
    public void handleMessage(Message message)
    {
        WorkerArgs args = (WorkerArgs)message.obj;
        int token = message.what;
        int event = message.arg1;

        switch (event)
        {
            case EVENT_ARG_QUERY:
                onQueryComplete(token, args.cookie, (Cursor)args.result);
                break;
            case EVENT_ARG_INSERT:
                onInsertComplete(token, args.cookie, (Uri)args.result);
                break;
            case EVENT_ARG_UPDATE:
                onUpdateComplete(token, args.cookie, (Integer)args.result);
                break;
            case EVENT_ARG_DELETE:
                onDeleteComplete(token, args.cookie, (Integer)args.result);
                break;
            default:
                break;
        }
    }
}
