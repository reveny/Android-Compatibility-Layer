package android.inputmethodservice;

import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Duration;

public class InputMethodService extends AbstractInputMethodService
{
    public static final int BACK_DISPOSITION_DEFAULT = 0;
    public static final int BACK_DISPOSITION_WILL_NOT_DISMISS = 1;
    public static final int BACK_DISPOSITION_WILL_DISMISS = 2;
    public static final int BACK_DISPOSITION_ADJUST_NOTHING = 3;

    private static final String TAG = "InputMethodService";
    private static final Duration STYLUS_HANDWRITING_IDLE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration STYLUS_HANDWRITING_IDLE_TIMEOUT_MAX = Duration.ofMinutes(3);

    private int mTheme;
    private Dialog mWindow;
    private LinearLayout mRootView;
    private FrameLayout mExtractFrame;
    private FrameLayout mCandidatesFrame;
    private FrameLayout mInputFrame;
    private View mExtractView;
    private View mCandidatesView;
    private View mInputView;
    private boolean mInitialized;
    private boolean mViewsCreated;
    private boolean mWindowVisible;
    private boolean mInShowWindow;
    private boolean mShowInputRequested;
    private boolean mInputStarted;
    private boolean mInputViewStarted;
    private boolean mCandidatesViewStarted;
    private boolean mIsInputViewShown;
    private boolean mIsFullscreen;
    private boolean mExtractViewShown;
    private int mCandidatesVisibility;
    private int mBackDisposition = BACK_DISPOSITION_DEFAULT;
    private int mStatusIcon;
    private IBinder mToken;
    private InputBinding mInputBinding;
    private InputConnection mInputConnection;
    private InputConnection mStartedInputConnection;
    private EditorInfo mInputEditorInfo;
    private boolean mHandwritingActive;
    private Region mHandwritingRegion;
    private Duration mHandwritingTimeout = STYLUS_HANDWRITING_IDLE_TIMEOUT;

    public InputMethodService()
    {
    }

    public static final class Insets
    {
        public static final int TOUCHABLE_INSETS_FRAME = 0;
        public static final int TOUCHABLE_INSETS_CONTENT = 1;
        public static final int TOUCHABLE_INSETS_VISIBLE = 2;
        public static final int TOUCHABLE_INSETS_REGION = 3;

        public int contentTopInsets;
        public int visibleTopInsets;
        public final Region touchableRegion = new Region();
        public int touchableInsets;

        public Insets()
        {
        }
    }

    public class InputMethodImpl extends AbstractInputMethodImpl
    {
        public InputMethodImpl()
        {
        }

        @Override
        public void attachToken(IBinder token)
        {
            if (mToken != null) throw new IllegalStateException("attachToken() must be called at most once");

            mToken = token;
        }

        @Override
        public void bindInput(InputBinding binding)
        {
            mInputBinding = binding;
            mInputConnection = binding == null ? null : binding.getConnection();
            onBindInput();
        }

        @Override
        public void unbindInput()
        {
            onUnbindInput();
            mInputBinding = null;
            mInputConnection = null;
        }

        @Override
        public void startInput(InputConnection inputConnection, EditorInfo editorInfo)
        {
            doStartInput(inputConnection, editorInfo, false);
        }

        @Override
        public void restartInput(InputConnection inputConnection, EditorInfo editorInfo)
        {
            doStartInput(inputConnection, editorInfo, true);
        }

        @Override
        public void showSoftInput(int flags, ResultReceiver resultReceiver)
        {
            boolean wasVisible = isInputViewShown();
            if (dispatchOnShowInputRequested(flags, false)) showWindow(true);

            if (resultReceiver == null) return;

            boolean visible = isInputViewShown();
            int result = visible ? (wasVisible ? InputMethodManager.RESULT_UNCHANGED_SHOWN : InputMethodManager.RESULT_SHOWN)
                                 : (wasVisible ? InputMethodManager.RESULT_HIDDEN : InputMethodManager.RESULT_UNCHANGED_HIDDEN);
            resultReceiver.send(result, null);
        }

