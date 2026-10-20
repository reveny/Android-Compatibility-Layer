package android.os;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ParcelFileDescriptor implements Closeable
{
    private final ParcelFileDescriptor mWrapped;
    private final FileDescriptor mDescriptor;
    private int mFd;

    public ParcelFileDescriptor(FileDescriptor descriptor, int fd)
    {
        mWrapped = null;
        mDescriptor = descriptor;
        mFd = fd;
    }

    public ParcelFileDescriptor(ParcelFileDescriptor wrapped)
    {
        mWrapped = wrapped;
        mDescriptor = null;
        mFd = -1;
    }

    public FileDescriptor getFileDescriptor()
    {
        if (mWrapped != null) return mWrapped.getFileDescriptor();
        return mDescriptor;
    }

    public int getFd()
    {
        if (mWrapped != null) return mWrapped.getFd();
        return mFd;
    }

    public long getStatSize()
    {
        return -1;
    }

    public int detachFd()
    {
        if (mWrapped != null) return mWrapped.detachFd();

        int detached = mFd;
        mFd = -1;
        if (detached >= 0) nativeInvalidate(mDescriptor);
        return detached;
    }

    @Override
    public void close() throws IOException
    {
        if (mWrapped != null)
        {
            mWrapped.close();
            return;
        }

        if (mFd < 0) return;

        int closing = mFd;
        mFd = -1;
        if (!nativeClose(mDescriptor, closing)) throw new IOException("could not close descriptor " + closing);
    }

    @Override
    public String toString()
    {
        return "{ParcelFileDescriptor: " + getFd() + "}";
    }

    static native boolean nativeClose(FileDescriptor descriptor, int fd);

    static native void nativeInvalidate(FileDescriptor descriptor);

    public static class AutoCloseInputStream extends FileInputStream
    {
        private final ParcelFileDescriptor mOwner;

        public AutoCloseInputStream(ParcelFileDescriptor owner)
        {
            super(owner.getFileDescriptor());
            mOwner = owner;
        }

        protected ParcelFileDescriptor owner()
        {
            return mOwner;
        }

        @Override
        public void close() throws IOException
        {
            try
            {
                super.close();
            }
            finally
            {
                mOwner.close();
            }
        }
    }

    public static class AutoCloseOutputStream extends FileOutputStream
    {
        private final ParcelFileDescriptor mOwner;

        public AutoCloseOutputStream(ParcelFileDescriptor owner)
        {
            super(owner.getFileDescriptor());
            mOwner = owner;
        }

        @Override
        public void close() throws IOException
        {
            try
            {
                super.close();
            }
            finally
            {
                mOwner.close();
            }
        }
    }
}
