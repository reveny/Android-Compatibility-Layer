package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class BackgroundColorSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan
{
    private final int mColor;

    public BackgroundColorSpan(int color)
    {
        mColor = color;
    }

    public BackgroundColorSpan(Parcel source)
    {
        mColor = source.readInt();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.BACKGROUND_COLOR_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mColor);
    }

    public int getBackgroundColor()
    {
        return mColor;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.bgColor = mColor;
    }

    @Override
    public String toString()
    {
        return "BackgroundColorSpan{color=#" + String.format("%08X", mColor) + "}";
    }
}
