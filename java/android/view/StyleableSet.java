package android.view;

import android.content.Context;
import android.content.res.Resources;
import java.util.HashMap;

public final class StyleableSet
{
    private final int[] mSorted;
    private final HashMap<String, Integer> mIndices = new HashMap<>();

    private StyleableSet(int[] sorted, HashMap<String, Integer> indices)
    {
        mSorted = sorted;
        mIndices.putAll(indices);
    }

    public static StyleableSet of(Resources resources, String... names)
    {
        int[] identifiers = new int[names.length];
        int found = 0;

        for (int index = 0; index < names.length; index++)
        {
            int identifier = resources.getIdentifier(names[index], "attr", "android");
            if (identifier != 0) identifiers[found++] = identifier;
        }

        int[] sorted = new int[found];
        System.arraycopy(identifiers, 0, sorted, 0, found);
        java.util.Arrays.sort(sorted);

        HashMap<String, Integer> indices = new HashMap<>();
        for (String name : names)
        {
            int identifier = resources.getIdentifier(name, "attr", "android");
            if (identifier == 0) continue;

            int position = java.util.Arrays.binarySearch(sorted, identifier);
            if (position >= 0) indices.put(name, position);
        }

        return new StyleableSet(sorted, indices);
    }

    public int[] attributes()
    {
        return mSorted;
    }

    public int indexOf(String name)
    {
        Integer position = mIndices.get(name);
        return position == null ? -1 : position;
    }

    public boolean isEmpty()
    {
        return mSorted.length == 0;
    }
}
