package android.app.backup;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BackupDataInput
{
    private final DataInputStream mInput;
    private String mKey;
    private int mDataSize = -1;
    private int mRemaining;

    BackupDataInput(FileDescriptor descriptor)
    {
        this(new FileInputStream(descriptor));
    }

    BackupDataInput(InputStream input)
    {
        mInput = new DataInputStream(input);
    }

    public boolean readNextHeader() throws IOException
    {
        if (mRemaining > 0) skipEntityData();

        int magic;
        try
        {
            magic = mInput.readInt();
        }
        catch (EOFException end)
        {
            mKey = null;
            mDataSize = -1;
            return false;
        }

        if (magic != BackupDataOutput.ENTITY_MAGIC) throw new IOException("malformed backup data: bad entity header 0x" + Integer.toHexString(magic));

        byte[] key = new byte[mInput.readInt()];
        mInput.readFully(key);
        mKey = new String(key, StandardCharsets.UTF_8);
        mDataSize = mInput.readInt();
        mRemaining = mDataSize < 0 ? 0 : mDataSize;
        return true;
    }

    public String getKey()
    {
        if (mKey == null) throw new IllegalStateException("Entity header not read");
        return mKey;
    }

    public int getDataSize()
    {
        if (mKey == null) throw new IllegalStateException("Entity header not read");
        return mDataSize;
    }

    public int readEntityData(byte[] data, int offset, int size) throws IOException
    {
        if (mKey == null) throw new IllegalStateException("Entity header not read");
        if (mRemaining == 0) return 0;

        int read = mInput.read(data, offset, Math.min(size, mRemaining));
        if (read < 0) throw new IOException("the backup data ended inside an entity");

        mRemaining -= read;
        return read;
    }

    public void skipEntityData() throws IOException
    {
        if (mKey == null) throw new IllegalStateException("Entity header not read");

        while (mRemaining > 0)
        {
            int skipped = (int)mInput.skip(mRemaining);
            if (skipped <= 0)
            {
                if (mInput.read() < 0) throw new IOException("the backup data ended inside an entity");
                skipped = 1;
            }

            mRemaining -= skipped;
        }
    }
}
