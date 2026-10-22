package android.view.textclassifier;

import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface TextClassifier
{
    String TYPE_UNKNOWN = "";
    String TYPE_OTHER = "other";
    String TYPE_EMAIL = "email";
    String TYPE_PHONE = "phone";
    String TYPE_ADDRESS = "address";
    String TYPE_URL = "url";
    String TYPE_DATE = "date";
    String TYPE_DATE_TIME = "datetime";
    String TYPE_FLIGHT_NUMBER = "flight";
    String TYPE_OTP = "otp";
    String EXTRA_TEXT_ORIGIN_PACKAGE = "android.view.textclassifier.extra.TEXT_ORIGIN_PACKAGE";

    String WIDGET_TYPE_TEXTVIEW = "textview";
    String WIDGET_TYPE_EDITTEXT = "edittext";
    String WIDGET_TYPE_UNSELECTABLE_TEXTVIEW = "nosel-textview";
    String WIDGET_TYPE_WEBVIEW = "webview";
    String WIDGET_TYPE_EDIT_WEBVIEW = "edit-webview";
    String WIDGET_TYPE_CUSTOM_TEXTVIEW = "customview";
    String WIDGET_TYPE_CUSTOM_EDITTEXT = "customedit";
    String WIDGET_TYPE_CUSTOM_UNSELECTABLE_TEXTVIEW = "nosel-customview";
    String WIDGET_TYPE_NOTIFICATION = "notification";
    String WIDGET_TYPE_CLIPBOARD = "clipboard";
    String WIDGET_TYPE_UNKNOWN = "unknown";

    String HINT_TEXT_IS_EDITABLE = "android.text_is_editable";
    String HINT_TEXT_IS_NOT_EDITABLE = "android.text_is_not_editable";

    String EXTRA_FROM_TEXT_CLASSIFIER = "android.view.textclassifier.extra.FROM_TEXT_CLASSIFIER";

    TextClassifier NO_OP = new TextClassifier()
    {
        @Override
        public String toString()
        {
            return "TextClassifier.NO_OP";
        }
    };

    default TextSelection suggestSelection(TextSelection.Request request)
    {
        return new TextSelection.Builder(request.getStartIndex(), request.getEndIndex()).build();
    }

    default TextSelection suggestSelection(CharSequence text, int selectionStartIndex, int selectionEndIndex, LocaleList defaultLocales)
    {
        TextSelection.Request request = new TextSelection.Request.Builder(text, selectionStartIndex, selectionEndIndex)
                                            .setDefaultLocales(defaultLocales)
                                            .build();
        return suggestSelection(request);
    }

    default TextClassification classifyText(TextClassification.Request request)
    {
        return TextClassification.EMPTY;
    }

    default TextClassification classifyText(CharSequence text, int startIndex, int endIndex, LocaleList defaultLocales)
    {
        TextClassification.Request request = new TextClassification.Request.Builder(text, startIndex, endIndex)
                                                 .setDefaultLocales(defaultLocales)
                                                 .build();
        return classifyText(request);
    }

    default TextLinks generateLinks(TextLinks.Request request)
    {
        return new TextLinks.Builder(request.getText().toString()).build();
    }

    default int getMaxGenerateLinksTextLength()
    {
        return Integer.MAX_VALUE;
    }

    default void onSelectionEvent(SelectionEvent event)
    {
    }

    default void onTextClassifierEvent(TextClassifierEvent event)
    {
    }

    default void destroy()
    {
    }

    default boolean isDestroyed()
    {
        return false;
    }

    final class EntityConfig implements Parcelable
    {
        private final List<String> mIncludedTypes;
        private final List<String> mExcludedTypes;
        private final List<String> mHints;
        private final boolean mIncludeTypesFromTextClassifier;

        private EntityConfig(Collection<String> includedTypes, Collection<String> excludedTypes, Collection<String> hints,
                             boolean includeTypesFromTextClassifier)
        {
            mIncludedTypes = includedTypes == null ? new ArrayList<>() : new ArrayList<>(includedTypes);
            mExcludedTypes = excludedTypes == null ? new ArrayList<>() : new ArrayList<>(excludedTypes);
            mHints = hints == null ? new ArrayList<>() : new ArrayList<>(hints);
            mIncludeTypesFromTextClassifier = includeTypesFromTextClassifier;
        }

        public static EntityConfig createWithHints(Collection<String> hints)
        {
            return new EntityConfig(null, null, hints, true);
        }

        public static EntityConfig create(Collection<String> hints, Collection<String> includedEntityTypes,
                                          Collection<String> excludedEntityTypes)
        {
            return new EntityConfig(includedEntityTypes, excludedEntityTypes, hints, true);
        }

        public static EntityConfig createWithExplicitEntityList(Collection<String> entityTypes)
        {
            return new EntityConfig(entityTypes, null, null, false);
        }

        public Collection<String> resolveEntityListModifications(Collection<String> entityTypes)
        {
            Set<String> resolved = new HashSet<>();

            if (mIncludeTypesFromTextClassifier && entityTypes != null) resolved.addAll(entityTypes);

            resolved.addAll(mIncludedTypes);
            resolved.removeAll(mExcludedTypes);
            return resolved;
        }

        public Collection<String> getHints()
        {
            return new ArrayList<>(mHints);
        }

        public boolean shouldIncludeTypesFromTextClassifier()
        {
            return mIncludeTypesFromTextClassifier;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeValue(mIncludedTypes);
            dest.writeValue(mExcludedTypes);
            dest.writeValue(mHints);
            dest.writeInt(mIncludeTypesFromTextClassifier ? 1 : 0);
        }

        @SuppressWarnings("unchecked")
        private static Collection<String> readStrings(Parcel source)
        {
            return (Collection<String>)source.readValue(null);
        }

        public static final Creator<EntityConfig> CREATOR = new Creator<EntityConfig>()
        {
            @Override
            public EntityConfig createFromParcel(Parcel source)
            {
                Collection<String> includedTypes = readStrings(source);
                Collection<String> excludedTypes = readStrings(source);
                Collection<String> hints = readStrings(source);
                return new EntityConfig(includedTypes, excludedTypes, hints, source.readInt() != 0);
            }

            @Override
            public EntityConfig[] newArray(int size)
            {
                return new EntityConfig[size];
            }
        };

        public static final class Builder
        {
            private Collection<String> mIncludedTypes;
            private Collection<String> mExcludedTypes;
            private Collection<String> mHints;
            private boolean mIncludeTypesFromTextClassifier = true;

            public Builder()
            {
            }

            public Builder setIncludedTypes(Collection<String> includedTypes)
            {
                mIncludedTypes = includedTypes;
                return this;
            }

            public Builder setExcludedTypes(Collection<String> excludedTypes)
            {
                mExcludedTypes = excludedTypes;
                return this;
            }

            public Builder includeTypesFromTextClassifier(boolean includeTypesFromTextClassifier)
            {
                mIncludeTypesFromTextClassifier = includeTypesFromTextClassifier;
                return this;
            }

            public Builder setHints(Collection<String> hints)
            {
                mHints = hints;
                return this;
            }

            public EntityConfig build()
            {
                return new EntityConfig(mIncludedTypes, mExcludedTypes, mHints, mIncludeTypesFromTextClassifier);
            }
        }
    }
}
