package android.util;

import java.util.ArrayList;

public class TimingLogger
{
    private final ArrayList<Long> mTimes = new ArrayList<>();
    private final ArrayList<String> mLabels = new ArrayList<>();

    private String mTag;
    private String mLabel;
    private boolean mEnabled;

    public TimingLogger(String tag, String label)
    {
        reset(tag, label);
    }

    public void reset(String tag, String label)
    {
        mTag = tag;
        mLabel = label;
        reset();
    }

    public void reset()
    {
        mEnabled = Log.isLoggable(mTag, Log.VERBOSE);

        mTimes.clear();
        mLabels.clear();

        addSplit(null);
    }

    public void addSplit(String splitLabel)
    {
        if (!mEnabled) return;

        mTimes.add(Long.valueOf(android.os.SystemClock.elapsedRealtime()));
        mLabels.add(splitLabel);
    }

    public void dumpToLog()
    {
        if (!mEnabled || mTimes.isEmpty()) return;

        Log.d(mTag, mLabel + ": begin");

        long first = mTimes.get(0).longValue();
        long previous = first;

        for (int index = 1; index < mTimes.size(); index++)
        {
            long now = mTimes.get(index).longValue();

            Log.d(mTag, mLabel + ":      " + (now - previous) + " ms, " + mLabels.get(index));
            previous = now;
        }

        Log.d(mTag, mLabel + ": end, " + (previous - first) + " ms");
    }
}
