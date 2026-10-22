package android.webkit;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

public abstract class WebViewDatabase
{
    public WebViewDatabase()
    {
    }

    public static WebViewDatabase getInstance(Context context)
    {
        return LocalWebViewDatabase.INSTANCE;
    }

    @Deprecated
    public abstract boolean hasUsernamePassword();

    @Deprecated
    public abstract void clearUsernamePassword();

    public abstract boolean hasHttpAuthUsernamePassword();

    public abstract void clearHttpAuthUsernamePassword();

    public abstract void setHttpAuthUsernamePassword(String host, String realm, String username, String password);

    public abstract String[] getHttpAuthUsernamePassword(String host, String realm);

    @Deprecated
    public abstract boolean hasFormData();

    public abstract void clearFormData();

    private static final class LocalWebViewDatabase extends WebViewDatabase
    {
        static final LocalWebViewDatabase INSTANCE = new LocalWebViewDatabase();

        private final Map<String, String[]> mHttpAuth = new HashMap<>();

        @Override
        public boolean hasUsernamePassword()
        {
            return false;
        }

        @Override
        public void clearUsernamePassword()
        {
        }

        @Override
        public synchronized boolean hasHttpAuthUsernamePassword()
        {
            return !mHttpAuth.isEmpty();
        }

        @Override
        public synchronized void clearHttpAuthUsernamePassword()
        {
            mHttpAuth.clear();
        }

        @Override
        public synchronized void setHttpAuthUsernamePassword(String host, String realm, String username, String password)
        {
            if (host == null || realm == null) return;
            mHttpAuth.put(host + "\u0000" + realm, new String[] {username, password});
        }

        @Override
        public synchronized String[] getHttpAuthUsernamePassword(String host, String realm)
        {
            if (host == null || realm == null) return null;
            String[] credentials = mHttpAuth.get(host + "\u0000" + realm);
            return credentials == null ? null : credentials.clone();
        }

        @Override
        public boolean hasFormData()
        {
            return false;
        }

        @Override
        public void clearFormData()
        {
        }
    }
}
