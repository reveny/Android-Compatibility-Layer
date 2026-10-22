package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class AbsSavedState implements Parcelable
{
    public static final AbsSavedState EMPTY_STATE = new AbsSavedState()
    {
    };

    public static final Parcelable.Creator<AbsSavedState> CREATOR = new Parcelable.ClassLoaderCreator<AbsSavedState>()
    {
        @Override
        public AbsSavedState createFromParcel(Parcel source, ClassLoader loader)
        {
            Parcelable superState = source.readParcelable(loader);
            if (superState != null) throw new IllegalStateException("superState must be null");
            return EMPTY_STATE;
        }

        @Override
        public AbsSavedState createFromParcel(Parcel source)
        {
            return createFromParcel(source, null);
        }

        @Override
        public AbsSavedState[] newArray(int size)
        {
            return new AbsSavedState[size];
        }
    };

    private final Parcelable mSuperState;

    private AbsSavedState()
    {
        mSuperState = null;
    }

    protected AbsSavedState(Parcelable superState)
    {
        if (superState == null) throw new IllegalArgumentException("superState must not be null");
        mSuperState = superState != EMPTY_STATE ? superState : null;
    }

    protected AbsSavedState(Parcel source)
    {
        this(source, null);
    }

    protected AbsSavedState(Parcel source, ClassLoader loader)
    {
        Parcelable superState = source.readParcelable(loader);
        mSuperState = superState != null ? superState : EMPTY_STATE;
    }

    public final Parcelable getSuperState()
    {
        return mSuperState;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(mSuperState, flags);
    }
}
