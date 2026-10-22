package android.os;

public class ResultReceiver implements Parcelable
{
    public static final Parcelable.Creator<ResultReceiver> CREATOR = new Parcelable.Creator<ResultReceiver>()
    {
        @Override
        public ResultReceiver createFromParcel(Parcel source)
        {
            Object value = source == null ? null : source.readValue(null);
            return value instanceof ResultReceiver ? (ResultReceiver)value : new ResultReceiver(null);
        }

        @Override
        public ResultReceiver[] newArray(int size)
        {
            return new ResultReceiver[size];
        }
    };

    private final Handler mHandler;

    public ResultReceiver(Handler handler)
    {
        mHandler = handler;
    }

    public void send(final int resultCode, final Bundle resultData)
    {
        if (mHandler == null)
        {
            onReceiveResult(resultCode, resultData);
            return;
        }

        mHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                onReceiveResult(resultCode, resultData);
            }
        });
    }

    protected void onReceiveResult(int resultCode, Bundle resultData)
    {
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeValue(this);
    }
}
