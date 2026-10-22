package android.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class AtomicFile
{
    private static final String LOG_TAG = "AtomicFile";

    private final File mBaseName;
    private final File mNewName;
    private final File mLegacyBackupName;

    public AtomicFile(File baseName)
    {
        mBaseName = baseName;
        mNewName = new File(baseName.getPath() + ".new");
        mLegacyBackupName = new File(baseName.getPath() + ".bak");
    }

    public File getBaseFile()
    {
        return mBaseName;
    }

    public void delete()
    {
        mBaseName.delete();
        mNewName.delete();
        mLegacyBackupName.delete();
    }

    public FileOutputStream startWrite() throws IOException
    {
        if (mLegacyBackupName.exists()) rename(mLegacyBackupName, mBaseName);

        try
        {
            return new FileOutputStream(mNewName);
        }
        catch (FileNotFoundException failure)
        {
            File parent = mNewName.getParentFile();
            if (parent == null || (!parent.isDirectory() && !parent.mkdirs()))
            {
                throw new IOException("Failed to create directory for " + mNewName);
            }

            try
            {
                return new FileOutputStream(mNewName);
            }
            catch (FileNotFoundException retryFailure)
            {
                throw new IOException("Failed to create new file " + mNewName, retryFailure);
            }
        }
    }

    public void finishWrite(FileOutputStream stream)
    {
        if (stream == null) return;

        if (!sync(stream)) Log.e(LOG_TAG, "Failed to sync file output stream");

        try
        {
            stream.close();
        }
        catch (IOException failure)
        {
            Log.e(LOG_TAG, "Failed to close file output stream", failure);
        }

        rename(mNewName, mBaseName);
    }

    public void failWrite(FileOutputStream stream)
    {
        if (stream == null) return;

        if (!sync(stream)) Log.e(LOG_TAG, "Failed to sync file output stream");

        try
        {
            stream.close();
        }
        catch (IOException failure)
        {
            Log.e(LOG_TAG, "Failed to close file output stream", failure);
        }

        if (!mNewName.delete()) Log.e(LOG_TAG, "Failed to delete new file " + mNewName);
    }

    public FileInputStream openRead() throws FileNotFoundException
    {
        if (mLegacyBackupName.exists()) rename(mLegacyBackupName, mBaseName);

        if (mNewName.exists() && mBaseName.exists())
        {
            if (!mNewName.delete()) Log.e(LOG_TAG, "Failed to delete outdated new file " + mNewName);
        }

        return new FileInputStream(mBaseName);
    }

    public long getLastModifiedTime()
    {
        if (mLegacyBackupName.exists()) return mLegacyBackupName.lastModified();

        return mBaseName.lastModified();
    }

    public byte[] readFully() throws IOException
    {
        try (FileInputStream stream = openRead())
        {
            int position = 0;
            int available = stream.available();
            byte[] data = new byte[Math.max(available, 16)];

            while (true)
            {
                int amount = stream.read(data, position, data.length - position);
                if (amount <= 0)
                {
                    if (position == data.length) return data;

                    byte[] trimmed = new byte[position];
                    System.arraycopy(data, 0, trimmed, 0, position);
                    return trimmed;
                }

                position += amount;
                available = stream.available();
                if (available > data.length - position || position == data.length)
                {
                    byte[] grown = new byte[position + Math.max(available, 16)];
                    System.arraycopy(data, 0, grown, 0, position);
                    data = grown;
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return "AtomicFile[" + mBaseName + "]";
    }

    private static boolean sync(FileOutputStream stream)
    {
        try
        {
            stream.getFD().sync();
            return true;
        }
        catch (IOException failure)
        {
            return false;
        }
    }

    private static void rename(File source, File target)
    {
        if (target.isDirectory() && !target.delete()) Log.e(LOG_TAG, "Failed to delete file which is a directory " + target);

        if (!source.renameTo(target)) Log.e(LOG_TAG, "Failed to rename " + source + " to " + target);
    }
}
