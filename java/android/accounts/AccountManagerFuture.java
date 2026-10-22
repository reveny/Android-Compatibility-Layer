package android.accounts;

public interface AccountManagerFuture<V>
{
    boolean cancel(boolean mayInterruptIfRunning);

    boolean isCancelled();

    boolean isDone();

    V getResult() throws AuthenticatorException, java.io.IOException, OperationCanceledException;

    V getResult(long timeout, java.util.concurrent.TimeUnit unit)
        throws AuthenticatorException, java.io.IOException, OperationCanceledException;
}
