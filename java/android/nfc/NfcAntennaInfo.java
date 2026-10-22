package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NfcAntennaInfo implements Parcelable
{
    public static final Parcelable.Creator<NfcAntennaInfo> CREATOR = new Parcelable.Creator<NfcAntennaInfo>()
    {
        @Override
        public NfcAntennaInfo createFromParcel(Parcel source)
        {
            int deviceWidth = source.readInt();
            int deviceHeight = source.readInt();
            boolean deviceFoldable = source.readBoolean();
            int count = source.readInt();
            List<AvailableNfcAntenna> antennas = new ArrayList<>();
            for (int index = 0; index < count; index++) antennas.add(AvailableNfcAntenna.CREATOR.createFromParcel(source));
            return new NfcAntennaInfo(deviceWidth, deviceHeight, deviceFoldable, antennas);
        }

        @Override
        public NfcAntennaInfo[] newArray(int size)
        {
            return new NfcAntennaInfo[size];
        }
    };

    private final int mDeviceWidth;
    private final int mDeviceHeight;
    private final boolean mDeviceFoldable;
    private final List<AvailableNfcAntenna> mAvailableNfcAntennas;

    public NfcAntennaInfo(int deviceWidth, int deviceHeight, boolean deviceFoldable, List<AvailableNfcAntenna> availableNfcAntennas)
    {
        mDeviceWidth = deviceWidth;
        mDeviceHeight = deviceHeight;
        mDeviceFoldable = deviceFoldable;
        mAvailableNfcAntennas = availableNfcAntennas == null ? new ArrayList<>() : new ArrayList<>(availableNfcAntennas);
    }

    public int getDeviceWidth()
    {
        return mDeviceWidth;
    }

    public int getDeviceHeight()
    {
        return mDeviceHeight;
    }

    public boolean isDeviceFoldable()
    {
        return mDeviceFoldable;
    }

    public List<AvailableNfcAntenna> getAvailableNfcAntennas()
    {
        return Collections.unmodifiableList(mAvailableNfcAntennas);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mDeviceWidth);
        dest.writeInt(mDeviceHeight);
        dest.writeBoolean(mDeviceFoldable);
        dest.writeInt(mAvailableNfcAntennas.size());
        for (AvailableNfcAntenna antenna : mAvailableNfcAntennas) antenna.writeToParcel(dest, flags);
    }
}
