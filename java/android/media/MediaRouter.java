package android.media;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;

public class MediaRouter
{
    public static final int CALLBACK_FLAG_PERFORM_ACTIVE_SCAN = 1;
    public static final int CALLBACK_FLAG_UNFILTERED_EVENTS = 2;
    public static final int ROUTE_TYPE_LIVE_AUDIO = 1;
    public static final int ROUTE_TYPE_LIVE_VIDEO = 2;
    public static final int ROUTE_TYPE_USER = 0x800000;

    private static final int DEFAULT_ROUTE_TYPES = ROUTE_TYPE_LIVE_AUDIO | ROUTE_TYPE_LIVE_VIDEO;

    private final Context mContext;
    private final Object mLock = new Object();
    private final List<RouteInfo> mRoutes = new ArrayList<>();
    private final List<RouteCategory> mCategories = new ArrayList<>();
    private final List<CallbackRecord> mCallbacks = new ArrayList<>();
    private final RouteCategory mSystemCategory;
    private final RouteInfo mDefaultAudioVideo;

    private RouteInfo mSelectedRoute;

    public MediaRouter(Context context)
    {
        mContext = context;

        mSystemCategory = new RouteCategory("System", DEFAULT_ROUTE_TYPES, false);
        mSystemCategory.mIsSystem = true;
        mSystemCategory.mRouter = this;

        mDefaultAudioVideo = new RouteInfo(mSystemCategory);
        mDefaultAudioVideo.mRouter = this;
        mDefaultAudioVideo.mName = "Device speaker";
        mDefaultAudioVideo.mSupportedTypes = DEFAULT_ROUTE_TYPES;
        mDefaultAudioVideo.mDeviceType = RouteInfo.DEVICE_TYPE_SPEAKER;
        mDefaultAudioVideo.mPlaybackType = RouteInfo.PLAYBACK_TYPE_LOCAL;
        mDefaultAudioVideo.mVolumeHandling = RouteInfo.PLAYBACK_VOLUME_VARIABLE;
        mDefaultAudioVideo.mPlaybackStream = AudioManager.STREAM_MUSIC;
        mDefaultAudioVideo.mIsSystemDefault = true;

        mCategories.add(mSystemCategory);
        mRoutes.add(mDefaultAudioVideo);
        mSelectedRoute = mDefaultAudioVideo;
    }

    Context getContext()
    {
        return mContext;
    }

    AudioManager getAudioManager()
    {
        if (mContext == null) return null;

        Object service = mContext.getSystemService(Context.AUDIO_SERVICE);
        return service instanceof AudioManager ? (AudioManager)service : null;
    }

    public RouteInfo getDefaultRoute()
    {
        return mDefaultAudioVideo;
    }

    public RouteInfo getSelectedRoute(int type)
    {
        synchronized (mLock)
        {
            if (mSelectedRoute != null && (mSelectedRoute.mSupportedTypes & type) != 0) return mSelectedRoute;
        }

        if (type == ROUTE_TYPE_USER) return null;

        return mDefaultAudioVideo;
    }

    public void addCallback(int types, Callback callback)
    {
        addCallback(types, callback, 0);
    }

    public void addCallback(int types, Callback callback, int flags)
    {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");

        synchronized (mLock)
        {
            for (CallbackRecord record : mCallbacks)
            {
                if (record.mCallback == callback)
                {
                    record.mTypes |= types;
                    record.mFlags |= flags;
                    return;
                }
            }

            mCallbacks.add(new CallbackRecord(callback, types, flags));
        }
    }

    public void removeCallback(Callback callback)
    {
        synchronized (mLock)
        {
            mCallbacks.removeIf(record -> record.mCallback == callback);
        }
    }

    public void selectRoute(int types, RouteInfo route)
    {
        if (route == null) throw new IllegalArgumentException("Route cannot be null.");

        RouteInfo oldRoute;

        synchronized (mLock)
        {
            oldRoute = mSelectedRoute;
            if (oldRoute == route) return;
            mSelectedRoute = route;
        }

        int oldTypes = oldRoute == null ? 0 : oldRoute.mSupportedTypes & types;

        if (oldRoute != null)
        {
            for (CallbackRecord record : callbacks())
            {
                if (record.filterRouteEvent(oldRoute)) record.mCallback.onRouteUnselected(this, oldTypes, oldRoute);
            }
        }

        for (CallbackRecord record : callbacks())
        {
            if (record.filterRouteEvent(route)) record.mCallback.onRouteSelected(this, types & route.mSupportedTypes, route);
        }
    }

