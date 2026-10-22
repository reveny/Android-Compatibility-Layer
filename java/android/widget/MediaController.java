package android.widget;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Formatter;
import java.util.Locale;

public class MediaController extends FrameLayout
{
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final int PROGRESS_STEPS = 1000;

    private MediaPlayerControl mPlayer;
    private final Context mContext;
    private View mAnchor;
    private View mRoot;
    private PopupWindow mWindow;
    private ProgressBar mProgress;
    private TextView mEndTime;
    private TextView mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private final boolean mUseFastForward;
    private boolean mFromXml;
    private boolean mListenersSet;
    private View.OnClickListener mNextListener;
    private View.OnClickListener mPreviousListener;
    private final StringBuilder mFormatBuilder = new StringBuilder();
    private final Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    private ImageButton mPauseButton;
    private ImageButton mFastForwardButton;
    private ImageButton mRewindButton;
    private ImageButton mNextButton;
    private ImageButton mPreviousButton;
    private final int[] mAnchorLocation = new int[2];

    private final View.OnLayoutChangeListener mLayoutChangeListener =
        (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
        {
            if (mShowing) updateWindowLayout();
        };

    private final Runnable mFadeOut = this::hide;

    private final Runnable mShowProgress = new Runnable()
    {
        @Override
        public void run()
        {
            int position = setProgress();
            if (!mDragging && mShowing && mPlayer != null && mPlayer.isPlaying()) postDelayed(this, 1000 - (position % 1000));
        }
    };

    public MediaController(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        mContext = context;
        mRoot = this;
        mUseFastForward = true;
        mFromXml = true;
    }

    public MediaController(Context context, boolean useFastForward)
    {
        super(context);

        mContext = context;
        mUseFastForward = useFastForward;
        initFloatingWindow();
    }

    public MediaController(Context context)
    {
        this(context, true);
    }

    private void initFloatingWindow()
    {
        mWindow = new PopupWindow(mContext);
        mWindow.setFocusable(false);
        mWindow.setBackgroundDrawable(new ColorDrawable(0));
        mWindow.setOutsideTouchable(false);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();
    }

    @Override
    public void onFinishInflate()
    {
        super.onFinishInflate();

        if (mRoot != null) initControllerView(mRoot);
    }

    public void setMediaPlayer(MediaPlayerControl player)
    {
        mPlayer = player;
        updatePausePlay();
    }

    public void setAnchorView(View view)
    {
        if (mAnchor != null) mAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);

        mAnchor = view;
        if (mAnchor != null) mAnchor.addOnLayoutChangeListener(mLayoutChangeListener);

        removeAllViews();
        View controller = makeControllerView();
        addView(controller, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    protected View makeControllerView()
    {
        LinearLayout root = new LinearLayout(mContext);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xCC000000);

        LinearLayout buttons = new LinearLayout(mContext);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        mPreviousButton = button("ic_media_previous", "Previous");
        mRewindButton = button("ic_media_rew", "Rewind");
        mPauseButton = button("ic_media_pause", "Pause");
        mFastForwardButton = button("ic_media_ff", "Fast forward");
        mNextButton = button("ic_media_next", "Next");

        buttons.addView(mPreviousButton);
        buttons.addView(mRewindButton);
        buttons.addView(mPauseButton);
        buttons.addView(mFastForwardButton);
        buttons.addView(mNextButton);

        LinearLayout timeline = new LinearLayout(mContext);
        timeline.setOrientation(LinearLayout.HORIZONTAL);
        timeline.setGravity(Gravity.CENTER_VERTICAL);

        mCurrentTime = timeLabel();
        mEndTime = timeLabel();
        SeekBar seekBar = new SeekBar(mContext);
        mProgress = seekBar;

        timeline.addView(mCurrentTime);
        timeline.addView(seekBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        timeline.addView(mEndTime);

        root.addView(buttons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(timeline, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        mRoot = root;
        initControllerView(root);
        return root;
    }

    private ImageButton button(String drawableName, String description)
    {
        ImageButton button = new ImageButton(mContext);
        int identifier = mContext.getResources().getIdentifier(drawableName, "drawable", "android");
        Drawable icon = null;

        if (identifier != 0)
        {
            try
            {
                icon = mContext.getDrawable(identifier);
            }
            catch (RuntimeException missing)
            {
                icon = null;
            }
        }

        if (icon != null) button.setImageDrawable(icon);
        button.setContentDescription(description);
        button.setBackground(null);

        int size = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()));
        button.setMinimumWidth(size);
        button.setMinimumHeight(size);
        return button;
    }

    private TextView timeLabel()
    {
        TextView label = new TextView(mContext);
        label.setTextColor(0xFFBEBEBE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
        label.setPadding(padding, padding, padding, padding);
        return label;
    }

    private void initControllerView(View root)
    {
        if (mPauseButton != null)
        {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(view ->
            {
                doPauseResume();
                show(DEFAULT_TIMEOUT);
            });
        }

        if (mFastForwardButton != null)
        {
            mFastForwardButton.setOnClickListener(view -> seekBy(15000));
            if (!mFromXml) mFastForwardButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
        }

        if (mRewindButton != null)
        {
            mRewindButton.setOnClickListener(view -> seekBy(-5000));
            if (!mFromXml) mRewindButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
        }

        if (mNextButton != null && !mFromXml && !mListenersSet) mNextButton.setVisibility(View.GONE);
        if (mPreviousButton != null && !mFromXml && !mListenersSet) mPreviousButton.setVisibility(View.GONE);

        if (mProgress instanceof SeekBar)
        {
            ((SeekBar)mProgress).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onStartTrackingTouch(SeekBar bar)
                {
                    show(3600000);
                    mDragging = true;
                    removeCallbacks(mShowProgress);
                }

                @Override
                public void onProgressChanged(SeekBar bar, int progress, boolean fromUser)
                {
                    if (!fromUser || mPlayer == null) return;

                    long duration = mPlayer.getDuration();
                    long position = duration * progress / PROGRESS_STEPS;
                    mPlayer.seekTo((int)position);
                    if (mCurrentTime != null) mCurrentTime.setText(stringForTime((int)position));
                }

                @Override
                public void onStopTrackingTouch(SeekBar bar)
                {
                    mDragging = false;
                    setProgress();
                    updatePausePlay();
                    show(DEFAULT_TIMEOUT);
                    post(mShowProgress);
                }
            });
        }

        if (mProgress != null) mProgress.setMax(PROGRESS_STEPS);

        installPrevNextListeners();
    }

    private void seekBy(int delta)
    {
        if (mPlayer == null) return;

        mPlayer.seekTo(mPlayer.getCurrentPosition() + delta);
        setProgress();
        show(DEFAULT_TIMEOUT);
    }

    private void installPrevNextListeners()
    {
        if (mNextButton != null)
        {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mNextListener != null);
        }

        if (mPreviousButton != null)
        {
            mPreviousButton.setOnClickListener(mPreviousListener);
            mPreviousButton.setEnabled(mPreviousListener != null);
        }
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener previous)
    {
        mNextListener = next;
        mPreviousListener = previous;
        mListenersSet = true;

        if (mRoot == null) return;

        installPrevNextListeners();

        if (mNextButton != null && !mFromXml) mNextButton.setVisibility(View.VISIBLE);
        if (mPreviousButton != null && !mFromXml) mPreviousButton.setVisibility(View.VISIBLE);
    }

