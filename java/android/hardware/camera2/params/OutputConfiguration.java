package android.hardware.camera2.params;

import android.hardware.camera2.CameraMetadata;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OutputConfiguration implements Parcelable
{
    public static final int SURFACE_GROUP_ID_NONE = -1;

    public static final int MIRROR_MODE_AUTO = 0;
    public static final int MIRROR_MODE_NONE = 1;
    public static final int MIRROR_MODE_H = 2;
    public static final int MIRROR_MODE_V = 3;

    public static final int TIMESTAMP_BASE_DEFAULT = 0;
    public static final int TIMESTAMP_BASE_SENSOR = 1;
    public static final int TIMESTAMP_BASE_MONOTONIC = 2;
    public static final int TIMESTAMP_BASE_REALTIME = 3;
    public static final int TIMESTAMP_BASE_CHOREOGRAPHER_SYNCED = 4;

    public static final Parcelable.Creator<OutputConfiguration> CREATOR = new Parcelable.Creator<OutputConfiguration>()
    {
        @Override
        public OutputConfiguration createFromParcel(Parcel source)
        {
            return new OutputConfiguration(source);
        }

        @Override
        public OutputConfiguration[] newArray(int size)
        {
            return new OutputConfiguration[size];
        }
    };

    private static final int MAX_SURFACES_COUNT = 4;
    private static final int SURFACE_TYPE_UNKNOWN = -1;
    private static final int SURFACE_TYPE_SURFACE_VIEW = 0;
    private static final int SURFACE_TYPE_SURFACE_TEXTURE = 1;
    private static final int SURFACE_TYPE_IMAGE_READER = 4;

    private static int sNextMultiResolutionGroupId;

    private final ArrayList<Surface> mSurfaces = new ArrayList<>();
    private final Map<Surface, Integer> mSurfaceMirrorModes = new HashMap<>();
    private final ArrayList<Integer> mSensorPixelModesUsed = new ArrayList<>();
    private final int mSurfaceGroupId;
    private final int mSurfaceType;
    private final Size mConfiguredSize;
    private final int mConfiguredFormat;
    private final boolean mIsDeferredConfig;
    private final long mUsage;
    private boolean mIsShared;
    private String mPhysicalCameraId;
    private long mDynamicRangeProfile = DynamicRangeProfiles.STANDARD;
    private long mStreamUseCase = CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT;
    private int mTimestampBase = TIMESTAMP_BASE_DEFAULT;
    private int mMirrorMode = MIRROR_MODE_AUTO;
    private boolean mReadoutTimestampEnabled;

    public OutputConfiguration(Surface surface)
    {
        this(SURFACE_GROUP_ID_NONE, surface);
    }

    public OutputConfiguration(int surfaceGroupId, Surface surface)
    {
        if (surface == null) throw new NullPointerException("Surface must not be null");
        mSurfaces.add(surface);
        mSurfaceGroupId = surfaceGroupId;
        mSurfaceType = SURFACE_TYPE_UNKNOWN;
        mConfiguredSize = null;
        mConfiguredFormat = 0;
        mIsDeferredConfig = false;
        mUsage = 0;
    }

    public <T> OutputConfiguration(Size surfaceSize, Class<T> klass)
    {
        if (surfaceSize == null) throw new NullPointerException("surfaceSize must not be null");
        if (klass == null) throw new NullPointerException("klass must not be null");

        if ("android.graphics.SurfaceTexture".equals(klass.getName()))
        {
            mSurfaceType = SURFACE_TYPE_SURFACE_TEXTURE;
        }
        else if ("android.view.SurfaceHolder".equals(klass.getName()))
        {
            mSurfaceType = SURFACE_TYPE_SURFACE_VIEW;
        }
        else
        {
            throw new IllegalArgumentException("Unknown surface source class type");
        }

        if (surfaceSize.getWidth() == 0 || surfaceSize.getHeight() == 0)
        {
            throw new IllegalArgumentException("Surface size needs to be non-zero");
        }

        mSurfaceGroupId = SURFACE_GROUP_ID_NONE;
        mConfiguredSize = surfaceSize;
        mConfiguredFormat = 0x22;
        mIsDeferredConfig = true;
        mUsage = 0;
    }

    public OutputConfiguration(int format, Size surfaceSize)
    {
        this(SURFACE_GROUP_ID_NONE, format, surfaceSize, 0);
    }

    public OutputConfiguration(int surfaceGroupId, int format, Size surfaceSize)
    {
        this(surfaceGroupId, format, surfaceSize, 0);
    }

    public OutputConfiguration(int format, Size surfaceSize, long usage)
    {
        this(SURFACE_GROUP_ID_NONE, format, surfaceSize, usage);
    }

    public OutputConfiguration(int surfaceGroupId, int format, Size surfaceSize, long usage)
    {
        if (surfaceSize == null) throw new NullPointerException("surfaceSize must not be null");
        if (surfaceSize.getWidth() == 0 || surfaceSize.getHeight() == 0)
        {
            throw new IllegalArgumentException("Surface size needs to be non-zero");
        }
        mSurfaceGroupId = surfaceGroupId;
        mSurfaceType = SURFACE_TYPE_IMAGE_READER;
        mConfiguredSize = surfaceSize;
        mConfiguredFormat = format;
        mIsDeferredConfig = true;
        mUsage = usage;
    }

    private OutputConfiguration(Parcel source)
    {
        mSurfaceGroupId = source.readInt();
        mSurfaceType = source.readInt();
        int width = source.readInt();
        int height = source.readInt();
        mConfiguredSize = width > 0 && height > 0 ? new Size(width, height) : null;
        mConfiguredFormat = source.readInt();
        mIsDeferredConfig = source.readInt() != 0;
        mUsage = source.readLong();
        mIsShared = source.readInt() != 0;
        mPhysicalCameraId = source.readString();
        mDynamicRangeProfile = source.readLong();
        mStreamUseCase = source.readLong();
        mTimestampBase = source.readInt();
        mMirrorMode = source.readInt();
        mReadoutTimestampEnabled = source.readInt() != 0;
    }

    public static List<OutputConfiguration> createInstancesForMultiResolutionOutput(Collection<MultiResolutionStreamInfo> streams,
            int format)
    {
        if (streams == null || streams.size() <= 1)
        {
            throw new IllegalArgumentException("The number of multi-resolution streams must be at least 2");
        }

        int groupId;
        synchronized (OutputConfiguration.class)
        {
            groupId = sNextMultiResolutionGroupId++;
        }

        List<OutputConfiguration> configurations = new ArrayList<>();
        for (MultiResolutionStreamInfo stream : streams)
        {
            OutputConfiguration configuration = new OutputConfiguration(groupId, format, new Size(stream.getWidth(), stream.getHeight()));
            configuration.setPhysicalCameraId(stream.getPhysicalCameraId());
            configurations.add(configuration);
        }
        return configurations;
    }

    public void enableSurfaceSharing()
    {
        mIsShared = true;
    }

    public void setPhysicalCameraId(String physicalCameraId)
    {
        mPhysicalCameraId = physicalCameraId;
    }

    public void addSensorPixelModeUsed(int sensorPixelModeUsed)
    {
        if (sensorPixelModeUsed != CameraMetadata.SENSOR_PIXEL_MODE_DEFAULT
                && sensorPixelModeUsed != CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
        {
            throw new IllegalArgumentException("Not a valid sensor pixel mode " + sensorPixelModeUsed);
        }
        if (mSensorPixelModesUsed.contains(sensorPixelModeUsed)) return;
        mSensorPixelModesUsed.add(sensorPixelModeUsed);
    }

    public void removeSensorPixelModeUsed(int sensorPixelModeUsed)
    {
        if (!mSensorPixelModesUsed.remove(Integer.valueOf(sensorPixelModeUsed)))
        {
            throw new IllegalArgumentException("sensorPixelMode " + sensorPixelModeUsed + "is not part of this output configuration");
        }
    }

    public void addSurface(Surface surface)
    {
        if (surface == null) throw new NullPointerException("Surface must not be null");
        if (mSurfaces.contains(surface)) throw new IllegalStateException("Surface is already added!");
        if (mSurfaces.size() == 1 && !mIsShared) throw new IllegalStateException("Cannot have 2 surfaces for a non-sharing configuration");
        if (mSurfaces.size() + 1 > MAX_SURFACES_COUNT) throw new IllegalArgumentException("Exceeds maximum number of surfaces");
        mSurfaces.add(surface);
    }

    public void removeSurface(Surface surface)
    {
        if (getSurface() == surface) throw new IllegalArgumentException("Cannot remove surface associated with this output configuration");
        if (!mSurfaces.remove(surface)) throw new IllegalArgumentException("Surface is not part of this output configuration");
        mSurfaceMirrorModes.remove(surface);
    }

    public void setTimestampBase(int timestampBase)
    {
        if (timestampBase < TIMESTAMP_BASE_DEFAULT || timestampBase > TIMESTAMP_BASE_CHOREOGRAPHER_SYNCED)
        {
            throw new IllegalArgumentException("Not a valid timestamp base value " + timestampBase);
        }
        mTimestampBase = timestampBase;
    }

    public int getTimestampBase()
    {
        return mTimestampBase;
    }

    public void setMirrorMode(int mirrorMode)
    {
        checkMirrorMode(mirrorMode);
        mMirrorMode = mirrorMode;
    }

    public void setMirrorMode(Surface surface, int mirrorMode)
    {
        if (surface == null) throw new NullPointerException("Surface must not be null");
        checkMirrorMode(mirrorMode);
        if (!mSurfaces.contains(surface)) throw new IllegalArgumentException("Surface not part of the OutputConfiguration");
        mSurfaceMirrorModes.put(surface, mirrorMode);
    }

    private static void checkMirrorMode(int mirrorMode)
    {
        if (mirrorMode < MIRROR_MODE_AUTO || mirrorMode > MIRROR_MODE_V)
        {
            throw new IllegalArgumentException("Not a valid mirror mode " + mirrorMode);
        }
    }

    public int getMirrorMode()
    {
        return mMirrorMode;
    }

    public int getMirrorMode(Surface surface)
    {
        if (surface == null) throw new NullPointerException("Surface must not be null");
        if (!mSurfaces.contains(surface)) throw new IllegalArgumentException("Surface not part of the OutputConfiguration");
        Integer mode = mSurfaceMirrorModes.get(surface);
        return mode == null ? mMirrorMode : mode;
    }

    public void setReadoutTimestampEnabled(boolean on)
    {
        mReadoutTimestampEnabled = on;
    }

    public boolean isReadoutTimestampEnabled()
    {
        return mReadoutTimestampEnabled;
    }

    public void setDynamicRangeProfile(long profile)
    {
        mDynamicRangeProfile = profile;
    }

    public long getDynamicRangeProfile()
    {
        return mDynamicRangeProfile;
    }

    public void setStreamUseCase(long streamUseCase)
    {
        if (streamUseCase < 0) throw new IllegalArgumentException("Not a valid stream use case value " + streamUseCase);
        mStreamUseCase = streamUseCase;
    }

    public long getStreamUseCase()
    {
        return mStreamUseCase;
    }

    public int getMaxSharedSurfaceCount()
    {
        return MAX_SURFACES_COUNT;
    }

    public Surface getSurface()
    {
        return mSurfaces.isEmpty() ? null : mSurfaces.get(0);
    }

    public List<Surface> getSurfaces()
    {
        return Collections.unmodifiableList(mSurfaces);
    }

    public int getSurfaceGroupId()
    {
        return mSurfaceGroupId;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        if (dest == null) throw new IllegalArgumentException("dest must not be null");
        dest.writeInt(mSurfaceGroupId);
        dest.writeInt(mSurfaceType);
        dest.writeInt(mConfiguredSize == null ? 0 : mConfiguredSize.getWidth());
        dest.writeInt(mConfiguredSize == null ? 0 : mConfiguredSize.getHeight());
        dest.writeInt(mConfiguredFormat);
        dest.writeInt(mIsDeferredConfig ? 1 : 0);
        dest.writeLong(mUsage);
        dest.writeInt(mIsShared ? 1 : 0);
        dest.writeString(mPhysicalCameraId);
        dest.writeLong(mDynamicRangeProfile);
        dest.writeLong(mStreamUseCase);
        dest.writeInt(mTimestampBase);
        dest.writeInt(mMirrorMode);
        dest.writeInt(mReadoutTimestampEnabled ? 1 : 0);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof OutputConfiguration)) return false;

        OutputConfiguration configuration = (OutputConfiguration)other;
        return mSurfaceGroupId == configuration.mSurfaceGroupId && mSurfaceType == configuration.mSurfaceType
                && mConfiguredFormat == configuration.mConfiguredFormat && mIsDeferredConfig == configuration.mIsDeferredConfig
                && mIsShared == configuration.mIsShared && mUsage == configuration.mUsage
                && mDynamicRangeProfile == configuration.mDynamicRangeProfile && mStreamUseCase == configuration.mStreamUseCase
                && mTimestampBase == configuration.mTimestampBase && mMirrorMode == configuration.mMirrorMode
                && mReadoutTimestampEnabled == configuration.mReadoutTimestampEnabled
                && Objects.equals(mConfiguredSize, configuration.mConfiguredSize)
                && Objects.equals(mPhysicalCameraId, configuration.mPhysicalCameraId)
                && mSurfaces.equals(configuration.mSurfaces) && mSensorPixelModesUsed.equals(configuration.mSensorPixelModesUsed);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mSurfaceGroupId, mSurfaceType, mConfiguredSize, mConfiguredFormat, mIsDeferredConfig, mIsShared, mUsage,
                mPhysicalCameraId, mDynamicRangeProfile, mStreamUseCase, mTimestampBase, mMirrorMode, mReadoutTimestampEnabled, mSurfaces);
    }
}
