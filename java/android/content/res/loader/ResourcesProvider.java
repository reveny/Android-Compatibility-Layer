package android.content.res.loader;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class ResourcesProvider implements AutoCloseable, Closeable
{
    private final AssetsProvider mAssetsProvider;
    private final ParcelFileDescriptor mDescriptor;
    private final String mPath;
    private boolean mOpen = true;

    ResourcesProvider()
    {
        this(null, null, null);
    }

    private ResourcesProvider(AssetsProvider assetsProvider, ParcelFileDescriptor descriptor, String path)
    {
        mAssetsProvider = assetsProvider;
        mDescriptor = descriptor;
        mPath = path;
    }

    public static ResourcesProvider empty(AssetsProvider assetsProvider)
    {
        return new ResourcesProvider(assetsProvider, null, null);
    }

    public static ResourcesProvider loadFromApk(ParcelFileDescriptor fileDescriptor) throws IOException
    {
        return loadFromApk(fileDescriptor, null);
    }

    public static ResourcesProvider loadFromApk(ParcelFileDescriptor fileDescriptor, AssetsProvider assetsProvider) throws IOException
    {
        if (fileDescriptor == null) throw new NullPointerException("fileDescriptor");
        return new ResourcesProvider(assetsProvider, fileDescriptor.dup(), null);
    }

    public static ResourcesProvider loadFromTable(ParcelFileDescriptor fileDescriptor, AssetsProvider assetsProvider) throws IOException
    {
        if (fileDescriptor == null) throw new NullPointerException("fileDescriptor");
        return new ResourcesProvider(assetsProvider, fileDescriptor.dup(), null);
    }

    public static ResourcesProvider loadFromSplit(Context context, String splitName) throws IOException
    {
        if (context == null || splitName == null) throw new NullPointerException();
        throw new IllegalArgumentException("split '" + splitName + "' is not installed");
    }

    public static ResourcesProvider loadFromDirectory(String path, AssetsProvider assetsProvider) throws IOException
    {
        if (path == null) throw new NullPointerException("path");
        if (!new File(path).isDirectory()) throw new IOException("failed to load resources from " + path);
        return new ResourcesProvider(assetsProvider, null, path);
    }

    @Override
    public void close()
    {
        synchronized (this)
        {
            if (!mOpen) return;
            mOpen = false;
        }

        if (mDescriptor == null) return;

        try
        {
            mDescriptor.close();
        }
        catch (IOException failure)
        {
            android.util.Log.w("ResourcesProvider", "failed to close the provider descriptor: " + failure);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            close();
        }
        finally
        {
            super.finalize();
        }
    }

    @Override
    public String toString()
    {
        return "ResourcesProvider{" + (mPath != null ? mPath : mDescriptor) + "}";
    }
}
