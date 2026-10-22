package android.view.inputmethod;

import android.os.Parcel;
import android.os.Parcelable;

public class ExtractedTextRequest implements Parcelable
{
    public static final Parcelable.Creator<ExtractedTextRequest> CREATOR = new Parcelable.Creator<ExtractedTextRequest>()
    {
        @Override
        public ExtractedTextRequest createFromParcel(Parcel source)
        {
            ExtractedTextRequest result = new ExtractedTextRequest();
            result.token = source.readInt();
            result.flags = source.readInt();
            result.hintMaxLines = source.readInt();
            result.hintMaxChars = source.readInt();
            return result;
        }

        @Override
        public ExtractedTextRequest[] newArray(int size)
        {
            return new ExtractedTextRequest[size];
        }
    };

    public int token;
    public int flags;
    public int hintMaxLines;
    public int hintMaxChars;

    public ExtractedTextRequest()
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
        dest.writeInt(token);
        dest.writeInt(flags);
        dest.writeInt(hintMaxLines);
        dest.writeInt(hintMaxChars);
    }
}
