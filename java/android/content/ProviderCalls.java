package android.content;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ProviderCalls
{
    private static final Object NOT_DECLARED = new Object();
    private static final Map<String, Object> sMethods = new ConcurrentHashMap<>();

    private ProviderCalls()
    {
    }

    private static Method find(ContentProvider provider, String name, Class<?>... types)
    {
        StringBuilder key = new StringBuilder(provider.getClass().getName()).append('#').append(name);
        for (Class<?> type : types) key.append(',').append(type.getName());

        String signature = key.toString();
        Object cached = sMethods.get(signature);
        if (cached == null)
        {
            try
            {
                cached = provider.getClass().getMethod(name, types);
            }
            catch (NoSuchMethodException missing)
            {
                cached = NOT_DECLARED;
            }

            sMethods.put(signature, cached);
        }

        return cached == NOT_DECLARED ? null : (Method)cached;
    }

    private static Object invoke(Method method, ContentProvider provider, Object... arguments) throws Exception
    {
        try
        {
            return method.invoke(provider, arguments);
        }
        catch (InvocationTargetException thrown)
        {
            Throwable cause = thrown.getCause();
            if (cause instanceof Exception) throw (Exception)cause;
            if (cause instanceof Error) throw (Error)cause;

            throw new RuntimeException(cause);
        }
    }

    private static RuntimeException unchecked(Exception failure)
    {
        if (failure instanceof RuntimeException) return (RuntimeException)failure;

        return new RuntimeException(failure);
    }

    static Cursor query(ContentProvider provider, Uri uri, String[] projection, Bundle queryArgs, CancellationSignal signal)
    {
        Method bundled = find(provider, "query", Uri.class, String[].class, Bundle.class, CancellationSignal.class);

        try
        {
            if (bundled != null) return (Cursor)invoke(bundled, provider, uri, projection, queryArgs, signal);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        String selection = queryArgs == null ? null : queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION);
        String[] selectionArgs = queryArgs == null ? null : queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS);
        return query(provider, uri, projection, selection, selectionArgs, sortOrder(queryArgs), signal);
    }

    static Cursor query(ContentProvider provider, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder,
                        CancellationSignal signal)
    {
        if (signal != null) signal.throwIfCanceled();

        Method signalled = find(provider, "query", Uri.class, String[].class, String.class, String[].class, String.class,
                                CancellationSignal.class);

        try
        {
            if (signalled != null)
            {
                return (Cursor)invoke(signalled, provider, uri, projection, selection, selectionArgs, sortOrder, signal);
            }
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        return provider.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    private static String sortOrder(Bundle queryArgs)
    {
        if (queryArgs == null) return null;

        String sortOrder = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER);
        if (sortOrder != null) return sortOrder;

        String[] columns = queryArgs.getStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS);
        if (columns == null || columns.length == 0) return null;

        StringBuilder clause = new StringBuilder(String.join(", ", columns));
        int direction = queryArgs.getInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, -1);
        if (direction == ContentResolver.QUERY_SORT_DIRECTION_ASCENDING) clause.append(" ASC");
        else if (direction == ContentResolver.QUERY_SORT_DIRECTION_DESCENDING) clause.append(" DESC");

        return clause.toString();
    }

    static Uri insert(ContentProvider provider, Uri uri, ContentValues values, Bundle extras)
    {
        Method bundled = find(provider, "insert", Uri.class, ContentValues.class, Bundle.class);

        try
        {
            if (bundled != null) return (Uri)invoke(bundled, provider, uri, values, extras);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        return provider.insert(uri, values);
    }

    static int update(ContentProvider provider, Uri uri, ContentValues values, Bundle extras)
    {
        Method bundled = find(provider, "update", Uri.class, ContentValues.class, Bundle.class);

        try
        {
            if (bundled != null) return (Integer)invoke(bundled, provider, uri, values, extras);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        String selection = extras == null ? null : extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION);
        String[] selectionArgs = extras == null ? null : extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS);
        return provider.update(uri, values, selection, selectionArgs);
    }

    static int delete(ContentProvider provider, Uri uri, Bundle extras)
    {
        Method bundled = find(provider, "delete", Uri.class, Bundle.class);

        try
        {
            if (bundled != null) return (Integer)invoke(bundled, provider, uri, extras);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        String selection = extras == null ? null : extras.getString(ContentResolver.QUERY_ARG_SQL_SELECTION);
        String[] selectionArgs = extras == null ? null : extras.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS);
        return provider.delete(uri, selection, selectionArgs);
    }

    static Bundle call(ContentProvider provider, String authority, String method, String argument, Bundle extras)
    {
        Method authorized = find(provider, "call", String.class, String.class, String.class, Bundle.class);

        try
        {
            if (authorized != null) return (Bundle)invoke(authorized, provider, authority, method, argument, extras);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        return provider.call(method, argument, extras);
    }

    static String[] getStreamTypes(ContentProvider provider, Uri uri, String mimeTypeFilter)
    {
        Method declared = find(provider, "getStreamTypes", Uri.class, String.class);

        try
        {
            if (declared != null) return (String[])invoke(declared, provider, uri, mimeTypeFilter);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        return null;
    }

    static Uri canonicalize(ContentProvider provider, Uri uri)
    {
        Method declared = find(provider, "canonicalize", Uri.class);

        try
        {
            return declared == null ? null : (Uri)invoke(declared, provider, uri);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }
    }

    static Uri uncanonicalize(ContentProvider provider, Uri uri)
    {
        Method declared = find(provider, "uncanonicalize", Uri.class);

        try
        {
            return declared == null ? uri : (Uri)invoke(declared, provider, uri);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }
    }

    static boolean refresh(ContentProvider provider, Uri uri, Bundle extras, CancellationSignal signal)
    {
        Method declared = find(provider, "refresh", Uri.class, Bundle.class, CancellationSignal.class);

        try
        {
            return declared != null && (Boolean)invoke(declared, provider, uri, extras, signal);
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }
    }

    static ParcelFileDescriptor openFile(ContentProvider provider, Uri uri, String mode, CancellationSignal signal)
        throws FileNotFoundException
    {
        if (signal != null) signal.throwIfCanceled();

        Method signalled = find(provider, "openFile", Uri.class, String.class, CancellationSignal.class);

        try
        {
            if (signalled != null) return (ParcelFileDescriptor)invoke(signalled, provider, uri, mode, signal);
        }
        catch (FileNotFoundException | RuntimeException failure)
        {
            throw failure;
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        return provider.openFile(uri, mode);
    }

    static AssetFileDescriptor openAssetFile(ContentProvider provider, Uri uri, String mode, CancellationSignal signal)
        throws FileNotFoundException
    {
        Method signalled = find(provider, "openAssetFile", Uri.class, String.class, CancellationSignal.class);
        Method plain = find(provider, "openAssetFile", Uri.class, String.class);

        try
        {
            if (signalled != null) return (AssetFileDescriptor)invoke(signalled, provider, uri, mode, signal);
            if (plain != null) return (AssetFileDescriptor)invoke(plain, provider, uri, mode);
        }
        catch (FileNotFoundException | RuntimeException failure)
        {
            throw failure;
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        ParcelFileDescriptor descriptor = openFile(provider, uri, mode, signal);
        return descriptor == null ? null : new AssetFileDescriptor(descriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    static AssetFileDescriptor openTypedAssetFile(ContentProvider provider, Uri uri, String mimeTypeFilter, Bundle options,
                                                  CancellationSignal signal) throws FileNotFoundException
    {
        Method signalled = find(provider, "openTypedAssetFile", Uri.class, String.class, Bundle.class, CancellationSignal.class);
        Method plain = find(provider, "openTypedAssetFile", Uri.class, String.class, Bundle.class);

        try
        {
            if (signalled != null) return (AssetFileDescriptor)invoke(signalled, provider, uri, mimeTypeFilter, options, signal);
            if (plain != null) return (AssetFileDescriptor)invoke(plain, provider, uri, mimeTypeFilter, options);
        }
        catch (FileNotFoundException | RuntimeException failure)
        {
            throw failure;
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        if ("*/*".equals(mimeTypeFilter)) return openAssetFile(provider, uri, "r", signal);

        String baseType = provider.getType(uri);
        if (baseType != null && ClipDescription.compareMimeTypes(baseType, mimeTypeFilter))
        {
            return openAssetFile(provider, uri, "r", signal);
        }

        throw new FileNotFoundException("Can't open " + uri + " as type " + mimeTypeFilter);
    }

    static ContentProviderResult[] applyBatch(ContentProvider provider, String authority, ArrayList<ContentProviderOperation> operations)
        throws OperationApplicationException
    {
        Method authorized = find(provider, "applyBatch", String.class, ArrayList.class);
        Method plain = find(provider, "applyBatch", ArrayList.class);

        try
        {
            if (authorized != null) return (ContentProviderResult[])invoke(authorized, provider, authority, operations);
            if (plain != null) return (ContentProviderResult[])invoke(plain, provider, operations);
        }
        catch (OperationApplicationException | RuntimeException failure)
        {
            throw failure;
        }
        catch (Exception failure)
        {
            throw unchecked(failure);
        }

        int count = operations.size();
        ContentProviderResult[] results = new ContentProviderResult[count];

        for (int index = 0; index < count; index++)
        {
            results[index] = operations.get(index).apply(provider, results, index);
        }

        return results;
    }
}
