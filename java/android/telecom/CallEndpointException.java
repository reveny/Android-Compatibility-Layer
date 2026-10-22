package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;

public final class CallEndpointException extends RuntimeException implements Parcelable
{
    public static final int ERROR_ENDPOINT_DOES_NOT_EXIST = 1;
    public static final int ERROR_REQUEST_TIME_OUT = 2;
    public static final int ERROR_ANOTHER_REQUEST = 3;
    public static final int ERROR_UNSPECIFIED = 4;

    public static final Parcelable.Creator<CallEndpointException> CREATOR = new Parcelable.Creator<CallEndpointException>()
    {
        @Override
        public CallEndpointException createFromParcel(Parcel source)
        {
            String message = source.readString();
            return new CallEndpointException(message, source.readInt());
        }

        @Override
        public CallEndpointException[] newArray(int size)
        {
            return new CallEndpointException[size];
        }
    };

    private final int mCode;

    public CallEndpointException(String message, int code)
    {
        super(message);
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
