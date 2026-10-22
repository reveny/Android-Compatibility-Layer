package android.webkit;

import android.os.Handler;
import java.util.ArrayList;
import java.util.List;

final class LocalWebMessagePort extends WebMessagePort
{
    private LocalWebMessagePort mPeer;
    private WebMessageCallback mCallback;
    private Handler mHandler;
    private boolean mClosed;
    private final List<WebMessage> mPending = new ArrayList<>();

    private LocalWebMessagePort()
    {
    }

    static WebMessagePort[] createChannel()
    {
        LocalWebMessagePort first = new LocalWebMessagePort();
        LocalWebMessagePort second = new LocalWebMessagePort();
        first.mPeer = second;
        second.mPeer = first;
        return new WebMessagePort[] {first, second};
    }

    @Override
    public void postMessage(WebMessage message)
    {
        LocalWebMessagePort peer;
        synchronized (this)
        {
            if (mClosed) throw new IllegalStateException("Port is already closed");
            peer = mPeer;
        }
        if (message != null && message.getPorts() != null)
        {
            for (WebMessagePort port : message.getPorts())
            {
                if (port == this || port == peer) throw new IllegalStateException("The port cannot be transferred to itself");
            }
        }
        if (peer != null) peer.receive(message);
    }

    @Override
    public void close()
    {
        synchronized (this)
        {
            mClosed = true;
            mPending.clear();
            mCallback = null;
        }
    }

    @Override
    public void setWebMessageCallback(WebMessageCallback callback)
    {
        setWebMessageCallback(callback, null);
    }

    @Override
    public void setWebMessageCallback(WebMessageCallback callback, Handler handler)
    {
        List<WebMessage> pending;
        synchronized (this)
        {
            if (mClosed) throw new IllegalStateException("Port is already closed");
            mCallback = callback;
            mHandler = handler;
            pending = new ArrayList<>(mPending);
            mPending.clear();
        }
        for (WebMessage message : pending) receive(message);
    }

    private void receive(final WebMessage message)
    {
        final WebMessageCallback callback;
        Handler handler;
        synchronized (this)
        {
            if (mClosed) return;
            if (mCallback == null)
            {
                mPending.add(message);
                return;
            }
            callback = mCallback;
            handler = mHandler;
        }
        Runnable delivery = new Runnable()
        {
            @Override
            public void run()
            {
                callback.onMessage(LocalWebMessagePort.this, message);
            }
        };
        if (handler != null)
        {
            handler.post(delivery);
        }
        else
        {
            WebkitCallbacks.post(delivery);
        }
    }
}
