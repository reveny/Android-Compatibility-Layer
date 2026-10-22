package android.graphics;

import java.io.OutputStream;

public class YuvImage
{
    private final byte[] mData;
    private final int mFormat;
    private final int[] mStrides;
    private final int mWidth;
    private final int mHeight;
    private final ColorSpace mColorSpace;

    public YuvImage(byte[] yuv, int format, int width, int height, int[] strides)
    {
        this(yuv, format, width, height, strides, ColorSpace.get(ColorSpace.Named.SRGB));
    }

    public YuvImage(byte[] yuv, int format, int width, int height, int[] strides, ColorSpace colorSpace)
    {
        if (format != ImageFormat.NV21 && format != ImageFormat.YUY2 && format != ImageFormat.YCBCR_P010
            && format != ImageFormat.YUV_420_888)
        {
            throw new IllegalArgumentException("only supports the following ImageFormat:NV21, YUY2, YCBCR_P010 and YUV_420_888");
        }

        if (width <= 0 || height <= 0) throw new IllegalArgumentException("width and height must large than 0");
        if (yuv == null) throw new IllegalArgumentException("yuv cannot be null");
        if (colorSpace == null) throw new IllegalArgumentException("ColorSpace cannot be null");

        mStrides = strides == null ? calculateStrides(width, format) : strides;
        mData = yuv;
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mColorSpace = colorSpace;
    }

    public boolean compressToJpeg(Rect rectangle, int quality, OutputStream stream)
    {
        if (mFormat != ImageFormat.NV21 && mFormat != ImageFormat.YUY2)
            throw new IllegalArgumentException("Only ImageFormat.NV21 and ImageFormat.YUY2 are supported.");

        Rect wholeImage = new Rect(0, 0, mWidth, mHeight);
        if (!wholeImage.contains(rectangle)) throw new IllegalArgumentException("rectangle is not inside the image");
        if (quality < 0 || quality > 100) throw new IllegalArgumentException("quality must be 0..100");
        if (stream == null) throw new IllegalArgumentException("stream cannot be null");

        int left = rectangle.left & ~1;
        int top = rectangle.top & ~1;
        int width = Math.max(1, rectangle.right - left);
        int height = Math.max(1, rectangle.bottom - top);

        int[] pixels = new int[width * height];

        for (int row = 0; row < height; row++)
        {
            int y = top + row;

            for (int column = 0; column < width; column++)
            {
                int x = left + column;
                int luma;
                int blue;
                int red;

                if (mFormat == ImageFormat.NV21)
                {
                    luma = mData[y * mStrides[0] + x] & 0xFF;
                    int chroma = mStrides[0] * mHeight + (y >> 1) * mStrides[1] + (x & ~1);
                    red = (mData[chroma] & 0xFF) - 128;
                    blue = (mData[chroma + 1] & 0xFF) - 128;
                }
                else
                {
                    int pair = y * mStrides[0] + (x & ~1) * 2;
                    luma = mData[pair + ((x & 1) == 0 ? 0 : 2)] & 0xFF;
                    blue = (mData[pair + 1] & 0xFF) - 128;
                    red = (mData[pair + 3] & 0xFF) - 128;
                }

                pixels[row * width + column] = toArgb(luma, blue, red);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);

        try
        {
            return bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        }
        finally
        {
            bitmap.recycle();
        }
    }

    public boolean compressToJpegR(YuvImage sdr, int quality, OutputStream stream)
    {
        if (sdr == null) throw new IllegalArgumentException("SDR input cannot be null");
        if (mData.length == 0 || sdr.getYuvData().length == 0) throw new IllegalArgumentException("Input images cannot be empty");
        if (mFormat != ImageFormat.YCBCR_P010 || sdr.getYuvFormat() != ImageFormat.YUV_420_888)
            throw new IllegalArgumentException("only support ImageFormat.YCBCR_P010 and ImageFormat.YUV_420_888");
        if (sdr.getWidth() != mWidth || sdr.getHeight() != mHeight) throw new IllegalArgumentException("HDR and SDR resolution mismatch");
        if (quality < 0 || quality > 100) throw new IllegalArgumentException("quality must be 0..100");
        if (stream == null) throw new IllegalArgumentException("stream cannot be null");

        int[] pixels = new int[mWidth * mHeight];
        byte[] data = sdr.mData;
        int lumaStride = sdr.mStrides[0];
        int chromaStride = sdr.mStrides.length > 1 ? sdr.mStrides[1] : mWidth >> 1;
        int lumaSize = lumaStride * mHeight;
        int chromaSize = chromaStride * ((mHeight + 1) >> 1);

        for (int y = 0; y < mHeight; y++)
        {
            for (int x = 0; x < mWidth; x++)
            {
                int luma = data[y * lumaStride + x] & 0xFF;
                int chroma = (y >> 1) * chromaStride + (x >> 1);
                int blue = lumaSize + chroma < data.length ? (data[lumaSize + chroma] & 0xFF) - 128 : 0;
                int red = lumaSize + chromaSize + chroma < data.length ? (data[lumaSize + chromaSize + chroma] & 0xFF) - 128 : 0;
                pixels[y * mWidth + x] = toArgb(luma, blue, red);
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(pixels, mWidth, mHeight, Bitmap.Config.ARGB_8888);

        try
        {
            return bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        }
        finally
        {
            bitmap.recycle();
        }
    }

    private static int toArgb(int luma, int blue, int red)
    {
        float y = luma;
        int r = clamp(Math.round(y + 1.402f * red));
        int g = clamp(Math.round(y - 0.344136f * blue - 0.714136f * red));
        int b = clamp(Math.round(y + 1.772f * blue));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value)
    {
        return value < 0 ? 0 : value > 255 ? 255 : value;
    }

    public byte[] getYuvData()
    {
        return mData;
    }

    public int getYuvFormat()
    {
        return mFormat;
    }

    public int[] getStrides()
    {
        return mStrides;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public ColorSpace getColorSpace()
    {
        return mColorSpace;
    }

    private static int[] calculateStrides(int width, int format)
    {
        if (format == ImageFormat.NV21) return new int[] {width, width};
        if (format == ImageFormat.YCBCR_P010) return new int[] {width * 2, width * 2};
        if (format == ImageFormat.YUV_420_888) return new int[] {width, (width + 1) / 2, (width + 1) / 2};

        return new int[] {width * 2};
    }
}
