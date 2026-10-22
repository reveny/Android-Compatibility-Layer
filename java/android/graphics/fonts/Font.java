package android.graphics.fonts;

import android.content.res.AssetManager;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public final class Font
{
    private final String mName;
    private final FontStyle mStyle;
    private final int mTtcIndex;
    private final FontVariationAxis[] mAxes;
    private final java.nio.ByteBuffer mBuffer;

    private Font(String name, FontStyle style, int ttcIndex, FontVariationAxis[] axes, java.nio.ByteBuffer buffer)
    {
        mName = name;
        mStyle = style;
        mTtcIndex = ttcIndex;
        mAxes = axes;
        mBuffer = buffer;
    }

    public FontStyle getStyle()
    {
        return mStyle;
    }

    public int getTtcIndex()
    {
        return mTtcIndex;
    }

    public FontVariationAxis[] getAxes()
    {
        return mAxes == null ? null : mAxes.clone();
    }

    public android.os.LocaleList getLocaleList()
    {
        return android.os.LocaleList.getEmptyLocaleList();
    }

    public float getGlyphBounds(int glyphId, Paint paint, RectF outBoundingBox)
    {
        String glyph = new String(Character.toChars(Math.max(0, glyphId)));
        float advance = paint.measureText(glyph);

        if (outBoundingBox != null)
        {
            android.graphics.Rect bounds = new android.graphics.Rect();
            paint.getTextBounds(glyph, 0, glyph.length(), bounds);
            outBoundingBox.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
        }

        return advance;
    }

    public void getMetrics(Paint paint, Paint.FontMetrics outMetrics)
    {
        if (outMetrics != null) paint.getFontMetrics(outMetrics);
    }

    public int getSourceIdentifier()
    {
        return mName.hashCode();
    }

    public File getFile()
    {
        if (mName.isEmpty() || mName.startsWith("resource/")) return null;

        File file = new File(mName);
        return file.isAbsolute() ? file : null;
    }

    public java.nio.ByteBuffer getBuffer()
    {
        if (mBuffer == null) return java.nio.ByteBuffer.allocate(0).asReadOnlyBuffer();

        return mBuffer.asReadOnlyBuffer();
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof Font)) return false;

        Font font = (Font)other;
        return mName.equals(font.mName) && mTtcIndex == font.mTtcIndex && mStyle.getWeight() == font.mStyle.getWeight()
               && mStyle.getSlant() == font.mStyle.getSlant() && Arrays.equals(mAxes, font.mAxes)
               && Objects.equals(mBuffer, font.mBuffer);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mName, mTtcIndex, mStyle.getWeight(), mStyle.getSlant(), Arrays.hashCode(mAxes));
    }

    @Override
    public String toString()
    {
        return "Font {path=" + mName + ", style=" + mStyle + ", ttcIndex=" + mTtcIndex + ", axes="
               + FontVariationAxis.toFontVariationSettings(mAxes) + "}";
    }

    public static final class Builder
    {
        private String mName = "";
        private int mWeight = FontStyle.FONT_WEIGHT_NORMAL;
        private int mSlant = FontStyle.FONT_SLANT_UPRIGHT;
        private int mTtcIndex;
        private FontVariationAxis[] mAxes;
        private java.nio.ByteBuffer mBuffer;
        private IOException mFailure;

        public Builder(File path)
        {
            Objects.requireNonNull(path, "path can not be null");
            mName = path.getPath();
        }

        public Builder(AssetManager assets, String path)
        {
            Objects.requireNonNull(assets, "manager can not be null");
            Objects.requireNonNull(path, "path can not be null");
            mName = path;

            try
            {
                mBuffer = readFully(assets.open(path));
            }
            catch (IOException failure)
            {
                mFailure = failure;
            }
        }

        public Builder(java.nio.ByteBuffer buffer)
        {
            Objects.requireNonNull(buffer, "buffer can not be null");
            if (!buffer.isDirect()) throw new IllegalArgumentException("Only direct buffer can be used as the source of font data.");

            mBuffer = buffer.duplicate();
        }

        public Builder(android.os.ParcelFileDescriptor descriptor)
        {
            this(descriptor, 0, -1);
        }

        public Builder(android.os.ParcelFileDescriptor descriptor, long offset, long size)
        {
            Objects.requireNonNull(descriptor, "fd can not be null");
            mName = "fd/" + System.identityHashCode(descriptor) + "/" + offset;

            try
            {
                java.io.FileInputStream stream = new java.io.FileInputStream(descriptor.getFileDescriptor());
                java.nio.channels.FileChannel channel = stream.getChannel();
                long length = size < 0 ? channel.size() - offset : size;
                mBuffer = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, offset, length);
            }
            catch (IOException failure)
            {
                mFailure = failure;
            }
        }

        public Builder(android.content.res.Resources resources, int resourceId)
        {
            Objects.requireNonNull(resources, "res can not be null");
            mName = "resource/0x" + Integer.toHexString(resourceId);

            try
            {
                mBuffer = readFully(resources.openRawResource(resourceId));
            }
            catch (IOException | RuntimeException failure)
            {
                mFailure = failure instanceof IOException ? (IOException)failure : new IOException(failure);
            }
        }

        public Builder(Font font)
        {
            mName = font.mName;
            mWeight = font.mStyle.getWeight();
            mSlant = font.mStyle.getSlant();
            mTtcIndex = font.mTtcIndex;
            mAxes = font.mAxes;
            mBuffer = font.mBuffer;
        }

        private static java.nio.ByteBuffer readFully(java.io.InputStream stream) throws IOException
        {
            try (java.io.InputStream input = stream)
            {
                java.io.ByteArrayOutputStream collected = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[16384];
                int read;

                while ((read = input.read(chunk)) > 0) collected.write(chunk, 0, read);

                byte[] data = collected.toByteArray();
                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(data.length);
                buffer.put(data);
                buffer.flip();
                return buffer;
            }
        }

        public Builder setWeight(int weight)
        {
            if (weight < FontStyle.FONT_WEIGHT_MIN || weight > FontStyle.FONT_WEIGHT_MAX)
            {
                throw new IllegalArgumentException("weight value must be in [" + FontStyle.FONT_WEIGHT_MIN + ", "
                                                   + FontStyle.FONT_WEIGHT_MAX + "] range");
            }

            mWeight = weight;
            return this;
        }

        public Builder setSlant(int slant)
        {
            mSlant = slant;
            return this;
        }

        public Builder setTtcIndex(int index)
        {
            mTtcIndex = index;
            return this;
        }

        public Builder setFontVariationSettings(String settings)
        {
            mAxes = FontVariationAxis.fromFontVariationSettings(settings);
            return this;
        }

        public Builder setFontVariationSettings(FontVariationAxis[] axes)
        {
            mAxes = axes == null ? null : axes.clone();
            return this;
        }

        public Font build() throws IOException
        {
            if (mFailure != null) throw new IOException("Failed to read font contents", mFailure);

            return new Font(mName, new FontStyle(mWeight, mSlant), mTtcIndex, mAxes, mBuffer);
        }
    }
}
