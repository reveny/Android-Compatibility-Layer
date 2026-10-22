package android.graphics.text;

import android.graphics.fonts.Font;

import java.util.Arrays;
import java.util.Objects;

public final class PositionedGlyphs
{
    private final int[] mGlyphIds;
    private final float[] mPositions;
    private final Font[] mFonts;
    private final float mAdvance;
    private final float mAscent;
    private final float mDescent;
    private final float mOffsetX;
    private final float mOffsetY;

    PositionedGlyphs(int[] glyphIds, float[] positions, Font[] fonts, float advance, float ascent, float descent, float offsetX,
                     float offsetY)
    {
        mGlyphIds = glyphIds;
        mPositions = positions;
        mFonts = fonts;
        mAdvance = advance;
        mAscent = ascent;
        mDescent = descent;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
    }

    public float getAdvance()
    {
        return mAdvance;
    }

    public float getAscent()
    {
        return mAscent;
    }

    public float getDescent()
    {
        return mDescent;
    }

    public float getOffsetX()
    {
        return mOffsetX;
    }

    public float getOffsetY()
    {
        return mOffsetY;
    }

    public int glyphCount()
    {
        return mGlyphIds.length;
    }

    public Font getFont(int index)
    {
        checkIndex(index);
        return mFonts[index];
    }

    public int getGlyphId(int index)
    {
        checkIndex(index);
        return mGlyphIds[index];
    }

    public float getGlyphX(int index)
    {
        checkIndex(index);
        return mPositions[index * 2];
    }

    public float getGlyphY(int index)
    {
        checkIndex(index);
        return mPositions[index * 2 + 1];
    }

    private void checkIndex(int index)
    {
        if (index < 0 || index >= mGlyphIds.length)
        {
            throw new IllegalArgumentException("Index " + index + " is out of range [0, " + mGlyphIds.length + ")");
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PositionedGlyphs)) return false;

        PositionedGlyphs glyphs = (PositionedGlyphs)other;
        return Float.compare(mAdvance, glyphs.mAdvance) == 0 && Float.compare(mAscent, glyphs.mAscent) == 0
               && Float.compare(mDescent, glyphs.mDescent) == 0 && Float.compare(mOffsetX, glyphs.mOffsetX) == 0
               && Float.compare(mOffsetY, glyphs.mOffsetY) == 0 && Arrays.equals(mGlyphIds, glyphs.mGlyphIds)
               && Arrays.equals(mPositions, glyphs.mPositions) && Arrays.equals(mFonts, glyphs.mFonts);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mAdvance, mAscent, mDescent, mOffsetX, mOffsetY, Arrays.hashCode(mGlyphIds), Arrays.hashCode(mPositions),
                            Arrays.hashCode(mFonts));
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < mGlyphIds.length; index++)
        {
            if (index > 0) builder.append(", ");
            builder.append("[ID = ").append(mGlyphIds[index]).append(", pos = (").append(mPositions[index * 2]).append(",")
                   .append(mPositions[index * 2 + 1]).append("), font = ").append(mFonts[index]).append("]");
        }
        builder.append("]");

        return "PositionedGlyphs{glyphs = " + builder + ", mXOffset=" + mOffsetX + ", mYOffset=" + mOffsetY + "}";
    }
}
