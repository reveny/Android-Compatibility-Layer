package android.accounts;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;

public abstract class AbstractAccountAuthenticator
{
    public static final String KEY_CUSTOM_TOKEN_EXPIRY = "android.accounts.expiry";

    private final Context mContext;
    private final android.os.Binder mBinder = new android.os.Binder("android.accounts.IAccountAuthenticator");

    public AbstractAccountAuthenticator(Context context)
    {
        mContext = context;
    }

    public final IBinder getIBinder()
    {
        return mBinder;
    }

    public abstract Bundle editProperties(AccountAuthenticatorResponse response, String accountType);

    public abstract Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
                                      String[] requiredFeatures, Bundle options) throws NetworkErrorException;

    public abstract Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options)
        throws NetworkErrorException;

    public abstract Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options)
        throws NetworkErrorException;

    public abstract String getAuthTokenLabel(String authTokenType);

    public abstract Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
                                             Bundle options) throws NetworkErrorException;

    public abstract Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
        throws NetworkErrorException;

    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException
    {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }

    public Bundle getAccountCredentialsForCloning(AccountAuthenticatorResponse response, Account account)
        throws NetworkErrorException
    {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    public Bundle addAccountFromCredentials(AccountAuthenticatorResponse response, Account account, Bundle accountCredentials)
        throws NetworkErrorException
    {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    public Bundle startAddAccountSession(AccountAuthenticatorResponse response, String accountType, String authTokenType,
                                         String[] requiredFeatures, Bundle options) throws NetworkErrorException
    {
        Bundle sessionBundle = new Bundle();
        sessionBundle.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        sessionBundle.putString("authTokenType", authTokenType);
        sessionBundle.putStringArray("requiredFeatures", requiredFeatures);
        sessionBundle.putBundle("options", options);

        Bundle result = new Bundle();
        result.putBundle(AccountManager.KEY_ACCOUNT_SESSION_BUNDLE, sessionBundle);
        return result;
    }

    public Bundle startUpdateCredentialsSession(AccountAuthenticatorResponse response, Account account, String authTokenType,
                                                Bundle options) throws NetworkErrorException
    {
        Bundle sessionBundle = new Bundle();
        sessionBundle.putString(AccountManager.KEY_ACCOUNT_NAME, account == null ? null : account.name);
        sessionBundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account == null ? null : account.type);
        sessionBundle.putString("authTokenType", authTokenType);
        sessionBundle.putBundle("options", options);

        Bundle result = new Bundle();
        result.putBundle(AccountManager.KEY_ACCOUNT_SESSION_BUNDLE, sessionBundle);
        return result;
    }

    public Bundle finishSession(AccountAuthenticatorResponse response, String accountType, Bundle sessionBundle)
        throws NetworkErrorException
    {
        Bundle result = new Bundle();
        result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
        result.putString(AccountManager.KEY_ERROR_MESSAGE, "finishSession is not supported by " + accountType);
        return result;
    }

    public Bundle isCredentialsUpdateSuggested(AccountAuthenticatorResponse response, Account account, String statusToken)
        throws NetworkErrorException
    {
        Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }
}
