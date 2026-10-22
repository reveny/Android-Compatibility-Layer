package android.view;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Pair;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

import java.util.ArrayList;
import java.util.List;

public class RuntimeViewStructure extends ViewStructure
{
    private final ArrayList<RuntimeViewStructure> mChildren = new ArrayList<>();
    private final Rect mDimensions = new Rect();

    private int mId = View.NO_ID;
    private String mIdPackage;
    private String mIdType;
    private String mIdEntry;
    private int mScrollX;
    private int mScrollY;
    private Matrix mTransformation;
    private float mElevation;
    private float mAlpha = 1.0f;
    private int mVisibility = View.VISIBLE;
    private boolean mEnabled = true;
    private boolean mClickable;
    private boolean mLongClickable;
    private boolean mContextClickable;
    private boolean mFocusable;
    private boolean mFocused;
    private boolean mAccessibilityFocused;
    private boolean mCheckable;
    private boolean mChecked;
    private boolean mSelected;
    private boolean mActivated;
    private boolean mOpaque;
    private String mClassName;
    private CharSequence mContentDescription;
    private CharSequence mText;
    private int mTextSelectionStart = -1;
    private int mTextSelectionEnd = -1;
    private float mTextSize;
    private int mTextColor;
    private int mTextBackgroundColor;
    private int mTextStyle;
    private int[] mLineCharOffsets;
    private int[] mLineBaselines;
    private String mTextIdEntry;
    private CharSequence mHint;
    private String mHintIdEntry;
    private Bundle mExtras;
    private AutofillId mAutofillId;
    private int mAutofillType;
    private String[] mAutofillHints;
    private AutofillValue mAutofillValue;
    private CharSequence[] mAutofillOptions;
    private int mImportantForAutofill;
    private String[] mReceiveContentMimeTypes;
    private int mInputType;
    private boolean mDataIsSensitive;
    private int mMinTextEms = -1;
    private int mMaxTextEms = -1;
    private int mMaxTextLength = -1;
    private String mWebDomain;
    private LocaleList mLocaleList;
    private HtmlInfo mHtmlInfo;

    public RuntimeViewStructure()
    {
    }

    @Override
    public void setId(int id, String packageName, String typeName, String entryName)
    {
        mId = id;
        mIdPackage = packageName;
        mIdType = typeName;
        mIdEntry = entryName;
    }

    public int getId()
    {
        return mId;
    }

    public String getIdPackage()
    {
        return mIdPackage;
    }

    public String getIdType()
    {
        return mIdType;
    }

    public String getIdEntry()
    {
        return mIdEntry;
    }

    @Override
    public void setDimens(int left, int top, int scrollX, int scrollY, int width, int height)
    {
        mDimensions.set(left, top, left + width, top + height);
        mScrollX = scrollX;
        mScrollY = scrollY;
    }

    public Rect getDimensions()
    {
        return new Rect(mDimensions);
    }

    public int getScrollX()
    {
        return mScrollX;
    }

    public int getScrollY()
    {
        return mScrollY;
    }

    @Override
    public void setTransformation(Matrix matrix)
    {
        mTransformation = matrix == null ? null : new Matrix(matrix);
    }

    public Matrix getTransformation()
    {
        return mTransformation;
    }

    @Override
    public void setElevation(float elevation)
    {
        mElevation = elevation;
    }

    public float getElevation()
    {
        return mElevation;
    }

    @Override
    public void setAlpha(float alpha)
    {
        mAlpha = alpha;
    }

    public float getAlpha()
    {
        return mAlpha;
    }

    @Override
    public void setVisibility(int visibility)
    {
        mVisibility = visibility;
    }

    public int getVisibility()
    {
        return mVisibility;
    }

