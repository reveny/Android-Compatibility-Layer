package android.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractCursor implements CrossProcessCursor
{
    protected int mPos = -1;
    protected boolean mClosed;
    protected ContentResolver mContentResolver;

    private final Object mSelfObserverLock = new Object();
    private final List<ContentObserver> mContentObservers = new ArrayList<>();
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private ContentObserver mSelfObserver;
    private boolean mSelfObserverRegistered;
    private List<Uri> mNotifyUris;
    private Bundle mExtras = Bundle.EMPTY;

    public static class SelfContentObserver extends ContentObserver
    {
        private final java.lang.ref.WeakReference<AbstractCursor> mCursor;

        public SelfContentObserver(AbstractCursor cursor)
        {
            super(null);
            mCursor = new java.lang.ref.WeakReference<>(cursor);
        }

        @Override
        public boolean deliverSelfNotifications()
        {
            return false;
        }

        @Override
        public void onChange(boolean selfChange)
        {
            AbstractCursor cursor = mCursor.get();
            if (cursor != null) cursor.onChange(false);
        }
    }

    public AbstractCursor()
    {
    }

    public abstract int getCount();

    public abstract String[] getColumnNames();

    public abstract String getString(int column);

    public abstract short getShort(int column);

    public abstract int getInt(int column);

    public abstract long getLong(int column);

    public abstract float getFloat(int column);

    public abstract double getDouble(int column);

    public abstract boolean isNull(int column);

    public int getType(int column)
    {
        return FIELD_TYPE_STRING;
    }

    public byte[] getBlob(int column)
    {
        throw new UnsupportedOperationException("getBlob is not supported");
    }

    public CursorWindow getWindow()
    {
        return null;
    }

    public int getColumnCount()
    {
        return getColumnNames().length;
    }

    public void deactivate()
    {
        onDeactivateOrClose();
    }

    protected void onDeactivateOrClose()
    {
        synchronized (mSelfObserverLock)
        {
            if (mSelfObserver != null && mSelfObserverRegistered && mContentResolver != null)
            {
                mContentResolver.unregisterContentObserver(mSelfObserver);
                mSelfObserverRegistered = false;
            }
        }

        mDataSetObservable.notifyInvalidated();
    }

    @Deprecated
    public boolean requery()
    {
        synchronized (mSelfObserverLock)
        {
            if (mSelfObserver != null && !mSelfObserverRegistered && mContentResolver != null && mNotifyUris != null)
            {
                for (Uri uri : mNotifyUris) mContentResolver.registerContentObserver(uri, true, mSelfObserver);
                mSelfObserverRegistered = true;
            }
        }

        mDataSetObservable.notifyChanged();
        return true;
    }

    public boolean isClosed()
    {
        return mClosed;
    }

    public void close()
    {
        mClosed = true;

        synchronized (mContentObservers)
        {
            mContentObservers.clear();
        }

        onDeactivateOrClose();
    }

    public boolean onMove(int oldPosition, int newPosition)
    {
        return true;
    }

    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer)
    {
        if (buffer == null) return;
        buffer.copyFrom(getString(columnIndex));
    }

    public final int getPosition()
    {
        return mPos;
    }

    public final boolean moveToPosition(int position)
    {
        final int count = getCount();

        if (position >= count)
        {
            mPos = count;
            return false;
        }

        if (position < 0)
        {
            mPos = -1;
            return false;
        }

        if (position == mPos) return true;

        boolean result = onMove(mPos, position);
        mPos = result ? position : -1;
        return result;
    }

    public void fillWindow(int position, CursorWindow window)
    {
        if (window == null || position < 0 || position >= getCount()) return;

        final int oldPosition = mPos;
        final int columnCount = getColumnCount();

        window.acquireReference();

        try
        {
            window.clear();
            window.setStartPosition(position);
            window.setNumColumns(columnCount);

            if (moveToPosition(position))
            {
                do
                {
                    if (!window.allocRow()) break;

                    int row = mPos;

                    for (int column = 0; column < columnCount; column++)
                    {
                        switch (getType(column))
                        {
                            case FIELD_TYPE_NULL: window.putNull(row, column); break;
                            case FIELD_TYPE_INTEGER: window.putLong(getLong(column), row, column); break;
                            case FIELD_TYPE_FLOAT: window.putDouble(getDouble(column), row, column); break;
                            case FIELD_TYPE_BLOB: window.putBlob(getBlob(column), row, column); break;
                            default: window.putString(getString(column), row, column); break;
                        }
                    }
                }
                while (moveToNext());
            }
        }
        finally
        {
            window.releaseReference();
        }

        moveToPosition(oldPosition);
    }

    public final boolean move(int offset)
    {
        return moveToPosition(mPos + offset);
    }

    public final boolean moveToFirst()
    {
        return moveToPosition(0);
    }

    public final boolean moveToLast()
    {
        return moveToPosition(getCount() - 1);
    }

    public final boolean moveToNext()
    {
        return moveToPosition(mPos + 1);
    }

    public final boolean moveToPrevious()
    {
        return moveToPosition(mPos - 1);
    }

    public final boolean isFirst()
    {
        return mPos == 0 && getCount() != 0;
    }

    public final boolean isLast()
    {
        int count = getCount();
        return mPos == count - 1 && count != 0;
    }

    public final boolean isBeforeFirst()
    {
        return getCount() == 0 || mPos == -1;
    }

    public final boolean isAfterLast()
    {
        return getCount() == 0 || mPos == getCount();
    }

    public int getColumnIndex(String columnName)
    {
        if (columnName == null) return -1;

        final int periodIndex = columnName.lastIndexOf('.');
        if (periodIndex != -1) columnName = columnName.substring(periodIndex + 1);

        String[] columnNames = getColumnNames();
        for (int index = 0; index < columnNames.length; index++)
        {
            if (columnNames[index].equalsIgnoreCase(columnName)) return index;
        }

        return -1;
    }

    public int getColumnIndexOrThrow(String columnName)
    {
        final int index = getColumnIndex(columnName);
        if (index < 0) throw new IllegalArgumentException("column '" + columnName + "' does not exist");

        return index;
    }

    public String getColumnName(int columnIndex)
    {
        return getColumnNames()[columnIndex];
    }

    public void registerContentObserver(ContentObserver observer)
    {
        if (observer == null) return;

        synchronized (mContentObservers)
        {
            if (mContentObservers.contains(observer)) throw new IllegalStateException("Observer " + observer + " is already registered.");
            mContentObservers.add(observer);
        }
    }

    public void unregisterContentObserver(ContentObserver observer)
    {
        if (mClosed) return;

        synchronized (mContentObservers)
        {
            mContentObservers.remove(observer);
        }
    }

    public void registerDataSetObserver(DataSetObserver observer)
    {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer)
    {
        mDataSetObservable.unregisterObserver(observer);
    }

    protected void onChange(boolean selfChange)
    {
        List<ContentObserver> observers;

        synchronized (mContentObservers)
        {
            observers = new ArrayList<>(mContentObservers);
        }

        for (ContentObserver observer : observers)
        {
            if (!selfChange || observer.deliverSelfNotifications()) observer.dispatchChange(selfChange, null);
        }
    }

    public void setNotificationUri(ContentResolver resolver, Uri notifyUri)
    {
        setNotificationUris(resolver, notifyUri == null ? null : Collections.singletonList(notifyUri));
    }

    public void setNotificationUris(ContentResolver resolver, List<Uri> notifyUris)
    {
        synchronized (mSelfObserverLock)
        {
            if (mSelfObserver != null && mSelfObserverRegistered && mContentResolver != null)
            {
                mContentResolver.unregisterContentObserver(mSelfObserver);
            }

            mNotifyUris = notifyUris == null ? null : new ArrayList<>(notifyUris);
            mContentResolver = resolver;
            mSelfObserverRegistered = false;

            if (resolver == null || mNotifyUris == null) return;

            if (mSelfObserver == null) mSelfObserver = new SelfContentObserver(this);

            for (Uri uri : mNotifyUris) resolver.registerContentObserver(uri, true, mSelfObserver);
            mSelfObserverRegistered = true;
        }
    }

    public Uri getNotificationUri()
    {
        synchronized (mSelfObserverLock)
        {
            return mNotifyUris == null || mNotifyUris.isEmpty() ? null : mNotifyUris.get(0);
        }
    }

    public List<Uri> getNotificationUris()
    {
        synchronized (mSelfObserverLock)
        {
            return mNotifyUris;
        }
    }

    public boolean getWantsAllOnMoveCalls()
    {
        return false;
    }

    public void setExtras(Bundle extras)
    {
        mExtras = extras == null ? Bundle.EMPTY : extras;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public Bundle respond(Bundle extras)
    {
        return Bundle.EMPTY;
    }

    @Deprecated
    protected boolean isFieldUpdated(int columnIndex)
    {
        return false;
    }

    @Deprecated
    protected Object getUpdatedField(int columnIndex)
    {
        return null;
    }

    protected void checkPosition()
    {
        if (mPos == -1 || getCount() == mPos) throw new CursorIndexOutOfBoundsException(mPos, getCount());
    }

    @Override
    protected void finalize()
    {
    }
}
