package android.app;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Rational;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PictureInPictureParams implements Parcelable
{
    private Rational mAspectRatio;
    private Rational mExpandedAspectRatio;
    private List<RemoteAction> mActions;
    private RemoteAction mCloseAction;
    private Rect mSourceRectHint;
    private boolean mAutoEnterEnabled;
    private boolean mSeamlessResizeEnabled = true;
    private CharSequence mTitle;
    private CharSequence mSubtitle;

    PictureInPictureParams()
    {
    }

    public Rational getAspectRatio()
    {
        return mAspectRatio;
    }

    public Rational getExpandedAspectRatio()
    {
        return mExpandedAspectRatio;
    }

    public List<RemoteAction> getActions()
    {
        return mActions == null ? new ArrayList<RemoteAction>() : mActions;
    }

    public RemoteAction getCloseAction()
    {
        return mCloseAction;
    }

    public Rect getSourceRectHint()
    {
        return mSourceRectHint;
    }

    public boolean isAutoEnterEnabled()
    {
        return mAutoEnterEnabled;
    }

    public boolean isSeamlessResizeEnabled()
    {
        return mSeamlessResizeEnabled;
    }

    public CharSequence getTitle()
    {
        return mTitle;
    }

    public CharSequence getSubtitle()
    {
        return mSubtitle;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof PictureInPictureParams)) return false;

        PictureInPictureParams that = (PictureInPictureParams)other;

        return mAutoEnterEnabled == that.mAutoEnterEnabled && mSeamlessResizeEnabled == that.mSeamlessResizeEnabled
               && Objects.equals(mAspectRatio, that.mAspectRatio) && Objects.equals(mExpandedAspectRatio, that.mExpandedAspectRatio)
               && Objects.equals(mActions, that.mActions) && Objects.equals(mCloseAction, that.mCloseAction)
               && Objects.equals(mSourceRectHint, that.mSourceRectHint) && Objects.equals(mTitle, that.mTitle)
               && Objects.equals(mSubtitle, that.mSubtitle);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(mAspectRatio, mExpandedAspectRatio, mActions, mCloseAction, mSourceRectHint, mAutoEnterEnabled,
                            mSeamlessResizeEnabled, mTitle, mSubtitle);
    }

    @Override
    public String toString()
    {
        return "PictureInPictureParams( aspectRatio=" + mAspectRatio + " expandedAspectRatio=" + mExpandedAspectRatio
               + " sourceRectHint=" + mSourceRectHint + " hasSetActions=" + (mActions != null) + " hasSetCloseAction="
               + (mCloseAction != null) + " isAutoPipEnabled=" + mAutoEnterEnabled + " isSeamlessResizeEnabled="
               + mSeamlessResizeEnabled + " title=" + mTitle + " subtitle=" + mSubtitle + ")";
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        writeRational(parcel, mAspectRatio);
        writeRational(parcel, mExpandedAspectRatio);
        parcel.writeInt(mActions == null ? -1 : mActions.size());
        if (mActions != null)
        {
            for (RemoteAction action : mActions) action.writeToParcel(parcel, flags);
        }

        parcel.writeBoolean(mCloseAction != null);
        if (mCloseAction != null) mCloseAction.writeToParcel(parcel, flags);

        parcel.writeBoolean(mSourceRectHint != null);
        if (mSourceRectHint != null) mSourceRectHint.writeToParcel(parcel, flags);

        parcel.writeBoolean(mAutoEnterEnabled);
        parcel.writeBoolean(mSeamlessResizeEnabled);
        parcel.writeValue(mTitle);
        parcel.writeValue(mSubtitle);
    }

    private static void writeRational(Parcel parcel, Rational value)
    {
        parcel.writeBoolean(value != null);
        if (value == null) return;

        parcel.writeInt(value.getNumerator());
        parcel.writeInt(value.getDenominator());
    }

    private static Rational readRational(Parcel parcel)
    {
        if (!parcel.readBoolean()) return null;

        int numerator = parcel.readInt();
        return new Rational(numerator, parcel.readInt());
    }

    public static final Creator<PictureInPictureParams> CREATOR = new Creator<PictureInPictureParams>()
    {
        @Override
        public PictureInPictureParams createFromParcel(Parcel parcel)
        {
            PictureInPictureParams params = new PictureInPictureParams();

            params.mAspectRatio = readRational(parcel);
            params.mExpandedAspectRatio = readRational(parcel);

            int actionCount = parcel.readInt();
            if (actionCount >= 0)
            {
                params.mActions = new ArrayList<>(actionCount);
                for (int index = 0; index < actionCount; index++) params.mActions.add(RemoteAction.CREATOR.createFromParcel(parcel));
            }

            if (parcel.readBoolean()) params.mCloseAction = RemoteAction.CREATOR.createFromParcel(parcel);
            if (parcel.readBoolean()) params.mSourceRectHint = Rect.CREATOR.createFromParcel(parcel);

            params.mAutoEnterEnabled = parcel.readBoolean();
            params.mSeamlessResizeEnabled = parcel.readBoolean();
            params.mTitle = (CharSequence)parcel.readValue(null);
            params.mSubtitle = (CharSequence)parcel.readValue(null);

            return params;
        }

        @Override
        public PictureInPictureParams[] newArray(int size)
        {
            return new PictureInPictureParams[size];
        }
    };

    public static class Builder
    {
        private Rational mAspectRatio;
        private Rational mExpandedAspectRatio;
        private List<RemoteAction> mActions;
        private RemoteAction mCloseAction;
        private Rect mSourceRectHint;
        private boolean mAutoEnterEnabled;
        private boolean mSeamlessResizeEnabled = true;
        private CharSequence mTitle;
        private CharSequence mSubtitle;

        public Builder()
        {
        }

        public Builder(PictureInPictureParams original)
        {
            mAspectRatio = original.mAspectRatio;
            mExpandedAspectRatio = original.mExpandedAspectRatio;
            mActions = original.mActions == null ? null : new ArrayList<>(original.mActions);
            mCloseAction = original.mCloseAction;
            mSourceRectHint = original.mSourceRectHint == null ? null : new Rect(original.mSourceRectHint);
            mAutoEnterEnabled = original.mAutoEnterEnabled;
            mSeamlessResizeEnabled = original.mSeamlessResizeEnabled;
            mTitle = original.mTitle;
            mSubtitle = original.mSubtitle;
        }

        public Builder setAspectRatio(Rational aspectRatio)
        {
            mAspectRatio = aspectRatio;
            return this;
        }

        public Builder setExpandedAspectRatio(Rational expandedAspectRatio)
        {
            mExpandedAspectRatio = expandedAspectRatio;
            return this;
        }

        public Builder setActions(List<RemoteAction> actions)
        {
            mActions = actions == null ? null : new ArrayList<>(actions);
            return this;
        }

        public Builder setCloseAction(RemoteAction action)
        {
            mCloseAction = action;
            return this;
        }

        public Builder setSourceRectHint(Rect launchBounds)
        {
            mSourceRectHint = launchBounds == null ? null : new Rect(launchBounds);
            return this;
        }

        public Builder setAutoEnterEnabled(boolean autoEnterEnabled)
        {
            mAutoEnterEnabled = autoEnterEnabled;
            return this;
        }

        public Builder setSeamlessResizeEnabled(boolean seamlessResizeEnabled)
        {
            mSeamlessResizeEnabled = seamlessResizeEnabled;
            return this;
        }

        public Builder setTitle(CharSequence title)
        {
            mTitle = title;
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle)
        {
            mSubtitle = subtitle;
            return this;
        }

        public PictureInPictureParams build()
        {
            PictureInPictureParams params = new PictureInPictureParams();

            params.mAspectRatio = mAspectRatio;
            params.mExpandedAspectRatio = mExpandedAspectRatio;
            params.mActions = mActions;
            params.mCloseAction = mCloseAction;
            params.mSourceRectHint = mSourceRectHint;
            params.mAutoEnterEnabled = mAutoEnterEnabled;
            params.mSeamlessResizeEnabled = mSeamlessResizeEnabled;
            params.mTitle = mTitle;
            params.mSubtitle = mSubtitle;

            return params;
        }
    }
}
