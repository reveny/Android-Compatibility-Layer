package android.webkit;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.WeakHashMap;

final class InMemoryCookieManager extends CookieManager
{
    static final InMemoryCookieManager INSTANCE = new InMemoryCookieManager();

    private static final String[] DATE_FORMATS = {
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd-MMM-yyyy HH:mm:ss zzz",
        "EEE, dd-MMM-yy HH:mm:ss zzz",
        "EEEE, dd-MMM-yy HH:mm:ss zzz",
        "EEE MMM d HH:mm:ss yyyy",
    };

    private final List<Cookie> mCookies = new ArrayList<>();
    private final Map<WebView, Boolean> mThirdPartyAcceptance = new WeakHashMap<>();
    private boolean mAcceptCookie = true;

    private InMemoryCookieManager()
    {
    }

    @Override
    public synchronized void setAcceptCookie(boolean accept)
    {
        mAcceptCookie = accept;
    }

    @Override
    public synchronized boolean acceptCookie()
    {
        return mAcceptCookie;
    }

    @Override
    public synchronized void setAcceptThirdPartyCookies(WebView webview, boolean accept)
    {
        if (webview == null) return;
        mThirdPartyAcceptance.put(webview, accept);
    }

    @Override
    public synchronized boolean acceptThirdPartyCookies(WebView webview)
    {
        if (webview == null) return false;
        Boolean accept = mThirdPartyAcceptance.get(webview);
        return accept != null && accept;
    }

    @Override
    public void setCookie(String url, String value)
    {
        store(url, value);
    }

    @Override
    public void setCookie(String url, String value, ValueCallback<Boolean> callback)
    {
        WebkitCallbacks.deliver(callback, store(url, value));
    }

    @Override
    public synchronized String getCookie(String url)
    {
        URI uri = parse(url);
        if (uri == null) return null;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
        boolean secure = "https".equalsIgnoreCase(uri.getScheme()) || "wss".equalsIgnoreCase(uri.getScheme());
        long now = System.currentTimeMillis();

        List<Cookie> matches = new ArrayList<>();
        for (Iterator<Cookie> iterator = mCookies.iterator(); iterator.hasNext();)
        {
            Cookie cookie = iterator.next();
            if (cookie.isExpired(now))
            {
                iterator.remove();
                continue;
            }
            if (cookie.mSecure && !secure) continue;
            if (!cookie.matchesHost(host) || !cookie.matchesPath(path)) continue;
            matches.add(cookie);
        }
        if (matches.isEmpty()) return null;

        Collections.sort(matches, new Comparator<Cookie>()
        {
            @Override
            public int compare(Cookie first, Cookie second)
            {
                int byPath = second.mPath.length() - first.mPath.length();
                if (byPath != 0) return byPath;
                return Long.compare(first.mCreationTime, second.mCreationTime);
            }
        });

        StringBuilder builder = new StringBuilder();
        for (Cookie cookie : matches)
        {
            if (builder.length() > 0) builder.append("; ");
            if (cookie.mName.isEmpty())
            {
                builder.append(cookie.mValue);
            }
            else
            {
                builder.append(cookie.mName).append('=').append(cookie.mValue);
            }
        }
        return builder.toString();
    }

    @Override
    public synchronized void removeSessionCookie()
    {
        removeSessionCookiesLocked();
    }

    @Override
    public void removeSessionCookies(ValueCallback<Boolean> callback)
    {
        boolean removed;
        synchronized (this)
        {
            removed = removeSessionCookiesLocked();
        }
        WebkitCallbacks.deliver(callback, removed);
    }

    @Override
    public synchronized void removeAllCookie()
    {
        mCookies.clear();
    }

    @Override
    public void removeAllCookies(ValueCallback<Boolean> callback)
    {
        boolean removed;
        synchronized (this)
        {
            removed = !mCookies.isEmpty();
            mCookies.clear();
        }
        WebkitCallbacks.deliver(callback, removed);
    }

    @Override
    public synchronized boolean hasCookies()
    {
        removeExpiredLocked();
        return !mCookies.isEmpty();
    }

    @Override
    public synchronized void removeExpiredCookie()
    {
        removeExpiredLocked();
    }

    @Override
    public void flush()
    {
    }

