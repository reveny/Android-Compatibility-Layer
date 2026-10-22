package android.media.tv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public final class TvInputInfo implements Parcelable
{
    public static final String EXTRA_INPUT_ID = "android.media.tv.extra.INPUT_ID";
    public static final int TYPE_COMPONENT = 1004;
    public static final int TYPE_COMPOSITE = 1001;
    public static final int TYPE_DISPLAY_PORT = 1008;
    public static final int TYPE_DVI = 1006;
    public static final int TYPE_HDMI = 1007;
    public static final int TYPE_OTHER = 1000;
    public static final int TYPE_SCART = 1003;
    public static final int TYPE_SVIDEO = 1002;
    public static final int TYPE_TUNER = 0;
    public static final int TYPE_VGA = 1005;

    public static final Parcelable.Creator<TvInputInfo> CREATOR = new Parcelable.Creator<TvInputInfo>()
    {
        @Override
        public TvInputInfo createFromParcel(Parcel source)
        {
            ServiceInfo service = ServiceInfo.CREATOR.createFromParcel(source);
            String id = source.readString();
            int type = source.readInt();
            String parentId = source.readString();
            String setupActivity = source.readString();
            boolean canRecord = source.readInt() != 0;
            boolean canPauseRecording = source.readInt() != 0;
            int tunerCount = source.readInt();
            Bundle extras = source.readBundle();
            return new TvInputInfo(service, id, type, parentId, setupActivity, canRecord, canPauseRecording, tunerCount, extras);
        }

        @Override
        public TvInputInfo[] newArray(int size)
        {
            return new TvInputInfo[size];
        }
    };

    private final ServiceInfo mService;
    private final String mId;
    private final int mType;
    private final String mParentId;
    private final String mSetupActivity;
    private final boolean mCanRecord;
    private final boolean mCanPauseRecording;
    private final int mTunerCount;
    private final Bundle mExtras;

    private TvInputInfo(ServiceInfo service, String id, int type, String parentId, String setupActivity, boolean canRecord,
        boolean canPauseRecording, int tunerCount, Bundle extras)
    {
        mService = service;
        mId = id;
        mType = type;
        mParentId = parentId;
        mSetupActivity = setupActivity;
        mCanRecord = canRecord;
        mCanPauseRecording = canPauseRecording;
        mTunerCount = tunerCount;
        mExtras = extras;
    }

    public boolean canPauseRecording()
    {
        return mCanPauseRecording;
    }

    public boolean canRecord()
    {
        return mCanRecord;
    }

    public Intent createSettingsIntent()
    {
        return null;
    }

    public Intent createSetupIntent()
    {
        if (mSetupActivity == null || mSetupActivity.isEmpty()) return null;

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(mService.packageName, mSetupActivity);
        intent.putExtra(EXTRA_INPUT_ID, mId);
        return intent;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof TvInputInfo)) return false;

        TvInputInfo other = (TvInputInfo)object;
        return mId.equals(other.mId) && mType == other.mType && mCanRecord == other.mCanRecord
            && mCanPauseRecording == other.mCanPauseRecording && mTunerCount == other.mTunerCount;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public String getId()
    {
        return mId;
    }

    public String getParentId()
    {
        return mParentId;
    }

    public ServiceInfo getServiceInfo()
    {
        return mService;
    }

    public int getTunerCount()
    {
        return mTunerCount;
    }

    public int getType()
    {
        return mType;
    }

    @Override
    public int hashCode()
    {
        return mId.hashCode();
    }

    public boolean isHidden(Context context)
    {
        return false;
    }

    public boolean isPassthroughInput()
    {
        return mType != TYPE_TUNER;
    }

    public CharSequence loadCustomLabel(Context context)
    {
        return null;
    }

    public Drawable loadIcon(Context context)
    {
        if (context == null || context.getPackageManager() == null) return null;

        return mService.loadIcon(context.getPackageManager());
    }

    public CharSequence loadLabel(Context context)
    {
        if (context == null || context.getPackageManager() == null) return mId;

        return mService.loadLabel(context.getPackageManager());
    }

    @Override
    public String toString()
    {
        return "TvInputInfo{id=" + mId + ", pkg=" + mService.packageName + ", service=" + mService.name + "}";
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        mService.writeToParcel(destination, flags);
        destination.writeString(mId);
        destination.writeInt(mType);
        destination.writeString(mParentId);
        destination.writeString(mSetupActivity);
        destination.writeInt(mCanRecord ? 1 : 0);
        destination.writeInt(mCanPauseRecording ? 1 : 0);
        destination.writeInt(mTunerCount);
        destination.writeBundle(mExtras);
    }

    public static final class Builder
    {
        private final Context mContext;
        private final ComponentName mComponent;
        private Boolean mCanRecord;
        private Boolean mCanPauseRecording;
        private Integer mTunerCount;
        private Bundle mExtras;

        public Builder(Context context, ComponentName component)
        {
            if (context == null) throw new IllegalArgumentException("context cannot be null.");
            if (component == null) throw new IllegalArgumentException("component cannot be null.");

            mContext = context;
            mComponent = component;
        }

        public TvInputInfo build()
        {
            ServiceInfo service;

            try
            {
                service = mContext.getPackageManager().getServiceInfo(mComponent, PackageManager.GET_META_DATA);
            }
            catch (PackageManager.NameNotFoundException exception)
            {
                throw new IllegalArgumentException("Invalid component. Can't find the service.");
            }

            if (service.packageName == null) service.packageName = mComponent.getPackageName();
            if (service.name == null) service.name = mComponent.getClassName();

            String id = new ComponentName(service.packageName, service.name).flattenToShortString();
            boolean canRecord = mCanRecord != null && mCanRecord;
            boolean canPauseRecording = mCanPauseRecording != null ? mCanPauseRecording : canRecord;
            int tunerCount = mTunerCount != null ? mTunerCount : 1;
            return new TvInputInfo(service, id, TYPE_TUNER, null, null, canRecord, canPauseRecording, tunerCount, mExtras);
        }

        public Builder setCanPauseRecording(boolean canPauseRecording)
        {
            mCanPauseRecording = canPauseRecording;
            return this;
        }

        public Builder setCanRecord(boolean canRecord)
        {
            mCanRecord = canRecord;
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public Builder setTunerCount(int tunerCount)
        {
            mTunerCount = tunerCount;
            return this;
        }
    }
}
