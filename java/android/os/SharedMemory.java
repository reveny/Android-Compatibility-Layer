package android.os;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class SharedMemory implements Closeable, Parcelable
{
    private static final int PROT_READ = 1;
    private static final int PROT_WRITE = 2;
    private static final int PROT_EXEC = 4;
    private static final int PROT_MASK = PROT_READ | PROT_WRITE | PROT_EXEC;

    private static final int ENOMEM = 12;
    private static final int EACCES = 13;

    private static final Map<ByteBuffer, Boolean> sMapped = Collections.synchronizedMap(new WeakHashMap<ByteBuffer, Boolean>());

    public static final Creator<SharedMemory> CREATOR = new Creator<SharedMemory>()
    {
        @Override
        public SharedMemory createFromParcel(Parcel source)
        {
            ParcelFileDescriptor descriptor = source.readFileDescriptor();
            return descriptor == null ? null : fromFileDescriptor(descriptor);
        }

        @Override
        public SharedMemory[] newArray(int size)
        {
            return new SharedMemory[size];
        }
    };

    private RandomAccessFile mFile;
    private FileChannel mChannel;
    private int mSize;
    private int mProtection = PROT_MASK;

    SharedMemory()
    {
    }

    public static SharedMemory create(String name, int size)
    {
        if (size <= 0) throw new IllegalArgumentException("Size must be greater than zero");

        File directory = new File("/dev/shm");
        if (!directory.isDirectory()) directory = new File(System.getProperty("java.io.tmpdir", "/tmp"));

        try
        {
            File backing = File.createTempFile("shm-" + (name == null ? "anonymous" : name.replace('/', '_')) + "-", ".mem", directory);
            RandomAccessFile file = new RandomAccessFile(backing, "rw");
            backing.delete();
            file.setLength(size);

            SharedMemory memory = new SharedMemory();
            memory.adopt(file, size);
            return memory;
        }
        catch (IOException failure)
        {
            throw SharedMemory.<RuntimeException>sneaky(errno("SharedMemory.create", ENOMEM, failure));
        }
    }

    public static SharedMemory fromFileDescriptor(ParcelFileDescriptor descriptor)
    {
        if (descriptor == null) throw new NullPointerException("descriptor");

        try
        {
            RandomAccessFile file = new RandomAccessFile("/proc/self/fd/" + descriptor.getFd(), "rw");
            SharedMemory memory = new SharedMemory();
            memory.adopt(file, (int)file.length());
            return memory;
        }
        catch (IOException failure)
        {
            throw new IllegalArgumentException("the file descriptor is not a valid shared memory region", failure);
        }
    }

    private void adopt(RandomAccessFile file, int size) throws IOException
    {
        mFile = file;
        mChannel = file.getChannel();
        mSize = size;
    }

    private void checkOpen()
    {
        if (mChannel == null) throw new IllegalStateException("SharedMemory is closed");
    }

    public synchronized int getSize()
    {
        checkOpen();
        return mSize;
    }

    public synchronized boolean setProtect(int protection)
    {
        checkOpen();
        if ((protection & ~PROT_MASK) != 0) throw new IllegalArgumentException("Invalid prot value");
        if ((protection & ~mProtection) != 0) return false;

        mProtection = protection;
        return true;
    }

    public ByteBuffer mapReadWrite()
    {
        return map(PROT_READ | PROT_WRITE, 0, getSize());
    }

    public ByteBuffer mapReadOnly()
    {
        return map(PROT_READ, 0, getSize());
    }

    public synchronized ByteBuffer map(int protection, int offset, int length)
    {
        checkOpen();
        if ((protection & ~PROT_MASK) != 0) throw new IllegalArgumentException("Invalid prot value");
        if (offset < 0) throw new IllegalArgumentException("Offset must be >= 0");
        if (length <= 0) throw new IllegalArgumentException("Length must be > 0");
        if (offset + length > mSize) throw new IllegalArgumentException("offset + length must not exceed getSize()");

        if ((protection & ~mProtection) != 0)
        {
            throw SharedMemory.<RuntimeException>sneaky(errno("mmap", EACCES, null));
        }

        try
        {
            boolean writable = (protection & PROT_WRITE) != 0;
            MappedByteBuffer mapped = mChannel.map(writable ? FileChannel.MapMode.READ_WRITE : FileChannel.MapMode.READ_ONLY, offset, length);
            sMapped.put(mapped, Boolean.TRUE);
            return mapped;
        }
        catch (IOException failure)
        {
            throw SharedMemory.<RuntimeException>sneaky(errno("mmap", ENOMEM, failure));
        }
    }

    public static void unmap(ByteBuffer buffer)
    {
        if (buffer == null || sMapped.remove(buffer) == null)
        {
            throw new IllegalArgumentException("ByteBuffer wasn't created by #map(int, int, int); can't unmap");
        }
    }

    @Override
    public synchronized void close()
    {
        if (mChannel == null) return;

        try
        {
            mFile.close();
        }
        catch (IOException ignored)
        {
        }

        mChannel = null;
        mFile = null;
    }

    @Override
    public int describeContents()
    {
        return CONTENTS_FILE_DESCRIPTOR;
    }

    @Override
    public synchronized void writeToParcel(Parcel destination, int flags)
    {
        checkOpen();

        try
        {
            destination.writeFileDescriptor(mFile.getFD());
        }
        catch (IOException failure)
        {
            throw new IllegalStateException("the shared memory descriptor is no longer valid", failure);
        }
    }

    private static Exception errno(String functionName, int errno, Throwable cause)
    {
        try
        {
            Class<?> type = Class.forName("android.system.ErrnoException");
            Exception exception = (Exception)type.getConstructor(String.class, int.class).newInstance(functionName, errno);
            if (cause != null) exception.initCause(cause);
            return exception;
        }
        catch (ReflectiveOperationException absent)
        {
            return new IllegalStateException(functionName + " failed with errno " + errno, cause);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneaky(Throwable throwable) throws T
    {
        throw (T)throwable;
    }
}
