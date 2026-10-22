package android.hardware.camera2.params;

import android.util.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MandatoryStreamCombination
{
    private final String mDescription;
    private final boolean mIsReprocessable;
    private final List<MandatoryStreamInformation> mStreamsInformation;

    MandatoryStreamCombination(List<MandatoryStreamInformation> streamsInformation, String description, boolean isReprocessable)
    {
        if (streamsInformation.isEmpty()) throw new IllegalArgumentException("Empty stream information");
        mStreamsInformation = new ArrayList<>(streamsInformation);
        mDescription = description;
        mIsReprocessable = isReprocessable;
    }

    public CharSequence getDescription()
    {
        return mDescription;
    }

    public boolean isReprocessable()
    {
        return mIsReprocessable;
    }

    public List<MandatoryStreamInformation> getStreamsInformation()
    {
        return Collections.unmodifiableList(mStreamsInformation);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof MandatoryStreamCombination)) return false;

        MandatoryStreamCombination combination = (MandatoryStreamCombination)other;
        return mIsReprocessable == combination.mIsReprocessable && Objects.equals(mDescription, combination.mDescription)
                && mStreamsInformation.equals(combination.mStreamsInformation);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mIsReprocessable, mDescription, mStreamsInformation);
    }

    public static final class MandatoryStreamInformation
    {
        private final boolean mIsInput;
        private final boolean mIsUltraHighResolution;
        private final boolean mIsMaximumSize;
        private final boolean mIs10BitCapable;
        private final int mFormat;
        private final long mStreamUseCase;
        private final List<Size> mAvailableSizes;

        MandatoryStreamInformation(List<Size> availableSizes, int format, boolean isMaximumSize, boolean isInput,
                boolean isUltraHighResolution, boolean is10BitCapable, long streamUseCase)
        {
            if (availableSizes.isEmpty()) throw new IllegalArgumentException("No available sizes");
            mAvailableSizes = new ArrayList<>(availableSizes);
            mFormat = format;
            mIsMaximumSize = isMaximumSize;
            mIsInput = isInput;
            mIsUltraHighResolution = isUltraHighResolution;
            mIs10BitCapable = is10BitCapable;
            mStreamUseCase = streamUseCase;
        }

        public boolean isInput()
        {
            return mIsInput;
        }

        public boolean isUltraHighResolution()
        {
            return mIsUltraHighResolution;
        }

        public boolean isMaximumSize()
        {
            return mIsMaximumSize;
        }

        public boolean is10BitCapable()
        {
            return mIs10BitCapable;
        }

        public List<Size> getAvailableSizes()
        {
            return Collections.unmodifiableList(mAvailableSizes);
        }

        public int getFormat()
        {
            return mFormat;
        }

        public int get10BitFormat()
        {
            if (!mIs10BitCapable) throw new UnsupportedOperationException("10-bit output is not supported!");
            return mFormat;
        }

        public long getStreamUseCase()
        {
            return mStreamUseCase;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof MandatoryStreamInformation)) return false;

            MandatoryStreamInformation information = (MandatoryStreamInformation)other;
            return mFormat == information.mFormat && mIsInput == information.mIsInput
                    && mIsUltraHighResolution == information.mIsUltraHighResolution && mIsMaximumSize == information.mIsMaximumSize
                    && mIs10BitCapable == information.mIs10BitCapable && mStreamUseCase == information.mStreamUseCase
                    && mAvailableSizes.equals(information.mAvailableSizes);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mFormat, mIsInput, mIsUltraHighResolution, mIsMaximumSize, mIs10BitCapable, mStreamUseCase,
                    mAvailableSizes);
        }
    }
}
