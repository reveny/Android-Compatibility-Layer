package android.bluetooth;

import java.io.Closeable;
import java.io.IOException;

public final class BluetoothServerSocket implements Closeable
{
    private BluetoothServerSocket()
    {
    }

    public BluetoothSocket accept() throws IOException
    {
        return accept(-1);
    }

    public BluetoothSocket accept(int timeout) throws IOException
    {
        throw new IOException("Bluetooth is off");
    }

    @Override
    public void close() throws IOException
    {
    }

    public int getPsm()
    {
        return -1;
    }

    @Override
    public String toString()
    {
        return "ServerSocket: Type: unknown";
    }
}
