package android.hardware.input;

import android.content.Context;
import android.os.Handler;
import android.view.Display;
import android.view.InputDevice;

import java.util.ArrayList;
import java.util.List;

public final class InputManager
{
    public static final String ACTION_QUERY_KEYBOARD_LAYOUTS = "android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS";
    public static final String META_DATA_KEYBOARD_LAYOUTS = "android.hardware.input.metadata.KEYBOARD_LAYOUTS";

    private final List<InputDeviceListener> mListeners = new ArrayList<>();

    public InputManager(Context context)
    {
    }

    public InputDevice getInputDevice(int id)
    {
        return InputDevice.getDevice(id);
    }

    public int[] getInputDeviceIds()
    {
        return InputDevice.getDeviceIds();
    }

    public HostUsiVersion getHostUsiVersion(Display display)
    {
        if (display == null) throw new NullPointerException("display");
        return null;
    }

    public float getMaximumObscuringOpacityForTouch()
    {
        return 0.8f;
    }

    public boolean isStylusPointerIconEnabled()
    {
        return false;
    }

    public void registerInputDeviceListener(InputDeviceListener listener, Handler handler)
    {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");

        synchronized (mListeners)
        {
            if (!mListeners.contains(listener)) mListeners.add(listener);
        }
    }

    public void unregisterInputDeviceListener(InputDeviceListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");

        synchronized (mListeners)
        {
            mListeners.remove(listener);
        }
    }

    public interface InputDeviceListener
    {
        void onInputDeviceAdded(int deviceId);

        void onInputDeviceRemoved(int deviceId);

        void onInputDeviceChanged(int deviceId);
    }
}
