package android.text.style;

import android.text.TextPaint;

public class AbsoluteSizeSpan extends MetricAffectingSpan
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
        if (paint != null) paint.setTextSize(mSize);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        if (paint != null) paint.setTextSize(mSize);
    }
}
