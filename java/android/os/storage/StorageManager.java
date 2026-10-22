package android.os.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.ProxyFileDescriptorCallback;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

public class StorageManager
{
    public static final String ACTION_MANAGE_STORAGE = "android.os.storage.action.MANAGE_STORAGE";
    public static final String ACTION_CLEAR_APP_CACHE = "android.os.storage.action.CLEAR_APP_CACHE";
    public static final String EXTRA_REQUESTED_BYTES = "android.os.storage.extra.REQUESTED_BYTES";
    public static final String EXTRA_UUID = "android.os.storage.extra.UUID";
    public static final UUID UUID_DEFAULT = UUID.fromString("41217664-9172-527a-b3d5-edabb50a7d69");

    private static final String TAG = "StorageManager";
    private static final String MEDIA_AUTHORITY = "media";
    private static final long MINIMUM_CACHE_QUOTA_BYTES = 64L * 1024L * 1024L;

    private final Context mContext;
    private final List<StorageVolumeCallback> mCallbacks = new ArrayList<>();

    public static class StorageVolumeCallback
    {
        public StorageVolumeCallback()
        {
        }

        public void onStateChanged(StorageVolume volume)
        {
        }
    }

    public StorageManager(Context context)
    {
        mContext = context;
    }

    public void registerStorageVolumeCallback(Executor executor, StorageVolumeCallback callback)
    {
        if (executor == null || callback == null) throw new NullPointerException("executor and callback are required");

        synchronized (mCallbacks)
        {
            mCallbacks.add(callback);
        }
    }

    public void unregisterStorageVolumeCallback(StorageVolumeCallback callback)
    {
        synchronized (mCallbacks)
        {
            mCallbacks.remove(callback);
        }
    }

    public boolean mountObb(String rawPath, String key, OnObbStateChangeListener listener)
    {
        if (rawPath == null) throw new NullPointerException("rawPath cannot be null");
        if (listener != null) listener.onObbStateChange(rawPath, OnObbStateChangeListener.ERROR_COULD_NOT_MOUNT);
        return false;
    }

    public boolean unmountObb(String rawPath, boolean force, OnObbStateChangeListener listener)
    {
        if (rawPath == null) throw new NullPointerException("rawPath cannot be null");
        if (listener != null) listener.onObbStateChange(rawPath, OnObbStateChangeListener.ERROR_NOT_MOUNTED);
        return false;
    }

    public boolean isObbMounted(String rawPath)
    {
        return false;
    }

    public String getMountedObbPath(String rawPath)
    {
        return null;
    }

    public android.app.PendingIntent getManageSpaceActivityIntent(String packageName, int requestCode)
    {
        return null;
    }

    public UUID getUuidForPath(File path) throws IOException
    {
        if (path == null) throw new NullPointerException("path");
        return UUID_DEFAULT;
    }

    public boolean isAllocationSupported(FileDescriptor descriptor)
    {
        return true;
    }

    public StorageVolume getStorageVolume(File file)
    {
        if (file == null) return null;

        String path = canonical(file);
        for (File root : roots())
        {
            String rootPath = canonical(root);
            if (path.equals(rootPath) || path.startsWith(rootPath + File.separator)) return getPrimaryStorageVolume();
        }
        return null;
    }

    public StorageVolume getStorageVolume(Uri uri)
    {
        if (uri == null) throw new NullPointerException("uri");

        List<String> segments = uri.getPathSegments();
        String volume = segments == null || segments.isEmpty() ? null : segments.get(0);
        if (MEDIA_AUTHORITY.equals(uri.getAuthority()) && ("external".equals(volume) || "external_primary".equals(volume)))
        {
            return getPrimaryStorageVolume();
        }

        throw new IllegalStateException("Unknown volume for " + uri);
    }

    public List<StorageVolume> getStorageVolumes()
    {
        List<StorageVolume> volumes = new ArrayList<>();
        volumes.add(getPrimaryStorageVolume());
        return volumes;
    }

    public List<StorageVolume> getStorageVolumesIncludingSharedProfiles()
    {
        return getStorageVolumes();
    }

    public List<StorageVolume> getRecentStorageVolumes()
    {
        return getStorageVolumes();
    }

    public StorageVolume getPrimaryStorageVolume()
    {
        return StorageVolume.primary(Environment.getExternalStorageDirectory());
    }

    public StorageVolume[] getVolumeList()
    {
        return new StorageVolume[] {getPrimaryStorageVolume()};
    }

    public boolean isEncrypted(File file)
    {
        return true;
    }

    public static boolean isFileEncryptedNativeOrEmulated()
    {
        return true;
    }

    public ParcelFileDescriptor openProxyFileDescriptor(int mode, ProxyFileDescriptorCallback callback, Handler handler)
        throws IOException
    {
        throw new IOException("proxy file descriptors are not supported on this device");
    }

    public long getCacheQuotaBytes(UUID storageUuid) throws IOException
    {
        long usable = dataDirectory().getUsableSpace();
        return Math.max(MINIMUM_CACHE_QUOTA_BYTES, usable / 10L);
    }

    public long getCacheSizeBytes(UUID storageUuid) throws IOException
    {
        long total = 0L;
        if (mContext == null) return total;

        total += sizeOf(mContext.getCacheDir());
        total += sizeOf(mContext.getCodeCacheDir());
        File externalCache = mContext.getExternalCacheDir();
        if (externalCache != null) total += sizeOf(externalCache);
        return total;
    }

    public long getAllocatableBytes(UUID storageUuid) throws IOException
    {
        return dataDirectory().getUsableSpace();
    }

    public void allocateBytes(UUID storageUuid, long bytes) throws IOException
    {
        long allocatable = getAllocatableBytes(storageUuid);
        if (bytes > allocatable) throw new IOException("Failed to allocate " + bytes + " bytes; only " + allocatable + " available");
    }

    public void allocateBytes(FileDescriptor descriptor, long bytes) throws IOException
    {
        allocateBytes(UUID_DEFAULT, bytes);
    }

    public void setCacheBehaviorGroup(File path, boolean group) throws IOException
    {
    }

    public boolean isCacheBehaviorGroup(File path) throws IOException
    {
        return false;
    }

    public void setCacheBehaviorTombstone(File path, boolean tombstone) throws IOException
    {
    }

    public boolean isCacheBehaviorTombstone(File path) throws IOException
    {
        return false;
    }

    public boolean isCheckpointSupported()
    {
        return false;
    }

    private File dataDirectory()
    {
        if (mContext != null)
        {
            File dataDir = mContext.getDataDir();
            if (dataDir != null) return dataDir;
        }
        return Environment.getDataDirectory();
    }

    private List<File> roots()
    {
        List<File> roots = new ArrayList<>();
        roots.add(Environment.getExternalStorageDirectory());

        if (mContext != null)
        {
            File external = mContext.getExternalFilesDir(null);
            if (external != null) roots.add(external);

            File externalCache = mContext.getExternalCacheDir();
            if (externalCache != null) roots.add(externalCache);
        }
        return roots;
    }

    private static String canonical(File file)
    {
        try
        {
            return file.getCanonicalPath();
        }
        catch (IOException unresolvable)
        {
            return file.getAbsolutePath();
        }
    }

    private static long sizeOf(File file)
    {
        if (file == null || !file.exists()) return 0L;
        if (file.isFile()) return file.length();

        long total = 0L;
        File[] children = file.listFiles();
        if (children == null) return total;

        for (File child : children) total += sizeOf(child);
        return total;
    }
}
