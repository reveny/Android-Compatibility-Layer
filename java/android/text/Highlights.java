package android.text;

import android.graphics.Paint;
import java.util.ArrayList;

public class Highlights
{
    private final Paint[] mPaints;
    private final int[][] mRanges;

    private Highlights(Paint[] paints, int[][] ranges)
    {
        mPaints = paints;
        mRanges = ranges;
    }

    public int getSize()
    {
        return mPaints.length;
    }

    public Paint getPaint(int index)
    {
        return mPaints[index];
    }

    public int[] getRanges(int index)
    {
        return mRanges[index];
    }

    public static final class Builder
    {
        private final ArrayList<Paint> mPaints = new ArrayList<>();
        private final ArrayList<int[]> mRanges = new ArrayList<>();

        public Builder()
        {
        }

        public Builder addRange(Paint paint, int start, int end)
        {
            if (start > end) throw new IllegalArgumentException("start must not be larger than end: " + start + ", " + end);
            if (paint == null) throw new NullPointerException("paint");

            mPaints.add(paint);
            mRanges.add(new int[] {start, end});
            return this;
        }

        public Builder addRanges(Paint paint, int... ranges)
        {
            if (ranges.length % 2 == 1) throw new IllegalArgumentException("Flatten ranges must have even numbered elements");

            for (int index = 0; index < ranges.length; index += 2)
            {
                if (ranges[index] > ranges[index + 1])
                {
                    throw new IllegalArgumentException("Reverse range found in the flatten range: " + ranges[index] + ", "
                                                       + ranges[index + 1] + " at " + index);
                }
            }

            if (paint == null) throw new NullPointerException("paint");

            mPaints.add(paint);
            mRanges.add(ranges.clone());
            return this;
        }

        public Highlights build()
        {
            return new Highlights(mPaints.toArray(new Paint[0]), mRanges.toArray(new int[0][]));
        }
    }
}