    public void addUserRoute(UserRouteInfo info)
    {
        addRouteInternal(info);
    }

    public void clearUserRoutes()
    {
        List<RouteInfo> userRoutes = new ArrayList<>();

        synchronized (mLock)
        {
            for (RouteInfo route : mRoutes)
            {
                if (route instanceof UserRouteInfo || route instanceof RouteGroup) userRoutes.add(route);
            }
        }

        for (RouteInfo route : userRoutes) removeRouteInternal(route);
    }

    public void removeUserRoute(UserRouteInfo info)
    {
        removeRouteInternal(info);
    }

    public RouteCategory createRouteCategory(CharSequence name, boolean isGroupable)
    {
        RouteCategory category = new RouteCategory(name, ROUTE_TYPE_USER, isGroupable);
        category.mRouter = this;
        return category;
    }

    public RouteCategory createRouteCategory(int nameResId, boolean isGroupable)
    {
        RouteCategory category = new RouteCategory(nameResId, ROUTE_TYPE_USER, isGroupable);
        category.mRouter = this;
        return category;
    }

    public UserRouteInfo createUserRoute(RouteCategory category)
    {
        UserRouteInfo route = new UserRouteInfo(category);
        route.mRouter = this;
        return route;
    }

    public int getCategoryCount()
    {
        synchronized (mLock)
        {
            return mCategories.size();
        }
    }

    public RouteCategory getCategoryAt(int index)
    {
        synchronized (mLock)
        {
            return mCategories.get(index);
        }
    }

    public int getRouteCount()
    {
        synchronized (mLock)
        {
            return mRoutes.size();
        }
    }

    public RouteInfo getRouteAt(int index)
    {
        synchronized (mLock)
        {
            return mRoutes.get(index);
        }
    }

    void addRouteInternal(RouteInfo info)
    {
        if (info == null) throw new IllegalArgumentException("Route cannot be null.");

        RouteCategory category = info.getCategory();
        info.mRouter = this;

        if (category != null && category.isGroupable() && !(info instanceof RouteGroup))
        {
            RouteGroup group = new RouteGroup(category);
            group.mRouter = this;
            group.mSupportedTypes = info.mSupportedTypes;

            synchronized (mLock)
            {
                if (!mCategories.contains(category)) mCategories.add(category);
                mRoutes.add(group);
            }

            dispatchRouteAdded(group);
            group.addRoute(info);
            return;
        }

        synchronized (mLock)
        {
            if (mRoutes.contains(info)) return;
            if (category != null && !mCategories.contains(category)) mCategories.add(category);
            mRoutes.add(info);
        }

        dispatchRouteAdded(info);
    }

    void removeRouteInternal(RouteInfo info)
    {
        RouteGroup group = info.getGroup();

        if (group != null)
        {
            group.removeRoute(info);
            return;
        }

        boolean removed;
        boolean wasSelected;

        synchronized (mLock)
        {
            removed = mRoutes.remove(info);
            wasSelected = mSelectedRoute == info;
            RouteCategory category = info.getCategory();
            boolean categoryInUse = false;

            for (RouteInfo route : mRoutes)
            {
                if (route.getCategory() == category) categoryInUse = true;
            }

            if (!categoryInUse && category != mSystemCategory) mCategories.remove(category);
        }

        if (!removed) return;

        if (wasSelected) selectRoute(DEFAULT_ROUTE_TYPES, mDefaultAudioVideo);

        for (CallbackRecord record : callbacks())
        {
            if (record.filterRouteEvent(info)) record.mCallback.onRouteRemoved(this, info);
        }
    }

    void dispatchRouteAdded(RouteInfo info)
    {
        for (CallbackRecord record : callbacks())
        {
            if (record.filterRouteEvent(info)) record.mCallback.onRouteAdded(this, info);
        }
    }

    void dispatchRouteChanged(RouteInfo info)
    {
        for (CallbackRecord record : callbacks())
        {
            if (record.filterRouteEvent(info)) record.mCallback.onRouteChanged(this, info);
        }
    }

