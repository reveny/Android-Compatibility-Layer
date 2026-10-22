package android.hardware;

import android.content.Context;

public final class ConsumerIrManager
{
    public ConsumerIrManager(Context context)
    {
    }

    public boolean hasIrEmitter()
    {
        return false;
    }

    public void transmit(int carrierFrequency, int[] pattern)
    {
        if (pattern == null) throw new NullPointerException("pattern");
    }

    public CarrierFrequencyRange[] getCarrierFrequencies()
    {
        return null;
    }

    public final class CarrierFrequencyRange
    {
        private final int mMinFrequency;
        private final int mMaxFrequency;

        public CarrierFrequencyRange(int min, int max)
        {
            mMinFrequency = min;
            mMaxFrequency = max;
        }

        public int getMinFrequency()
        {
            return mMinFrequency;
        }

        public int getMaxFrequency()
        {
            return mMaxFrequency;
        }
    }
}
