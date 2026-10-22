package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class SurroundingText implements Parcelable
{
    private final CharSequence mText;
    private final int mSelectionStart;
    private final int mSelectionEnd;
    private final int mOffset;

    public SurroundingText(CharSequence text, int selectionStart, int selectionEnd, int offset)
    {
        mText = text == null ? "" : copy(text);
        mSelectionStart = selectionStart;
        mSelectionEnd = selectionEnd;
        mOffset = offset;
    }

    private static CharSequence copy(CharSequence text)
    {
        return text instanceof android.text.Spanned ? new android.text.SpannableString(text) : text.toString();
    }

    public CharSequence getText()
    {
        return mText;
    }

    public int getSelectionStart()
    {
        return mSelectionStart;
    }

    public int getSelectionEnd()
    {
        return mSelectionEnd;
    }

    public int getOffset()
    {
        return mOffset;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        TextUtils.writeToParcel(mText, out, flags);
        out.writeInt(mSelectionStart);
        out.writeInt(mSelectionEnd);
        out.writeInt(mOffset);
    }

    public static final Parcelable.Creator<SurroundingText> CREATOR = new Parcelable.Creator<SurroundingText>()
    {
        @Override
        public SurroundingText createFromParcel(Parcel source)
        {
            CharSequence text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            int selectionStart = source.readInt();
            int selectionEnd = source.readInt();
            int offset = source.readInt();
            return new SurroundingText(text, selectionStart, selectionEnd, offset);
        }

        @Override
        public SurroundingText[] newArray(int size)
        {
            return new SurroundingText[size];
        }
    };
}