    private boolean removeSessionCookiesLocked()
    {
        boolean removed = false;
        for (Iterator<Cookie> iterator = mCookies.iterator(); iterator.hasNext();)
        {
            if (iterator.next().mExpiry < 0)
            {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    private void removeExpiredLocked()
    {
        long now = System.currentTimeMillis();
        for (Iterator<Cookie> iterator = mCookies.iterator(); iterator.hasNext();)
        {
            if (iterator.next().isExpired(now)) iterator.remove();
        }
    }

    private synchronized boolean store(String url, String value)
    {
        if (!mAcceptCookie || value == null) return false;
        URI uri = parse(url);
        if (uri == null) return false;
        if ("file".equalsIgnoreCase(uri.getScheme()) && !allowFileSchemeCookies()) return false;

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String[] parts = value.split(";");
        String pair = parts[0].trim();
        int equals = pair.indexOf('=');
        String name = equals < 0 ? "" : pair.substring(0, equals).trim();
        String cookieValue = equals < 0 ? pair : pair.substring(equals + 1).trim();
        if (name.isEmpty() && cookieValue.isEmpty()) return false;

        Cookie cookie = new Cookie(name, cookieValue);
        cookie.mDomain = host;
        cookie.mHostOnly = true;
        cookie.mPath = defaultPath(uri.getRawPath());
        long now = System.currentTimeMillis();
        boolean maxAgeSeen = false;

        for (int index = 1; index < parts.length; index++)
        {
            String attribute = parts[index].trim();
            if (attribute.isEmpty()) continue;
            int separator = attribute.indexOf('=');
            String key = (separator < 0 ? attribute : attribute.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
            String argument = separator < 0 ? "" : attribute.substring(separator + 1).trim();
            switch (key)
            {
                case "domain":
                {
                    String domain = argument.startsWith(".") ? argument.substring(1) : argument;
                    domain = domain.toLowerCase(Locale.ROOT);
                    if (domain.isEmpty()) break;
                    if (!host.equals(domain) && !host.endsWith("." + domain)) return false;
                    cookie.mDomain = domain;
                    cookie.mHostOnly = false;
                    break;
                }
                case "path":
                    if (argument.startsWith("/")) cookie.mPath = argument;
                    break;
                case "max-age":
                    try
                    {
                        long seconds = Long.parseLong(argument);
                        cookie.mExpiry = seconds <= 0 ? 0 : now + seconds * 1000L;
                        maxAgeSeen = true;
                    }
                    catch (NumberFormatException ignored)
                    {
                    }
                    break;
                case "expires":
                    if (!maxAgeSeen)
                    {
                        Date date = parseDate(argument);
                        if (date != null) cookie.mExpiry = Math.max(0, date.getTime());
                    }
                    break;
                case "secure":
                    cookie.mSecure = true;
                    break;
                case "httponly":
                    cookie.mHttpOnly = true;
                    break;
                default:
                    break;
            }
        }

        for (Iterator<Cookie> iterator = mCookies.iterator(); iterator.hasNext();)
        {
            Cookie existing = iterator.next();
            if (existing.mName.equals(cookie.mName) && existing.mDomain.equals(cookie.mDomain) && existing.mPath.equals(cookie.mPath))
            {
                cookie.mCreationTime = existing.mCreationTime;
                iterator.remove();
            }
        }
        if (cookie.isExpired(now)) return true;
        mCookies.add(cookie);
        return true;
    }

    private static URI parse(String url)
    {
        if (url == null) return null;
        String candidate = url.trim();
        if (candidate.isEmpty()) return null;
        if (!candidate.contains("://")) candidate = "http://" + candidate;
        try
        {
            URI uri = new URI(candidate);
            if (uri.getHost() == null) return null;
            return uri;
        }
        catch (Exception exception)
        {
            return null;
        }
    }

    private static String defaultPath(String path)
    {
        if (path == null || path.isEmpty() || path.charAt(0) != '/') return "/";
        int slash = path.lastIndexOf('/');
        if (slash == 0) return "/";
        return path.substring(0, slash);
    }

    private static Date parseDate(String text)
    {
        for (String format : DATE_FORMATS)
        {
            SimpleDateFormat parser = new SimpleDateFormat(format, Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("GMT"));
            parser.setLenient(true);
            try
            {
                return parser.parse(text);
            }
            catch (ParseException ignored)
            {
            }
        }
        return null;
    }

    private static final class Cookie
    {
        final String mName;
        final String mValue;
        String mDomain;
        String mPath;
        boolean mHostOnly;
        boolean mSecure;
        boolean mHttpOnly;
        long mExpiry = -1;
        long mCreationTime = System.nanoTime();

        Cookie(String name, String value)
        {
            mName = name;
            mValue = value;
        }

        boolean isExpired(long now)
        {
            return mExpiry >= 0 && mExpiry <= now;
        }

        boolean matchesHost(String host)
        {
            if (host.equals(mDomain)) return true;
            return !mHostOnly && host.endsWith("." + mDomain);
        }

        boolean matchesPath(String path)
        {
            if (path.equals(mPath)) return true;
            if (!path.startsWith(mPath)) return false;
            return mPath.endsWith("/") || path.charAt(mPath.length()) == '/';
        }
    }
}
