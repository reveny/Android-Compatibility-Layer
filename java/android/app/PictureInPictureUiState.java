package android.app;

import android.os.Parcel;
import android.os.Parcelable;

public final class PictureInPictureUiState implements Parcelable
{
    private boolean mStashed;
    private boolean mTransitioningToPip;

    PictureInPictureUiState()
    {
    }

    PictureInPictureUiState(boolean stashed, boolean transitioningToPip)
    {
        mStashed = stashed;
        mTransitioningToPip = transitioningToPip;
    }

    public boolean isStashed()
    {
        return mStashed;
    }

    public boolean isTransitioningToPip()
    {
        return mTransitioningToPip;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PictureInPictureUiState)) return false;

        PictureInPictureUiState that = (PictureInPictureUiState)other;
        return mStashed == that.mStashed && mTransitioningToPip == that.mTransitioningToPip;
    }

    @Override
    public int hashCode()
    {
        return (mStashed ? 1 : 0) * 31 + (mTransitioningToPip ? 1 : 0);
    }

    @Override
    public String toString()
    {
        return "PictureInPictureUiState{stashed=" + mStashed + ", transitioningToPip=" + mTransitioningToPip + "}";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeBoolean(mStashed);
        parcel.writeBoolean(mTransitioningToPip);
    }

    public static final Creator<PictureInPictureUiState> CREATOR = new Creator<PictureInPictureUiState>()
    {
        @Override
        public PictureInPictureUiState createFromParcel(Parcel parcel)
        {
            boolean stashed = parcel.readBoolean();
            return new PictureInPictureUiState(stashed, parcel.readBoolean());
        }

        @Override
        public PictureInPictureUiState[] newArray(int size)
        {
            return new PictureInPictureUiState[size];
        }
    };
}