        @Override
        public void hideSoftInput(int flags, ResultReceiver resultReceiver)
        {
            boolean wasVisible = isInputViewShown();
            mShowInputRequested = false;
            hideWindow();

            if (resultReceiver != null)
            {
                resultReceiver.send(wasVisible ? InputMethodManager.RESULT_HIDDEN : InputMethodManager.RESULT_UNCHANGED_HIDDEN, null);
            }
        }

        @Override
        public void changeInputMethodSubtype(InputMethodSubtype subtype)
        {
            onCurrentInputMethodSubtypeChanged(subtype);
        }
    }

    public class InputMethodSessionImpl extends AbstractInputMethodSessionImpl
    {
        public InputMethodSessionImpl()
        {
        }

        @Override
        public void finishInput()
        {
            if (!isEnabled()) return;

            doFinishInput();
        }

        @Override
        public void displayCompletions(CompletionInfo[] completions)
        {
            if (!isEnabled()) return;

            onDisplayCompletions(completions);
        }

        @Override
        public void updateExtractedText(int token, ExtractedText text)
        {
            if (!isEnabled()) return;

            onUpdateExtractedText(token, text);
        }

        @Override
        public void updateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart,
                                    int candidatesEnd)
        {
            if (!isEnabled()) return;

            onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        }

        @Override
        public void viewClicked(boolean focusChanged)
        {
            if (!isEnabled()) return;

            onViewClicked(focusChanged);
        }

        @Override
        public void updateCursor(Rect newCursor)
        {
            if (!isEnabled()) return;

            onUpdateCursor(newCursor);
        }

        @Override
        public void appPrivateCommand(String action, Bundle data)
        {
            if (!isEnabled()) return;

            onAppPrivateCommand(action, data);
        }

        @Override
        public void toggleSoftInput(int showFlags, int hideFlags)
        {
            if (isInputViewShown())
            {
                requestHideSelf(hideFlags);
                return;
            }

            if (dispatchOnShowInputRequested(showFlags, false)) showWindow(true);
        }

