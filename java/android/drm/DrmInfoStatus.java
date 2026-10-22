package android.drm;

public class DrmInfoStatus
{
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_OK = 1;
    public final ProcessedData data;
    public final int infoType;
    public final String mimeType;
    public final int statusCode;

    public DrmInfoStatus(int statusCode, int infoType, ProcessedData data, String mimeType)
    {
        if (infoType < DrmInfoRequest.TYPE_REGISTRATION_INFO || infoType > DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_PROGRESS_INFO)
        {
            throw new IllegalArgumentException("infoType: " + infoType);
        }

        if (statusCode != STATUS_OK && statusCode != STATUS_ERROR)
        {
            throw new IllegalArgumentException("Unsupported status code: " + statusCode);
        }

        if (mimeType == null || mimeType.isEmpty()) throw new IllegalArgumentException("mimeType is null or an empty string");

        this.statusCode = statusCode;
        this.infoType = infoType;
        this.data = data;
        this.mimeType = mimeType;
    }
}
