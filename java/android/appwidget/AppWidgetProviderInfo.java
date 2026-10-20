package android.appwidget;

import android.content.ComponentName;

public class AppWidgetProviderInfo
{
    public static final int WIDGET_CATEGORY_HOME_SCREEN = 1 << 0;

    public ComponentName provider;
    public int minWidth;
    public int minHeight;
    public int minResizeWidth;
    public int minResizeHeight;
    public int updatePeriodMillis;
    public int initialLayout;
    public int icon;
    public String label;
    public int widgetCategory;

    public CharSequence loadLabel(android.content.pm.PackageManager packageManager)
    {
        return label != null ? label : "";
    }

    public android.graphics.drawable.Drawable loadIcon(android.content.Context context, int density)
    {
        return null;
    }
}
