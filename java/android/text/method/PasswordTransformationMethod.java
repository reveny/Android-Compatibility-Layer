package android.text.method;

import android.graphics.Rect;
import android.text.Editable;
import android.text.GetChars;
import android.text.TextWatcher;
import android.view.View;

public class PasswordTransformationMethod implements TransformationMethod, TextWatcher
{
    private static final char DOT = '•';
    private static PasswordTransformationMethod sInstance;

    public PasswordTransformationMethod()
    {
    }

    public static PasswordTransformationMethod getInstance()
    {
        if (sInstance == null) sInstance = new PasswordTransformationMethod();
        return sInstance;
    }

    @Override
    public CharSequence getTransformation(CharSequence source, View view)
    {
        if (source == null) return null;

        return new PasswordCharSequence(source);
    }

    @Override
    public void beforeTextChanged(CharSequence text, int start, int count, int after)
    {
    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count)
    {
    }

    @Override
    public void afterTextChanged(Editable text)
    {
    }

    @Override
    public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect)
    {
    }

    private static class PasswordCharSequence implements CharSequence, GetChars
    {
        private final CharSequence mSource;

        PasswordCharSequence(CharSequence source)
        {
            mSource = source;
        }

        @Override
        public int length()
        {
            return mSource.length();
        }

        @Override
        public char charAt(int index)
        {
            return DOT;
        }

        @Override
        public CharSequence subSequence(int start, int end)
        {
            char[] characters = new char[end - start];
            getChars(start, end, characters, 0);
            return new String(characters);
        }

        @Override
        public String toString()
        {
            return subSequence(0, length()).toString();
        }

        @Override
        public void getChars(int start, int end, char[] destination, int offset)
        {
            for (int index = start; index < end; index++)
            {
                destination[offset + index - start] = DOT;
            }
        }
    }
}
