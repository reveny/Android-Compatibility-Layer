package android.view.autofill;

import android.os.Parcel;
import android.os.Parcelable;

public final class AutofillId implements Parcelable
{
    public static final AutofillId NO_AUTOFILL_ID = new AutofillId(0);

    private static final int NO_VIRTUAL_ID = -1;

    public static final Parcelable.Creator<AutofillId> CREATOR = new Parcelable.Creator<AutofillId>()
    {
        @Override
        public AutofillId createFromParcel(Parcel source)
        {
            int viewId = source.readInt();
            long virtualId = source.readLong();
            int sessionId = source.readInt();
            return new AutofillId(viewId, virtualId, sessionId);
        }

        @Override
        public AutofillId[] newArray(int size)
        {
            return new AutofillId[size];
        }
    };

    private final int mViewId;
    private final long mVirtualId;
    private final int mSessionId;

    public AutofillId(int id)
    {
        this(id, NO_VIRTUAL_ID, 0);
    }

    public AutofillId(int parentId, int virtualChildId)
    {
        this(parentId, virtualChildId, 0);
    }

    public AutofillId(AutofillId parent, long virtualChildId, int sessionId)
    {
        this(parent == null ? 0 : parent.mViewId, virtualChildId, sessionId);
    }

    private AutofillId(int viewId, long virtualId, int sessionId)
    {
        mViewId = viewId;
        mVirtualId = virtualId;
        mSessionId = sessionId;
    }

    public static AutofillId create(android.view.View host, int virtualId)
    {
        if (host == null) throw new NullPointerException("host cannot be null");

        return new AutofillId(host.getAutofillId().mViewId, virtualId);
    }

    public int getViewId()
    {
        return mViewId;
    }

    public boolean isVirtualInt()
    {
        return mVirtualId != NO_VIRTUAL_ID && mVirtualId >= Integer.MIN_VALUE && mVirtualId <= Integer.MAX_VALUE;
    }

    public boolean isVirtualLong()
    {
        return mVirtualId != NO_VIRTUAL_ID && !isVirtualInt();
    }

    public boolean isNonVirtual()
    {
        return mVirtualId == NO_VIRTUAL_ID;
    }

    public int getVirtualChildIntId()
    {
        return (int)mVirtualId;
    }

    public long getVirtualChildLongId()
    {
        return mVirtualId;
    }

    public boolean isVirtual()
    {
        return !isNonVirtual();
    }

    public int getAutofillVirtualId()
    {
        return isVirtualInt() ? (int)mVirtualId : android.view.View.NO_ID;
    }

    public boolean isInAutofillSession()
    {
        return mSessionId != 0;
    }

    public int getSessionId()
    {
        return mSessionId;
    }

    @Override
    public int hashCode()
    {
        return mViewId * 31 + (int)(mVirtualId ^ (mVirtualId >>> 32));
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof AutofillId)) return false;

        AutofillId id = (AutofillId)other;
        return id.mViewId == mViewId && id.mVirtualId == mVirtualId;
    }

    @Override
    public String toString()
    {
        if (mVirtualId == NO_VIRTUAL_ID) return Integer.toString(mViewId);
        return mViewId + ":" + mVirtualId;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mViewId);
        parcel.writeLong(mVirtualId);
        parcel.writeInt(mSessionId);
    }
}
