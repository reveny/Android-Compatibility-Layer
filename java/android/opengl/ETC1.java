package android.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class ETC1
{
    public static final int DECODED_BLOCK_SIZE = 48;
    public static final int ENCODED_BLOCK_SIZE = 8;
    public static final int ETC1_RGB8_OES = 0x8D64;
    public static final int ETC_PKM_HEADER_SIZE = 16;

    private static final int[][] MODIFIERS = {
        {2, 8}, {5, 17}, {9, 29}, {13, 42}, {18, 60}, {24, 80}, {33, 106}, {47, 183}
    };

    private static final byte[] MAGIC = {'P', 'K', 'M', ' ', '1', '0'};
    private static final int ETC1_RGB_NO_MIPMAPS = 0;
    private static final int ENCODED_WIDTH_OFFSET = 8;
    private static final int ENCODED_HEIGHT_OFFSET = 10;
    private static final int WIDTH_OFFSET = 12;
    private static final int HEIGHT_OFFSET = 14;

    public ETC1()
    {
    }

    public static void encodeBlock(Buffer in, int validPixelMask, Buffer out)
    {
        ByteBuffer input = bytes(in, DECODED_BLOCK_SIZE, "in");
        ByteBuffer output = bytes(out, ENCODED_BLOCK_SIZE, "out");

        byte[] pixels = new byte[DECODED_BLOCK_SIZE];
        get(input, pixels);
        byte[] encoded = new byte[ENCODED_BLOCK_SIZE];
        encode(pixels, validPixelMask, encoded);
        put(output, encoded);
    }

    public static void decodeBlock(Buffer in, Buffer out)
    {
        ByteBuffer input = bytes(in, ENCODED_BLOCK_SIZE, "in");
        ByteBuffer output = bytes(out, DECODED_BLOCK_SIZE, "out");

        byte[] encoded = new byte[ENCODED_BLOCK_SIZE];
        get(input, encoded);
        byte[] pixels = new byte[DECODED_BLOCK_SIZE];
        decode(encoded, pixels);
        put(output, pixels);
    }

    public static int getEncodedDataSize(int width, int height)
    {
        return (((width + 3) & ~3) * ((height + 3) & ~3)) >> 1;
    }

    public static void encodeImage(Buffer in, int width, int height, int pixelSize, int stride, Buffer out)
    {
        checkPixelSize(pixelSize);
        ByteBuffer input = bytes(in, stride * (height - 1) + width * pixelSize, "in");
        ByteBuffer output = bytes(out, getEncodedDataSize(width, height), "out");
        int inputBase = input.position();
        int outputPosition = output.position();

        byte[] block = new byte[DECODED_BLOCK_SIZE];
        byte[] encoded = new byte[ENCODED_BLOCK_SIZE];
        for (int blockY = 0; blockY < height; blockY += 4)
        {
            int rows = Math.min(4, height - blockY);
            for (int blockX = 0; blockX < width; blockX += 4)
            {
                int columns = Math.min(4, width - blockX);
                int mask = 0;
                java.util.Arrays.fill(block, (byte) 0);

                for (int y = 0; y < rows; y++)
                {
                    int row = inputBase + (blockY + y) * stride + blockX * pixelSize;
                    for (int x = 0; x < columns; x++)
                    {
                        int source = row + x * pixelSize;
                        int target = (y * 4 + x) * 3;
                        if (pixelSize == 3)
                        {
                            block[target] = input.get(source);
                            block[target + 1] = input.get(source + 1);
                            block[target + 2] = input.get(source + 2);
                        }
                        else
                        {
                            int pixel = ((input.get(source + 1) & 0xFF) << 8) | (input.get(source) & 0xFF);
                            int red = (pixel >> 11) & 0x1F;
                            int green = (pixel >> 5) & 0x3F;
                            int blue = pixel & 0x1F;
                            block[target] = (byte) ((red << 3) | (red >> 2));
                            block[target + 1] = (byte) ((green << 2) | (green >> 4));
                            block[target + 2] = (byte) ((blue << 3) | (blue >> 2));
                        }
                        mask |= 1 << (y * 4 + x);
                    }
                }

                encode(block, mask, encoded);
                for (int index = 0; index < ENCODED_BLOCK_SIZE; index++) output.put(outputPosition++, encoded[index]);
            }
        }
    }

    public static void decodeImage(Buffer in, Buffer out, int width, int height, int pixelSize, int stride)
    {
        checkPixelSize(pixelSize);
        ByteBuffer input = bytes(in, getEncodedDataSize(width, height), "in");
        ByteBuffer output = bytes(out, stride * (height - 1) + width * pixelSize, "out");
        int inputPosition = input.position();
        int outputBase = output.position();

        byte[] encoded = new byte[ENCODED_BLOCK_SIZE];
        byte[] block = new byte[DECODED_BLOCK_SIZE];
        for (int blockY = 0; blockY < height; blockY += 4)
        {
            int rows = Math.min(4, height - blockY);
            for (int blockX = 0; blockX < width; blockX += 4)
            {
                int columns = Math.min(4, width - blockX);
                for (int index = 0; index < ENCODED_BLOCK_SIZE; index++) encoded[index] = input.get(inputPosition++);
                decode(encoded, block);

                for (int y = 0; y < rows; y++)
                {
                    int row = outputBase + (blockY + y) * stride + blockX * pixelSize;
                    for (int x = 0; x < columns; x++)
                    {
                        int source = (y * 4 + x) * 3;
                        int target = row + x * pixelSize;
                        if (pixelSize == 3)
                        {
                            output.put(target, block[source]);
                            output.put(target + 1, block[source + 1]);
                            output.put(target + 2, block[source + 2]);
                        }
                        else
                        {
                            int red = block[source] & 0xFF;
                            int green = block[source + 1] & 0xFF;
                            int blue = block[source + 2] & 0xFF;
                            int pixel = ((red >> 3) << 11) | ((green >> 2) << 5) | (blue >> 3);
                            output.put(target, (byte) pixel);
                            output.put(target + 1, (byte) (pixel >> 8));
                        }
                    }
                }
            }
        }
    }

    public static void formatHeader(Buffer header, int width, int height)
    {
        ByteBuffer output = bytes(header, ETC_PKM_HEADER_SIZE, "header");
        int base = output.position();

        for (int index = 0; index < MAGIC.length; index++) output.put(base + index, MAGIC[index]);
        writeShort(output, base + 6, ETC1_RGB_NO_MIPMAPS);
        writeShort(output, base + ENCODED_WIDTH_OFFSET, (width + 3) & ~3);
        writeShort(output, base + ENCODED_HEIGHT_OFFSET, (height + 3) & ~3);
        writeShort(output, base + WIDTH_OFFSET, width);
        writeShort(output, base + HEIGHT_OFFSET, height);
    }

    public static boolean isValid(Buffer header)
    {
        ByteBuffer input = bytes(header, ETC_PKM_HEADER_SIZE, "header");
        int base = input.position();

        for (int index = 0; index < MAGIC.length; index++)
        {
            if (input.get(base + index) != MAGIC[index]) return false;
        }

        int format = readShort(input, base + 6);
        int encodedWidth = readShort(input, base + ENCODED_WIDTH_OFFSET);
        int encodedHeight = readShort(input, base + ENCODED_HEIGHT_OFFSET);
        int width = readShort(input, base + WIDTH_OFFSET);
        int height = readShort(input, base + HEIGHT_OFFSET);

        return format == ETC1_RGB_NO_MIPMAPS && encodedWidth >= width && encodedWidth - width < 4 && encodedHeight >= height
                && encodedHeight - height < 4;
    }

    public static int getWidth(Buffer header)
    {
        ByteBuffer input = bytes(header, ETC_PKM_HEADER_SIZE, "header");
        return readShort(input, input.position() + WIDTH_OFFSET);
    }

    public static int getHeight(Buffer header)
    {
        ByteBuffer input = bytes(header, ETC_PKM_HEADER_SIZE, "header");
        return readShort(input, input.position() + HEIGHT_OFFSET);
    }

    private static void checkPixelSize(int pixelSize)
    {
        if (pixelSize != 2 && pixelSize != 3) throw new IllegalArgumentException("pixelSize must be 2 or 3");
    }

    private static ByteBuffer bytes(Buffer buffer, int needed, String name)
    {
        if (buffer == null) throw new IllegalArgumentException(name + " == null");
        if (!(buffer instanceof ByteBuffer)) throw new IllegalArgumentException(name + " must be a ByteBuffer");
        if (buffer.remaining() < needed) throw new IllegalArgumentException(name + ".remaining() < " + needed);
        return (ByteBuffer) buffer;
    }

    private static void get(ByteBuffer buffer, byte[] into)
    {
        int position = buffer.position();
        for (int index = 0; index < into.length; index++) into[index] = buffer.get(position + index);
    }

    private static void put(ByteBuffer buffer, byte[] from)
    {
        int position = buffer.position();
        for (int index = 0; index < from.length; index++) buffer.put(position + index, from[index]);
    }

    private static int readShort(ByteBuffer buffer, int offset)
    {
        return ((buffer.get(offset) & 0xFF) << 8) | (buffer.get(offset + 1) & 0xFF);
    }

    private static void writeShort(ByteBuffer buffer, int offset, int value)
    {
        buffer.put(offset, (byte) (value >> 8));
        buffer.put(offset + 1, (byte) value);
    }

    private static int clamp(int value)
    {
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }

    private static boolean inFirstSubblock(int x, int y, boolean flip)
    {
        return flip ? y < 2 : x < 2;
    }

    private static void decode(byte[] encoded, byte[] pixels)
    {
        long high = ((encoded[0] & 0xFFL) << 24) | ((encoded[1] & 0xFFL) << 16) | ((encoded[2] & 0xFFL) << 8) | (encoded[3] & 0xFFL);
        long low = ((encoded[4] & 0xFFL) << 24) | ((encoded[5] & 0xFFL) << 16) | ((encoded[6] & 0xFFL) << 8) | (encoded[7] & 0xFFL);

        boolean differential = (high & 2) != 0;
        boolean flip = (high & 1) != 0;
        int[] first = new int[3];
        int[] second = new int[3];

        for (int channel = 0; channel < 3; channel++)
        {
            int shift = 24 - channel * 8;
            if (differential)
            {
                int base = (int) ((high >> (shift + 3)) & 0x1F);
                int delta = (int) ((high >> shift) & 0x7);
                if (delta >= 4) delta -= 8;
                int other = (base + delta) & 0x1F;
                first[channel] = (base << 3) | (base >> 2);
                second[channel] = (other << 3) | (other >> 2);
            }
            else
            {
                int base = (int) ((high >> (shift + 4)) & 0xF);
                int other = (int) ((high >> shift) & 0xF);
                first[channel] = (base << 4) | base;
                second[channel] = (other << 4) | other;
            }
        }

        int firstTable = (int) ((high >> 5) & 0x7);
        int secondTable = (int) ((high >> 2) & 0x7);

        for (int y = 0; y < 4; y++)
        {
            for (int x = 0; x < 4; x++)
            {
                boolean inFirst = inFirstSubblock(x, y, flip);
                int[] base = inFirst ? first : second;
                int table = inFirst ? firstTable : secondTable;
                int bit = x * 4 + y;
                int index = (int) ((((low >> (bit + 16)) & 1) << 1) | ((low >> bit) & 1));
                int modifier = modifier(table, index);
                int target = (y * 4 + x) * 3;
                pixels[target] = (byte) clamp(base[0] + modifier);
                pixels[target + 1] = (byte) clamp(base[1] + modifier);
                pixels[target + 2] = (byte) clamp(base[2] + modifier);
            }
        }
    }

    private static int modifier(int table, int index)
    {
        int value = MODIFIERS[table][index & 1];
        return (index & 2) != 0 ? -value : value;
    }

    private static final class Candidate
    {
        long mError = Long.MAX_VALUE;
        long mHigh;
        long mLow;
    }

    private static void encode(byte[] pixels, int mask, byte[] encoded)
    {
        Candidate best = new Candidate();

        for (int flipIndex = 0; flipIndex < 2; flipIndex++)
        {
            boolean flip = flipIndex == 1;
            float[] firstAverage = average(pixels, mask, flip, true);
            float[] secondAverage = average(pixels, mask, flip, false);

            int[] firstQuantized = new int[3];
            int[] secondQuantized = new int[3];
            boolean differentialFits = true;
            for (int channel = 0; channel < 3; channel++)
            {
                firstQuantized[channel] = Math.round(firstAverage[channel] * 31.0f / 255.0f);
                secondQuantized[channel] = Math.round(secondAverage[channel] * 31.0f / 255.0f);
                int delta = secondQuantized[channel] - firstQuantized[channel];
                if (delta < -4 || delta > 3) differentialFits = false;
            }
            if (differentialFits) tryMode(pixels, mask, flip, true, firstQuantized, secondQuantized, best);

            for (int channel = 0; channel < 3; channel++)
            {
                firstQuantized[channel] = Math.round(firstAverage[channel] * 15.0f / 255.0f);
                secondQuantized[channel] = Math.round(secondAverage[channel] * 15.0f / 255.0f);
            }
            tryMode(pixels, mask, flip, false, firstQuantized, secondQuantized, best);
        }

        for (int index = 0; index < 4; index++)
        {
            encoded[index] = (byte) (best.mHigh >> (24 - index * 8));
            encoded[index + 4] = (byte) (best.mLow >> (24 - index * 8));
        }
    }

    private static float[] average(byte[] pixels, int mask, boolean flip, boolean first)
    {
        float[] sum = new float[3];
        int count = 0;
        for (int y = 0; y < 4; y++)
        {
            for (int x = 0; x < 4; x++)
            {
                if (inFirstSubblock(x, y, flip) != first) continue;
                if ((mask & (1 << (y * 4 + x))) == 0) continue;

                int source = (y * 4 + x) * 3;
                sum[0] += pixels[source] & 0xFF;
                sum[1] += pixels[source + 1] & 0xFF;
                sum[2] += pixels[source + 2] & 0xFF;
                count++;
            }
        }

        if (count > 0)
        {
            for (int channel = 0; channel < 3; channel++) sum[channel] /= count;
        }
        return sum;
    }

    private static void tryMode(byte[] pixels, int mask, boolean flip, boolean differential, int[] firstQuantized,
            int[] secondQuantized, Candidate best)
    {
        int[] firstColor = new int[3];
        int[] secondColor = new int[3];
        for (int channel = 0; channel < 3; channel++)
        {
            if (differential)
            {
                firstColor[channel] = (firstQuantized[channel] << 3) | (firstQuantized[channel] >> 2);
                secondColor[channel] = (secondQuantized[channel] << 3) | (secondQuantized[channel] >> 2);
            }
            else
            {
                firstColor[channel] = (firstQuantized[channel] << 4) | firstQuantized[channel];
                secondColor[channel] = (secondQuantized[channel] << 4) | secondQuantized[channel];
            }
        }

        long[] firstResult = bestTable(pixels, mask, flip, true, firstColor);
        long[] secondResult = bestTable(pixels, mask, flip, false, secondColor);
        long error = firstResult[0] + secondResult[0];
        if (error >= best.mError) return;

        long high = 0;
        for (int channel = 0; channel < 3; channel++)
        {
            int shift = 24 - channel * 8;
            if (differential)
            {
                int delta = (secondQuantized[channel] - firstQuantized[channel]) & 0x7;
                high |= (long) ((firstQuantized[channel] << 3) | delta) << shift;
            }
            else
            {
                high |= (long) ((firstQuantized[channel] << 4) | secondQuantized[channel]) << shift;
            }
        }
        high |= firstResult[1] << 5;
        high |= secondResult[1] << 2;
        if (differential) high |= 2;
        if (flip) high |= 1;

        best.mError = error;
        best.mHigh = high;
        best.mLow = firstResult[2] | secondResult[2];
    }

    private static long[] bestTable(byte[] pixels, int mask, boolean flip, boolean first, int[] color)
    {
        long bestError = Long.MAX_VALUE;
        long bestTable = 0;
        long bestBits = 0;

        for (int table = 0; table < MODIFIERS.length; table++)
        {
            long error = 0;
            long bits = 0;
            for (int y = 0; y < 4; y++)
            {
                for (int x = 0; x < 4; x++)
                {
                    if (inFirstSubblock(x, y, flip) != first) continue;

                    int source = (y * 4 + x) * 3;
                    boolean valid = (mask & (1 << (y * 4 + x))) != 0;
                    long pixelError = Long.MAX_VALUE;
                    int pixelIndex = 0;
                    for (int index = 0; index < 4; index++)
                    {
                        int modifier = modifier(table, index);
                        long candidate = 0;
                        for (int channel = 0; channel < 3; channel++)
                        {
                            int difference = clamp(color[channel] + modifier) - (pixels[source + channel] & 0xFF);
                            candidate += (long) difference * difference;
                        }
                        if (candidate < pixelError)
                        {
                            pixelError = candidate;
                            pixelIndex = index;
                        }
                    }

                    if (valid) error += pixelError;
                    int bit = x * 4 + y;
                    bits |= (long) ((pixelIndex >> 1) & 1) << (bit + 16);
                    bits |= (long) (pixelIndex & 1) << bit;
                }
            }

            if (error < bestError)
            {
                bestError = error;
                bestTable = table;
                bestBits = bits;
            }
        }

        return new long[] {bestError, bestTable, bestBits};
    }
}
