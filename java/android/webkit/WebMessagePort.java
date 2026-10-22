package android.webkit;

import android.os.Handler;

public abstract class WebMessagePort
{
    WebMessagePort()
    {
    }

    public abstract void postMessage(WebMessage message);

    public abstract void close();

    public abstract void setWebMessageCallback(WebMessageCallback callback);

    public abstract void setWebMessageCallback(WebMessageCallback callback, Handler handler);

    public abstract static class WebMessageCallback
    {
        public WebMessageCallback()
        {
        }

        public void onMessage(WebMessagePort port, WebMessage message)
        {
        }
    }
}
