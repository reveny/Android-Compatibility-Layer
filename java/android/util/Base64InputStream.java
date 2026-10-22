package android.util;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Base64InputStream extends FilterInputStream
{
    private final int mFlags;
    private final boolean mEncode;

    private byte[] mOutput;
    private int mOutputStart;

    public Base64InputStream(InputStream in, int flags)
    {
        this(in, flags, false);
    }

    public Base64InputStream(InputStream in, int flags, boolean encode)
    {
        super(in);
        mFlags = flags;
        mEncode = encode;
    }

    @Override
    public boolean markSupported()
    {
        return false;
    }

    @Override
    public void mark(int readLimit)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException
    {
        in.close();
        mOutput = new byte[0];
        mOutputStart = 0;
    }

    @Override
    public int available()
    {
        return mOutput == null ? 0 : mOutput.length - mOutputStart;
    }

    @Override
    public long skip(long count) throws IOException
    {
        fill();

        long skipped = Math.max(0, Math.min(count, mOutput.length - mOutputStart));
        mOutputStart += (int)skipped;
        return skipped;
    }

    @Override
    public int read() throws IOException
    {
        fill();

        if (mOutputStart >= mOutput.length) return -1;
        return mOutput[mOutputStart++] & 0xff;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException
    {
        fill();

        if (mOutputStart >= mOutput.length) return -1;

        int count = Math.min(length, mOutput.length - mOutputStart);
        System.arraycopy(mOutput, mOutputStart, buffer, offset, count);
        mOutputStart += count;
        return count;
    }

    private void fill() throws IOException
    {
        if (mOutput != null) return;

        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] chunk = new byte[2048];
        int count;
        while ((count = in.read(chunk)) != -1)
        {
            collected.write(chunk, 0, count);
        }

        byte[] input = collected.toByteArray();
        try
        {
            mOutput = mEncode ? Base64.encode(input, mFlags) : Base64.decode(input, mFlags);
        }
        catch (IllegalArgumentException failure)
        {
            throw new IOException("bad base-64", failure);
        }
        mOutputStart = 0;
    }
}
