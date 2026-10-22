package android.media.midi;

import android.os.Parcel;
import android.os.Parcelable;

public final class MidiDeviceStatus implements Parcelable
{
    public static final Parcelable.Creator<MidiDeviceStatus> CREATOR = new Parcelable.Creator<MidiDeviceStatus>()
    {
        @Override
        public MidiDeviceStatus createFromParcel(Parcel source)
        {
            MidiDeviceInfo deviceInfo = MidiDeviceInfo.CREATOR.createFromParcel(source);
            boolean[] inputPortOpen = source.createBooleanArray();
            int[] outputPortOpenCount = source.createIntArray();
            return new MidiDeviceStatus(deviceInfo, inputPortOpen, outputPortOpenCount);
        }

        @Override
        public MidiDeviceStatus[] newArray(int size)
        {
            return new MidiDeviceStatus[size];
        }
    };

    private final MidiDeviceInfo mDeviceInfo;
    private final boolean[] mInputPortOpen;
    private final int[] mOutputPortOpenCount;

    MidiDeviceStatus(MidiDeviceInfo deviceInfo, boolean[] inputPortOpen, int[] outputPortOpenCount)
    {
        mDeviceInfo = deviceInfo;
        mInputPortOpen = inputPortOpen;
        mOutputPortOpenCount = outputPortOpenCount;
    }

    MidiDeviceStatus(MidiDeviceInfo deviceInfo)
    {
        this(deviceInfo, new boolean[deviceInfo.getInputPortCount()], new int[deviceInfo.getOutputPortCount()]);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public MidiDeviceInfo getDeviceInfo()
    {
        return mDeviceInfo;
    }

    public int getOutputPortOpenCount(int portNumber)
    {
        return mOutputPortOpenCount[portNumber];
    }

    public boolean isInputPortOpen(int portNumber)
    {
        return mInputPortOpen[portNumber];
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(mDeviceInfo.toString());
        builder.append(" mInputPortOpen=[");

        for (int index = 0; index < mInputPortOpen.length; index++)
        {
            builder.append(mInputPortOpen[index]);
            if (index < mInputPortOpen.length - 1) builder.append(",");
        }

        builder.append("] mOutputPortOpenCount=[");

        for (int index = 0; index < mOutputPortOpenCount.length; index++)
        {
            builder.append(mOutputPortOpenCount[index]);
            if (index < mOutputPortOpenCount.length - 1) builder.append(",");
        }

        builder.append("]");
        return builder.toString();
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        mDeviceInfo.writeToParcel(destination, flags);
        destination.writeBooleanArray(mInputPortOpen);
        destination.writeIntArray(mOutputPortOpenCount);
    }
}
