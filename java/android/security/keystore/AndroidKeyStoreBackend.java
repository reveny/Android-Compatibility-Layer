package android.security.keystore;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

final class AndroidKeyStoreBackend
{
    private static final String TAG = "AndroidKeyStore";
    private static final int MAGIC = 0x41434b53;
    private static final int VERSION = 1;

    private static final Map<String, KeyStoreRecord> sRecords = new LinkedHashMap<>();
    private static File sFile;
    private static boolean sLoaded;

    private AndroidKeyStoreBackend()
    {
    }

    static synchronized void setFile(File file)
    {
        sFile = file;
        sLoaded = false;
        sRecords.clear();
    }

    private static void ensureLoaded()
    {
        if (sLoaded) return;
        sLoaded = true;
        if (sFile == null || !sFile.isFile()) return;

        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(sFile))))
        {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) throw new IOException("unknown keystore format");
            int count = input.readInt();
            for (int index = 0; index < count; index++)
            {
                KeyStoreRecord record = KeyStoreRecord.read(input);
                sRecords.put(record.mAlias, record);
            }
        }
        catch (IOException exception)
        {
            Log.w(TAG, "could not read " + sFile + ", starting empty", exception);
            sRecords.clear();
        }
    }

    private static void save()
    {
        if (sFile == null) return;

        File parent = sFile.getParentFile();
        if (parent != null && !parent.isDirectory()) parent.mkdirs();

        File temporary = new File(sFile.getPath() + ".tmp");
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temporary))))
        {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(sRecords.size());
            for (KeyStoreRecord record : sRecords.values()) record.write(output);
        }
        catch (IOException exception)
        {
            Log.w(TAG, "could not write " + temporary, exception);
            temporary.delete();
            return;
        }

        if (!temporary.renameTo(sFile)) Log.w(TAG, "could not replace " + sFile);
    }

    static synchronized void load()
    {
        ensureLoaded();
    }

    static synchronized KeyStoreRecord get(String alias)
    {
        ensureLoaded();
        return sRecords.get(alias);
    }

    static synchronized void put(KeyStoreRecord record)
    {
        ensureLoaded();
        sRecords.remove(record.mAlias);
        sRecords.put(record.mAlias, record);
        save();
    }

    static synchronized boolean remove(String alias)
    {
        ensureLoaded();
        boolean removed = sRecords.remove(alias) != null;
        if (removed) save();
        return removed;
    }

    static synchronized List<String> aliases()
    {
        ensureLoaded();
        return new ArrayList<>(sRecords.keySet());
    }

    static synchronized KeyStoreRecord findByKey(Key key)
    {
        ensureLoaded();
        if (key == null) return null;
        byte[] encoded = key.getEncoded();
        if (encoded == null) return null;

        for (KeyStoreRecord record : sRecords.values())
        {
            if (record.mKeyEncoded != null && Arrays.equals(record.mKeyEncoded, encoded)) return record;
        }

        if (key instanceof PrivateKey)
        {
            for (KeyStoreRecord record : sRecords.values())
            {
                if (record.mType != KeyStoreRecord.TYPE_PRIVATE_KEY) continue;
                try
                {
                    if (decodeKey(record).equals(key)) return record;
                }
                catch (GeneralSecurityException exception)
                {
                    Log.w(TAG, "could not decode " + record.mAlias, exception);
                }
            }
        }
        return null;
    }

    static synchronized String findCertificateAlias(Certificate certificate)
    {
        ensureLoaded();
        if (certificate == null) return null;
        try
        {
            byte[] encoded = certificate.getEncoded();
            for (KeyStoreRecord record : sRecords.values())
            {
                if (record.mCertificateChain.length > 0 && Arrays.equals(record.mCertificateChain[0], encoded))
                {
                    return record.mAlias;
                }
            }
        }
        catch (GeneralSecurityException exception)
        {
            Log.w(TAG, "could not encode certificate", exception);
        }
        return null;
    }

    static Key decodeKey(KeyStoreRecord record) throws GeneralSecurityException
    {
        if (record.mType == KeyStoreRecord.TYPE_SECRET_KEY) return new SecretKeySpec(record.mKeyEncoded, record.mAlgorithm);
        if (record.mType == KeyStoreRecord.TYPE_PRIVATE_KEY)
        {
            return KeyFactory.getInstance(record.mAlgorithm).generatePrivate(new PKCS8EncodedKeySpec(record.mKeyEncoded));
        }
        return null;
    }

    static Certificate[] decodeChain(KeyStoreRecord record) throws GeneralSecurityException
    {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        Certificate[] chain = new Certificate[record.mCertificateChain.length];
        for (int index = 0; index < chain.length; index++)
        {
            chain[index] = factory.generateCertificate(new ByteArrayInputStream(record.mCertificateChain[index]));
        }
        return chain;
    }
}
