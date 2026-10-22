package android.view.accessibility;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public final class AccessibilityEvent extends AccessibilityRecord implements Parcelable
{
    public static final int TYPE_VIEW_CLICKED = 0x00000001;
    public static final int TYPE_VIEW_LONG_CLICKED = 0x00000002;
    public static final int TYPE_VIEW_SELECTED = 0x00000004;
    public static final int TYPE_VIEW_FOCUSED = 0x00000008;
    public static final int TYPE_VIEW_TEXT_CHANGED = 0x00000010;
    public static final int TYPE_WINDOW_STATE_CHANGED = 0x00000020;
    public static final int TYPE_NOTIFICATION_STATE_CHANGED = 0x00000040;
    public static final int TYPE_VIEW_HOVER_ENTER = 0x00000080;
    public static final int TYPE_VIEW_HOVER_EXIT = 0x00000100;
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_START = 0x00000200;
    public static final int TYPE_TOUCH_EXPLORATION_GESTURE_END = 0x00000400;
    public static final int TYPE_WINDOW_CONTENT_CHANGED = 0x00000800;
    public static final int TYPE_VIEW_SCROLLED = 0x00001000;
    public static final int TYPE_VIEW_TEXT_SELECTION_CHANGED = 0x00002000;
    public static final int TYPE_ANNOUNCEMENT = 0x00004000;
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUSED = 0x00008000;
    public static final int TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED = 0x00010000;
    public static final int TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY = 0x00020000;
    public static final int TYPE_GESTURE_DETECTION_START = 0x00040000;
    public static final int TYPE_GESTURE_DETECTION_END = 0x00080000;
    public static final int TYPE_TOUCH_INTERACTION_START = 0x00100000;
    public static final int TYPE_TOUCH_INTERACTION_END = 0x00200000;
    public static final int TYPE_WINDOWS_CHANGED = 0x00400000;
    public static final int TYPE_VIEW_CONTEXT_CLICKED = 0x00800000;
    public static final int TYPE_ASSIST_READING_CONTEXT = 0x01000000;
    public static final int TYPE_SPEECH_STATE_CHANGE = 0x02000000;
    public static final int TYPE_VIEW_TARGETED_BY_SCROLL = 0x04000000;

    public static final int INVALID_POSITION = -1;
    public static final int MAX_TEXT_LENGTH = 500;

    public static final int SPEECH_STATE_SPEAKING_START = 0x00000001;
    public static final int SPEECH_STATE_SPEAKING_END = 0x00000002;
    public static final int SPEECH_STATE_LISTENING_START = 0x00000004;
    public static final int SPEECH_STATE_LISTENING_END = 0x00000008;

    public static final int WINDOWS_CHANGE_ADDED = 0x00000001;
    public static final int WINDOWS_CHANGE_REMOVED = 0x00000002;
    public static final int WINDOWS_CHANGE_TITLE = 0x00000004;
    public static final int WINDOWS_CHANGE_BOUNDS = 0x00000008;
    public static final int WINDOWS_CHANGE_LAYER = 0x00000010;
    public static final int WINDOWS_CHANGE_ACTIVE = 0x00000020;
    public static final int WINDOWS_CHANGE_FOCUSED = 0x00000040;
    public static final int WINDOWS_CHANGE_ACCESSIBILITY_FOCUSED = 0x00000080;
    public static final int WINDOWS_CHANGE_PARENT = 0x00000100;
    public static final int WINDOWS_CHANGE_CHILDREN = 0x00000200;
    public static final int WINDOWS_CHANGE_PIP = 0x00000400;
    public static final int TYPES_ALL_MASK = 0xFFFFFFFF;

    public static final int CONTENT_CHANGE_TYPE_UNDEFINED = 0x00000000;
    public static final int CONTENT_CHANGE_TYPE_SUBTREE = 0x00000001;
    public static final int CONTENT_CHANGE_TYPE_TEXT = 0x00000002;
    public static final int CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION = 0x00000004;
    public static final int CONTENT_CHANGE_TYPE_STATE_DESCRIPTION = 0x00000040;
    public static final int CONTENT_CHANGE_TYPE_PANE_TITLE = 0x00000008;
    public static final int CONTENT_CHANGE_TYPE_PANE_APPEARED = 0x00000010;
    public static final int CONTENT_CHANGE_TYPE_PANE_DISAPPEARED = 0x00000020;
    public static final int CONTENT_CHANGE_TYPE_DRAG_STARTED = 0x00000080;
    public static final int CONTENT_CHANGE_TYPE_DRAG_DROPPED = 0x00000100;
    public static final int CONTENT_CHANGE_TYPE_DRAG_CANCELLED = 0x00000200;
    public static final int CONTENT_CHANGE_TYPE_CONTENT_INVALID = 0x00000400;
    public static final int CONTENT_CHANGE_TYPE_ERROR = 0x00000800;
    public static final int CONTENT_CHANGE_TYPE_ENABLED = 0x00001000;
    public static final int CONTENT_CHANGE_TYPE_CHECKED = 0x00002000;
    public static final int CONTENT_CHANGE_TYPE_EXPANDED = 0x00004000;
    public static final int CONTENT_CHANGE_TYPE_SUPPLEMENTAL_DESCRIPTION = 0x00008000;

    public static final Parcelable.Creator<AccessibilityEvent> CREATOR = new Parcelable.Creator<AccessibilityEvent>()
    {
        @Override
        public AccessibilityEvent createFromParcel(Parcel source)
        {
            AccessibilityEvent event = new AccessibilityEvent();
            event.initFromParcel(source);
            return event;
        }

        @Override
        public AccessibilityEvent[] newArray(int size)
        {
            return new AccessibilityEvent[size];
        }
    };

    private final List<AccessibilityRecord> mRecords = new ArrayList<>();

    private int mEventType;
    private int mContentChangeTypes;
    private int mAction;
    private int mMovementGranularity;
    private long mEventTime;
    private CharSequence mPackageName;
    private int mWindowChangeTypes;
    private int mSpeechStateChangeTypes;

    public AccessibilityEvent()
    {
    }

    public AccessibilityEvent(int eventType)
    {
        mEventType = eventType;
    }

    public AccessibilityEvent(AccessibilityEvent event)
    {
        super(event);

        if (event == null) return;

        mRecords.addAll(event.mRecords);
        mEventType = event.mEventType;
        mContentChangeTypes = event.mContentChangeTypes;
        mAction = event.mAction;
        mMovementGranularity = event.mMovementGranularity;
        mEventTime = event.mEventTime;
        mPackageName = event.mPackageName;
        mWindowChangeTypes = event.mWindowChangeTypes;
        mSpeechStateChangeTypes = event.mSpeechStateChangeTypes;
    }

    public int getWindowChanges()
    {
        return mWindowChangeTypes;
    }

    public void setWindowChanges(int changes)
    {
        mWindowChangeTypes = changes;
    }

    public int getSpeechStateChangeTypes()
    {
        return mSpeechStateChangeTypes;
    }

    public void setSpeechStateChangeTypes(int state)
    {
        mSpeechStateChangeTypes = state;
    }

    @Override
    public void recycle()
    {
    }

    public static AccessibilityEvent obtain()
    {
        return new AccessibilityEvent();
    }

    public static AccessibilityEvent obtain(int eventType)
    {
        return new AccessibilityEvent(eventType);
    }

    public static AccessibilityEvent obtain(AccessibilityEvent event)
    {
        return new AccessibilityEvent(event);
    }

    public int getRecordCount()
    {
        return mRecords.size();
    }

    public void appendRecord(AccessibilityRecord record)
    {
        mRecords.add(record);
    }

    public AccessibilityRecord getRecord(int index)
    {
        return mRecords.get(index);
    }

    public int getEventType()
    {
        return mEventType;
    }

    public void setEventType(int eventType)
    {
        mEventType = eventType;
    }

    public int getContentChangeTypes()
    {
        return mContentChangeTypes;
    }

    public void setContentChangeTypes(int types)
    {
        mContentChangeTypes = types;
    }

    public long getEventTime()
    {
        return mEventTime;
    }

    public void setEventTime(long eventTime)
    {
        mEventTime = eventTime;
    }

    public CharSequence getPackageName()
    {
        return mPackageName;
    }

    public void setPackageName(CharSequence packageName)
    {
        mPackageName = packageName;
    }

    public int getMovementGranularity()
    {
        return mMovementGranularity;
    }

    public void setMovementGranularity(int granularity)
    {
        mMovementGranularity = granularity;
    }

    public int getAction()
    {
        return mAction;
    }

    public void setAction(int action)
    {
        mAction = action;
    }

    public boolean isAccessibilityDataSensitive()
    {
        return false;
    }

    public void setAccessibilityDataSensitive(boolean sensitive)
    {
    }

    public static String eventTypeToString(int eventType)
    {
        return "TYPE_" + Integer.toHexString(eventType);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mEventType);
        dest.writeInt(mMovementGranularity);
        dest.writeInt(mAction);
        dest.writeInt(mContentChangeTypes);
        android.text.TextUtils.writeToParcel(mPackageName, dest, flags);
        dest.writeLong(mEventTime);
        dest.writeInt(mWindowChangeTypes);
        dest.writeInt(mSpeechStateChangeTypes);
        writeRecordToParcel(dest, flags);

        dest.writeInt(mRecords.size());
        for (AccessibilityRecord record : mRecords) record.writeRecordToParcel(dest, flags);
    }

    public void initFromParcel(Parcel source)
    {
        mEventType = source.readInt();
        mMovementGranularity = source.readInt();
        mAction = source.readInt();
        mContentChangeTypes = source.readInt();
        mPackageName = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mEventTime = source.readLong();
        mWindowChangeTypes = source.readInt();
        mSpeechStateChangeTypes = source.readInt();
        readRecordFromParcel(source);

        mRecords.clear();
        int count = source.readInt();
        for (int index = 0; index < count; index++)
        {
            AccessibilityRecord record = new AccessibilityRecord();
            record.readRecordFromParcel(source);
            mRecords.add(record);
        }
    }

    @Override
    public String toString()
    {
        return "AccessibilityEvent[type=" + mEventType + " " + super.toString() + "]";
    }
}
