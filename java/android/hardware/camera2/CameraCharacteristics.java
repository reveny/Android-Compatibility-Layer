package android.hardware.camera2;

import android.graphics.Rect;
import android.hardware.camera2.params.BlackLevelPattern;
import android.hardware.camera2.params.Capability;
import android.hardware.camera2.params.ColorSpaceProfiles;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.DeviceStateSensorOrientationMap;
import android.hardware.camera2.params.DynamicRangeProfiles;
import android.hardware.camera2.params.MandatoryStreamCombination;
import android.hardware.camera2.params.MultiResolutionStreamConfigurationMap;
import android.hardware.camera2.params.RecommendedStreamConfigurationMap;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.util.SizeF;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CameraCharacteristics extends CameraMetadata<CameraCharacteristics.Key<?>>
{
    public static final Key<int[]> AUTOMOTIVE_LENS_FACING = new Key<>("android.automotive.lens.facing", int[].class);
    public static final Key<Integer> AUTOMOTIVE_LOCATION = new Key<>("android.automotive.location", Integer.class);
    public static final Key<int[]> COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES =
            new Key<>("android.colorCorrection.availableAberrationModes", int[].class);
    public static final Key<int[]> COLOR_CORRECTION_AVAILABLE_MODES = new Key<>("android.colorCorrection.availableModes", int[].class);
    public static final Key<Range<Integer>> COLOR_CORRECTION_COLOR_TEMPERATURE_RANGE =
            new Key<>("android.colorCorrection.colorTemperatureRange");
    public static final Key<int[]> CONTROL_AE_AVAILABLE_ANTIBANDING_MODES =
            new Key<>("android.control.aeAvailableAntibandingModes", int[].class);
    public static final Key<int[]> CONTROL_AE_AVAILABLE_MODES = new Key<>("android.control.aeAvailableModes", int[].class);
    public static final Key<int[]> CONTROL_AE_AVAILABLE_PRIORITY_MODES = new Key<>("android.control.aeAvailablePriorityModes", int[].class);
    public static final Key<Range<Integer>[]> CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES =
            new Key<>("android.control.aeAvailableTargetFpsRanges");
    public static final Key<Range<Integer>> CONTROL_AE_COMPENSATION_RANGE = new Key<>("android.control.aeCompensationRange");
    public static final Key<Rational> CONTROL_AE_COMPENSATION_STEP = new Key<>("android.control.aeCompensationStep", Rational.class);
    public static final Key<Boolean> CONTROL_AE_LOCK_AVAILABLE = new Key<>("android.control.aeLockAvailable", Boolean.class);
    public static final Key<int[]> CONTROL_AF_AVAILABLE_MODES = new Key<>("android.control.afAvailableModes", int[].class);
    public static final Key<Boolean> CONTROL_AUTOFRAMING_AVAILABLE = new Key<>("android.control.autoframingAvailable", Boolean.class);
    public static final Key<int[]> CONTROL_AVAILABLE_EFFECTS = new Key<>("android.control.availableEffects", int[].class);
    public static final Key<Capability[]> CONTROL_AVAILABLE_EXTENDED_SCENE_MODE_CAPABILITIES =
            new Key<>("android.control.availableExtendedSceneModeCapabilities", Capability[].class);
    public static final Key<int[]> CONTROL_AVAILABLE_MODES = new Key<>("android.control.availableModes", int[].class);
    public static final Key<int[]> CONTROL_AVAILABLE_SCENE_MODES = new Key<>("android.control.availableSceneModes", int[].class);
    public static final Key<int[]> CONTROL_AVAILABLE_SETTINGS_OVERRIDES =
            new Key<>("android.control.availableSettingsOverrides", int[].class);
    public static final Key<int[]> CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES =
            new Key<>("android.control.availableVideoStabilizationModes", int[].class);
    public static final Key<int[]> CONTROL_AWB_AVAILABLE_MODES = new Key<>("android.control.awbAvailableModes", int[].class);
    public static final Key<Boolean> CONTROL_AWB_LOCK_AVAILABLE = new Key<>("android.control.awbLockAvailable", Boolean.class);
    public static final Key<Range<Float>> CONTROL_LOW_LIGHT_BOOST_INFO_LUMINANCE_RANGE =
            new Key<>("android.control.lowLightBoostInfoLuminanceRange");
    public static final Key<Integer> CONTROL_MAX_REGIONS_AE = new Key<>("android.control.maxRegionsAe", Integer.class);
    public static final Key<Integer> CONTROL_MAX_REGIONS_AF = new Key<>("android.control.maxRegionsAf", Integer.class);
    public static final Key<Integer> CONTROL_MAX_REGIONS_AWB = new Key<>("android.control.maxRegionsAwb", Integer.class);
    public static final Key<Range<Integer>> CONTROL_POST_RAW_SENSITIVITY_BOOST_RANGE =
            new Key<>("android.control.postRawSensitivityBoostRange");
    public static final Key<Range<Float>> CONTROL_ZOOM_RATIO_RANGE = new Key<>("android.control.zoomRatioRange");
    public static final Key<Boolean> DEPTH_DEPTH_IS_EXCLUSIVE = new Key<>("android.depth.depthIsExclusive", Boolean.class);
    public static final Key<int[]> DISTORTION_CORRECTION_AVAILABLE_MODES =
            new Key<>("android.distortionCorrection.availableModes", int[].class);
    public static final Key<int[]> EDGE_AVAILABLE_EDGE_MODES = new Key<>("android.edge.availableEdgeModes", int[].class);
    public static final Key<Boolean> FLASH_INFO_AVAILABLE = new Key<>("android.flash.info.available", Boolean.class);
    public static final Key<Integer> FLASH_INFO_STRENGTH_DEFAULT_LEVEL =
            new Key<>("android.flash.info.strengthDefaultLevel", Integer.class);
    public static final Key<Integer> FLASH_INFO_STRENGTH_MAXIMUM_LEVEL =
            new Key<>("android.flash.info.strengthMaximumLevel", Integer.class);
    public static final Key<Integer> FLASH_SINGLE_STRENGTH_DEFAULT_LEVEL =
            new Key<>("android.flash.singleStrengthDefaultLevel", Integer.class);
    public static final Key<Integer> FLASH_SINGLE_STRENGTH_MAX_LEVEL = new Key<>("android.flash.singleStrengthMaxLevel", Integer.class);
    public static final Key<Integer> FLASH_TORCH_STRENGTH_DEFAULT_LEVEL =
            new Key<>("android.flash.torchStrengthDefaultLevel", Integer.class);
    public static final Key<Integer> FLASH_TORCH_STRENGTH_MAX_LEVEL = new Key<>("android.flash.torchStrengthMaxLevel", Integer.class);
    public static final Key<int[]> HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES = new Key<>("android.hotPixel.availableHotPixelModes", int[].class);
    public static final Key<DeviceStateSensorOrientationMap> INFO_DEVICE_STATE_SENSOR_ORIENTATION_MAP =
            new Key<>("android.info.deviceStateSensorOrientationMap", DeviceStateSensorOrientationMap.class);
    public static final Key<Integer> INFO_SESSION_CONFIGURATION_QUERY_VERSION =
            new Key<>("android.info.sessionConfigurationQueryVersion", Integer.class);
    public static final Key<Integer> INFO_SUPPORTED_HARDWARE_LEVEL = new Key<>("android.info.supportedHardwareLevel", Integer.class);
    public static final Key<String> INFO_VERSION = new Key<>("android.info.version", String.class);
    public static final Key<Size[]> JPEG_AVAILABLE_THUMBNAIL_SIZES = new Key<>("android.jpeg.availableThumbnailSizes", Size[].class);
    public static final Key<float[]> LENS_DISTORTION = new Key<>("android.lens.distortion", float[].class);
    public static final Key<float[]> LENS_DISTORTION_MAXIMUM_RESOLUTION =
            new Key<>("android.lens.distortionMaximumResolution", float[].class);
    public static final Key<Integer> LENS_FACING = new Key<>("android.lens.facing", Integer.class);
    public static final Key<float[]> LENS_INFO_AVAILABLE_APERTURES = new Key<>("android.lens.info.availableApertures", float[].class);
    public static final Key<float[]> LENS_INFO_AVAILABLE_FILTER_DENSITIES =
            new Key<>("android.lens.info.availableFilterDensities", float[].class);
    public static final Key<float[]> LENS_INFO_AVAILABLE_FOCAL_LENGTHS =
            new Key<>("android.lens.info.availableFocalLengths", float[].class);
    public static final Key<int[]> LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION =
            new Key<>("android.lens.info.availableOpticalStabilization", int[].class);
    public static final Key<Integer> LENS_INFO_FOCUS_DISTANCE_CALIBRATION =
            new Key<>("android.lens.info.focusDistanceCalibration", Integer.class);
    public static final Key<Float> LENS_INFO_HYPERFOCAL_DISTANCE = new Key<>("android.lens.info.hyperfocalDistance", Float.class);
    public static final Key<Float> LENS_INFO_MINIMUM_FOCUS_DISTANCE = new Key<>("android.lens.info.minimumFocusDistance", Float.class);
    public static final Key<float[]> LENS_INTRINSIC_CALIBRATION = new Key<>("android.lens.intrinsicCalibration", float[].class);
    public static final Key<float[]> LENS_INTRINSIC_CALIBRATION_MAXIMUM_RESOLUTION =
            new Key<>("android.lens.intrinsicCalibrationMaximumResolution", float[].class);
    public static final Key<Integer> LENS_POSE_REFERENCE = new Key<>("android.lens.poseReference", Integer.class);
    public static final Key<float[]> LENS_POSE_ROTATION = new Key<>("android.lens.poseRotation", float[].class);
    public static final Key<float[]> LENS_POSE_TRANSLATION = new Key<>("android.lens.poseTranslation", float[].class);
    public static final Key<float[]> LENS_RADIAL_DISTORTION = new Key<>("android.lens.radialDistortion", float[].class);
    public static final Key<Integer> LOGICAL_MULTI_CAMERA_SENSOR_SYNC_TYPE =
            new Key<>("android.logicalMultiCamera.sensorSyncType", Integer.class);
    public static final Key<int[]> NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES =
            new Key<>("android.noiseReduction.availableNoiseReductionModes", int[].class);
    public static final Key<Integer> REPROCESS_MAX_CAPTURE_STALL = new Key<>("android.reprocess.maxCaptureStall", Integer.class);
    public static final Key<int[]> REQUEST_AVAILABLE_CAPABILITIES = new Key<>("android.request.availableCapabilities", int[].class);
    public static final Key<ColorSpaceProfiles> REQUEST_AVAILABLE_COLOR_SPACE_PROFILES =
            new Key<>("android.request.availableColorSpaceProfiles", ColorSpaceProfiles.class);
    public static final Key<DynamicRangeProfiles> REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES =
            new Key<>("android.request.availableDynamicRangeProfiles", DynamicRangeProfiles.class);
    public static final Key<Integer> REQUEST_MAX_NUM_INPUT_STREAMS = new Key<>("android.request.maxNumInputStreams", Integer.class);
    public static final Key<Integer> REQUEST_MAX_NUM_OUTPUT_PROC = new Key<>("android.request.maxNumOutputProc", Integer.class);
    public static final Key<Integer> REQUEST_MAX_NUM_OUTPUT_PROC_STALLING =
            new Key<>("android.request.maxNumOutputProcStalling", Integer.class);
    public static final Key<Integer> REQUEST_MAX_NUM_OUTPUT_RAW = new Key<>("android.request.maxNumOutputRaw", Integer.class);
    public static final Key<Integer> REQUEST_PARTIAL_RESULT_COUNT = new Key<>("android.request.partialResultCount", Integer.class);
    public static final Key<Byte> REQUEST_PIPELINE_MAX_DEPTH = new Key<>("android.request.pipelineMaxDepth", Byte.class);
    public static final Key<Long> REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE =
            new Key<>("android.request.recommendedTenBitDynamicRangeProfile", Long.class);
    public static final Key<Float> SCALER_AVAILABLE_MAX_DIGITAL_ZOOM = new Key<>("android.scaler.availableMaxDigitalZoom", Float.class);
    public static final Key<int[]> SCALER_AVAILABLE_ROTATE_AND_CROP_MODES =
            new Key<>("android.scaler.availableRotateAndCropModes", int[].class);
    public static final Key<long[]> SCALER_AVAILABLE_STREAM_USE_CASES = new Key<>("android.scaler.availableStreamUseCases", long[].class);
    public static final Key<Integer> SCALER_CROPPING_TYPE = new Key<>("android.scaler.croppingType", Integer.class);
    public static final Key<Size> SCALER_DEFAULT_SECURE_IMAGE_SIZE = new Key<>("android.scaler.defaultSecureImageSize", Size.class);
    public static final Key<MandatoryStreamCombination[]> SCALER_MANDATORY_CONCURRENT_STREAM_COMBINATIONS =
            new Key<>("android.scaler.mandatoryConcurrentStreamCombinations", MandatoryStreamCombination[].class);
    public static final Key<MandatoryStreamCombination[]> SCALER_MANDATORY_MAXIMUM_RESOLUTION_STREAM_COMBINATIONS =
            new Key<>("android.scaler.mandatoryMaximumResolutionStreamCombinations", MandatoryStreamCombination[].class);
    public static final Key<MandatoryStreamCombination[]> SCALER_MANDATORY_PREVIEW_STABILIZATION_OUTPUT_STREAM_COMBINATIONS =
            new Key<>("android.scaler.mandatoryPreviewStabilizationOutputStreamCombinations", MandatoryStreamCombination[].class);
    public static final Key<MandatoryStreamCombination[]> SCALER_MANDATORY_STREAM_COMBINATIONS =
            new Key<>("android.scaler.mandatoryStreamCombinations", MandatoryStreamCombination[].class);
    public static final Key<MandatoryStreamCombination[]> SCALER_MANDATORY_TEN_BIT_OUTPUT_STREAM_COMBINATIONS =
            new Key<>("android.scaler.mandatoryTenBitOutputStreamCombinations", MandatoryStreamCombination[].class);
    public static final Key<MandatoryStreamCombination[]> SCALER_MANDATORY_USE_CASE_STREAM_COMBINATIONS =
            new Key<>("android.scaler.mandatoryUseCaseStreamCombinations", MandatoryStreamCombination[].class);
    public static final Key<MultiResolutionStreamConfigurationMap> SCALER_MULTI_RESOLUTION_STREAM_CONFIGURATION_MAP =
            new Key<>("android.scaler.multiResolutionStreamConfigurationMap", MultiResolutionStreamConfigurationMap.class);
    public static final Key<StreamConfigurationMap> SCALER_STREAM_CONFIGURATION_MAP =
            new Key<>("android.scaler.streamConfigurationMap", StreamConfigurationMap.class);
    public static final Key<StreamConfigurationMap> SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION =
            new Key<>("android.scaler.streamConfigurationMapMaximumResolution", StreamConfigurationMap.class);
    public static final Key<int[]> SENSOR_AVAILABLE_TEST_PATTERN_MODES = new Key<>("android.sensor.availableTestPatternModes", int[].class);
    public static final Key<BlackLevelPattern> SENSOR_BLACK_LEVEL_PATTERN =
            new Key<>("android.sensor.blackLevelPattern", BlackLevelPattern.class);
    public static final Key<ColorSpaceTransform> SENSOR_CALIBRATION_TRANSFORM1 =
            new Key<>("android.sensor.calibrationTransform1", ColorSpaceTransform.class);
    public static final Key<ColorSpaceTransform> SENSOR_CALIBRATION_TRANSFORM2 =
            new Key<>("android.sensor.calibrationTransform2", ColorSpaceTransform.class);
    public static final Key<ColorSpaceTransform> SENSOR_COLOR_TRANSFORM1 =
            new Key<>("android.sensor.colorTransform1", ColorSpaceTransform.class);
    public static final Key<ColorSpaceTransform> SENSOR_COLOR_TRANSFORM2 =
            new Key<>("android.sensor.colorTransform2", ColorSpaceTransform.class);
    public static final Key<ColorSpaceTransform> SENSOR_FORWARD_MATRIX1 =
            new Key<>("android.sensor.forwardMatrix1", ColorSpaceTransform.class);
    public static final Key<ColorSpaceTransform> SENSOR_FORWARD_MATRIX2 =
            new Key<>("android.sensor.forwardMatrix2", ColorSpaceTransform.class);
    public static final Key<Rect> SENSOR_INFO_ACTIVE_ARRAY_SIZE = new Key<>("android.sensor.info.activeArraySize", Rect.class);
    public static final Key<Rect> SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION =
            new Key<>("android.sensor.info.activeArraySizeMaximumResolution", Rect.class);
    public static final Key<Size> SENSOR_INFO_BINNING_FACTOR = new Key<>("android.sensor.info.binningFactor", Size.class);
    public static final Key<Integer> SENSOR_INFO_COLOR_FILTER_ARRANGEMENT =
            new Key<>("android.sensor.info.colorFilterArrangement", Integer.class);
    public static final Key<Range<Long>> SENSOR_INFO_EXPOSURE_TIME_RANGE = new Key<>("android.sensor.info.exposureTimeRange");
    public static final Key<Boolean> SENSOR_INFO_LENS_SHADING_APPLIED = new Key<>("android.sensor.info.lensShadingApplied", Boolean.class);
    public static final Key<Long> SENSOR_INFO_MAX_FRAME_DURATION = new Key<>("android.sensor.info.maxFrameDuration", Long.class);
    public static final Key<SizeF> SENSOR_INFO_PHYSICAL_SIZE = new Key<>("android.sensor.info.physicalSize", SizeF.class);
    public static final Key<Size> SENSOR_INFO_PIXEL_ARRAY_SIZE = new Key<>("android.sensor.info.pixelArraySize", Size.class);
    public static final Key<Size> SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION =
            new Key<>("android.sensor.info.pixelArraySizeMaximumResolution", Size.class);
    public static final Key<Rect> SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE =
            new Key<>("android.sensor.info.preCorrectionActiveArraySize", Rect.class);
    public static final Key<Rect> SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION =
            new Key<>("android.sensor.info.preCorrectionActiveArraySizeMaximumResolution", Rect.class);
    public static final Key<Range<Integer>> SENSOR_INFO_SENSITIVITY_RANGE = new Key<>("android.sensor.info.sensitivityRange");
    public static final Key<Integer> SENSOR_INFO_TIMESTAMP_SOURCE = new Key<>("android.sensor.info.timestampSource", Integer.class);
    public static final Key<Integer> SENSOR_INFO_WHITE_LEVEL = new Key<>("android.sensor.info.whiteLevel", Integer.class);
    public static final Key<Integer> SENSOR_MAX_ANALOG_SENSITIVITY = new Key<>("android.sensor.maxAnalogSensitivity", Integer.class);
    public static final Key<Rect[]> SENSOR_OPTICAL_BLACK_REGIONS = new Key<>("android.sensor.opticalBlackRegions", Rect[].class);
    public static final Key<Integer> SENSOR_ORIENTATION = new Key<>("android.sensor.orientation", Integer.class);
    public static final Key<Integer> SENSOR_READOUT_TIMESTAMP = new Key<>("android.sensor.readoutTimestamp", Integer.class);
    public static final Key<Integer> SENSOR_REFERENCE_ILLUMINANT1 = new Key<>("android.sensor.referenceIlluminant1", Integer.class);
    public static final Key<Byte> SENSOR_REFERENCE_ILLUMINANT2 = new Key<>("android.sensor.referenceIlluminant2", Byte.class);
    public static final Key<int[]> SHADING_AVAILABLE_MODES = new Key<>("android.shading.availableModes", int[].class);
    public static final Key<int[]> STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES =
            new Key<>("android.statistics.info.availableFaceDetectModes", int[].class);
    public static final Key<boolean[]> STATISTICS_INFO_AVAILABLE_HOT_PIXEL_MAP_MODES =
            new Key<>("android.statistics.info.availableHotPixelMapModes", boolean[].class);
    public static final Key<int[]> STATISTICS_INFO_AVAILABLE_LENS_SHADING_MAP_MODES =
            new Key<>("android.statistics.info.availableLensShadingMapModes", int[].class);
    public static final Key<int[]> STATISTICS_INFO_AVAILABLE_OIS_DATA_MODES =
            new Key<>("android.statistics.info.availableOisDataModes", int[].class);
    public static final Key<Integer> STATISTICS_INFO_MAX_FACE_COUNT = new Key<>("android.statistics.info.maxFaceCount", Integer.class);
    public static final Key<Integer> SYNC_MAX_LATENCY = new Key<>("android.sync.maxLatency", Integer.class);
    public static final Key<int[]> TONEMAP_AVAILABLE_TONE_MAP_MODES = new Key<>("android.tonemap.availableToneMapModes", int[].class);
    public static final Key<Integer> TONEMAP_MAX_CURVE_POINTS = new Key<>("android.tonemap.maxCurvePoints", Integer.class);

    private final Set<String> mPhysicalCameraIds;

    CameraCharacteristics(Map<Key<?>, Object> values, Set<String> physicalCameraIds)
    {
        super(values);
        mPhysicalCameraIds = Collections.unmodifiableSet(physicalCameraIds);
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

    public List<CaptureRequest.Key<?>> getAvailableCaptureRequestKeys()
    {
        return Collections.emptyList();
    }

    public List<CaptureResult.Key<?>> getAvailableCaptureResultKeys()
    {
        return Collections.emptyList();
    }

    public List<CaptureRequest.Key<?>> getAvailablePhysicalCameraRequestKeys()
    {
        return null;
    }

    public List<CameraCharacteristics.Key<?>> getAvailableSessionCharacteristicsKeys()
    {
        return Collections.emptyList();
    }

    public List<CaptureRequest.Key<?>> getAvailableSessionKeys()
    {
        return null;
    }

    public List<CameraCharacteristics.Key<?>> getKeysNeedingPermission()
    {
        return Collections.emptyList();
    }

    public Set<String> getPhysicalCameraIds()
    {
        return mPhysicalCameraIds;
    }

    public RecommendedStreamConfigurationMap getRecommendedStreamConfigurationMap(int usecase)
    {
        if (usecase < RecommendedStreamConfigurationMap.USECASE_PREVIEW || usecase > RecommendedStreamConfigurationMap.USECASE_10BIT_OUTPUT)
        {
            throw new IllegalArgumentException("Invalid use case: " + usecase);
        }
        return null;
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
            return "CameraCharacteristics.Key(" + mName + ")";
        }
    }
}
