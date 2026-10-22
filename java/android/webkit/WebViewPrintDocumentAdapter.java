package android.webkit;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class WebViewPrintDocumentAdapter extends PrintDocumentAdapter
{
    private static final int POINTS_PER_INCH = 72;
    private static final int MILS_PER_INCH = 1000;

    private final String mDocumentName;
    private PrintAttributes mAttributes;

    WebViewPrintDocumentAdapter(String documentName)
    {
        mDocumentName = documentName == null || documentName.isEmpty() ? "default" : documentName;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal,
        LayoutResultCallback callback, Bundle extras)
    {
        if (cancellationSignal != null && cancellationSignal.isCanceled())
        {
            callback.onLayoutCancelled();
            return;
        }
        mAttributes = newAttributes;
        PrintDocumentInfo info = new PrintDocumentInfo.Builder(mDocumentName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build();
        callback.onLayoutFinished(info, oldAttributes == null || !newAttributes.equals(oldAttributes));
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal,
        WriteResultCallback callback)
    {
        if (cancellationSignal != null && cancellationSignal.isCanceled())
        {
            callback.onWriteCancelled();
            return;
        }
        try
        {
            FileOutputStream output = new FileOutputStream(destination.getFileDescriptor());
            output.write(blankPdf());
            output.flush();
            callback.onWriteFinished(new PageRange[] {new PageRange(0, 0)});
        }
        catch (IOException exception)
        {
            callback.onWriteFailed(exception.getMessage());
        }
    }

    private byte[] blankPdf()
    {
        int width = 612;
        int height = 792;
        PrintAttributes.MediaSize mediaSize = mAttributes == null ? null : mAttributes.getMediaSize();
        if (mediaSize != null)
        {
            width = mediaSize.getWidthMils() * POINTS_PER_INCH / MILS_PER_INCH;
            height = mediaSize.getHeightMils() * POINTS_PER_INCH / MILS_PER_INCH;
        }

        String[] objects = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + width + " " + height + "] /Contents 4 0 R /Resources << >> >>",
            "<< /Length 0 >>\nstream\n\nendstream",
        };

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        append(buffer, "%PDF-1.4\n");
        for (int index = 0; index < objects.length; index++)
        {
            offsets.add(buffer.size());
            append(buffer, (index + 1) + " 0 obj\n" + objects[index] + "\nendobj\n");
        }
        int xref = buffer.size();
        StringBuilder trailer = new StringBuilder();
        trailer.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int offset : offsets) trailer.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        trailer.append("trailer\n<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\nstartxref\n").append(xref)
            .append("\n%%EOF\n");
        append(buffer, trailer.toString());
        return buffer.toByteArray();
    }

    private static void append(ByteArrayOutputStream buffer, String text)
    {
        byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
        buffer.write(bytes, 0, bytes.length);
    }
}
