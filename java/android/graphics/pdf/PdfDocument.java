package android.graphics.pdf;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;

public class PdfDocument
{
    private final ArrayList<PageInfo> mPages = new ArrayList<>();
    private final ArrayList<Picture> mPictures = new ArrayList<>();
    private Page mCurrentPage;
    private boolean mClosed;

    public PdfDocument()
    {
    }

    public Page startPage(PageInfo pageInfo)
    {
        throwIfClosed();
        if (mCurrentPage != null) throw new IllegalStateException("Previous page not finished!");
        if (pageInfo == null) throw new IllegalArgumentException("page cannot be null");

        Picture picture = new Picture();
        Canvas canvas = picture.beginRecording(pageInfo.getPageWidth(), pageInfo.getPageHeight());
        Rect content = pageInfo.getContentRect();
        canvas.clipRect(content);
        canvas.translate(content.left, content.top);

        mCurrentPage = new Page(canvas, pageInfo, picture);
        return mCurrentPage;
    }

    public void finishPage(Page page)
    {
        throwIfClosed();
        if (page == null) throw new IllegalArgumentException("page cannot be null");
        if (page != mCurrentPage) throw new IllegalStateException("invalid page");
        if (page.isFinished()) throw new IllegalStateException("page already finished");

        page.finish();
        mPages.add(page.getInfo());
        mPictures.add(page.mPicture);
        mCurrentPage = null;
    }

    public void writeTo(OutputStream out) throws IOException
    {
        throwIfClosed();
        throwIfCurrentPageNotFinished();
        if (out == null) throw new IllegalArgumentException("out cannot be null!");

        PdfWriter writer = new PdfWriter(out);
        int pageCount = mPages.size();
        int pagesObject = 2;
        int firstPageObject = 3;

        writer.header();
        writer.object(1, "<< /Type /Catalog /Pages " + pagesObject + " 0 R >>");

        StringBuilder kids = new StringBuilder();
        for (int index = 0; index < pageCount; index++)
        {
            if (index > 0) kids.append(' ');
            kids.append(firstPageObject + index * 3).append(" 0 R");
        }
        writer.object(pagesObject, "<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");

        for (int index = 0; index < pageCount; index++)
        {
            PageInfo info = mPages.get(index);
            int pageObject = firstPageObject + index * 3;
            int contentObject = pageObject + 1;
            int imageObject = pageObject + 2;
            int width = Math.max(1, info.getPageWidth());
            int height = Math.max(1, info.getPageHeight());

            writer.object(pageObject, "<< /Type /Page /Parent " + pagesObject + " 0 R /MediaBox [0 0 " + width + " " + height
                                          + "] /Resources << /XObject << /Page" + index + " " + imageObject
                                          + " 0 R >> >> /Contents " + contentObject + " 0 R >>");

            byte[] drawing = ("q " + width + " 0 0 " + height + " 0 0 cm /Page" + index + " Do Q").getBytes(StandardCharsets.US_ASCII);
            writer.stream(contentObject, "", drawing);

            byte[] pixels = deflate(rasterise(mPictures.get(index), width, height));
            writer.stream(imageObject, "/Type /XObject /Subtype /Image /Width " + width + " /Height " + height
                                           + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode", pixels);
        }

        writer.trailer(1);
        out.flush();
    }

    private static byte[] rasterise(Picture picture, int width, int height)
    {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xFFFFFFFF);
        picture.draw(new Canvas(bitmap));

        int[] row = new int[width];
        byte[] rgb = new byte[width * height * 3];
        int cursor = 0;

        for (int y = 0; y < height; y++)
        {
            bitmap.getPixels(row, 0, width, 0, y, width, 1);
            for (int x = 0; x < width; x++)
            {
                int color = row[x];
                int alpha = color >>> 24;
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;

                if (alpha != 0xFF)
                {
                    red = (red * alpha + 0xFF * (0xFF - alpha)) / 0xFF;
                    green = (green * alpha + 0xFF * (0xFF - alpha)) / 0xFF;
                    blue = (blue * alpha + 0xFF * (0xFF - alpha)) / 0xFF;
                }

                rgb[cursor++] = (byte)red;
                rgb[cursor++] = (byte)green;
                rgb[cursor++] = (byte)blue;
            }
        }

        bitmap.recycle();
        return rgb;
    }

    private static byte[] deflate(byte[] data)
    {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream compressed = new ByteArrayOutputStream(Math.max(64, data.length / 4));
        byte[] buffer = new byte[16384];

        while (!deflater.finished())
        {
            int got = deflater.deflate(buffer);
            compressed.write(buffer, 0, got);
        }

        deflater.end();
        return compressed.toByteArray();
    }

