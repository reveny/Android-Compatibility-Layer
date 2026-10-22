package android.app;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

@Deprecated
public abstract class IntentService extends Service
{
    private final String mName;
    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private boolean mRedelivery;

    private final class ServiceHandler extends Handler
    {
        ServiceHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message message)
        {
            onHandleIntent((Intent)message.obj);
            stopSelf(message.arg1);
        }
    }

    public IntentService(String name)
    {
        super();
        mName = name;
    }

    public void setIntentRedelivery(boolean enabled)
    {
        mRedelivery = enabled;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Message message = mServiceHandler.obtainMessage();
        message.arg1 = startId;
        message.obj = intent;
        mServiceHandler.sendMessage(message);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        onStart(intent, startId);
        return mRedelivery ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    protected abstract void onHandleIntent(Intent intent);
}
