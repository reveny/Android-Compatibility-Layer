package android.view;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public final class KeyboardShortcutGroup implements Parcelable
{
    public static final Parcelable.Creator<KeyboardShortcutGroup> CREATOR = new Parcelable.Creator<KeyboardShortcutGroup>()
    {
        @Override
        public KeyboardShortcutGroup createFromParcel(Parcel source)
        {
            CharSequence label = android.text.TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            ArrayList<KeyboardShortcutInfo> items = source.createTypedArrayList(KeyboardShortcutInfo.CREATOR);
            return new KeyboardShortcutGroup(label, items == null ? new ArrayList<KeyboardShortcutInfo>() : items);
        }

        @Override
        public KeyboardShortcutGroup[] newArray(int size)
        {
            return new KeyboardShortcutGroup[size];
        }
    };

    private final CharSequence mLabel;
    private final List<KeyboardShortcutInfo> mItems;

    public KeyboardShortcutGroup(CharSequence label, List<KeyboardShortcutInfo> items)
    {
        mLabel = label;
        mItems = new ArrayList<>(items);
    }

    public KeyboardShortcutGroup(CharSequence label)
    {
        this(label, new ArrayList<KeyboardShortcutInfo>());
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public List<KeyboardShortcutInfo> getItems()
    {
        return mItems;
    }

    public void addItem(KeyboardShortcutInfo item)
    {
        mItems.add(item);
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
        dest.writeTypedList(mItems, flags);
    }
}
