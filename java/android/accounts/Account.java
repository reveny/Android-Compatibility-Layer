package android.accounts;

import android.os.Parcel;
import android.os.Parcelable;

public class Account implements Parcelable
{
    public final String name;
    public final String type;

    public Account(String name, String type)
    {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("the account name must not be empty: " + name);
        if (type == null || type.isEmpty()) throw new IllegalArgumentException("the account type must not be empty: " + type);

        this.name = name;
        this.type = type;
    }

    public Account(Parcel source)
    {
        this.name = source.readString();
        this.type = source.readString();
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof Account)) return false;

        Account account = (Account)other;
        return name.equals(account.name) && type.equals(account.type);
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(name);
        dest.writeString(type);
    }

    @Override
    public String toString()
    {
        return "Account {name=" + name + ", type=" + type + "}";
    }

    public static final Creator<Account> CREATOR = new Creator<Account>()
    {
        @Override
        public Account createFromParcel(Parcel source)
        {
            return new Account(source);
        }

        @Override
        public Account[] newArray(int size)
        {
            return new Account[size];
        }
    };
}
