package android.media.midi;

import java.io.Closeable;
import java.io.IOException;

public final class MidiInputPort extends MidiReceiver implements Closeable
{
    private static final int MAX_PACKET_DATA_SIZE = 1015;

    private final int mPortNumber;
    private final MidiReceiver mTarget;
    private boolean mClosed;

    MidiInputPort(int portNumber, MidiReceiver target)
    {
        super(MAX_PACKET_DATA_SIZE);
        mPortNumber = portNumber;
        mTarget = target;
    }

    @Override
    public void close() throws IOException
    {
        synchronized (this)
        {
            mClosed = true;
        }
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
    public void onFlush() throws IOException
    {
        MidiReceiver target = checkOpen();

        if (target != null) target.flush();
    }

    @Override
    public void onSend(byte[] message, int offset, int count, long timestamp) throws IOException
    {
        if (offset < 0 || count < 0 || offset + count > message.length) throw new IllegalArgumentException("offset or count out of range");
        if (count > MAX_PACKET_DATA_SIZE) throw new IllegalArgumentException("count exceeds max message size");

        MidiReceiver target = checkOpen();

        if (target != null) target.send(message, offset, count, timestamp);
    }

    private synchronized MidiReceiver checkOpen() throws IOException
    {
        if (mClosed) throw new IOException("MidiInputPort is closed");

        return mTarget;
    }
}
