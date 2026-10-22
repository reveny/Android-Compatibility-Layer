package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;

public final class QueryLocationException extends RuntimeException implements Parcelable
{
    public static final int ERROR_REQUEST_TIME_OUT = 1;
    public static final int ERROR_PREVIOUS_REQUEST_EXISTS = 2;
    public static final int ERROR_NOT_PERMITTED = 3;
    public static final int ERROR_NOT_ALLOWED_FOR_NON_EMERGENCY_CONNECTIONS = 4;
    public static final int ERROR_SERVICE_UNAVAILABLE = 5;
    public static final int ERROR_UNSPECIFIED = 6;

    public static final Parcelable.Creator<QueryLocationException> CREATOR = new Parcelable.Creator<QueryLocationException>()
    {
        @Override
        public QueryLocationException createFromParcel(Parcel source)
        {
            String message = source.readString();
            return new QueryLocationException(message, source.readInt());
        }

        @Override
        public QueryLocationException[] newArray(int size)
        {
            return new QueryLocationException[size];
        }
    };

    private final int mCode;

    public QueryLocationException(String message)
    {
        this(message, ERROR_UNSPECIFIED);
    }

    public QueryLocationException(String message, int code)
    {
        super(message);
        mCode = code;
    }

    public QueryLocationException(String message, int code, Throwable cause)
    {
        super(message, cause);
        mCode = code;
    }

    public int getCode()
    {
        return mCode;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(getMessage());
        dest.writeInt(mCode);
    }
}
