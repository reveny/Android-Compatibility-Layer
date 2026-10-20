package android.content;

import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

public abstract class ContentProvider
{
    private Context mContext;
    private String mAuthority;

    public abstract boolean onCreate();

    public abstract Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    public abstract String getType(Uri uri);

    public abstract Uri insert(Uri uri, ContentValues values);

    public abstract int delete(Uri uri, String selection, String[] selectionArgs);

    public abstract int update(Uri uri, ContentValues values, String selection, String[] selectionArgs);

    public android.os.ParcelFileDescriptor openFile(Uri uri, String mode) throws java.io.FileNotFoundException
    {
        throw new java.io.FileNotFoundException("no file is behind " + uri);
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

    public android.os.Bundle call(String method, String argument, android.os.Bundle extras)
    {
        return null;
    }

    public final Context getContext()
    {
        return mContext;
    }

    protected final void setAuthorities(String authorities)
    {
        mAuthority = authorities;
    }

    public final String getAuthority()
    {
        return mAuthority;
    }

    public void attachInfo(Context context, ProviderInfo info)
    {
        mContext = context;
        if (info != null)
        {
            setAuthorities(info.authority);
            ContentResolver.addProvider(info.authority, this);
        }

        onCreate();
    }
}
