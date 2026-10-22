package android.media;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaMetadataRetriever implements AutoCloseable
{
    public static final int METADATA_KEY_ALBUM = 1;
    public static final int METADATA_KEY_ALBUMARTIST = 13;
    public static final int METADATA_KEY_ARTIST = 2;
    public static final int METADATA_KEY_AUTHOR = 3;
    public static final int METADATA_KEY_BITRATE = 20;
    public static final int METADATA_KEY_BITS_PER_SAMPLE = 39;
    public static final int METADATA_KEY_CAPTURE_FRAMERATE = 25;
    public static final int METADATA_KEY_CD_TRACK_NUMBER = 0;
    public static final int METADATA_KEY_COLOR_RANGE = 37;
    public static final int METADATA_KEY_COLOR_STANDARD = 35;
    public static final int METADATA_KEY_COLOR_TRANSFER = 36;
    public static final int METADATA_KEY_COMPILATION = 15;
    public static final int METADATA_KEY_COMPOSER = 4;
    public static final int METADATA_KEY_DATE = 5;
    public static final int METADATA_KEY_DISC_NUMBER = 14;
    public static final int METADATA_KEY_DURATION = 9;
    public static final int METADATA_KEY_EXIF_LENGTH = 34;
    public static final int METADATA_KEY_EXIF_OFFSET = 33;
    public static final int METADATA_KEY_GENRE = 6;
    public static final int METADATA_KEY_HAS_AUDIO = 16;
    public static final int METADATA_KEY_HAS_IMAGE = 26;
    public static final int METADATA_KEY_HAS_VIDEO = 17;
    public static final int METADATA_KEY_IMAGE_COUNT = 27;
    public static final int METADATA_KEY_IMAGE_HEIGHT = 30;
    public static final int METADATA_KEY_IMAGE_PRIMARY = 28;
    public static final int METADATA_KEY_IMAGE_ROTATION = 31;
    public static final int METADATA_KEY_IMAGE_WIDTH = 29;
    public static final int METADATA_KEY_LOCATION = 23;
    public static final int METADATA_KEY_MIMETYPE = 12;
    public static final int METADATA_KEY_NUM_TRACKS = 10;
    public static final int METADATA_KEY_SAMPLERATE = 38;
    public static final int METADATA_KEY_TITLE = 7;
    public static final int METADATA_KEY_VIDEO_FRAME_COUNT = 32;
    public static final int METADATA_KEY_VIDEO_HEIGHT = 19;
    public static final int METADATA_KEY_VIDEO_ROTATION = 24;
    public static final int METADATA_KEY_VIDEO_WIDTH = 18;
    public static final int METADATA_KEY_WRITER = 11;
    public static final int METADATA_KEY_XMP_LENGTH = 42;
    public static final int METADATA_KEY_XMP_OFFSET = 41;
    public static final int METADATA_KEY_YEAR = 8;
    public static final int OPTION_CLOSEST = 3;
    public static final int OPTION_CLOSEST_SYNC = 2;
    public static final int OPTION_NEXT_SYNC = 1;
    public static final int OPTION_PREVIOUS_SYNC = 0;

    private static final int MAX_BUFFERED_BYTES = 64 * 1024 * 1024;

    private byte[] mData;
    private boolean mDataComplete;
    private final Map<Integer, String> mMetadata = new HashMap<>();

    public MediaMetadataRetriever()
    {
    }

    public void close() throws IOException
    {
        release();
    }

    public String extractMetadata(int keyCode)
    {
        return mMetadata.get(keyCode);
    }

    public byte[] getEmbeddedPicture()
    {
        return null;
    }

    public Bitmap getFrameAtIndex(int frameIndex)
    {
        return getFrameAtIndex(frameIndex, null);
    }

    public Bitmap getFrameAtIndex(int frameIndex, MediaMetadataRetriever.BitmapParams params)
    {
        List<Bitmap> frames = getFramesAtIndex(frameIndex, 1, params);

        return frames.get(0);
    }

    public Bitmap getFrameAtTime()
    {
        return getFrameAtTime(-1, OPTION_CLOSEST_SYNC);
    }

    public Bitmap getFrameAtTime(long timeUs)
    {
        return getFrameAtTime(timeUs, OPTION_CLOSEST_SYNC);
    }

    public Bitmap getFrameAtTime(long timeUs, int option)
    {
        return getFrameAtTime(timeUs, option, null);
    }

    public Bitmap getFrameAtTime(long timeUs, int option, MediaMetadataRetriever.BitmapParams params)
    {
        checkOption(option);
        return null;
    }

    public List<Bitmap> getFramesAtIndex(int frameIndex, int numFrames)
    {
        return getFramesAtIndex(frameIndex, numFrames, null);
    }

    public List<Bitmap> getFramesAtIndex(int frameIndex, int numFrames, MediaMetadataRetriever.BitmapParams params)
    {
        if (!"yes".equals(mMetadata.get(METADATA_KEY_HAS_VIDEO)))
        {
            throw new IllegalStateException("Does not contain video or image sequences");
        }

        String frameCount = mMetadata.get(METADATA_KEY_VIDEO_FRAME_COUNT);
        int count = frameCount == null ? 0 : Integer.parseInt(frameCount);

        if (frameIndex < 0 || numFrames < 1 || frameIndex >= count || frameIndex > count - numFrames)
        {
            throw new IllegalArgumentException("Invalid frameIndex or numFrames: " + frameIndex + ", " + numFrames);
        }

        return new ArrayList<>();
    }

    public Bitmap getImageAtIndex(int imageIndex)
    {
        return getImageAtIndex(imageIndex, null);
    }

    public Bitmap getImageAtIndex(int imageIndex, MediaMetadataRetriever.BitmapParams params)
    {
        if (!"yes".equals(mMetadata.get(METADATA_KEY_HAS_IMAGE))) throw new IllegalStateException("Does not contain still images");

        if (imageIndex != 0) throw new IllegalArgumentException("Invalid image index: " + imageIndex);

        return decodeStillImage(params);
    }

    public Bitmap getPrimaryImage()
    {
        return getPrimaryImage(null);
    }

    public Bitmap getPrimaryImage(MediaMetadataRetriever.BitmapParams params)
    {
        if (!"yes".equals(mMetadata.get(METADATA_KEY_HAS_IMAGE))) throw new IllegalStateException("Does not contain still images");

        return decodeStillImage(params);
    }

    public Bitmap getScaledFrameAtTime(long timeUs, int option, int dstWidth, int dstHeight)
    {
        return getScaledFrameAtTime(timeUs, option, dstWidth, dstHeight, null);
    }

    public Bitmap getScaledFrameAtTime(long timeUs, int option, int dstWidth, int dstHeight, MediaMetadataRetriever.BitmapParams params)
    {
        checkOption(option);

        if (dstWidth <= 0) throw new IllegalArgumentException("Invalid width: " + dstWidth);
        if (dstHeight <= 0) throw new IllegalArgumentException("Invalid height: " + dstHeight);

        return null;
    }

    public void release() throws IOException
    {
        mData = null;
        mDataComplete = false;
        mMetadata.clear();
    }

    public void setDataSource(Context context, Uri uri) throws IllegalArgumentException, SecurityException
    {
        if (uri == null) throw new IllegalArgumentException("null uri");

        String scheme = uri.getScheme();

        if (scheme == null || ContentResolver.SCHEME_FILE.equals(scheme))
        {
            setDataSource(uri.getPath());
            return;
        }

        if (context == null) throw new IllegalArgumentException("null context");

        try (InputStream stream = context.getContentResolver().openInputStream(uri))
        {
            if (stream == null) throw new IllegalArgumentException("Unable to open " + uri);

            load(stream);
        }
        catch (IOException failure)
        {
            throw new IllegalArgumentException(failure);
        }
    }

    public void setDataSource(MediaDataSource dataSource) throws IllegalArgumentException
    {
        if (dataSource == null) throw new IllegalArgumentException("null MediaDataSource");

        try
        {
            long size = dataSource.getSize();
            int length = (int)Math.min(size < 0 ? MAX_BUFFERED_BYTES : size, MAX_BUFFERED_BYTES);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] chunk = new byte[64 * 1024];
            long position = 0;

            while (position < length)
            {
                int read = dataSource.readAt(position, chunk, 0, (int)Math.min(chunk.length, length - position));

                if (read <= 0) break;

                output.write(chunk, 0, read);
                position += read;
            }

            accept(output.toByteArray(), size >= 0 && size <= MAX_BUFFERED_BYTES);
        }
        catch (IOException failure)
        {
            throw new IllegalArgumentException(failure);
        }
    }

    public void setDataSource(FileDescriptor fileDescriptor) throws IllegalArgumentException
    {
        setDataSource(fileDescriptor, 0, 0x7ffffffffffffffL);
    }

    public void setDataSource(FileDescriptor fileDescriptor, long offset, long length) throws IllegalArgumentException
    {
        if (fileDescriptor == null || !fileDescriptor.valid()) throw new IllegalArgumentException("Invalid file descriptor");
        if (offset < 0 || length < 0) throw new IllegalArgumentException("Invalid offset or length");

        try
        {
            FileChannel channel = new FileInputStream(fileDescriptor).getChannel();
            long available = Math.max(0, channel.size() - offset);
            long wanted = Math.min(available, length);
            int toRead = (int)Math.min(wanted, MAX_BUFFERED_BYTES);
            ByteBuffer buffer = ByteBuffer.allocate(toRead);
            long position = offset;

            while (buffer.hasRemaining())
            {
                int read = channel.read(buffer, position);

                if (read <= 0) break;

                position += read;
            }

            byte[] data = new byte[buffer.position()];
            buffer.flip();
            buffer.get(data);
            accept(data, wanted <= MAX_BUFFERED_BYTES);
        }
        catch (IOException failure)
        {
            throw new IllegalArgumentException(failure);
        }
    }

    public void setDataSource(String path) throws IllegalArgumentException
    {
        if (path == null) throw new IllegalArgumentException("null path");

        Uri uri = Uri.parse(path);
        String scheme = uri.getScheme();

        if (scheme != null && !ContentResolver.SCHEME_FILE.equals(scheme))
        {
            if ("http".equals(scheme) || "https".equals(scheme))
            {
                setDataSource(path, new HashMap<String, String>());
                return;
            }

            throw new IllegalArgumentException("Unsupported data source: " + path);
        }

        File file = new File(scheme == null ? path : uri.getPath());

        if (!file.isFile()) throw new IllegalArgumentException("File not found: " + path);

        try (RandomAccessFile input = new RandomAccessFile(file, "r"))
        {
            long size = input.length();
            byte[] data = new byte[(int)Math.min(size, MAX_BUFFERED_BYTES)];
            input.readFully(data);
            accept(data, size <= MAX_BUFFERED_BYTES);
        }
        catch (IOException failure)
        {
            throw new IllegalArgumentException(failure);
        }
    }

    public void setDataSource(String uri, Map<String, String> headers) throws IllegalArgumentException
    {
        if (uri == null) throw new IllegalArgumentException("null uri");

        try
        {
            java.net.URLConnection connection = new java.net.URL(uri).openConnection();

            if (headers != null)
            {
                for (Map.Entry<String, String> header : headers.entrySet())
                {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            try (InputStream stream = connection.getInputStream())
            {
                load(stream);
            }
        }
        catch (IOException | RuntimeException failure)
        {
            throw new IllegalArgumentException(failure);
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            release();
        }
        finally
        {
            super.finalize();
        }
    }

    private void load(InputStream stream) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] chunk = new byte[64 * 1024];
        boolean complete = true;

        while (true)
        {
            int read = stream.read(chunk);

            if (read < 0) break;

            output.write(chunk, 0, read);

            if (output.size() >= MAX_BUFFERED_BYTES)
            {
                complete = stream.read() < 0;
                break;
            }
        }

        accept(output.toByteArray(), complete);
    }

    private void accept(byte[] data, boolean complete)
    {
        mMetadata.clear();
        mData = data;
        mDataComplete = complete;

        String mimeType = sniffMimeType(data);

        if (mimeType == null) throw new IllegalArgumentException("Unrecognized media format");

        mMetadata.put(METADATA_KEY_MIMETYPE, mimeType);

        if (mimeType.startsWith("image/"))
        {
            describeImage(data);
        }
        else if (mimeType.equals("audio/x-wav"))
        {
            describeWave(data);
        }
        else if (mimeType.startsWith("audio/"))
        {
            mMetadata.put(METADATA_KEY_HAS_AUDIO, "yes");
        }
    }

    private void describeImage(byte[] data)
    {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return;

        mMetadata.put(METADATA_KEY_HAS_IMAGE, "yes");
        mMetadata.put(METADATA_KEY_IMAGE_COUNT, "1");
        mMetadata.put(METADATA_KEY_IMAGE_PRIMARY, "0");
        mMetadata.put(METADATA_KEY_IMAGE_WIDTH, String.valueOf(bounds.outWidth));
        mMetadata.put(METADATA_KEY_IMAGE_HEIGHT, String.valueOf(bounds.outHeight));

        int rotation = 0;

        try
        {
            rotation = new ExifInterface(new java.io.ByteArrayInputStream(data)).getRotationDegrees();
        }
        catch (IOException | RuntimeException ignored)
        {
            rotation = 0;
        }

        mMetadata.put(METADATA_KEY_IMAGE_ROTATION, String.valueOf(rotation));
    }

    private void describeWave(byte[] data)
    {
        int position = 12;
        int channels = 0;
        int sampleRate = 0;
        int byteRate = 0;
        int bitsPerSample = 0;
        long dataLength = -1;

        while (position + 8 <= data.length)
        {
            String id = new String(data, position, 4, java.nio.charset.StandardCharsets.US_ASCII);
            long chunkLength = readLittleInt(data, position + 4) & 0xffffffffL;
            int body = position + 8;

            if (id.equals("fmt ") && body + 16 <= data.length)
            {
                channels = readLittleShort(data, body + 2);
                sampleRate = readLittleInt(data, body + 4);
                byteRate = readLittleInt(data, body + 8);
                bitsPerSample = readLittleShort(data, body + 14);
            }
            else if (id.equals("data"))
            {
                dataLength = chunkLength;
                break;
            }

            position = (int)Math.min(Integer.MAX_VALUE, body + chunkLength + (chunkLength & 1));
        }

        mMetadata.put(METADATA_KEY_HAS_AUDIO, "yes");
        mMetadata.put(METADATA_KEY_NUM_TRACKS, "1");

        if (sampleRate > 0) mMetadata.put(METADATA_KEY_SAMPLERATE, String.valueOf(sampleRate));
        if (bitsPerSample > 0) mMetadata.put(METADATA_KEY_BITS_PER_SAMPLE, String.valueOf(bitsPerSample));
        if (byteRate > 0) mMetadata.put(METADATA_KEY_BITRATE, String.valueOf(byteRate * 8L));

        if (byteRate > 0 && dataLength >= 0)
        {
            mMetadata.put(METADATA_KEY_DURATION, String.valueOf(dataLength * 1000L / byteRate));
        }
        else if (channels > 0 && sampleRate > 0 && bitsPerSample > 0 && dataLength >= 0)
        {
            long frameBytes = (long)channels * bitsPerSample / 8;
            mMetadata.put(METADATA_KEY_DURATION, String.valueOf(dataLength * 1000L / (frameBytes * sampleRate)));
        }
    }

    private Bitmap decodeStillImage(MediaMetadataRetriever.BitmapParams params)
    {
        if (mData == null || !mDataComplete) return null;

        BitmapFactory.Options options = new BitmapFactory.Options();

        if (params != null && params.getPreferredConfig() != null) options.inPreferredConfig = params.getPreferredConfig();

        Bitmap bitmap = BitmapFactory.decodeByteArray(mData, 0, mData.length, options);

        if (bitmap != null && params != null) params.setActualConfig(bitmap.getConfig());

        return bitmap;
    }

    private static void checkOption(int option)
    {
        if (option < OPTION_PREVIOUS_SYNC || option > OPTION_CLOSEST) throw new IllegalArgumentException("Unsupported option: " + option);
    }

    private static int readLittleInt(byte[] data, int offset)
    {
        return (data[offset] & 0xff) | (data[offset + 1] & 0xff) << 8 | (data[offset + 2] & 0xff) << 16 | (data[offset + 3] & 0xff) << 24;
    }

    private static int readLittleShort(byte[] data, int offset)
    {
        return (data[offset] & 0xff) | (data[offset + 1] & 0xff) << 8;
    }

    private static boolean matches(byte[] data, int offset, String ascii)
    {
        if (data.length < offset + ascii.length()) return false;

        for (int index = 0; index < ascii.length(); index++)
        {
            if (data[offset + index] != (byte)ascii.charAt(index)) return false;
        }

        return true;
    }

    private static String sniffMimeType(byte[] data)
    {
        if (data == null || data.length < 4) return null;

        int first = data[0] & 0xff;
        int second = data[1] & 0xff;

        if (first == 0xff && second == 0xd8) return "image/jpeg";
        if (first == 0x89 && matches(data, 1, "PNG")) return "image/png";
        if (matches(data, 0, "GIF8")) return "image/gif";
        if (matches(data, 0, "BM")) return "image/bmp";
        if (matches(data, 0, "RIFF") && matches(data, 8, "WEBP")) return "image/webp";
        if (matches(data, 0, "RIFF") && matches(data, 8, "WAVE")) return "audio/x-wav";
        if (matches(data, 0, "RIFF") && matches(data, 8, "AVI ")) return "video/avi";
        if (matches(data, 4, "ftyp"))
        {
            if (matches(data, 8, "heic") || matches(data, 8, "heix") || matches(data, 8, "mif1") || matches(data, 8, "msf1"))
            {
                return "image/heif";
            }

            if (matches(data, 8, "avif")) return "image/avif";
            if (matches(data, 8, "M4A ")) return "audio/mp4";
            if (matches(data, 8, "3gp")) return "video/3gpp";

            return "video/mp4";
        }
        if (matches(data, 0, "OggS")) return "audio/ogg";
        if (matches(data, 0, "fLaC")) return "audio/flac";
        if (matches(data, 0, "ID3")) return "audio/mpeg";
        if (matches(data, 0, "#!AMR")) return "audio/amr";
        if (first == 0x1a && second == 0x45 && (data[2] & 0xff) == 0xdf && (data[3] & 0xff) == 0xa3) return "video/webm";
        if (first == 0xff && (second & 0xf6) == 0xf0) return "audio/mp4a-latm";
        if (first == 0xff && (second & 0xe0) == 0xe0) return "audio/mpeg";
        if (first == 0x47 && data.length > 188 && (data[188] & 0xff) == 0x47) return "video/mp2ts";

        return null;
    }

    public static final class BitmapParams
    {
        private Bitmap.Config mPreferredConfig = Bitmap.Config.ARGB_8888;
        private Bitmap.Config mActualConfig = Bitmap.Config.ARGB_8888;

        public BitmapParams()
        {
        }

        public Bitmap.Config getActualConfig()
        {
            return mActualConfig;
        }

        public Bitmap.Config getPreferredConfig()
        {
            return mPreferredConfig;
        }

        public void setPreferredConfig(Bitmap.Config config)
        {
            if (config == null) throw new IllegalArgumentException("preferred config can't be null");

            mPreferredConfig = config;
        }

        void setActualConfig(Bitmap.Config config)
        {
            mActualConfig = config;
        }
    }
}
