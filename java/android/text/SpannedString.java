package android.text;

public final class SpannedString implements CharSequence, GetChars, Spanned
{
    private final SpannableStringInternal mStore;

    public SpannedString(CharSequence source)
    {
        this(source, false);
    }

    SpannedString(CharSequence source, boolean ignoreNoCopySpan)
    {
        CharSequence text = source == null ? "" : source;
        mStore = new SpannableStringInternal(this, text, 0, text.length(), ignoreNoCopySpan);
    }

    private SpannedString(CharSequence source, int start, int end)
    {
        mStore = new SpannableStringInternal(this, source, start, end, false);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return new SpannedString(this, start, end);
    }

    public static SpannedString valueOf(CharSequence source)
    {
        if (source instanceof SpannedString) return (SpannedString)source;

        return new SpannedString(source);
    }

    @Override
    public <T> T[] getSpans(int queryStart, int queryEnd, Class<T> kind)
    {
        return mStore.getSpans(queryStart, queryEnd, kind);
    }

    @Override
    public final void getChars(int start, int end, char[] destination, int offset)
    {
        mStore.getChars(start, end, destination, offset);
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
    public final int length()
    {
        return mStore.length();
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
    public final char charAt(int index)
    {
        return mStore.charAt(index);
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class kind)
    {
        return mStore.nextSpanTransition(start, limit, kind);
    }
}
