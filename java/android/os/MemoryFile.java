package android.os;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MemoryFile
{
    private final String mName;
    private byte[] mData;
    private boolean mAllowPurging = false;

    public MemoryFile(String name, int length) throws IOException
    {
        if (length < 0) throw new IOException("Invalid length: " + length);

        mName = name;
        mData = new byte[length];
    }

    public void close()
    {
        mData = null;
    }

    public int length()
    {
        return mData == null ? 0 : mData.length;
    }

    public boolean isPurgingAllowed()
    {
        return mAllowPurging;
    }

    public synchronized boolean allowPurging(boolean allowPurging) throws IOException
    {
        boolean oldValue = mAllowPurging;
        mAllowPurging = allowPurging;
        return oldValue;
    }

    public InputStream getInputStream()
    {
        return new MemoryInputStream();
    }

    public OutputStream getOutputStream()
    {
        return new MemoryOutputStream();
    }

    public int readBytes(byte[] buffer, int srcOffset, int destOffset, int count) throws IOException
    {
        byte[] data = checkOpen();
        if (destOffset < 0 || destOffset > buffer.length || count < 0 || count > buffer.length - destOffset || srcOffset < 0
            || srcOffset > data.length || count > data.length - srcOffset)
        {
            throw new IndexOutOfBoundsException();
        }

        System.arraycopy(data, srcOffset, buffer, destOffset, count);
        return count;
    }

    public void writeBytes(byte[] buffer, int srcOffset, int destOffset, int count) throws IOException
    {
        byte[] data = checkOpen();
        if (srcOffset < 0 || srcOffset > buffer.length || count < 0 || count > buffer.length - srcOffset || destOffset < 0
            || destOffset > data.length || count > data.length - destOffset)
        {
            throw new IndexOutOfBoundsException();
        }

        System.arraycopy(buffer, srcOffset, data, destOffset, count);
    }

    private byte[] checkOpen() throws IOException
    {
        byte[] data = mData;
        if (data == null) throw new IOException("MemoryFile " + mName + " is closed");
        return data;
    }

    private class MemoryInputStream extends InputStream
    {
        private int mMark = 0;
        private int mOffset = 0;
        private byte[] mSingleByte;

        @Override
        public int available() throws IOException
        {
            if (mOffset >= length()) return 0;
            return length() - mOffset;
        }

        @Override
        public boolean markSupported()
        {
            return true;
        }

        @Override
        public void mark(int readLimit)
        {
            mMark = mOffset;
        }

        @Override
        public void reset() throws IOException
        {
            mOffset = mMark;
        }

        @Override
        public int read() throws IOException
        {
            if (mSingleByte == null) mSingleByte = new byte[1];

            int result = read(mSingleByte, 0, 1);
            if (result != 1) return -1;
            return mSingleByte[0] & 0xff;
        }

        @Override
        public int read(byte[] buffer, int offset, int count) throws IOException
        {
            if (offset < 0 || count < 0 || offset + count > buffer.length) throw new IndexOutOfBoundsException();

            count = Math.min(count, available());
            if (count < 1) return -1;

            int result = readBytes(buffer, mOffset, offset, count);
            if (result > 0) mOffset += result;
            return result;
        }

        @Override
        public long skip(long n) throws IOException
        {
            if (mOffset + n > length()) n = length() - mOffset;
            mOffset += (int)n;
            return n;
        }
    }

    private class MemoryOutputStream extends OutputStream
    {
        private int mOffset = 0;
        private byte[] mSingleByte;

        @Override
        public void write(byte[] buffer, int offset, int count) throws IOException
        {
            writeBytes(buffer, offset, mOffset, count);
            mOffset += count;
        }

        @Override
        public void write(int oneByte) throws IOException
        {
            if (mSingleByte == null) mSingleByte = new byte[1];

            mSingleByte[0] = (byte)oneByte;
            write(mSingleByte, 0, 1);
        }
    }
}
