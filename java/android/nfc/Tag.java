package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

public final class Tag implements Parcelable
{
    public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>()
    {
        @Override
        public Tag createFromParcel(Parcel source)
        {
            byte[] id = source.createByteArray();
            int count = source.readInt();
            String[] techList = new String[Math.max(0, count)];
            for (int index = 0; index < techList.length; index++) techList[index] = source.readString();
            return new Tag(id, techList);
        }

        @Override
        public Tag[] newArray(int size)
        {
            return new Tag[size];
        }
    };

    private final byte[] mId;
    private final String[] mTechList;

    Tag(byte[] id, String[] techList)
    {
        mId = id == null ? new byte[0] : id;
        mTechList = techList == null ? new String[0] : techList;
    }

    public byte[] getId()
    {
        return mId;
    }

    public String[] getTechList()
    {
        return mTechList;
    }

    @Override
    public String toString()
    {
        return "TAG: Tech " + Arrays.toString(mTechList);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeByteArray(mId);
        dest.writeInt(mTechList.length);
        for (String tech : mTechList) dest.writeString(tech);
    }
}
