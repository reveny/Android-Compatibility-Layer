package android.database;

public final class CharArrayBuffer
{
    public char[] data;
    public int sizeCopied;

    public CharArrayBuffer(int size)
    {
        data = new char[size];
    }

    public CharArrayBuffer(char[] buffer)
    {
        data = buffer;
    }

    void copyFrom(String value)
    {
        if (value == null)
        {
            sizeCopied = 0;
            return;
        }

        if (data == null || data.length < value.length()) data = new char[value.length()];

        value.getChars(0, value.length(), data, 0);
        sizeCopied = value.length();
    }
}
