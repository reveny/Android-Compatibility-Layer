package android.text;

import android.os.Parcel;

public class Annotation implements ParcelableSpan
{
    private final String mKey;
    private final String mValue;

    public Annotation(String key, String value)
    {
        mKey = key;
        mValue = value;
    }

    public Annotation(Parcel source)
    {
        mKey = source.readString();
        mValue = source.readString();
    }

    @Override
    public int getSpanTypeId()
    {
        return TextUtils.ANNOTATION;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mKey);
        dest.writeString(mValue);
    }

    public String getKey()
    {
        return mKey;
    }

    public String getValue()
    {
        return mValue;
    }
}
