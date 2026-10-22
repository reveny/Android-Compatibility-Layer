package android.webkit;

import android.os.Handler;

public class HttpAuthHandler extends Handler
{
    private boolean mHandled;

    HttpAuthHandler()
    {
    }

    public boolean useHttpAuthUsernamePassword()
    {
        return false;
    }

    public void cancel()
    {
        mHandled = true;
    }

    public void proceed(String username, String password)
    {
        mHandled = true;
    }

    public boolean suppressDialog()
    {
        return false;
    }

    boolean isHandled()
    {
        return mHandled;
    }
}
