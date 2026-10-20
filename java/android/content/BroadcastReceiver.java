package android.content;

public abstract class BroadcastReceiver
{
    private boolean mAborted;
    private int mResultCode;
    private String mResultData;
    private android.os.Bundle mResultExtras;
    private PendingResult mPendingResult;

    public abstract void onReceive(Context context, Intent intent);

    public final PendingResult goAsync()
    {
        mPendingResult = new PendingResult(this);
        return mPendingResult;
    }

    public final void abortBroadcast()
    {
        mAborted = true;
    }

    public final void clearAbortBroadcast()
    {
        mAborted = false;
    }

    public final boolean getAbortBroadcast()
    {
        return mAborted;
    }

    public final boolean isAborted()
    {
        return mAborted;
    }

    public final boolean isOrderedBroadcast()
    {
        return true;
    }

    public final void setResultCode(int code)
    {
        mResultCode = code;
    }

    public final int getResultCode()
    {
        return mResultCode;
    }

    public final void setResultData(String data)
    {
        mResultData = data;
    }

    public final String getResultData()
    {
        return mResultData;
    }

    public final void setResultExtras(android.os.Bundle extras)
    {
        mResultExtras = extras;
    }

    public final android.os.Bundle getResultExtras(boolean makeMap)
    {
        if (mResultExtras == null && makeMap) mResultExtras = new android.os.Bundle();
        return mResultExtras;
    }

    public final void setResult(int code, String data, android.os.Bundle extras)
    {
        mResultCode = code;
        mResultData = data;
        mResultExtras = extras;
    }

    public static class PendingResult
    {
        private final BroadcastReceiver mOwner;
        private boolean mFinished;

        PendingResult(BroadcastReceiver owner)
        {
            mOwner = owner;
        }

        public final void finish()
        {
            if (mFinished) return;

            mFinished = true;
            android.util.Log.i("BroadcastReceiver", mOwner.getClass().getName() + " finished its asynchronous broadcast");
        }

        public final void setResultCode(int code)
        {
            mOwner.setResultCode(code);
        }

        public final int getResultCode()
        {
            return mOwner.getResultCode();
        }

        public final void setResultData(String data)
        {
            mOwner.setResultData(data);
        }

        public final String getResultData()
        {
            return mOwner.getResultData();
        }

        public final void abortBroadcast()
        {
            mOwner.abortBroadcast();
        }
    }
}
