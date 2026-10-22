package android.graphics;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Size;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

public final class ImageDecoder implements AutoCloseable
{
    public static final int ALLOCATOR_DEFAULT = 0;
    public static final int ALLOCATOR_SOFTWARE = 1;
    public static final int ALLOCATOR_SHARED_MEMORY = 2;
    public static final int ALLOCATOR_HARDWARE = 3;

    public static final int MEMORY_POLICY_LOW_RAM = 0;
    public static final int MEMORY_POLICY_DEFAULT = 1;

    private final Source mSource;
    private final byte[] mData;
    private final ImageInfo mInfo;

    private int mTargetWidth;
    private int mTargetHeight;
    private int mSampleSize = 1;
    private int mAllocator = ALLOCATOR_DEFAULT;
    private boolean mUnpremultipliedRequired;
    private boolean mMutableRequired;
    private boolean mDecodeAsAlphaMask;
    private int mMemoryPolicy = MEMORY_POLICY_DEFAULT;
    private PostProcessor mPostProcessor;
    private OnPartialImageListener mPartialImageListener;
    private Rect mCrop;
    private ColorSpace mTargetColorSpace;
    private boolean mClosed;

    private ImageDecoder(Source source, byte[] data, ImageInfo info)
    {
        mSource = source;
        mData = data;
        mInfo = info;
        mTargetWidth = info.mWidth;
        mTargetHeight = info.mHeight;
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

    public static boolean isMimeTypeSupported(String mimeType)
    {
        if (mimeType == null) throw new NullPointerException("mimeType");

        switch (mimeType.toLowerCase(java.util.Locale.US))
        {
            case "image/png":
            case "image/jpeg":
            case "image/webp":
            case "image/gif":
            case "image/bmp":
            case "image/x-ico":
            case "image/vnd.wap.wbmp":
                return true;

            default:
                return false;
        }
    }

    public static Source createSource(Resources resources, int resourceId)
    {
        return new ResourceSource(resources, resourceId);
    }

    public static Source createSource(ContentResolver resolver, Uri uri)
    {
        return new ContentResolverSource(resolver, uri);
    }

    public static Source createSource(AssetManager assets, String fileName)
    {
        return new AssetSource(assets, fileName);
    }

    public static Source createSource(byte[] data, int offset, int length) throws ArrayIndexOutOfBoundsException
    {
        if (data == null) throw new NullPointerException("null byte[] in createSource!");
        if (offset < 0 || length < 0 || offset >= data.length || offset + length > data.length)
        {
            throw new ArrayIndexOutOfBoundsException("invalid offset/length!");
        }

        return new ByteArraySource(data, offset, length);
    }

    public static Source createSource(byte[] data)
    {
        return createSource(data, 0, data.length);
    }

    public static Source createSource(ByteBuffer buffer)
    {
        return new ByteBufferSource(buffer);
    }

    public static Source createSource(File file)
    {
        return new FileSource(file);
    }

    public static Source createSource(Callable<AssetFileDescriptor> callable)
    {
        return new CallableSource(callable);
    }

    public void setTargetSize(int width, int height)
    {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Dimensions must be positive! provided " + width + "x" + height);

        mTargetWidth = width;
        mTargetHeight = height;
    }

    public void setTargetSampleSize(int sampleSize)
    {
        if (sampleSize <= 0) throw new IllegalArgumentException("sampleSize must be positive! provided " + sampleSize);

        mSampleSize = sampleSize;
        mTargetWidth = Math.max(1, (mInfo.mWidth + sampleSize - 1) / sampleSize);
        mTargetHeight = Math.max(1, (mInfo.mHeight + sampleSize - 1) / sampleSize);
    }

    public void setAllocator(int allocator)
    {
        if (allocator < ALLOCATOR_DEFAULT || allocator > ALLOCATOR_HARDWARE)
        {
            throw new IllegalArgumentException("invalid allocator " + allocator);
        }

        mAllocator = allocator;
    }

    public int getAllocator()
    {
        return mAllocator;
    }

    public void setUnpremultipliedRequired(boolean unpremultipliedRequired)
    {
        mUnpremultipliedRequired = unpremultipliedRequired;
    }

    public boolean isUnpremultipliedRequired()
    {
        return mUnpremultipliedRequired;
    }

    public void setPostProcessor(PostProcessor postProcessor)
    {
        mPostProcessor = postProcessor;
    }

    public PostProcessor getPostProcessor()
    {
        return mPostProcessor;
    }

    public void setOnPartialImageListener(OnPartialImageListener listener)
    {
        mPartialImageListener = listener;
    }

    public OnPartialImageListener getOnPartialImageListener()
    {
        return mPartialImageListener;
    }

    public void setCrop(Rect subset)
    {
        mCrop = subset == null ? null : new Rect(subset);
    }

    public Rect getCrop()
    {
        return mCrop;
    }

    public void setMutableRequired(boolean mutable)
    {
        mMutableRequired = mutable;
    }

    public boolean isMutableRequired()
    {
        return mMutableRequired;
    }

    public void setMemorySizePolicy(int policy)
    {
        mMemoryPolicy = policy;
    }

    public int getMemorySizePolicy()
    {
        return mMemoryPolicy;
    }

    public void setDecodeAsAlphaMaskEnabled(boolean enabled)
    {
        mDecodeAsAlphaMask = enabled;
    }

    public boolean isDecodeAsAlphaMaskEnabled()
    {
        return mDecodeAsAlphaMask;
    }

    public void setTargetColorSpace(ColorSpace colorSpace)
    {
        mTargetColorSpace = colorSpace;
    }

    @Override
    public void close()
    {
        mClosed = true;
    }

    public static Drawable decodeDrawable(Source source, OnHeaderDecodedListener listener) throws IOException
    {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null! Use decodeDrawable(Source)");

        return decodeDrawableImpl(source, listener);
    }

    public static Drawable decodeDrawable(Source source) throws IOException
    {
        return decodeDrawableImpl(source, null);
    }

    public static Bitmap decodeBitmap(Source source, OnHeaderDecodedListener listener) throws IOException
    {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null! Use decodeBitmap(Source)");

        return decodeBitmapImpl(source, listener);
    }

    public static Bitmap decodeBitmap(Source source) throws IOException
    {
        return decodeBitmapImpl(source, null);
    }

    private static Drawable decodeDrawableImpl(Source source, OnHeaderDecodedListener listener) throws IOException
    {
        ImageDecoder decoder = open(source, listener);

        try
        {
            Bitmap bitmap = decoder.decode();
            Resources resources = source.resources();

            if (decoder.mInfo.isAnimated()) return new AnimatedImageDrawable(resources, bitmap);

            return new BitmapDrawable(resources, bitmap);
        }
        finally
        {
            decoder.close();
        }
    }

    private static Bitmap decodeBitmapImpl(Source source, OnHeaderDecodedListener listener) throws IOException
    {
        ImageDecoder decoder = open(source, listener);

        try
        {
            return decoder.decode();
        }
        finally
        {
            decoder.close();
        }
    }

    private static ImageDecoder open(Source source, OnHeaderDecodedListener listener) throws IOException
    {
        if (source == null) throw new NullPointerException("source");

        byte[] data = source.read();
        if (data == null || data.length == 0)
        {
            throw new DecodeException(DecodeException.SOURCE_INCOMPLETE, "empty image source", source);
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0)
        {
            throw new DecodeException(DecodeException.SOURCE_MALFORMED_DATA, "Failed to create image decoder: input contained an error",
                                      source);
        }

        String mimeType = mimeTypeOf(data);
        ImageInfo info = new ImageInfo(bounds.outWidth, bounds.outHeight, mimeType, isAnimated(data, mimeType));
        ImageDecoder decoder = new ImageDecoder(source, data, info);

        if (listener != null) listener.onHeaderDecoded(decoder, info, source);

        return decoder;
    }

    private Bitmap decode() throws IOException
    {
        if (mClosed) throw new IllegalStateException("Cannot use closed ImageDecoder!");

        Bitmap decoded = BitmapFactory.decodeByteArray(mData, 0, mData.length);

        if (decoded == null)
        {
            DecodeException failure = new DecodeException(DecodeException.SOURCE_MALFORMED_DATA, "Failed to decode image", mSource);
            if (mPartialImageListener != null) mPartialImageListener.onPartialImage(failure);

            throw failure;
        }

        Bitmap result = decoded;

        if (mTargetWidth != decoded.getWidth() || mTargetHeight != decoded.getHeight())
        {
            result = Bitmap.createScaledBitmap(decoded, mTargetWidth, mTargetHeight, true);
        }

        if (mCrop != null)
        {
            Rect crop = new Rect(mCrop);

            if (crop.left < 0 || crop.top < 0 || crop.right > mTargetWidth || crop.bottom > mTargetHeight || crop.isEmpty())
            {
                throw new IllegalStateException("Subset " + crop + " must be contained within the scaled image dimensions "
                                                + mTargetWidth + "x" + mTargetHeight);
            }

            Bitmap cropped = Bitmap.createBitmap(crop.width(), crop.height(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(cropped);
            canvas.drawBitmap(result, crop, new Rect(0, 0, crop.width(), crop.height()), null);
            result = cropped;
        }

        if (!result.isMutable() && (mMutableRequired || mPostProcessor != null))
        {
            result = result.copy(result.getConfig(), true);
        }

        if (mPostProcessor != null)
        {
            Canvas canvas = new Canvas(result);
            mPostProcessor.onPostProcess(canvas);
        }

        return result;
    }

    private static String mimeTypeOf(byte[] data)
    {
        int length = data.length;

        if (length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') return "image/png";
        if (length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) return "image/jpeg";
        if (length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F' && data[8] == 'W') return "image/webp";
        if (length >= 6 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F') return "image/gif";
        if (length >= 2 && data[0] == 'B' && data[1] == 'M') return "image/bmp";
        if (length >= 12 && data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p') return "image/heif";

        return "";
    }

    private static boolean isAnimated(byte[] data, String mimeType)
    {
        if ("image/gif".equals(mimeType)) return countGifFrames(data) > 1;
        if ("image/webp".equals(mimeType)) return contains(data, new byte[] {'A', 'N', 'I', 'M'});
        if ("image/png".equals(mimeType)) return contains(data, new byte[] {'a', 'c', 'T', 'L'});

        return false;
    }

    private static int countGifFrames(byte[] data)
    {
        int frames = 0;

        for (int index = 0; index + 2 < data.length; index++)
        {
            if ((data[index] & 0xFF) == 0x21 && (data[index + 1] & 0xFF) == 0xF9 && (data[index + 2] & 0xFF) == 0x04)
            {
                frames++;
                if (frames > 1) return frames;
            }
        }

        return frames;
    }

    private static boolean contains(byte[] data, byte[] pattern)
    {
        int limit = Math.min(data.length, 4096) - pattern.length;

        for (int index = 0; index <= limit; index++)
        {
            boolean matched = true;

            for (int offset = 0; offset < pattern.length && matched; offset++)
            {
                if (data[index + offset] != pattern[offset]) matched = false;
            }

            if (matched) return true;
        }

        return false;
    }

    private static byte[] readFully(InputStream stream) throws IOException
    {
        if (stream == null) return null;

        try
        {
            ByteArrayOutputStream collected = new ByteArrayOutputStream();
            byte[] chunk = new byte[16384];

            int read;
            while ((read = stream.read(chunk)) > 0) collected.write(chunk, 0, read);

            return collected.toByteArray();
        }
        finally
        {
            stream.close();
        }
    }

    public abstract static class Source
    {
        Source()
        {
        }

        abstract byte[] read() throws IOException;

        Resources resources()
        {
            return null;
        }
    }

    private static final class ByteArraySource extends Source
    {
        private final byte[] mData;
        private final int mOffset;
        private final int mLength;

        ByteArraySource(byte[] data, int offset, int length)
        {
            mData = data;
            mOffset = offset;
            mLength = length;
        }

        @Override
        byte[] read()
        {
            if (mOffset == 0 && mLength == mData.length) return mData;

            byte[] copy = new byte[mLength];
            System.arraycopy(mData, mOffset, copy, 0, mLength);
            return copy;
        }
    }

    private static final class ByteBufferSource extends Source
    {
        private final ByteBuffer mBuffer;

        ByteBufferSource(ByteBuffer buffer)
        {
            mBuffer = buffer;
        }

        @Override
        byte[] read()
        {
            ByteBuffer view = mBuffer.slice();
            byte[] data = new byte[view.remaining()];
            view.get(data);
            return data;
        }
    }

    private static final class FileSource extends Source
    {
        private final File mFile;

        FileSource(File file)
        {
            mFile = file;
        }

        @Override
        byte[] read() throws IOException
        {
            return readFully(new FileInputStream(mFile));
        }
    }

    private static final class AssetSource extends Source
    {
        private final AssetManager mAssets;
        private final String mFileName;

        AssetSource(AssetManager assets, String fileName)
        {
            mAssets = assets;
            mFileName = fileName;
        }

        @Override
        byte[] read() throws IOException
        {
            return readFully(mAssets.open(mFileName));
        }
    }

    private static final class ResourceSource extends Source
    {
        private final Resources mResources;
        private final int mResourceId;

        ResourceSource(Resources resources, int resourceId)
        {
            mResources = resources;
            mResourceId = resourceId;
        }

        @Override
        byte[] read() throws IOException
        {
            return readFully(mResources.openRawResource(mResourceId));
        }

        @Override
        Resources resources()
        {
            return mResources;
        }
    }

    private static final class ContentResolverSource extends Source
    {
        private final ContentResolver mResolver;
        private final Uri mUri;

        ContentResolverSource(ContentResolver resolver, Uri uri)
        {
            mResolver = resolver;
            mUri = uri;
        }

        @Override
        byte[] read() throws IOException
        {
            InputStream stream = mResolver.openInputStream(mUri);
            if (stream == null) throw new java.io.FileNotFoundException(mUri.toString());

            return readFully(stream);
        }
    }

    private static final class CallableSource extends Source
    {
        private final Callable<AssetFileDescriptor> mCallable;

        CallableSource(Callable<AssetFileDescriptor> callable)
        {
            mCallable = callable;
        }

        @Override
        byte[] read() throws IOException
        {
            AssetFileDescriptor descriptor;

            try
            {
                descriptor = mCallable.call();
            }
            catch (IOException failure)
            {
                throw failure;
            }
            catch (Exception failure)
            {
                throw new IOException(failure);
            }

            if (descriptor == null) throw new java.io.FileNotFoundException("null AssetFileDescriptor");

            return readFully(descriptor.createInputStream());
        }
    }

    public static class ImageInfo
    {
        private final int mWidth;
        private final int mHeight;
        private final String mMimeType;
        private final boolean mAnimated;

        ImageInfo(int width, int height, String mimeType, boolean animated)
        {
            mWidth = width;
            mHeight = height;
            mMimeType = mimeType;
            mAnimated = animated;
        }

        public Size getSize()
        {
            return new Size(mWidth, mHeight);
        }

        public String getMimeType()
        {
            return mMimeType;
        }

        public boolean isAnimated()
        {
            return mAnimated;
        }

        public ColorSpace getColorSpace()
        {
            return ColorSpace.get(ColorSpace.Named.SRGB);
        }
    }

    public interface OnHeaderDecodedListener
    {
        void onHeaderDecoded(ImageDecoder decoder, ImageInfo info, Source source);
    }

    public interface OnPartialImageListener
    {
        boolean onPartialImage(DecodeException exception);
    }

    public static final class DecodeException extends IOException
    {
        public static final int SOURCE_EXCEPTION = 1;
        public static final int SOURCE_INCOMPLETE = 2;
        public static final int SOURCE_MALFORMED_DATA = 3;

        private final int mError;
        private final transient Source mSource;

        DecodeException(int error, String message, Source source)
        {
            super(message);
            mError = error;
            mSource = source;
        }

        DecodeException(int error, String message, Throwable cause, Source source)
        {
            super(message, cause);
            mError = error;
            mSource = source;
        }

        public int getError()
        {
            return mError;
        }

        public Source getSource()
        {
            return mSource;
        }
    }
}
