package android.view.inputmethod;

import android.os.Bundle;

public class EditorInfo implements android.text.InputType, android.os.Parcelable
{
    public static final int IME_ACTION_UNSPECIFIED = 0;
    public static final int IME_ACTION_NONE = 1;
    public static final int IME_ACTION_GO = 2;
    public static final int IME_ACTION_SEARCH = 3;
    public static final int IME_ACTION_SEND = 4;
    public static final int IME_ACTION_NEXT = 5;
    public static final int IME_ACTION_DONE = 6;
    public static final int IME_ACTION_PREVIOUS = 7;
    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 0x1000000;
    public static final int IME_FLAG_NO_FULLSCREEN = 0x2000000;
    public static final int IME_FLAG_NAVIGATE_PREVIOUS = 0x4000000;
    public static final int IME_FLAG_NAVIGATE_NEXT = 0x8000000;
    public static final int IME_FLAG_NO_EXTRACT_UI = 0x10000000;
    public static final int IME_FLAG_NO_ACCESSORY_ACTION = 0x20000000;
    public static final int IME_FLAG_NO_ENTER_ACTION = 0x40000000;
    public static final int IME_FLAG_FORCE_ASCII = 0x80000000;
    public static final int IME_MASK_ACTION = 0x000000ff;
    public static final int IME_NULL = 0;

    public int inputType = 0;
    public int imeOptions = IME_NULL;
    public String privateImeOptions;
    public CharSequence actionLabel;
    public int actionId = 0;
    public int initialSelStart = -1;
    public int initialSelEnd = -1;
    public int initialCapsMode = 0;
    public CharSequence hintText;
    public String packageName;
    public int fieldId = 0;
    public String fieldName;
    public Bundle extras;
    public String[] contentMimeTypes;
    public android.os.LocaleList hintLocales;
    public CharSequence label;

    public final void makeCompatible(int targetSdkVersion)
    {
        if (targetSdkVersion >= 11) return;

        switch (inputType & (TYPE_MASK_CLASS | TYPE_MASK_VARIATION))
        {
            case TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:
                inputType = TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_NORMAL | (inputType & TYPE_MASK_FLAGS);
                break;
            case TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_WEB_PASSWORD:
                inputType = TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD | (inputType & TYPE_MASK_FLAGS);
                break;
            case TYPE_CLASS_NUMBER | TYPE_NUMBER_VARIATION_NORMAL:
            case TYPE_CLASS_NUMBER | TYPE_NUMBER_VARIATION_PASSWORD:
                inputType = TYPE_CLASS_NUMBER | (inputType & TYPE_MASK_FLAGS);
                break;
            default:
                break;
        }
    }

    public void setAutofillId(android.view.autofill.AutofillId autofillId)
    {
        mAutofillId = autofillId;
    }

    public android.view.autofill.AutofillId getAutofillId()
    {
        return mAutofillId;
    }

    public void setWritingToolsEnabled(boolean enabled)
    {
        mWritingToolsEnabled = enabled;
    }

    public boolean isWritingToolsEnabled()
    {
        return mWritingToolsEnabled;
    }

    public SurroundingText getInitialSurroundingText(int beforeLength, int afterLength, int flags)
    {
        int selectionStart = initialSelStart - mInitialSurroundingOffset;
        int selectionEnd = initialSelEnd - mInitialSurroundingOffset;
        if (selectionStart < 0 || selectionEnd < selectionStart || selectionEnd > mInitialSurroundingText.length()) return null;

        int start = Math.max(0, selectionStart - Math.max(0, beforeLength));
        int end = Math.min(mInitialSurroundingText.length(), selectionEnd + Math.max(0, afterLength));
        CharSequence text = mInitialSurroundingText.subSequence(start, end);

        return new SurroundingText(text, selectionStart - start, selectionEnd - start, mInitialSurroundingOffset + start);
    }

    public void dump(android.util.Printer printer, String prefix)
    {
        printer.println(prefix + "inputType=0x" + Integer.toHexString(inputType) + " imeOptions=0x" + Integer.toHexString(imeOptions)
                        + " privateImeOptions=" + privateImeOptions);
        printer.println(prefix + "actionLabel=" + actionLabel + " actionId=" + actionId);
        printer.println(prefix + "initialSelStart=" + initialSelStart + " initialSelEnd=" + initialSelEnd
                        + " initialCapsMode=0x" + Integer.toHexString(initialCapsMode));
        printer.println(prefix + "hintText=" + hintText + " label=" + label);
        printer.println(prefix + "packageName=" + packageName + " fieldId=" + fieldId + " fieldName=" + fieldName);
        printer.println(prefix + "extras=" + extras);
        printer.println(prefix + "hintLocales=" + hintLocales);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel out, int flags)
    {
        out.writeInt(inputType);
        out.writeInt(imeOptions);
        out.writeString(privateImeOptions);
        android.text.TextUtils.writeToParcel(actionLabel, out, flags);
        out.writeInt(actionId);
        out.writeInt(initialSelStart);
        out.writeInt(initialSelEnd);
        out.writeInt(initialCapsMode);
        android.text.TextUtils.writeToParcel(hintText, out, flags);
        android.text.TextUtils.writeToParcel(label, out, flags);
        out.writeString(packageName);
        out.writeInt(fieldId);
        out.writeString(fieldName);
        out.writeBundle(extras);
        out.writeStringArray(contentMimeTypes);
        android.text.TextUtils.writeToParcel(mInitialSurroundingText, out, flags);
        out.writeInt(mInitialSurroundingOffset);
        out.writeInt(mStylusHandwritingEnabled ? 1 : 0);
        out.writeInt(mWritingToolsEnabled ? 1 : 0);
        out.writeInt(mInitialToolType);
    }

