package android.text.style;

import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class AbsoluteSizeSpan extends MetricAffectingSpan implements ParcelableSpan
{
    private final int mSize;
    private final boolean mDip;

    public AbsoluteSizeSpan(int size)
    {
        this(size, false);
    }

    public AbsoluteSizeSpan(int size, boolean dip)
    {
        mSize = size;
        mDip = dip;
    }

    public AbsoluteSizeSpan(Parcel source)
    {
        mSize = source.readInt();
        mDip = source.readInt() != 0;
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.ABSOLUTE_SIZE_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mSize);
        dest.writeInt(mDip ? 1 : 0);
    }

    public int getSize()
    {
        return mSize;
    }

    public boolean getDip()
    {
        return mDip;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        apply(paint);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        apply(paint);
    }

    private void apply(TextPaint paint)
    {
        if (paint == null) return;

        if (mDip) paint.setTextSize(mSize * paint.density);
        else paint.setTextSize(mSize);
    }

    @Override
    public String toString()
    {
        return "AbsoluteSizeSpan{size=" + mSize + ", isDip=" + mDip + "}";
    }
}
