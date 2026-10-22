package android.view;

import android.graphics.Matrix;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

public final class MotionEvent extends InputEvent implements Parcelable
{
    public static final int INVALID_POINTER_ID = -1;

    public static final int ACTION_MASK = 0xff;
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
    public static final int ACTION_BUTTON_PRESS = 11;
    public static final int ACTION_BUTTON_RELEASE = 12;

    public static final int ACTION_POINTER_INDEX_MASK = 0xff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    @Deprecated
    public static final int ACTION_POINTER_1_DOWN = ACTION_POINTER_DOWN | 0x0000;
    @Deprecated
    public static final int ACTION_POINTER_2_DOWN = ACTION_POINTER_DOWN | 0x0100;
    @Deprecated
    public static final int ACTION_POINTER_3_DOWN = ACTION_POINTER_DOWN | 0x0200;
    @Deprecated
    public static final int ACTION_POINTER_1_UP = ACTION_POINTER_UP | 0x0000;
    @Deprecated
    public static final int ACTION_POINTER_2_UP = ACTION_POINTER_UP | 0x0100;
    @Deprecated
    public static final int ACTION_POINTER_3_UP = ACTION_POINTER_UP | 0x0200;
    @Deprecated
    public static final int ACTION_POINTER_ID_MASK = 0xff00;
    @Deprecated
    public static final int ACTION_POINTER_ID_SHIFT = 8;

    public static final int FLAG_WINDOW_IS_OBSCURED = 0x1;
    public static final int FLAG_WINDOW_IS_PARTIALLY_OBSCURED = 0x2;
    public static final int FLAG_CANCELED = 0x20;

    public static final int EDGE_TOP = 0x00000001;
    public static final int EDGE_BOTTOM = 0x00000002;
    public static final int EDGE_LEFT = 0x00000004;
    public static final int EDGE_RIGHT = 0x00000008;

    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_PRESSURE = 2;
    public static final int AXIS_SIZE = 3;
    public static final int AXIS_TOUCH_MAJOR = 4;
    public static final int AXIS_TOUCH_MINOR = 5;
    public static final int AXIS_TOOL_MAJOR = 6;
    public static final int AXIS_TOOL_MINOR = 7;
    public static final int AXIS_ORIENTATION = 8;
    public static final int AXIS_VSCROLL = 9;
    public static final int AXIS_HSCROLL = 10;
    public static final int AXIS_Z = 11;
    public static final int AXIS_RX = 12;
    public static final int AXIS_RY = 13;
    public static final int AXIS_RZ = 14;
    public static final int AXIS_HAT_X = 15;
    public static final int AXIS_HAT_Y = 16;
    public static final int AXIS_LTRIGGER = 17;
    public static final int AXIS_RTRIGGER = 18;
    public static final int AXIS_THROTTLE = 19;
    public static final int AXIS_RUDDER = 20;
    public static final int AXIS_WHEEL = 21;
    public static final int AXIS_GAS = 22;
    public static final int AXIS_BRAKE = 23;
    public static final int AXIS_DISTANCE = 24;
    public static final int AXIS_TILT = 25;
    public static final int AXIS_SCROLL = 26;
    public static final int AXIS_RELATIVE_X = 27;
    public static final int AXIS_RELATIVE_Y = 28;
    public static final int AXIS_GENERIC_1 = 32;
    public static final int AXIS_GENERIC_2 = 33;
    public static final int AXIS_GENERIC_3 = 34;
    public static final int AXIS_GENERIC_4 = 35;
    public static final int AXIS_GENERIC_5 = 36;
    public static final int AXIS_GENERIC_6 = 37;
    public static final int AXIS_GENERIC_7 = 38;
    public static final int AXIS_GENERIC_8 = 39;
    public static final int AXIS_GENERIC_9 = 40;
    public static final int AXIS_GENERIC_10 = 41;
    public static final int AXIS_GENERIC_11 = 42;
    public static final int AXIS_GENERIC_12 = 43;
    public static final int AXIS_GENERIC_13 = 44;
    public static final int AXIS_GENERIC_14 = 45;
    public static final int AXIS_GENERIC_15 = 46;
    public static final int AXIS_GENERIC_16 = 47;
    public static final int AXIS_GESTURE_X_OFFSET = 48;
    public static final int AXIS_GESTURE_Y_OFFSET = 49;
    public static final int AXIS_GESTURE_SCROLL_X_DISTANCE = 50;
    public static final int AXIS_GESTURE_SCROLL_Y_DISTANCE = 51;
    public static final int AXIS_GESTURE_PINCH_SCALE_FACTOR = 52;

    public static final int BUTTON_PRIMARY = 1 << 0;
    public static final int BUTTON_SECONDARY = 1 << 1;
    public static final int BUTTON_TERTIARY = 1 << 2;
    public static final int BUTTON_BACK = 1 << 3;
    public static final int BUTTON_FORWARD = 1 << 4;
    public static final int BUTTON_STYLUS_PRIMARY = 1 << 5;
    public static final int BUTTON_STYLUS_SECONDARY = 1 << 6;

    public static final int CLASSIFICATION_NONE = 0;
    public static final int CLASSIFICATION_AMBIGUOUS_GESTURE = 1;
    public static final int CLASSIFICATION_DEEP_PRESS = 2;
    public static final int CLASSIFICATION_TWO_FINGER_SWIPE = 3;
    public static final int CLASSIFICATION_PINCH = 5;

    public static final int TOOL_TYPE_UNKNOWN = 0;
    public static final int TOOL_TYPE_FINGER = 1;
    public static final int TOOL_TYPE_STYLUS = 2;
    public static final int TOOL_TYPE_MOUSE = 3;
    public static final int TOOL_TYPE_ERASER = 4;

