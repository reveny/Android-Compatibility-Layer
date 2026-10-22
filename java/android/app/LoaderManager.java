package android.app;

import android.content.Loader;
import android.os.Bundle;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class LoaderManager
{
    static boolean sDebug;

    public interface LoaderCallbacks<D>
    {
        Loader<D> onCreateLoader(int id, Bundle args);

        void onLoadFinished(Loader<D> loader, D data);

        void onLoaderReset(Loader<D> loader);
    }

    public LoaderManager()
    {
    }

    public abstract <D> Loader<D> initLoader(int id, Bundle args, LoaderCallbacks<D> callback);

    public abstract <D> Loader<D> restartLoader(int id, Bundle args, LoaderCallbacks<D> callback);

    public abstract void destroyLoader(int id);

    public abstract <D> Loader<D> getLoader(int id);

    public abstract void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args);

    public static void enableDebugLogging(boolean enabled)
    {
        sDebug = enabled;
    }
}
