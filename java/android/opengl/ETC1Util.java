package android.opengl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ETC1Util
{
    public ETC1Util()
    {
    }

    public static void loadTexture(int target, int level, int border, int fallbackFormat, int fallbackType, InputStream input)
            throws IOException
    {
        loadTexture(target, level, border, fallbackFormat, fallbackType, createTexture(input));
    }

    public static void loadTexture(int target, int level, int border, int fallbackFormat, int fallbackType, ETC1Texture texture)
    {
        if (fallbackFormat != GLES10.GL_RGB) throw new IllegalArgumentException("fallbackFormat must be GL_RGB");
        if (!(fallbackType == GLES10.GL_UNSIGNED_SHORT_5_6_5 || fallbackType == GLES10.GL_UNSIGNED_BYTE))
        {
            throw new IllegalArgumentException("Unsupported fallbackType");
        }

        int width = texture.getWidth();
        int height = texture.getHeight();
        Buffer data = texture.getData();

        if (isETC1Supported())
        {
            int imageSize = data.remaining();
            GLES10.glCompressedTexImage2D(target, level, ETC1.ETC1_RGB8_OES, width, height, border, imageSize, data);
            return;
        }

        boolean useShorts = fallbackType != GLES10.GL_UNSIGNED_BYTE;
        int pixelSize = useShorts ? 2 : 3;
        int stride = pixelSize * width;
        ByteBuffer decoded = ByteBuffer.allocateDirect(stride * height).order(ByteOrder.nativeOrder());
        ETC1.decodeImage(data, decoded, width, height, pixelSize, stride);
        GLES10.glTexImage2D(target, level, fallbackFormat, width, height, border, fallbackFormat, fallbackType, decoded);
    }

    public static boolean isETC1Supported()
    {
        int[] results = new int[20];
        GLES10.glGetIntegerv(GLES10.GL_NUM_COMPRESSED_TEXTURE_FORMATS, results, 0);
        int formatCount = results[0];
        if (formatCount > results.length) results = new int[formatCount];

        GLES10.glGetIntegerv(GLES10.GL_COMPRESSED_TEXTURE_FORMATS, results, 0);
        for (int index = 0; index < formatCount; index++)
        {
            if (results[index] == ETC1.ETC1_RGB8_OES) return true;
        }
        return false;
    }

    public static ETC1Texture createTexture(InputStream input) throws IOException
    {
        byte[] ioBuffer = new byte[4096];
        if (readFully(input, ioBuffer, ETC1.ETC_PKM_HEADER_SIZE) != ETC1.ETC_PKM_HEADER_SIZE)
        {
            throw new IOException("Unable to read PKM file header.");
        }

        ByteBuffer headerBuffer = ByteBuffer.allocateDirect(ETC1.ETC_PKM_HEADER_SIZE).order(ByteOrder.nativeOrder());
        headerBuffer.put(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE).position(0);
        if (!ETC1.isValid(headerBuffer)) throw new IOException("Not a PKM file.");

        int width = ETC1.getWidth(headerBuffer);
        int height = ETC1.getHeight(headerBuffer);
        int encodedSize = ETC1.getEncodedDataSize(width, height);
        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(encodedSize).order(ByteOrder.nativeOrder());

        for (int index = 0; index < encodedSize; )
        {
            int chunkSize = Math.min(ioBuffer.length, encodedSize - index);
            if (readFully(input, ioBuffer, chunkSize) != chunkSize) throw new IOException("Unable to read PKM file data.");

            dataBuffer.put(ioBuffer, 0, chunkSize);
            index += chunkSize;
        }

        dataBuffer.position(0);
        return new ETC1Texture(width, height, dataBuffer);
    }

    public static ETC1Texture compressTexture(Buffer input, int width, int height, int pixelSize, int stride)
    {
        int encodedImageSize = ETC1.getEncodedDataSize(width, height);
        ByteBuffer compressedImage = ByteBuffer.allocateDirect(encodedImageSize).order(ByteOrder.nativeOrder());
        ETC1.encodeImage(input, width, height, pixelSize, stride, compressedImage);
        return new ETC1Texture(width, height, compressedImage);
    }

    public static void writeTexture(ETC1Texture texture, OutputStream output) throws IOException
    {
        ByteBuffer dataBuffer = texture.getData();
        int originalPosition = dataBuffer.position();

        try
        {
            int width = texture.getWidth();
            int height = texture.getHeight();
            ByteBuffer header = ByteBuffer.allocateDirect(ETC1.ETC_PKM_HEADER_SIZE).order(ByteOrder.nativeOrder());
            ETC1.formatHeader(header, width, height);

            byte[] ioBuffer = new byte[4096];
            header.get(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE);
            output.write(ioBuffer, 0, ETC1.ETC_PKM_HEADER_SIZE);

            int encodedSize = ETC1.getEncodedDataSize(width, height);
            for (int index = 0; index < encodedSize; )
            {
                int chunkSize = Math.min(ioBuffer.length, encodedSize - index);
                dataBuffer.get(ioBuffer, 0, chunkSize);
                output.write(ioBuffer, 0, chunkSize);
                index += chunkSize;
            }
        }
        finally
        {
            dataBuffer.position(originalPosition);
        }
    }

    private static int readFully(InputStream input, byte[] buffer, int length) throws IOException
    {
        int total = 0;
        while (total < length)
        {
            int read = input.read(buffer, total, length - total);
            if (read < 0) break;
            total += read;
        }
        return total;
    }

    public static class ETC1Texture
    {
        private final int mWidth;
        private final int mHeight;
        private final ByteBuffer mData;

        public ETC1Texture(int width, int height, ByteBuffer data)
        {
            mWidth = width;
            mHeight = height;
            mData = data;
        }

        public int getWidth()
        {
            return mWidth;
        }

        public int getHeight()
        {
            return mHeight;
        }

        public ByteBuffer getData()
        {
            return mData;
        }
    }
}
