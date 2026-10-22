package android.app;

import android.os.Parcel;
import android.os.Parcelable;

public final class GameState implements Parcelable
{
    public static final int MODE_UNKNOWN = 0;
    public static final int MODE_NONE = 1;
    public static final int MODE_GAMEPLAY_INTERRUPTIBLE = 2;
    public static final int MODE_GAMEPLAY_UNINTERRUPTIBLE = 3;
    public static final int MODE_CONTENT = 4;

    public static final Parcelable.Creator<GameState> CREATOR = new Parcelable.Creator<GameState>()
    {
        @Override
        public GameState createFromParcel(Parcel source)
        {
            return new GameState(source.readInt() != 0, source.readInt(), source.readInt(), source.readInt());
        }

        @Override
        public GameState[] newArray(int size)
        {
            return new GameState[size];
        }
    };

    private final boolean mIsLoading;
    private final int mMode;
    private final int mLabel;
    private final int mQuality;

    public GameState(boolean isLoading, int mode)
    {
        this(isLoading, mode, -1, -1);
    }

    public GameState(boolean isLoading, int mode, int label, int quality)
    {
        mIsLoading = isLoading;
        mMode = mode;
        mLabel = label;
        mQuality = quality;
    }

    public boolean isLoading()
    {
        return mIsLoading;
    }

    public int getMode()
    {
        return mMode;
    }

    public int getLabel()
    {
        return mLabel;
    }

    public int getQuality()
    {
        return mQuality;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mIsLoading ? 1 : 0);
        dest.writeInt(mMode);
        dest.writeInt(mLabel);
        dest.writeInt(mQuality);
    }
}
