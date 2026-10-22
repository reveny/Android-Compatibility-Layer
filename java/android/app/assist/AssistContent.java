package android.app.assist;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class AssistContent implements Parcelable
{
    public static final String EXTRA_APP_FUNCTION_DATA = "android.app.assist.extra.APP_FUNCTION_DATA";

    private boolean mIsAppProvidedIntent;
    private boolean mIsAppProvidedWebUri;
    private Intent mIntent;
    private String mStructuredData;
    private ClipData mClipData;
    private Uri mUri;
    private Uri mSessionTransferUri;
    private final Bundle mExtras;

    public AssistContent()
    {
        mExtras = new Bundle();
    }

    AssistContent(Parcel in)
    {
        if (in.readInt() != 0) mIntent = Intent.CREATOR.createFromParcel(in);
        if (in.readInt() != 0) mClipData = ClipData.CREATOR.createFromParcel(in);
        if (in.readInt() != 0) mUri = Uri.CREATOR.createFromParcel(in);
        if (in.readInt() != 0) mStructuredData = in.readString();
        if (in.readInt() != 0) mSessionTransferUri = Uri.CREATOR.createFromParcel(in);

        mIsAppProvidedIntent = in.readInt() == 1;
        Bundle extras = in.readBundle();
        mExtras = extras != null ? extras : new Bundle();
        mIsAppProvidedWebUri = in.readInt() == 1;
    }

    public void setDefaultIntent(Intent intent)
    {
        mIntent = intent;
        mIsAppProvidedIntent = false;
        mUri = null;

        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()))
        {
            Uri uri = intent.getData();

            if (uri != null && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) mUri = uri;
        }
    }

    public void setIntent(Intent intent)
    {
        mIsAppProvidedIntent = true;
        mIntent = intent;
    }

    public Intent getIntent()
    {
        return mIntent;
    }

    public boolean isAppProvidedIntent()
    {
        return mIsAppProvidedIntent;
    }

    public void setClipData(ClipData clip)
    {
        mClipData = clip;
    }

    public ClipData getClipData()
    {
        return mClipData;
    }

    public void setStructuredData(String structuredData)
    {
        mStructuredData = structuredData;
    }

    public String getStructuredData()
    {
        return mStructuredData;
    }

    public void setWebUri(Uri uri)
    {
        mIsAppProvidedWebUri = true;
        mUri = uri;
    }

    public Uri getWebUri()
    {
        return mUri;
    }

    public boolean isAppProvidedWebUri()
    {
        return mIsAppProvidedWebUri;
    }

    public void setSessionTransferUri(Uri uri)
    {
        mSessionTransferUri = uri;
    }

    public Uri getSessionTransferUri()
    {
        return mSessionTransferUri;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        writeOptional(dest, mIntent, flags);
        writeOptional(dest, mClipData, flags);
        writeOptional(dest, mUri, flags);

        dest.writeInt(mStructuredData != null ? 1 : 0);
        if (mStructuredData != null) dest.writeString(mStructuredData);

        writeOptional(dest, mSessionTransferUri, flags);

        dest.writeInt(mIsAppProvidedIntent ? 1 : 0);
        dest.writeBundle(mExtras);
        dest.writeInt(mIsAppProvidedWebUri ? 1 : 0);
    }

    private static void writeOptional(Parcel dest, Parcelable value, int flags)
    {
        dest.writeInt(value != null ? 1 : 0);
        if (value != null) value.writeToParcel(dest, flags);
    }

    public static final Parcelable.Creator<AssistContent> CREATOR = new Parcelable.Creator<AssistContent>()
    {
        @Override
        public AssistContent createFromParcel(Parcel in)
        {
            return new AssistContent(in);
        }

        @Override
        public AssistContent[] newArray(int size)
        {
            return new AssistContent[size];
        }
    };
}
