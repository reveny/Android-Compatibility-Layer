package android.text;

import java.util.Iterator;

public class TextUtils
{
    private TextUtils()
    {
    }

    public static boolean isEmpty(CharSequence text)
    {
        return text == null || text.length() == 0;
    }

    public static int getTrimmedLength(CharSequence text)
    {
        if (text == null) return 0;

        int length = text.length();
        int start = 0;
        while (start < length && text.charAt(start) <= ' ')
        {
            start++;
        }

        int end = length;
        while (end > start && text.charAt(end - 1) <= ' ')
        {
            end--;
        }

        return end - start;
    }

    public static boolean equals(CharSequence first, CharSequence second)
    {
        if (first == second) return true;
        if (first == null || second == null) return false;
        if (first.length() != second.length()) return false;

        for (int index = 0; index < first.length(); index++)
        {
            if (first.charAt(index) != second.charAt(index)) return false;
        }

        return true;
    }

    public static String join(CharSequence delimiter, Iterable<?> tokens)
    {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iterator = tokens.iterator();
        boolean first = true;

        while (iterator.hasNext())
        {
            if (!first) builder.append(delimiter);
            builder.append(iterator.next());
            first = false;
        }

        return builder.toString();
    }

    public static String join(CharSequence delimiter, Object[] tokens)
    {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < tokens.length; index++)
        {
            if (index > 0) builder.append(delimiter);
            builder.append(tokens[index]);
        }
        return builder.toString();
    }

    public static String[] split(String text, String expression)
    {
        return text.isEmpty() ? new String[0] : text.split(expression, -1);
    }

    public static CharSequence concat(CharSequence... parts)
    {
        StringBuilder builder = new StringBuilder();
        for (CharSequence part : parts)
        {
            if (part != null) builder.append(part);
        }
        return builder.toString();
    }

    public static boolean isDigitsOnly(CharSequence text)
    {
        if (isEmpty(text)) return false;

        for (int index = 0; index < text.length(); index++)
        {
            if (!Character.isDigit(text.charAt(index))) return false;
        }

        return true;
    }

    public static int getLayoutDirectionFromLocale(java.util.Locale locale)
    {
        return 0;
    }

    public static String htmlEncode(String text)
    {
        if (text == null) return null;

        StringBuilder encoded = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++)
        {
            char character = text.charAt(index);
            switch (character)
            {
                case '<': encoded.append("&lt;"); break;
                case '>': encoded.append("&gt;"); break;
                case '&': encoded.append("&amp;"); break;
                case '"': encoded.append("&quot;"); break;
                case '\'': encoded.append("&#39;"); break;
                default: encoded.append(character); break;
            }
        }

        return encoded.toString();
    }

    public enum TruncateAt
    {
        START,
        MIDDLE,
        END,
        MARQUEE
    }

    public static CharSequence ellipsize(CharSequence text, TextPaint paint, float available, TruncateAt where)
    {
        if (text == null) return null;
        if (paint == null || available <= 0) return text;

        if (paint.measureText(text.toString()) <= available) return text;

        String ellipsis = "…";
        float ellipsisWidth = paint.measureText(ellipsis);

        if (ellipsisWidth > available) return "";

        float budget = available - ellipsisWidth;
        String value = text.toString();

        if (where == TruncateAt.START)
        {
            int start = value.length();

            while (start > 0 && paint.measureText(value.substring(start - 1)) <= budget) start--;

            return ellipsis + value.substring(start);
        }

        if (where == TruncateAt.MIDDLE)
        {
            int half = value.length() / 2;
            int left = half;
            int right = half;

            while (left > 0 && paint.measureText(value.substring(0, left - 1) + value.substring(right)) > budget) left--;
            while (right < value.length()
                   && paint.measureText(value.substring(0, left) + value.substring(right)) > budget)
            {
                right++;
            }

            return value.substring(0, left) + ellipsis + value.substring(right);
        }

        int end = 0;

        while (end < value.length() && paint.measureText(value.substring(0, end + 1)) <= budget) end++;

        return value.substring(0, end) + ellipsis;
    }

    public static CharSequence ellipsize(CharSequence text, TextPaint paint, float available, TruncateAt where,
                                         boolean preserveLength, EllipsizeCallback callback)
    {
        return ellipsize(text, paint, available, where);
    }

    public interface EllipsizeCallback
    {
        void ellipsized(int start, int end);
    }
}