    private void disableUnsupportedButtons()
    {
        if (mPlayer == null) return;

        if (mPauseButton != null && !mPlayer.canPause()) mPauseButton.setEnabled(false);
        if (mRewindButton != null && !mPlayer.canSeekBackward()) mRewindButton.setEnabled(false);
        if (mFastForwardButton != null && !mPlayer.canSeekForward()) mFastForwardButton.setEnabled(false);
        if (mProgress != null && !mPlayer.canSeekBackward() && !mPlayer.canSeekForward()) mProgress.setEnabled(false);
    }

    public void show()
    {
        show(DEFAULT_TIMEOUT);
    }

    public void show(int timeout)
    {
        if (!mShowing && mAnchor != null)
        {
            setProgress();
            if (mPauseButton != null) mPauseButton.requestFocus();
            disableUnsupportedButtons();

            if (mWindow != null)
            {
                mWindow.setContentView(this);
                updateWindowLayout();
                mAnchor.getLocationInWindow(mAnchorLocation);
                mWindow.showAtLocation(mAnchor, Gravity.NO_GRAVITY, mAnchorLocation[0],
                                       mAnchorLocation[1] + mAnchor.getHeight() - controllerHeight());
            }
            else
            {
                setVisibility(View.VISIBLE);
            }

            mShowing = true;
        }

        updatePausePlay();
        post(mShowProgress);

        removeCallbacks(mFadeOut);
        if (timeout != 0) postDelayed(mFadeOut, timeout);
    }

    private int controllerHeight()
    {
        int width = mAnchor == null ? getResources().getDisplayMetrics().widthPixels : mAnchor.getWidth();
        measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return getMeasuredHeight();
    }

