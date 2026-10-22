package android.hardware.camera2;

import android.util.AndroidException;

public class CameraAccessException extends AndroidException
{
    public static final int CAMERA_DISABLED = 1;
    public static final int CAMERA_DISCONNECTED = 2;
    public static final int CAMERA_ERROR = 3;
    public static final int CAMERA_IN_USE = 4;
    public static final int MAX_CAMERAS_IN_USE = 5;

    private final int mReason;

    public CameraAccessException(int problem)
    {
        super(getDefaultMessage(problem));
        mReason = problem;
    }

    public CameraAccessException(int problem, String message)
    {
        super(getCombinedMessage(problem, message));
        mReason = problem;
    }

    public CameraAccessException(int problem, String message, Throwable cause)
    {
        super(getCombinedMessage(problem, message), cause);
        mReason = problem;
    }

    public CameraAccessException(int problem, Throwable cause)
    {
        super(getDefaultMessage(problem), cause);
        mReason = problem;
    }

    public final int getReason()
    {
        return mReason;
    }

    private static String getCombinedMessage(int problem, String message)
    {
        String problemString = getProblemString(problem);
        return String.format("%s (%d): %s", problemString, problem, message);
    }

    private static String getProblemString(int problem)
    {
        switch (problem)
        {
            case CAMERA_IN_USE: return "CAMERA_IN_USE";
            case MAX_CAMERAS_IN_USE: return "MAX_CAMERAS_IN_USE";
            case CAMERA_DISCONNECTED: return "CAMERA_DISCONNECTED";
            case CAMERA_DISABLED: return "CAMERA_DISABLED";
            case CAMERA_ERROR: return "CAMERA_ERROR";
            default: return "<UNKNOWN ERROR>";
        }
    }

    private static String getDefaultMessage(int problem)
    {
        switch (problem)
        {
            case CAMERA_IN_USE: return "The camera device is in use already";
            case MAX_CAMERAS_IN_USE:
                return "The system-wide limit for number of open cameras has been reached, and more camera devices cannot be opened "
                        + "until previous instances are closed.";
            case CAMERA_DISCONNECTED:
                return "The camera device is removable and has been disconnected from the Android device, or the camera service has "
                        + "shut down the connection due to a higher-priority access request for the camera device.";
            case CAMERA_DISABLED: return "The camera is disabled due to a device policy, and cannot be opened.";
            case CAMERA_ERROR: return "The camera device is currently in the error state; no further calls to it will succeed.";
            default: return null;
        }
    }
}
