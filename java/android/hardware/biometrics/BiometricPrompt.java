package android.hardware.biometrics;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.CancellationSignal;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;

import java.security.Signature;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.Mac;

public class BiometricPrompt
{
    public static final int BIOMETRIC_ACQUIRED_GOOD = 0;
    public static final int BIOMETRIC_ACQUIRED_PARTIAL = 1;
    public static final int BIOMETRIC_ACQUIRED_INSUFFICIENT = 2;
    public static final int BIOMETRIC_ACQUIRED_IMAGER_DIRTY = 3;
    public static final int BIOMETRIC_ACQUIRED_TOO_SLOW = 4;
    public static final int BIOMETRIC_ACQUIRED_TOO_FAST = 5;

    public static final int BIOMETRIC_ERROR_HW_UNAVAILABLE = 1;
    public static final int BIOMETRIC_ERROR_UNABLE_TO_PROCESS = 2;
    public static final int BIOMETRIC_ERROR_TIMEOUT = 3;
    public static final int BIOMETRIC_ERROR_NO_SPACE = 4;
    public static final int BIOMETRIC_ERROR_CANCELED = 5;
    public static final int BIOMETRIC_ERROR_LOCKOUT = 7;
    public static final int BIOMETRIC_ERROR_VENDOR = 8;
    public static final int BIOMETRIC_ERROR_LOCKOUT_PERMANENT = 9;
    public static final int BIOMETRIC_ERROR_USER_CANCELED = 10;
    public static final int BIOMETRIC_ERROR_NO_BIOMETRICS = 11;
    public static final int BIOMETRIC_ERROR_HW_NOT_PRESENT = 12;
    public static final int BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL = 14;
    public static final int BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED = 15;
    public static final int BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE = 20;
    public static final int BIOMETRIC_ERROR_NOT_ENABLED_FOR_APPS = 21;

    public static final int AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL = 1;
    public static final int AUTHENTICATION_RESULT_TYPE_BIOMETRIC = 2;

    public static final long BIOMETRIC_NO_AUTHENTICATION = -1L;

    private final Context mContext;
    private final CharSequence mTitle;
    private final CharSequence mSubtitle;
    private final CharSequence mDescription;
    private final CharSequence mNegativeButtonText;
    private final Executor mNegativeButtonExecutor;
    private final DialogInterface.OnClickListener mNegativeButtonListener;
    private final boolean mConfirmationRequired;
    private final int mAllowedAuthenticators;
    private final Bitmap mLogoBitmap;
    private final int mLogoRes;
    private final String mLogoDescription;
    private final PromptContentView mContentView;

