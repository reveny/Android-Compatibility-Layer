package android.media;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import java.util.Arrays;
import java.util.Objects;

public final class VolumeShaper implements AutoCloseable
{
    private Configuration mConfiguration;
    private boolean mReversed;
    private boolean mRunning;
    private boolean mClosed;
    private float mPosition;
    private long mStartTimeMillis;

    VolumeShaper(Configuration configuration)
    {
        mConfiguration = Objects.requireNonNull(configuration);
    }

    public synchronized void apply(Operation operation)
    {
        Objects.requireNonNull(operation);

        if (mClosed) throw new IllegalStateException("VolumeShaper is closed");

        mPosition = currentPosition();
        mReversed = operation.isReverse();
        mRunning = true;
        mStartTimeMillis = SystemClock.uptimeMillis();
    }

    @Override
    public synchronized void close()
    {
        mPosition = currentPosition();
        mRunning = false;
        mClosed = true;
    }

    @Override
    protected void finalize()
    {
        close();
    }

    public synchronized float getVolume()
    {
        return mConfiguration.volumeAt(currentPosition());
    }

    public synchronized void replace(Configuration configuration, Operation operation, boolean join)
    {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(operation);

        if (mClosed) throw new IllegalStateException("VolumeShaper is closed");

        float currentVolume = getVolume();
        mConfiguration = configuration;
        mPosition = join ? configuration.positionForVolume(currentVolume) : 0.0f;
        mReversed = operation.isReverse();
        mRunning = true;
        mStartTimeMillis = SystemClock.uptimeMillis();
    }

    private float currentPosition()
    {
        if (!mRunning) return mPosition;

        long duration = Math.max(1, mConfiguration.getDuration());
        float elapsed = (SystemClock.uptimeMillis() - mStartTimeMillis) / (float)duration;
        float position = mReversed ? mPosition - elapsed : mPosition + elapsed;

        return Math.max(0.0f, Math.min(1.0f, position));
    }

    public static final class Operation implements Parcelable
    {
        private static final int FLAG_REVERSE = 1;

        public static final Operation PLAY = new Operation(0);
        public static final Operation REVERSE = new Operation(FLAG_REVERSE);

        public static final Parcelable.Creator<Operation> CREATOR = new Parcelable.Creator<Operation>()
        {
            @Override
            public Operation createFromParcel(Parcel source)
            {
                return new Operation(source.readInt());
            }

            @Override
            public Operation[] newArray(int size)
            {
                return new Operation[size];
            }
        };

        private final int mFlags;

        Operation(int flags)
        {
            mFlags = flags;
        }

        boolean isReverse()
        {
            return (mFlags & FLAG_REVERSE) != 0;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) return true;
            if (!(object instanceof Operation)) return false;

            return mFlags == ((Operation)object).mFlags;
        }

        @Override
        public int hashCode()
        {
            return Integer.hashCode(mFlags);
        }

