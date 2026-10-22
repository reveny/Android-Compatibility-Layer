package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.inputmethod.EditorInfo;

import org.xmlpull.v1.XmlPullParser;

public final class SearchableInfo implements Parcelable
{
    private static final String TAG = "SearchableInfo";
    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String MD_LABEL_SEARCHABLE = "android.app.searchable";
    private static final String MD_XML_ELEMENT_SEARCHABLE = "searchable";

    private static final int SEARCH_MODE_QUERY_REWRITE_FROM_DATA = 0x10;
    private static final int SEARCH_MODE_QUERY_REWRITE_FROM_TEXT = 0x20;
    private static final int VOICE_SEARCH_SHOW_BUTTON = 1;
    private static final int VOICE_SEARCH_LAUNCH_WEB_SEARCH = 2;
    private static final int VOICE_SEARCH_LAUNCH_RECOGNIZER = 4;

    private int mLabelId;
    private ComponentName mSearchActivity;
    private int mHintId;
    private int mSearchMode;
    private int mSearchButtonText;
    private int mSearchInputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL;
    private int mSearchImeOptions = EditorInfo.IME_ACTION_GO;
    private boolean mIncludeInGlobalSearch;
    private boolean mQueryAfterZeroResults;
    private boolean mAutoUrlDetect;
    private int mSettingsDescriptionId;
    private String mSuggestAuthority;
    private String mSuggestPath;
    private String mSuggestSelection;
    private String mSuggestIntentAction;
    private String mSuggestIntentData;
    private int mSuggestThreshold;
    private int mVoiceSearchMode;
    private int mVoiceLanguageModeId;
    private int mVoicePromptTextId;
    private int mVoiceLanguageId;
    private int mVoiceMaxResults;
    private String mSuggestProviderPackage;

    SearchableInfo()
    {
    }

    static SearchableInfo getActivityMetaData(Context context, ComponentName component)
    {
        PackageManager packageManager = context.getPackageManager();

        ActivityInfo activity;
        try
        {
            activity = packageManager.getActivityInfo(component, PackageManager.GET_META_DATA);
        }
        catch (PackageManager.NameNotFoundException missing)
        {
            return null;
        }

        XmlResourceParser parser = activity.loadXmlMetaData(packageManager, MD_LABEL_SEARCHABLE);
        if (parser == null) return null;

        try
        {
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (type != XmlPullParser.START_TAG || !MD_XML_ELEMENT_SEARCHABLE.equals(parser.getName())) continue;

                SearchableInfo info = new SearchableInfo();
                info.read(Xml.asAttributeSet(parser), component, context.getPackageName());
                return info.mLabelId == 0 ? null : info;
            }
        }
        catch (Exception failure)
        {
            Log.w(TAG, "could not read searchable metadata of " + component.flattenToShortString(), failure);
        }
        finally
        {
            parser.close();
        }

