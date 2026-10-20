package android.text;

import java.util.ArrayList;
import java.util.List;

public class SpannableStringBuilder implements Editable
{
    private final StringBuilder mText;
    private final List<SpanRecord> mSpans = new ArrayList<>();
    private InputFilter[] mFilters = NO_FILTERS;

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];

    public SpannableStringBuilder()
    {
        mText = new StringBuilder();
    }

    public SpannableStringBuilder(CharSequence text)
    {
        mText = new StringBuilder(text == null ? "" : text.toString());
        copySpansFrom(text, 0, mText.length(), 0);
    }

    public SpannableStringBuilder(CharSequence text, int start, int end)
    {
        String whole = text == null ? "" : text.toString();

        int from = Math.max(0, Math.min(start, whole.length()));
        int to = Math.max(from, Math.min(end, whole.length()));

        mText = new StringBuilder(whole.substring(from, to));
        copySpansFrom(text, start, end, -start);
    }

    public static SpannableStringBuilder valueOf(CharSequence source)
    {
        if (source instanceof SpannableStringBuilder) return (SpannableStringBuilder)source;

        return new SpannableStringBuilder(source);
    }

    @Override
    public int length()
    {
        return mText.length();
    }

    @Override
    public char charAt(int index)
    {
        return mText.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return new SpannableStringBuilder(this, start, end);
    }

    @Override
    public String toString()
    {
        return mText.toString();
    }

    @Override
    public void getChars(int start, int end, char[] destination, int destinationOffset)
    {
        mText.getChars(start, end, destination, destinationOffset);
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags)
    {
        if (what == null) return;

        removeSpan(what);
        mSpans.add(new SpanRecord(what, clamp(start), clamp(end), flags));
    }

    @Override
    public void removeSpan(Object what)
    {
        for (int index = mSpans.size() - 1; index >= 0; --index)
        {
            if (mSpans.get(index).what == what) mSpans.remove(index);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] getSpans(int start, int end, Class<T> type)
    {
        List<T> found = new ArrayList<>();

        for (SpanRecord record : mSpans)
        {
            if (type != null && !type.isInstance(record.what)) continue;
            if (record.end < start || record.start > end) continue;
            if (record.start == record.end && (record.start < start || record.start > end)) continue;

            found.add((T)record.what);
        }

        return found.toArray((T[])java.lang.reflect.Array.newInstance(type == null ? Object.class : type, found.size()));
    }

    @Override
    public int getSpanStart(Object span)
    {
        SpanRecord record = find(span);
        return record == null ? -1 : record.start;
    }

    @Override
    public int getSpanEnd(Object span)
    {
        SpanRecord record = find(span);
        return record == null ? -1 : record.end;
    }

    @Override
    public int getSpanFlags(Object span)
    {
        SpanRecord record = find(span);
        return record == null ? 0 : record.flags;
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class<?> type)
    {
        int transition = limit;

        for (SpanRecord record : mSpans)
        {
            if (type != null && !type.isInstance(record.what)) continue;
            if (record.start > start && record.start < transition) transition = record.start;
            if (record.end > start && record.end < transition) transition = record.end;
        }

        return transition;
    }

    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence text)
    {
        return replace(start, end, text == null ? "" : text, 0, text == null ? 0 : text.length());
    }

    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence source, int sourceStart, int sourceEnd)
    {
        int from = clamp(start);
        int to = Math.max(from, clamp(end));

        CharSequence replacement = source == null ? "" : source.subSequence(sourceStart, sourceEnd);

        for (InputFilter filter : mFilters)
        {
            CharSequence filtered = filter.filter(replacement, 0, replacement.length(), this, from, to);
            if (filtered != null) replacement = filtered;
        }

        int removed = to - from;
        int inserted = replacement.length();

        mText.replace(from, to, replacement.toString());
        adjustSpans(from, removed, inserted);
        return this;
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence text, int start, int end)
    {
        return replace(where, where, text, start, end);
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence text)
    {
        return replace(where, where, text);
    }

    @Override
    public SpannableStringBuilder delete(int start, int end)
    {
        return replace(start, end, "");
    }

    @Override
    public SpannableStringBuilder append(CharSequence text)
    {
        int end = length();
        return replace(end, end, text);
    }

    @Override
    public SpannableStringBuilder append(CharSequence text, int start, int end)
    {
        int at = length();
        return replace(at, at, text, start, end);
    }

    @Override
    public SpannableStringBuilder append(char text)
    {
        return append(String.valueOf(text));
    }

    @Override
    public void clear()
    {
        replace(0, length(), "");
    }

    @Override
    public void clearSpans()
    {
        mSpans.clear();
    }

    @Override
    public void setFilters(InputFilter[] filters)
    {
        mFilters = filters == null ? NO_FILTERS : filters;
    }

    @Override
    public InputFilter[] getFilters()
    {
        return mFilters;
    }

    private SpanRecord find(Object span)
    {
        for (SpanRecord record : mSpans)
        {
            if (record.what == span) return record;
        }

        return null;
    }

    private int clamp(int value)
    {
        if (value < 0) return 0;
        if (value > mText.length()) return mText.length();

        return value;
    }

    private void adjustSpans(int at, int removed, int inserted)
    {
        int shift = inserted - removed;
        if (shift == 0) return;

        for (SpanRecord record : mSpans)
        {
            record.start = shiftOffset(record.start, at, removed, shift);
            record.end = shiftOffset(record.end, at, removed, shift);
        }
    }

    private int shiftOffset(int offset, int at, int removed, int shift)
    {
        if (offset <= at) return offset;
        if (offset >= at + removed) return offset + shift;

        return at;
    }

    private void copySpansFrom(CharSequence text, int start, int end, int shift)
    {
        if (!(text instanceof Spanned)) return;

        Spanned source = (Spanned)text;

        for (Object span : source.getSpans(start, end, Object.class))
        {
            int spanStart = Math.max(start, source.getSpanStart(span)) + shift;
            int spanEnd = Math.min(end, source.getSpanEnd(span)) + shift;
            mSpans.add(new SpanRecord(span, spanStart, spanEnd, source.getSpanFlags(span)));
        }
    }

    private static final class SpanRecord
    {
        final Object what;
        int start;
        int end;
        final int flags;

        SpanRecord(Object what, int start, int end, int flags)
        {
            this.what = what;
            this.start = start;
            this.end = end;
            this.flags = flags;
        }
    }
}
