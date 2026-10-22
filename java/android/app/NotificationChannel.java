package android.app;

import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.VibrationEffect;

import java.util.Arrays;
import java.util.Objects;

public final class NotificationChannel implements Parcelable
{
    public static final String DEFAULT_CHANNEL_ID = "miscellaneous";

    public static final String EDIT_CONVERSATION = "conversation";
    public static final String EDIT_IMPORTANCE = "importance";
    public static final String EDIT_LAUNCHER = "launcher";
    public static final String EDIT_LOCKED_DEVICE = "locked";
    public static final String EDIT_SOUND = "sound";
    public static final String EDIT_VIBRATION = "vibration";
    public static final String EDIT_ZEN = "zen";

    private static final int MAX_TEXT_LENGTH = 1000;
    private static final int MAX_VIBRATION_LENGTH = 1000;
    private static final int VISIBILITY_NO_OVERRIDE = -1000;
    private static final int DEFAULT_ALLOW_BUBBLE = -1;
    private static final int ALLOW_BUBBLE_ON = 1;
    private static final int ALLOW_BUBBLE_OFF = 0;

    public static final Parcelable.Creator<NotificationChannel> CREATOR = new Parcelable.Creator<NotificationChannel>()
    {
        @Override
        public NotificationChannel createFromParcel(Parcel source)
        {
            NotificationChannel channel = new NotificationChannel(source.readString(), (CharSequence)source.readValue(null),
                                                                  source.readInt());
            channel.mDescription = source.readString();
            channel.mBypassDnd = source.readBoolean();
            channel.mLockscreenVisibility = source.readInt();
            channel.mSound = (Uri)source.readValue(null);
            channel.mAudioAttributes = (AudioAttributes)source.readValue(null);
            channel.mLights = source.readBoolean();
            channel.mLightColor = source.readInt();
            channel.mVibrationEnabled = source.readBoolean();
            channel.mVibrationPattern = source.createLongArray();
            channel.mVibrationEffect = (VibrationEffect)source.readValue(null);
            channel.mGroup = source.readString();
            channel.mShowBadge = source.readBoolean();
            channel.mAllowBubbles = source.readInt();
            channel.mBlockable = source.readBoolean();
            channel.mParentId = source.readString();
            channel.mConversationId = source.readString();
            channel.mImportantConversation = source.readBoolean();
            channel.mDemoted = source.readBoolean();
            return channel;
        }

        @Override
        public NotificationChannel[] newArray(int size)
        {
            return new NotificationChannel[size];
        }
    };

    private final String mId;
    private CharSequence mName;
    private int mImportance;
    private String mDescription;
    private boolean mBypassDnd;
    private int mLockscreenVisibility = VISIBILITY_NO_OVERRIDE;
    private Uri mSound = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
    private AudioAttributes mAudioAttributes = Notification.AUDIO_ATTRIBUTES_DEFAULT;
    private boolean mLights;
    private int mLightColor;
    private boolean mVibrationEnabled;
    private long[] mVibrationPattern;
    private VibrationEffect mVibrationEffect;
    private String mGroup;
    private boolean mShowBadge = true;
    private int mAllowBubbles = DEFAULT_ALLOW_BUBBLE;
    private boolean mBlockable;
    private String mParentId;
    private String mConversationId;
    private boolean mImportantConversation;
    private boolean mDemoted;

    public NotificationChannel(String id, CharSequence name, int importance)
    {
        mId = trimmed(id);
        mName = name == null ? null : trimmed(name.toString());
        mImportance = importance;
    }

    private static String trimmed(String input)
    {
        if (input != null && input.length() > MAX_TEXT_LENGTH) return input.substring(0, MAX_TEXT_LENGTH);
        return input;
    }

    public String getId()
    {
        return mId;
    }

    public CharSequence getName()
    {
        return mName;
    }

    public void setName(CharSequence name)
    {
        mName = name == null ? null : trimmed(name.toString());
    }

    public int getImportance()
    {
        return mImportance;
    }