    public List<PageInfo> getPages()
    {
        return Collections.unmodifiableList(mPages);
    }

    public void close()
    {
        if (mClosed) return;

        mClosed = true;
        mPictures.clear();
        if (mCurrentPage != null) mCurrentPage.finish();
        mCurrentPage = null;
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

    private void throwIfClosed()
    {
        if (mClosed) throw new IllegalStateException("document is closed!");
    }

    private void throwIfCurrentPageNotFinished()
    {
        if (mCurrentPage != null) throw new IllegalStateException("Current page not finished!");
    }

    private static final class PdfWriter
    {
        private final OutputStream mOut;
        private final ArrayList<Long> mOffsets = new ArrayList<>();
        private long mWritten;

        PdfWriter(OutputStream out)
        {
            mOut = out;
        }

        void header() throws IOException
        {
            write("%PDF-1.4\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1));
        }

        void object(int number, String body) throws IOException
        {
            begin(number);
            write((body + "\nendobj\n").getBytes(StandardCharsets.US_ASCII));
        }

        void stream(int number, String dictionary, byte[] data) throws IOException
        {
            begin(number);
            write(("<< " + dictionary + " /Length " + data.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));
            write(data);
            write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }

        void trailer(int root) throws IOException
        {
            long crossReference = mWritten;
            StringBuilder table = new StringBuilder();
            table.append("xref\n0 ").append(mOffsets.size() + 1).append("\n0000000000 65535 f \n");

            for (Long offset : mOffsets) table.append(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offset));

            table.append("trailer\n<< /Size ").append(mOffsets.size() + 1).append(" /Root ").append(root).append(" 0 R >>\n");
            table.append("startxref\n").append(crossReference).append("\n%%EOF\n");
            write(table.toString().getBytes(StandardCharsets.US_ASCII));
        }

        private void begin(int number) throws IOException
        {
            while (mOffsets.size() < number) mOffsets.add(0L);
            mOffsets.set(number - 1, mWritten);
            write((number + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
        }

        private void write(byte[] data) throws IOException
        {
            mOut.write(data);
            mWritten += data.length;
        }
    }

    public static final class PageInfo
    {
        private int mPageWidth;
        private int mPageHeight;
        private Rect mContentRect;
        private int mPageNumber;

        PageInfo()
        {
        }

        public int getPageWidth()
        {
            return mPageWidth;
        }

        public int getPageHeight()
        {
            return mPageHeight;
        }

        public Rect getContentRect()
        {
            return mContentRect;
        }

        public int getPageNumber()
        {
            return mPageNumber;
        }

        public static final class Builder
        {
            private final PageInfo mPageInfo = new PageInfo();

            public Builder(int pageWidth, int pageHeight, int pageNumber)
            {
                if (pageWidth <= 0) throw new IllegalArgumentException("page width must be positive");
                if (pageHeight <= 0) throw new IllegalArgumentException("page width must be positive");
                if (pageNumber < 0) throw new IllegalArgumentException("pageNumber must be non negative");

                mPageInfo.mPageWidth = pageWidth;
                mPageInfo.mPageHeight = pageHeight;
                mPageInfo.mPageNumber = pageNumber;
            }

            public Builder setContentRect(Rect contentRect)
            {
                if (contentRect != null && (contentRect.left < 0 || contentRect.top < 0 || contentRect.right > mPageInfo.mPageWidth
                                            || contentRect.bottom > mPageInfo.mPageHeight))
                {
                    throw new IllegalArgumentException("contentRect does not fit the page");
                }

                mPageInfo.mContentRect = contentRect;
                return this;
            }

            public PageInfo create()
            {
                if (mPageInfo.mContentRect == null) mPageInfo.mContentRect = new Rect(0, 0, mPageInfo.mPageWidth, mPageInfo.mPageHeight);
                return mPageInfo;
            }
        }
    }

    public static final class Page
    {
        private final PageInfo mPageInfo;
        private final Picture mPicture;
        private Canvas mCanvas;

        Page()
        {
            this(null, null, null);
        }

        private Page(Canvas canvas, PageInfo pageInfo, Picture picture)
        {
            mCanvas = canvas;
            mPageInfo = pageInfo;
            mPicture = picture;
        }

        public Canvas getCanvas()
        {
            return mCanvas;
        }

        public PageInfo getInfo()
        {
            return mPageInfo;
        }

        boolean isFinished()
        {
            return mCanvas == null;
        }

        void finish()
        {
            if (mPicture != null) mPicture.endRecording();
            mCanvas = null;
        }
    }
}
