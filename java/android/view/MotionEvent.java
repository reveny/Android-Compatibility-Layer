package android.view;

public final class MotionEvent extends InputEvent
{
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_CANCEL = 3;
    public static final int ACTION_OUTSIDE = 4;
    public static final int ACTION_POINTER_DOWN = 5;
    public static final int ACTION_POINTER_UP = 6;
    public static final int ACTION_HOVER_MOVE = 7;
    public static final int ACTION_SCROLL = 8;
    public static final int ACTION_HOVER_ENTER = 9;
    public static final int ACTION_HOVER_EXIT = 10;

    public static final int ACTION_MASK = 0x000000ff;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_PRESSURE = 2;
    public static final int AXIS_SIZE = 3;
    public static final int AXIS_VSCROLL = 9;
    public static final int AXIS_HSCROLL = 10;

    public static final int TOOL_TYPE_UNKNOWN = 0;
    public static final int TOOL_TYPE_FINGER = 1;
    public static final int TOOL_TYPE_STYLUS = 2;
    public static final int TOOL_TYPE_MOUSE = 3;

    private final long mDownTime;
    private final long mEventTime;
    private final int mAction;
    private final int mMetaState;
    private final int mDeviceId;
    private final int mSource;
    private final int mPointerCount;
    private final int[] mPointerIds;
    private final float[] mX;
    private final float[] mY;
    private final float[] mPressure;
    private final float[] mRawX;
    private final float[] mRawY;

    private MotionEvent(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, float[] x, float[] y,
                        float[] pressure, int metaState, int deviceId, int source)
    {
        mDownTime = downTime;
        mEventTime = eventTime;
        mAction = action;
        mPointerCount = pointerCount;
        mPointerIds = pointerIds;
        mX = x;
        mY = y;
        mPressure = pressure;
        mMetaState = metaState;
        mDeviceId = deviceId;
        mSource = source;
        mRawX = x.clone();
        mRawY = y.clone();
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, float x, float y, int metaState)
    {
        return new MotionEvent(downTime, eventTime, action, 1, new int[] {0}, new float[] {x}, new float[] {y},
                               new float[] {1.0f}, metaState, 0, 0x1002);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, float[] x,
                                     float[] y, float[] pressure, int metaState, int deviceId, int source)
    {
        return new MotionEvent(downTime, eventTime, action, pointerCount, pointerIds, x, y, pressure, metaState, deviceId, source);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount,
                                     PointerProperties[] pointerProperties, PointerCoords[] pointerCoords, int metaState,
                                     int buttonState, float xPrecision, float yPrecision, int deviceId, int edgeFlags,
                                     int source, int flags)
    {
        int[] ids = new int[pointerCount];
        float[] x = new float[pointerCount];
        float[] y = new float[pointerCount];
        float[] pressure = new float[pointerCount];

        for (int index = 0; index < pointerCount; index++)
        {
            ids[index] = pointerProperties == null ? index : pointerProperties[index].id;
            x[index] = pointerCoords == null ? 0 : pointerCoords[index].x;
            y[index] = pointerCoords == null ? 0 : pointerCoords[index].y;
            pressure[index] = pointerCoords == null ? 1.0f : pointerCoords[index].pressure;
        }

        return new MotionEvent(downTime, eventTime, action, pointerCount, ids, x, y, pressure, metaState, deviceId, source);
    }

    public static class PointerProperties
    {
        public int id;
        public int toolType = TOOL_TYPE_FINGER;

        public PointerProperties()
        {
        }

        public PointerProperties(PointerProperties other)
        {
            copyFrom(other);
        }

        public void copyFrom(PointerProperties other)
        {
            if (other == null) return;

            id = other.id;
            toolType = other.toolType;
        }

        public void clear()
        {
            id = 0;
            toolType = TOOL_TYPE_UNKNOWN;
        }
    }

    public static class PointerCoords
    {
        public float x;
        public float y;
        public float pressure = 1.0f;
        public float size;
        public float touchMajor;
        public float touchMinor;
        public float toolMajor;
        public float toolMinor;
        public float orientation;

        public PointerCoords()
        {
        }

        public PointerCoords(PointerCoords other)
        {
            copyFrom(other);
        }

        public void copyFrom(PointerCoords other)
        {
            if (other == null) return;

            x = other.x;
            y = other.y;
            pressure = other.pressure;
            size = other.size;
        }

        public void clear()
        {
            x = 0;
            y = 0;
            pressure = 0;
            size = 0;
        }

        public float getAxisValue(int axis)
        {
            if (axis == AXIS_X) return x;
            if (axis == AXIS_Y) return y;
            if (axis == AXIS_PRESSURE) return pressure;
            if (axis == AXIS_SIZE) return size;

            return 0;
        }

        public void setAxisValue(int axis, float value)
        {
            if (axis == AXIS_X) x = value;
            else if (axis == AXIS_Y) y = value;
            else if (axis == AXIS_PRESSURE) pressure = value;
            else if (axis == AXIS_SIZE) size = value;
        }
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, float[] x,
                                     float[] y, int metaState)
    {
        int[] ids = new int[pointerCount];
        float[] copiedX = new float[pointerCount];
        float[] copiedY = new float[pointerCount];
        float[] pressure = new float[pointerCount];

        for (int index = 0; index < pointerCount; index++)
        {
            ids[index] = pointerIds[index];
            copiedX[index] = x[index];
            copiedY[index] = y[index];
            pressure[index] = 1.0f;
        }

        return new MotionEvent(downTime, eventTime, action, pointerCount, ids, copiedX, copiedY, pressure, metaState, 0,
                               0x1002);
    }

