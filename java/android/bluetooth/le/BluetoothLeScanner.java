package android.bluetooth.le;

import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

public final class BluetoothLeScanner
{
    public static final String EXTRA_CALLBACK_TYPE = "android.bluetooth.le.extra.CALLBACK_TYPE";
    public static final String EXTRA_ERROR_CODE = "android.bluetooth.le.extra.ERROR_CODE";
    public static final String EXTRA_LIST_SCAN_RESULT = "android.bluetooth.le.extra.LIST_SCAN_RESULT";

    BluetoothLeScanner()
    {
    }

    public void startScan(ScanCallback callback)
    {
        startScan(null, new ScanSettings.Builder().build(), callback);
    }

    public void startScan(List<ScanFilter> filters, ScanSettings settings, final ScanCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("callback is null");
        if (settings == null) throw new IllegalArgumentException("settings is null");

        Looper main = Looper.getMainLooper();
        if (main == null) return;

        new Handler(main).post(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onScanFailed(ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED);
            }
        });
    }

    public int startScan(List<ScanFilter> filters, ScanSettings settings, PendingIntent callbackIntent)
    {
        return ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED;
    }

    public void stopScan(ScanCallback callback)
    {
    }

    public void stopScan(PendingIntent callbackIntent)
    {
    }

    public void flushPendingScanResults(ScanCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("callback cannot be null!");
    }
}
