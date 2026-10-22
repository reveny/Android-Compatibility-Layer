package android.hardware.fingerprint;

import android.content.Context;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.Mac;

@Deprecated
public class FingerprintManager
{
    public static final int FINGERPRINT_ERROR_HW_UNAVAILABLE = 1;
    public static final int FINGERPRINT_ERROR_UNABLE_TO_PROCESS = 2;
    public static final int FINGERPRINT_ERROR_TIMEOUT = 3;
    public static final int FINGERPRINT_ERROR_NO_SPACE = 4;
    public static final int FINGERPRINT_ERROR_CANCELED = 5;
    public static final int FINGERPRINT_ERROR_LOCKOUT = 7;
    public static final int FINGERPRINT_ERROR_VENDOR = 8;
    public static final int FINGERPRINT_ERROR_LOCKOUT_PERMANENT = 9;
    public static final int FINGERPRINT_ERROR_USER_CANCELED = 10;
    public static final int FINGERPRINT_ERROR_NO_FINGERPRINTS = 11;
    public static final int FINGERPRINT_ERROR_HW_NOT_PRESENT = 12;

    public static final int FINGERPRINT_ACQUIRED_GOOD = 0;
    public static final int FINGERPRINT_ACQUIRED_PARTIAL = 1;
    public static final int FINGERPRINT_ACQUIRED_INSUFFICIENT = 2;
    public static final int FINGERPRINT_ACQUIRED_IMAGER_DIRTY = 3;
    public static final int FINGERPRINT_ACQUIRED_TOO_SLOW = 4;
    public static final int FINGERPRINT_ACQUIRED_TOO_FAST = 5;

    public FingerprintManager(Context context)
    {
    }

    @Deprecated
    public void authenticate(CryptoObject crypto, CancellationSignal cancel, int flags, AuthenticationCallback callback, Handler handler)
    {
        if (callback == null) throw new IllegalArgumentException("Must supply an authentication callback");
        if (cancel != null && cancel.isCanceled()) return;

        Handler target = handler != null ? handler : new Handler(Looper.getMainLooper());
        target.post(() ->
        {
            if (cancel == null || !cancel.isCanceled())
            {
                callback.onAuthenticationError(FINGERPRINT_ERROR_HW_UNAVAILABLE, "Fingerprint hardware not available.");
            }
        });
    }

    @Deprecated
    public boolean hasEnrolledFingerprints()
    {
        return false;
    }

    @Deprecated
    public boolean isHardwareDetected()
    {
        return false;
    }

    @Deprecated
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

    @Deprecated
    public static class AuthenticationResult
    {
        private final CryptoObject mCryptoObject;

        AuthenticationResult()
        {
            this(null);
        }

        AuthenticationResult(CryptoObject crypto)
        {
            mCryptoObject = crypto;
        }

        public CryptoObject getCryptoObject()
        {
            return mCryptoObject;
        }
    }

    @Deprecated
    public static final class CryptoObject
    {
        private final Object mCrypto;

        public CryptoObject(Signature signature)
        {
            mCrypto = signature;
        }

        public CryptoObject(Cipher cipher)
        {
            mCrypto = cipher;
        }

        public CryptoObject(Mac mac)
        {
            mCrypto = mac;
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
    }
}
