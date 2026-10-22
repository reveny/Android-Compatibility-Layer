package android.text.style;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextUtils;
import android.view.View;

public class URLSpan extends ClickableSpan implements ParcelableSpan
{
    private static final String EXTRA_APPLICATION_ID = "com.android.browser.application_id";

    private final String mURL;

    public URLSpan(String url)
    {
        mURL = url;
    }

    public URLSpan(Parcel source)
    {
        mURL = source.readString();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.URL_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mURL);
    }

    public String getURL()
    {
        return mURL;
    }

    @Override
    public void onClick(View widget)
    {
        if (widget == null || mURL == null) return;

        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mURL));
        intent.putExtra(EXTRA_APPLICATION_ID, context.getPackageName());

        try
        {
            context.startActivity(intent);
        }
        catch (RuntimeException exception)
        {
            android.util.Log.w("URLSpan", "Actvity was not found for intent, " + intent.toString());
        }
    }

    @Override
    public String toString()
    {
        return "URLSpan{URL='" + mURL + "'}";
    }
}
