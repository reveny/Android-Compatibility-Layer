package android.media;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.Objects;

public class MediaDescription implements Parcelable
{
    public static final long BT_FOLDER_TYPE_ALBUMS = 2L;
    public static final long BT_FOLDER_TYPE_ARTISTS = 3L;
    public static final long BT_FOLDER_TYPE_GENRES = 4L;
    public static final long BT_FOLDER_TYPE_MIXED = 0L;
    public static final long BT_FOLDER_TYPE_PLAYLISTS = 5L;
    public static final long BT_FOLDER_TYPE_TITLES = 1L;
    public static final long BT_FOLDER_TYPE_YEARS = 6L;
    public static final String EXTRA_BT_FOLDER_TYPE = "android.media.extra.BT_FOLDER_TYPE";

    public static final Parcelable.Creator<MediaDescription> CREATOR = new Parcelable.Creator<MediaDescription>()
    {
        @Override
        public MediaDescription createFromParcel(Parcel source)
        {
            return new MediaDescription(source);
        }

        @Override
        public MediaDescription[] newArray(int size)
        {
            return new MediaDescription[size];
        }
    };

    private final String mMediaId;
    private final CharSequence mTitle;
    private final CharSequence mSubtitle;
    private final CharSequence mDescription;
    private final Bitmap mIcon;
    private final Uri mIconUri;
    private final Bundle mExtras;
    private final Uri mMediaUri;

    private MediaDescription(String mediaId, CharSequence title, CharSequence subtitle, CharSequence description, Bitmap icon,
            Uri iconUri, Bundle extras, Uri mediaUri)
    {
        mMediaId = mediaId;
        mTitle = title;
        mSubtitle = subtitle;
        mDescription = description;
        mIcon = icon;
        mIconUri = iconUri;
        mExtras = extras;
        mMediaUri = mediaUri;
    }

    private MediaDescription(Parcel source)
    {
        mMediaId = source.readString();
        mTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mSubtitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mDescription = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mIcon = source.readTypedObject(Bitmap.CREATOR);
        mIconUri = source.readTypedObject(Uri.CREATOR);
        mExtras = source.readBundle();
        mMediaUri = source.readTypedObject(Uri.CREATOR);
    }

    public String getMediaId()
    {
        return mMediaId;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public CharSequence getSubtitle()
    {
        return mSubtitle;
    }

    public CharSequence getDescription()
    {
        return mDescription;
    }

    public Bitmap getIconBitmap()
    {
        return mIcon;
    }

    public Uri getIconUri()
    {
        return mIconUri;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public Uri getMediaUri()
    {
        return mMediaUri;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mMediaId);
        TextUtils.writeToParcel(mTitle, dest, 0);
        TextUtils.writeToParcel(mSubtitle, dest, 0);
        TextUtils.writeToParcel(mDescription, dest, 0);
        dest.writeTypedObject(mIcon, flags);
        dest.writeTypedObject(mIconUri, flags);
        dest.writeBundle(mExtras);
        dest.writeTypedObject(mMediaUri, flags);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) return true;
        if (!(object instanceof MediaDescription)) return false;

        MediaDescription other = (MediaDescription)object;

        return Objects.equals(mMediaId, other.mMediaId) && TextUtils.equals(mTitle, other.mTitle)
                && TextUtils.equals(mSubtitle, other.mSubtitle) && TextUtils.equals(mDescription, other.mDescription)
                && Objects.equals(mIconUri, other.mIconUri) && Objects.equals(mMediaUri, other.mMediaUri);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mMediaId, String.valueOf(mTitle), String.valueOf(mSubtitle), mIconUri, mMediaUri);
    }

    @Override
    public String toString()
    {
        return mTitle + ", " + mSubtitle + ", " + mDescription;
    }

    public static class Builder
    {
        private String mMediaId;
        private CharSequence mTitle;
        private CharSequence mSubtitle;
        private CharSequence mDescription;
        private Bitmap mIcon;
        private Uri mIconUri;
        private Bundle mExtras;
        private Uri mMediaUri;

        public Builder()
        {
        }

        public Builder setMediaId(String mediaId)
        {
            mMediaId = mediaId;
            return this;
        }

        public Builder setTitle(CharSequence title)
        {
            mTitle = title;
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle)
        {
            mSubtitle = subtitle;
            return this;
        }

        public Builder setDescription(CharSequence description)
        {
            mDescription = description;
            return this;
        }

        public Builder setIconBitmap(Bitmap icon)
        {
            mIcon = icon;
            return this;
        }

        public Builder setIconUri(Uri iconUri)
        {
            mIconUri = iconUri;
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public Builder setMediaUri(Uri mediaUri)
        {
            mMediaUri = mediaUri;
            return this;
        }

        public MediaDescription build()
        {
            return new MediaDescription(mMediaId, mTitle, mSubtitle, mDescription, mIcon, mIconUri, mExtras, mMediaUri);
        }
    }
}
