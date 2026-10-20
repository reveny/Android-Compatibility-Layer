package runtime;

public final class Sqlite
{
    public static final int STEP_ROW = 1;
    public static final int STEP_DONE = 2;
    public static final int STEP_ERROR = 3;

    public static final int TYPE_INTEGER = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_TEXT = 3;
    public static final int TYPE_BLOB = 4;
    public static final int TYPE_NULL = 5;

    private Sqlite()
    {
    }

    public static native long nativeOpen(String path, boolean readOnly);

    public static native void nativeClose(long handle);

    public static native int nativeExec(long handle, String sql);

    public static native long nativePrepare(long handle, String sql);

    public static native void nativeFinalize(long statement);

    public static native int nativeStep(long statement);

    public static native void nativeReset(long statement);

    public static native void nativeBindNull(long statement, int index);

    public static native void nativeBindLong(long statement, int index, long value);

    public static native void nativeBindDouble(long statement, int index, double value);

    public static native void nativeBindString(long statement, int index, String value);

    public static native void nativeBindBlob(long statement, int index, byte[] value);

    public static native int nativeColumnCount(long statement);

    public static native String nativeColumnName(long statement, int index);

    public static native int nativeColumnType(long statement, int index);

    public static native long nativeColumnLong(long statement, int index);

    public static native double nativeColumnDouble(long statement, int index);

    public static native String nativeColumnString(long statement, int index);

    public static native byte[] nativeColumnBlob(long statement, int index);

    public static native int nativeChanges(long handle);

    public static native long nativeLastInsertRowId(long handle);

    public static native String nativeErrorMessage(long handle);

    public static native int nativeErrorCode(long handle);

    public static native int nativeParameterCount(long statement);
}
