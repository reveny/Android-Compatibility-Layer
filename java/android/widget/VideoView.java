package android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.InputStream;
import java.util.Map;

public class VideoView extends SurfaceView implements MediaController.MediaPlayerControl
{
    private static final String TAG = "VideoView";

    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;

    private static final int AUDIOFOCUS_NONE = 0;
    private static final int AUDIOFOCUS_GAIN = 1;
    private static final int AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4;

    private Uri mUri;
    private Map<String, String> mHeaders;
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mMediaPlayer;
    private int mAudioSession;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private MediaController mMediaController;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;
    private MediaPlayer.OnInfoListener mOnInfoListener;
    private int mCurrentBufferPercentage;
    private int mSeekWhenPrepared;
    private boolean mCanPause;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;
    private int mAudioFocusType = AUDIOFOCUS_GAIN;
    private AudioAttributes mAudioAttributes;

    public VideoView(Context context)
    {
        this(context, null);
    }

    public VideoView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0);
    }

    public VideoView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public VideoView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mAudioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                                                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                                        .build();

        getHolder().addCallback(mSurfaceCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);

        if (mVideoWidth > 0 && mVideoHeight > 0)
        {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY)
            {
                width = widthSize;
                height = heightSize;

                if (mVideoWidth * height < width * mVideoHeight) width = height * mVideoWidth / mVideoHeight;
                else if (mVideoWidth * height > width * mVideoHeight) height = width * mVideoHeight / mVideoWidth;
            }
            else if (widthMode == MeasureSpec.EXACTLY)
            {
                width = widthSize;
                height = width * mVideoHeight / mVideoWidth;
                if (heightMode == MeasureSpec.AT_MOST && height > heightSize) height = heightSize;
            }
            else if (heightMode == MeasureSpec.EXACTLY)
            {
                height = heightSize;
                width = height * mVideoWidth / mVideoHeight;
                if (widthMode == MeasureSpec.AT_MOST && width > widthSize) width = widthSize;
            }
            else
            {
                width = mVideoWidth;
                height = mVideoHeight;

                if (heightMode == MeasureSpec.AT_MOST && height > heightSize)
                {
                    height = heightSize;
                    width = height * mVideoWidth / mVideoHeight;
                }

                if (widthMode == MeasureSpec.AT_MOST && width > widthSize)
                {
                    width = widthSize;
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void draw(Canvas canvas)
    {
        super.draw(canvas);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return VideoView.class.getName();
    }

    public int resolveAdjustedSize(int desiredSize, int measureSpec)
    {
        return getDefaultSize(desiredSize, measureSpec);
    }

    public void setVideoPath(String path)
    {
        setVideoURI(Uri.parse(path));
    }

    public void setVideoURI(Uri uri)
    {
        setVideoURI(uri, null);
    }

    public void setVideoURI(Uri uri, Map<String, String> headers)
    {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void setAudioFocusRequest(int focusGain)
    {
        if (focusGain < AUDIOFOCUS_NONE || focusGain > AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        {
            throw new IllegalArgumentException("Illegal audio focus type " + focusGain);
        }

        mAudioFocusType = focusGain;
    }

    public void setAudioAttributes(AudioAttributes attributes)
    {
        if (attributes == null) throw new IllegalArgumentException("Illegal null AudioAttributes");

        mAudioAttributes = attributes;
    }

    public void addSubtitleSource(InputStream is, MediaFormat format)
    {
        android.util.Log.w(TAG, "subtitle rendering is not available here, so the subtitle source is ignored");
    }

    public void stopPlayback()
    {
        if (mMediaPlayer == null) return;

        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
        abandonAudioFocus();
    }

    private void openVideo()
    {
        if (mUri == null || mSurfaceHolder == null) return;

        release(false);

        if (mAudioFocusType != AUDIOFOCUS_NONE)
        {
            AudioManager manager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
            if (manager != null) manager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, mAudioFocusType);
        }

        try
        {
            mMediaPlayer = new MediaPlayer();
            if (mAudioSession != 0) mMediaPlayer.setAudioSessionId(mAudioSession);
            else mAudioSession = mMediaPlayer.getAudioSessionId();

            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(getContext(), mUri, mHeaders);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioAttributes(mAudioAttributes);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();

            mCurrentState = STATE_PREPARING;
            attachMediaController();
        }
        catch (java.io.IOException | IllegalArgumentException | IllegalStateException | SecurityException failure)
        {
            android.util.Log.w(TAG, "unable to open content: " + mUri, failure);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    public void setMediaController(MediaController controller)
    {
        if (mMediaController != null) mMediaController.hide();

        mMediaController = controller;
        attachMediaController();
    }

    private void attachMediaController()
    {
        if (mMediaPlayer == null || mMediaController == null) return;

        mMediaController.setMediaPlayer(this);
        View anchor = getParent() instanceof View ? (View)getParent() : this;
        mMediaController.setAnchorView(anchor);
        mMediaController.setEnabled(isInPlaybackState());
    }

    private final MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = (player, width, height) ->
    {
        mVideoWidth = player.getVideoWidth();
        mVideoHeight = player.getVideoHeight();

        if (mVideoWidth != 0 && mVideoHeight != 0)
        {
            getHolder().setFixedSize(mVideoWidth, mVideoHeight);
            requestLayout();
        }
    };

    private final MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener()
    {
        @Override
        public void onPrepared(MediaPlayer player)
        {
            mCurrentState = STATE_PREPARED;
            mCanPause = true;
            mCanSeekBack = true;
            mCanSeekForward = true;

            if (mOnPreparedListener != null) mOnPreparedListener.onPrepared(mMediaPlayer);
            if (mMediaController != null) mMediaController.setEnabled(true);

            mVideoWidth = player.getVideoWidth();
            mVideoHeight = player.getVideoHeight();

            int seekToPosition = mSeekWhenPrepared;
            if (seekToPosition != 0) seekTo(seekToPosition);

            if (mVideoWidth != 0 && mVideoHeight != 0)
            {
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);

                if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight)
                {
                    if (mTargetState == STATE_PLAYING)
                    {
                        start();
                        if (mMediaController != null) mMediaController.show();
                    }
                    else if (!isPlaying() && (seekToPosition != 0 || getCurrentPosition() > 0))
                    {
                        if (mMediaController != null) mMediaController.show(0);
                    }
                }
            }
            else if (mTargetState == STATE_PLAYING)
            {
                start();
            }
        }
    };

    private final MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener()
    {
        @Override
        public void onCompletion(MediaPlayer player)
        {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;

            if (mMediaController != null) mMediaController.hide();
            if (mOnCompletionListener != null) mOnCompletionListener.onCompletion(mMediaPlayer);
            if (mAudioFocusType != AUDIOFOCUS_NONE) abandonAudioFocus();
        }
    };

    private final MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener()
    {
        @Override
        public boolean onInfo(MediaPlayer player, int what, int extra)
        {
            if (mOnInfoListener != null) mOnInfoListener.onInfo(player, what, extra);
            return true;
        }
    };

    private final MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener()
    {
        @Override
        public boolean onError(MediaPlayer player, int frameworkError, int implementationError)
        {
            android.util.Log.d(TAG, "Error: " + frameworkError + "," + implementationError);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

            if (mMediaController != null) mMediaController.hide();
            if (mOnErrorListener != null && mOnErrorListener.onError(mMediaPlayer, frameworkError, implementationError)) return true;

            if (getWindowToken() != null)
            {
                String message = frameworkError == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
                                 ? "This video isn't valid for streaming to this device."
                                 : "Can't play this video.";

                new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Can't play video")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) ->
                    {
                        if (mOnCompletionListener != null) mOnCompletionListener.onCompletion(mMediaPlayer);
                    })
                    .setCancelable(false)
                    .show();
            }

            return true;
        }
    };

    private final MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
        (player, percent) -> mCurrentBufferPercentage = percent;

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener listener)
    {
        mOnPreparedListener = listener;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener)
    {
        mOnCompletionListener = listener;
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener listener)
    {
        mOnErrorListener = listener;
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener listener)
    {
        mOnInfoListener = listener;
    }

    private final SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback()
    {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
        {
            mSurfaceWidth = width;
            mSurfaceHeight = height;

            boolean validState = mTargetState == STATE_PLAYING;
            boolean hasValidSize = mVideoWidth == width && mVideoHeight == height;

            if (mMediaPlayer != null && validState && hasValidSize)
            {
                if (mSeekWhenPrepared != 0) seekTo(mSeekWhenPrepared);
                start();
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder)
        {
            mSurfaceHolder = holder;
            openVideo();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder)
        {
            mSurfaceHolder = null;
            if (mMediaController != null) mMediaController.hide();
            release(true);
        }
    };

    private void release(boolean clearTargetState)
    {
        if (mMediaPlayer == null) return;

        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
        mCurrentState = STATE_IDLE;
        if (clearTargetState) mTargetState = STATE_IDLE;
        if (mAudioFocusType != AUDIOFOCUS_NONE) abandonAudioFocus();
    }

    private void abandonAudioFocus()
    {
        AudioManager manager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
        if (manager != null) manager.abandonAudioFocus(null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN && isInPlaybackState() && mMediaController != null) toggleMediaControlsVisiblity();

        return super.onTouchEvent(event);
    }

    public boolean onTrackballEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN && isInPlaybackState() && mMediaController != null) toggleMediaControlsVisiblity();

        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_VOLUME_UP
                                     && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_MUTE
                                     && keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_CALL
                                     && keyCode != KeyEvent.KEYCODE_ENDCALL;

        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null)
        {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            {
                if (mMediaPlayer.isPlaying())
                {
                    pause();
                    mMediaController.show();
                }
                else
                {
                    start();
                    mMediaController.hide();
                }

                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY)
            {
                if (!mMediaPlayer.isPlaying())
                {
                    start();
                    mMediaController.hide();
                }

                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)
            {
                if (mMediaPlayer.isPlaying())
                {
                    pause();
                    mMediaController.show();
                }

                return true;
            }

            toggleMediaControlsVisiblity();
        }

        return super.onKeyDown(keyCode, event);
    }

    private void toggleMediaControlsVisiblity()
    {
        if (mMediaController.isShowing()) mMediaController.hide();
        else mMediaController.show();
    }

    @Override
    public void start()
    {
        if (isInPlaybackState())
        {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }

        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause()
    {
        if (isInPlaybackState() && mMediaPlayer.isPlaying())
        {
            mMediaPlayer.pause();
            mCurrentState = STATE_PAUSED;
        }

        mTargetState = STATE_PAUSED;
    }

    public void suspend()
    {
        release(false);
    }

    public void resume()
    {
        openVideo();
    }

    @Override
    public int getDuration()
    {
        return isInPlaybackState() ? mMediaPlayer.getDuration() : -1;
    }

    @Override
    public int getCurrentPosition()
    {
        return isInPlaybackState() ? mMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public void seekTo(int milliseconds)
    {
        if (isInPlaybackState())
        {
            mMediaPlayer.seekTo(milliseconds);
            mSeekWhenPrepared = 0;
        }
        else
        {
            mSeekWhenPrepared = milliseconds;
        }
    }

    @Override
    public boolean isPlaying()
    {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage()
    {
        return mMediaPlayer != null ? mCurrentBufferPercentage : 0;
    }

    private boolean isInPlaybackState()
    {
        return mMediaPlayer != null && mCurrentState != STATE_ERROR && mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING;
    }

    @Override
    public boolean canPause()
    {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward()
    {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward()
    {
        return mCanSeekForward;
    }

    @Override
    public int getAudioSessionId()
    {
        if (mAudioSession == 0)
        {
            MediaPlayer player = new MediaPlayer();
            mAudioSession = player.getAudioSessionId();
            player.release();
        }

        return mAudioSession;
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
    }
}
