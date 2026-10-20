package android.appwidget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;

public class AppWidgetHostView extends FrameLayout
{
    private int mAppWidgetId = -1;
    private AppWidgetProviderInfo mInfo;
    private View mChild;
    private java.util.concurrent.Executor mExecutor;
    private long mFadeDuration;

    public AppWidgetHostView(Context context)
    {
        super(context);
    }

    public AppWidgetHostView(Context context, int animationIn, int animationOut)
    {
        super(context);
    }

    public AppWidgetHostView(Context context, AttributeSet attributes)
    {
        super(context, attributes);
    }

    public void setAppWidget(int appWidgetId, AppWidgetProviderInfo info)
    {
        mAppWidgetId = appWidgetId;
        mInfo = info;
    }

    public int getAppWidgetId()
    {
        return mAppWidgetId;
    }

    public AppWidgetProviderInfo getAppWidgetInfo()
    {
        return mInfo;
    }

    public void updateAppWidget(RemoteViews remoteViews)
    {
        if (remoteViews == null)
        {
            removeAllViews();
            mChild = null;
            return;
        }

        View applied = remoteViews.apply(getContext(), this);

        if (applied == null) return;

        removeAllViews();
        addView(applied);
        mChild = applied;
    }

    public void updateAppWidgetSize(Bundle newOptions, int minWidth, int minHeight, int maxWidth, int maxHeight)
    {
    }

    public void updateAppWidgetOptions(Bundle options)
    {
    }

    public void setExecutor(java.util.concurrent.Executor executor)
    {
        mExecutor = executor;
    }

    public void setColorResources(android.util.SparseIntArray colors)
    {
    }

    public void resetColorResources()
    {
    }

    public void setOnLightBackground(boolean onLightBackground)
    {
    }

    public void setFadeDuration(long duration)
    {
        mFadeDuration = duration;
    }

    protected View getErrorView()
    {
        return new View(getContext());
    }

    protected View getDefaultView()
    {
        return new View(getContext());
    }

    protected Context getRemoteContext()
    {
        return getContext();
    }

    protected void prepareView(View view)
    {
    }

    public static Rect getDefaultPaddingForWidget(Context context, android.content.ComponentName component, Rect padding)
    {
        Rect into = padding == null ? new Rect() : padding;
        into.set(0, 0, 0, 0);

        return into;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        if (mChild == null)
        {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }

        mChild.layout(0, 0, right - left, bottom - top);
    }
}