        @Override
        public String toString()
        {
            return "VolumeShaper.Operation{ mFlags = 0x" + Integer.toHexString(mFlags).toUpperCase() + " }";
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
            parcel.writeInt(mFlags);
        }
    }

    public static final class Configuration implements Parcelable
    {
        public static final int INTERPOLATOR_TYPE_CUBIC = 2;
        public static final int INTERPOLATOR_TYPE_CUBIC_MONOTONIC = 3;
        public static final int INTERPOLATOR_TYPE_LINEAR = 1;
        public static final int INTERPOLATOR_TYPE_STEP = 0;

        private static final int MAXIMUM_CURVE_POINTS = 16;
        private static final long DEFAULT_DURATION_MILLIS = 1000;

        public static final Configuration LINEAR_RAMP =
            new Configuration(INTERPOLATOR_TYPE_LINEAR, DEFAULT_DURATION_MILLIS, new float[] {0.0f, 1.0f}, new float[] {0.0f, 1.0f});
        public static final Configuration CUBIC_RAMP =
            new Configuration(INTERPOLATOR_TYPE_CUBIC, DEFAULT_DURATION_MILLIS, new float[] {0.0f, 1.0f}, new float[] {0.0f, 1.0f});
        public static final Configuration SINE_RAMP;
        public static final Configuration SCURVE_RAMP;

        static
        {
            float[] times = new float[MAXIMUM_CURVE_POINTS];
            float[] sineVolumes = new float[MAXIMUM_CURVE_POINTS];
            float[] scurveVolumes = new float[MAXIMUM_CURVE_POINTS];

            for (int index = 0; index < MAXIMUM_CURVE_POINTS; index++)
            {
                times[index] = (float)index / (MAXIMUM_CURVE_POINTS - 1);
                float sine = (float)Math.sin(times[index] * Math.PI / 2.0);
                sineVolumes[index] = sine;
                scurveVolumes[index] = sine * sine;
            }

            SINE_RAMP = new Configuration(INTERPOLATOR_TYPE_CUBIC, DEFAULT_DURATION_MILLIS, times, sineVolumes);
            SCURVE_RAMP = new Configuration(INTERPOLATOR_TYPE_CUBIC, DEFAULT_DURATION_MILLIS, times, scurveVolumes);
        }

        public static final Parcelable.Creator<Configuration> CREATOR = new Parcelable.Creator<Configuration>()
        {
            @Override
            public Configuration createFromParcel(Parcel source)
            {
                int interpolatorType = source.readInt();
                long durationMillis = source.readLong();
                float[] times = source.createFloatArray();
                float[] volumes = source.createFloatArray();
                return new Configuration(interpolatorType, durationMillis, times, volumes);
            }

            @Override
            public Configuration[] newArray(int size)
            {
                return new Configuration[size];
            }
        };

        private final int mInterpolatorType;
        private final long mDurationMillis;
        private final float[] mTimes;
        private final float[] mVolumes;

        private Configuration(int interpolatorType, long durationMillis, float[] times, float[] volumes)
        {
            mInterpolatorType = interpolatorType;
            mDurationMillis = durationMillis;
            mTimes = times;
            mVolumes = volumes;
        }

        public static int getMaximumCurvePoints()
        {
            return MAXIMUM_CURVE_POINTS;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) return true;
            if (!(object instanceof Configuration)) return false;

            Configuration other = (Configuration)object;
            return mInterpolatorType == other.mInterpolatorType && mDurationMillis == other.mDurationMillis
                && Arrays.equals(mTimes, other.mTimes) && Arrays.equals(mVolumes, other.mVolumes);
        }

        public long getDuration()
        {
            return mDurationMillis;
        }

        public int getInterpolatorType()
        {
            return mInterpolatorType;
        }

        public float[] getTimes()
        {
            return mTimes.clone();
        }

        public float[] getVolumes()
        {
            return mVolumes.clone();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mInterpolatorType, mDurationMillis, Arrays.hashCode(mTimes), Arrays.hashCode(mVolumes));
        }

        @Override
        public String toString()
        {
            return "VolumeShaper.Configuration{ mInterpolatorType = " + mInterpolatorType + ", mDurationMs = " + mDurationMillis
                + ", mTimes[] = " + Arrays.toString(mTimes) + ", mVolumes[] = " + Arrays.toString(mVolumes) + " }";
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags)
        {
            parcel.writeInt(mInterpolatorType);
            parcel.writeLong(mDurationMillis);
            parcel.writeFloatArray(mTimes);
            parcel.writeFloatArray(mVolumes);
        }

        float volumeAt(float position)
        {
            int last = mTimes.length - 1;

            if (position <= mTimes[0]) return mVolumes[0];
            if (position >= mTimes[last]) return mVolumes[last];

            int segment = 0;
            while (segment < last - 1 && position >= mTimes[segment + 1])
            {
                segment++;
            }

            float startTime = mTimes[segment];
            float endTime = mTimes[segment + 1];
            float startVolume = mVolumes[segment];
            float endVolume = mVolumes[segment + 1];
            float width = endTime - startTime;
            float fraction = (position - startTime) / width;

            switch (mInterpolatorType)
            {
                case INTERPOLATOR_TYPE_STEP:
                    return startVolume;
                case INTERPOLATOR_TYPE_LINEAR:
                    return startVolume + (endVolume - startVolume) * fraction;
                default:
                    float startSlope = slopeAt(segment) * width;
                    float endSlope = slopeAt(segment + 1) * width;
                    float square = fraction * fraction;
                    float cube = square * fraction;
                    float value = (2 * cube - 3 * square + 1) * startVolume + (cube - 2 * square + fraction) * startSlope
                        + (-2 * cube + 3 * square) * endVolume + (cube - square) * endSlope;

                    if (mInterpolatorType == INTERPOLATOR_TYPE_CUBIC_MONOTONIC)
                    {
                        value = Math.max(Math.min(startVolume, endVolume), Math.min(Math.max(startVolume, endVolume), value));
                    }

                    return value;
            }
        }

        private float slopeAt(int index)
        {
            int last = mTimes.length - 1;

            if (last < 1) return 0.0f;

            if (index == 0) return (mVolumes[1] - mVolumes[0]) / (mTimes[1] - mTimes[0]);
            if (index == last) return (mVolumes[last] - mVolumes[last - 1]) / (mTimes[last] - mTimes[last - 1]);

            float before = (mVolumes[index] - mVolumes[index - 1]) / (mTimes[index] - mTimes[index - 1]);
            float after = (mVolumes[index + 1] - mVolumes[index]) / (mTimes[index + 1] - mTimes[index]);

            if (mInterpolatorType == INTERPOLATOR_TYPE_CUBIC_MONOTONIC && before * after <= 0) return 0.0f;

            return (before + after) / 2.0f;
        }

        float positionForVolume(float volume)
        {
            int last = mTimes.length - 1;
            float bestPosition = 0.0f;
            float bestDistance = Float.MAX_VALUE;

            for (int step = 0; step <= 100; step++)
            {
                float position = mTimes[0] + (mTimes[last] - mTimes[0]) * step / 100.0f;
                float distance = Math.abs(volumeAt(position) - volume);

                if (distance < bestDistance)
                {
                    bestDistance = distance;
                    bestPosition = position;
                }
            }

            return bestPosition;
        }

        public static final class Builder
        {
            private int mInterpolatorType = INTERPOLATOR_TYPE_CUBIC;
            private long mDurationMillis = DEFAULT_DURATION_MILLIS;
            private float[] mTimes;
            private float[] mVolumes;

            public Builder()
            {
            }

            public Builder(Configuration configuration)
            {
                mInterpolatorType = configuration.mInterpolatorType;
                mDurationMillis = configuration.mDurationMillis;
                mTimes = configuration.mTimes.clone();
                mVolumes = configuration.mVolumes.clone();
            }

            public Configuration build()
            {
                checkCurveForErrorsAndThrowException(mTimes, mVolumes);
                return new Configuration(mInterpolatorType, mDurationMillis, mTimes.clone(), mVolumes.clone());
            }

            public Builder invertVolumes()
            {
                checkCurveForErrorsAndThrowException(mTimes, mVolumes);

                float minimum = mVolumes[0];
                float maximum = mVolumes[0];

                for (float volume : mVolumes)
                {
                    minimum = Math.min(minimum, volume);
                    maximum = Math.max(maximum, volume);
                }

                for (int index = 0; index < mVolumes.length; index++)
                {
                    mVolumes[index] = maximum + minimum - mVolumes[index];
                }

                return this;
            }

            public Builder reflectTimes()
            {
                checkCurveForErrorsAndThrowException(mTimes, mVolumes);

                int length = mTimes.length;
                float[] times = new float[length];
                float[] volumes = new float[length];

                for (int index = 0; index < length; index++)
                {
                    times[index] = 1.0f - mTimes[length - 1 - index];
                    volumes[index] = mVolumes[length - 1 - index];
                }

                mTimes = times;
                mVolumes = volumes;
                return this;
            }

            public Builder scaleToEndVolume(float volume)
            {
                checkCurveForErrorsAndThrowException(mTimes, mVolumes);
                checkVolume(volume);

                int last = mVolumes.length - 1;
                float startVolume = mVolumes[0];
                float endVolume = mVolumes[last];

                if (startVolume == endVolume)
                {
                    float offset = volume - startVolume;
                    for (int index = 0; index <= last; index++)
                    {
                        mVolumes[index] = mVolumes[index] + offset * mTimes[index];
                    }
                }
                else
                {
                    float scale = (volume - startVolume) / (endVolume - startVolume);
                    for (int index = 0; index <= last; index++)
                    {
                        mVolumes[index] = scale * (mVolumes[index] - startVolume) + startVolume;
                    }
                }

                clampVolumes();
                return this;
            }

            public Builder scaleToStartVolume(float volume)
            {
                checkCurveForErrorsAndThrowException(mTimes, mVolumes);
                checkVolume(volume);

                int last = mVolumes.length - 1;
                float startVolume = mVolumes[0];
                float endVolume = mVolumes[last];

                if (startVolume == endVolume)
                {
                    float offset = volume - startVolume;
                    for (int index = 0; index <= last; index++)
                    {
                        mVolumes[index] = mVolumes[index] + offset * (1.0f - mTimes[index]);
                    }
                }
                else
                {
                    float scale = (volume - endVolume) / (startVolume - endVolume);
                    for (int index = 0; index <= last; index++)
                    {
                        mVolumes[index] = scale * (mVolumes[index] - endVolume) + endVolume;
                    }
                }

                clampVolumes();
                return this;
            }

            public Builder setCurve(float[] times, float[] volumes)
            {
                checkCurveForErrorsAndThrowException(times, volumes);

                mTimes = times.clone();
                mVolumes = volumes.clone();
                return this;
            }

            public Builder setDuration(long durationMillis)
            {
                if (durationMillis <= 0) throw new IllegalArgumentException("duration: " + durationMillis + " not positive");

                mDurationMillis = durationMillis;
                return this;
            }

            public Builder setInterpolatorType(int interpolatorType)
            {
                if (interpolatorType < INTERPOLATOR_TYPE_STEP || interpolatorType > INTERPOLATOR_TYPE_CUBIC_MONOTONIC)
                {
                    throw new IllegalArgumentException("invalid interpolatorType: " + interpolatorType);
                }

                mInterpolatorType = interpolatorType;
                return this;
            }

            private void clampVolumes()
            {
                for (int index = 0; index < mVolumes.length; index++)
                {
                    mVolumes[index] = Math.max(0.0f, Math.min(1.0f, mVolumes[index]));
                }
            }

            private static void checkVolume(float volume)
            {
                if (!(volume >= 0.0f && volume <= 1.0f)) throw new IllegalArgumentException("volume out of range: " + volume);
            }

            private static void checkCurveForErrorsAndThrowException(float[] times, float[] volumes)
            {
                if (times == null) throw new IllegalStateException("times array must be non-null");
                if (volumes == null) throw new IllegalStateException("volumes array must be non-null");
                if (times.length != volumes.length) throw new IllegalStateException("array length must match");
                if (times.length < 2) throw new IllegalStateException("array length must be at least 2");
                if (times.length > MAXIMUM_CURVE_POINTS)
                {
                    throw new IllegalStateException("array length must be no larger than " + MAXIMUM_CURVE_POINTS);
                }

                if (times[0] != 0.0f) throw new IllegalStateException("times must start at 0.f");
                if (times[times.length - 1] != 1.0f) throw new IllegalStateException("times must end at 1.f");

                for (int index = 1; index < times.length; index++)
                {
                    if (!(times[index] > times[index - 1])) throw new IllegalStateException("times not monotonic increasing");
                }

                for (float volume : volumes)
                {
                    if (!(volume >= 0.0f && volume <= 1.0f)) throw new IllegalStateException("volumes out of range");
                }
            }
        }
    }
}
