package android.content;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

public class IntentSender implements Parcelable
{
    public static final Parcelable.Creator<IntentSender> CREATOR = new Parcelable.Creator<IntentSender>()
    {
        @Override
        public IntentSender createFromParcel(Parcel source)
        {
            return readIntentSenderOrNullFromParcel(source);
        }

        @Override
        public IntentSender[] newArray(int size)
        {
            return new IntentSender[size];
        }
    };

    private final PendingIntent mPendingIntent;

    public IntentSender(PendingIntent pendingIntent)
    {
        mPendingIntent = pendingIntent;
    }

    public static class SendIntentException extends android.util.AndroidException
    {
        public SendIntentException()
        {
        }

        public SendIntentException(String name)
        {
            super(name);
        }

        public SendIntentException(Exception cause)
        {
            super(cause);
        }
    }

    public interface OnFinished
    {
        void onSendFinished(IntentSender intentSender, Intent intent, int resultCode, String resultData, Bundle resultExtras);
    }

    public void sendIntent(Context context, int code, Intent intent, OnFinished onFinished, Handler handler) throws SendIntentException
    {
        sendIntent(context, code, intent, onFinished, handler, null);
    }

    public void sendIntent(Context context, int code, Intent intent, final OnFinished onFinished, Handler handler,
                           String requiredPermission) throws SendIntentException
    {
        PendingIntent.OnFinished finished = null;
        if (onFinished != null)
        {
            finished = new PendingIntent.OnFinished()
            {
                @Override
                public void onSendFinished(PendingIntent pendingIntent, Intent sent, int resultCode, String resultData,
                                           Bundle resultExtras)
                {
                    onFinished.onSendFinished(IntentSender.this, sent, resultCode, resultData, resultExtras);
                }
            };
        }

        try
        {
            mPendingIntent.send(context, code, intent, finished, handler, requiredPermission, null);
        }
        catch (PendingIntent.CanceledException cancelled)
        {
            throw new SendIntentException(cancelled);
        }
    }

    public PendingIntent getPendingIntent()
    {
        return mPendingIntent;
    }

    public String getTargetPackage()
    {
        return mPendingIntent.getTargetPackage();
    }

    public String getCreatorPackage()
    {
        return mPendingIntent.getCreatorPackage();
    }

    public int getCreatorUid()
    {
        return mPendingIntent.getCreatorUid();
    }

    public android.os.UserHandle getCreatorUserHandle()
    {
        return mPendingIntent.getCreatorUserHandle();
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof IntentSender && ((IntentSender)other).mPendingIntent == mPendingIntent;
    }

    @Override
    public int hashCode()
    {
        return System.identityHashCode(mPendingIntent);
    }

    @Override
    public String toString()
    {
        return "IntentSender{" + Integer.toHexString(System.identityHashCode(this)) + ": " + mPendingIntent + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeValue(mPendingIntent);
    }

    public static void writeIntentSenderOrNullToParcel(IntentSender sender, Parcel out)
    {
        out.writeValue(sender == null ? null : sender.mPendingIntent);
    }

    public static IntentSender readIntentSenderOrNullFromParcel(Parcel in)
    {
        Object value = in.readValue(null);
        return value instanceof PendingIntent ? new IntentSender((PendingIntent)value) : null;
    }
}
