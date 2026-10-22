package android.app;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class RemoteInput implements Parcelable
{
    public static final String EXTRA_RESULTS_DATA = "android.remoteinput.resultsData";
    public static final String RESULTS_CLIP_LABEL = "android.remoteinput.results";

    public static final int SOURCE_FREE_FORM_INPUT = 0;
    public static final int SOURCE_CHOICE = 1;

    public static final int EDIT_CHOICES_BEFORE_SENDING_AUTO = 0;
    public static final int EDIT_CHOICES_BEFORE_SENDING_DISABLED = 1;
    public static final int EDIT_CHOICES_BEFORE_SENDING_ENABLED = 2;

    private static final String EXTRA_DATA_TYPE_RESULTS_DATA = "android.remoteinput.dataTypeResultsData";
    private static final String EXTRA_RESULTS_SOURCE = "android.remoteinput.resultsSource";
    private static final int FLAG_DISALLOW_FREE_FORM_INPUT = 0x1;

    public static final Parcelable.Creator<RemoteInput> CREATOR = new Parcelable.Creator<RemoteInput>()
    {
        @Override
        public RemoteInput createFromParcel(Parcel source)
        {
            RemoteInput input = new RemoteInput();
            input.mResultKey = source.readString();
            input.mLabel = (CharSequence)source.readValue(null);
            input.mChoices = (CharSequence[])source.readValue(null);
            input.mFlags = source.readInt();
            input.mEditChoicesBeforeSending = source.readInt();
            Bundle extras = source.readBundle();
            input.mExtras = extras == null ? new Bundle() : extras;

            String[] types = source.createStringArray();
            if (types != null)
            {
                for (String type : types) input.mAllowedDataTypes.add(type);
            }

            return input;
        }

        @Override
        public RemoteInput[] newArray(int size)
        {
            return new RemoteInput[size];
        }
    };

    private String mResultKey;
    private CharSequence mLabel;
    private CharSequence[] mChoices;
    private int mFlags;
    private int mEditChoicesBeforeSending;
    private Bundle mExtras = new Bundle();
    private final Set<String> mAllowedDataTypes = new LinkedHashSet<>();

    RemoteInput()
    {
    }

    public String getResultKey()
    {
        return mResultKey;
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public CharSequence[] getChoices()
    {
        return mChoices;
    }

    public Set<String> getAllowedDataTypes()
    {
        return mAllowedDataTypes;
    }

    public boolean isDataOnly()
    {
        return !getAllowFreeFormInput() && (mChoices == null || mChoices.length == 0) && !mAllowedDataTypes.isEmpty();
    }

    public boolean getAllowFreeFormInput()
    {
        return (mFlags & FLAG_DISALLOW_FREE_FORM_INPUT) == 0;
    }

    public int getEditChoicesBeforeSending()
    {
        return mEditChoicesBeforeSending;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    private static Intent getClipDataIntentFromIntent(Intent intent)
    {
        ClipData clipData = intent.getClipData();
        if (clipData == null) return null;

        ClipDescription description = clipData.getDescription();
        if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_INTENT)) return null;
        if (description.getLabel() == null || !RESULTS_CLIP_LABEL.contentEquals(description.getLabel())) return null;

        return clipData.getItemAt(0).getIntent();
    }

    private static String getExtraResultsKeyForData(String mimeType)
    {
        return EXTRA_DATA_TYPE_RESULTS_DATA + mimeType;
    }

    public static Map<String, Uri> getDataResultsFromIntent(Intent intent, String remoteInputResultKey)
    {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null || clipDataIntent.getExtras() == null) return null;

        Map<String, Uri> results = new HashMap<>();
        Bundle extras = clipDataIntent.getExtras();

        for (String key : extras.keySet())
        {
            if (!key.startsWith(EXTRA_DATA_TYPE_RESULTS_DATA)) continue;

            String mimeType = key.substring(EXTRA_DATA_TYPE_RESULTS_DATA.length());
            if (mimeType.isEmpty()) continue;

            Bundle bundle = clipDataIntent.getBundleExtra(key);
            String uriString = bundle == null ? null : bundle.getString(remoteInputResultKey);
            if (uriString == null || uriString.isEmpty()) continue;

            results.put(mimeType, Uri.parse(uriString));
        }

        return results.isEmpty() ? null : results;
    }

    public static Bundle getResultsFromIntent(Intent intent)
    {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) return null;

        return clipDataIntent.getBundleExtra(EXTRA_RESULTS_DATA);
    }

    public static void addResultsToIntent(RemoteInput[] remoteInputs, Intent intent, Bundle results)
    {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) clipDataIntent = new Intent();

        Bundle resultsBundle = clipDataIntent.getBundleExtra(EXTRA_RESULTS_DATA);
        if (resultsBundle == null) resultsBundle = new Bundle();

        for (RemoteInput remoteInput : remoteInputs)
        {
            Object result = results.get(remoteInput.getResultKey());
            if (result instanceof CharSequence) resultsBundle.putCharSequence(remoteInput.getResultKey(), (CharSequence)result);
        }

        clipDataIntent.putExtra(EXTRA_RESULTS_DATA, resultsBundle);
        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
    }

    public static void addDataResultToIntent(RemoteInput remoteInput, Intent intent, Map<String, Uri> results)
    {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) clipDataIntent = new Intent();

        for (Map.Entry<String, Uri> entry : results.entrySet())
        {
            String mimeType = entry.getKey();
            Uri uri = entry.getValue();
            if (mimeType == null) continue;

            Bundle resultsBundle = clipDataIntent.getBundleExtra(getExtraResultsKeyForData(mimeType));
            if (resultsBundle == null) resultsBundle = new Bundle();

            resultsBundle.putString(remoteInput.getResultKey(), uri == null ? null : uri.toString());
            clipDataIntent.putExtra(getExtraResultsKeyForData(mimeType), resultsBundle);
        }

        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
    }

    public static void setResultsSource(Intent intent, int source)
    {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) clipDataIntent = new Intent();

        clipDataIntent.putExtra(EXTRA_RESULTS_SOURCE, source);
        intent.setClipData(ClipData.newIntent(RESULTS_CLIP_LABEL, clipDataIntent));
    }

    public static int getResultsSource(Intent intent)
    {
        Intent clipDataIntent = getClipDataIntentFromIntent(intent);
        if (clipDataIntent == null) return SOURCE_FREE_FORM_INPUT;

        return clipDataIntent.getExtras() == null ? SOURCE_FREE_FORM_INPUT
                                                  : clipDataIntent.getIntExtra(EXTRA_RESULTS_SOURCE, SOURCE_FREE_FORM_INPUT);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeString(mResultKey);
        parcel.writeValue(mLabel);
        parcel.writeValue(mChoices == null ? null : mChoices.clone());
        parcel.writeInt(mFlags);
        parcel.writeInt(mEditChoicesBeforeSending);
        parcel.writeBundle(mExtras);
        parcel.writeStringArray(mAllowedDataTypes.toArray(new String[0]));
    }

    public static final class Builder
    {
        private final String mResultKey;
        private final Set<String> mAllowedDataTypes = new LinkedHashSet<>();
        private final Bundle mExtras = new Bundle();
        private CharSequence mLabel;
        private CharSequence[] mChoices;
        private int mFlags;
        private int mEditChoicesBeforeSending = EDIT_CHOICES_BEFORE_SENDING_AUTO;

        public Builder(String resultKey)
        {
            if (resultKey == null) throw new IllegalArgumentException("Result key can't be null");

            mResultKey = resultKey;
        }

        public Builder setLabel(CharSequence label)
        {
            mLabel = label;
            return this;
        }

        public Builder setChoices(CharSequence[] choices)
        {
            mChoices = choices == null ? null : choices.clone();
            return this;
        }

        public Builder setAllowDataType(String mimeType, boolean doAllow)
        {
            if (doAllow) mAllowedDataTypes.add(mimeType);
            else mAllowedDataTypes.remove(mimeType);

            return this;
        }

        public Builder setAllowFreeFormInput(boolean allowFreeFormTextInput)
        {
            if (allowFreeFormTextInput) mFlags &= ~FLAG_DISALLOW_FREE_FORM_INPUT;
            else mFlags |= FLAG_DISALLOW_FREE_FORM_INPUT;

            return this;
        }

        public Builder setEditChoicesBeforeSending(int editChoicesBeforeSending)
        {
            if (editChoicesBeforeSending != EDIT_CHOICES_BEFORE_SENDING_AUTO
                && editChoicesBeforeSending != EDIT_CHOICES_BEFORE_SENDING_DISABLED
                && editChoicesBeforeSending != EDIT_CHOICES_BEFORE_SENDING_ENABLED)
            {
                throw new IllegalArgumentException("Invalid value: " + editChoicesBeforeSending);
            }

            mEditChoicesBeforeSending = editChoicesBeforeSending;
            return this;
        }

        public Builder addExtras(Bundle extras)
        {
            if (extras != null) mExtras.putAll(extras);
            return this;
        }

        public Bundle getExtras()
        {
            return mExtras;
        }

        public RemoteInput build()
        {
            if (mEditChoicesBeforeSending == EDIT_CHOICES_BEFORE_SENDING_ENABLED
                && (mFlags & FLAG_DISALLOW_FREE_FORM_INPUT) != 0)
            {
                throw new IllegalArgumentException("setEditChoicesBeforeSending requires setAllowFreeFormInput");
            }

            RemoteInput input = new RemoteInput();
            input.mResultKey = mResultKey;
            input.mLabel = mLabel;
            input.mChoices = mChoices;
            input.mFlags = mFlags;
            input.mEditChoicesBeforeSending = mEditChoicesBeforeSending;
            input.mExtras = mExtras;
            input.mAllowedDataTypes.addAll(mAllowedDataTypes);
            return input;
        }
    }
}
