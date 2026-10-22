package android.print;

import android.os.Parcel;
import android.os.Parcelable;

public final class PageRange implements Parcelable
{
    public static final PageRange ALL_PAGES = new PageRange(0, Integer.MAX_VALUE);

    public static final Parcelable.Creator<PageRange> CREATOR = new Parcelable.Creator<PageRange>()
    {
        @Override
        public PageRange createFromParcel(Parcel source)
        {
            int start = source.readInt();
            int end = source.readInt();
            return new PageRange(start, end);
        }

        @Override
        public PageRange[] newArray(int size)
        {
            return new PageRange[size];
        }
    };

    private final int mStart;
    private final int mEnd;

    public PageRange(int start, int end)
    {
        if (start < 0) throw new IllegalArgumentException("start cannot be less than zero.");
        if (end < 0) throw new IllegalArgumentException("end cannot be less than zero.");
        if (start > end) throw new IllegalArgumentException("start must be lesser than end.");

        mStart = start;
        mEnd = end;
    }

    public int getStart()
    {
        return mStart;
    }

    public int getEnd()
    {
        return mEnd;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mStart);
        dest.writeInt(mEnd);
    }

    @Override
    public int hashCode()
    {
        return 31 * (31 + mEnd) + mStart;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PageRange)) return false;

        PageRange range = (PageRange)other;
        return mStart == range.mStart && mEnd == range.mEnd;
    }

    @Override
    public String toString()
    {
        if (mStart == 0 && mEnd == Integer.MAX_VALUE) return "PageRange[<all pages>]";
        return "PageRange[" + mStart + " - " + mEnd + "]";
    }
}
