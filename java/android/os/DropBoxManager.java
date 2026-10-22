package android.os;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class DropBoxManager
{
    public static final String ACTION_DROPBOX_ENTRY_ADDED = "android.intent.action.DROPBOX_ENTRY_ADDED";
    public static final String EXTRA_TAG = "tag";
    public static final String EXTRA_TIME = "time";
    public static final String EXTRA_DROPPED_COUNT = "android.os.extra.DROPPED_COUNT";

    public static final int IS_EMPTY = 1;
    public static final int IS_TEXT = 2;
    public static final int IS_GZIPPED = 4;

    private static final int MAX_ENTRIES = 64;
    private static final List<Entry> sEntries = new ArrayList<>();
    private static long sLastTime;

    protected DropBoxManager()
    {
    }

    public DropBoxManager(android.content.Context context)
    {
    }

    public void addText(String tag, String data)
    {
        if (tag == null || data == null) throw new NullPointerException("tag and data are required");
        store(new Entry(tag, nextTime(), data));
    }

    public void addData(String tag, byte[] data, int flags)
    {
        if (tag == null) throw new NullPointerException("tag");
        if ((flags & IS_EMPTY) != 0) throw new IllegalArgumentException("Bad flags: " + flags);
        store(new Entry(tag, nextTime(), data, flags));
    }

    public void addFile(String tag, File file, int flags) throws IOException
    {
        if (tag == null || file == null) throw new NullPointerException("tag and file are required");
        if ((flags & IS_EMPTY) != 0) throw new IllegalArgumentException("Bad flags: " + flags);
        store(new Entry(tag, nextTime(), file, flags));
    }

    public boolean isTagEnabled(String tag)
    {
        return true;
    }

    public Entry getNextEntry(String tag, long msec)
    {
        synchronized (sEntries)
        {
            for (Entry entry : sEntries)
            {
                if (entry.mTimeMillis <= msec) continue;
                if (tag != null && !tag.equals(entry.mTag)) continue;

                return new Entry(entry.mTag, entry.mTimeMillis, entry.mData, entry.mFlags);
            }
            return null;
        }
    }

    private static long nextTime()
    {
        synchronized (sEntries)
        {
            long now = System.currentTimeMillis();
            sLastTime = now <= sLastTime ? sLastTime + 1 : now;
            return sLastTime;
        }
    }

    private static void store(Entry entry)
    {
        synchronized (sEntries)
        {
            sEntries.add(entry);
            while (sEntries.size() > MAX_ENTRIES) sEntries.remove(0);
        }
    }

    public static class Entry implements Parcelable, Closeable
    {
        public static final Parcelable.Creator<Entry> CREATOR = new Parcelable.Creator<Entry>()
        {
            @Override
            public Entry createFromParcel(Parcel source)
            {
                String tag = source.readString();
                long millis = source.readLong();
                int flags = source.readInt();
                return new Entry(tag, millis, source.createByteArray(), flags);
            }

            @Override
            public Entry[] newArray(int size)
            {
                return new Entry[size];
            }
        };

        private final String mTag;
        private final long mTimeMillis;
        private final byte[] mData;
        private final int mFlags;

        public Entry(String tag, long millis)
        {
            if (tag == null) throw new NullPointerException("tag == null");

            mTag = tag;
            mTimeMillis = millis;
            mData = null;
            mFlags = IS_EMPTY;
        }

        public Entry(String tag, long millis, String text)
        {
            if (tag == null) throw new NullPointerException("tag == null");
            if (text == null) throw new NullPointerException("text == null");

            mTag = tag;
            mTimeMillis = millis;
            mData = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            mFlags = IS_TEXT;
        }

        public Entry(String tag, long millis, byte[] data, int flags)
        {
            if (tag == null) throw new NullPointerException("tag == null");
            if (((flags & IS_EMPTY) != 0) != (data == null)) throw new IllegalArgumentException("Bad flags: " + flags);

            mTag = tag;
            mTimeMillis = millis;
            mData = data == null ? null : data.clone();
            mFlags = flags;
        }

        public Entry(String tag, long millis, ParcelFileDescriptor data, int flags)
        {
            this(tag, millis, readAll(data), flags);
        }

        public Entry(String tag, long millis, File data, int flags) throws IOException
        {
            this(tag, millis, readFile(data), flags);
        }

        private static byte[] readAll(ParcelFileDescriptor descriptor)
        {
            if (descriptor == null) return null;

            try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor))
            {
                return readStream(input);
            }
            catch (IOException unreadable)
            {
                return new byte[0];
            }
        }

        private static byte[] readFile(File file) throws IOException
        {
            try (InputStream input = new FileInputStream(file))
            {
                return readStream(input);
            }
        }

        private static byte[] readStream(InputStream input) throws IOException
        {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) > 0) output.write(buffer, 0, count);
            return output.toByteArray();
        }

        @Override
        public void close()
        {
        }

        public String getTag()
        {
            return mTag;
        }

        public long getTimeMillis()
        {
            return mTimeMillis;
        }

        public int getFlags()
        {
            return mFlags & ~IS_GZIPPED;
        }

        public String getText(int maxBytes)
        {
            if ((mFlags & IS_TEXT) == 0 || mData == null) return null;

            try (InputStream input = getInputStream())
            {
                if (input == null) return null;

                byte[] buffer = new byte[Math.max(0, maxBytes)];
                int total = 0;
                while (total < buffer.length)
                {
                    int count = input.read(buffer, total, buffer.length - total);
                    if (count < 0) break;
                    total += count;
                }
                return new String(buffer, 0, total, java.nio.charset.StandardCharsets.UTF_8);
            }
            catch (IOException unreadable)
            {
                return null;
            }
        }

        public InputStream getInputStream() throws IOException
        {
            if (mData == null) return null;

            InputStream input = new ByteArrayInputStream(mData);
            return (mFlags & IS_GZIPPED) != 0 ? new GZIPInputStream(input) : input;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(mTag);
            dest.writeLong(mTimeMillis);
            dest.writeInt(mFlags);
            dest.writeByteArray(mData);
        }
    }
}
