package android.os;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class FileUtils
{
    private static final int BUFFER_BYTES = 8192;

    private FileUtils()
    {
    }

    public static long copy(InputStream from, OutputStream to) throws IOException
    {
        return copy(from, to, null, null, null);
    }

    public static long copy(InputStream from, OutputStream to, CancellationSignal signal, java.util.concurrent.Executor executor,
                            ProgressListener listener) throws IOException
    {
        if (from == null || to == null) return 0;

        byte[] buffer = new byte[BUFFER_BYTES];
        long total = 0;

        while (true)
        {
            if (signal != null) signal.throwIfCanceled();

            int read = from.read(buffer);
            if (read < 0) break;

            to.write(buffer, 0, read);
            total += read;

            if (listener != null) listener.onProgress(total);
        }

        return total;
    }

    public static long copy(File from, File to) throws IOException
    {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to))
        {
            return copy(in, out);
        }
    }

    public static boolean contains(File directory, File file)
    {
        if (directory == null || file == null) return false;

        String directoryPath = directory.getAbsolutePath();
        String filePath = file.getAbsolutePath();

        if (directoryPath.equals(filePath)) return true;

        if (!directoryPath.endsWith("/")) directoryPath = directoryPath + "/";

        return filePath.startsWith(directoryPath);
    }

    public static boolean deleteContents(File directory)
    {
        if (directory == null) return true;

        File[] entries = directory.listFiles();
        boolean removed = true;

        if (entries == null) return true;

        for (File entry : entries)
        {
            if (entry.isDirectory()) removed &= deleteContents(entry);

            if (!entry.delete()) removed = false;
        }

        return removed;
    }

    public static boolean deleteContentsAndDir(File directory)
    {
        return deleteContents(directory) && (directory == null || directory.delete());
    }

    public interface ProgressListener
    {
        void onProgress(long progress);
    }
}
