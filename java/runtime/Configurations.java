package runtime;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;

public final class Configurations
{
    private static final String TAG = "Configurations";

    private static android.app.ContextImpl sBase;

    private Configurations()
    {
    }

    public static void attach(android.app.ContextImpl base)
    {
        sBase = base;
    }

    public static boolean apply(int widthPixels, int heightPixels, int densityDpi)
    {
        if (sBase == null) return false;

        Configuration before = new Configuration();
        before.setTo(sBase.getResources().getConfiguration());

        sBase.updateMetricsForWindow(widthPixels, heightPixels, densityDpi);

        Configuration after = sBase.getResources().getConfiguration();
        int changed = before.diff(after);

        if (changed == 0)
        {
            Log.i(TAG, "the window changed but nothing in the configuration did");
            return false;
        }

        Log.i(TAG, "the configuration changed: " + describe(changed) + " (" + widthPixels + "x" + heightPixels + " at "
                   + densityDpi + " dpi, " + after.screenWidthDp + "x" + after.screenHeightDp + " dp)");

        android.content.Context.dispatchConfigurationChanged(after);

        int handled = 0;
        int recreated = 0;

        for (String className : Activities.classNames())
        {
            int declared = Bootstrap.nativeActivityConfigChanges(className);

            if ((declared & changed) == changed)
            {
                handled += 1;
                continue;
            }

            recreated += 1;
        }

        Log.i(TAG, handled + " activity(ies) declared they handle it, " + recreated + " will be recreated");

        Activities.deliverConfigurationChange(after, changed);

        return true;
    }

    private static String describe(int changed)
    {
        StringBuilder text = new StringBuilder();

        if ((changed & ActivityInfo.CONFIG_ORIENTATION) != 0) text.append("orientation ");
        if ((changed & ActivityInfo.CONFIG_SCREEN_SIZE) != 0) text.append("screenSize ");
        if ((changed & ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE) != 0) text.append("smallestScreenSize ");
        if ((changed & ActivityInfo.CONFIG_DENSITY) != 0) text.append("density ");
        if ((changed & ActivityInfo.CONFIG_LOCALE) != 0) text.append("locale ");
        if ((changed & ActivityInfo.CONFIG_UI_MODE) != 0) text.append("uiMode ");
        if ((changed & ActivityInfo.CONFIG_FONT_SCALE) != 0) text.append("fontScale ");

        if (text.length() == 0) text.append("0x").append(Integer.toHexString(changed));

        return text.toString().trim();
    }
}
