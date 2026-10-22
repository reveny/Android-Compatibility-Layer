package android.text.util;

import java.util.ArrayList;
import java.util.Collection;

public class Rfc822Tokenizer implements android.widget.MultiAutoCompleteTextView.Tokenizer
{
    public Rfc822Tokenizer()
    {
    }

    public static void tokenize(CharSequence text, Collection<Rfc822Token> out)
    {
        StringBuilder name = new StringBuilder();
        StringBuilder address = new StringBuilder();
        StringBuilder comment = new StringBuilder();

        int index = 0;
        int cursor = text.length();

        while (index < cursor)
        {
            char character = text.charAt(index);

            if (character == ',' || character == ';')
            {
                index++;

                while (index < cursor && text.charAt(index) == ' ') index++;

                crunch(name);

                if (address.length() > 0) out.add(new Rfc822Token(name.toString(), address.toString(), comment.toString()));
                else if (name.length() > 0) out.add(new Rfc822Token(null, name.toString(), comment.toString()));

                name.setLength(0);
                address.setLength(0);
                comment.setLength(0);
            }
            else if (character == '"')
            {
                index++;

                while (index < cursor)
                {
                    character = text.charAt(index);

                    if (character == '"')
                    {
                        index++;
                        break;
                    }
                    else if (character == '\\')
                    {
                        if (index + 1 < cursor) name.append(text.charAt(index + 1));
                        index += 2;
                    }
                    else
                    {
                        name.append(character);
                        index++;
                    }
                }
            }
            else if (character == '(')
            {
                int level = 1;
                index++;

                while (index < cursor && level > 0)
                {
                    character = text.charAt(index);

                    if (character == ')')
                    {
                        if (level > 1) comment.append(character);
                        level--;
                        index++;
                    }
                    else if (character == '(')
                    {
                        comment.append(character);
                        level++;
                        index++;
                    }
                    else if (character == '\\')
                    {
                        if (index + 1 < cursor) comment.append(text.charAt(index + 1));
                        index += 2;
                    }
                    else
                    {
                        comment.append(character);
                        index++;
                    }
                }
            }
            else if (character == '<')
            {
                index++;

                while (index < cursor)
                {
                    character = text.charAt(index);

                    if (character == '>')
                    {
                        index++;
                        break;
                    }

                    address.append(character);
                    index++;
                }
            }
            else if (character == ' ')
            {
                name.append('\0');
                index++;
            }
            else
            {
                name.append(character);
                index++;
            }
        }

        crunch(name);

        if (address.length() > 0) out.add(new Rfc822Token(name.toString(), address.toString(), comment.toString()));
        else if (name.length() > 0) out.add(new Rfc822Token(null, name.toString(), comment.toString()));
    }

    public static Rfc822Token[] tokenize(CharSequence text)
    {
        ArrayList<Rfc822Token> out = new ArrayList<>();
        tokenize(text, out);
        return out.toArray(new Rfc822Token[out.size()]);
    }

    private static void crunch(StringBuilder builder)
    {
        int index = 0;
        int length = builder.length();

        while (index < length)
        {
            char character = builder.charAt(index);

            if (character != '\0')
            {
                index++;
                continue;
            }

            if (index == 0 || index == length - 1 || builder.charAt(index - 1) == ' ' || builder.charAt(index - 1) == '\0'
                || builder.charAt(index + 1) == ' ' || builder.charAt(index + 1) == '\0')
            {
                builder.deleteCharAt(index);
                length--;
            }
            else
            {
                builder.setCharAt(index, ' ');
                index++;
            }
        }
    }

    public int findTokenStart(CharSequence text, int cursor)
    {
        int best = 0;
        int index = 0;

        while (index < cursor)
        {
            index = findTokenEnd(text, index);

            if (index < cursor)
            {
                index++;

                while (index < cursor && text.charAt(index) == ' ') index++;

                if (index < cursor) best = index;
            }
        }

        return best;
    }

    public int findTokenEnd(CharSequence text, int cursor)
    {
        int length = text.length();
        int index = cursor;

        while (index < length)
        {
            char character = text.charAt(index);

            if (character == ',' || character == ';') return index;

            if (character == '"')
            {
                index++;

                while (index < length)
                {
                    character = text.charAt(index);

                    if (character == '"')
                    {
                        index++;
                        break;
                    }
                    else if (character == '\\' && index + 1 < length)
                    {
                        index += 2;
                    }
                    else
                    {
                        index++;
                    }
                }
            }
            else if (character == '(')
            {
                int level = 1;
                index++;

                while (index < length && level > 0)
                {
                    character = text.charAt(index);

                    if (character == ')') level--;
                    else if (character == '(') level++;
                    else if (character == '\\' && index + 1 < length) index++;

                    index++;
                }
            }
            else if (character == '<')
            {
                index++;

                while (index < length)
                {
                    character = text.charAt(index);
                    index++;

                    if (character == '>') break;
                }
            }
            else
            {
                index++;
            }
        }

        return index;
    }

    public CharSequence terminateToken(CharSequence text)
    {
        return text + ", ";
    }
}