    private BiometricPrompt(Builder builder)
    {
        mContext = builder.mContext;
        mTitle = builder.mTitle;
        mSubtitle = builder.mSubtitle;
        mDescription = builder.mDescription;
        mNegativeButtonText = builder.mNegativeButtonText;
        mNegativeButtonExecutor = builder.mNegativeButtonExecutor;
        mNegativeButtonListener = builder.mNegativeButtonListener;
        mConfirmationRequired = builder.mConfirmationRequired;
        mAllowedAuthenticators = builder.mAllowedAuthenticators;
        mLogoBitmap = builder.mLogoBitmap;
        mLogoRes = builder.mLogoRes;
        mLogoDescription = builder.mLogoDescription;
        mContentView = builder.mContentView;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public CharSequence getSubtitle()
    {
        return mSubtitle;
    }

    public CharSequence getDescription()
    {
        return mDescription;
    }

    public CharSequence getNegativeButtonText()
    {
        return mNegativeButtonText;
    }

    public boolean isConfirmationRequired()
    {
        return mConfirmationRequired;
    }

    public int getAllowedAuthenticators()
    {
        return mAllowedAuthenticators;
    }

    public Bitmap getLogoBitmap()
    {
        return mLogoBitmap;
    }

    public int getLogoRes()
    {
        return mLogoRes;
    }

    public String getLogoDescription()
    {
        return mLogoDescription;
    }

    public PromptContentView getContentView()
    {
        return mContentView;
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, Executor executor, AuthenticationCallback callback)
    {
        if (crypto == null) throw new IllegalArgumentException("Must supply a crypto object");
        authenticateInternal(cancel, executor, callback);
    }

    public void authenticate(CancellationSignal cancel, Executor executor, AuthenticationCallback callback)
    {
        authenticateInternal(cancel, executor, callback);
    }

    private void authenticateInternal(CancellationSignal cancel, Executor executor, AuthenticationCallback callback)
    {
        if (cancel == null) throw new IllegalArgumentException("Must supply a cancellation signal");
        if (executor == null) throw new IllegalArgumentException("Must supply an executor");
        if (callback == null) throw new IllegalArgumentException("Must supply a callback");
        if (cancel.isCanceled()) return;

        final boolean credentialAllowed = (mAllowedAuthenticators & BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0;
        final int error = credentialAllowed ? BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL : BIOMETRIC_ERROR_HW_NOT_PRESENT;
        final CharSequence message = credentialAllowed
            ? "No PIN, pattern, or password set"
            : "This device does not have a biometric sensor";

        executor.execute(() ->
        {
            if (!cancel.isCanceled()) callback.onAuthenticationError(error, message);
        });
    }

    public abstract static class AuthenticationCallback
    {
        public AuthenticationCallback()
        {
        }

        public void onAuthenticationError(int errorCode, CharSequence errString)
        {
        }

        public void onAuthenticationHelp(int helpCode, CharSequence helpString)
        {
        }

        public void onAuthenticationSucceeded(AuthenticationResult result)
        {
        }

        public void onAuthenticationFailed()
        {
        }
    }

    public static class AuthenticationResult
    {
        private final CryptoObject mCryptoObject;
        private final int mAuthenticationType;

        AuthenticationResult()
        {
            this(null, AUTHENTICATION_RESULT_TYPE_BIOMETRIC);
        }

        AuthenticationResult(CryptoObject crypto, int authenticationType)
        {
            mCryptoObject = crypto;
            mAuthenticationType = authenticationType;
        }

        public CryptoObject getCryptoObject()
        {
            return mCryptoObject;
        }

        public int getAuthenticationType()
        {
            return mAuthenticationType;
        }
    }

    public static final class CryptoObject
    {
        private final Object mCrypto;
        private final long mOperationHandle;

        public CryptoObject(Signature signature)
        {
            mCrypto = signature;
            mOperationHandle = 0;
        }

        public CryptoObject(Cipher cipher)
        {
            mCrypto = cipher;
            mOperationHandle = 0;
        }

        public CryptoObject(Mac mac)
        {
            mCrypto = mac;
            mOperationHandle = 0;
        }

        @Deprecated
        public CryptoObject(IdentityCredential credential)
        {
            mCrypto = credential;
            mOperationHandle = 0;
        }

        public CryptoObject(PresentationSession session)
        {
            mCrypto = session;
            mOperationHandle = 0;
        }

        public CryptoObject(long operationHandle)
        {
            mCrypto = null;
            mOperationHandle = operationHandle;
        }

        public Signature getSignature()
        {
            return mCrypto instanceof Signature ? (Signature)mCrypto : null;
        }

        public Cipher getCipher()
        {
            return mCrypto instanceof Cipher ? (Cipher)mCrypto : null;
        }

        public Mac getMac()
        {
            return mCrypto instanceof Mac ? (Mac)mCrypto : null;
        }

        @Deprecated
        public IdentityCredential getIdentityCredential()
        {
            return mCrypto instanceof IdentityCredential ? (IdentityCredential)mCrypto : null;
        }

        public PresentationSession getPresentationSession()
        {
            return mCrypto instanceof PresentationSession ? (PresentationSession)mCrypto : null;
        }

        public long getOperationHandle()
        {
            return mOperationHandle;
        }
    }

    public static class Builder
    {
        private final Context mContext;
        private CharSequence mTitle;
        private CharSequence mSubtitle;
        private CharSequence mDescription;
        private CharSequence mNegativeButtonText;
        private Executor mNegativeButtonExecutor;
        private DialogInterface.OnClickListener mNegativeButtonListener;
        private boolean mConfirmationRequired = true;
        private int mAllowedAuthenticators;
        private Bitmap mLogoBitmap;
        private int mLogoRes;
        private String mLogoDescription;
        private PromptContentView mContentView;

        public Builder(Context context)
        {
            mContext = context;
        }

        public Builder setTitle(CharSequence title)
        {
            mTitle = title;
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle)
        {
            mSubtitle = subtitle;
            return this;
        }

        public Builder setDescription(CharSequence description)
        {
            mDescription = description;
            return this;
        }

        public Builder setLogoRes(int logoRes)
        {
            if (mLogoBitmap != null) throw new IllegalStateException("Exclusively one of logo resource or logo bitmap can be set");
            mLogoRes = logoRes;
            return this;
        }

        public Builder setLogoBitmap(Bitmap logoBitmap)
        {
            if (mLogoRes != 0) throw new IllegalStateException("Exclusively one of logo resource or logo bitmap can be set");
            mLogoBitmap = logoBitmap;
            return this;
        }

        public Builder setLogoDescription(String logoDescription)
        {
            if (logoDescription == null || logoDescription.isEmpty())
            {
                throw new IllegalArgumentException("Logo description passed in can not be null or empty");
            }
            mLogoDescription = logoDescription;
            return this;
        }

        public Builder setContentView(PromptContentView view)
        {
            mContentView = view;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, Executor executor, DialogInterface.OnClickListener listener)
        {
            if (text == null || text.length() == 0) throw new IllegalArgumentException("Text must be set and non-empty");
            if (executor == null) throw new IllegalArgumentException("Executor must not be null");
            if (listener == null) throw new IllegalArgumentException("Listener must not be null");

            mNegativeButtonText = text;
            mNegativeButtonExecutor = executor;
            mNegativeButtonListener = listener;
            return this;
        }

        public Builder setConfirmationRequired(boolean requireConfirmation)
        {
            mConfirmationRequired = requireConfirmation;
            return this;
        }

        @Deprecated
        public Builder setDeviceCredentialAllowed(boolean allowed)
        {
            if (allowed) mAllowedAuthenticators |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            else mAllowedAuthenticators &= ~BiometricManager.Authenticators.DEVICE_CREDENTIAL;
            return this;
        }

        public Builder setAllowedAuthenticators(int authenticators)
        {
            mAllowedAuthenticators = authenticators;
            return this;
        }

        public BiometricPrompt build()
        {
            final boolean credentialAllowed = (mAllowedAuthenticators & BiometricManager.Authenticators.DEVICE_CREDENTIAL) != 0;

            if (mTitle == null || mTitle.length() == 0) throw new IllegalArgumentException("Title must be set and non-empty");
            if (mNegativeButtonText == null && !credentialAllowed)
            {
                throw new IllegalArgumentException("Negative text must be set and non-empty");
            }
            if (mNegativeButtonText != null && credentialAllowed)
            {
                throw new IllegalArgumentException("Can't have both negative button behavior and device credential enabled");
            }

            return new BiometricPrompt(this);
        }
    }
}
