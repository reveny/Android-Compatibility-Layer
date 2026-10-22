package android.hardware.camera2;

import android.graphics.Rect;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.TonemapCurve;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CaptureRequest extends CameraMetadata<CaptureRequest.Key<?>> implements Parcelable
{
    public static final Key<Boolean> BLACK_LEVEL_LOCK = new Key<>("android.blackLevel.lock", Boolean.class);
    public static final Key<Integer> COLOR_CORRECTION_ABERRATION_MODE = new Key<>("android.colorCorrection.aberrationMode", Integer.class);
    public static final Key<Integer> COLOR_CORRECTION_COLOR_TEMPERATURE =
            new Key<>("android.colorCorrection.colorTemperature", Integer.class);
    public static final Key<Integer> COLOR_CORRECTION_COLOR_TINT = new Key<>("android.colorCorrection.colorTint", Integer.class);
    public static final Key<RggbChannelVector> COLOR_CORRECTION_GAINS = new Key<>("android.colorCorrection.gains", RggbChannelVector.class);
    public static final Key<Integer> COLOR_CORRECTION_MODE = new Key<>("android.colorCorrection.mode", Integer.class);
    public static final Key<ColorSpaceTransform> COLOR_CORRECTION_TRANSFORM =
            new Key<>("android.colorCorrection.transform", ColorSpaceTransform.class);
    public static final Key<Integer> CONTROL_AE_ANTIBANDING_MODE = new Key<>("android.control.aeAntibandingMode", Integer.class);
    public static final Key<Integer> CONTROL_AE_EXPOSURE_COMPENSATION = new Key<>("android.control.aeExposureCompensation", Integer.class);
    public static final Key<Boolean> CONTROL_AE_LOCK = new Key<>("android.control.aeLock", Boolean.class);
    public static final Key<Integer> CONTROL_AE_MODE = new Key<>("android.control.aeMode", Integer.class);
    public static final Key<Integer> CONTROL_AE_PRECAPTURE_TRIGGER = new Key<>("android.control.aePrecaptureTrigger", Integer.class);
    public static final Key<Integer> CONTROL_AE_PRIORITY_MODE = new Key<>("android.control.aePriorityMode", Integer.class);
    public static final Key<MeteringRectangle[]> CONTROL_AE_REGIONS = new Key<>("android.control.aeRegions", MeteringRectangle[].class);
    public static final Key<Range<Integer>> CONTROL_AE_TARGET_FPS_RANGE = new Key<>("android.control.aeTargetFpsRange");
    public static final Key<Integer> CONTROL_AF_MODE = new Key<>("android.control.afMode", Integer.class);
    public static final Key<MeteringRectangle[]> CONTROL_AF_REGIONS = new Key<>("android.control.afRegions", MeteringRectangle[].class);
    public static final Key<Integer> CONTROL_AF_TRIGGER = new Key<>("android.control.afTrigger", Integer.class);
    public static final Key<Integer> CONTROL_AUTOFRAMING = new Key<>("android.control.autoframing", Integer.class);
    public static final Key<Boolean> CONTROL_AWB_LOCK = new Key<>("android.control.awbLock", Boolean.class);
    public static final Key<Integer> CONTROL_AWB_MODE = new Key<>("android.control.awbMode", Integer.class);
    public static final Key<MeteringRectangle[]> CONTROL_AWB_REGIONS = new Key<>("android.control.awbRegions", MeteringRectangle[].class);
    public static final Key<Integer> CONTROL_CAPTURE_INTENT = new Key<>("android.control.captureIntent", Integer.class);
    public static final Key<Integer> CONTROL_EFFECT_MODE = new Key<>("android.control.effectMode", Integer.class);
    public static final Key<Boolean> CONTROL_ENABLE_ZSL = new Key<>("android.control.enableZsl", Boolean.class);
    public static final Key<Integer> CONTROL_EXTENDED_SCENE_MODE = new Key<>("android.control.extendedSceneMode", Integer.class);
    public static final Key<Integer> CONTROL_MODE = new Key<>("android.control.mode", Integer.class);
    public static final Key<Integer> CONTROL_POST_RAW_SENSITIVITY_BOOST =
            new Key<>("android.control.postRawSensitivityBoost", Integer.class);
    public static final Key<Integer> CONTROL_SCENE_MODE = new Key<>("android.control.sceneMode", Integer.class);
    public static final Key<Integer> CONTROL_SETTINGS_OVERRIDE = new Key<>("android.control.settingsOverride", Integer.class);
    public static final Key<Integer> CONTROL_VIDEO_STABILIZATION_MODE = new Key<>("android.control.videoStabilizationMode", Integer.class);
    public static final Key<Integer> CONTROL_ZOOM_METHOD = new Key<>("android.control.zoomMethod", Integer.class);
    public static final Key<Float> CONTROL_ZOOM_RATIO = new Key<>("android.control.zoomRatio", Float.class);
    public static final Key<Integer> DISTORTION_CORRECTION_MODE = new Key<>("android.distortionCorrection.mode", Integer.class);
    public static final Key<Integer> EDGE_MODE = new Key<>("android.edge.mode", Integer.class);
    public static final Key<Integer> EXTENSION_STRENGTH = new Key<>("android.extension.strength", Integer.class);
    public static final Key<Integer> FLASH_MODE = new Key<>("android.flash.mode", Integer.class);
    public static final Key<Integer> FLASH_STRENGTH_LEVEL = new Key<>("android.flash.strengthLevel", Integer.class);
    public static final Key<Integer> HOT_PIXEL_MODE = new Key<>("android.hotPixel.mode", Integer.class);
    public static final Key<Location> JPEG_GPS_LOCATION = new Key<>("android.jpeg.gpsLocation", Location.class);
    public static final Key<Integer> JPEG_ORIENTATION = new Key<>("android.jpeg.orientation", Integer.class);
    public static final Key<Byte> JPEG_QUALITY = new Key<>("android.jpeg.quality", Byte.class);
    public static final Key<Byte> JPEG_THUMBNAIL_QUALITY = new Key<>("android.jpeg.thumbnailQuality", Byte.class);
    public static final Key<Size> JPEG_THUMBNAIL_SIZE = new Key<>("android.jpeg.thumbnailSize", Size.class);
    public static final Key<Float> LENS_APERTURE = new Key<>("android.lens.aperture", Float.class);
    public static final Key<Float> LENS_FILTER_DENSITY = new Key<>("android.lens.filterDensity", Float.class);
    public static final Key<Float> LENS_FOCAL_LENGTH = new Key<>("android.lens.focalLength", Float.class);
    public static final Key<Float> LENS_FOCUS_DISTANCE = new Key<>("android.lens.focusDistance", Float.class);
    public static final Key<Integer> LENS_OPTICAL_STABILIZATION_MODE = new Key<>("android.lens.opticalStabilizationMode", Integer.class);
    public static final Key<Integer> NOISE_REDUCTION_MODE = new Key<>("android.noiseReduction.mode", Integer.class);
    public static final Key<Float> REPROCESS_EFFECTIVE_EXPOSURE_FACTOR =
            new Key<>("android.reprocess.effectiveExposureFactor", Float.class);
    public static final Key<Rect> SCALER_CROP_REGION = new Key<>("android.scaler.cropRegion", Rect.class);
    public static final Key<Integer> SCALER_ROTATE_AND_CROP = new Key<>("android.scaler.rotateAndCrop", Integer.class);
    public static final Key<Long> SENSOR_EXPOSURE_TIME = new Key<>("android.sensor.exposureTime", Long.class);
    public static final Key<Long> SENSOR_FRAME_DURATION = new Key<>("android.sensor.frameDuration", Long.class);
    public static final Key<Integer> SENSOR_PIXEL_MODE = new Key<>("android.sensor.pixelMode", Integer.class);
    public static final Key<Integer> SENSOR_SENSITIVITY = new Key<>("android.sensor.sensitivity", Integer.class);
    public static final Key<int[]> SENSOR_TEST_PATTERN_DATA = new Key<>("android.sensor.testPatternData", int[].class);
    public static final Key<Integer> SENSOR_TEST_PATTERN_MODE = new Key<>("android.sensor.testPatternMode", Integer.class);
    public static final Key<Integer> SHADING_MODE = new Key<>("android.shading.mode", Integer.class);
    public static final Key<Integer> STATISTICS_FACE_DETECT_MODE = new Key<>("android.statistics.faceDetectMode", Integer.class);
    public static final Key<Boolean> STATISTICS_HOT_PIXEL_MAP_MODE = new Key<>("android.statistics.hotPixelMapMode", Boolean.class);
    public static final Key<Integer> STATISTICS_LENS_SHADING_MAP_MODE = new Key<>("android.statistics.lensShadingMapMode", Integer.class);
    public static final Key<Integer> STATISTICS_OIS_DATA_MODE = new Key<>("android.statistics.oisDataMode", Integer.class);
    public static final Key<TonemapCurve> TONEMAP_CURVE = new Key<>("android.tonemap.curve", TonemapCurve.class);
    public static final Key<Float> TONEMAP_GAMMA = new Key<>("android.tonemap.gamma", Float.class);
    public static final Key<Integer> TONEMAP_MODE = new Key<>("android.tonemap.mode", Integer.class);
    public static final Key<Integer> TONEMAP_PRESET_CURVE = new Key<>("android.tonemap.presetCurve", Integer.class);

    public static final Parcelable.Creator<CaptureRequest> CREATOR = new Parcelable.Creator<CaptureRequest>()
    {
        @Override
        public CaptureRequest createFromParcel(Parcel source)
        {
            boolean reprocess = source.readInt() != 0;
            String cameraId = source.readString();
            return new CaptureRequest(new LinkedHashMap<>(), new LinkedHashSet<>(), null, reprocess, cameraId, new HashMap<>());
        }

        @Override
        public CaptureRequest[] newArray(int size)
        {
            return new CaptureRequest[size];
        }
    };

    private final Set<Surface> mTargets;
    private final Object mTag;
    private final boolean mReprocess;
    private final String mLogicalCameraId;
    private final Map<String, Map<Key<?>, Object>> mPhysicalValues;

    CaptureRequest(Map<Key<?>, Object> values, Set<Surface> targets, Object tag, boolean reprocess, String logicalCameraId,
            Map<String, Map<Key<?>, Object>> physicalValues)
    {
        super(values);
        mTargets = Collections.unmodifiableSet(new LinkedHashSet<>(targets));
        mTag = tag;
        mReprocess = reprocess;
        mLogicalCameraId = logicalCameraId;
        mPhysicalValues = physicalValues;
    }

    public <T> T get(Key<T> key)
    {
        return getValue(key);
    }

    @Override
    public List<Key<?>> getKeys()
    {
        return super.getKeys();
    }

    public Object getTag()
    {
        return mTag;
    }

    public boolean isReprocess()
    {
        return mReprocess;
    }

    Set<Surface> getTargets()
    {
        return mTargets;
    }

    String getLogicalCameraId()
    {
        return mLogicalCameraId;
    }

    Map<String, Map<Key<?>, Object>> getPhysicalValues()
    {
        return mPhysicalValues;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof CaptureRequest)) return false;

        CaptureRequest request = (CaptureRequest)other;
        return mReprocess == request.mReprocess && Objects.equals(mLogicalCameraId, request.mLogicalCameraId)
                && mTargets.equals(request.mTargets) && mValues.equals(request.mValues);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mValues.keySet(), mTargets, mReprocess, mLogicalCameraId);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mReprocess ? 1 : 0);
        dest.writeString(mLogicalCameraId);
    }

    public static final class Key<T>
    {
        private final String mName;
        private final Class<T> mType;

        public Key(String name, Class<T> type)
        {
            if (name == null) throw new NullPointerException("Key needs a valid name");
            if (type == null) throw new NullPointerException("Type needs to be non-null");
            mName = name;
            mType = type;
        }

        Key(String name)
        {
            mName = name;
            mType = null;
        }

        public String getName()
        {
            return mName;
        }

        @Override
        public boolean equals(Object other)
        {
            return other instanceof Key && ((Key<?>)other).mName.equals(mName);
        }

        @Override
        public int hashCode()
        {
            return mName.hashCode();
        }

        @Override
        public String toString()
        {
            return "CaptureRequest.Key(" + mName + ")";
        }
    }

    public static final class Builder
    {
        private final Map<Key<?>, Object> mValues;
        private final Set<Surface> mTargets = new LinkedHashSet<>();
        private final Map<String, Map<Key<?>, Object>> mPhysicalValues = new HashMap<>();
        private final boolean mReprocess;
        private final String mLogicalCameraId;
        private Object mTag;

        Builder(Map<Key<?>, Object> values, boolean reprocess, String logicalCameraId, Set<String> physicalCameraIds)
        {
            mValues = new LinkedHashMap<>(values);
            mReprocess = reprocess;
            mLogicalCameraId = logicalCameraId;
            if (physicalCameraIds != null)
            {
                for (String physicalCameraId : physicalCameraIds)
                {
                    mPhysicalValues.put(physicalCameraId, new LinkedHashMap<>(values));
                }
            }
        }

        public void addTarget(Surface outputTarget)
        {
            if (outputTarget == null) throw new IllegalArgumentException("outputTarget must not be null");
            mTargets.add(outputTarget);
        }

        public void removeTarget(Surface outputTarget)
        {
            mTargets.remove(outputTarget);
        }

        public <T> void set(Key<T> key, T value)
        {
            if (key == null) throw new IllegalArgumentException("key must not be null");
            if (value == null)
            {
                mValues.remove(key);
            }
            else
            {
                mValues.put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T get(Key<T> key)
        {
            if (key == null) throw new IllegalArgumentException("key must not be null");
            return (T)mValues.get(key);
        }

        public <T> Builder setPhysicalCameraKey(Key<T> key, T value, String physicalCameraId)
        {
            Map<Key<?>, Object> values = mPhysicalValues.get(physicalCameraId);
            if (values == null) throw new IllegalArgumentException("Physical camera id: " + physicalCameraId + " is not valid!");
            if (value == null)
            {
                values.remove(key);
            }
            else
            {
                values.put(key, value);
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> T getPhysicalCameraKey(Key<T> key, String physicalCameraId)
        {
            Map<Key<?>, Object> values = mPhysicalValues.get(physicalCameraId);
            if (values == null) throw new IllegalArgumentException("Physical camera id: " + physicalCameraId + " is not valid!");
            return (T)values.get(key);
        }

        public void setTag(Object tag)
        {
            mTag = tag;
        }

        public CaptureRequest build()
        {
            Map<String, Map<Key<?>, Object>> physicalValues = new HashMap<>();
            for (Map.Entry<String, Map<Key<?>, Object>> entry : mPhysicalValues.entrySet())
            {
                physicalValues.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }
            return new CaptureRequest(mValues, mTargets, mTag, mReprocess, mLogicalCameraId, physicalValues);
        }
    }
}
