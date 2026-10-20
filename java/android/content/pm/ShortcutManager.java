package android.content.pm;

import java.util.ArrayList;
import java.util.List;

public class ShortcutManager
{
    public ShortcutManager(android.content.Context context)
    {
    }

    public List<ShortcutInfo> getDynamicShortcuts()
    {
        return new ArrayList<>();
    }

    public List<ShortcutInfo> getManifestShortcuts()
    {
        return new ArrayList<>();
    }

    public List<ShortcutInfo> getPinnedShortcuts()
    {
        return new ArrayList<>();
    }

    public boolean setDynamicShortcuts(List<ShortcutInfo> shortcuts)
    {
        return false;
    }

    public boolean addDynamicShortcuts(List<ShortcutInfo> shortcuts)
    {
        return false;
    }

    public void removeAllDynamicShortcuts()
    {
    }

    public boolean isRequestPinShortcutSupported()
    {
        return false;
    }

    public int getMaxShortcutCountPerActivity()
    {
        return 0;
    }

    public boolean isRateLimitingActive()
    {
        return false;
    }
}
