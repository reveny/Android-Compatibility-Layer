package android.app;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DownloadManager
{
    public static final String ACTION_DOWNLOAD_COMPLETE = "android.intent.action.DOWNLOAD_COMPLETE";
    public static final String ACTION_NOTIFICATION_CLICKED = "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED";
    public static final String ACTION_VIEW_DOWNLOADS = "android.intent.action.VIEW_DOWNLOADS";

    public static final String COLUMN_BYTES_DOWNLOADED_SO_FAR = "bytes_so_far";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
    public static final String COLUMN_LOCAL_FILENAME = "local_filename";
    public static final String COLUMN_LOCAL_URI = "local_uri";
    public static final String COLUMN_MEDIAPROVIDER_URI = "mediaprovider_uri";
    public static final String COLUMN_MEDIA_TYPE = "media_type";
    public static final String COLUMN_REASON = "reason";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TOTAL_SIZE_BYTES = "total_size";
    public static final String COLUMN_URI = "uri";

    public static final int ERROR_UNKNOWN = 1000;
    public static final int ERROR_FILE_ERROR = 1001;
    public static final int ERROR_UNHANDLED_HTTP_CODE = 1002;
    public static final int ERROR_HTTP_DATA_ERROR = 1004;
    public static final int ERROR_TOO_MANY_REDIRECTS = 1005;
    public static final int ERROR_INSUFFICIENT_SPACE = 1006;
    public static final int ERROR_DEVICE_NOT_FOUND = 1007;
    public static final int ERROR_CANNOT_RESUME = 1008;
    public static final int ERROR_FILE_ALREADY_EXISTS = 1009;

    public static final String EXTRA_DOWNLOAD_ID = "extra_download_id";
    public static final String EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS = "extra_click_download_ids";
    public static final String INTENT_EXTRAS_SORT_BY_SIZE = "android.app.DownloadManager.extra_sortBySize";

    public static final int PAUSED_WAITING_TO_RETRY = 1;
    public static final int PAUSED_WAITING_FOR_NETWORK = 2;
    public static final int PAUSED_QUEUED_FOR_WIFI = 3;
    public static final int PAUSED_UNKNOWN = 4;

    public static final int STATUS_PENDING = 1;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_PAUSED = 4;
    public static final int STATUS_SUCCESSFUL = 8;
    public static final int STATUS_FAILED = 16;

    private static final String TAG = "DownloadManager";
    private static final int MODE_READ_ONLY = 0x10000000;

    private static final String[] COLUMNS = {
        COLUMN_ID, COLUMN_TITLE, COLUMN_DESCRIPTION, COLUMN_URI, COLUMN_MEDIA_TYPE, COLUMN_TOTAL_SIZE_BYTES, COLUMN_LOCAL_URI,
        COLUMN_LOCAL_FILENAME, COLUMN_STATUS, COLUMN_REASON, COLUMN_BYTES_DOWNLOADED_SO_FAR, COLUMN_LAST_MODIFIED_TIMESTAMP,
        COLUMN_MEDIAPROVIDER_URI,
    };

    private static final Map<Long, Download> sDownloads = new LinkedHashMap<>();
    private static long sNextId = 1;

    private final Context mContext;

    public DownloadManager(Context context)
    {
        mContext = context;
    }

    public long enqueue(Request request)
    {
        if (request == null) throw new NullPointerException("request must not be null");

        final Download download = new Download();
        download.mTitle = request.mTitle == null ? "" : request.mTitle.toString();
        download.mDescription = request.mDescription == null ? "" : request.mDescription.toString();
        download.mUri = request.mUri.toString();
        download.mMediaType = request.mMimeType;
        download.mDestination = request.mDestinationUri;
        download.mStatus = STATUS_PENDING;
        download.mModified = System.currentTimeMillis();

        synchronized (sDownloads)
        {
            download.mId = sNextId++;
            sDownloads.put(download.mId, download);
        }

        Log.i(TAG, "download " + download.mId + " of " + download.mUri + " was queued, but there is no network to fetch it");

        Runnable fail = new Runnable()
        {
            @Override
            public void run()
            {
                synchronized (sDownloads)
                {
                    if (!sDownloads.containsKey(download.mId)) return;

                    download.mStatus = STATUS_FAILED;
                    download.mReason = ERROR_HTTP_DATA_ERROR;
                    download.mModified = System.currentTimeMillis();
                }

                Log.w(TAG, "download " + download.mId + " failed because the network is unreachable");
                announceCompletion(download.mId);
            }
        };

        Looper main = Looper.getMainLooper();
        if (main != null) new Handler(main).post(fail);
        else fail.run();

        return download.mId;
    }

    public int remove(long... ids)
    {
        if (ids == null || ids.length == 0) throw new IllegalArgumentException("input param 'ids' can't be null");

        int removed = 0;

        synchronized (sDownloads)
        {
            for (long id : ids)
            {
                if (sDownloads.remove(id) != null) removed++;
            }
        }

        return removed;
    }

    public Cursor query(Query query)
    {
        MatrixCursor cursor = new MatrixCursor(COLUMNS);
        List<Download> downloads;

        synchronized (sDownloads)
        {
            downloads = new ArrayList<>(sDownloads.values());
        }

        for (Download download : downloads)
        {
            if (query != null && !query.matches(download)) continue;

            String localUri = download.mStatus == STATUS_SUCCESSFUL && download.mDestination != null ? download.mDestination.toString() : null;
            String localFile = download.mStatus == STATUS_SUCCESSFUL && download.mDestination != null ? download.mDestination.getPath() : null;

            cursor.addRow(new Object[] {
                download.mId, download.mTitle, download.mDescription, download.mUri, download.mMediaType, download.mTotalBytes,
                localUri, localFile, download.mStatus, download.mReason, download.mBytesSoFar, download.mModified, null,
            });
        }

        return cursor;
    }

    public ParcelFileDescriptor openDownloadedFile(long id) throws FileNotFoundException
    {
        File file = completedFile(id);
        if (file == null) throw new FileNotFoundException("download " + id + " has not completed");

        return ParcelFileDescriptor.open(file, MODE_READ_ONLY);
    }

    public Uri getUriForDownloadedFile(long id)
    {
        File file = completedFile(id);
        return file == null ? null : Uri.fromFile(file);
    }

    public String getMimeTypeForDownloadedFile(long id)
    {
        synchronized (sDownloads)
        {
            Download download = sDownloads.get(id);
            return download == null ? null : download.mMediaType;
        }
    }

    public static Long getMaxBytesOverMobile(Context context)
    {
        return null;
    }

    public static Long getRecommendedMaxBytesOverMobile(Context context)
    {
        return null;
    }

    public long addCompletedDownload(String title, String description, boolean isMediaScannerScannable, String mimeType, String path,
                                     long length, boolean showNotification)
    {
        return addCompletedDownload(title, description, isMediaScannerScannable, mimeType, path, length, showNotification, null, null);
    }

    public long addCompletedDownload(String title, String description, boolean isMediaScannerScannable, String mimeType, String path,
                                     long length, boolean showNotification, Uri uri, Uri referer)
    {
        if (title == null || title.isEmpty()) throw new IllegalArgumentException("title can't be null or empty");
        if (description == null || description.isEmpty()) throw new IllegalArgumentException("description can't be null or empty");
        if (mimeType == null || mimeType.isEmpty()) throw new IllegalArgumentException("mimeType can't be null or empty");
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("path can't be null or empty");
        if (length < 0) throw new IllegalArgumentException("invalid value for param: totalBytes");

        Download download = new Download();
        download.mTitle = title;
        download.mDescription = description;
        download.mUri = uri == null ? "" : uri.toString();
        download.mMediaType = mimeType;
        download.mDestination = Uri.fromFile(new File(path));
        download.mStatus = STATUS_SUCCESSFUL;
        download.mTotalBytes = length;
        download.mBytesSoFar = length;
        download.mModified = System.currentTimeMillis();

        synchronized (sDownloads)
        {
            download.mId = sNextId++;
            sDownloads.put(download.mId, download);
        }

        return download.mId;
    }

    private File completedFile(long id)
    {
        synchronized (sDownloads)
        {
            Download download = sDownloads.get(id);
            if (download == null || download.mStatus != STATUS_SUCCESSFUL || download.mDestination == null) return null;

            String path = download.mDestination.getPath();
            return path == null ? null : new File(path);
        }
    }

    private void announceCompletion(long id)
    {
        if (mContext == null) return;

        Intent intent = new Intent(ACTION_DOWNLOAD_COMPLETE);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra(EXTRA_DOWNLOAD_ID, id);

        try
        {
            mContext.sendBroadcast(intent);
        }
        catch (RuntimeException undelivered)
        {
            Log.w(TAG, "the completion of download " + id + " could not be broadcast", undelivered);
        }
    }

    private static final class Download
    {
        long mId;
        String mTitle;
        String mDescription;
        String mUri;
        String mMediaType;
        Uri mDestination;
        int mStatus;
        int mReason;
        long mTotalBytes = -1;
        long mBytesSoFar;
        long mModified;
    }

    public static class Request
    {
        public static final int NETWORK_MOBILE = 1;
        public static final int NETWORK_WIFI = 2;

        public static final int VISIBILITY_VISIBLE = 0;
        public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED = 1;
        public static final int VISIBILITY_HIDDEN = 2;
        public static final int VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION = 3;

        private final Uri mUri;
        private final List<Pair<String, String>> mRequestHeaders = new ArrayList<>();
        private Uri mDestinationUri;
        private CharSequence mTitle;
        private CharSequence mDescription;
        private String mMimeType;
        private int mNotificationVisibility = VISIBILITY_VISIBLE;
        private int mAllowedNetworkTypes = ~0;
        private boolean mRoamingAllowed = true;
        private boolean mMeteredAllowed = true;
        private boolean mRequiresCharging;
        private boolean mRequiresDeviceIdle;
        private boolean mVisibleInDownloadsUi = true;
        private boolean mScannable;

        public Request(Uri uri)
        {
            if (uri == null) throw new NullPointerException();

            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https")))
            {
                throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
            }

            mUri = uri;
        }

        public Request setDestinationUri(Uri uri)
        {
            mDestinationUri = uri;
            return this;
        }

        public Request setDestinationInExternalFilesDir(Context context, String dirType, String subPath)
        {
            File base = context.getExternalFilesDir(dirType);
            if (base == null) throw new IllegalStateException("Failed to get external storage files directory");

            mDestinationUri = Uri.fromFile(subPath == null ? base : new File(base, subPath));
            return this;
        }

        public Request setDestinationInExternalPublicDir(String dirType, String subPath)
        {
            File base = android.os.Environment.getExternalStoragePublicDirectory(dirType);
            if (base == null) throw new IllegalStateException("Failed to get external storage public directory");
            if (!base.exists() && !base.mkdirs()) throw new IllegalStateException("Unable to create directory: " + base.getAbsolutePath());

            mDestinationUri = Uri.fromFile(subPath == null ? base : new File(base, subPath));
            return this;
        }

        public void allowScanningByMediaScanner()
        {
            mScannable = true;
        }

        public Request addRequestHeader(String header, String value)
        {
            if (header == null) throw new NullPointerException("header cannot be null");
            if (header.contains(":")) throw new IllegalArgumentException("header may not contain ':'");

            mRequestHeaders.add(Pair.create(header, value == null ? "" : value));
            return this;
        }

        public Request setTitle(CharSequence title)
        {
            mTitle = title;
            return this;
        }

        public Request setDescription(CharSequence description)
        {
            mDescription = description;
            return this;
        }

        public Request setMimeType(String mimeType)
        {
            mMimeType = mimeType;
            return this;
        }

        public Request setShowRunningNotification(boolean show)
        {
            return setNotificationVisibility(show ? VISIBILITY_VISIBLE : VISIBILITY_HIDDEN);
        }

        public Request setNotificationVisibility(int visibility)
        {
            mNotificationVisibility = visibility;
            return this;
        }

        public Request setAllowedNetworkTypes(int flags)
        {
            mAllowedNetworkTypes = flags;
            return this;
        }

        public Request setAllowedOverRoaming(boolean allowed)
        {
            mRoamingAllowed = allowed;
            return this;
        }

        public Request setAllowedOverMetered(boolean allow)
        {
            mMeteredAllowed = allow;
            return this;
        }

        public Request setRequiresCharging(boolean requiresCharging)
        {
            mRequiresCharging = requiresCharging;
            return this;
        }

        public Request setRequiresDeviceIdle(boolean requiresDeviceIdle)
        {
            mRequiresDeviceIdle = requiresDeviceIdle;
            return this;
        }

        public Request setVisibleInDownloadsUi(boolean isVisible)
        {
            mVisibleInDownloadsUi = isVisible;
            return this;
        }
    }

    public static class Query
    {
        private long[] mIds;
        private Integer mStatusFlags;

        public Query()
        {
        }

        public Query setFilterById(long... ids)
        {
            mIds = ids;
            return this;
        }

        public Query setFilterByStatus(int flags)
        {
            mStatusFlags = flags;
            return this;
        }

        boolean matches(Download download)
        {
            if (mStatusFlags != null && (mStatusFlags & download.mStatus) == 0) return false;
            if (mIds == null) return true;

            for (long id : mIds)
            {
                if (id == download.mId) return true;
            }

            return false;
        }
    }
}
