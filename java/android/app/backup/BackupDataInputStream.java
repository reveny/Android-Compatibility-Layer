package android.app.backup;

import java.io.IOException;
import java.io.InputStream;

public class BackupDataInputStream extends InputStream
{
    private final BackupDataInput mData;
    private final byte[] mOneByte = new byte[1];

    String key;
    int dataSize;

    BackupDataInputStream(BackupDataInput data)
    {
        mData = data;
    }

    @Override
    public int read() throws IOException
    {
        int read = mData.readEntityData(mOneByte, 0, 1);
        return read <= 0 ? -1 : mOneByte[0] & 0xff;
    }

    @Override
    public int read(byte[] buffer, int offset, int size) throws IOException
    {
        int read = mData.readEntityData(buffer, offset, size);
        return read <= 0 && size > 0 ? -1 : read;
    }

    @Override
    public int read(byte[] buffer) throws IOException
    {
        return read(buffer, 0, buffer.length);
    }

    public String getKey()
    {
        return key;
    }

    public int size()
    {
        return dataSize;
    }
}
