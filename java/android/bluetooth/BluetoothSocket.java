package android.bluetooth;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class BluetoothSocket implements Closeable
{
    public static final int TYPE_RFCOMM = 1;
    public static final int TYPE_SCO = 2;
    public static final int TYPE_L2CAP = 3;

    private final BluetoothDevice mDevice;
    private final int mType;
    private volatile boolean mClosed;

    BluetoothSocket(BluetoothDevice device, int type)
    {
        mDevice = device;
        mType = type;
    }

    public BluetoothDevice getRemoteDevice()
    {
        return mDevice;
    }

    public InputStream getInputStream() throws IOException
    {
        throw new IOException("socket not connected");
    }

    public OutputStream getOutputStream() throws IOException
    {
        throw new IOException("socket not connected");
    }

    public boolean isConnected()
    {
        return false;
    }

    public void connect() throws IOException
    {
        if (mClosed) throw new IOException("socket closed");

        throw new IOException("Bluetooth is off");
    }

    @Override
    public void close() throws IOException
    {
        mClosed = true;
    }

    public int getMaxTransmitPacketSize()
    {
        return 0;
    }

    public int getMaxReceivePacketSize()
    {
        return 0;
    }

    public int getConnectionType()
    {
        return mType;
    }

    @Override
    public String toString()
    {
        return "BluetoothSocket{type=" + mType + ", device=" + mDevice + "}";
    }
}
