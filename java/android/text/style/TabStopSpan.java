package android.text.style;

public interface TabStopSpan extends ParagraphStyle
{
    int getTabStop();

    class Standard implements TabStopSpan
    {
        private final int mTabOffset;

        public Standard(int offset)
        {
            if (offset < 0) throw new IllegalArgumentException("Tab stop must be a non-negative value, got " + offset);

            mTabOffset = offset;
        }

        @Override
        public int getTabStop()
        {
            return mTabOffset;
        }

        @Override
        public String toString()
        {
            return "TabStopSpan.Standard{tabOffset=" + mTabOffset + "}";
        }
    }
}
