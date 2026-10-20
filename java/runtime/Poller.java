package runtime;

public interface Poller
{
    void poll(long timeoutMillis);

    void wake();
}
