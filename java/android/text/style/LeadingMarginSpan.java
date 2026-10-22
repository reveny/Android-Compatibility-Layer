package android.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.TextUtils;

public interface LeadingMarginSpan extends ParagraphStyle
{
    int getLeadingMargin(boolean first);

    void drawLeadingMargin(Canvas canvas, Paint paint, int x, int direction, int top, int baseline, int bottom,
                           CharSequence text, int start, int end, boolean first, Layout layout);

    interface LeadingMarginSpan2 extends LeadingMarginSpan, WrapTogetherSpan
    {
        int getLeadingMarginLineCount();
    }

    class Standard implements LeadingMarginSpan, ParcelableSpan
    {
        private final int mFirst;
        private final int mRest;

        public Standard(int first, int rest)
        {
            mFirst = first;
            mRest = rest;
        }

        public Standard(int every)
        {
            this(every, every);
        }

        public Standard(Parcel source)
        {
            mFirst = source.readInt();
            mRest = source.readInt();
        }

        @Override
        public int getSpanTypeId()
        {
            return TextUtils.LEADING_MARGIN_SPAN;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(mFirst);
            dest.writeInt(mRest);
        }

        @Override
        public int getLeadingMargin(boolean first)
        {
            return first ? mFirst : mRest;
        }

        @Override
        public void drawLeadingMargin(Canvas canvas, Paint paint, int x, int direction, int top, int baseline, int bottom,
                                      CharSequence text, int start, int end, boolean first, Layout layout)
        {
        }
    }
}
