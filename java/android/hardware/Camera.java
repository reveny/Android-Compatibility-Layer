package android.hardware;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Camera
{
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";
    public static final String ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO";

    public static final int CAMERA_ERROR_UNKNOWN = 1;
    public static final int CAMERA_ERROR_EVICTED = 2;
    public static final int CAMERA_ERROR_SERVER_DIED = 100;

    private static final int IMAGE_FORMAT_NV21 = 17;
    private static final int IMAGE_FORMAT_JPEG = 256;

    private final Parameters mParameters;

    Camera()
    {
        mParameters = new Parameters();
    }

    public static int getNumberOfCameras()
    {
        return 0;
    }

    public static void getCameraInfo(int cameraId, CameraInfo cameraInfo)
    {
        throw new RuntimeException("Unknown camera ID");
    }

    public static Camera open(int cameraId)
    {
        throw new RuntimeException("Fail to connect to camera service");
    }

    public static Camera open()
    {
        return null;
    }

    public final void release()
    {
    }

    public final void unlock()
    {
    }

    public final void lock()
    {
    }

    public final void reconnect() throws IOException
    {
        throw new IOException("reconnect failed");
    }

    public final void setPreviewDisplay(SurfaceHolder holder) throws IOException
    {
    }

    public final void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException
    {
    }

    public final void startPreview()
    {
    }

    public final void stopPreview()
    {
    }

    public final void setPreviewCallback(PreviewCallback callback)
    {
    }

    public final void setOneShotPreviewCallback(PreviewCallback callback)
    {
    }

    public final void setPreviewCallbackWithBuffer(PreviewCallback callback)
    {
    }

    public final void addCallbackBuffer(byte[] callbackBuffer)
    {
    }

    public final void autoFocus(AutoFocusCallback callback)
    {
        if (callback != null) callback.onAutoFocus(false, this);
    }

    public final void cancelAutoFocus()
    {
    }

    public void setAutoFocusMoveCallback(AutoFocusMoveCallback callback)
    {
    }

    public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback jpeg)
    {
        takePicture(shutter, raw, null, jpeg);
    }

    public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postview, PictureCallback jpeg)
    {
        throw new RuntimeException("takePicture failed");
    }

    public final void startSmoothZoom(int value)
    {
    }

    public final void stopSmoothZoom()
    {
    }

    public final void setDisplayOrientation(int degrees)
    {
    }

    public final boolean enableShutterSound(boolean enabled)
    {
        return false;
    }

    public final void setZoomChangeListener(OnZoomChangeListener listener)
    {
    }

    public final void setFaceDetectionListener(FaceDetectionListener listener)
    {
    }

    public final void startFaceDetection()
    {
        throw new IllegalArgumentException("face detection is not supported");
    }

    public final void stopFaceDetection()
    {
    }

    public final void setErrorCallback(ErrorCallback callback)
    {
    }

    public void setParameters(Parameters params)
    {
        if (params != null) mParameters.unflatten(params.flatten());
    }

    public Parameters getParameters()
    {
        Parameters copy = new Parameters();
        copy.unflatten(mParameters.flatten());
        return copy;
    }

    public static class CameraInfo
    {
        public static final int CAMERA_FACING_BACK = 0;
        public static final int CAMERA_FACING_FRONT = 1;

        public int facing;
        public int orientation;
        public boolean canDisableShutterSound;

        public CameraInfo()
        {
        }
    }

    public interface PreviewCallback
    {
        void onPreviewFrame(byte[] data, Camera camera);
    }

    public interface AutoFocusCallback
    {
        void onAutoFocus(boolean success, Camera camera);
    }

    public interface AutoFocusMoveCallback
    {
        void onAutoFocusMoving(boolean start, Camera camera);
    }

    public interface ShutterCallback
    {
        void onShutter();
    }

    public interface PictureCallback
    {
        void onPictureTaken(byte[] data, Camera camera);
    }

    public interface OnZoomChangeListener
    {
        void onZoomChange(int zoomValue, boolean stopped, Camera camera);
    }

    public interface FaceDetectionListener
    {
        void onFaceDetection(Face[] faces, Camera camera);
    }

    public interface ErrorCallback
    {
        void onError(int error, Camera camera);
    }

    public static class Face
    {
        public Rect rect;
        public int score;
        public int id = -1;
        public Point leftEye;
        public Point rightEye;
        public Point mouth;

        public Face()
        {
        }
    }

    public class Size
    {
        public int width;
        public int height;

        public Size(int width, int height)
        {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof Size)) return false;

            Size size = (Size)other;
            return width == size.width && height == size.height;
        }

        @Override
        public int hashCode()
        {
            return width * 32713 + height;
        }
    }

    public static class Area
    {
        public Rect rect;
        public int weight;

        public Area(Rect rect, int weight)
        {
            this.rect = rect;
            this.weight = weight;
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof Area)) return false;

            Area area = (Area)other;
            if (rect == null ? area.rect != null : !rect.equals(area.rect)) return false;
            return weight == area.weight;
        }
    }

    public class Parameters
    {
        public static final String WHITE_BALANCE_AUTO = "auto";
        public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
        public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
        public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
        public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
        public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
        public static final String WHITE_BALANCE_TWILIGHT = "twilight";
        public static final String WHITE_BALANCE_SHADE = "shade";

        public static final String EFFECT_NONE = "none";
        public static final String EFFECT_MONO = "mono";
        public static final String EFFECT_NEGATIVE = "negative";
        public static final String EFFECT_SOLARIZE = "solarize";
        public static final String EFFECT_SEPIA = "sepia";
        public static final String EFFECT_POSTERIZE = "posterize";
        public static final String EFFECT_WHITEBOARD = "whiteboard";
        public static final String EFFECT_BLACKBOARD = "blackboard";
        public static final String EFFECT_AQUA = "aqua";

        public static final String ANTIBANDING_AUTO = "auto";
        public static final String ANTIBANDING_50HZ = "50hz";
        public static final String ANTIBANDING_60HZ = "60hz";
        public static final String ANTIBANDING_OFF = "off";

        public static final String FLASH_MODE_OFF = "off";
        public static final String FLASH_MODE_AUTO = "auto";
        public static final String FLASH_MODE_ON = "on";
        public static final String FLASH_MODE_RED_EYE = "red-eye";
        public static final String FLASH_MODE_TORCH = "torch";

        public static final String SCENE_MODE_AUTO = "auto";
        public static final String SCENE_MODE_ACTION = "action";
        public static final String SCENE_MODE_PORTRAIT = "portrait";
        public static final String SCENE_MODE_LANDSCAPE = "landscape";
        public static final String SCENE_MODE_NIGHT = "night";
        public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
        public static final String SCENE_MODE_THEATRE = "theatre";
        public static final String SCENE_MODE_BEACH = "beach";
        public static final String SCENE_MODE_SNOW = "snow";
        public static final String SCENE_MODE_SUNSET = "sunset";
        public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
        public static final String SCENE_MODE_FIREWORKS = "fireworks";
        public static final String SCENE_MODE_SPORTS = "sports";
        public static final String SCENE_MODE_PARTY = "party";
        public static final String SCENE_MODE_CANDLELIGHT = "candlelight";
        public static final String SCENE_MODE_BARCODE = "barcode";
        public static final String SCENE_MODE_HDR = "hdr";

        public static final String FOCUS_MODE_AUTO = "auto";
        public static final String FOCUS_MODE_INFINITY = "infinity";
        public static final String FOCUS_MODE_MACRO = "macro";
        public static final String FOCUS_MODE_FIXED = "fixed";
        public static final String FOCUS_MODE_EDOF = "edof";
        public static final String FOCUS_MODE_CONTINUOUS_VIDEO = "continuous-video";
        public static final String FOCUS_MODE_CONTINUOUS_PICTURE = "continuous-picture";

        public static final int FOCUS_DISTANCE_NEAR_INDEX = 0;
        public static final int FOCUS_DISTANCE_OPTIMAL_INDEX = 1;
        public static final int FOCUS_DISTANCE_FAR_INDEX = 2;

        public static final int PREVIEW_FPS_MIN_INDEX = 0;
        public static final int PREVIEW_FPS_MAX_INDEX = 1;

        private final LinkedHashMap<String, String> mMap = new LinkedHashMap<>();
        private List<Area> mFocusAreas;
        private List<Area> mMeteringAreas;

        Parameters()
        {
        }

        public String flatten()
        {
            StringBuilder flattened = new StringBuilder();

            for (Map.Entry<String, String> entry : mMap.entrySet())
            {
                if (flattened.length() > 0) flattened.append(';');
                flattened.append(entry.getKey()).append('=').append(entry.getValue());
            }

            return flattened.toString();
        }

        public void unflatten(String flattened)
        {
            mMap.clear();
            if (flattened == null || flattened.isEmpty()) return;

            for (String pair : flattened.split(";"))
            {
                int separator = pair.indexOf('=');
                if (separator <= 0) continue;
                mMap.put(pair.substring(0, separator), pair.substring(separator + 1));
            }
        }

        public void remove(String key)
        {
            mMap.remove(key);
        }

        public void set(String key, String value)
        {
            if (key == null || value == null) return;
            if (key.indexOf('=') != -1 || key.indexOf(';') != -1 || value.indexOf('=') != -1 || value.indexOf(';') != -1) return;
            mMap.put(key, value);
        }

        public void set(String key, int value)
        {
            set(key, Integer.toString(value));
        }

        public String get(String key)
        {
            return mMap.get(key);
        }

        public int getInt(String key)
        {
            String value = mMap.get(key);
            if (value == null) throw new NumberFormatException("null");
            return Integer.parseInt(value);
        }

        private int getInt(String key, int defaultValue)
        {
            try
            {
                return getInt(key);
            }
            catch (NumberFormatException exception)
            {
                return defaultValue;
            }
        }

        private float getFloat(String key, float defaultValue)
        {
            String value = mMap.get(key);
            if (value == null) return defaultValue;

            try
            {
                return Float.parseFloat(value);
            }
            catch (NumberFormatException exception)
            {
                return defaultValue;
            }
        }

        private boolean getBoolean(String key)
        {
            return "true".equals(mMap.get(key));
        }

        private Size getSize(String key)
        {
            String value = mMap.get(key);
            if (value == null) return null;

            int separator = value.indexOf('x');
            if (separator <= 0) return null;

            try
            {
                return new Size(Integer.parseInt(value.substring(0, separator)), Integer.parseInt(value.substring(separator + 1)));
            }
            catch (NumberFormatException exception)
            {
                return null;
            }
        }

        private List<String> getList(String key)
        {
            String value = mMap.get(key);
            if (value == null) return null;

            List<String> list = new ArrayList<>();
            for (String item : value.split(",")) if (!item.isEmpty()) list.add(item);
            return list;
        }

        private List<Size> getSizeList(String key)
        {
            List<String> items = getList(key);
            if (items == null) return null;

            List<Size> sizes = new ArrayList<>();
            for (String item : items)
            {
                int separator = item.indexOf('x');
                if (separator <= 0) continue;

                try
                {
                    sizes.add(new Size(Integer.parseInt(item.substring(0, separator)), Integer.parseInt(item.substring(separator + 1))));
                }
                catch (NumberFormatException exception)
                {
                }
            }
            return sizes;
        }

        private List<Integer> getIntegerList(String key)
        {
            List<String> items = getList(key);
            if (items == null) return null;

            List<Integer> integers = new ArrayList<>();
            for (String item : items)
            {
                try
                {
                    integers.add(Integer.parseInt(item));
                }
                catch (NumberFormatException exception)
                {
                }
            }
            return integers;
        }

        public void setPreviewSize(int width, int height)
        {
            set("preview-size", width + "x" + height);
        }

        public Size getPreviewSize()
        {
            return getSize("preview-size");
        }

        public List<Size> getSupportedPreviewSizes()
        {
            return getSizeList("preview-size-values");
        }

        public List<Size> getSupportedVideoSizes()
        {
            return getSizeList("video-size-values");
        }

        public Size getPreferredPreviewSizeForVideo()
        {
            return getSize("preferred-preview-size-for-video");
        }

        public void setJpegThumbnailSize(int width, int height)
        {
            set("jpeg-thumbnail-width", width);
            set("jpeg-thumbnail-height", height);
        }

        public Size getJpegThumbnailSize()
        {
            return new Size(getInt("jpeg-thumbnail-width", 0), getInt("jpeg-thumbnail-height", 0));
        }

        public List<Size> getSupportedJpegThumbnailSizes()
        {
            return getSizeList("jpeg-thumbnail-size-values");
        }

        public void setJpegThumbnailQuality(int quality)
        {
            set("jpeg-thumbnail-quality", quality);
        }

        public int getJpegThumbnailQuality()
        {
            return getInt("jpeg-thumbnail-quality", 0);
        }

        public void setJpegQuality(int quality)
        {
            set("jpeg-quality", quality);
        }

        public int getJpegQuality()
        {
            return getInt("jpeg-quality", 0);
        }

        public void setPreviewFrameRate(int fps)
        {
            set("preview-frame-rate", fps);
        }

        public int getPreviewFrameRate()
        {
            return getInt("preview-frame-rate", 0);
        }

        public List<Integer> getSupportedPreviewFrameRates()
        {
            return getIntegerList("preview-frame-rate-values");
        }

        public void setPreviewFpsRange(int min, int max)
        {
            set("preview-fps-range", min + "," + max);
        }

        public void getPreviewFpsRange(int[] range)
        {
            if (range == null || range.length != 2) throw new IllegalArgumentException("range must be an array with two elements.");

            List<Integer> values = getIntegerList("preview-fps-range");
            range[PREVIEW_FPS_MIN_INDEX] = values != null && values.size() == 2 ? values.get(0) : 0;
            range[PREVIEW_FPS_MAX_INDEX] = values != null && values.size() == 2 ? values.get(1) : 0;
        }

        public List<int[]> getSupportedPreviewFpsRange()
        {
            String value = mMap.get("preview-fps-range-values");
            List<int[]> ranges = new ArrayList<>();
            if (value == null) return ranges;

            for (String item : value.split("\\),?\\(?"))
            {
                String trimmed = item.replace("(", "").replace(")", "");
                int separator = trimmed.indexOf(',');
                if (separator <= 0) continue;

                try
                {
                    int minimum = Integer.parseInt(trimmed.substring(0, separator));
                    int maximum = Integer.parseInt(trimmed.substring(separator + 1));
                    ranges.add(new int[] {minimum, maximum});
                }
                catch (NumberFormatException exception)
                {
                }
            }
            return ranges;
        }

        public void setPreviewFormat(int pixelFormat)
        {
            set("preview-format", pixelFormat == IMAGE_FORMAT_JPEG ? "jpeg" : "yuv420sp");
        }

        public int getPreviewFormat()
        {
            return "jpeg".equals(get("preview-format")) ? IMAGE_FORMAT_JPEG : IMAGE_FORMAT_NV21;
        }

        public List<Integer> getSupportedPreviewFormats()
        {
            List<Integer> formats = new ArrayList<>();
            formats.add(IMAGE_FORMAT_NV21);
            return formats;
        }

        public void setPictureSize(int width, int height)
        {
            set("picture-size", width + "x" + height);
        }

        public Size getPictureSize()
        {
            return getSize("picture-size");
        }

        public List<Size> getSupportedPictureSizes()
        {
            return getSizeList("picture-size-values");
        }

        public void setPictureFormat(int pixelFormat)
        {
            set("picture-format", pixelFormat == IMAGE_FORMAT_JPEG ? "jpeg" : "yuv420sp");
        }

        public int getPictureFormat()
        {
            return "yuv420sp".equals(get("picture-format")) ? IMAGE_FORMAT_NV21 : IMAGE_FORMAT_JPEG;
        }

        public List<Integer> getSupportedPictureFormats()
        {
            List<Integer> formats = new ArrayList<>();
            formats.add(IMAGE_FORMAT_JPEG);
            return formats;
        }

        public void setRotation(int rotation)
        {
            if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270)
            {
                throw new IllegalArgumentException("Invalid rotation=" + rotation);
            }
            set("rotation", rotation);
        }

        public void setGpsLatitude(double latitude)
        {
            set("gps-latitude", Double.toString(latitude));
        }

        public void setGpsLongitude(double longitude)
        {
            set("gps-longitude", Double.toString(longitude));
        }

        public void setGpsAltitude(double altitude)
        {
            set("gps-altitude", Double.toString(altitude));
        }

        public void setGpsTimestamp(long timestamp)
        {
            set("gps-timestamp", Long.toString(timestamp));
        }

        public void setGpsProcessingMethod(String processingMethod)
        {
            set("gps-processing-method", processingMethod);
        }

        public void removeGpsData()
        {
            remove("gps-latitude");
            remove("gps-longitude");
            remove("gps-altitude");
            remove("gps-timestamp");
            remove("gps-processing-method");
        }

        public String getWhiteBalance()
        {
            return get("whitebalance");
        }

        public void setWhiteBalance(String value)
        {
            set("whitebalance", value);
        }

        public List<String> getSupportedWhiteBalance()
        {
            return getList("whitebalance-values");
        }

        public String getColorEffect()
        {
            return get("effect");
        }

        public void setColorEffect(String value)
        {
            set("effect", value);
        }

        public List<String> getSupportedColorEffects()
        {
            return getList("effect-values");
        }

        public String getAntibanding()
        {
            return get("antibanding");
        }

        public void setAntibanding(String antibanding)
        {
            set("antibanding", antibanding);
        }

        public List<String> getSupportedAntibanding()
        {
            return getList("antibanding-values");
        }

        public String getSceneMode()
        {
            return get("scene-mode");
        }

        public void setSceneMode(String value)
        {
            set("scene-mode", value);
        }

        public List<String> getSupportedSceneModes()
        {
            return getList("scene-mode-values");
        }

        public String getFlashMode()
        {
            return get("flash-mode");
        }

        public void setFlashMode(String value)
        {
            set("flash-mode", value);
        }

        public List<String> getSupportedFlashModes()
        {
            return getList("flash-mode-values");
        }

        public String getFocusMode()
        {
            return get("focus-mode");
        }

        public void setFocusMode(String value)
        {
            set("focus-mode", value);
        }

        public List<String> getSupportedFocusModes()
        {
            List<String> modes = getList("focus-mode-values");
            return modes == null ? new ArrayList<>() : modes;
        }

        public float getFocalLength()
        {
            return getFloat("focal-length", 0.0f);
        }

        public float getHorizontalViewAngle()
        {
            return getFloat("horizontal-view-angle", 0.0f);
        }

        public float getVerticalViewAngle()
        {
            return getFloat("vertical-view-angle", 0.0f);
        }

        public int getExposureCompensation()
        {
            return getInt("exposure-compensation", 0);
        }

        public void setExposureCompensation(int value)
        {
            set("exposure-compensation", value);
        }

        public int getMaxExposureCompensation()
        {
            return getInt("max-exposure-compensation", 0);
        }

        public int getMinExposureCompensation()
        {
            return getInt("min-exposure-compensation", 0);
        }

        public float getExposureCompensationStep()
        {
            return getFloat("exposure-compensation-step", 0.0f);
        }

        public void setAutoExposureLock(boolean toggle)
        {
            set("auto-exposure-lock", toggle ? "true" : "false");
        }

        public boolean getAutoExposureLock()
        {
            return getBoolean("auto-exposure-lock");
        }

        public boolean isAutoExposureLockSupported()
        {
            return getBoolean("auto-exposure-lock-supported");
        }

        public void setAutoWhiteBalanceLock(boolean toggle)
        {
            set("auto-whitebalance-lock", toggle ? "true" : "false");
        }

        public boolean getAutoWhiteBalanceLock()
        {
            return getBoolean("auto-whitebalance-lock");
        }

        public boolean isAutoWhiteBalanceLockSupported()
        {
            return getBoolean("auto-whitebalance-lock-supported");
        }

        public int getZoom()
        {
            return getInt("zoom", 0);
        }

        public void setZoom(int value)
        {
            set("zoom", value);
        }

        public boolean isZoomSupported()
        {
            return getBoolean("zoom-supported");
        }

        public int getMaxZoom()
        {
            return getInt("max-zoom", 0);
        }

        public List<Integer> getZoomRatios()
        {
            return getIntegerList("zoom-ratios");
        }

        public boolean isSmoothZoomSupported()
        {
            return getBoolean("smooth-zoom-supported");
        }

        public void getFocusDistances(float[] output)
        {
            if (output == null || output.length != 3)
            {
                throw new IllegalArgumentException("output must be a float array with three elements.");
            }

            String value = get("focus-distances");
            String[] parts = value == null ? new String[0] : value.split(",");
            for (int index = 0; index < 3; index++)
            {
                float distance = 0.0f;
                if (index < parts.length)
                {
                    distance = "Infinity".equals(parts[index]) ? Float.POSITIVE_INFINITY : parseFloat(parts[index]);
                }
                output[index] = distance;
            }
        }

        private float parseFloat(String value)
        {
            try
            {
                return Float.parseFloat(value);
            }
            catch (NumberFormatException exception)
            {
                return 0.0f;
            }
        }

        public int getMaxNumFocusAreas()
        {
            return getInt("max-num-focus-areas", 0);
        }

        public List<Area> getFocusAreas()
        {
            return mFocusAreas;
        }

        public void setFocusAreas(List<Area> focusAreas)
        {
            mFocusAreas = focusAreas;
        }

        public int getMaxNumMeteringAreas()
        {
            return getInt("max-num-metering-areas", 0);
        }

        public List<Area> getMeteringAreas()
        {
            return mMeteringAreas;
        }

        public void setMeteringAreas(List<Area> meteringAreas)
        {
            mMeteringAreas = meteringAreas;
        }

        public int getMaxNumDetectedFaces()
        {
            return getInt("max-num-detected-faces-hw", 0);
        }

        public void setRecordingHint(boolean hint)
        {
            set("recording-hint", hint ? "true" : "false");
        }

        public boolean isVideoSnapshotSupported()
        {
            return getBoolean("video-snapshot-supported");
        }

        public void setVideoStabilization(boolean toggle)
        {
            set("video-stabilization", toggle ? "true" : "false");
        }

        public boolean getVideoStabilization()
        {
            return getBoolean("video-stabilization");
        }

        public boolean isVideoStabilizationSupported()
        {
            return getBoolean("video-stabilization-supported");
        }
    }
}
