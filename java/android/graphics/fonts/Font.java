package android.graphics.fonts;

import android.content.res.AssetManager;

import java.io.File;
import java.io.IOException;

public final class Font
{
    private final String mName;
    private final FontStyle mStyle;
    private final int mTtcIndex;

    private Font(String name, FontStyle style, int ttcIndex)
    {
        mName = name;
        mStyle = style;
        mTtcIndex = ttcIndex;
    }

    public FontStyle getStyle()
    {
        return mStyle;
    }

    public int getTtcIndex()
    {
        return mTtcIndex;
    }

    public File getFile()
    {
        return null;
    }

    public java.nio.ByteBuffer getBuffer()
    {
        return java.nio.ByteBuffer.allocate(0);
    }

    @Override
    public String toString()
    {
        return "Font{" + mName + "}";
    }

    public static final class Builder
    {
        private String mName = "";
        private int mWeight = FontStyle.FONT_WEIGHT_NORMAL;
        private int mSlant = FontStyle.FONT_SLANT_UPRIGHT;
        private int mTtcIndex;

        public Builder(File path)
        {
            mName = path == null ? "" : path.getPath();
        }

        public Builder(AssetManager assets, String path)
        {
            mName = path == null ? "" : path;
        }

        public Builder(java.nio.ByteBuffer buffer)
        {
        }

        public Builder(android.content.res.Resources resources, int resourceId)
        {
            mName = "resource/0x" + Integer.toHexString(resourceId);
        }

        public Builder setWeight(int weight)
        {
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
            return this;
        }

        public Font build() throws IOException
        {
            return new Font(mName, new FontStyle(mWeight, mSlant), mTtcIndex);
        }
    }
}
