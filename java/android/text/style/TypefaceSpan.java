package android.text.style;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.TextUtils;

public class TypefaceSpan extends MetricAffectingSpan implements ParcelableSpan
{
    private final String mFamily;
    private final Typeface mTypeface;

    public TypefaceSpan(String family)
    {
        mFamily = family;
        mTypeface = null;
    }

    public TypefaceSpan(Typeface typeface)
    {
        mFamily = null;
        mTypeface = typeface;
    }

    public TypefaceSpan(Parcel source)
    {
        mFamily = source.readString();
        mTypeface = null;
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.TYPEFACE_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mFamily);
    }

    public String getFamily()
    {
        return mFamily;
    }

    public Typeface getTypeface()
    {
        return mTypeface;
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

    private void apply(Paint paint)
    {
        if (paint == null) return;

        if (mTypeface != null)
        {
            Typeface old = paint.getTypeface();
            int oldStyle = old == null ? Typeface.NORMAL : old.getStyle();
            Typeface styled = Typeface.create(mTypeface, oldStyle);
            int fake = oldStyle & ~styled.getStyle();

            if ((fake & Typeface.BOLD) != 0) paint.setFakeBoldText(true);
            if ((fake & Typeface.ITALIC) != 0) paint.setTextSkewX(-0.25f);

            paint.setTypeface(styled);
            return;
        }

        if (mFamily == null) return;

        Typeface old = paint.getTypeface();
        int oldStyle = old == null ? Typeface.NORMAL : old.getStyle();
        Typeface styled = Typeface.create(mFamily, oldStyle);
        if (styled == null) return;

        int fake = oldStyle & ~styled.getStyle();
        if ((fake & Typeface.BOLD) != 0) paint.setFakeBoldText(true);
        if ((fake & Typeface.ITALIC) != 0) paint.setTextSkewX(-0.25f);

        paint.setTypeface(styled);
    }

    @Override
    public String toString()
    {
        return "TypefaceSpan{family='" + mFamily + "', typeface=" + mTypeface + "}";
    }
}