    public static MotionEvent obtain(MotionEvent other)
    {
        if (other == null) return null;

        return new MotionEvent(other.mDownTime, other.mEventTime, other.mAction, other.mPointerCount,
                               other.mPointerIds.clone(), other.mX.clone(), other.mY.clone(), other.mPressure.clone(),
                               other.mMetaState, other.mDeviceId, other.mSource);
    }

    public static MotionEvent obtainNoHistory(MotionEvent other)
    {
        return obtain(other);
    }

    public final void getPointerProperties(int pointerIndex, PointerProperties into)
    {
        if (into == null) return;

        into.id = mPointerIds[pointerIndex];
        into.toolType = TOOL_TYPE_FINGER;
    }

    public final void getPointerCoords(int pointerIndex, PointerCoords into)
    {
        if (into == null) return;

        into.x = mX[pointerIndex];
        into.y = mY[pointerIndex];
        into.pressure = mPressure[pointerIndex];
    }

    public final void getHistoricalPointerCoords(int pointerIndex, int position, PointerCoords into)
    {
        getPointerCoords(pointerIndex, into);
    }

    public final int findPointerIndex(int pointerId)
    {
        for (int index = 0; index < mPointerCount; index++)
        {
            if (mPointerIds[index] == pointerId) return index;
        }

        return -1;
    }

    public void recycle()
    {
    }

    public final void offsetLocation(float deltaX, float deltaY)
    {
        for (int index = 0; index < mPointerCount; index++)
        {
            mX[index] += deltaX;
            mY[index] += deltaY;
        }
    }

    public final void setLocation(float x, float y)
    {
        offsetLocation(x - mX[0], y - mY[0]);
    }

    public final int getAction()
    {
        return mAction;
    }

    public final int getActionMasked()
    {
        return mAction & ACTION_MASK;
    }

    public final int getActionIndex()
    {
        return (mAction & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    public final long getDownTime()
    {
        return mDownTime;
    }

    public final long getEventTime()
    {
        return mEventTime;
    }

    public final int getMetaState()
    {
        return mMetaState;
    }

    public final int getDeviceId()
    {
        return mDeviceId;
    }

    public final int getSource()
    {
        return mSource;
    }

    public final int getPointerCount()
    {
        return mPointerCount;
    }

    public final int getPointerId(int pointerIndex)
    {
        return mPointerIds[pointerIndex];
    }

    public final int getToolType(int pointerIndex)
    {
        return TOOL_TYPE_FINGER;
    }

    public final float getX()
    {
        return mX[0];
    }

    public final float getY()
    {
        return mY[0];
    }

    public final float getX(int pointerIndex)
    {
        return mX[pointerIndex];
    }

    public final float getY(int pointerIndex)
    {
        return mY[pointerIndex];
    }

    public final float getRawX()
    {
        return mRawX[0];
    }

    public final float getRawY()
    {
        return mRawY[0];
    }

    public final float getRawX(int pointerIndex)
    {
        return mRawX[pointerIndex];
    }

    public final float getRawY(int pointerIndex)
    {
        return mRawY[pointerIndex];
    }

    public final float getPressure()
    {
        return mPressure[0];
    }

    public final float getPressure(int pointerIndex)
    {
        return mPressure[pointerIndex];
    }

    public final float getAxisValue(int axis)
    {
        return getAxisValue(axis, 0);
    }

    public final float getAxisValue(int axis, int pointerIndex)
    {
        if (axis == AXIS_X) return mX[pointerIndex];
        if (axis == AXIS_Y) return mY[pointerIndex];
        if (axis == AXIS_PRESSURE) return mPressure[pointerIndex];

        return 0.0f;
    }

    public final int getHistorySize()
    {
        return 0;
    }

    public final int getFlags()
    {
        return 0;
    }

    public final float getXPrecision()
    {
        return 1.0f;
    }

    public final float getYPrecision()
    {
        return 1.0f;
    }

    public final int getEdgeFlags()
    {
        return 0;
    }

    public final int getButtonState()
    {
        return 0;
    }

    public final int getActionButton()
    {
        return 0;
    }

    public final int getClassification()
    {
        return 0;
    }

    public final float getSize()
    {
        return getSize(0);
    }

    public final float getSize(int pointerIndex)
    {
        return 0.0f;
    }

    public final float getTouchMajor()
    {
        return getTouchMajor(0);
    }

    public final float getTouchMajor(int pointerIndex)
    {
        return 0.0f;
    }

    public final float getTouchMinor()
    {
        return getTouchMinor(0);
    }

    public final float getTouchMinor(int pointerIndex)
    {
        return 0.0f;
    }

    public final float getToolMajor()
    {
        return getToolMajor(0);
    }

    public final float getToolMajor(int pointerIndex)
    {
        return 0.0f;
    }

    public final float getToolMinor()
    {
        return getToolMinor(0);
    }

    public final float getToolMinor(int pointerIndex)
    {
        return 0.0f;
    }

    public final float getOrientation()
    {
        return getOrientation(0);
    }

    public final float getOrientation(int pointerIndex)
    {
        return 0.0f;
    }

    public final long getHistoricalEventTime(int position)
    {
        return mEventTime;
    }

    public final float getHistoricalX(int pointerIndex, int position)
    {
        return getX(pointerIndex);
    }

    public final float getHistoricalY(int pointerIndex, int position)
    {
        return getY(pointerIndex);
    }

    public final float getHistoricalAxisValue(int axis, int pointerIndex, int position)
    {
        return getAxisValue(axis, pointerIndex);
    }

    @Override
    public String toString()
    {
        return "MotionEvent(action=" + mAction + " pointers=" + mPointerCount + ")";
    }
}
