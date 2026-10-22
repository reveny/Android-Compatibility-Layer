package android.text;

public class AndroidCharacter
{
    public static final int EAST_ASIAN_WIDTH_NEUTRAL = 0;
    public static final int EAST_ASIAN_WIDTH_AMBIGUOUS = 1;
    public static final int EAST_ASIAN_WIDTH_HALF_WIDTH = 2;
    public static final int EAST_ASIAN_WIDTH_FULL_WIDTH = 3;
    public static final int EAST_ASIAN_WIDTH_NARROW = 4;
    public static final int EAST_ASIAN_WIDTH_WIDE = 5;

    private static final char[] MIRROR_PAIRS = {'(', ')', '<', '>', '[', ']', '{', '}', '«', '»', '‹', '›',
                                                '⁅', '⁆', '⁽', '⁾', '₍', '₎', '≤', '≥',
                                                '〈', '〉', '《', '》', '「', '」', '『', '』',
                                                '【', '】', '（', '）', '＜', '＞', '［', '］',
                                                '｛', '｝'};

    public AndroidCharacter()
    {
    }

    public static void getDirectionalities(char[] source, byte[] destination, int count)
    {
        for (int index = 0; index < count; index++)
        {
            char character = source[index];

            if (Character.isHighSurrogate(character) && index + 1 < count && Character.isLowSurrogate(source[index + 1]))
            {
                byte direction = Character.getDirectionality(Character.toCodePoint(character, source[index + 1]));
                destination[index] = direction;
                destination[index + 1] = direction;
                index++;
                continue;
            }

            destination[index] = Character.getDirectionality(character);
        }
    }

    public static int getEastAsianWidth(char input)
    {
        if (input >= 0xFF61 && input <= 0xFFDC) return EAST_ASIAN_WIDTH_HALF_WIDTH;
        if (input >= 0xFFE8 && input <= 0xFFEE) return EAST_ASIAN_WIDTH_HALF_WIDTH;
        if (input == 0x3000 || (input >= 0xFF01 && input <= 0xFF60) || (input >= 0xFFE0 && input <= 0xFFE6))
        {
            return EAST_ASIAN_WIDTH_FULL_WIDTH;
        }

        if ((input >= 0x1100 && input <= 0x115F) || (input >= 0x2E80 && input <= 0x303E) || (input >= 0x3041 && input <= 0x33FF)
            || (input >= 0x3400 && input <= 0x4DBF) || (input >= 0x4E00 && input <= 0x9FFF) || (input >= 0xA000 && input <= 0xA4CF)
            || (input >= 0xAC00 && input <= 0xD7A3) || (input >= 0xF900 && input <= 0xFAFF) || (input >= 0xFE30 && input <= 0xFE4F))
        {
            return EAST_ASIAN_WIDTH_WIDE;
        }

        if ((input >= 0x20 && input <= 0x7E) || input == 0xA2 || input == 0xA3 || input == 0xA5 || input == 0xA6 || input == 0xAC
            || input == 0xAF)
        {
            return EAST_ASIAN_WIDTH_NARROW;
        }

        if (input == 0xA1 || input == 0xA4 || input == 0xA7 || input == 0xA8 || input == 0xAA || input == 0xAD || input == 0xAE
            || (input >= 0xB0 && input <= 0xB4) || (input >= 0xB6 && input <= 0xBA) || (input >= 0xBC && input <= 0xBF)
            || (input >= 0x2010 && input <= 0x203E) || (input >= 0x2460 && input <= 0x24FF) || (input >= 0x2500 && input <= 0x257F)
            || (input >= 0x25A0 && input <= 0x25FF) || (input >= 0xE000 && input <= 0xF8FF))
        {
            return EAST_ASIAN_WIDTH_AMBIGUOUS;
        }

        return EAST_ASIAN_WIDTH_NEUTRAL;
    }

    public static void getEastAsianWidths(char[] source, int start, int count, byte[] destination)
    {
        for (int index = 0; index < count; index++)
        {
            destination[index] = (byte)getEastAsianWidth(source[start + index]);
        }
    }

    public static boolean mirror(char[] text, int start, int count)
    {
        boolean changed = false;

        for (int index = start; index < start + count; index++)
        {
            char mirrored = getMirror(text[index]);

            if (mirrored != text[index])
            {
                text[index] = mirrored;
                changed = true;
            }
        }

        return changed;
    }

    public static char getMirror(char character)
    {
        for (int index = 0; index < MIRROR_PAIRS.length; index++)
        {
            if (MIRROR_PAIRS[index] == character) return (index & 1) == 0 ? MIRROR_PAIRS[index + 1] : MIRROR_PAIRS[index - 1];
        }

        return character;
    }
}
