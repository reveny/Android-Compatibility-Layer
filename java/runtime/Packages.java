package runtime;

import java.util.ArrayList;
import java.util.List;

public final class Packages
{
    public static final class Entry
    {
        public String packageName;
        public String label;
        public String archivePath;
        public String iconPath;
        public int targetSdkVersion;
    }

    private static List<Entry> sEntries;

    private Packages()
    {
    }

    public static List<Entry> installed()
    {
        if (sEntries != null) return sEntries;

        List<Entry> entries = new ArrayList<>();
        int count = 0;

        try
        {
            count = nativeCount();
        }
        catch (UnsatisfiedLinkError absent)
        {
            count = 0;
        }

        for (int slot = 0; slot < count; slot++)
        {
            Entry entry = new Entry();
            entry.packageName = nativeNameAt(slot);
            entry.label = nativeLabelAt(slot);
            entry.archivePath = nativeArchiveAt(slot);
            entry.iconPath = nativeIconAt(slot);
            entry.targetSdkVersion = nativeTargetSdkAt(slot);

            if (entry.packageName == null || entry.packageName.isEmpty()) continue;
            if (entry.label == null || entry.label.isEmpty()) entry.label = entry.packageName;

            entries.add(entry);
        }

        sEntries = entries;
        return sEntries;
    }

    public static Entry find(String packageName)
    {
        if (packageName == null) return null;

        for (Entry entry : installed())
        {
            if (packageName.equals(entry.packageName)) return entry;
        }

        return null;
    }

    public static boolean isHome()
    {
        return !installed().isEmpty();
    }

    public static boolean launch(String packageName)
    {
        if (packageName == null || packageName.isEmpty()) return false;

        try
        {
            return nativeLaunch(packageName);
        }
        catch (UnsatisfiedLinkError absent)
        {
            return false;
        }
    }

    private static native boolean nativeLaunch(String packageName);

    private static native int nativeCount();

    private static native String nativeNameAt(int slot);

    private static native String nativeLabelAt(int slot);

    private static native String nativeArchiveAt(int slot);

    private static native String nativeIconAt(int slot);

    private static native int nativeTargetSdkAt(int slot);
}
