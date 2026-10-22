package android.view.textclassifier;

import android.app.RemoteAction;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TextClassification implements Parcelable
{
    static final TextClassification EMPTY = new TextClassification.Builder().build();

    private final String mText;
    private final Drawable mLegacyIcon;
    private final String mLegacyLabel;
    private final Intent mLegacyIntent;
    private final View.OnClickListener mLegacyOnClickListener;
    private final List<RemoteAction> mActions;
    private final EntityConfidence mEntityConfidence;
    private final String mId;
    private final Bundle mExtras;

    private TextClassification(String text, Drawable legacyIcon, String legacyLabel, Intent legacyIntent,
                               View.OnClickListener legacyOnClickListener, List<RemoteAction> actions, Map<String, Float> entityConfidence,
                               String id, Bundle extras)
    {
        mText = text;
        mLegacyIcon = legacyIcon;
        mLegacyLabel = legacyLabel;
        mLegacyIntent = legacyIntent;
        mLegacyOnClickListener = legacyOnClickListener;
        mActions = new ArrayList<>(actions);
        mEntityConfidence = new EntityConfidence(entityConfidence);
        mId = id;
        mExtras = extras == null ? Bundle.EMPTY : extras;
    }

    public String getText()
    {
        return mText;
    }

    public int getEntityCount()
    {
        return mEntityConfidence.getEntityCount();
    }

    public String getEntity(int index)
    {
        return mEntityConfidence.getEntity(index);
    }

    public float getConfidenceScore(String entity)
    {
        return mEntityConfidence.getConfidenceScore(entity);
    }

    public List<RemoteAction> getActions()
    {
        return new ArrayList<>(mActions);
    }

    public Drawable getIcon()
    {
        return mLegacyIcon;
    }

    public CharSequence getLabel()
    {
        return mLegacyLabel;
    }

    public Intent getIntent()
    {
        return mLegacyIntent;
    }

    public View.OnClickListener getOnClickListener()
    {
        return mLegacyOnClickListener;
    }

    public String getId()
    {
        return mId;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    @Override
    public String toString()
    {
        return "TextClassification {text=" + mText + ", entities=" + mEntityConfidence + ", actions=" + mActions + ", id=" + mId + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mText);
        dest.writeValue(new ArrayList<>(mActions));
        dest.writeValue(mEntityConfidence.toMap());
        dest.writeString(mId);
        dest.writeBundle(mExtras);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readTyped(Parcel source)
    {
        return (T)source.readValue(null);
    }

    public static final Creator<TextClassification> CREATOR = new Creator<TextClassification>()
    {
        @Override
        public TextClassification createFromParcel(Parcel source)
        {
            String text = source.readString();
            List<RemoteAction> actions = readTyped(source);
            Map<String, Float> scores = readTyped(source);
            String id = source.readString();
            Bundle extras = source.readBundle();
            return new TextClassification(text, null, null, null, null, actions == null ? new ArrayList<>() : actions, scores, id, extras);
        }

        @Override
        public TextClassification[] newArray(int size)
        {
            return new TextClassification[size];
        }
    };

    public static final class Builder
    {
        private final List<RemoteAction> mActions = new ArrayList<>();
        private final Map<String, Float> mTypeScoreMap = new HashMap<>();
        private String mText;
        private Drawable mLegacyIcon;
        private String mLegacyLabel;
        private Intent mLegacyIntent;
        private View.OnClickListener mLegacyOnClickListener;
        private String mId;
        private Bundle mExtras;

        public Builder()
        {
        }

        public Builder setText(String text)
        {
            mText = text;
            return this;
        }

        public Builder setEntityType(String type, float confidenceScore)
        {
            mTypeScoreMap.put(type, confidenceScore);
            return this;
        }

        public Builder addAction(RemoteAction action)
        {
            if (action == null) throw new IllegalArgumentException("action");

            mActions.add(action);
            return this;
        }

        public Builder setIcon(Drawable icon)
        {
            mLegacyIcon = icon;
            return this;
        }

        public Builder setLabel(String label)
        {
            mLegacyLabel = label;
            return this;
        }

        public Builder setIntent(Intent intent)
        {
            mLegacyIntent = intent;
            return this;
        }

        public Builder setOnClickListener(View.OnClickListener onClickListener)
        {
            mLegacyOnClickListener = onClickListener;
            return this;
        }

        public Builder setId(String id)
        {
            mId = id;
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public TextClassification build()
        {
            return new TextClassification(mText, mLegacyIcon, mLegacyLabel, mLegacyIntent, mLegacyOnClickListener, mActions, mTypeScoreMap,
                                          mId, mExtras);
        }
    }

    public static final class Request implements Parcelable
    {
        private final CharSequence mText;
        private final int mStartIndex;
        private final int mEndIndex;
        private final LocaleList mDefaultLocales;
        private final ZonedDateTime mReferenceTime;
        private final Bundle mExtras;
        private String mCallingPackageName;

        private Request(CharSequence text, int startIndex, int endIndex, LocaleList defaultLocales, ZonedDateTime referenceTime,
                        Bundle extras)
        {
            mText = text;
            mStartIndex = startIndex;
            mEndIndex = endIndex;
            mDefaultLocales = defaultLocales;
            mReferenceTime = referenceTime;
            mExtras = extras == null ? Bundle.EMPTY : extras;
        }

        public CharSequence getText()
        {
            return mText;
        }

        public int getStartIndex()
        {
            return mStartIndex;
        }

        public int getEndIndex()
        {
            return mEndIndex;
        }

        public LocaleList getDefaultLocales()
        {
            return mDefaultLocales;
        }

        public ZonedDateTime getReferenceTime()
        {
            return mReferenceTime;
        }

        public String getCallingPackageName()
        {
            return mCallingPackageName;
        }

        public Bundle getExtras()
        {
            return mExtras;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeValue(mText);
            dest.writeInt(mStartIndex);
            dest.writeInt(mEndIndex);
            dest.writeValue(mDefaultLocales);
            dest.writeValue(mReferenceTime);
            dest.writeBundle(mExtras);
            dest.writeString(mCallingPackageName);
        }

        public static final Creator<Request> CREATOR = new Creator<Request>()
        {
            @Override
            public Request createFromParcel(Parcel source)
            {
                CharSequence text = (CharSequence)source.readValue(null);
                int startIndex = source.readInt();
                int endIndex = source.readInt();
                LocaleList locales = (LocaleList)source.readValue(null);
                ZonedDateTime referenceTime = (ZonedDateTime)source.readValue(null);
                Bundle extras = source.readBundle();

                Request request = new Request(text, startIndex, endIndex, locales, referenceTime, extras);
                request.mCallingPackageName = source.readString();
                return request;
            }

            @Override
            public Request[] newArray(int size)
            {
                return new Request[size];
            }
        };

        public static final class Builder
        {
            private final CharSequence mText;
            private final int mStartIndex;
            private final int mEndIndex;
            private LocaleList mDefaultLocales;
            private ZonedDateTime mReferenceTime;
            private Bundle mExtras;

            public Builder(CharSequence text, int startIndex, int endIndex)
            {
                if (text == null) throw new NullPointerException("text");
                if (startIndex < 0 || endIndex > text.length() || startIndex >= endIndex)
                {
                    throw new IllegalArgumentException("Invalid range " + startIndex + ".." + endIndex + " for text of length "
                                                       + text.length());
                }

                mText = text;
                mStartIndex = startIndex;
                mEndIndex = endIndex;
            }

            public Builder setDefaultLocales(LocaleList defaultLocales)
            {
                mDefaultLocales = defaultLocales;
                return this;
            }

            public Builder setReferenceTime(ZonedDateTime referenceTime)
            {
                mReferenceTime = referenceTime;
                return this;
            }

            public Builder setExtras(Bundle extras)
            {
                mExtras = extras;
                return this;
            }

            public Request build()
            {
                return new Request(mText.toString(), mStartIndex, mEndIndex, mDefaultLocales, mReferenceTime, mExtras);
            }
        }
    }
}
