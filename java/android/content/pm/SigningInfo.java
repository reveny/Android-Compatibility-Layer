package android.content.pm;

public final class SigningInfo
{
    private Signature[] mSigningCertificateHistory;
    private Signature[] mApkContentsSigners;
    private boolean mHasMultipleSigners;

    public SigningInfo()
    {
        mSigningCertificateHistory = new Signature[0];
        mApkContentsSigners = new Signature[0];
    }

    public SigningInfo(Signature[] signers, boolean hasMultipleSigners)
    {
        Signature[] copy = signers == null ? new Signature[0] : signers.clone();

        mApkContentsSigners = copy;
        mSigningCertificateHistory = hasMultipleSigners ? new Signature[0] : copy;
        mHasMultipleSigners = hasMultipleSigners;
    }

    public boolean hasMultipleSigners()
    {
        return mHasMultipleSigners;
    }

    public boolean hasPastSigningCertificates()
    {
        return !mHasMultipleSigners && mSigningCertificateHistory.length > 1;
    }

    public Signature[] getApkContentsSigners()
    {
        return mApkContentsSigners.clone();
    }

    public Signature[] getSigningCertificateHistory()
    {
        return mSigningCertificateHistory.clone();
    }
}
