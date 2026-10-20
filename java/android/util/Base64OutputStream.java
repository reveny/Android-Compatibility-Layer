package android.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Base64OutputStream extends FilterOutputStream
{
    private final int mFlags;
    private final java.io.ByteArrayOutputStream mPending = new java.io.ByteArrayOutputStream();

    private boolean mClosed;

    public Base64OutputStream(OutputStream out, int flags)
    {
        super(out);
        mFlags = flags;
    }

    @Override
    public void write(int oneByte) throws IOException
    {
        mPending.write(oneByte);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException
    {
        mPending.write(buffer, offset, length);
    }

    @Override
    public void flush() throws IOException
    {
        out.flush();
    }

    @Override
    public void close() throws IOException
    {
        if (mClosed) return;

        mClosed = true;

        byte[] encoded = Base64.encode(mPending.toByteArray(), mFlags);
        if (encoded != null) out.write(encoded);

        out.flush();

        if ((mFlags & Base64.NO_CLOSE) == 0) out.close();
    }
}
