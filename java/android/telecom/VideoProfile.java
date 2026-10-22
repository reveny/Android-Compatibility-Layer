package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;

public class VideoProfile implements Parcelable
{
    public static final int QUALITY_HIGH = 1;
    public static final int QUALITY_MEDIUM = 2;
    public static final int QUALITY_LOW = 3;
    public static final int QUALITY_DEFAULT = 4;

    public static final int STATE_AUDIO_ONLY = 0x0;
    public static final int STATE_TX_ENABLED = 0x1;
    public static final int STATE_RX_ENABLED = 0x2;
    public static final int STATE_BIDIRECTIONAL = STATE_TX_ENABLED | STATE_RX_ENABLED;
    public static final int STATE_PAUSED = 0x4;

    public static final Parcelable.Creator<VideoProfile> CREATOR = new Parcelable.Creator<VideoProfile>()
    {
        @Override
        public VideoProfile createFromParcel(Parcel source)
        {
            int state = source.readInt();
            int quality = source.readInt();
            return new VideoProfile(state, quality);
        }

        @Override
        public VideoProfile[] newArray(int size)
        {
            return new VideoProfile[size];
        }
    };

    private final int mVideoState;
    private final int mQuality;

    public VideoProfile(int videoState)
    {
        this(videoState, QUALITY_DEFAULT);
    }

    public VideoProfile(int videoState, int quality)
    {
        mVideoState = videoState;
        mQuality = quality;
    }

    public int getVideoState()
    {
        return mVideoState;
    }

    public int getQuality()
    {
        return mQuality;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mVideoState);
        dest.writeInt(mQuality);
    }

    @Override
    public String toString()
    {
        return "[VideoProfile videoState = " + videoStateToString(mVideoState) + " videoQuality = " + mQuality + "]";
    }

    public static String videoStateToString(int videoState)
    {
        StringBuilder builder = new StringBuilder("Audio");
        if (videoState == STATE_AUDIO_ONLY)
        {
            builder.append(" Only");
        }
        else
        {
            if (isTransmissionEnabled(videoState)) builder.append(" Tx");
            if (isReceptionEnabled(videoState)) builder.append(" Rx");
            if (isPaused(videoState)) builder.append(" Pause");
        }
        return builder.toString();
    }

    public static boolean isAudioOnly(int videoState)
    {
        return !hasState(videoState, STATE_TX_ENABLED) && !hasState(videoState, STATE_RX_ENABLED);
    }

    public static boolean isVideo(int videoState)
    {
        return hasState(videoState, STATE_TX_ENABLED) || hasState(videoState, STATE_RX_ENABLED)
            || hasState(videoState, STATE_BIDIRECTIONAL);
    }

    public static boolean isTransmissionEnabled(int videoState)
    {
        return hasState(videoState, STATE_TX_ENABLED);
    }

    public static boolean isReceptionEnabled(int videoState)
    {
        return hasState(videoState, STATE_RX_ENABLED);
    }

    public static boolean isBidirectional(int videoState)
    {
        return hasState(videoState, STATE_BIDIRECTIONAL);
    }

    public static boolean isPaused(int videoState)
    {
        return hasState(videoState, STATE_PAUSED);
    }

    private static boolean hasState(int videoState, int state)
    {
        return (videoState & state) == state;
    }

    public static final class CameraCapabilities implements Parcelable
    {
        public static final Parcelable.Creator<CameraCapabilities> CREATOR = new Parcelable.Creator<CameraCapabilities>()
        {
            @Override
            public CameraCapabilities createFromParcel(Parcel source)
            {
                int width = source.readInt();
                int height = source.readInt();
                boolean zoomSupported = source.readBoolean();
                float maxZoom = source.readFloat();
                return new CameraCapabilities(width, height, zoomSupported, maxZoom);
            }

            @Override
            public CameraCapabilities[] newArray(int size)
            {
                return new CameraCapabilities[size];
            }
        };

        private final int mWidth;
        private final int mHeight;
        private final boolean mZoomSupported;
        private final float mMaxZoom;

        public CameraCapabilities(int width, int height)
        {
            this(width, height, false, 1.0f);
        }

        public CameraCapabilities(int width, int height, boolean zoomSupported, float maxZoom)
        {
            mWidth = width;
            mHeight = height;
            mZoomSupported = zoomSupported;
            mMaxZoom = maxZoom;
        }

        public int getWidth()
        {
            return mWidth;
        }

        public int getHeight()
        {
            return mHeight;
        }

        public boolean isZoomSupported()
        {
            return mZoomSupported;
        }

        public float getMaxZoom()
        {
            return mMaxZoom;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(mWidth);
            dest.writeInt(mHeight);
            dest.writeBoolean(mZoomSupported);
            dest.writeFloat(mMaxZoom);
        }
    }
}
