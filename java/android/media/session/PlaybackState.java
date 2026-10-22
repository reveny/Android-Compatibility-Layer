package android.media.session;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public final class PlaybackState implements Parcelable
{
    public static final long ACTION_FAST_FORWARD = 64L;
    public static final long ACTION_PAUSE = 2L;
    public static final long ACTION_PLAY = 4L;
    public static final long ACTION_PLAY_FROM_MEDIA_ID = 1024L;
    public static final long ACTION_PLAY_FROM_SEARCH = 2048L;
    public static final long ACTION_PLAY_FROM_URI = 8192L;
    public static final long ACTION_PLAY_PAUSE = 512L;
    public static final long ACTION_PREPARE = 16384L;
    public static final long ACTION_PREPARE_FROM_MEDIA_ID = 32768L;
    public static final long ACTION_PREPARE_FROM_SEARCH = 65536L;
    public static final long ACTION_PREPARE_FROM_URI = 131072L;
    public static final long ACTION_REWIND = 8L;
    public static final long ACTION_SEEK_TO = 256L;
    public static final long ACTION_SET_PLAYBACK_SPEED = 4194304L;
    public static final long ACTION_SET_RATING = 128L;
    public static final long ACTION_SKIP_TO_NEXT = 32L;
    public static final long ACTION_SKIP_TO_PREVIOUS = 16L;
    public static final long ACTION_SKIP_TO_QUEUE_ITEM = 4096L;
    public static final long ACTION_STOP = 1L;
    public static final long PLAYBACK_POSITION_UNKNOWN = -1L;
    public static final int STATE_BUFFERING = 6;
    public static final int STATE_CONNECTING = 8;
    public static final int STATE_ERROR = 7;
    public static final int STATE_FAST_FORWARDING = 4;
    public static final int STATE_NONE = 0;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_REWINDING = 5;
    public static final int STATE_SKIPPING_TO_NEXT = 10;
    public static final int STATE_SKIPPING_TO_PREVIOUS = 9;
    public static final int STATE_SKIPPING_TO_QUEUE_ITEM = 11;
    public static final int STATE_STOPPED = 1;

    public static final Parcelable.Creator<PlaybackState> CREATOR = new Parcelable.Creator<PlaybackState>()
    {
        @Override
        public PlaybackState createFromParcel(Parcel source)
        {
            return new PlaybackState(source);
        }

        @Override
        public PlaybackState[] newArray(int size)
        {
            return new PlaybackState[size];
        }
    };

    private final int mState;
    private final long mPosition;
    private final long mBufferedPosition;
    private final float mSpeed;
    private final long mActions;
    private final List<CustomAction> mCustomActions;
    private final CharSequence mErrorMessage;
    private final long mUpdateTime;
    private final long mActiveItemId;
    private final Bundle mExtras;

    private PlaybackState(int state, long position, long updateTime, float speed, long bufferedPosition, long actions,
            List<CustomAction> customActions, long activeItemId, CharSequence errorMessage, Bundle extras)
    {
        mState = state;
        mPosition = position;
        mSpeed = speed;
        mUpdateTime = updateTime;
        mBufferedPosition = bufferedPosition;
        mActions = actions;
        mCustomActions = new ArrayList<>(customActions);
        mActiveItemId = activeItemId;
        mErrorMessage = errorMessage;
        mExtras = extras;
    }

    private PlaybackState(Parcel source)
    {
        mState = source.readInt();
        mPosition = source.readLong();
        mSpeed = source.readFloat();
        mUpdateTime = source.readLong();
        mBufferedPosition = source.readLong();
        mActions = source.readLong();
        List<CustomAction> customActions = source.createTypedArrayList(CustomAction.CREATOR);
        mCustomActions = customActions == null ? new ArrayList<>() : customActions;
        mActiveItemId = source.readLong();
        mErrorMessage = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        mExtras = source.readBundle();
    }

    public int getState()
    {
        return mState;
    }

    public long getPosition()
    {
        return mPosition;
    }

    public long getBufferedPosition()
    {
        return mBufferedPosition;
    }

    public float getPlaybackSpeed()
    {
        return mSpeed;
    }

    public long getActions()
    {
        return mActions;
    }

    public List<CustomAction> getCustomActions()
    {
        return mCustomActions;
    }

    public CharSequence getErrorMessage()
    {
        return mErrorMessage;
    }

    public long getLastPositionUpdateTime()
    {
        return mUpdateTime;
    }

    public long getActiveQueueItemId()
    {
        return mActiveItemId;
    }

    public Bundle getExtras()
    {
        return mExtras;
    }

    public boolean isActive()
    {
        switch (mState)
        {
            case STATE_FAST_FORWARDING:
            case STATE_REWINDING:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_SKIPPING_TO_QUEUE_ITEM:
            case STATE_BUFFERING:
            case STATE_CONNECTING:
            case STATE_PLAYING:
                return true;
            default:
                return false;
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeInt(mState);
        dest.writeLong(mPosition);
        dest.writeFloat(mSpeed);
        dest.writeLong(mUpdateTime);
        dest.writeLong(mBufferedPosition);
        dest.writeLong(mActions);
        dest.writeTypedList(mCustomActions);
        dest.writeLong(mActiveItemId);
        TextUtils.writeToParcel(mErrorMessage, dest, 0);
        dest.writeBundle(mExtras);
    }

    @Override
    public String toString()
    {
        return "PlaybackState {state=" + mState + ", position=" + mPosition + ", buffered position=" + mBufferedPosition
                + ", speed=" + mSpeed + ", updated=" + mUpdateTime + ", actions=" + mActions + ", custom actions="
                + mCustomActions + ", active item id=" + mActiveItemId + ", error=" + mErrorMessage + "}";
    }

    public static final class CustomAction implements Parcelable
    {
        public static final Parcelable.Creator<CustomAction> CREATOR = new Parcelable.Creator<CustomAction>()
        {
            @Override
            public CustomAction createFromParcel(Parcel source)
            {
                String action = source.readString();
                CharSequence name = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
                int icon = source.readInt();
                Bundle extras = source.readBundle();
                return new CustomAction(action, name, icon, extras);
            }

            @Override
            public CustomAction[] newArray(int size)
            {
                return new CustomAction[size];
            }
        };

        private final String mAction;
        private final CharSequence mName;
        private final int mIcon;
        private final Bundle mExtras;

        private CustomAction(String action, CharSequence name, int icon, Bundle extras)
        {
            mAction = action;
            mName = name;
            mIcon = icon;
            mExtras = extras;
        }

        public String getAction()
        {
            return mAction;
        }

        public CharSequence getName()
        {
            return mName;
        }

        public int getIcon()
        {
            return mIcon;
        }

        public Bundle getExtras()
        {
            return mExtras;
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(mAction);
            TextUtils.writeToParcel(mName, dest, flags);
            dest.writeInt(mIcon);
            dest.writeBundle(mExtras);
        }

        @Override
        public String toString()
        {
            return "Action:mName='" + mName + ", mIcon=" + mIcon + ", mExtras=" + mExtras;
        }

        public static final class Builder
        {
            private final String mAction;
            private final CharSequence mName;
            private final int mIcon;
            private Bundle mExtras;

            public Builder(String action, CharSequence name, int icon)
            {
                if (TextUtils.isEmpty(action))
                {
                    throw new IllegalArgumentException("You must specify an action to build a CustomAction");
                }

                if (TextUtils.isEmpty(name))
                {
                    throw new IllegalArgumentException("You must specify a name to build a CustomAction");
                }

                if (icon == 0)
                {
                    throw new IllegalArgumentException("You must specify an icon resource id to build a CustomAction");
                }

                mAction = action;
                mName = name;
                mIcon = icon;
            }

            public Builder setExtras(Bundle extras)
            {
                mExtras = extras;
                return this;
            }

            public CustomAction build()
            {
                return new CustomAction(mAction, mName, mIcon, mExtras);
            }
        }
    }

    public static final class Builder
    {
        private final List<CustomAction> mCustomActions = new ArrayList<>();

        private int mState;
        private long mPosition;
        private long mBufferedPosition;
        private float mSpeed;
        private long mActions;
        private CharSequence mErrorMessage;
        private long mUpdateTime;
        private long mActiveItemId = MediaSession.QueueItem.UNKNOWN_ID;
        private Bundle mExtras;

        public Builder()
        {
        }

        public Builder(PlaybackState from)
        {
            if (from == null) return;

            mState = from.mState;
            mPosition = from.mPosition;
            mBufferedPosition = from.mBufferedPosition;
            mSpeed = from.mSpeed;
            mActions = from.mActions;
            mCustomActions.addAll(from.mCustomActions);
            mErrorMessage = from.mErrorMessage;
            mUpdateTime = from.mUpdateTime;
            mActiveItemId = from.mActiveItemId;
            mExtras = from.mExtras;
        }

        public Builder setState(int state, long position, float playbackSpeed, long updateTime)
        {
            mState = state;
            mPosition = position;
            mUpdateTime = updateTime;
            mSpeed = playbackSpeed;
            return this;
        }

        public Builder setState(int state, long position, float playbackSpeed)
        {
            return setState(state, position, playbackSpeed, SystemClock.elapsedRealtime());
        }

        public Builder setActions(long actions)
        {
            mActions = actions;
            return this;
        }

        public Builder addCustomAction(String action, String name, int icon)
        {
            return addCustomAction(new CustomAction(action, name, icon, null));
        }

        public Builder addCustomAction(CustomAction customAction)
        {
            if (customAction == null)
            {
                throw new IllegalArgumentException("You may not add a null CustomAction to PlaybackState.");
            }

            mCustomActions.add(customAction);
            return this;
        }

        public Builder setBufferedPosition(long bufferedPosition)
        {
            mBufferedPosition = bufferedPosition;
            return this;
        }

        public Builder setActiveQueueItemId(long id)
        {
            mActiveItemId = id;
            return this;
        }

        public Builder setErrorMessage(CharSequence error)
        {
            mErrorMessage = error;
            return this;
        }

        public Builder setExtras(Bundle extras)
        {
            mExtras = extras;
            return this;
        }

        public PlaybackState build()
        {
            return new PlaybackState(mState, mPosition, mUpdateTime, mSpeed, mBufferedPosition, mActions, mCustomActions,
                    mActiveItemId, mErrorMessage, mExtras);
        }
    }
}