    private static final int DEFAULT_SOURCE = 0x1002;
    private static final int HISTORY_CURRENT = Integer.MIN_VALUE;
    private static final long NANOS_PER_MILLISECOND = 1000000L;

    private static final String[] AXIS_NAMES = {"X", "Y", "PRESSURE", "SIZE", "TOUCH_MAJOR", "TOUCH_MINOR", "TOOL_MAJOR", "TOOL_MINOR",
                                                "ORIENTATION", "VSCROLL", "HSCROLL", "Z", "RX", "RY", "RZ", "HAT_X", "HAT_Y", "LTRIGGER",
                                                "RTRIGGER", "THROTTLE", "RUDDER", "WHEEL", "GAS", "BRAKE", "DISTANCE", "TILT", "SCROLL",
                                                "RELATIVE_X", "RELATIVE_Y", null, null, null, "GENERIC_1", "GENERIC_2", "GENERIC_3",
                                                "GENERIC_4", "GENERIC_5", "GENERIC_6", "GENERIC_7", "GENERIC_8", "GENERIC_9",
                                                "GENERIC_10", "GENERIC_11", "GENERIC_12", "GENERIC_13", "GENERIC_14", "GENERIC_15",
                                                "GENERIC_16", "GESTURE_X_OFFSET", "GESTURE_Y_OFFSET", "GESTURE_SCROLL_X_DISTANCE",
                                                "GESTURE_SCROLL_Y_DISTANCE", "GESTURE_PINCH_SCALE_FACTOR"};

    public static final Parcelable.Creator<MotionEvent> CREATOR = new Parcelable.Creator<MotionEvent>()
    {
        @Override
        public MotionEvent createFromParcel(Parcel in)
        {
            in.readInt();
            return createFromParcelBody(in);
        }

        @Override
        public MotionEvent[] newArray(int size)
        {
            return new MotionEvent[size];
        }
    };

    private long mDownTimeNanos;
    private int mAction;
    private int mActionButton;
    private int mFlags;
    private int mEdgeFlags;
    private int mMetaState;
    private int mButtonState;
    private int mClassification;
    private int mDeviceId;
    private int mSource = DEFAULT_SOURCE;
    private int mDisplayId;
    private float mXPrecision = 1.0f;
    private float mYPrecision = 1.0f;
    private final float[] mTransform = {1, 0, 0, 1, 0, 0};

    private int mPointerCount;
    private PointerProperties[] mProperties = new PointerProperties[0];
    private int mSampleCount;
    private long[] mSampleTimesNanos = new long[1];
    private PointerCoords[] mSamples = new PointerCoords[0];

    private MotionEvent()
    {
    }

    private static MotionEvent create(long downTime, long eventTime, int action, int pointerCount, PointerProperties[] properties,
                                      PointerCoords[] coords, int metaState, int buttonState, float xPrecision, float yPrecision,
                                      int deviceId, int edgeFlags, int source, int displayId, int flags, int classification)
    {
        if (pointerCount < 1) throw new IllegalArgumentException("pointerCount must be at least 1");
        if (properties == null || properties.length < pointerCount)
        {
            throw new IllegalArgumentException("pointerProperties array must be large enough to hold all pointers");
        }
        if (coords == null || coords.length < pointerCount)
        {
            throw new IllegalArgumentException("pointerCoords array must be large enough to hold all pointers");
        }

        MotionEvent event = new MotionEvent();
        event.mDownTimeNanos = downTime * NANOS_PER_MILLISECOND;
        event.mAction = action;
        event.mMetaState = metaState;
        event.mButtonState = buttonState;
        event.mXPrecision = xPrecision;
        event.mYPrecision = yPrecision;
        event.mDeviceId = deviceId;
        event.mEdgeFlags = edgeFlags;
        event.mSource = source;
        event.mDisplayId = displayId;
        event.mFlags = flags;
        event.mClassification = classification;
        event.mPointerCount = pointerCount;
        event.mProperties = new PointerProperties[pointerCount];

        for (int index = 0; index < pointerCount; index++)
        {
            event.mProperties[index] = new PointerProperties(properties[index]);
        }

        event.mSampleCount = 0;
        event.mSamples = new PointerCoords[pointerCount * 2];
        event.mSampleTimesNanos = new long[2];
        event.appendSample(eventTime * NANOS_PER_MILLISECOND, coords, false);
        return event;
    }

    private void appendSample(long eventTimeNanos, PointerCoords[] coords, boolean untransform)
    {
        int needed = (mSampleCount + 1) * mPointerCount;

        if (mSamples.length < needed) mSamples = Arrays.copyOf(mSamples, Math.max(needed, mSamples.length * 2));
        if (mSampleTimesNanos.length < mSampleCount + 1) mSampleTimesNanos = Arrays.copyOf(mSampleTimesNanos, (mSampleCount + 1) * 2);

        float[] inverse = untransform ? inverseTransform() : null;

        for (int pointer = 0; pointer < mPointerCount; pointer++)
        {
            PointerCoords source = pointer < coords.length && coords[pointer] != null ? coords[pointer] : sampleAt(mSampleCount - 1, pointer);
            PointerCoords copy = new PointerCoords(source);

            if (inverse != null)
            {
                float x = copy.x;
                float y = copy.y;
                copy.x = inverse[0] * x + inverse[1] * y + inverse[4];
                copy.y = inverse[2] * x + inverse[3] * y + inverse[5];
            }

            mSamples[mSampleCount * mPointerCount + pointer] = copy;
        }

        mSampleTimesNanos[mSampleCount] = eventTimeNanos;
        mSampleCount++;
    }

    private PointerCoords sampleAt(int sample, int pointer)
    {
        if (sample < 0) return new PointerCoords();

        return mSamples[sample * mPointerCount + pointer];
    }

