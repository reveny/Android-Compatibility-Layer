package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class ForegroundColorSpan extends CharacterStyle implements UpdateAppearance, ParcelableSpan
{
    private final int mColor;

    public ForegroundColorSpan(int color)
    {
        mColor = color;
    }

    public ForegroundColorSpan(Parcel source)
    {
        mColor = source.readInt();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.FOREGROUND_COLOR_SPAN;
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

    public int getForegroundColor()
    {
        return mColor;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setColor(mColor);
    }

    @Override
    public String toString()
    {
        return "ForegroundColorSpan{color=#" + String.format("%08X", mColor) + "}";
    }
}
