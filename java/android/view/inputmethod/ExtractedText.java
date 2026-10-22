package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class ExtractedText implements Parcelable
{
    public static final int FLAG_SINGLE_LINE = 0x0001;
    public static final int FLAG_SELECTING = 0x0002;

    public static final Parcelable.Creator<ExtractedText> CREATOR = new Parcelable.Creator<ExtractedText>()
    {
        @Override
        public ExtractedText createFromParcel(Parcel source)
        {
            ExtractedText result = new ExtractedText();
            result.text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            result.startOffset = source.readInt();
            result.partialStartOffset = source.readInt();
            result.partialEndOffset = source.readInt();
            result.selectionStart = source.readInt();
            result.selectionEnd = source.readInt();
            result.flags = source.readInt();
            result.hint = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            return result;
        }

        @Override
        public ExtractedText[] newArray(int size)
        {
            return new ExtractedText[size];
        }
    };

    public CharSequence text;
    public int startOffset;
    public int partialStartOffset;
    public int partialEndOffset;
    public int selectionStart;
    public int selectionEnd;
    public int flags;
    public CharSequence hint;

    public ExtractedText()
    {
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int parcelableFlags)
    {
        TextUtils.writeToParcel(text, dest, parcelableFlags);
        dest.writeInt(startOffset);
        dest.writeInt(partialStartOffset);
        dest.writeInt(partialEndOffset);
        dest.writeInt(selectionStart);
        dest.writeInt(selectionEnd);
        dest.writeInt(flags);
        TextUtils.writeToParcel(hint, dest, parcelableFlags);
    }
}