    private float[] inverseTransform()
    {
        float a = mTransform[0];
        float b = mTransform[1];
        float c = mTransform[2];
        float d = mTransform[3];
        float determinant = a * d - b * c;
        if (determinant == 0) return null;

        float inverseA = d / determinant;
        float inverseB = -b / determinant;
        float inverseC = -c / determinant;
        float inverseD = a / determinant;
        float tx = mTransform[4];
        float ty = mTransform[5];

        return new float[] {inverseA, inverseB, inverseC, inverseD, -(inverseA * tx + inverseB * ty), -(inverseC * tx + inverseD * ty)};
    }

    private boolean isIdentity()
    {
        return mTransform[0] == 1 && mTransform[1] == 0 && mTransform[2] == 0 && mTransform[3] == 1 && mTransform[4] == 0
               && mTransform[5] == 0;
    }

    private static PointerProperties[] defaultProperties(int pointerCount, int[] pointerIds)
    {
        PointerProperties[] properties = new PointerProperties[pointerCount];

        for (int index = 0; index < pointerCount; index++)
        {
            properties[index] = new PointerProperties();
            properties[index].id = pointerIds == null ? index : pointerIds[index];
            properties[index].toolType = TOOL_TYPE_FINGER;
        }

        return properties;
    }

    private static PointerCoords coords(float x, float y, float pressure, float size)
    {
        PointerCoords coords = new PointerCoords();
        coords.x = x;
        coords.y = y;
        coords.pressure = pressure;
        coords.size = size;
        return coords;
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, PointerProperties[] pointerProperties,
                                     PointerCoords[] pointerCoords, int metaState, int buttonState, float xPrecision, float yPrecision,
                                     int deviceId, int edgeFlags, int source, int displayId, int flags, int classification)
    {
        return create(downTime, eventTime, action, pointerCount, pointerProperties, pointerCoords, metaState, buttonState, xPrecision,
                      yPrecision, deviceId, edgeFlags, source, displayId, flags, classification);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, PointerProperties[] pointerProperties,
                                     PointerCoords[] pointerCoords, int metaState, int buttonState, float xPrecision, float yPrecision,
                                     int deviceId, int edgeFlags, int source, int displayId, int flags)
    {
        return create(downTime, eventTime, action, pointerCount, pointerProperties, pointerCoords, metaState, buttonState, xPrecision,
                      yPrecision, deviceId, edgeFlags, source, displayId, flags, CLASSIFICATION_NONE);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, PointerProperties[] pointerProperties,
                                     PointerCoords[] pointerCoords, int metaState, int buttonState, float xPrecision, float yPrecision,
                                     int deviceId, int edgeFlags, int source, int flags)
    {
        return create(downTime, eventTime, action, pointerCount, pointerProperties, pointerCoords, metaState, buttonState, xPrecision,
                      yPrecision, deviceId, edgeFlags, source, Display.DEFAULT_DISPLAY, flags, CLASSIFICATION_NONE);
    }

