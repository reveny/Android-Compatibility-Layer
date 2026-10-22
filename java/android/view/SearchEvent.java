package android.view;

public class SearchEvent
{
    private final InputDevice mInputDevice;

    public SearchEvent(InputDevice inputDevice)
    {
        mInputDevice = inputDevice;
    }

    public InputDevice getInputDevice()
    {
        return mInputDevice;
    }
}
