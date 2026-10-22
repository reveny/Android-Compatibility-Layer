package android.security.identity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class CredentialDataRequest
{
    private Map<String, Collection<String>> mDeviceSignedEntriesToRequest = new LinkedHashMap<>();
    private Map<String, Collection<String>> mIssuerSignedEntriesToRequest = new LinkedHashMap<>();
    private boolean mAllowUsingExhaustedKeys = true;
    private boolean mAllowUsingExpiredKeys;
    private boolean mIncrementUseCount = true;
    private byte[] mRequestMessage;
    private byte[] mReaderSignature;

    CredentialDataRequest()
    {
    }

    public Map<String, Collection<String>> getDeviceSignedEntriesToRequest()
    {
        return mDeviceSignedEntriesToRequest;
    }

    public Map<String, Collection<String>> getIssuerSignedEntriesToRequest()
    {
        return mIssuerSignedEntriesToRequest;
    }

    public boolean isAllowUsingExhaustedKeys()
    {
        return mAllowUsingExhaustedKeys;
    }

    public boolean isAllowUsingExpiredKeys()
    {
        return mAllowUsingExpiredKeys;
    }

    public boolean isIncrementUseCount()
    {
        return mIncrementUseCount;
    }

    public byte[] getRequestMessage()
    {
        return mRequestMessage;
    }

    public byte[] getReaderSignature()
    {
        return mReaderSignature;
    }

    public static final class Builder
    {
        private final CredentialDataRequest mRequest = new CredentialDataRequest();

        public Builder()
        {
        }

        public Builder setDeviceSignedEntriesToRequest(Map<String, Collection<String>> entriesToRequest)
        {
            mRequest.mDeviceSignedEntriesToRequest = entriesToRequest;
            return this;
        }

        public Builder setIssuerSignedEntriesToRequest(Map<String, Collection<String>> entriesToRequest)
        {
            mRequest.mIssuerSignedEntriesToRequest = entriesToRequest;
            return this;
        }

        public Builder setAllowUsingExhaustedKeys(boolean allowUsingExhaustedKeys)
        {
            mRequest.mAllowUsingExhaustedKeys = allowUsingExhaustedKeys;
            return this;
        }

        public Builder setAllowUsingExpiredKeys(boolean allowUsingExpiredKeys)
        {
            mRequest.mAllowUsingExpiredKeys = allowUsingExpiredKeys;
            return this;
        }

        public Builder setIncrementUseCount(boolean incrementUseCount)
        {
            mRequest.mIncrementUseCount = incrementUseCount;
            return this;
        }

        public Builder setRequestMessage(byte[] requestMessage)
        {
            mRequest.mRequestMessage = requestMessage;
            return this;
        }

        public Builder setReaderSignature(byte[] readerSignature)
        {
            mRequest.mReaderSignature = readerSignature;
            return this;
        }

        public CredentialDataRequest build()
        {
            return mRequest;
        }
    }
}
