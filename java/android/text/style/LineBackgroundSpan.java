package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextUtils;

public interface LineBackgroundSpan extends ParagraphStyle
{
    void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence text,
                        int start, int end, int lineNumber);

    class Standard implements LineBackgroundSpan, ParcelableSpan
    {
        private final int mColor;

        public Standard(int color)
        {
            mColor = color;
        }

        public Standard(Parcel source)
        {
            mColor = source.readInt();
        }

        @Override
        public int getSpanTypeId()
        {
            return TextUtils.LINE_BACKGROUND_SPAN;
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
        }

        public final int getColor()
        {
            return mColor;
        }

        @Override
        public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom,
                                   CharSequence text, int start, int end, int lineNumber)
        {
            int original = paint.getColor();
            paint.setColor(mColor);
            canvas.drawRect(left, top, right, bottom, paint);
            paint.setColor(original);
        }
    }
}
