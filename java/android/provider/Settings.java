package android.provider;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.HashMap;
import java.util.Map;

public final class Settings
{
    public static final String ACTION_SETTINGS = "android.settings.SETTINGS";
    public static final String ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS";
    public static final String ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION";

    private Settings()
    {
    }

    public static class SettingNotFoundException extends android.util.AndroidException
    {
        public SettingNotFoundException(String message)
        {
            super(message);
        }
    }

    private static final class Table
    {
        private final Uri contentUri;
        private final Map<String, String> values = new HashMap<>();

        Table(String authorityPath)
        {
            contentUri = Uri.parse("content://settings/" + authorityPath);
        }

        synchronized String get(String name)
        {
            return values.get(name);
        }

        synchronized boolean put(String name, String value)
        {
            String previous = value == null ? values.remove(name) : values.put(name, value);
            return value == null ? previous != null : !value.equals(previous);
        }

        void seed(String name, String value)
        {
            values.put(name, value);
        }

        Uri uriFor(String name)
        {
            return Uri.withAppendedPath(contentUri, name);
        }

        boolean write(String name, String value)
        {
            if (!put(name, value)) return true;

            ContentResolver.dispatchChange(uriFor(name), null, 0);
            return true;
        }
    }

    private static int readInt(Table table, String name, int fallback)
    {
        String value = table.get(name);
        if (value == null) return fallback;

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException malformed)
        {
            return fallback;
        }
    }

    private static int requireInt(Table table, String name) throws SettingNotFoundException
    {
        String value = table.get(name);
        if (value == null) throw new SettingNotFoundException(name);

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException malformed)
        {
            throw new SettingNotFoundException(name);
        }
    }

    private static float requireFloat(Table table, String name) throws SettingNotFoundException
    {
        String value = table.get(name);
        if (value == null) throw new SettingNotFoundException(name);

        try
        {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException malformed)
        {
            throw new SettingNotFoundException(name);
        }
    }

    private static long requireLong(Table table, String name) throws SettingNotFoundException
    {
        String value = table.get(name);
        if (value == null) throw new SettingNotFoundException(name);

        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException malformed)
        {
            throw new SettingNotFoundException(name);
        }
    }

    public static final class System
    {
        public static final String ACCELEROMETER_ROTATION = "accelerometer_rotation";
        public static final String USER_ROTATION = "user_rotation";
        public static final String SCREEN_BRIGHTNESS = "screen_brightness";
        public static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
        public static final String SCREEN_OFF_TIMEOUT = "screen_off_timeout";
        public static final String HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled";
        public static final String SOUND_EFFECTS_ENABLED = "sound_effects_enabled";
        public static final String TEXT_SHOW_PASSWORD = "show_password";
        public static final String TIME_12_24 = "time_12_24";
        public static final String DATE_FORMAT = "date_format";
        public static final String FONT_SCALE = "font_scale";
        public static final String VIBRATE_ON = "vibrate_on";

        public static final int SCREEN_BRIGHTNESS_MODE_MANUAL = 0;
        public static final int SCREEN_BRIGHTNESS_MODE_AUTOMATIC = 1;

        private static final Table TABLE = new Table("system");

        public static final Uri CONTENT_URI = TABLE.contentUri;

        static
        {
            TABLE.seed(ACCELEROMETER_ROTATION, "1");
            TABLE.seed(USER_ROTATION, "0");
            TABLE.seed(SCREEN_BRIGHTNESS, "255");
            TABLE.seed(SCREEN_BRIGHTNESS_MODE, "0");
            TABLE.seed(SCREEN_OFF_TIMEOUT, "600000");
            TABLE.seed(HAPTIC_FEEDBACK_ENABLED, "1");
            TABLE.seed(SOUND_EFFECTS_ENABLED, "1");
            TABLE.seed(TIME_12_24, "24");
            TABLE.seed(FONT_SCALE, "1.0");
            TABLE.seed(VIBRATE_ON, "1");
        }

        private System()
        {
        }

        public static Uri getUriFor(String name)
        {
            return TABLE.uriFor(name);
        }

        public static String getString(ContentResolver resolver, String name)
        {
            return TABLE.get(name);
        }

        public static boolean putString(ContentResolver resolver, String name, String value)
        {
            return TABLE.write(name, value);
        }

        public static int getInt(ContentResolver resolver, String name, int fallback)
        {
            return readInt(TABLE, name, fallback);
        }

        public static int getInt(ContentResolver resolver, String name) throws SettingNotFoundException
        {
            return requireInt(TABLE, name);
        }

        public static boolean putInt(ContentResolver resolver, String name, int value)
        {
            return TABLE.write(name, Integer.toString(value));
        }

        public static long getLong(ContentResolver resolver, String name, long fallback)
        {
            String value = TABLE.get(name);
            if (value == null) return fallback;

            try
            {
                return Long.parseLong(value);
            }
            catch (NumberFormatException malformed)
            {
                return fallback;
            }
        }

        public static long getLong(ContentResolver resolver, String name) throws SettingNotFoundException
        {
            return requireLong(TABLE, name);
        }

        public static boolean putLong(ContentResolver resolver, String name, long value)
        {
            return TABLE.write(name, Long.toString(value));
        }

        public static float getFloat(ContentResolver resolver, String name, float fallback)
        {
            String value = TABLE.get(name);
            if (value == null) return fallback;

            try
            {
                return Float.parseFloat(value);
            }
            catch (NumberFormatException malformed)
            {
                return fallback;
            }
        }

        public static float getFloat(ContentResolver resolver, String name) throws SettingNotFoundException
        {
            return requireFloat(TABLE, name);
        }

        public static boolean putFloat(ContentResolver resolver, String name, float value)
        {
            return TABLE.write(name, Float.toString(value));
        }

        public static boolean canWrite(android.content.Context context)
        {
            return true;
        }
    }

    public static final class Secure
    {
        public static final String ANDROID_ID = "android_id";
        public static final String ACCESSIBILITY_ENABLED = "accessibility_enabled";
        public static final String ENABLED_ACCESSIBILITY_SERVICES = "enabled_accessibility_services";
        public static final String DEFAULT_INPUT_METHOD = "default_input_method";
        public static final String INSTALL_NON_MARKET_APPS = "install_non_market_apps";
        public static final String ALLOW_MOCK_LOCATION = "mock_location";
        public static final String LOCATION_MODE = "location_mode";

        private static final Table TABLE = new Table("secure");

        public static final Uri CONTENT_URI = TABLE.contentUri;

        static
        {
            TABLE.seed(ANDROID_ID, "0000000000000000");
            TABLE.seed(ACCESSIBILITY_ENABLED, "0");
            TABLE.seed(ENABLED_ACCESSIBILITY_SERVICES, "");
            TABLE.seed(INSTALL_NON_MARKET_APPS, "0");
            TABLE.seed(ALLOW_MOCK_LOCATION, "0");
            TABLE.seed(LOCATION_MODE, "0");
        }

        private Secure()
        {
        }

        public static Uri getUriFor(String name)
        {
            return TABLE.uriFor(name);
        }

        public static String getString(ContentResolver resolver, String name)
        {
            return TABLE.get(name);
        }

        public static boolean putString(ContentResolver resolver, String name, String value)
        {
            return TABLE.write(name, value);
        }

        public static int getInt(ContentResolver resolver, String name, int fallback)
        {
            return readInt(TABLE, name, fallback);
        }

        public static int getInt(ContentResolver resolver, String name) throws SettingNotFoundException
        {
            return requireInt(TABLE, name);
        }

        public static boolean putInt(ContentResolver resolver, String name, int value)
        {
            return TABLE.write(name, Integer.toString(value));
        }

        public static long getLong(ContentResolver resolver, String name, long fallback)
        {
            String value = TABLE.get(name);
            if (value == null) return fallback;

            try
            {
                return Long.parseLong(value);
            }
            catch (NumberFormatException malformed)
            {
                return fallback;
            }
        }

        public static float getFloat(ContentResolver resolver, String name, float fallback)
        {
            String value = TABLE.get(name);
            if (value == null) return fallback;

            try
            {
                return Float.parseFloat(value);
            }
            catch (NumberFormatException malformed)
            {
                return fallback;
            }
        }
    }

    public static final class Global
    {
        public static final String AIRPLANE_MODE_ON = "airplane_mode_on";
        public static final String ANIMATOR_DURATION_SCALE = "animator_duration_scale";
        public static final String TRANSITION_ANIMATION_SCALE = "transition_animation_scale";
        public static final String WINDOW_ANIMATION_SCALE = "window_animation_scale";
        public static final String DEVELOPMENT_SETTINGS_ENABLED = "development_settings_enabled";
        public static final String ADB_ENABLED = "adb_enabled";
        public static final String AUTO_TIME = "auto_time";
        public static final String DEVICE_NAME = "device_name";

        private static final Table TABLE = new Table("global");

        public static final Uri CONTENT_URI = TABLE.contentUri;

        static
        {
            TABLE.seed(AIRPLANE_MODE_ON, "0");
            TABLE.seed(ANIMATOR_DURATION_SCALE, "1.0");
            TABLE.seed(TRANSITION_ANIMATION_SCALE, "1.0");
            TABLE.seed(WINDOW_ANIMATION_SCALE, "1.0");
            TABLE.seed(DEVELOPMENT_SETTINGS_ENABLED, "0");
            TABLE.seed(ADB_ENABLED, "0");
            TABLE.seed(AUTO_TIME, "1");
        }

        private Global()
        {
        }

        public static Uri getUriFor(String name)
        {
            return TABLE.uriFor(name);
        }

        public static String getString(ContentResolver resolver, String name)
        {
            return TABLE.get(name);
        }

        public static boolean putString(ContentResolver resolver, String name, String value)
        {
            return TABLE.write(name, value);
        }

        public static int getInt(ContentResolver resolver, String name, int fallback)
        {
            return readInt(TABLE, name, fallback);
        }

        public static int getInt(ContentResolver resolver, String name) throws SettingNotFoundException
        {
            return requireInt(TABLE, name);
        }

        public static boolean putInt(ContentResolver resolver, String name, int value)
        {
            return TABLE.write(name, Integer.toString(value));
        }

        public static long getLong(ContentResolver resolver, String name, long fallback)
        {
            String value = TABLE.get(name);
            if (value == null) return fallback;

            try
            {
                return Long.parseLong(value);
            }
            catch (NumberFormatException malformed)
            {
                return fallback;
            }
        }

        public static float getFloat(ContentResolver resolver, String name, float fallback)
        {
            String value = TABLE.get(name);
            if (value == null) return fallback;

            try
            {
                return Float.parseFloat(value);
            }
            catch (NumberFormatException malformed)
            {
                return fallback;
            }
        }
    }
}
