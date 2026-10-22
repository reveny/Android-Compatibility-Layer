package android.hardware.biometrics;

import android.content.Context;

public class BiometricManager
{
    public static final int BIOMETRIC_SUCCESS = 0;
    public static final int BIOMETRIC_ERROR_HW_UNAVAILABLE = 1;
    public static final int BIOMETRIC_ERROR_NONE_ENROLLED = 11;
    public static final int BIOMETRIC_ERROR_NO_HARDWARE = 12;
    public static final int BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED = 15;
    public static final int BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE = 20;
    public static final int BIOMETRIC_ERROR_NOT_ENABLED_FOR_APPS = 21;

    public static final long BIOMETRIC_NO_AUTHENTICATION = -1L;

    public BiometricManager(Context context)
    {
    }

    @Deprecated
    public int canAuthenticate()
    {
        return canAuthenticate(Authenticators.BIOMETRIC_WEAK);
    }

    public int canAuthenticate(int authenticators)
    {
        if ((authenticators & Authenticators.DEVICE_CREDENTIAL) != 0) return BIOMETRIC_ERROR_NONE_ENROLLED;
        if ((authenticators & Authenticators.IDENTITY_CHECK) != 0) return BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE;
        return BIOMETRIC_ERROR_NO_HARDWARE;
    }

    public long getLastAuthenticationTime(int authenticators)
    {
        if (authenticators == 0) throw new IllegalArgumentException("authenticators must not be empty");
        return BIOMETRIC_NO_AUTHENTICATION;
    }

    public Strings getStrings(int authenticators)
    {
        return new Strings(authenticators);
    }

    public interface Authenticators
    {
        int BIOMETRIC_STRONG = 0x000F;
        int BIOMETRIC_WEAK = 0x00FF;
        int DEVICE_CREDENTIAL = 1 << 15;
        int IDENTITY_CHECK = 1 << 16;
    }

    public static class Strings
    {
        private final int mAuthenticators;

        Strings(int authenticators)
        {
            mAuthenticators = authenticators;
        }

        public CharSequence getButtonLabel()
        {
            if ((mAuthenticators & Authenticators.DEVICE_CREDENTIAL) != 0) return "Use screen lock";
            return null;
        }

        public CharSequence getPromptMessage()
        {
            if ((mAuthenticators & Authenticators.DEVICE_CREDENTIAL) != 0) return "Enter your screen lock to continue";
            return null;
        }

        public CharSequence getSettingName()
        {
            if ((mAuthenticators & Authenticators.DEVICE_CREDENTIAL) != 0) return "Use screen lock";
            return null;
        }
    }
}
