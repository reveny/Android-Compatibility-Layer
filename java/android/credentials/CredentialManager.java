package android.credentials;

import android.content.ComponentName;
import android.content.Context;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.Executor;

public final class CredentialManager
{
    private static final String TAG = "CredentialManager";

    private final Context mContext;

    public CredentialManager(Context context)
    {
        mContext = context;
    }

    public void getCredential(Context context, GetCredentialRequest request, CancellationSignal cancellationSignal, Executor executor,
                              OutcomeReceiver<GetCredentialResponse, GetCredentialException> callback)
    {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        if (alreadyCanceled(cancellationSignal)) return;

        reportNoCredential(executor, callback);
    }

    public void getCredential(Context context, PrepareGetCredentialResponse.PendingGetCredentialHandle pendingGetCredentialHandle,
                              CancellationSignal cancellationSignal, Executor executor,
                              OutcomeReceiver<GetCredentialResponse, GetCredentialException> callback)
    {
        Objects.requireNonNull(pendingGetCredentialHandle, "pendingGetCredentialHandle must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        if (alreadyCanceled(cancellationSignal)) return;

        reportNoCredential(executor, callback);
    }

    public void prepareGetCredential(final GetCredentialRequest request, CancellationSignal cancellationSignal, Executor executor,
                                     final OutcomeReceiver<PrepareGetCredentialResponse, GetCredentialException> callback)
    {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        if (alreadyCanceled(cancellationSignal)) return;

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onResult(new PrepareGetCredentialResponse(request));
            }
        });
    }

    public void createCredential(Context context, CreateCredentialRequest request, CancellationSignal cancellationSignal, Executor executor,
                                 final OutcomeReceiver<CreateCredentialResponse, CreateCredentialException> callback)
    {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        if (alreadyCanceled(cancellationSignal)) return;

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onError(new CreateCredentialException(CreateCredentialException.TYPE_NO_CREATE_OPTIONS,
                                                               "no credential provider is installed to save the credential"));
            }
        });
    }

    public void clearCredentialState(ClearCredentialStateRequest request, CancellationSignal cancellationSignal, Executor executor,
                                     final OutcomeReceiver<Void, ClearCredentialStateException> callback)
    {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        if (alreadyCanceled(cancellationSignal)) return;

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onResult(null);
            }
        });
    }

    public boolean isEnabledCredentialProviderService(ComponentName componentName)
    {
        return false;
    }

    private static boolean alreadyCanceled(CancellationSignal cancellationSignal)
    {
        if (cancellationSignal == null || !cancellationSignal.isCanceled()) return false;

        Log.w(TAG, "the request was already canceled, so nothing is asked");
        return true;
    }

    private static void reportNoCredential(Executor executor, final OutcomeReceiver<GetCredentialResponse, GetCredentialException> callback)
    {
        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onError(new GetCredentialException(GetCredentialException.TYPE_NO_CREDENTIAL, "no credential provider holds a credential"));
            }
        });
    }
}