    @Override
    public void setEnabled(boolean state)
    {
        mEnabled = state;
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    @Override
    public void setClickable(boolean state)
    {
        mClickable = state;
    }

    public boolean isClickable()
    {
        return mClickable;
    }

    @Override
    public void setLongClickable(boolean state)
    {
        mLongClickable = state;
    }

    public boolean isLongClickable()
    {
        return mLongClickable;
    }

    @Override
    public void setContextClickable(boolean state)
    {
        mContextClickable = state;
    }

    public boolean isContextClickable()
    {
        return mContextClickable;
    }

    @Override
    public void setFocusable(boolean state)
    {
        mFocusable = state;
    }

    public boolean isFocusable()
    {
        return mFocusable;
    }

    @Override
    public void setFocused(boolean state)
    {
        mFocused = state;
    }

    public boolean isFocused()
    {
        return mFocused;
    }

    @Override
    public void setAccessibilityFocused(boolean state)
    {
        mAccessibilityFocused = state;
    }

    public boolean isAccessibilityFocused()
    {
        return mAccessibilityFocused;
    }

    @Override
    public void setCheckable(boolean state)
    {
        mCheckable = state;
    }

    public boolean isCheckable()
    {
        return mCheckable;
    }

    @Override
    public void setChecked(boolean state)
    {
        mChecked = state;
    }

    public boolean isChecked()
    {
        return mChecked;
    }

    @Override
    public void setSelected(boolean state)
    {
        mSelected = state;
    }

    public boolean isSelected()
    {
        return mSelected;
    }

    @Override
    public void setActivated(boolean state)
    {
        mActivated = state;
    }

    public boolean isActivated()
    {
        return mActivated;
    }

    @Override
    public void setOpaque(boolean opaque)
    {
        mOpaque = opaque;
    }

    public boolean isOpaque()
    {
        return mOpaque;
    }

    @Override
    public void setClassName(String className)
    {
        mClassName = className;
    }

    public String getClassName()
    {
        return mClassName;
    }

    @Override
    public void setContentDescription(CharSequence contentDescription)
    {
        mContentDescription = contentDescription;
    }

    public CharSequence getContentDescription()
    {
        return mContentDescription;
    }

    @Override
    public void setText(CharSequence text)
    {
        mText = text;
        mTextSelectionStart = -1;
        mTextSelectionEnd = -1;
    }

    @Override
    public void setText(CharSequence text, int selectionStart, int selectionEnd)
    {
        mText = text;
        mTextSelectionStart = selectionStart;
        mTextSelectionEnd = selectionEnd;
    }

    @Override
    public void setTextStyle(float size, int foregroundColor, int backgroundColor, int style)
    {
        mTextSize = size;
        mTextColor = foregroundColor;
        mTextBackgroundColor = backgroundColor;
        mTextStyle = style;
    }

    public float getTextSize()
    {
        return mTextSize;
    }

    public int getTextColor()
    {
        return mTextColor;
    }

    public int getTextBackgroundColor()
    {
        return mTextBackgroundColor;
    }

    public int getTextStyle()
    {
        return mTextStyle;
    }

    @Override
    public void setTextLines(int[] charOffsets, int[] baselines)
    {
        mLineCharOffsets = charOffsets;
        mLineBaselines = baselines;
    }

    public int[] getTextLineCharOffsets()
    {
        return mLineCharOffsets;
    }

    public int[] getTextLineBaselines()
    {
        return mLineBaselines;
    }

    @Override
    public void setTextIdEntry(String entryName)
    {
        mTextIdEntry = entryName;
    }

    public String getTextIdEntry()
    {
        return mTextIdEntry;
    }

    @Override
    public void setHint(CharSequence hint)
    {
        mHint = hint;
    }

    @Override
    public void setHintIdEntry(String entryName)
    {
        mHintIdEntry = entryName;
    }

    public String getHintIdEntry()
    {
        return mHintIdEntry;
    }

    @Override
    public CharSequence getText()
    {
        return mText;
    }

    @Override
    public int getTextSelectionStart()
    {
        return mTextSelectionStart;
    }

    @Override
    public int getTextSelectionEnd()
    {
        return mTextSelectionEnd;
    }

    @Override
    public CharSequence getHint()
    {
        return mHint;
    }

    @Override
    public Bundle getExtras()
    {
        if (mExtras == null) mExtras = new Bundle();
        return mExtras;
    }

    @Override
    public boolean hasExtras()
    {
        return mExtras != null;
    }

    @Override
    public void setChildCount(int num)
    {
        while (mChildren.size() > num) mChildren.remove(mChildren.size() - 1);
        while (mChildren.size() < num) mChildren.add(new RuntimeViewStructure());
    }

    @Override
    public int addChildCount(int num)
    {
        int start = mChildren.size();
        setChildCount(start + num);
        return start;
    }

    @Override
    public int getChildCount()
    {
        return mChildren.size();
    }

    @Override
    public ViewStructure newChild(int index)
    {
        RuntimeViewStructure child = new RuntimeViewStructure();
        mChildren.set(index, child);
        return child;
    }

    @Override
    public ViewStructure asyncNewChild(int index)
    {
        return newChild(index);
    }

    public RuntimeViewStructure getChildAt(int index)
    {
        return mChildren.get(index);
    }

    @Override
    public AutofillId getAutofillId()
    {
        return mAutofillId;
    }

    @Override
    public void setAutofillId(AutofillId id)
    {
        mAutofillId = id;
    }

    @Override
    public void setAutofillId(AutofillId parentId, int virtualId)
    {
        mAutofillId = new AutofillId(parentId == null ? 0 : parentId.getViewId(), virtualId);
    }

    @Override
    public void setAutofillType(int type)
    {
        mAutofillType = type;
    }

    public int getAutofillType()
    {
        return mAutofillType;
    }

    @Override
    public void setAutofillHints(String[] hint)
    {
        mAutofillHints = hint;
    }

    public String[] getAutofillHints()
    {
        return mAutofillHints;
    }

    @Override
    public void setAutofillValue(AutofillValue value)
    {
        mAutofillValue = value;
    }

    public AutofillValue getAutofillValue()
    {
        return mAutofillValue;
    }

    @Override
    public void setAutofillOptions(CharSequence[] options)
    {
        mAutofillOptions = options;
    }

    public CharSequence[] getAutofillOptions()
    {
        return mAutofillOptions;
    }

    @Override
    public void setImportantForAutofill(int mode)
    {
        mImportantForAutofill = mode;
    }

    public int getImportantForAutofill()
    {
        return mImportantForAutofill;
    }

    @Override
    public void setReceiveContentMimeTypes(String[] mimeTypes)
    {
        mReceiveContentMimeTypes = mimeTypes;
    }

    public String[] getReceiveContentMimeTypes()
    {
        return mReceiveContentMimeTypes;
    }

    @Override
    public void setInputType(int inputType)
    {
        mInputType = inputType;
    }

    public int getInputType()
    {
        return mInputType;
    }

    @Override
    public void setDataIsSensitive(boolean sensitive)
    {
        mDataIsSensitive = sensitive;
    }

    public boolean isDataSensitive()
    {
        return mDataIsSensitive;
    }

    @Override
    public void setMinTextEms(int minEms)
    {
        mMinTextEms = minEms;
    }

    public int getMinTextEms()
    {
        return mMinTextEms;
    }

    @Override
    public void setMaxTextEms(int maxEms)
    {
        mMaxTextEms = maxEms;
    }

    public int getMaxTextEms()
    {
        return mMaxTextEms;
    }

    @Override
    public void setMaxTextLength(int maxLength)
    {
        mMaxTextLength = maxLength;
    }

    public int getMaxTextLength()
    {
        return mMaxTextLength;
    }

    @Override
    public void asyncCommit()
    {
    }

    @Override
    public void setWebDomain(String domain)
    {
        mWebDomain = domain;
    }

    public String getWebDomain()
    {
        return mWebDomain;
    }

    @Override
    public void setLocaleList(LocaleList localeList)
    {
        mLocaleList = localeList;
    }

    public LocaleList getLocaleList()
    {
        return mLocaleList;
    }

    @Override
    public HtmlInfo.Builder newHtmlInfoBuilder(String tagName)
    {
        return new RuntimeHtmlInfoBuilder(tagName);
    }

    @Override
    public void setHtmlInfo(HtmlInfo htmlInfo)
    {
        mHtmlInfo = htmlInfo;
    }

    public HtmlInfo getHtmlInfo()
    {
        return mHtmlInfo;
    }

    private static final class RuntimeHtmlInfo extends HtmlInfo
    {
        private final String mTag;
        private final List<Pair<String, String>> mAttributes;

        RuntimeHtmlInfo(String tag, List<Pair<String, String>> attributes)
        {
            mTag = tag;
            mAttributes = attributes;
        }

        @Override
        public String getTag()
        {
            return mTag;
        }

        @Override
        public List<Pair<String, String>> getAttributes()
        {
            return mAttributes;
        }
    }

    private static final class RuntimeHtmlInfoBuilder extends HtmlInfo.Builder
    {
        private final String mTag;
        private final ArrayList<Pair<String, String>> mAttributes = new ArrayList<>();

        RuntimeHtmlInfoBuilder(String tag)
        {
            mTag = tag;
        }

        @Override
        public HtmlInfo.Builder addAttribute(String name, String value)
        {
            mAttributes.add(new Pair<>(name, value));
            return this;
        }

        @Override
        public HtmlInfo build()
        {
            return new RuntimeHtmlInfo(mTag, new ArrayList<>(mAttributes));
        }
    }
}
