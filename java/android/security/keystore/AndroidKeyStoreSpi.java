package android.security.keystore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

import javax.crypto.SecretKey;

final class AndroidKeyStoreSpi extends KeyStoreSpi
{
    @Override
    public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException
    {
        KeyStoreRecord record = AndroidKeyStoreBackend.get(alias);
        if (record == null || record.mType == KeyStoreRecord.TYPE_CERTIFICATE) return null;
        try
        {
            return AndroidKeyStoreBackend.decodeKey(record);
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw exception;
        }
        catch (GeneralSecurityException exception)
        {
            UnrecoverableKeyException failure = new UnrecoverableKeyException("Failed to load key " + alias);
            failure.initCause(exception);
            throw failure;
        }
    }

    @Override
    public Certificate[] engineGetCertificateChain(String alias)
    {
        KeyStoreRecord record = AndroidKeyStoreBackend.get(alias);
        if (record == null || record.mType != KeyStoreRecord.TYPE_PRIVATE_KEY) return null;
        try
        {
            return AndroidKeyStoreBackend.decodeChain(record);
        }
        catch (GeneralSecurityException exception)
        {
            return null;
        }
    }

    @Override
    public Certificate engineGetCertificate(String alias)
    {
        KeyStoreRecord record = AndroidKeyStoreBackend.get(alias);
        if (record == null || record.mCertificateChain.length == 0) return null;
        try
        {
            return AndroidKeyStoreBackend.decodeChain(record)[0];
        }
        catch (GeneralSecurityException exception)
        {
            return null;
        }
    }

    @Override
    public Date engineGetCreationDate(String alias)
    {
        KeyStoreRecord record = AndroidKeyStoreBackend.get(alias);
        return record != null ? new Date(record.mCreationDate) : null;
    }

    @Override
    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException
    {
        if (password != null && password.length > 0)
        {
            throw new KeyStoreException("entries cannot be protected with passwords");
        }
        if (key instanceof PrivateKey)
        {
            setPrivateKeyEntry(alias, (PrivateKey)key, chain, null);
        }
        else if (key instanceof SecretKey)
        {
            setSecretKeyEntry(alias, (SecretKey)key, null);
        }
        else
        {
            throw new KeyStoreException("Only PrivateKey and SecretKey are supported");
        }
    }

    @Override
    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException
    {
        throw new KeyStoreException("Operation not supported because key encoding is unknown");
    }

    @Override
    public void engineSetCertificateEntry(String alias, Certificate certificate) throws KeyStoreException
    {
        if (alias == null) throw new NullPointerException("alias == null");
        if (certificate == null) throw new NullPointerException("cert == null");

        KeyStoreRecord existing = AndroidKeyStoreBackend.get(alias);
        if (existing != null && existing.mType != KeyStoreRecord.TYPE_CERTIFICATE)
        {
            throw new KeyStoreException("Entry exists and is not a trusted certificate");
        }

        KeyStoreRecord record = new KeyStoreRecord(alias, KeyStoreRecord.TYPE_CERTIFICATE);
        record.mCertificateChain = new byte[][] {encode(certificate)};
        AndroidKeyStoreBackend.put(record);
    }

    @Override
    public void engineDeleteEntry(String alias) throws KeyStoreException
    {
        AndroidKeyStoreBackend.remove(alias);
    }

    @Override
    public Enumeration<String> engineAliases()
    {
        return Collections.enumeration(AndroidKeyStoreBackend.aliases());
    }

    @Override
    public boolean engineContainsAlias(String alias)
    {
        if (alias == null) throw new NullPointerException("alias == null");
        return AndroidKeyStoreBackend.get(alias) != null;
    }

    @Override
    public int engineSize()
    {
        return AndroidKeyStoreBackend.aliases().size();
    }

    @Override
    public boolean engineIsKeyEntry(String alias)
    {
        KeyStoreRecord record = AndroidKeyStoreBackend.get(alias);
        return record != null && record.mType != KeyStoreRecord.TYPE_CERTIFICATE;
    }

    @Override
    public boolean engineIsCertificateEntry(String alias)
    {
        KeyStoreRecord record = AndroidKeyStoreBackend.get(alias);
        return record != null && record.mType == KeyStoreRecord.TYPE_CERTIFICATE;
    }

    @Override
    public String engineGetCertificateAlias(Certificate certificate)
    {
        return AndroidKeyStoreBackend.findCertificateAlias(certificate);
    }

    @Override
    public void engineStore(OutputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        throw new UnsupportedOperationException("Can not serialize AndroidKeyStore to OutputStream");
    }

