package android.view.contentcapture;

import android.os.Parcel;
import android.os.Parcelable;

public final class ContentCaptureCondition implements Parcelable
{
    public static final int FLAG_IS_REGEX = 2;

    public static final Parcelable.Creator<ContentCaptureCondition> CREATOR = new Parcelable.Creator<ContentCaptureCondition>()
    {
        @Override
        public ContentCaptureCondition createFromParcel(Parcel source)
        {
            return new ContentCaptureCondition(source.readString(), source.readInt());
        }

        @Override
        public ContentCaptureCondition[] newArray(int size)
        {
            return new ContentCaptureCondition[size];
        }
    };

    private final String mLocus;
    private final int mFlags;

    private ContentCaptureCondition(String locus, int flags)
    {
        mLocus = locus;
        mFlags = flags;
    }

    public ContentCaptureCondition(android.content.LocusId locusId, int flags)
    {
        this(locusId.getId(), flags);
    }

    public android.content.LocusId getLocusId()
    {
        return new android.content.LocusId(mLocus);
    }

    public int getFlags()
    {
        return mFlags;
    }

    @Override
    public int hashCode()
    {
        return (mLocus == null ? 0 : mLocus.hashCode()) * 31 + mFlags;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof ContentCaptureCondition)) return false;

        ContentCaptureCondition condition = (ContentCaptureCondition)other;
        return mFlags == condition.mFlags && java.util.Objects.equals(mLocus, condition.mLocus);
    }

    @Override
    public String toString()
    {
        return "ContentCaptureCondition[locus=" + mLocus + ", flags=" + mFlags + "]";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeString(mLocus);
        parcel.writeInt(mFlags);
    }
}
