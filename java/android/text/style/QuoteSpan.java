package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.TextUtils;

public class QuoteSpan implements LeadingMarginSpan, ParcelableSpan
{
    public static final int STANDARD_STRIPE_WIDTH_PX = 2;
    public static final int STANDARD_GAP_WIDTH_PX = 2;
    public static final int STANDARD_COLOR = 0xFF0000FF;

    private final int mColor;
    private final int mStripeWidth;
    private final int mGapWidth;

    public QuoteSpan()
    {
        this(STANDARD_COLOR, STANDARD_STRIPE_WIDTH_PX, STANDARD_GAP_WIDTH_PX);
    }

    public QuoteSpan(int color)
    {
        this(color, STANDARD_STRIPE_WIDTH_PX, STANDARD_GAP_WIDTH_PX);
    }

    public QuoteSpan(int color, int stripeWidth, int gapWidth)
    {
        mColor = color;
        mStripeWidth = stripeWidth;
        mGapWidth = gapWidth;
    }

    public QuoteSpan(Parcel source)
    {
        mColor = source.readInt();
        mStripeWidth = source.readInt();
        mGapWidth = source.readInt();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.QUOTE_SPAN;
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
        dest.writeInt(mStripeWidth);
        dest.writeInt(mGapWidth);
    }

    public int getColor()
    {
        return mColor;
    }

    public int getStripeWidth()
    {
        return mStripeWidth;
    }

    public int getGapWidth()
    {
        return mGapWidth;
    }

    @Override
    public int getLeadingMargin(boolean first)
    {
        return mStripeWidth + mGapWidth;
    }

    @Override
    public void drawLeadingMargin(Canvas canvas, Paint paint, int x, int direction, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout)
    {
        Paint.Style style = paint.getStyle();
        int color = paint.getColor();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mColor);

        canvas.drawRect(x, top, x + direction * mStripeWidth, bottom, paint);

        paint.setStyle(style);
        paint.setColor(color);
    }

    @Override
    public String toString()
    {
        return "QuoteSpan{color=#" + String.format("%08X", mColor) + ", stripeWidth=" + mStripeWidth + ", gapWidth=" + mGapWidth + "}";
    }
}