    @Deprecated
    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds,
                                     PointerCoords[] pointerCoords, int metaState, float xPrecision, float yPrecision, int deviceId,
                                     int edgeFlags, int source, int flags)
    {
        return create(downTime, eventTime, action, pointerCount, defaultProperties(pointerCount, pointerIds), pointerCoords, metaState, 0,
                      xPrecision, yPrecision, deviceId, edgeFlags, source, Display.DEFAULT_DISPLAY, flags, CLASSIFICATION_NONE);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, float x, float y, float pressure, float size,
                                     int metaState, float xPrecision, float yPrecision, int deviceId, int edgeFlags)
    {
        return create(downTime, eventTime, action, 1, defaultProperties(1, null), new PointerCoords[] {coords(x, y, pressure, size)},
                      metaState, 0, xPrecision, yPrecision, deviceId, edgeFlags, InputDevice.SOURCE_UNKNOWN, Display.DEFAULT_DISPLAY, 0,
                      CLASSIFICATION_NONE);
    }

    @Deprecated
    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, float x, float y, float pressure,
                                     float size, int metaState, float xPrecision, float yPrecision, int deviceId, int edgeFlags)
    {
        return obtain(downTime, eventTime, action, x, y, pressure, size, metaState, xPrecision, yPrecision, deviceId, edgeFlags);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, float x, float y, int metaState)
    {
        return create(downTime, eventTime, action, 1, defaultProperties(1, null), new PointerCoords[] {coords(x, y, 1.0f, 1.0f)},
                      metaState, 0, 1.0f, 1.0f, 0, 0, DEFAULT_SOURCE, Display.DEFAULT_DISPLAY, 0, CLASSIFICATION_NONE);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, float[] x,
                                     float[] y, float[] pressure, int metaState, int deviceId, int source)
    {
        PointerCoords[] coords = new PointerCoords[pointerCount];
        for (int index = 0; index < pointerCount; index++)
        {
            coords[index] = coords(x[index], y[index], pressure == null ? 1.0f : pressure[index], 0);
        }

        return create(downTime, eventTime, action, pointerCount, defaultProperties(pointerCount, pointerIds), coords, metaState, 0, 1.0f,
                      1.0f, deviceId, 0, source, Display.DEFAULT_DISPLAY, 0, CLASSIFICATION_NONE);
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, int pointerCount, int[] pointerIds, float[] x,
                                     float[] y, int metaState)
    {
        return obtain(downTime, eventTime, action, pointerCount, pointerIds, x, y, null, metaState, 0, DEFAULT_SOURCE);
    }

    public static MotionEvent obtain(MotionEvent other)
    {
        if (other == null) throw new IllegalArgumentException("other motion event must not be null");

        MotionEvent event = other.copyHeader();
        event.mSampleCount = other.mSampleCount;
        event.mSampleTimesNanos = Arrays.copyOf(other.mSampleTimesNanos, Math.max(1, other.mSampleCount));
        event.mSamples = new PointerCoords[other.mSampleCount * other.mPointerCount];

        for (int index = 0; index < event.mSamples.length; index++)
        {
            event.mSamples[index] = new PointerCoords(other.mSamples[index]);
        }

        return event;
    }

    public static MotionEvent obtainNoHistory(MotionEvent other)
    {
        if (other == null) throw new IllegalArgumentException("other motion event must not be null");

        MotionEvent event = other.copyHeader();
        int last = other.mSampleCount - 1;
        event.mSampleCount = 1;
        event.mSampleTimesNanos = new long[] {other.mSampleTimesNanos[last]};
        event.mSamples = new PointerCoords[other.mPointerCount];

        for (int pointer = 0; pointer < other.mPointerCount; pointer++)
        {
            event.mSamples[pointer] = new PointerCoords(other.sampleAt(last, pointer));
        }

        return event;
    }

    private MotionEvent copyHeader()
    {
        MotionEvent event = new MotionEvent();
        event.mDownTimeNanos = mDownTimeNanos;
        event.mAction = mAction;
        event.mActionButton = mActionButton;
        event.mFlags = mFlags;
        event.mEdgeFlags = mEdgeFlags;
        event.mMetaState = mMetaState;
        event.mButtonState = mButtonState;
        event.mClassification = mClassification;
        event.mDeviceId = mDeviceId;
        event.mSource = mSource;
        event.mDisplayId = mDisplayId;
        event.mXPrecision = mXPrecision;
        event.mYPrecision = mYPrecision;
        System.arraycopy(mTransform, 0, event.mTransform, 0, mTransform.length);
        event.mPointerCount = mPointerCount;
        event.mProperties = new PointerProperties[mPointerCount];

        for (int index = 0; index < mPointerCount; index++)
        {
            event.mProperties[index] = new PointerProperties(mProperties[index]);
        }

        return event;
    }

    static MotionEvent createFromParcelBody(Parcel in)
    {
        MotionEvent event = new MotionEvent();
        int pointerCount = in.readInt();
        int sampleCount = in.readInt();

        event.mDeviceId = in.readInt();
        event.mSource = in.readInt();
        event.mDisplayId = in.readInt();
        event.mAction = in.readInt();
        event.mActionButton = in.readInt();
        event.mFlags = in.readInt();
        event.mEdgeFlags = in.readInt();
        event.mMetaState = in.readInt();
        event.mButtonState = in.readInt();
        event.mClassification = in.readInt();
        event.mXPrecision = in.readFloat();
        event.mYPrecision = in.readFloat();
        event.mDownTimeNanos = in.readLong();

        for (int index = 0; index < event.mTransform.length; index++)
        {
            event.mTransform[index] = in.readFloat();
        }

        event.mPointerCount = pointerCount;
        event.mProperties = new PointerProperties[pointerCount];

        for (int index = 0; index < pointerCount; index++)
        {
            PointerProperties properties = new PointerProperties();
            properties.id = in.readInt();
            properties.toolType = in.readInt();
            event.mProperties[index] = properties;
        }

        event.mSampleCount = sampleCount;
        event.mSampleTimesNanos = new long[Math.max(1, sampleCount)];
        event.mSamples = new PointerCoords[sampleCount * pointerCount];

        for (int sample = 0; sample < sampleCount; sample++)
        {
            event.mSampleTimesNanos[sample] = in.readLong();

            for (int pointer = 0; pointer < pointerCount; pointer++)
            {
                event.mSamples[sample * pointerCount + pointer] = PointerCoords.readFromParcel(in);
            }
        }

        return event;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(PARCEL_TOKEN_MOTION_EVENT);
        out.writeInt(mPointerCount);
        out.writeInt(mSampleCount);
        out.writeInt(mDeviceId);
        out.writeInt(mSource);
        out.writeInt(mDisplayId);
        out.writeInt(mAction);
        out.writeInt(mActionButton);
        out.writeInt(mFlags);
        out.writeInt(mEdgeFlags);
        out.writeInt(mMetaState);
        out.writeInt(mButtonState);
        out.writeInt(mClassification);
        out.writeFloat(mXPrecision);
        out.writeFloat(mYPrecision);
        out.writeLong(mDownTimeNanos);

        for (float value : mTransform)
        {
            out.writeFloat(value);
        }

        for (int index = 0; index < mPointerCount; index++)
        {
            out.writeInt(mProperties[index].id);
            out.writeInt(mProperties[index].toolType);
        }

        for (int sample = 0; sample < mSampleCount; sample++)
        {
            out.writeLong(mSampleTimesNanos[sample]);

            for (int pointer = 0; pointer < mPointerCount; pointer++)
            {
                sampleAt(sample, pointer).writeToParcel(out);
            }
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public void recycle()
    {
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public final void addBatch(long eventTime, float x, float y, float pressure, float size, int metaState)
    {
        PointerCoords[] coords = new PointerCoords[mPointerCount];
        coords[0] = coords(x, y, pressure, size);

        for (int pointer = 1; pointer < mPointerCount; pointer++)
        {
            coords[pointer] = transformedCoords(mSampleCount - 1, pointer);
        }

        appendSample(eventTime * NANOS_PER_MILLISECOND, coords, true);
        mMetaState |= metaState;
    }

    public final void addBatch(long eventTime, PointerCoords[] pointerCoords, int metaState)
    {
        if (pointerCoords == null) throw new IllegalArgumentException("pointerCoords must not be null");

        appendSample(eventTime * NANOS_PER_MILLISECOND, pointerCoords, true);
        mMetaState |= metaState;
    }

    private PointerCoords transformedCoords(int sample, int pointer)
    {
        PointerCoords raw = sampleAt(sample, pointer);
        PointerCoords result = new PointerCoords(raw);
        if (isIdentity()) return result;

        result.x = mTransform[0] * raw.x + mTransform[1] * raw.y + mTransform[4];
        result.y = mTransform[2] * raw.x + mTransform[3] * raw.y + mTransform[5];

        float relativeX = raw.getAxisValue(AXIS_RELATIVE_X);
        float relativeY = raw.getAxisValue(AXIS_RELATIVE_Y);
        if (relativeX != 0 || relativeY != 0)
        {
            result.setAxisValue(AXIS_RELATIVE_X, mTransform[0] * relativeX + mTransform[1] * relativeY);
            result.setAxisValue(AXIS_RELATIVE_Y, mTransform[2] * relativeX + mTransform[3] * relativeY);
        }

        if (raw.orientation != 0 || mTransform[1] != 0 || mTransform[2] != 0)
        {
            float vectorX = (float)Math.sin(raw.orientation);
            float vectorY = (float)-Math.cos(raw.orientation);
            float transformedX = mTransform[0] * vectorX + mTransform[1] * vectorY;
            float transformedY = mTransform[2] * vectorX + mTransform[3] * vectorY;
            result.orientation = (float)Math.atan2(transformedX, -transformedY);
        }

        return result;
    }

    private int sampleIndex(int position)
    {
        if (position == HISTORY_CURRENT) return mSampleCount - 1;
        if (position < 0 || position >= mSampleCount - 1) throw new IllegalArgumentException("historyPos out of range");

        return position;
    }

    private void checkPointer(int pointerIndex)
    {
        if (pointerIndex < 0 || pointerIndex >= mPointerCount) throw new IllegalArgumentException("pointerIndex out of range");
    }

    private float axis(int axis, int pointerIndex, int position)
    {
        checkPointer(pointerIndex);

        return transformedCoords(sampleIndex(position), pointerIndex).getAxisValue(axis);
    }

    private float rawAxis(int axis, int pointerIndex)
    {
        checkPointer(pointerIndex);

        return sampleAt(mSampleCount - 1, pointerIndex).getAxisValue(axis);
    }

    public final void offsetLocation(float deltaX, float deltaY)
    {
        if (deltaX == 0 && deltaY == 0) return;

        mTransform[4] += deltaX;
        mTransform[5] += deltaY;
    }

    public final void setLocation(float x, float y)
    {
        offsetLocation(x - getX(), y - getY());
    }

    public final void transform(Matrix matrix)
    {
        if (matrix == null) throw new IllegalArgumentException("matrix must not be null");

        float[] values = new float[9];
        matrix.getValues(values);

        float a = values[Matrix.MSCALE_X];
        float b = values[Matrix.MSKEW_X];
        float tx = values[Matrix.MTRANS_X];
        float c = values[Matrix.MSKEW_Y];
        float d = values[Matrix.MSCALE_Y];
        float ty = values[Matrix.MTRANS_Y];

        float[] current = mTransform.clone();
        mTransform[0] = a * current[0] + b * current[2];
        mTransform[1] = a * current[1] + b * current[3];
        mTransform[2] = c * current[0] + d * current[2];
        mTransform[3] = c * current[1] + d * current[3];
        mTransform[4] = a * current[4] + b * current[5] + tx;
        mTransform[5] = c * current[4] + d * current[5] + ty;
    }

    public final int getAction()
    {
        return mAction;
    }

    public final void setAction(int action)
    {
        mAction = action;
    }

    public final int getActionMasked()
    {
        return mAction & ACTION_MASK;
    }

    public final int getActionIndex()
    {
        return (mAction & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    public final int getActionButton()
    {
        return mActionButton;
    }

    public final void setActionButton(int button)
    {
        mActionButton = button;
    }

    public final int getFlags()
    {
        return mFlags;
    }

    public final long getDownTime()
    {
        return mDownTimeNanos / NANOS_PER_MILLISECOND;
    }

    public final void setDownTime(long downTime)
    {
        mDownTimeNanos = downTime * NANOS_PER_MILLISECOND;
    }

    @Override
    public final long getEventTime()
    {
        return mSampleTimesNanos[mSampleCount - 1] / NANOS_PER_MILLISECOND;
    }

    public final long getEventTimeNanos()
    {
        return mSampleTimesNanos[mSampleCount - 1];
    }

    public final int getMetaState()
    {
        return mMetaState;
    }

    public final int getButtonState()
    {
        return mButtonState;
    }

    public final void setButtonState(int buttonState)
    {
        mButtonState = buttonState;
    }

    public final boolean isButtonPressed(int button)
    {
        if (button == 0) return false;

        return (getButtonState() & button) == button;
    }

    public final int getClassification()
    {
        return mClassification;
    }

    @Override
    public final int getDeviceId()
    {
        return mDeviceId;
    }

    @Override
    public final int getSource()
    {
        return mSource;
    }

    public final void setSource(int source)
    {
        mSource = source;
    }

    public final int getDisplayId()
    {
        return mDisplayId;
    }

    public final void setDisplayId(int displayId)
    {
        mDisplayId = displayId;
    }

    public final int getEdgeFlags()
    {
        return mEdgeFlags;
    }

    public final void setEdgeFlags(int flags)
    {
        mEdgeFlags = flags;
    }

    public final float getXPrecision()
    {
        return mXPrecision;
    }

    public final float getYPrecision()
    {
        return mYPrecision;
    }

    public final int getPointerCount()
    {
        return mPointerCount;
    }

    public final int getPointerId(int pointerIndex)
    {
        checkPointer(pointerIndex);
        return mProperties[pointerIndex].id;
    }

    public final int getToolType(int pointerIndex)
    {
        checkPointer(pointerIndex);
        return mProperties[pointerIndex].toolType;
    }

    public final int findPointerIndex(int pointerId)
    {
        for (int index = 0; index < mPointerCount; index++)
        {
            if (mProperties[index].id == pointerId) return index;
        }

        return -1;
    }

    public final void getPointerProperties(int pointerIndex, PointerProperties outPointerProperties)
    {
        checkPointer(pointerIndex);
        if (outPointerProperties == null) throw new IllegalArgumentException("outPointerProperties must not be null");

        outPointerProperties.copyFrom(mProperties[pointerIndex]);
    }

    public final void getPointerCoords(int pointerIndex, PointerCoords outPointerCoords)
    {
        getHistoricalPointerCoords(pointerIndex, HISTORY_CURRENT, outPointerCoords);
    }

    public final void getHistoricalPointerCoords(int pointerIndex, int pos, PointerCoords outPointerCoords)
    {
        checkPointer(pointerIndex);
        if (outPointerCoords == null) throw new IllegalArgumentException("outPointerCoords must not be null");

        outPointerCoords.copyFrom(transformedCoords(sampleIndex(pos), pointerIndex));
    }

    public final float getX()
    {
        return axis(AXIS_X, 0, HISTORY_CURRENT);
    }

    public final float getY()
    {
        return axis(AXIS_Y, 0, HISTORY_CURRENT);
    }

    public final float getPressure()
    {
        return axis(AXIS_PRESSURE, 0, HISTORY_CURRENT);
    }

    public final float getSize()
    {
        return axis(AXIS_SIZE, 0, HISTORY_CURRENT);
    }

    public final float getTouchMajor()
    {
        return axis(AXIS_TOUCH_MAJOR, 0, HISTORY_CURRENT);
    }

    public final float getTouchMinor()
    {
        return axis(AXIS_TOUCH_MINOR, 0, HISTORY_CURRENT);
    }

    public final float getToolMajor()
    {
        return axis(AXIS_TOOL_MAJOR, 0, HISTORY_CURRENT);
    }

    public final float getToolMinor()
    {
        return axis(AXIS_TOOL_MINOR, 0, HISTORY_CURRENT);
    }

    public final float getOrientation()
    {
        return axis(AXIS_ORIENTATION, 0, HISTORY_CURRENT);
    }

    public final float getAxisValue(int axis)
    {
        return axis(axis, 0, HISTORY_CURRENT);
    }

    public final float getX(int pointerIndex)
    {
        return axis(AXIS_X, pointerIndex, HISTORY_CURRENT);
    }

    public final float getY(int pointerIndex)
    {
        return axis(AXIS_Y, pointerIndex, HISTORY_CURRENT);
    }

    public final float getPressure(int pointerIndex)
    {
        return axis(AXIS_PRESSURE, pointerIndex, HISTORY_CURRENT);
    }

    public final float getSize(int pointerIndex)
    {
        return axis(AXIS_SIZE, pointerIndex, HISTORY_CURRENT);
    }

    public final float getTouchMajor(int pointerIndex)
    {
        return axis(AXIS_TOUCH_MAJOR, pointerIndex, HISTORY_CURRENT);
    }

    public final float getTouchMinor(int pointerIndex)
    {
        return axis(AXIS_TOUCH_MINOR, pointerIndex, HISTORY_CURRENT);
    }

    public final float getToolMajor(int pointerIndex)
    {
        return axis(AXIS_TOOL_MAJOR, pointerIndex, HISTORY_CURRENT);
    }

    public final float getToolMinor(int pointerIndex)
    {
        return axis(AXIS_TOOL_MINOR, pointerIndex, HISTORY_CURRENT);
    }

    public final float getOrientation(int pointerIndex)
    {
        return axis(AXIS_ORIENTATION, pointerIndex, HISTORY_CURRENT);
    }

    public final float getAxisValue(int axis, int pointerIndex)
    {
        return axis(axis, pointerIndex, HISTORY_CURRENT);
    }

    public final float getRawX()
    {
        return rawAxis(AXIS_X, 0);
    }

    public final float getRawY()
    {
        return rawAxis(AXIS_Y, 0);
    }

    public float getRawX(int pointerIndex)
    {
        return rawAxis(AXIS_X, pointerIndex);
    }

    public float getRawY(int pointerIndex)
    {
        return rawAxis(AXIS_Y, pointerIndex);
    }

    public final int getHistorySize()
    {
        return mSampleCount - 1;
    }

    public final long getHistoricalEventTime(int pos)
    {
        return mSampleTimesNanos[sampleIndex(pos)] / NANOS_PER_MILLISECOND;
    }

    public long getHistoricalEventTimeNanos(int pos)
    {
        return mSampleTimesNanos[sampleIndex(pos)];
    }

    public final float getHistoricalX(int pos)
    {
        return axis(AXIS_X, 0, pos);
    }

    public final float getHistoricalY(int pos)
    {
        return axis(AXIS_Y, 0, pos);
    }

    public final float getHistoricalPressure(int pos)
    {
        return axis(AXIS_PRESSURE, 0, pos);
    }

    public final float getHistoricalSize(int pos)
    {
        return axis(AXIS_SIZE, 0, pos);
    }

    public final float getHistoricalTouchMajor(int pos)
    {
        return axis(AXIS_TOUCH_MAJOR, 0, pos);
    }

    public final float getHistoricalTouchMinor(int pos)
    {
        return axis(AXIS_TOUCH_MINOR, 0, pos);
    }

    public final float getHistoricalToolMajor(int pos)
    {
        return axis(AXIS_TOOL_MAJOR, 0, pos);
    }

    public final float getHistoricalToolMinor(int pos)
    {
        return axis(AXIS_TOOL_MINOR, 0, pos);
    }

    public final float getHistoricalOrientation(int pos)
    {
        return axis(AXIS_ORIENTATION, 0, pos);
    }

    public final float getHistoricalAxisValue(int axis, int pos)
    {
        return axis(axis, 0, pos);
    }

    public final float getHistoricalX(int pointerIndex, int pos)
    {
        return axis(AXIS_X, pointerIndex, pos);
    }

    public final float getHistoricalY(int pointerIndex, int pos)
    {
        return axis(AXIS_Y, pointerIndex, pos);
    }

    public final float getHistoricalPressure(int pointerIndex, int pos)
    {
        return axis(AXIS_PRESSURE, pointerIndex, pos);
    }

    public final float getHistoricalSize(int pointerIndex, int pos)
    {
        return axis(AXIS_SIZE, pointerIndex, pos);
    }

    public final float getHistoricalTouchMajor(int pointerIndex, int pos)
    {
        return axis(AXIS_TOUCH_MAJOR, pointerIndex, pos);
    }

    public final float getHistoricalTouchMinor(int pointerIndex, int pos)
    {
        return axis(AXIS_TOUCH_MINOR, pointerIndex, pos);
    }

    public final float getHistoricalToolMajor(int pointerIndex, int pos)
    {
        return axis(AXIS_TOOL_MAJOR, pointerIndex, pos);
    }

    public final float getHistoricalToolMinor(int pointerIndex, int pos)
    {
        return axis(AXIS_TOOL_MINOR, pointerIndex, pos);
    }

    public final float getHistoricalOrientation(int pointerIndex, int pos)
    {
        return axis(AXIS_ORIENTATION, pointerIndex, pos);
    }

    public final float getHistoricalAxisValue(int axis, int pointerIndex, int pos)
    {
        return axis(axis, pointerIndex, pos);
    }

    public static String actionToString(int action)
    {
        switch (action)
        {
            case ACTION_DOWN:
                return "ACTION_DOWN";
            case ACTION_UP:
                return "ACTION_UP";
            case ACTION_CANCEL:
                return "ACTION_CANCEL";
            case ACTION_OUTSIDE:
                return "ACTION_OUTSIDE";
            case ACTION_MOVE:
                return "ACTION_MOVE";
            case ACTION_HOVER_MOVE:
                return "ACTION_HOVER_MOVE";
            case ACTION_SCROLL:
                return "ACTION_SCROLL";
            case ACTION_HOVER_ENTER:
                return "ACTION_HOVER_ENTER";
            case ACTION_HOVER_EXIT:
                return "ACTION_HOVER_EXIT";
            case ACTION_BUTTON_PRESS:
                return "ACTION_BUTTON_PRESS";
            case ACTION_BUTTON_RELEASE:
                return "ACTION_BUTTON_RELEASE";
            default:
                break;
        }

        int index = (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

        switch (action & ACTION_MASK)
        {
            case ACTION_POINTER_DOWN:
                return "ACTION_POINTER_DOWN(" + index + ")";
            case ACTION_POINTER_UP:
                return "ACTION_POINTER_UP(" + index + ")";
            default:
                return Integer.toString(action);
        }
    }

    public static String axisToString(int axis)
    {
        if (axis >= 0 && axis < AXIS_NAMES.length && AXIS_NAMES[axis] != null) return "AXIS_" + AXIS_NAMES[axis];

        return Integer.toString(axis);
    }

    public static int axisFromString(String symbolicName)
    {
        if (symbolicName == null) throw new IllegalArgumentException("symbolicName must not be null");

        if (symbolicName.startsWith("AXIS_"))
        {
            String name = symbolicName.substring("AXIS_".length());

            for (int axis = 0; axis < AXIS_NAMES.length; axis++)
            {
                if (name.equals(AXIS_NAMES[axis])) return axis;
            }
        }

        try
        {
            return Integer.parseInt(symbolicName, 10);
        }
        catch (NumberFormatException malformed)
        {
            return -1;
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("MotionEvent { action=").append(actionToString(getAction()));

        for (int index = 0; index < mPointerCount; index++)
        {
            builder.append(", id[").append(index).append("]=").append(getPointerId(index));
            builder.append(", x[").append(index).append("]=").append(getX(index));
            builder.append(", y[").append(index).append("]=").append(getY(index));
            builder.append(", toolType[").append(index).append("]=").append(getToolType(index));
        }

        builder.append(", buttonState=").append(mButtonState);
        builder.append(", metaState=").append(KeyEvent.metaStateToString(mMetaState));
        builder.append(", flags=0x").append(Integer.toHexString(mFlags));
        builder.append(", edgeFlags=0x").append(Integer.toHexString(mEdgeFlags));
        builder.append(", pointerCount=").append(mPointerCount);
        builder.append(", historySize=").append(getHistorySize());
        builder.append(", eventTime=").append(getEventTime());
        builder.append(", downTime=").append(getDownTime());
        builder.append(", deviceId=").append(mDeviceId);
        builder.append(", source=0x").append(Integer.toHexString(mSource));
        builder.append(", displayId=").append(mDisplayId);
        return builder.append(" }").toString();
    }

    public static final class PointerProperties
    {
        public int id;
        public int toolType;

        public PointerProperties()
        {
            clear();
        }

        public PointerProperties(PointerProperties other)
        {
            copyFrom(other);
        }

        public void clear()
        {
            id = INVALID_POINTER_ID;
            toolType = TOOL_TYPE_UNKNOWN;
        }

        public void copyFrom(PointerProperties other)
        {
            id = other.id;
            toolType = other.toolType;
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof PointerProperties)) return false;

            PointerProperties properties = (PointerProperties)other;
            return id == properties.id && toolType == properties.toolType;
        }

        @Override
        public int hashCode()
        {
            return id | (toolType << 8);
        }
    }

    public static final class PointerCoords
    {
        public float x;
        public float y;
        public float pressure;
        public float size;
        public float touchMajor;
        public float touchMinor;
        public float toolMajor;
        public float toolMinor;
        public float orientation;

        private long mPackedAxisBits;
        private float[] mPackedAxisValues;
        private boolean mIsResampled;

        public PointerCoords()
        {
        }

        public PointerCoords(PointerCoords other)
        {
            copyFrom(other);
        }

        public void clear()
        {
            mPackedAxisBits = 0;
            mPackedAxisValues = null;
            x = 0;
            y = 0;
            pressure = 0;
            size = 0;
            touchMajor = 0;
            touchMinor = 0;
            toolMajor = 0;
            toolMinor = 0;
            orientation = 0;
            mIsResampled = false;
        }

        public void copyFrom(PointerCoords other)
        {
            mPackedAxisBits = other.mPackedAxisBits;
            mPackedAxisValues = other.mPackedAxisValues == null ? null : other.mPackedAxisValues.clone();
            x = other.x;
            y = other.y;
            pressure = other.pressure;
            size = other.size;
            touchMajor = other.touchMajor;
            touchMinor = other.touchMinor;
            toolMajor = other.toolMajor;
            toolMinor = other.toolMinor;
            orientation = other.orientation;
            mIsResampled = other.mIsResampled;
        }

        public boolean isResampled()
        {
            return mIsResampled;
        }

        public float getAxisValue(int axis)
        {
            switch (axis)
            {
                case AXIS_X:
                    return x;
                case AXIS_Y:
                    return y;
                case AXIS_PRESSURE:
                    return pressure;
                case AXIS_SIZE:
                    return size;
                case AXIS_TOUCH_MAJOR:
                    return touchMajor;
                case AXIS_TOUCH_MINOR:
                    return touchMinor;
                case AXIS_TOOL_MAJOR:
                    return toolMajor;
                case AXIS_TOOL_MINOR:
                    return toolMinor;
                case AXIS_ORIENTATION:
                    return orientation;
                default:
                    break;
            }

            if (axis < 0 || axis > 63) throw new IllegalArgumentException("Axis out of range.");

            long bit = 1L << axis;
            if ((mPackedAxisBits & bit) == 0) return 0;

            return mPackedAxisValues[Long.bitCount(mPackedAxisBits & (bit - 1))];
        }

        public void setAxisValue(int axis, float value)
        {
            switch (axis)
            {
                case AXIS_X:
                    x = value;
                    return;
                case AXIS_Y:
                    y = value;
                    return;
                case AXIS_PRESSURE:
                    pressure = value;
                    return;
                case AXIS_SIZE:
                    size = value;
                    return;
                case AXIS_TOUCH_MAJOR:
                    touchMajor = value;
                    return;
                case AXIS_TOUCH_MINOR:
                    touchMinor = value;
                    return;
                case AXIS_TOOL_MAJOR:
                    toolMajor = value;
                    return;
                case AXIS_TOOL_MINOR:
                    toolMinor = value;
                    return;
                case AXIS_ORIENTATION:
                    orientation = value;
                    return;
                default:
                    break;
            }

            if (axis < 0 || axis > 63) throw new IllegalArgumentException("Axis out of range.");

            long bit = 1L << axis;
            int index = Long.bitCount(mPackedAxisBits & (bit - 1));

            if ((mPackedAxisBits & bit) != 0)
            {
                mPackedAxisValues[index] = value;
                return;
            }

            int count = Long.bitCount(mPackedAxisBits);
            float[] values = new float[count + 1];
            if (mPackedAxisValues != null)
            {
                System.arraycopy(mPackedAxisValues, 0, values, 0, index);
                System.arraycopy(mPackedAxisValues, index, values, index + 1, count - index);
            }

            values[index] = value;
            mPackedAxisValues = values;
            mPackedAxisBits |= bit;
        }

        void writeToParcel(Parcel out)
        {
            out.writeFloat(x);
            out.writeFloat(y);
            out.writeFloat(pressure);
            out.writeFloat(size);
            out.writeFloat(touchMajor);
            out.writeFloat(touchMinor);
            out.writeFloat(toolMajor);
            out.writeFloat(toolMinor);
            out.writeFloat(orientation);
            out.writeLong(mPackedAxisBits);

            int count = Long.bitCount(mPackedAxisBits);
            for (int index = 0; index < count; index++)
            {
                out.writeFloat(mPackedAxisValues[index]);
            }

            out.writeInt(mIsResampled ? 1 : 0);
        }

        static PointerCoords readFromParcel(Parcel in)
        {
            PointerCoords coords = new PointerCoords();
            coords.x = in.readFloat();
            coords.y = in.readFloat();
            coords.pressure = in.readFloat();
            coords.size = in.readFloat();
            coords.touchMajor = in.readFloat();
            coords.touchMinor = in.readFloat();
            coords.toolMajor = in.readFloat();
            coords.toolMinor = in.readFloat();
            coords.orientation = in.readFloat();
            coords.mPackedAxisBits = in.readLong();

            int count = Long.bitCount(coords.mPackedAxisBits);
            if (count > 0)
            {
                coords.mPackedAxisValues = new float[count];
                for (int index = 0; index < count; index++)
                {
                    coords.mPackedAxisValues[index] = in.readFloat();
                }
            }

            coords.mIsResampled = in.readInt() != 0;
            return coords;
        }
    }
}
