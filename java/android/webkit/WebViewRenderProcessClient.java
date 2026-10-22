package android.webkit;

public abstract class WebViewRenderProcessClient
{
    public WebViewRenderProcessClient()
    {
    }

    public abstract void onRenderProcessUnresponsive(WebView view, WebViewRenderProcess renderer);

    public abstract void onRenderProcessResponsive(WebView view, WebViewRenderProcess renderer);
}
