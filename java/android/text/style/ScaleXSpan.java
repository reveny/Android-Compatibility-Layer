package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class ScaleXSpan extends MetricAffectingSpan implements ParcelableSpan
{
    private final float mProportion;

    public ScaleXSpan(float proportion)
    {
        mProportion = proportion;
    }

    public ScaleXSpan(Parcel source)
    {
        mProportion = source.readFloat();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.SCALE_X_SPAN;
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

    public float getScaleX()
    {
        return mProportion;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        if (paint != null) paint.setTextScaleX(paint.getTextScaleX() * mProportion);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        if (paint != null) paint.setTextScaleX(paint.getTextScaleX() * mProportion);
    }

    @Override
    public String toString()
    {
        return "ScaleXSpan{scaleX=" + mProportion + "}";
    }
}
