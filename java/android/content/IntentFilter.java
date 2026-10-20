package android.content;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IntentFilter
{
    private final List<String> mActions = new ArrayList<>();
    private final List<String> mCategories = new ArrayList<>();
    private final List<String> mSchemes = new ArrayList<>();
    private final List<String> mTypes = new ArrayList<>();

    private int mPriority;

    public IntentFilter()
    {
    }

    public IntentFilter(String action)
    {
        addAction(action);
    }

    public IntentFilter(String action, String dataType)
    {
        addAction(action);
        addDataType(dataType);
    }

    public final void addAction(String action)
    {
        if (action != null && !mActions.contains(action)) mActions.add(action);
    }

    public final int countActions()
    {
        return mActions.size();
    }

    public final String getAction(int index)
    {
        return mActions.get(index);
    }

    public final boolean hasAction(String action)
    {
        return action != null && mActions.contains(action);
    }

    public final Iterator<String> actionsIterator()
    {
        return mActions.iterator();
    }

    public final void addCategory(String category)
    {
        if (category != null && !mCategories.contains(category)) mCategories.add(category);
    }

    public final boolean hasCategory(String category)
    {
        return category != null && mCategories.contains(category);
    }

    public final void addDataScheme(String scheme)
    {
        if (scheme != null && !mSchemes.contains(scheme)) mSchemes.add(scheme);
    }

    public final boolean hasDataScheme(String scheme)
    {
        return scheme != null && mSchemes.contains(scheme);
    }

    private final java.util.ArrayList<String> mSchemeSpecificParts = new java.util.ArrayList<>();
    private final java.util.ArrayList<String> mDataPaths = new java.util.ArrayList<>();

    public final void addDataSchemeSpecificPart(String part, int type)
    {
        if (part != null && !mSchemeSpecificParts.contains(part)) mSchemeSpecificParts.add(part);
    }

    public final boolean hasDataSchemeSpecificPart(String part)
    {
        return part != null && mSchemeSpecificParts.contains(part);
    }

    public final int countDataSchemeSpecificParts()
    {
        return mSchemeSpecificParts.size();
    }

    public final String getDataSchemeSpecificPart(int index)
    {
        return mSchemeSpecificParts.get(index);
    }

    public final void addDataPath(String path, int type)
    {
        if (path != null && !mDataPaths.contains(path)) mDataPaths.add(path);
    }

    public final boolean hasDataPath(String path)
    {
        return path != null && mDataPaths.contains(path);
    }

    public final int countDataPaths()
    {
        return mDataPaths.size();
    }

    public final void addDataType(String type)
    {
        if (type != null && !mTypes.contains(type)) mTypes.add(type);
    }

    public final boolean hasDataType(String type)
    {
        return type != null && mTypes.contains(type);
    }

    public final void setPriority(int priority)
    {
        mPriority = priority;
    }

    public final int getPriority()
    {
        return mPriority;
    }

    public final boolean matchAction(String action)
    {
        return hasAction(action);
    }

    public final int countCategories()
    {
        return mCategories.size();
    }

    public final String getCategory(int index)
    {
        return mCategories.get(index);
    }

    public final Iterator<String> categoriesIterator()
    {
        return mCategories.iterator();
    }

    public final int countDataSchemes()
    {
        return mSchemes.size();
    }

    public final String getDataScheme(int index)
    {
        return mSchemes.get(index);
    }

    public final int countDataTypes()
    {
        return mTypes.size();
    }

    public final String getDataType(int index)
    {
        return mTypes.get(index);
    }

    public final int match(String action, String type, String scheme, android.net.Uri data, java.util.Set<String> categories,
                           String logTag)
    {
        if (!mActions.isEmpty() && (action == null || !mActions.contains(action))) return NO_MATCH_ACTION;

        if (!mCategories.isEmpty() || categories != null)
        {
            if (categories != null)
            {
                for (String category : categories)
                {
                    if (android.content.Intent.CATEGORY_DEFAULT.equals(category)) continue;
                    if (!mCategories.contains(category)) return NO_MATCH_CATEGORY;
                }
            }
        }

        if (!mSchemes.isEmpty())
        {
            String candidate = scheme != null ? scheme : (data != null ? data.getScheme() : null);
            if (candidate == null || !mSchemes.contains(candidate)) return NO_MATCH_DATA;
        }

        if (!mTypes.isEmpty())
        {
            if (type == null) return NO_MATCH_TYPE;
            if (!matchesType(type)) return NO_MATCH_TYPE;
        }

        return MATCH_CATEGORY_EMPTY + mPriority;
    }

    public final int match(ContentResolver resolver, Intent intent, boolean resolve, String logTag)
    {
        if (intent == null) return NO_MATCH_ACTION;

        return match(intent.getAction(), intent.getType(), intent.getScheme(), intent.getData(), intent.getCategories(), logTag);
    }

    private boolean matchesType(String type)
    {
        for (String candidate : mTypes)
        {
            if (candidate.equals(type)) return true;
            if (!candidate.endsWith("/*")) continue;

            String prefix = candidate.substring(0, candidate.length() - 1);
            if (type.startsWith(prefix)) return true;
        }

        return false;
    }

    public static final int NO_MATCH_ACTION = -3;
    public static final int NO_MATCH_CATEGORY = -4;
    public static final int NO_MATCH_DATA = -2;
    public static final int NO_MATCH_TYPE = -1;
    public static final int MATCH_CATEGORY_EMPTY = 0x00100000;
    public static final int SYSTEM_HIGH_PRIORITY = 1000;
    public static final int SYSTEM_LOW_PRIORITY = -1000;
}
