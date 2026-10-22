package android.content;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.OperationCanceledException;
import android.os.SystemClock;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.Executor;

public abstract class AsyncTaskLoader<D> extends Loader<D>
{
    private final Executor mExecutor;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private LoadTask mTask;
    private LoadTask mCancellingTask;
    private long mUpdateThrottle;
    private long mLastLoadCompleteTime = -10000;

    private final class LoadTask implements Runnable
    {
        private volatile boolean mCancelled;
        private boolean mWaiting;
        private boolean mSubmitted;
        private D mResult;
        private boolean mFailedByCancellation;

        private final Runnable mDispatch = new Runnable()
        {
            @Override
            public void run()
            {
                if (mCancelled || mFailedByCancellation) dispatchOnCancelled(LoadTask.this, mResult);
                else dispatchOnLoadComplete(LoadTask.this, mResult);
            }
        };

        private final Runnable mSubmit = new Runnable()
        {
            @Override
            public void run()
            {
                mWaiting = false;
                executePendingTask();
            }
        };

        @Override
        public void run()
        {
            if (!mCancelled)
            {
                try
                {
                    mResult = onLoadInBackground();
                }
                catch (OperationCanceledException cancelled)
                {
                    mFailedByCancellation = true;
                }
            }

            mHandler.post(mDispatch);
        }

        boolean cancel()
        {
            if (mCancelled) return false;

            mCancelled = true;
            return true;
        }
    }

    public AsyncTaskLoader(Context context)
    {
        this(context, AsyncTask.THREAD_POOL_EXECUTOR);
    }

    AsyncTaskLoader(Context context, Executor executor)
    {
        super(context);
        mExecutor = executor;
    }

    public void setUpdateThrottle(long delayMillis)
    {
        mUpdateThrottle = delayMillis;
    }

    @Override
    protected void onForceLoad()
    {
        super.onForceLoad();
        cancelLoad();
        mTask = new LoadTask();
        executePendingTask();
    }

    @Override
    protected boolean onCancelLoad()
    {
        if (mTask == null) return false;

        if (!mStarted) mContentChanged = true;

        if (mCancellingTask != null)
        {
            if (mTask.mWaiting)
            {
                mTask.mWaiting = false;
                mHandler.removeCallbacks(mTask.mSubmit);
            }

            mTask = null;
            return false;
        }

        if (mTask.mWaiting)
        {
            mTask.mWaiting = false;
            mHandler.removeCallbacks(mTask.mSubmit);
            mTask = null;
            return false;
        }

        boolean cancelled = mTask.cancel();

        if (cancelled)
        {
            mCancellingTask = mTask;
            cancelLoadInBackground();
        }

        mTask = null;
        return cancelled;
    }

    public void onCanceled(D data)
    {
    }

    void executePendingTask()
    {
        if (mCancellingTask != null || mTask == null || mTask.mSubmitted) return;

        if (mTask.mWaiting)
        {
            mTask.mWaiting = false;
            mHandler.removeCallbacks(mTask.mSubmit);
        }

        if (mUpdateThrottle > 0)
        {
            long now = SystemClock.uptimeMillis();

            if (now < mLastLoadCompleteTime + mUpdateThrottle)
            {
                mTask.mWaiting = true;
                mHandler.postAtTime(mTask.mSubmit, mLastLoadCompleteTime + mUpdateThrottle);
                return;
            }
        }

        mTask.mSubmitted = true;
        mExecutor.execute(mTask);
    }

    void dispatchOnCancelled(LoadTask task, D data)
    {
        onCanceled(data);

        if (mCancellingTask == task)
        {
            rollbackContentChanged();
            mLastLoadCompleteTime = SystemClock.uptimeMillis();
            mCancellingTask = null;
            deliverCancellation();
            executePendingTask();
        }
    }

    void dispatchOnLoadComplete(LoadTask task, D data)
    {
        if (mTask != task)
        {
            dispatchOnCancelled(task, data);
            return;
        }

        if (isAbandoned())
        {
            onCanceled(data);
            return;
        }

        commitContentChanged();
        mLastLoadCompleteTime = SystemClock.uptimeMillis();
        mTask = null;
        deliverResult(data);
    }

    public abstract D loadInBackground();

    protected D onLoadInBackground()
    {
        return loadInBackground();
    }

    public void cancelLoadInBackground()
    {
    }

    public boolean isLoadInBackgroundCanceled()
    {
        return mCancellingTask != null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args)
    {
        super.dump(prefix, fd, writer, args);

        writer.print(prefix);
        writer.print("mTask=");
        writer.print(mTask);
        writer.print(" mCancellingTask=");
        writer.println(mCancellingTask);

        if (mUpdateThrottle != 0)
        {
            writer.print(prefix);
            writer.print("mUpdateThrottle=");
            writer.print(mUpdateThrottle);
            writer.print(" mLastLoadCompleteTime=");
            writer.println(mLastLoadCompleteTime);
        }
    }
}
