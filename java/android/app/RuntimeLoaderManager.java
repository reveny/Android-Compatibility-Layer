package android.app;

import android.content.Loader;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import java.io.FileDescriptor;
import java.io.PrintWriter;

final class RuntimeLoaderManager extends LoaderManager
{
    private static final String TAG = "LoaderManager";

    private final String mWho;
    private final SparseArray<LoaderInfo> mLoaders = new SparseArray<>();
    private boolean mStarted;
    private boolean mCreatingLoader;

    private final class LoaderInfo implements Loader.OnLoadCompleteListener<Object>, Loader.OnLoadCanceledListener<Object>
    {
        final int mId;
        final Bundle mArgs;
        LoaderCallbacks<Object> mCallbacks;
        Loader<Object> mLoader;
        boolean mHaveData;
        boolean mDeliveredData;
        Object mData;
        boolean mStarted;
        boolean mListenerRegistered;
        boolean mDestroyed;

        LoaderInfo(int id, Bundle args, LoaderCallbacks<Object> callbacks)
        {
            mId = id;
            mArgs = args;
            mCallbacks = callbacks;
        }

        void start()
        {
            if (mStarted || mLoader == null) return;

            mStarted = true;

            if (!mListenerRegistered)
            {
                mLoader.registerListener(mId, this);
                mLoader.registerOnLoadCanceledListener(this);
                mListenerRegistered = true;
            }

            mLoader.startLoading();
        }

        void stop()
        {
            mStarted = false;

            if (mLoader == null) return;

            if (mListenerRegistered)
            {
                mListenerRegistered = false;
                mLoader.unregisterListener(this);
                mLoader.unregisterOnLoadCanceledListener(this);
            }

            mLoader.stopLoading();
        }

        void destroy()
        {
            mDestroyed = true;

            if (mCallbacks != null && mLoader != null && mHaveData && mDeliveredData) mCallbacks.onLoaderReset(mLoader);

            mCallbacks = null;
            mData = null;
            mHaveData = false;

            if (mLoader == null) return;

            if (mListenerRegistered)
            {
                mListenerRegistered = false;
                mLoader.unregisterListener(this);
                mLoader.unregisterOnLoadCanceledListener(this);
            }

            mLoader.reset();
        }

        @Override
        public void onLoadComplete(Loader<Object> loader, Object data)
        {
            if (mDestroyed || mLoaders.get(mId) != this) return;

            if (mData != data || !mHaveData)
            {
                mData = data;
                mHaveData = true;
                if (mStarted) deliver();
            }
        }

        @Override
        public void onLoadCanceled(Loader<Object> loader)
        {
            if (sDebug) Log.v(TAG, "load of loader " + mId + " in " + mWho + " was cancelled");
        }

        void deliver()
        {
            if (mCallbacks == null) return;

            mCallbacks.onLoadFinished(mLoader, mData);
            mDeliveredData = true;
        }
    }

    RuntimeLoaderManager(String who, boolean started)
    {
        mWho = who;
        mStarted = started;
    }

    private static void requireMainThread(String operation)
    {
        Looper main = Looper.getMainLooper();
        if (main != null && !main.isCurrentThread()) throw new IllegalStateException(operation + " must be called on the main thread");
    }

    @SuppressWarnings("unchecked")
    private LoaderInfo create(int id, Bundle args, LoaderCallbacks<?> callback)
    {
        LoaderInfo info = new LoaderInfo(id, args, (LoaderCallbacks<Object>)callback);

        mCreatingLoader = true;
        try
        {
            info.mLoader = (Loader<Object>)callback.onCreateLoader(id, args);
        }
        finally
        {
            mCreatingLoader = false;
        }

        if (info.mLoader != null && info.mLoader.getClass().isMemberClass()
            && !java.lang.reflect.Modifier.isStatic(info.mLoader.getClass().getModifiers()))
        {
            throw new IllegalArgumentException("Object returned from onCreateLoader must not be a non-static inner member class: "
                                               + info.mLoader);
        }

        mLoaders.put(id, info);
        if (mStarted) info.start();

        return info;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> Loader<D> initLoader(int id, Bundle args, LoaderCallbacks<D> callback)
    {
        if (mCreatingLoader) throw new IllegalStateException("Called while creating a loader");
        requireMainThread("initLoader");

        LoaderInfo info = mLoaders.get(id);

        if (info == null)
        {
            info = create(id, args, callback);
        }
        else
        {
            info.mCallbacks = (LoaderCallbacks<Object>)callback;
            if (info.mHaveData && mStarted) info.deliver();
        }

        return (Loader<D>)info.mLoader;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> Loader<D> restartLoader(int id, Bundle args, LoaderCallbacks<D> callback)
    {
        if (mCreatingLoader) throw new IllegalStateException("Called while creating a loader");
        requireMainThread("restartLoader");

        LoaderInfo previous = mLoaders.get(id);
        if (previous != null)
        {
            mLoaders.remove(id);
            if (previous.mLoader != null) previous.mLoader.abandon();
            previous.destroy();
        }

        return (Loader<D>)create(id, args, callback).mLoader;
    }

    @Override
    public void destroyLoader(int id)
    {
        if (mCreatingLoader) throw new IllegalStateException("Called while creating a loader");
        requireMainThread("destroyLoader");

        LoaderInfo info = mLoaders.get(id);
        if (info == null) return;

        mLoaders.remove(id);
        info.destroy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <D> Loader<D> getLoader(int id)
    {
        if (mCreatingLoader) throw new IllegalStateException("Called while creating a loader");

        LoaderInfo info = mLoaders.get(id);
        return info == null ? null : (Loader<D>)info.mLoader;
    }

    void doStart()
    {
        if (mStarted) return;

        mStarted = true;
        for (int index = mLoaders.size() - 1; index >= 0; index--) mLoaders.valueAt(index).start();
    }

    void doStop()
    {
        if (!mStarted) return;

        for (int index = mLoaders.size() - 1; index >= 0; index--) mLoaders.valueAt(index).stop();
        mStarted = false;
    }

    void doDestroy()
    {
        for (int index = mLoaders.size() - 1; index >= 0; index--) mLoaders.valueAt(index).destroy();
        mLoaders.clear();
        mStarted = false;
    }

    boolean hasRunningLoaders()
    {
        for (int index = 0; index < mLoaders.size(); index++)
        {
            LoaderInfo info = mLoaders.valueAt(index);
            if (info.mStarted && !info.mDeliveredData) return true;
        }

        return false;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args)
    {
        if (mLoaders.size() == 0) return;

        writer.print(prefix);
        writer.println("Active Loaders:");

        String inner = prefix + "    ";
        for (int index = 0; index < mLoaders.size(); index++)
        {
            LoaderInfo info = mLoaders.valueAt(index);

            writer.print(prefix);
            writer.print("  #");
            writer.print(mLoaders.keyAt(index));
            writer.print(": ");
            writer.println(info.mLoader);

            if (info.mLoader != null) info.mLoader.dump(inner, fd, writer, args);

            writer.print(inner);
            writer.print("mHaveData=");
            writer.print(info.mHaveData);
            writer.print(" mDeliveredData=");
            writer.print(info.mDeliveredData);
            writer.print(" mStarted=");
            writer.println(info.mStarted);
        }
    }

    @Override
    public String toString()
    {
        return "LoaderManager{" + Integer.toHexString(System.identityHashCode(this)) + " in " + mWho + "}";
    }
}
