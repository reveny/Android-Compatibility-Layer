package android.widget;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public abstract class Filter
{
    private static final String THREAD_NAME = "Filter";
    private static final long WORKER_IDLE_TIMEOUT = 3000;

    private final Object mLock = new Object();
    private final Handler mResultHandler;
    private Handler mThreadHandler;
    private HandlerThread mThread;
    private Runnable mPendingRequest;

    private final Runnable mQuitWorker = new Runnable()
    {
        @Override
        public void run()
        {
            synchronized (mLock)
            {
                if (mThreadHandler == null) return;

                mThreadHandler.getLooper().quit();
                mThreadHandler = null;
                mThread = null;
            }
        }
    };

    public Filter()
    {
        Looper looper = Looper.myLooper();
        mResultHandler = new Handler(looper != null ? looper : Looper.getMainLooper());
    }

    public final void filter(CharSequence constraint)
    {
        filter(constraint, null);
    }

    public final void filter(CharSequence constraint, FilterListener listener)
    {
        synchronized (mLock)
        {
            if (mThreadHandler == null)
            {
                mThread = new HandlerThread(THREAD_NAME + "@" + Integer.toHexString(System.identityHashCode(this)));
                mThread.start();
                mThreadHandler = new Handler(mThread.getLooper());
            }

            final CharSequence request = constraint == null ? null : constraint.toString();
            final FilterListener requestListener = listener;

            Runnable work = new Runnable()
            {
                @Override
                public void run()
                {
                    runRequest(this, request, requestListener);
                }
            };

            mThreadHandler.removeCallbacks(mQuitWorker);
            if (mPendingRequest != null) mThreadHandler.removeCallbacks(mPendingRequest);

            mPendingRequest = work;
            mThreadHandler.post(work);
        }
    }

    private void runRequest(Runnable work, final CharSequence constraint, final FilterListener listener)
    {
        FilterResults computed;

        try
        {
            computed = performFiltering(constraint);
        }
        catch (Exception failure)
        {
            android.util.Log.w("Filter", "An exception occured during performFiltering()!", failure);
            computed = new FilterResults();
        }

        final FilterResults results = computed == null ? new FilterResults() : computed;

        mResultHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                publishResults(constraint, results);
                if (listener != null) listener.onFilterComplete(results.count);
            }
        });

        synchronized (mLock)
        {
            if (mPendingRequest == work) mPendingRequest = null;
            if (mThreadHandler != null) mThreadHandler.postDelayed(mQuitWorker, WORKER_IDLE_TIMEOUT);
        }
    }

    protected abstract FilterResults performFiltering(CharSequence constraint);

    protected abstract void publishResults(CharSequence constraint, FilterResults results);

    public CharSequence convertResultToString(Object resultValue)
    {
        return resultValue == null ? "" : resultValue.toString();
    }

    public static class FilterResults
    {
        public int count;
        public Object values;

        public FilterResults()
        {
        }
    }

    public interface FilterListener
    {
        void onFilterComplete(int count);
    }
}
