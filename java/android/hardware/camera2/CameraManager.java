package android.hardware.camera2;

import android.content.Context;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Handler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public final class CameraManager
{
    private final Context mContext;
    private final Map<AvailabilityCallback, Object> mAvailabilityCallbacks = new LinkedHashMap<>();
    private final Map<TorchCallback, Object> mTorchCallbacks = new LinkedHashMap<>();

    public CameraManager(Context context)
    {
        mContext = context;
    }

    public String[] getCameraIdList() throws CameraAccessException
    {
        return new String[0];
    }

    public Set<Set<String>> getConcurrentCameraIds() throws CameraAccessException
    {
        return Collections.emptySet();
    }

    public boolean isConcurrentSessionConfigurationSupported(Map<String, SessionConfiguration> cameraIdAndSessionConfig)
            throws CameraAccessException
    {
        if (cameraIdAndSessionConfig == null) throw new IllegalArgumentException("cameraIdAndSessionConfig was null");
        if (cameraIdAndSessionConfig.isEmpty()) return false;
        throw new IllegalArgumentException("Camera ids " + cameraIdAndSessionConfig.keySet() + " are not concurrent capable");
    }

    public CameraCharacteristics getCameraCharacteristics(String cameraId) throws CameraAccessException
    {
        throw unknownCamera(cameraId);
    }

    public CameraExtensionCharacteristics getCameraExtensionCharacteristics(String cameraId) throws CameraAccessException
    {
        throw unknownCamera(cameraId);
    }

    public CameraDevice.CameraDeviceSetup getCameraDeviceSetup(String cameraId) throws CameraAccessException
    {
        throw unknownCamera(cameraId);
    }

    public boolean isCameraDeviceSetupSupported(String cameraId) throws CameraAccessException
    {
        throw unknownCamera(cameraId);
    }

    public void openCamera(String cameraId, CameraDevice.StateCallback callback, Handler handler) throws CameraAccessException
    {
        if (callback == null) throw new IllegalArgumentException("callback was null");
        throw unknownCamera(cameraId);
    }

    public void openCamera(String cameraId, Executor executor, CameraDevice.StateCallback callback) throws CameraAccessException
    {
        if (executor == null) throw new IllegalArgumentException("executor was null");
        if (callback == null) throw new IllegalArgumentException("callback was null");
        throw unknownCamera(cameraId);
    }

    public void setTorchMode(String cameraId, boolean enabled) throws CameraAccessException
    {
        throw unknownCamera(cameraId);
    }

    public void turnOnTorchWithStrengthLevel(String cameraId, int torchStrength) throws CameraAccessException
    {
        throw unknownCamera(cameraId);
    }

    public int getTorchStrengthLevel(String cameraId) throws CameraAccessException
    {
        throw unknownCamera(cameraId);
    }

    public void registerAvailabilityCallback(AvailabilityCallback callback, Handler handler)
    {
        if (callback == null) throw new IllegalArgumentException("callback was null");
        synchronized (mAvailabilityCallbacks)
        {
            mAvailabilityCallbacks.put(callback, handler);
        }
    }

    public void registerAvailabilityCallback(Executor executor, AvailabilityCallback callback)
    {
        if (executor == null) throw new IllegalArgumentException("executor was null");
        if (callback == null) throw new IllegalArgumentException("callback was null");
        synchronized (mAvailabilityCallbacks)
        {
            mAvailabilityCallbacks.put(callback, executor);
        }
    }

    public void unregisterAvailabilityCallback(AvailabilityCallback callback)
    {
        synchronized (mAvailabilityCallbacks)
        {
            mAvailabilityCallbacks.remove(callback);
        }
    }

    public void registerTorchCallback(TorchCallback callback, Handler handler)
    {
        if (callback == null) throw new IllegalArgumentException("callback was null");
        synchronized (mTorchCallbacks)
        {
            mTorchCallbacks.put(callback, handler);
        }
    }

    public void registerTorchCallback(Executor executor, TorchCallback callback)
    {
        if (executor == null) throw new IllegalArgumentException("executor was null");
        if (callback == null) throw new IllegalArgumentException("callback was null");
        synchronized (mTorchCallbacks)
        {
            mTorchCallbacks.put(callback, executor);
        }
    }

    public void unregisterTorchCallback(TorchCallback callback)
    {
        synchronized (mTorchCallbacks)
        {
            mTorchCallbacks.remove(callback);
        }
    }

    private static IllegalArgumentException unknownCamera(String cameraId)
    {
        if (cameraId == null) return new IllegalArgumentException("cameraId was null");
        return new IllegalArgumentException("Camera id " + cameraId + " does not match any currently connected camera device");
    }

    public abstract static class AvailabilityCallback
    {
        public AvailabilityCallback()
        {
        }

        public void onCameraAvailable(String cameraId)
        {
        }

        public void onCameraUnavailable(String cameraId)
        {
        }

        public void onCameraAccessPrioritiesChanged()
        {
        }

        public void onPhysicalCameraAvailable(String cameraId, String physicalCameraId)
        {
        }

        public void onPhysicalCameraUnavailable(String cameraId, String physicalCameraId)
        {
        }
    }

    public abstract static class TorchCallback
    {
        public TorchCallback()
        {
        }

        public void onTorchModeUnavailable(String cameraId)
        {
        }

        public void onTorchModeChanged(String cameraId, boolean enabled)
        {
        }

        public void onTorchStrengthLevelChanged(String cameraId, int newStrengthLevel)
        {
        }
    }
}
