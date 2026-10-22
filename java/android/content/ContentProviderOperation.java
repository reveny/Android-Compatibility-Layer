package android.content;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ContentProviderOperation implements Parcelable
{
    static final int TYPE_INSERT = 1;
    static final int TYPE_UPDATE = 2;
    static final int TYPE_DELETE = 3;
    static final int TYPE_ASSERT = 4;
    static final int TYPE_CALL = 5;

    public static final Parcelable.Creator<ContentProviderOperation> CREATOR = new Parcelable.Creator<ContentProviderOperation>()
    {
        @Override
        public ContentProviderOperation createFromParcel(Parcel source)
        {
            return (ContentProviderOperation)source.readValue(null);
        }

        @Override
        public ContentProviderOperation[] newArray(int size)
        {
            return new ContentProviderOperation[size];
        }
    };

    private final int mType;
    private final Uri mUri;
    private final String mMethod;
    private final String mArg;
    private final Map<String, Object> mValues;
    private final Map<String, Object> mExtras;
    private final String mSelection;
    private final Object[] mSelectionArgs;
    private final Integer mExpectedCount;
    private final boolean mYieldAllowed;
    private final boolean mExceptionAllowed;

    private ContentProviderOperation(Builder builder)
    {
        mType = builder.mType;
        mUri = builder.mUri;
        mMethod = builder.mMethod;
        mArg = builder.mArg;
        mValues = builder.mValues;
        mExtras = builder.mExtras;
        mSelection = builder.mSelection;
        mSelectionArgs = builder.mSelectionArgs;
        mExpectedCount = builder.mExpectedCount;
        mYieldAllowed = builder.mYieldAllowed;
        mExceptionAllowed = builder.mExceptionAllowed;
    }

    public static Builder newInsert(Uri uri)
    {
        return new Builder(TYPE_INSERT, uri);
    }

    public static Builder newUpdate(Uri uri)
    {
        return new Builder(TYPE_UPDATE, uri);
    }

    public static Builder newDelete(Uri uri)
    {
        return new Builder(TYPE_DELETE, uri);
    }

    public static Builder newAssertQuery(Uri uri)
    {
        return new Builder(TYPE_ASSERT, uri);
    }

    public static Builder newCall(Uri uri, String method, String arg)
    {
        return new Builder(TYPE_CALL, uri, method, arg);
    }

    public Uri getUri()
    {
        return mUri;
    }

    public boolean isYieldAllowed()
    {
        return mYieldAllowed;
    }

    public boolean isExceptionAllowed()
    {
        return mExceptionAllowed;
    }

    public boolean isInsert()
    {
        return mType == TYPE_INSERT;
    }

    public boolean isDelete()
    {
        return mType == TYPE_DELETE;
    }

    public boolean isUpdate()
    {
        return mType == TYPE_UPDATE;
    }

    public boolean isAssertQuery()
    {
        return mType == TYPE_ASSERT;
    }

    public boolean isCall()
    {
        return mType == TYPE_CALL;
    }

    public boolean isWriteOperation()
    {
        return mType == TYPE_DELETE || mType == TYPE_INSERT || mType == TYPE_UPDATE;
    }

    public boolean isReadOperation()
    {
        return mType == TYPE_ASSERT;
    }

    public ContentProviderResult apply(ContentProvider provider, ContentProviderResult[] backRefs, int numBackRefs)
        throws OperationApplicationException
    {
        if (!mExceptionAllowed) return applyInternal(provider, backRefs, numBackRefs);

        try
        {
            return applyInternal(provider, backRefs, numBackRefs);
        }
        catch (Exception failure)
        {
            return new ContentProviderResult(failure);
        }
    }

    private ContentProviderResult applyInternal(ContentProvider provider, ContentProviderResult[] backRefs, int numBackRefs)
        throws OperationApplicationException
    {
        ContentValues values = resolveValueBackReferences(backRefs, numBackRefs);
        Bundle extras = resolveExtrasBackReferences(backRefs, numBackRefs);
        String[] selectionArgs = resolveSelectionArgsBackReferences(backRefs, numBackRefs);

        if (mType == TYPE_INSERT)
        {
            Uri newUri = provider.insert(mUri, values);
            if (newUri == null) throw new OperationApplicationException("Insert into " + mUri + " returned no result");

            return new ContentProviderResult(newUri);
        }

        if (mType == TYPE_CALL) return new ContentProviderResult(provider.call(mMethod, mArg, extras));

        int numRows;

        if (mType == TYPE_DELETE)
        {
            numRows = provider.delete(mUri, mSelection, selectionArgs);
        }
        else if (mType == TYPE_UPDATE)
        {
            numRows = provider.update(mUri, values, mSelection, selectionArgs);
        }
        else if (mType == TYPE_ASSERT)
        {
            String[] projection = null;
            if (values != null && values.size() > 0) projection = values.keySet().toArray(new String[0]);

            Cursor cursor = provider.query(mUri, projection, mSelection, selectionArgs, null);
            if (cursor == null) throw new OperationApplicationException("Query on " + mUri + " returned no cursor");

            try
            {
                numRows = cursor.getCount();
                if (projection != null)
                {
                    while (cursor.moveToNext())
                    {
                        for (int index = 0; index < projection.length; index++)
                        {
                            String cursorValue = cursor.getString(index);
                            String expectedValue = values.getAsString(projection[index]);
                            if (!Objects.equals(cursorValue, expectedValue))
                            {
                                throw new OperationApplicationException("Found value " + cursorValue + " when expected " + expectedValue
                                                                        + " for column " + projection[index]);
                            }
                        }
                    }
                }
            }
            finally
            {
                cursor.close();
            }
        }
        else
        {
            throw new IllegalStateException("bad type, " + mType);
        }

        if (mExpectedCount != null && mExpectedCount != numRows)
        {
            throw new OperationApplicationException("Expected " + mExpectedCount + " rows but actual " + numRows);
        }

        return new ContentProviderResult(numRows);
    }

    public ContentValues resolveValueBackReferences(ContentProviderResult[] backRefs, int numBackRefs)
    {
        if (mValues == null) return null;

        ContentValues values = new ContentValues();
        for (Map.Entry<String, Object> entry : mValues.entrySet())
        {
            Object value = entry.getValue();
            if (value instanceof BackReference) value = ((BackReference)value).resolve(backRefs, numBackRefs);
            values.putObject(entry.getKey(), value);
        }

        return values;
    }

    public Bundle resolveExtrasBackReferences(ContentProviderResult[] backRefs, int numBackRefs)
    {
        if (mExtras == null) return null;

        Bundle extras = new Bundle();
        for (Map.Entry<String, Object> entry : mExtras.entrySet())
        {
            Object value = entry.getValue();
            if (value instanceof BackReference) value = ((BackReference)value).resolve(backRefs, numBackRefs);
            putBundleValue(extras, entry.getKey(), value);
        }

        return extras;
    }

    public String[] resolveSelectionArgsBackReferences(ContentProviderResult[] backRefs, int numBackRefs)
    {
        if (mSelectionArgs == null) return null;

        String[] selectionArgs = new String[mSelectionArgs.length];
        for (int index = 0; index < mSelectionArgs.length; index++)
        {
            Object value = mSelectionArgs[index];
            if (value instanceof BackReference) value = ((BackReference)value).resolve(backRefs, numBackRefs);
            selectionArgs[index] = value == null ? null : String.valueOf(value);
        }

        return selectionArgs;
    }

    private static void putBundleValue(Bundle bundle, String key, Object value)
    {
        if (value == null) bundle.putString(key, null);
        else if (value instanceof String) bundle.putString(key, (String)value);
        else if (value instanceof Integer) bundle.putInt(key, (Integer)value);
        else if (value instanceof Long) bundle.putLong(key, (Long)value);
        else if (value instanceof Boolean) bundle.putBoolean(key, (Boolean)value);
        else if (value instanceof Double) bundle.putDouble(key, (Double)value);
        else if (value instanceof Float) bundle.putFloat(key, (Float)value);
        else if (value instanceof byte[]) bundle.putByteArray(key, (byte[])value);
        else if (value instanceof Bundle) bundle.putBundle(key, (Bundle)value);
        else if (value instanceof Parcelable) bundle.putParcelable(key, (Parcelable)value);
        else if (value instanceof CharSequence) bundle.putCharSequence(key, (CharSequence)value);
        else if (value instanceof java.io.Serializable) bundle.putSerializable(key, (java.io.Serializable)value);
        else throw new IllegalArgumentException("Unsupported type " + value.getClass());
    }

    @Override
    public String toString()
    {
        StringBuilder described = new StringBuilder("ContentProviderOperation(");
        described.append("type=").append(mType).append(' ');
        if (mUri != null) described.append("uri=").append(mUri).append(' ');
        if (mValues != null) described.append("values=").append(mValues).append(' ');
        if (mSelection != null) described.append("selection=").append(mSelection).append(' ');
        if (mExpectedCount != null) described.append("expectedCount=").append(mExpectedCount).append(' ');
        if (mYieldAllowed) described.append("yieldAllowed ");
        if (mExceptionAllowed) described.append("exceptionAllowed ");

        described.setLength(described.length() - 1);
        return described.append(')').toString();
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags)
    {
        destination.writeValue(this);
    }

    static final class BackReference
    {
        private final int mFromIndex;
        private final String mFromKey;

        BackReference(int fromIndex, String fromKey)
        {
            mFromIndex = fromIndex;
            mFromKey = fromKey;
        }

        Object resolve(ContentProviderResult[] backRefs, int numBackRefs)
        {
            if (mFromIndex >= numBackRefs)
            {
                throw new ArrayIndexOutOfBoundsException("asked for back ref " + mFromIndex + " but there are only " + numBackRefs
                                                         + " back refs");
            }

            ContentProviderResult backRef = backRefs[mFromIndex];
            if (backRef.extras != null) return backRef.extras.get(mFromKey);
            if (backRef.uri != null) return ContentUris.parseId(backRef.uri);

            return (long)backRef.count;
        }

        @Override
        public String toString()
        {
            return "BackReference(" + mFromIndex + (mFromKey == null ? "" : ", " + mFromKey) + ")";
        }
    }

    public static class Builder
    {
        private final int mType;
        private final Uri mUri;
        private final String mMethod;
        private final String mArg;
        private Map<String, Object> mValues;
        private Map<String, Object> mExtras;
        private String mSelection;
        private Object[] mSelectionArgs;
        private Integer mExpectedCount;
        private boolean mYieldAllowed;
        private boolean mExceptionAllowed;

        private Builder(int type, Uri uri)
        {
            this(type, uri, null, null);
        }

        private Builder(int type, Uri uri, String method, String arg)
        {
            mType = type;
            mUri = Objects.requireNonNull(uri);
            mMethod = method;
            mArg = arg;
        }

        public ContentProviderOperation build()
        {
            if (mType == TYPE_UPDATE && (mValues == null || mValues.isEmpty()))
            {
                throw new IllegalArgumentException("Empty values");
            }

            if (mType == TYPE_ASSERT && (mValues == null || mValues.isEmpty()) && mExpectedCount == null)
            {
                throw new IllegalArgumentException("Empty values");
            }

            return new ContentProviderOperation(this);
        }

        private void ensureValues()
        {
            if (mType != TYPE_INSERT && mType != TYPE_UPDATE && mType != TYPE_ASSERT)
            {
                throw new IllegalArgumentException("only inserts, updates, and asserts can have values");
            }

            if (mValues == null) mValues = new LinkedHashMap<>();
        }

        private void ensureExtras()
        {
            if (mExtras == null) mExtras = new LinkedHashMap<>();
        }

        public Builder withValues(ContentValues values)
        {
            ensureValues();
            for (String key : values.keySet())
            {
                mValues.put(key, values.get(key));
            }

            return this;
        }

        public Builder withValue(String key, Object value)
        {
            ensureValues();
            mValues.put(key, value);
            return this;
        }

        public Builder withValueBackReferences(ContentValues backReferences)
        {
            ensureValues();
            for (String key : backReferences.keySet())
            {
                mValues.put(key, new BackReference(backReferences.getAsInteger(key), null));
            }

            return this;
        }

        public Builder withValueBackReference(String key, int previousResult)
        {
            return withValueBackReference(key, previousResult, null);
        }

        public Builder withValueBackReference(String key, int fromIndex, String fromKey)
        {
            ensureValues();
            mValues.put(key, new BackReference(fromIndex, fromKey));
            return this;
        }

        public Builder withExtras(Bundle extras)
        {
            ensureExtras();
            for (String key : extras.keySet())
            {
                mExtras.put(key, extras.get(key));
            }

            return this;
        }

        public Builder withExtra(String key, Object value)
        {
            ensureExtras();
            mExtras.put(key, value);
            return this;
        }

        public Builder withExtraBackReference(String key, int previousResult)
        {
            return withExtraBackReference(key, previousResult, null);
        }

        public Builder withExtraBackReference(String key, int fromIndex, String fromKey)
        {
            ensureExtras();
            mExtras.put(key, new BackReference(fromIndex, fromKey));
            return this;
        }

        public Builder withSelection(String selection, String[] selectionArgs)
        {
            if (mType != TYPE_UPDATE && mType != TYPE_DELETE && mType != TYPE_ASSERT)
            {
                throw new IllegalArgumentException("only updates, deletes, and asserts can have selections");
            }

            mSelection = selection;
            mSelectionArgs = selectionArgs == null ? null : selectionArgs.clone();
            return this;
        }

        public Builder withSelectionBackReference(int index, int previousResult)
        {
            return withSelectionBackReference(index, previousResult, null);
        }

        public Builder withSelectionBackReference(int index, int fromIndex, String fromKey)
        {
            if (mType != TYPE_UPDATE && mType != TYPE_DELETE && mType != TYPE_ASSERT)
            {
                throw new IllegalArgumentException("only updates, deletes, and asserts can have selection back-references");
            }

            if (mSelectionArgs == null) mSelectionArgs = new Object[index + 1];
            if (mSelectionArgs.length <= index) mSelectionArgs = java.util.Arrays.copyOf(mSelectionArgs, index + 1);

            mSelectionArgs[index] = new BackReference(fromIndex, fromKey);
            return this;
        }

        public Builder withExpectedCount(int count)
        {
            if (mType != TYPE_UPDATE && mType != TYPE_DELETE && mType != TYPE_ASSERT)
            {
                throw new IllegalArgumentException("only updates, deletes, and asserts can have expected counts");
            }

            mExpectedCount = count;
            return this;
        }

        public Builder withYieldAllowed(boolean yieldAllowed)
        {
            mYieldAllowed = yieldAllowed;
            return this;
        }

        public Builder withExceptionAllowed(boolean exceptionAllowed)
        {
            mExceptionAllowed = exceptionAllowed;
            return this;
        }
    }
}
