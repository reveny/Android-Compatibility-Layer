package android.media.audiofx;

public final class DynamicsProcessing extends AudioEffect
{
    public static final int VARIANT_FAVOR_FREQUENCY_RESOLUTION = 0;
    public static final int VARIANT_FAVOR_TIME_RESOLUTION = 1;

    private static final int DEFAULT_CHANNEL_COUNT = 2;
    private static final float DEFAULT_MIN_FREQUENCY = 220.0f;
    private static final float DEFAULT_MAX_FREQUENCY = 20000.0f;
    private static final float DEFAULT_PREFERRED_FRAME_DURATION = 10.0f;
    private static final float DEFAULT_INPUT_GAIN = 0.0f;
    private static final float DEFAULT_EQ_GAIN = 0.0f;
    private static final float DEFAULT_MBC_ATTACK_TIME = 3.0f;
    private static final float DEFAULT_MBC_RELEASE_TIME = 80.0f;
    private static final float DEFAULT_MBC_RATIO = 1.0f;
    private static final float DEFAULT_MBC_THRESHOLD = -45.0f;
    private static final float DEFAULT_MBC_KNEE_WIDTH = 0.0f;
    private static final float DEFAULT_MBC_NOISE_GATE_THRESHOLD = -90.0f;
    private static final float DEFAULT_MBC_EXPANDER_RATIO = 1.0f;
    private static final float DEFAULT_MBC_PRE_GAIN = 0.0f;
    private static final float DEFAULT_MBC_POST_GAIN = 0.0f;
    private static final float DEFAULT_LIMITER_ATTACK_TIME = 1.0f;
    private static final float DEFAULT_LIMITER_RELEASE_TIME = 60.0f;
    private static final float DEFAULT_LIMITER_RATIO = 10.0f;
    private static final float DEFAULT_LIMITER_THRESHOLD = -2.0f;
    private static final float DEFAULT_LIMITER_POST_GAIN = 0.0f;
    private static final int DEFAULT_LINK_GROUP = 0;

    private final Config mConfig;

    public DynamicsProcessing(int audioSession)
    {
        this(0, audioSession, null);
    }

    public DynamicsProcessing(int priority, int audioSession, DynamicsProcessing.Config config)
    {
        super(EFFECT_TYPE_DYNAMICS_PROCESSING, EFFECT_TYPE_NULL, priority, audioSession);
        mConfig = config != null ? config : new Config.Builder(VARIANT_FAVOR_FREQUENCY_RESOLUTION, DEFAULT_CHANNEL_COUNT,
            true, 6, true, 6, true, 6, true).build();
    }

    private static float defaultCutoff(int band, int bandCount)
    {
        if (bandCount <= 1) return DEFAULT_MAX_FREQUENCY;

        double ratio = Math.pow(DEFAULT_MAX_FREQUENCY / DEFAULT_MIN_FREQUENCY, band / (double)(bandCount - 1));
        return (float)(DEFAULT_MIN_FREQUENCY * ratio);
    }

    public DynamicsProcessing.Channel getChannelByChannelIndex(int channelIndex)
    {
        return mConfig.getChannelByChannelIndex(channelIndex);
    }

    public int getChannelCount()
    {
        return mConfig.mChannelCount;
    }

    public DynamicsProcessing.Config getConfig()
    {
        return mConfig;
    }

    public float getInputGainByChannelIndex(int channelIndex)
    {
        return mConfig.getInputGainByChannelIndex(channelIndex);
    }

    public DynamicsProcessing.Limiter getLimiterByChannelIndex(int channelIndex)
    {
        return mConfig.getLimiterByChannelIndex(channelIndex);
    }

    public DynamicsProcessing.MbcBand getMbcBandByChannelIndex(int channelIndex, int band)
    {
        return mConfig.getMbcBandByChannelIndex(channelIndex, band);
    }

    public DynamicsProcessing.Mbc getMbcByChannelIndex(int channelIndex)
    {
        return mConfig.getMbcByChannelIndex(channelIndex);
    }

    public DynamicsProcessing.EqBand getPostEqBandByChannelIndex(int channelIndex, int band)
    {
        return mConfig.getPostEqBandByChannelIndex(channelIndex, band);
    }

    public DynamicsProcessing.Eq getPostEqByChannelIndex(int channelIndex)
    {
        return mConfig.getPostEqByChannelIndex(channelIndex);
    }

    public DynamicsProcessing.EqBand getPreEqBandByChannelIndex(int channelIndex, int band)
    {
        return mConfig.getPreEqBandByChannelIndex(channelIndex, band);
    }

    public DynamicsProcessing.Eq getPreEqByChannelIndex(int channelIndex)
    {
        return mConfig.getPreEqByChannelIndex(channelIndex);
    }

    public void setAllChannelsTo(DynamicsProcessing.Channel channel)
    {
        mConfig.setAllChannelsTo(channel);
    }

