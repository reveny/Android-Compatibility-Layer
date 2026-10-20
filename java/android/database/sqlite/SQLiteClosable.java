package android.database.sqlite;

public abstract class SQLiteClosable implements java.io.Closeable
{
    private int mReferenceCount = 1;

    protected abstract void onAllReferencesReleased();

    public void acquireReference()
    {
        synchronized (this)
        {
            if (mReferenceCount <= 0) throw new IllegalStateException("this object is already closed");
            mReferenceCount++;
        }
    }

    public void releaseReference()
    {
        boolean released;

        synchronized (this)
        {
            mReferenceCount--;
            released = mReferenceCount == 0;
        }

        if (released) onAllReferencesReleased();
    }

    @Override
    public void close()
    {
        releaseReference();
    }
}
