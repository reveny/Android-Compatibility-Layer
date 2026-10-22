package android.print;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class PrintDocumentInfo implements Parcelable
{
    public static final int PAGE_COUNT_UNKNOWN = -1;

    public static final int CONTENT_TYPE_UNKNOWN = -1;
    public static final int CONTENT_TYPE_DOCUMENT = 0;
    public static final int CONTENT_TYPE_PHOTO = 1;

    public static final Parcelable.Creator<PrintDocumentInfo> CREATOR = new Parcelable.Creator<PrintDocumentInfo>()
    {
        @Override
        public PrintDocumentInfo createFromParcel(Parcel source)
        {
            PrintDocumentInfo info = new PrintDocumentInfo();
            info.mName = source.readString();
            info.mPageCount = source.readInt();
            info.mContentType = source.readInt();
            info.mDataSize = source.readLong();
            return info;
        }

        @Override
        public PrintDocumentInfo[] newArray(int size)
        {
            return new PrintDocumentInfo[size];
        }
    };

    private String mName;
    private int mPageCount = PAGE_COUNT_UNKNOWN;
    private int mContentType = CONTENT_TYPE_DOCUMENT;
    private long mDataSize;

    private PrintDocumentInfo()
    {
    }

    private PrintDocumentInfo(PrintDocumentInfo prototype)
    {
        mName = prototype.mName;
        mPageCount = prototype.mPageCount;
        mContentType = prototype.mContentType;
        mDataSize = prototype.mDataSize;
    }

    public String getName()
    {
        return mName;
    }

    public int getPageCount()
    {
        return mPageCount;
    }

    public int getContentType()
    {
        return mContentType;
    }

    public long getDataSize()
    {
        return mDataSize;
    }

    void setDataSize(long dataSize)
    {
        mDataSize = dataSize;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mName);
        dest.writeInt(mPageCount);
        dest.writeInt(mContentType);
        dest.writeLong(mDataSize);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mName, mContentType, mPageCount, mDataSize);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PrintDocumentInfo)) return false;

        PrintDocumentInfo info = (PrintDocumentInfo)other;
        return Objects.equals(mName, info.mName) && mContentType == info.mContentType && mPageCount == info.mPageCount
            && mDataSize == info.mDataSize;
    }

    @Override
    public String toString()
    {
        return "PrintDocumentInfo{name=" + mName + ", pageCount=" + mPageCount + ", contentType=" + contentTypeToString(mContentType)
            + ", dataSize=" + mDataSize + "}";
    }

    private static String contentTypeToString(int contentType)
    {
        switch (contentType)
        {
            case CONTENT_TYPE_DOCUMENT: return "CONTENT_TYPE_DOCUMENT";
            case CONTENT_TYPE_PHOTO: return "CONTENT_TYPE_PHOTO";
            default: return "CONTENT_TYPE_UNKNOWN";
        }
    }

    public static final class Builder
    {
        private final PrintDocumentInfo mPrototype;

        public Builder(String name)
        {
            if (name == null || name.isEmpty()) throw new IllegalArgumentException("name cannot be empty");

            mPrototype = new PrintDocumentInfo();
            mPrototype.mName = name;
        }

        public Builder setPageCount(int pageCount)
        {
            if (pageCount < 0 && pageCount != PAGE_COUNT_UNKNOWN)
            {
                throw new IllegalArgumentException("pageCount must be greater than or equal to zero or DocumentInfo#PAGE_COUNT_UNKNOWN");
            }
            mPrototype.mPageCount = pageCount;
            return this;
        }

        public Builder setContentType(int type)
        {
            mPrototype.mContentType = type;
            return this;
        }

        public PrintDocumentInfo build()
        {
            if (mPrototype.mPageCount == 0) mPrototype.mPageCount = PAGE_COUNT_UNKNOWN;
            return new PrintDocumentInfo(mPrototype);
        }
    }
}
