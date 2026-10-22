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
    public static final String EXTRA_AUTHENTICATION_RESULT = "android.view.autofill.extra.AUTHENTICATION_RESULT";
    public static final String EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET =
        "android.view.autofill.extra.AUTHENTICATION_RESULT_EPHEMERAL_DATASET";
    public static final String EXTRA_CLIENT_STATE = "android.view.autofill.extra.CLIENT_STATE";
    public static final String EXTRA_INLINE_SUGGESTIONS_REQUEST = "android.view.autofill.extra.INLINE_SUGGESTIONS_REQUEST";

    private static final int FIRST_GENERATED_AUTOFILL_ID = 0x40000000;

    private final java.util.concurrent.atomic.AtomicInteger mNextAutofillId =
        new java.util.concurrent.atomic.AtomicInteger(FIRST_GENERATED_AUTOFILL_ID);

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

    public void notifyVirtualViewsReady(View view, android.util.SparseArray<VirtualViewFillInfo> infos)
    {
        if (view == null || infos == null) throw new NullPointerException();
        if (infos.size() == 0) throw new IllegalArgumentException("No VirtualViewInfo found");
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

    public AutofillId getNextAutofillId()
    {
        return new AutofillId(mNextAutofillId.getAndIncrement());
    }

    public void notifyViewVisibilityChanged(View view, boolean visible)
    {
    }

    public void notifyViewClicked(View view)
    {
    }

    public void notifyViewClicked(View view, int virtualId)
    {
    }

    public String getUserDataId()
    {
        return null;
    }

    public boolean isFieldClassificationEnabled()
    {
        return false;
    }

    public String getDefaultFieldClassificationAlgorithm()
    {
        return null;
    }

    public java.util.List<String> getAvailableFieldClassificationAlgorithms()
    {
        return new java.util.ArrayList<>();
    }

    public boolean showAutofillDialog(View view)
    {
        return false;
    }

    public boolean showAutofillDialog(View view, int virtualId)
    {
        return false;
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
