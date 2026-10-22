package android.view.accessibility;

import android.os.Bundle;
import android.os.Message;
import android.view.View;

import java.lang.ref.WeakReference;

public abstract class AccessibilityRequestPreparer
{
    public static final int REQUEST_TYPE_EXTRA_DATA = 0x00000001;

    private final WeakReference<View> mViewRef;
    private final int mRequestTypes;

    public AccessibilityRequestPreparer(View view, int requestTypes)
    {
        if (!view.isAttachedToWindow()) throw new IllegalStateException("View must be attached to a window");

        mViewRef = new WeakReference<>(view);
        mRequestTypes = requestTypes;
    }

    public abstract void onPrepareExtraData(int virtualViewId, String extraDataKey, Bundle args, Message preparationFinishedMessage);

    public View getView()
    {
        return mViewRef.get();
    }

    int getRequestTypes()
    {
        return mRequestTypes;
    }
}
