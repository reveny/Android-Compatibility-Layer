package android.media;

import android.os.Handler;

public final class AudioFocusRequest
{
    private static final AudioAttributes DEFAULT_ATTRIBUTES =
        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();

    private final AudioManager.OnAudioFocusChangeListener mListener;
    private final Handler mHandler;
    private final AudioAttributes mAttributes;
    private final int mFocusGain;
    private final boolean mAcceptsDelayedFocusGain;
    private final boolean mPausesWhenDucked;
    private final boolean mForceDucking;

    private AudioFocusRequest(AudioManager.OnAudioFocusChangeListener listener, Handler handler, AudioAttributes attributes,
        int focusGain, boolean acceptsDelayedFocusGain, boolean pausesWhenDucked, boolean forceDucking)
    {
        mListener = listener;
        mHandler = handler;
        mAttributes = attributes;
        mFocusGain = focusGain;
        mAcceptsDelayedFocusGain = acceptsDelayedFocusGain;
        mPausesWhenDucked = pausesWhenDucked;
        mForceDucking = forceDucking;
    }

    static boolean isValidFocusGain(int focusGain)
    {
        return focusGain >= AudioManager.AUDIOFOCUS_GAIN && focusGain <= AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE;
    }

    public boolean acceptsDelayedFocusGain()
    {
        return mAcceptsDelayedFocusGain;
    }

    public AudioAttributes getAudioAttributes()
    {
        return mAttributes;
    }

    public int getFocusGain()
    {
        return mFocusGain;
    }

    public boolean willPauseWhenDucked()
    {
        return mPausesWhenDucked;
    }

    AudioManager.OnAudioFocusChangeListener getOnAudioFocusChangeListener()
    {
        return mListener;
    }

    Handler getOnAudioFocusChangeListenerHandler()
    {
        return mHandler;
    }

    boolean forcesDucking()
    {
        return mForceDucking;
    }

    public static final class Builder
    {
        private AudioManager.OnAudioFocusChangeListener mListener;
        private Handler mHandler;
        private AudioAttributes mAttributes = DEFAULT_ATTRIBUTES;
        private int mFocusGain;
        private boolean mAcceptsDelayedFocusGain;
        private boolean mPausesWhenDucked;
        private boolean mForceDucking;

        public Builder(AudioFocusRequest request)
        {
            if (request == null) throw new IllegalArgumentException("Illegal null AudioFocusRequest");

            mListener = request.mListener;
            mHandler = request.mHandler;
            mAttributes = request.mAttributes;
            mFocusGain = request.mFocusGain;
            mAcceptsDelayedFocusGain = request.mAcceptsDelayedFocusGain;
            mPausesWhenDucked = request.mPausesWhenDucked;
            mForceDucking = request.mForceDucking;
        }

        public Builder(int focusGain)
        {
            setFocusGain(focusGain);
        }

        public AudioFocusRequest build()
        {
            if ((mAcceptsDelayedFocusGain || mPausesWhenDucked) && mListener == null)
            {
                throw new IllegalStateException("Can't use delayed focus or pause on duck without a listener");
            }

            return new AudioFocusRequest(mListener, mHandler, mAttributes, mFocusGain, mAcceptsDelayedFocusGain,
                mPausesWhenDucked, mForceDucking);
        }

        public Builder setAcceptsDelayedFocusGain(boolean acceptsDelayedFocusGain)
        {
            mAcceptsDelayedFocusGain = acceptsDelayedFocusGain;
            return this;
        }

        public Builder setAudioAttributes(AudioAttributes attributes)
        {
            if (attributes == null) throw new NullPointerException("Illegal null AudioAttributes");

            mAttributes = attributes;
            return this;
        }

        public Builder setFocusGain(int focusGain)
        {
            if (!isValidFocusGain(focusGain)) throw new IllegalArgumentException("Illegal audio focus gain type " + focusGain);

            mFocusGain = focusGain;
            return this;
        }

        public Builder setForceDucking(boolean forceDucking)
        {
            mForceDucking = forceDucking;
            return this;
        }

        public Builder setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener listener)
        {
            if (listener == null) throw new NullPointerException("Illegal null focus listener");

            mListener = listener;
            mHandler = null;
            return this;
        }

        public Builder setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener listener, Handler handler)
        {
            if (listener == null || handler == null) throw new NullPointerException("Illegal null focus listener or handler");

            mListener = listener;
            mHandler = handler;
            return this;
        }

        public Builder setWillPauseWhenDucked(boolean pauseOnDuck)
        {
            mPausesWhenDucked = pauseOnDuck;
            return this;
        }
    }
}
