package android.security.keystore;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyFactorySpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

final class AndroidKeyStoreKeyFactorySpi extends KeyFactorySpi
{
    private final String mAlgorithm;

    AndroidKeyStoreKeyFactorySpi(String algorithm)
    {
        mAlgorithm = algorithm;
    }

    private KeyFactory hostFactory() throws InvalidKeySpecException
    {
        try
        {
            return KeyFactory.getInstance(mAlgorithm);
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw new InvalidKeySpecException(exception);
        }
    }

    @Override
    protected PublicKey engineGeneratePublic(KeySpec keySpec) throws InvalidKeySpecException
    {
        return hostFactory().generatePublic(keySpec);
    }

    @Override
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec) throws InvalidKeySpecException
    {
        throw new InvalidKeySpecException("To generate a key pair in Android Keystore, use KeyPairGenerator initialized with "
                + KeyGenParameterSpec.class.getName());
    }

    @Override
    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec) throws InvalidKeySpecException
    {
        if (key == null) throw new InvalidKeySpecException("key == null");
        if (keySpec == null) throw new InvalidKeySpecException("keySpecClass == null");

        if (KeyInfo.class.equals(keySpec))
        {
            if (!(key instanceof PrivateKey)) throw new InvalidKeySpecException("Unsupported key type: " + key.getClass().getName());
            KeyStoreRecord record = AndroidKeyStoreBackend.findByKey(key);
            if (record == null || record.mType != KeyStoreRecord.TYPE_PRIVATE_KEY)
            {
                throw new InvalidKeySpecException("Only Android KeyStore private keys are supported");
            }
            return keySpec.cast(new KeyInfo(record));
        }

        if (key instanceof PrivateKey && AndroidKeyStoreBackend.findByKey(key) != null)
        {
            throw new InvalidKeySpecException("Key material export of Android Keystore private keys is not supported");
        }
        return hostFactory().getKeySpec(key, keySpec);
    }

    @Override
    protected Key engineTranslateKey(Key key) throws InvalidKeyException
    {
        if (key == null) throw new InvalidKeyException("key == null");
        if (key instanceof PublicKey || AndroidKeyStoreBackend.findByKey(key) != null) return key;
        throw new InvalidKeyException("To import a key into Android Keystore, use KeyStore.setEntry");
    }
}
