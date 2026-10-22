package android.os;

import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ParcelFileDescriptor implements Parcelable, Closeable
{
    public static final int MODE_WORLD_READABLE = 0x00000001;
    public static final int MODE_WORLD_WRITEABLE = 0x00000002;
    public static final int MODE_READ_ONLY = 0x10000000;
    public static final int MODE_WRITE_ONLY = 0x20000000;
    public static final int MODE_READ_WRITE = 0x30000000;
    public static final int MODE_CREATE = 0x08000000;
    public static final int MODE_TRUNCATE = 0x04000000;
    public static final int MODE_APPEND = 0x02000000;

    private static final int MODE_ACCESS_MASK = 0x30000000;

    public static final Parcelable.Creator<ParcelFileDescriptor> CREATOR = new Parcelable.Creator<ParcelFileDescriptor>()
    {
        @Override
        public ParcelFileDescriptor createFromParcel(Parcel source)
        {
            Object value = source.readValue(null);
            return value instanceof ParcelFileDescriptor ? (ParcelFileDescriptor)value : null;
        }

        @Override
        public ParcelFileDescriptor[] newArray(int size)
        {
            return new ParcelFileDescriptor[size];
        }
    };

    private final ParcelFileDescriptor mWrapped;
    private final FileDescriptor mDescriptor;
    private int mFd;
    private Closeable mResource;
    private OnCloseListener mCloseListener;
    private Handler mCloseHandler;

    public interface OnCloseListener
    {
        void onClose(IOException exception);
    }

    public static class FileDescriptorDetachedException extends IOException
    {
        public FileDescriptorDetachedException()
        {
            super("Remote side is detached");
        }
    }

    public ParcelFileDescriptor(FileDescriptor descriptor, int fd)
    {
        mWrapped = null;
        mDescriptor = descriptor;
        mFd = fd;
    }

    public ParcelFileDescriptor(ParcelFileDescriptor wrapped)
    {
        mWrapped = wrapped;
        mDescriptor = null;
        mFd = -1;
    }

    private ParcelFileDescriptor(FileDescriptor descriptor, Closeable resource)
    {
        mWrapped = null;
        mDescriptor = descriptor;
        mResource = resource;
        mFd = descriptorInt(descriptor);
    }

    public static ParcelFileDescriptor open(File file, int mode) throws FileNotFoundException
    {
        if (file == null) throw new NullPointerException("file");

        int access = mode & MODE_ACCESS_MASK;
        if (access == 0) throw new IllegalArgumentException("Must specify MODE_READ_ONLY, MODE_WRITE_ONLY, or MODE_READ_WRITE");

        if ((mode & MODE_CREATE) == 0 && !file.exists()) throw new FileNotFoundException(file + ": open failed: ENOENT");

        try
        {
            if (access == MODE_READ_ONLY)
            {
                FileInputStream stream = new FileInputStream(file);
                return new ParcelFileDescriptor(stream.getFD(), stream);
            }

            if (access == MODE_WRITE_ONLY && (mode & MODE_TRUNCATE) != 0)
            {
                FileOutputStream stream = new FileOutputStream(file, false);
                return new ParcelFileDescriptor(stream.getFD(), stream);
            }

            if (access == MODE_WRITE_ONLY && (mode & MODE_APPEND) != 0)
            {
                FileOutputStream stream = new FileOutputStream(file, true);
                return new ParcelFileDescriptor(stream.getFD(), stream);
            }

            RandomAccessFile random = new RandomAccessFile(file, "rw");
            if ((mode & MODE_TRUNCATE) != 0) random.setLength(0);
            if ((mode & MODE_APPEND) != 0) random.seek(random.length());
            return new ParcelFileDescriptor(random.getFD(), random);
        }
        catch (FileNotFoundException missing)
        {
            throw missing;
        }
        catch (IOException failure)
        {
            FileNotFoundException wrapped = new FileNotFoundException(file + ": open failed: " + failure.getMessage());
            wrapped.initCause(failure);
            throw wrapped;
        }
    }

    public static ParcelFileDescriptor open(File file, int mode, Handler handler, OnCloseListener listener) throws IOException
    {
        if (handler == null) throw new IllegalArgumentException("Handler must not be null");
        if (listener == null) throw new IllegalArgumentException("Listener must not be null");

        ParcelFileDescriptor descriptor = open(file, mode);
        descriptor.mCloseHandler = handler;
        descriptor.mCloseListener = listener;
        return descriptor;
    }

    public static ParcelFileDescriptor wrap(ParcelFileDescriptor descriptor, Handler handler, OnCloseListener listener)
        throws IOException
    {
        ParcelFileDescriptor duplicate = descriptor.dup();
        duplicate.mCloseHandler = handler;
        duplicate.mCloseListener = listener;
        return duplicate;
    }

    public static ParcelFileDescriptor dup(FileDescriptor original) throws IOException
    {
        return fromFd(descriptorInt(original));
    }

    public ParcelFileDescriptor dup() throws IOException
    {
        return fromFd(getFd());
    }

    public static ParcelFileDescriptor fromFd(int fd) throws IOException
    {
        if (fd < 0) throw new IOException("invalid file descriptor " + fd);

        File path = new File("/proc/self/fd/" + fd);
        try
        {
            RandomAccessFile random = new RandomAccessFile(path, "rw");
            return new ParcelFileDescriptor(random.getFD(), random);
        }
        catch (IOException readOnly)
        {
            if (path.canRead())
            {
                FileInputStream stream = new FileInputStream(path);
                return new ParcelFileDescriptor(stream.getFD(), stream);
            }

            FileOutputStream stream = new FileOutputStream(path, true);
            return new ParcelFileDescriptor(stream.getFD(), stream);
        }
    }

    public static ParcelFileDescriptor adoptFd(int fd)
    {
        FileDescriptor descriptor = new FileDescriptor();
        setDescriptorInt(descriptor, fd);
        return new ParcelFileDescriptor(descriptor, fd);
    }

    public static ParcelFileDescriptor fromSocket(java.net.Socket socket)
    {
        return null;
    }

    public static ParcelFileDescriptor fromDatagramSocket(java.net.DatagramSocket socket)
    {
        return null;
    }

    public static ParcelFileDescriptor[] createPipe() throws IOException
    {
        java.nio.channels.Pipe pipe = java.nio.channels.Pipe.open();
        FileDescriptor source = channelDescriptor(pipe.source());
        FileDescriptor sink = channelDescriptor(pipe.sink());

        if (source == null || sink == null)
        {
            pipe.source().close();
            pipe.sink().close();
            throw new IOException("pipes are not available on this runtime");
        }

        pipe.source().configureBlocking(true);
        pipe.sink().configureBlocking(true);

        return new ParcelFileDescriptor[] {new ParcelFileDescriptor(source, pipe.source()), new ParcelFileDescriptor(sink, pipe.sink())};
    }

    public static ParcelFileDescriptor[] createReliablePipe() throws IOException
    {
        return createPipe();
    }

    public static ParcelFileDescriptor[] createSocketPair() throws IOException
    {
        throw new IOException("socket pairs are not available on this runtime");
    }

    public static ParcelFileDescriptor[] createReliableSocketPair() throws IOException
    {
        return createSocketPair();
    }

    public static int parseMode(String mode)
    {
        if ("r".equals(mode)) return MODE_READ_ONLY;
        if ("w".equals(mode) || "wt".equals(mode)) return MODE_WRITE_ONLY | MODE_CREATE | MODE_TRUNCATE;
        if ("wa".equals(mode)) return MODE_WRITE_ONLY | MODE_CREATE | MODE_APPEND;
        if ("rw".equals(mode)) return MODE_READ_WRITE | MODE_CREATE;
        if ("rwt".equals(mode)) return MODE_READ_WRITE | MODE_CREATE | MODE_TRUNCATE;

        throw new IllegalArgumentException("Bad mode: " + mode);
    }

    public FileDescriptor getFileDescriptor()
    {
        if (mWrapped != null) return mWrapped.getFileDescriptor();
        return mDescriptor;
    }

    public int getFd()
    {
        if (mWrapped != null) return mWrapped.getFd();
        if (mFd < 0 && mDescriptor != null && mResource != null) return descriptorInt(mDescriptor);
        return mFd;
    }

    public long getStatSize()
    {
        if (mWrapped != null) return mWrapped.getStatSize();

        try
        {
            if (mResource instanceof RandomAccessFile) return ((RandomAccessFile)mResource).length();
        }
        catch (IOException unreadable)
        {
            return -1;
        }

        int fd = getFd();
        if (fd < 0) return -1;

        File target = new File("/proc/self/fd/" + fd);
        return target.isFile() ? target.length() : -1;
    }

    public int detachFd()
    {
        if (mWrapped != null) return mWrapped.detachFd();

        int detached = getFd();
        mFd = -1;
        mResource = null;
        if (detached >= 0) nativeInvalidate(mDescriptor);
        return detached;
    }

    @Override
    public void close() throws IOException
    {
        if (mWrapped != null)
        {
            mWrapped.close();
            return;
        }

        if (mResource != null)
        {
            Closeable resource = mResource;
            mResource = null;
            mFd = -1;
            resource.close();
            notifyClosed(null);
            return;
        }

        if (mFd < 0) return;

        int closing = mFd;
        mFd = -1;
        if (!nativeClose(mDescriptor, closing)) throw new IOException("could not close descriptor " + closing);
        notifyClosed(null);
    }

    public void closeWithError(String message) throws IOException
    {
        close();
    }

    public boolean canDetectErrors()
    {
        return false;
    }

    public void checkError() throws IOException
    {
    }

    private void notifyClosed(final IOException exception)
    {
        final OnCloseListener listener = mCloseListener;
        if (listener == null) return;

        mCloseListener = null;
        Runnable report = new Runnable()
        {
            @Override
            public void run()
            {
                listener.onClose(exception);
            }
        };

        if (mCloseHandler != null) mCloseHandler.post(report);
        else report.run();
    }

    @Override
    public int describeContents()
    {
        return Parcelable.CONTENTS_FILE_DESCRIPTOR;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeValue(this);
    }

    @Override
    public String toString()
    {
        return "{ParcelFileDescriptor: " + getFd() + "}";
    }

    private static FileDescriptor channelDescriptor(Object channel)
    {
        for (Class<?> type = channel.getClass(); type != null; type = type.getSuperclass())
        {
            try
            {
                java.lang.reflect.Method method = type.getDeclaredMethod("getFD");
                method.setAccessible(true);
                Object result = method.invoke(channel);
                if (result instanceof FileDescriptor) return (FileDescriptor)result;
            }
            catch (ReflectiveOperationException | RuntimeException missing)
            {
                continue;
            }
        }
        return null;
    }

    static int descriptorInt(FileDescriptor descriptor)
    {
        if (descriptor == null) return -1;

        try
        {
            java.lang.reflect.Method method = FileDescriptor.class.getDeclaredMethod("getInt$");
            method.setAccessible(true);
            return (Integer)method.invoke(descriptor);
        }
        catch (ReflectiveOperationException | RuntimeException missing)
        {
            return descriptorField(descriptor);
        }
    }

    private static int descriptorField(FileDescriptor descriptor)
    {
        for (String name : new String[] {"descriptor", "fd"})
        {
            try
            {
                java.lang.reflect.Field field = FileDescriptor.class.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(descriptor);
            }
            catch (ReflectiveOperationException | RuntimeException missing)
            {
                continue;
            }
        }
        return -1;
    }

    private static void setDescriptorInt(FileDescriptor descriptor, int fd)
    {
        try
        {
            java.lang.reflect.Method method = FileDescriptor.class.getDeclaredMethod("setInt$", int.class);
            method.setAccessible(true);
            method.invoke(descriptor, fd);
            return;
        }
        catch (ReflectiveOperationException | RuntimeException missing)
        {
            for (String name : new String[] {"descriptor", "fd"})
            {
                try
                {
                    java.lang.reflect.Field field = FileDescriptor.class.getDeclaredField(name);
                    field.setAccessible(true);
                    field.setInt(descriptor, fd);
                    return;
                }
                catch (ReflectiveOperationException | RuntimeException absent)
                {
                    continue;
                }
            }
        }
    }

    static native boolean nativeClose(FileDescriptor descriptor, int fd);

    static native void nativeInvalidate(FileDescriptor descriptor);

    public static class AutoCloseInputStream extends FileInputStream
    {
        private final ParcelFileDescriptor mOwner;

        public AutoCloseInputStream(ParcelFileDescriptor owner)
        {
            super(owner.getFileDescriptor());
            mOwner = owner;
        }

        protected ParcelFileDescriptor owner()
        {
            return mOwner;
        }

        @Override
        public void close() throws IOException
        {
            try
            {
                super.close();
            }
            finally
            {
                mOwner.close();
            }
        }
    }

    public static class AutoCloseOutputStream extends FileOutputStream
    {
        private final ParcelFileDescriptor mOwner;

        public AutoCloseOutputStream(ParcelFileDescriptor owner)
        {
            super(owner.getFileDescriptor());
            mOwner = owner;
        }

        @Override
        public void close() throws IOException
        {
            try
            {
                super.close();
            }
            finally
            {
                mOwner.close();
            }
        }
    }
}
