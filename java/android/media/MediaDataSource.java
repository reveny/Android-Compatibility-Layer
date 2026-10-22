package android.media;

import java.io.Closeable;
import java.io.IOException;

public abstract class MediaDataSource implements Closeable
{
    public MediaDataSource()
    {
    }

    public abstract long getSize() throws IOException;

    public abstract int readAt(long position, byte[] buffer, int offset, int size) throws IOException;
}
