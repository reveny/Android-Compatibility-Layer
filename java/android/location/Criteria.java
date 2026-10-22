package android.location;

import android.os.Parcel;
import android.os.Parcelable;

public class Criteria implements Parcelable
{
    public static final int NO_REQUIREMENT = 0;
    public static final int POWER_LOW = 1;
    public static final int POWER_MEDIUM = 2;
    public static final int POWER_HIGH = 3;
    public static final int ACCURACY_FINE = 1;
    public static final int ACCURACY_COARSE = 2;
    public static final int ACCURACY_LOW = 1;
    public static final int ACCURACY_MEDIUM = 2;
    public static final int ACCURACY_HIGH = 3;

    private int mHorizontalAccuracy = NO_REQUIREMENT;
    private int mVerticalAccuracy = NO_REQUIREMENT;
    private int mSpeedAccuracy = NO_REQUIREMENT;
    private int mBearingAccuracy = NO_REQUIREMENT;
    private int mPowerRequirement = NO_REQUIREMENT;
    private boolean mAltitudeRequired;
    private boolean mBearingRequired;
    private boolean mSpeedRequired;
    private boolean mCostAllowed;

    public Criteria()
    {
    }

    public Criteria(Criteria criteria)
    {
        mHorizontalAccuracy = criteria.mHorizontalAccuracy;
        mVerticalAccuracy = criteria.mVerticalAccuracy;
        mSpeedAccuracy = criteria.mSpeedAccuracy;
        mBearingAccuracy = criteria.mBearingAccuracy;
        mPowerRequirement = criteria.mPowerRequirement;
        mAltitudeRequired = criteria.mAltitudeRequired;
        mBearingRequired = criteria.mBearingRequired;
        mSpeedRequired = criteria.mSpeedRequired;
        mCostAllowed = criteria.mCostAllowed;
    }

    public void setHorizontalAccuracy(int accuracy)
    {
        if (accuracy < NO_REQUIREMENT || accuracy > ACCURACY_HIGH) throw new IllegalArgumentException("accuracy=" + accuracy);

        mHorizontalAccuracy = accuracy;
    }

    public int getHorizontalAccuracy()
    {
        return mHorizontalAccuracy;
    }

    public void setVerticalAccuracy(int accuracy)
    {
        if (accuracy < NO_REQUIREMENT || accuracy > ACCURACY_HIGH) throw new IllegalArgumentException("accuracy=" + accuracy);

        mVerticalAccuracy = accuracy;
    }

    public int getVerticalAccuracy()
    {
        return mVerticalAccuracy;
    }

    public void setSpeedAccuracy(int accuracy)
    {
        if (accuracy < NO_REQUIREMENT || accuracy > ACCURACY_HIGH) throw new IllegalArgumentException("accuracy=" + accuracy);

        mSpeedAccuracy = accuracy;
    }

    public int getSpeedAccuracy()
    {
        return mSpeedAccuracy;
    }

    public void setBearingAccuracy(int accuracy)
    {
        if (accuracy < NO_REQUIREMENT || accuracy > ACCURACY_HIGH) throw new IllegalArgumentException("accuracy=" + accuracy);

        mBearingAccuracy = accuracy;
    }

    public int getBearingAccuracy()
    {
        return mBearingAccuracy;
    }

    public void setAccuracy(int accuracy)
    {
        if (accuracy < NO_REQUIREMENT || accuracy > ACCURACY_COARSE) throw new IllegalArgumentException("accuracy=" + accuracy);

        if (accuracy == ACCURACY_FINE) mHorizontalAccuracy = ACCURACY_HIGH;
        else mHorizontalAccuracy = ACCURACY_LOW;
    }

    public int getAccuracy()
    {
        if (mHorizontalAccuracy >= ACCURACY_HIGH) return ACCURACY_FINE;

        return ACCURACY_COARSE;
    }

    public void setPowerRequirement(int level)
    {
        if (level < NO_REQUIREMENT || level > POWER_HIGH) throw new IllegalArgumentException("level=" + level);

        mPowerRequirement = level;
    }

    public int getPowerRequirement()
    {
        return mPowerRequirement;
    }

    public void setCostAllowed(boolean costAllowed)
    {
        mCostAllowed = costAllowed;
    }

    public boolean isCostAllowed()
    {
        return mCostAllowed;
    }

    public void setAltitudeRequired(boolean altitudeRequired)
    {
        mAltitudeRequired = altitudeRequired;
    }

    public boolean isAltitudeRequired()
    {
        return mAltitudeRequired;
    }

    public void setSpeedRequired(boolean speedRequired)
    {
        mSpeedRequired = speedRequired;
    }

    public boolean isSpeedRequired()
    {
        return mSpeedRequired;
    }

    public void setBearingRequired(boolean bearingRequired)
    {
        mBearingRequired = bearingRequired;
    }

    public boolean isBearingRequired()
    {
        return mBearingRequired;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeInt(mHorizontalAccuracy);
        parcel.writeInt(mVerticalAccuracy);
        parcel.writeInt(mSpeedAccuracy);
        parcel.writeInt(mBearingAccuracy);
        parcel.writeInt(mPowerRequirement);
        parcel.writeInt(mAltitudeRequired ? 1 : 0);
        parcel.writeInt(mBearingRequired ? 1 : 0);
        parcel.writeInt(mSpeedRequired ? 1 : 0);
        parcel.writeInt(mCostAllowed ? 1 : 0);
    }

    @Override
    public String toString()
    {
        return "Criteria[power=" + mPowerRequirement + ", accuracy=" + mHorizontalAccuracy + "]";
    }

    public static final Creator<Criteria> CREATOR = new Creator<Criteria>()
    {
        @Override
        public Criteria createFromParcel(Parcel parcel)
        {
            Criteria criteria = new Criteria();
            criteria.mHorizontalAccuracy = parcel.readInt();
            criteria.mVerticalAccuracy = parcel.readInt();
            criteria.mSpeedAccuracy = parcel.readInt();
            criteria.mBearingAccuracy = parcel.readInt();
            criteria.mPowerRequirement = parcel.readInt();
            criteria.mAltitudeRequired = parcel.readInt() != 0;
            criteria.mBearingRequired = parcel.readInt() != 0;
            criteria.mSpeedRequired = parcel.readInt() != 0;
            criteria.mCostAllowed = parcel.readInt() != 0;
            return criteria;
        }

        @Override
        public Criteria[] newArray(int size)
        {
            return new Criteria[size];
        }
    };
}
