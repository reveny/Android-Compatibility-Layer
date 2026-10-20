package runtime;

import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Permissions
{
    private static final String TAG = "Permissions";
    public static final String GRANT_FILE = "permissions.txt";

    private static final String[] DANGEROUS = {
        "android.permission.READ_CALENDAR",          "android.permission.WRITE_CALENDAR",
        "android.permission.CAMERA",                 "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",         "android.permission.GET_ACCOUNTS",
        "android.permission.ACCESS_FINE_LOCATION",   "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",       "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",             "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",         "android.permission.BODY_SENSORS",
        "android.permission.SEND_SMS",               "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",               "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",       "android.permission.READ_MEDIA_AUDIO",
        "android.permission.POST_NOTIFICATIONS",     "android.permission.ACTIVITY_RECOGNITION",
        "android.permission.BLUETOOTH_CONNECT",      "android.permission.BLUETOOTH_SCAN",
        "android.permission.BLUETOOTH_ADVERTISE",    "android.permission.NEARBY_WIFI_DEVICES",
    };

    private static final Set<String> DANGEROUS_SET = new HashSet<>(Arrays.asList(DANGEROUS));

    private static String[] sDeclared = new String[0];
    private static final Set<String> sGranted = new HashSet<>();
    private static final Set<String> sRefused = new HashSet<>();
    private static File sGrantFile;
    private static boolean sReady;

    private Permissions()
    {
    }

    public static synchronized void initialize(String dataDir)
    {
        if (sReady) return;

        sReady = true;

        int count = Bootstrap.nativePermissionCount();
        List<String> declared = new ArrayList<>();

        for (int index = 0; index < count; index++)
        {
            String name = Bootstrap.nativePermissionAt(index);
            if (name != null && !name.isEmpty()) declared.add(name);
        }

        sDeclared = declared.toArray(new String[0]);

        for (String permission : sDeclared)
        {
            if (!isDangerous(permission)) sGranted.add(permission);
        }

        if (dataDir != null && !dataDir.isEmpty())
        {
            sGrantFile = new File(dataDir, GRANT_FILE);

            if (sGrantFile.exists()) readGrantFile();
            else writeGrantFile();
        }

        Log.i(TAG, "the manifest declares " + sDeclared.length + " permission(s), " + sGranted.size()
                   + " of them granted; the rest need a grant before they answer yes");
    }

    private static void readGrantFile()
    {
        if (sGrantFile == null || !sGrantFile.exists()) return;

        BufferedReader reader = null;

        try
        {
            reader = new BufferedReader(new FileReader(sGrantFile));
            String line;

            while ((line = reader.readLine()) != null)
            {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                if (trimmed.startsWith("-"))
                {
                    String refused = trimmed.substring(1).trim();
                    sGranted.remove(refused);
                    sRefused.add(refused);
                    continue;
                }

                sGranted.add(trimmed);
                sRefused.remove(trimmed);
            }
        }
        catch (java.io.IOException failure)
        {
            Log.w(TAG, "the grant file could not be read: " + failure);
        }
        finally
        {
            if (reader != null) try { reader.close(); } catch (java.io.IOException ignored) { }
        }
    }

    private static void writeGrantFile()
    {
        if (sGrantFile == null) return;

        FileWriter writer = null;

        try
        {
            writer = new FileWriter(sGrantFile);
            writer.write("# one granted permission per line; a leading - records an explicit refusal\n");
            writer.write("# what is not listed at all is declared but never asked about\n");

            for (String permission : sGranted) writer.write(permission + "\n");
            for (String permission : sRefused) writer.write("-" + permission + "\n");

            for (String permission : sDeclared)
            {
                if (sGranted.contains(permission) || sRefused.contains(permission)) continue;

                writer.write("-" + permission + "\n");
            }
        }
        catch (java.io.IOException failure)
        {
            Log.w(TAG, "the grant file could not be written: " + failure);
        }
        finally
        {
            if (writer != null) try { writer.close(); } catch (java.io.IOException ignored) { }
        }
    }

    public static boolean isDangerous(String permission)
    {
        return permission != null && DANGEROUS_SET.contains(permission);
    }

    public static synchronized boolean isDeclared(String permission)
    {
        if (permission == null) return false;

        for (String declared : sDeclared)
        {
            if (declared.equals(permission)) return true;
        }

        return false;
    }

    public static synchronized String[] declared()
    {
        return sDeclared.clone();
    }

    public static synchronized int check(String permission)
    {
        if (permission == null) return PackageManager.PERMISSION_DENIED;
        if (!isDeclared(permission)) return PackageManager.PERMISSION_DENIED;

        return sGranted.contains(permission) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
    }

    public static synchronized boolean grant(String permission)
    {
        if (!isDeclared(permission)) return false;

        sGranted.add(permission);
        sRefused.remove(permission);
        writeGrantFile();

        return true;
    }

    public static synchronized boolean deny(String permission)
    {
        if (!isDeclared(permission)) return false;

        sGranted.remove(permission);
        sRefused.add(permission);
        writeGrantFile();

        return true;
    }

    public static synchronized boolean wasRefused(String permission)
    {
        return sRefused.contains(permission);
    }
}
