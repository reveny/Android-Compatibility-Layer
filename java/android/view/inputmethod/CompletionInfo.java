package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class CompletionInfo implements Parcelable
{
    public static final Parcelable.Creator<CompletionInfo> CREATOR = new Parcelable.Creator<CompletionInfo>()
    {
        @Override
        public CompletionInfo createFromParcel(Parcel source)
        {
            long id = source.readLong();
            int position = source.readInt();
            CharSequence text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            CharSequence label = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            return new CompletionInfo(id, position, text, label);
        }

        @Override
        public CompletionInfo[] newArray(int size)
        {
            return new CompletionInfo[size];
        }
    };

    private final long mId;
    private final int mPosition;
    private final CharSequence mText;
    private final CharSequence mLabel;

    public CompletionInfo(long id, int index, CharSequence text)
    {
        this(id, index, text, null);
    }

    public CompletionInfo(long id, int index, CharSequence text, CharSequence label)
    {
        mId = id;
        mPosition = index;
        mText = text;
        mLabel = label;
    }

    public long getId()
    {
        return mId;
    }

    public int getPosition()
    {
        return mPosition;
    }

    public CharSequence getText()
    {
        return mText;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    @Override
    public String toString()
    {
        return "CompletionInfo{#" + mPosition + " \"" + mText + "\" id=" + mId + " label=" + mLabel + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(mId);
        dest.writeInt(mPosition);
        TextUtils.writeToParcel(mText, dest, flags);
        TextUtils.writeToParcel(mLabel, dest, flags);
    }
}
