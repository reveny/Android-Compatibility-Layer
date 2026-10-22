package android.security.keystore;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyGenerator;
import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

final class AndroidKeyStoreKeyGeneratorSpi extends KeyGeneratorSpi
{
    private final String mAlgorithm;
    private final int mDefaultKeySize;
    private KeyGenParameterSpec mSpec;
    private int mKeySize;
    private SecureRandom mRandom;

    AndroidKeyStoreKeyGeneratorSpi(String algorithm, int defaultKeySize)
    {
        mAlgorithm = algorithm;
        mDefaultKeySize = defaultKeySize;
    }

    @Override
    protected void engineInit(SecureRandom random)
    {
        throw new UnsupportedOperationException("Cannot initialize without a " + KeyGenParameterSpec.class.getName()
                + " parameter");
    }

    @Override
    protected void engineInit(int keySize, SecureRandom random)
    {
        throw new UnsupportedOperationException("Cannot initialize without a " + KeyGenParameterSpec.class.getName()
                + " parameter");
    }

    @Override
    protected void engineInit(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException
    {
        if (!(params instanceof KeyGenParameterSpec))
        {
            throw new InvalidAlgorithmParameterException("Cannot initialize without a " + KeyGenParameterSpec.class.getName()
                    + " parameter");
        }

        KeyGenParameterSpec spec = (KeyGenParameterSpec)params;
        if (spec.getKeystoreAlias() == null) throw new InvalidAlgorithmParameterException("KeyStore entry alias not provided");

        int keySize = spec.getKeySize() == -1 ? mDefaultKeySize : spec.getKeySize();
        if (keySize <= 0 || keySize % 8 != 0) throw new InvalidAlgorithmParameterException("Key size must be a multiple of 8: " + keySize);
        if ("AES".equals(mAlgorithm) && keySize != 128 && keySize != 192 && keySize != 256)
        {
            throw new InvalidAlgorithmParameterException("Unsupported key size: " + keySize + ". Supported: 128, 192, 256.");
        }
        if ("DESede".equals(mAlgorithm) && keySize != 168)
        {
            throw new InvalidAlgorithmParameterException("3DES key size must be 168 bits.");
        }
        if (mAlgorithm.startsWith("Hmac") && (keySize < 64 || keySize > 512))
        {
            throw new InvalidAlgorithmParameterException("HMAC key sizes must be within 64-512 bits, inclusive.");
        }

        mSpec = spec;
        mKeySize = keySize;
        mRandom = random;
    }

    @Override
    protected SecretKey engineGenerateKey()
    {
        if (mSpec == null) throw new IllegalStateException("Not initialized");
        if (mSpec.isStrongBoxBacked()) throw new StrongBoxUnavailableException("No StrongBox available");

        byte[] material;
        if (mAlgorithm.startsWith("Hmac"))
        {
            material = new byte[mKeySize / 8];
            (mRandom != null ? mRandom : new SecureRandom()).nextBytes(material);
        }
        else
        {
            try
            {
                KeyGenerator generator = KeyGenerator.getInstance(mAlgorithm);
                if (mRandom != null) generator.init(mKeySize, mRandom);
                else generator.init(mKeySize);
                material = generator.generateKey().getEncoded();
            }
            catch (GeneralSecurityException exception)
            {
                throw new ProviderException("Failed to generate key", exception);
            }
        }

        KeyStoreRecord record = new KeyStoreRecord(mSpec.getKeystoreAlias(), KeyStoreRecord.TYPE_SECRET_KEY);
        record.mAlgorithm = mAlgorithm;
        record.mKeyEncoded = material;
        record.applyGenerationSpec(mSpec, mKeySize);
        AndroidKeyStoreBackend.put(record);

        return new SecretKeySpec(material, mAlgorithm);
    }
}
