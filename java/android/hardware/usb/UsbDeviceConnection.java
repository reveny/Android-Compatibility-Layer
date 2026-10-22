package android.hardware.usb;

import java.util.concurrent.TimeoutException;

public class UsbDeviceConnection
{
    private final UsbDevice mDevice;
    private boolean mOpen = true;

    UsbDeviceConnection(UsbDevice device)
    {
        mDevice = device;
    }

    boolean isOpen()
    {
        return mOpen;
    }

    public void close()
    {
        mOpen = false;
    }

    public int getFileDescriptor()
    {
        return -1;
    }

    public byte[] getRawDescriptors()
    {
        return null;
    }

    public boolean claimInterface(UsbInterface usbInterface, boolean force)
    {
        return false;
    }

    public boolean releaseInterface(UsbInterface usbInterface)
    {
        return false;
    }

    public boolean setInterface(UsbInterface usbInterface)
    {
        return false;
    }

    public boolean setConfiguration(UsbConfiguration configuration)
    {
        return false;
    }

    public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int length, int timeout)
    {
        return controlTransfer(requestType, request, value, index, buffer, 0, length, timeout);
    }

    public int controlTransfer(int requestType, int request, int value, int index, byte[] buffer, int offset, int length, int timeout)
    {
        checkBounds(buffer, offset, length);
        return -1;
    }

    public int bulkTransfer(UsbEndpoint endpoint, byte[] buffer, int length, int timeout)
    {
        return bulkTransfer(endpoint, buffer, 0, length, timeout);
    }

    public int bulkTransfer(UsbEndpoint endpoint, byte[] buffer, int offset, int length, int timeout)
    {
        checkBounds(buffer, offset, length);
        return -1;
    }

    public UsbRequest requestWait()
    {
        return null;
    }

    public UsbRequest requestWait(long timeout) throws TimeoutException
    {
        if (timeout < 0) throw new IllegalArgumentException("timeout must be >= 0");
        throw new TimeoutException();
    }

    public String getSerial()
    {
        return mDevice == null ? null : mDevice.getSerialNumber();
    }

    private static void checkBounds(byte[] buffer, int start, int length)
    {
        final int bufferLength = buffer != null ? buffer.length : 0;
        if (length < 0 || start < 0 || start + length > bufferLength)
        {
            throw new IllegalArgumentException("Buffer start or length out of bounds.");
        }
    }
}