    void dispatchRouteVolumeChanged(RouteInfo info)
    {
        for (CallbackRecord record : callbacks())
        {
            if (record.filterRouteEvent(info)) record.mCallback.onRouteVolumeChanged(this, info);
        }
    }

    void dispatchRouteGrouped(RouteInfo info, RouteGroup group, int index)
    {
        for (CallbackRecord record : callbacks())
        {
            if (record.filterRouteEvent(group)) record.mCallback.onRouteGrouped(this, info, group, index);
        }
    }

    void dispatchRouteUngrouped(RouteInfo info, RouteGroup group)
    {
        for (CallbackRecord record : callbacks())
        {
            if (record.filterRouteEvent(group)) record.mCallback.onRouteUngrouped(this, info, group);
        }
    }

    boolean containsRoute(RouteInfo info)
    {
        synchronized (mLock)
        {
            return mRoutes.contains(info);
        }
    }

    List<RouteInfo> routesSnapshot()
    {
        synchronized (mLock)
        {
            return new ArrayList<>(mRoutes);
        }
    }

    private List<CallbackRecord> callbacks()
    {
        synchronized (mLock)
        {
            return new ArrayList<>(mCallbacks);
        }
    }

    private static final class CallbackRecord
    {
        final Callback mCallback;
        int mTypes;
        int mFlags;

        CallbackRecord(Callback callback, int types, int flags)
        {
            mCallback = callback;
            mTypes = types;
            mFlags = flags;
        }

        boolean filterRouteEvent(RouteInfo route)
        {
            return (mFlags & CALLBACK_FLAG_UNFILTERED_EVENTS) != 0 || (mTypes & route.mSupportedTypes) != 0;
        }
    }

    public static class RouteInfo
    {
        public static final int DEVICE_TYPE_BLUETOOTH = 3;
        public static final int DEVICE_TYPE_SPEAKER = 2;
        public static final int DEVICE_TYPE_TV = 1;
        public static final int DEVICE_TYPE_UNKNOWN = 0;
        public static final int PLAYBACK_TYPE_LOCAL = 0;
        public static final int PLAYBACK_TYPE_REMOTE = 1;
        public static final int PLAYBACK_VOLUME_FIXED = 0;
        public static final int PLAYBACK_VOLUME_VARIABLE = 1;

        final RouteCategory mCategory;

        MediaRouter mRouter;
        CharSequence mName;
        int mNameResId;
        CharSequence mDescription;
        CharSequence mStatus;
        RouteGroup mGroup;
        int mSupportedTypes;
        int mDeviceType = DEVICE_TYPE_UNKNOWN;
        int mPlaybackType = PLAYBACK_TYPE_LOCAL;
        int mVolumeMax = 15;
        int mVolume = 15;
        int mVolumeHandling = PLAYBACK_VOLUME_VARIABLE;
        int mPlaybackStream = AudioManager.STREAM_MUSIC;
        Drawable mIcon;
        Object mTag;
        boolean mEnabled = true;
        boolean mConnecting;
        boolean mIsSystemDefault;
        VolumeCallback mVolumeCallback;

        RouteInfo(RouteCategory category)
        {
            mCategory = category;
        }

        public CharSequence getName()
        {
            return getName(mRouter == null ? null : mRouter.getContext());
        }

        public CharSequence getName(Context context)
        {
            if (mNameResId != 0)
            {
                Resources resources = context != null ? context.getResources() : Resources.getSystem();

                if (resources != null)
                {
                    try
                    {
                        return resources.getText(mNameResId);
                    }
                    catch (Resources.NotFoundException exception)
                    {
                        return mName;
                    }
                }
            }

            return mName;
        }

        public CharSequence getDescription()
        {
            return mDescription;
        }

        public CharSequence getStatus()
        {
            return mStatus;
        }

        public int getSupportedTypes()
        {
            return mSupportedTypes;
        }

        public int getDeviceType()
        {
            return mDeviceType;
        }

        public RouteCategory getCategory()
        {
            return mCategory;
        }

        public RouteGroup getGroup()
        {
            return mGroup;
        }

        public Drawable getIconDrawable()
        {
            return mIcon;
        }

        public void setTag(Object tag)
        {
            mTag = tag;
            routeUpdated();
        }

        public Object getTag()
        {
            return mTag;
        }

        public int getPlaybackType()
        {
            return mPlaybackType;
        }

        public int getPlaybackStream()
        {
            return mPlaybackStream;
        }

