package android.database;

import android.net.Uri;
import android.os.Handler;

public abstract class ContentObserver
{
    private final Handler mHandler;

    public ContentObserver(Handler handler)
    {
        mHandler = handler;
    }

    public boolean deliverSelfNotifications()
    {
        return false;
    }

    public void onChange(boolean selfChange)
    {
    }

    public void onChange(boolean selfChange, Uri uri)
    {
        onChange(selfChange);
    }

    public void onChange(boolean selfChange, Uri uri, int flags)
    {
        onChange(selfChange, uri);
    }

    public final void dispatchChange(boolean selfChange)
    {
        dispatchChange(selfChange, null);
    }

    public final void dispatchChange(boolean selfChange, Uri uri)
    {
        dispatchChange(selfChange, uri, 0);
    }

    public final void dispatchChange(final boolean selfChange, final Uri uri, final int flags)
    {
        if (mHandler == null)
        {
            onChange(selfChange, uri, flags);
            return;
        }

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                onChange(selfChange, uri, flags);
            }
        });
    }
}
