package android.app;

import android.content.Context;
import android.content.Intent;

public final class PendingIntent
{
    public static final int FLAG_ONE_SHOT = 1 << 30;
    public static final int FLAG_NO_CREATE = 1 << 29;
    public static final int FLAG_CANCEL_CURRENT = 1 << 28;
    public static final int FLAG_UPDATE_CURRENT = 1 << 27;
    public static final int FLAG_IMMUTABLE = 1 << 26;
    public static final int FLAG_MUTABLE = 1 << 25;

    static final int TARGET_ACTIVITY = 0;
    static final int TARGET_SERVICE = 1;
    static final int TARGET_BROADCAST = 2;

    private final Context mContext;
    private final int mRequestCode;
    private final Intent mIntent;
    private final int mTarget;
    private final int mFlags;

    private boolean mCancelled;

    private PendingIntent(Context context, int requestCode, Intent intent, int target, int flags)
    {
        mContext = context;
        mRequestCode = requestCode;
        mIntent = intent == null ? null : new Intent(intent);
        mTarget = target;
        mFlags = flags;
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags)
    {
        return new PendingIntent(context, requestCode, intent, TARGET_ACTIVITY, flags);
    }

    public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags)
    {
        return new PendingIntent(context, requestCode, intent, TARGET_SERVICE, flags);
    }

    public static PendingIntent getForegroundService(Context context, int requestCode, Intent intent, int flags)
    {
        return new PendingIntent(context, requestCode, intent, TARGET_SERVICE, flags);
    }

    public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags)
    {
        return new PendingIntent(context, requestCode, intent, TARGET_BROADCAST, flags);
    }

    public int getRequestCode()
    {
        return mRequestCode;
    }

    public Intent getIntent()
    {
        return mIntent;
    }

    public void cancel()
    {
        mCancelled = true;
    }

    public void send()
    {
        send(0);
    }

    public void send(int code)
    {
        if (mCancelled)
        {
            android.util.Log.w("PendingIntent", "a cancelled pending intent was sent and did nothing: " + mIntent);
            return;
        }

        if (mIntent == null) return;

        if (mTarget == TARGET_ACTIVITY) runtime.Activities.start(mIntent, -1);
        else if (mTarget == TARGET_SERVICE) runtime.Services.start(mIntent);
        else runtime.Broadcasts.send(mIntent, null, false);

        if ((mFlags & FLAG_ONE_SHOT) != 0) mCancelled = true;
    }

    @Override
    public String toString()
    {
        return "PendingIntent{" + describeTarget() + " " + mIntent + "}";
    }

    private String describeTarget()
    {
        if (mTarget == TARGET_ACTIVITY) return "activity";
        if (mTarget == TARGET_SERVICE) return "service";

        return "broadcast";
    }
}
