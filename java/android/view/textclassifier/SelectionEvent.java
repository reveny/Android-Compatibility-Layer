package android.view.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class SelectionEvent implements Parcelable
{
    public static final int ACTION_OVERTYPE = 100;
    public static final int ACTION_COPY = 101;
    public static final int ACTION_PASTE = 102;
    public static final int ACTION_CUT = 103;
    public static final int ACTION_SHARE = 104;
    public static final int ACTION_SMART_SHARE = 105;
    public static final int ACTION_DRAG = 106;
    public static final int ACTION_ABANDON = 107;
    public static final int ACTION_OTHER = 108;
    public static final int ACTION_SELECT_ALL = 200;
    public static final int ACTION_RESET = 201;

    public static final int EVENT_SELECTION_STARTED = 1;
    public static final int EVENT_SELECTION_MODIFIED = 2;
    public static final int EVENT_SMART_SELECTION_SINGLE = 3;
    public static final int EVENT_SMART_SELECTION_MULTI = 4;
    public static final int EVENT_AUTO_SELECTION = 5;

    public static final int INVOCATION_UNKNOWN = 0;
    public static final int INVOCATION_MANUAL = 1;
    public static final int INVOCATION_LINK = 2;

    private final int mAbsoluteStart;
    private final int mAbsoluteEnd;
    private final int mEventType;
    private final String mEntityType;
    private final int mInvocationMethod;
    private final String mResultId;
    private final long mEventTime;
    private String mPackageName = "";
    private String mWidgetType = TextClassifier.WIDGET_TYPE_UNKNOWN;
    private String mWidgetVersion;
    private long mDurationSinceSessionStart;
    private long mDurationSincePreviousEvent;
    private int mEventIndex;
    private TextClassificationSessionId mSessionId;
    private int mStart;
    private int mEnd;
    private int mSmartStart;
    private int mSmartEnd;

    private SelectionEvent(int start, int end, int eventType, String entityType, int invocationMethod, String resultId)
    {
        mAbsoluteStart = start;
        mAbsoluteEnd = end;
        mEventType = eventType;
        mEntityType = entityType == null ? TextClassifier.TYPE_UNKNOWN : entityType;
        mInvocationMethod = invocationMethod;
        mResultId = resultId;
        mEventTime = System.currentTimeMillis();
    }

    public static SelectionEvent createSelectionStartedEvent(int invocationMethod, int start)
    {
        return new SelectionEvent(start, start + 1, EVENT_SELECTION_STARTED, TextClassifier.TYPE_UNKNOWN, invocationMethod, null);
    }

    public static SelectionEvent createSelectionModifiedEvent(int start, int end)
    {
        checkRange(start, end);
        return new SelectionEvent(start, end, EVENT_SELECTION_MODIFIED, TextClassifier.TYPE_UNKNOWN, INVOCATION_UNKNOWN, null);
    }

    public static SelectionEvent createSelectionModifiedEvent(int start, int end, TextClassification classification)
    {
        checkRange(start, end);
        String entityType = classification.getEntityCount() > 0 ? classification.getEntity(0) : TextClassifier.TYPE_UNKNOWN;
        return new SelectionEvent(start, end, EVENT_SELECTION_MODIFIED, entityType, INVOCATION_UNKNOWN, classification.getId());
    }

    public static SelectionEvent createSelectionModifiedEvent(int start, int end, TextSelection selection)
    {
        checkRange(start, end);
        String entityType = selection.getEntityCount() > 0 ? selection.getEntity(0) : TextClassifier.TYPE_UNKNOWN;
        return new SelectionEvent(start, end, EVENT_AUTO_SELECTION, entityType, INVOCATION_UNKNOWN, selection.getId());
    }

    public static SelectionEvent createSelectionActionEvent(int start, int end, int actionType)
    {
        checkRange(start, end);
        return new SelectionEvent(start, end, actionType, TextClassifier.TYPE_UNKNOWN, INVOCATION_UNKNOWN, null);
    }

    public static SelectionEvent createSelectionActionEvent(int start, int end, int actionType, TextClassification classification)
    {
        checkRange(start, end);
        String entityType = classification.getEntityCount() > 0 ? classification.getEntity(0) : TextClassifier.TYPE_UNKNOWN;
        return new SelectionEvent(start, end, actionType, entityType, INVOCATION_UNKNOWN, classification.getId());
    }

    private static void checkRange(int start, int end)
    {
        if (end < start) throw new IllegalArgumentException("end cannot be less than start");
    }

    public static boolean isTerminal(int eventType)
    {
        switch (eventType)
        {
        case ACTION_OVERTYPE:
        case ACTION_COPY:
        case ACTION_PASTE:
        case ACTION_CUT:
        case ACTION_SHARE:
        case ACTION_SMART_SHARE:
        case ACTION_DRAG:
        case ACTION_ABANDON:
        case ACTION_OTHER:
            return true;
        default:
            return false;
        }
    }

    public int getEventType()
    {
        return mEventType;
    }

    public String getEntityType()
    {
        return mEntityType;
    }

    public String getPackageName()
    {
        return mPackageName;
    }

    public String getWidgetType()
    {
        return mWidgetType;
    }

    public String getWidgetVersion()
    {
        return mWidgetVersion;
    }

    public int getInvocationMethod()
    {
        return mInvocationMethod;
    }

    public String getResultId()
    {
        return mResultId;
    }

    public long getEventTime()
    {
        return mEventTime;
    }

    public long getDurationSinceSessionStart()
    {
        return mDurationSinceSessionStart;
    }

    public long getDurationSincePreviousEvent()
    {
        return mDurationSincePreviousEvent;
    }

    public int getEventIndex()
    {
        return mEventIndex;
    }

    public TextClassificationSessionId getSessionId()
    {
        return mSessionId;
    }

    public int getStart()
    {
        return mStart;
    }

    public int getEnd()
    {
        return mEnd;
    }

    public int getSmartStart()
    {
        return mSmartStart;
    }

    public int getSmartEnd()
    {
        return mSmartEnd;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mAbsoluteStart, mAbsoluteEnd, mEventType, mEntityType, mWidgetVersion, mPackageName, mWidgetType,
                            mInvocationMethod, mResultId, mEventTime, mEventIndex, mSessionId, mStart, mEnd, mSmartStart, mSmartEnd);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof SelectionEvent)) return false;

        SelectionEvent that = (SelectionEvent)other;
        return mAbsoluteStart == that.mAbsoluteStart && mAbsoluteEnd == that.mAbsoluteEnd && mEventType == that.mEventType
            && Objects.equals(mEntityType, that.mEntityType) && Objects.equals(mWidgetVersion, that.mWidgetVersion)
            && Objects.equals(mPackageName, that.mPackageName) && Objects.equals(mWidgetType, that.mWidgetType)
            && mInvocationMethod == that.mInvocationMethod && Objects.equals(mResultId, that.mResultId) && mEventTime == that.mEventTime
            && mEventIndex == that.mEventIndex && Objects.equals(mSessionId, that.mSessionId) && mStart == that.mStart
            && mEnd == that.mEnd && mSmartStart == that.mSmartStart && mSmartEnd == that.mSmartEnd;
    }

    @Override
    public String toString()
    {
        return "SelectionEvent {absoluteStart=" + mAbsoluteStart + ", absoluteEnd=" + mAbsoluteEnd + ", eventType=" + mEventType
               + ", entityType=" + mEntityType + ", invocationMethod=" + mInvocationMethod + ", resultId=" + mResultId + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mAbsoluteStart);
        dest.writeInt(mAbsoluteEnd);
        dest.writeInt(mEventType);
        dest.writeString(mEntityType);
        dest.writeInt(mInvocationMethod);
        dest.writeString(mResultId);
    }

    public static final Creator<SelectionEvent> CREATOR = new Creator<SelectionEvent>()
    {
        @Override
        public SelectionEvent createFromParcel(Parcel source)
        {
            int start = source.readInt();
            int end = source.readInt();
            int eventType = source.readInt();
            String entityType = source.readString();
            int invocationMethod = source.readInt();
            String resultId = source.readString();
            return new SelectionEvent(start, end, eventType, entityType, invocationMethod, resultId);
        }

        @Override
        public SelectionEvent[] newArray(int size)
        {
            return new SelectionEvent[size];
        }
    };
}
