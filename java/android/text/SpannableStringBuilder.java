package android.text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class SpannableStringBuilder implements CharSequence, GetChars, Spannable, Editable, Appendable
{
    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final int POINT = 2;
    private static final int PARAGRAPH = 3;

    private final StringBuilder mText;
    private final List<SpanRecord> mSpans = new ArrayList<>();
    private InputFilter[] mFilters = NO_FILTERS;
    private int mTextWatcherDepth;
    private int mInsertionOrder;

    public SpannableStringBuilder()
    {
        this("");
    }

    public SpannableStringBuilder(CharSequence text)
    {
        this(text, 0, text == null ? 0 : text.length());
    }

    public SpannableStringBuilder(CharSequence text, int start, int end)
    {
        CharSequence source = text == null ? "" : text;

        if (end < start) throw new StringIndexOutOfBoundsException("start > end");

        mText = new StringBuilder(end - start);
        mText.append(source, start, end);

        if (source instanceof Spanned)
        {
            Spanned spanned = (Spanned)source;
            Object[] spans = spanned.getSpans(start, end, Object.class);

            for (Object span : spans)
            {
                if (span instanceof NoCopySpan) continue;

                int spanStart = spanned.getSpanStart(span) - start;
                int spanEnd = spanned.getSpanEnd(span) - start;
                int flags = spanned.getSpanFlags(span);

                if (spanStart < 0) spanStart = 0;
                if (spanStart > end - start) spanStart = end - start;
                if (spanEnd < 0) spanEnd = 0;
                if (spanEnd > end - start) spanEnd = end - start;

                setSpan(false, span, spanStart, spanEnd, flags, false);
            }
        }
    }

    public static SpannableStringBuilder valueOf(CharSequence source)
    {
        if (source instanceof SpannableStringBuilder) return (SpannableStringBuilder)source;

        return new SpannableStringBuilder(source);
    }

    @Override
    public char charAt(int index)
    {
        int length = length();

        if (index < 0) throw new IndexOutOfBoundsException("charAt: " + index + " < 0");
        if (index >= length) throw new IndexOutOfBoundsException("charAt: " + index + " >= length " + length);

        return mText.charAt(index);
    }

    @Override
    public int length()
    {
        return mText.length();
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence text, int start, int end)
    {
        return replace(where, where, text, start, end);
    }

    @Override
    public SpannableStringBuilder insert(int where, CharSequence text)
    {
        return replace(where, where, text, 0, text.length());
    }

    @Override
    public SpannableStringBuilder delete(int start, int end)
    {
        return replace(start, end, "", 0, 0);
    }

    @Override
    public void clear()
    {
        replace(0, length(), "", 0, 0);
    }

    @Override
    public void clearSpans()
    {
        for (int index = mSpans.size() - 1; index >= 0; index--)
        {
            SpanRecord record = mSpans.get(index);
            mSpans.remove(index);
            sendSpanRemoved(record.what, record.start, record.end);
        }
    }

    @Override
    public SpannableStringBuilder append(CharSequence text)
    {
        int length = length();
        return replace(length, length, text, 0, text.length());
    }

    public SpannableStringBuilder append(CharSequence text, Object what, int flags)
    {
        int start = length();
        append(text);
        setSpan(what, start, length(), flags);
        return this;
    }

    @Override
    public SpannableStringBuilder append(CharSequence text, int start, int end)
    {
        int length = length();
        return replace(length, length, text, start, end);
    }

    @Override
    public SpannableStringBuilder append(char text)
    {
        return append(String.valueOf(text));
    }

    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence text)
    {
        return replace(start, end, text, 0, text.length());
    }

    @Override
    public SpannableStringBuilder replace(final int start, final int end, CharSequence text, int textStart, int textEnd)
    {
        checkRange("replace", start, end);

        for (InputFilter filter : mFilters)
        {
            CharSequence filtered = filter.filter(text, textStart, textEnd, this, start, end);

            if (filtered != null)
            {
                text = filtered;
                textStart = 0;
                textEnd = filtered.length();
            }
        }

        int originalLength = end - start;
        int newLength = textEnd - textStart;

        if (originalLength == 0 && newLength == 0 && !hasNonExclusiveExclusiveSpanAt(text, textStart)) return this;

        TextWatcher[] watchers = getSpans(start, start + originalLength, TextWatcher.class);
        sendBeforeTextChanged(watchers, start, originalLength, newLength);

        boolean adjustSelection = originalLength != 0 && newLength != 0;
        int selectionStart = 0;
        int selectionEnd = 0;

        if (adjustSelection)
        {
            selectionStart = Selection.getSelectionStart(this);
            selectionEnd = Selection.getSelectionEnd(this);
        }

        change(start, end, text, textStart, textEnd);

        if (adjustSelection)
        {
            if (selectionStart > start && selectionStart < end)
            {
                long offset = (long)(selectionStart - start) * newLength / originalLength;
                selectionStart = start + (int)offset;
                setSpan(false, Selection.SELECTION_START, selectionStart, selectionStart, Spanned.SPAN_POINT_POINT, true);
            }

            if (selectionEnd > start && selectionEnd < end)
            {
                long offset = (long)(selectionEnd - start) * newLength / originalLength;
                selectionEnd = start + (int)offset;
                setSpan(false, Selection.SELECTION_END, selectionEnd, selectionEnd, Spanned.SPAN_POINT_POINT, true);
            }
        }

        sendTextChanged(watchers, start, originalLength, newLength);
        sendAfterTextChanged(watchers);

        return this;
    }

    private static boolean hasNonExclusiveExclusiveSpanAt(CharSequence text, int offset)
    {
        if (!(text instanceof Spanned)) return false;

        Spanned spanned = (Spanned)text;
        Object[] spans = spanned.getSpans(offset, offset, Object.class);

        for (Object span : spans)
        {
            if (spanned.getSpanFlags(span) != Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) return true;
        }

        return false;
    }

    private void change(int start, int end, CharSequence text, int textStart, int textEnd)
    {
        int replacedLength = end - start;
        int replacementLength = textEnd - textStart;
        int delta = replacementLength - replacedLength;
        boolean textIsRemoved = replacedLength > 0;

        ArrayList<SpanRecord> removed = new ArrayList<>();

        for (int index = mSpans.size() - 1; index >= 0; index--)
        {
            SpanRecord record = mSpans.get(index);

            if ((record.flags & Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) == Spanned.SPAN_EXCLUSIVE_EXCLUSIVE && record.start >= start
                && record.start <= end && record.end >= start && record.end <= end
                && (textIsRemoved || record.start > start || record.end < end))
            {
                mSpans.remove(index);
                removed.add(record);
            }
        }

        int count = mSpans.size();
        int[] oldStarts = new int[count];
        int[] oldEnds = new int[count];
        Object[] what = new Object[count];

        for (int index = 0; index < count; index++)
        {
            SpanRecord record = mSpans.get(index);
            oldStarts[index] = record.start;
            oldEnds[index] = record.end;
            what[index] = record.what;
        }

        String replacement = textStart == 0 && textEnd == text.length() ? text.toString() : text.subSequence(textStart, textEnd).toString();
        mText.replace(start, end, replacement);

        int newLength = mText.length();

        for (SpanRecord record : mSpans)
        {
            int flags = record.flags;
            int startFlag = (flags & Spanned.SPAN_POINT_MARK_MASK) >> 4;
            int endFlag = flags & 0x3;

            record.start = updatedBound(record.start, start, end, replacementLength, startFlag, false);
            record.end = updatedBound(record.end, start, end, replacementLength, endFlag, true);

            if ((flags & Spanned.SPAN_POINT_MARK_MASK) == Spanned.SPAN_PARAGRAPH)
            {
                record.start = paragraphBoundary(record.start, newLength);
                record.end = paragraphBoundary(record.end, newLength);
            }

            if (record.end < record.start) record.end = record.start;
        }

        ArrayList<SpanRecord> added = new ArrayList<>();

        if (text instanceof Spanned)
        {
            Spanned spanned = (Spanned)text;
            Object[] spans = spanned.getSpans(textStart, textEnd, Object.class);

            for (Object span : spans)
            {
                int spanStart = spanned.getSpanStart(span);
                int spanEnd = spanned.getSpanEnd(span);

                if (spanStart < textStart) spanStart = textStart;
                if (spanEnd > textEnd) spanEnd = textEnd;

                if (getSpanStart(span) >= 0) continue;

                int copyStart = spanStart - textStart + start;
                int copyEnd = spanEnd - textStart + start;
                int flags = spanned.getSpanFlags(span);

                if ((flags & Spanned.SPAN_POINT_MARK_MASK) == Spanned.SPAN_PARAGRAPH
                    && (isIndexFollowsNextLine(copyStart) || isIndexFollowsNextLine(copyEnd)))
                {
                    continue;
                }

                SpanRecord record = new SpanRecord(span, copyStart, copyEnd, flags, mInsertionOrder++);
                mSpans.add(record);
                added.add(record);
            }
        }

        for (SpanRecord record : removed)
        {
            sendSpanRemoved(record.what, record.start, record.end);
        }

        for (int index = 0; index < count; index++)
        {
            SpanRecord record = find(what[index]);
            if (record == null) continue;

            int oldStart = oldStarts[index];
            int oldEnd = oldEnds[index];

            boolean moved = record.start != oldStart || record.end != oldEnd;
            boolean touched = delta != 0 && (oldStart >= start && oldStart <= end || oldEnd >= start && oldEnd <= end);

            if (moved || touched) sendSpanChanged(record.what, oldStart, oldEnd, record.start, record.end);
        }

        for (SpanRecord record : added)
        {
            sendSpanAdded(record.what, record.start, record.end);
        }
    }

    private int paragraphBoundary(int offset, int length)
    {
        if (offset <= 0 || offset >= length) return offset;

        while (offset < length && mText.charAt(offset - 1) != '\n') offset++;

        return offset;
    }

    private static int updatedBound(int offset, int start, int end, int replacementLength, int flag, boolean atEnd)
    {
        if (offset < start) return offset;

        int delta = replacementLength - (end - start);

        if (offset > end) return offset + delta;

        if (offset == end && end > start) return start + replacementLength;

        if (flag == POINT) return start + replacementLength;
        if (flag == PARAGRAPH) return atEnd ? start + replacementLength : start;

        return start;
    }

    private void sendBeforeTextChanged(TextWatcher[] watchers, int start, int before, int after)
    {
        mTextWatcherDepth++;

        for (TextWatcher watcher : watchers)
        {
            watcher.beforeTextChanged(this, start, before, after);
        }

        mTextWatcherDepth--;
    }

    private void sendTextChanged(TextWatcher[] watchers, int start, int before, int after)
    {
        mTextWatcherDepth++;

        for (TextWatcher watcher : watchers)
        {
            watcher.onTextChanged(this, start, before, after);
        }

        mTextWatcherDepth--;
    }

    private void sendAfterTextChanged(TextWatcher[] watchers)
    {
        mTextWatcherDepth++;

        for (TextWatcher watcher : watchers)
        {
            watcher.afterTextChanged(this);
        }

        mTextWatcherDepth--;
    }

    private void sendSpanAdded(Object what, int start, int end)
    {
        SpanWatcher[] watchers = getSpans(start, end, SpanWatcher.class);

        for (SpanWatcher watcher : watchers)
        {
            watcher.onSpanAdded(this, what, start, end);
        }
    }

    private void sendSpanRemoved(Object what, int start, int end)
    {
        SpanWatcher[] watchers = getSpans(start, end, SpanWatcher.class);

        for (SpanWatcher watcher : watchers)
        {
            watcher.onSpanRemoved(this, what, start, end);
        }
    }

    private void sendSpanChanged(Object what, int oldStart, int oldEnd, int start, int end)
    {
        SpanWatcher[] watchers = getSpans(Math.min(oldStart, start), Math.min(Math.max(oldEnd, end), length()), SpanWatcher.class);

        for (SpanWatcher watcher : watchers)
        {
            watcher.onSpanChanged(this, what, oldStart, oldEnd, start, end);
        }
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags)
    {
        setSpan(true, what, start, end, flags, true);
    }

    private boolean isIndexFollowsNextLine(int index)
    {
        return index != 0 && index != length() && mText.charAt(index - 1) != '\n';
    }

    private void setSpan(boolean send, Object what, int start, int end, int flags, boolean enforceParagraph)
    {
        checkRange("setSpan", start, end);

        int flagsStart = (flags & Spanned.SPAN_POINT_MARK_MASK) >> 4;
        if (isInvalidParagraph(start, flagsStart))
        {
            if (!enforceParagraph) return;
            throw new RuntimeException("PARAGRAPH span must start at paragraph boundary");
        }

        int flagsEnd = flags & 0x3;
        if (isInvalidParagraph(end, flagsEnd))
        {
            if (!enforceParagraph) return;
            throw new RuntimeException("PARAGRAPH span must end at paragraph boundary");
        }

        if (flagsStart == POINT && flagsEnd == 1 && start == end)
        {
            if (send) android.util.Log.e("SpannableStringBuilder", "SPAN_EXCLUSIVE_EXCLUSIVE spans cannot have a zero length");
            return;
        }

        SpanRecord existing = find(what);

        if (existing != null)
        {
            int oldStart = existing.start;
            int oldEnd = existing.end;

            existing.start = start;
            existing.end = end;
            existing.flags = flags;

            if (send) sendSpanChanged(what, oldStart, oldEnd, start, end);
            return;
        }

        mSpans.add(new SpanRecord(what, start, end, flags, mInsertionOrder++));

        if (send) sendSpanAdded(what, start, end);
    }

    private boolean isInvalidParagraph(int index, int flag)
    {
        return flag == PARAGRAPH && index != 0 && index != length() && mText.charAt(index - 1) != '\n';
    }

    @Override
    public void removeSpan(Object what)
    {
        for (int index = mSpans.size() - 1; index >= 0; index--)
        {
            SpanRecord record = mSpans.get(index);
            if (record.what != what) continue;

            mSpans.remove(index);
            sendSpanRemoved(what, record.start, record.end);
            return;
        }
    }

    @Override
    public int getSpanStart(Object what)
    {
        SpanRecord record = find(what);
        return record == null ? -1 : record.start;
    }

    @Override
    public int getSpanEnd(Object what)
    {
        SpanRecord record = find(what);
        return record == null ? -1 : record.end;
    }

    @Override
    public int getSpanFlags(Object what)
    {
        SpanRecord record = find(what);
        return record == null ? 0 : record.flags;
    }

    @Override
    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind)
    {
        return getSpans(queryStart, queryEnd, kind, true);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind, boolean sortByInsertionOrder)
    {
        if (kind == null) return (T[])new Object[0];

        ArrayList<SpanRecord> found = new ArrayList<>();

        for (SpanRecord record : mSpans)
        {
            int spanStart = record.start;
            int spanEnd = record.end;

            if (spanStart > queryEnd || spanEnd < queryStart) continue;
            if (spanStart != spanEnd && queryStart != queryEnd && (spanStart == queryEnd || spanEnd == queryStart)) continue;
            if (kind != Object.class && !kind.isInstance(record.what)) continue;

            found.add(record);
        }

        found.sort((first, second) ->
        {
            int firstPriority = first.flags & Spanned.SPAN_PRIORITY;
            int secondPriority = second.flags & Spanned.SPAN_PRIORITY;

            if (firstPriority != secondPriority) return firstPriority > secondPriority ? -1 : 1;

            return Integer.compare(first.order, second.order);
        });

        T[] result = (T[])Array.newInstance(kind, found.size());
        for (int index = 0; index < found.size(); index++)
        {
            result[index] = (T)found.get(index).what;
        }

        return result;
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class kind)
    {
        if (kind == null) kind = Object.class;

        for (SpanRecord record : mSpans)
        {
            if (record.start > start && record.start < limit && kind.isInstance(record.what)) limit = record.start;
            if (record.end > start && record.end < limit && kind.isInstance(record.what)) limit = record.end;
        }

        return limit;
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return new SpannableStringBuilder(this, start, end);
    }

    @Override
    public void getChars(int start, int end, char[] destination, int destinationOffset)
    {
        checkRange("getChars", start, end);
        mText.getChars(start, end, destination, destinationOffset);
    }

    @Override
    public String toString()
    {
        return mText.toString();
    }

    public String substring(int start, int end)
    {
        return mText.substring(start, end);
    }

    public int getTextWatcherDepth()
    {
        return mTextWatcherDepth;
    }

    @Deprecated
    public int getTextRunCursor(int contextStart, int contextEnd, int direction, int offset, int cursorOpt, android.graphics.Paint paint)
    {
        int length = contextEnd - contextStart;
        if (length <= 0) return offset;

        if (cursorOpt == 0)
        {
            if (offset >= contextEnd) return contextEnd;
            return TextUtils.getOffsetAfter(this, offset);
        }

        if (cursorOpt == 2)
        {
            if (offset <= contextStart) return contextStart;
            return TextUtils.getOffsetBefore(this, offset);
        }

        return offset;
    }

    @Override
    public void setFilters(InputFilter[] filters)
    {
        if (filters == null) throw new IllegalArgumentException();

        mFilters = filters;
    }

    @Override
    public InputFilter[] getFilters()
    {
        return mFilters;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Spanned) || !toString().equals(other.toString())) return false;

        Spanned spanned = (Spanned)other;
        Object[] otherSpans = spanned.getSpans(0, spanned.length(), Object.class);
        Object[] spans = getSpans(0, length(), Object.class);

        if (spans.length != otherSpans.length) return false;

        for (int index = 0; index < spans.length; index++)
        {
            Object span = spans[index];
            Object otherSpan = otherSpans[index];

            if (span == this)
            {
                if (spanned != otherSpan) return false;
            }
            else if (!span.equals(otherSpan))
            {
                return false;
            }

            if (getSpanStart(span) != spanned.getSpanStart(otherSpan) || getSpanEnd(span) != spanned.getSpanEnd(otherSpan)
                || getSpanFlags(span) != spanned.getSpanFlags(otherSpan))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = toString().hashCode();
        hash = hash * 31 + mSpans.size();

        for (SpanRecord record : mSpans)
        {
            if (record.what != this) hash = hash * 31 + record.what.hashCode();

            hash = hash * 31 + record.start;
            hash = hash * 31 + record.end;
            hash = hash * 31 + record.flags;
        }

        return hash;
    }

    private SpanRecord find(Object what)
    {
        for (int index = mSpans.size() - 1; index >= 0; index--)
        {
            SpanRecord record = mSpans.get(index);
            if (record.what == what) return record;
        }

        return null;
    }

    private void checkRange(String operation, int start, int end)
    {
        if (end < start)
        {
            throw new IndexOutOfBoundsException(operation + " " + region(start, end) + " has end before start");
        }

        int length = length();

        if (start > length || end > length)
        {
            throw new IndexOutOfBoundsException(operation + " " + region(start, end) + " ends beyond length " + length);
        }

        if (start < 0 || end < 0)
        {
            throw new IndexOutOfBoundsException(operation + " " + region(start, end) + " starts before 0");
        }
    }

    private static String region(int start, int end)
    {
        return "(" + start + " ... " + end + ")";
    }

    private static final class SpanRecord
    {
        final Object what;
        int start;
        int end;
        int flags;
        final int order;

        SpanRecord(Object what, int start, int end, int flags, int order)
        {
            this.what = what;
            this.start = start;
            this.end = end;
            this.flags = flags;
            this.order = order;
        }
    }
}
