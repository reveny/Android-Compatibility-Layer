package android.webkit;

import android.os.Handler;
import android.os.Looper;

final class WebkitCallbacks
{
    private WebkitCallbacks()
    {
    }

    static void post(Runnable runnable)
    {
        Handler handler = handler();
        if (handler == null)
        {
            runnable.run();
            return;
        }
        handler.post(runnable);
    }

    static <T> void deliver(final ValueCallback<T> callback, final T value)
    {
        if (callback == null) return;
        post(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onReceiveValue(value);
            }
        });
    }

    private static Handler handler()
    {
        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        if (looper == null) return null;
        return new Handler(looper);
    }
}
