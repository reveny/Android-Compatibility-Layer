package android.text;

import java.util.Locale;

public interface InputFilter
{
    CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destinationStart, int destinationEnd);

    class AllCaps implements InputFilter
    {
        private final Locale mLocale;

        public AllCaps()
        {
            mLocale = null;
        }

        public AllCaps(Locale locale)
        {
            if (locale == null) throw new NullPointerException("locale");

            mLocale = locale;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destinationStart, int destinationEnd)
        {
            boolean lowerOrTitle = false;

            for (int index = start; index < end; )
            {
                int codePoint = Character.codePointAt(source, index);

                if (Character.isLowerCase(codePoint) || Character.isTitleCase(codePoint))
                {
                    lowerOrTitle = true;
                    break;
                }

                index += Character.charCount(codePoint);
            }

            if (!lowerOrTitle) return null;

            Locale locale = mLocale != null ? mLocale : Locale.getDefault();
            String upper = source.subSequence(start, end).toString().toUpperCase(locale);

            if (!(source instanceof Spanned) || upper.length() != end - start) return upper;

            SpannableString spannable = new SpannableString(upper);
            TextUtils.copySpansFrom((Spanned)source, start, end, null, spannable, 0);
            return spannable;
        }
    }

    class LengthFilter implements InputFilter
    {
        private final int mMax;

        public LengthFilter(int max)
        {
            mMax = max;
        }

        public int getMax()
        {
            return mMax;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destinationStart, int destinationEnd)
        {
            int keep = mMax - (dest.length() - (destinationEnd - destinationStart));

            if (keep <= 0) return "";
            if (keep >= end - start) return null;

            keep += start;

            if (Character.isHighSurrogate(source.charAt(keep - 1)))
            {
                keep--;
                if (keep == start) return "";
            }

            return source.subSequence(start, keep);
        }
    }
}
