package android.view.contentcapture;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public final class ContentCaptureContext implements Parcelable
{
    public static final Parcelable.Creator<ContentCaptureContext> CREATOR = new Parcelable.Creator<ContentCaptureContext>()
    {
        @Override
        public ContentCaptureContext createFromParcel(Parcel source)
        {
            return new ContentCaptureContext(source.readString(), source.readBundle());
        }

        @Override
        public ContentCaptureContext[] newArray(int size)
        {
            return new ContentCaptureContext[size];
        }
    };

    private final String mLocus;
    private final Bundle mExtras;

    private ContentCaptureContext(String locus, Bundle extras)
    {
        mLocus = locus;
        mExtras = extras;
    }

    public static ContentCaptureContext forLocusId(String id)
    {
        return new ContentCaptureContext(id, null);
    }

    public static final class Builder
    {
        private final android.content.LocusId mId;
        private Bundle mExtras;
        private boolean mDestroyed;

        public Builder(android.content.LocusId id)
        {
            if (id == null) throw new NullPointerException("id");

            mId = id;
        }

        public Builder setExtras(Bundle extras)
        {
            if (extras == null) throw new NullPointerException("extras");
            if (mDestroyed) throw new IllegalStateException("Already called #build()");

            mExtras = extras;
            return this;
        }

        public ContentCaptureContext build()
        {
            if (mDestroyed) throw new IllegalStateException("Already called #build()");

            mDestroyed = true;
            return new ContentCaptureContext(mId.getId(), mExtras);
        }
    }

    public android.content.LocusId getLocusId()
    {
        return mLocus == null ? null : new android.content.LocusId(mLocus);
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    @Override
    public String toString()
    {
        return "Context[locus=" + mLocus + ", hasExtras=" + (mExtras != null) + "]";
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
        parcel.writeBundle(mExtras);
    }
}
