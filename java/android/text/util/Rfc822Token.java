package android.text.util;

public class Rfc822Token
{
    private String mName;
    private String mAddress;
    private String mComment;

    public Rfc822Token(String name, String address, String comment)
    {
        mName = name;
        mAddress = address;
        mComment = comment;
    }

    public String getName()
    {
        return mName;
    }

    public String getAddress()
    {
        return mAddress;
    }

    public String getComment()
    {
        return mComment;
    }

    public void setName(String name)
    {
        mName = name;
    }

    public void setAddress(String address)
    {
        mAddress = address;
    }

    public void setComment(String comment)
    {
        mComment = comment;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        if (mName != null && mName.length() != 0)
        {
            builder.append(quoteNameIfNecessary(mName));
            builder.append(' ');
        }

        if (mComment != null && mComment.length() != 0)
        {
            builder.append('(');
            builder.append(quoteComment(mComment));
            builder.append(") ");
        }

        if (mAddress != null && mAddress.length() != 0)
        {
            builder.append('<');
            builder.append(mAddress);
            builder.append('>');
        }

        return builder.toString();
    }

    public static String quoteNameIfNecessary(String name)
    {
        int length = name.length();

        for (int index = 0; index < length; index++)
        {
            char character = name.charAt(index);

            if (!((character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z') || character == ' '
                  || (character >= '0' && character <= '9')))
            {
                return '"' + quoteName(name) + '"';
            }
        }

        return name;
    }

    public static String quoteName(String name)
    {
        StringBuilder builder = new StringBuilder();
        int length = name.length();

        for (int index = 0; index < length; index++)
        {
            char character = name.charAt(index);

            if (character == '\\' || character == '"') builder.append('\\');
            builder.append(character);
        }

        return builder.toString();
    }

    public static String quoteComment(String comment)
    {
        int length = comment.length();
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < length; index++)
        {
            char character = comment.charAt(index);

            if (character == '(' || character == ')' || character == '\\') builder.append('\\');
            builder.append(character);
        }

        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        if (mName != null) result = 31 * result + mName.hashCode();
        if (mAddress != null) result = 31 * result + mAddress.hashCode();
        if (mComment != null) result = 31 * result + mComment.hashCode();
        return result;
    }

    private static boolean stringEquals(String first, String second)
    {
        if (first == null) return second == null;

        return first.equals(second);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Rfc822Token)) return false;

        Rfc822Token token = (Rfc822Token)other;
        return stringEquals(mName, token.mName) && stringEquals(mAddress, token.mAddress) && stringEquals(mComment, token.mComment);
    }
}
