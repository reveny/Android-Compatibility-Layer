package android.text;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class TextUtils
{
    public static final int ALIGNMENT_SPAN = 1;
    public static final int FOREGROUND_COLOR_SPAN = 2;
    public static final int RELATIVE_SIZE_SPAN = 3;
    public static final int SCALE_X_SPAN = 4;
    public static final int STRIKETHROUGH_SPAN = 5;
    public static final int UNDERLINE_SPAN = 6;
    public static final int STYLE_SPAN = 7;
    public static final int BULLET_SPAN = 8;
    public static final int QUOTE_SPAN = 9;
    public static final int LEADING_MARGIN_SPAN = 10;
    public static final int URL_SPAN = 11;
    public static final int BACKGROUND_COLOR_SPAN = 12;
    public static final int TYPEFACE_SPAN = 13;
    public static final int SUPERSCRIPT_SPAN = 14;
    public static final int SUBSCRIPT_SPAN = 15;
    public static final int ABSOLUTE_SIZE_SPAN = 16;
    public static final int TEXT_APPEARANCE_SPAN = 17;
    public static final int ANNOTATION = 18;
    public static final int SUGGESTION_SPAN = 19;
    public static final int SPELL_CHECK_SPAN = 20;
    public static final int SUGGESTION_RANGE_SPAN = 21;
    public static final int EASY_EDIT_SPAN = 22;
    public static final int LOCALE_SPAN = 23;
    public static final int TTS_SPAN = 24;
    public static final int ACCESSIBILITY_CLICKABLE_SPAN = 25;
    public static final int ACCESSIBILITY_URL_SPAN = 26;
    public static final int LINE_BACKGROUND_SPAN = 27;
    public static final int LINE_HEIGHT_SPAN = 28;
    public static final int ACCESSIBILITY_REPLACEMENT_SPAN = 29;

    public static final int CAP_MODE_CHARACTERS = InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
    public static final int CAP_MODE_WORDS = InputType.TYPE_TEXT_FLAG_CAP_WORDS;
    public static final int CAP_MODE_SENTENCES = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;

    public static final int SAFE_STRING_FLAG_TRIM = 0x1;
    public static final int SAFE_STRING_FLAG_SINGLE_LINE = 0x2;
    public static final int SAFE_STRING_FLAG_FIRST_LINE = 0x4;

    private static final String ELLIPSIS = "…";

    public static final android.os.Parcelable.Creator<CharSequence> CHAR_SEQUENCE_CREATOR =
        new android.os.Parcelable.Creator<CharSequence>()
        {
            @Override
            public CharSequence createFromParcel(android.os.Parcel source)
            {
                Object value = source.readValue(null);
                return value instanceof CharSequence ? (CharSequence)value : null;
            }

            @Override
            public CharSequence[] newArray(int size)
            {
                return new CharSequence[size];
            }
        };

    private TextUtils()
    {
    }

    public static void getChars(CharSequence text, int start, int end, char[] destination, int destinationOffset)
    {
        if (text instanceof String)
        {
            ((String)text).getChars(start, end, destination, destinationOffset);
            return;
        }

        if (text instanceof GetChars)
        {
            ((GetChars)text).getChars(start, end, destination, destinationOffset);
            return;
        }

        if (text instanceof StringBuilder)
        {
            ((StringBuilder)text).getChars(start, end, destination, destinationOffset);
            return;
        }

        if (text instanceof StringBuffer)
        {
            ((StringBuffer)text).getChars(start, end, destination, destinationOffset);
            return;
        }

        for (int index = start; index < end; index++)
        {
            destination[destinationOffset++] = text.charAt(index);
        }
    }

    public static int indexOf(CharSequence text, char character)
    {
        return indexOf(text, character, 0);
    }

    public static int indexOf(CharSequence text, char character, int start)
    {
        if (text instanceof String) return ((String)text).indexOf(character, start);

        return indexOf(text, character, start, text.length());
    }

    public static int indexOf(CharSequence text, char character, int start, int end)
    {
        for (int index = Math.max(0, start); index < end; index++)
        {
            if (text.charAt(index) == character) return index;
        }

        return -1;
    }

    public static int lastIndexOf(CharSequence text, char character)
    {
        return lastIndexOf(text, character, text.length() - 1);
    }

    public static int lastIndexOf(CharSequence text, char character, int last)
    {
        if (text instanceof String) return ((String)text).lastIndexOf(character, last);

        return lastIndexOf(text, character, 0, last);
    }

    public static int lastIndexOf(CharSequence text, char character, int start, int last)
    {
        if (last < 0) return -1;
        if (last >= text.length()) last = text.length() - 1;

        for (int index = last; index >= start; index--)
        {
            if (text.charAt(index) == character) return index;
        }

        return -1;
    }

    public static int indexOf(CharSequence text, CharSequence needle)
    {
        return indexOf(text, needle, 0, text.length());
    }

    public static int indexOf(CharSequence text, CharSequence needle, int start)
    {
        return indexOf(text, needle, start, text.length());
    }

    public static int indexOf(CharSequence text, CharSequence needle, int start, int end)
    {
        int needleLength = needle.length();
        if (needleLength == 0) return start;

        char first = needle.charAt(0);

        for (;;)
        {
            start = indexOf(text, first, start);
            if (start > end - needleLength) break;
            if (start < 0) return -1;
            if (regionMatches(text, start, needle, 0, needleLength)) return start;

            start++;
        }

        return -1;
    }

    public static boolean regionMatches(CharSequence first, int firstStart, CharSequence second, int secondStart, int length)
    {
        if (firstStart < 0 || secondStart < 0 || length < 0) throw new IndexOutOfBoundsException();
        if (firstStart + length > first.length() || secondStart + length > second.length()) throw new IndexOutOfBoundsException();

        for (int index = 0; index < length; index++)
        {
            if (first.charAt(firstStart + index) != second.charAt(secondStart + index)) return false;
        }

        return true;
    }

    public static String substring(CharSequence source, int start, int end)
    {
        if (source instanceof String) return ((String)source).substring(start, end);

        char[] buffer = new char[end - start];
        getChars(source, start, end, buffer, 0);
        return new String(buffer);
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
        if (first instanceof String && second instanceof String) return first.equals(second);

        for (int index = 0; index < first.length(); index++)
        {
            if (first.charAt(index) != second.charAt(index)) return false;
        }

        return true;
    }

    public static String join(CharSequence delimiter, Iterable tokens)
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

    public static String[] split(String text, Pattern pattern)
    {
        return text.isEmpty() ? new String[0] : pattern.split(text, -1);
    }

    public static CharSequence stringOrSpannedString(CharSequence source)
    {
        if (source == null) return null;
        if (source instanceof SpannedString) return source;
        if (source instanceof Spanned) return new SpannedString(source);

        return source.toString();
    }

    public static CharSequence getReverse(CharSequence source, int start, int end)
    {
        return new StringBuilder(source.subSequence(start, end)).reverse().toString();
    }

    public static void writeToParcel(CharSequence text, android.os.Parcel parcel, int parcelableFlags)
    {
        if (text instanceof Spanned) parcel.writeValue(new SpannedString(text));
        else parcel.writeValue(text == null ? null : text.toString());
    }

    public static void dumpSpans(CharSequence text, android.util.Printer printer, String prefix)
    {
        if (!(text instanceof Spanned))
        {
            printer.println(prefix + text + ": (no spans)");
            return;
        }

        Spanned spanned = (Spanned)text;
        Object[] spans = spanned.getSpans(0, text.length(), Object.class);

        for (Object span : spans)
        {
            printer.println(prefix + text.subSequence(spanned.getSpanStart(span), spanned.getSpanEnd(span)) + ": "
                            + Integer.toHexString(System.identityHashCode(span)) + " " + span.getClass().getCanonicalName()
                            + " (" + spanned.getSpanStart(span) + "-" + spanned.getSpanEnd(span) + ") fl=#"
                            + spanned.getSpanFlags(span));
        }
    }

    public static CharSequence replace(CharSequence template, String[] sources, CharSequence[] destinations)
    {
        SpannableStringBuilder builder = new SpannableStringBuilder(template);

        for (int index = 0; index < sources.length; index++)
        {
            int where = indexOf(builder, sources[index]);
            if (where >= 0) builder.setSpan(sources[index], where, where + sources[index].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        for (int index = 0; index < sources.length; index++)
        {
            int start = builder.getSpanStart(sources[index]);
            int end = builder.getSpanEnd(sources[index]);
            if (start >= 0) builder.replace(start, end, destinations[index]);
        }

        return builder;
    }

    public static CharSequence expandTemplate(CharSequence template, CharSequence... values)
    {
        if (values.length > 9) throw new IllegalArgumentException("max of 9 values are supported");

        SpannableStringBuilder builder = new SpannableStringBuilder(template);

        int index = 0;
        while (index < builder.length())
        {
            if (builder.charAt(index) != '^')
            {
                index++;
                continue;
            }

            char next = index + 1 < builder.length() ? builder.charAt(index + 1) : 0;

            if (next == '^')
            {
                builder.delete(index + 1, index + 2);
                index++;
                continue;
            }

            if (!Character.isDigit(next))
            {
                index++;
                continue;
            }

            int which = Character.getNumericValue(next) - 1;
            if (which < 0) throw new IllegalArgumentException("template requests value ^0");
            if (which >= values.length) throw new IllegalArgumentException("template requests value ^" + (which + 1)
                                                                           + "; only " + values.length + " provided");

            builder.replace(index, index + 2, values[which]);
            index += values[which].length();
        }

        return builder;
    }

    public static int getOffsetBefore(CharSequence text, int offset)
    {
        if (offset == 0) return 0;
        if (offset == 1) return 0;

        char character = text.charAt(offset - 1);

        if (Character.isLowSurrogate(character) && Character.isHighSurrogate(text.charAt(offset - 2))) offset -= 2;
        else offset -= 1;

        if (text instanceof Spanned)
        {
            android.text.style.ReplacementSpan[] spans =
                ((Spanned)text).getSpans(offset, offset, android.text.style.ReplacementSpan.class);

            for (android.text.style.ReplacementSpan span : spans)
            {
                int start = ((Spanned)text).getSpanStart(span);
                int end = ((Spanned)text).getSpanEnd(span);
                if (start < offset && end > offset) offset = start;
            }
        }

        return offset;
    }

    public static int getOffsetAfter(CharSequence text, int offset)
    {
        int length = text.length();

        if (offset == length) return length;
        if (offset == length - 1) return length;

        char character = text.charAt(offset);

        if (Character.isHighSurrogate(character) && Character.isLowSurrogate(text.charAt(offset + 1))) offset += 2;
        else offset += 1;

        if (text instanceof Spanned)
        {
            android.text.style.ReplacementSpan[] spans =
                ((Spanned)text).getSpans(offset, offset, android.text.style.ReplacementSpan.class);

            for (android.text.style.ReplacementSpan span : spans)
            {
                int start = ((Spanned)text).getSpanStart(span);
                int end = ((Spanned)text).getSpanEnd(span);
                if (start < offset && end > offset) offset = end;
            }
        }

        return offset;
    }

    public static void copySpansFrom(Spanned source, int start, int end, Class kind, Spannable destination, int destinationOffset)
    {
        if (kind == null) kind = Object.class;

        Object[] spans = source.getSpans(start, end, kind);

        for (Object span : spans)
        {
            int spanStart = Math.max(source.getSpanStart(span), start);
            int spanEnd = Math.min(source.getSpanEnd(span), end);

            destination.setSpan(span, spanStart - start + destinationOffset, spanEnd - start + destinationOffset,
                                source.getSpanFlags(span));
        }
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

    public static CharSequence concat(CharSequence... parts)
    {
        if (parts.length == 0) return "";
        if (parts.length == 1) return parts[0];

        boolean spanned = false;
        for (CharSequence part : parts)
        {
            if (part instanceof Spanned)
            {
                spanned = true;
                break;
            }
        }

        if (spanned)
        {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            for (CharSequence part : parts)
            {
                builder.append(part == null ? "null" : part);
            }
            return new SpannedString(builder);
        }

        StringBuilder builder = new StringBuilder();
        for (CharSequence part : parts)
        {
            builder.append(part);
        }
        return builder.toString();
    }

    public static boolean isGraphic(CharSequence text)
    {
        for (int index = 0; index < text.length(); )
        {
            int codePoint = Character.codePointAt(text, index);
            if (isGraphicCodePoint(codePoint)) return true;

            index += Character.charCount(codePoint);
        }

        return false;
    }

    public static boolean isGraphic(char character)
    {
        return isGraphicCodePoint(character);
    }

    private static boolean isGraphicCodePoint(int codePoint)
    {
        int type = Character.getType(codePoint);

        return type != Character.CONTROL && type != Character.FORMAT && type != Character.SURROGATE && type != Character.UNASSIGNED
               && type != Character.LINE_SEPARATOR && type != Character.PARAGRAPH_SEPARATOR && type != Character.SPACE_SEPARATOR;
    }

    public static boolean isDigitsOnly(CharSequence text)
    {
        if (isEmpty(text)) return false;

        for (int index = 0; index < text.length(); )
        {
            int codePoint = Character.codePointAt(text, index);
            if (!Character.isDigit(codePoint)) return false;

            index += Character.charCount(codePoint);
        }

        return true;
    }

    public static int getCapsMode(CharSequence text, int offset, int requestedModes)
    {
        if (offset < 0) return 0;

        int mode = 0;
        if ((requestedModes & CAP_MODE_CHARACTERS) != 0) mode |= CAP_MODE_CHARACTERS;
        if ((requestedModes & (CAP_MODE_WORDS | CAP_MODE_SENTENCES)) == 0) return mode;

        int index = offset;
        while (index > 0)
        {
            char character = text.charAt(index - 1);
            if (character != '"' && character != '\'' && Character.getType(character) != Character.START_PUNCTUATION) break;
            index--;
        }

        int spaces = index;
        while (spaces > 0 && (text.charAt(spaces - 1) == ' ' || text.charAt(spaces - 1) == '\t')) spaces--;

        if (spaces == 0 || text.charAt(spaces - 1) == '\n') return mode | CAP_MODE_WORDS | CAP_MODE_SENTENCES;
        if ((requestedModes & CAP_MODE_SENTENCES) == 0)
        {
            if (index != spaces) mode |= CAP_MODE_WORDS;
            return mode;
        }

        if (index == spaces) return mode;

        int end = spaces;
        while (end > 0)
        {
            char character = text.charAt(end - 1);
            if (character != '"' && character != '\'' && Character.getType(character) != Character.END_PUNCTUATION) break;
            end--;
        }

        if (end > 0)
        {
            char character = text.charAt(end - 1);

            if (character == '.' || character == '?' || character == '!')
            {
                if (character == '.')
                {
                    for (int scan = end - 2; scan >= 0; scan--)
                    {
                        char previous = text.charAt(scan);
                        if (previous == '.') return mode;
                        if (!Character.isLetter(previous)) break;
                    }
                }

                return mode | CAP_MODE_SENTENCES;
            }
        }

        return mode;
    }

    public static int getLayoutDirectionFromLocale(java.util.Locale locale)
    {
        if (locale == null || locale.equals(java.util.Locale.ROOT)) return 0;

        String language = locale.getLanguage();
        if (language.equals("ar") || language.equals("fa") || language.equals("he") || language.equals("iw") || language.equals("ur")
            || language.equals("yi") || language.equals("ji") || language.equals("ps") || language.equals("sd")
            || language.equals("ug") || language.equals("dv") || language.equals("ckb"))
        {
            return 1;
        }

        return 0;
    }

    public static CharSequence makeSafeForPresentation(String unclean, int maxCharactersToConsider, float ellipsizeDip, int flags)
    {
        String text = unclean == null ? "" : unclean;
        if (maxCharactersToConsider > 0 && text.length() > maxCharactersToConsider) text = text.substring(0, maxCharactersToConsider);

        if ((flags & SAFE_STRING_FLAG_FIRST_LINE) != 0)
        {
            int newline = text.indexOf('\n');
            if (newline >= 0) text = text.substring(0, newline);
        }

        if ((flags & SAFE_STRING_FLAG_SINGLE_LINE) != 0) text = text.replace('\n', ' ');

        StringBuilder builder = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++)
        {
            char character = text.charAt(index);
            if (character == '\n' || character == '\t' || !Character.isISOControl(character)) builder.append(character);
        }

        String result = builder.toString();
        if ((flags & SAFE_STRING_FLAG_TRIM) != 0) result = result.trim();

        return result;
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
        return ellipsize(text, paint, available, where, false, null);
    }

    public static CharSequence ellipsize(CharSequence text, TextPaint paint, float available, TruncateAt where,
                                         boolean preserveLength, EllipsizeCallback callback)
    {
        if (text == null) return null;

        int length = text.length();

        if (paint == null || paint.measureText(text, 0, length) <= available)
        {
            if (callback != null) callback.ellipsized(0, 0);
            return text;
        }

        float ellipsisWidth = paint.measureText(ELLIPSIS);
        float budget = available - ellipsisWidth;

        int start;
        int end;

        if (budget < 0)
        {
            start = 0;
            end = length;
        }
        else if (where == TruncateAt.START)
        {
            start = 0;
            end = length;
            while (end > 0 && paint.measureText(text, end - 1, length) <= budget) end--;
        }
        else if (where == TruncateAt.MIDDLE)
        {
            float half = budget / 2f;
            int left = 0;
            while (left < length && paint.measureText(text, 0, left + 1) <= half) left++;

            int right = length;
            while (right > left && paint.measureText(text, 0, left) + paint.measureText(text, right - 1, length) <= budget) right--;

            start = left;
            end = right;
        }
        else
        {
            start = 0;
            while (start < length && paint.measureText(text, 0, start + 1) <= budget) start++;
            end = length;
        }

        if (start > 0 && start < length && Character.isLowSurrogate(text.charAt(start))) start--;
        if (end > 0 && end < length && Character.isLowSurrogate(text.charAt(end))) end++;

        if (callback != null) callback.ellipsized(start, end);

        if (preserveLength)
        {
            char[] buffer = new char[length];
            getChars(text, 0, length, buffer, 0);

            if (end > start)
            {
                buffer[start] = ELLIPSIS.charAt(0);
                for (int index = start + 1; index < end; index++)
                {
                    buffer[index] = '﻿';
                }
            }

            String result = new String(buffer);
            if (!(text instanceof Spanned)) return result;

            SpannableString spannable = new SpannableString(result);
            copySpansFrom((Spanned)text, 0, length, Object.class, spannable, 0);
            return spannable;
        }

        if (budget < 0) return "";

        if (!(text instanceof Spanned))
        {
            return new StringBuilder(length - (end - start) + 1).append(text, 0, start).append(ELLIPSIS).append(text, end, length)
                .toString();
        }

        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.replace(start, end, ELLIPSIS);
        return builder;
    }

    public static CharSequence listEllipsize(android.content.Context context, List<CharSequence> elements, String separator,
                                             TextPaint paint, float available, int moreId)
    {
        if (elements == null) return "";

        int total = elements.size();
        if (total == 0) return "";

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < total; index++)
        {
            if (index > 0) builder.append(separator);
            builder.append(elements.get(index));
        }

        if (paint == null || paint.measureText(builder.toString()) <= available) return builder.toString();

        for (int count = total - 1; count > 0; count--)
        {
            StringBuilder partial = new StringBuilder();
            for (int index = 0; index < count; index++)
            {
                if (index > 0) partial.append(separator);
                partial.append(elements.get(index));
            }

            partial.append(separator).append('+').append(total - count);
            if (paint.measureText(partial.toString()) <= available) return partial.toString();
        }

        return "";
    }

    public static CharSequence commaEllipsize(CharSequence text, TextPaint paint, float available, String oneMore, String more)
    {
        if (text == null) return null;
        if (paint == null || paint.measureText(text, 0, text.length()) <= available) return text;

        String value = text.toString();
        String[] parts = value.split(",", -1);

        for (int count = parts.length - 1; count > 0; count--)
        {
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < count; index++)
            {
                if (index > 0) builder.append(',');
                builder.append(parts[index]);
            }

            int remaining = parts.length - count;
            String suffix = remaining == 1 ? oneMore : String.format(more, remaining);
            if (suffix != null) builder.append(suffix);

            if (paint.measureText(builder.toString()) <= available) return builder.toString();
        }

        return "";
    }

    public interface EllipsizeCallback
    {
        void ellipsized(int start, int end);
    }

    public interface StringSplitter extends Iterable<String>
    {
        void setString(String string);
    }

    public static class SimpleStringSplitter implements StringSplitter, Iterator<String>
    {
        private String mString;
        private final char mDelimiter;
        private int mPosition;
        private int mLength;

        public SimpleStringSplitter(char delimiter)
        {
            mDelimiter = delimiter;
        }

        @Override
        public void setString(String string)
        {
            mString = string;
            mPosition = 0;
            mLength = mString.length();
        }

        @Override
        public Iterator<String> iterator()
        {
            return this;
        }

        @Override
        public boolean hasNext()
        {
            return mPosition < mLength;
        }

        @Override
        public String next()
        {
            int end = mString.indexOf(mDelimiter, mPosition);
            if (end == -1) end = mLength;

            String next = mString.substring(mPosition, end);
            mPosition = end + 1;
            return next;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
