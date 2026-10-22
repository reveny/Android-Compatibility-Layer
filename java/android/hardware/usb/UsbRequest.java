package android.hardware.usb;

import java.nio.ByteBuffer;

public class UsbRequest
{
    private UsbEndpoint mEndpoint;
    private UsbDeviceConnection mConnection;
    private Object mClientData;

    public UsbRequest()
    {
    }

    public boolean initialize(UsbDeviceConnection connection, UsbEndpoint endpoint)
    {
        if (connection == null || endpoint == null || !connection.isOpen()) return false;

        mConnection = connection;
        mEndpoint = endpoint;
        return true;
    }

    public void close()
    {
        mConnection = null;
        mEndpoint = null;
    }

    public UsbEndpoint getEndpoint()
    {
        return mEndpoint;
    }

    public Object getClientData()
    {
        return mClientData;
    }

    public void setClientData(Object data)
    {
        mClientData = data;
    }

    @Deprecated
    public boolean queue(ByteBuffer buffer, int length)
    {
        return false;
    }

    public boolean queue(ByteBuffer buffer)
    {
        if (mConnection == null) throw new IllegalStateException("Request is not initialized");
        return false;
    }

    public boolean cancel()
    {
        return false;
    }
}