    public void setImportance(int importance)
    {
        mImportance = importance;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public void setDescription(String description)
    {
        mDescription = trimmed(description);
    }

    public String getGroup()
    {
        return mGroup;
    }

    public void setGroup(String groupId)
    {
        mGroup = groupId;
    }

    public boolean canShowBadge()
    {
        return mShowBadge;
    }

    public void setShowBadge(boolean showBadge)
    {
        mShowBadge = showBadge;
    }

    public boolean canBypassDnd()
    {
        return mBypassDnd;
    }

    public void setBypassDnd(boolean bypassDnd)
    {
        mBypassDnd = bypassDnd;
    }

    public int getLockscreenVisibility()
    {
        return mLockscreenVisibility;
    }

    public void setLockscreenVisibility(int lockscreenVisibility)
    {
        mLockscreenVisibility = lockscreenVisibility;
    }

    public Uri getSound()
    {
        return mSound;
    }

    public AudioAttributes getAudioAttributes()
    {
        return mAudioAttributes;
    }

    public void setSound(Uri sound, AudioAttributes audioAttributes)
    {
        mSound = sound;
        mAudioAttributes = audioAttributes;
    }

    public boolean shouldShowLights()
    {
        return mLights;
    }

    public void enableLights(boolean lights)
    {
        mLights = lights;
    }

    public int getLightColor()
    {
        return mLightColor;
    }

    public void setLightColor(int argb)
    {
        mLightColor = argb;
    }

    public boolean shouldVibrate()
    {
        return mVibrationEnabled;
    }

    public void enableVibration(boolean vibration)
    {
        mVibrationEnabled = vibration;
    }

    public long[] getVibrationPattern()
    {
        return mVibrationPattern;
    }

    public void setVibrationPattern(long[] vibrationPattern)
    {
        mVibrationEnabled = vibrationPattern != null && vibrationPattern.length > 0;

        if (vibrationPattern != null && vibrationPattern.length > MAX_VIBRATION_LENGTH)
        {
            vibrationPattern = Arrays.copyOf(vibrationPattern, MAX_VIBRATION_LENGTH);
        }

        mVibrationPattern = vibrationPattern;
        mVibrationEffect = mVibrationEnabled ? VibrationEffect.createWaveform(vibrationPattern, -1) : null;
    }

    public VibrationEffect getVibrationEffect()
    {
        return mVibrationEffect;
    }

    public void setVibrationEffect(VibrationEffect effect)
    {
        mVibrationEnabled = effect != null;
        mVibrationEffect = effect;
        if (effect == null) mVibrationPattern = null;
    }

    public boolean canBubble()
    {
        return mAllowBubbles == ALLOW_BUBBLE_ON;
    }

    public void setAllowBubbles(boolean allowBubbles)
    {
        mAllowBubbles = allowBubbles ? ALLOW_BUBBLE_ON : ALLOW_BUBBLE_OFF;
    }

    public boolean isBlockable()
    {
        return mBlockable;
    }

    public void setBlockable(boolean blockable)
    {
        mBlockable = blockable;
    }

    public void setConversationId(String parentChannelId, String conversationId)
    {
        mParentId = parentChannelId;
        mConversationId = conversationId;
    }

    public String getParentChannelId()
    {
        return mParentId;
    }

    public String getConversationId()
    {
        return mConversationId;
    }

    public boolean isConversation()
    {
        return mConversationId != null && !mConversationId.isEmpty();
    }

    public boolean isImportantConversation()
    {
        return mImportantConversation;
    }

    public boolean isDemoted()
    {
        return mDemoted;
    }

    public boolean hasUserSetImportance()
    {
        return false;
    }

    public boolean hasUserSetSound()
    {
        return false;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags)
    {
        parcel.writeString(mId);
        parcel.writeValue(mName);
        parcel.writeInt(mImportance);
        parcel.writeString(mDescription);
        parcel.writeBoolean(mBypassDnd);
        parcel.writeInt(mLockscreenVisibility);
        parcel.writeValue(mSound);
        parcel.writeValue(mAudioAttributes);
        parcel.writeBoolean(mLights);
        parcel.writeInt(mLightColor);
        parcel.writeBoolean(mVibrationEnabled);
        parcel.writeLongArray(mVibrationPattern);
        parcel.writeValue(mVibrationEffect);
        parcel.writeString(mGroup);
        parcel.writeBoolean(mShowBadge);
        parcel.writeInt(mAllowBubbles);
        parcel.writeBoolean(mBlockable);
        parcel.writeString(mParentId);
        parcel.writeString(mConversationId);
        parcel.writeBoolean(mImportantConversation);
        parcel.writeBoolean(mDemoted);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof NotificationChannel)) return false;

        NotificationChannel that = (NotificationChannel)other;
        return mImportance == that.mImportance && mBypassDnd == that.mBypassDnd
               && mLockscreenVisibility == that.mLockscreenVisibility && mLights == that.mLights
               && mLightColor == that.mLightColor && mVibrationEnabled == that.mVibrationEnabled
               && mShowBadge == that.mShowBadge && mAllowBubbles == that.mAllowBubbles && mBlockable == that.mBlockable
               && mImportantConversation == that.mImportantConversation && mDemoted == that.mDemoted
               && Objects.equals(mId, that.mId) && Objects.equals(textOf(mName), textOf(that.mName))
               && Objects.equals(mDescription, that.mDescription) && Objects.equals(mSound, that.mSound)
               && Arrays.equals(mVibrationPattern, that.mVibrationPattern) && Objects.equals(mGroup, that.mGroup)
               && Objects.equals(mAudioAttributes, that.mAudioAttributes) && Objects.equals(mParentId, that.mParentId)
               && Objects.equals(mConversationId, that.mConversationId);
    }

    private static String textOf(CharSequence value)
    {
        return value == null ? null : value.toString();
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(mId, textOf(mName), mImportance, mBypassDnd, mLockscreenVisibility, mSound, mLights,
                                  mLightColor, mVibrationEnabled, mGroup, mShowBadge, mAllowBubbles, mBlockable,
                                  mParentId, mConversationId, mImportantConversation, mDemoted, mDescription);
        return 31 * result + Arrays.hashCode(mVibrationPattern);
    }

    @Override
    public String toString()
    {
        return "NotificationChannel{mId='" + mId + "', mName=" + mName + ", mDescription=" + mDescription
               + ", mImportance=" + mImportance + ", mBypassDnd=" + mBypassDnd + ", mLockscreenVisibility="
               + mLockscreenVisibility + ", mSound=" + mSound + ", mLights=" + mLights + ", mLightColor=" + mLightColor
               + ", mVibrationPattern=" + Arrays.toString(mVibrationPattern) + ", mVibrationEnabled=" + mVibrationEnabled
               + ", mShowBadge=" + mShowBadge + ", mGroup='" + mGroup + "', mAllowBubbles=" + mAllowBubbles
               + ", mParentId='" + mParentId + "', mConversationId='" + mConversationId + "', mImportantConvo="
               + mImportantConversation + "}";
    }
}
