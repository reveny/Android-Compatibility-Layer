package android.view;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class KeyCharacterMap implements Parcelable
{
    public static final int BUILT_IN_KEYBOARD = 0;
    public static final int VIRTUAL_KEYBOARD = -1;

    public static final int NUMERIC = 1;
    public static final int PREDICTIVE = 2;
    public static final int ALPHA = 3;
    public static final int FULL = 4;
    public static final int SPECIAL_FUNCTION = 5;

    public static final char HEX_INPUT = '';
    public static final char PICKER_DIALOG_INPUT = '';

    public static final int MODIFIER_BEHAVIOR_CHORDED = 0;
    public static final int MODIFIER_BEHAVIOR_CHORDED_OR_TOGGLED = 1;

    public static final int COMBINING_ACCENT = 0x80000000;
    public static final int COMBINING_ACCENT_MASK = 0x7FFFFFFF;

    public static final Parcelable.Creator<KeyCharacterMap> CREATOR = new Parcelable.Creator<KeyCharacterMap>()
    {
        @Override
        public KeyCharacterMap createFromParcel(Parcel source)
        {
            source.readInt();
            return INSTANCE;
        }

        @Override
        public KeyCharacterMap[] newArray(int size)
        {
            return new KeyCharacterMap[size];
        }
    };

    public static class KeyData
    {
        public static final int META_LENGTH = 4;

        public char displayLabel;
        public char number;
        public char[] meta = new char[META_LENGTH];

        public KeyData()
        {
        }
    }

    private static final KeyCharacterMap INSTANCE = new KeyCharacterMap();

    private static final char[] BASE = new char[KeyEvent.getMaxKeyCode() + 1];
    private static final char[] SHIFTED = new char[KeyEvent.getMaxKeyCode() + 1];

    private static void map(int keyCode, char base, char shifted)
    {
        BASE[keyCode] = base;
        SHIFTED[keyCode] = shifted;
    }

    static
    {
        for (int letter = 0; letter < 26; letter++) map(KeyEvent.KEYCODE_A + letter, (char)('a' + letter), (char)('A' + letter));

        String shiftedDigits = ")!@#$%^&*(";
        for (int digit = 0; digit < 10; digit++) map(KeyEvent.KEYCODE_0 + digit, (char)('0' + digit), shiftedDigits.charAt(digit));

        for (int digit = 0; digit < 10; digit++) map(KeyEvent.KEYCODE_NUMPAD_0 + digit, (char)('0' + digit), (char)('0' + digit));

        map(KeyEvent.KEYCODE_SPACE, ' ', ' ');
        map(KeyEvent.KEYCODE_TAB, '\t', '\t');
        map(KeyEvent.KEYCODE_ENTER, '\n', '\n');
        map(KeyEvent.KEYCODE_NUMPAD_ENTER, '\n', '\n');
        map(KeyEvent.KEYCODE_COMMA, ',', '<');
        map(KeyEvent.KEYCODE_PERIOD, '.', '>');
        map(KeyEvent.KEYCODE_GRAVE, '`', '~');
        map(KeyEvent.KEYCODE_MINUS, '-', '_');
        map(KeyEvent.KEYCODE_EQUALS, '=', '+');
        map(KeyEvent.KEYCODE_LEFT_BRACKET, '[', '{');
        map(KeyEvent.KEYCODE_RIGHT_BRACKET, ']', '}');
        map(KeyEvent.KEYCODE_BACKSLASH, '\\', '|');
        map(KeyEvent.KEYCODE_SEMICOLON, ';', ':');
        map(KeyEvent.KEYCODE_APOSTROPHE, '\'', '"');
        map(KeyEvent.KEYCODE_SLASH, '/', '?');
        map(KeyEvent.KEYCODE_AT, '@', '@');
        map(KeyEvent.KEYCODE_PLUS, '+', '+');
        map(KeyEvent.KEYCODE_STAR, '*', '*');
        map(KeyEvent.KEYCODE_POUND, '#', '#');
        map(KeyEvent.KEYCODE_NUMPAD_DIVIDE, '/', '/');
        map(KeyEvent.KEYCODE_NUMPAD_MULTIPLY, '*', '*');
        map(KeyEvent.KEYCODE_NUMPAD_SUBTRACT, '-', '-');
        map(KeyEvent.KEYCODE_NUMPAD_ADD, '+', '+');
        map(KeyEvent.KEYCODE_NUMPAD_DOT, '.', '.');
        map(KeyEvent.KEYCODE_NUMPAD_COMMA, ',', ',');
        map(KeyEvent.KEYCODE_NUMPAD_EQUALS, '=', '=');
        map(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN, '(', '(');
        map(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN, ')', ')');
    }

    KeyCharacterMap()
    {
    }

    static int unicodeFor(int keyCode, int metaState)
    {
        if (keyCode < 0 || keyCode >= BASE.length) return 0;
        if ((metaState & (KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_META_ON)) != 0) return 0;

        boolean shifted = (metaState & KeyEvent.META_SHIFT_ON) != 0;
        boolean letter = keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z;
        if (letter && (metaState & KeyEvent.META_CAPS_LOCK_ON) != 0) shifted = !shifted;

        return shifted ? SHIFTED[keyCode] : BASE[keyCode];
    }

    public static KeyCharacterMap load(int deviceId)
    {
        return INSTANCE;
    }

    public int get(int keyCode, int metaState)
    {
        return unicodeFor(keyCode, metaState);
    }

    public char getNumber(int keyCode)
    {
        char base = keyCode >= 0 && keyCode < BASE.length ? BASE[keyCode] : 0;
        return Character.isDigit(base) || base == '*' || base == '#' || base == '+' ? base : 0;
    }

    public char getMatch(int keyCode, char[] chars)
    {
        return getMatch(keyCode, chars, 0);
    }

    public char getMatch(int keyCode, char[] chars, int metaState)
    {
        if (chars == null) throw new IllegalArgumentException("chars must not be null.");

        char primary = (char)unicodeFor(keyCode, metaState);
        char base = (char)unicodeFor(keyCode, 0);
        char shifted = (char)unicodeFor(keyCode, KeyEvent.META_SHIFT_ON);

        for (char candidate : chars)
        {
            if (candidate != 0 && candidate == primary) return candidate;
        }

        for (char candidate : chars)
        {
            if (candidate != 0 && (candidate == base || candidate == shifted)) return candidate;
        }

        return 0;
    }

    public char getDisplayLabel(int keyCode)
    {
        return (char)Character.toUpperCase(unicodeFor(keyCode, 0));
    }

    public static int getDeadChar(int accent, int character)
    {
        return 0;
    }

    @Deprecated
    public boolean getKeyData(int keyCode, KeyData results)
    {
        if (results.meta.length < KeyData.META_LENGTH) throw new IndexOutOfBoundsException("results.meta.length must be >= " + KeyData.META_LENGTH);

        char label = getDisplayLabel(keyCode);
        if (label == 0) return false;

        results.displayLabel = label;
        results.number = getNumber(keyCode);
        results.meta[0] = (char)unicodeFor(keyCode, 0);
        results.meta[1] = (char)unicodeFor(keyCode, KeyEvent.META_SHIFT_ON);
        results.meta[2] = 0;
        results.meta[3] = 0;
        return true;
    }

    public KeyEvent[] getEvents(char[] chars)
    {
        if (chars == null) throw new IllegalArgumentException("chars must not be null.");

        List<KeyEvent> events = new ArrayList<>();
        long now = android.os.SystemClock.uptimeMillis();

        for (char wanted : chars)
        {
            int keyCode = -1;
            int metaState = 0;

            for (int candidate = 0; candidate < BASE.length && keyCode < 0; candidate++)
            {
                if (BASE[candidate] == wanted) keyCode = candidate;
                else if (SHIFTED[candidate] == wanted)
                {
                    keyCode = candidate;
                    metaState = KeyEvent.META_SHIFT_ON;
                }
            }

            if (keyCode < 0) return null;

            events.add(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState, VIRTUAL_KEYBOARD, 0, 0,
                                    InputDevice.SOURCE_KEYBOARD));
            events.add(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState, VIRTUAL_KEYBOARD, 0, 0,
                                    InputDevice.SOURCE_KEYBOARD));
        }

        return events.toArray(new KeyEvent[0]);
    }

    public boolean isPrintingKey(int keyCode)
    {
        return unicodeFor(keyCode, 0) != 0;
    }

    public int getKeyboardType()
    {
        return FULL;
    }

    public int getModifierBehavior()
    {
        return MODIFIER_BEHAVIOR_CHORDED;
    }

    public static boolean deviceHasKey(int keyCode)
    {
        return keyCode >= 0 && keyCode <= KeyEvent.getMaxKeyCode();
    }

    public static boolean[] deviceHasKeys(int[] keyCodes)
    {
        boolean[] present = new boolean[keyCodes.length];
        for (int index = 0; index < keyCodes.length; index++) present[index] = deviceHasKey(keyCodes[index]);

        return present;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(VIRTUAL_KEYBOARD);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof KeyCharacterMap;
    }

    @Override
    public int hashCode()
    {
        return 1;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public static class UnavailableException extends android.util.AndroidRuntimeException
    {
        public UnavailableException(String message)
        {
            super(message);
        }
    }
}
