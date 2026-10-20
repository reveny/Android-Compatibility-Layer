package android.text;

public interface TextDirectionHeuristic
{
    boolean isRtl(char[] array, int start, int count);

    boolean isRtl(CharSequence text, int start, int count);
}
