package android.os;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class VibrationEffect implements Parcelable
{
    public static final int DEFAULT_AMPLITUDE = -1;
    public static final int MAX_AMPLITUDE = 255;

    public static final int EFFECT_CLICK = 0;
    public static final int EFFECT_DOUBLE_CLICK = 1;
    public static final int EFFECT_TICK = 2;
    public static final int EFFECT_HEAVY_CLICK = 5;

    public static final Parcelable.Creator<VibrationEffect> CREATOR = new Parcelable.Creator<VibrationEffect>()
    {
        @Override
        public VibrationEffect createFromParcel(Parcel source)
        {
            long[] timings = source.createLongArray();
            int[] amplitudes = source.createIntArray();
            int repeat = source.readInt();
            return new Waveform(timings == null ? new long[0] : timings, amplitudes == null ? new int[0] : amplitudes, repeat);
        }

        @Override
        public VibrationEffect[] newArray(int size)
        {
            return new VibrationEffect[size];
        }
    };

    VibrationEffect()
    {
    }

    public static VibrationEffect createOneShot(long milliseconds, int amplitude)
    {
        if (milliseconds <= 0) throw new IllegalArgumentException("timing must be positive");
        if (amplitude != DEFAULT_AMPLITUDE && (amplitude < 1 || amplitude > MAX_AMPLITUDE))
        {
            throw new IllegalArgumentException("amplitude must either be DEFAULT_AMPLITUDE, or between 1 and 255 inclusive");
        }

        return new Waveform(new long[] {milliseconds}, new int[] {amplitude}, -1);
    }

    public static VibrationEffect createWaveform(long[] timings, int repeat)
    {
        if (timings == null) throw new NullPointerException("timings");

        int[] amplitudes = new int[timings.length];
        for (int index = 0; index < amplitudes.length; index++) amplitudes[index] = index % 2 == 0 ? 0 : DEFAULT_AMPLITUDE;
        return createWaveform(timings, amplitudes, repeat);
    }

    public static VibrationEffect createWaveform(long[] timings, int[] amplitudes, int repeat)
    {
        if (timings == null || amplitudes == null) throw new NullPointerException("timings and amplitudes are required");
        if (timings.length != amplitudes.length)
        {
            throw new IllegalArgumentException("timing and amplitude arrays must be of equal length");
        }
        if (repeat < -1 || repeat >= timings.length)
        {
            throw new IllegalArgumentException("repeat index must be within the bounds of the timings array");
        }

        return new Waveform(timings.clone(), amplitudes.clone(), repeat);
    }

    public static VibrationEffect createPredefined(int effectId)
    {
        switch (effectId)
        {
            case EFFECT_CLICK:
                return new Waveform(new long[] {12}, new int[] {DEFAULT_AMPLITUDE}, -1);
            case EFFECT_DOUBLE_CLICK:
                return new Waveform(new long[] {0, 30, 100, 30}, new int[] {0, DEFAULT_AMPLITUDE, 0, DEFAULT_AMPLITUDE}, -1);
            case EFFECT_TICK:
                return new Waveform(new long[] {5}, new int[] {DEFAULT_AMPLITUDE}, -1);
            case EFFECT_HEAVY_CLICK:
                return new Waveform(new long[] {20}, new int[] {DEFAULT_AMPLITUDE}, -1);
            default:
                throw new IllegalArgumentException("Unknown predefined effect " + effectId);
        }
    }

    public static Composition startComposition()
    {
        return new Composition();
    }

    public abstract long getDuration();

    @Override
    public int describeContents()
    {
        return 0;
    }

    static final class Waveform extends VibrationEffect
    {
        private final long[] mTimings;
        private final int[] mAmplitudes;
        private final int mRepeat;

        Waveform(long[] timings, int[] amplitudes, int repeat)
        {
            mTimings = timings;
            mAmplitudes = amplitudes;
            mRepeat = repeat;
        }

        @Override
        public long getDuration()
        {
            if (mRepeat >= 0) return Long.MAX_VALUE;

            long total = 0;
            for (long timing : mTimings) total += timing;
            return total;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeLongArray(mTimings);
            dest.writeIntArray(mAmplitudes);
            dest.writeInt(mRepeat);
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof Waveform)) return false;

            Waveform waveform = (Waveform)other;
            return Arrays.equals(mTimings, waveform.mTimings) && Arrays.equals(mAmplitudes, waveform.mAmplitudes)
                && mRepeat == waveform.mRepeat;
        }

        @Override
        public int hashCode()
        {
            return Arrays.hashCode(mTimings) * 31 + Arrays.hashCode(mAmplitudes) + mRepeat;
        }

        @Override
        public String toString()
        {
            return "Waveform{mTimings=" + Arrays.toString(mTimings) + ", mAmplitudes=" + Arrays.toString(mAmplitudes)
                   + ", mRepeat=" + mRepeat + "}";
        }
    }

    public static final class Composition
    {
        public static final int PRIMITIVE_CLICK = 1;
        public static final int PRIMITIVE_THUD = 2;
        public static final int PRIMITIVE_SPIN = 3;
        public static final int PRIMITIVE_QUICK_RISE = 4;
        public static final int PRIMITIVE_SLOW_RISE = 5;
        public static final int PRIMITIVE_QUICK_FALL = 6;
        public static final int PRIMITIVE_TICK = 7;
        public static final int PRIMITIVE_LOW_TICK = 8;

        private final List<long[]> mSegments = new ArrayList<>();

        Composition()
        {
        }

        public Composition addPrimitive(int primitiveId)
        {
            return addPrimitive(primitiveId, 1f, 0);
        }

        public Composition addPrimitive(int primitiveId, float scale)
        {
            return addPrimitive(primitiveId, scale, 0);
        }

        public Composition addPrimitive(int primitiveId, float scale, int delay)
        {
            if (primitiveId < PRIMITIVE_CLICK || primitiveId > PRIMITIVE_LOW_TICK)
            {
                throw new IllegalArgumentException("primitiveId must be a valid primitive, got " + primitiveId);
            }
            if (scale < 0f || scale > 1f) throw new IllegalArgumentException("scale must be between 0 and 1");
            if (delay < 0) throw new IllegalArgumentException("delay must be non-negative");

            mSegments.add(new long[] {delay, 10, Math.max(1, Math.round(scale * MAX_AMPLITUDE))});
            return this;
        }

        public VibrationEffect compose()
        {
            if (mSegments.isEmpty()) throw new IllegalStateException("Composition must have at least one element to compose.");

            long[] timings = new long[mSegments.size() * 2];
            int[] amplitudes = new int[timings.length];
            for (int index = 0; index < mSegments.size(); index++)
            {
                long[] segment = mSegments.get(index);
                timings[index * 2] = segment[0];
                amplitudes[index * 2] = 0;
                timings[index * 2 + 1] = segment[1];
                amplitudes[index * 2 + 1] = (int)segment[2];
            }
            return new Waveform(timings, amplitudes, -1);
        }
    }
}
