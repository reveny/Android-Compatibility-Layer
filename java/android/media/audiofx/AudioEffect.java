package android.media.audiofx;

import java.util.Objects;
import java.util.UUID;

public class AudioEffect
{
    public static final String ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION = "android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION";
    public static final String ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL = "android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL";
    public static final String ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION = "android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION";
    public static final int ALREADY_EXISTS = -2;
    public static final int CONTENT_TYPE_GAME = 2;
    public static final int CONTENT_TYPE_MOVIE = 1;
    public static final int CONTENT_TYPE_MUSIC = 0;
    public static final int CONTENT_TYPE_VOICE = 3;
    public static final String EFFECT_AUXILIARY = "Auxiliary";
    public static final String EFFECT_INSERT = "Insert";
    public static final String EFFECT_POST_PROCESSING = "Post Processing";
    public static final String EFFECT_PRE_PROCESSING = "Pre Processing";
    public static final UUID EFFECT_TYPE_AEC = UUID.fromString("7b491460-8d4d-11e0-bd61-0002a5d5c51b");
    public static final UUID EFFECT_TYPE_AGC = UUID.fromString("0a8abfe0-654c-11e0-ba26-0002a5d5c51b");
    public static final UUID EFFECT_TYPE_BASS_BOOST = UUID.fromString("0634f220-ddd4-11db-a0fc-0002a5d5c51b");
    public static final UUID EFFECT_TYPE_DYNAMICS_PROCESSING = UUID.fromString("7261676f-6d75-7369-6364-28e2fd3ac39e");
    public static final UUID EFFECT_TYPE_ENV_REVERB = UUID.fromString("c2e5d5f0-94bd-4763-9cac-4e234d06839e");
    public static final UUID EFFECT_TYPE_EQUALIZER = UUID.fromString("0bed4300-ddd6-11db-8f34-0002a5d5c51b");
    public static final UUID EFFECT_TYPE_HAPTIC_GENERATOR = UUID.fromString("1411e6d6-aecd-4021-a1cf-a6aceb0d71e5");
    public static final UUID EFFECT_TYPE_LOUDNESS_ENHANCER = UUID.fromString("fe3199be-aed0-413f-87bb-11260eb63cf1");
    public static final UUID EFFECT_TYPE_NS = UUID.fromString("58b4b260-8e06-11e0-aa8e-0002a5d5c51b");
    public static final UUID EFFECT_TYPE_PRESET_REVERB = UUID.fromString("47382d60-ddd8-11db-bf3a-0002a5d5c51b");
    public static final UUID EFFECT_TYPE_VIRTUALIZER = UUID.fromString("37cc2c00-dddd-11db-8577-0002a5d5c51b");
    public static final int ERROR = -1;
    public static final int ERROR_BAD_VALUE = -4;
    public static final int ERROR_DEAD_OBJECT = -7;
    public static final int ERROR_INVALID_OPERATION = -5;
    public static final int ERROR_NO_INIT = -3;
    public static final int ERROR_NO_MEMORY = -6;
    public static final String EXTRA_AUDIO_SESSION = "android.media.extra.AUDIO_SESSION";
    public static final String EXTRA_CONTENT_TYPE = "android.media.extra.CONTENT_TYPE";
    public static final String EXTRA_PACKAGE_NAME = "android.media.extra.PACKAGE_NAME";
    public static final int SUCCESS = 0;

    static final UUID EFFECT_TYPE_NULL = UUID.fromString("ec7178ec-e5e1-4432-a3f4-4657e6795210");

    private static final int STATE_UNINITIALIZED = 0;

    private final int mState = STATE_UNINITIALIZED;
    private OnEnableStatusChangeListener mEnableStatusListener;
    private OnControlStatusChangeListener mControlStatusListener;

    AudioEffect(UUID type, UUID uuid, int priority, int audioSession)
        throws IllegalArgumentException, UnsupportedOperationException, RuntimeException
    {
        if (type == null || uuid == null) throw new IllegalArgumentException("Effect type and uuid must not be null");
        throw new UnsupportedOperationException("Effect library not loaded");
    }

    public AudioEffect.Descriptor getDescriptor() throws IllegalStateException
    {
        checkState("getDescriptor()");
        return null;
    }

    public boolean getEnabled() throws IllegalStateException
    {
        checkState("getEnabled()");
        return false;
    }

    public int getId() throws IllegalStateException
    {
        checkState("getId()");
        return 0;
    }

    public boolean hasControl() throws IllegalStateException
    {
        checkState("hasControl()");
        return false;
    }

    public static AudioEffect.Descriptor[] queryEffects()
    {
        return new Descriptor[0];
    }

    static boolean isEffectTypeAvailable(UUID type)
    {
        for (Descriptor descriptor : queryEffects())
        {
            if (descriptor.type.equals(type)) return true;
        }

        return false;
    }

    public void release()
    {
        synchronized (this)
        {
            mEnableStatusListener = null;
            mControlStatusListener = null;
        }
    }

    @Override
    protected void finalize()
    {
        release();
    }

    public void setControlStatusListener(AudioEffect.OnControlStatusChangeListener listener)
    {
        synchronized (this)
        {
            mControlStatusListener = listener;
        }
    }

    public void setEnableStatusListener(AudioEffect.OnEnableStatusChangeListener listener)
    {
        synchronized (this)
        {
            mEnableStatusListener = listener;
        }
    }

    public int setEnabled(boolean enabled) throws IllegalStateException
    {
        checkState("setEnabled()");
        return ERROR_NO_INIT;
    }

    void checkState(String methodName) throws IllegalStateException
    {
        if (mState == STATE_UNINITIALIZED) throw new IllegalStateException(methodName + " called on uninitialized AudioEffect.");
    }

    public interface OnEnableStatusChangeListener
    {
        void onEnableStatusChange(AudioEffect effect, boolean enabled);
    }

    public interface OnControlStatusChangeListener
    {
        void onControlStatusChange(AudioEffect effect, boolean controlGranted);
    }

    public static class Descriptor
    {
        public String connectMode;
        public String implementor;
        public String name;
        public UUID type;
        public UUID uuid;

        public Descriptor()
        {
        }

        public Descriptor(String type, String uuid, String connectMode, String name, String implementor)
        {
            this.type = UUID.fromString(type);
            this.uuid = UUID.fromString(uuid);
            this.connectMode = connectMode;
            this.name = name;
            this.implementor = implementor;
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) return true;
            if (!(object instanceof Descriptor)) return false;

            Descriptor other = (Descriptor)object;
            return Objects.equals(type, other.type) && Objects.equals(uuid, other.uuid) && Objects.equals(connectMode, other.connectMode)
                && Objects.equals(name, other.name) && Objects.equals(implementor, other.implementor);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(type, uuid, connectMode, name, implementor);
        }
    }
}