    public void setChannelTo(int channelIndex, DynamicsProcessing.Channel channel)
    {
        mConfig.setChannelTo(channelIndex, channel);
    }

    public void setInputGainAllChannelsTo(float inputGain)
    {
        mConfig.setInputGainAllChannelsTo(inputGain);
    }

    public void setInputGainbyChannel(int channelIndex, float inputGain)
    {
        mConfig.setInputGainByChannelIndex(channelIndex, inputGain);
    }

    public void setLimiterAllChannelsTo(DynamicsProcessing.Limiter limiter)
    {
        mConfig.setLimiterAllChannelsTo(limiter);
    }

    public void setLimiterByChannelIndex(int channelIndex, DynamicsProcessing.Limiter limiter)
    {
        mConfig.setLimiterByChannelIndex(channelIndex, limiter);
    }

    public void setMbcAllChannelsTo(DynamicsProcessing.Mbc mbc)
    {
        mConfig.setMbcAllChannelsTo(mbc);
    }

    public void setMbcBandAllChannelsTo(int band, DynamicsProcessing.MbcBand mbcBand)
    {
        mConfig.setMbcBandAllChannelsTo(band, mbcBand);
    }

    public void setMbcBandByChannelIndex(int channelIndex, int band, DynamicsProcessing.MbcBand mbcBand)
    {
        mConfig.setMbcBandByChannelIndex(channelIndex, band, mbcBand);
    }

    public void setMbcByChannelIndex(int channelIndex, DynamicsProcessing.Mbc mbc)
    {
        mConfig.setMbcByChannelIndex(channelIndex, mbc);
    }

    public void setPostEqAllChannelsTo(DynamicsProcessing.Eq postEq)
    {
        mConfig.setPostEqAllChannelsTo(postEq);
    }

    public void setPostEqBandAllChannelsTo(int band, DynamicsProcessing.EqBand postEqBand)
    {
        mConfig.setPostEqBandAllChannelsTo(band, postEqBand);
    }

    public void setPostEqBandByChannelIndex(int channelIndex, int band, DynamicsProcessing.EqBand postEqBand)
    {
        mConfig.setPostEqBandByChannelIndex(channelIndex, band, postEqBand);
    }

    public void setPostEqByChannelIndex(int channelIndex, DynamicsProcessing.Eq postEq)
    {
        mConfig.setPostEqByChannelIndex(channelIndex, postEq);
    }

    public void setPreEqAllChannelsTo(DynamicsProcessing.Eq preEq)
    {
        mConfig.setPreEqAllChannelsTo(preEq);
    }

    public void setPreEqBandAllChannelsTo(int band, DynamicsProcessing.EqBand preEqBand)
    {
        mConfig.setPreEqBandAllChannelsTo(band, preEqBand);
    }

    public void setPreEqBandByChannelIndex(int channelIndex, int band, DynamicsProcessing.EqBand preEqBand)
    {
        mConfig.setPreEqBandByChannelIndex(channelIndex, band, preEqBand);
    }

    public void setPreEqByChannelIndex(int channelIndex, DynamicsProcessing.Eq preEq)
    {
        mConfig.setPreEqByChannelIndex(channelIndex, preEq);
    }

    public static class Stage
    {
        private boolean mInUse;
        private boolean mEnabled;

        public Stage(boolean inUse, boolean enabled)
        {
            mInUse = inUse;
            mEnabled = enabled;
        }

        public boolean isEnabled()
        {
            return mEnabled;
        }

        public boolean isInUse()
        {
            return mInUse;
        }

        public void setEnabled(boolean enabled)
        {
            mEnabled = enabled;
        }

        @Override
        public String toString()
        {
            return String.format(" Stage InUse: %b\n Stage Enabled: %b\n", mInUse, mEnabled);
        }
    }

    public static class BandStage extends DynamicsProcessing.Stage
    {
        private final int mBandCount;

        public BandStage(boolean inUse, boolean enabled, int bandCount)
        {
            super(inUse, enabled);
            mBandCount = inUse ? bandCount : 0;
        }

        public int getBandCount()
        {
            return mBandCount;
        }

        @Override
        public String toString()
        {
            String text = super.toString();
            if (isInUse()) text += " Band Count: " + mBandCount + "\n";
            return text;
        }
    }

    public static class BandBase
    {
        private boolean mEnabled;
        private float mCutoffFrequency;

        public BandBase(boolean enabled, float cutoffFrequency)
        {
            mEnabled = enabled;
            mCutoffFrequency = cutoffFrequency;
        }

        public float getCutoffFrequency()
        {
            return mCutoffFrequency;
        }

        public boolean isEnabled()
        {
            return mEnabled;
        }

        public void setCutoffFrequency(float frequency)
        {
            mCutoffFrequency = frequency;
        }

        public void setEnabled(boolean enabled)
        {
            mEnabled = enabled;
        }

