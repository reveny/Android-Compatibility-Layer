package android.drm;

public class DrmConvertedStatus
{
    public static final int STATUS_ERROR = 3;
    public static final int STATUS_INPUTDATA_ERROR = 2;
    public static final int STATUS_OK = 1;
    public final byte[] convertedData;
    public final int offset;
    public final int statusCode;

    public DrmConvertedStatus(int statusCode, byte[] convertedData, int offset)
    {
        if (statusCode < STATUS_OK || statusCode > STATUS_ERROR)
        {
            throw new IllegalArgumentException("Unsupported status code: " + statusCode);
        }

        this.statusCode = statusCode;
        this.convertedData = convertedData;
        this.offset = offset;
    }
}
