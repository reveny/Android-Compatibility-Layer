package android.webkit;

public class JsResult
{
    private final ResultReceiver mReceiver;
    private boolean mResult;

    public JsResult(ResultReceiver receiver)
    {
        mReceiver = receiver;
    }

    public final void cancel()
    {
        mResult = false;
        wakeUp();
    }

    public final void confirm()
    {
        mResult = true;
        wakeUp();
    }

    public final boolean getResult()
    {
        return mResult;
    }

    private void wakeUp()
    {
        if (mReceiver != null) mReceiver.onJsResultComplete(this);
    }

    public interface ResultReceiver
    {
        void onJsResultComplete(JsResult result);
    }
}