    private void updateWindowLayout()
    {
        if (mWindow == null || mAnchor == null) return;

        int height = controllerHeight();
        mAnchor.getLocationInWindow(mAnchorLocation);
        int x = mAnchorLocation[0];
        int y = mAnchorLocation[1] + mAnchor.getHeight() - height;

        mWindow.setWidth(mAnchor.getWidth());
        mWindow.setHeight(height);
        if (mWindow.isShowing()) mWindow.update(x, y, mAnchor.getWidth(), height);
    }

    public boolean isShowing()
    {
        return mShowing;
    }

    public void hide()
    {
        if (mAnchor == null || !mShowing) return;

        removeCallbacks(mShowProgress);
        removeCallbacks(mFadeOut);

        if (mWindow != null) mWindow.dismiss();
        else setVisibility(View.GONE);

        mShowing = false;
    }

    private String stringForTime(int timeMs)
    {
        int totalSeconds = Math.max(0, timeMs) / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();

        return mFormatter.format("%02d:%02d", minutes, seconds).toString();
    }

    private int setProgress()
    {
        if (mPlayer == null || mDragging) return 0;

        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();

        if (mProgress != null)
        {
            if (duration > 0) mProgress.setProgress((int)(PROGRESS_STEPS * (long)position / duration));
            mProgress.setSecondaryProgress(mPlayer.getBufferPercentage() * 10);
        }

        if (mEndTime != null) mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null) mCurrentTime.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                show(0);
                break;
            case MotionEvent.ACTION_UP:
                show(DEFAULT_TIMEOUT);
                break;
            case MotionEvent.ACTION_CANCEL:
                hide();
                break;
            default:
                break;
        }

        return true;
    }

    public boolean onTrackballEvent(MotionEvent event)
    {
        show(DEFAULT_TIMEOUT);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        int keyCode = event.getKeyCode();
        boolean uniqueDown = event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;

        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE)
        {
            if (uniqueDown)
            {
                doPauseResume();
                show(DEFAULT_TIMEOUT);
                if (mPauseButton != null) mPauseButton.requestFocus();
            }

            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY)
        {
            if (uniqueDown && mPlayer != null && !mPlayer.isPlaying())
            {
                mPlayer.start();
                updatePausePlay();
                show(DEFAULT_TIMEOUT);
            }

            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)
        {
            if (uniqueDown && mPlayer != null && mPlayer.isPlaying())
            {
                mPlayer.pause();
                updatePausePlay();
                show(DEFAULT_TIMEOUT);
            }

            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
            || keyCode == KeyEvent.KEYCODE_CAMERA)
        {
            return super.dispatchKeyEvent(event);
        }

        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
        {
            if (uniqueDown) hide();
            return true;
        }

        show(DEFAULT_TIMEOUT);
        return super.dispatchKeyEvent(event);
    }

    private void updatePausePlay()
    {
        if (mRoot == null || mPauseButton == null) return;

        boolean playing = mPlayer != null && mPlayer.isPlaying();
        String name = playing ? "ic_media_pause" : "ic_media_play";
        int identifier = mContext.getResources().getIdentifier(name, "drawable", "android");

        if (identifier != 0)
        {
            try
            {
                mPauseButton.setImageDrawable(mContext.getDrawable(identifier));
            }
            catch (RuntimeException missing)
            {
                mPauseButton.setImageDrawable(null);
            }
        }

        mPauseButton.setContentDescription(playing ? "Pause" : "Play");
    }

    private void doPauseResume()
    {
        if (mPlayer == null) return;

        if (mPlayer.isPlaying()) mPlayer.pause();
        else mPlayer.start();

        updatePausePlay();
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        if (mPauseButton != null) mPauseButton.setEnabled(enabled);
        if (mFastForwardButton != null) mFastForwardButton.setEnabled(enabled);
        if (mRewindButton != null) mRewindButton.setEnabled(enabled);
        if (mNextButton != null) mNextButton.setEnabled(enabled && mNextListener != null);
        if (mPreviousButton != null) mPreviousButton.setEnabled(enabled && mPreviousListener != null);
        if (mProgress != null) mProgress.setEnabled(enabled);

        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return MediaController.class.getName();
    }

    public interface MediaPlayerControl
    {
        void start();

        void pause();

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos);

        boolean isPlaying();

        int getBufferPercentage();

        boolean canPause();

        boolean canSeekBackward();

        boolean canSeekForward();

        int getAudioSessionId();
    }
}
