package android.app.usage;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class StorageStatsManager
{
    private final Context mContext;

    public StorageStatsManager(Context context)
    {
        mContext = context;
    }

    public long getTotalBytes(UUID storageUuid) throws IOException
    {
        return volume().getTotalSpace();
    }

    public long getFreeBytes(UUID storageUuid) throws IOException
    {
        return volume().getUsableSpace();
    }

    public StorageStats queryStatsForPackage(UUID storageUuid, String packageName, UserHandle user)
        throws IOException, PackageManager.NameNotFoundException
    {
        if (packageName == null || !packageName.equals(mContext.getPackageName())) throw new PackageManager.NameNotFoundException(packageName);

        return ownStats();
    }

    public StorageStats queryStatsForUid(UUID storageUuid, int uid) throws IOException
    {
        return ownStats();
    }

    public StorageStats queryStatsForUser(UUID storageUuid, UserHandle user) throws IOException
    {
        return ownStats();
    }

    public ExternalStorageStats queryExternalStatsForUser(UUID storageUuid, UserHandle user) throws IOException
    {
        File external = mContext.getExternalFilesDir(null);
        File externalRoot = external == null ? null : external.getParentFile();
        long total = sizeOf(externalRoot) + sizeOf(mContext.getExternalCacheDir());
        return new ExternalStorageStats(total, 0, 0, 0, total);
    }

    private File volume()
    {
        File data = mContext.getDataDir();
        return data == null ? new File("/") : data;
    }

    private StorageStats ownStats()
    {
        String codePath = mContext.getPackageCodePath();
        long appBytes = codePath == null ? 0 : new File(codePath).length();
        long cacheBytes = sizeOf(mContext.getCacheDir()) + sizeOf(mContext.getCodeCacheDir());
        long externalCacheBytes = sizeOf(mContext.getExternalCacheDir());
        long dataBytes = sizeOf(mContext.getDataDir());
        return new StorageStats(appBytes, dataBytes, cacheBytes, externalCacheBytes);
    }

    private static long sizeOf(File file)
    {
        if (file == null || !file.exists()) return 0;
        if (!file.isDirectory()) return file.length();

        File[] children = file.listFiles();
        if (children == null) return 0;

        long total = 0;
        for (File child : children)
        {
            if (java.nio.file.Files.isSymbolicLink(child.toPath())) continue;

            total += sizeOf(child);
        }

        return total;
    }
}
