package android.text.method;

import android.graphics.Rect;
import android.text.Editable;
import android.text.GetChars;
import android.text.Spannable;
import android.text.Spanned;
import android.view.View;

public abstract class ReplacementTransformationMethod implements TransformationMethod
{
    public ReplacementTransformationMethod()
    {
    }

    protected abstract char[] getOriginal();

    protected abstract char[] getReplacement();

    @Override
    public CharSequence getTransformation(CharSequence source, View view)
    {
        if (source == null) return null;

        char[] original = getOriginal();
        char[] replacement = getReplacement();

        if (!(source instanceof Editable))
        {
            boolean unchanged = true;

            for (int index = 0; index < source.length() && unchanged; index++)
            {
                char character = source.charAt(index);

                for (char candidate : original)
                {
                    if (candidate == character)
                    {
                        unchanged = false;
                        break;
                    }
                }
            }

            if (unchanged) return source;

            if (!(source instanceof Spannable))
            {
                if (source instanceof Spanned) return new SpannedReplacementCharSequence((Spanned)source, original, replacement);

                return new ReplacementCharSequence(source, original, replacement).toString();
            }
        }

        if (source instanceof Spanned) return new SpannedReplacementCharSequence((Spanned)source, original, replacement);

        return new ReplacementCharSequence(source, original, replacement);
    }

    @Override
    public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect)
    {
    }

    private static class ReplacementCharSequence implements CharSequence, GetChars
    {
        private final char[] mOriginal;
        private final char[] mReplacement;
        private final CharSequence mSource;

        ReplacementCharSequence(CharSequence source, char[] original, char[] replacement)
        {
            mSource = source;
            mOriginal = original;
            mReplacement = replacement;
        }

        @Override
        public int length()
        {
            return mSource.length();
        }

        @Override
        public char charAt(int index)
        {
            char character = mSource.charAt(index);

            for (int position = 0; position < mOriginal.length; position++)
            {
                if (character == mOriginal[position]) character = mReplacement[position];
            }

            return character;
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
            char[] characters = new char[length()];
            getChars(0, length(), characters, 0);
            return new String(characters);
        }

        @Override
        public void getChars(int start, int end, char[] destination, int offset)
        {
            for (int index = start; index < end; index++)
            {
                destination[offset + index - start] = charAt(index);
            }
        }
    }

    private static class SpannedReplacementCharSequence extends ReplacementCharSequence implements Spanned
    {
        private final Spanned mSpanned;

        SpannedReplacementCharSequence(Spanned source, char[] original, char[] replacement)
        {
            super(source, original, replacement);
            mSpanned = source;
        }

        @Override
        public CharSequence subSequence(int start, int end)
        {
            return new android.text.SpannableString(this).subSequence(start, end);
        }

        @Override
        public <T> T[] getSpans(int start, int end, Class<T> type)
        {
            return mSpanned.getSpans(start, end, type);
        }

        @Override
        public int getSpanStart(Object tag)
        {
            return mSpanned.getSpanStart(tag);
        }

        @Override
        public int getSpanEnd(Object tag)
        {
            return mSpanned.getSpanEnd(tag);
        }

        @Override
        public int getSpanFlags(Object tag)
        {
            return mSpanned.getSpanFlags(tag);
        }

        @Override
        public int nextSpanTransition(int start, int end, Class type)
        {
            return mSpanned.nextSpanTransition(start, end, type);
        }
    }
}
