package android.text;

public final class TextDirectionHeuristics
{
    public static final TextDirectionHeuristic LTR = new Fixed(false);
    public static final TextDirectionHeuristic RTL = new Fixed(true);
    public static final TextDirectionHeuristic FIRSTSTRONG_LTR = new FirstStrong(false);
    public static final TextDirectionHeuristic FIRSTSTRONG_RTL = new FirstStrong(true);
    public static final TextDirectionHeuristic ANYRTL_LTR = new AnyRtl();
    public static final TextDirectionHeuristic LOCALE = new FirstStrong(false);

    private TextDirectionHeuristics()
    {
    }

    private static final class Fixed implements TextDirectionHeuristic
    {
        private final boolean mRtl;

        Fixed(boolean rtl)
        {
            mRtl = rtl;
        }

        @Override
        public boolean isRtl(char[] array, int start, int count)
        {
            return mRtl;
        }

        @Override
        public boolean isRtl(CharSequence text, int start, int count)
        {
            return mRtl;
        }
    }

    private static final class FirstStrong implements TextDirectionHeuristic
    {
        private final boolean mFallback;

        FirstStrong(boolean fallback)
        {
            mFallback = fallback;
        }

        @Override
        public boolean isRtl(char[] array, int start, int count)
        {
            return isRtl(new String(array, start, count), 0, count);
        }

        @Override
        public boolean isRtl(CharSequence text, int start, int count)
        {
            if (text == null) return mFallback;

            for (int index = start; index < start + count && index < text.length(); index++)
            {
                byte direction = Character.getDirectionality(text.charAt(index));

                if (direction == Character.DIRECTIONALITY_LEFT_TO_RIGHT) return false;

                if (direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC)
                {
                    return true;
                }
            }

            return mFallback;
        }
    }

    private static final class AnyRtl implements TextDirectionHeuristic
    {
        @Override
        public boolean isRtl(char[] array, int start, int count)
        {
            return isRtl(new String(array, start, count), 0, count);
        }

        @Override
        public boolean isRtl(CharSequence text, int start, int count)
        {
            if (text == null) return false;

            for (int index = start; index < start + count && index < text.length(); index++)
            {
                byte direction = Character.getDirectionality(text.charAt(index));

                if (direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC)
                {
                    return true;
                }
            }

            return false;
        }
    }
}
