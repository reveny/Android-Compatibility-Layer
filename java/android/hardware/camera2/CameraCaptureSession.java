package android.hardware.camera2;

import android.hardware.camera2.params.OutputConfiguration;
import android.os.Handler;
import android.view.Surface;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class CameraCaptureSession implements AutoCloseable
{
    public CameraCaptureSession()
    {
    }

    public abstract CameraDevice getDevice();

    public abstract void prepare(Surface surface) throws CameraAccessException;

    public abstract void finalizeOutputConfigurations(List<OutputConfiguration> outputConfigs) throws CameraAccessException;

    public abstract int capture(CaptureRequest request, CaptureCallback listener, Handler handler) throws CameraAccessException;

    public int captureSingleRequest(CaptureRequest request, Executor executor, CaptureCallback listener) throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public abstract int captureBurst(List<CaptureRequest> requests, CaptureCallback listener, Handler handler)
            throws CameraAccessException;

    public int captureBurstRequests(List<CaptureRequest> requests, Executor executor, CaptureCallback listener)
            throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public abstract int setRepeatingRequest(CaptureRequest request, CaptureCallback listener, Handler handler)
            throws CameraAccessException;

    public int setSingleRepeatingRequest(CaptureRequest request, Executor executor, CaptureCallback listener)
            throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public abstract int setRepeatingBurst(List<CaptureRequest> requests, CaptureCallback listener, Handler handler)
            throws CameraAccessException;

    public int setRepeatingBurstRequests(List<CaptureRequest> requests, Executor executor, CaptureCallback listener)
            throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public abstract void stopRepeating() throws CameraAccessException;

    public abstract void abortCaptures() throws CameraAccessException;

    public abstract boolean isReprocessable();

    public abstract Surface getInputSurface();

    public void updateOutputConfiguration(OutputConfiguration config) throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public CameraOfflineSession switchToOffline(Collection<Surface> offlineSurfaces, Executor executor,
            CameraOfflineSession.CameraOfflineSessionCallback listener) throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public boolean supportsOfflineProcessing(Surface surface)
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    @Override
    public abstract void close();

    public abstract static class StateCallback
    {
        public StateCallback()
        {
        }

        public abstract void onConfigured(CameraCaptureSession session);

        public abstract void onConfigureFailed(CameraCaptureSession session);

        public void onReady(CameraCaptureSession session)
        {
        }

        public void onActive(CameraCaptureSession session)
        {
        }

        public void onCaptureQueueEmpty(CameraCaptureSession session)
        {
        }

        public void onClosed(CameraCaptureSession session)
        {
        }

        public void onSurfacePrepared(CameraCaptureSession session, Surface surface)
        {
        }
    }

    public abstract static class CaptureCallback
    {
        public CaptureCallback()
        {
        }

        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber)
        {
        }

        public void onReadoutStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber)
        {
        }

        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult)
        {
        }

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result)
        {
        }

        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure)
        {
        }

        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber)
        {
        }

        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId)
        {
        }

        public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request, Surface target, long frameNumber)
        {
        }
    }
}
