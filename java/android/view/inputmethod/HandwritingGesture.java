package android.view.inputmethod;

public abstract class HandwritingGesture
{
    public static final int GRANULARITY_WORD = 1;
    public static final int GRANULARITY_CHARACTER = 2;

    static final int GESTURE_TYPE_NONE = 0x0000;
    static final int GESTURE_TYPE_SELECT = 0x0001;
    static final int GESTURE_TYPE_INSERT = 1 << 1;
    static final int GESTURE_TYPE_DELETE = 1 << 2;
    static final int GESTURE_TYPE_REMOVE_SPACE = 1 << 3;
    static final int GESTURE_TYPE_JOIN_OR_SPLIT = 1 << 4;
    static final int GESTURE_TYPE_SELECT_RANGE = 1 << 5;
    static final int GESTURE_TYPE_DELETE_RANGE = 1 << 6;
    static final int GESTURE_TYPE_INSERT_MODE = 1 << 7;

    int mType = GESTURE_TYPE_NONE;
    String mFallbackText;

    HandwritingGesture()
    {
    }

    public final String getFallbackText()
    {
        return mFallbackText;
    }

    int getGestureType()
    {
        return mType;
    }

    static boolean same(Object first, Object second)
    {
        return first == null ? second == null : first.equals(second);
    }
}
