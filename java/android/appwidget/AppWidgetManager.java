package android.appwidget;

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;

import java.util.ArrayList;
import java.util.List;

public class AppWidgetManager
{
    public static final String ACTION_APPWIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";
    public static final String EXTRA_APPWIDGET_ID = "appWidgetId";
    public static final int INVALID_APPWIDGET_ID = 0;

    private static final String TAG = "AppWidgetManager";

    private static boolean sReported;

    private AppWidgetManager()
    {
    }

    public static AppWidgetManager getInstance(Context context)
    {
        if (!sReported)
        {
            sReported = true;
            android.util.Log.i(TAG, "this runtime hosts no app widgets, so every provider list is empty");
        }

        return new AppWidgetManager();
    }

    public List<AppWidgetProviderInfo> getInstalledProviders()
    {
        return new ArrayList<>();
    }

    public List<AppWidgetProviderInfo> getInstalledProvidersForProfile(UserHandle profile)
    {
        return new ArrayList<>();
    }

    public List<AppWidgetProviderInfo> getInstalledProvidersForPackage(String packageName, UserHandle profile)
    {
        return new ArrayList<>();
    }

    public AppWidgetProviderInfo getAppWidgetInfo(int appWidgetId)
    {
        return null;
    }

    public int[] getAppWidgetIds(ComponentName provider)
    {
        return new int[0];
    }

    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, ComponentName provider)
    {
        return false;
    }

    public void updateAppWidget(int appWidgetId, android.widget.RemoteViews views)
    {
    }

    public void notifyAppWidgetViewDataChanged(int[] appWidgetIds, int viewId)
    {
    }

    public boolean isRequestPinAppWidgetSupported()
    {
        return false;
    }
}