        @Override
        public String toString()
        {
            return String.format(" Enabled: %b\n CutoffFrequency: %f\n", mEnabled, mCutoffFrequency);
        }
    }

    public static final class MbcBand extends DynamicsProcessing.BandBase
    {
        private float mAttackTime;
        private float mReleaseTime;
        private float mRatio;
        private float mThreshold;
        private float mKneeWidth;
        private float mNoiseGateThreshold;
        private float mExpanderRatio;
        private float mPreGain;
        private float mPostGain;

        public MbcBand(DynamicsProcessing.MbcBand mbcBand)
        {
            super(mbcBand.isEnabled(), mbcBand.getCutoffFrequency());
            mAttackTime = mbcBand.mAttackTime;
            mReleaseTime = mbcBand.mReleaseTime;
            mRatio = mbcBand.mRatio;
            mThreshold = mbcBand.mThreshold;
            mKneeWidth = mbcBand.mKneeWidth;
            mNoiseGateThreshold = mbcBand.mNoiseGateThreshold;
            mExpanderRatio = mbcBand.mExpanderRatio;
            mPreGain = mbcBand.mPreGain;
            mPostGain = mbcBand.mPostGain;
        }

        public MbcBand(boolean enabled, float cutoffFrequency, float attackTime, float releaseTime, float ratio, float threshold,
            float kneeWidth, float noiseGateThreshold, float expanderRatio, float preGain, float postGain)
        {
            super(enabled, cutoffFrequency);
            mAttackTime = attackTime;
            mReleaseTime = releaseTime;
            mRatio = ratio;
            mThreshold = threshold;
            mKneeWidth = kneeWidth;
            mNoiseGateThreshold = noiseGateThreshold;
            mExpanderRatio = expanderRatio;
            mPreGain = preGain;
            mPostGain = postGain;
        }

        public float getAttackTime()
        {
            return mAttackTime;
        }

        public float getExpanderRatio()
        {
            return mExpanderRatio;
        }

        public float getKneeWidth()
        {
            return mKneeWidth;
        }

        public float getNoiseGateThreshold()
        {
            return mNoiseGateThreshold;
        }

        public float getPostGain()
        {
            return mPostGain;
        }

        public float getPreGain()
        {
            return mPreGain;
        }

        public float getRatio()
        {
            return mRatio;
        }

        public float getReleaseTime()
        {
            return mReleaseTime;
        }

        public float getThreshold()
        {
            return mThreshold;
        }

        public void setAttackTime(float attackTime)
        {
            mAttackTime = attackTime;
        }

        public void setExpanderRatio(float expanderRatio)
        {
            mExpanderRatio = expanderRatio;
        }

        public void setKneeWidth(float kneeWidth)
        {
            mKneeWidth = kneeWidth;
        }

        public void setNoiseGateThreshold(float noiseGateThreshold)
        {
            mNoiseGateThreshold = noiseGateThreshold;
        }

        public void setPostGain(float postGain)
        {
            mPostGain = postGain;
        }

        public void setPreGain(float preGain)
        {
            mPreGain = preGain;
        }

        public void setRatio(float ratio)
        {
            mRatio = ratio;
        }

        public void setReleaseTime(float releaseTime)
        {
            mReleaseTime = releaseTime;
        }

        public void setThreshold(float threshold)
        {
            mThreshold = threshold;
        }

        @Override
        public String toString()
        {
            return super.toString() + String.format(" AttackTime: %f (ms)\n ReleaseTime: %f (ms)\n Ratio: 1:%f\n Threshold: %f (dB)\n"
                + " NoiseGateThreshold: %f(dB)\n ExpanderRatio: %f:1\n PreGain: %f (dB)\n PostGain: %f (dB)\n", mAttackTime,
                mReleaseTime, mRatio, mThreshold, mNoiseGateThreshold, mExpanderRatio, mPreGain, mPostGain);
        }
    }

    public static final class Mbc extends DynamicsProcessing.BandStage
    {
        private final MbcBand[] mBands;

        public Mbc(DynamicsProcessing.Mbc mbc)
        {
            super(mbc.isInUse(), mbc.isEnabled(), mbc.getBandCount());
            mBands = new MbcBand[getBandCount()];
            for (int band = 0; band < mBands.length; band++)
            {
                mBands[band] = new MbcBand(mbc.mBands[band]);
            }
        }

        public Mbc(boolean inUse, boolean enabled, int bandCount)
        {
            super(inUse, enabled, bandCount);
            mBands = new MbcBand[getBandCount()];
            for (int band = 0; band < mBands.length; band++)
            {
                mBands[band] = new MbcBand(true, defaultCutoff(band, mBands.length), DEFAULT_MBC_ATTACK_TIME, DEFAULT_MBC_RELEASE_TIME,
                    DEFAULT_MBC_RATIO, DEFAULT_MBC_THRESHOLD, DEFAULT_MBC_KNEE_WIDTH, DEFAULT_MBC_NOISE_GATE_THRESHOLD,
                    DEFAULT_MBC_EXPANDER_RATIO, DEFAULT_MBC_PRE_GAIN, DEFAULT_MBC_POST_GAIN);
            }
        }

