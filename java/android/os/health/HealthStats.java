package android.os.health;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class HealthStats
{
    private final String mDataType;

    private final int[] mTimerKeys;
    private final int[] mTimerCounts;
    private final long[] mTimerTimes;

    private final int[] mMeasurementKeys;
    private final long[] mMeasurementValues;

    private final int[] mStatsKeys;
    private final Map<String, HealthStats>[] mStatsValues;

    private final int[] mTimersKeys;
    private final Map<String, TimerStat>[] mTimersValues;

    private final int[] mMeasurementsKeys;
    private final Map<String, Long>[] mMeasurementsValues;

    static final class Builder
    {
        private final String mDataType;
        private final TreeMap<Integer, TimerStat> mTimers = new TreeMap<>();
        private final TreeMap<Integer, Long> mMeasurements = new TreeMap<>();
        private final TreeMap<Integer, Map<String, HealthStats>> mStats = new TreeMap<>();
        private final TreeMap<Integer, Map<String, TimerStat>> mTimersMaps = new TreeMap<>();
        private final TreeMap<Integer, Map<String, Long>> mMeasurementsMaps = new TreeMap<>();

        Builder(String dataType)
        {
            mDataType = dataType;
        }

        Builder timer(int key, int count, long timeMs)
        {
            mTimers.put(key, new TimerStat(count, timeMs));
            return this;
        }

        Builder measurement(int key, long value)
        {
            mMeasurements.put(key, value);
            return this;
        }

        Builder stats(int key, String name, HealthStats value)
        {
            Map<String, HealthStats> map = mStats.get(key);
            if (map == null)
            {
                map = new TreeMap<>();
                mStats.put(key, map);
            }

            map.put(name, value);
            return this;
        }

        HealthStats build()
        {
            return new HealthStats(this);
        }
    }

    HealthStats()
    {
        this(new Builder(null));
    }

    @SuppressWarnings("unchecked")
    private HealthStats(Builder builder)
    {
        mDataType = builder.mDataType;

        mTimerKeys = keys(builder.mTimers);
        mTimerCounts = new int[mTimerKeys.length];
        mTimerTimes = new long[mTimerKeys.length];
        for (int index = 0; index < mTimerKeys.length; index++)
        {
            TimerStat timer = builder.mTimers.get(mTimerKeys[index]);
            mTimerCounts[index] = timer.getCount();
            mTimerTimes[index] = timer.getTime();
        }

        mMeasurementKeys = keys(builder.mMeasurements);
        mMeasurementValues = new long[mMeasurementKeys.length];
        for (int index = 0; index < mMeasurementKeys.length; index++)
        {
            mMeasurementValues[index] = builder.mMeasurements.get(mMeasurementKeys[index]);
        }

        mStatsKeys = keys(builder.mStats);
        mStatsValues = new Map[mStatsKeys.length];
        for (int index = 0; index < mStatsKeys.length; index++)
        {
            mStatsValues[index] = Collections.unmodifiableMap(builder.mStats.get(mStatsKeys[index]));
        }

        mTimersKeys = keys(builder.mTimersMaps);
        mTimersValues = new Map[mTimersKeys.length];
        for (int index = 0; index < mTimersKeys.length; index++)
        {
            mTimersValues[index] = Collections.unmodifiableMap(builder.mTimersMaps.get(mTimersKeys[index]));
        }

        mMeasurementsKeys = keys(builder.mMeasurementsMaps);
        mMeasurementsValues = new Map[mMeasurementsKeys.length];
        for (int index = 0; index < mMeasurementsKeys.length; index++)
        {
            mMeasurementsValues[index] = Collections.unmodifiableMap(builder.mMeasurementsMaps.get(mMeasurementsKeys[index]));
        }
    }

    private static int[] keys(TreeMap<Integer, ?> map)
    {
        int[] keys = new int[map.size()];
        int index = 0;
        for (Integer key : map.keySet()) keys[index++] = key;
        return keys;
    }

    private static int indexOf(int[] keys, int key)
    {
        int index = Arrays.binarySearch(keys, key);
        return index < 0 ? -1 : index;
    }

    private static int require(int[] keys, int key, String kind)
    {
        int index = indexOf(keys, key);
        if (index < 0) throw new IndexOutOfBoundsException("Bad " + kind + " key dataType=" + key);
        return index;
    }

    public String getDataType()
    {
        return mDataType;
    }

    public boolean hasTimer(int key)
    {
        return indexOf(mTimerKeys, key) >= 0;
    }

    public TimerStat getTimer(int key)
    {
        int index = require(mTimerKeys, key, "timer");
        return new TimerStat(mTimerCounts[index], mTimerTimes[index]);
    }

    public int getTimerCount(int key)
    {
        return mTimerCounts[require(mTimerKeys, key, "timer")];
    }

    public long getTimerTime(int key)
    {
        return mTimerTimes[require(mTimerKeys, key, "timer")];
    }

    public int getTimerKeyCount()
    {
        return mTimerKeys.length;
    }

    public int getTimerKeyAt(int index)
    {
        return mTimerKeys[index];
    }

    public boolean hasMeasurement(int key)
    {
        return indexOf(mMeasurementKeys, key) >= 0;
    }

    public long getMeasurement(int key)
    {
        return mMeasurementValues[require(mMeasurementKeys, key, "measurement")];
    }

    public int getMeasurementKeyCount()
    {
        return mMeasurementKeys.length;
    }

    public int getMeasurementKeyAt(int index)
    {
        return mMeasurementKeys[index];
    }

    public boolean hasStats(int key)
    {
        return indexOf(mStatsKeys, key) >= 0;
    }

    public Map<String, HealthStats> getStats(int key)
    {
        return mStatsValues[require(mStatsKeys, key, "stats")];
    }

    public int getStatsKeyCount()
    {
        return mStatsKeys.length;
    }

    public int getStatsKeyAt(int index)
    {
        return mStatsKeys[index];
    }

    public boolean hasTimers(int key)
    {
        return indexOf(mTimersKeys, key) >= 0;
    }

    public Map<String, TimerStat> getTimers(int key)
    {
        return mTimersValues[require(mTimersKeys, key, "timers")];
    }

    public int getTimersKeyCount()
    {
        return mTimersKeys.length;
    }

    public int getTimersKeyAt(int index)
    {
        return mTimersKeys[index];
    }

    public boolean hasMeasurements(int key)
    {
        return indexOf(mMeasurementsKeys, key) >= 0;
    }

    public Map<String, Long> getMeasurements(int key)
    {
        return mMeasurementsValues[require(mMeasurementsKeys, key, "measurements")];
    }

    public int getMeasurementsKeyCount()
    {
        return mMeasurementsKeys.length;
    }

    public int getMeasurementsKeyAt(int index)
    {
        return mMeasurementsKeys[index];
    }
}
