package android.database.sqlite;

import java.io.Closeable;

public abstract class SQLiteClosable implements Closeable
{
    private int mReferenceCount = 1;

    public SQLiteClosable()
    {
    }

    protected abstract void onAllReferencesReleased();

    @Deprecated
    protected void onAllReferencesReleasedFromContainer()
    {
        onAllReferencesReleased();
    }

    public void acquireReference()
    {
        synchronized (this)
        {
            if (mReferenceCount <= 0) throw new IllegalStateException("attempt to re-open an already-closed object: " + this);
            mReferenceCount++;
        }
    }

    public void releaseReference()
    {
        boolean released;

        synchronized (this)
        {
            released = --mReferenceCount == 0;
        }

        if (released) onAllReferencesReleased();
    }

    @Deprecated
    public void releaseReferenceFromContainer()
    {
        boolean released;

        synchronized (this)
        {
            released = --mReferenceCount == 0;
        }

        if (released) onAllReferencesReleasedFromContainer();
    }

    public void close()
    {
        releaseReference();
    }
}
