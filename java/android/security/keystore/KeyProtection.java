package android.security.keystore;

import java.security.KeyStore;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

public final class KeyProtection implements KeyStore.ProtectionParameter
{
    private final int mPurposes;
    private final Date mKeyValidityStart;
    private final Date mKeyValidityForOriginationEnd;
    private final Date mKeyValidityForConsumptionEnd;
    private final String[] mEncryptionPaddings;
    private final String[] mSignaturePaddings;
    private final String[] mDigests;
    private final Set<String> mMgf1Digests;
    private final String[] mBlockModes;
    private final boolean mRandomizedEncryptionRequired;
    private final boolean mUserAuthenticationRequired;
    private final int mUserAuthenticationType;
    private final int mUserAuthenticationValidityDurationSeconds;
    private final boolean mUserPresenceRequired;
    private final boolean mUserAuthenticationValidWhileOnBody;
    private final boolean mInvalidatedByBiometricEnrollment;
    private final boolean mUserConfirmationRequired;
    private final boolean mUnlockedDeviceRequired;
    private final boolean mIsStrongBoxBacked;
    private final int mMaxUsageCount;

    private KeyProtection(Builder builder)
    {
        mPurposes = builder.mPurposes;
        mKeyValidityStart = copy(builder.mKeyValidityStart);
        mKeyValidityForOriginationEnd = copy(builder.mKeyValidityForOriginationEnd);
        mKeyValidityForConsumptionEnd = copy(builder.mKeyValidityForConsumptionEnd);
        mEncryptionPaddings = builder.mEncryptionPaddings != null ? builder.mEncryptionPaddings.clone() : new String[0];
        mSignaturePaddings = builder.mSignaturePaddings != null ? builder.mSignaturePaddings.clone() : new String[0];
        mDigests = builder.mDigests != null ? builder.mDigests.clone() : null;
        mMgf1Digests = builder.mMgf1Digests != null ? Collections.unmodifiableSet(new LinkedHashSet<>(builder.mMgf1Digests))
                                                   : Collections.emptySet();
        mBlockModes = builder.mBlockModes != null ? builder.mBlockModes.clone() : new String[0];
        mRandomizedEncryptionRequired = builder.mRandomizedEncryptionRequired;
        mUserAuthenticationRequired = builder.mUserAuthenticationRequired;
        mUserAuthenticationType = builder.mUserAuthenticationType;
        mUserAuthenticationValidityDurationSeconds = builder.mUserAuthenticationValidityDurationSeconds;
        mUserPresenceRequired = builder.mUserPresenceRequired;
        mUserAuthenticationValidWhileOnBody = builder.mUserAuthenticationValidWhileOnBody;
        mInvalidatedByBiometricEnrollment = builder.mInvalidatedByBiometricEnrollment;
        mUserConfirmationRequired = builder.mUserConfirmationRequired;
        mUnlockedDeviceRequired = builder.mUnlockedDeviceRequired;
        mIsStrongBoxBacked = builder.mIsStrongBoxBacked;
        mMaxUsageCount = builder.mMaxUsageCount;
    }

    private static Date copy(Date date)
    {
        return date != null ? new Date(date.getTime()) : null;
    }

    public Date getKeyValidityStart()
    {
        return copy(mKeyValidityStart);
    }

    public Date getKeyValidityForConsumptionEnd()
    {
        return copy(mKeyValidityForConsumptionEnd);
    }

    public Date getKeyValidityForOriginationEnd()
    {
        return copy(mKeyValidityForOriginationEnd);
    }

    public int getPurposes()
    {
        return mPurposes;
    }

    public String[] getEncryptionPaddings()
    {
        return mEncryptionPaddings.clone();
    }

    public String[] getSignaturePaddings()
    {
        return mSignaturePaddings.clone();
    }

    public String[] getDigests()
    {
        if (mDigests == null) throw new IllegalStateException("Digests not specified");
        return mDigests.clone();
    }

    public boolean isDigestsSpecified()
    {
        return mDigests != null;
    }

    public Set<String> getMgf1Digests()
    {
        if (mMgf1Digests.isEmpty()) throw new IllegalStateException("Mgf1 digests not specified");
        return mMgf1Digests;
    }

    public boolean isMgf1DigestsSpecified()
    {
        return !mMgf1Digests.isEmpty();
    }

    public String[] getBlockModes()
    {
        return mBlockModes.clone();
    }

    public boolean isRandomizedEncryptionRequired()
    {
        return mRandomizedEncryptionRequired;
    }

    public boolean isUserAuthenticationRequired()
    {
        return mUserAuthenticationRequired;
    }

    public boolean isUserConfirmationRequired()
    {
        return mUserConfirmationRequired;
    }

    public int getUserAuthenticationType()
    {
        return mUserAuthenticationType;
    }

    public int getUserAuthenticationValidityDurationSeconds()
    {
        return mUserAuthenticationValidityDurationSeconds;
    }

    public boolean isUserPresenceRequired()
    {
        return mUserPresenceRequired;
    }

    public boolean isUserAuthenticationValidWhileOnBody()
    {
        return mUserAuthenticationValidWhileOnBody;
    }

    public boolean isInvalidatedByBiometricEnrollment()
    {
        return mInvalidatedByBiometricEnrollment;
    }

