package android.os;

import android.util.SparseArray;

public abstract class CombinedVibration implements Parcelable
{
    public static final Parcelable.Creator<CombinedVibration> CREATOR = new Parcelable.Creator<CombinedVibration>()
    {
        @Override
        public CombinedVibration createFromParcel(Parcel source)
        {
            return new Mono(VibrationEffect.CREATOR.createFromParcel(source));
        }

        @Override
        public CombinedVibration[] newArray(int size)
        {
            return new CombinedVibration[size];
        }
    };

    CombinedVibration()
    {
    }

    public static CombinedVibration createParallel(VibrationEffect effect)
    {
        if (effect == null) throw new NullPointerException("effect");
        return new Mono(effect);
    }

    public static ParallelCombination startParallel()
    {
        return new ParallelCombination();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    static final class Mono extends CombinedVibration
    {
        private final VibrationEffect mEffect;

        Mono(VibrationEffect effect)
        {
            mEffect = effect;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            mEffect.writeToParcel(dest, flags);
        }
    }

    static final class Stereo extends CombinedVibration
    {
        private final SparseArray<VibrationEffect> mEffects;

        Stereo(SparseArray<VibrationEffect> effects)
        {
            mEffects = effects;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            if (mEffects.size() == 0) return;
            mEffects.valueAt(0).writeToParcel(dest, flags);
        }
    }

    public static final class ParallelCombination
    {
        private final SparseArray<VibrationEffect> mEffects = new SparseArray<>();

        ParallelCombination()
        {
        }

        public ParallelCombination addVibrator(int vibratorId, VibrationEffect effect)
        {
            mEffects.put(vibratorId, effect);
            return this;
        }

        public CombinedVibration combine()
        {
            if (mEffects.size() == 0) throw new IllegalStateException("Combination must have at least one element to combine.");
            return new Stereo(mEffects.clone());
        }
    }
}
