package android.text;

public class SpannableString implements CharSequence, GetChars, Spannable
{
    private final SpannableStringInternal mStore;

    public SpannableString(CharSequence source)
    {
        this(source, false);
    }

    SpannableString(CharSequence source, boolean ignoreNoCopySpan)
    {
        CharSequence text = source == null ? "" : source;
        mStore = new SpannableStringInternal(this, text, 0, text.length(), ignoreNoCopySpan);
    }

    private SpannableString(CharSequence source, int start, int end)
    {
        mStore = new SpannableStringInternal(this, source, start, end, false);
    }

    public static SpannableString valueOf(CharSequence source)
    {
        if (source instanceof SpannableString) return (SpannableString)source;

        return new SpannableString(source);
    }

    @Override
    public void setSpan(Object what, int start, int end, int flags)
    {
        mStore.setSpan(what, start, end, flags);
    }

    @Override
    public void removeSpan(Object what)
    {
        mStore.removeSpan(what);
    }

    @Override
    public final CharSequence subSequence(int start, int end)
    {
        return new SpannableString(this, start, end);
    }

    @Override
    public final void getChars(int start, int end, char[] destination, int offset)
    {
        mStore.getChars(start, end, destination, offset);
    }

    @Override
    public final int length()
    {
        return mStore.length();
    }

    @Override
    public int getSpanStart(Object what)
    {
        return mStore.getSpanStart(what);
    }

    @Override
    public int getSpanFlags(Object what)
    {
        return mStore.getSpanFlags(what);
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class kind)
    {
        return mStore.nextSpanTransition(start, limit, kind);
    }

    @Override
    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind)
    {
        return mStore.getSpans(queryStart, queryEnd, kind);
    }

    @Override
    public int hashCode()
    {
        return mStore.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
        return mStore.equals(other);
    }

    @Override
    public int getSpanEnd(Object what)
    {
        return mStore.getSpanEnd(what);
    }

    @Override
    public final String toString()
    {
        return mStore.toString();
    }

    @Override
    public final char charAt(int index)
    {
        return mStore.charAt(index);
    }
}
