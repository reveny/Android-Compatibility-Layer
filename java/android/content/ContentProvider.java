package android.content;

import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

public abstract class ContentProvider implements ComponentCallbacks2
{
    public interface PipeDataWriter<T>
    {
        void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts, T args);
    }

    private Context mContext;
    private String mAuthority;
    private String mReadPermission;
    private String mWritePermission;

    public ContentProvider()
    {
    }

    public abstract boolean onCreate();

    public abstract Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder,
                        CancellationSignal cancellationSignal)
    {
        return query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal)
    {
        Bundle arguments = queryArgs != null ? queryArgs : Bundle.EMPTY;

        String selection = arguments.getString(ContentResolver.QUERY_ARG_SQL_SELECTION);
        String[] selectionArgs = arguments.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS);
        String sortOrder = arguments.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER);

        return query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    public abstract String getType(Uri uri);

    public String getTypeAnonymous(Uri uri)
    {
        return getType(uri);
    }

    public abstract Uri insert(Uri uri, ContentValues values);

    public Uri insert(Uri uri, ContentValues values, Bundle extras)
    {
        return insert(uri, values);
    }

    public abstract int delete(Uri uri, String selection, String[] selectionArgs);

    public int delete(Uri uri, Bundle extras)
    {
        String selection = extras != null ? extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION) : null;
        String[] selectionArgs = extras != null ? extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS) : null;

        return delete(uri, selection, selectionArgs);
    }

    public abstract int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);

    public int update(Uri uri, ContentValues values, Bundle extras)
    {
        String selection = extras != null ? extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION) : null;
        String[] selectionArgs = extras != null ? extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS) : null;

        return update(uri, values, selection, selectionArgs);
    }

    public Uri canonicalize(Uri url)
    {
        return null;
    }

    public Uri uncanonicalize(Uri url)
    {
        return url;
    }

    public boolean refresh(Uri uri, Bundle extras, CancellationSignal cancellationSignal)
    {
        return false;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException
    {
        throw new FileNotFoundException("No files supported by provider at " + uri);
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException
    {
        return openFile(uri, mode);
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException
    {
        ParcelFileDescriptor descriptor = openFile(uri, mode);
        return descriptor != null ? new AssetFileDescriptor(descriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH) : null;
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException
    {
        return openAssetFile(uri, mode);
    }

    public String[] getStreamTypes(Uri uri, String mimeTypeFilter)
    {
        return null;
    }

    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts) throws FileNotFoundException
    {
        if ("*/*".equals(mimeTypeFilter)) return openAssetFile(uri, "r");

        String baseType = getType(uri);
        if (baseType != null && ClipDescription.compareMimeTypes(baseType, mimeTypeFilter)) return openAssetFile(uri, "r");

        throw new FileNotFoundException("Can't open " + uri + " as type " + mimeTypeFilter);
    }

    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts, CancellationSignal signal)
        throws FileNotFoundException
    {
        return openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    protected final ParcelFileDescriptor openFileHelper(Uri uri, String mode) throws FileNotFoundException
    {
        Cursor cursor = query(uri, new String[] {"_data"}, null, null, null);
        int count = cursor != null ? cursor.getCount() : 0;

        if (count != 1)
        {
            if (cursor != null) cursor.close();
            if (count == 0) throw new FileNotFoundException("No entry for " + uri);
            throw new FileNotFoundException("Multiple items at " + uri);
        }

        cursor.moveToFirst();
        int column = cursor.getColumnIndex("_data");
        String path = column >= 0 ? cursor.getString(column) : null;
        cursor.close();

        if (path == null) throw new FileNotFoundException("Column _data not found.");

        return ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.parseMode(mode));
    }

    public <T> ParcelFileDescriptor openPipeHelper(Uri uri, String mimeType, Bundle opts, T args, PipeDataWriter<T> func)
        throws FileNotFoundException
    {
        try
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            Thread writer = new Thread(() ->
            {
                func.writeDataToPipe(pipe[1], uri, mimeType, opts, args);

                try
                {
                    pipe[1].close();
                }
                catch (java.io.IOException ignored)
                {
                }
            }, "ContentProvider pipe");

            writer.start();
            return pipe[0];
        }
        catch (java.io.IOException failure)
        {
            throw new FileNotFoundException("failure making pipe");
        }
    }

    public int bulkInsert(Uri uri, ContentValues[] values)
    {
        if (values == null) return 0;

        int inserted = 0;
        for (ContentValues value : values)
        {
            if (insert(uri, value) != null) inserted++;
        }

        return inserted;
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException
    {
        int count = operations.size();
        ContentProviderResult[] results = new ContentProviderResult[count];

        for (int index = 0; index < count; index++)
            results[index] = operations.get(index).apply(this, results, index);

        return results;
    }

    public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations)
        throws OperationApplicationException
    {
        return applyBatch(operations);
    }

    public Bundle call(String method, String arg, Bundle extras)
    {
        return null;
    }

    public Bundle call(String authority, String method, String arg, Bundle extras)
    {
        return call(method, arg, extras);
    }

    public final Context getContext()
    {
        return mContext;
    }

    public final Context requireContext()
    {
        if (mContext == null) throw new IllegalStateException("Cannot find context from the provider.");
        return mContext;
    }

    public final String getCallingPackage()
    {
        return mContext != null ? mContext.getPackageName() : null;
    }

    public final String getCallingPackageUnchecked()
    {
        return getCallingPackage();
    }

    public final String getCallingAttributionTag()
    {
        return null;
    }

    public void onCallingPackageChanged()
    {
    }

    protected final void setAuthorities(String authorities)
    {
        mAuthority = authorities;
    }

    public final String getAuthority()
    {
        return mAuthority;
    }

    protected final void setReadPermission(String permission)
    {
        mReadPermission = permission;
    }

    public final String getReadPermission()
    {
        return mReadPermission;
    }

    protected final void setWritePermission(String permission)
    {
        mWritePermission = permission;
    }

    public final String getWritePermission()
    {
        return mWritePermission;
    }

    protected boolean isTemporary()
    {
        return false;
    }

    public void shutdown()
    {
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args)
    {
        writer.println("nothing to dump");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
    }

    @Override
    public void onLowMemory()
    {
    }

    @Override
    public void onTrimMemory(int level)
    {
    }

    public void attachInfo(Context context, ProviderInfo info)
    {
        if (mContext != null) return;

        mContext = context;
        if (info != null)
        {
            setReadPermission(info.readPermission);
            setWritePermission(info.writePermission);
            setAuthorities(info.authority);
            ContentResolver.addProvider(info.authority, this);
        }

        onCreate();
    }
}
