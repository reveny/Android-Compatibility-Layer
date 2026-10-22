package android.drm;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class DrmManagerClient implements AutoCloseable
{
    public static final int ERROR_NONE = 0;
    public static final int ERROR_UNKNOWN = -2000;

    private static final int INVALID_SESSION = -1;

    private final Context mContext;
    private OnInfoListener mOnInfoListener;
    private OnEventListener mOnEventListener;
    private OnErrorListener mOnErrorListener;
    private boolean mClosed;

    public DrmManagerClient(Context context)
    {
        mContext = context;
    }

    private static String convertUriToPath(Uri uri)
    {
        if (uri == null || Uri.EMPTY.equals(uri)) throw new IllegalArgumentException("Given uri is not valid");

        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("file")) return uri.getPath();

        return uri.toString();
    }

    private static void checkPath(String path)
    {
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("Given path is invalid/null");
    }

    private static boolean isValidAction(int action)
    {
        return action >= DrmStore.Action.DEFAULT && action <= DrmStore.Action.DISPLAY;
    }

    public DrmInfo acquireDrmInfo(DrmInfoRequest drmInfoRequest)
    {
        if (drmInfoRequest == null || !drmInfoRequest.isValid())
        {
            throw new IllegalArgumentException("Given drmInfoRequest is invalid/null");
        }

        return null;
    }

    public int acquireRights(DrmInfoRequest drmInfoRequest)
    {
        DrmInfo drmInfo = acquireDrmInfo(drmInfoRequest);
        if (drmInfo == null) return ERROR_UNKNOWN;

        return processDrmInfo(drmInfo);
    }

    public boolean canHandle(Uri uri, String mimeType)
    {
        if ((uri == null || Uri.EMPTY.equals(uri)) && (mimeType == null || mimeType.isEmpty()))
        {
            throw new IllegalArgumentException("Uri or the mimetype should be non null");
        }

        return false;
    }

    public boolean canHandle(String path, String mimeType)
    {
        if ((path == null || path.isEmpty()) && (mimeType == null || mimeType.isEmpty()))
        {
            throw new IllegalArgumentException("Path or the mimetype should be non null");
        }

        return false;
    }

    public int checkRightsStatus(Uri content)
    {
        return checkRightsStatus(convertUriToPath(content), DrmStore.Action.DEFAULT);
    }

    public int checkRightsStatus(Uri content, int action)
    {
        return checkRightsStatus(convertUriToPath(content), action);
    }

    public int checkRightsStatus(String path)
    {
        return checkRightsStatus(path, DrmStore.Action.DEFAULT);
    }

    public int checkRightsStatus(String path, int action)
    {
        checkPath(path);
        if (!isValidAction(action)) throw new IllegalArgumentException("Given path or action is not valid");

        return DrmStore.RightsStatus.RIGHTS_INVALID;
    }

    public void close()
    {
        mClosed = true;
        mOnInfoListener = null;
        mOnEventListener = null;
        mOnErrorListener = null;
    }

    public DrmConvertedStatus closeConvertSession(int convertId)
    {
        return new DrmConvertedStatus(DrmConvertedStatus.STATUS_ERROR, null, 0);
    }

    public DrmConvertedStatus convertData(int convertId, byte[] inputData)
    {
        if (inputData == null || inputData.length <= 0) throw new IllegalArgumentException("Given inputData should be non null");

        return new DrmConvertedStatus(DrmConvertedStatus.STATUS_ERROR, null, 0);
    }

    public String[] getAvailableDrmEngines()
    {
        return new String[0];
    }

    public Collection<DrmSupportInfo> getAvailableDrmSupportInfo()
    {
        return new ArrayList<>();
    }

    public ContentValues getConstraints(Uri uri, int action)
    {
        return getConstraints(convertUriToPath(uri), action);
    }

    public ContentValues getConstraints(String path, int action)
    {
        checkPath(path);
        if (!isValidAction(action)) throw new IllegalArgumentException("Given usage or path is invalid/null");

        return null;
    }

    public int getDrmObjectType(Uri uri, String mimeType)
    {
        if ((uri == null || Uri.EMPTY.equals(uri)) && (mimeType == null || mimeType.isEmpty()))
        {
            throw new IllegalArgumentException("Given uri or mimetype is null");
        }

        return DrmStore.DrmObjectType.UNKNOWN;
    }

    public int getDrmObjectType(String path, String mimeType)
    {
        if ((path == null || path.isEmpty()) && (mimeType == null || mimeType.isEmpty()))
        {
            throw new IllegalArgumentException("Path or the mimetype should be non null");
        }

        return DrmStore.DrmObjectType.UNKNOWN;
    }

    public ContentValues getMetadata(Uri uri)
    {
        return getMetadata(convertUriToPath(uri));
    }

    public ContentValues getMetadata(String path)
    {
        checkPath(path);
        return null;
    }

    public String getOriginalMimeType(Uri uri)
    {
        return getOriginalMimeType(convertUriToPath(uri));
    }

    public String getOriginalMimeType(String path)
    {
        checkPath(path);
        return null;
    }

    public int openConvertSession(String mimeType)
    {
        if (mimeType == null || mimeType.isEmpty()) throw new IllegalArgumentException("Path or the mimeType should be non null");

        return INVALID_SESSION;
    }

    public int processDrmInfo(DrmInfo drmInfo)
    {
        if (drmInfo == null || !drmInfo.isValid()) throw new IllegalArgumentException("Given drmInfo is invalid/null");

        return ERROR_UNKNOWN;
    }

    public void release()
    {
        close();
    }

    public int removeAllRights()
    {
        return ERROR_UNKNOWN;
    }

    public int removeRights(Uri uri)
    {
        return removeRights(convertUriToPath(uri));
    }

    public int removeRights(String path)
    {
        checkPath(path);
        return ERROR_UNKNOWN;
    }

    public int saveRights(DrmRights drmRights, String rightsPath, String contentPath) throws IOException
    {
        if (drmRights == null || !drmRights.isValid()) throw new IllegalArgumentException("Given drmRights or contentPath is not valid");

        return ERROR_UNKNOWN;
    }

    public synchronized void setOnErrorListener(DrmManagerClient.OnErrorListener errorListener)
    {
        mOnErrorListener = errorListener;
    }

    public synchronized void setOnEventListener(DrmManagerClient.OnEventListener eventListener)
    {
        mOnEventListener = eventListener;
    }

    public synchronized void setOnInfoListener(DrmManagerClient.OnInfoListener infoListener)
    {
        mOnInfoListener = infoListener;
    }

    public interface OnInfoListener
    {
        void onInfo(DrmManagerClient client, DrmInfoEvent event);
    }

    public interface OnEventListener
    {
        void onEvent(DrmManagerClient client, DrmEvent event);
    }

    public interface OnErrorListener
    {
        void onError(DrmManagerClient client, DrmErrorEvent event);
    }
}
