package android.text;

public interface Spannable extends Spanned
{
    void setSpan(Object what, int start, int end, int flags);

    void removeSpan(Object what);

    class Factory
    {
        private static final Factory INSTANCE = new Factory();

        public Factory()
        {
        }

        public static Factory getInstance()
        {
            return INSTANCE;
        }

        public Spannable newSpannable(CharSequence source)
        {
            return new SpannableString(source);
        }
    }
}
