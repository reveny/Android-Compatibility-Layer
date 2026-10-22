package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class AudioMixerAttributes implements Parcelable
{
    public static final int MIXER_BEHAVIOR_BIT_PERFECT = 1;
    public static final int MIXER_BEHAVIOR_DEFAULT = 0;

    public static final Parcelable.Creator<AudioMixerAttributes> CREATOR = new Parcelable.Creator<AudioMixerAttributes>()
    {
        @Override
        public AudioMixerAttributes createFromParcel(Parcel source)
        {
            AudioFormat format = AudioFormat.CREATOR.createFromParcel(source);
            int mixerBehavior = source.readInt();
            return new AudioMixerAttributes(format, mixerBehavior);
        }

        @Override
        public AudioMixerAttributes[] newArray(int size)
        {
            return new AudioMixerAttributes[size];
        }
    };

    private final AudioFormat mFormat;
    private final int mMixerBehavior;

    private AudioMixerAttributes(AudioFormat format, int mixerBehavior)
    {
        mFormat = format;
        mMixerBehavior = mixerBehavior;
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
        if (!(object instanceof AudioMixerAttributes)) return false;

        AudioMixerAttributes other = (AudioMixerAttributes)object;
        return mMixerBehavior == other.mMixerBehavior && mFormat.equals(other.mFormat);
    }

    public AudioFormat getFormat()
    {
        return mFormat;
    }

    public int getMixerBehavior()
    {
        return mMixerBehavior;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mFormat, mMixerBehavior);
    }

    @Override
    public String toString()
    {
        return "AudioMixerAttributes{format:" + mFormat + ", mixer behavior:" + mMixerBehavior + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        mFormat.writeToParcel(parcel, flags);
        parcel.writeInt(mMixerBehavior);
    }

    public static final class Builder
    {
        private final AudioFormat mFormat;
        private int mMixerBehavior = MIXER_BEHAVIOR_DEFAULT;

        public Builder(AudioFormat format)
        {
            mFormat = Objects.requireNonNull(format);
        }

        public AudioMixerAttributes build()
        {
            return new AudioMixerAttributes(mFormat, mMixerBehavior);
        }

        public Builder setMixerBehavior(int mixerBehavior)
        {
            if (mixerBehavior != MIXER_BEHAVIOR_DEFAULT && mixerBehavior != MIXER_BEHAVIOR_BIT_PERFECT)
            {
                throw new IllegalArgumentException("Invalid mixer behavior " + mixerBehavior);
            }

            mMixerBehavior = mixerBehavior;
            return this;
        }
    }
}
