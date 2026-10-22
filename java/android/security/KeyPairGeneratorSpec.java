package android.security;

import android.content.Context;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

@Deprecated
public final class KeyPairGeneratorSpec implements AlgorithmParameterSpec
{
    private final Context mContext;
    private final String mKeystoreAlias;
    private final String mKeyType;
    private final int mKeySize;
    private final AlgorithmParameterSpec mSpec;
    private final X500Principal mSubjectDN;
    private final BigInteger mSerialNumber;
    private final Date mStartDate;
    private final Date mEndDate;
    private final boolean mEncryptionRequired;

    private KeyPairGeneratorSpec(Builder builder)
    {
        if (builder.mContext == null) throw new IllegalArgumentException("context == null");
        if (builder.mKeystoreAlias == null || builder.mKeystoreAlias.isEmpty())
        {
            throw new IllegalArgumentException("keyStoreAlias must not be empty");
        }
        if (builder.mSubjectDN == null) throw new IllegalArgumentException("subjectDN == null");
        if (builder.mSerialNumber == null) throw new IllegalArgumentException("serialNumber == null");
        if (builder.mStartDate == null) throw new IllegalArgumentException("startDate == null");
        if (builder.mEndDate == null) throw new IllegalArgumentException("endDate == null");
        if (builder.mEndDate.before(builder.mStartDate)) throw new IllegalArgumentException("endDate < startDate");

        mContext = builder.mContext;
        mKeystoreAlias = builder.mKeystoreAlias;
        mKeyType = builder.mKeyType;
        mKeySize = builder.mKeySize;
        mSpec = builder.mSpec;
        mSubjectDN = builder.mSubjectDN;
        mSerialNumber = builder.mSerialNumber;
        mStartDate = new Date(builder.mStartDate.getTime());
        mEndDate = new Date(builder.mEndDate.getTime());
        mEncryptionRequired = builder.mEncryptionRequired;
    }

    public Context getContext()
    {
        return mContext;
    }

    public String getKeystoreAlias()
    {
        return mKeystoreAlias;
    }

    public String getKeyType()
    {
        return mKeyType;
    }

    public int getKeySize()
    {
        return mKeySize;
    }

    public AlgorithmParameterSpec getAlgorithmParameterSpec()
    {
        return mSpec;
    }

    public X500Principal getSubjectDN()
    {
        return mSubjectDN;
    }

    public BigInteger getSerialNumber()
    {
        return mSerialNumber;
    }

    public Date getStartDate()
    {
        return new Date(mStartDate.getTime());
    }

    public Date getEndDate()
    {
        return new Date(mEndDate.getTime());
    }

    public boolean isEncryptionRequired()
    {
        return mEncryptionRequired;
    }

    @Deprecated
    public static final class Builder
    {
        private final Context mContext;
        private String mKeystoreAlias;
        private String mKeyType = "RSA";
        private int mKeySize = -1;
        private AlgorithmParameterSpec mSpec;
        private X500Principal mSubjectDN;
        private BigInteger mSerialNumber;
        private Date mStartDate;
        private Date mEndDate;
        private boolean mEncryptionRequired;

        public Builder(Context context)
        {
            if (context == null) throw new NullPointerException("context == null");
            mContext = context;
        }

        public Builder setAlias(String alias)
        {
            if (alias == null) throw new NullPointerException("alias == null");
            mKeystoreAlias = alias;
            return this;
        }

        public Builder setKeyType(String keyType) throws NoSuchAlgorithmException
        {
            if (keyType == null) throw new NullPointerException("keyType == null");
            if (!"RSA".equalsIgnoreCase(keyType) && !"EC".equalsIgnoreCase(keyType))
            {
                throw new NoSuchAlgorithmException("Unsupported key type: " + keyType);
            }
            mKeyType = keyType;
            return this;
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

        public Builder setSubject(X500Principal subject)
        {
            if (subject == null) throw new NullPointerException("subject == null");
            mSubjectDN = subject;
            return this;
        }

        public Builder setSerialNumber(BigInteger serialNumber)
        {
            if (serialNumber == null) throw new NullPointerException("serialNumber == null");
            mSerialNumber = serialNumber;
            return this;
        }

        public Builder setStartDate(Date startDate)
        {
            if (startDate == null) throw new NullPointerException("startDate == null");
            mStartDate = startDate;
            return this;
        }

        public Builder setEndDate(Date endDate)
        {
            if (endDate == null) throw new NullPointerException("endDate == null");
            mEndDate = endDate;
            return this;
        }

        public Builder setEncryptionRequired()
        {
            mEncryptionRequired = true;
            return this;
        }

        public KeyPairGeneratorSpec build()
        {
            return new KeyPairGeneratorSpec(this);
        }
    }
}