        public DynamicsProcessing.MbcBand getBand(int band)
        {
            checkBand(band, mBands.length);
            return mBands[band];
        }

        public void setBand(int band, DynamicsProcessing.MbcBand bandCfg)
        {
            checkBand(band, mBands.length);
            mBands[band] = new MbcBand(bandCfg);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(super.toString());
            for (int band = 0; band < mBands.length; band++)
            {
                builder.append(" band: ").append(band).append('\n').append(mBands[band]);
            }

            return builder.toString();
        }
    }

    public static final class Limiter extends DynamicsProcessing.Stage
    {
        private int mLinkGroup;
        private float mAttackTime;
        private float mReleaseTime;
        private float mRatio;
        private float mThreshold;
        private float mPostGain;

        public Limiter(DynamicsProcessing.Limiter limiter)
        {
            super(limiter.isInUse(), limiter.isEnabled());
            mLinkGroup = limiter.mLinkGroup;
            mAttackTime = limiter.mAttackTime;
            mReleaseTime = limiter.mReleaseTime;
            mRatio = limiter.mRatio;
            mThreshold = limiter.mThreshold;
            mPostGain = limiter.mPostGain;
        }

        public Limiter(boolean inUse, boolean enabled, int linkGroup, float attackTime, float releaseTime, float ratio,
            float threshold, float postGain)
        {
            super(inUse, enabled);
            mLinkGroup = linkGroup;
            mAttackTime = attackTime;
            mReleaseTime = releaseTime;
            mRatio = ratio;
            mThreshold = threshold;
            mPostGain = postGain;
        }

        public float getAttackTime()
        {
            return mAttackTime;
        }

        public int getLinkGroup()
        {
            return mLinkGroup;
        }

        public float getPostGain()
        {
            return mPostGain;
        }

        public float getRatio()
        {
            return mRatio;
        }

        public float getReleaseTime()
        {
            return mReleaseTime;
        }

        public float getThreshold()
        {
            return mThreshold;
        }

        public void setAttackTime(float attackTime)
        {
            mAttackTime = attackTime;
        }

        public void setLinkGroup(int linkGroup)
        {
            mLinkGroup = linkGroup;
        }

        public void setPostGain(float postGain)
        {
            mPostGain = postGain;
        }

        public void setRatio(float ratio)
        {
            mRatio = ratio;
        }

        public void setReleaseTime(float releaseTime)
        {
            mReleaseTime = releaseTime;
        }

        public void setThreshold(float threshold)
        {
            mThreshold = threshold;
        }

        @Override
        public String toString()
        {
            String text = super.toString();
            if (!isInUse()) return text;

            return text + String.format(" LinkGroup: %d (group)\n AttackTime: %f (ms)\n ReleaseTime: %f (ms)\n Ratio: 1:%f\n"
                + " Threshold: %f (dB)\n PostGain: %f (dB)\n", mLinkGroup, mAttackTime, mReleaseTime, mRatio, mThreshold, mPostGain);
        }
    }

    public static final class EqBand extends DynamicsProcessing.BandBase
    {
        private float mGain;

        public EqBand(DynamicsProcessing.EqBand eqBand)
        {
            super(eqBand.isEnabled(), eqBand.getCutoffFrequency());
            mGain = eqBand.mGain;
        }

        public EqBand(boolean enabled, float cutoffFrequency, float gain)
        {
            super(enabled, cutoffFrequency);
            mGain = gain;
        }

        public float getGain()
        {
            return mGain;
        }

        public void setGain(float gain)
        {
            mGain = gain;
        }

        @Override
        public String toString()
        {
            return super.toString() + String.format(" Gain: %f\n", mGain);
        }
    }

    public static final class Eq extends DynamicsProcessing.BandStage
    {
        private final EqBand[] mBands;

        public Eq(DynamicsProcessing.Eq eq)
        {
            super(eq.isInUse(), eq.isEnabled(), eq.getBandCount());
            mBands = new EqBand[getBandCount()];
            for (int band = 0; band < mBands.length; band++)
            {
                mBands[band] = new EqBand(eq.mBands[band]);
            }
        }

        public Eq(boolean inUse, boolean enabled, int bandCount)
        {
            super(inUse, enabled, bandCount);
            mBands = new EqBand[getBandCount()];
            for (int band = 0; band < mBands.length; band++)
            {
                mBands[band] = new EqBand(true, defaultCutoff(band, mBands.length), DEFAULT_EQ_GAIN);
            }
        }

        public DynamicsProcessing.EqBand getBand(int band)
        {
            checkBand(band, mBands.length);
            return mBands[band];
        }

