package android.security.keystore;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.Date;

final class KeyStoreRecord
{
    static final int TYPE_SECRET_KEY = 1;
    static final int TYPE_PRIVATE_KEY = 2;
    static final int TYPE_CERTIFICATE = 3;

    static final long NO_DATE = Long.MIN_VALUE;

    private static final String[] NO_STRINGS = new String[0];

    String mAlias;
    int mType;
    String mAlgorithm;
    byte[] mKeyEncoded;
    byte[][] mCertificateChain = new byte[0][];
    long mCreationDate = System.currentTimeMillis();
    int mOrigin = KeyProperties.ORIGIN_UNKNOWN;
    int mKeySize = -1;
    long mValidityStart = NO_DATE;
    long mValidityForOriginationEnd = NO_DATE;
    long mValidityForConsumptionEnd = NO_DATE;
    int mPurposes;
    String[] mBlockModes = NO_STRINGS;
    String[] mEncryptionPaddings = NO_STRINGS;
    String[] mSignaturePaddings = NO_STRINGS;
    String[] mDigests = NO_STRINGS;
    boolean mUserAuthenticationRequired;
    boolean mUserConfirmationRequired;
    int mUserAuthenticationValidityDurationSeconds;
    int mUserAuthenticationType = KeyProperties.AUTH_BIOMETRIC_STRONG;
    boolean mUserAuthenticationValidWhileOnBody;
    boolean mInvalidatedByBiometricEnrollment;
    boolean mUserPresenceRequired;
    int mMaxUsageCount = KeyProperties.UNRESTRICTED_USAGE_COUNT;

    KeyStoreRecord(String alias, int type)
    {
        mAlias = alias;
        mType = type;
    }

    private static long time(Date date)
    {
        return date != null ? date.getTime() : NO_DATE;
    }

    private static String[] strings(String[] values)
    {
        return values != null ? values.clone() : NO_STRINGS;
    }

    static int keySizeOf(Key key, byte[] encoded)
    {
        if (key instanceof RSAKey) return ((RSAKey)key).getModulus().bitLength();
        if (key instanceof ECKey) return ((ECKey)key).getParams().getCurve().getField().getFieldSize();
        return encoded != null ? encoded.length * 8 : -1;
    }

    void applyGenerationSpec(KeyGenParameterSpec spec, int keySize)
    {
        mOrigin = KeyProperties.ORIGIN_GENERATED;
        mKeySize = keySize;
        mValidityStart = time(spec.getKeyValidityStart());
        mValidityForOriginationEnd = time(spec.getKeyValidityForOriginationEnd());
        mValidityForConsumptionEnd = time(spec.getKeyValidityForConsumptionEnd());
        mPurposes = spec.getPurposes();
        mBlockModes = strings(spec.getBlockModes());
        mEncryptionPaddings = strings(spec.getEncryptionPaddings());
        mSignaturePaddings = strings(spec.getSignaturePaddings());
        mDigests = spec.isDigestsSpecified() ? strings(spec.getDigests()) : NO_STRINGS;
        mUserAuthenticationRequired = spec.isUserAuthenticationRequired();
        mUserConfirmationRequired = spec.isUserConfirmationRequired();
        mUserAuthenticationValidityDurationSeconds = spec.getUserAuthenticationValidityDurationSeconds();
        mUserAuthenticationType = spec.getUserAuthenticationType();
        mUserAuthenticationValidWhileOnBody = spec.isUserAuthenticationValidWhileOnBody();
        mInvalidatedByBiometricEnrollment = spec.isUserAuthenticationRequired() && spec.isInvalidatedByBiometricEnrollment();
        mUserPresenceRequired = spec.isUserPresenceRequired();
        mMaxUsageCount = spec.getMaxUsageCount();
    }

    void applyProtection(KeyProtection protection, int keySize)
    {
        mOrigin = KeyProperties.ORIGIN_IMPORTED;
        mKeySize = keySize;
        if (protection == null)
        {
            mPurposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN
                    | KeyProperties.PURPOSE_VERIFY;
            mDigests = new String[] {KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_MD5, KeyProperties.DIGEST_SHA1,
                    KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384,
                    KeyProperties.DIGEST_SHA512};
            mEncryptionPaddings = new String[] {KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1,
                    KeyProperties.ENCRYPTION_PADDING_RSA_OAEP};
            mSignaturePaddings = new String[] {KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS};
            return;
        }

        mValidityStart = time(protection.getKeyValidityStart());
        mValidityForOriginationEnd = time(protection.getKeyValidityForOriginationEnd());
        mValidityForConsumptionEnd = time(protection.getKeyValidityForConsumptionEnd());
        mPurposes = protection.getPurposes();
        mBlockModes = strings(protection.getBlockModes());
        mEncryptionPaddings = strings(protection.getEncryptionPaddings());
        mSignaturePaddings = strings(protection.getSignaturePaddings());
        mDigests = protection.isDigestsSpecified() ? strings(protection.getDigests()) : NO_STRINGS;
        mUserAuthenticationRequired = protection.isUserAuthenticationRequired();
        mUserConfirmationRequired = protection.isUserConfirmationRequired();
        mUserAuthenticationValidityDurationSeconds = protection.getUserAuthenticationValidityDurationSeconds();
        mUserAuthenticationType = protection.getUserAuthenticationType();
        mUserAuthenticationValidWhileOnBody = protection.isUserAuthenticationValidWhileOnBody();
        mInvalidatedByBiometricEnrollment =
                protection.isUserAuthenticationRequired() && protection.isInvalidatedByBiometricEnrollment();
        mUserPresenceRequired = protection.isUserPresenceRequired();
        mMaxUsageCount = protection.getMaxUsageCount();
    }

