package android.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class DocumentsContract
{
    public static final String ACTION_DOCUMENT_SETTINGS = "android.provider.action.DOCUMENT_SETTINGS";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_EXCLUDE_SELF = "android.provider.extra.EXCLUDE_SELF";
    public static final String EXTRA_INFO = "info";
    public static final String EXTRA_INITIAL_URI = "android.provider.extra.INITIAL_URI";
    public static final String EXTRA_LOADING = "loading";
    public static final String EXTRA_ORIENTATION = "android.provider.extra.ORIENTATION";
    public static final String EXTRA_PROMPT = "android.provider.extra.PROMPT";
    public static final String METADATA_EXIF = "android:documentExif";
    public static final String METADATA_TREE_COUNT = "android:metadataTreeCount";
    public static final String METADATA_TREE_SIZE = "android:metadataTreeSize";
    public static final String METADATA_TYPES = "android:documentMetadataTypes";
    public static final String PROVIDER_INTERFACE = "android.content.action.DOCUMENTS_PROVIDER";
    public static final String QUERY_ARG_DISPLAY_NAME = "android:query-arg-display-name";
    public static final String QUERY_ARG_EXCLUDE_MEDIA = "android:query-arg-exclude-media";
    public static final String QUERY_ARG_FILE_SIZE_OVER = "android:query-arg-file-size-over";
    public static final String QUERY_ARG_LAST_MODIFIED_AFTER = "android:query-arg-last-modified-after";
    public static final String QUERY_ARG_MIME_TYPES = "android:query-arg-mime-types";

    private static final String PATH_ROOT = "root";
    private static final String PATH_RECENT = "recent";
    private static final String PATH_DOCUMENT = "document";
    private static final String PATH_CHILDREN = "children";
    private static final String PATH_SEARCH = "search";
    private static final String PATH_TREE = "tree";
    private static final String PARAM_QUERY = "query";

    private static final String EXTRA_URI = "uri";
    private static final String EXTRA_PARENT_URI = "parentUri";
    private static final String EXTRA_TARGET_URI = "android.content.extra.TARGET_URI";
    private static final String EXTRA_RESULT = "result";
    private static final String EXTRA_OPTIONS = "options";

    private static final String METHOD_CREATE_DOCUMENT = "android:createDocument";
    private static final String METHOD_RENAME_DOCUMENT = "android:renameDocument";
    private static final String METHOD_DELETE_DOCUMENT = "android:deleteDocument";
    private static final String METHOD_COPY_DOCUMENT = "android:copyDocument";
    private static final String METHOD_MOVE_DOCUMENT = "android:moveDocument";
    private static final String METHOD_IS_CHILD_DOCUMENT = "android:isChildDocument";
    private static final String METHOD_REMOVE_DOCUMENT = "android:removeDocument";
    private static final String METHOD_EJECT_ROOT = "android:ejectRoot";
    private static final String METHOD_FIND_DOCUMENT_PATH = "android:findDocumentPath";
    private static final String METHOD_CREATE_WEB_LINK_INTENT = "android:createWebLinkIntent";
    private static final String METHOD_GET_DOCUMENT_METADATA = "android:getDocumentMetadata";

    private DocumentsContract()
    {
    }

    public static final class Document
    {
        public static final String COLUMN_DISPLAY_NAME = "_display_name";
        public static final String COLUMN_DOCUMENT_ID = "document_id";
        public static final String COLUMN_FLAGS = "flags";
        public static final String COLUMN_ICON = "icon";
        public static final String COLUMN_LAST_MODIFIED = "last_modified";
        public static final String COLUMN_MIME_TYPE = "mime_type";
        public static final String COLUMN_SIZE = "_size";
        public static final String COLUMN_SUMMARY = "summary";
        public static final int FLAG_DIR_BLOCKS_OPEN_DOCUMENT_TREE = 32768;
        public static final int FLAG_DIR_PREFERS_GRID = 16;
        public static final int FLAG_DIR_PREFERS_LAST_MODIFIED = 32;
        public static final int FLAG_DIR_SUPPORTS_CREATE = 8;
        public static final int FLAG_PARTIAL = 8192;
        public static final int FLAG_SUPPORTS_COPY = 128;
        public static final int FLAG_SUPPORTS_DELETE = 4;
        public static final int FLAG_SUPPORTS_METADATA = 16384;
        public static final int FLAG_SUPPORTS_MOVE = 256;
        public static final int FLAG_SUPPORTS_REMOVE = 1024;
        public static final int FLAG_SUPPORTS_RENAME = 64;
        public static final int FLAG_SUPPORTS_SETTINGS = 2048;
        public static final int FLAG_SUPPORTS_THUMBNAIL = 1;
        public static final int FLAG_SUPPORTS_WRITE = 2;
        public static final int FLAG_VIRTUAL_DOCUMENT = 512;
        public static final int FLAG_WEB_LINKABLE = 4096;
        public static final String MIME_TYPE_DIR = "vnd.android.document/directory";

        private Document()
        {
        }
    }

    public static final class Root
    {
        public static final String COLUMN_AVAILABLE_BYTES = "available_bytes";
        public static final String COLUMN_CAPACITY_BYTES = "capacity_bytes";
        public static final String COLUMN_DOCUMENT_ID = "document_id";
        public static final String COLUMN_FLAGS = "flags";
        public static final String COLUMN_ICON = "icon";
        public static final String COLUMN_MIME_TYPES = "mime_types";
        public static final String COLUMN_QUERY_ARGS = "query_args";
        public static final String COLUMN_ROOT_ID = "root_id";
        public static final String COLUMN_SUMMARY = "summary";
        public static final String COLUMN_TITLE = "title";
        public static final int FLAG_EMPTY = 64;
        public static final int FLAG_LOCAL_ONLY = 2;
        public static final int FLAG_SUPPORTS_CREATE = 1;
        public static final int FLAG_SUPPORTS_EJECT = 32;
        public static final int FLAG_SUPPORTS_IS_CHILD = 16;
        public static final int FLAG_SUPPORTS_RECENTS = 4;
        public static final int FLAG_SUPPORTS_SEARCH = 8;
        public static final String MIME_TYPE_ITEM = "vnd.android.document/root";

        private Root()
        {
        }
    }

    public static final class Path implements Parcelable
    {
        public static final Parcelable.Creator<Path> CREATOR = new Parcelable.Creator<Path>()
        {
            @Override
            public Path createFromParcel(Parcel source)
            {
                String rootId = source.readString();
                List<String> path = source.createStringArrayList();
                return new Path(rootId, path);
            }

            @Override
            public Path[] newArray(int size)
            {
                return new Path[size];
            }
        };

        private final String mRootId;
        private final List<String> mPath;

        public Path(String rootId, List<String> path)
        {
            if (path == null || path.isEmpty()) throw new IllegalArgumentException("Path cannot be empty.");
            mRootId = rootId;
            mPath = path;
        }

        public String getRootId()
        {
            return mRootId;
        }

        public List<String> getPath()
        {
            return mPath;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof Path)) return false;
            Path path = (Path)other;
            return Objects.equals(mRootId, path.mRootId) && Objects.equals(mPath, path.mPath);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(mRootId, mPath);
        }

        @Override
        public String toString()
        {
            return "DocumentsContract.Path{rootId=" + mRootId + ", path=" + mPath + "}";
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel destination, int flags)
        {
            destination.writeString(mRootId);
            destination.writeStringList(mPath);
        }
    }

    public static Uri buildRootsUri(String authority)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_ROOT).build();
    }

    public static Uri buildRootUri(String authority, String rootId)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_ROOT)
            .appendPath(rootId).build();
    }

    public static Uri buildRecentDocumentsUri(String authority, String rootId)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_ROOT)
            .appendPath(rootId).appendPath(PATH_RECENT).build();
    }

    public static Uri buildTreeDocumentUri(String authority, String documentId)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_TREE)
            .appendPath(documentId).build();
    }

    public static Uri buildDocumentUri(String authority, String documentId)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_DOCUMENT)
            .appendPath(documentId).build();
    }

    public static Uri buildDocumentUriUsingTree(Uri treeUri, String documentId)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(treeUri.getAuthority()).appendPath(PATH_TREE)
            .appendPath(getTreeDocumentId(treeUri)).appendPath(PATH_DOCUMENT).appendPath(documentId).build();
    }

    public static Uri buildChildDocumentsUri(String authority, String parentDocumentId)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_DOCUMENT)
            .appendPath(parentDocumentId).appendPath(PATH_CHILDREN).build();
    }

    public static Uri buildChildDocumentsUriUsingTree(Uri treeUri, String parentDocumentId)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(treeUri.getAuthority()).appendPath(PATH_TREE)
            .appendPath(getTreeDocumentId(treeUri)).appendPath(PATH_DOCUMENT).appendPath(parentDocumentId)
            .appendPath(PATH_CHILDREN).build();
    }

    public static Uri buildSearchDocumentsUri(String authority, String rootId, String query)
    {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).appendPath(PATH_ROOT)
            .appendPath(rootId).appendPath(PATH_SEARCH).appendQueryParameter(PARAM_QUERY, query).build();
    }

    private static boolean isContentUri(Uri uri)
    {
        return uri != null && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme());
    }

    public static boolean isDocumentUri(Context context, Uri uri)
    {
        if (!isContentUri(uri)) return false;

        List<String> paths = uri.getPathSegments();
        if (paths.size() == 2) return PATH_DOCUMENT.equals(paths.get(0));
        if (paths.size() == 4) return PATH_TREE.equals(paths.get(0)) && PATH_DOCUMENT.equals(paths.get(2));
        return false;
    }

    public static boolean isRootUri(Context context, Uri uri)
    {
        if (!isContentUri(uri)) return false;

        List<String> paths = uri.getPathSegments();
        return paths.size() == 2 && PATH_ROOT.equals(paths.get(0));
    }

    public static boolean isRootsUri(Context context, Uri uri)
    {
        if (!isContentUri(uri)) return false;

        List<String> paths = uri.getPathSegments();
        return paths.size() == 1 && PATH_ROOT.equals(paths.get(0));
    }

    public static boolean isTreeUri(Uri uri)
    {
        if (uri == null) return false;

        List<String> paths = uri.getPathSegments();
        return paths.size() >= 2 && PATH_TREE.equals(paths.get(0));
    }

    public static String getRootId(Uri rootUri)
    {
        List<String> paths = rootUri.getPathSegments();
        if (paths.size() >= 2 && PATH_ROOT.equals(paths.get(0))) return paths.get(1);
        throw new IllegalArgumentException("Invalid URI: " + rootUri);
    }

    public static String getDocumentId(Uri documentUri)
    {
        List<String> paths = documentUri.getPathSegments();
        if (paths.size() >= 2 && PATH_DOCUMENT.equals(paths.get(0))) return paths.get(1);
        if (paths.size() >= 4 && PATH_TREE.equals(paths.get(0)) && PATH_DOCUMENT.equals(paths.get(2))) return paths.get(3);
        throw new IllegalArgumentException("Invalid URI: " + documentUri);
    }

    public static String getTreeDocumentId(Uri documentUri)
    {
        List<String> paths = documentUri.getPathSegments();
        if (paths.size() >= 2 && PATH_TREE.equals(paths.get(0))) return paths.get(1);
        throw new IllegalArgumentException("Invalid URI: " + documentUri);
    }

    public static String getSearchDocumentsQuery(Uri searchDocumentsUri)
    {
        return searchDocumentsUri.getQueryParameter(PARAM_QUERY);
    }

    private static Bundle invoke(ContentResolver content, Uri uri, String method, Bundle in) throws FileNotFoundException
    {
        Bundle out;

        try
        {
            out = content.call(uri, method, null, in);
        }
        catch (RuntimeException failure)
        {
            FileNotFoundException missing = new FileNotFoundException(failure.getMessage());
            missing.initCause(failure);
            throw missing;
        }

        if (out == null) throw new FileNotFoundException("No documents provider answered " + method + " for " + uri);
        return out;
    }

    public static Uri createDocument(ContentResolver content, Uri parentDocumentUri, String mimeType, String displayName)
        throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, parentDocumentUri);
        in.putString(Document.COLUMN_MIME_TYPE, mimeType);
        in.putString(Document.COLUMN_DISPLAY_NAME, displayName);
        return invoke(content, parentDocumentUri, METHOD_CREATE_DOCUMENT, in).getParcelable(EXTRA_URI);
    }

    public static Uri renameDocument(ContentResolver content, Uri documentUri, String displayName) throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, documentUri);
        in.putString(Document.COLUMN_DISPLAY_NAME, displayName);
        Uri outUri = invoke(content, documentUri, METHOD_RENAME_DOCUMENT, in).getParcelable(EXTRA_URI);
        return outUri != null ? outUri : documentUri;
    }

    public static boolean deleteDocument(ContentResolver content, Uri documentUri) throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, documentUri);
        invoke(content, documentUri, METHOD_DELETE_DOCUMENT, in);
        return true;
    }

    public static Uri copyDocument(ContentResolver content, Uri sourceDocumentUri, Uri targetParentDocumentUri)
        throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, sourceDocumentUri);
        in.putParcelable(EXTRA_TARGET_URI, targetParentDocumentUri);
        return invoke(content, sourceDocumentUri, METHOD_COPY_DOCUMENT, in).getParcelable(EXTRA_URI);
    }

    public static Uri moveDocument(ContentResolver content, Uri sourceDocumentUri, Uri sourceParentDocumentUri,
        Uri targetParentDocumentUri) throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, sourceDocumentUri);
        in.putParcelable(EXTRA_PARENT_URI, sourceParentDocumentUri);
        in.putParcelable(EXTRA_TARGET_URI, targetParentDocumentUri);
        return invoke(content, sourceDocumentUri, METHOD_MOVE_DOCUMENT, in).getParcelable(EXTRA_URI);
    }

    public static boolean removeDocument(ContentResolver content, Uri documentUri, Uri parentDocumentUri)
        throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, documentUri);
        in.putParcelable(EXTRA_PARENT_URI, parentDocumentUri);
        invoke(content, documentUri, METHOD_REMOVE_DOCUMENT, in);
        return true;
    }

    public static boolean isChildDocument(ContentResolver content, Uri parentDocumentUri, Uri childDocumentUri)
        throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, parentDocumentUri);
        in.putParcelable(EXTRA_TARGET_URI, childDocumentUri);
        return invoke(content, parentDocumentUri, METHOD_IS_CHILD_DOCUMENT, in).getBoolean(EXTRA_RESULT);
    }

    public static void ejectRoot(ContentResolver content, Uri rootUri)
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, rootUri);

        try
        {
            invoke(content, rootUri, METHOD_EJECT_ROOT, in);
        }
        catch (FileNotFoundException missing)
        {
            throw new IllegalStateException(missing);
        }
    }

    public static Path findDocumentPath(ContentResolver content, Uri treeUri) throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, treeUri);
        return invoke(content, treeUri, METHOD_FIND_DOCUMENT_PATH, in).getParcelable(EXTRA_RESULT);
    }

    public static IntentSender createWebLinkIntent(ContentResolver content, Uri uri, Bundle options) throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, uri);
        if (options != null) in.putBundle(EXTRA_OPTIONS, options);
        return invoke(content, uri, METHOD_CREATE_WEB_LINK_INTENT, in).getParcelable(EXTRA_RESULT);
    }

    public static Bundle getDocumentMetadata(ContentResolver content, Uri documentUri) throws FileNotFoundException
    {
        Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, documentUri);
        return invoke(content, documentUri, METHOD_GET_DOCUMENT_METADATA, in);
    }

    public static Bitmap getDocumentThumbnail(ContentResolver content, Uri documentUri, Point size, CancellationSignal signal)
        throws FileNotFoundException
    {
        try
        {
            return content.loadThumbnail(documentUri, new Size(size.x, size.y), signal);
        }
        catch (FileNotFoundException missing)
        {
            throw missing;
        }
        catch (IOException failure)
        {
            FileNotFoundException missing = new FileNotFoundException(failure.getMessage());
            missing.initCause(failure);
            throw missing;
        }
    }
}
