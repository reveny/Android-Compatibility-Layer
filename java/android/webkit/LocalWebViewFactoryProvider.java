package android.webkit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.List;

final class LocalWebViewFactoryProvider implements WebViewFactoryProvider
{
    private final Statics mStatics = new LocalStatics();

    @Override
    public Statics getStatics()
    {
        return mStatics;
    }

    @Override
    public WebViewProvider createWebView(WebView webView, WebView.PrivateAccess privateAccess)
    {
        return null;
    }

    @Override
    public GeolocationPermissions getGeolocationPermissions()
    {
        return GeolocationPermissions.getInstance();
    }

    @Override
    public CookieManager getCookieManager()
    {
        return CookieManager.getInstance();
    }

    @Override
    public TracingController getTracingController()
    {
        return TracingController.getInstance();
    }

    @Override
    public ServiceWorkerController getServiceWorkerController()
    {
        return ServiceWorkerController.getInstance();
    }

    @Override
    public WebStorage getWebStorage()
    {
        return WebStorage.getInstance();
    }

    @Override
    public WebViewDatabase getWebViewDatabase(Context context)
    {
        return WebViewDatabase.getInstance(context);
    }

    @Override
    public ClassLoader getWebViewClassLoader()
    {
        return WebView.class.getClassLoader();
    }

    private static final class LocalStatics implements Statics
    {
        @Override
        public String findAddress(String address)
        {
            return null;
        }

        @Override
        public String getDefaultUserAgent(Context context)
        {
            return LocalWebSettings.defaultUserAgent();
        }

        @Override
        public void freeMemoryForTests()
        {
        }

        @Override
        public void setWebContentsDebuggingEnabled(boolean enable)
        {
        }

        @Override
        public void clearClientCertPreferences(Runnable onCleared)
        {
            if (onCleared != null) WebkitCallbacks.post(onCleared);
        }

        @Override
        public void enableSlowWholeDocumentDraw()
        {
        }

        @Override
        public Uri[] parseFileChooserResult(int resultCode, Intent intent)
        {
            return WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
        }

        @Override
        public void initSafeBrowsing(Context context, ValueCallback<Boolean> callback)
        {
            WebkitCallbacks.deliver(callback, Boolean.FALSE);
        }

        @Override
        public void setSafeBrowsingWhitelist(List<String> hosts, ValueCallback<Boolean> callback)
        {
            WebkitCallbacks.deliver(callback, Boolean.TRUE);
        }

        @Override
        public Uri getSafeBrowsingPrivacyPolicyUrl()
        {
            return Uri.parse("https://www.google.com/chrome/browser/privacy/");
        }
    }
}
