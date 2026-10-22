package android.print;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

public final class PrintJobInfo implements Parcelable
{
    public static final int STATE_CREATED = 1;
    public static final int STATE_QUEUED = 2;
    public static final int STATE_STARTED = 3;
    public static final int STATE_BLOCKED = 4;
    public static final int STATE_COMPLETED = 5;
    public static final int STATE_FAILED = 6;
    public static final int STATE_CANCELED = 7;

    public static final Parcelable.Creator<PrintJobInfo> CREATOR = new Parcelable.Creator<PrintJobInfo>()
    {
        @Override
        public PrintJobInfo createFromParcel(Parcel source)
        {
            PrintJobInfo info = new PrintJobInfo();
            info.mId = source.readInt() != 0 ? PrintJobId.CREATOR.createFromParcel(source) : null;
            info.mLabel = source.readString();
            info.mPrinterId = source.readInt() != 0 ? PrinterId.CREATOR.createFromParcel(source) : null;
            info.mState = source.readInt();
            info.mCreationTime = source.readLong();
            info.mCopies = source.readInt();
            int pageCount = source.readInt();
            if (pageCount >= 0)
            {
                info.mPageRanges = new PageRange[pageCount];
                for (int index = 0; index < pageCount; index++) info.mPageRanges[index] = PageRange.CREATOR.createFromParcel(source);
            }
            info.mAttributes = source.readInt() != 0 ? PrintAttributes.CREATOR.createFromParcel(source) : null;
            info.mAdvancedOptions = source.readBundle();
            return info;
        }

        @Override
        public PrintJobInfo[] newArray(int size)
        {
            return new PrintJobInfo[size];
        }
    };

    private PrintJobId mId;
    private String mLabel;
    private PrinterId mPrinterId;
    private int mState;
    private long mCreationTime;
    private int mCopies;
    private PageRange[] mPageRanges;
    private PrintAttributes mAttributes;
    private Bundle mAdvancedOptions;

    PrintJobInfo()
    {
    }

    PrintJobInfo(PrintJobInfo other)
    {
        mId = other.mId;
        mLabel = other.mLabel;
        mPrinterId = other.mPrinterId;
        mState = other.mState;
        mCreationTime = other.mCreationTime;
        mCopies = other.mCopies;
        mPageRanges = other.mPageRanges;
        mAttributes = other.mAttributes;
        mAdvancedOptions = other.mAdvancedOptions;
    }

    public PrintJobId getId()
    {
        return mId;
    }

    void setId(PrintJobId id)
    {
        mId = id;
    }

    public String getLabel()
    {
        return mLabel;
    }

    void setLabel(String label)
    {
        mLabel = label;
    }

    public PrinterId getPrinterId()
    {
        return mPrinterId;
    }

    public int getState()
    {
        return mState;
    }

    void setState(int state)
    {
        mState = state;
    }

    public long getCreationTime()
    {
        return mCreationTime;
    }

    void setCreationTime(long creationTime)
    {
        mCreationTime = creationTime;
    }

    public int getCopies()
    {
        return mCopies;
    }

    void setCopies(int copies)
    {
        mCopies = copies;
    }

    public PageRange[] getPages()
    {
        return mPageRanges;
    }

    void setPages(PageRange[] pageRanges)
    {
        mPageRanges = pageRanges;
    }

    public PrintAttributes getAttributes()
    {
        return mAttributes;
    }

    void setAttributes(PrintAttributes attributes)
    {
        mAttributes = attributes;
    }

    public boolean hasAdvancedOption(String key)
    {
        return mAdvancedOptions != null && mAdvancedOptions.containsKey(key);
    }

    public String getAdvancedStringOption(String key)
    {
        return mAdvancedOptions == null ? null : mAdvancedOptions.getString(key);
    }

    public int getAdvancedIntOption(String key)
    {
        return mAdvancedOptions == null ? 0 : mAdvancedOptions.getInt(key);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mId != null ? 1 : 0);
        if (mId != null) mId.writeToParcel(dest, flags);
        dest.writeString(mLabel);
        dest.writeInt(mPrinterId != null ? 1 : 0);
        if (mPrinterId != null) mPrinterId.writeToParcel(dest, flags);
        dest.writeInt(mState);
        dest.writeLong(mCreationTime);
        dest.writeInt(mCopies);
        dest.writeInt(mPageRanges == null ? -1 : mPageRanges.length);
        if (mPageRanges != null) for (PageRange range : mPageRanges) range.writeToParcel(dest, flags);
        dest.writeInt(mAttributes != null ? 1 : 0);
        if (mAttributes != null) mAttributes.writeToParcel(dest, flags);
        dest.writeBundle(mAdvancedOptions);
    }

    @Override
    public String toString()
    {
        return "PrintJobInfo{label: " + mLabel + ", id: " + mId + ", state: " + stateToString(mState) + ", printer: " + mPrinterId
            + ", copies: " + mCopies + ", attributes: " + mAttributes + ", pages: "
            + (mPageRanges != null ? Arrays.toString(mPageRanges) : null) + ", creationTime: " + mCreationTime + "}";
    }

    static String stateToString(int state)
    {
        switch (state)
        {
            case STATE_CREATED: return "PRINT_JOB_STATE_CREATED";
            case STATE_QUEUED: return "PRINT_JOB_STATE_QUEUED";
            case STATE_STARTED: return "PRINT_JOB_STATE_STARTED";
            case STATE_BLOCKED: return "PRINT_JOB_STATE_BLOCKED";
            case STATE_FAILED: return "PRINT_JOB_STATE_FAILED";
            case STATE_COMPLETED: return "PRINT_JOB_STATE_COMPLETED";
            case STATE_CANCELED: return "PRINT_JOB_STATE_CANCELED";
            default: return "PRINT_JOB_STATE_UNKNOWN";
        }
    }
}
