package android.os;

import java.io.File;

public class StatFs
{
    private static final long BLOCK_SIZE = 4096L;

    private long mTotalBytes;
    private long mFreeBytes;
    private long mAvailableBytes;

    public StatFs(String path)
    {
        restat(path);
    }

    public void restat(String path)
    {
        if (path == null) throw new NullPointerException("path");

        File file = new File(path);
        if (!file.exists()) throw new IllegalArgumentException("Invalid path: " + path);

        mTotalBytes = file.getTotalSpace();
        mFreeBytes = file.getFreeSpace();
        mAvailableBytes = file.getUsableSpace();
    }

    public int getBlockSize()
    {
        return (int)BLOCK_SIZE;
    }

    public long getBlockSizeLong()
    {
        return BLOCK_SIZE;
    }

    public int getBlockCount()
    {
        return (int)getBlockCountLong();
    }

    public long getBlockCountLong()
    {
        return mTotalBytes / BLOCK_SIZE;
    }

    public int getFreeBlocks()
    {
        return (int)getFreeBlocksLong();
    }

    public long getFreeBlocksLong()
    {
        return mFreeBytes / BLOCK_SIZE;
    }

    public long getFreeBytes()
    {
        return getFreeBlocksLong() * BLOCK_SIZE;
    }

    public int getAvailableBlocks()
    {
        return (int)getAvailableBlocksLong();
    }

    public long getAvailableBlocksLong()
    {
        return mAvailableBytes / BLOCK_SIZE;
    }

    public long getAvailableBytes()
    {
        return getAvailableBlocksLong() * BLOCK_SIZE;
    }

    public long getTotalBytes()
    {
        return getBlockCountLong() * BLOCK_SIZE;
    }
}
