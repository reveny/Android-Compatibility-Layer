package android.os;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class FileObserver
{
    public static final int ACCESS = 0x00000001;
    public static final int MODIFY = 0x00000002;
    public static final int ATTRIB = 0x00000004;
    public static final int CLOSE_WRITE = 0x00000008;
    public static final int CLOSE_NOWRITE = 0x00000010;
    public static final int OPEN = 0x00000020;
    public static final int MOVED_FROM = 0x00000040;
    public static final int MOVED_TO = 0x00000080;
    public static final int CREATE = 0x00000100;
    public static final int DELETE = 0x00000200;
    public static final int DELETE_SELF = 0x00000400;
    public static final int MOVE_SELF = 0x00000800;
    public static final int ALL_EVENTS = ACCESS | MODIFY | ATTRIB | CLOSE_WRITE | CLOSE_NOWRITE | OPEN | MOVED_FROM | MOVED_TO | DELETE
                                         | CREATE | DELETE_SELF | MOVE_SELF;

    private static final String TAG = "FileObserver";
    private static final long POLL_INTERVAL_MILLIS = 1000L;

    private static final CopyOnWriteArrayList<FileObserver> sActive = new CopyOnWriteArrayList<>();
    private static Thread sPoller;

    private final List<File> mFiles;
    private final int mMask;
    private final Map<File, Map<String, long[]>> mSnapshots = new HashMap<>();

    public FileObserver(String path)
    {
        this(new File(path));
    }

    public FileObserver(File file)
    {
        this(Collections.singletonList(file));
    }

    public FileObserver(List<File> files)
    {
        this(files, ALL_EVENTS);
    }

    public FileObserver(String path, int mask)
    {
        this(new File(path), mask);
    }

    public FileObserver(File file, int mask)
    {
        this(Collections.singletonList(file), mask);
    }

    public FileObserver(List<File> files, int mask)
    {
        mFiles = new ArrayList<>(files);
        mMask = mask;
    }

    @Override
    protected void finalize()
    {
        stopWatching();
    }

    public void startWatching()
    {
        synchronized (mSnapshots)
        {
            mSnapshots.clear();
            for (File file : mFiles) mSnapshots.put(file, snapshot(file));
        }

        if (!sActive.addIfAbsent(this)) return;

        synchronized (FileObserver.class)
        {
            if (sPoller != null) return;

            sPoller = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    poll();
                }
            }, TAG);
            sPoller.setDaemon(true);
            sPoller.start();
        }
    }

    public void stopWatching()
    {
        sActive.remove(this);
    }

    public abstract void onEvent(int event, String path);

    private static void poll()
    {
        for (;;)
        {
            try
            {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            }
            catch (InterruptedException interrupted)
            {
                synchronized (FileObserver.class)
                {
                    sPoller = null;
                }
                return;
            }

            for (FileObserver observer : sActive)
            {
                try
                {
                    observer.check();
                }
                catch (Throwable failure)
                {
                    android.util.Log.e(TAG, "an observer threw while handling a file event", failure);
                }
            }
        }
    }

    private void check()
    {
        for (File file : mFiles)
        {
            Map<String, long[]> previous;
            Map<String, long[]> current = snapshot(file);

            synchronized (mSnapshots)
            {
                previous = mSnapshots.put(file, current);
            }

            if (previous == null) continue;

            for (Map.Entry<String, long[]> entry : previous.entrySet())
            {
                String name = entry.getKey();
                long[] now = current.get(name);

                if (now == null)
                {
                    dispatch(name.isEmpty() ? DELETE_SELF : DELETE, name.isEmpty() ? null : name);
                    continue;
                }

                long[] before = entry.getValue();
                if (before[0] != now[0] || before[1] != now[1])
                {
                    dispatch(MODIFY, name.isEmpty() ? null : name);
                    dispatch(CLOSE_WRITE, name.isEmpty() ? null : name);
                }
            }

            for (String name : current.keySet())
            {
                if (!previous.containsKey(name)) dispatch(CREATE, name.isEmpty() ? null : name);
            }
        }
    }

    private void dispatch(int event, String path)
    {
        if ((mMask & event) != 0) onEvent(event, path);
    }

    private static Map<String, long[]> snapshot(File file)
    {
        Map<String, long[]> entries = new HashMap<>();
        if (!file.exists()) return entries;

        entries.put("", new long[] {file.isDirectory() ? 0L : file.lastModified(), file.isDirectory() ? 0L : file.length()});

        if (!file.isDirectory()) return entries;

        File[] children = file.listFiles();
        if (children == null) return entries;

        for (File child : children) entries.put(child.getName(), new long[] {child.lastModified(), child.length()});
        return entries;
    }
}
