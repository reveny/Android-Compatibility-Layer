package android.security.identity;

import java.security.cert.X509Certificate;

public class AccessControlProfile
{
    private AccessControlProfileId mAccessControlProfileId;
    private X509Certificate mReaderCertificate;
    private boolean mUserAuthenticationRequired = true;
    private long mUserAuthenticationTimeout;

    private AccessControlProfile()
    {
    }

    AccessControlProfileId getAccessControlProfileId()
    {
        return mAccessControlProfileId;
    }

    X509Certificate getReaderCertificate()
    {
        return mReaderCertificate;
    }

    boolean isUserAuthenticationRequired()
    {
        return mUserAuthenticationRequired;
    }

    long getUserAuthenticationTimeout()
    {
        return mUserAuthenticationTimeout;
    }

    public static final class Builder
    {
        private final AccessControlProfile mProfile = new AccessControlProfile();

        public Builder(AccessControlProfileId accessControlProfileId)
        {
            mProfile.mAccessControlProfileId = accessControlProfileId;
        }

        public Builder setUserAuthenticationRequired(boolean userAuthenticationRequired)
        {
            mProfile.mUserAuthenticationRequired = userAuthenticationRequired;
            return this;
        }

        public Builder setReaderCertificate(X509Certificate readerCertificate)
        {
            mProfile.mReaderCertificate = readerCertificate;
            return this;
        }

        public Builder setUserAuthenticationTimeout(long userAuthenticationTimeoutMillis)
        {
            mProfile.mUserAuthenticationTimeout = userAuthenticationTimeoutMillis;
            return this;
        }

        public AccessControlProfile build()
        {
            return mProfile;
        }
    }
}