        public int getVolume()
        {
            if (mIsSystemDefault && mRouter != null)
            {
                AudioManager audioManager = mRouter.getAudioManager();
                if (audioManager != null) return audioManager.getStreamVolume(mPlaybackStream);
            }

            return mVolume;
        }

        public void requestSetVolume(int volume)
        {
            if (mIsSystemDefault && mRouter != null)
            {
                AudioManager audioManager = mRouter.getAudioManager();

                if (audioManager != null)
                {
                    audioManager.setStreamVolume(mPlaybackStream, volume, 0);
                    mRouter.dispatchRouteVolumeChanged(this);
                }
            }
        }

        public void requestUpdateVolume(int direction)
        {
            if (mIsSystemDefault)
            {
                int volume = Math.max(0, Math.min(getVolume() + direction, getVolumeMax()));
                requestSetVolume(volume);
            }
        }

        public int getVolumeMax()
        {
            if (mIsSystemDefault && mRouter != null)
            {
                AudioManager audioManager = mRouter.getAudioManager();
                if (audioManager != null) return audioManager.getStreamMaxVolume(mPlaybackStream);
            }

            return mVolumeMax;
        }

        public int getVolumeHandling()
        {
            return mVolumeHandling;
        }

        public Display getPresentationDisplay()
        {
            return null;
        }

        public boolean isEnabled()
        {
            return mEnabled;
        }

        public boolean isConnecting()
        {
            return mConnecting;
        }

        void routeUpdated()
        {
            if (mRouter != null && mRouter.containsRoute(this)) mRouter.dispatchRouteChanged(this);
            if (mGroup != null) mGroup.memberUpdated();
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + "{ name=" + getName() + ", description=" + getDescription() + ", status="
                    + getStatus() + ", category=" + getCategory() + ", supportedTypes=" + mSupportedTypes
                    + ", presentationDisplay=null }";
        }
    }

    public static class UserRouteInfo extends RouteInfo
    {
        private RemoteControlClient mRemoteControlClient;

        UserRouteInfo(RouteCategory category)
        {
            super(category);
            mSupportedTypes = ROUTE_TYPE_USER;
            mPlaybackType = PLAYBACK_TYPE_REMOTE;
            mVolumeHandling = PLAYBACK_VOLUME_FIXED;
        }

        public void setName(CharSequence name)
        {
            mNameResId = 0;
            mName = name;
            routeUpdated();
        }

        public void setName(int resId)
        {
            mNameResId = resId;
            mName = null;
            routeUpdated();
        }

        public void setDescription(CharSequence description)
        {
            mDescription = description;
            routeUpdated();
        }

        public void setStatus(CharSequence status)
        {
            mStatus = status;
            routeUpdated();
        }

        public void setRemoteControlClient(RemoteControlClient remoteControlClient)
        {
            mRemoteControlClient = remoteControlClient;
        }

        public RemoteControlClient getRemoteControlClient()
        {
            return mRemoteControlClient;
        }

        public void setIconDrawable(Drawable icon)
        {
            mIcon = icon;
        }

        public void setIconResource(int resId)
        {
            Context context = mRouter == null ? null : mRouter.getContext();
            Resources resources = context != null ? context.getResources() : null;
            setIconDrawable(resources == null ? null : resources.getDrawable(resId));
        }

        public void setVolumeCallback(VolumeCallback volumeCallback)
        {
            mVolumeCallback = volumeCallback;
        }

        public void setPlaybackType(int type)
        {
            if (mPlaybackType != type) mPlaybackType = type;
        }

        public void setVolumeHandling(int volumeHandling)
        {
            if (mVolumeHandling != volumeHandling) mVolumeHandling = volumeHandling;
        }

        public void setVolume(int volume)
        {
            volume = Math.max(0, Math.min(volume, getVolumeMax()));

            if (mVolume != volume)
            {
                mVolume = volume;
                if (mRouter != null) mRouter.dispatchRouteVolumeChanged(this);
                if (mGroup != null) mGroup.memberVolumeChanged(this);
            }
        }

        @Override
        public void requestSetVolume(int volume)
        {
            if (mVolumeHandling == PLAYBACK_VOLUME_VARIABLE && mVolumeCallback != null)
            {
                mVolumeCallback.onVolumeSetRequest(this, volume);
            }
        }

