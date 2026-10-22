package android.security.keystore;

import android.security.KeyPairGeneratorSpec;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyPairGeneratorSpi;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

final class AndroidKeyStoreKeyPairGeneratorSpi extends KeyPairGeneratorSpi
{
    private final String mAlgorithm;
    private KeyGenParameterSpec mSpec;
    private String mAlias;
    private int mKeySize;
    private AlgorithmParameterSpec mAlgorithmSpec;
    private X500Principal mSubject;
    private BigInteger mSerialNumber;
    private Date mNotBefore;
    private Date mNotAfter;
    private SecureRandom mRandom;

    AndroidKeyStoreKeyPairGeneratorSpi(String algorithm)
    {
        mAlgorithm = algorithm;
    }

    private boolean isRsa()
    {
        return "RSA".equals(mAlgorithm);
    }

    @Override
    public void initialize(int keySize, SecureRandom random)
    {
        throw new IllegalArgumentException(KeyGenParameterSpec.class.getName() + " or " + KeyPairGeneratorSpec.class.getName()
                + " required to initialize this KeyPairGenerator");
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException
    {
        mRandom = random;

        if (params instanceof KeyGenParameterSpec)
        {
            KeyGenParameterSpec spec = (KeyGenParameterSpec)params;
            mSpec = spec;
            mAlias = spec.getKeystoreAlias();
            mKeySize = spec.getKeySize();
            mAlgorithmSpec = spec.getAlgorithmParameterSpec();
            mSubject = spec.getCertificateSubject();
            mSerialNumber = spec.getCertificateSerialNumber();
            mNotBefore = spec.getCertificateNotBefore();
            mNotAfter = spec.getCertificateNotAfter();
        }
        else if (params instanceof KeyPairGeneratorSpec)
        {
            KeyPairGeneratorSpec spec = (KeyPairGeneratorSpec)params;
            if (spec.getKeyType() != null && !mAlgorithm.equalsIgnoreCase(spec.getKeyType()))
            {
                throw new InvalidAlgorithmParameterException("Key type " + spec.getKeyType() + " does not match " + mAlgorithm);
            }
            mSpec = new KeyGenParameterSpec.Builder(spec.getKeystoreAlias(), KeyProperties.PURPOSE_ENCRYPT
                    | KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_MD5, KeyProperties.DIGEST_SHA1,
                            KeyProperties.DIGEST_SHA224, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384,
                            KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE, KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1,
                            KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                    .setRandomizedEncryptionRequired(false)
                    .build();
            mAlias = spec.getKeystoreAlias();
            mKeySize = spec.getKeySize();
            mAlgorithmSpec = spec.getAlgorithmParameterSpec();
            mSubject = spec.getSubjectDN();
            mSerialNumber = spec.getSerialNumber();
            mNotBefore = spec.getStartDate();
            mNotAfter = spec.getEndDate();
        }
        else
        {
            throw new InvalidAlgorithmParameterException("Unsupported params class: "
                    + (params != null ? params.getClass().getName() : "null") + ". Supported: " + KeyGenParameterSpec.class.getName()
                    + ", " + KeyPairGeneratorSpec.class.getName());
        }

        if (mKeySize == -1)
        {
            if (mAlgorithmSpec instanceof RSAKeyGenParameterSpec) mKeySize = ((RSAKeyGenParameterSpec)mAlgorithmSpec).getKeysize();
            else mKeySize = isRsa() ? 2048 : 256;
        }

        if (isRsa() && (mKeySize < 512 || mKeySize > 8192))
        {
            throw new InvalidAlgorithmParameterException("RSA key size must be >= 512 and <= 8192");
        }
        if (!isRsa() && !(mAlgorithmSpec instanceof ECGenParameterSpec) && mKeySize != 224 && mKeySize != 256 && mKeySize != 384
                && mKeySize != 521)
        {
            throw new InvalidAlgorithmParameterException("Unsupported EC key size: " + mKeySize
                    + " bits. Supported: 224, 256, 384, 521");
        }
    }

    @Override
    public KeyPair generateKeyPair()
    {
        if (mSpec == null) throw new IllegalStateException("Not initialized");
        if (mSpec.isStrongBoxBacked()) throw new StrongBoxUnavailableException("No StrongBox available");

        try
        {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(mAlgorithm);
            if (mAlgorithmSpec instanceof ECGenParameterSpec || mAlgorithmSpec instanceof RSAKeyGenParameterSpec)
            {
                if (mRandom != null) generator.initialize(mAlgorithmSpec, mRandom);
                else generator.initialize(mAlgorithmSpec);
            }
            else if (mRandom != null)
            {
                generator.initialize(mKeySize, mRandom);
            }
            else
            {
                generator.initialize(mKeySize);
            }

            KeyPair keyPair = generator.generateKeyPair();
            X509Certificate certificate = SelfSignedCertificate.create(keyPair, mSubject, mSerialNumber, mNotBefore, mNotAfter);

            KeyStoreRecord record = new KeyStoreRecord(mAlias, KeyStoreRecord.TYPE_PRIVATE_KEY);
            record.mAlgorithm = mAlgorithm;
            record.mKeyEncoded = keyPair.getPrivate().getEncoded();
            record.mCertificateChain = new byte[][] {certificate.getEncoded()};
            record.applyGenerationSpec(mSpec, KeyStoreRecord.keySizeOf(keyPair.getPrivate(), null));
            AndroidKeyStoreBackend.put(record);

            return new KeyPair(certificate.getPublicKey(), keyPair.getPrivate());
        }
        catch (GeneralSecurityException exception)
        {
            throw new ProviderException("Failed to generate key pair", exception);
        }
    }
}
