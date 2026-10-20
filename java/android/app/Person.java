package android.app;

import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;

public final class Person implements Parcelable
{
    private CharSequence mName;
    private Icon mIcon;
    private String mUri;
    private String mKey;
    private boolean mBot;
    private boolean mImportant;

    private Person()
    {
    }

    public CharSequence getName()
    {
        return mName;
    }

    public Icon getIcon()
    {
        return mIcon;
    }

    public String getUri()
    {
        return mUri;
    }

    public String getKey()
    {
        return mKey;
    }

    public boolean isBot()
    {
        return mBot;
    }

    public boolean isImportant()
    {
        return mImportant;
    }

    public Builder toBuilder()
    {
        return new Builder(this);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeValue(mName);
        parcel.writeValue(mUri);
        parcel.writeValue(mKey);
        parcel.writeValue(Boolean.valueOf(mBot));
        parcel.writeValue(Boolean.valueOf(mImportant));
    }

    public static final Creator<Person> CREATOR = new Creator<Person>()
    {
        @Override
        public Person createFromParcel(Parcel parcel)
        {
            Person person = new Person();

            person.mName = (CharSequence)parcel.readValue(null);
            person.mUri = (String)parcel.readValue(null);
            person.mKey = (String)parcel.readValue(null);
            person.mBot = Boolean.TRUE.equals(parcel.readValue(null));
            person.mImportant = Boolean.TRUE.equals(parcel.readValue(null));

            return person;
        }

        @Override
        public Person[] newArray(int size)
        {
            return new Person[size];
        }
    };

    public static final class Builder
    {
        private final Person mPerson;

        public Builder()
        {
            mPerson = new Person();
        }

        Builder(Person source)
        {
            mPerson = new Person();
            mPerson.mName = source.mName;
            mPerson.mIcon = source.mIcon;
            mPerson.mUri = source.mUri;
            mPerson.mKey = source.mKey;
            mPerson.mBot = source.mBot;
            mPerson.mImportant = source.mImportant;
        }

        public Builder setName(CharSequence name)
        {
            mPerson.mName = name;
            return this;
        }

        public Builder setIcon(Icon icon)
        {
            mPerson.mIcon = icon;
            return this;
        }

        public Builder setUri(String uri)
        {
            mPerson.mUri = uri;
            return this;
        }

        public Builder setKey(String key)
        {
            mPerson.mKey = key;
            return this;
        }

        public Builder setBot(boolean bot)
        {
            mPerson.mBot = bot;
            return this;
        }

        public Builder setImportant(boolean important)
        {
            mPerson.mImportant = important;
            return this;
        }

        public Person build()
        {
            return mPerson;
        }
    }
}