    public static final android.os.Parcelable.Creator<EditorInfo> CREATOR = new android.os.Parcelable.Creator<EditorInfo>()
    {
        @Override
        public EditorInfo createFromParcel(android.os.Parcel source)
        {
            EditorInfo info = new EditorInfo();
            info.inputType = source.readInt();
            info.imeOptions = source.readInt();
            info.privateImeOptions = source.readString();
            info.actionLabel = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            info.actionId = source.readInt();
            info.initialSelStart = source.readInt();
            info.initialSelEnd = source.readInt();
            info.initialCapsMode = source.readInt();
            info.hintText = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            info.label = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            info.packageName = source.readString();
            info.fieldId = source.readInt();
            info.fieldName = source.readString();
            info.extras = source.readBundle();
            info.contentMimeTypes = source.createStringArray();
            CharSequence surrounding = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            info.mInitialSurroundingText = surrounding == null ? "" : surrounding;
            info.mInitialSurroundingOffset = source.readInt();
            info.mStylusHandwritingEnabled = source.readInt() != 0;
            info.mWritingToolsEnabled = source.readInt() != 0;
            info.mInitialToolType = source.readInt();
            return info;
        }

        @Override
        public EditorInfo[] newArray(int size)
        {
            return new EditorInfo[size];
        }
    };

    public void setSupportedHandwritingGestures(java.util.List<Class<? extends HandwritingGesture>> gestures)
    {
        mSupportedGestures = gestures == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(gestures);
    }

    public java.util.List<Class<? extends HandwritingGesture>> getSupportedHandwritingGestures()
    {
        return new java.util.ArrayList<>(mSupportedGestures);
    }

    public void setSupportedHandwritingGesturePreviews(java.util.Set<Class<? extends PreviewableHandwritingGesture>> gestures)
    {
        mSupportedGesturePreviews = gestures == null ? new java.util.HashSet<>() : new java.util.HashSet<>(gestures);
    }

    public java.util.Set<Class<? extends PreviewableHandwritingGesture>> getSupportedHandwritingGesturePreviews()
    {
        return new java.util.HashSet<>(mSupportedGesturePreviews);
    }

    public void setStylusHandwritingEnabled(boolean enabled)
    {
        mStylusHandwritingEnabled = enabled;
    }

    public boolean isStylusHandwritingEnabled()
    {
        return mStylusHandwritingEnabled;
    }

    public int getInitialToolType()
    {
        return mInitialToolType;
    }

    public void setInitialToolType(int toolType)
    {
        mInitialToolType = toolType;
    }

    public void setInitialSurroundingText(CharSequence text)
    {
        setInitialSurroundingSubText(text, 0);
    }

    public void setInitialSurroundingSubText(CharSequence text, int subTextStart)
    {
        mInitialSurroundingText = text == null ? "" : text;
        mInitialSurroundingOffset = subTextStart;
    }

    public CharSequence getInitialTextBeforeCursor(int length, int flags)
    {
        int end = initialSelStart - mInitialSurroundingOffset;
        if (end <= 0) return "";

        int start = Math.max(0, end - length);
        if (end > mInitialSurroundingText.length()) end = mInitialSurroundingText.length();
        if (start >= end) return "";

        return mInitialSurroundingText.subSequence(start, end);
    }

    public CharSequence getInitialTextAfterCursor(int length, int flags)
    {
        int start = initialSelEnd - mInitialSurroundingOffset;
        if (start < 0) return "";
        if (start >= mInitialSurroundingText.length()) return "";

        int end = Math.min(mInitialSurroundingText.length(), start + length);
        return mInitialSurroundingText.subSequence(start, end);
    }

    public CharSequence getInitialSelectedText(int flags)
    {
        int start = initialSelStart - mInitialSurroundingOffset;
        int end = initialSelEnd - mInitialSurroundingOffset;

        if (start < 0 || end > mInitialSurroundingText.length() || start >= end) return "";

        return mInitialSurroundingText.subSequence(start, end);
    }

    @Override
    public String toString()
    {
        return "EditorInfo(inputType=" + inputType + " imeOptions=" + imeOptions + " hint=" + hintText + ")";
    }

    private CharSequence mInitialSurroundingText = "";
    private int mInitialSurroundingOffset = 0;
    private java.util.List<Class<? extends HandwritingGesture>> mSupportedGestures = new java.util.ArrayList<>();
    private java.util.Set<Class<? extends PreviewableHandwritingGesture>> mSupportedGesturePreviews = new java.util.HashSet<>();
    private boolean mStylusHandwritingEnabled;
    private int mInitialToolType;
    private boolean mWritingToolsEnabled = true;
    private android.view.autofill.AutofillId mAutofillId;
}
