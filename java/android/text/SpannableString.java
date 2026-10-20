package android.text;

import java.util.ArrayList;
import java.util.List;

public class SpannableString implements Spannable, GetChars, CharSequence
{
    private final String mText;
    private final List<Record> mSpans = new ArrayList<>();

    public SpannableString(CharSequence source)
    {
        mText = source == null ? "" : source.toString();

        if (source instanceof Spanned) copySpansFrom((Spanned) source);
    }

    public static SpannableString valueOf(CharSequence source)
    {
        if (source instanceof SpannableString) return (SpannableString) source;

        return new SpannableString(source);
    }

    private void copySpansFrom(Spanned source)
    {
        for (Object span : source.getSpans(0, source.length(), Object.class))
        {
            setSpan(span, source.getSpanStart(span), source.getSpanEnd(span), source.getSpanFlags(span));
        }
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
        return new SpannableString(mText.substring(start, end));
    }

    @Override
    public String toString()
    {
        return mText;
    }

    @Override
    public void getChars(int start, int end, char[] destination, int destinationStart)
    {
        mText.getChars(start, end, destination, destinationStart);
    }

    @Override
    public void setSpan(Object span, int start, int end, int flags)
    {
        if (span == null) return;

        removeSpan(span);
        mSpans.add(new Record(span, start, end, flags));
    }

    @Override
    public void removeSpan(Object span)
    {
        for (int index = mSpans.size() - 1; index >= 0; index--)
        {
            if (mSpans.get(index).span == span) mSpans.remove(index);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] getSpans(int start, int end, Class<T> kind)
    {
        List<T> found = new ArrayList<>();

        for (Record record : mSpans)
        {
            if (record.end < start || record.start > end) continue;
            if (kind != null && !kind.isInstance(record.span)) continue;

            found.add((T) record.span);
        }

        return found.toArray((T[]) java.lang.reflect.Array.newInstance(kind == null ? Object.class : kind, found.size()));
    }

    @Override
    public int getSpanStart(Object span)
    {
        Record record = recordFor(span);
        return record == null ? -1 : record.start;
    }

    @Override
    public int getSpanEnd(Object span)
    {
        Record record = recordFor(span);
        return record == null ? -1 : record.end;
    }

    @Override
    public int getSpanFlags(Object span)
    {
        Record record = recordFor(span);
        return record == null ? 0 : record.flags;
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class kind)
    {
        int next = limit;

        for (Record record : mSpans)
        {
            if (kind != null && !kind.isInstance(record.span)) continue;

            if (record.start > start && record.start < next) next = record.start;
            if (record.end > start && record.end < next) next = record.end;
        }

        return next;
    }

    private Record recordFor(Object span)
    {
        for (Record record : mSpans)
        {
            if (record.span == span) return record;
        }

        return null;
    }

    private static final class Record
    {
        final Object span;
        final int start;
        final int end;
        final int flags;

        Record(Object span, int start, int end, int flags)
        {
            this.span = span;
            this.start = start;
            this.end = end;
            this.flags = flags;
        }
    }
}
