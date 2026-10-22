package android.view;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public final class InputDevice implements Parcelable
{
    public static final int SOURCE_CLASS_MASK = 0x000000ff;
    public static final int SOURCE_CLASS_NONE = 0x00000000;
    public static final int SOURCE_CLASS_BUTTON = 0x00000001;
    public static final int SOURCE_CLASS_POINTER = 0x00000002;
    public static final int SOURCE_CLASS_TRACKBALL = 0x00000004;
    public static final int SOURCE_CLASS_POSITION = 0x00000008;
    public static final int SOURCE_CLASS_JOYSTICK = 0x00000010;
    public static final int SOURCE_UNKNOWN = 0x00000000;
    public static final int SOURCE_KEYBOARD = 0x00000100 | SOURCE_CLASS_BUTTON;
    public static final int SOURCE_DPAD = 0x00000200 | SOURCE_CLASS_BUTTON;
    public static final int SOURCE_GAMEPAD = 0x00000400 | SOURCE_CLASS_BUTTON;
    public static final int SOURCE_TOUCHSCREEN = 0x00001000 | SOURCE_CLASS_POINTER;
    public static final int SOURCE_MOUSE = 0x00002000 | SOURCE_CLASS_POINTER;
    public static final int SOURCE_STYLUS = 0x00004000 | SOURCE_CLASS_POINTER;
    public static final int SOURCE_BLUETOOTH_STYLUS = 0x00008000 | SOURCE_STYLUS;
    public static final int SOURCE_TRACKBALL = 0x00010000 | SOURCE_CLASS_TRACKBALL;
    public static final int SOURCE_MOUSE_RELATIVE = 0x00020000 | SOURCE_CLASS_TRACKBALL;
    public static final int SOURCE_TOUCHPAD = 0x00100000 | SOURCE_CLASS_POSITION;
    public static final int SOURCE_TOUCH_NAVIGATION = 0x00200000 | SOURCE_CLASS_NONE;
    public static final int SOURCE_ROTARY_ENCODER = 0x00400000 | SOURCE_CLASS_NONE;
    public static final int SOURCE_JOYSTICK = 0x01000000 | SOURCE_CLASS_JOYSTICK;
    public static final int SOURCE_HDMI = 0x02000000 | SOURCE_CLASS_BUTTON;
    public static final int SOURCE_SENSOR = 0x04000000 | SOURCE_CLASS_NONE;
    public static final int SOURCE_ANY = 0xffffff00;

    @Deprecated
    public static final int MOTION_RANGE_X = MotionEvent.AXIS_X;
    @Deprecated
    public static final int MOTION_RANGE_Y = MotionEvent.AXIS_Y;
    @Deprecated
    public static final int MOTION_RANGE_PRESSURE = MotionEvent.AXIS_PRESSURE;
    @Deprecated
    public static final int MOTION_RANGE_SIZE = MotionEvent.AXIS_SIZE;
    @Deprecated
    public static final int MOTION_RANGE_TOUCH_MAJOR = MotionEvent.AXIS_TOUCH_MAJOR;
    @Deprecated
    public static final int MOTION_RANGE_TOUCH_MINOR = MotionEvent.AXIS_TOUCH_MINOR;
    @Deprecated
    public static final int MOTION_RANGE_TOOL_MAJOR = MotionEvent.AXIS_TOOL_MAJOR;
    @Deprecated
    public static final int MOTION_RANGE_TOOL_MINOR = MotionEvent.AXIS_TOOL_MINOR;
    @Deprecated
    public static final int MOTION_RANGE_ORIENTATION = MotionEvent.AXIS_ORIENTATION;

    public static final int KEYBOARD_TYPE_NONE = 0;
    public static final int KEYBOARD_TYPE_NON_ALPHABETIC = 1;
    public static final int KEYBOARD_TYPE_ALPHABETIC = 2;

    private static final int TOUCHSCREEN_ID = 1;
    private static final int KEYBOARD_ID = 2;
    private static final int MOUSE_ID = 3;

    public static final Parcelable.Creator<InputDevice> CREATOR = new Parcelable.Creator<InputDevice>()
    {
        @Override
        public InputDevice createFromParcel(Parcel source)
        {
            int id = source.readInt();
            String name = source.readString();
            int sources = source.readInt();
            int keyboardType = source.readInt();

            InputDevice known = getDevice(id);
            if (known != null) return known;

            return new InputDevice(id, name, sources, keyboardType);
        }

        @Override
        public InputDevice[] newArray(int size)
        {
            return new InputDevice[size];
        }
    };

    private static final InputDevice[] DEVICES = {
        new InputDevice(TOUCHSCREEN_ID, "acl-touchscreen", SOURCE_TOUCHSCREEN, KEYBOARD_TYPE_NONE),
        new InputDevice(KEYBOARD_ID, "acl-keyboard", SOURCE_KEYBOARD | SOURCE_DPAD, KEYBOARD_TYPE_ALPHABETIC),
        new InputDevice(MOUSE_ID, "acl-mouse", SOURCE_MOUSE, KEYBOARD_TYPE_NONE),
    };

    public static final class ViewBehavior
    {
        private final InputDevice mInputDevice;
        private boolean mShouldSmoothScroll;

        ViewBehavior(InputDevice device)
        {
            mInputDevice = device;
        }

        public boolean shouldSmoothScroll(int axis, int source)
        {
            if (axis != MotionEvent.AXIS_SCROLL && axis != MotionEvent.AXIS_HSCROLL && axis != MotionEvent.AXIS_VSCROLL) return false;
            if (mInputDevice != null && !mInputDevice.supportsSource(source)) return false;

            return mShouldSmoothScroll;
        }
    }

    public static final class MotionRange
    {
        private final int mAxis;
        private final int mSource;
        private final float mMin;
        private final float mMax;

        MotionRange(int axis, int source, float min, float max)
        {
            mAxis = axis;
            mSource = source;
            mMin = min;
            mMax = max;
        }

        public int getAxis()
        {
            return mAxis;
        }

        public int getSource()
        {
            return mSource;
        }

        public boolean isFromSource(int source)
        {
            return (mSource & source) == source;
        }

        public float getMin()
        {
            return mMin;
        }

        public float getMax()
        {
            return mMax;
        }

        public float getRange()
        {
            return mMax - mMin;
        }

        public float getFlat()
        {
            return 0f;
        }

        public float getFuzz()
        {
            return 0f;
        }

        public float getResolution()
        {
            return 0f;
        }
    }

    private final int mId;
    private final String mName;
    private final int mSources;
    private final int mKeyboardType;
    private final List<MotionRange> mRanges = new ArrayList<>();
    private android.os.Vibrator mVibrator;
    private android.os.VibratorManager mVibratorManager;

    private InputDevice(int id, String name, int sources, int keyboardType)
    {
        mId = id;
        mName = name;
        mSources = sources;
        mKeyboardType = keyboardType;

        if ((sources & SOURCE_CLASS_POINTER) != 0)
        {
            mRanges.add(new MotionRange(MotionEvent.AXIS_X, sources, 0f, 4096f));
            mRanges.add(new MotionRange(MotionEvent.AXIS_Y, sources, 0f, 4096f));
        }
    }

    public static InputDevice getDevice(int id)
    {
        for (InputDevice device : DEVICES)
        {
            if (device.mId == id) return device;
        }

        return null;
    }

    public static int[] getDeviceIds()
    {
        int[] ids = new int[DEVICES.length];
        for (int index = 0; index < DEVICES.length; index++) ids[index] = DEVICES[index].mId;

        return ids;
    }

    public int getId()
    {
        return mId;
    }

    public int getControllerNumber()
    {
        return 0;
    }

    public int getVendorId()
    {
        return 0;
    }

    public int getProductId()
    {
        return 0;
    }

    public String getDescriptor()
    {
        return mName;
    }

    public boolean isVirtual()
    {
        return false;
    }

    public boolean isExternal()
    {
        return mId != TOUCHSCREEN_ID;
    }

    public String getName()
    {
        return mName;
    }

    public int getSources()
    {
        return mSources;
    }

    public boolean supportsSource(int source)
    {
        return (mSources & source) == source;
    }

    public int getKeyboardType()
    {
        return mKeyboardType;
    }

    public boolean[] hasKeys(int... keys)
    {
        boolean[] present = new boolean[keys.length];
        for (int index = 0; index < keys.length; index++) present[index] = mKeyboardType != KEYBOARD_TYPE_NONE;

        return present;
    }

    public KeyCharacterMap getKeyCharacterMap()
    {
        return KeyCharacterMap.load(mId);
    }

    public android.os.Vibrator getVibrator()
    {
        synchronized (this)
        {
            if (mVibrator == null) mVibrator = new android.os.Vibrator();
            return mVibrator;
        }
    }

    public android.os.VibratorManager getVibratorManager()
    {
        synchronized (this)
        {
            if (mVibratorManager == null) mVibratorManager = new android.os.VibratorManager(null);
            return mVibratorManager;
        }
    }

    public int getKeyCodeForKeyLocation(int locationKeyCode)
    {
        return locationKeyCode;
    }

    public MotionRange getMotionRange(int axis)
    {
        for (MotionRange range : mRanges)
        {
            if (range.getAxis() == axis) return range;
        }

        return null;
    }

    public MotionRange getMotionRange(int axis, int source)
    {
        for (MotionRange range : mRanges)
        {
            if (range.getAxis() == axis && range.isFromSource(source)) return range;
        }

        return null;
    }

    public List<MotionRange> getMotionRanges()
    {
        return mRanges;
    }

    public boolean isEnabled()
    {
        return true;
    }

    public boolean hasMicrophone()
    {
        return false;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mId);
        dest.writeString(mName);
        dest.writeInt(mSources);
        dest.writeInt(mKeyboardType);
    }

    @Override
    public String toString()
    {
        return "Input Device " + mId + ": " + mName;
    }
}