        public void setBand(int band, DynamicsProcessing.EqBand bandCfg)
        {
            checkBand(band, mBands.length);
            mBands[band] = new EqBand(bandCfg);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(super.toString());
            for (int band = 0; band < mBands.length; band++)
            {
                builder.append(" band: ").append(band).append('\n').append(mBands[band]);
            }

            return builder.toString();
        }
    }

    private static void checkBand(int band, int bandCount)
    {
        if (band < 0 || band >= bandCount)
        {
            throw new IllegalArgumentException("band index " + band + " out of bounds. Max band count " + bandCount);
        }
    }

    public static final class Config
    {
        private final int mVariant;
        private final int mChannelCount;
        private final boolean mPreEqInUse;
        private final int mPreEqBandCount;
        private final boolean mMbcInUse;
        private final int mMbcBandCount;
        private final boolean mPostEqInUse;
        private final int mPostEqBandCount;
        private final boolean mLimiterInUse;
        private final float mPreferredFrameDuration;
        private final Channel[] mChannels;

        private Config(int variant, float preferredFrameDuration, int channelCount, boolean preEqInUse, int preEqBandCount,
            boolean mbcInUse, int mbcBandCount, boolean postEqInUse, int postEqBandCount, boolean limiterInUse, Channel[] channels)
        {
            mVariant = variant;
            mPreferredFrameDuration = preferredFrameDuration;
            mChannelCount = channelCount;
            mPreEqInUse = preEqInUse;
            mPreEqBandCount = preEqBandCount;
            mMbcInUse = mbcInUse;
            mMbcBandCount = mbcBandCount;
            mPostEqInUse = postEqInUse;
            mPostEqBandCount = postEqBandCount;
            mLimiterInUse = limiterInUse;
            mChannels = new Channel[channelCount];

            for (int channel = 0; channel < channelCount; channel++)
            {
                if (channel < channels.length) mChannels[channel] = new Channel(channels[channel]);
            }
        }

        private void checkChannel(int channelIndex)
        {
            if (channelIndex < 0 || channelIndex >= mChannels.length)
            {
                throw new IllegalArgumentException("ChannelIndex out of bounds");
            }
        }

