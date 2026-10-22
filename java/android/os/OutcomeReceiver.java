package android.os;

public interface OutcomeReceiver<R, E extends Throwable>
{
    void onResult(R result);

    default void onError(E error)
    {
    }
}
