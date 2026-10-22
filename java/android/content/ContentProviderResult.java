package android.content;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class ContentProviderResult implements Parcelable
{
    public static final Parcelable.Creator<ContentProviderResult> CREATOR = new Parcelable.Creator<ContentProviderResult>()
    {
        @Override
        public ContentProviderResult createFromParcel(Parcel source)
        {
            return new ContentProviderResult(source);
        }

        @Override
        public ContentProviderResult[] newArray(int size)
        {
            return new ContentProviderResult[size];
        }
    };

    public final Uri uri;
    public final Integer count;
    public final Bundle extras;
    public final Throwable exception;

    public ContentProviderResult(Uri uri)
    {
        this(uri, null, null, null);
    }

    public ContentProviderResult(int count)
    {
        this(null, count, null, null);
    }

    public ContentProviderResult(Bundle extras)
    {
        this(null, null, extras, null);
    }

    public ContentProviderResult(Throwable exception)
    {
        this(null, null, null, exception);
    }

    public ContentProviderResult(Parcel source)
    {
        uri = (Uri)source.readValue(null);
        count = (Integer)source.readValue(null);
        extras = source.readBundle();
        exception = (Throwable)source.readValue(null);
    }

    private ContentProviderResult(Uri uri, Integer count, Bundle extras, Throwable exception)
    {
        this.uri = uri;
        this.count = count;
        this.extras = extras;
        this.exception = exception;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeValue(uri);
        destination.writeValue(count);
        destination.writeBundle(extras);
        destination.writeValue(exception);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        StringBuilder described = new StringBuilder("ContentProviderResult(");
        if (uri != null) described.append("uri=").append(uri).append(' ');
        if (count != null) described.append("count=").append(count).append(' ');
        if (extras != null) described.append("extras=").append(extras).append(' ');
        if (exception != null) described.append("exception=").append(exception).append(' ');

        if (described.charAt(described.length() - 1) == ' ') described.setLength(described.length() - 1);
        return described.append(')').toString();
    }
}
