package android.view;

public abstract class InputEvent
{
    public abstract int getDeviceId();

    public abstract int getSource();

    public abstract long getEventTime();

    public boolean isFromSource(int source)
    {
        return (getSource() & source) == source;
    }

    public void recycle()
    {
    }
}
