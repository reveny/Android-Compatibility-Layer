package android.os;

import java.util.ArrayDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AsyncTask<Params, Progress, Result>
{
    private static final String LOG_TAG = "AsyncTask";

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 20;
    private static final int BACKUP_POOL_SIZE = 5;
    private static final int KEEP_ALIVE_SECONDS = 3;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory()
    {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable)
        {
            return new Thread(runnable, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static ThreadPoolExecutor sBackupExecutor;
    private static LinkedBlockingQueue<Runnable> sBackupExecutorQueue;

    private static final RejectedExecutionHandler RUN_ON_SERIAL_POLICY = new RejectedExecutionHandler()
    {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor)
        {
            android.util.Log.w(LOG_TAG, "Exceeded ThreadPoolExecutor pool size");

            synchronized (this)
            {
                if (sBackupExecutor == null)
                {
                    sBackupExecutorQueue = new LinkedBlockingQueue<Runnable>();
                    sBackupExecutor = new ThreadPoolExecutor(BACKUP_POOL_SIZE, BACKUP_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                                                             sBackupExecutorQueue, THREAD_FACTORY);
                    sBackupExecutor.allowCoreThreadTimeOut(true);
                }
            }
            sBackupExecutor.execute(runnable);
        }
    };

    public static final Executor THREAD_POOL_EXECUTOR;

    static
    {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                                                                       new SynchronousQueue<Runnable>(), THREAD_FACTORY);
        threadPoolExecutor.setRejectedExecutionHandler(RUN_ON_SERIAL_POLICY);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;

    private static volatile Executor sDefaultExecutor = SERIAL_EXECUTOR;
    private static InternalHandler sHandler;

    private final WorkerRunnable<Params, Result> mWorker;
    private final FutureTask<Result> mFuture;
    private final Handler mHandler;

    private volatile Status mStatus = Status.PENDING;

    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    private static class SerialExecutor implements Executor
    {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        @Override
        public synchronized void execute(final Runnable runnable)
        {
            mTasks.offer(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        runnable.run();
                    }
                    finally
                    {
                        scheduleNext();
                    }
                }
            });

            if (mActive == null) scheduleNext();
        }

        protected synchronized void scheduleNext()
        {
            if ((mActive = mTasks.poll()) != null) THREAD_POOL_EXECUTOR.execute(mActive);
        }
    }

    public enum Status
    {
        PENDING,
        RUNNING,
        FINISHED,
    }

    private static Handler getMainHandler()
    {
        synchronized (AsyncTask.class)
        {
            if (sHandler == null) sHandler = new InternalHandler(Looper.getMainLooper());
            return sHandler;
        }
    }

    private Handler getHandler()
    {
        return mHandler;
    }

    public static void setDefaultExecutor(Executor executor)
    {
        sDefaultExecutor = executor;
    }

    public AsyncTask()
    {
        this((Looper)null);
    }

    public AsyncTask(Handler handler)
    {
        this(handler != null ? handler.getLooper() : null);
    }

    public AsyncTask(Looper callbackLooper)
    {
        mHandler = callbackLooper == null || callbackLooper == Looper.getMainLooper() ? getMainHandler() : new Handler(callbackLooper);

        mWorker = new WorkerRunnable<Params, Result>()
        {
            @Override
            public Result call() throws Exception
            {
                mTaskInvoked.set(true);
                Result result = null;
                try
                {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    result = doInBackground(mParams);
                    Binder.flushPendingCommands();
                }
                catch (Throwable throwable)
                {
                    mCancelled.set(true);
                    throw throwable;
                }
                finally
                {
                    postResult(result);
                }
                return result;
            }
        };

        mFuture = new FutureTask<Result>(mWorker)
        {
            @Override
            protected void done()
            {
                try
                {
                    postResultIfNotInvoked(get());
                }
                catch (InterruptedException interrupted)
                {
                    android.util.Log.w(LOG_TAG, interrupted);
                }
                catch (ExecutionException failure)
                {
                    throw new RuntimeException("An error occurred while executing doInBackground()", failure.getCause());
                }
                catch (CancellationException cancelled)
                {
                    postResultIfNotInvoked(null);
                }
            }
        };
    }

    private void postResultIfNotInvoked(Result result)
    {
        final boolean wasTaskInvoked = mTaskInvoked.get();
        if (!wasTaskInvoked) postResult(result);
    }

    private Result postResult(Result result)
    {
        Message message = getHandler().obtainMessage(MESSAGE_POST_RESULT, new AsyncTaskResult<Result>(this, result));
        message.sendToTarget();
        return result;
    }

    public final Status getStatus()
    {
        return mStatus;
    }

    protected abstract Result doInBackground(Params... params);

    protected void onPreExecute()
    {
    }

    protected void onPostExecute(Result result)
    {
    }

    protected void onProgressUpdate(Progress... values)
    {
    }

    protected void onCancelled(Result result)
    {
        onCancelled();
    }

    protected void onCancelled()
    {
    }

    public final boolean isCancelled()
    {
        return mCancelled.get();
    }

    public final boolean cancel(boolean mayInterruptIfRunning)
    {
        mCancelled.set(true);
        return mFuture.cancel(mayInterruptIfRunning);
    }

    public final Result get() throws InterruptedException, ExecutionException
    {
        return mFuture.get();
    }

    public final Result get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
    {
        return mFuture.get(timeout, unit);
    }

    @SafeVarargs
    public final AsyncTask<Params, Progress, Result> execute(Params... params)
    {
        return executeOnExecutor(sDefaultExecutor, params);
    }

    @SafeVarargs
    public final AsyncTask<Params, Progress, Result> executeOnExecutor(Executor executor, Params... params)
    {
        if (mStatus != Status.PENDING)
        {
            switch (mStatus)
            {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task: the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task: the task has already been executed "
                                                    + "(a task can be executed only once)");
                default:
                    break;
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = params;
        executor.execute(mFuture);

        return this;
    }

    public static void execute(Runnable runnable)
    {
        sDefaultExecutor.execute(runnable);
    }

    @SafeVarargs
    protected final void publishProgress(Progress... values)
    {
        if (!isCancelled())
        {
            getHandler().obtainMessage(MESSAGE_POST_PROGRESS, new AsyncTaskResult<Progress>(this, values)).sendToTarget();
        }
    }

    private void finish(Result result)
    {
        if (isCancelled()) onCancelled(result);
        else onPostExecute(result);

        mStatus = Status.FINISHED;
    }

    private static class InternalHandler extends Handler
    {
        InternalHandler(Looper looper)
        {
            super(looper);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void handleMessage(Message message)
        {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>)message.obj;
            switch (message.what)
            {
                case MESSAGE_POST_RESULT:
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
                default:
                    break;
            }
        }
    }

    private abstract static class WorkerRunnable<Params, Result> implements Callable<Result>
    {
        Params[] mParams;
    }

    @SuppressWarnings({"rawtypes"})
    private static class AsyncTaskResult<Data>
    {
        final AsyncTask mTask;
        final Data[] mData;

        @SafeVarargs
        AsyncTaskResult(AsyncTask task, Data... data)
        {
            mTask = task;
            mData = data;
        }
    }
}
