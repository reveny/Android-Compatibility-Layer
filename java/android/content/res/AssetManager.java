package android.content.res;

import android.os.ParcelFileDescriptor;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class AssetManager
{
    public static final int ACCESS_UNKNOWN = 0;
    public static final int ACCESS_RANDOM = 1;
    public static final int ACCESS_STREAMING = 2;
    public static final int ACCESS_BUFFER = 3;

    private final Resources mResources;
    private final long mHandle;

    AssetManager(Resources resources, long handle)
    {
        mResources = resources;
        mHandle = handle;
    }

    long getHandle()
    {
        return mHandle;
    }

    public XmlResourceParser openXmlResourceParser(int cookie, String fileName) throws IOException
    {
        runtime.Strict.unimplemented("android.content.res.AssetManager.openXmlResourceParser");
        return null;
    }

    public String[] list(String path) throws IOException
    {
        String[] names = Resources.nativeAssetList(mHandle, path == null ? "" : path);
        return names == null ? new String[0] : names;
    }

    public InputStream open(String fileName) throws IOException
    {
        return open(fileName, ACCESS_STREAMING);
    }

    public InputStream open(String fileName, int accessMode) throws IOException
    {
        if (fileName == null) throw new FileNotFoundException("null");

        long asset = Resources.nativeAssetOpen(mHandle, fileName);
        if (asset == 0) throw new FileNotFoundException(fileName);

        return new AssetInputStream(asset);
    }

    public AssetFileDescriptor openFd(String fileName) throws IOException
    {
        if (fileName == null) throw new FileNotFoundException("null");
        return descriptorFor(fileName, Resources.nativeAssetOpen(mHandle, fileName));
    }

    public InputStream openNonAsset(String fileName) throws IOException
    {
        if (fileName == null) throw new FileNotFoundException("null");

        long asset = Resources.nativeAssetOpenNonAsset(mHandle, fileName);
        if (asset == 0) throw new FileNotFoundException(fileName);

        return new AssetInputStream(asset);
    }

    public AssetFileDescriptor openNonAssetFd(String fileName) throws IOException
    {
        return openNonAssetFd(0, fileName);
    }

    public AssetFileDescriptor openNonAssetFd(int cookie, String fileName) throws IOException
    {
        if (fileName == null) throw new FileNotFoundException("null");
        return descriptorFor(fileName, Resources.nativeAssetOpenNonAsset(mHandle, fileName));
    }

    private AssetFileDescriptor descriptorFor(String fileName, long asset) throws IOException
    {
        if (asset == 0) throw new FileNotFoundException(fileName);

        long[] extent = new long[3];
        FileDescriptor descriptor;
        try
        {
            descriptor = Resources.nativeAssetOpenFd(asset, extent);
        }
        finally
        {
            Resources.nativeAssetClose(asset);
        }

        if (descriptor == null) throw new FileNotFoundException(fileName + " is compressed in the archive, so it has no file descriptor");

        return new AssetFileDescriptor(new ParcelFileDescriptor(descriptor, (int)extent[0]), extent[1], extent[2]);
    }

    public void close()
    {
    }

    public final class AssetInputStream extends InputStream
    {
        private long mAsset;
        private final long mLength;
        private long mConsumed;

        AssetInputStream(long asset)
        {
            mAsset = asset;
            mLength = Resources.nativeAssetLength(asset);
        }

        @Override
        public int read() throws IOException
        {
            byte[] single = new byte[1];
            int got = read(single, 0, 1);
            return got <= 0 ? -1 : single[0] & 0xFF;
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException
        {
            if (mAsset == 0) throw new IOException("asset stream is closed");
            if (buffer == null) throw new NullPointerException("buffer");
            if (offset < 0 || count < 0 || offset > buffer.length || count > buffer.length - offset) throw new IndexOutOfBoundsException();
            if (count == 0) return 0;

            int got = Resources.nativeAssetRead(mAsset, buffer, offset, count);
            if (got == -2) throw new IOException("could not read the asset - the archive entry may be corrupt");
            if (got < 0) return -1;

            mConsumed += got;
            return got;
        }

        @Override
        public int available() throws IOException
        {
            if (mAsset == 0) return 0;
            long remaining = mLength - mConsumed;
            return remaining < 0 ? 0 : (int)Math.min(remaining, Integer.MAX_VALUE);
        }

        @Override
        public void close()
        {
            if (mAsset == 0) return;

            Resources.nativeAssetClose(mAsset);
            mAsset = 0;
        }
    }
}
