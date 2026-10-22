package android.text.format;

import android.content.Context;

public final class Formatter
{
    private static final int FLAG_SHORTER = 1;
    private static final String[] SUFFIXES = {"B", "kB", "MB", "GB", "TB", "PB"};

    public Formatter()
    {
    }

    public static String formatFileSize(Context context, long sizeBytes)
    {
        return formatBytes(sizeBytes, 0);
    }

    public static String formatShortFileSize(Context context, long sizeBytes)
    {
        return formatBytes(sizeBytes, FLAG_SHORTER);
    }

    public static String formatIpAddress(int ipv4Address)
    {
        return (ipv4Address & 0xFF) + "." + ((ipv4Address >> 8) & 0xFF) + "." + ((ipv4Address >> 16) & 0xFF) + "."
            + ((ipv4Address >> 24) & 0xFF);
    }

    private static String formatBytes(long sizeBytes, int flags)
    {
        boolean negative = sizeBytes < 0;
        float result = negative ? -(float)sizeBytes : (float)sizeBytes;
        int suffix = 0;
        long multiplier = 1;

        while (result > 900 && suffix < SUFFIXES.length - 1)
        {
            suffix++;
            multiplier *= 1000;
            result = result / 1000;
        }

        String format;
        if (multiplier == 1 || result >= 100) format = "%.0f";
        else if (result < 1) format = "%.2f";
        else if (result < 10) format = (flags & FLAG_SHORTER) != 0 ? "%.1f" : "%.2f";
        else format = (flags & FLAG_SHORTER) != 0 ? "%.0f" : "%.2f";

        if (negative) result = -result;

        return String.format(java.util.Locale.getDefault(), format, result) + " " + SUFFIXES[suffix];
    }
}
