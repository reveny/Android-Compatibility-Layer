package android.hardware.camera2;

public abstract class CameraOfflineSession extends CameraCaptureSession
{
    public CameraOfflineSession()
    {
    }

    @Override
    public abstract void close();

    public abstract static class CameraOfflineSessionCallback
    {
        public static final int STATUS_INTERNAL_ERROR = 0;

        public CameraOfflineSessionCallback()
        {
        }

        public abstract void onReady(CameraOfflineSession session);

        public abstract void onSwitchFailed(CameraOfflineSession session);

        public abstract void onIdle(CameraOfflineSession session);

        public abstract void onError(CameraOfflineSession session, int status);

        public abstract void onClosed(CameraOfflineSession session);
    }
}
