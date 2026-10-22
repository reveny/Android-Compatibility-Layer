package android.view.inputmethod;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Layout;
import android.text.SegmentFinder;

import java.util.Arrays;

public final class TextBoundsInfo implements Parcelable
{
    public static final int FLAG_CHARACTER_WHITESPACE = 1;
    public static final int FLAG_CHARACTER_LINEFEED = 1 << 1;
    public static final int FLAG_CHARACTER_PUNCTUATION = 1 << 2;
    public static final int FLAG_LINE_IS_RTL = 1 << 3;

    public static final Parcelable.Creator<TextBoundsInfo> CREATOR = new Parcelable.Creator<TextBoundsInfo>()
    {
        @Override
        public TextBoundsInfo createFromParcel(Parcel source)
        {
            int start = source.readInt();
            int end = source.readInt();
            float[] matrix = source.createFloatArray();
            float[] bounds = source.createFloatArray();
            int[] flags = source.createIntArray();
            int[] bidiLevels = source.createIntArray();
            SegmentFinder grapheme = new RangeSegmentFinder(source.createIntArray());
            SegmentFinder word = new RangeSegmentFinder(source.createIntArray());
            SegmentFinder line = new RangeSegmentFinder(source.createIntArray());

            return new TextBoundsInfo(start, end, matrix, bounds, flags, bidiLevels, grapheme, word, line);
        }

        @Override
        public TextBoundsInfo[] newArray(int size)
        {
            return new TextBoundsInfo[size];
        }
    };

    private final int mStart;
    private final int mEnd;
    private final float[] mMatrixValues;
    private final float[] mCharacterBounds;
    private final int[] mCharacterFlags;
    private final int[] mCharacterBidiLevels;
    private final SegmentFinder mGraphemeSegmentFinder;
    private final SegmentFinder mWordSegmentFinder;
    private final SegmentFinder mLineSegmentFinder;

    private TextBoundsInfo(int start, int end, float[] matrixValues, float[] characterBounds, int[] characterFlags, int[] bidiLevels,
                           SegmentFinder grapheme, SegmentFinder word, SegmentFinder line)
    {
        mStart = start;
        mEnd = end;
        mMatrixValues = matrixValues;
        mCharacterBounds = characterBounds;
        mCharacterFlags = characterFlags;
        mCharacterBidiLevels = bidiLevels;
        mGraphemeSegmentFinder = grapheme;
        mWordSegmentFinder = word;
        mLineSegmentFinder = line;
    }

    public void getMatrix(Matrix matrix)
    {
        if (matrix == null) throw new NullPointerException("matrix");

        matrix.setValues(mMatrixValues);
    }

    public int getStartIndex()
    {
        return mStart;
    }

    public int getEndIndex()
    {
        return mEnd;
    }

    private int checkIndex(int index)
    {
        if (index < mStart || index >= mEnd)
        {
            throw new IndexOutOfBoundsException("Index is out of the bounds of [" + mStart + ", " + mEnd + ").");
        }

        return index - mStart;
    }

    public void getCharacterBounds(int index, RectF bounds)
    {
        int offset = checkIndex(index) * 4;
        bounds.set(mCharacterBounds[offset], mCharacterBounds[offset + 1], mCharacterBounds[offset + 2], mCharacterBounds[offset + 3]);
    }

    public int getCharacterFlags(int index)
    {
        return mCharacterFlags[checkIndex(index)];
    }

    public int getCharacterBidiLevel(int index)
    {
        return mCharacterBidiLevels[checkIndex(index)];
    }

    public SegmentFinder getGraphemeSegmentFinder()
    {
        return mGraphemeSegmentFinder;
    }

    public SegmentFinder getWordSegmentFinder()
    {
        return mWordSegmentFinder;
    }

    public SegmentFinder getLineSegmentFinder()
    {
        return mLineSegmentFinder;
    }

    public int getOffsetForPosition(float x, float y)
    {
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        boolean bestAfter = false;

        for (int index = 0; index < mEnd - mStart; index++)
        {
            float left = mCharacterBounds[index * 4];
            float top = mCharacterBounds[index * 4 + 1];
            float right = mCharacterBounds[index * 4 + 2];
            float bottom = mCharacterBounds[index * 4 + 3];

            if (y < top || y > bottom) continue;

            float center = (left + right) / 2;
            float distance = x < left ? left - x : (x > right ? x - right : 0);

            if (distance < bestDistance)
            {
                bestDistance = distance;
                best = index;

                boolean rtl = (mCharacterBidiLevels[index] & 1) != 0;
                bestAfter = rtl ? x < center : x > center;
            }
        }

        if (best < 0) return -1;

        return mStart + best + (bestAfter ? 1 : 0);
    }

