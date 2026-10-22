package android.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class MediaScannerConnection implements ServiceConnection
{
    private final Context mContext;
    private final MediaScannerConnectionClient mClient;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mConnected;
    private boolean mConnecting;

    public MediaScannerConnection(Context context, MediaScannerConnection.MediaScannerConnectionClient client)
    {
        mContext = context;
        mClient = client;
    }

    public void connect()
    {
        synchronized (this)
        {
            if (mConnected || mConnecting) return;

            mConnecting = true;
        }

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                onServiceConnected(new ComponentName("com.android.providers.media", "com.android.providers.media.MediaScannerService"),
                    null);
            }
        });
    }

    public void disconnect()
    {
        synchronized (this)
        {
            mConnecting = false;
            mConnected = false;
        }
    }

    public synchronized boolean isConnected()
    {
        return mConnected;
    }

    public void onServiceConnected(ComponentName className, IBinder service)
    {
        synchronized (this)
        {
            if (!mConnecting) return;

            mConnecting = false;
            mConnected = true;
        }

        if (mClient != null) mClient.onMediaScannerConnected();
    }

    public void onServiceDisconnected(ComponentName className)
    {
        synchronized (this)
        {
            mConnected = false;
        }
    }

    public static void scanFile(Context context, String[] paths, String[] mimeTypes,
        MediaScannerConnection.OnScanCompletedListener callback)
    {
        if (paths == null) return;

        final String[] pending = paths.clone();
        final OnScanCompletedListener listener = callback;

        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                if (listener == null) return;

                for (String path : pending)
                {
                    listener.onScanCompleted(path, null);
                }
            }
        });
    }

    public void scanFile(String path, String mimeType)
    {
        synchronized (this)
        {
            if (!mConnected) throw new IllegalStateException("not connected to MediaScannerService");
        }

        final String scannedPath = path;

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (mClient != null) mClient.onScanCompleted(scannedPath, null);
            }
        });
    }

    public interface OnScanCompletedListener
    {
        void onScanCompleted(String path, Uri uri);
    }

    public interface MediaScannerConnectionClient extends MediaScannerConnection.OnScanCompletedListener
    {
        void onMediaScannerConnected();
    }
}
