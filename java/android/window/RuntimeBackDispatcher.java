package android.window;

import java.util.ArrayList;

public final class RuntimeBackDispatcher implements OnBackInvokedDispatcher
{
    private final ArrayList<OnBackInvokedCallback> mCallbacks = new ArrayList<>();

    @Override
    public void registerOnBackInvokedCallback(int priority, OnBackInvokedCallback callback)
    {
        if (callback != null) mCallbacks.add(callback);
    }

    @Override
    public void unregisterOnBackInvokedCallback(OnBackInvokedCallback callback)
    {
        mCallbacks.remove(callback);
    }

    public boolean dispatchBack()
    {
        if (mCallbacks.isEmpty()) return false;

        mCallbacks.get(mCallbacks.size() - 1).onBackInvoked();
        return true;
    }
}
