package android.accounts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AccountManager
{
    public static final String ACTION_ACCOUNT_REMOVED = "android.accounts.action.ACCOUNT_REMOVED";
    public static final String ACTION_AUTHENTICATOR_INTENT = "android.accounts.AccountAuthenticator";
    public static final String AUTHENTICATOR_ATTRIBUTES_NAME = "account-authenticator";
    public static final String AUTHENTICATOR_META_DATA_NAME = "android.accounts.AccountAuthenticator";
    public static final int ERROR_CODE_BAD_ARGUMENTS = 7;
    public static final int ERROR_CODE_BAD_AUTHENTICATION = 9;
    public static final int ERROR_CODE_BAD_REQUEST = 8;
    public static final int ERROR_CODE_CANCELED = 4;
    public static final int ERROR_CODE_INVALID_RESPONSE = 5;
    public static final int ERROR_CODE_NETWORK_ERROR = 3;
    public static final int ERROR_CODE_REMOTE_EXCEPTION = 1;
    public static final int ERROR_CODE_UNSUPPORTED_OPERATION = 6;
    public static final String KEY_ACCOUNTS = "accounts";
    public static final String KEY_ACCOUNT_AUTHENTICATOR_RESPONSE = "accountAuthenticatorResponse";
    public static final String KEY_ACCOUNT_MANAGER_RESPONSE = "accountManagerResponse";
    public static final String KEY_ACCOUNT_NAME = "authAccount";
    public static final String KEY_ACCOUNT_SESSION_BUNDLE = "accountSessionBundle";
    public static final String KEY_ACCOUNT_STATUS_TOKEN = "accountStatusToken";
    public static final String KEY_ACCOUNT_TYPE = "accountType";
    public static final String KEY_ANDROID_PACKAGE_NAME = "androidPackageName";
    public static final String KEY_AUTHENTICATOR_TYPES = "authenticator_types";
    public static final String KEY_AUTHTOKEN = "authtoken";
    public static final String KEY_AUTH_FAILED_MESSAGE = "authFailedMessage";
    public static final String KEY_AUTH_TOKEN_LABEL = "authTokenLabelKey";
    public static final String KEY_BOOLEAN_RESULT = "booleanResult";
    public static final String KEY_CALLER_PID = "callerPid";
    public static final String KEY_CALLER_UID = "callerUid";
    public static final String KEY_ERROR_CODE = "errorCode";
    public static final String KEY_ERROR_MESSAGE = "errorMessage";
    public static final String KEY_INTENT = "intent";
    public static final String KEY_LAST_AUTHENTICATED_TIME = "lastAuthenticatedTime";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_USERDATA = "userdata";
    public static final String LOGIN_ACCOUNTS_CHANGED_ACTION = "android.accounts.LOGIN_ACCOUNTS_CHANGED";
    public static final String PACKAGE_NAME_KEY_LEGACY_NOT_VISIBLE = "android:accounts:key_legacy_not_visible";
    public static final String PACKAGE_NAME_KEY_LEGACY_VISIBLE = "android:accounts:key_legacy_visible";
    public static final int VISIBILITY_NOT_VISIBLE = 3;
    public static final int VISIBILITY_UNDEFINED = 0;
    public static final int VISIBILITY_USER_MANAGED_NOT_VISIBLE = 4;
    public static final int VISIBILITY_USER_MANAGED_VISIBLE = 2;
    public static final int VISIBILITY_VISIBLE = 1;

    private static final String TAG = "AccountManager";
    private static final Account[] NO_ACCOUNTS = new Account[0];

    private static AccountManager sInstance;

    private final Context mContext;
    private final Map<OnAccountsUpdateListener, Handler> mListeners = new LinkedHashMap<>();

    public AccountManager(Context context)
    {
        mContext = context;
    }

    public static synchronized AccountManager get(Context context)
    {
        if (context == null) throw new IllegalArgumentException("context is null");

        Object service = context.getSystemService(Context.ACCOUNT_SERVICE);
        if (service instanceof AccountManager) return (AccountManager)service;

        if (sInstance == null) sInstance = new AccountManager(context.getApplicationContext());
        return sInstance;
    }

    public String getPassword(Account account)
    {
        requireAccount(account);
        return null;
    }

    public String getUserData(Account account, String key)
    {
        requireAccount(account);
        if (key == null) throw new IllegalArgumentException("key is null");

        return null;
    }

    public AuthenticatorDescription[] getAuthenticatorTypes()
    {
        return new AuthenticatorDescription[0];
    }

    public Account[] getAccounts()
    {
        return NO_ACCOUNTS;
    }

    public Account[] getAccountsByTypeForPackage(String type, String packageName)
    {
        return NO_ACCOUNTS;
    }

    public Account[] getAccountsByType(String type)
    {
        return NO_ACCOUNTS;
    }

    public AccountManagerFuture<Boolean> hasFeatures(Account account, String[] features, AccountManagerCallback<Boolean> callback,
                                                     Handler handler)
    {
        requireAccount(account);
        if (features == null) throw new IllegalArgumentException("features is null");

        return succeed(Boolean.FALSE, callback, handler);
    }

    public AccountManagerFuture<Account[]> getAccountsByTypeAndFeatures(String type, String[] features,
                                                                        AccountManagerCallback<Account[]> callback, Handler handler)
    {
        if (type == null) throw new IllegalArgumentException("type is null");

        return succeed(NO_ACCOUNTS, callback, handler);
    }

    public boolean addAccountExplicitly(Account account, String password, Bundle userdata)
    {
        requireAccount(account);
        Log.i(TAG, "no authenticator for " + account.type + " belongs to this package, so the account was not added");
        return false;
    }

    public boolean addAccountExplicitly(Account account, String password, Bundle extras, Map<String, Integer> visibility)
    {
        return addAccountExplicitly(account, password, extras);
    }

    public Map<String, Integer> getPackagesAndVisibilityForAccount(Account account)
    {
        requireAccount(account);
        return new HashMap<>();
    }

    public Map<Account, Integer> getAccountsAndVisibilityForPackage(String packageName, String accountType)
    {
        return new HashMap<>();
    }

    public boolean setAccountVisibility(Account account, String packageName, int visibility)
    {
        requireAccount(account);
        return false;
    }

    public int getAccountVisibility(Account account, String packageName)
    {
        requireAccount(account);
        return VISIBILITY_UNDEFINED;
    }

    public boolean notifyAccountAuthenticated(Account account)
    {
        requireAccount(account);
        return false;
    }

    public AccountManagerFuture<Account> renameAccount(Account account, String newName, AccountManagerCallback<Account> callback,
                                                       Handler handler)
    {
        requireAccount(account);
        return fail(noAuthenticator(account.type), callback, handler);
    }

    public String getPreviousName(Account account)
    {
        requireAccount(account);
        return null;
    }

    public AccountManagerFuture<Boolean> removeAccount(Account account, AccountManagerCallback<Boolean> callback, Handler handler)
    {
        requireAccount(account);
        return succeed(Boolean.FALSE, callback, handler);
    }

    public AccountManagerFuture<Bundle> removeAccount(Account account, Activity activity, AccountManagerCallback<Bundle> callback,
                                                      Handler handler)
    {
        requireAccount(account);

        Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return succeed(result, callback, handler);
    }

    public boolean removeAccountExplicitly(Account account)
    {
        requireAccount(account);
        return false;
    }

    public void invalidateAuthToken(String accountType, String authToken)
    {
        if (accountType == null) throw new IllegalArgumentException("accountType is null");
    }

    public String peekAuthToken(Account account, String authTokenType)
    {
        requireAccount(account);
        if (authTokenType == null) throw new IllegalArgumentException("authTokenType is null");

        return null;
    }

    public void setPassword(Account account, String password)
    {
        requireAccount(account);
    }

    public void clearPassword(Account account)
    {
        requireAccount(account);
    }

    public void setUserData(Account account, String key, String value)
    {
        requireAccount(account);
        if (key == null) throw new IllegalArgumentException("key is null");
    }

    public void setAuthToken(Account account, String authTokenType, String authToken)
    {
        requireAccount(account);
        if (authTokenType == null) throw new IllegalArgumentException("authTokenType is null");
    }

    public String blockingGetAuthToken(Account account, String authTokenType, boolean notifyAuthFailure)
        throws OperationCanceledException, java.io.IOException, AuthenticatorException
    {
        requireAccount(account);
        if (authTokenType == null) throw new IllegalArgumentException("authTokenType is null");

        throw noAuthenticator(account.type);
    }

    public AccountManagerFuture<Bundle> getAuthToken(Account account, String authTokenType, Bundle options, Activity activity,
                                                     AccountManagerCallback<Bundle> callback, Handler handler)
    {
        requireAccount(account);
        if (authTokenType == null) throw new IllegalArgumentException("authTokenType is null");

        return fail(noAuthenticator(account.type), callback, handler);
    }

    public AccountManagerFuture<Bundle> getAuthToken(Account account, String authTokenType, boolean notifyAuthFailure,
                                                     AccountManagerCallback<Bundle> callback, Handler handler)
    {
        return getAuthToken(account, authTokenType, null, notifyAuthFailure, callback, handler);
    }

    public AccountManagerFuture<Bundle> getAuthToken(Account account, String authTokenType, Bundle options, boolean notifyAuthFailure,
                                                     AccountManagerCallback<Bundle> callback, Handler handler)
    {
        return getAuthToken(account, authTokenType, options, (Activity)null, callback, handler);
    }

    public AccountManagerFuture<Bundle> addAccount(String accountType, String authTokenType, String[] requiredFeatures,
                                                   Bundle addAccountOptions, Activity activity, AccountManagerCallback<Bundle> callback,
                                                   Handler handler)
    {
        if (accountType == null) throw new IllegalArgumentException("accountType is null");

        return fail(noAuthenticator(accountType), callback, handler);
    }

    public AccountManagerFuture<Bundle> confirmCredentials(Account account, Bundle options, Activity activity,
                                                           AccountManagerCallback<Bundle> callback, Handler handler)
    {
        requireAccount(account);
        return fail(noAuthenticator(account.type), callback, handler);
    }

    public AccountManagerFuture<Bundle> updateCredentials(Account account, String authTokenType, Bundle options, Activity activity,
                                                          AccountManagerCallback<Bundle> callback, Handler handler)
    {
        requireAccount(account);
        return fail(noAuthenticator(account.type), callback, handler);
    }

    public AccountManagerFuture<Bundle> editProperties(String accountType, Activity activity, AccountManagerCallback<Bundle> callback,
                                                       Handler handler)
    {
        if (accountType == null) throw new IllegalArgumentException("accountType is null");

        return fail(noAuthenticator(accountType), callback, handler);
    }

    public AccountManagerFuture<Bundle> getAuthTokenByFeatures(String accountType, String authTokenType, String[] features,
                                                               Activity activity, Bundle addAccountOptions, Bundle getAuthTokenOptions,
                                                               AccountManagerCallback<Bundle> callback, Handler handler)
    {
        if (accountType == null) throw new IllegalArgumentException("account type is null");
        if (authTokenType == null) throw new IllegalArgumentException("authTokenType is null");

        return fail(noAuthenticator(accountType), callback, handler);
    }

    public static Intent newChooseAccountIntent(Account selectedAccount, ArrayList<Account> allowableAccounts,
                                                String[] allowableAccountTypes, boolean alwaysPromptForAccount,
                                                String descriptionOverrideText, String addAccountAuthTokenType,
                                                String[] addAccountRequiredFeatures, Bundle addAccountOptions)
    {
        return newChooseAccountIntent(selectedAccount, (List<Account>)allowableAccounts, allowableAccountTypes, descriptionOverrideText,
                                      addAccountAuthTokenType, addAccountRequiredFeatures, addAccountOptions);
    }

    public static Intent newChooseAccountIntent(Account selectedAccount, List<Account> allowableAccounts, String[] allowableAccountTypes,
                                                String descriptionOverrideText, String addAccountAuthTokenType,
                                                String[] addAccountRequiredFeatures, Bundle addAccountOptions)
    {
        Bundle extras = new Bundle();
        extras.putParcelableArrayList("allowableAccounts", allowableAccounts == null ? null : new ArrayList<>(allowableAccounts));
        extras.putStringArray("allowableAccountTypes", allowableAccountTypes);
        extras.putBundle("addAccountOptions", addAccountOptions);
        extras.putParcelable("selectedAccount", selectedAccount);
        extras.putString("descriptionTextOverride", descriptionOverrideText);
        extras.putString("authTokenType", addAccountAuthTokenType);
        extras.putStringArray("addAccountRequiredFeatures", addAccountRequiredFeatures);

        Intent intent = new Intent();
        intent.setClassName("android", "android.accounts.ChooseTypeAndAccountActivity");
        intent.putExtras(extras);
        return intent;
    }

    public void addOnAccountsUpdatedListener(OnAccountsUpdateListener listener, Handler handler, boolean updateImmediately)
    {
        addOnAccountsUpdatedListener(listener, handler, updateImmediately, null);
    }

    public void addOnAccountsUpdatedListener(final OnAccountsUpdateListener listener, Handler handler, boolean updateImmediately,
                                             String[] accountTypes)
    {
        if (listener == null) throw new IllegalArgumentException("the listener is null");

        synchronized (mListeners)
        {
            if (mListeners.containsKey(listener)) throw new IllegalStateException("this listener is already added");

            mListeners.put(listener, handler);
        }

        if (!updateImmediately) return;

        post(handler, new Runnable()
        {
            @Override
            public void run()
            {
                listener.onAccountsUpdated(NO_ACCOUNTS);
            }
        });
    }

    public void removeOnAccountsUpdatedListener(OnAccountsUpdateListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("listener is null");

        synchronized (mListeners)
        {
            if (mListeners.remove(listener) == null) Log.e(TAG, "the listener was not previously added");
        }
    }

    public AccountManagerFuture<Bundle> startAddAccountSession(String accountType, String authTokenType, String[] requiredFeatures,
                                                               Bundle options, Activity activity, AccountManagerCallback<Bundle> callback,
                                                               Handler handler)
    {
        if (accountType == null) throw new IllegalArgumentException("accountType is null");

        return fail(noAuthenticator(accountType), callback, handler);
    }

    public AccountManagerFuture<Bundle> startUpdateCredentialsSession(Account account, String authTokenType, Bundle options,
                                                                      Activity activity, AccountManagerCallback<Bundle> callback,
                                                                      Handler handler)
    {
        requireAccount(account);
        return fail(noAuthenticator(account.type), callback, handler);
    }

    public AccountManagerFuture<Bundle> finishSession(Bundle sessionBundle, Activity activity, AccountManagerCallback<Bundle> callback,
                                                      Handler handler)
    {
        if (sessionBundle == null) throw new IllegalArgumentException("sessionBundle is null");

        return fail(new AuthenticatorException("the account session cannot be finished without an authenticator"), callback, handler);
    }

    public AccountManagerFuture<Boolean> isCredentialsUpdateSuggested(Account account, String statusToken,
                                                                      AccountManagerCallback<Boolean> callback, Handler handler)
    {
        requireAccount(account);
        return succeed(Boolean.FALSE, callback, handler);
    }

    private static void requireAccount(Account account)
    {
        if (account == null) throw new IllegalArgumentException("account is null");
    }

    private static AuthenticatorException noAuthenticator(String accountType)
    {
        return new AuthenticatorException("no authenticator is registered for the account type " + accountType);
    }

    private static void post(Handler handler, Runnable runnable)
    {
        if (handler != null)
        {
            handler.post(runnable);
            return;
        }

        Looper main = Looper.getMainLooper();
        if (main != null) new Handler(main).post(runnable);
        else runnable.run();
    }

    private static <V> AccountManagerFuture<V> succeed(V value, AccountManagerCallback<V> callback, Handler handler)
    {
        return deliver(new Outcome<>(value, null), callback, handler);
    }

    private static <V> AccountManagerFuture<V> fail(AccountsException failure, AccountManagerCallback<V> callback, Handler handler)
    {
        return deliver(new Outcome<V>(null, failure), callback, handler);
    }

    private static <V> AccountManagerFuture<V> deliver(final Outcome<V> outcome, final AccountManagerCallback<V> callback,
                                                       Handler handler)
    {
        if (callback == null) return outcome;

        post(handler, new Runnable()
        {
            @Override
            public void run()
            {
                callback.run(outcome);
            }
        });

        return outcome;
    }

    private static final class Outcome<V> implements AccountManagerFuture<V>
    {
        private final V mValue;
        private final AccountsException mFailure;

        Outcome(V value, AccountsException failure)
        {
            mValue = value;
            mFailure = failure;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return false;
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public boolean isDone()
        {
            return true;
        }

        @Override
        public V getResult() throws AuthenticatorException, java.io.IOException, OperationCanceledException
        {
            if (mFailure instanceof AuthenticatorException) throw (AuthenticatorException)mFailure;
            if (mFailure instanceof OperationCanceledException) throw (OperationCanceledException)mFailure;
            if (mFailure != null) throw new AuthenticatorException(mFailure);

            return mValue;
        }

        @Override
        public V getResult(long timeout, TimeUnit unit) throws AuthenticatorException, java.io.IOException, OperationCanceledException
        {
            return getResult();
        }
    }
}
