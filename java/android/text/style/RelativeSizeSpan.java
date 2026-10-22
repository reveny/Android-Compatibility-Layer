package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class RelativeSizeSpan extends MetricAffectingSpan implements ParcelableSpan
{
    private final float mProportion;

    public RelativeSizeSpan(float proportion)
    {
        mProportion = proportion;
    }

    public RelativeSizeSpan(Parcel source)
    {
        mProportion = source.readFloat();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.RELATIVE_SIZE_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeFloat(mProportion);
    }

    public float getSizeChange()
    {
        return mProportion;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setTextSize(paint.getTextSize() * mProportion);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        if (paint != null) paint.setTextSize(paint.getTextSize() * mProportion);
    }

    @Override
    public String toString()
    {
        return "RelativeSizeSpan{proportion=" + mProportion + "}";
    }
}
