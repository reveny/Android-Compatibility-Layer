package android.hardware.camera2;

public class CaptureFailure
{
    public static final int REASON_ERROR = 0;
    public static final int REASON_FLUSHED = 1;

    private final CaptureRequest mRequest;
    private final int mReason;
    private final boolean mWasImageCaptured;
    private final int mSequenceId;
    private final long mFrameNumber;
    private final String mPhysicalCameraId;

    CaptureFailure(CaptureRequest request, int reason, boolean wasImageCaptured, int sequenceId, long frameNumber,
            String physicalCameraId)
    {
        mRequest = request;
        mReason = reason;
        mWasImageCaptured = wasImageCaptured;
        mSequenceId = sequenceId;
        mFrameNumber = frameNumber;
        mPhysicalCameraId = physicalCameraId;
    }

    public CaptureRequest getRequest()
    {
        return mRequest;
    }

    public long getFrameNumber()
    {
        return mFrameNumber;
    }

    public int getReason()
    {
        return mReason;
    }

    public boolean wasImageCaptured()
    {
        return mWasImageCaptured;
    }

    public int getSequenceId()
    {
        return mSequenceId;
    }

    public String getPhysicalCameraId()
    {
        return mPhysicalCameraId;
    }
}
