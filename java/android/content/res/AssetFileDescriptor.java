package android.content.res;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AssetFileDescriptor implements Closeable
{
    public static final long UNKNOWN_LENGTH = -1;

    private final ParcelFileDescriptor mDescriptor;
    private final long mStartOffset;
    private final long mLength;
    private final Bundle mExtras;

    public AssetFileDescriptor(ParcelFileDescriptor descriptor, long startOffset, long length)
    {
        this(descriptor, startOffset, length, null);
    }

    public AssetFileDescriptor(ParcelFileDescriptor descriptor, long startOffset, long length, Bundle extras)
    {
        if (descriptor == null) throw new IllegalArgumentException("fd must not be null");
        if (length < 0 && startOffset != 0) throw new IllegalArgumentException("startOffset must be 0 when using UNKNOWN_LENGTH");

        mDescriptor = descriptor;
        mStartOffset = startOffset;
        mLength = length;
        mExtras = extras;
    }

    public ParcelFileDescriptor getParcelFileDescriptor()
    {
        return mDescriptor;
    }

    public FileDescriptor getFileDescriptor()
    {
        return mDescriptor.getFileDescriptor();
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public long getStartOffset()
    {
        return mStartOffset;
    }

    public long getLength()
    {
        if (mLength >= 0) return mLength;

        long total = mDescriptor.getStatSize();
        if (total < 0) return UNKNOWN_LENGTH;
        return total - mStartOffset;
    }

    public long getDeclaredLength()
    {
        return mLength;
    }

    public FileInputStream createInputStream() throws IOException
    {
        return new AutoCloseInputStream(this);
    }

    public FileOutputStream createOutputStream() throws IOException
    {
        return new AutoCloseOutputStream(this);
    }

    @Override
    public void close() throws IOException
    {
        mDescriptor.close();
    }

    @Override
    public String toString()
    {
        return "{AssetFileDescriptor: " + mDescriptor + " start=" + mStartOffset + " len=" + mLength + "}";
    }

    public static class AutoCloseInputStream extends ParcelFileDescriptor.AutoCloseInputStream
    {
        private long mRemaining;

        public AutoCloseInputStream(AssetFileDescriptor owner) throws IOException
        {
            super(owner.getParcelFileDescriptor());

            long length = owner.getDeclaredLength();
            mRemaining = length < 0 ? Long.MAX_VALUE : length;

            long skipped = 0;
            while (skipped < owner.getStartOffset())
            {
                long step = super.skip(owner.getStartOffset() - skipped);
                if (step <= 0) throw new IOException("could not seek to " + owner.getStartOffset());
                skipped += step;
            }
        }

        @Override
        public int read() throws IOException
        {
            if (mRemaining <= 0) return -1;

            int value = super.read();
            if (value >= 0) mRemaining -= 1;
            return value;
        }

        @Override
        public int read(byte[] buffer) throws IOException
        {
            return read(buffer, 0, buffer.length);
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException
        {
            if (mRemaining <= 0) return -1;
            if (count > mRemaining) count = (int)mRemaining;

            int got = super.read(buffer, offset, count);
            if (got > 0) mRemaining -= got;
            return got;
        }

        @Override
        public long skip(long count) throws IOException
        {
            if (count <= 0) return 0;
            if (count > mRemaining) count = mRemaining;

            long stepped = super.skip(count);
            if (stepped > 0) mRemaining -= stepped;
            return stepped;
        }

        @Override
        public int available() throws IOException
        {
            long known = Math.min(mRemaining, super.available());
            return known > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)known;
        }
    }

    public static class AutoCloseOutputStream extends ParcelFileDescriptor.AutoCloseOutputStream
    {
        public AutoCloseOutputStream(AssetFileDescriptor owner) throws IOException
        {
            super(owner.getParcelFileDescriptor());

            if (owner.getStartOffset() > 0) throw new IOException("cannot write to an asset at a non-zero offset");
        }
    }
}
