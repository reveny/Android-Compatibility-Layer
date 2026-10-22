package android.text.style;

import android.os.Parcel;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.TextUtils;

public interface AlignmentSpan extends ParagraphStyle
{
    Layout.Alignment getAlignment();

    class Standard implements AlignmentSpan, ParcelableSpan
    {
        private final Layout.Alignment mAlignment;

        public Standard(Layout.Alignment alignment)
        {
            mAlignment = alignment;
        }

        public Standard(Parcel source)
        {
            mAlignment = Layout.Alignment.valueOf(source.readString());
        }

        @Override
        public int getSpanTypeId()
        {
            return TextUtils.ALIGNMENT_SPAN;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(mAlignment.name());
        }

        @Override
        public Layout.Alignment getAlignment()
        {
            return mAlignment;
        }

        @Override
        public String toString()
        {
            return "AlignmentSpan.Standard{alignment=" + mAlignment + "}";
        }
    }
}
