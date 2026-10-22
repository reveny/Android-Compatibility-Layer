package android.hardware.camera2;

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.LensIntrinsicsSample;
import android.hardware.camera2.params.LensShadingMap;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.OisSample;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.TonemapCurve;
import android.location.Location;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.util.Size;

import java.util.List;
import java.util.Map;

public class CaptureResult extends CameraMetadata<CaptureResult.Key<?>>
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
    public static final Key<Integer> CONTROL_AE_STATE = new Key<>("android.control.aeState", Integer.class);
    public static final Key<Range<Integer>> CONTROL_AE_TARGET_FPS_RANGE = new Key<>("android.control.aeTargetFpsRange");
    public static final Key<Integer> CONTROL_AF_MODE = new Key<>("android.control.afMode", Integer.class);
    public static final Key<MeteringRectangle[]> CONTROL_AF_REGIONS = new Key<>("android.control.afRegions", MeteringRectangle[].class);
    public static final Key<Integer> CONTROL_AF_SCENE_CHANGE = new Key<>("android.control.afSceneChange", Integer.class);
    public static final Key<Integer> CONTROL_AF_STATE = new Key<>("android.control.afState", Integer.class);
    public static final Key<Integer> CONTROL_AF_TRIGGER = new Key<>("android.control.afTrigger", Integer.class);
    public static final Key<Integer> CONTROL_AUTOFRAMING = new Key<>("android.control.autoframing", Integer.class);
    public static final Key<Integer> CONTROL_AUTOFRAMING_STATE = new Key<>("android.control.autoframingState", Integer.class);
    public static final Key<Boolean> CONTROL_AWB_LOCK = new Key<>("android.control.awbLock", Boolean.class);
    public static final Key<Integer> CONTROL_AWB_MODE = new Key<>("android.control.awbMode", Integer.class);
    public static final Key<MeteringRectangle[]> CONTROL_AWB_REGIONS = new Key<>("android.control.awbRegions", MeteringRectangle[].class);
    public static final Key<Integer> CONTROL_AWB_STATE = new Key<>("android.control.awbState", Integer.class);
    public static final Key<Integer> CONTROL_CAPTURE_INTENT = new Key<>("android.control.captureIntent", Integer.class);
    public static final Key<Integer> CONTROL_EFFECT_MODE = new Key<>("android.control.effectMode", Integer.class);
    public static final Key<Boolean> CONTROL_ENABLE_ZSL = new Key<>("android.control.enableZsl", Boolean.class);
    public static final Key<Integer> CONTROL_EXTENDED_SCENE_MODE = new Key<>("android.control.extendedSceneMode", Integer.class);
    public static final Key<Integer> CONTROL_LOW_LIGHT_BOOST_STATE = new Key<>("android.control.lowLightBoostState", Integer.class);
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
    public static final Key<Integer> EXTENSION_CURRENT_TYPE = new Key<>("android.extension.currentType", Integer.class);
    public static final Key<Integer> EXTENSION_NIGHT_MODE_INDICATOR = new Key<>("android.extension.nightModeIndicator", Integer.class);
    public static final Key<Integer> EXTENSION_STRENGTH = new Key<>("android.extension.strength", Integer.class);
    public static final Key<Integer> FLASH_MODE = new Key<>("android.flash.mode", Integer.class);
    public static final Key<Integer> FLASH_STATE = new Key<>("android.flash.state", Integer.class);
    public static final Key<Integer> FLASH_STRENGTH_LEVEL = new Key<>("android.flash.strengthLevel", Integer.class);
    public static final Key<Integer> HOT_PIXEL_MODE = new Key<>("android.hotPixel.mode", Integer.class);
    public static final Key<Location> JPEG_GPS_LOCATION = new Key<>("android.jpeg.gpsLocation", Location.class);
    public static final Key<Integer> JPEG_ORIENTATION = new Key<>("android.jpeg.orientation", Integer.class);
    public static final Key<Byte> JPEG_QUALITY = new Key<>("android.jpeg.quality", Byte.class);
    public static final Key<Byte> JPEG_THUMBNAIL_QUALITY = new Key<>("android.jpeg.thumbnailQuality", Byte.class);
    public static final Key<Size> JPEG_THUMBNAIL_SIZE = new Key<>("android.jpeg.thumbnailSize", Size.class);
    public static final Key<Float> LENS_APERTURE = new Key<>("android.lens.aperture", Float.class);
    public static final Key<float[]> LENS_DISTORTION = new Key<>("android.lens.distortion", float[].class);
    public static final Key<Float> LENS_FILTER_DENSITY = new Key<>("android.lens.filterDensity", Float.class);
    public static final Key<Float> LENS_FOCAL_LENGTH = new Key<>("android.lens.focalLength", Float.class);
    public static final Key<Float> LENS_FOCUS_DISTANCE = new Key<>("android.lens.focusDistance", Float.class);
    public static final Key<Pair<Float, Float>> LENS_FOCUS_RANGE = new Key<>("android.lens.focusRange");
    public static final Key<float[]> LENS_INTRINSIC_CALIBRATION = new Key<>("android.lens.intrinsicCalibration", float[].class);
    public static final Key<Integer> LENS_OPTICAL_STABILIZATION_MODE = new Key<>("android.lens.opticalStabilizationMode", Integer.class);
    public static final Key<float[]> LENS_POSE_ROTATION = new Key<>("android.lens.poseRotation", float[].class);
    public static final Key<float[]> LENS_POSE_TRANSLATION = new Key<>("android.lens.poseTranslation", float[].class);
    public static final Key<float[]> LENS_RADIAL_DISTORTION = new Key<>("android.lens.radialDistortion", float[].class);
    public static final Key<Integer> LENS_STATE = new Key<>("android.lens.state", Integer.class);
    public static final Key<String> LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID =
            new Key<>("android.logicalMultiCamera.activePhysicalId", String.class);
    public static final Key<Rect> LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_SENSOR_CROP_REGION =
            new Key<>("android.logicalMultiCamera.activePhysicalSensorCropRegion", Rect.class);
    public static final Key<Integer> NOISE_REDUCTION_MODE = new Key<>("android.noiseReduction.mode", Integer.class);
    public static final Key<Float> REPROCESS_EFFECTIVE_EXPOSURE_FACTOR =
            new Key<>("android.reprocess.effectiveExposureFactor", Float.class);
    public static final Key<Byte> REQUEST_PIPELINE_DEPTH = new Key<>("android.request.pipelineDepth", Byte.class);
    public static final Key<Rect> SCALER_CROP_REGION = new Key<>("android.scaler.cropRegion", Rect.class);
    public static final Key<Rect> SCALER_RAW_CROP_REGION = new Key<>("android.scaler.rawCropRegion", Rect.class);
    public static final Key<Integer> SCALER_ROTATE_AND_CROP = new Key<>("android.scaler.rotateAndCrop", Integer.class);
    public static final Key<float[]> SENSOR_DYNAMIC_BLACK_LEVEL = new Key<>("android.sensor.dynamicBlackLevel", float[].class);
    public static final Key<Integer> SENSOR_DYNAMIC_WHITE_LEVEL = new Key<>("android.sensor.dynamicWhiteLevel", Integer.class);
    public static final Key<Long> SENSOR_EXPOSURE_TIME = new Key<>("android.sensor.exposureTime", Long.class);
    public static final Key<Long> SENSOR_FRAME_DURATION = new Key<>("android.sensor.frameDuration", Long.class);
    public static final Key<Float> SENSOR_GREEN_SPLIT = new Key<>("android.sensor.greenSplit", Float.class);
    public static final Key<Rational[]> SENSOR_NEUTRAL_COLOR_POINT = new Key<>("android.sensor.neutralColorPoint", Rational[].class);
    public static final Key<Pair<Double, Double>[]> SENSOR_NOISE_PROFILE = new Key<>("android.sensor.noiseProfile");
    public static final Key<Integer> SENSOR_PIXEL_MODE = new Key<>("android.sensor.pixelMode", Integer.class);
    public static final Key<Boolean> SENSOR_RAW_BINNING_FACTOR_USED = new Key<>("android.sensor.rawBinningFactorUsed", Boolean.class);
    public static final Key<Long> SENSOR_ROLLING_SHUTTER_SKEW = new Key<>("android.sensor.rollingShutterSkew", Long.class);
    public static final Key<Integer> SENSOR_SENSITIVITY = new Key<>("android.sensor.sensitivity", Integer.class);
    public static final Key<int[]> SENSOR_TEST_PATTERN_DATA = new Key<>("android.sensor.testPatternData", int[].class);
    public static final Key<Integer> SENSOR_TEST_PATTERN_MODE = new Key<>("android.sensor.testPatternMode", Integer.class);
    public static final Key<Long> SENSOR_TIMESTAMP = new Key<>("android.sensor.timestamp", Long.class);
    public static final Key<Integer> SHADING_MODE = new Key<>("android.shading.mode", Integer.class);
    public static final Key<Face[]> STATISTICS_FACES = new Key<>("android.statistics.faces", Face[].class);
    public static final Key<Integer> STATISTICS_FACE_DETECT_MODE = new Key<>("android.statistics.faceDetectMode", Integer.class);
    public static final Key<Point[]> STATISTICS_HOT_PIXEL_MAP = new Key<>("android.statistics.hotPixelMap", Point[].class);
    public static final Key<Boolean> STATISTICS_HOT_PIXEL_MAP_MODE = new Key<>("android.statistics.hotPixelMapMode", Boolean.class);
    public static final Key<LensIntrinsicsSample[]> STATISTICS_LENS_INTRINSICS_SAMPLES =
            new Key<>("android.statistics.lensIntrinsicsSamples", LensIntrinsicsSample[].class);
    public static final Key<LensShadingMap> STATISTICS_LENS_SHADING_CORRECTION_MAP =
            new Key<>("android.statistics.lensShadingCorrectionMap", LensShadingMap.class);
    public static final Key<Integer> STATISTICS_LENS_SHADING_MAP_MODE = new Key<>("android.statistics.lensShadingMapMode", Integer.class);
    public static final Key<Integer> STATISTICS_OIS_DATA_MODE = new Key<>("android.statistics.oisDataMode", Integer.class);
    public static final Key<OisSample[]> STATISTICS_OIS_SAMPLES = new Key<>("android.statistics.oisSamples", OisSample[].class);
    public static final Key<Integer> STATISTICS_SCENE_FLICKER = new Key<>("android.statistics.sceneFlicker", Integer.class);
    public static final Key<TonemapCurve> TONEMAP_CURVE = new Key<>("android.tonemap.curve", TonemapCurve.class);
    public static final Key<Float> TONEMAP_GAMMA = new Key<>("android.tonemap.gamma", Float.class);
    public static final Key<Integer> TONEMAP_MODE = new Key<>("android.tonemap.mode", Integer.class);
    public static final Key<Integer> TONEMAP_PRESET_CURVE = new Key<>("android.tonemap.presetCurve", Integer.class);

    private final String mCameraId;
    private final CaptureRequest mRequest;
    private final int mSequenceId;
    private final long mFrameNumber;

    CaptureResult(String cameraId, Map<Key<?>, Object> values, CaptureRequest request, int sequenceId, long frameNumber)
    {
        super(values);
        mCameraId = cameraId;
        mRequest = request;
        mSequenceId = sequenceId;
        mFrameNumber = frameNumber;
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

    public String getCameraId()
    {
        return mCameraId;
    }

    public CaptureRequest getRequest()
    {
        return mRequest;
    }

    public long getFrameNumber()
    {
        return mFrameNumber;
    }

    public int getSequenceId()
    {
        return mSequenceId;
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
            return "CaptureResult.Key(" + mName + ")";
        }
    }
}
