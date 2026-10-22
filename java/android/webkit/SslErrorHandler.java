package android.webkit;

import android.os.Handler;

public class SslErrorHandler extends Handler
{
    private boolean mProceeded;

    SslErrorHandler()
    {
    }

    public void proceed()
    {
        mProceeded = true;
    }

    public void cancel()
    {
        mProceeded = false;
    }

    boolean hasProceeded()
    {
        return mProceeded;
    }
}
