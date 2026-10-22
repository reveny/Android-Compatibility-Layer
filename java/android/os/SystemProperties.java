package android.os;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemProperties
{
    public static final int PROP_VALUE_MAX = 91;

    private static final Map<String, String> sProperties = new HashMap<>();
    private static final List<Runnable> sChangeCallbacks = new ArrayList<>();

    static
    {
        String abis = String.join(",", Build.SUPPORTED_ABIS);

        sProperties.put("ro.build.version.sdk", Integer.toString(Build.VERSION.SDK_INT));
        sProperties.put("ro.build.version.release", Build.VERSION.RELEASE);
        sProperties.put("ro.build.version.release_or_codename", Build.VERSION.RELEASE_OR_CODENAME);
        sProperties.put("ro.build.version.codename", Build.VERSION.CODENAME);
        sProperties.put("ro.build.version.incremental", Build.VERSION.INCREMENTAL);
        sProperties.put("ro.build.version.security_patch", Build.VERSION.SECURITY_PATCH);
        sProperties.put("ro.build.version.preview_sdk", Integer.toString(Build.VERSION.PREVIEW_SDK_INT));
        sProperties.put("ro.build.id", Build.ID);
        sProperties.put("ro.build.display.id", Build.DISPLAY);
        sProperties.put("ro.build.type", Build.TYPE);
        sProperties.put("ro.build.tags", Build.TAGS);
        sProperties.put("ro.build.host", Build.HOST);
        sProperties.put("ro.build.user", Build.USER);
        sProperties.put("ro.build.fingerprint", Build.FINGERPRINT);
        sProperties.put("ro.build.date.utc", Long.toString(Build.TIME / 1000L));
        sProperties.put("ro.product.model", Build.MODEL);
        sProperties.put("ro.product.brand", Build.BRAND);
        sProperties.put("ro.product.name", Build.PRODUCT);
        sProperties.put("ro.product.device", Build.DEVICE);
        sProperties.put("ro.product.board", Build.BOARD);
        sProperties.put("ro.product.manufacturer", Build.MANUFACTURER);
        sProperties.put("ro.product.cpu.abi", Build.SUPPORTED_ABIS[0]);
        sProperties.put("ro.product.cpu.abilist", abis);
        sProperties.put("ro.product.cpu.abilist64", String.join(",", Build.SUPPORTED_64_BIT_ABIS));
        sProperties.put("ro.product.cpu.abilist32", String.join(",", Build.SUPPORTED_32_BIT_ABIS));
        sProperties.put("ro.product.first_api_level", Integer.toString(Build.VERSION.SDK_INT));
        sProperties.put("ro.hardware", Build.HARDWARE);
        sProperties.put("ro.board.platform", Build.BOARD);
        sProperties.put("ro.soc.model", Build.SOC_MODEL);
        sProperties.put("ro.soc.manufacturer", Build.SOC_MANUFACTURER);
        sProperties.put("ro.bootloader", Build.BOOTLOADER);
        sProperties.put("ro.serialno", Build.SERIAL);
        sProperties.put("ro.debuggable", "0");
        sProperties.put("ro.secure", "1");
        sProperties.put("ro.adb.secure", "1");
        sProperties.put("ro.kernel.qemu", "0");
        sProperties.put("ro.boot.qemu", "0");
        sProperties.put("ro.crypto.state", "encrypted");
        sProperties.put("ro.crypto.type", "file");
        sProperties.put("ro.opengles.version", "196610");
        sProperties.put("ro.sf.lcd_density", "420");
        sProperties.put("ro.zygote", "zygote64");
        sProperties.put("ro.config.low_ram", "false");
        sProperties.put("ro.boot.verifiedbootstate", "green");
        sProperties.put("ro.boot.flash.locked", "1");
        sProperties.put("ro.treble.enabled", "true");
        sProperties.put("sys.boot_completed", "1");
        sProperties.put("dalvik.vm.heapsize", "512m");
        sProperties.put("dalvik.vm.heapgrowthlimit", "256m");
        sProperties.put("persist.sys.timezone", java.util.TimeZone.getDefault().getID());
    }

    private SystemProperties()
    {
    }

    public static String get(String key)
    {
        return get(key, "");
    }

    public static String get(String key, String def)
    {
        if (key == null) throw new NullPointerException("key");

        synchronized (sProperties)
        {
            String value = sProperties.get(key);
            return value == null || value.isEmpty() ? def : value;
        }
    }

    public static int getInt(String key, int def)
    {
        try
        {
            return Integer.decode(get(key, Integer.toString(def)).trim());
        }
        catch (NumberFormatException malformed)
        {
            return def;
        }
    }

    public static long getLong(String key, long def)
    {
        try
        {
            return Long.decode(get(key, Long.toString(def)).trim());
        }
        catch (NumberFormatException malformed)
        {
            return def;
        }
    }

    public static boolean getBoolean(String key, boolean def)
    {
        String value = get(key, "");
        if (value.isEmpty()) return def;

        if ("1".equals(value) || "y".equals(value) || "yes".equals(value) || "on".equals(value) || "true".equals(value)) return true;
        if ("0".equals(value) || "n".equals(value) || "no".equals(value) || "off".equals(value) || "false".equals(value)) return false;
        return def;
    }

    public static void set(String key, String value)
    {
        if (key == null) throw new NullPointerException("key");
        if (value != null && value.length() > PROP_VALUE_MAX && !key.startsWith("ro."))
        {
            throw new IllegalArgumentException("value of system property '" + key + "' is longer than " + PROP_VALUE_MAX
                                               + " characters: " + value);
        }

        List<Runnable> callbacks;
        synchronized (sProperties)
        {
            if (key.startsWith("ro.") && sProperties.containsKey(key)) return;

            sProperties.put(key, value == null ? "" : value);
            callbacks = new ArrayList<>(sChangeCallbacks);
        }

        for (Runnable callback : callbacks) callback.run();
    }

    public static void addChangeCallback(Runnable callback)
    {
        synchronized (sProperties)
        {
            sChangeCallbacks.add(callback);
        }
    }

    public static void removeChangeCallback(Runnable callback)
    {
        synchronized (sProperties)
        {
            sChangeCallbacks.remove(callback);
        }
    }
}
