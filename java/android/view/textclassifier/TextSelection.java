package android.view.textclassifier;

import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public final class TextSelection implements Parcelable
{
    private final int mStartIndex;
    private final int mEndIndex;
    private final EntityConfidence mEntityConfidence;
    private final String mId;
    private final TextClassification mTextClassification;
    private final Bundle mExtras;

    private TextSelection(int startIndex, int endIndex, Map<String, Float> entityConfidence, String id,
                          TextClassification textClassification, Bundle extras)
    {
        mStartIndex = startIndex;
        mEndIndex = endIndex;
        mEntityConfidence = new EntityConfidence(entityConfidence);
        mId = id;
        mTextClassification = textClassification;
        mExtras = extras == null ? Bundle.EMPTY : extras;
    }

    public int getSelectionStartIndex()
    {
        return mStartIndex;
    }

    public int getSelectionEndIndex()
    {
        return mEndIndex;
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

    public String getId()
    {
        return mId;
    }

    public TextClassification getTextClassification()
    {
        return mTextClassification;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    @Override
    public String toString()
    {
        return "TextSelection {id=" + mId + ", startIndex=" + mStartIndex + ", endIndex=" + mEndIndex + " entities=" + mEntityConfidence
               + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mStartIndex);
        dest.writeInt(mEndIndex);
        dest.writeValue(mEntityConfidence.toMap());
        dest.writeString(mId);
        dest.writeBundle(mExtras);
        dest.writeParcelable(mTextClassification, flags);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Float> readScores(Parcel source)
    {
        return (Map<String, Float>)source.readValue(null);
    }

    public static final Creator<TextSelection> CREATOR = new Creator<TextSelection>()
    {
        @Override
        public TextSelection createFromParcel(Parcel source)
        {
            int startIndex = source.readInt();
            int endIndex = source.readInt();
            Map<String, Float> scores = readScores(source);
            String id = source.readString();
            Bundle extras = source.readBundle();
            TextClassification classification = source.readParcelable(TextClassification.class.getClassLoader());
            return new TextSelection(startIndex, endIndex, scores, id, classification, extras);
        }

        @Override
        public TextSelection[] newArray(int size)
        {
            return new TextSelection[size];
        }
    };

    public static final class Builder
    {
        private final int mStartIndex;
        private final int mEndIndex;
        private final Map<String, Float> mEntityConfidence = new HashMap<>();
        private String mId;
        private TextClassification mTextClassification;
        private Bundle mExtras;

        public Builder(int startIndex, int endIndex)
        {
            if (startIndex < 0 || endIndex <= startIndex)
            {
                throw new IllegalArgumentException("Invalid range " + startIndex + ".." + endIndex);
            }

            mStartIndex = startIndex;
            mEndIndex = endIndex;
        }

        public Builder setEntityType(String type, float confidenceScore)
        {
            mEntityConfidence.put(type, confidenceScore);
            return this;
        }

        public Builder setId(String id)
        {
            mId = id;
            return this;
        }

        public Builder setTextClassification(TextClassification textClassification)
        {
            mTextClassification = textClassification;
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public TextSelection build()
        {
            return new TextSelection(mStartIndex, mEndIndex, mEntityConfidence, mId, mTextClassification, mExtras);
        }
    }

    public static final class Request implements Parcelable
    {
        private final CharSequence mText;
        private final int mStartIndex;
        private final int mEndIndex;
        private final LocaleList mDefaultLocales;
        private final boolean mIncludeTextClassification;
        private final Bundle mExtras;
        private String mCallingPackageName;

        private Request(CharSequence text, int startIndex, int endIndex, LocaleList defaultLocales, boolean includeTextClassification,
                        Bundle extras)
        {
            mText = text;
            mStartIndex = startIndex;
            mEndIndex = endIndex;
            mDefaultLocales = defaultLocales;
            mIncludeTextClassification = includeTextClassification;
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

        public String getCallingPackageName()
        {
            return mCallingPackageName;
        }

        public boolean shouldIncludeTextClassification()
        {
            return mIncludeTextClassification;
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
            dest.writeInt(mIncludeTextClassification ? 1 : 0);
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
                boolean includeClassification = source.readInt() != 0;
                Bundle extras = source.readBundle();

                Request request = new Request(text, startIndex, endIndex, locales, includeClassification, extras);
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
            private boolean mIncludeTextClassification;
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

            public Builder setIncludeTextClassification(boolean includeTextClassification)
            {
                mIncludeTextClassification = includeTextClassification;
                return this;
            }

            public Builder setExtras(Bundle extras)
            {
                mExtras = extras;
                return this;
            }

            public Request build()
            {
                return new Request(mText.toString(), mStartIndex, mEndIndex, mDefaultLocales, mIncludeTextClassification, mExtras);
            }
        }
    }
}
