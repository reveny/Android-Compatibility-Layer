package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class UnderlineSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan
{
    public UnderlineSpan()
    {
    }

    public UnderlineSpan(Parcel source)
    {
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.UNDERLINE_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setUnderlineText(true);
    }

    @Override
    public String toString()
    {
        return "UnderlineSpan{}";
    }
}