        @Override
        public void requestUpdateVolume(int direction)
        {
            if (mVolumeHandling == PLAYBACK_VOLUME_VARIABLE && mVolumeCallback != null)
            {
                mVolumeCallback.onVolumeUpdateRequest(this, direction);
            }
        }

        public void setVolumeMax(int volumeMax)
        {
            if (mVolumeMax != volumeMax) mVolumeMax = volumeMax;
        }

        public void setPlaybackStream(int stream)
        {
            if (mPlaybackStream != stream) mPlaybackStream = stream;
        }
    }

    public static class RouteGroup extends RouteInfo
    {
        private final List<RouteInfo> mRoutes = new ArrayList<>();

        RouteGroup(RouteCategory category)
        {
            super(category);
            mGroup = this;
            mVolumeHandling = PLAYBACK_VOLUME_FIXED;
        }

        @Override
        public CharSequence getName(Context context)
        {
            if (mName != null || mNameResId != 0) return super.getName(context);

            StringBuilder builder = new StringBuilder();

            for (RouteInfo route : mRoutes)
            {
                if (builder.length() > 0) builder.append(", ");
                builder.append(route.getName(context));
            }

            return builder.toString();
        }

        public void addRoute(RouteInfo route)
        {
            addRoute(route, mRoutes.size());
        }

        public void addRoute(RouteInfo route, int insertAt)
        {
            if (route.getGroup() != null && route.getGroup() != this)
            {
                throw new IllegalStateException("Route " + route + " is already part of a group.");
            }

            if (route.getCategory() != mCategory)
            {
                throw new IllegalArgumentException("Route cannot be added to a group with a different category.");
            }

            mRoutes.add(insertAt, route);
            route.mGroup = this;
            route.mRouter = mRouter;
            mSupportedTypes |= route.mSupportedTypes;

            if (mRouter != null) mRouter.dispatchRouteGrouped(route, this, insertAt);

            routeUpdated();
        }

        public void removeRoute(RouteInfo route)
        {
            if (route.getGroup() != this)
            {
                throw new IllegalArgumentException("Route " + route + " is not a member of this group.");
            }

            mRoutes.remove(route);
            route.mGroup = null;

            if (mRouter != null) mRouter.dispatchRouteUngrouped(route, this);

            if (mRoutes.isEmpty() && mRouter != null)
            {
                mRouter.removeRouteInternal(this);
                return;
            }

            routeUpdated();
        }

        public void removeRoute(int index)
        {
            removeRoute(mRoutes.get(index));
        }

        public int getRouteCount()
        {
            return mRoutes.size();
        }

        public RouteInfo getRouteAt(int index)
        {
            return mRoutes.get(index);
        }

        public void setIconDrawable(Drawable icon)
        {
            mIcon = icon;
        }

        public void setIconResource(int resId)
        {
            Context context = mRouter == null ? null : mRouter.getContext();
            Resources resources = context != null ? context.getResources() : null;
            setIconDrawable(resources == null ? null : resources.getDrawable(resId));
        }

        @Override
        public void requestSetVolume(int volume)
        {
            int maxVolume = getVolumeMax();
            if (maxVolume == 0) return;

            float scaledVolume = (float)volume / maxVolume;

            for (RouteInfo route : mRoutes)
            {
                route.requestSetVolume((int)(scaledVolume * route.getVolumeMax()));
            }

            if (volume != mVolume)
            {
                mVolume = volume;
                if (mRouter != null) mRouter.dispatchRouteVolumeChanged(this);
            }
        }

        @Override
        public void requestUpdateVolume(int direction)
        {
            int maxVolume = getVolumeMax();
            if (maxVolume == 0) return;

            for (RouteInfo route : mRoutes) route.requestUpdateVolume(direction);

            int volume = Math.max(0, Math.min(mVolume + direction, maxVolume));

            if (volume != mVolume)
            {
                mVolume = volume;
                if (mRouter != null) mRouter.dispatchRouteVolumeChanged(this);
            }
        }

        void memberUpdated()
        {
            int maxVolume = 0;

            for (RouteInfo route : mRoutes)
            {
                if (route.getPlaybackType() == PLAYBACK_TYPE_REMOTE) mPlaybackType = PLAYBACK_TYPE_REMOTE;
                if (route.getVolumeHandling() == PLAYBACK_VOLUME_VARIABLE) mVolumeHandling = PLAYBACK_VOLUME_VARIABLE;
                maxVolume = Math.max(maxVolume, route.getVolumeMax());
            }

            mVolumeMax = maxVolume;

            if (mRouter != null && mRouter.containsRoute(this)) mRouter.dispatchRouteChanged(this);
        }

