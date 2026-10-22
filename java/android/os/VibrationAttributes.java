package android.os;

public final class VibrationAttributes implements Parcelable
{
    public static final int FLAG_BYPASS_INTERRUPTION_POLICY = 1;

    public static final int USAGE_CLASS_UNKNOWN = 0;
    public static final int USAGE_CLASS_ALARM = 1;
    public static final int USAGE_CLASS_FEEDBACK = 2;
    public static final int USAGE_CLASS_MEDIA = 3;
    public static final int USAGE_CLASS_MASK = 15;

    public static final int USAGE_UNKNOWN = 0;
    public static final int USAGE_ALARM = 17;
    public static final int USAGE_TOUCH = 18;
    public static final int USAGE_MEDIA = 19;
    public static final int USAGE_RINGTONE = 33;
    public static final int USAGE_PHYSICAL_EMULATION = 34;
    public static final int USAGE_NOTIFICATION = 49;
    public static final int USAGE_HARDWARE_FEEDBACK = 50;
    public static final int USAGE_COMMUNICATION_REQUEST = 65;
    public static final int USAGE_ACCESSIBILITY = 66;

    public static final Parcelable.Creator<VibrationAttributes> CREATOR = new Parcelable.Creator<VibrationAttributes>()
    {
        @Override
        public VibrationAttributes createFromParcel(Parcel source)
        {
            int usage = source.readInt();
            return new VibrationAttributes(usage, source.readInt());
        }

        @Override
        public VibrationAttributes[] newArray(int size)
        {
            return new VibrationAttributes[size];
        }
    };

    private final int mUsage;
    private final int mFlags;

    private VibrationAttributes(int usage, int flags)
    {
        mUsage = usage;
        mFlags = flags;
    }

    public static VibrationAttributes createForUsage(int usage)
    {
        return new Builder().setUsage(usage).build();
    }

    public int getUsageClass()
    {
        return mUsage & USAGE_CLASS_MASK;
    }

    public int getUsage()
    {
        return mUsage;
    }

    public int getFlags()
    {
        return mFlags;
    }

    public boolean isFlagSet(int flag)
    {
        return (mFlags & flag) > 0;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mUsage);
        dest.writeInt(mFlags);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof VibrationAttributes)) return false;

        VibrationAttributes attributes = (VibrationAttributes)other;
        return mUsage == attributes.mUsage && mFlags == attributes.mFlags;
    }

    @Override
    public int hashCode()
    {
        return mUsage * 31 + mFlags;
    }

    @Override
    public String toString()
    {
        return "VibrationAttributes{mUsage=" + mUsage + ", mFlags=" + mFlags + "}";
    }

    public static final class Builder
    {
        private int mUsage = USAGE_UNKNOWN;
        private int mFlags;

        public Builder()
        {
        }

        public Builder(VibrationAttributes attributes)
        {
            if (attributes == null) return;

            mUsage = attributes.mUsage;
            mFlags = attributes.mFlags;
        }

        public VibrationAttributes build()
        {
            return new VibrationAttributes(mUsage, mFlags);
        }

        public Builder setUsage(int usage)
        {
            mUsage = usage;
            return this;
        }

        public Builder setFlags(int flags, int mask)
        {
            mFlags = (mFlags & ~mask) | (flags & mask);
            return this;
        }
    }
}
