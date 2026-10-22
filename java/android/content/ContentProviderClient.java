package android.content;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ContentProviderClient implements AutoCloseable
{
    private final ContentResolver mContentResolver;
    private final ContentProvider mContentProvider;
    private final String mAuthority;
    private final boolean mStable;
    private boolean mClosed;

    ContentProviderClient(ContentResolver contentResolver, ContentProvider contentProvider, String authority, boolean stable)
    {
        mContentResolver = contentResolver;
        mContentProvider = contentProvider;
        mAuthority = authority;
        mStable = stable;
    }

    private void beforeRemote()
    {
        if (mClosed) throw new IllegalStateException("Client is closed");
    }

    public Cursor query(Uri url, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws RemoteException
    {
        return query(url, projection, selection, selectionArgs, sortOrder, null);
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder,
                        CancellationSignal cancellationSignal) throws RemoteException
    {
        beforeRemote();
        return ProviderCalls.query(mContentProvider, uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) throws RemoteException
    {
        beforeRemote();
        return ProviderCalls.query(mContentProvider, uri, projection, queryArgs, cancellationSignal);
    }

    public String getType(Uri url) throws RemoteException
    {
        beforeRemote();
        return mContentProvider.getType(url);
    }

    public String[] getStreamTypes(Uri url, String mimeTypeFilter) throws RemoteException
    {
        beforeRemote();
        return ProviderCalls.getStreamTypes(mContentProvider, url, mimeTypeFilter);
    }

    public final Uri canonicalize(Uri url) throws RemoteException
    {
        beforeRemote();
        return ProviderCalls.canonicalize(mContentProvider, url);
    }

    public final Uri uncanonicalize(Uri url) throws RemoteException
    {
        beforeRemote();
        return ProviderCalls.uncanonicalize(mContentProvider, url);
    }

    public boolean refresh(Uri url, Bundle extras, CancellationSignal cancellationSignal) throws RemoteException
    {
        beforeRemote();
        return ProviderCalls.refresh(mContentProvider, url, extras, cancellationSignal);
    }

    public Uri insert(Uri url, ContentValues initialValues) throws RemoteException
    {
        return insert(url, initialValues, null);
    }

    public Uri insert(Uri uri, ContentValues values, Bundle extras) throws RemoteException
    {
        beforeRemote();

        Uri inserted = ProviderCalls.insert(mContentProvider, uri, values, extras);
        if (inserted != null) mContentResolver.notifyChange(inserted, null, ContentResolver.NOTIFY_INSERT);

        return inserted;
    }

    public int bulkInsert(Uri url, ContentValues[] initialValues) throws RemoteException
    {
        beforeRemote();

        int inserted = mContentProvider.bulkInsert(url, initialValues);
        if (inserted != 0) mContentResolver.notifyChange(url, null, ContentResolver.NOTIFY_INSERT);

        return inserted;
    }

    public int delete(Uri url, String selection, String[] selectionArgs) throws RemoteException
    {
        beforeRemote();

        int removed = mContentProvider.delete(url, selection, selectionArgs);
        if (removed != 0) mContentResolver.notifyChange(url, null, ContentResolver.NOTIFY_DELETE);

        return removed;
    }

    public int delete(Uri url, Bundle extras) throws RemoteException
    {
        beforeRemote();

        int removed = ProviderCalls.delete(mContentProvider, url, extras);
        if (removed != 0) mContentResolver.notifyChange(url, null, ContentResolver.NOTIFY_DELETE);

        return removed;
    }

    public int update(Uri url, ContentValues values, String selection, String[] selectionArgs) throws RemoteException
    {
        beforeRemote();

        int changed = mContentProvider.update(url, values, selection, selectionArgs);
        if (changed != 0) mContentResolver.notifyChange(url, null, ContentResolver.NOTIFY_UPDATE);

        return changed;
    }

    public int update(Uri url, ContentValues values, Bundle extras) throws RemoteException
    {
        beforeRemote();

        int changed = ProviderCalls.update(mContentProvider, url, values, extras);
        if (changed != 0) mContentResolver.notifyChange(url, null, ContentResolver.NOTIFY_UPDATE);

        return changed;
    }

    public ParcelFileDescriptor openFile(Uri url, String mode) throws RemoteException, FileNotFoundException
    {
        return openFile(url, mode, null);
    }

    public ParcelFileDescriptor openFile(Uri url, String mode, CancellationSignal signal) throws RemoteException, FileNotFoundException
    {
        beforeRemote();
        return ProviderCalls.openFile(mContentProvider, url, mode, signal);
    }

    public AssetFileDescriptor openAssetFile(Uri url, String mode) throws RemoteException, FileNotFoundException
    {
        return openAssetFile(url, mode, null);
    }

    public AssetFileDescriptor openAssetFile(Uri url, String mode, CancellationSignal signal) throws RemoteException, FileNotFoundException
    {
        beforeRemote();
        return ProviderCalls.openAssetFile(mContentProvider, url, mode, signal);
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle options)
        throws RemoteException, FileNotFoundException
    {
        return openTypedAssetFileDescriptor(uri, mimeType, options, null);
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle options,
                                                                  CancellationSignal signal) throws RemoteException, FileNotFoundException
    {
        return openTypedAssetFile(uri, mimeType, options, signal);
    }

    public final AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle options, CancellationSignal signal)
        throws RemoteException, FileNotFoundException
    {
        beforeRemote();
        return ProviderCalls.openTypedAssetFile(mContentProvider, uri, mimeTypeFilter, options, signal);
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
        throws RemoteException, OperationApplicationException
    {
        return applyBatch(mAuthority, operations);
    }

    public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations)
        throws RemoteException, OperationApplicationException
    {
        beforeRemote();
        return ProviderCalls.applyBatch(mContentProvider, authority, operations);
    }

    public Bundle call(String method, String arg, Bundle extras) throws RemoteException
    {
        return call(mAuthority, method, arg, extras);
    }

    public Bundle call(String authority, String method, String arg, Bundle extras) throws RemoteException
    {
        beforeRemote();
        return ProviderCalls.call(mContentProvider, authority, method, arg, extras);
    }

    @Override
    public void close()
    {
        mClosed = true;
    }

    @Deprecated
    public boolean release()
    {
        if (mClosed) throw new IllegalStateException("Already closed");

        mClosed = true;
        return true;
    }

    public ContentProvider getLocalContentProvider()
    {
        return mContentProvider;
    }

    @Override
    public String toString()
    {
        return "ContentProviderClient{" + mAuthority + (mStable ? "" : ", unstable") + "}";
    }
}