        void memberVolumeChanged(RouteInfo info)
        {
            int volume = 0;

            for (RouteInfo route : mRoutes) volume = Math.max(volume, route.getVolume());

            if (volume != mVolume)
            {
                mVolume = volume;
                if (mRouter != null) mRouter.dispatchRouteVolumeChanged(this);
            }
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(super.toString());
            builder.append('[');

            for (int index = 0; index < mRoutes.size(); index++)
            {
                if (index > 0) builder.append(", ");
                builder.append(mRoutes.get(index));
            }

            builder.append(']');
            return builder.toString();
        }
    }

    public static class RouteCategory
    {
        final int mTypes;
        final boolean mGroupable;

        MediaRouter mRouter;
        CharSequence mName;
        int mNameResId;
        boolean mIsSystem;

        RouteCategory(CharSequence name, int types, boolean groupable)
        {
            mName = name;
            mTypes = types;
            mGroupable = groupable;
        }

        RouteCategory(int nameResId, int types, boolean groupable)
        {
            mNameResId = nameResId;
            mTypes = types;
            mGroupable = groupable;
        }

        public CharSequence getName()
        {
            return getName(mRouter == null ? null : mRouter.getContext());
        }

        public CharSequence getName(Context context)
        {
            if (mNameResId != 0)
            {
                Resources resources = context != null ? context.getResources() : Resources.getSystem();

                if (resources != null)
                {
                    try
                    {
                        return resources.getText(mNameResId);
                    }
                    catch (Resources.NotFoundException exception)
                    {
                        return mName;
                    }
                }
            }

            return mName;
        }

        public List<RouteInfo> getRoutes(List<RouteInfo> out)
        {
            if (out == null) out = new ArrayList<>();
            else out.clear();

            if (mRouter == null) return out;

            for (RouteInfo route : mRouter.routesSnapshot())
            {
                if (route.mCategory == this) out.add(route);
            }

            return out;
        }

        public int getSupportedTypes()
        {
            return mTypes;
        }

        public boolean isGroupable()
        {
            return mGroupable;
        }

        boolean isSystem()
        {
            return mIsSystem;
        }

        @Override
        public String toString()
        {
            return "RouteCategory{ name=" + getName() + " types=" + mTypes + " groupable=" + mGroupable + " }";
        }
    }

    public abstract static class Callback
    {
        public Callback()
        {
        }

        public abstract void onRouteSelected(MediaRouter router, int type, RouteInfo info);

        public abstract void onRouteUnselected(MediaRouter router, int type, RouteInfo info);

        public abstract void onRouteAdded(MediaRouter router, RouteInfo info);

        public abstract void onRouteRemoved(MediaRouter router, RouteInfo info);

        public abstract void onRouteChanged(MediaRouter router, RouteInfo info);

        public abstract void onRouteGrouped(MediaRouter router, RouteInfo info, RouteGroup group, int index);

        public abstract void onRouteUngrouped(MediaRouter router, RouteInfo info, RouteGroup group);

        public abstract void onRouteVolumeChanged(MediaRouter router, RouteInfo info);

        public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo info)
        {
        }
    }

    public static class SimpleCallback extends Callback
    {
        public SimpleCallback()
        {
        }

        @Override
        public void onRouteSelected(MediaRouter router, int type, RouteInfo info)
        {
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, RouteInfo info)
        {
        }

        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo info)
        {
        }

        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo info)
        {
        }

        @Override
        public void onRouteChanged(MediaRouter router, RouteInfo info)
        {
        }

        @Override
        public void onRouteGrouped(MediaRouter router, RouteInfo info, RouteGroup group, int index)
        {
        }

        @Override
        public void onRouteUngrouped(MediaRouter router, RouteInfo info, RouteGroup group)
        {
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, RouteInfo info)
        {
        }
    }

    public abstract static class VolumeCallback
    {
        public VolumeCallback()
        {
        }

        public abstract void onVolumeUpdateRequest(RouteInfo info, int direction);

        public abstract void onVolumeSetRequest(RouteInfo info, int volume);
    }
}
