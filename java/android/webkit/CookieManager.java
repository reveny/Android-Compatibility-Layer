package android.webkit;

public abstract class CookieManager
{
    private static boolean sAcceptFileSchemeCookies;

    public CookieManager()
    {
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("doesn't implement Cloneable");
    }

    public static CookieManager getInstance()
    {
        return InMemoryCookieManager.INSTANCE;
    }

    public abstract void setAcceptCookie(boolean accept);

    public abstract boolean acceptCookie();

    public abstract void setAcceptThirdPartyCookies(WebView webview, boolean accept);

    public abstract boolean acceptThirdPartyCookies(WebView webview);

    public abstract void setCookie(String url, String value);

    public abstract void setCookie(String url, String value, ValueCallback<Boolean> callback);

    public abstract String getCookie(String url);

    public String getCookie(String url, boolean privateBrowsing)
    {
        return getCookie(url);
    }

    public abstract void removeSessionCookie();

    public abstract void removeSessionCookies(ValueCallback<Boolean> callback);

    public abstract void removeAllCookie();

    public abstract void removeAllCookies(ValueCallback<Boolean> callback);

    public abstract boolean hasCookies();

    public boolean hasCookies(boolean privateBrowsing)
    {
        return hasCookies();
    }

    public abstract void removeExpiredCookie();

    public abstract void flush();

    public static boolean allowFileSchemeCookies()
    {
        return sAcceptFileSchemeCookies;
    }

    public static void setAcceptFileSchemeCookies(boolean accept)
    {
        sAcceptFileSchemeCookies = accept;
    }
}
