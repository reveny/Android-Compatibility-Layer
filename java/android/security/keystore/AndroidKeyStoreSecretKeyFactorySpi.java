package android.security.keystore;

import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactorySpi;

final class AndroidKeyStoreSecretKeyFactorySpi extends SecretKeyFactorySpi
{
    @Override
    protected KeySpec engineGetKeySpec(SecretKey key, Class keySpecClass) throws InvalidKeySpecException
    {
        if (keySpecClass == null) throw new InvalidKeySpecException("keySpecClass == null");
        if (!KeyInfo.class.equals(keySpecClass))
        {
            throw new InvalidKeySpecException("Unsupported key spec: " + keySpecClass.getName());
        }

        KeyStoreRecord record = AndroidKeyStoreBackend.findByKey(key);
        if (record == null || record.mType != KeyStoreRecord.TYPE_SECRET_KEY)
        {
            throw new InvalidKeySpecException("Only Android KeyStore secret keys are supported");
        }
        return new KeyInfo(record);
    }

    @Override
    protected SecretKey engineGenerateSecret(KeySpec keySpec) throws InvalidKeySpecException
    {
        throw new InvalidKeySpecException("To generate secret key in Android Keystore, use KeyGenerator initialized with "
                + KeyGenParameterSpec.class.getName());
    }

    @Override
    protected SecretKey engineTranslateKey(SecretKey key) throws InvalidKeyException
    {
        if (key == null) throw new InvalidKeyException("key == null");
        if (AndroidKeyStoreBackend.findByKey(key) == null)
        {
            throw new InvalidKeyException("To import a secret key into Android Keystore, use KeyStore.setEntry");
        }
        return key;
    }
}
