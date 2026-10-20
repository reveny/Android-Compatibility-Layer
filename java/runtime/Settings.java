package runtime;

public final class Settings
{
    private Settings()
    {
    }

    private static native String nativeLookup(String name);

    public static String get(String name)
    {
        String value = nativeLookup(name);
        if (value == null || value.isEmpty()) return null;

        return value;
    }

    public static boolean has(String name)
    {
        return get(name) != null;
    }

    public static boolean enabled(String name)
    {
        String value = get(name);

        return value != null && !"0".equals(value);
    }

    public static int integer(String name, int fallback)
    {
        String value = get(name);
        if (value == null) return fallback;

        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException malformed)
        {
            throw new IllegalArgumentException(name + " is not an integer: '" + value + "'", malformed);
        }
    }

    public static boolean tracing(String area)
    {
        String areas = get("trace");
        if (areas == null) return false;

        for (String candidate : areas.split("[, ]"))
        {
            if (candidate.equals("all") || candidate.equals(area)) return true;
        }

        return false;
    }
}
