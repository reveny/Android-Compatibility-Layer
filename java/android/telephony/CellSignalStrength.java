package android.telephony;

public abstract class CellSignalStrength
{
    public static final int SIGNAL_STRENGTH_NONE_OR_UNKNOWN = 0;
    public static final int SIGNAL_STRENGTH_POOR = 1;
    public static final int SIGNAL_STRENGTH_MODERATE = 2;
    public static final int SIGNAL_STRENGTH_GOOD = 3;
    public static final int SIGNAL_STRENGTH_GREAT = 4;

    static final int NUM_SIGNAL_STRENGTH_BINS = 5;

    CellSignalStrength()
    {
    }

    public abstract int getLevel();

    public abstract int getAsuLevel();

    public abstract int getDbm();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object other);

    static int inRangeOrUnavailable(int value, int minimum, int maximum)
    {
        return value < minimum || value > maximum ? CellInfo.UNAVAILABLE : value;
    }

    static int levelFromThresholds(int value, int[] thresholds)
    {
        if (value == CellInfo.UNAVAILABLE) return SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        int level = SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        for (int threshold : thresholds)
        {
            if (value < threshold) break;
            level++;
        }
        return level;
    }
}
