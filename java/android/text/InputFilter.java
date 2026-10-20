package android.text;

public interface InputFilter
{
    CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destinationStart, int destinationEnd);

    class AllCaps implements InputFilter
    {
        public AllCaps()
        {
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int destinationStart, int destinationEnd)
        {
            return source == null ? null : source.toString().toUpperCase();
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
            return source;
        }
    }
}
