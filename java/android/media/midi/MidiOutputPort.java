package android.media.midi;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MidiOutputPort extends MidiSender implements Closeable
{
    private final int mPortNumber;
    private final CopyOnWriteArrayList<MidiReceiver> mReceivers = new CopyOnWriteArrayList<>();
    private volatile boolean mClosed;

    MidiOutputPort(int portNumber)
    {
        mPortNumber = portNumber;
    }

    @Override
    public void close() throws IOException
    {
        mClosed = true;
        mReceivers.clear();
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            close();
        }
        finally
        {
            super.finalize();
        }
    }

    public int getPortNumber()
    {
        return mPortNumber;
    }

    @Override
    public void onConnect(MidiReceiver receiver)
    {
        if (!mClosed) mReceivers.addIfAbsent(receiver);
    }

    @Override
    public void onDisconnect(MidiReceiver receiver)
    {
        mReceivers.remove(receiver);
    }

    void dispatch(byte[] message, int offset, int count, long timestamp) throws IOException
    {
        for (MidiReceiver receiver : mReceivers)
        {
            receiver.send(message, offset, count, timestamp);
        }
    }
}
