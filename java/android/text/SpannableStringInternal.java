package android.text;

import java.lang.reflect.Array;

final class SpannableStringInternal
{
    private static final int START = 0;
    private static final int END = 1;
    private static final int FLAGS = 2;
    private static final int COLUMNS = 3;

    private final CharSequence mOwner;
    private final String mText;
    private Object[] mSpans;
    private int[] mSpanData;
    private int mSpanCount;

    SpannableStringInternal(CharSequence owner, CharSequence source, int start, int end, boolean ignoreNoCopySpan)
    {
        mOwner = owner;

        if (start == 0 && end == source.length()) mText = source.toString();
        else mText = source.toString().substring(start, end);

        mSpans = new Object[0];
        mSpanData = new int[0];

        if (source instanceof Spanned) copySpans((Spanned)source, start, end, ignoreNoCopySpan);
    }

    private void copySpans(Spanned source, int start, int end, boolean ignoreNoCopySpan)
    {
        Object[] spans = source.getSpans(start, end, Object.class);

        for (Object span : spans)
        {
            if (ignoreNoCopySpan && span instanceof NoCopySpan) continue;

            int spanStart = source.getSpanStart(span);
            int spanEnd = source.getSpanEnd(span);
            int flags = source.getSpanFlags(span);

            if (spanStart < start) spanStart = start;
            if (spanEnd > end) spanEnd = end;

            setSpan(span, spanStart - start, spanEnd - start, flags, false);
        }
    }

    public final int length()
    {
        return mText.length();
    }

    public final char charAt(int index)
    {
        return mText.charAt(index);
    }

    public final String toString()
    {
        return mText;
    }

    public final void getChars(int start, int end, char[] destination, int offset)
    {
        mText.getChars(start, end, destination, offset);
    }

    void setSpan(Object what, int start, int end, int flags)
    {
        setSpan(what, start, end, flags, true);
    }

    private boolean isIndexFollowsNextLine(int index)
    {
        return index != 0 && index != length() && charAt(index - 1) != '\n';
    }

    private void setSpan(Object what, int start, int end, int flags, boolean enforceParagraph)
    {
        checkRange("setSpan", start, end);

        if ((flags & Spanned.SPAN_POINT_MARK_MASK) == Spanned.SPAN_PARAGRAPH)
        {
            if (isIndexFollowsNextLine(start))
            {
                if (!enforceParagraph) return;
                throw new RuntimeException("PARAGRAPH span must start at paragraph boundary (" + start + " follows " + charAt(start - 1)
                                           + ")");
            }

            if (isIndexFollowsNextLine(end))
            {
                if (!enforceParagraph) return;
                throw new RuntimeException("PARAGRAPH span must end at paragraph boundary (" + end + " follows " + charAt(end - 1)
                                           + ")");
            }
        }

        for (int index = 0; index < mSpanCount; index++)
        {
            if (mSpans[index] != what) continue;

            int oldStart = mSpanData[index * COLUMNS + START];
            int oldEnd = mSpanData[index * COLUMNS + END];

            mSpanData[index * COLUMNS + START] = start;
            mSpanData[index * COLUMNS + END] = end;
            mSpanData[index * COLUMNS + FLAGS] = flags;

            sendSpanChanged(what, oldStart, oldEnd, start, end);
            return;
        }

        if (mSpanCount + 1 >= mSpans.length)
        {
            int capacity = Math.max(4, mSpans.length * 2);
            Object[] spans = new Object[capacity];
            int[] data = new int[capacity * COLUMNS];

            System.arraycopy(mSpans, 0, spans, 0, mSpanCount);
            System.arraycopy(mSpanData, 0, data, 0, mSpanCount * COLUMNS);

            mSpans = spans;
            mSpanData = data;
        }

        mSpans[mSpanCount] = what;
        mSpanData[mSpanCount * COLUMNS + START] = start;
        mSpanData[mSpanCount * COLUMNS + END] = end;
        mSpanData[mSpanCount * COLUMNS + FLAGS] = flags;
        mSpanCount++;

        if (mOwner instanceof Spannable) sendSpanAdded(what, start, end);
    }

    void removeSpan(Object what)
    {
        for (int index = mSpanCount - 1; index >= 0; index--)
        {
            if (mSpans[index] != what) continue;

            int start = mSpanData[index * COLUMNS + START];
            int end = mSpanData[index * COLUMNS + END];

            int count = mSpanCount - (index + 1);
            System.arraycopy(mSpans, index + 1, mSpans, index, count);
            System.arraycopy(mSpanData, (index + 1) * COLUMNS, mSpanData, index * COLUMNS, count * COLUMNS);

            mSpanCount--;
            mSpans[mSpanCount] = null;

            sendSpanRemoved(what, start, end);
            return;
        }
    }

    public int getSpanStart(Object what)
    {
        for (int index = mSpanCount - 1; index >= 0; index--)
        {
            if (mSpans[index] == what) return mSpanData[index * COLUMNS + START];
        }

        return -1;
    }

