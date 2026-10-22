package android.security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public final class KeyChain
{
    public static final String ACTION_KEYCHAIN_CHANGED = "android.security.action.KEYCHAIN_CHANGED";
    public static final String ACTION_KEY_ACCESS_CHANGED = "android.security.action.KEY_ACCESS_CHANGED";
    public static final String ACTION_STORAGE_CHANGED = "android.security.STORAGE_CHANGED";
    public static final String ACTION_TRUST_STORE_CHANGED = "android.security.action.TRUST_STORE_CHANGED";
    public static final String EXTRA_CERTIFICATE = "CERT";
    public static final String EXTRA_KEY_ACCESSIBLE = "android.security.extra.KEY_ACCESSIBLE";
    public static final String EXTRA_KEY_ALIAS = "android.security.extra.KEY_ALIAS";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_PKCS12 = "PKCS12";
    public static final String KEY_ALIAS_SELECTION_DENIED = "android:alias-selection-denied";

    private static final String ACTION_INSTALL = "android.credentials.INSTALL";
    private static final String ACTION_MANAGE_CREDENTIALS = "android.security.MANAGE_CREDENTIALS";
    private static final String EXTRA_AUTHENTICATION_POLICY = "android.security.extra.AUTHENTICATION_POLICY";

    public KeyChain()
    {
    }

    public static Intent createInstallIntent()
    {
        return new Intent(ACTION_INSTALL);
    }

    public static Intent createManageCredentialsIntent(AppUriAuthenticationPolicy policy)
    {
        Intent intent = new Intent(ACTION_MANAGE_CREDENTIALS);
        intent.putExtra(EXTRA_AUTHENTICATION_POLICY, policy);
        return intent;
    }

    public static void choosePrivateKeyAlias(Activity activity, KeyChainAliasCallback response, String[] keyTypes,
            Principal[] issuers, String host, int port, String alias)
    {
        choosePrivateKeyAlias(activity, response, keyTypes, issuers, null, alias);
    }

    public static void choosePrivateKeyAlias(Activity activity, KeyChainAliasCallback response, String[] keyTypes,
            Principal[] issuers, Uri uri, String alias)
    {
        if (activity == null) throw new NullPointerException("activity == null");
        if (response == null) throw new NullPointerException("response == null");

        Thread thread = new Thread(() -> response.alias(null), "KeyChainAliasCallback");
        thread.setDaemon(true);
        thread.start();
    }

    public static boolean isCredentialManagementApp(Context context)
    {
        return false;
    }

    public static AppUriAuthenticationPolicy getCredentialManagementAppPolicy(Context context) throws SecurityException
    {
        throw new SecurityException("The caller is not the current credential management application");
    }

    public static boolean removeCredentialManagementApp(Context context)
    {
        return false;
    }

    public static PrivateKey getPrivateKey(Context context, String alias) throws KeyChainException, InterruptedException
    {
        if (alias == null) throw new NullPointerException("alias == null");
        if (context == null) throw new NullPointerException("context == null");
        return null;
    }

    public static X509Certificate[] getCertificateChain(Context context, String alias)
            throws KeyChainException, InterruptedException
    {
        if (alias == null) throw new NullPointerException("alias == null");
        return null;
    }

    public static boolean isKeyAlgorithmSupported(String algorithm)
    {
        return "RSA".equalsIgnoreCase(algorithm) || "EC".equalsIgnoreCase(algorithm);
    }

    @Deprecated
    public static boolean isBoundKeyAlgorithm(String algorithm)
    {
        return false;
    }
}
