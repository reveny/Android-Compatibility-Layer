package android.media.midi;

import java.io.IOException;

public abstract class MidiReceiver
{
    private final int mMaxMessageSize;

    public MidiReceiver()
    {
        mMaxMessageSize = Integer.MAX_VALUE;
    }

    public MidiReceiver(int maxMessageSize)
    {
        mMaxMessageSize = maxMessageSize;
    }

    public void flush() throws IOException
    {
        onFlush();
    }

    public final int getMaxMessageSize()
    {
        return mMaxMessageSize;
    }

    public void onFlush() throws IOException
    {
    }

    public abstract void onSend(byte[] message, int offset, int count, long timestamp) throws IOException;

    public void send(byte[] message, int offset, int count) throws IOException
    {
        send(message, offset, count, System.nanoTime());
    }

    public void send(byte[] message, int offset, int count, long timestamp) throws IOException
    {
        int messageSize = getMaxMessageSize();

        while (count > 0)
        {
            int length = count > messageSize ? messageSize : count;
            onSend(message, offset, length, timestamp);
            offset += length;
            count -= length;
        }
    }
}