        public DynamicsProcessing.Channel getChannelByChannelIndex(int channelIndex)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex];
        }

        public float getInputGainByChannelIndex(int channelIndex)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex].getInputGain();
        }

        public DynamicsProcessing.Limiter getLimiterByChannelIndex(int channelIndex)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex].getLimiter();
        }

        public DynamicsProcessing.MbcBand getMbcBandByChannelIndex(int channelIndex, int band)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex].getMbcBand(band);
        }

        public int getMbcBandCount()
        {
            return mMbcBandCount;
        }

        public DynamicsProcessing.Mbc getMbcByChannelIndex(int channelIndex)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex].getMbc();
        }

        public DynamicsProcessing.EqBand getPostEqBandByChannelIndex(int channelIndex, int band)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex].getPostEqBand(band);
        }

        public int getPostEqBandCount()
        {
            return mPostEqBandCount;
        }

        public DynamicsProcessing.Eq getPostEqByChannelIndex(int channelIndex)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex].getPostEq();
        }

        public DynamicsProcessing.EqBand getPreEqBandByChannelIndex(int channelIndex, int band)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex].getPreEqBand(band);
        }

        public int getPreEqBandCount()
        {
            return mPreEqBandCount;
        }

        public DynamicsProcessing.Eq getPreEqByChannelIndex(int channelIndex)
        {
            checkChannel(channelIndex);
            return mChannels[channelIndex].getPreEq();
        }

        public float getPreferredFrameDuration()
        {
            return mPreferredFrameDuration;
        }

        public int getVariant()
        {
            return mVariant;
        }

        public boolean isLimiterInUse()
        {
            return mLimiterInUse;
        }

        public boolean isMbcInUse()
        {
            return mMbcInUse;
        }

        public boolean isPostEqInUse()
        {
            return mPostEqInUse;
        }

        public boolean isPreEqInUse()
        {
            return mPreEqInUse;
        }

        public void setAllChannelsTo(DynamicsProcessing.Channel channel)
        {
            for (int index = 0; index < mChannels.length; index++)
            {
                setChannelTo(index, channel);
            }
        }

        public void setChannelTo(int channelIndex, DynamicsProcessing.Channel channel)
        {
            checkChannel(channelIndex);

            if (mMbcBandCount != channel.getMbc().getBandCount())
            {
                throw new IllegalArgumentException("MbcBandCount changed from " + mMbcBandCount + " to " + channel.getMbc().getBandCount());
            }

            if (mPreEqBandCount != channel.getPreEq().getBandCount())
            {
                throw new IllegalArgumentException("PreEqBandCount changed from " + mPreEqBandCount + " to "
                    + channel.getPreEq().getBandCount());
            }

            if (mPostEqBandCount != channel.getPostEq().getBandCount())
            {
                throw new IllegalArgumentException("PostEqBandCount changed from " + mPostEqBandCount + " to "
                    + channel.getPostEq().getBandCount());
            }

            mChannels[channelIndex] = new Channel(channel);
        }

        public void setInputGainAllChannelsTo(float inputGain)
        {
            for (Channel channel : mChannels)
            {
                channel.setInputGain(inputGain);
            }
        }

        public void setInputGainByChannelIndex(int channelIndex, float inputGain)
        {
            checkChannel(channelIndex);
            mChannels[channelIndex].setInputGain(inputGain);
        }

        public void setLimiterAllChannelsTo(DynamicsProcessing.Limiter limiter)
        {
            for (Channel channel : mChannels)
            {
                channel.setLimiter(limiter);
            }
        }

        public void setLimiterByChannelIndex(int channelIndex, DynamicsProcessing.Limiter limiter)
        {
            checkChannel(channelIndex);
            mChannels[channelIndex].setLimiter(limiter);
        }

        public void setMbcAllChannelsTo(DynamicsProcessing.Mbc mbc)
        {
            for (Channel channel : mChannels)
            {
                channel.setMbc(mbc);
            }
        }

        public void setMbcBandAllChannelsTo(int band, DynamicsProcessing.MbcBand mbcBand)
        {
            for (Channel channel : mChannels)
            {
                channel.setMbcBand(band, mbcBand);
            }
        }

        public void setMbcBandByChannelIndex(int channelIndex, int band, DynamicsProcessing.MbcBand mbcBand)
        {
            checkChannel(channelIndex);
            mChannels[channelIndex].setMbcBand(band, mbcBand);
        }

        public void setMbcByChannelIndex(int channelIndex, DynamicsProcessing.Mbc mbc)
        {
            checkChannel(channelIndex);
            mChannels[channelIndex].setMbc(mbc);
        }

        public void setPostEqAllChannelsTo(DynamicsProcessing.Eq postEq)
        {
            for (Channel channel : mChannels)
            {
                channel.setPostEq(postEq);
            }
        }

        public void setPostEqBandAllChannelsTo(int band, DynamicsProcessing.EqBand postEqBand)
        {
            for (Channel channel : mChannels)
            {
                channel.setPostEqBand(band, postEqBand);
            }
        }

        public void setPostEqBandByChannelIndex(int channelIndex, int band, DynamicsProcessing.EqBand postEqBand)
        {
            checkChannel(channelIndex);
            mChannels[channelIndex].setPostEqBand(band, postEqBand);
        }

        public void setPostEqByChannelIndex(int channelIndex, DynamicsProcessing.Eq postEq)
        {
            checkChannel(channelIndex);
            mChannels[channelIndex].setPostEq(postEq);
        }

        public void setPreEqAllChannelsTo(DynamicsProcessing.Eq preEq)
        {
            for (Channel channel : mChannels)
            {
                channel.setPreEq(preEq);
            }
        }

        public void setPreEqBandAllChannelsTo(int band, DynamicsProcessing.EqBand preEqBand)
        {
            for (Channel channel : mChannels)
            {
                channel.setPreEqBand(band, preEqBand);
            }
        }

        public void setPreEqBandByChannelIndex(int channelIndex, int band, DynamicsProcessing.EqBand preEqBand)
        {
            checkChannel(channelIndex);
            mChannels[channelIndex].setPreEqBand(band, preEqBand);
        }

        public void setPreEqByChannelIndex(int channelIndex, DynamicsProcessing.Eq preEq)
        {
            checkChannel(channelIndex);
            mChannels[channelIndex].setPreEq(preEq);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("Variant %d\n", mVariant));
            builder.append(String.format("PreferredFrameDuration: %f\n", mPreferredFrameDuration));
            builder.append(String.format("ChannelCount: %d\n", mChannelCount));
            builder.append(String.format("PreEq inUse: %b, bandCount:%d\n", mPreEqInUse, mPreEqBandCount));
            builder.append(String.format("Mbc inUse: %b, bandCount: %d\n", mMbcInUse, mMbcBandCount));
            builder.append(String.format("PostEq inUse: %b, bandCount: %d\n", mPostEqInUse, mPostEqBandCount));
            builder.append(String.format("Limiter inUse: %b\n", mLimiterInUse));

            for (int index = 0; index < mChannels.length; index++)
            {
                builder.append(String.format("==Channel %d\n", index)).append(mChannels[index]);
            }

            return builder.toString();
        }

        public static final class Builder
        {
            private final int mVariant;
            private final int mChannelCount;
            private final boolean mPreEqInUse;
            private final int mPreEqBandCount;
            private final boolean mMbcInUse;
            private final int mMbcBandCount;
            private final boolean mPostEqInUse;
            private final int mPostEqBandCount;
            private final boolean mLimiterInUse;
            private final Channel[] mChannels;
            private float mPreferredFrameDuration = DEFAULT_PREFERRED_FRAME_DURATION;

            public Builder(int variant, int channelCount, boolean preEqInUse, int preEqBandCount, boolean mbcInUse, int mbcBandCount,
                boolean postEqInUse, int postEqBandCount, boolean limiterInUse)
            {
                mVariant = variant;
                mChannelCount = channelCount;
                mPreEqInUse = preEqInUse;
                mPreEqBandCount = preEqBandCount;
                mMbcInUse = mbcInUse;
                mMbcBandCount = mbcBandCount;
                mPostEqInUse = postEqInUse;
                mPostEqBandCount = postEqBandCount;
                mLimiterInUse = limiterInUse;
                mChannels = new Channel[channelCount];

                for (int channel = 0; channel < channelCount; channel++)
                {
                    mChannels[channel] = new Channel(DEFAULT_INPUT_GAIN, preEqInUse, preEqBandCount, mbcInUse, mbcBandCount,
                        postEqInUse, postEqBandCount, limiterInUse);
                }
            }

            private void checkChannel(int channelIndex)
            {
                if (channelIndex < 0 || channelIndex >= mChannels.length)
                {
                    throw new IllegalArgumentException("ChannelIndex out of bounds");
                }
            }

            public DynamicsProcessing.Config build()
            {
                return new Config(mVariant, mPreferredFrameDuration, mChannelCount, mPreEqInUse, mPreEqBandCount, mMbcInUse,
                    mMbcBandCount, mPostEqInUse, mPostEqBandCount, mLimiterInUse, mChannels);
            }

            public DynamicsProcessing.Config.Builder setAllChannelsTo(DynamicsProcessing.Channel channel)
            {
                for (int index = 0; index < mChannels.length; index++)
                {
                    setChannelTo(index, channel);
                }

                return this;
            }

            public DynamicsProcessing.Config.Builder setChannelTo(int channelIndex, DynamicsProcessing.Channel channel)
            {
                checkChannel(channelIndex);

                if (mMbcBandCount != channel.getMbc().getBandCount())
                {
                    throw new IllegalArgumentException("MbcBandCount changed from " + mMbcBandCount + " to "
                        + channel.getMbc().getBandCount());
                }

                if (mPreEqBandCount != channel.getPreEq().getBandCount())
                {
                    throw new IllegalArgumentException("PreEqBandCount changed from " + mPreEqBandCount + " to "
                        + channel.getPreEq().getBandCount());
                }

                if (mPostEqBandCount != channel.getPostEq().getBandCount())
                {
                    throw new IllegalArgumentException("PostEqBandCount changed from " + mPostEqBandCount + " to "
                        + channel.getPostEq().getBandCount());
                }

                mChannels[channelIndex] = new Channel(channel);
                return this;
            }

            public DynamicsProcessing.Config.Builder setInputGainAllChannelsTo(float inputGain)
            {
                for (Channel channel : mChannels)
                {
                    channel.setInputGain(inputGain);
                }

                return this;
            }

            public DynamicsProcessing.Config.Builder setInputGainByChannelIndex(int channelIndex, float inputGain)
            {
                checkChannel(channelIndex);
                mChannels[channelIndex].setInputGain(inputGain);
                return this;
            }

            public DynamicsProcessing.Config.Builder setLimiterAllChannelsTo(DynamicsProcessing.Limiter limiter)
            {
                for (Channel channel : mChannels)
                {
                    channel.setLimiter(limiter);
                }

                return this;
            }

            public DynamicsProcessing.Config.Builder setLimiterByChannelIndex(int channelIndex, DynamicsProcessing.Limiter limiter)
            {
                checkChannel(channelIndex);
                mChannels[channelIndex].setLimiter(limiter);
                return this;
            }

            public DynamicsProcessing.Config.Builder setMbcAllChannelsTo(DynamicsProcessing.Mbc mbc)
            {
                for (Channel channel : mChannels)
                {
                    channel.setMbc(mbc);
                }

                return this;
            }

            public DynamicsProcessing.Config.Builder setMbcByChannelIndex(int channelIndex, DynamicsProcessing.Mbc mbc)
            {
                checkChannel(channelIndex);
                mChannels[channelIndex].setMbc(mbc);
                return this;
            }

            public DynamicsProcessing.Config.Builder setPostEqAllChannelsTo(DynamicsProcessing.Eq postEq)
            {
                for (Channel channel : mChannels)
                {
                    channel.setPostEq(postEq);
                }

                return this;
            }

            public DynamicsProcessing.Config.Builder setPostEqByChannelIndex(int channelIndex, DynamicsProcessing.Eq postEq)
            {
                checkChannel(channelIndex);
                mChannels[channelIndex].setPostEq(postEq);
                return this;
            }

            public DynamicsProcessing.Config.Builder setPreEqAllChannelsTo(DynamicsProcessing.Eq preEq)
            {
                for (Channel channel : mChannels)
                {
                    channel.setPreEq(preEq);
                }

                return this;
            }

            public DynamicsProcessing.Config.Builder setPreEqByChannelIndex(int channelIndex, DynamicsProcessing.Eq preEq)
            {
                checkChannel(channelIndex);
                mChannels[channelIndex].setPreEq(preEq);
                return this;
            }

            public DynamicsProcessing.Config.Builder setPreferredFrameDuration(float frameDuration)
            {
                if (frameDuration < 0) throw new IllegalArgumentException("Expected positive frameDuration");
                mPreferredFrameDuration = frameDuration;
                return this;
            }
        }
    }

    public static final class Channel
    {
        private float mInputGain;
        private Eq mPreEq;
        private Mbc mMbc;
        private Eq mPostEq;
        private Limiter mLimiter;

        public Channel(DynamicsProcessing.Channel channel)
        {
            mInputGain = channel.mInputGain;
            mPreEq = new Eq(channel.mPreEq);
            mMbc = new Mbc(channel.mMbc);
            mPostEq = new Eq(channel.mPostEq);
            mLimiter = new Limiter(channel.mLimiter);
        }

        public Channel(float inputGain, boolean preEqInUse, int preEqBandCount, boolean mbcInUse, int mbcBandCount,
            boolean postEqInUse, int postEqBandCount, boolean limiterInUse)
        {
            mInputGain = inputGain;
            mPreEq = new Eq(preEqInUse, true, preEqBandCount);
            mMbc = new Mbc(mbcInUse, true, mbcBandCount);
            mPostEq = new Eq(postEqInUse, true, postEqBandCount);
            mLimiter = new Limiter(limiterInUse, true, DEFAULT_LINK_GROUP, DEFAULT_LIMITER_ATTACK_TIME, DEFAULT_LIMITER_RELEASE_TIME,
                DEFAULT_LIMITER_RATIO, DEFAULT_LIMITER_THRESHOLD, DEFAULT_LIMITER_POST_GAIN);
        }

        public float getInputGain()
        {
            return mInputGain;
        }

        public DynamicsProcessing.Limiter getLimiter()
        {
            return mLimiter;
        }

        public DynamicsProcessing.Mbc getMbc()
        {
            return mMbc;
        }

        public DynamicsProcessing.MbcBand getMbcBand(int band)
        {
            return mMbc.getBand(band);
        }

        public DynamicsProcessing.Eq getPostEq()
        {
            return mPostEq;
        }

        public DynamicsProcessing.EqBand getPostEqBand(int band)
        {
            return mPostEq.getBand(band);
        }

        public DynamicsProcessing.Eq getPreEq()
        {
            return mPreEq;
        }

        public DynamicsProcessing.EqBand getPreEqBand(int band)
        {
            return mPreEq.getBand(band);
        }

        public void setInputGain(float inputGain)
        {
            mInputGain = inputGain;
        }

        public void setLimiter(DynamicsProcessing.Limiter limiter)
        {
            if (limiter.isInUse() != mLimiter.isInUse()) throw new IllegalArgumentException("Limiter inUse changed");
            mLimiter = new Limiter(limiter);
        }

        public void setMbc(DynamicsProcessing.Mbc mbc)
        {
            if (mbc.getBandCount() != mMbc.getBandCount())
            {
                throw new IllegalArgumentException("MbcBandCount changed from " + mMbc.getBandCount() + " to " + mbc.getBandCount());
            }

            mMbc = new Mbc(mbc);
        }

        public void setMbcBand(int band, DynamicsProcessing.MbcBand mbcBand)
        {
            mMbc.setBand(band, mbcBand);
        }

        public void setPostEq(DynamicsProcessing.Eq postEq)
        {
            if (postEq.getBandCount() != mPostEq.getBandCount())
            {
                throw new IllegalArgumentException("PostEqBandCount changed from " + mPostEq.getBandCount() + " to "
                    + postEq.getBandCount());
            }

            mPostEq = new Eq(postEq);
        }

        public void setPostEqBand(int band, DynamicsProcessing.EqBand postEqBand)
        {
            mPostEq.setBand(band, postEqBand);
        }

        public void setPreEq(DynamicsProcessing.Eq preEq)
        {
            if (preEq.getBandCount() != mPreEq.getBandCount())
            {
                throw new IllegalArgumentException("PreEqBandCount changed from " + mPreEq.getBandCount() + " to "
                    + preEq.getBandCount());
            }

            mPreEq = new Eq(preEq);
        }

        public void setPreEqBand(int band, DynamicsProcessing.EqBand preEqBand)
        {
            mPreEq.setBand(band, preEqBand);
        }

        @Override
        public String toString()
        {
            return String.format(" InputGain: %f\n", mInputGain) + "-->PreEq\n" + mPreEq + "-->MBC\n" + mMbc + "-->PostEq\n" + mPostEq
                + "-->Limiter\n" + mLimiter;
        }
    }
}
