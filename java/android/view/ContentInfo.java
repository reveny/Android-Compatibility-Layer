package android.view;

import android.content.ClipData;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public final class ContentInfo implements Parcelable
{
    public static final int SOURCE_APP = 0;
    public static final int SOURCE_CLIPBOARD = 1;
    public static final int SOURCE_INPUT_METHOD = 2;
    public static final int SOURCE_DRAG_AND_DROP = 3;
    public static final int SOURCE_AUTOFILL = 4;
    public static final int SOURCE_PROCESS_TEXT = 5;

    public static final int FLAG_CONVERT_TO_PLAIN_TEXT = 1;

    public static final Parcelable.Creator<ContentInfo> CREATOR = new Parcelable.Creator<ContentInfo>()
    {
        @Override
        public ContentInfo createFromParcel(Parcel source)
        {
            ClipData clip = ClipData.CREATOR.createFromParcel(source);
            int contentSource = source.readInt();
            int flags = source.readInt();
            String link = source.readString();
            Bundle extras = source.readBundle();

            Builder builder = new Builder(clip, contentSource).setFlags(flags).setExtras(extras);
            if (link != null) builder.setLinkUri(Uri.parse(link));
            return builder.build();
        }

        @Override
        public ContentInfo[] newArray(int size)
        {
            return new ContentInfo[size];
        }
    };

    private final ClipData mClip;
    private final int mSource;
    private final int mFlags;
    private final Uri mLinkUri;
    private final Bundle mExtras;

    private ContentInfo(Builder builder)
    {
        mClip = builder.mClip;
        mSource = builder.mSource;
        mFlags = builder.mFlags;
        mLinkUri = builder.mLinkUri;
        mExtras = builder.mExtras;
    }

    public ClipData getClip()
    {
        return mClip;
    }

    public int getSource()
    {
        return mSource;
    }

    public int getFlags()
    {
        return mFlags;
    }

    public Uri getLinkUri()
    {
        return mLinkUri;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    @Override
    public String toString()
    {
        return "ContentInfo{clip=" + mClip + ", source=" + mSource + ", flags=" + mFlags + ", linkUri=" + mLinkUri + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        mClip.writeToParcel(dest, flags);
        dest.writeInt(mSource);
        dest.writeInt(mFlags);
        dest.writeString(mLinkUri == null ? null : mLinkUri.toString());
        dest.writeBundle(mExtras);
    }

    public static final class Builder
    {
        private ClipData mClip;
        private int mSource;
        private int mFlags;
        private Uri mLinkUri;
        private Bundle mExtras;

        public Builder(ContentInfo other)
        {
            mClip = other.mClip;
            mSource = other.mSource;
            mFlags = other.mFlags;
            mLinkUri = other.mLinkUri;
            mExtras = other.mExtras;
        }

        public Builder(ClipData clip, int source)
        {
            mClip = clip;
            mSource = source;
        }

        public Builder setClip(ClipData clip)
        {
            mClip = clip;
            return this;
        }

        public Builder setSource(int source)
        {
            mSource = source;
            return this;
        }

        public Builder setFlags(int flags)
        {
            mFlags = flags;
            return this;
        }

        public Builder setLinkUri(Uri linkUri)
        {
            mLinkUri = linkUri;
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public ContentInfo build()
        {
            if (mClip == null) throw new NullPointerException("clip must not be null");
            if (mSource < SOURCE_APP || mSource > SOURCE_PROCESS_TEXT) throw new IllegalArgumentException("Invalid source " + mSource);
            if ((mFlags & ~FLAG_CONVERT_TO_PLAIN_TEXT) != 0) throw new IllegalArgumentException("Invalid flags " + mFlags);
            return new ContentInfo(this);
        }
    }
}
