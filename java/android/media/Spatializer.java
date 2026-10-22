package android.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

public class Spatializer
{
    public static final int SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL = 1;
    public static final int SPATIALIZER_IMMERSIVE_LEVEL_NONE = 0;
    public static final int SPATIALIZER_IMMERSIVE_LEVEL_OTHER = -1;

    private final List<OnHeadTrackerAvailableListener> mHeadTrackerListeners = new CopyOnWriteArrayList<>();
    private final List<OnSpatializerStateChangedListener> mStateListeners = new CopyOnWriteArrayList<>();

    Spatializer()
    {
    }

    public void addOnHeadTrackerAvailableListener(Executor executor, OnHeadTrackerAvailableListener listener)
    {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(listener);

        if (!mHeadTrackerListeners.contains(listener)) mHeadTrackerListeners.add(listener);
    }

    public void addOnSpatializerStateChangedListener(Executor executor, OnSpatializerStateChangedListener listener)
    {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(listener);

        if (!mStateListeners.contains(listener)) mStateListeners.add(listener);
    }

    public boolean canBeSpatialized(AudioAttributes attributes, AudioFormat format)
    {
        Objects.requireNonNull(attributes);
        Objects.requireNonNull(format);

        return false;
    }

    public int getImmersiveAudioLevel()
    {
        return SPATIALIZER_IMMERSIVE_LEVEL_NONE;
    }

    public List<Integer> getSpatializedChannelMasks()
    {
        return new ArrayList<>();
    }

    public boolean isAvailable()
    {
        return false;
    }

    public boolean isEnabled()
    {
        return false;
    }

    public boolean isHeadTrackerAvailable()
    {
        return false;
    }

    public void removeOnHeadTrackerAvailableListener(OnHeadTrackerAvailableListener listener)
    {
        mHeadTrackerListeners.remove(listener);
    }

    public void removeOnSpatializerStateChangedListener(OnSpatializerStateChangedListener listener)
    {
        mStateListeners.remove(listener);
    }

    public interface OnSpatializerStateChangedListener
    {
        void onSpatializerAvailableChanged(Spatializer spatializer, boolean available);

        void onSpatializerEnabledChanged(Spatializer spatializer, boolean enabled);
    }

    public interface OnHeadTrackerAvailableListener
    {
        void onHeadTrackerAvailableChanged(Spatializer spatializer, boolean available);
    }
}
