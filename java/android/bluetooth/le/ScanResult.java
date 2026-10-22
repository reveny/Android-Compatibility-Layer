package android.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public final class ScanResult implements Parcelable
{
    public static final int DATA_COMPLETE = 0x00;
    public static final int DATA_TRUNCATED = 0x02;
    public static final int PHY_UNUSED = 0x00;
    public static final int SID_NOT_PRESENT = 0xFF;
    public static final int TX_POWER_NOT_PRESENT = 0x7F;
    public static final int PERIODIC_INTERVAL_NOT_PRESENT = 0x00;

    private static final int ET_LEGACY_MASK = 0x10;
    private static final int ET_CONNECTABLE_MASK = 0x01;

    private final BluetoothDevice mDevice;
    private final ScanRecord mScanRecord;
    private final int mRssi;
    private final long mTimestampNanos;
    private final int mEventType;
    private final int mPrimaryPhy;
    private final int mSecondaryPhy;
    private final int mAdvertisingSid;
    private final int mTxPower;
    private final int mPeriodicAdvertisingInterval;

    @Deprecated
    public ScanResult(BluetoothDevice device, ScanRecord scanRecord, int rssi, long timestampNanos)
    {
        this(device, (DATA_COMPLETE << 5) | ET_LEGACY_MASK | ET_CONNECTABLE_MASK, BluetoothDevice.PHY_LE_1M, PHY_UNUSED, SID_NOT_PRESENT,
             TX_POWER_NOT_PRESENT, rssi, PERIODIC_INTERVAL_NOT_PRESENT, scanRecord, timestampNanos);
    }

    public ScanResult(BluetoothDevice device, int eventType, int primaryPhy, int secondaryPhy, int advertisingSid, int txPower, int rssi,
                      int periodicAdvertisingInterval, ScanRecord scanRecord, long timestampNanos)
    {
        mDevice = device;
        mEventType = eventType;
        mPrimaryPhy = primaryPhy;
        mSecondaryPhy = secondaryPhy;
        mAdvertisingSid = advertisingSid;
        mTxPower = txPower;
        mRssi = rssi;
        mPeriodicAdvertisingInterval = periodicAdvertisingInterval;
        mScanRecord = scanRecord;
        mTimestampNanos = timestampNanos;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(mDevice, flags);
        dest.writeInt(mRssi);
        dest.writeLong(mTimestampNanos);
        dest.writeInt(mEventType);
        dest.writeInt(mPrimaryPhy);
        dest.writeInt(mSecondaryPhy);
        dest.writeInt(mAdvertisingSid);
        dest.writeInt(mTxPower);
        dest.writeInt(mPeriodicAdvertisingInterval);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public BluetoothDevice getDevice()
    {
        return mDevice;
    }

    public ScanRecord getScanRecord()
    {
        return mScanRecord;
    }

    public int getRssi()
    {
        return mRssi;
    }

    public long getTimestampNanos()
    {
        return mTimestampNanos;
    }

    public boolean isLegacy()
    {
        return (mEventType & ET_LEGACY_MASK) != 0;
    }

    public boolean isConnectable()
    {
        return (mEventType & ET_CONNECTABLE_MASK) != 0;
    }

    public int getDataStatus()
    {
        return (mEventType >> 5) & 0x03;
    }

    public int getPrimaryPhy()
    {
        return mPrimaryPhy;
    }

    public int getSecondaryPhy()
    {
        return mSecondaryPhy;
    }

    public int getAdvertisingSid()
    {
        return mAdvertisingSid;
    }

    public int getTxPower()
    {
        return mTxPower;
    }

    public int getPeriodicAdvertisingInterval()
    {
        return mPeriodicAdvertisingInterval;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mDevice, mRssi, mScanRecord, mTimestampNanos, mEventType, mPrimaryPhy, mSecondaryPhy, mAdvertisingSid, mTxPower,
                            mPeriodicAdvertisingInterval);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof ScanResult)) return false;

        ScanResult result = (ScanResult)other;
        return Objects.equals(mDevice, result.mDevice) && mRssi == result.mRssi && Objects.equals(mScanRecord, result.mScanRecord)
            && mTimestampNanos == result.mTimestampNanos && mEventType == result.mEventType && mPrimaryPhy == result.mPrimaryPhy
            && mSecondaryPhy == result.mSecondaryPhy && mAdvertisingSid == result.mAdvertisingSid && mTxPower == result.mTxPower
            && mPeriodicAdvertisingInterval == result.mPeriodicAdvertisingInterval;
    }

    @Override
    public String toString()
    {
        return "ScanResult{device=" + mDevice + ", scanRecord=" + mScanRecord + ", rssi=" + mRssi + ", timestampNanos=" + mTimestampNanos
            + ", eventType=" + mEventType + ", primaryPhy=" + mPrimaryPhy + ", secondaryPhy=" + mSecondaryPhy + ", advertisingSid="
            + mAdvertisingSid + ", txPower=" + mTxPower + ", periodicAdvertisingInterval=" + mPeriodicAdvertisingInterval + '}';
    }

    public static final Creator<ScanResult> CREATOR = new Creator<ScanResult>()
    {
        @Override
        public ScanResult createFromParcel(Parcel source)
        {
            BluetoothDevice device = source.readParcelable(BluetoothDevice.class.getClassLoader());
            int rssi = source.readInt();
            long timestampNanos = source.readLong();
            int eventType = source.readInt();
            int primaryPhy = source.readInt();
            int secondaryPhy = source.readInt();
            int advertisingSid = source.readInt();
            int txPower = source.readInt();
            int periodicAdvertisingInterval = source.readInt();
            return new ScanResult(device, eventType, primaryPhy, secondaryPhy, advertisingSid, txPower, rssi, periodicAdvertisingInterval,
                                  null, timestampNanos);
        }

        @Override
        public ScanResult[] newArray(int size)
        {
            return new ScanResult[size];
        }
    };
}
