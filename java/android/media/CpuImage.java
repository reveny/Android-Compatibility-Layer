package android.media;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class CpuImage extends Image
{
    interface Owner
    {
        void onImageClosed(CpuImage image);
    }

    private final int mFormat;
    private final int mWidth;
    private final int mHeight;
    private final Owner mOwner;
    private CpuPlane[] mPlanes;
    private boolean mValid = true;

    CpuImage(int format, int width, int height, Owner owner)
    {
        mFormat = format;
        mWidth = width;
        mHeight = height;
        mOwner = owner;
        mPlanes = createPlanes(format, width, height);
    }

    static boolean isSupportedFormat(int format)
    {
        switch (format)
        {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
            case ImageFormat.Y8:
            case ImageFormat.Y16:
            case ImageFormat.DEPTH16:
            case ImageFormat.JPEG:
            case ImageFormat.RGB_565:
            case ImageFormat.PRIVATE:
            case PixelFormat.RGBA_8888:
            case PixelFormat.RGBX_8888:
            case PixelFormat.RGB_888:
            case PixelFormat.RGBA_F16:
            case PixelFormat.RGBA_1010102:
                return true;
            default:
                return false;
        }
    }

    private static CpuPlane[] createPlanes(int format, int width, int height)
    {
        int chromaWidth = (width + 1) / 2;
        int chromaHeight = (height + 1) / 2;

        switch (format)
        {
            case ImageFormat.YUV_420_888:
            case ImageFormat.YV12:
                return new CpuPlane[] {new CpuPlane(width, height, 1, width), new CpuPlane(chromaWidth, chromaHeight, 1, chromaWidth),
                    new CpuPlane(chromaWidth, chromaHeight, 1, chromaWidth)};
            case ImageFormat.NV21:
                return new CpuPlane[] {new CpuPlane(width, height, 1, width),
                    new CpuPlane(chromaWidth * 2, chromaHeight, 1, chromaWidth * 2)};
            case ImageFormat.Y8:
                return new CpuPlane[] {new CpuPlane(width, height, 1, width)};
            case ImageFormat.Y16:
            case ImageFormat.DEPTH16:
            case ImageFormat.RGB_565:
                return new CpuPlane[] {new CpuPlane(width, height, 2, width * 2)};
            case ImageFormat.JPEG:
                return new CpuPlane[] {new CpuPlane(width, Math.max(height, 1), 0, 0)};
            case PixelFormat.RGB_888:
                return new CpuPlane[] {new CpuPlane(width, height, 3, width * 3)};
            case PixelFormat.RGBA_F16:
                return new CpuPlane[] {new CpuPlane(width, height, 8, width * 8)};
            case PixelFormat.RGBA_8888:
            case PixelFormat.RGBX_8888:
            case PixelFormat.RGBA_1010102:
                return new CpuPlane[] {new CpuPlane(width, height, 4, width * 4)};
            default:
                return new CpuPlane[0];
        }
    }

    @Override
    public void close()
    {
        if (!mValid) return;

        mValid = false;
        mPlanes = null;

        if (mOwner != null) mOwner.onImageClosed(this);
    }

    @Override
    public int getFormat()
    {
        throwIfImageIsInvalid();
        return mFormat;
    }

    @Override
    public int getHeight()
    {
        throwIfImageIsInvalid();
        return mHeight;
    }

    @Override
    public int getWidth()
    {
        throwIfImageIsInvalid();
        return mWidth;
    }

    @Override
    public Image.Plane[] getPlanes()
    {
        throwIfImageIsInvalid();
        return mPlanes.clone();
    }

    @Override
    public long getTimestamp()
    {
        throwIfImageIsInvalid();
        return getStoredTimestamp();
    }

    @Override
    boolean isImageValid()
    {
        return mValid;
    }

    private static final class CpuPlane extends Image.Plane
    {
        private final ByteBuffer mBuffer;
        private final int mPixelStride;
        private final int mRowStride;

        CpuPlane(int width, int height, int pixelStride, int rowStride)
        {
            int size = rowStride == 0 ? width * height : rowStride * height;
            mBuffer = ByteBuffer.allocateDirect(Math.max(size, 0)).order(ByteOrder.nativeOrder());
            mPixelStride = pixelStride;
            mRowStride = rowStride;
        }

        @Override
        public ByteBuffer getBuffer()
        {
            return mBuffer;
        }

        @Override
        public int getPixelStride()
        {
            return mPixelStride;
        }

        @Override
        public int getRowStride()
        {
            return mRowStride;
        }
    }
}
