package android.util;

import java.nio.charset.StandardCharsets;

public final class Base64
{
    public static final int DEFAULT = 0;
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int CRLF = 4;
    public static final int URL_SAFE = 8;
    public static final int NO_CLOSE = 16;

    private static final int WRAP_COLUMNS = 76;

    private Base64()
    {
    }

    public static byte[] encode(byte[] input, int flags)
    {
        return input == null ? null : encode(input, 0, input.length, flags);
    }

    public static byte[] encode(byte[] input, int offset, int length, int flags)
    {
        String encoded = encodeToString(input, offset, length, flags);

        return encoded == null ? null : encoded.getBytes(StandardCharsets.US_ASCII);
    }

    public static String encodeToString(byte[] input, int flags)
    {
        return input == null ? null : encodeToString(input, 0, input.length, flags);
    }

    public static String encodeToString(byte[] input, int offset, int length, int flags)
    {
        if (input == null) return null;

        byte[] slice = new byte[length];
        System.arraycopy(input, offset, slice, 0, length);

        java.util.Base64.Encoder encoder = (flags & URL_SAFE) != 0 ? java.util.Base64.getUrlEncoder() : java.util.Base64.getEncoder();

        if ((flags & NO_PADDING) != 0) encoder = encoder.withoutPadding();

        String encoded = encoder.encodeToString(slice);

        if ((flags & NO_WRAP) != 0) return encoded;

        String separator = (flags & CRLF) != 0 ? "\r\n" : "\n";
        StringBuilder wrapped = new StringBuilder(encoded.length() + encoded.length() / WRAP_COLUMNS * 2 + 2);

        for (int at = 0; at < encoded.length(); at += WRAP_COLUMNS)
        {
            wrapped.append(encoded, at, Math.min(at + WRAP_COLUMNS, encoded.length()));
            wrapped.append(separator);
        }

        if (encoded.isEmpty()) wrapped.append(separator);

        return wrapped.toString();
    }

    public static byte[] decode(String input, int flags)
    {
        return input == null ? null : decode(input.getBytes(StandardCharsets.US_ASCII), flags);
    }

    public static byte[] decode(byte[] input, int flags)
    {
        return input == null ? null : decode(input, 0, input.length, flags);
    }

    public static byte[] decode(byte[] input, int offset, int length, int flags)
    {
        if (input == null) return null;

        StringBuilder cleaned = new StringBuilder(length);

        for (int at = offset; at < offset + length; at++)
        {
            char character = (char)(input[at] & 0xff);

            if (character == '-') character = '+';
            else if (character == '_') character = '/';

            if (character == '+' || character == '/' || character == '=' || Character.isLetterOrDigit(character))
            {
                cleaned.append(character);
            }
        }

        return java.util.Base64.getMimeDecoder().decode(cleaned.toString());
    }
}