    public boolean isUnlockedDeviceRequired()
    {
        return mUnlockedDeviceRequired;
    }

    public int getMaxUsageCount()
    {
        return mMaxUsageCount;
    }

    boolean isStrongBoxBacked()
    {
        return mIsStrongBoxBacked;
    }

    public static final class Builder
    {
        private final int mPurposes;
        private Date mKeyValidityStart;
        private Date mKeyValidityForOriginationEnd;
        private Date mKeyValidityForConsumptionEnd;
        private String[] mEncryptionPaddings;
        private String[] mSignaturePaddings;
        private String[] mDigests;
        private Set<String> mMgf1Digests;
        private String[] mBlockModes;
        private boolean mRandomizedEncryptionRequired = true;
        private boolean mUserAuthenticationRequired;
        private int mUserAuthenticationType = KeyProperties.AUTH_BIOMETRIC_STRONG;
        private int mUserAuthenticationValidityDurationSeconds;
        private boolean mUserPresenceRequired;
        private boolean mUserAuthenticationValidWhileOnBody;
        private boolean mInvalidatedByBiometricEnrollment = true;
        private boolean mUserConfirmationRequired;
        private boolean mUnlockedDeviceRequired;
        private boolean mIsStrongBoxBacked;
        private int mMaxUsageCount = KeyProperties.UNRESTRICTED_USAGE_COUNT;

        public Builder(int purposes)
        {
            mPurposes = purposes;
        }

        public Builder setKeyValidityStart(Date startDate)
        {
            mKeyValidityStart = copy(startDate);
            return this;
        }

        public Builder setKeyValidityEnd(Date endDate)
        {
            setKeyValidityForOriginationEnd(endDate);
            setKeyValidityForConsumptionEnd(endDate);
            return this;
        }

        public Builder setKeyValidityForOriginationEnd(Date endDate)
        {
            mKeyValidityForOriginationEnd = copy(endDate);
            return this;
        }

        public Builder setKeyValidityForConsumptionEnd(Date endDate)
        {
            mKeyValidityForConsumptionEnd = copy(endDate);
            return this;
        }

        public Builder setEncryptionPaddings(String... paddings)
        {
            mEncryptionPaddings = paddings != null ? paddings.clone() : null;
            return this;
        }

        public Builder setSignaturePaddings(String... paddings)
        {
            mSignaturePaddings = paddings != null ? paddings.clone() : null;
            return this;
        }

        public Builder setDigests(String... digests)
        {
            mDigests = digests != null ? digests.clone() : null;
            return this;
        }

        public Builder setMgf1Digests(String... mgf1Digests)
        {
            mMgf1Digests = new LinkedHashSet<>();
            if (mgf1Digests != null) Collections.addAll(mMgf1Digests, mgf1Digests);
            return this;
        }

        public Builder setBlockModes(String... blockModes)
        {
            mBlockModes = blockModes != null ? blockModes.clone() : null;
            return this;
        }

        public Builder setRandomizedEncryptionRequired(boolean required)
        {
            mRandomizedEncryptionRequired = required;
            return this;
        }

        public Builder setUserAuthenticationRequired(boolean required)
        {
            mUserAuthenticationRequired = required;
            return this;
        }

        public Builder setUserConfirmationRequired(boolean required)
        {
            mUserConfirmationRequired = required;
            return this;
        }

        @Deprecated
        public Builder setUserAuthenticationValidityDurationSeconds(int seconds)
        {
            if (seconds < -1) throw new IllegalArgumentException("seconds must be -1 or larger");
            if (seconds == -1) return setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
            return setUserAuthenticationParameters(seconds, KeyProperties.AUTH_DEVICE_CREDENTIAL | KeyProperties.AUTH_BIOMETRIC_STRONG);
        }

        public Builder setUserAuthenticationParameters(int timeout, int type)
        {
            if (timeout < 0) throw new IllegalArgumentException("timeout must be 0 or larger");
            mUserAuthenticationValidityDurationSeconds = timeout;
            mUserAuthenticationType = type;
            return this;
        }

        public Builder setUserPresenceRequired(boolean required)
        {
            mUserPresenceRequired = required;
            return this;
        }

        public Builder setUserAuthenticationValidWhileOnBody(boolean remainsValid)
        {
            mUserAuthenticationValidWhileOnBody = remainsValid;
            return this;
        }

        public Builder setInvalidatedByBiometricEnrollment(boolean invalidateKey)
        {
            mInvalidatedByBiometricEnrollment = invalidateKey;
            return this;
        }

        public Builder setUnlockedDeviceRequired(boolean unlockedDeviceRequired)
        {
            mUnlockedDeviceRequired = unlockedDeviceRequired;
            return this;
        }

        public Builder setIsStrongBoxBacked(boolean isStrongBoxBacked)
        {
            mIsStrongBoxBacked = isStrongBoxBacked;
            return this;
        }

        public Builder setMaxUsageCount(int maxUsageCount)
        {
            if (maxUsageCount != KeyProperties.UNRESTRICTED_USAGE_COUNT && maxUsageCount <= 0)
            {
                throw new IllegalArgumentException("maxUsageCount is not valid");
            }
            mMaxUsageCount = maxUsageCount;
            return this;
        }

        public KeyProtection build()
        {
            return new KeyProtection(this);
        }
    }
}
