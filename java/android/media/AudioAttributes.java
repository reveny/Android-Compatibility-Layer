package android.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class AudioAttributes implements Parcelable
{
    public static final int ALLOW_CAPTURE_BY_ALL = 1;
    public static final int ALLOW_CAPTURE_BY_NONE = 3;
    public static final int ALLOW_CAPTURE_BY_SYSTEM = 2;
    public static final int CONTENT_TYPE_MOVIE = 3;
    public static final int CONTENT_TYPE_MUSIC = 2;
    public static final int CONTENT_TYPE_SONIFICATION = 4;
    public static final int CONTENT_TYPE_SPEECH = 1;
    public static final int CONTENT_TYPE_UNKNOWN = 0;
    public static final int FLAG_AUDIBILITY_ENFORCED = 1;
    public static final int FLAG_HW_AV_SYNC = 16;
    public static final int FLAG_LOW_LATENCY = 256;
    public static final int SPATIALIZATION_BEHAVIOR_AUTO = 0;
    public static final int SPATIALIZATION_BEHAVIOR_NEVER = 1;
    public static final int USAGE_ALARM = 4;
    public static final int USAGE_ASSISTANCE_ACCESSIBILITY = 11;
    public static final int USAGE_ASSISTANCE_NAVIGATION_GUIDANCE = 12;
    public static final int USAGE_ASSISTANCE_SONIFICATION = 13;
    public static final int USAGE_ASSISTANT = 16;
    public static final int USAGE_GAME = 14;
    public static final int USAGE_MEDIA = 1;
    public static final int USAGE_NOTIFICATION = 5;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_DELAYED = 9;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_INSTANT = 8;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_REQUEST = 7;
    public static final int USAGE_NOTIFICATION_EVENT = 10;
    public static final int USAGE_NOTIFICATION_RINGTONE = 6;
    public static final int USAGE_UNKNOWN = 0;
    public static final int USAGE_VOICE_COMMUNICATION = 2;
    public static final int USAGE_VOICE_COMMUNICATION_SIGNALLING = 3;

    private static final int FLAG_SCO = 4;
    private static final int FLAG_BEACON = 8;
    private static final int FLAG_NO_MEDIA_PROJECTION = 1024;
    private static final int FLAG_NO_SYSTEM_CAPTURE = 4096;
    private static final int FLAG_MUTE_HAPTIC = 2048;
    private static final int FLAG_CONTENT_SPATIALIZED = 16384;
    private static final int FLAG_NEVER_SPATIALIZE = 32768;
    private static final int PUBLIC_FLAGS = FLAG_AUDIBILITY_ENFORCED | FLAG_HW_AV_SYNC | FLAG_LOW_LATENCY;
    private static final int STREAM_UNSET = -1;

    public static final Parcelable.Creator<AudioAttributes> CREATOR = new Parcelable.Creator<AudioAttributes>()
    {
        @Override
        public AudioAttributes createFromParcel(Parcel source)
        {
            AudioAttributes attributes = new AudioAttributes();
            attributes.mUsage = source.readInt();
            attributes.mContentType = source.readInt();
            attributes.mFlags = source.readInt();
            attributes.mLegacyStreamType = source.readInt();
            return attributes;
        }

        @Override
        public AudioAttributes[] newArray(int size)
        {
            return new AudioAttributes[size];
        }
    };

    private int mUsage = USAGE_UNKNOWN;
    private int mContentType = CONTENT_TYPE_UNKNOWN;
    private int mFlags;
    private int mLegacyStreamType = STREAM_UNSET;

    private AudioAttributes()
    {
    }

    public boolean areHapticChannelsMuted()
    {
        return (mFlags & FLAG_MUTE_HAPTIC) != 0;
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
        if (!(object instanceof AudioAttributes)) return false;

        AudioAttributes other = (AudioAttributes)object;
        return mUsage == other.mUsage && mContentType == other.mContentType && mFlags == other.mFlags
            && mLegacyStreamType == other.mLegacyStreamType;
    }

    public int getAllowedCapturePolicy()
    {
        if ((mFlags & FLAG_NO_SYSTEM_CAPTURE) != 0) return ALLOW_CAPTURE_BY_NONE;
        if ((mFlags & FLAG_NO_MEDIA_PROJECTION) != 0) return ALLOW_CAPTURE_BY_SYSTEM;

        return ALLOW_CAPTURE_BY_ALL;
    }

    public int getContentType()
    {
        return mContentType;
    }

    public int getFlags()
    {
        return mFlags & PUBLIC_FLAGS;
    }

    public int getSpatializationBehavior()
    {
        return (mFlags & FLAG_NEVER_SPATIALIZE) != 0 ? SPATIALIZATION_BEHAVIOR_NEVER : SPATIALIZATION_BEHAVIOR_AUTO;
    }

    public int getUsage()
    {
        return mUsage;
    }

    public int getVolumeControlStream()
    {
        if (mLegacyStreamType != STREAM_UNSET) return mLegacyStreamType;

        return toLegacyStreamType(mUsage, mFlags);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mUsage, mContentType, mFlags, mLegacyStreamType);
    }

    public boolean isContentSpatialized()
    {
        return (mFlags & FLAG_CONTENT_SPATIALIZED) != 0;
    }

    @Override
    public String toString()
    {
        return "AudioAttributes: usage=" + usageToString(mUsage) + " content=" + mContentType + " flags=0x"
            + Integer.toHexString(mFlags).toUpperCase();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mUsage);
        parcel.writeInt(mContentType);
        parcel.writeInt(mFlags);
        parcel.writeInt(mLegacyStreamType);
    }

    public static String usageToString(int usage)
    {
        switch (usage)
        {
            case USAGE_UNKNOWN:
                return "USAGE_UNKNOWN";
            case USAGE_MEDIA:
                return "USAGE_MEDIA";
            case USAGE_VOICE_COMMUNICATION:
                return "USAGE_VOICE_COMMUNICATION";
            case USAGE_VOICE_COMMUNICATION_SIGNALLING:
                return "USAGE_VOICE_COMMUNICATION_SIGNALLING";
            case USAGE_ALARM:
                return "USAGE_ALARM";
            case USAGE_NOTIFICATION:
                return "USAGE_NOTIFICATION";
            case USAGE_NOTIFICATION_RINGTONE:
                return "USAGE_NOTIFICATION_RINGTONE";
            case USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
                return "USAGE_NOTIFICATION_COMMUNICATION_REQUEST";
            case USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
                return "USAGE_NOTIFICATION_COMMUNICATION_INSTANT";
            case USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
                return "USAGE_NOTIFICATION_COMMUNICATION_DELAYED";
            case USAGE_NOTIFICATION_EVENT:
                return "USAGE_NOTIFICATION_EVENT";
            case USAGE_ASSISTANCE_ACCESSIBILITY:
                return "USAGE_ASSISTANCE_ACCESSIBILITY";
            case USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
                return "USAGE_ASSISTANCE_NAVIGATION_GUIDANCE";
            case USAGE_ASSISTANCE_SONIFICATION:
                return "USAGE_ASSISTANCE_SONIFICATION";
            case USAGE_GAME:
                return "USAGE_GAME";
            case USAGE_ASSISTANT:
                return "USAGE_ASSISTANT";
            default:
                return "unknown usage " + usage;
        }
    }

    static boolean isValidUsage(int usage)
    {
        return (usage >= USAGE_UNKNOWN && usage <= USAGE_GAME) || usage == USAGE_ASSISTANT;
    }

    static int toLegacyStreamType(int usage, int flags)
    {
        if ((flags & FLAG_AUDIBILITY_ENFORCED) != 0) return 7;
        if ((flags & FLAG_SCO) != 0) return 6;
        if ((flags & FLAG_BEACON) != 0) return 9;

        switch (usage)
        {
            case USAGE_VOICE_COMMUNICATION:
                return AudioManager.STREAM_VOICE_CALL;
            case USAGE_VOICE_COMMUNICATION_SIGNALLING:
                return AudioManager.STREAM_DTMF;
            case USAGE_ALARM:
                return AudioManager.STREAM_ALARM;
            case USAGE_NOTIFICATION_RINGTONE:
                return AudioManager.STREAM_RING;
            case USAGE_NOTIFICATION:
            case USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
            case USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
            case USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
            case USAGE_NOTIFICATION_EVENT:
                return AudioManager.STREAM_NOTIFICATION;
            case USAGE_ASSISTANCE_ACCESSIBILITY:
                return AudioManager.STREAM_ACCESSIBILITY;
            case USAGE_ASSISTANCE_SONIFICATION:
                return AudioManager.STREAM_SYSTEM;
            default:
                return AudioManager.STREAM_MUSIC;
        }
    }

    public static class Builder
    {
        private int mUsage = USAGE_UNKNOWN;
        private int mContentType = CONTENT_TYPE_UNKNOWN;
        private int mFlags;
        private int mLegacyStreamType = STREAM_UNSET;

        public Builder()
        {
        }

        public Builder(AudioAttributes attributes)
        {
            mUsage = attributes.mUsage;
            mContentType = attributes.mContentType;
            mFlags = attributes.mFlags;
            mLegacyStreamType = attributes.mLegacyStreamType;
        }

        public AudioAttributes build()
        {
            AudioAttributes attributes = new AudioAttributes();
            attributes.mUsage = mUsage;
            attributes.mContentType = mContentType;
            attributes.mFlags = mFlags;
            attributes.mLegacyStreamType = mLegacyStreamType;
            return attributes;
        }

        public Builder setAllowedCapturePolicy(int capturePolicy)
        {
            mFlags &= ~(FLAG_NO_MEDIA_PROJECTION | FLAG_NO_SYSTEM_CAPTURE);

            switch (capturePolicy)
            {
                case ALLOW_CAPTURE_BY_NONE:
                    mFlags |= FLAG_NO_MEDIA_PROJECTION | FLAG_NO_SYSTEM_CAPTURE;
                    break;
                case ALLOW_CAPTURE_BY_SYSTEM:
                    mFlags |= FLAG_NO_MEDIA_PROJECTION;
                    break;
                case ALLOW_CAPTURE_BY_ALL:
                    break;
                default:
                    throw new IllegalArgumentException("Unknown allow playback capture policy");
            }

            return this;
        }

        public Builder setContentType(int contentType)
        {
            if (contentType >= CONTENT_TYPE_UNKNOWN && contentType <= CONTENT_TYPE_SONIFICATION)
            {
                mContentType = contentType;
            }
            else
            {
                mContentType = CONTENT_TYPE_UNKNOWN;
            }

            return this;
        }

        public Builder setFlags(int flags)
        {
            mFlags |= flags & PUBLIC_FLAGS;
            return this;
        }

        public Builder setHapticChannelsMuted(boolean muted)
        {
            if (muted)
            {
                mFlags |= FLAG_MUTE_HAPTIC;
            }
            else
            {
                mFlags &= ~FLAG_MUTE_HAPTIC;
            }

            return this;
        }

        public Builder setIsContentSpatialized(boolean isSpatialized)
        {
            if (isSpatialized)
            {
                mFlags |= FLAG_CONTENT_SPATIALIZED;
            }
            else
            {
                mFlags &= ~FLAG_CONTENT_SPATIALIZED;
            }

            return this;
        }

        public Builder setLegacyStreamType(int streamType)
        {
            switch (streamType)
            {
                case AudioManager.STREAM_VOICE_CALL:
                    mContentType = CONTENT_TYPE_SPEECH;
                    mUsage = USAGE_VOICE_COMMUNICATION;
                    break;
                case 7:
                    mFlags |= FLAG_AUDIBILITY_ENFORCED;
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    mUsage = USAGE_ASSISTANCE_SONIFICATION;
                    break;
                case AudioManager.STREAM_SYSTEM:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    mUsage = USAGE_ASSISTANCE_SONIFICATION;
                    break;
                case AudioManager.STREAM_RING:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    mUsage = USAGE_NOTIFICATION_RINGTONE;
                    break;
                case AudioManager.STREAM_MUSIC:
                    mContentType = CONTENT_TYPE_MUSIC;
                    mUsage = USAGE_MEDIA;
                    break;
                case AudioManager.STREAM_ALARM:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    mUsage = USAGE_ALARM;
                    break;
                case AudioManager.STREAM_NOTIFICATION:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    mUsage = USAGE_NOTIFICATION;
                    break;
                case 6:
                    mContentType = CONTENT_TYPE_SPEECH;
                    mUsage = USAGE_VOICE_COMMUNICATION;
                    mFlags |= FLAG_SCO;
                    break;
                case AudioManager.STREAM_DTMF:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    mUsage = USAGE_VOICE_COMMUNICATION_SIGNALLING;
                    break;
                case 9:
                    mContentType = CONTENT_TYPE_SONIFICATION;
                    mUsage = USAGE_ASSISTANCE_ACCESSIBILITY;
                    mFlags |= FLAG_BEACON;
                    break;
                case AudioManager.STREAM_ACCESSIBILITY:
                    mContentType = CONTENT_TYPE_SPEECH;
                    mUsage = USAGE_ASSISTANCE_ACCESSIBILITY;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid stream type " + streamType + " for AudioAttributes");
            }

            mLegacyStreamType = streamType;
            return this;
        }

        public Builder setSpatializationBehavior(int behavior)
        {
            if (behavior == SPATIALIZATION_BEHAVIOR_NEVER)
            {
                mFlags |= FLAG_NEVER_SPATIALIZE;
            }
            else if (behavior == SPATIALIZATION_BEHAVIOR_AUTO)
            {
                mFlags &= ~FLAG_NEVER_SPATIALIZE;
            }
            else
            {
                throw new IllegalArgumentException("Invalid spatialization behavior " + behavior);
            }

            return this;
        }

        public Builder setUsage(int usage)
        {
            mUsage = isValidUsage(usage) ? usage : USAGE_UNKNOWN;
            return this;
        }
    }
}
