package android.os;

public abstract class ProxyFileDescriptorCallback
{
    public long onGetSize()
    {
        throw new UnsupportedOperationException("onGetSize is not implemented");
    }

    public int onRead(long offset, int size, byte[] data)
    {
        throw new UnsupportedOperationException("onRead is not implemented");
    }

    public int onWrite(long offset, int size, byte[] data)
    {
        throw new UnsupportedOperationException("onWrite is not implemented");
    }

    public void onFsync()
    {
        throw new UnsupportedOperationException("onFsync is not implemented");
    }

    public abstract void onRelease();
}
