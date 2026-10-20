package android.appwidget;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppWidgetHost
{
    private final Context mContext;
    private final int mHostId;
    private final Map<Integer, AppWidgetHostView> mViews = new HashMap<>();

    private static int sNextAppWidgetId = 1;
    private boolean mListening;

    public AppWidgetHost(Context context, int hostId)
    {
        mContext = context;
        mHostId = hostId;
    }

    public void startListening()
    {
        mListening = true;
    }

    public void stopListening()
    {
        mListening = false;
    }

    public boolean isListening()
    {
        return mListening;
    }

    public int allocateAppWidgetId()
    {
        return sNextAppWidgetId++;
    }

    public void deleteAppWidgetId(int appWidgetId)
    {
        mViews.remove(Integer.valueOf(appWidgetId));
    }

    public void deleteHost()
    {
        mViews.clear();
    }

    public static void deleteAllHosts()
    {
    }

    public int[] getAppWidgetIds()
    {
        int[] ids = new int[mViews.size()];
        int index = 0;

        for (Integer id : mViews.keySet()) ids[index++] = id.intValue();

        return ids;
    }

    public final AppWidgetHostView createView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget)
    {
        AppWidgetHostView view = onCreateView(context, appWidgetId, appWidget);

        view.setAppWidget(appWidgetId, appWidget);
        mViews.put(Integer.valueOf(appWidgetId), view);

        return view;
    }

    protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget)
    {
        return new AppWidgetHostView(context);
    }

    protected void onProviderChanged(int appWidgetId, AppWidgetProviderInfo appWidget)
    {
    }

    protected void onProvidersChanged()
    {
    }

    protected void onAppWidgetRemoved(int appWidgetId)
    {
    }

    protected void clearViews()
    {
        mViews.clear();
    }

    public void setAppWidgetHidden(String packageName, int hostId)
    {
    }

}
