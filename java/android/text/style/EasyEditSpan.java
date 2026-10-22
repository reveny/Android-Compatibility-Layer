package android.text.style;

import android.app.PendingIntent;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextUtils;

public class EasyEditSpan implements ParcelableSpan
{
    public static final String EXTRA_TEXT_CHANGED_TYPE = "android.text.style.EXTRA_TEXT_CHANGED_TYPE";
    public static final int TEXT_DELETED = 1;
    public static final int TEXT_MODIFIED = 2;

    private final PendingIntent mPendingIntent;
    private boolean mDeleteEnabled;

    public EasyEditSpan()
    {
        mPendingIntent = null;
        mDeleteEnabled = true;
    }

    public EasyEditSpan(PendingIntent pendingIntent)
    {
        mPendingIntent = pendingIntent;
        mDeleteEnabled = true;
    }

    public EasyEditSpan(Parcel source)
    {
        mPendingIntent = (PendingIntent)source.readValue(null);
        mDeleteEnabled = source.readInt() == 1;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(mPendingIntent);
        dest.writeInt(mDeleteEnabled ? 1 : 0);
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.EASY_EDIT_SPAN;
    }

    public boolean isDeleteEnabled()
    {
        return mDeleteEnabled;
    }

    public void setDeleteEnabled(boolean enabled)
    {
        mDeleteEnabled = enabled;
    }

    public PendingIntent getPendingIntent()
    {
        return mPendingIntent;
    }
}
