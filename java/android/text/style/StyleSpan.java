package android.text.style;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class StyleSpan extends MetricAffectingSpan implements ParcelableSpan
{
    private final int mStyle;
    private final int mFontWeightAdjustment;

    public StyleSpan(int style)
    {
        this(style, Integer.MAX_VALUE);
    }

    public StyleSpan(int style, int fontWeightAdjustment)
    {
        mStyle = style;
        mFontWeightAdjustment = fontWeightAdjustment;
    }

    public StyleSpan(Parcel source)
    {
        mStyle = source.readInt();
        mFontWeightAdjustment = source.readInt();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.STYLE_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mStyle);
        dest.writeInt(mFontWeightAdjustment);
    }

    public int getStyle()
    {
        return mStyle;
    }

    public int getFontWeightAdjustment()
    {
        return mFontWeightAdjustment;
    }

    @Override
    public void updateDrawState(TextPaint paint)
    {
        apply(paint, mStyle);
    }

    @Override
    public void updateMeasureState(TextPaint paint)
    {
        apply(paint, mStyle);
    }

    static void apply(Paint paint, int style)
    {
        if (paint == null) return;

        Typeface old = paint.getTypeface();
        int oldStyle = old == null ? Typeface.NORMAL : old.getStyle();
        int want = oldStyle | style;

        Typeface typeface = old == null ? Typeface.defaultFromStyle(want) : Typeface.create(old, want);
        if (typeface == null)
        {
            if ((want & Typeface.BOLD) != 0) paint.setFakeBoldText(true);
            if ((want & Typeface.ITALIC) != 0) paint.setTextSkewX(-0.25f);
            return;
        }

        int fake = want & ~typeface.getStyle();
        if ((fake & Typeface.BOLD) != 0) paint.setFakeBoldText(true);
        if ((fake & Typeface.ITALIC) != 0) paint.setTextSkewX(-0.25f);

        paint.setTypeface(typeface);
    }

    @Override
    public String toString()
    {
        return "StyleSpan{style=" + mStyle + ", fontWeightAdjustment=" + mFontWeightAdjustment + "}";
    }
}
