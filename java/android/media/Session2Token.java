package android.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import java.util.Objects;

public final class Session2Token implements Parcelable
{
    public static final int TYPE_SESSION = 0;
    public static final int TYPE_SESSION_SERVICE = 1;

    public static final Parcelable.Creator<Session2Token> CREATOR = new Parcelable.Creator<Session2Token>()
    {
        @Override
        public Session2Token createFromParcel(Parcel source)
        {
            int uid = source.readInt();
            int type = source.readInt();
            String packageName = source.readString();
            String serviceName = source.readString();
            Bundle extras = source.readBundle();
            return new Session2Token(uid, type, packageName, serviceName, extras);
        }

        @Override
        public Session2Token[] newArray(int size)
        {
            return new Session2Token[size];
        }
    };

    private final int mUid;
    private final int mType;
    private final String mPackageName;
    private final String mServiceName;
    private final Bundle mExtras;

    public Session2Token(Context context, ComponentName serviceComponent)
    {
        if (context == null) throw new IllegalArgumentException("context shouldn't be null");
        if (serviceComponent == null) throw new IllegalArgumentException("serviceComponent shouldn't be null");

        ApplicationInfo info = context.getPackageName().equals(serviceComponent.getPackageName()) ? context.getApplicationInfo() : null;

        mUid = info != null ? info.uid : Process.myUid();
        mType = TYPE_SESSION_SERVICE;
        mPackageName = serviceComponent.getPackageName();
        mServiceName = serviceComponent.getClassName();
        mExtras = Bundle.EMPTY;
    }

    Session2Token(int uid, int type, String packageName, String serviceName, Bundle extras)
    {
        mUid = uid;
        mType = type;
        mPackageName = packageName;
        mServiceName = serviceName;
        mExtras = extras == null ? Bundle.EMPTY : extras;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public String getServiceName()
    {
        return mServiceName;
    }

    public int getUid()
    {
        return mUid;
    }

    public int getType()
    {
        return mType;
    }

    public Bundle getExtras()
    {
        return new Bundle(mExtras);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mUid);
        dest.writeInt(mType);
        dest.writeString(mPackageName);
        dest.writeString(mServiceName);
        dest.writeBundle(mExtras);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) return true;
        if (!(object instanceof Session2Token)) return false;

        Session2Token other = (Session2Token)object;

        return mUid == other.mUid && mType == other.mType && Objects.equals(mPackageName, other.mPackageName)
                && Objects.equals(mServiceName, other.mServiceName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mType, mUid, mPackageName, mServiceName);
    }

    @Override
    public String toString()
    {
        return "Session2Token {pkg=" + mPackageName + " type=" + mType + " service=" + mServiceName + "}";
    }
}
