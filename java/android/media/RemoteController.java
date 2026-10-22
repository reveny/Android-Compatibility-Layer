package android.media;

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;

import java.util.List;

public final class RemoteController
{
    public static final int POSITION_SYNCHRONIZATION_CHECK = 1;
    public static final int POSITION_SYNCHRONIZATION_NONE = 0;

    private final Context mContext;
    private final OnClientUpdateListener mOnClientUpdateListener;
    private final Handler mHandler;
    private final MediaSessionManager mSessionManager;

    private int mArtworkWidth = -1;
    private int mArtworkHeight = -1;
    private int mSynchronizationMode = POSITION_SYNCHRONIZATION_NONE;

    public RemoteController(Context context, OnClientUpdateListener updateListener) throws IllegalArgumentException
    {
        this(context, updateListener, null);
    }

    public RemoteController(Context context, OnClientUpdateListener updateListener, Looper looper) throws IllegalArgumentException
    {
        if (context == null) throw new IllegalArgumentException("Invalid null Context");
        if (updateListener == null) throw new IllegalArgumentException("Invalid null OnClientUpdateListener");

        if (looper == null)
        {
            looper = Looper.myLooper();
            if (looper == null) throw new IllegalArgumentException("Calling thread not associated with a looper");
        }

        mContext = context;
        mOnClientUpdateListener = updateListener;
        mHandler = new Handler(looper);
        mSessionManager = new MediaSessionManager(context);
    }

    private MediaController currentController()
    {
        List<MediaController> controllers = mSessionManager.getActiveSessions(null);
        return controllers.isEmpty() ? null : controllers.get(0);
    }

    public long getEstimatedMediaPosition()
    {
        MediaController controller = currentController();
        if (controller == null) return -1;

        PlaybackState state = controller.getPlaybackState();
        if (state == null) return -1;

        long position = state.getPosition();
        if (position < 0 || state.getState() != PlaybackState.STATE_PLAYING) return position;

        long elapsed = SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime();
        return position + (long)(elapsed * state.getPlaybackSpeed());
    }

    public boolean sendMediaKeyEvent(KeyEvent keyEvent) throws IllegalArgumentException
    {
        if (keyEvent == null || !KeyEvent.isMediaSessionKey(keyEvent.getKeyCode()))
        {
            throw new IllegalArgumentException("not a media key event");
        }

        MediaController controller = currentController();
        return controller != null && controller.dispatchMediaButtonEvent(keyEvent);
    }

    public boolean seekTo(long ms) throws IllegalArgumentException
    {
        if (ms < 0) throw new IllegalArgumentException("illegal negative time value");

        MediaController controller = currentController();
        if (controller == null) return false;

        controller.getTransportControls().seekTo(ms);
        return true;
    }

    public boolean setArtworkConfiguration(int width, int height) throws IllegalArgumentException
    {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid dimensions");

        mArtworkWidth = width;
        mArtworkHeight = height;
        return true;
    }

    public boolean clearArtworkConfiguration()
    {
        mArtworkWidth = -1;
        mArtworkHeight = -1;
        return true;
    }

    public boolean setSynchronizationMode(int sync) throws IllegalArgumentException
    {
        if (sync != POSITION_SYNCHRONIZATION_NONE && sync != POSITION_SYNCHRONIZATION_CHECK)
        {
            throw new IllegalArgumentException("Unknown synchronization mode " + sync);
        }

        mSynchronizationMode = sync;
        return true;
    }

    public MetadataEditor editMetadata()
    {
        MetadataEditor editor = new MetadataEditor();
        editor.mEditableKeys.add(MediaMetadataEditor.RATING_KEY_BY_USER);
        return editor;
    }

    Context getContext()
    {
        return mContext;
    }

    OnClientUpdateListener getUpdateListener()
    {
        return mOnClientUpdateListener;
    }

    Handler getHandler()
    {
        return mHandler;
    }

    public interface OnClientUpdateListener
    {
        void onClientChange(boolean clearing);

        void onClientPlaybackStateUpdate(int state);

        void onClientPlaybackStateUpdate(int state, long stateChangeTimeMs, long currentPosMs, float speed);

        void onClientTransportControlUpdate(int transportControlFlags);

        void onClientMetadataUpdate(MetadataEditor metadataEditor);
    }

    public class MetadataEditor extends MediaMetadataEditor
    {
        MetadataEditor()
        {
        }

        @Override
        public synchronized void apply()
        {
            if (!mMetadataChanged) return;

            mMetadataChanged = false;

            Object rating = mEditorMetadata.getParcelable(String.valueOf(MediaMetadataEditor.RATING_KEY_BY_USER));
            if (!(rating instanceof Rating)) return;

            MediaController controller = currentController();
            if (controller != null) controller.getTransportControls().setRating((Rating)rating);
        }
    }
}
