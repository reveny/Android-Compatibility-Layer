package android.text.style;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public final class SuggestionRangeSpan extends CharacterStyle implements ParcelableSpan
{
    public static final Parcelable.Creator<SuggestionRangeSpan> CREATOR = new Parcelable.Creator<SuggestionRangeSpan>()
    {
        @Override
        public SuggestionRangeSpan createFromParcel(Parcel source)
        {
            SuggestionRangeSpan span = new SuggestionRangeSpan();
            span.mBackgroundColor = source.readInt();
            return span;
        }

        @Override
        public SuggestionRangeSpan[] newArray(int size)
        {
            return new SuggestionRangeSpan[size];
        }
    };

    private int mBackgroundColor;

    public SuggestionRangeSpan()
    {
        mBackgroundColor = 0;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mBackgroundColor);
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.SUGGESTION_RANGE_SPAN;
    }

    public void setBackgroundColor(int backgroundColor)
    {
        mBackgroundColor = backgroundColor;
    }

    public int getBackgroundColor()
    {
        return mBackgroundColor;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.bgColor = mBackgroundColor;
    }
}
