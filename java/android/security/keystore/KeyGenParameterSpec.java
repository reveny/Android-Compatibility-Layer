package android.security.keystore;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

public final class KeyGenParameterSpec implements AlgorithmParameterSpec
{
    private static final X500Principal DEFAULT_CERTIFICATE_SUBJECT = new X500Principal("CN=fake");
    private static final BigInteger DEFAULT_CERTIFICATE_SERIAL_NUMBER = BigInteger.ONE;
    private static final Date DEFAULT_CERTIFICATE_NOT_BEFORE = new Date(0L);
    private static final Date DEFAULT_CERTIFICATE_NOT_AFTER = new Date(2461449600000L);

    private final String mKeystoreAlias;
    private final int mPurposes;
    private final int mKeySize;
    private final AlgorithmParameterSpec mSpec;
    private final X500Principal mCertificateSubject;
    private final BigInteger mCertificateSerialNumber;
    private final Date mCertificateNotBefore;
    private final Date mCertificateNotAfter;
    private final Date mKeyValidityStart;
    private final Date mKeyValidityForOriginationEnd;
    private final Date mKeyValidityForConsumptionEnd;
    private final String[] mDigests;
    private final Set<String> mMgf1Digests;
    private final String[] mEncryptionPaddings;
    private final String[] mSignaturePaddings;
    private final String[] mBlockModes;
    private final boolean mRandomizedEncryptionRequired;
    private final boolean mUserAuthenticationRequired;
    private final int mUserAuthenticationValidityDurationSeconds;
    private final int mUserAuthenticationType;
    private final boolean mUserPresenceRequired;
    private final byte[] mAttestationChallenge;
    private final boolean mDevicePropertiesAttestationIncluded;
    private final boolean mUserAuthenticationValidWhileOnBody;
    private final boolean mInvalidatedByBiometricEnrollment;
    private final boolean mIsStrongBoxBacked;
    private final boolean mUserConfirmationRequired;
    private final boolean mUnlockedDeviceRequired;
    private final int mMaxUsageCount;
    private final String mAttestKeyAlias;

    private KeyGenParameterSpec(Builder builder)
    {
        mKeystoreAlias = builder.mKeystoreAlias;
        mPurposes = builder.mPurposes;
        mKeySize = builder.mKeySize;
        mSpec = builder.mSpec;
        mCertificateSubject = builder.mCertificateSubject != null ? builder.mCertificateSubject : DEFAULT_CERTIFICATE_SUBJECT;
        mCertificateSerialNumber =
                builder.mCertificateSerialNumber != null ? builder.mCertificateSerialNumber : DEFAULT_CERTIFICATE_SERIAL_NUMBER;
        mCertificateNotBefore = copy(builder.mCertificateNotBefore != null ? builder.mCertificateNotBefore
                                                                           : DEFAULT_CERTIFICATE_NOT_BEFORE);
        mCertificateNotAfter = copy(builder.mCertificateNotAfter != null ? builder.mCertificateNotAfter
                                                                         : DEFAULT_CERTIFICATE_NOT_AFTER);
        if (mCertificateNotAfter.getTime() < mCertificateNotBefore.getTime())
        {
            throw new IllegalArgumentException("certificateNotAfter < certificateNotBefore");
        }
        mKeyValidityStart = copy(builder.mKeyValidityStart);
        mKeyValidityForOriginationEnd = copy(builder.mKeyValidityForOriginationEnd);
        mKeyValidityForConsumptionEnd = copy(builder.mKeyValidityForConsumptionEnd);
        mDigests = builder.mDigests != null ? builder.mDigests.clone() : null;
        mMgf1Digests = builder.mMgf1Digests != null ? Collections.unmodifiableSet(new LinkedHashSet<>(builder.mMgf1Digests))
                                                   : Collections.emptySet();
        mEncryptionPaddings = clone(builder.mEncryptionPaddings);
        mSignaturePaddings = clone(builder.mSignaturePaddings);
        mBlockModes = clone(builder.mBlockModes);
        mRandomizedEncryptionRequired = builder.mRandomizedEncryptionRequired;
        mUserAuthenticationRequired = builder.mUserAuthenticationRequired;
        mUserAuthenticationValidityDurationSeconds = builder.mUserAuthenticationValidityDurationSeconds;
        mUserAuthenticationType = builder.mUserAuthenticationType;
        mUserPresenceRequired = builder.mUserPresenceRequired;
        mAttestationChallenge = builder.mAttestationChallenge != null ? builder.mAttestationChallenge.clone() : null;
        mDevicePropertiesAttestationIncluded = builder.mDevicePropertiesAttestationIncluded;
        mUserAuthenticationValidWhileOnBody = builder.mUserAuthenticationValidWhileOnBody;
        mInvalidatedByBiometricEnrollment = builder.mInvalidatedByBiometricEnrollment;
        mIsStrongBoxBacked = builder.mIsStrongBoxBacked;
        mUserConfirmationRequired = builder.mUserConfirmationRequired;
        mUnlockedDeviceRequired = builder.mUnlockedDeviceRequired;
        mMaxUsageCount = builder.mMaxUsageCount;
        mAttestKeyAlias = builder.mAttestKeyAlias;
    }

    private static Date copy(Date date)
    {
        return date != null ? new Date(date.getTime()) : null;
    }

    private static String[] clone(String[] values)
    {
        return values != null ? values.clone() : new String[0];
    }

    public String getKeystoreAlias()
    {
        return mKeystoreAlias;
    }

    public int getKeySize()
    {
        return mKeySize;
    }

    public AlgorithmParameterSpec getAlgorithmParameterSpec()
    {
        return mSpec;
    }

    public X500Principal getCertificateSubject()
    {
        return mCertificateSubject;
    }

