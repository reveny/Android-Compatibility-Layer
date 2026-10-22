package android.security.keystore;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.function.Supplier;

public final class AndroidKeyStoreProvider extends Provider
{
    public static final String PROVIDER_NAME = "AndroidKeyStore";

    private static final String KEYSTORE_FILE = "keystore/AndroidKeyStore.bin";

    private AndroidKeyStoreProvider()
    {
        super(PROVIDER_NAME, 1.0, "Android KeyStore security provider");

        register("KeyStore", PROVIDER_NAME, AndroidKeyStoreSpi::new);

        register("KeyPairGenerator", KeyProperties.KEY_ALGORITHM_RSA,
                () -> new AndroidKeyStoreKeyPairGeneratorSpi(KeyProperties.KEY_ALGORITHM_RSA));
        register("KeyPairGenerator", KeyProperties.KEY_ALGORITHM_EC,
                () -> new AndroidKeyStoreKeyPairGeneratorSpi(KeyProperties.KEY_ALGORITHM_EC));

        register("KeyFactory", KeyProperties.KEY_ALGORITHM_RSA,
                () -> new AndroidKeyStoreKeyFactorySpi(KeyProperties.KEY_ALGORITHM_RSA));
        register("KeyFactory", KeyProperties.KEY_ALGORITHM_EC,
                () -> new AndroidKeyStoreKeyFactorySpi(KeyProperties.KEY_ALGORITHM_EC));

        registerKeyGenerator(KeyProperties.KEY_ALGORITHM_AES, 128);
        registerKeyGenerator(KeyProperties.KEY_ALGORITHM_3DES, 168);
        registerKeyGenerator(KeyProperties.KEY_ALGORITHM_HMAC_SHA1, 160);
        registerKeyGenerator(KeyProperties.KEY_ALGORITHM_HMAC_SHA224, 224);
        registerKeyGenerator(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, 256);
        registerKeyGenerator(KeyProperties.KEY_ALGORITHM_HMAC_SHA384, 384);
        registerKeyGenerator(KeyProperties.KEY_ALGORITHM_HMAC_SHA512, 512);
    }

    public static synchronized void install(File dataDirectory)
    {
        AndroidKeyStoreBackend.setFile(dataDirectory != null ? new File(dataDirectory, KEYSTORE_FILE) : null);
        if (Security.getProvider(PROVIDER_NAME) == null) Security.addProvider(new AndroidKeyStoreProvider());
    }

    private void registerKeyGenerator(String algorithm, int defaultKeySize)
    {
        register("KeyGenerator", algorithm, () -> new AndroidKeyStoreKeyGeneratorSpi(algorithm, defaultKeySize));
        register("SecretKeyFactory", algorithm, AndroidKeyStoreSecretKeyFactorySpi::new);
    }

    private void register(String type, String algorithm, Supplier<Object> factory)
    {
        putService(new FactoryService(this, type, algorithm, factory));
    }

    private static final class FactoryService extends Provider.Service
    {
        private final Supplier<Object> mFactory;

        FactoryService(Provider provider, String type, String algorithm, Supplier<Object> factory)
        {
            super(provider, type, algorithm, AndroidKeyStoreProvider.class.getName() + "$" + type + "$" + algorithm, null, null);
            mFactory = factory;
        }

        @Override
        public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException
        {
            return mFactory.get();
        }
    }
}