    public int getSpanEnd(Object what)
    {
        for (int index = mSpanCount - 1; index >= 0; index--)
        {
            if (mSpans[index] == what) return mSpanData[index * COLUMNS + END];
        }

        return -1;
    }

    public int getSpanFlags(Object what)
    {
        for (int index = mSpanCount - 1; index >= 0; index--)
        {
            if (mSpans[index] == what) return mSpanData[index * COLUMNS + FLAGS];
        }

        return 0;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind)
    {
        int count = 0;
        Object[] found = null;
        int[] priorities = null;

        for (int index = 0; index < mSpanCount; index++)
        {
            int spanStart = mSpanData[index * COLUMNS + START];
            int spanEnd = mSpanData[index * COLUMNS + END];

            if (spanStart > queryEnd) continue;
            if (spanEnd < queryStart) continue;

            if (spanStart != spanEnd && queryStart != queryEnd)
            {
                if (spanStart == queryEnd) continue;
                if (spanEnd == queryStart) continue;
            }

            if (kind != null && kind != Object.class && !kind.isInstance(mSpans[index])) continue;

            if (found == null)
            {
                found = new Object[mSpanCount - index];
                priorities = new int[mSpanCount - index];
            }

            int priority = mSpanData[index * COLUMNS + FLAGS] & Spanned.SPAN_PRIORITY;
            int insertAt = count;

            if (priority != 0)
            {
                insertAt = 0;
                while (insertAt < count && priority <= priorities[insertAt]) insertAt++;

                System.arraycopy(found, insertAt, found, insertAt + 1, count - insertAt);
                System.arraycopy(priorities, insertAt, priorities, insertAt + 1, count - insertAt);
            }

            found[insertAt] = mSpans[index];
            priorities[insertAt] = priority;
            count++;
        }

        T[] result = (T[])Array.newInstance(kind == null ? Object.class : kind, count);
        if (count > 0) System.arraycopy(found, 0, result, 0, count);

        return result;
    }

    public int nextSpanTransition(int start, int limit, Class kind)
    {
        if (kind == null) kind = Object.class;

        for (int index = 0; index < mSpanCount; index++)
        {
            int spanStart = mSpanData[index * COLUMNS + START];
            int spanEnd = mSpanData[index * COLUMNS + END];

            if (spanStart > start && spanStart < limit && kind.isInstance(mSpans[index])) limit = spanStart;
            if (spanEnd > start && spanEnd < limit && kind.isInstance(mSpans[index])) limit = spanEnd;
        }

        return limit;
    }

    private void sendSpanAdded(Object what, int start, int end)
    {
        SpanWatcher[] watchers = getSpans(start, end, SpanWatcher.class);

        for (SpanWatcher watcher : watchers)
        {
            watcher.onSpanAdded((Spannable)mOwner, what, start, end);
        }
    }

    private void sendSpanRemoved(Object what, int start, int end)
    {
        if (!(mOwner instanceof Spannable)) return;

        SpanWatcher[] watchers = getSpans(start, end, SpanWatcher.class);

        for (SpanWatcher watcher : watchers)
        {
            watcher.onSpanRemoved((Spannable)mOwner, what, start, end);
        }
    }

    private void sendSpanChanged(Object what, int oldStart, int oldEnd, int start, int end)
    {
        if (!(mOwner instanceof Spannable)) return;

        SpanWatcher[] watchers = getSpans(Math.min(oldStart, start), Math.max(oldEnd, end), SpanWatcher.class);

        for (SpanWatcher watcher : watchers)
        {
            watcher.onSpanChanged((Spannable)mOwner, what, oldStart, oldEnd, start, end);
        }
    }

    private void checkRange(String operation, int start, int end)
    {
        if (end < start)
        {
            throw new IndexOutOfBoundsException(operation + " (" + start + " ... " + end + ") has end before start");
        }

        int length = length();

        if (start > length || end > length)
        {
            throw new IndexOutOfBoundsException(operation + " (" + start + " ... " + end + ") ends beyond length " + length);
        }

        if (start < 0 || end < 0)
        {
            throw new IndexOutOfBoundsException(operation + " (" + start + " ... " + end + ") starts before 0");
        }
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

            if (span == mOwner)
            {
                if (spanned != otherSpan) return false;
            }
            else if (span == null ? otherSpan != null : !span.equals(otherSpan))
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
        hash = hash * 31 + mSpanCount;

        for (int index = 0; index < mSpanCount; index++)
        {
            Object span = mSpans[index];
            if (span != mOwner) hash = hash * 31 + span.hashCode();

            hash = hash * 31 + mSpanData[index * COLUMNS + START];
            hash = hash * 31 + mSpanData[index * COLUMNS + END];
            hash = hash * 31 + mSpanData[index * COLUMNS + FLAGS];
        }

        return hash;
    }
}
