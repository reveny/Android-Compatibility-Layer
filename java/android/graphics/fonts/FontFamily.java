package android.graphics.fonts;

import java.util.ArrayList;

public final class FontFamily
{
    private final ArrayList<Font> mFonts;

    private FontFamily(ArrayList<Font> fonts)
    {
        mFonts = fonts;
    }

    public int getSize()
    {
        return mFonts.size();
    }

    public Font getFont(int index)
    {
        return mFonts.get(index);
    }

    public static final class Builder
    {
        private final ArrayList<Font> mFonts = new ArrayList<>();

        public Builder(Font font)
        {
            if (font != null) mFonts.add(font);
        }

        public Builder addFont(Font font)
        {
            if (font != null) mFonts.add(font);
            return this;
        }

        public FontFamily build()
        {
            return new FontFamily(mFonts);
        }
    }
}
