package android.webkit;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.view.View;

public final class WebViewDelegate
{
    WebViewDelegate()
    {
    }

    public void setOnTraceEnabledChangeListener(OnTraceEnabledChangeListener listener)
    {
    }

    public boolean isTraceTagEnabled()
    {
        return false;
    }

    @Deprecated
    public boolean canInvokeDrawGlFunctor(View containerView)
    {
        return true;
    }

    @Deprecated
    public void invokeDrawGlFunctor(View containerView, long nativeDrawGLFunctor, boolean waitForCompletion)
    {
    }

    @Deprecated
    public void callDrawGlFunction(Canvas canvas, long nativeDrawGLFunctor)
    {
    }

    @Deprecated
    public void callDrawGlFunction(Canvas canvas, long nativeDrawGLFunctor, Runnable releasedRunnable)
    {
        if (releasedRunnable != null) releasedRunnable.run();
    }

    public void drawWebViewFunctor(Canvas canvas, int functor)
    {
    }

    @Deprecated
    public void detachDrawGlFunctor(View containerView, long nativeDrawGLFunctor)
    {
    }

    public int getPackageId(Resources resources, String packageName)
    {
        return 0x7f;
    }

    public Application getApplication()
    {
        return runtime.Bootstrap.getApplication();
    }

    public String getErrorString(Context context, int errorCode)
    {
        switch (errorCode)
        {
            case WebViewClient.ERROR_HOST_LOOKUP: return "net::ERR_NAME_NOT_RESOLVED";
            case WebViewClient.ERROR_CONNECT: return "net::ERR_CONNECTION_REFUSED";
            case WebViewClient.ERROR_TIMEOUT: return "net::ERR_TIMED_OUT";
            case WebViewClient.ERROR_UNSUPPORTED_SCHEME: return "net::ERR_UNKNOWN_URL_SCHEME";
            case WebViewClient.ERROR_BAD_URL: return "net::ERR_INVALID_URL";
            case WebViewClient.ERROR_FILE_NOT_FOUND: return "net::ERR_FILE_NOT_FOUND";
            default: return "net::ERR_FAILED";
        }
    }

    public void addWebViewAssetPath(Context context)
    {
    }

    public boolean isMultiProcessEnabled()
    {
        return false;
    }

    public String getDataDirectorySuffix()
    {
        return WebViewFactory.getDataDirectorySuffix();
    }

    public interface OnTraceEnabledChangeListener
    {
        void onTraceEnabledChange(boolean enabled);
    }
}
