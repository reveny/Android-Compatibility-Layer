package android.hardware.camera2;

import android.util.Range;
import android.util.Size;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class CameraExtensionCharacteristics
{
    public static final int EXTENSION_AUTOMATIC = 0;
    public static final int EXTENSION_BEAUTY = 1;
    public static final int EXTENSION_BOKEH = 2;
    public static final int EXTENSION_FACE_RETOUCH = 1;
    public static final int EXTENSION_HDR = 3;
    public static final int EXTENSION_NIGHT = 4;

    private final String mCameraId;

    CameraExtensionCharacteristics(String cameraId)
    {
        mCameraId = cameraId;
    }

    public List<Integer> getSupportedExtensions()
    {
        return Collections.emptyList();
    }

    public <T> T get(int extension, CameraCharacteristics.Key<T> key)
    {
        throw unsupported(extension);
    }

    public Set<CameraCharacteristics.Key> getKeys(int extension)
    {
        throw unsupported(extension);
    }

    public boolean isPostviewAvailable(int extension)
    {
        throw unsupported(extension);
    }

    public List<Size> getPostviewSupportedSizes(int extension, Size captureSize, int format)
    {
        throw unsupported(extension);
    }

    public <T> List<Size> getExtensionSupportedSizes(int extension, Class<T> klass)
    {
        throw unsupported(extension);
    }

    public List<Size> getExtensionSupportedSizes(int extension, int format)
    {
        throw unsupported(extension);
    }

    public Range<Long> getEstimatedCaptureLatencyRangeMillis(int extension, Size captureOutputSize, int format)
    {
        throw unsupported(extension);
    }

    public boolean isCaptureProcessProgressAvailable(int extension)
    {
        throw unsupported(extension);
    }

    public Set<CaptureRequest.Key> getAvailableCaptureRequestKeys(int extension)
    {
        throw unsupported(extension);
    }

    public Set<CaptureResult.Key> getAvailableCaptureResultKeys(int extension)
    {
        throw unsupported(extension);
    }

    private IllegalArgumentException unsupported(int extension)
    {
        return new IllegalArgumentException("Unsupported extension " + extension + " for camera " + mCameraId);
    }
}