    public BigInteger getCertificateSerialNumber()
    {
        return mCertificateSerialNumber;
    }

    public Date getCertificateNotBefore()
    {
        return copy(mCertificateNotBefore);
    }

    public Date getCertificateNotAfter()
    {
        return copy(mCertificateNotAfter);
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

    public String[] getEncryptionPaddings()
    {
        return mEncryptionPaddings.clone();
    }

    public String[] getSignaturePaddings()
    {
        return mSignaturePaddings.clone();
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

    public int getUserAuthenticationValidityDurationSeconds()
    {
        return mUserAuthenticationValidityDurationSeconds;
    }

    public int getUserAuthenticationType()
    {
        return mUserAuthenticationType;
    }

    public boolean isUserPresenceRequired()
    {
        return mUserPresenceRequired;
    }

    public byte[] getAttestationChallenge()
    {
        return mAttestationChallenge != null ? mAttestationChallenge.clone() : null;
    }

    public boolean isDevicePropertiesAttestationIncluded()
    {
        return mDevicePropertiesAttestationIncluded;
    }

    public String getAttestKeyAlias()
    {
        return mAttestKeyAlias;
    }

    public boolean isUserAuthenticationValidWhileOnBody()
    {
        return mUserAuthenticationValidWhileOnBody;
    }

    public boolean isInvalidatedByBiometricEnrollment()
    {
        return mInvalidatedByBiometricEnrollment;
    }

    public boolean isStrongBoxBacked()
    {
        return mIsStrongBoxBacked;
    }

    public boolean isUnlockedDeviceRequired()
    {
        return mUnlockedDeviceRequired;
    }

    public int getMaxUsageCount()
    {
        return mMaxUsageCount;
    }

    public static final class Builder
    {
        private final String mKeystoreAlias;
        private int mPurposes;
        private int mKeySize = -1;
        private AlgorithmParameterSpec mSpec;
        private X500Principal mCertificateSubject;
        private BigInteger mCertificateSerialNumber;
        private Date mCertificateNotBefore;
        private Date mCertificateNotAfter;
        private Date mKeyValidityStart;
        private Date mKeyValidityForOriginationEnd;
        private Date mKeyValidityForConsumptionEnd;
        private String[] mDigests;
        private Set<String> mMgf1Digests;
        private String[] mEncryptionPaddings;
        private String[] mSignaturePaddings;
        private String[] mBlockModes;
        private boolean mRandomizedEncryptionRequired = true;
        private boolean mUserAuthenticationRequired;
        private int mUserAuthenticationValidityDurationSeconds;
        private int mUserAuthenticationType = KeyProperties.AUTH_BIOMETRIC_STRONG;
        private boolean mUserPresenceRequired;
        private byte[] mAttestationChallenge;
        private boolean mDevicePropertiesAttestationIncluded;
        private boolean mUserAuthenticationValidWhileOnBody;
        private boolean mInvalidatedByBiometricEnrollment = true;
        private boolean mIsStrongBoxBacked;
        private boolean mUserConfirmationRequired;
        private boolean mUnlockedDeviceRequired;
        private int mMaxUsageCount = KeyProperties.UNRESTRICTED_USAGE_COUNT;
        private String mAttestKeyAlias;

        public Builder(String keystoreAlias, int purposes)
        {
            if (keystoreAlias == null) throw new NullPointerException("keystoreAlias == null");
            if (keystoreAlias.isEmpty()) throw new IllegalArgumentException("keystoreAlias must not be empty");
            mKeystoreAlias = keystoreAlias;
            mPurposes = purposes;
        }

        public Builder setKeySize(int keySize)
        {
            if (keySize < 0) throw new IllegalArgumentException("keySize < 0");
            mKeySize = keySize;
            return this;
        }

        public Builder setAlgorithmParameterSpec(AlgorithmParameterSpec spec)
        {
            if (spec == null) throw new NullPointerException("spec == null");
            mSpec = spec;
            return this;
        }

        public Builder setCertificateSubject(X500Principal subject)
        {
            if (subject == null) throw new NullPointerException("subject == null");
            mCertificateSubject = subject;
            return this;
        }

        public Builder setCertificateSerialNumber(BigInteger serialNumber)
        {
            if (serialNumber == null) throw new NullPointerException("serialNumber == null");
            mCertificateSerialNumber = serialNumber;
            return this;
        }

        public Builder setCertificateNotBefore(Date date)
        {
            if (date == null) throw new NullPointerException("date == null");
            mCertificateNotBefore = copy(date);
            return this;
        }

        public Builder setCertificateNotAfter(Date date)
        {
            if (date == null) throw new NullPointerException("date == null");
            mCertificateNotAfter = copy(date);
            return this;
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

        public Builder setAttestationChallenge(byte[] attestationChallenge)
        {
            mAttestationChallenge = attestationChallenge;
            return this;
        }

        public Builder setDevicePropertiesAttestationIncluded(boolean devicePropertiesAttestationIncluded)
        {
            mDevicePropertiesAttestationIncluded = devicePropertiesAttestationIncluded;
            return this;
        }

        public Builder setAttestKeyAlias(String attestKeyAlias)
        {
            mAttestKeyAlias = attestKeyAlias;
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

        public Builder setIsStrongBoxBacked(boolean isStrongBoxBacked)
        {
            mIsStrongBoxBacked = isStrongBoxBacked;
            return this;
        }

        public Builder setUnlockedDeviceRequired(boolean unlockedDeviceRequired)
        {
            mUnlockedDeviceRequired = unlockedDeviceRequired;
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

        public KeyGenParameterSpec build()
        {
            return new KeyGenParameterSpec(this);
        }
    }
}
