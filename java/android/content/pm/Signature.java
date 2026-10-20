package android.content.pm;

import java.util.Arrays;

public class Signature implements android.os.Parcelable
{
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final byte[] mSignature;
    private int mHashCode;
    private boolean mHaveHashCode;
    private String mStringRef;

    public Signature(byte[] signature)
    {
        mSignature = signature == null ? new byte[0] : signature.clone();
    }

    public Signature(String text)
    {
        mSignature = fromChars(text);
    }

    public byte[] toByteArray()
    {
        return mSignature.clone();
    }

    public char[] toChars()
    {
        return toChars(null, null);
    }

    public char[] toChars(char[] existing, int[] outLength)
    {
        byte[] signature = mSignature;
        int wanted = signature.length * 2;
        char[] text = existing != null && existing.length >= wanted ? existing : new char[wanted];

        for (int index = 0; index < signature.length; index++)
        {
            int value = signature[index] & 0xff;
            text[index * 2] = HEX[value >> 4];
            text[index * 2 + 1] = HEX[value & 0x0f];
        }

        if (outLength != null && outLength.length > 0) outLength[0] = wanted;

        return text;
    }

    public String toCharsString()
    {
        String cached = mStringRef;
        if (cached != null) return cached;

        cached = new String(toChars());
        mStringRef = cached;

        return cached;
    }

    public byte[] toByteArrayNoCopy()
    {
        return mSignature;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof Signature)) return false;

        return Arrays.equals(mSignature, ((Signature)other).mSignature);
    }

    @Override
    public int hashCode()
    {
        if (mHaveHashCode) return mHashCode;

        mHashCode = Arrays.hashCode(mSignature);
        mHaveHashCode = true;

        return mHashCode;
    }

    @Override
    public String toString()
    {
        return toCharsString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags)
    {
        dest.writeValue(mSignature);
    }

    private static byte[] fromChars(String text)
    {
        if (text == null || (text.length() & 1) != 0) throw new IllegalArgumentException("bad signature text");

        byte[] bytes = new byte[text.length() / 2];

        for (int index = 0; index < bytes.length; index++)
        {
            bytes[index] = (byte)Integer.parseInt(text.substring(index * 2, index * 2 + 2), 16);
        }

        return bytes;
    }
}
