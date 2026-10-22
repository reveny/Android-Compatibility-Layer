package android.security.keystore;

import java.security.spec.KeySpec;
import java.util.Date;

public class KeyInfo implements KeySpec
{
    private final KeyStoreRecord mRecord;

    KeyInfo(KeyStoreRecord record)
    {
        mRecord = record;
    }

    private static Date toDate(long time)
    {
        return time != KeyStoreRecord.NO_DATE ? new Date(time) : null;
    }

    public String getKeystoreAlias()
    {
        return mRecord.mAlias;
    }

    @Deprecated
    public boolean isInsideSecureHardware()
    {
        return false;
    }

    public int getSecurityLevel()
    {
        return KeyProperties.SECURITY_LEVEL_SOFTWARE;
    }

    public int getOrigin()
    {
        return mRecord.mOrigin;
    }

    public int getKeySize()
    {
        return mRecord.mKeySize;
    }

    public Date getKeyValidityStart()
    {
        return toDate(mRecord.mValidityStart);
    }

    public Date getKeyValidityForConsumptionEnd()
    {
        return toDate(mRecord.mValidityForConsumptionEnd);
    }

    public Date getKeyValidityForOriginationEnd()
    {
        return toDate(mRecord.mValidityForOriginationEnd);
    }

    public int getPurposes()
    {
        return mRecord.mPurposes;
    }

    public String[] getBlockModes()
    {
        return mRecord.mBlockModes.clone();
    }

    public String[] getEncryptionPaddings()
    {
        return mRecord.mEncryptionPaddings.clone();
    }

    public String[] getSignaturePaddings()
    {
        return mRecord.mSignaturePaddings.clone();
    }

    public String[] getDigests()
    {
        return mRecord.mDigests.clone();
    }

    public boolean isUserAuthenticationRequired()
    {
        return mRecord.mUserAuthenticationRequired;
    }

    public boolean isUserConfirmationRequired()
    {
        return mRecord.mUserConfirmationRequired;
    }

    public int getUserAuthenticationValidityDurationSeconds()
    {
        return mRecord.mUserAuthenticationValidityDurationSeconds;
    }

    public int getUserAuthenticationType()
    {
        return mRecord.mUserAuthenticationType;
    }

    public boolean isUserAuthenticationRequirementEnforcedBySecureHardware()
    {
        return false;
    }

    public boolean isUserAuthenticationValidWhileOnBody()
    {
        return mRecord.mUserAuthenticationValidWhileOnBody;
    }

    public boolean isInvalidatedByBiometricEnrollment()
    {
        return mRecord.mInvalidatedByBiometricEnrollment;
    }

    public boolean isTrustedUserPresenceRequired()
    {
        return mRecord.mUserPresenceRequired;
    }

    public int getRemainingUsageCount()
    {
        return mRecord.mMaxUsageCount;
    }
}
