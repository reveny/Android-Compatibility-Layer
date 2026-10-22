package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

public class AudioDescriptor implements Parcelable
{
    public static final int STANDARD_EDID = 1;
    public static final int STANDARD_NONE = 0;
    public static final int STANDARD_SADB = 2;
    public static final int STANDARD_VSADB = 3;

    public static final Parcelable.Creator<AudioDescriptor> CREATOR = new Parcelable.Creator<AudioDescriptor>()
    {
        @Override
        public AudioDescriptor createFromParcel(Parcel source)
        {
            int standard = source.readInt();
            int encapsulationType = source.readInt();
            byte[] descriptor = source.createByteArray();
            return new AudioDescriptor(standard, encapsulationType, descriptor);
        }

        @Override
        public AudioDescriptor[] newArray(int size)
        {
            return new AudioDescriptor[size];
        }
    };

    private final int mStandard;
    private final int mEncapsulationType;
    private final byte[] mDescriptor;

    AudioDescriptor(int standard, int encapsulationType, byte[] descriptor)
    {
        mStandard = standard;
        mEncapsulationType = encapsulationType;
        mDescriptor = descriptor == null ? new byte[0] : descriptor;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) return true;
        if (!(object instanceof AudioDescriptor)) return false;

        AudioDescriptor other = (AudioDescriptor)object;
        return mStandard == other.mStandard && mEncapsulationType == other.mEncapsulationType
            && Arrays.equals(mDescriptor, other.mDescriptor);
    }

    public byte[] getDescriptor()
    {
        return mDescriptor;
    }

    public int getEncapsulationType()
    {
        return mEncapsulationType;
    }

    public int getStandard()
    {
        return mStandard;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mStandard, mEncapsulationType, Arrays.hashCode(mDescriptor));
    }

    @Override
    public String toString()
    {
        return "{standard=" + mStandard + ", encapsulation type=" + mEncapsulationType + ", descriptor length="
            + mDescriptor.length + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mStandard);
        parcel.writeInt(mEncapsulationType);
        parcel.writeByteArray(mDescriptor);
    }
}
