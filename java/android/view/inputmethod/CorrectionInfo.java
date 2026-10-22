package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class CorrectionInfo implements Parcelable
{
    public static final Parcelable.Creator<CorrectionInfo> CREATOR = new Parcelable.Creator<CorrectionInfo>()
    {
        @Override
        public CorrectionInfo createFromParcel(Parcel source)
        {
            int offset = source.readInt();
            CharSequence oldText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            CharSequence newText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            return new CorrectionInfo(offset, oldText, newText);
        }

        @Override
        public CorrectionInfo[] newArray(int size)
        {
            return new CorrectionInfo[size];
        }
    };

    private final int mOffset;
    private final CharSequence mOldText;
    private final CharSequence mNewText;

    public CorrectionInfo(int offset, CharSequence oldText, CharSequence newText)
    {
        mOffset = offset;
        mOldText = oldText;
        mNewText = newText;
    }

    public int getOffset()
    {
        return mOffset;
    }

    public CharSequence getOldText()
    {
        return mOldText;
    }

    public CharSequence getNewText()
    {
        return mNewText;
    }

    @Override
    public String toString()
    {
        return "CorrectionInfo{#" + mOffset + " \"" + mOldText + "\" -> \"" + mNewText + "\"}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mOffset);
        TextUtils.writeToParcel(mOldText, dest, flags);
        TextUtils.writeToParcel(mNewText, dest, flags);
    }
}
