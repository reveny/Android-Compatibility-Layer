package android.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class RemoteViews
{
    private static final String TAG = "RemoteViews";

    private final String mPackageName;
    private final int mLayoutId;

    public RemoteViews(String packageName, int layoutId)
    {
        mPackageName = packageName;
        mLayoutId = layoutId;
    }

    public String getPackage()
    {
        return mPackageName;
    }

    public int getLayoutId()
    {
        return mLayoutId;
    }

    public View apply(Context context, ViewGroup parent)
    {
        android.util.Log.w(TAG, "there is no remote view host here, so nothing was inflated for " + mPackageName);
        return null;
    }

    public void reapply(Context context, View view)
    {
    }

    public void setTextViewText(int viewId, CharSequence text)
    {
    }

    public void setImageViewResource(int viewId, int resourceId)
    {
    }

    public void setViewVisibility(int viewId, int visibility)
    {
    }

    public void setOnClickPendingIntent(int viewId, android.app.PendingIntent intent)
    {
    }
}
