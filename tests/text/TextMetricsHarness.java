package acl;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import java.util.Locale;

public class TextMetricsHarness
{
    private static final String FONT_PATH = "/data/local/tmp/acl/fonts/Roboto-Regular.ttf";

    private static final float[] SIZES = {12.0f, 14.0f, 16.0f, 20.0f, 24.0f, 32.0f, 48.0f};

    private static final String[] CORPUS = {
        "Hello World!",
        "AV",
        "To",
        "Yo",
        "Ta",
        "0123456789",
        "iiiiiiiiii",
        "WWWWWWWWWW",
        "The quick brown fox jumps over the lazy dog",
    };

    private static final String[] FAMILIES = {"sans-serif", "sans-serif-light", "sans-serif-medium", "monospace"};

    private static void installSystemFontMap()
    {
        try
        {
            java.lang.reflect.Method loader = Typeface.class.getDeclaredMethod("loadPreinstalledSystemFontMap");
            loader.setAccessible(true);
            loader.invoke(null);
        }
        catch (Throwable unavailable)
        {
            System.out.println("# system_font_map_unavailable " + unavailable);
        }
    }

    public static void main(String[] args)
    {
        installSystemFontMap();

        System.out.println("# device_model " + android.os.Build.MODEL);
        System.out.println("# device_manufacturer " + android.os.Build.MANUFACTURER);
        System.out.println("# android_sdk " + android.os.Build.VERSION.SDK_INT);
        System.out.println("# locale " + Locale.getDefault());

        Typeface fromFile = null;
        try
        {
            fromFile = Typeface.createFromFile(FONT_PATH);
        }
        catch (Throwable missing)
        {
            System.out.println("# font_file_unavailable " + missing);
        }

        if (fromFile != null)
        {
            emit("file", "Roboto-Regular", fromFile);
        }

        for (String family : FAMILIES)
        {
            emit("family", family, Typeface.create(family, Typeface.NORMAL));
        }

        System.out.println("# end");
    }

    private static void emit(String section, String label, Typeface typeface)
    {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(typeface);

        for (float size : SIZES)
        {
            paint.setTextSize(size);

            Paint.FontMetrics metrics = paint.getFontMetrics();
            Paint.FontMetricsInt metricsInt = paint.getFontMetricsInt();

            System.out.println(row(section, label, size, "metrics",
                                   fixed(metrics.top) + " " + fixed(metrics.ascent) + " " + fixed(metrics.descent) + " "
                                   + fixed(metrics.bottom) + " " + fixed(metrics.leading)));

            System.out.println(row(section, label, size, "metricsInt",
                                   metricsInt.top + " " + metricsInt.ascent + " " + metricsInt.descent + " " + metricsInt.bottom
                                   + " " + metricsInt.leading));

            for (String text : CORPUS)
            {
                System.out.println(row(section, label, size, "measure", fixed(paint.measureText(text)) + " " + quote(text)));

                Rect bounds = new Rect();
                paint.getTextBounds(text, 0, text.length(), bounds);
                System.out.println(row(section, label, size, "bounds",
                                       bounds.left + " " + bounds.top + " " + bounds.right + " " + bounds.bottom + " " + quote(text)));

                float[] widths = new float[text.length()];
                paint.getTextWidths(text, widths);
                StringBuilder perGlyph = new StringBuilder();
                for (int index = 0; index < widths.length; index++)
                {
                    if (index > 0) perGlyph.append(' ');
                    perGlyph.append(fixed(widths[index]));
                }
                System.out.println(row(section, label, size, "widths", perGlyph + " " + quote(text)));
            }
        }
    }

    private static String row(String section, String label, float size, String kind, String payload)
    {
        return String.format(Locale.ROOT, "%s %s %s %s %s", section, label, fixed(size), kind, payload);
    }

    private static String fixed(float value)
    {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static String quote(String text)
    {
        return "\"" + text + "\"";
    }
}