        @Override
        public void updateCursorAnchorInfo(CursorAnchorInfo info)
        {
            if (!isEnabled()) return;

            onUpdateCursorAnchorInfo(info);
        }
    }

    @Override
    public void setTheme(int theme)
    {
        if (mWindow != null) throw new IllegalStateException("Must be called before onCreate()");

        mTheme = theme;
        super.setTheme(theme);
    }

    @Deprecated
    public boolean enableHardwareAcceleration()
    {
        if (mWindow != null) throw new IllegalStateException("Must be called before onCreate()");

        return false;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        mWindow = new Dialog(this, mTheme);
        mRootView = new LinearLayout(this);
        mRootView.setOrientation(LinearLayout.VERTICAL);

        mExtractFrame = new FrameLayout(this);
        mExtractFrame.setVisibility(View.GONE);
        mCandidatesFrame = new FrameLayout(this);
        mCandidatesVisibility = getCandidatesHiddenVisibility();
        mCandidatesFrame.setVisibility(mCandidatesVisibility);
        mInputFrame = new FrameLayout(this);
        mInputFrame.setVisibility(View.GONE);

        LinearLayout.LayoutParams fill = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mRootView.addView(mExtractFrame, fill);
        mRootView.addView(mCandidatesFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                           ViewGroup.LayoutParams.WRAP_CONTENT));
        mRootView.addView(mInputFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                      ViewGroup.LayoutParams.WRAP_CONTENT));

        mWindow.setContentView(mRootView);
        Window window = mWindow.getWindow();
        if (window != null) window.setGravity(Gravity.BOTTOM);

        initViews();
    }

    private void initViews()
    {
        mInitialized = false;
        mViewsCreated = false;
        mShowInputRequested = false;
        mExtractView = null;
        mCandidatesView = null;
        mInputView = null;
        mExtractFrame.removeAllViews();
        mCandidatesFrame.removeAllViews();
        mInputFrame.removeAllViews();
    }

    private void initialize()
    {
        if (mInitialized) return;

        mInitialized = true;
        onInitializeInterface();
    }

    public void onInitializeInterface()
    {
    }

    @Override
    public void onDestroy()
    {
        hideWindow();
        if (mWindow != null) mWindow.dismiss();
        mWindow = null;
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration)
    {
        super.onConfigurationChanged(newConfiguration);

        if (mWindow == null) return;

        boolean visible = mWindowVisible;
        boolean showingInput = mShowInputRequested;
        boolean candidatesVisible = mCandidatesVisibility == View.VISIBLE;
        EditorInfo editorInfo = mInputEditorInfo;

        finishViews(false);
        initViews();

        if (!visible) return;

        if (showingInput)
        {
            if (onShowInputRequested(0, true)) showWindow(true);
            else hideWindow();
        }
        else if (candidatesVisible)
        {
            showWindow(false);
        }
        else
        {
            hideWindow();
        }

        if (editorInfo != null && mInputStarted) onUpdateExtractingVisibility(editorInfo);
    }

    @Override
    public AbstractInputMethodImpl onCreateInputMethodInterface()
    {
        return new InputMethodImpl();
    }

    @Override
    public AbstractInputMethodSessionImpl onCreateInputMethodSessionInterface()
    {
        return new InputMethodSessionImpl();
    }

    public LayoutInflater getLayoutInflater()
    {
        return LayoutInflater.from(this);
    }

    public Dialog getWindow()
    {
        return mWindow;
    }

    public void setBackDisposition(int disposition)
    {
        if (disposition == mBackDisposition) return;

        if (disposition < BACK_DISPOSITION_DEFAULT || disposition > BACK_DISPOSITION_ADJUST_NOTHING)
        {
            android.util.Log.e(TAG, "invalid back disposition value (" + disposition + ") specified");
            return;
        }

        mBackDisposition = disposition;
    }

    public int getBackDisposition()
    {
        return mBackDisposition;
    }

    @Deprecated
    public int getMaxWidth()
    {
        return getResources().getDisplayMetrics().widthPixels;
    }

    public InputBinding getCurrentInputBinding()
    {
        return mInputBinding;
    }

    public InputConnection getCurrentInputConnection()
    {
        return mStartedInputConnection != null ? mStartedInputConnection : mInputConnection;
    }

    public boolean getCurrentInputStarted()
    {
        return mInputStarted;
    }

    public EditorInfo getCurrentInputEditorInfo()
    {
        return mInputEditorInfo;
    }

    public void updateFullscreenMode()
    {
        boolean fullscreen = mShowInputRequested && onEvaluateFullscreenMode();
        boolean changed = mIsFullscreen != fullscreen || !mViewsCreated;

        if (changed)
        {
            mIsFullscreen = fullscreen;

            if (fullscreen && mExtractView == null)
            {
                View extract = onCreateExtractTextView();
                if (extract != null) setExtractView(extract);
            }
        }

        if (mWindow != null && mWindow.getWindow() != null)
        {
            onConfigureWindow(mWindow.getWindow(), fullscreen, !mShowInputRequested);
        }

        if (changed && mInputEditorInfo != null) onUpdateExtractingVisibility(mInputEditorInfo);
    }

    public void onConfigureWindow(Window window, boolean isFullscreen, boolean isCandidatesOnly)
    {
        int height = isFullscreen ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
    }

    public boolean isFullscreenMode()
    {
        return mIsFullscreen;
    }

    public boolean onEvaluateFullscreenMode()
    {
        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) return false;
        if (mInputEditorInfo != null && (mInputEditorInfo.imeOptions & EditorInfo.IME_FLAG_NO_FULLSCREEN) != 0) return false;

        return true;
    }

    public void setExtractViewShown(boolean shown)
    {
        if (mExtractViewShown == shown) return;

        mExtractViewShown = shown;
        if (mExtractFrame != null) mExtractFrame.setVisibility(shown && mIsFullscreen ? View.VISIBLE : View.GONE);
    }

    public boolean isExtractViewShown()
    {
        return mIsFullscreen && mExtractViewShown;
    }

    public void onComputeInsets(Insets outInsets)
    {
        if (mRootView == null || !mWindowVisible)
        {
            int height = getResources().getDisplayMetrics().heightPixels;
            outInsets.contentTopInsets = height;
            outInsets.visibleTopInsets = height;
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_FRAME;
            outInsets.touchableRegion.setEmpty();
            return;
        }

        int[] location = new int[2];
        View top = isInputViewShown() ? mInputFrame : mCandidatesFrame;
        top.getLocationInWindow(location);

        outInsets.contentTopInsets = isFullscreenMode() ? mRootView.getHeight() : location[1];

        if (mCandidatesFrame.getVisibility() == View.VISIBLE)
        {
            mCandidatesFrame.getLocationInWindow(location);
        }

        outInsets.visibleTopInsets = location[1];
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_VISIBLE;
        outInsets.touchableRegion.setEmpty();
    }

    public void updateInputViewShown()
    {
        boolean shown = mShowInputRequested && onEvaluateInputViewShown();
        if (mIsInputViewShown == shown || !mWindowVisible) return;

        mIsInputViewShown = shown;
        mInputFrame.setVisibility(shown ? View.VISIBLE : View.GONE);

        if (mInputView != null) return;

        initialize();
        View view = onCreateInputView();
        if (view != null) setInputView(view);
    }

    public boolean isShowInputRequested()
    {
        return mShowInputRequested;
    }

    public boolean isInputViewShown()
    {
        return mWindowVisible && mIsInputViewShown;
    }

    public boolean onEvaluateInputViewShown()
    {
        Configuration configuration = getResources().getConfiguration();
        return configuration.keyboard == Configuration.KEYBOARD_NOKEYS
               || configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES;
    }

    public void setCandidatesViewShown(boolean shown)
    {
        updateCandidatesVisibility(shown);

        if (mShowInputRequested || mWindowVisible == shown) return;

        if (shown) showWindow(false);
        else hideWindow();
    }

    private void updateCandidatesVisibility(boolean shown)
    {
        int visibility = shown ? View.VISIBLE : getCandidatesHiddenVisibility();
        if (mCandidatesVisibility == visibility) return;

        mCandidatesVisibility = visibility;
        if (mCandidatesFrame != null) mCandidatesFrame.setVisibility(visibility);
    }

    public int getCandidatesHiddenVisibility()
    {
        return isExtractViewShown() ? View.GONE : View.INVISIBLE;
    }

    public void showStatusIcon(int iconResId)
    {
        mStatusIcon = iconResId;
    }

    public void hideStatusIcon()
    {
        mStatusIcon = 0;
    }

    public void switchInputMethod(String id)
    {
        android.util.Log.w(TAG, "there is no system input method service here, so " + id + " cannot be switched to");
    }

    public final void switchInputMethod(String id, InputMethodSubtype subtype)
    {
        switchInputMethod(id);
    }

    public final boolean switchToPreviousInputMethod()
    {
        return false;
    }

    public final boolean switchToNextInputMethod(boolean onlyCurrentIme)
    {
        return false;
    }

    public final boolean shouldOfferSwitchingToNextInputMethod()
    {
        return false;
    }

    public void setExtractView(View view)
    {
        mExtractFrame.removeAllViews();
        mExtractView = view;
        if (view != null)
        {
            mExtractFrame.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                      ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    public void setCandidatesView(View view)
    {
        mCandidatesFrame.removeAllViews();
        mCandidatesView = view;
        if (view != null)
        {
            mCandidatesFrame.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                         ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    public void setInputView(View view)
    {
        mInputFrame.removeAllViews();
        mInputView = view;
        if (view != null)
        {
            mInputFrame.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    public View onCreateExtractTextView()
    {
        return null;
    }

    public View onCreateCandidatesView()
    {
        return null;
    }

    public View onCreateInputView()
    {
        return null;
    }

    public void onStartInputView(EditorInfo info, boolean restarting)
    {
    }

    public void onFinishInputView(boolean finishingInput)
    {
        if (finishingInput) return;

        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.finishComposingText();
    }

    public void onStartCandidatesView(EditorInfo info, boolean restarting)
    {
    }

    public void onFinishCandidatesView(boolean finishingInput)
    {
        if (finishingInput) return;

        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.finishComposingText();
    }

    public boolean onStartStylusHandwriting()
    {
        return false;
    }

    public boolean onStartConnectionlessStylusHandwriting(int inputType, CursorAnchorInfo cursorAnchorInfo)
    {
        return false;
    }

    public void onPrepareStylusHandwriting()
    {
    }

    public void onStylusHandwritingMotionEvent(MotionEvent motionEvent)
    {
    }

    public void onUpdateEditorToolType(int toolType)
    {
    }

    public void onFinishStylusHandwriting()
    {
    }

    public final Window getStylusHandwritingWindow()
    {
        return null;
    }

    public final void finishStylusHandwriting()
    {
        if (!mHandwritingActive) return;

        mHandwritingActive = false;
        onFinishStylusHandwriting();
    }

    public final void finishConnectionlessStylusHandwriting(CharSequence text)
    {
        finishStylusHandwriting();
    }

    public final void setStylusHandwritingRegion(Region handwritingRegion)
    {
        mHandwritingRegion = handwritingRegion == null ? null : new Region(handwritingRegion);
    }

    public final void setStylusHandwritingSessionTimeout(Duration duration)
    {
        if (duration.isNegative() || duration.isZero())
        {
            throw new IllegalStateException("A positive value should be set for Stylus handwriting session timeout.");
        }

        mHandwritingTimeout = duration.compareTo(STYLUS_HANDWRITING_IDLE_TIMEOUT_MAX) > 0 ? STYLUS_HANDWRITING_IDLE_TIMEOUT_MAX : duration;
    }

    public final Duration getStylusHandwritingSessionTimeout()
    {
        return mHandwritingTimeout;
    }

    public static final Duration getStylusHandwritingIdleTimeoutMax()
    {
        return STYLUS_HANDWRITING_IDLE_TIMEOUT_MAX;
    }

    public void onCustomImeSwitcherButtonRequestedVisible(boolean visible)
    {
    }

    public void onBindInput()
    {
    }

    public void onUnbindInput()
    {
    }

    public void onStartInput(EditorInfo attribute, boolean restarting)
    {
    }

    public void onFinishInput()
    {
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.finishComposingText();
    }

    public void onDisplayCompletions(CompletionInfo[] completions)
    {
    }

    public void onUpdateExtractedText(int token, ExtractedText text)
    {
    }

    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart,
                                  int candidatesEnd)
    {
    }

    public void onViewClicked(boolean focusChanged)
    {
    }

    @Deprecated
    public void onUpdateCursor(Rect newCursor)
    {
    }

    public void onUpdateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo)
    {
    }

    public void requestHideSelf(int flags)
    {
        mShowInputRequested = false;
        hideWindow();
    }

    public final void requestShowSelf(int flags)
    {
        if (dispatchOnShowInputRequested(flags, false)) showWindow(true);
    }

    private boolean dispatchOnShowInputRequested(int flags, boolean configChange)
    {
        boolean result = onShowInputRequested(flags, configChange);
        mShowInputRequested = result;
        return result;
    }

    public boolean onShowInputRequested(int flags, boolean configChange)
    {
        if (!onEvaluateInputViewShown()) return false;
        if ((flags & InputMethod.SHOW_EXPLICIT) != 0) return true;
        if (!configChange && onEvaluateFullscreenMode() && !isInputViewShown()) return false;

        Configuration configuration = getResources().getConfiguration();
        return configChange || configuration.keyboard == Configuration.KEYBOARD_NOKEYS;
    }

    public void showWindow(boolean showInput)
    {
        if (mInShowWindow || mWindow == null) return;

        mInShowWindow = true;

        try
        {
            boolean wasVisible = mWindowVisible;
            mWindowVisible = true;

            if (!mShowInputRequested && mInputStarted && showInput) mShowInputRequested = true;

            initialize();
            updateFullscreenMode();
            updateInputViewShown();

            if (!mViewsCreated)
            {
                mViewsCreated = true;
                initialize();
                View candidates = onCreateCandidatesView();
                if (candidates != null) setCandidatesView(candidates);
            }

            if (mShowInputRequested)
            {
                if (!mInputViewStarted)
                {
                    mInputViewStarted = true;
                    onStartInputView(mInputEditorInfo, false);
                }
            }
            else if (!mCandidatesViewStarted)
            {
                mCandidatesViewStarted = true;
                onStartCandidatesView(mInputEditorInfo, false);
            }

            if (!wasVisible)
            {
                onWindowShown();
                mWindow.show();
            }
        }
        finally
        {
            mInShowWindow = false;
        }
    }

    public void hideWindow()
    {
        finishViews(false);

        if (!mWindowVisible) return;

        if (mWindow != null) mWindow.hide();
        mWindowVisible = false;
        mIsInputViewShown = false;
        if (mInputFrame != null) mInputFrame.setVisibility(View.GONE);
        onWindowHidden();
    }

    private void finishViews(boolean finishingInput)
    {
        if (mInputViewStarted) onFinishInputView(finishingInput);
        else if (mCandidatesViewStarted) onFinishCandidatesView(finishingInput);

        mInputViewStarted = false;
        mCandidatesViewStarted = false;
    }

    public void onWindowShown()
    {
    }

    public void onWindowHidden()
    {
    }

    private void doFinishInput()
    {
        finishViews(true);

        if (mInputStarted) onFinishInput();

        mInputStarted = false;
        mStartedInputConnection = null;
    }

    private void doStartInput(InputConnection connection, EditorInfo attribute, boolean restarting)
    {
        if (!restarting && mInputStarted) doFinishInput();

        mInputStarted = true;
        mStartedInputConnection = connection;
        mInputEditorInfo = attribute;

        initialize();
        onStartInput(attribute, restarting);

        if (!mWindowVisible) return;

        if (mShowInputRequested)
        {
            mInputViewStarted = true;
            onStartInputView(mInputEditorInfo, restarting);
        }
        else if (mCandidatesVisibility == View.VISIBLE)
        {
            mCandidatesViewStarted = true;
            onStartCandidatesView(mInputEditorInfo, restarting);
        }
    }

    public void onAppPrivateCommand(String action, Bundle data)
    {
    }

    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype)
    {
    }

    @Deprecated
    public int getInputMethodWindowRecommendedHeight()
    {
        return 0;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) return false;
        if (!handleBack(false)) return false;

        event.startTracking();
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event)
    {
        return false;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event)
    {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) return false;
        if (!event.isTracking() || event.isCanceled()) return false;

        return handleBack(true);
    }

    private boolean handleBack(boolean doIt)
    {
        if (!mWindowVisible) return false;
        if (mBackDisposition == BACK_DISPOSITION_WILL_NOT_DISMISS) return false;

        if (isInputViewShown() || mCandidatesVisibility == View.VISIBLE)
        {
            if (doIt)
            {
                if (isInputViewShown()) requestHideSelf(0);
                else setCandidatesViewShown(false);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event)
    {
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        return false;
    }

    @Override
    public boolean onShouldVerifyKeyEvent(KeyEvent event)
    {
        return false;
    }

    public void sendDownUpKeyEvents(int keyEventCode)
    {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) return;

        long eventTime = SystemClock.uptimeMillis();
        int flags = KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;
        connection.sendKeyEvent(new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyEventCode, 0, 0,
                                             KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags));
        connection.sendKeyEvent(new KeyEvent(eventTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyEventCode, 0, 0,
                                             KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags));
    }

    public boolean sendDefaultEditorAction(boolean fromEnterKey)
    {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null) return false;
        if (fromEnterKey && (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return false;

        int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        if (action == EditorInfo.IME_ACTION_NONE) return false;

        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.performEditorAction(action);
        return true;
    }

    public void sendKeyChar(char charCode)
    {
        if (charCode == '\n')
        {
            if (!sendDefaultEditorAction(true)) sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
            return;
        }

        if (charCode >= '0' && charCode <= '9')
        {
            sendDownUpKeyEvents(charCode - '0' + KeyEvent.KEYCODE_0);
            return;
        }

        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.commitText(String.valueOf(charCode), 1);
    }

    public void onExtractedSelectionChanged(int start, int end)
    {
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.setSelection(start, end);
    }

    public void onExtractedTextClicked()
    {
        if (mInputEditorInfo == null) return;

        if (isExtractViewShown() && isFullscreenMode()) setCandidatesViewShown(false);
    }

    public void onExtractedCursorMovement(int dx, int dy)
    {
        if (dy != 0 && isExtractViewShown()) setCandidatesViewShown(false);
    }

    public boolean onExtractTextContextMenuItem(int id)
    {
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) connection.performContextMenuAction(id);
        return true;
    }

    public CharSequence getTextForImeAction(int imeOptions)
    {
        switch (imeOptions & EditorInfo.IME_MASK_ACTION)
        {
            case EditorInfo.IME_ACTION_NONE:
                return null;
            case EditorInfo.IME_ACTION_GO:
                return "Go";
            case EditorInfo.IME_ACTION_SEARCH:
                return "Search";
            case EditorInfo.IME_ACTION_SEND:
                return "Send";
            case EditorInfo.IME_ACTION_NEXT:
                return "Next";
            case EditorInfo.IME_ACTION_DONE:
                return "Done";
            case EditorInfo.IME_ACTION_PREVIOUS:
                return "Previous";
            default:
                return "Execute";
        }
    }

    public void onUpdateExtractingVisibility(EditorInfo editorInfo)
    {
        if (editorInfo.inputType == InputType.TYPE_NULL || (editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_EXTRACT_UI) != 0)
        {
            setExtractViewShown(false);
            return;
        }

        setExtractViewShown(true);
    }

    public void onUpdateExtractingViews(EditorInfo editorInfo)
    {
        if (!isExtractViewShown()) return;

        onUpdateExtractingVisibility(editorInfo);
    }

    public void onExtractingInputChanged(EditorInfo editorInfo)
    {
        if (editorInfo.inputType == InputType.TYPE_NULL) requestHideSelf(InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public Object getSystemService(String name)
    {
        return super.getSystemService(name);
    }

    @Override
    protected void dump(FileDescriptor descriptor, PrintWriter writer, String[] arguments)
    {
        writer.println("Input method service state for " + this + ":");
        writer.println("  mViewsCreated=" + mViewsCreated + " mWindowVisible=" + mWindowVisible);
        writer.println("  mInputStarted=" + mInputStarted + " mInputViewStarted=" + mInputViewStarted
                       + " mCandidatesViewStarted=" + mCandidatesViewStarted);
        writer.println("  mInputEditorInfo=" + mInputEditorInfo);
        writer.println("  mShowInputRequested=" + mShowInputRequested + " mIsInputViewShown=" + mIsInputViewShown
                       + " mIsFullscreen=" + mIsFullscreen);
        writer.println("  mCandidatesVisibility=" + mCandidatesVisibility + " mBackDisposition=" + mBackDisposition
                       + " mStatusIcon=" + mStatusIcon);
        writer.println("  mExtractViewShown=" + mExtractViewShown + " mHandwritingRegion=" + mHandwritingRegion);
    }
}
