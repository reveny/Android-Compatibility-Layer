package android.content.pm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShortcutManager
{
    public static final int FLAG_MATCH_MANIFEST = 1;
    public static final int FLAG_MATCH_DYNAMIC = 2;
    public static final int FLAG_MATCH_PINNED = 4;
    public static final int FLAG_MATCH_CACHED = 8;

    private static final int MAX_SHORTCUTS_PER_ACTIVITY = 15;
    private static final int ICON_SIZE_DP = 48;

    private static final Map<String, ShortcutInfo> sShortcuts = new LinkedHashMap<>();

    private final android.content.Context mContext;

    public ShortcutManager(android.content.Context context)
    {
        mContext = context;
    }

    public List<ShortcutInfo> getDynamicShortcuts()
    {
        return getShortcuts(FLAG_MATCH_DYNAMIC);
    }

    public List<ShortcutInfo> getManifestShortcuts()
    {
        return new ArrayList<>();
    }

    public List<ShortcutInfo> getPinnedShortcuts()
    {
        return getShortcuts(FLAG_MATCH_PINNED);
    }

    public List<ShortcutInfo> getShortcuts(int matchFlags)
    {
        List<ShortcutInfo> matches = new ArrayList<>();

        synchronized (sShortcuts)
        {
            for (ShortcutInfo shortcut : sShortcuts.values())
            {
                boolean dynamic = (matchFlags & FLAG_MATCH_DYNAMIC) != 0 && shortcut.isDynamic();
                boolean pinned = (matchFlags & FLAG_MATCH_PINNED) != 0 && shortcut.isPinned();
                if (dynamic || pinned) matches.add(shortcut);
            }
        }

        return matches;
    }

    public boolean setDynamicShortcuts(List<ShortcutInfo> shortcuts)
    {
        if (shortcuts != null && shortcuts.size() > MAX_SHORTCUTS_PER_ACTIVITY)
        {
            throw new IllegalArgumentException("Max number of dynamic shortcuts exceeded");
        }

        removeAllDynamicShortcuts();
        return addDynamicShortcuts(shortcuts);
    }

    public boolean addDynamicShortcuts(List<ShortcutInfo> shortcuts)
    {
        if (shortcuts == null) return true;

        synchronized (sShortcuts)
        {
            for (ShortcutInfo shortcut : shortcuts)
            {
                if (shortcut == null) continue;

                shortcut.setDynamic(true);
                sShortcuts.put(shortcut.getId(), shortcut);
            }

            trimDynamic();
        }

        return true;
    }

    public void pushDynamicShortcut(ShortcutInfo shortcut)
    {
        if (shortcut == null) throw new NullPointerException("shortcut cannot be null");

        synchronized (sShortcuts)
        {
            sShortcuts.remove(shortcut.getId());
            shortcut.setDynamic(true);
            sShortcuts.put(shortcut.getId(), shortcut);
            trimDynamic();
        }
    }

    private void trimDynamic()
    {
        int dynamic = 0;
        for (ShortcutInfo shortcut : sShortcuts.values())
        {
            if (shortcut.isDynamic()) dynamic++;
        }

        java.util.Iterator<ShortcutInfo> iterator = sShortcuts.values().iterator();

        while (dynamic > MAX_SHORTCUTS_PER_ACTIVITY && iterator.hasNext())
        {
            ShortcutInfo shortcut = iterator.next();
            if (!shortcut.isDynamic()) continue;

            iterator.remove();
            dynamic--;
        }
    }

    public void removeDynamicShortcuts(List<String> shortcutIds)
    {
        if (shortcutIds == null) throw new NullPointerException("shortcutIds cannot be null");

        synchronized (sShortcuts)
        {
            for (String id : shortcutIds)
            {
                ShortcutInfo shortcut = sShortcuts.get(id);
                if (shortcut == null || !shortcut.isDynamic()) continue;

                if (shortcut.isPinned()) shortcut.setDynamic(false);
                else sShortcuts.remove(id);
            }
        }
    }

    public void removeAllDynamicShortcuts()
    {
        synchronized (sShortcuts)
        {
            java.util.Iterator<ShortcutInfo> iterator = sShortcuts.values().iterator();

            while (iterator.hasNext())
            {
                ShortcutInfo shortcut = iterator.next();
                if (!shortcut.isDynamic()) continue;

                if (shortcut.isPinned()) shortcut.setDynamic(false);
                else iterator.remove();
            }
        }
    }

    public void removeLongLivedShortcuts(List<String> shortcutIds)
    {
        if (shortcutIds == null) throw new NullPointerException("shortcutIds cannot be null");

        synchronized (sShortcuts)
        {
            for (String id : shortcutIds) sShortcuts.remove(id);
        }
    }

    public boolean updateShortcuts(List<ShortcutInfo> shortcuts)
    {
        if (shortcuts == null) return true;

        synchronized (sShortcuts)
        {
            for (ShortcutInfo update : shortcuts)
            {
                if (update == null) continue;

                ShortcutInfo existing = sShortcuts.get(update.getId());
                if (existing != null) existing.copyNonNullFieldsFrom(update);
            }
        }

        return true;
    }

    public void disableShortcuts(List<String> shortcutIds)
    {
        disableShortcuts(shortcutIds, null);
    }

    public void disableShortcuts(List<String> shortcutIds, CharSequence disabledMessage)
    {
        if (shortcutIds == null) throw new NullPointerException("shortcutIds cannot be null");

        synchronized (sShortcuts)
        {
            for (String id : shortcutIds)
            {
                ShortcutInfo shortcut = sShortcuts.get(id);
                if (shortcut == null) continue;

                if (shortcut.isPinned())
                {
                    shortcut.setDynamic(false);
                    shortcut.setEnabled(false, disabledMessage);
                }
                else
                {
                    sShortcuts.remove(id);
                }
            }
        }
    }

    public void enableShortcuts(List<String> shortcutIds)
    {
        if (shortcutIds == null) throw new NullPointerException("shortcutIds cannot be null");

        synchronized (sShortcuts)
        {
            for (String id : shortcutIds)
            {
                ShortcutInfo shortcut = sShortcuts.get(id);
                if (shortcut != null) shortcut.setEnabled(true, null);
            }
        }
    }

    public boolean isRequestPinShortcutSupported()
    {
        return false;
    }

    public boolean requestPinShortcut(ShortcutInfo shortcut, android.content.IntentSender resultIntent)
    {
        if (shortcut == null) throw new NullPointerException("shortcut cannot be null");

        return false;
    }

    public android.content.Intent createShortcutResultIntent(ShortcutInfo shortcut)
    {
        if (shortcut == null) throw new NullPointerException("shortcut cannot be null");

        return null;
    }

    public int getMaxShortcutCountPerActivity()
    {
        return MAX_SHORTCUTS_PER_ACTIVITY;
    }

    public boolean isRateLimitingActive()
    {
        return false;
    }

    public int getIconMaxWidth()
    {
        return iconMaxSize();
    }

    public int getIconMaxHeight()
    {
        return iconMaxSize();
    }

    private int iconMaxSize()
    {
        float density = mContext == null ? 1f : mContext.getResources().getDisplayMetrics().density;
        return (int)(ICON_SIZE_DP * density + 0.5f);
    }

    public void reportShortcutUsed(String shortcutId)
    {
    }
}
