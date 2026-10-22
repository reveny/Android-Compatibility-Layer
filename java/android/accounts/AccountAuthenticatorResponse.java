package android.accounts;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class AccountAuthenticatorResponse implements Parcelable
{
    private static final String TAG = "AccountAuthenticator";

    public AccountAuthenticatorResponse(Parcel parcel)
    {
    }

    AccountAuthenticatorResponse()
    {
    }

    public void onResult(Bundle result)
    {
        Log.i(TAG, "an authenticator returned a result that no account manager will receive");
    }

    public void onRequestContinued()
    {
    }

    public void onError(int errorCode, String errorMessage)
    {
        Log.w(TAG, "an authenticator reported error " + errorCode + ": " + errorMessage);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
    }

    public static final Creator<AccountAuthenticatorResponse> CREATOR = new Creator<AccountAuthenticatorResponse>()
    {
        @Override
        public AccountAuthenticatorResponse createFromParcel(Parcel source)
        {
            return new AccountAuthenticatorResponse(source);
        }

        @Override
        public AccountAuthenticatorResponse[] newArray(int size)
        {
            return new AccountAuthenticatorResponse[size];
        }
    };
}
