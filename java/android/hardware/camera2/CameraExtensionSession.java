package android.hardware.camera2;

import java.util.Objects;
import java.util.concurrent.Executor;

public abstract class CameraExtensionSession implements AutoCloseable
{
    CameraExtensionSession()
    {
    }

    public CameraDevice getDevice()
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public int capture(CaptureRequest request, Executor executor, ExtensionCaptureCallback listener) throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public int setRepeatingRequest(CaptureRequest request, Executor executor, ExtensionCaptureCallback listener)
            throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public void stopRepeating() throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public StillCaptureLatency getRealtimeStillCaptureLatency() throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    @Override
    public void close() throws CameraAccessException
    {
        throw new UnsupportedOperationException("Subclasses must override this method");
    }

    public abstract static class ExtensionCaptureCallback
    {
        public ExtensionCaptureCallback()
        {
        }

        public void onCaptureStarted(CameraExtensionSession session, CaptureRequest request, long timestamp)
        {
        }

        public void onCaptureProcessStarted(CameraExtensionSession session, CaptureRequest request)
        {
        }

        public void onCaptureFailed(CameraExtensionSession session, CaptureRequest request)
        {
        }

        public void onCaptureFailed(CameraExtensionSession session, CaptureRequest request, int failure)
        {
        }

        public void onCaptureSequenceCompleted(CameraExtensionSession session, int sequenceId)
        {
        }

        public void onCaptureSequenceAborted(CameraExtensionSession session, int sequenceId)
        {
        }

        public void onCaptureResultAvailable(CameraExtensionSession session, CaptureRequest request, TotalCaptureResult result)
        {
        }

        public void onCaptureProcessProgressed(CameraExtensionSession session, CaptureRequest request, int progress)
        {
        }
    }

    public abstract static class StateCallback
    {
        public StateCallback()
        {
        }

        public abstract void onConfigured(CameraExtensionSession session);

        public abstract void onConfigureFailed(CameraExtensionSession session);

        public void onClosed(CameraExtensionSession session)
        {
        }
    }

    public static final class StillCaptureLatency
    {
        private final long mCaptureLatency;
        private final long mProcessingLatency;

        public StillCaptureLatency(long captureLatency, long processingLatency)
        {
            mCaptureLatency = captureLatency;
            mProcessingLatency = processingLatency;
        }

        public long getCaptureLatency()
        {
            return mCaptureLatency;
        }

        public long getProcessingLatency()
        {
            return mProcessingLatency;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof StillCaptureLatency)) return false;

            StillCaptureLatency latency = (StillCaptureLatency)other;
            return mCaptureLatency == latency.mCaptureLatency && mProcessingLatency == latency.mProcessingLatency;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mCaptureLatency, mProcessingLatency);
        }

        @Override
        public String toString()
        {
            return "StillCaptureLatency(processingLatency:" + mProcessingLatency + ", captureLatency: " + mCaptureLatency + ")";
        }
    }
}