        return null;
    }

    private void read(AttributeSet attributes, ComponentName component, String packageName)
    {
        mSearchActivity = component;
        mLabelId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "label", 0);
        mHintId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "hint", 0);
        mSearchMode = attributes.getAttributeIntValue(ANDROID_NAMESPACE, "searchMode", 0);
        mSearchButtonText = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "searchButtonText", 0);
        mSearchInputType = attributes.getAttributeIntValue(ANDROID_NAMESPACE, "inputType", mSearchInputType);
        mSearchImeOptions = attributes.getAttributeIntValue(ANDROID_NAMESPACE, "imeOptions", mSearchImeOptions);
        mIncludeInGlobalSearch = attributes.getAttributeBooleanValue(ANDROID_NAMESPACE, "includeInGlobalSearch", false);
        mQueryAfterZeroResults = attributes.getAttributeBooleanValue(ANDROID_NAMESPACE, "queryAfterZeroResults", false);
        mAutoUrlDetect = attributes.getAttributeBooleanValue(ANDROID_NAMESPACE, "autoUrlDetect", false);
        mSettingsDescriptionId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "searchSettingsDescription", 0);
        mSuggestAuthority = attributes.getAttributeValue(ANDROID_NAMESPACE, "searchSuggestAuthority");
        mSuggestPath = attributes.getAttributeValue(ANDROID_NAMESPACE, "searchSuggestPath");
        mSuggestSelection = attributes.getAttributeValue(ANDROID_NAMESPACE, "searchSuggestSelection");
        mSuggestIntentAction = attributes.getAttributeValue(ANDROID_NAMESPACE, "searchSuggestIntentAction");
        mSuggestIntentData = attributes.getAttributeValue(ANDROID_NAMESPACE, "searchSuggestIntentData");
        mSuggestThreshold = attributes.getAttributeIntValue(ANDROID_NAMESPACE, "searchSuggestThreshold", 0);
        mVoiceSearchMode = attributes.getAttributeIntValue(ANDROID_NAMESPACE, "voiceSearchMode", 0);
        mVoiceLanguageModeId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "voiceLanguageModel", 0);
        mVoicePromptTextId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "voicePromptText", 0);
        mVoiceLanguageId = attributes.getAttributeResourceValue(ANDROID_NAMESPACE, "voiceLanguage", 0);
        mVoiceMaxResults = attributes.getAttributeIntValue(ANDROID_NAMESPACE, "voiceMaxResults", 0);

        if (mSuggestAuthority != null) mSuggestProviderPackage = packageName;
    }

    public String getSuggestAuthority()
    {
        return mSuggestAuthority;
    }

    public String getSuggestPackage()
    {
        return mSuggestProviderPackage;
    }

    public ComponentName getSearchActivity()
    {
        return mSearchActivity;
    }

    public boolean shouldRewriteQueryFromData()
    {
        return (mSearchMode & SEARCH_MODE_QUERY_REWRITE_FROM_DATA) != 0;
    }

    public boolean shouldRewriteQueryFromText()
    {
        return (mSearchMode & SEARCH_MODE_QUERY_REWRITE_FROM_TEXT) != 0;
    }

    public int getSettingsDescriptionId()
    {
        return mSettingsDescriptionId;
    }

    public String getSuggestPath()
    {
        return mSuggestPath;
    }

    public String getSuggestSelection()
    {
        return mSuggestSelection;
    }

    public String getSuggestIntentAction()
    {
        return mSuggestIntentAction;
    }

    public String getSuggestIntentData()
    {
        return mSuggestIntentData;
    }

    public int getSuggestThreshold()
    {
        return mSuggestThreshold;
    }

    public int getLabelId()
    {
        return mLabelId;
    }

    public int getHintId()
    {
        return mHintId;
    }

    public int getSearchButtonText()
    {
        return mSearchButtonText;
    }

    public boolean getVoiceSearchEnabled()
    {
        return (mVoiceSearchMode & VOICE_SEARCH_SHOW_BUTTON) != 0;
    }

    public boolean getVoiceSearchLaunchWebSearch()
    {
        return (mVoiceSearchMode & VOICE_SEARCH_LAUNCH_WEB_SEARCH) != 0;
    }

    public boolean getVoiceSearchLaunchRecognizer()
    {
        return (mVoiceSearchMode & VOICE_SEARCH_LAUNCH_RECOGNIZER) != 0;
    }

    public int getVoiceLanguageModeId()
    {
        return mVoiceLanguageModeId;
    }

    public int getVoicePromptTextId()
    {
        return mVoicePromptTextId;
    }

    public int getVoiceLanguageId()
    {
        return mVoiceLanguageId;
    }

    public int getVoiceMaxResults()
    {
        return mVoiceMaxResults;
    }

    public int getInputType()
    {
        return mSearchInputType;
    }

    public int getImeOptions()
    {
        return mSearchImeOptions;
    }

    public boolean shouldIncludeInGlobalSearch()
    {
        return mIncludeInGlobalSearch;
    }

    public boolean queryAfterZeroResults()
    {
        return mQueryAfterZeroResults;
    }

    public boolean autoUrlDetect()
    {
        return mAutoUrlDetect;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mLabelId);
        dest.writeString(mSearchActivity == null ? null : mSearchActivity.flattenToString());
        dest.writeInt(mHintId);
        dest.writeInt(mSearchMode);
        dest.writeInt(mSearchButtonText);
        dest.writeInt(mSearchInputType);
        dest.writeInt(mSearchImeOptions);
        dest.writeInt(mIncludeInGlobalSearch ? 1 : 0);
        dest.writeInt(mQueryAfterZeroResults ? 1 : 0);
        dest.writeInt(mAutoUrlDetect ? 1 : 0);
        dest.writeInt(mSettingsDescriptionId);
        dest.writeString(mSuggestAuthority);
        dest.writeString(mSuggestPath);
        dest.writeString(mSuggestSelection);
        dest.writeString(mSuggestIntentAction);
        dest.writeString(mSuggestIntentData);
        dest.writeInt(mSuggestThreshold);
        dest.writeString(mSuggestProviderPackage);
        dest.writeInt(mVoiceSearchMode);
        dest.writeInt(mVoiceLanguageModeId);
        dest.writeInt(mVoicePromptTextId);
        dest.writeInt(mVoiceLanguageId);
        dest.writeInt(mVoiceMaxResults);
    }

    public static final Parcelable.Creator<SearchableInfo> CREATOR = new Parcelable.Creator<SearchableInfo>()
    {
        @Override
        public SearchableInfo createFromParcel(Parcel in)
        {
            SearchableInfo info = new SearchableInfo();

            info.mLabelId = in.readInt();
            String activity = in.readString();
            info.mSearchActivity = activity == null ? null : ComponentName.unflattenFromString(activity);
            info.mHintId = in.readInt();
            info.mSearchMode = in.readInt();
            info.mSearchButtonText = in.readInt();
            info.mSearchInputType = in.readInt();
            info.mSearchImeOptions = in.readInt();
            info.mIncludeInGlobalSearch = in.readInt() != 0;
            info.mQueryAfterZeroResults = in.readInt() != 0;
            info.mAutoUrlDetect = in.readInt() != 0;
            info.mSettingsDescriptionId = in.readInt();
            info.mSuggestAuthority = in.readString();
            info.mSuggestPath = in.readString();
            info.mSuggestSelection = in.readString();
            info.mSuggestIntentAction = in.readString();
            info.mSuggestIntentData = in.readString();
            info.mSuggestThreshold = in.readInt();
            info.mSuggestProviderPackage = in.readString();
            info.mVoiceSearchMode = in.readInt();
            info.mVoiceLanguageModeId = in.readInt();
            info.mVoicePromptTextId = in.readInt();
            info.mVoiceLanguageId = in.readInt();
            info.mVoiceMaxResults = in.readInt();

            return info;
        }

        @Override
        public SearchableInfo[] newArray(int size)
        {
            return new SearchableInfo[size];
        }
    };
}
