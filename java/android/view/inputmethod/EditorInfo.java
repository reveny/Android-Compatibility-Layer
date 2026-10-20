package android.view.inputmethod;

import android.os.Bundle;

public class EditorInfo
{
    public static final int IME_ACTION_UNSPECIFIED = 0;
    public static final int IME_ACTION_NONE = 1;
    public static final int IME_ACTION_GO = 2;
    public static final int IME_ACTION_SEARCH = 3;
    public static final int IME_ACTION_SEND = 4;
    public static final int IME_ACTION_NEXT = 5;
    public static final int IME_ACTION_DONE = 6;
    public static final int IME_ACTION_PREVIOUS = 7;
    public static final int IME_FLAG_NO_FULLSCREEN = 0x2000000;
    public static final int IME_FLAG_NO_EXTRACT_UI = 0x10000000;
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

    public void makeCompatible(int targetSdkVersion)
    {
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
}
