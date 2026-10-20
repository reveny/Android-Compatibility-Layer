package android.view;

public class KeyEvent extends InputEvent
{
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MULTIPLE = 2;

    public static final int KEYCODE_UNKNOWN = 0;
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_DPAD_UP = 19;
    public static final int KEYCODE_DPAD_DOWN = 20;
    public static final int KEYCODE_DPAD_LEFT = 21;
    public static final int KEYCODE_DPAD_RIGHT = 22;
    public static final int KEYCODE_DPAD_CENTER = 23;
    public static final int KEYCODE_ENTER = 66;
    public static final int KEYCODE_DEL = 67;
    public static final int KEYCODE_FORWARD_DEL = 112;

    public static final int META_SHIFT_ON = 0x1;
    public static final int META_ALT_ON = 0x2;
    public static final int META_CTRL_ON = 0x1000;

    private final int mAction;
    private final int mKeyCode;
    private final int mRepeat;
    private final int mMetaState;
    private final long mDownTime;
    private final long mEventTime;
    private final int mScanCode;
    private final int mFlags;
    private final int mSource;
    private final int mDeviceId;

    public KeyEvent(int action, int code)
    {
        this(0L, 0L, action, code, 0, 0, -1, 0, 0, 0x101);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat)
    {
        this(downTime, eventTime, action, code, repeat, 0, -1, 0, 0, 0x101);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState)
    {
        this(downTime, eventTime, action, code, repeat, metaState, -1, 0, 0, 0x101);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState, int deviceId, int scanCode,
                    int flags, int source)
    {
        mDownTime = downTime;
        mEventTime = eventTime;
        mAction = action;
        mKeyCode = code;
        mRepeat = repeat;
        mMetaState = metaState;
        mDeviceId = deviceId;
        mScanCode = scanCode;
        mFlags = flags;
        mSource = source;
    }

    public final int getAction()
    {
        return mAction;
    }

    public final int getKeyCode()
    {
        return mKeyCode;
    }

    public final int getRepeatCount()
    {
        return mRepeat;
    }

    public final int getMetaState()
    {
        return mMetaState;
    }

    public final long getDownTime()
    {
        return mDownTime;
    }

    public final long getEventTime()
    {
        return mEventTime;
    }

    public final int getScanCode()
    {
        return mScanCode;
    }

    public final int getFlags()
    {
        return mFlags;
    }

    public final int getSource()
    {
        return mSource;
    }

    public final int getDeviceId()
    {
        return mDeviceId;
    }

    public final int getUnicodeChar()
    {
        return getUnicodeChar(mMetaState);
    }

    public final int getUnicodeChar(int metaState)
    {
        if (mKeyCode >= 7 && mKeyCode <= 16) return '0' + (mKeyCode - 7);
        if (mKeyCode >= 29 && mKeyCode <= 54)
        {
            int letter = 'a' + (mKeyCode - 29);
            return (metaState & META_SHIFT_ON) != 0 ? Character.toUpperCase(letter) : letter;
        }

        if (mKeyCode == 62) return ' ';
        return 0;
    }

    public final int getModifiers()
    {
        return mMetaState;
    }

    public final boolean isShiftPressed()
    {
        return (mMetaState & META_SHIFT_ON) != 0;
    }

    public final boolean isAltPressed()
    {
        return (mMetaState & META_ALT_ON) != 0;
    }

    public final boolean isCtrlPressed()
    {
        return (mMetaState & META_CTRL_ON) != 0;
    }

    @Override
    public String toString()
    {
        return "KeyEvent(action=" + mAction + " code=" + mKeyCode + ")";
    }
}
