package android.media;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MicrophoneInfo
{
    public static final int CHANNEL_MAPPING_DIRECT = 1;
    public static final int CHANNEL_MAPPING_PROCESSED = 2;
    public static final int DIRECTIONALITY_BI_DIRECTIONAL = 2;
    public static final int DIRECTIONALITY_CARDIOID = 3;
    public static final int DIRECTIONALITY_HYPER_CARDIOID = 4;
    public static final int DIRECTIONALITY_OMNI = 1;
    public static final int DIRECTIONALITY_SUPER_CARDIOID = 5;
    public static final int DIRECTIONALITY_UNKNOWN = 0;
    public static final int GROUP_UNKNOWN = -1;
    public static final int INDEX_IN_THE_GROUP_UNKNOWN = -1;
    public static final int LOCATION_MAINBODY = 1;
    public static final int LOCATION_MAINBODY_MOVABLE = 2;
    public static final int LOCATION_PERIPHERAL = 3;
    public static final int LOCATION_UNKNOWN = 0;
    public static final Coordinate3F ORIENTATION_UNKNOWN = new Coordinate3F(0.0f, 0.0f, 0.0f);
    public static final Coordinate3F POSITION_UNKNOWN = new Coordinate3F(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
    public static final float SENSITIVITY_UNKNOWN = -3.4028235E38f;
    public static final float SPL_UNKNOWN = -3.4028235E38f;

    private final String mDeviceId;
    private final int mType;
    private final String mAddress;
    private final int mLocation;
    private final int mGroup;
    private final int mIndexInTheGroup;
    private final int mPortId;
    private final List<Pair<Integer, Integer>> mChannelMapping;

    MicrophoneInfo(String deviceId, int type, String address, int location, int group, int indexInTheGroup, int portId,
        int channelCount)
    {
        mDeviceId = deviceId;
        mType = type;
        mAddress = address;
        mLocation = location;
        mGroup = group;
        mIndexInTheGroup = indexInTheGroup;
        mPortId = portId;
        mChannelMapping = new ArrayList<>();

        for (int channel = 0; channel < channelCount; channel++)
        {
            mChannelMapping.add(new Pair<>(channel, CHANNEL_MAPPING_DIRECT));
        }
    }

    public String getAddress()
    {
        return mAddress;
    }

    public List<Pair<Integer, Integer>> getChannelMapping()
    {
        return new ArrayList<>(mChannelMapping);
    }

    public String getDescription()
    {
        return mDeviceId;
    }

    public int getDirectionality()
    {
        return DIRECTIONALITY_OMNI;
    }

    public List<Pair<Float, Float>> getFrequencyResponse()
    {
        return new ArrayList<>();
    }

    public int getGroup()
    {
        return mGroup;
    }

    public int getId()
    {
        return mPortId;
    }

    public int getIndexInTheGroup()
    {
        return mIndexInTheGroup;
    }

    public int getLocation()
    {
        return mLocation;
    }

    public float getMaxSpl()
    {
        return SPL_UNKNOWN;
    }

    public float getMinSpl()
    {
        return SPL_UNKNOWN;
    }

    public Coordinate3F getOrientation()
    {
        return ORIENTATION_UNKNOWN;
    }

    public Coordinate3F getPosition()
    {
        return POSITION_UNKNOWN;
    }

    public float getSensitivity()
    {
        return SENSITIVITY_UNKNOWN;
    }

    public int getType()
    {
        return mType;
    }

    public static final class Coordinate3F
    {
        public final float x;
        public final float y;
        public final float z;

        Coordinate3F(float x, float y, float z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) return true;
            if (!(object instanceof Coordinate3F)) return false;

            Coordinate3F other = (Coordinate3F)object;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(x, y, z);
        }
    }
}
