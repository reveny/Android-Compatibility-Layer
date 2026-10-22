package android.hardware.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class UsbManager
{
    public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_ACCESSORY_ATTACHED = "android.hardware.usb.action.USB_ACCESSORY_ATTACHED";
    public static final String ACTION_USB_ACCESSORY_DETACHED = "android.hardware.usb.action.USB_ACCESSORY_DETACHED";
    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_ACCESSORY = "accessory";
    public static final String EXTRA_PERMISSION_GRANTED = "permission";

    private final Context mContext;

    public UsbManager(Context context)
    {
        mContext = context;
    }

    public HashMap<String, UsbDevice> getDeviceList()
    {
        return new HashMap<>();
    }

    public UsbDeviceConnection openDevice(UsbDevice device)
    {
        if (device == null) throw new IllegalArgumentException("device cannot be null");
        return null;
    }

    public UsbAccessory[] getAccessoryList()
    {
        return null;
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory accessory)
    {
        if (accessory == null) throw new IllegalArgumentException("accessory cannot be null");
        return null;
    }

    public InputStream openAccessoryInputStream(UsbAccessory accessory)
    {
        if (accessory == null) throw new IllegalArgumentException("accessory cannot be null");
        return null;
    }

    public OutputStream openAccessoryOutputStream(UsbAccessory accessory)
    {
        if (accessory == null) throw new IllegalArgumentException("accessory cannot be null");
        return null;
    }

    public boolean hasPermission(UsbDevice device)
    {
        return false;
    }

    public boolean hasPermission(UsbAccessory accessory)
    {
        return false;
    }

    public void requestPermission(UsbDevice device, PendingIntent pendingIntent)
    {
        Intent extras = new Intent();
        extras.putExtra(EXTRA_DEVICE, device);
        extras.putExtra(EXTRA_PERMISSION_GRANTED, false);
        sendPermissionResult(pendingIntent, extras);
    }

    public void requestPermission(UsbAccessory accessory, PendingIntent pendingIntent)
    {
        Intent extras = new Intent();
        extras.putExtra(EXTRA_ACCESSORY, accessory);
        extras.putExtra(EXTRA_PERMISSION_GRANTED, false);
        sendPermissionResult(pendingIntent, extras);
    }

    private void sendPermissionResult(PendingIntent pendingIntent, Intent extras)
    {
        if (pendingIntent == null) return;

        try
        {
            pendingIntent.send(mContext, 0, extras);
        }
        catch (PendingIntent.CanceledException exception)
        {
        }
    }
}
