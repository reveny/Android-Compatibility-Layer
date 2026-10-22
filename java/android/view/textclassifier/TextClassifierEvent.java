package android.view.textclassifier;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public abstract class TextClassifierEvent implements Parcelable
{
    public static final int CATEGORY_SELECTION = 1;
    public static final int CATEGORY_LINKIFY = 2;
    public static final int CATEGORY_CONVERSATION_ACTIONS = 3;
    public static final int CATEGORY_LANGUAGE_DETECTION = 4;

    public static final int TYPE_SELECTION_STARTED = 1;
    public static final int TYPE_SELECTION_MODIFIED = 2;
    public static final int TYPE_SMART_SELECTION_SINGLE = 3;
    public static final int TYPE_SMART_SELECTION_MULTI = 4;
    public static final int TYPE_AUTO_SELECTION = 5;
    public static final int TYPE_ACTIONS_SHOWN = 6;
    public static final int TYPE_LINK_CLICKED = 7;
    public static final int TYPE_OVERTYPE = 8;
    public static final int TYPE_COPY_ACTION = 9;
    public static final int TYPE_PASTE_ACTION = 10;
    public static final int TYPE_CUT_ACTION = 11;
    public static final int TYPE_SHARE_ACTION = 12;
    public static final int TYPE_SMART_ACTION = 13;
    public static final int TYPE_SELECTION_DRAG = 14;
    public static final int TYPE_SELECTION_DESTROYED = 15;
    public static final int TYPE_OTHER_ACTION = 16;
    public static final int TYPE_SELECT_ALL = 17;
    public static final int TYPE_SELECTION_RESET = 18;
    public static final int TYPE_MANUAL_REPLY = 19;
    public static final int TYPE_ACTIONS_GENERATED = 20;
    public static final int TYPE_LINKS_GENERATED = 21;

    private final int mEventCategory;
    private final int mEventType;
    private final String[] mEntityTypes;
    private final TextClassificationContext mEventContext;
    private final String mResultId;
    private final int mEventIndex;
    private final float[] mScores;
    private final String mModelName;
    private final int[] mActionIndices;
    private final Bundle mExtras;

    TextClassifierEvent(int eventCategory, int eventType, String[] entityTypes, TextClassificationContext eventContext, String resultId,
                        int eventIndex, float[] scores, String modelName, int[] actionIndices, Bundle extras)
    {
        mEventCategory = eventCategory;
        mEventType = eventType;
        mEntityTypes = entityTypes == null ? new String[0] : entityTypes;
        mEventContext = eventContext;
        mResultId = resultId;
        mEventIndex = eventIndex;
        mScores = scores == null ? new float[0] : scores;
        mModelName = modelName;
        mActionIndices = actionIndices == null ? new int[0] : actionIndices;
        mExtras = extras == null ? Bundle.EMPTY : extras;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mEventCategory);
        dest.writeInt(mEventType);
        dest.writeValue(mEntityTypes);
        dest.writeParcelable(mEventContext, flags);
        dest.writeString(mResultId);
        dest.writeInt(mEventIndex);
        dest.writeValue(mScores);
        dest.writeString(mModelName);
        dest.writeValue(mActionIndices);
        dest.writeBundle(mExtras);
    }

    public static final Creator<TextClassifierEvent> CREATOR = new Creator<TextClassifierEvent>()
    {
        @Override
        public TextClassifierEvent createFromParcel(Parcel source)
        {
            int category = source.readInt();
            int type = source.readInt();
            String[] entityTypes = (String[])source.readValue(null);
            TextClassificationContext context = source.readParcelable(TextClassificationContext.class.getClassLoader());
            String resultId = source.readString();
            int eventIndex = source.readInt();
            float[] scores = (float[])source.readValue(null);
            String modelName = source.readString();
            int[] actionIndices = (int[])source.readValue(null);
            Bundle extras = source.readBundle();
            return new ParceledEvent(category, type, entityTypes, context, resultId, eventIndex, scores, modelName, actionIndices, extras);
        }

        @Override
        public TextClassifierEvent[] newArray(int size)
        {
            return new TextClassifierEvent[size];
        }
    };

    public int getEventCategory()
    {
        return mEventCategory;
    }

    public int getEventType()
    {
        return mEventType;
    }

    public String[] getEntityTypes()
    {
        return mEntityTypes;
    }

    public TextClassificationContext getEventContext()
    {
        return mEventContext;
    }

    public String getResultId()
    {
        return mResultId;
    }

    public int getEventIndex()
    {
        return mEventIndex;
    }

    public float[] getScores()
    {
        return mScores;
    }

    public String getModelName()
    {
        return mModelName;
    }

    public int[] getActionIndices()
    {
        return mActionIndices;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{mEventCategory=" + mEventCategory + ", mEventType=" + mEventType + ", mResultId="
               + mResultId + ", mEventIndex=" + mEventIndex + ", mModelName=" + mModelName + "}";
    }

    private static final class ParceledEvent extends TextClassifierEvent
    {
        ParceledEvent(int eventCategory, int eventType, String[] entityTypes, TextClassificationContext eventContext, String resultId,
                      int eventIndex, float[] scores, String modelName, int[] actionIndices, Bundle extras)
        {
            super(eventCategory, eventType, entityTypes, eventContext, resultId, eventIndex, scores, modelName, actionIndices, extras);
        }
    }
}
