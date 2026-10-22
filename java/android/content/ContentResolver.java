package android.content;

import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentResolver
{
    public static final String SCHEME_CONTENT = "content";
    public static final String SCHEME_FILE = "file";
    public static final String SCHEME_ANDROID_RESOURCE = "android.resource";

    public static final String CURSOR_ITEM_BASE_TYPE = "vnd.android.cursor.item";
    public static final String CURSOR_DIR_BASE_TYPE = "vnd.android.cursor.dir";
    public static final String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";

    public static final String EXTRA_HONORED_ARGS = "android.content.extra.HONORED_ARGS";
    public static final String EXTRA_REFRESH_SUPPORTED = "android.content.extra.REFRESH_SUPPORTED";
    public static final String EXTRA_SIZE = "android.content.extra.SIZE";
    public static final String EXTRA_TOTAL_COUNT = "android.content.extra.TOTAL_COUNT";

    public static final String QUERY_ARG_GROUP_COLUMNS = "android:query-arg-group-columns";
    public static final String QUERY_ARG_LIMIT = "android:query-arg-limit";
    public static final String QUERY_ARG_OFFSET = "android:query-arg-offset";
    public static final String QUERY_ARG_SORT_COLLATION = "android:query-arg-sort-collation";
    public static final String QUERY_ARG_SORT_COLUMNS = "android:query-arg-sort-columns";
    public static final String QUERY_ARG_SORT_DIRECTION = "android:query-arg-sort-direction";
    public static final String QUERY_ARG_SORT_LOCALE = "android:query-arg-sort-locale";
    public static final String QUERY_ARG_SQL_GROUP_BY = "android:query-arg-sql-group-by";
    public static final String QUERY_ARG_SQL_HAVING = "android:query-arg-sql-having";
    public static final String QUERY_ARG_SQL_LIMIT = "android:query-arg-sql-limit";
    public static final String QUERY_ARG_SQL_SELECTION = "android:query-arg-sql-selection";
    public static final String QUERY_ARG_SQL_SELECTION_ARGS = "android:query-arg-sql-selection-args";
    public static final String QUERY_ARG_SQL_SORT_ORDER = "android:query-arg-sql-sort-order";
    public static final int QUERY_SORT_DIRECTION_ASCENDING = 0;
    public static final int QUERY_SORT_DIRECTION_DESCENDING = 1;

    public static final String SYNC_EXTRAS_ACCOUNT = "account";
    public static final String SYNC_EXTRAS_DISCARD_LOCAL_DELETIONS = "discard_deletions";
    public static final String SYNC_EXTRAS_DO_NOT_RETRY = "do_not_retry";
    public static final String SYNC_EXTRAS_EXPEDITED = "expedited";
    public static final String SYNC_EXTRAS_FORCE = "force";
    public static final String SYNC_EXTRAS_IGNORE_BACKOFF = "ignore_backoff";
    public static final String SYNC_EXTRAS_IGNORE_SETTINGS = "ignore_settings";
    public static final String SYNC_EXTRAS_INITIALIZE = "initialize";
    public static final String SYNC_EXTRAS_MANUAL = "force";
    public static final String SYNC_EXTRAS_OVERRIDE_TOO_MANY_DELETIONS = "deletions_override";
    public static final String SYNC_EXTRAS_REQUIRE_CHARGING = "require_charging";
    public static final String SYNC_EXTRAS_SCHEDULE_AS_EXPEDITED_JOB = "schedule_as_expedited_job";
    public static final String SYNC_EXTRAS_UPLOAD = "upload";
    public static final int SYNC_OBSERVER_TYPE_SETTINGS = 1;
    public static final int SYNC_OBSERVER_TYPE_PENDING = 2;
    public static final int SYNC_OBSERVER_TYPE_ACTIVE = 4;

    public static final int NOTIFY_SYNC_TO_NETWORK = 1 << 0;
    public static final int NOTIFY_SKIP_NOTIFY_FOR_DESCENDANTS = 1 << 1;
    public static final int NOTIFY_INSERT = 1 << 2;
    public static final int NOTIFY_UPDATE = 1 << 3;
    public static final int NOTIFY_DELETE = 1 << 4;

    private static final String TAG = "ContentResolver";

    private static final Map<String, ContentProvider> sProviders = new HashMap<>();
    private static final List<Registration> sObservers = new ArrayList<>();

    private final Context mContext;
    private final ContentProvider mWrappedProvider;

    public ContentResolver(Context context)
    {
        this(context, null);
    }

    private ContentResolver(Context context, ContentProvider wrappedProvider)
    {
        mContext = context;
        mWrappedProvider = wrappedProvider;
    }

    public static ContentResolver wrap(ContentProvider wrapped)
    {
        return new ContentResolver(wrapped.getContext(), wrapped);
    }

    public static ContentResolver wrap(ContentProviderClient wrapped)
    {
        return wrap(wrapped.getLocalContentProvider());
    }

    public static void addProvider(String authority, ContentProvider provider)
    {
        if (authority == null || provider == null) return;

        synchronized (sProviders)
        {
            for (String single : authority.split(";"))
            {
                String trimmed = single.trim();
                if (!trimmed.isEmpty()) sProviders.put(trimmed, provider);
            }
        }
    }

    public static ContentProvider findProvider(String authority)
    {
        if (authority == null) return null;

        synchronized (sProviders)
        {
            return sProviders.get(authority);
        }
    }

    private ContentProvider providerFor(Uri uri)
    {
        if (mWrappedProvider != null) return mWrappedProvider;

        return uri == null ? null : findProvider(uri.getAuthority());
    }

    private ContentProvider providerForAuthority(String authority)
    {
        if (mWrappedProvider != null) return mWrappedProvider;

        return findProvider(authority);
    }

    public final void registerContentObserver(Uri uri, boolean notifyForDescendants, ContentObserver observer)
    {
        if (observer == null) throw new NullPointerException("observer");

        synchronized (sObservers)
        {
            sObservers.add(new Registration(uri == null ? "" : uri.toString(), notifyForDescendants, observer));
        }
    }

    public final void unregisterContentObserver(ContentObserver observer)
    {
        if (observer == null) throw new NullPointerException("observer");

        synchronized (sObservers)
        {
            for (int index = sObservers.size() - 1; index >= 0; index--)
            {
                if (sObservers.get(index).observer == observer) sObservers.remove(index);
            }
        }
    }

    public final String getType(Uri uri)
    {
        ContentProvider provider = providerFor(uri);
        return provider == null ? null : provider.getType(uri);
    }

    public String[] getStreamTypes(Uri uri, String mimeTypeFilter)
    {
        ContentProvider provider = providerFor(uri);
        return provider == null ? null : ProviderCalls.getStreamTypes(provider, uri, mimeTypeFilter);
    }

    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        return query(uri, projection, selection, selectionArgs, sortOrder, null);
    }

    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder,
                              CancellationSignal cancellationSignal)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return null;

        return ProviderCalls.query(provider, uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
    }

    public final Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return null;

        return ProviderCalls.query(provider, uri, projection, queryArgs, cancellationSignal);
    }

    public final Uri canonicalize(Uri url)
    {
        ContentProvider provider = providerFor(url);
        return provider == null ? null : ProviderCalls.canonicalize(provider, url);
    }

    public final Uri uncanonicalize(Uri url)
    {
        ContentProvider provider = providerFor(url);
        return provider == null ? null : ProviderCalls.uncanonicalize(provider, url);
    }

    public final boolean refresh(Uri url, Bundle extras, CancellationSignal cancellationSignal)
    {
        ContentProvider provider = providerFor(url);
        return provider != null && ProviderCalls.refresh(provider, url, extras, cancellationSignal);
    }

    public final java.io.InputStream openInputStream(Uri uri) throws FileNotFoundException
    {
        if (uri == null) throw new FileNotFoundException("no uri");

        String scheme = uri.getScheme();

        if (SCHEME_ANDROID_RESOURCE.equals(scheme))
        {
            try
            {
                return mContext.getResources().openRawResource(resourceIdFor(uri));
            }
            catch (android.content.res.Resources.NotFoundException missing)
            {
                throw new FileNotFoundException("Resource does not exist: " + uri);
            }
        }

        if (SCHEME_FILE.equals(scheme)) return new java.io.FileInputStream(uri.getPath());

        AssetFileDescriptor descriptor = openAssetFileDescriptor(uri, "r", null);
        if (descriptor == null) throw new FileNotFoundException("the provider returned nothing for " + uri);

        try
        {
            return descriptor.createInputStream();
        }
        catch (java.io.IOException unreadable)
        {
            throw new FileNotFoundException("Unable to create stream for " + uri);
        }
    }

    private int resourceIdFor(Uri uri) throws FileNotFoundException
    {
        List<String> path = uri.getPathSegments();
        String authority = uri.getAuthority();

        if (path.size() == 1)
        {
            try
            {
                return Integer.parseInt(path.get(0));
            }
            catch (NumberFormatException malformed)
            {
                throw new FileNotFoundException("Single path segment is not a resource ID: " + uri);
            }
        }

        if (path.size() == 2)
        {
            int resourceId = mContext.getResources().getIdentifier(path.get(1), path.get(0), authority);
            if (resourceId == 0) throw new FileNotFoundException("No resource found for: " + uri);

            return resourceId;
        }

        throw new FileNotFoundException("More than two path segments: " + uri);
    }

    public final java.io.OutputStream openOutputStream(Uri uri) throws FileNotFoundException
    {
        return openOutputStream(uri, "w");
    }

    public final java.io.OutputStream openOutputStream(Uri uri, String mode) throws FileNotFoundException
    {
        if (uri == null) throw new FileNotFoundException("no uri");

        if (SCHEME_FILE.equals(uri.getScheme()))
        {
            return new java.io.FileOutputStream(uri.getPath(), mode != null && mode.contains("a"));
        }

        AssetFileDescriptor descriptor = openAssetFileDescriptor(uri, mode, null);
        if (descriptor == null) throw new FileNotFoundException("the provider returned nothing for " + uri);

        try
        {
            return descriptor.createOutputStream();
        }
        catch (java.io.IOException unwritable)
        {
            throw new FileNotFoundException("Unable to create stream for " + uri);
        }
    }

    public final ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException
    {
        return openFileDescriptor(uri, mode, signal);
    }

    public final ParcelFileDescriptor openFileDescriptor(Uri uri, String mode) throws FileNotFoundException
    {
        return openFileDescriptor(uri, mode, null);
    }

    public final ParcelFileDescriptor openFileDescriptor(Uri uri, String mode, CancellationSignal cancellationSignal)
        throws FileNotFoundException
    {
        AssetFileDescriptor descriptor = openAssetFileDescriptor(uri, mode, cancellationSignal);
        if (descriptor == null) return null;

        if (descriptor.getDeclaredLength() >= 0 || descriptor.getStartOffset() != 0)
        {
            throw new FileNotFoundException("Not a whole file");
        }

        return descriptor.getParcelFileDescriptor();
    }

    public final AssetFileDescriptor openAssetFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException
    {
        return openAssetFileDescriptor(uri, mode, signal);
    }

    public final AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode) throws FileNotFoundException
    {
        return openAssetFileDescriptor(uri, mode, null);
    }

    public final AssetFileDescriptor openAssetFileDescriptor(Uri uri, String mode, CancellationSignal cancellationSignal)
        throws FileNotFoundException
    {
        if (uri == null) throw new NullPointerException("uri");
        if (mode == null) throw new NullPointerException("mode");

        String scheme = uri.getScheme();

        if (SCHEME_ANDROID_RESOURCE.equals(scheme))
        {
            if (!"r".equals(mode)) throw new FileNotFoundException("Can't write resources: " + uri);

            try
            {
                return mContext.getResources().openRawResourceFd(resourceIdFor(uri));
            }
            catch (android.content.res.Resources.NotFoundException missing)
            {
                throw new FileNotFoundException("Resource does not exist: " + uri);
            }
        }

        if (SCHEME_FILE.equals(scheme))
        {
            ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(new File(uri.getPath()), ParcelFileDescriptor.parseMode(mode));
            return new AssetFileDescriptor(descriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        ContentProvider provider = providerFor(uri);
        if (provider == null) throw new FileNotFoundException("No content provider: " + uri);

        return ProviderCalls.openAssetFile(provider, uri, mode, cancellationSignal);
    }

    public final AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle options, CancellationSignal signal)
        throws FileNotFoundException
    {
        return openTypedAssetFileDescriptor(uri, mimeTypeFilter, options, signal);
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle options) throws FileNotFoundException
    {
        return openTypedAssetFileDescriptor(uri, mimeType, options, null);
    }

    public final AssetFileDescriptor openTypedAssetFileDescriptor(Uri uri, String mimeType, Bundle options,
                                                                  CancellationSignal cancellationSignal) throws FileNotFoundException
    {
        if (uri == null) throw new NullPointerException("uri");
        if (mimeType == null) throw new NullPointerException("mimeType");

        ContentProvider provider = providerFor(uri);
        if (provider == null)
        {
            if (SCHEME_FILE.equals(uri.getScheme()) || SCHEME_ANDROID_RESOURCE.equals(uri.getScheme()))
            {
                return openAssetFileDescriptor(uri, "r", cancellationSignal);
            }

            throw new FileNotFoundException("No content provider: " + uri);
        }

        return ProviderCalls.openTypedAssetFile(provider, uri, mimeType, options, cancellationSignal);
    }

    public final Uri insert(Uri uri, ContentValues values)
    {
        return insert(uri, values, null);
    }

    public final Uri insert(Uri uri, ContentValues values, Bundle extras)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return missingProvider(uri, null);

        Uri inserted = ProviderCalls.insert(provider, uri, values, extras);
        if (inserted != null) notifyChange(inserted, null, NOTIFY_INSERT);

        return inserted;
    }

    public ContentProviderResult[] applyBatch(String authority, ArrayList<ContentProviderOperation> operations)
        throws OperationApplicationException, android.os.RemoteException
    {
        if (authority == null) throw new NullPointerException("authority");
        if (operations == null) throw new NullPointerException("operations");

        ContentProvider provider = providerForAuthority(authority);
        if (provider == null) throw new IllegalArgumentException("Unknown authority " + authority);

        return ProviderCalls.applyBatch(provider, authority, operations);
    }

    public final int bulkInsert(Uri uri, ContentValues[] values)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return missingProvider(uri, 0);

        int inserted = provider.bulkInsert(uri, values);
        if (inserted != 0) notifyChange(uri, null, NOTIFY_INSERT);

        return inserted;
    }

    public final int delete(Uri uri, String selection, String[] selectionArgs)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return missingProvider(uri, 0);

        int removed = provider.delete(uri, selection, selectionArgs);
        if (removed != 0) notifyChange(uri, null, NOTIFY_DELETE);

        return removed;
    }

    public final int delete(Uri uri, Bundle extras)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return missingProvider(uri, 0);

        int removed = ProviderCalls.delete(provider, uri, extras);
        if (removed != 0) notifyChange(uri, null, NOTIFY_DELETE);

        return removed;
    }

    public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return missingProvider(uri, 0);

        int changed = provider.update(uri, values, selection, selectionArgs);
        if (changed != 0) notifyChange(uri, null, NOTIFY_UPDATE);

        return changed;
    }

    public final int update(Uri uri, ContentValues values, Bundle extras)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return missingProvider(uri, 0);

        int changed = ProviderCalls.update(provider, uri, values, extras);
        if (changed != 0) notifyChange(uri, null, NOTIFY_UPDATE);

        return changed;
    }

    public final Bundle call(Uri uri, String method, String argument, Bundle extras)
    {
        if (uri == null) throw new NullPointerException("uri");

        return call(uri.getAuthority(), method, argument, extras);
    }

    public final Bundle call(String authority, String method, String argument, Bundle extras)
    {
        if (authority == null) throw new NullPointerException("authority");
        if (method == null) throw new NullPointerException("method");

        ContentProvider provider = providerForAuthority(authority);
        if (provider == null) return missingProvider(authority, null);

        return ProviderCalls.call(provider, authority, method, argument, extras);
    }

    private static <T> T missingProvider(Object target, T fallback)
    {
        android.util.Log.w(TAG, "no content provider is installed for " + target);
        return fallback;
    }

    public final ContentProviderClient acquireContentProviderClient(Uri uri)
    {
        ContentProvider provider = providerFor(uri);
        return provider == null ? null : new ContentProviderClient(this, provider, uri.getAuthority(), true);
    }

    public final ContentProviderClient acquireContentProviderClient(String name)
    {
        ContentProvider provider = providerForAuthority(name);
        return provider == null ? null : new ContentProviderClient(this, provider, name, true);
    }

    public final ContentProviderClient acquireUnstableContentProviderClient(Uri uri)
    {
        ContentProvider provider = providerFor(uri);
        return provider == null ? null : new ContentProviderClient(this, provider, uri.getAuthority(), false);
    }

    public final ContentProviderClient acquireUnstableContentProviderClient(String name)
    {
        ContentProvider provider = providerForAuthority(name);
        return provider == null ? null : new ContentProviderClient(this, provider, name, false);
    }

    public void notifyChange(Uri uri, ContentObserver observer)
    {
        notifyChange(uri, observer, 0);
    }

    public void notifyChange(Uri uri, ContentObserver observer, boolean syncToNetwork)
    {
        notifyChange(uri, observer, syncToNetwork ? NOTIFY_SYNC_TO_NETWORK : 0);
    }

    public void notifyChange(Uri uri, ContentObserver observer, int flags)
    {
        dispatchChange(uri, observer, flags);
    }

    public void notifyChange(Collection<Uri> uris, ContentObserver observer, int flags)
    {
        for (Uri uri : uris)
        {
            dispatchChange(uri, observer, flags);
        }
    }

    public static void dispatchChange(Uri uri, ContentObserver from, int flags)
    {
        String changed = uri == null ? "" : uri.toString();

        List<Registration> matched = new ArrayList<>();
        synchronized (sObservers)
        {
            for (Registration registration : sObservers)
            {
                if (registration.matches(changed)) matched.add(registration);
            }
        }

        for (Registration registration : matched)
        {
            boolean self = registration.observer == from;
            if (self && !registration.observer.deliverSelfNotifications()) continue;

            registration.observer.dispatchChange(self, uri, flags);
        }
    }

    public void takePersistableUriPermission(Uri uri, int modeFlags)
    {
        android.util.Log.i(TAG, "persisting the uri permission for " + uri + " has no effect in a single-app sandbox");
    }

    public void releasePersistableUriPermission(Uri uri, int modeFlags)
    {
    }

    public List<UriPermission> getPersistedUriPermissions()
    {
        return Collections.emptyList();
    }

    public List<UriPermission> getOutgoingPersistedUriPermissions()
    {
        return Collections.emptyList();
    }

    public void startSync(Uri uri, Bundle extras)
    {
        validateSyncExtrasBundle(extras);
    }

    public void cancelSync(Uri uri)
    {
    }

    public static boolean getMasterSyncAutomatically()
    {
        return true;
    }

    public static void setMasterSyncAutomatically(boolean sync)
    {
    }

    public static Object addStatusChangeListener(int mask, Object callback)
    {
        return new Object();
    }

    public static void removeStatusChangeListener(Object handle)
    {
    }

    public static void validateSyncExtrasBundle(Bundle extras)
    {
        if (extras == null) return;

        for (String key : extras.keySet())
        {
            Object value = extras.get(key);
            if (value == null || value instanceof Long || value instanceof Integer || value instanceof Boolean || value instanceof Float
                || value instanceof Double || value instanceof String || value instanceof Byte || value instanceof Short
                || value instanceof Character)
            {
                continue;
            }

            if (value.getClass().getName().equals("android.accounts.Account")) continue;

            throw new IllegalArgumentException("unexpected value type: " + value.getClass().getName());
        }
    }

    public android.graphics.Bitmap loadThumbnail(Uri uri, android.util.Size size, CancellationSignal signal) throws java.io.IOException
    {
        if (uri == null) throw new NullPointerException("uri");
        if (size == null) throw new NullPointerException("size");

        AssetFileDescriptor descriptor = openTypedAssetFileDescriptor(uri, "image/*", null, signal);
        if (descriptor == null) throw new FileNotFoundException("no thumbnail for " + uri);

        android.graphics.Bitmap decoded;
        try (java.io.InputStream stream = descriptor.createInputStream())
        {
            decoded = android.graphics.BitmapFactory.decodeStream(stream);
        }

        if (decoded == null) throw new java.io.IOException("the thumbnail for " + uri + " did not decode");

        int width = decoded.getWidth();
        int height = decoded.getHeight();
        if (width <= size.getWidth() && height <= size.getHeight()) return decoded;

        float scale = Math.min((float)size.getWidth() / width, (float)size.getHeight() / height);
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));
        return android.graphics.Bitmap.createScaledBitmap(decoded, scaledWidth, scaledHeight, true);
    }

    public final Context getContext()
    {
        return mContext;
    }

    private static final class Registration
    {
        final String uri;
        final boolean descendants;
        final ContentObserver observer;

        Registration(String uri, boolean descendants, ContentObserver observer)
        {
            this.uri = uri;
            this.descendants = descendants;
            this.observer = observer;
        }

        boolean matches(String changed)
        {
            if (uri.isEmpty() || changed.equals(uri)) return true;
            if (!descendants) return false;

            return changed.startsWith(uri) && (uri.endsWith("/") || changed.charAt(uri.length()) == '/');
        }
    }
}
