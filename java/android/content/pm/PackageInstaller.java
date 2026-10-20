package android.content.pm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class PackageInstaller
{
    public static final String ACTION_SESSION_COMMITTED = "android.content.pm.action.SESSION_COMMITTED";
    public static final String ACTION_SESSION_UPDATED = "android.content.pm.action.SESSION_UPDATED";
    public static final String EXTRA_SESSION = "android.content.pm.extra.SESSION";
    public static final String EXTRA_SESSION_ID = "android.content.pm.extra.SESSION_ID";
    public static final String EXTRA_STATUS = "android.content.pm.extra.STATUS";
    public static final String EXTRA_PACKAGE_NAME = "android.content.pm.extra.PACKAGE_NAME";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAILURE = 1;

    private final List<SessionCallback> mCallbacks = new ArrayList<>();
    private final List<SessionInfo> mSessions = new ArrayList<>();

    public List<SessionInfo> getAllSessions()
    {
        return new ArrayList<>(mSessions);
    }

    public List<SessionInfo> getMySessions()
    {
        return new ArrayList<>(mSessions);
    }

    public List<SessionInfo> getActiveStagedSessions()
    {
        return new ArrayList<>();
    }

    public SessionInfo getSessionInfo(int sessionId)
    {
        for (SessionInfo session : mSessions)
        {
            if (session.getSessionId() == sessionId) return session;
        }

        return null;
    }

    public void registerSessionCallback(SessionCallback callback)
    {
        if (callback != null && !mCallbacks.contains(callback)) mCallbacks.add(callback);
    }

    public void registerSessionCallback(SessionCallback callback, Handler handler)
    {
        registerSessionCallback(callback);
    }

    public void unregisterSessionCallback(SessionCallback callback)
    {
        mCallbacks.remove(callback);
    }

    public void abandonSession(int sessionId)
    {
        SessionInfo session = getSessionInfo(sessionId);
        if (session != null) mSessions.remove(session);
    }

    public abstract static class SessionCallback
    {
        public abstract void onCreated(int sessionId);

        public abstract void onBadgingChanged(int sessionId);

        public abstract void onActiveChanged(int sessionId, boolean active);

        public abstract void onProgressChanged(int sessionId, float progress);

        public abstract void onFinished(int sessionId, boolean success);
    }

    public static class SessionInfo implements Parcelable
    {
        private int mSessionId;
        private String mInstallerPackageName;
        private String mAppPackageName;
        private CharSequence mAppLabel;
        private Bitmap mAppIcon;
        private float mProgress;
        private boolean mActive;
        private int mInstallReason;

        public int getSessionId()
        {
            return mSessionId;
        }

        public String getInstallerPackageName()
        {
            return mInstallerPackageName;
        }

        public String getAppPackageName()
        {
            return mAppPackageName;
        }

        public CharSequence getAppLabel()
        {
            return mAppLabel;
        }

        public Bitmap getAppIcon()
        {
            return mAppIcon;
        }

        public float getProgress()
        {
            return mProgress;
        }

        public boolean isActive()
        {
            return mActive;
        }

        public int getInstallReason()
        {
            return mInstallReason;
        }

        public Intent createDetailsIntent()
        {
            return new Intent();
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
            parcel.writeValue(Integer.valueOf(mSessionId));
            parcel.writeValue(mAppPackageName);
            parcel.writeValue(mInstallerPackageName);
        }

        public static final Creator<SessionInfo> CREATOR = new Creator<SessionInfo>()
        {
            @Override
            public SessionInfo createFromParcel(Parcel parcel)
            {
                SessionInfo session = new SessionInfo();

                Object sessionId = parcel.readValue(null);
                session.mSessionId = sessionId instanceof Integer ? ((Integer)sessionId).intValue() : 0;
                session.mAppPackageName = (String)parcel.readValue(null);
                session.mInstallerPackageName = (String)parcel.readValue(null);

                return session;
            }

            @Override
            public SessionInfo[] newArray(int size)
            {
                return new SessionInfo[size];
            }
        };
    }

    public static class SessionParams
    {
        public static final int MODE_FULL_INSTALL = 1;
        public static final int MODE_INHERIT_EXISTING = 2;

        private final int mMode;

        public SessionParams(int mode)
        {
            mMode = mode;
        }

        public int getMode()
        {
            return mMode;
        }

        public void setAppPackageName(String packageName)
        {
        }

        public void setAppLabel(CharSequence label)
        {
        }

        public void setAppIcon(Bitmap icon)
        {
        }

        public void setInstallReason(int reason)
        {
        }
    }
}
