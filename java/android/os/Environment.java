package android.os;

import java.io.File;

public class Environment
{
    public static final String MEDIA_UNKNOWN = "unknown";
    public static final String MEDIA_REMOVED = "removed";
    public static final String MEDIA_UNMOUNTED = "unmounted";
    public static final String MEDIA_CHECKING = "checking";
    public static final String MEDIA_NOFS = "nofs";
    public static final String MEDIA_MOUNTED = "mounted";
    public static final String MEDIA_MOUNTED_READ_ONLY = "mounted_ro";
    public static final String MEDIA_SHARED = "shared";
    public static final String MEDIA_BAD_REMOVAL = "bad_removal";
    public static final String MEDIA_UNMOUNTABLE = "unmountable";
    public static final String MEDIA_EJECTING = "ejecting";

    public static final String DIRECTORY_MUSIC = "Music";
    public static final String DIRECTORY_PODCASTS = "Podcasts";
    public static final String DIRECTORY_RINGTONES = "Ringtones";
    public static final String DIRECTORY_ALARMS = "Alarms";
    public static final String DIRECTORY_NOTIFICATIONS = "Notifications";
    public static final String DIRECTORY_PICTURES = "Pictures";
    public static final String DIRECTORY_MOVIES = "Movies";
    public static final String DIRECTORY_DOWNLOADS = "Download";
    public static final String DIRECTORY_DCIM = "DCIM";
    public static final String DIRECTORY_DOCUMENTS = "Documents";

    private static File sRoot = new File("/");
    private static File sDataDirectory = new File("/data");
    private static File sExternalStorage = new File("/sdcard");

    public static void setDirectories(File root, File dataDirectory, File externalStorage)
    {
        if (root != null) sRoot = root;
        if (dataDirectory != null) sDataDirectory = dataDirectory;
        if (externalStorage != null) sExternalStorage = externalStorage;
    }

    public static File getRootDirectory()
    {
        return sRoot;
    }

    public static File getDataDirectory()
    {
        return sDataDirectory;
    }

    public static File getDownloadCacheDirectory()
    {
        return ensure(new File(sDataDirectory, "cache"));
    }

    public static File getExternalStorageDirectory()
    {
        return ensure(sExternalStorage);
    }

    public static File getExternalStoragePublicDirectory(String type)
    {
        File external = ensure(sExternalStorage);
        if (type == null || type.isEmpty()) return external;

        return ensure(new File(external, type));
    }

    public static String getExternalStorageState()
    {
        return MEDIA_MOUNTED;
    }

    public static String getExternalStorageState(File path)
    {
        return MEDIA_MOUNTED;
    }

    public static String getStorageState(File path)
    {
        return MEDIA_MOUNTED;
    }

    public static boolean isExternalStorageEmulated()
    {
        return true;
    }

    public static boolean isExternalStorageEmulated(File path)
    {
        return true;
    }

    public static boolean isExternalStorageRemovable()
    {
        return false;
    }

    public static boolean isExternalStorageRemovable(File path)
    {
        return false;
    }

    public static boolean isExternalStorageManager()
    {
        return true;
    }

    public static boolean isExternalStorageLegacy()
    {
        return false;
    }

    private static File ensure(File directory)
    {
        if (!directory.isDirectory()) directory.mkdirs();
        return directory;
    }
}
