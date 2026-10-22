package android.app.backup;

import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class BackupDataOutput
{
    static final int ENTITY_MAGIC = 0x44617461;

    private final DataOutputStream mOutput;
    private final long mQuota;
    private final int mTransportFlags;
    private String mKeyPrefix;
    private int mPendingBytes = -1;

    BackupDataOutput(FileDescriptor descriptor, long quota, int transportFlags)
    {
        this(new FileOutputStream(descriptor), quota, transportFlags);
    }

    BackupDataOutput(OutputStream output, long quota, int transportFlags)
    {
        mOutput = new DataOutputStream(output);
        mQuota = quota;
        mTransportFlags = transportFlags;
    }

    void setKeyPrefix(String keyPrefix)
    {
        mKeyPrefix = keyPrefix;
    }

    public long getQuota()
    {
        return mQuota;
    }

    public int getTransportFlags()
    {
        return mTransportFlags;
    }

    public int writeEntityHeader(String key, int dataSize) throws IOException
    {
        if (key == null) throw new IllegalArgumentException("key");
        if (mPendingBytes > 0) throw new IOException("the previous entity still expects " + mPendingBytes + " byte(s)");

        String fullKey = mKeyPrefix == null ? key : mKeyPrefix + ":" + key;
        byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);

        mOutput.writeInt(ENTITY_MAGIC);
        mOutput.writeInt(keyBytes.length);
        mOutput.write(keyBytes);
        mOutput.writeInt(dataSize);
        mOutput.flush();

        mPendingBytes = dataSize < 0 ? 0 : dataSize;
        return 12 + keyBytes.length;
    }

    public int writeEntityData(byte[] data, int size) throws IOException
    {
        if (mPendingBytes < 0) throw new IOException("writeEntityHeader must be called before writeEntityData");
        if (size > mPendingBytes) throw new IOException("the entity data is " + (size - mPendingBytes) + " byte(s) longer than its header said");

        mOutput.write(data, 0, size);
        mOutput.flush();
        mPendingBytes -= size;
        return size;
    }
}