    @Override
    public void engineLoad(InputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException
    {
        if (stream != null) throw new IllegalArgumentException("InputStream not supported");
        if (password != null) throw new IllegalArgumentException("password not supported");
        AndroidKeyStoreBackend.load();
    }

    @Override
    public void engineLoad(KeyStore.LoadStoreParameter parameter)
            throws IOException, NoSuchAlgorithmException, CertificateException
    {
        AndroidKeyStoreBackend.load();
    }

    @Override
    public KeyStore.Entry engineGetEntry(String alias, KeyStore.ProtectionParameter parameter)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableEntryException
    {
        if (alias == null) throw new NullPointerException("alias == null");
        KeyStoreRecord record = AndroidKeyStoreBackend.get(alias);
        if (record == null) return null;

        try
        {
            switch (record.mType)
            {
                case KeyStoreRecord.TYPE_SECRET_KEY:
                    return new KeyStore.SecretKeyEntry((SecretKey)AndroidKeyStoreBackend.decodeKey(record));
                case KeyStoreRecord.TYPE_PRIVATE_KEY:
                    return new KeyStore.PrivateKeyEntry((PrivateKey)AndroidKeyStoreBackend.decodeKey(record),
                            AndroidKeyStoreBackend.decodeChain(record));
                default:
                    return new KeyStore.TrustedCertificateEntry(AndroidKeyStoreBackend.decodeChain(record)[0]);
            }
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw exception;
        }
        catch (GeneralSecurityException exception)
        {
            UnrecoverableEntryException failure = new UnrecoverableEntryException("Failed to load entry " + alias);
            failure.initCause(exception);
            throw failure;
        }
    }

    @Override
    public void engineSetEntry(String alias, KeyStore.Entry entry, KeyStore.ProtectionParameter parameter)
            throws KeyStoreException
    {
        if (entry == null) throw new KeyStoreException("entry == null");
        if (parameter != null && !(parameter instanceof KeyProtection))
        {
            throw new KeyStoreException("Unsupported protection parameter class: " + parameter.getClass().getName()
                    + ". Supported: " + KeyProtection.class.getName());
        }
        KeyProtection protection = (KeyProtection)parameter;

        if (entry instanceof KeyStore.TrustedCertificateEntry)
        {
            engineSetCertificateEntry(alias, ((KeyStore.TrustedCertificateEntry)entry).getTrustedCertificate());
        }
        else if (entry instanceof KeyStore.PrivateKeyEntry)
        {
            KeyStore.PrivateKeyEntry privateEntry = (KeyStore.PrivateKeyEntry)entry;
            setPrivateKeyEntry(alias, privateEntry.getPrivateKey(), privateEntry.getCertificateChain(), protection);
        }
        else if (entry instanceof KeyStore.SecretKeyEntry)
        {
            setSecretKeyEntry(alias, ((KeyStore.SecretKeyEntry)entry).getSecretKey(), protection);
        }
        else
        {
            throw new KeyStoreException("Entry must be a PrivateKeyEntry, SecretKeyEntry or TrustedCertificateEntry; was " + entry);
        }
    }

    @Override
    public boolean engineEntryInstanceOf(String alias, Class<? extends KeyStore.Entry> entryClass)
    {
        KeyStoreRecord record = AndroidKeyStoreBackend.get(alias);
        if (record == null) return false;
        switch (record.mType)
        {
            case KeyStoreRecord.TYPE_SECRET_KEY:
                return entryClass == KeyStore.SecretKeyEntry.class;
            case KeyStoreRecord.TYPE_PRIVATE_KEY:
                return entryClass == KeyStore.PrivateKeyEntry.class;
            default:
                return entryClass == KeyStore.TrustedCertificateEntry.class;
        }
    }

    private static byte[] encode(Certificate certificate) throws KeyStoreException
    {
        try
        {
            return certificate.getEncoded();
        }
        catch (CertificateEncodingException exception)
        {
            throw new KeyStoreException("Failed to encode certificate", exception);
        }
    }

    private static void setPrivateKeyEntry(String alias, PrivateKey key, Certificate[] chain, KeyProtection protection)
            throws KeyStoreException
    {
        if (alias == null) throw new NullPointerException("alias == null");
        if (chain == null || chain.length == 0) throw new KeyStoreException("Must supply at least one Certificate with PrivateKey");
        if (protection != null && protection.isStrongBoxBacked())
        {
            throw new KeyStoreException("Failed to import key", new StrongBoxUnavailableException("No StrongBox available"));
        }

        byte[] encoded = key.getEncoded();
        if (encoded == null || !"PKCS#8".equals(key.getFormat()))
        {
            throw new KeyStoreException("Unsupported private key export format: " + key.getFormat()
                    + ". Only private keys which export their key material in PKCS#8 format are supported.");
        }

        KeyStoreRecord record = new KeyStoreRecord(alias, KeyStoreRecord.TYPE_PRIVATE_KEY);
        record.mAlgorithm = key.getAlgorithm();
        record.mKeyEncoded = encoded;
        record.mCertificateChain = new byte[chain.length][];
        for (int index = 0; index < chain.length; index++) record.mCertificateChain[index] = encode(chain[index]);
        record.applyProtection(protection, KeyStoreRecord.keySizeOf(key, encoded));
        AndroidKeyStoreBackend.put(record);
    }

    private static void setSecretKeyEntry(String alias, SecretKey key, KeyProtection protection) throws KeyStoreException
    {
        if (alias == null) throw new NullPointerException("alias == null");
        if (protection == null)
        {
            throw new KeyStoreException("Protection parameters must be specified when importing a symmetric key");
        }
        if (protection.isStrongBoxBacked())
        {
            throw new KeyStoreException("Failed to import key", new StrongBoxUnavailableException("No StrongBox available"));
        }

        byte[] encoded = key.getEncoded();
        if (encoded == null || !"RAW".equals(key.getFormat()))
        {
            throw new KeyStoreException("Unsupported secret key material export format: " + key.getFormat()
                    + ". Only secret keys which export their key material in RAW format are supported.");
        }

        KeyStoreRecord record = new KeyStoreRecord(alias, KeyStoreRecord.TYPE_SECRET_KEY);
        record.mAlgorithm = key.getAlgorithm();
        record.mKeyEncoded = encoded;
        record.applyProtection(protection, encoded.length * 8);
        AndroidKeyStoreBackend.put(record);
    }
}
