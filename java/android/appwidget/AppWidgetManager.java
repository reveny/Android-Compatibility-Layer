package android.appwidget;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

public class AppWidgetManager
{
    public static final String ACTION_APPWIDGET_BIND = "android.appwidget.action.APPWIDGET_BIND";
    public static final String ACTION_APPWIDGET_CONFIGURE = "android.appwidget.action.APPWIDGET_CONFIGURE";
    public static final String ACTION_APPWIDGET_DELETED = "android.appwidget.action.APPWIDGET_DELETED";
    public static final String ACTION_APPWIDGET_DISABLED = "android.appwidget.action.APPWIDGET_DISABLED";
    public static final String ACTION_APPWIDGET_ENABLED = "android.appwidget.action.APPWIDGET_ENABLED";
    public static final String ACTION_APPWIDGET_HOST_RESTORED = "android.appwidget.action.APPWIDGET_HOST_RESTORED";
    public static final String ACTION_APPWIDGET_OPTIONS_CHANGED = "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS";
    public static final String ACTION_APPWIDGET_PICK = "android.appwidget.action.APPWIDGET_PICK";
    public static final String ACTION_APPWIDGET_RESTORED = "android.appwidget.action.APPWIDGET_RESTORED";
    public static final String ACTION_APPWIDGET_UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";
    public static final String EXTRA_APPWIDGET_ID = "appWidgetId";
    public static final String EXTRA_APPWIDGET_IDS = "appWidgetIds";
    public static final String EXTRA_APPWIDGET_OLD_IDS = "appWidgetOldIds";
    public static final String EXTRA_APPWIDGET_OPTIONS = "appWidgetOptions";
    public static final String EXTRA_APPWIDGET_PREVIEW = "appWidgetPreview";
    public static final String EXTRA_APPWIDGET_PROVIDER = "appWidgetProvider";
    public static final String EXTRA_APPWIDGET_PROVIDER_PROFILE = "appWidgetProviderProfile";
    public static final String EXTRA_CUSTOM_EXTRAS = "customExtras";
    public static final String EXTRA_CUSTOM_INFO = "customInfo";
    public static final String EXTRA_HOST_ID = "hostId";
    public static final int INVALID_APPWIDGET_ID = 0;
    public static final String META_DATA_APPWIDGET_PROVIDER = "android.appwidget.provider";
    public static final String OPTION_APPWIDGET_HOST_CATEGORY = "appWidgetCategory";
    public static final String OPTION_APPWIDGET_MAX_HEIGHT = "appWidgetMaxHeight";
    public static final String OPTION_APPWIDGET_MAX_WIDTH = "appWidgetMaxWidth";
    public static final String OPTION_APPWIDGET_MIN_HEIGHT = "appWidgetMinHeight";
    public static final String OPTION_APPWIDGET_MIN_WIDTH = "appWidgetMinWidth";
    public static final String OPTION_APPWIDGET_RESTORE_COMPLETED = "appWidgetRestoreCompleted";
    public static final String OPTION_APPWIDGET_SIZES = "appWidgetSizes";

    private static final String TAG = "AppWidgetManager";

    private static final AppWidgetManager sInstance = new AppWidgetManager();

    private static boolean sReported;
    private static boolean sReportedUpdate;

    AppWidgetManager()
    {
    }

    public static AppWidgetManager getInstance(Context context)
    {
        if (!sReported)
        {
            sReported = true;
            android.util.Log.i(TAG, "this runtime hosts no app widgets, so every provider list is empty");
        }

        return sInstance;
    }

    public void updateAppWidget(int[] appWidgetIds, RemoteViews views)
    {
        noteUpdate(appWidgetIds == null ? 0 : appWidgetIds.length);
    }

    public void updateAppWidgetOptions(int appWidgetId, Bundle options)
    {
    }

    public Bundle getAppWidgetOptions(int appWidgetId)
    {
        return new Bundle();
    }

    public void updateAppWidget(int appWidgetId, RemoteViews views)
    {
        noteUpdate(1);
    }

    public void partiallyUpdateAppWidget(int[] appWidgetIds, RemoteViews views)
    {
        noteUpdate(appWidgetIds == null ? 0 : appWidgetIds.length);
    }

    public void partiallyUpdateAppWidget(int appWidgetId, RemoteViews views)
    {
        noteUpdate(1);
    }

    public void updateAppWidget(ComponentName provider, RemoteViews views)
    {
        noteUpdate(0);
    }

    public void updateAppWidgetProviderInfo(ComponentName provider, String metaDataKey)
    {
    }

    public void notifyAppWidgetViewDataChanged(int[] appWidgetIds, int viewId)
    {
    }

    public void notifyAppWidgetViewDataChanged(int appWidgetId, int viewId)
    {
    }

    public List<AppWidgetProviderInfo> getInstalledProvidersForProfile(UserHandle profile)
    {
        return new ArrayList<>();
    }

    public List<AppWidgetProviderInfo> getInstalledProvidersForPackage(String packageName, UserHandle profile)
    {
        return new ArrayList<>();
    }

    public List<AppWidgetProviderInfo> getInstalledProviders()
    {
        return new ArrayList<>();
    }

    public AppWidgetProviderInfo getAppWidgetInfo(int appWidgetId)
    {
        return null;
    }

    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, ComponentName provider)
    {
        return false;
    }

    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, ComponentName provider, Bundle options)
    {
        return false;
    }

    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, UserHandle user, ComponentName provider, Bundle options)
    {
        return false;
    }

    public int[] getAppWidgetIds(ComponentName provider)
    {
        return new int[0];
    }

    public boolean isRequestPinAppWidgetSupported()
    {
        return false;
    }

    public boolean requestPinAppWidget(ComponentName provider, Bundle extras, PendingIntent successCallback)
    {
        android.util.Log.i(TAG, "a request to pin " + provider + " was declined because no launcher hosts widgets here");
        return false;
    }

    private static void noteUpdate(int count)
    {
        if (sReportedUpdate) return;

        sReportedUpdate = true;
        android.util.Log.i(TAG, "widget updates are accepted and dropped because no host shows them (first update had " + count
                                    + " id(s))");
    }
}
