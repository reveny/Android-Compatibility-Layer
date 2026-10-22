package android.view.inputmethod;

import android.content.ClipDescription;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class InputContentInfo implements Parcelable
{
    private final Uri mContentUri;
    private final ClipDescription mDescription;
    private final Uri mLinkUri;

    public InputContentInfo(Uri contentUri, ClipDescription description)
    {
        this(contentUri, description, null);
    }

    public InputContentInfo(Uri contentUri, ClipDescription description, Uri linkUri)
    {
        if (contentUri == null) throw new NullPointerException("contentUri");
        if (description == null) throw new NullPointerException("description");

        mContentUri = contentUri;
        mDescription = description;
        mLinkUri = linkUri;
    }

    public Uri getContentUri()
    {
        return mContentUri;
    }

    public ClipDescription getDescription()
    {
        return mDescription;
    }

    public Uri getLinkUri()
    {
        return mLinkUri;
    }

    public void requestPermission()
    {
    }

    public void releasePermission()
    {
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(mContentUri);
        dest.writeValue(mDescription);
        dest.writeValue(mLinkUri);
    }

    public static final Creator<InputContentInfo> CREATOR = new Creator<InputContentInfo>()
    {
        @Override
        public InputContentInfo createFromParcel(Parcel source)
        {
            Uri contentUri = (Uri)source.readValue(null);
            ClipDescription description = (ClipDescription)source.readValue(null);
            Uri linkUri = (Uri)source.readValue(null);
            return new InputContentInfo(contentUri, description, linkUri);
        }

        @Override
        public InputContentInfo[] newArray(int size)
        {
            return new InputContentInfo[size];
        }
    };
}
