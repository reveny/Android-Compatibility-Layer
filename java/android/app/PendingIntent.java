package android.app;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PendingIntent implements Parcelable
{
    public static final int FLAG_ONE_SHOT = 1 << 30;
    public static final int FLAG_NO_CREATE = 1 << 29;
    public static final int FLAG_CANCEL_CURRENT = 1 << 28;
    public static final int FLAG_UPDATE_CURRENT = 1 << 27;
    public static final int FLAG_IMMUTABLE = 1 << 26;
    public static final int FLAG_MUTABLE = 1 << 25;
    public static final int FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT = 1 << 24;

    static final int TARGET_ACTIVITY = 0;
    static final int TARGET_SERVICE = 1;
    static final int TARGET_BROADCAST = 2;
    static final int TARGET_FOREGROUND_SERVICE = 3;

    private static final String TAG = "PendingIntent";

    private static final Map<Key, PendingIntent> sActive = new HashMap<>();

    public static final Parcelable.Creator<PendingIntent> CREATOR = new Parcelable.Creator<PendingIntent>()
    {
        @Override
        public PendingIntent createFromParcel(Parcel source)
        {
            return (PendingIntent)source.readValue(null);
        }

        @Override
        public PendingIntent[] newArray(int size)
        {
            return new PendingIntent[size];
        }
    };

    private final Context mContext;
    private final int mRequestCode;
    private final Intent[] mIntents;
    private final int mTarget;
    private final int mFlags;
    private final Bundle mOptions;
    private final Key mKey;

    private boolean mCancelled;

    public static class CanceledException extends android.util.AndroidException
    {
        public CanceledException()
        {
        }

        public CanceledException(String name)
        {
            super(name);
        }

        public CanceledException(Exception cause)
        {
            super(cause);
        }
    }

    public interface OnFinished
    {
        void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras);
    }

    private PendingIntent(Context context, int requestCode, Intent[] intents, int target, int flags, Bundle options, Key key)
    {
        mContext = context;
        mRequestCode = requestCode;
        mIntents = new Intent[intents.length];
        for (int index = 0; index < intents.length; index++)
        {
            mIntents[index] = intents[index] == null ? null : new Intent(intents[index]);
        }
        mTarget = target;
        mFlags = flags;
        mOptions = options;
        mKey = key;
    }

    private static PendingIntent obtain(Context context, int requestCode, Intent[] intents, int target, int flags, Bundle options)
    {
        Intent primary = intents.length == 0 ? null : intents[intents.length - 1];
        Key key = new Key(target, requestCode, primary);

        synchronized (sActive)
        {
            PendingIntent existing = sActive.get(key);

            if (existing != null && (flags & FLAG_CANCEL_CURRENT) != 0)
            {
                existing.mCancelled = true;
                sActive.remove(key);
                existing = null;
            }

            if ((flags & FLAG_NO_CREATE) != 0) return existing;

            if (existing != null)
            {
                if ((flags & FLAG_UPDATE_CURRENT) != 0)
                {
                    for (int index = 0; index < intents.length && index < existing.mIntents.length; index++)
                    {
                        if (intents[index] != null && existing.mIntents[index] != null)
                        {
                            existing.mIntents[index].replaceExtras(intents[index]);
                        }
                    }
                }

                return existing;
            }

            PendingIntent created = new PendingIntent(context, requestCode, intents, target, flags, options, key);
            sActive.put(key, created);
            return created;
        }
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags)
    {
        return getActivity(context, requestCode, intent, flags, null);
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags, Bundle options)
    {
        return obtain(context, requestCode, new Intent[] {intent}, TARGET_ACTIVITY, flags, options);
    }

    public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags)
    {
        return getActivities(context, requestCode, intents, flags, null);
    }

    public static PendingIntent getActivities(Context context, int requestCode, Intent[] intents, int flags, Bundle options)
    {
        return obtain(context, requestCode, intents, TARGET_ACTIVITY, flags, options);
    }

    public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags)
    {
        return obtain(context, requestCode, new Intent[] {intent}, TARGET_SERVICE, flags, null);
    }

    public static PendingIntent getForegroundService(Context context, int requestCode, Intent intent, int flags)
    {
        return obtain(context, requestCode, new Intent[] {intent}, TARGET_FOREGROUND_SERVICE, flags, null);
    }

    public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags)
    {
        return obtain(context, requestCode, new Intent[] {intent}, TARGET_BROADCAST, flags, null);
    }

    public int getRequestCode()
    {
        return mRequestCode;
    }

    public Intent getIntent()
    {
        return mIntents.length == 0 ? null : mIntents[mIntents.length - 1];
    }

    public IntentSender getIntentSender()
    {
        return new IntentSender(this);
    }

    public void cancel()
    {
        mCancelled = true;

        synchronized (sActive)
        {
            if (sActive.get(mKey) == this) sActive.remove(mKey);
        }
    }

    public void send()
    {
        try
        {
            send(null, 0, null, null, null, null, null);
        }
        catch (CanceledException cancelled)
        {
            android.util.Log.w(TAG, "a cancelled pending intent was sent and did nothing: " + getIntent());
        }
    }

    public void send(int code)
    {
        try
        {
            send(null, code, null, null, null, null, null);
        }
        catch (CanceledException cancelled)
        {
            android.util.Log.w(TAG, "a cancelled pending intent was sent and did nothing: " + getIntent());
        }
    }

    public void send(Bundle options) throws CanceledException
    {
        send(null, 0, null, null, null, null, options);
    }

    public void send(int code, OnFinished onFinished, Handler handler) throws CanceledException
    {
        send(null, code, null, onFinished, handler, null, null);
    }

    public void send(Context context, int code, Intent intent) throws CanceledException
    {
        send(context, code, intent, null, null, null, null);
    }

    public void send(Context context, int code, Intent intent, OnFinished onFinished, Handler handler) throws CanceledException
    {
        send(context, code, intent, onFinished, handler, null, null);
    }

    public void send(Context context, int code, Intent intent, OnFinished onFinished, Handler handler, String requiredPermission)
        throws CanceledException
    {
        send(context, code, intent, onFinished, handler, requiredPermission, null);
    }

    public void send(Context context, int code, Intent intent, final OnFinished onFinished, Handler handler, String requiredPermission,
                     Bundle options) throws CanceledException
    {
        if (mCancelled) throw new CanceledException("the pending intent was cancelled");

        Intent primary = getIntent();
        if (primary == null) return;

        final Intent sent = new Intent(primary);
        if (intent != null && (mFlags & FLAG_IMMUTABLE) == 0) sent.fillIn(intent, 0);

        if (mTarget == TARGET_ACTIVITY)
        {
            for (int index = 0; index < mIntents.length - 1; index++)
            {
                if (mIntents[index] != null) runtime.Activities.start(new Intent(mIntents[index]), -1);
            }

            runtime.Activities.start(sent, -1);
        }
        else if (mTarget == TARGET_SERVICE || mTarget == TARGET_FOREGROUND_SERVICE)
        {
            runtime.Services.start(sent);
        }
        else
        {
            runtime.Broadcasts.send(sent, null, false);
        }

        if ((mFlags & FLAG_ONE_SHOT) != 0) cancel();

        if (onFinished == null) return;

        final int resultCode = code;
        Runnable finished = new Runnable()
        {
            @Override
            public void run()
            {
                onFinished.onSendFinished(PendingIntent.this, sent, resultCode, null, null);
            }
        };

        if (handler != null) handler.post(finished);
        else finished.run();
    }

    public String getTargetPackage()
    {
        return getCreatorPackage();
    }

    public String getCreatorPackage()
    {
        return mContext == null ? null : mContext.getPackageName();
    }

    public int getCreatorUid()
    {
        return android.os.Process.myUid();
    }

    public android.os.UserHandle getCreatorUserHandle()
    {
        return android.os.Process.myUserHandle();
    }

    public boolean isActivity()
    {
        return mTarget == TARGET_ACTIVITY;
    }

    public boolean isForegroundService()
    {
        return mTarget == TARGET_FOREGROUND_SERVICE;
    }

    public boolean isService()
    {
        return mTarget == TARGET_SERVICE;
    }

    public boolean isBroadcast()
    {
        return mTarget == TARGET_BROADCAST;
    }

    public boolean isImmutable()
    {
        return (mFlags & FLAG_IMMUTABLE) != 0;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeValue(this);
    }

    public static void writePendingIntentOrNullToParcel(PendingIntent sender, Parcel out)
    {
        out.writeValue(sender);
    }

    public static PendingIntent readPendingIntentOrNullFromParcel(Parcel in)
    {
        Object value = in.readValue(null);
        return value instanceof PendingIntent ? (PendingIntent)value : null;
    }

    @Override
    public String toString()
    {
        return "PendingIntent{" + Integer.toHexString(System.identityHashCode(this)) + ": " + describeTarget() + " " + getIntent() + "}";
    }

    private String describeTarget()
    {
        if (mTarget == TARGET_ACTIVITY) return "activity";
        if (mTarget == TARGET_SERVICE) return "service";
        if (mTarget == TARGET_FOREGROUND_SERVICE) return "foreground service";

        return "broadcast";
    }

    private static final class Key
    {
        private final int mTarget;
        private final int mRequestCode;
        private final Intent.FilterComparison mIntent;

        Key(int target, int requestCode, Intent intent)
        {
            mTarget = target;
            mRequestCode = requestCode;
            mIntent = intent == null ? null : new Intent.FilterComparison(new Intent(intent));
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof Key)) return false;

            Key compared = (Key)other;
            return mTarget == compared.mTarget && mRequestCode == compared.mRequestCode && Objects.equals(mIntent, compared.mIntent);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mTarget, mRequestCode, mIntent);
        }
    }
}
