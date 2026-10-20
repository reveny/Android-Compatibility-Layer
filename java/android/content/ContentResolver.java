package android.content;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentResolver
{
    public static final String SCHEME_CONTENT = "content";
    public static final String SCHEME_FILE = "file";
    public static final String SCHEME_ANDROID_RESOURCE = "android.resource";

    public static final int NOTIFY_SYNC_TO_NETWORK = 1 << 0;
    public static final int NOTIFY_SKIP_NOTIFY_FOR_DESCENDANTS = 1 << 1;
    public static final int NOTIFY_INSERT = 1 << 2;
    public static final int NOTIFY_UPDATE = 1 << 3;
    public static final int NOTIFY_DELETE = 1 << 4;

    private static final Map<String, ContentProvider> sProviders = new HashMap<>();
    private static final List<Registration> sObservers = new ArrayList<>();

    private final Context mContext;

    public ContentResolver(Context context)
    {
        mContext = context;
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

    public final android.database.Cursor query(Uri uri, String[] projection, android.os.Bundle queryArgs,
                                               Object cancellationSignal)
    {
        return query(uri, projection, null, null, null);
    }

    public final java.io.InputStream openInputStream(Uri uri) throws java.io.FileNotFoundException
    {
        if (uri == null) throw new java.io.FileNotFoundException("no uri");

        String scheme = uri.getScheme();

        if (SCHEME_FILE.equals(scheme)) return new java.io.FileInputStream(uri.getPath());

        ContentProvider provider = providerFor(uri);
        if (provider == null) throw new java.io.FileNotFoundException("no provider for " + uri);

        android.os.ParcelFileDescriptor descriptor = provider.openFile(uri, "r");
        if (descriptor == null) throw new java.io.FileNotFoundException("the provider returned nothing for " + uri);

        return new java.io.FileInputStream(descriptor.getFileDescriptor());
    }

    public final java.io.OutputStream openOutputStream(Uri uri) throws java.io.FileNotFoundException
    {
        return openOutputStream(uri, "w");
    }

    public final java.io.OutputStream openOutputStream(Uri uri, String mode) throws java.io.FileNotFoundException
    {
        if (uri == null) throw new java.io.FileNotFoundException("no uri");

        String scheme = uri.getScheme();

        if (SCHEME_FILE.equals(scheme)) return new java.io.FileOutputStream(uri.getPath());

        ContentProvider provider = providerFor(uri);
        if (provider == null) throw new java.io.FileNotFoundException("no provider for " + uri);

        android.os.ParcelFileDescriptor descriptor = provider.openFile(uri, mode);
        if (descriptor == null) throw new java.io.FileNotFoundException("the provider returned nothing for " + uri);

        return new java.io.FileOutputStream(descriptor.getFileDescriptor());
    }

    public final android.os.ParcelFileDescriptor openFileDescriptor(Uri uri, String mode) throws java.io.FileNotFoundException
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) throw new java.io.FileNotFoundException("no provider for " + uri);

        return provider.openFile(uri, mode);
    }

    public android.os.Bundle call(Uri uri, String method, String argument, android.os.Bundle extras)
    {
        ContentProvider provider = uri == null ? null : findProvider(uri.getAuthority());

        if (provider == null) return null;

        return provider.call(method, argument, extras);
    }

    public android.os.Bundle call(String authority, String method, String argument, android.os.Bundle extras)
    {
        ContentProvider provider = findProvider(authority);

        if (provider == null) return null;

        return provider.call(method, argument, extras);
    }

    public void notifyChange(Uri uri, ContentObserver from)
    {
        notifyChange(uri, from, 0);
    }

    public void notifyChange(Uri uri, ContentObserver from, boolean syncToNetwork)
    {
        notifyChange(uri, from, syncToNetwork ? NOTIFY_SYNC_TO_NETWORK : 0);
    }

    public void notifyChange(Uri uri, ContentObserver from, int flags)
    {
        dispatchChange(uri, from, flags);
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

    public final Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        ContentProvider provider = providerFor(uri);
        return provider == null ? null : provider.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public final String getType(Uri uri)
    {
        ContentProvider provider = providerFor(uri);
        return provider == null ? null : provider.getType(uri);
    }

    public final Uri insert(Uri uri, ContentValues values)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return null;

        Uri inserted = provider.insert(uri, values);
        if (inserted != null) notifyChange(inserted, null);

        return inserted;
    }

    public final int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return 0;

        int changed = provider.update(uri, values, selection, selectionArgs);
        if (changed != 0) notifyChange(uri, null);

        return changed;
    }

    public final int delete(Uri uri, String selection, String[] selectionArgs)
    {
        ContentProvider provider = providerFor(uri);
        if (provider == null) return 0;

        int removed = provider.delete(uri, selection, selectionArgs);
        if (removed != 0) notifyChange(uri, null);

        return removed;
    }

    public final Context getContext()
    {
        return mContext;
    }

    private static ContentProvider providerFor(Uri uri)
    {
        return uri == null ? null : findProvider(uri.getAuthority());
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
