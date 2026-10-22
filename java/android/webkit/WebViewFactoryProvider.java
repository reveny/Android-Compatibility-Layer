package android.webkit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.util.List;

public interface WebViewFactoryProvider
{
    Statics getStatics();

    WebViewProvider createWebView(WebView webView, WebView.PrivateAccess privateAccess);

    GeolocationPermissions getGeolocationPermissions();

    CookieManager getCookieManager();

    TracingController getTracingController();

    ServiceWorkerController getServiceWorkerController();

    WebStorage getWebStorage();

    WebViewDatabase getWebViewDatabase(Context context);

    ClassLoader getWebViewClassLoader();

    interface Statics
    {
        String findAddress(String address);

        String getDefaultUserAgent(Context context);

        void freeMemoryForTests();

        void setWebContentsDebuggingEnabled(boolean enable);

        void clearClientCertPreferences(Runnable onCleared);

        void enableSlowWholeDocumentDraw();

        Uri[] parseFileChooserResult(int resultCode, Intent intent);

        void initSafeBrowsing(Context context, ValueCallback<Boolean> callback);

        void setSafeBrowsingWhitelist(List<String> hosts, ValueCallback<Boolean> callback);

        Uri getSafeBrowsingPrivacyPolicyUrl();
    }
}
