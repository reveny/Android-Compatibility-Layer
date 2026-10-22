package android.content;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class Loader<D>
{
    int mId;
    OnLoadCompleteListener<D> mListener;
    OnLoadCanceledListener<D> mOnLoadCanceledListener;
    Context mContext;
    boolean mStarted;
    boolean mAbandoned;
    boolean mReset = true;
    boolean mContentChanged;
    boolean mProcessingChange;

    public final class ForceLoadContentObserver extends ContentObserver
    {
        public ForceLoadContentObserver()
        {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public boolean deliverSelfNotifications()
        {
            return true;
        }

        @Override
        public void onChange(boolean selfChange)
        {
            onContentChanged();
        }
    }

    public interface OnLoadCompleteListener<D>
    {
        void onLoadComplete(Loader<D> loader, D data);
    }

    public interface OnLoadCanceledListener<D>
    {
        void onLoadCanceled(Loader<D> loader);
    }

    public Loader(Context context)
    {
        mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
    }

    public void deliverResult(D data)
    {
        if (mListener != null) mListener.onLoadComplete(this, data);
    }

    public void deliverCancellation()
    {
        if (mOnLoadCanceledListener != null) mOnLoadCanceledListener.onLoadCanceled(this);
    }

    public Context getContext()
    {
        return mContext;
    }

    public int getId()
    {
        return mId;
    }

    public void registerListener(int id, OnLoadCompleteListener<D> listener)
    {
        if (mListener != null) throw new IllegalStateException("There is already a listener registered");

        mListener = listener;
        mId = id;
    }

    public void unregisterListener(OnLoadCompleteListener<D> listener)
    {
        if (mListener == null) throw new IllegalStateException("No listener register");
        if (mListener != listener) throw new IllegalArgumentException("Attempting to unregister the wrong listener");

        mListener = null;
    }

    public void registerOnLoadCanceledListener(OnLoadCanceledListener<D> listener)
    {
        if (mOnLoadCanceledListener != null) throw new IllegalStateException("There is already a listener registered");

        mOnLoadCanceledListener = listener;
    }

    public void unregisterOnLoadCanceledListener(OnLoadCanceledListener<D> listener)
    {
        if (mOnLoadCanceledListener == null) throw new IllegalStateException("No listener register");
        if (mOnLoadCanceledListener != listener) throw new IllegalArgumentException("Attempting to unregister the wrong listener");

        mOnLoadCanceledListener = null;
    }

    public boolean isStarted()
    {
        return mStarted;
    }

    public boolean isAbandoned()
    {
        return mAbandoned;
    }

    public boolean isReset()
    {
        return mReset;
    }

    public final void startLoading()
    {
        mStarted = true;
        mReset = false;
        mAbandoned = false;
        onStartLoading();
    }

    protected void onStartLoading()
    {
    }

    public boolean cancelLoad()
    {
        return onCancelLoad();
    }

    protected boolean onCancelLoad()
    {
        return false;
    }

    public void forceLoad()
    {
        onForceLoad();
    }

    protected void onForceLoad()
    {
    }

    public void stopLoading()
    {
        mStarted = false;
        onStopLoading();
    }

    protected void onStopLoading()
    {
    }

    public void abandon()
    {
        mAbandoned = true;
        onAbandon();
    }

    protected void onAbandon()
    {
    }

    public void reset()
    {
        onReset();
        mReset = true;
        mStarted = false;
        mAbandoned = false;
        mContentChanged = false;
        mProcessingChange = false;
    }

    protected void onReset()
    {
    }

    public boolean takeContentChanged()
    {
        boolean changed = mContentChanged;
        mContentChanged = false;
        mProcessingChange |= changed;
        return changed;
    }

    public void commitContentChanged()
    {
        mProcessingChange = false;
    }

    public void rollbackContentChanged()
    {
        if (mProcessingChange) onContentChanged();
    }

    public void onContentChanged()
    {
        if (mStarted)
        {
            forceLoad();
            return;
        }

        mContentChanged = true;
    }

    public String dataToString(D data)
    {
        StringBuilder builder = new StringBuilder(64);

        if (data == null)
        {
            builder.append("null");
            return builder.toString();
        }

        builder.append(data.getClass().getSimpleName()).append("{").append(Integer.toHexString(System.identityHashCode(data)))
            .append("}");

        return builder.toString();
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{" + Integer.toHexString(System.identityHashCode(this)) + " id=" + mId + "}";
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args)
    {
        writer.print(prefix);
        writer.print("mId=");
        writer.print(mId);
        writer.print(" mListener=");
        writer.println(mListener);

        if (mStarted || mContentChanged || mProcessingChange)
        {
            writer.print(prefix);
            writer.print("mStarted=");
            writer.print(mStarted);
            writer.print(" mContentChanged=");
            writer.print(mContentChanged);
            writer.print(" mProcessingChange=");
            writer.println(mProcessingChange);
        }

        if (mAbandoned || mReset)
        {
            writer.print(prefix);
            writer.print("mAbandoned=");
            writer.print(mAbandoned);
            writer.print(" mReset=");
            writer.println(mReset);
        }
    }
}
