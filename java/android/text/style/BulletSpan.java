package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.Spanned;
import android.text.TextUtils;

public class BulletSpan implements LeadingMarginSpan, ParcelableSpan
{
    public static final int STANDARD_GAP_WIDTH = 2;

    private static final int STANDARD_BULLET_RADIUS = 4;
    private static final int STANDARD_COLOR = 0;

    private final int mGapWidth;
    private final int mBulletRadius;
    private final int mColor;
    private final boolean mWantColor;

    public BulletSpan()
    {
        this(STANDARD_GAP_WIDTH, STANDARD_COLOR, false, STANDARD_BULLET_RADIUS);
    }

    public BulletSpan(int gapWidth)
    {
        this(gapWidth, STANDARD_COLOR, false, STANDARD_BULLET_RADIUS);
    }

    public BulletSpan(int gapWidth, int color)
    {
        this(gapWidth, color, true, STANDARD_BULLET_RADIUS);
    }

    public BulletSpan(int gapWidth, int color, int bulletRadius)
    {
        this(gapWidth, color, true, bulletRadius);
    }

    private BulletSpan(int gapWidth, int color, boolean wantColor, int bulletRadius)
    {
        mGapWidth = gapWidth;
        mBulletRadius = bulletRadius;
        mColor = color;
        mWantColor = wantColor;
    }

    public BulletSpan(Parcel source)
    {
        mGapWidth = source.readInt();
        mWantColor = source.readInt() != 0;
        mColor = source.readInt();
        mBulletRadius = source.readInt();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.BULLET_SPAN;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mGapWidth);
        dest.writeInt(mWantColor ? 1 : 0);
        dest.writeInt(mColor);
        dest.writeInt(mBulletRadius);
    }

    @Override
    public int getLeadingMargin(boolean first)
    {
        return 2 * mBulletRadius + mGapWidth;
    }

    public int getGapWidth()
    {
        return mGapWidth;
    }

    public int getBulletRadius()
    {
        return mBulletRadius;
    }

    public int getColor()
    {
        return mColor;
    }

    @Override
    public void drawLeadingMargin(Canvas canvas, Paint paint, int x, int direction, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout)
    {
        if (!(text instanceof Spanned) || ((Spanned)text).getSpanStart(this) != start) return;

        Paint.Style style = paint.getStyle();
        int oldColor = 0;

        if (mWantColor)
        {
            oldColor = paint.getColor();
            paint.setColor(mColor);
        }

        paint.setStyle(Paint.Style.FILL);

        float lineBottom = bottom;
        if (layout != null)
        {
            int line = layout.getLineForOffset(start);
            lineBottom = bottom - layout.getLineBottom(line) + layout.getLineBaseline(line) + layout.getLineDescent(line);
        }

        float centerY = (top + lineBottom) / 2.0f;
        canvas.drawCircle(x + direction * mBulletRadius, centerY, mBulletRadius, paint);

        if (mWantColor) paint.setColor(oldColor);
        paint.setStyle(style);
    }

    @Override
    public String toString()
    {
        return "BulletSpan{gapWidth=" + mGapWidth + ", bulletRadius=" + mBulletRadius + ", color=#" + String.format("%08X", mColor)
               + "}";
    }
}
