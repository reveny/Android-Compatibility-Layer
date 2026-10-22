package android.print;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

public abstract class PrintDocumentAdapter
{
    public static final String EXTRA_PRINT_PREVIEW = "EXTRA_PRINT_PREVIEW";

    public PrintDocumentAdapter()
    {
    }

    public void onStart()
    {
    }

    public abstract void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal,
        LayoutResultCallback callback, Bundle extras);

    public abstract void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal,
        WriteResultCallback callback);

    public void onFinish()
    {
    }

    public abstract static class WriteResultCallback
    {
        WriteResultCallback()
        {
        }

        public void onWriteFinished(PageRange[] pages)
        {
        }

        public void onWriteFailed(CharSequence error)
        {
        }

        public void onWriteCancelled()
        {
        }
    }

    public abstract static class LayoutResultCallback
    {
        LayoutResultCallback()
        {
        }

        public void onLayoutFinished(PrintDocumentInfo info, boolean changed)
        {
        }

        public void onLayoutFailed(CharSequence error)
        {
        }

        public void onLayoutCancelled()
        {
        }
    }
}
