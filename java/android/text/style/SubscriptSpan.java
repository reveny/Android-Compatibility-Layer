package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class SubscriptSpan extends MetricAffectingSpan implements ParcelableSpan
{
    public SubscriptSpan()
    {
    }

    public SubscriptSpan(Parcel source)
    {
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.SUBSCRIPT_SPAN;
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
        if (paint != null) paint.baselineShift -= (int)(paint.ascent() / 2);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        if (paint != null) paint.baselineShift -= (int)(paint.ascent() / 2);
    }

    @Override
    public String toString()
    {
        return "SubscriptSpan{}";
    }
}
