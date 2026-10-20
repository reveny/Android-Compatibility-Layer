package android.text;

public interface GetChars extends CharSequence
{
    void getChars(int start, int end, char[] destination, int destinationOffset);
}