    void write(DataOutputStream output) throws IOException
    {
        output.writeUTF(mAlias);
        output.writeInt(mType);
        output.writeUTF(mAlgorithm != null ? mAlgorithm : "");
        writeBytes(output, mKeyEncoded);
        output.writeInt(mCertificateChain.length);
        for (byte[] certificate : mCertificateChain) writeBytes(output, certificate);
        output.writeLong(mCreationDate);
        output.writeInt(mOrigin);
        output.writeInt(mKeySize);
        output.writeLong(mValidityStart);
        output.writeLong(mValidityForOriginationEnd);
        output.writeLong(mValidityForConsumptionEnd);
        output.writeInt(mPurposes);
        writeStrings(output, mBlockModes);
        writeStrings(output, mEncryptionPaddings);
        writeStrings(output, mSignaturePaddings);
        writeStrings(output, mDigests);
        output.writeBoolean(mUserAuthenticationRequired);
        output.writeBoolean(mUserConfirmationRequired);
        output.writeInt(mUserAuthenticationValidityDurationSeconds);
        output.writeInt(mUserAuthenticationType);
        output.writeBoolean(mUserAuthenticationValidWhileOnBody);
        output.writeBoolean(mInvalidatedByBiometricEnrollment);
        output.writeBoolean(mUserPresenceRequired);
        output.writeInt(mMaxUsageCount);
    }

    static KeyStoreRecord read(DataInputStream input) throws IOException
    {
        KeyStoreRecord record = new KeyStoreRecord(input.readUTF(), input.readInt());
        String algorithm = input.readUTF();
        record.mAlgorithm = algorithm.isEmpty() ? null : algorithm;
        record.mKeyEncoded = readBytes(input);
        int chainLength = input.readInt();
        if (chainLength < 0 || chainLength > 64) throw new IOException("corrupt certificate chain");
        record.mCertificateChain = new byte[chainLength][];
        for (int index = 0; index < chainLength; index++) record.mCertificateChain[index] = readBytes(input);
        record.mCreationDate = input.readLong();
        record.mOrigin = input.readInt();
        record.mKeySize = input.readInt();
        record.mValidityStart = input.readLong();
        record.mValidityForOriginationEnd = input.readLong();
        record.mValidityForConsumptionEnd = input.readLong();
        record.mPurposes = input.readInt();
        record.mBlockModes = readStrings(input);
        record.mEncryptionPaddings = readStrings(input);
        record.mSignaturePaddings = readStrings(input);
        record.mDigests = readStrings(input);
        record.mUserAuthenticationRequired = input.readBoolean();
        record.mUserConfirmationRequired = input.readBoolean();
        record.mUserAuthenticationValidityDurationSeconds = input.readInt();
        record.mUserAuthenticationType = input.readInt();
        record.mUserAuthenticationValidWhileOnBody = input.readBoolean();
        record.mInvalidatedByBiometricEnrollment = input.readBoolean();
        record.mUserPresenceRequired = input.readBoolean();
        record.mMaxUsageCount = input.readInt();
        return record;
    }

    private static void writeBytes(DataOutputStream output, byte[] value) throws IOException
    {
        if (value == null)
        {
            output.writeInt(-1);
            return;
        }
        output.writeInt(value.length);
        output.write(value);
    }

    private static byte[] readBytes(DataInputStream input) throws IOException
    {
        int length = input.readInt();
        if (length < 0) return null;
        if (length > (1 << 24)) throw new IOException("corrupt entry");
        byte[] value = new byte[length];
        input.readFully(value);
        return value;
    }

    private static void writeStrings(DataOutputStream output, String[] values) throws IOException
    {
        output.writeInt(values.length);
        for (String value : values) output.writeUTF(value != null ? value : "");
    }

    private static String[] readStrings(DataInputStream input) throws IOException
    {
        int count = input.readInt();
        if (count < 0 || count > 256) throw new IOException("corrupt entry");
        String[] values = new String[count];
        for (int index = 0; index < count; index++) values[index] = input.readUTF();
        return values;
    }
}