    public int[] getRangeForRect(RectF area, SegmentFinder segmentFinder, Layout.TextInclusionStrategy inclusionStrategy)
    {
        int rangeStart = -1;
        int rangeEnd = -1;
        int start = segmentFinder.nextStartBoundary(mStart - 1);
        RectF segmentBounds = new RectF();

        while (start != SegmentFinder.DONE && start < mEnd)
        {
            int end = segmentFinder.nextEndBoundary(start);
            if (end == SegmentFinder.DONE) break;

            segmentBounds.setEmpty();
            boolean first = true;

            for (int index = Math.max(start, mStart); index < Math.min(end, mEnd); index++)
            {
                int offset = (index - mStart) * 4;
                RectF character = new RectF(mCharacterBounds[offset], mCharacterBounds[offset + 1], mCharacterBounds[offset + 2],
                                            mCharacterBounds[offset + 3]);

                if (first) segmentBounds.set(character);
                else segmentBounds.union(character);

                first = false;
            }

            if (!first && inclusionStrategy.isSegmentInside(segmentBounds, area))
            {
                if (rangeStart < 0) rangeStart = start;
                rangeEnd = end;
            }

            start = segmentFinder.nextStartBoundary(start);
        }

        return rangeStart < 0 ? null : new int[] {rangeStart, rangeEnd};
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mStart);
        dest.writeInt(mEnd);
        dest.writeFloatArray(mMatrixValues);
        dest.writeFloatArray(mCharacterBounds);
        dest.writeIntArray(mCharacterFlags);
        dest.writeIntArray(mCharacterBidiLevels);
        dest.writeIntArray(encode(mGraphemeSegmentFinder));
        dest.writeIntArray(encode(mWordSegmentFinder));
        dest.writeIntArray(encode(mLineSegmentFinder));
    }

    private int[] encode(SegmentFinder finder)
    {
        int[] ranges = new int[8];
        int count = 0;
        int start = finder.nextStartBoundary(mStart - 1);

        while (start != SegmentFinder.DONE && start < mEnd)
        {
            int end = finder.nextEndBoundary(start);
            if (end == SegmentFinder.DONE) break;

            if (count + 2 > ranges.length) ranges = Arrays.copyOf(ranges, ranges.length * 2);
            ranges[count++] = start;
            ranges[count++] = end;

            start = finder.nextStartBoundary(start);
        }

        return Arrays.copyOf(ranges, count);
    }

    private static final class RangeSegmentFinder extends SegmentFinder
    {
        private final int[] mRanges;

        RangeSegmentFinder(int[] ranges)
        {
            mRanges = ranges == null ? new int[0] : ranges;
        }

        @Override
        public int previousStartBoundary(int offset)
        {
            for (int index = mRanges.length - 2; index >= 0; index -= 2)
            {
                if (mRanges[index] < offset) return mRanges[index];
            }

            return DONE;
        }

        @Override
        public int previousEndBoundary(int offset)
        {
            for (int index = mRanges.length - 1; index >= 1; index -= 2)
            {
                if (mRanges[index] < offset) return mRanges[index];
            }

            return DONE;
        }

        @Override
        public int nextStartBoundary(int offset)
        {
            for (int index = 0; index < mRanges.length; index += 2)
            {
                if (mRanges[index] > offset) return mRanges[index];
            }

            return DONE;
        }

        @Override
        public int nextEndBoundary(int offset)
        {
            for (int index = 1; index < mRanges.length; index += 2)
            {
                if (mRanges[index] > offset) return mRanges[index];
            }

            return DONE;
        }
    }

    public static final class Builder
    {
        private int mStart = -1;
        private int mEnd = -1;
        private float[] mMatrixValues;
        private float[] mCharacterBounds;
        private int[] mCharacterFlags;
        private int[] mCharacterBidiLevels;
        private SegmentFinder mGraphemeSegmentFinder;
        private SegmentFinder mWordSegmentFinder;
        private SegmentFinder mLineSegmentFinder;

        public Builder(int start, int end)
        {
            setStartAndEnd(start, end);
        }

        public Builder clear()
        {
            mStart = -1;
            mEnd = -1;
            mMatrixValues = null;
            mCharacterBounds = null;
            mCharacterFlags = null;
            mCharacterBidiLevels = null;
            mGraphemeSegmentFinder = null;
            mWordSegmentFinder = null;
            mLineSegmentFinder = null;
            return this;
        }

        public Builder setMatrix(Matrix matrix)
        {
            if (mMatrixValues == null) mMatrixValues = new float[9];

            if (matrix == null) Matrix.IDENTITY_MATRIX.getValues(mMatrixValues);
            else matrix.getValues(mMatrixValues);

            return this;
        }

        public Builder setStartAndEnd(int start, int end)
        {
            if (start < 0) throw new IllegalArgumentException("start must be greater or equal to 0");
            if (start > end) throw new IllegalArgumentException("end must be greater or equal to start");

            mStart = start;
            mEnd = end;
            return this;
        }

        public Builder setCharacterBounds(float[] characterBounds)
        {
            if (characterBounds == null) throw new NullPointerException("characterBounds");

            mCharacterBounds = characterBounds;
            return this;
        }

        public Builder setCharacterFlags(int[] characterFlags)
        {
            if (characterFlags == null) throw new NullPointerException("characterFlags");

            for (int flag : characterFlags)
            {
                if ((flag & ~(FLAG_CHARACTER_WHITESPACE | FLAG_CHARACTER_LINEFEED | FLAG_CHARACTER_PUNCTUATION | FLAG_LINE_IS_RTL)) != 0)
                {
                    throw new IllegalArgumentException("characterFlags contains invalid flags.");
                }
            }

            mCharacterFlags = characterFlags;
            return this;
        }

        public Builder setCharacterBidiLevel(int[] characterBidiLevels)
        {
            if (characterBidiLevels == null) throw new NullPointerException("characterBidiLevels");

            mCharacterBidiLevels = characterBidiLevels;
            return this;
        }

        public Builder setGraphemeSegmentFinder(SegmentFinder graphemeSegmentFinder)
        {
            if (graphemeSegmentFinder == null) throw new NullPointerException("graphemeSegmentFinder");

            mGraphemeSegmentFinder = graphemeSegmentFinder;
            return this;
        }

        public Builder setWordSegmentFinder(SegmentFinder wordSegmentFinder)
        {
            if (wordSegmentFinder == null) throw new NullPointerException("wordSegmentFinder");

            mWordSegmentFinder = wordSegmentFinder;
            return this;
        }

        public Builder setLineSegmentFinder(SegmentFinder lineSegmentFinder)
        {
            if (lineSegmentFinder == null) throw new NullPointerException("lineSegmentFinder");

            mLineSegmentFinder = lineSegmentFinder;
            return this;
        }

        public TextBoundsInfo build()
        {
            if (mStart < 0 || mEnd < 0) throw new IllegalStateException("Start and end must be set.");
            if (mMatrixValues == null) throw new IllegalStateException("Matrix must be set.");
            if (mCharacterBounds == null) throw new IllegalStateException("CharacterBounds must be set.");
            if (mCharacterFlags == null) throw new IllegalStateException("CharacterFlags must be set.");
            if (mCharacterBidiLevels == null) throw new IllegalStateException("CharacterBidiLevel must be set.");
            if (mGraphemeSegmentFinder == null) throw new IllegalStateException("GraphemeSegmentFinder must be set.");
            if (mWordSegmentFinder == null) throw new IllegalStateException("WordSegmentFinder must be set.");
            if (mLineSegmentFinder == null) throw new IllegalStateException("LineSegmentFinder must be set.");

            int length = mEnd - mStart;
            if (mCharacterBounds.length != length * 4)
            {
                throw new IllegalStateException("The length of characterBounds doesn't match the length of the given start and end.");
            }

            if (mCharacterFlags.length != length)
            {
                throw new IllegalStateException("The length of characterFlags doesn't match the length of the given start and end.");
            }

            if (mCharacterBidiLevels.length != length)
            {
                throw new IllegalStateException("The length of characterBidiLevels doesn't match the length of the given start and end.");
            }

            return new TextBoundsInfo(mStart, mEnd, mMatrixValues.clone(), mCharacterBounds.clone(), mCharacterFlags.clone(),
                                      mCharacterBidiLevels.clone(), mGraphemeSegmentFinder, mWordSegmentFinder, mLineSegmentFinder);
        }
    }
}
