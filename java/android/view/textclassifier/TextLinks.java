package android.view.textclassifier;

import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.View;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class TextLinks implements Parcelable
{
    public static final int STATUS_LINKS_APPLIED = 0;
    public static final int STATUS_NO_LINKS_FOUND = 1;
    public static final int STATUS_NO_LINKS_APPLIED = 2;
    public static final int STATUS_DIFFERENT_TEXT = 3;
    public static final int STATUS_UNSUPPORTED_CHARACTER = 4;

    public static final int APPLY_STRATEGY_IGNORE = 0;
    public static final int APPLY_STRATEGY_REPLACE = 1;

    private final String mFullText;
    private final List<TextLink> mLinks;
    private final Bundle mExtras;

    private TextLinks(String fullText, List<TextLink> links, Bundle extras)
    {
        mFullText = fullText;
        mLinks = Collections.unmodifiableList(new ArrayList<>(links));
        mExtras = extras == null ? Bundle.EMPTY : extras;
    }

    public CharSequence getText()
    {
        return mFullText;
    }

    public Collection<TextLink> getLinks()
    {
        return mLinks;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public int apply(Spannable text, int applyStrategy, Function<TextLink, TextLinkSpan> spanFactory)
    {
        if (text == null) throw new NullPointerException("text");
        if (applyStrategy != APPLY_STRATEGY_IGNORE && applyStrategy != APPLY_STRATEGY_REPLACE)
        {
            throw new IllegalArgumentException("Invalid apply strategy.");
        }

        if (mLinks.isEmpty()) return STATUS_NO_LINKS_FOUND;
        if (!mFullText.equals(text.toString())) return STATUS_DIFFERENT_TEXT;

        int applied = 0;
        for (TextLink link : mLinks)
        {
            TextLinkSpan span = spanFactory != null ? spanFactory.apply(link) : new TextLinkSpan(link);
            if (span == null) continue;

            ClickableSpan[] existing = text.getSpans(link.getStart(), link.getEnd(), ClickableSpan.class);
            if (existing != null && existing.length > 0)
            {
                if (applyStrategy == APPLY_STRATEGY_IGNORE) continue;

                for (ClickableSpan old : existing)
                {
                    text.removeSpan(old);
                }
            }

            text.setSpan(span, link.getStart(), link.getEnd(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            applied++;
        }

        return applied == 0 ? STATUS_NO_LINKS_APPLIED : STATUS_LINKS_APPLIED;
    }

    @Override
    public String toString()
    {
        return "TextLinks{fullText=" + mFullText + ", links=" + mLinks + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(mFullText);
        dest.writeValue(new ArrayList<>(mLinks));
        dest.writeBundle(mExtras);
    }

    @SuppressWarnings("unchecked")
    private static <T> T readTyped(Parcel source)
    {
        return (T)source.readValue(null);
    }

    public static final Creator<TextLinks> CREATOR = new Creator<TextLinks>()
    {
        @Override
        public TextLinks createFromParcel(Parcel source)
        {
            String fullText = source.readString();
            List<TextLink> links = readTyped(source);
            Bundle extras = source.readBundle();
            return new TextLinks(fullText, links == null ? new ArrayList<>() : links, extras);
        }

        @Override
        public TextLinks[] newArray(int size)
        {
            return new TextLinks[size];
        }
    };

    public static final class TextLink implements Parcelable
    {
        private final EntityConfidence mEntityScores;
        private final int mStart;
        private final int mEnd;
        private final Bundle mExtras;

        private TextLink(int start, int end, Map<String, Float> entityScores, Bundle extras)
        {
            if (entityScores == null || entityScores.isEmpty()) throw new IllegalArgumentException("entityScores");
            if (start >= end) throw new IllegalArgumentException("start must be smaller than end");

            mStart = start;
            mEnd = end;
            mEntityScores = new EntityConfidence(entityScores);
            mExtras = extras == null ? Bundle.EMPTY : extras;
        }

        public int getStart()
        {
            return mStart;
        }

        public int getEnd()
        {
            return mEnd;
        }

        public int getEntityCount()
        {
            return mEntityScores.getEntityCount();
        }

        public String getEntity(int index)
        {
            return mEntityScores.getEntity(index);
        }

        public float getConfidenceScore(String entityType)
        {
            return mEntityScores.getConfidenceScore(entityType);
        }

        public Bundle getExtras()
        {
            return mExtras;
        }

        @Override
        public String toString()
        {
            return "TextLink{start=" + mStart + ", end=" + mEnd + ", entityScores=" + mEntityScores + "}";
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeValue(mEntityScores.toMap());
            dest.writeInt(mStart);
            dest.writeInt(mEnd);
            dest.writeBundle(mExtras);
        }

        public static final Creator<TextLink> CREATOR = new Creator<TextLink>()
        {
            @Override
            public TextLink createFromParcel(Parcel source)
            {
                Map<String, Float> scores = readTyped(source);
                int start = source.readInt();
                int end = source.readInt();
                Bundle extras = source.readBundle();
                return new TextLink(start, end, scores, extras);
            }

            @Override
            public TextLink[] newArray(int size)
            {
                return new TextLink[size];
            }
        };
    }

    public static class TextLinkSpan extends ClickableSpan
    {
        private final TextLink mTextLink;

        public TextLinkSpan(TextLink textLink)
        {
            mTextLink = textLink;
        }

        @Override
        public void onClick(View widget)
        {
        }

        public final TextLink getTextLink()
        {
            return mTextLink;
        }
    }

    public static final class Request implements Parcelable
    {
        private final CharSequence mText;
        private final LocaleList mDefaultLocales;
        private final TextClassifier.EntityConfig mEntityConfig;
        private final ZonedDateTime mReferenceTime;
        private final Bundle mExtras;
        private String mCallingPackageName;

        private Request(CharSequence text, LocaleList defaultLocales, TextClassifier.EntityConfig entityConfig, ZonedDateTime referenceTime,
                        Bundle extras)
        {
            mText = text;
            mDefaultLocales = defaultLocales;
            mEntityConfig = entityConfig;
            mReferenceTime = referenceTime;
            mExtras = extras == null ? Bundle.EMPTY : extras;
        }

        public CharSequence getText()
        {
            return mText;
        }

        public LocaleList getDefaultLocales()
        {
            return mDefaultLocales;
        }

        public TextClassifier.EntityConfig getEntityConfig()
        {
            return mEntityConfig;
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
            dest.writeValue(mDefaultLocales);
            dest.writeParcelable(mEntityConfig, flags);
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
                LocaleList locales = (LocaleList)source.readValue(null);
                TextClassifier.EntityConfig entityConfig = source.readParcelable(TextClassifier.EntityConfig.class.getClassLoader());
                ZonedDateTime referenceTime = (ZonedDateTime)source.readValue(null);
                Bundle extras = source.readBundle();

                Request request = new Request(text, locales, entityConfig, referenceTime, extras);
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
            private LocaleList mDefaultLocales;
            private TextClassifier.EntityConfig mEntityConfig;
            private ZonedDateTime mReferenceTime;
            private Bundle mExtras;

            public Builder(CharSequence text)
            {
                if (text == null) throw new NullPointerException("text");

                mText = text;
            }

            public Builder setDefaultLocales(LocaleList defaultLocales)
            {
                mDefaultLocales = defaultLocales;
                return this;
            }

            public Builder setEntityConfig(TextClassifier.EntityConfig entityConfig)
            {
                mEntityConfig = entityConfig;
                return this;
            }

            public Builder setExtras(Bundle extras)
            {
                mExtras = extras;
                return this;
            }

            public Builder setReferenceTime(ZonedDateTime referenceTime)
            {
                mReferenceTime = referenceTime;
                return this;
            }

            public Request build()
            {
                return new Request(mText, mDefaultLocales, mEntityConfig, mReferenceTime, mExtras);
            }
        }
    }

    public static final class Builder
    {
        private final String mFullText;
        private final ArrayList<TextLink> mLinks = new ArrayList<>();
        private Bundle mExtras;

        public Builder(String fullText)
        {
            if (fullText == null) throw new NullPointerException("fullText");

            mFullText = fullText;
        }

        public Builder addLink(int start, int end, Map<String, Float> entityScores)
        {
            return addLink(start, end, entityScores, Bundle.EMPTY);
        }

        public Builder addLink(int start, int end, Map<String, Float> entityScores, Bundle extras)
        {
            mLinks.add(new TextLink(start, end, entityScores, extras));
            return this;
        }

        public Builder clearTextLinks()
        {
            mLinks.clear();
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public TextLinks build()
        {
            return new TextLinks(mFullText, mLinks, mExtras);
        }
    }
}
