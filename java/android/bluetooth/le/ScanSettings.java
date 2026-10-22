package android.bluetooth.le;

import android.os.Parcel;
import android.os.Parcelable;

public final class ScanSettings implements Parcelable
{
    public static final long AUTO_BATCH_MIN_REPORT_DELAY_MILLIS = 600000L;
    public static final int CALLBACK_TYPE_ALL_MATCHES = 1;
    public static final int CALLBACK_TYPE_FIRST_MATCH = 2;
    public static final int CALLBACK_TYPE_MATCH_LOST = 4;
    public static final int CALLBACK_TYPE_ALL_MATCHES_AUTO_BATCH = 8;
    public static final int MATCH_MODE_AGGRESSIVE = 1;
    public static final int MATCH_MODE_STICKY = 2;
    public static final int MATCH_NUM_ONE_ADVERTISEMENT = 1;
    public static final int MATCH_NUM_FEW_ADVERTISEMENT = 2;
    public static final int MATCH_NUM_MAX_ADVERTISEMENT = 3;
    public static final int PHY_LE_ALL_SUPPORTED = 255;
    public static final int SCAN_MODE_OPPORTUNISTIC = -1;
    public static final int SCAN_MODE_LOW_POWER = 0;
    public static final int SCAN_MODE_BALANCED = 1;
    public static final int SCAN_MODE_LOW_LATENCY = 2;

    private static final int SCAN_RESULT_TYPE_FULL = 0;

    private final int mScanMode;
    private final int mCallbackType;
    private final int mScanResultType;
    private final long mReportDelayMillis;
    private final int mMatchMode;
    private final int mNumOfMatchesPerFilter;
    private final boolean mLegacy;
    private final int mPhy;

    private ScanSettings(int scanMode, int callbackType, int scanResultType, long reportDelayMillis, int matchMode,
                         int numOfMatchesPerFilter, boolean legacy, int phy)
    {
        mScanMode = scanMode;
        mCallbackType = callbackType;
        mScanResultType = scanResultType;
        mReportDelayMillis = reportDelayMillis;
        mMatchMode = matchMode;
        mNumOfMatchesPerFilter = numOfMatchesPerFilter;
        mLegacy = legacy;
        mPhy = phy;
    }

    public int getScanMode()
    {
        return mScanMode;
    }

    public int getCallbackType()
    {
        return mCallbackType;
    }

    public int getScanResultType()
    {
        return mScanResultType;
    }

    public boolean getLegacy()
    {
        return mLegacy;
    }

    public int getPhy()
    {
        return mPhy;
    }

    public long getReportDelayMillis()
    {
        return mReportDelayMillis;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mScanMode);
        dest.writeInt(mCallbackType);
        dest.writeInt(mScanResultType);
        dest.writeLong(mReportDelayMillis);
        dest.writeInt(mMatchMode);
        dest.writeInt(mNumOfMatchesPerFilter);
        dest.writeInt(mLegacy ? 1 : 0);
        dest.writeInt(mPhy);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<ScanSettings> CREATOR = new Creator<ScanSettings>()
    {
        @Override
        public ScanSettings createFromParcel(Parcel in)
        {
            return new ScanSettings(in.readInt(), in.readInt(), in.readInt(), in.readLong(), in.readInt(), in.readInt(), in.readInt() != 0,
                                    in.readInt());
        }

        @Override
        public ScanSettings[] newArray(int size)
        {
            return new ScanSettings[size];
        }
    };

    public static final class Builder
    {
        private int mScanMode = SCAN_MODE_LOW_POWER;
        private int mCallbackType = CALLBACK_TYPE_ALL_MATCHES;
        private long mReportDelayMillis = 0;
        private int mMatchMode = MATCH_MODE_AGGRESSIVE;
        private int mNumOfMatchesPerFilter = MATCH_NUM_MAX_ADVERTISEMENT;
        private boolean mLegacy = true;
        private int mPhy = PHY_LE_ALL_SUPPORTED;

        public Builder()
        {
        }

        public Builder setScanMode(int scanMode)
        {
            if (scanMode < SCAN_MODE_OPPORTUNISTIC || scanMode > SCAN_MODE_LOW_LATENCY)
            {
                throw new IllegalArgumentException("invalid scan mode " + scanMode);
            }

            mScanMode = scanMode;
            return this;
        }

        public Builder setCallbackType(int callbackType)
        {
            boolean valid = callbackType == CALLBACK_TYPE_ALL_MATCHES || callbackType == CALLBACK_TYPE_ALL_MATCHES_AUTO_BATCH
                || (callbackType & ~(CALLBACK_TYPE_FIRST_MATCH | CALLBACK_TYPE_MATCH_LOST)) == 0 && callbackType != 0;

            if (!valid) throw new IllegalArgumentException("invalid callback type - " + callbackType);

            mCallbackType = callbackType;
            return this;
        }

        public Builder setReportDelay(long reportDelayMillis)
        {
            if (reportDelayMillis < 0) throw new IllegalArgumentException("reportDelay must be > 0");

            mReportDelayMillis = reportDelayMillis;
            return this;
        }

        public Builder setNumOfMatches(int numOfMatches)
        {
            if (numOfMatches < MATCH_NUM_ONE_ADVERTISEMENT || numOfMatches > MATCH_NUM_MAX_ADVERTISEMENT)
            {
                throw new IllegalArgumentException("invalid numOfMatches " + numOfMatches);
            }

            mNumOfMatchesPerFilter = numOfMatches;
            return this;
        }

        public Builder setMatchMode(int matchMode)
        {
            if (matchMode < MATCH_MODE_AGGRESSIVE || matchMode > MATCH_MODE_STICKY)
            {
                throw new IllegalArgumentException("invalid matchMode " + matchMode);
            }

            mMatchMode = matchMode;
            return this;
        }

        public Builder setLegacy(boolean legacy)
        {
            mLegacy = legacy;
            return this;
        }

        public Builder setPhy(int phy)
        {
            mPhy = phy;
            return this;
        }

        public ScanSettings build()
        {
            if (mCallbackType == CALLBACK_TYPE_ALL_MATCHES_AUTO_BATCH && mReportDelayMillis == 0)
            {
                mReportDelayMillis = AUTO_BATCH_MIN_REPORT_DELAY_MILLIS;
            }

            return new ScanSettings(mScanMode, mCallbackType, SCAN_RESULT_TYPE_FULL, mReportDelayMillis, mMatchMode, mNumOfMatchesPerFilter,
                                    mLegacy, mPhy);
        }
    }
}
