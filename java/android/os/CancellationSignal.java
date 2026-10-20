package android.os;

public final class CancellationSignal
{
    private boolean mCancelled;
    private OnCancelListener mListener;

    public interface OnCancelListener
    {
        void onCancel();
    }

    public boolean isCanceled()
    {
        synchronized (this)
        {
            return mCancelled;
        }
    }

    public void throwIfCanceled()
    {
        if (isCanceled()) throw new OperationCanceledException();
    }

    public void cancel()
    {
        OnCancelListener listener;

        synchronized (this)
        {
            if (mCancelled) return;

            mCancelled = true;
            listener = mListener;
        }

        if (listener != null) listener.onCancel();
    }

    public void setOnCancelListener(OnCancelListener listener)
    {
        boolean alreadyCancelled;

        synchronized (this)
        {
            mListener = listener;
            alreadyCancelled = mCancelled;
        }

        if (alreadyCancelled && listener != null) listener.onCancel();
    }
}
