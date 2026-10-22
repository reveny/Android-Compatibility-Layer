package android.media;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioManager
{
    public static final String ACTION_AUDIO_BECOMING_NOISY = "android.media.AUDIO_BECOMING_NOISY";
    public static final String ACTION_HDMI_AUDIO_PLUG = "android.media.action.HDMI_AUDIO_PLUG";
    public static final String ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    public static final String ACTION_MICROPHONE_MUTE_CHANGED = "android.media.action.MICROPHONE_MUTE_CHANGED";
    public static final String ACTION_SCO_AUDIO_STATE_CHANGED = "android.media.SCO_AUDIO_STATE_CHANGED";
    public static final String ACTION_SCO_AUDIO_STATE_UPDATED = "android.media.ACTION_SCO_AUDIO_STATE_UPDATED";
    public static final String ACTION_SPEAKERPHONE_STATE_CHANGED = "android.media.action.SPEAKERPHONE_STATE_CHANGED";
    public static final int ADJUST_LOWER = -1;
    public static final int ADJUST_MUTE = -100;
    public static final int ADJUST_RAISE = 1;
    public static final int ADJUST_SAME = 0;
    public static final int ADJUST_TOGGLE_MUTE = 101;
    public static final int ADJUST_UNMUTE = 100;
    public static final int AUDIOFOCUS_GAIN = 1;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
    public static final int AUDIOFOCUS_LOSS = -1;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT = -2;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = -3;
    public static final int AUDIOFOCUS_NONE = 0;
    public static final int AUDIOFOCUS_REQUEST_DELAYED = 2;
    public static final int AUDIOFOCUS_REQUEST_FAILED = 0;
    public static final int AUDIOFOCUS_REQUEST_GRANTED = 1;
    public static final int AUDIO_SESSION_ID_GENERATE = 0;
    public static final int DIRECT_PLAYBACK_BITSTREAM_SUPPORTED = 4;
    public static final int DIRECT_PLAYBACK_NOT_SUPPORTED = 0;
    public static final int DIRECT_PLAYBACK_OFFLOAD_GAPLESS_SUPPORTED = 3;
    public static final int DIRECT_PLAYBACK_OFFLOAD_SUPPORTED = 1;
    public static final int ENCODED_SURROUND_OUTPUT_ALWAYS = 2;
    public static final int ENCODED_SURROUND_OUTPUT_AUTO = 0;
    public static final int ENCODED_SURROUND_OUTPUT_MANUAL = 3;
    public static final int ENCODED_SURROUND_OUTPUT_NEVER = 1;
    public static final int ENCODED_SURROUND_OUTPUT_UNKNOWN = -1;
    public static final int ERROR = -1;
    public static final int ERROR_DEAD_OBJECT = -6;
    public static final String EXTRA_AUDIO_PLUG_STATE = "android.media.extra.AUDIO_PLUG_STATE";
    public static final String EXTRA_ENCODINGS = "android.media.extra.ENCODINGS";
    public static final String EXTRA_MAX_CHANNEL_COUNT = "android.media.extra.MAX_CHANNEL_COUNT";
    public static final String EXTRA_RINGER_MODE = "android.media.EXTRA_RINGER_MODE";
    public static final String EXTRA_SCO_AUDIO_PREVIOUS_STATE = "android.media.extra.SCO_AUDIO_PREVIOUS_STATE";
    public static final String EXTRA_SCO_AUDIO_STATE = "android.media.extra.SCO_AUDIO_STATE";
    public static final String EXTRA_VIBRATE_SETTING = "android.media.EXTRA_VIBRATE_SETTING";
    public static final String EXTRA_VIBRATE_TYPE = "android.media.EXTRA_VIBRATE_TYPE";
    public static final int FLAG_ALLOW_RINGER_MODES = 2;
    public static final int FLAG_PLAY_SOUND = 4;
    public static final int FLAG_REMOVE_SOUND_AND_VIBRATE = 8;
    public static final int FLAG_SHOW_UI = 1;
    public static final int FLAG_VIBRATE = 16;
    public static final int FX_BACK = 10;
    public static final int FX_FOCUS_NAVIGATION_DOWN = 2;
    public static final int FX_FOCUS_NAVIGATION_LEFT = 3;
    public static final int FX_FOCUS_NAVIGATION_RIGHT = 4;
    public static final int FX_FOCUS_NAVIGATION_UP = 1;
    public static final int FX_KEYPRESS_DELETE = 7;
    public static final int FX_KEYPRESS_INVALID = 9;
    public static final int FX_KEYPRESS_RETURN = 8;
    public static final int FX_KEYPRESS_SPACEBAR = 6;
    public static final int FX_KEYPRESS_STANDARD = 5;
    public static final int FX_KEY_CLICK = 0;
    public static final int GET_DEVICES_ALL = 3;
    public static final int GET_DEVICES_INPUTS = 1;
    public static final int GET_DEVICES_OUTPUTS = 2;
    public static final int MODE_CALL_REDIRECT = 5;
    public static final int MODE_CALL_SCREENING = 4;
    public static final int MODE_COMMUNICATION_REDIRECT = 6;
    public static final int MODE_CURRENT = -1;
    public static final int MODE_INVALID = -2;
    public static final int MODE_IN_CALL = 2;
    public static final int MODE_IN_COMMUNICATION = 3;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_RINGTONE = 1;
    public static final int NUM_STREAMS = 5;
    public static final int PLAYBACK_OFFLOAD_GAPLESS_SUPPORTED = 2;
    public static final int PLAYBACK_OFFLOAD_NOT_SUPPORTED = 0;
    public static final int PLAYBACK_OFFLOAD_SUPPORTED = 1;
    public static final String PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER";
    public static final String PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE";
    public static final String PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED = "android.media.property.SUPPORT_AUDIO_SOURCE_UNPROCESSED";
    public static final String PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND = "android.media.property.SUPPORT_MIC_NEAR_ULTRASOUND";
    public static final String PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND = "android.media.property.SUPPORT_SPEAKER_NEAR_ULTRASOUND";
    public static final String RINGER_MODE_CHANGED_ACTION = "android.media.RINGER_MODE_CHANGED";
    public static final int RINGER_MODE_NORMAL = 2;
    public static final int RINGER_MODE_SILENT = 0;
    public static final int RINGER_MODE_VIBRATE = 1;
    public static final int ROUTE_ALL = -1;
    public static final int ROUTE_BLUETOOTH = 4;
    public static final int ROUTE_BLUETOOTH_A2DP = 16;
    public static final int ROUTE_BLUETOOTH_SCO = 4;
    public static final int ROUTE_EARPIECE = 1;
    public static final int ROUTE_HEADSET = 8;
    public static final int ROUTE_SPEAKER = 2;
    public static final int SCO_AUDIO_STATE_CONNECTED = 1;
    public static final int SCO_AUDIO_STATE_CONNECTING = 2;
    public static final int SCO_AUDIO_STATE_DISCONNECTED = 0;
    public static final int SCO_AUDIO_STATE_ERROR = -1;
    public static final int STREAM_ACCESSIBILITY = 10;
    public static final int STREAM_ALARM = 4;
    public static final int STREAM_DTMF = 8;
    public static final int STREAM_MUSIC = 3;
    public static final int STREAM_NOTIFICATION = 5;
    public static final int STREAM_RING = 2;
    public static final int STREAM_SYSTEM = 1;
    public static final int STREAM_VOICE_CALL = 0;
    public static final int USE_DEFAULT_STREAM_TYPE = -2147483648;
    public static final String VIBRATE_SETTING_CHANGED_ACTION = "android.media.VIBRATE_SETTING_CHANGED";
    public static final int VIBRATE_SETTING_OFF = 0;
    public static final int VIBRATE_SETTING_ON = 1;
    public static final int VIBRATE_SETTING_ONLY_SILENT = 2;
    public static final int VIBRATE_TYPE_NOTIFICATION = 1;
    public static final int VIBRATE_TYPE_RINGER = 0;

    private static final int STREAM_COUNT = 12;
    private static final int[] MAX_STREAM_VOLUME = {5, 7, 7, 15, 7, 7, 15, 7, 15, 15, 15, 15};
    private static final int[] MIN_STREAM_VOLUME = {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0};
    private static final int[] DEFAULT_STREAM_VOLUME = {4, 7, 5, 10, 6, 5, 7, 7, 5, 5, 5, 5};
    private static final int SOUND_EFFECT_COUNT = 16;
    private static final int OUTPUT_SAMPLE_RATE = 48000;
    private static final int OUTPUT_FRAMES_PER_BUFFER = 256;

    private static final AudioDeviceInfo BUILTIN_SPEAKER =
        new AudioDeviceInfo(2, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true, "", "acl");
    private static final AudioDeviceInfo BUILTIN_MIC =
        new AudioDeviceInfo(3, AudioDeviceInfo.TYPE_BUILTIN_MIC, false, "bottom", "acl");

    private static final Object sLock = new Object();
    private static final AtomicInteger sNextSessionId = new AtomicInteger(1);
    private static final AtomicInteger sNextPlayerId = new AtomicInteger(1);
    private static final int[] sStreamVolume = DEFAULT_STREAM_VOLUME.clone();
    private static final boolean[] sStreamMuted = new boolean[STREAM_COUNT];
    private static final int[] sVibrateSetting = {VIBRATE_SETTING_ONLY_SILENT, VIBRATE_SETTING_ONLY_SILENT};
    private static final List<FocusOwner> sFocusStack = new ArrayList<>();
    private static final Map<Object, AudioPlaybackConfiguration> sActivePlayers = new IdentityHashMap<>();
    private static final List<CallbackEntry<AudioPlaybackCallback>> sPlaybackCallbacks = new CopyOnWriteArrayList<>();
    private static final List<CallbackEntry<AudioRecordingCallback>> sRecordingCallbacks = new CopyOnWriteArrayList<>();
    private static final List<CallbackEntry<AudioDeviceCallback>> sDeviceCallbacks = new CopyOnWriteArrayList<>();
    private static final List<ExecutorEntry<OnModeChangedListener>> sModeListeners = new CopyOnWriteArrayList<>();
    private static final List<ExecutorEntry<OnCommunicationDeviceChangedListener>> sCommunicationDeviceListeners =
        new CopyOnWriteArrayList<>();
    private static final List<ExecutorEntry<OnPreferredMixerAttributesChangedListener>> sMixerAttributesListeners =
        new CopyOnWriteArrayList<>();
    private static final Set<Object> sMediaButtonReceivers = new HashSet<>();
    private static final Set<RemoteControlClient> sRemoteControlClients = Collections.newSetFromMap(new IdentityHashMap<>());

    private static int sMode = MODE_NORMAL;
    private static int sRingerMode = RINGER_MODE_NORMAL;
    private static boolean sMicrophoneMuted;
    private static boolean sSpeakerphoneOn = true;
    private static boolean sBluetoothScoOn;
    private static boolean sBluetoothA2dpOn;
    private static boolean sWiredHeadsetOn;
    private static int sAllowedCapturePolicy = AudioAttributes.ALLOW_CAPTURE_BY_ALL;
    private static AudioDeviceInfo sCommunicationDevice = BUILTIN_SPEAKER;

    private Spatializer mSpatializer;

    public AudioManager()
    {
    }

    public interface OnPreferredMixerAttributesChangedListener
    {
        void onPreferredMixerAttributesChanged(AudioAttributes attributes, AudioDeviceInfo device,
            AudioMixerAttributes mixerAttributes);
    }

    public interface OnModeChangedListener
    {
        void onModeChanged(int mode);
    }

    public interface OnCommunicationDeviceChangedListener
    {
        void onCommunicationDeviceChanged(AudioDeviceInfo device);
    }

    public interface OnAudioFocusChangeListener
    {
        void onAudioFocusChange(int focusChange);
    }

    public abstract static class AudioRecordingCallback
    {
        public AudioRecordingCallback()
        {
        }

        public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs)
        {
        }
    }

    public abstract static class AudioPlaybackCallback
    {
        public AudioPlaybackCallback()
        {
        }

        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs)
        {
        }
    }

    private static final class FocusOwner
    {
        final Object mKey;
        final AudioFocusRequest mRequest;
        int mLoss = AUDIOFOCUS_NONE;

        FocusOwner(Object key, AudioFocusRequest request)
        {
            mKey = key;
            mRequest = request;
        }
    }

    private static final class CallbackEntry<T>
    {
        final T mCallback;
        final Handler mHandler;

        CallbackEntry(T callback, Handler handler)
        {
            mCallback = callback;
            mHandler = handler;
        }
    }

    private static final class ExecutorEntry<T>
    {
        final T mListener;
        final Executor mExecutor;

        ExecutorEntry(T listener, Executor executor)
        {
            mListener = listener;
            mExecutor = executor;
        }
    }

    static int newAudioSessionId()
    {
        return sNextSessionId.getAndIncrement();
    }

    static int registerActivePlayer(Object player, AudioAttributes attributes, int sessionId)
    {
        List<AudioPlaybackConfiguration> snapshot;
        int playerId = sNextPlayerId.getAndIncrement();

        synchronized (sLock)
        {
            sActivePlayers.put(player, new AudioPlaybackConfiguration(playerId, sessionId, attributes, BUILTIN_SPEAKER));
            snapshot = new ArrayList<>(sActivePlayers.values());
        }

        dispatchPlaybackConfigurations(snapshot);
        return playerId;
    }

    static void unregisterActivePlayer(Object player)
    {
        List<AudioPlaybackConfiguration> snapshot;

        synchronized (sLock)
        {
            if (sActivePlayers.remove(player) == null) return;

            snapshot = new ArrayList<>(sActivePlayers.values());
        }

        dispatchPlaybackConfigurations(snapshot);
    }

    private static void dispatchPlaybackConfigurations(List<AudioPlaybackConfiguration> snapshot)
    {
        for (CallbackEntry<AudioPlaybackCallback> entry : sPlaybackCallbacks)
        {
            post(entry.mHandler, () -> entry.mCallback.onPlaybackConfigChanged(new ArrayList<>(snapshot)));
        }
    }

    private static void post(Handler handler, Runnable action)
    {
        if (handler != null)
        {
            handler.post(action);
            return;
        }

        Looper mainLooper = Looper.getMainLooper();
        if (mainLooper != null)
        {
            new Handler(mainLooper).post(action);
        }
        else
        {
            action.run();
        }
    }

    private static void checkStreamType(int streamType)
    {
        if (streamType < 0 || streamType >= STREAM_COUNT) throw new IllegalArgumentException("Bad stream type " + streamType);
    }

    private static AudioDeviceInfo[] devicesFor(int flags)
    {
        List<AudioDeviceInfo> devices = new ArrayList<>();

        if ((flags & GET_DEVICES_OUTPUTS) != 0) devices.add(BUILTIN_SPEAKER);
        if ((flags & GET_DEVICES_INPUTS) != 0) devices.add(BUILTIN_MIC);

        return devices.toArray(new AudioDeviceInfo[0]);
    }

    public int abandonAudioFocus(OnAudioFocusChangeListener listener)
    {
        abandonFocus(listener);
        return AUDIOFOCUS_REQUEST_GRANTED;
    }

    public int abandonAudioFocusRequest(AudioFocusRequest focusRequest)
    {
        if (focusRequest == null) throw new IllegalArgumentException("Illegal null AudioFocusRequest");

        abandonFocus(focusKey(focusRequest));
        return AUDIOFOCUS_REQUEST_GRANTED;
    }

    private static Object focusKey(AudioFocusRequest request)
    {
        OnAudioFocusChangeListener listener = request.getOnAudioFocusChangeListener();
        return listener != null ? listener : request;
    }

    private static void abandonFocus(Object key)
    {
        FocusOwner regained = null;

        synchronized (sLock)
        {
            int index = indexOfFocusOwner(key);
            if (index < 0) return;

            boolean wasTop = index == sFocusStack.size() - 1;
            sFocusStack.remove(index);

            if (wasTop && !sFocusStack.isEmpty())
            {
                FocusOwner top = sFocusStack.get(sFocusStack.size() - 1);
                if (top.mLoss != AUDIOFOCUS_NONE)
                {
                    top.mLoss = AUDIOFOCUS_NONE;
                    regained = top;
                }
            }
        }

        if (regained != null) dispatchFocusChange(regained, AUDIOFOCUS_GAIN);
    }

    private static int indexOfFocusOwner(Object key)
    {
        for (int index = 0; index < sFocusStack.size(); index++)
        {
            if (sFocusStack.get(index).mKey == key) return index;
        }

        return -1;
    }

    private static void dispatchFocusChange(FocusOwner owner, int focusChange)
    {
        OnAudioFocusChangeListener listener = owner.mRequest.getOnAudioFocusChangeListener();
        if (listener == null) return;

        post(owner.mRequest.getOnAudioFocusChangeListenerHandler(), () -> listener.onAudioFocusChange(focusChange));
    }

    private static int grantFocus(Object key, AudioFocusRequest request)
    {
        List<FocusOwner> losers = new ArrayList<>();
        List<Integer> losses = new ArrayList<>();
        int gain = request.getFocusGain();

        synchronized (sLock)
        {
            int existing = indexOfFocusOwner(key);
            if (existing >= 0) sFocusStack.remove(existing);

            if (gain == AUDIOFOCUS_GAIN)
            {
                for (FocusOwner owner : sFocusStack)
                {
                    if (owner.mLoss != AUDIOFOCUS_LOSS)
                    {
                        losers.add(owner);
                        losses.add(AUDIOFOCUS_LOSS);
                    }
                }

                sFocusStack.clear();
            }
            else if (!sFocusStack.isEmpty())
            {
                FocusOwner top = sFocusStack.get(sFocusStack.size() - 1);
                int loss = gain == AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ? AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK : AUDIOFOCUS_LOSS_TRANSIENT;

                if (top.mLoss == AUDIOFOCUS_NONE)
                {
                    top.mLoss = loss;

                    boolean notify = loss != AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK || top.mRequest.willPauseWhenDucked()
                        || request.forcesDucking();
                    if (notify)
                    {
                        losers.add(top);
                        losses.add(loss);
                    }
                }
            }

            sFocusStack.add(new FocusOwner(key, request));
        }

        for (int index = 0; index < losers.size(); index++)
        {
            dispatchFocusChange(losers.get(index), losses.get(index));
        }

        return AUDIOFOCUS_REQUEST_GRANTED;
    }

    public void addOnCommunicationDeviceChangedListener(Executor executor, OnCommunicationDeviceChangedListener listener)
    {
        addExecutorListener(sCommunicationDeviceListeners, executor, listener, "addOnCommunicationDeviceChangedListener");
    }

    public void addOnModeChangedListener(Executor executor, OnModeChangedListener listener)
    {
        addExecutorListener(sModeListeners, executor, listener, "addOnModeChangedListener");
    }

    public void addOnPreferredMixerAttributesChangedListener(Executor executor, OnPreferredMixerAttributesChangedListener listener)
    {
        addExecutorListener(sMixerAttributesListeners, executor, listener, "addOnPreferredMixerAttributesChangedListener");
    }

    private static <T> void addExecutorListener(List<ExecutorEntry<T>> listeners, Executor executor, T listener, String caller)
    {
        Objects.requireNonNull(executor, "Executor must not be null");
        Objects.requireNonNull(listener, "Listener must not be null");

        synchronized (listeners)
        {
            for (ExecutorEntry<T> entry : listeners)
            {
                if (entry.mListener == listener)
                {
                    throw new IllegalArgumentException("attempt to call " + caller + "() on a previously registered listener");
                }
            }

            listeners.add(new ExecutorEntry<>(listener, executor));
        }
    }

    private static <T> void removeExecutorListener(List<ExecutorEntry<T>> listeners, T listener, String caller)
    {
        Objects.requireNonNull(listener, "Listener must not be null");

        synchronized (listeners)
        {
            for (ExecutorEntry<T> entry : listeners)
            {
                if (entry.mListener == listener)
                {
                    listeners.remove(entry);
                    return;
                }
            }
        }

        throw new IllegalArgumentException("attempt to call " + caller + "() on an unregistered listener");
    }

    public void adjustStreamVolume(int streamType, int direction, int flags)
    {
        checkStreamType(streamType);

        synchronized (sLock)
        {
            switch (direction)
            {
                case ADJUST_RAISE:
                    sStreamMuted[streamType] = false;
                    sStreamVolume[streamType] = Math.min(MAX_STREAM_VOLUME[streamType], sStreamVolume[streamType] + 1);
                    break;
                case ADJUST_LOWER:
                    sStreamVolume[streamType] = Math.max(MIN_STREAM_VOLUME[streamType], sStreamVolume[streamType] - 1);
                    break;
                case ADJUST_SAME:
                    break;
                case ADJUST_MUTE:
                    sStreamMuted[streamType] = true;
                    break;
                case ADJUST_UNMUTE:
                    sStreamMuted[streamType] = false;
                    break;
                case ADJUST_TOGGLE_MUTE:
                    sStreamMuted[streamType] = !sStreamMuted[streamType];
                    break;
                default:
                    throw new IllegalArgumentException("Bad direction " + direction);
            }
        }
    }

    public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags)
    {
        adjustStreamVolume(suggestedStreamType == USE_DEFAULT_STREAM_TYPE ? STREAM_MUSIC : suggestedStreamType, direction, flags);
    }

    public void adjustVolume(int direction, int flags)
    {
        adjustStreamVolume(STREAM_MUSIC, direction, flags);
    }

    public void adjustVolumeGroupVolume(int groupId, int direction, int flags)
    {
        if (groupId >= 0 && groupId < STREAM_COUNT) adjustStreamVolume(groupId, direction, flags);
    }

    public void clearCommunicationDevice()
    {
        setCommunicationDeviceInternal(BUILTIN_SPEAKER);
    }

    public boolean clearPreferredMixerAttributes(AudioAttributes attributes, AudioDeviceInfo device)
    {
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(device);

        return false;
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent)
    {
        if (keyEvent == null) throw new IllegalArgumentException("Illegal null KeyEvent");
    }

    public int generateAudioSessionId()
    {
        return newAudioSessionId();
    }

    public List<AudioPlaybackConfiguration> getActivePlaybackConfigurations()
    {
        synchronized (sLock)
        {
            return new ArrayList<>(sActivePlayers.values());
        }
    }

    public List<AudioRecordingConfiguration> getActiveRecordingConfigurations()
    {
        return new ArrayList<>();
    }

    public int getAllowedCapturePolicy()
    {
        return sAllowedCapturePolicy;
    }

    public List<AudioDeviceInfo> getAudioDevicesForAttributes(AudioAttributes attributes)
    {
        Objects.requireNonNull(attributes);

        List<AudioDeviceInfo> devices = new ArrayList<>();
        devices.add(BUILTIN_SPEAKER);
        return devices;
    }

    public int getAudioHwSyncForSession(int sessionId)
    {
        return 0;
    }

    public List<AudioDeviceInfo> getAvailableCommunicationDevices()
    {
        List<AudioDeviceInfo> devices = new ArrayList<>();
        devices.add(BUILTIN_SPEAKER);
        return devices;
    }

    public AudioDeviceInfo getCommunicationDevice()
    {
        synchronized (sLock)
        {
            return sCommunicationDevice;
        }
    }

    public AudioDeviceInfo[] getDevices(int flags)
    {
        return devicesFor(flags);
    }

    public static int getDirectPlaybackSupport(AudioFormat format, AudioAttributes attributes)
    {
        Objects.requireNonNull(format);
        Objects.requireNonNull(attributes);

        return DIRECT_PLAYBACK_NOT_SUPPORTED;
    }

    public List<AudioProfile> getDirectProfilesForAttributes(AudioAttributes attributes)
    {
        Objects.requireNonNull(attributes);

        return new ArrayList<>();
    }

    public int getEncodedSurroundMode()
    {
        return ENCODED_SURROUND_OUTPUT_AUTO;
    }

    public List<MicrophoneInfo> getMicrophones() throws IOException
    {
        List<MicrophoneInfo> microphones = new ArrayList<>();
        microphones.add(new MicrophoneInfo("builtin_mic", AudioDeviceInfo.TYPE_BUILTIN_MIC, BUILTIN_MIC.getAddress(),
            MicrophoneInfo.LOCATION_MAINBODY, MicrophoneInfo.GROUP_UNKNOWN, MicrophoneInfo.INDEX_IN_THE_GROUP_UNKNOWN,
            BUILTIN_MIC.getId(), 1));
        return microphones;
    }

    public int getMode()
    {
        synchronized (sLock)
        {
            return sMode;
        }
    }

    public String getParameters(String keys)
    {
        return "";
    }

    public static int getPlaybackOffloadSupport(AudioFormat format, AudioAttributes attributes)
    {
        Objects.requireNonNull(format);
        Objects.requireNonNull(attributes);

        return PLAYBACK_OFFLOAD_NOT_SUPPORTED;
    }

    public AudioMixerAttributes getPreferredMixerAttributes(AudioAttributes attributes, AudioDeviceInfo device)
    {
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(device);

        return null;
    }

    public String getProperty(String key)
    {
        if (PROPERTY_OUTPUT_SAMPLE_RATE.equals(key)) return Integer.toString(OUTPUT_SAMPLE_RATE);
        if (PROPERTY_OUTPUT_FRAMES_PER_BUFFER.equals(key)) return Integer.toString(OUTPUT_FRAMES_PER_BUFFER);

        return null;
    }

    public int getRingerMode()
    {
        synchronized (sLock)
        {
            return sRingerMode;
        }
    }

    public int getRouting(int mode)
    {
        return -1;
    }

    public Spatializer getSpatializer()
    {
        if (mSpatializer == null) mSpatializer = new Spatializer();

        return mSpatializer;
    }

    public int getStreamMaxVolume(int streamType)
    {
        checkStreamType(streamType);

        return MAX_STREAM_VOLUME[streamType];
    }

    public int getStreamMinVolume(int streamType)
    {
        checkStreamType(streamType);

        return MIN_STREAM_VOLUME[streamType];
    }

    public int getStreamVolume(int streamType)
    {
        checkStreamType(streamType);

        synchronized (sLock)
        {
            return sStreamMuted[streamType] ? 0 : sStreamVolume[streamType];
        }
    }

    public float getStreamVolumeDb(int streamType, int index, int deviceType)
    {
        checkStreamType(streamType);

        if (index < MIN_STREAM_VOLUME[streamType] || index > MAX_STREAM_VOLUME[streamType])
        {
            throw new IllegalArgumentException("Invalid stream volume index " + index);
        }

        if (index == 0) return Float.NEGATIVE_INFINITY;

        float fraction = (float)index / MAX_STREAM_VOLUME[streamType];
        return -58.0f * (1.0f - fraction) * (1.0f - fraction);
    }

    public Set<Integer> getSupportedDeviceTypes(int direction)
    {
        if (direction != GET_DEVICES_OUTPUTS && direction != GET_DEVICES_INPUTS)
        {
            throw new IllegalArgumentException("Invalid direction " + direction);
        }

        Set<Integer> types = new HashSet<>();
        types.add(direction == GET_DEVICES_OUTPUTS ? AudioDeviceInfo.TYPE_BUILTIN_SPEAKER : AudioDeviceInfo.TYPE_BUILTIN_MIC);
        return types;
    }

    public List<AudioMixerAttributes> getSupportedMixerAttributes(AudioDeviceInfo device)
    {
        Objects.requireNonNull(device);

        List<AudioMixerAttributes> attributes = new ArrayList<>();
        if (!device.isSink()) return attributes;

        AudioFormat format = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(OUTPUT_SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build();
        attributes.add(new AudioMixerAttributes.Builder(format).build());
        return attributes;
    }

    public int getVibrateSetting(int vibrateType)
    {
        if (vibrateType != VIBRATE_TYPE_RINGER && vibrateType != VIBRATE_TYPE_NOTIFICATION) return VIBRATE_SETTING_OFF;

        synchronized (sLock)
        {
            return sVibrateSetting[vibrateType];
        }
    }

    public int getVolumeGroupIdForAttributes(AudioAttributes attributes)
    {
        Objects.requireNonNull(attributes);

        return attributes.getVolumeControlStream();
    }

    public boolean isBluetoothA2dpOn()
    {
        return sBluetoothA2dpOn;
    }

    public boolean isBluetoothScoAvailableOffCall()
    {
        return false;
    }

    public boolean isBluetoothScoOn()
    {
        return sBluetoothScoOn;
    }

    public boolean isCallScreeningModeSupported()
    {
        return false;
    }

    public static boolean isHapticPlaybackSupported()
    {
        return false;
    }

    public boolean isMicrophoneMute()
    {
        return sMicrophoneMuted;
    }

    public boolean isMusicActive()
    {
        synchronized (sLock)
        {
            for (AudioPlaybackConfiguration configuration : sActivePlayers.values())
            {
                if (configuration.getAudioAttributes().getVolumeControlStream() == STREAM_MUSIC) return true;
            }

            return false;
        }
    }

    public static boolean isOffloadedPlaybackSupported(AudioFormat format, AudioAttributes attributes)
    {
        if (format == null) throw new NullPointerException("Illegal null AudioFormat");
        if (attributes == null) throw new NullPointerException("Illegal null AudioAttributes");

        return false;
    }

    public boolean isRampingRingerEnabled()
    {
        return false;
    }

    public boolean isSpeakerphoneOn()
    {
        return sSpeakerphoneOn;
    }

    public boolean isStreamMute(int streamType)
    {
        checkStreamType(streamType);

        synchronized (sLock)
        {
            return sStreamMuted[streamType];
        }
    }

    public boolean isSurroundFormatEnabled(int audioFormat)
    {
        return false;
    }

    public boolean isVolumeFixed()
    {
        return false;
    }

    public boolean isVolumeGroupMuted(int groupId)
    {
        if (groupId < 0 || groupId >= STREAM_COUNT) return false;

        return isStreamMute(groupId);
    }

    public boolean isWiredHeadsetOn()
    {
        return sWiredHeadsetOn;
    }

    public void loadSoundEffects()
    {
    }

    public void playSoundEffect(int effectType)
    {
        playSoundEffect(effectType, -1.0f);
    }

    public void playSoundEffect(int effectType, float volume)
    {
        if (effectType < 0 || effectType >= SOUND_EFFECT_COUNT) return;
    }

    public void registerAudioDeviceCallback(AudioDeviceCallback callback, Handler handler)
    {
        if (callback == null) return;

        for (CallbackEntry<AudioDeviceCallback> entry : sDeviceCallbacks)
        {
            if (entry.mCallback == callback) return;
        }

        sDeviceCallbacks.add(new CallbackEntry<>(callback, handler));
        post(handler, () -> callback.onAudioDevicesAdded(devicesFor(GET_DEVICES_ALL)));
    }

    public void registerAudioPlaybackCallback(AudioPlaybackCallback callback, Handler handler)
    {
        if (callback == null) throw new IllegalArgumentException("Illegal null AudioPlaybackCallback argument");

        for (CallbackEntry<AudioPlaybackCallback> entry : sPlaybackCallbacks)
        {
            if (entry.mCallback == callback) return;
        }

        sPlaybackCallbacks.add(new CallbackEntry<>(callback, handler));
    }

    public void registerAudioRecordingCallback(AudioRecordingCallback callback, Handler handler)
    {
        if (callback == null) throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");

        for (CallbackEntry<AudioRecordingCallback> entry : sRecordingCallbacks)
        {
            if (entry.mCallback == callback) return;
        }

        sRecordingCallbacks.add(new CallbackEntry<>(callback, handler));
    }

    public void registerMediaButtonEventReceiver(PendingIntent eventReceiver)
    {
        if (eventReceiver == null) return;

        synchronized (sMediaButtonReceivers)
        {
            sMediaButtonReceivers.add(eventReceiver);
        }
    }

    public void registerMediaButtonEventReceiver(ComponentName eventReceiver)
    {
        if (eventReceiver == null) return;

        synchronized (sMediaButtonReceivers)
        {
            sMediaButtonReceivers.add(eventReceiver);
        }
    }

    public void registerRemoteControlClient(RemoteControlClient remoteControlClient)
    {
        if (remoteControlClient == null) return;

        synchronized (sRemoteControlClients)
        {
            sRemoteControlClients.add(remoteControlClient);
        }
    }

    public boolean registerRemoteController(RemoteController remoteController)
    {
        if (remoteController == null) return false;

        return false;
    }

    public void removeOnCommunicationDeviceChangedListener(OnCommunicationDeviceChangedListener listener)
    {
        removeExecutorListener(sCommunicationDeviceListeners, listener, "removeOnCommunicationDeviceChangedListener");
    }

    public void removeOnModeChangedListener(OnModeChangedListener listener)
    {
        removeExecutorListener(sModeListeners, listener, "removeOnModeChangedListener");
    }

    public void removeOnPreferredMixerAttributesChangedListener(OnPreferredMixerAttributesChangedListener listener)
    {
        removeExecutorListener(sMixerAttributesListeners, listener, "removeOnPreferredMixerAttributesChangedListener");
    }

    public int requestAudioFocus(AudioFocusRequest focusRequest)
    {
        if (focusRequest == null) throw new NullPointerException("Illegal null AudioFocusRequest");

        return grantFocus(focusKey(focusRequest), focusRequest);
    }

    public int requestAudioFocus(OnAudioFocusChangeListener listener, int streamType, int durationHint)
    {
        if (!AudioFocusRequest.isValidFocusGain(durationHint))
        {
            throw new IllegalArgumentException("Invalid duration hint, audio focus request denied");
        }

        AudioAttributes attributes = new AudioAttributes.Builder().setLegacyStreamType(streamType).build();
        AudioFocusRequest.Builder builder = new AudioFocusRequest.Builder(durationHint).setAudioAttributes(attributes);
        if (listener != null) builder.setOnAudioFocusChangeListener(listener);

        AudioFocusRequest request = builder.build();
        return grantFocus(listener != null ? listener : request, request);
    }

    public void setAllowedCapturePolicy(int capturePolicy)
    {
        if (capturePolicy < AudioAttributes.ALLOW_CAPTURE_BY_ALL || capturePolicy > AudioAttributes.ALLOW_CAPTURE_BY_NONE)
        {
            throw new IllegalArgumentException("Unknown allow playback capture policy");
        }

        sAllowedCapturePolicy = capturePolicy;
    }

    public void setBluetoothA2dpOn(boolean on)
    {
    }

    public void setBluetoothScoOn(boolean on)
    {
        sBluetoothScoOn = on;
    }

    public boolean setCommunicationDevice(AudioDeviceInfo device)
    {
        Objects.requireNonNull(device);

        if (!device.isSink() || !getAvailableCommunicationDevices().contains(device))
        {
            throw new IllegalArgumentException("invalid device type " + device.getType());
        }

        setCommunicationDeviceInternal(device);
        return true;
    }

    private static void setCommunicationDeviceInternal(AudioDeviceInfo device)
    {
        synchronized (sLock)
        {
            if (sCommunicationDevice == device) return;

            sCommunicationDevice = device;
        }

        for (ExecutorEntry<OnCommunicationDeviceChangedListener> entry : sCommunicationDeviceListeners)
        {
            entry.mExecutor.execute(() -> entry.mListener.onCommunicationDeviceChanged(device));
        }
    }

    public boolean setEncodedSurroundMode(int mode)
    {
        return false;
    }

    public void setMicrophoneMute(boolean on)
    {
        sMicrophoneMuted = on;
    }

    public void setMode(int mode)
    {
        if (mode < MODE_NORMAL || mode > MODE_COMMUNICATION_REDIRECT) return;

        synchronized (sLock)
        {
            if (sMode == mode) return;

            sMode = mode;
        }

        for (ExecutorEntry<OnModeChangedListener> entry : sModeListeners)
        {
            entry.mExecutor.execute(() -> entry.mListener.onModeChanged(mode));
        }
    }

    public void setParameters(String keyValuePairs)
    {
    }

    public boolean setPreferredMixerAttributes(AudioAttributes attributes, AudioDeviceInfo device,
        AudioMixerAttributes mixerAttributes)
    {
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(device);
        Objects.requireNonNull(mixerAttributes);

        return false;
    }

    public void setRingerMode(int ringerMode)
    {
        if (ringerMode < RINGER_MODE_SILENT || ringerMode > RINGER_MODE_NORMAL) return;

        synchronized (sLock)
        {
            sRingerMode = ringerMode;
        }
    }

    public void setRouting(int mode, int routes, int mask)
    {
    }

    public void setSpeakerphoneOn(boolean on)
    {
        sSpeakerphoneOn = on;
    }

    public void setStreamMute(int streamType, boolean state)
    {
        checkStreamType(streamType);

        synchronized (sLock)
        {
            sStreamMuted[streamType] = state;
        }
    }

    public void setStreamSolo(int streamType, boolean state)
    {
        checkStreamType(streamType);
    }

    public void setStreamVolume(int streamType, int index, int flags)
    {
        checkStreamType(streamType);

        synchronized (sLock)
        {
            int clamped = Math.max(MIN_STREAM_VOLUME[streamType], Math.min(MAX_STREAM_VOLUME[streamType], index));
            sStreamVolume[streamType] = clamped;
            sStreamMuted[streamType] = index == 0 && MIN_STREAM_VOLUME[streamType] == 0;
        }
    }

    public boolean setSurroundFormatEnabled(int audioFormat, boolean enabled)
    {
        return false;
    }

    public void setVibrateSetting(int vibrateType, int vibrateSetting)
    {
        if (vibrateType != VIBRATE_TYPE_RINGER && vibrateType != VIBRATE_TYPE_NOTIFICATION) return;
        if (vibrateSetting < VIBRATE_SETTING_OFF || vibrateSetting > VIBRATE_SETTING_ONLY_SILENT) return;

        synchronized (sLock)
        {
            sVibrateSetting[vibrateType] = vibrateSetting;
        }
    }

    public void setWiredHeadsetOn(boolean on)
    {
    }

    public boolean shouldVibrate(int vibrateType)
    {
        int ringerMode = getRingerMode();

        switch (getVibrateSetting(vibrateType))
        {
            case VIBRATE_SETTING_ON:
                return ringerMode != RINGER_MODE_SILENT;
            case VIBRATE_SETTING_ONLY_SILENT:
                return ringerMode == RINGER_MODE_VIBRATE;
            default:
                return false;
        }
    }

    public void startBluetoothSco()
    {
    }

    public void stopBluetoothSco()
    {
    }

    public void unloadSoundEffects()
    {
    }

    public void unregisterAudioDeviceCallback(AudioDeviceCallback callback)
    {
        for (CallbackEntry<AudioDeviceCallback> entry : sDeviceCallbacks)
        {
            if (entry.mCallback == callback) sDeviceCallbacks.remove(entry);
        }
    }

    public void unregisterAudioPlaybackCallback(AudioPlaybackCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("Illegal null AudioPlaybackCallback argument");

        for (CallbackEntry<AudioPlaybackCallback> entry : sPlaybackCallbacks)
        {
            if (entry.mCallback == callback) sPlaybackCallbacks.remove(entry);
        }
    }

    public void unregisterAudioRecordingCallback(AudioRecordingCallback callback)
    {
        if (callback == null) throw new IllegalArgumentException("Illegal null AudioRecordingCallback argument");

        for (CallbackEntry<AudioRecordingCallback> entry : sRecordingCallbacks)
        {
            if (entry.mCallback == callback) sRecordingCallbacks.remove(entry);
        }
    }

    public void unregisterMediaButtonEventReceiver(PendingIntent eventReceiver)
    {
        synchronized (sMediaButtonReceivers)
        {
            sMediaButtonReceivers.remove(eventReceiver);
        }
    }

    public void unregisterMediaButtonEventReceiver(ComponentName eventReceiver)
    {
        synchronized (sMediaButtonReceivers)
        {
            sMediaButtonReceivers.remove(eventReceiver);
        }
    }

    public void unregisterRemoteControlClient(RemoteControlClient remoteControlClient)
    {
        synchronized (sRemoteControlClients)
        {
            sRemoteControlClients.remove(remoteControlClient);
        }
    }

    public void unregisterRemoteController(RemoteController remoteController)
    {
    }
}
