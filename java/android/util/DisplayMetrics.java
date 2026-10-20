package android.util;

public class DisplayMetrics
{
    public static final int DENSITY_LOW = 120;
    public static final int DENSITY_MEDIUM = 160;
    public static final int DENSITY_HIGH = 240;
    public static final int DENSITY_XHIGH = 320;
    public static final int DENSITY_XXHIGH = 480;
    public static final int DENSITY_XXXHIGH = 640;
    public static final int DENSITY_DEFAULT = DENSITY_MEDIUM;

    public static final int DENSITY_DEVICE_STABLE = densityDeviceStable();

    private static int densityDeviceStable()
    {
        String value = runtime.Settings.get("density");

        if (value != null && !value.isEmpty())
        {
            try
            {
                int parsed = Integer.parseInt(value.trim());
                if (parsed > 0) return parsed;
            }
            catch (NumberFormatException ignored)
            {
            }
        }

        return DENSITY_MEDIUM;
    }

    public int widthPixels;
    public int heightPixels;
    public float density = 1.0f;
    public int densityDpi = DENSITY_DEFAULT;
    public float scaledDensity = 1.0f;
    public float xdpi = DENSITY_DEFAULT;
    public float ydpi = DENSITY_DEFAULT;

    public void setToDefaults()
    {
        widthPixels = 0;
        heightPixels = 0;
        density = 1.0f;
        densityDpi = DENSITY_DEFAULT;
        scaledDensity = 1.0f;
        xdpi = DENSITY_DEFAULT;
        ydpi = DENSITY_DEFAULT;
    }

    public void setTo(DisplayMetrics other)
    {
        widthPixels = other.widthPixels;
        heightPixels = other.heightPixels;
        density = other.density;
        densityDpi = other.densityDpi;
        scaledDensity = other.scaledDensity;
        xdpi = other.xdpi;
        ydpi = other.ydpi;
    }

    @Override
    public String toString()
    {
        return "DisplayMetrics{" + widthPixels + "x" + heightPixels + " density=" + density + " dpi=" + densityDpi + "}";
    }
}
