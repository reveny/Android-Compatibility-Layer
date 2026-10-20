package android.content;

import java.util.ArrayList;
import java.util.List;

public class ClipData
{
    private final CharSequence mLabel;
    private final List<Item> mItems = new ArrayList<>();

    public static class Item
    {
        private final CharSequence mText;

        public Item(CharSequence text)
        {
            mText = text;
        }

        public CharSequence getText()
        {
            return mText;
        }

        public CharSequence coerceToText(Context context)
        {
            return mText == null ? "" : mText;
        }

        @Override
        public String toString()
        {
            return mText == null ? "" : mText.toString();
        }
    }

    public ClipData(CharSequence label, String[] mimeTypes, Item item)
    {
        mLabel = label;
        if (item != null) mItems.add(item);
    }

    public static ClipData newPlainText(CharSequence label, CharSequence text)
    {
        return new ClipData(label, new String[] {"text/plain"}, new Item(text));
    }

    public CharSequence getLabel()
    {
        return mLabel;
    }

    public int getItemCount()
    {
        return mItems.size();
    }

    public Item getItemAt(int index)
    {
        return mItems.get(index);
    }

    public void addItem(Item item)
    {
        if (item != null) mItems.add(item);
    }
}
