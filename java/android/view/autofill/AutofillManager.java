package android.view.autofill;

import android.graphics.Rect;
import android.view.View;

public final class AutofillManager
{
    public static final String EXTRA_ASSIST_STRUCTURE = "android.view.autofill.extra.ASSIST_STRUCTURE";
    public static final int ACTION_START_SESSION = 1;
    public static final int ACTION_VIEW_ENTERED = 2;
    public static final int ACTION_VIEW_EXITED = 3;
    public static final int ACTION_VALUE_CHANGED = 4;

    public boolean isEnabled()
    {
        return false;
    }

    public boolean isAutofillSupported()
    {
        return false;
    }

    public void notifyViewEntered(View view)
    {
    }

    public void notifyViewEntered(View view, int virtualId, Rect bounds)
    {
    }

    public void notifyViewExited(View view)
    {
    }

    public void notifyViewExited(View view, int virtualId)
    {
    }

    public void notifyValueChanged(View view)
    {
    }

    public void notifyValueChanged(View view, int virtualId, AutofillValue value)
    {
    }

    public void notifyViewVisibilityChanged(View view, int virtualId, boolean visible)
    {
    }

    public void requestAutofill(View view)
    {
    }

    public void requestAutofill(View view, int virtualId, Rect bounds)
    {
    }

    public void cancel()
    {
    }

    public void commit()
    {
    }

    public void disableAutofillServices()
    {
    }

    public boolean hasEnabledAutofillServices()
    {
        return false;
    }

    public android.content.ComponentName getAutofillServiceComponentName()
    {
        return null;
    }

    public void registerCallback(AutofillCallback callback)
    {
    }

    public void unregisterCallback(AutofillCallback callback)
    {
    }

    public abstract static class AutofillCallback
    {
        public static final int EVENT_INPUT_SHOWN = 1;
        public static final int EVENT_INPUT_HIDDEN = 2;
        public static final int EVENT_INPUT_UNAVAILABLE = 3;

        public void onAutofillEvent(android.view.View view, int event)
        {
        }

        public void onAutofillEvent(android.view.View view, int virtualId, int event)
        {
        }
    }
}
