package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public final class KeyboardShortcutInfo implements Parcelable
{
    public static final Parcelable.Creator<KeyboardShortcutInfo> CREATOR = new Parcelable.Creator<KeyboardShortcutInfo>()
    {
        @Override
        public KeyboardShortcutInfo createFromParcel(Parcel source)
        {
            return new KeyboardShortcutInfo(source);
        }

        @Override
        public KeyboardShortcutInfo[] newArray(int size)
        {
            return new KeyboardShortcutInfo[size];
        }
    };

    private final CharSequence mLabel;
    private final int mKeycode;
    private final char mBaseCharacter;
    private final int mModifiers;

    public KeyboardShortcutInfo(CharSequence label, int keycode, int modifiers)
    {
        mLabel = label;
        mKeycode = keycode;
        mBaseCharacter = 0;
        mModifiers = modifiers;
    }

    public KeyboardShortcutInfo(CharSequence label, char baseCharacter, int modifiers)
    {
        mLabel = label;
        mKeycode = 0;
        mBaseCharacter = baseCharacter;
        mModifiers = modifiers;
    }

    private KeyboardShortcutInfo(Parcel source)
    {
        mLabel = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mBaseCharacter = (char)source.readInt();
        mKeycode = source.readInt();
        mModifiers = source.readInt();
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public int getKeycode()
    {
        return mKeycode;
    }

    public char getBaseCharacter()
    {
        return mBaseCharacter;
    }

    public int getModifiers()
    {
        return mModifiers;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        android.text.TextUtils.writeToParcel(mLabel, dest, flags);
        dest.writeInt(mBaseCharacter);
        dest.writeInt(mKeycode);
        dest.writeInt(mModifiers);
    }
}
