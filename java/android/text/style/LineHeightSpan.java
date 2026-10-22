package android.text.style;

import android.graphics.Paint;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.TextUtils;

public interface LineHeightSpan extends ParagraphStyle, WrapTogetherSpan
{
    void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight, Paint.FontMetricsInt fontMetrics);

    interface WithDensity extends LineHeightSpan
    {
        void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight,
                          Paint.FontMetricsInt fontMetrics, android.text.TextPaint paint);
    }

    class Standard implements LineHeightSpan, ParcelableSpan
    {
        private final int mHeight;

        public Standard(int height)
        {
            if (height <= 0) throw new IllegalArgumentException("Height:" + height + "must be positive");

            mHeight = height;
        }

        public Standard(Parcel source)
        {
            mHeight = source.readInt();
        }

        public int getHeight()
        {
            return mHeight;
        }

        @Override
        public int getSpanTypeId()
        {
            return TextUtils.LINE_HEIGHT_SPAN;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(mHeight);
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight,
                                 Paint.FontMetricsInt fontMetrics)
        {
            int originalHeight = fontMetrics.descent - fontMetrics.ascent;
            if (originalHeight <= 0) return;

            float ratio = mHeight * 1.0f / originalHeight;
            fontMetrics.descent = Math.round(fontMetrics.descent * ratio);
            fontMetrics.ascent = fontMetrics.descent - mHeight;
        }
    }
}
