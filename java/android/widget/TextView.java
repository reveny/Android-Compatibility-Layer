package android.widget;

import android.content.ClipData;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.BoringLayout;
import android.text.Editable;
import android.text.GetChars;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.PrecomputedText;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.DateKeyListener;
import android.text.method.DateTimeKeyListener;
import android.text.method.DialerKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.text.method.LinkMovementMethod;
import android.text.method.MetaKeyKeyListener;
import android.text.method.MovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TextKeyListener;
import android.text.method.TimeKeyListener;
import android.text.method.TransformationMethod;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ParagraphStyle;
import android.text.style.SuggestionSpan;
import android.text.style.URLSpan;
import android.text.style.UpdateAppearance;
import android.text.style.UpdateLayout;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContentInfo;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.StyleableSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class TextView extends View implements ViewTreeObserver.OnPreDrawListener
{
    public static final int AUTO_SIZE_TEXT_TYPE_NONE = 0;
    public static final int AUTO_SIZE_TEXT_TYPE_UNIFORM = 1;
    public static final int FOCUSED_SEARCH_RESULT_INDEX_NONE = -1;

    private static final int ID_SELECT_ALL = 0x0102001f;
    private static final int ID_CUT = 0x01020020;
    private static final int ID_COPY = 0x01020021;
    private static final int ID_PASTE = 0x01020022;
    private static final int ID_PASTE_AS_PLAIN_TEXT = 0x01020031;

    private static final int LINES = 1;
    private static final int EMS = LINES;
    private static final int PIXELS = 2;
    private static final int VERY_WIDE = 1024 * 1024;
    private static final int CHANGE_WATCHER_PRIORITY = 100;
    private static final int DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP = 12;
    private static final int DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP = 112;
    private static final int DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX = 1;
    private static final float UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE = -1f;
    private static final int DEFAULT_TYPEFACE = -1;
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;
    private static final int CURSOR_BLINK_INTERVAL = 500;
    private static final int DEFAULT_HIGHLIGHT_COLOR = 0x6633B5E5;
    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final Spanned EMPTY_SPANNED = new SpannedString("");

    private static StyleableSet sStyleable;
    private static StyleableSet sAppearanceStyleable;

    public enum BufferType
    {
        NORMAL,
        SPANNABLE,
        EDITABLE
    }

    public interface OnEditorActionListener
    {
        boolean onEditorAction(TextView view, int actionId, KeyEvent event);
    }

    private final TextPaint mTextPaint;
    private final Paint mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<TextWatcher> mListeners = new ArrayList<>();
    private final Rect mTempRect = new Rect();

    private CharSequence mText = "";
    private CharSequence mTransformed = "";
    private BufferType mBufferType = BufferType.NORMAL;
    private Editable.Factory mEditableFactory = Editable.Factory.getInstance();
    private Spannable.Factory mSpannableFactory = Spannable.Factory.getInstance();
    private ChangeWatcher mChangeWatcher;
    private boolean mInSetText;

    private CharSequence mHint;
    private Layout mLayout;
    private Layout mHintLayout;

    private ColorStateList mTextColor;
    private ColorStateList mHintTextColor;
    private ColorStateList mLinkTextColor;
    private int mCurTextColor = 0xFF000000;
    private int mCurHintTextColor;
    private int mHighlightColor = DEFAULT_HIGHLIGHT_COLOR;

    private int mGravity = Gravity.TOP | Gravity.START;
    private boolean mHorizontallyScrolling;
    private boolean mSingleLine;
    private TextUtils.TruncateAt mEllipsize;
    private int mMarqueeRepeatLimit = 3;

    private int mMaximum = Integer.MAX_VALUE;
    private int mMaxMode = LINES;
    private int mMinimum;
    private int mMinMode = LINES;
    private int mOldMaximum = Integer.MAX_VALUE;
    private int mOldMaxMode = LINES;
    private int mMaxWidthValue = Integer.MAX_VALUE;
    private int mMaxWidthMode = PIXELS;
    private int mMinWidthValue;
    private int mMinWidthMode = PIXELS;

    private float mSpacingMultiplier = 1.0f;
    private float mSpacingAdd;
    private boolean mIncludePad = true;
    private boolean mUseFallbackLineSpacing = true;
    private int mBreakStrategy = Layout.BREAK_STRATEGY_SIMPLE;
    private int mHyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE;
    private int mJustificationMode = Layout.JUSTIFICATION_MODE_NONE;
    private int mLineBreakStyle;
    private int mLineBreakWordStyle;
    private String mFontVariationSettings;

    private MovementMethod mMovement;
    private KeyListener mKeyListener;
    private TransformationMethod mTransformation;
    private boolean mAllowTransformationLengthChange;
    private InputFilter[] mFilters = NO_FILTERS;
    private boolean mAllCaps;

    private int mInputType = InputType.TYPE_NULL;
    private int mImeOptions = EditorInfo.IME_NULL;
    private CharSequence mImeActionLabel;
    private int mImeActionId;
    private String mPrivateImeOptions;
    private LocaleList mImeHintLocales;
    private Bundle mInputExtras;
    private OnEditorActionListener mEditorActionListener;
    private boolean mShowSoftInputOnFocus = true;

    private CharSequence mError;
    private Drawable mErrorDrawable;

    private final Drawable[] mDrawables = new Drawable[4];
    private Drawable mDrawableStart;
    private Drawable mDrawableEnd;
    private boolean mDrawablesRelative;
    private int mDrawablePadding;
    private ColorStateList mDrawableTintList;
    private PorterDuff.Mode mDrawableTintMode;
    private BlendMode mDrawableTintBlendMode;
    private boolean mHasDrawableTint;
    private boolean mHasDrawableTintMode;

    private int mAutoLinkMask;
    private boolean mLinksClickable = true;

    private float mShadowRadius;
    private float mShadowDx;
    private float mShadowDy;
    private int mShadowColor;

    private int mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_NONE;
    private float mAutoSizeMinTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
    private float mAutoSizeMaxTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
    private float mAutoSizeStepGranularityInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
    private int[] mAutoSizeTextSizesInPx = new int[0];
    private boolean mHasPresetAutoSizeValues;
    private boolean mNeedsAutoSizeText;

    private int mTextSizeUnit = TypedValue.COMPLEX_UNIT_SP;
    private boolean mCursorVisible = true;
    private boolean mCursorBlinkOn = true;
    private Runnable mBlink;
    private boolean mTextIsSelectable;
    private boolean mSelectAllOnFocus;
    private boolean mFreezesText;
    private boolean mTouchFocusSelected;
    private int mBatchEditNesting;
    private boolean mPreDrawRegistered;
    private Drawable mTextCursorDrawable;
    private int mTextCursorDrawableResource;
    private Drawable mTextSelectHandle;
    private Drawable mTextSelectHandleLeft;
    private Drawable mTextSelectHandleRight;
    private ActionMode.Callback mCustomSelectionActionModeCallback;
    private ActionMode.Callback mCustomInsertionActionModeCallback;
    private TextClassifier mTextClassifier;
    private Scroller mScroller;
    private int mFirstBaselineToTopHeight = -1;
    private int mLastBaselineToBottomHeight = -1;
    private int[] mSearchResultHighlights;
    private int mFocusedSearchResultIndex = FOCUSED_SEARCH_RESULT_INDEX_NONE;
    private int mSearchResultHighlightColor = 0xFFFFFF00;
    private int mFocusedSearchResultHighlightColor = 0xFFFF9800;
    private android.text.Highlights mHighlights;

    public TextView(Context context)
    {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attributes)
    {
        this(context, attributes, attribute(context, "textViewStyle"));
    }

    public TextView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public TextView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.density = metrics.density;
        mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, metrics));

        mMovement = getDefaultMovementMethod();

        CharSequence text = "";
        CharSequence hint = null;
        BufferType bufferType = BufferType.EDITABLE;
        boolean editable = getDefaultEditable();
        int inputType = InputType.TYPE_NULL;
        boolean singleLine = false;
        boolean scrollHorizontally = false;
        int maxLength = -1;
        int ellipsize = -1;
        boolean password = false;
        boolean selectAllOnFocus = false;
        boolean textIsSelectable = false;
        CharSequence digits = null;
        int numeric = 0;
        boolean phone = false;
        boolean autoText = false;
        int autoCap = -1;
        float autoSizeMin = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        float autoSizeMax = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        float autoSizeStep = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        int firstBaselineToTopHeight = -1;
        int lastBaselineToBottomHeight = -1;
        int lineHeight = -1;

        Drawable left = null;
        Drawable top = null;
        Drawable right = null;
        Drawable bottom = null;
        Drawable start = null;
        Drawable end = null;

        Appearance appearance = new Appearance();
        StyleableSet styleable = styleable(context);
        TypedArray array = null;

        if (!styleable.isEmpty()) array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);

        if (array != null)
        {
            Reader reader = new Reader(array, styleable);

            int appearanceId = reader.resourceId("textAppearance", 0);
            if (appearanceId != 0) readAppearance(context, appearanceId, appearance);

            readAppearance(reader, appearance);

            if (reader.has("text")) text = reader.text("text");
            if (reader.has("hint")) hint = reader.text("hint");
            if (reader.has("editable")) editable = reader.bool("editable", editable);
            if (reader.has("inputType")) inputType = reader.integer("inputType", InputType.TYPE_NULL);
            if (reader.has("imeOptions")) mImeOptions = reader.integer("imeOptions", mImeOptions);
            if (reader.has("imeActionLabel")) mImeActionLabel = reader.text("imeActionLabel");
            if (reader.has("imeActionId")) mImeActionId = reader.integer("imeActionId", mImeActionId);
            if (reader.has("privateImeOptions")) mPrivateImeOptions = reader.string("privateImeOptions");
            if (reader.has("singleLine")) singleLine = reader.bool("singleLine", false);
            if (reader.has("maxLength")) maxLength = reader.integer("maxLength", -1);
            if (reader.has("ellipsize")) ellipsize = reader.integer("ellipsize", -1);
            if (reader.has("password")) password = reader.bool("password", false);
            if (reader.has("selectAllOnFocus")) selectAllOnFocus = reader.bool("selectAllOnFocus", false);
            if (reader.has("textIsSelectable")) textIsSelectable = reader.bool("textIsSelectable", false);
            if (reader.has("digits")) digits = reader.text("digits");
            if (reader.has("numeric")) numeric = reader.integer("numeric", 0);
            if (reader.has("phoneNumber")) phone = reader.bool("phoneNumber", false);
            if (reader.has("autoText")) autoText = reader.bool("autoText", false);
            if (reader.has("capitalize")) autoCap = reader.integer("capitalize", -1);
            if (reader.has("bufferType")) bufferType = BufferType.values()[Math.max(0, Math.min(2, reader.integer("bufferType", 0)))];
            if (reader.has("autoLink")) mAutoLinkMask = reader.integer("autoLink", 0);
            if (reader.has("linksClickable")) mLinksClickable = reader.bool("linksClickable", true);
            if (reader.has("cursorVisible")) mCursorVisible = reader.bool("cursorVisible", true);
            if (reader.has("freezesText")) mFreezesText = reader.bool("freezesText", false);
            if (reader.has("marqueeRepeatLimit")) mMarqueeRepeatLimit = reader.integer("marqueeRepeatLimit", mMarqueeRepeatLimit);
            if (reader.has("includeFontPadding")) mIncludePad = reader.bool("includeFontPadding", true);
            if (reader.has("fallbackLineSpacing")) mUseFallbackLineSpacing = reader.bool("fallbackLineSpacing", true);
            if (reader.has("breakStrategy")) mBreakStrategy = reader.integer("breakStrategy", mBreakStrategy);
            if (reader.has("hyphenationFrequency")) mHyphenationFrequency = reader.integer("hyphenationFrequency", 0);
            if (reader.has("justificationMode")) mJustificationMode = reader.integer("justificationMode", 0);
            if (reader.has("lineBreakStyle")) mLineBreakStyle = reader.integer("lineBreakStyle", 0);
            if (reader.has("lineBreakWordStyle")) mLineBreakWordStyle = reader.integer("lineBreakWordStyle", 0);
            if (reader.has("scrollHorizontally")) scrollHorizontally = reader.bool("scrollHorizontally", false);
            if (reader.has("gravity")) mGravity = reader.integer("gravity", mGravity);
            if (reader.has("lineSpacingExtra")) mSpacingAdd = reader.dimensionPixelSize("lineSpacingExtra", 0);
            if (reader.has("lineSpacingMultiplier")) mSpacingMultiplier = reader.floatValue("lineSpacingMultiplier", 1f);
            if (reader.has("lineHeight")) lineHeight = reader.dimensionPixelSize("lineHeight", -1);
            if (reader.has("firstBaselineToTopHeight"))
            {
                firstBaselineToTopHeight = reader.dimensionPixelSize("firstBaselineToTopHeight", -1);
            }
            if (reader.has("lastBaselineToBottomHeight"))
            {
                lastBaselineToBottomHeight = reader.dimensionPixelSize("lastBaselineToBottomHeight", -1);
            }

            if (reader.has("maxLines")) setMaxLinesInternal(reader.integer("maxLines", -1));
            if (reader.has("maxHeight")) setMaxHeightInternal(reader.dimensionPixelSize("maxHeight", -1));
            if (reader.has("lines")) setLinesInternal(reader.integer("lines", -1));
            if (reader.has("height")) setHeightInternal(reader.dimensionPixelSize("height", -1));
            if (reader.has("minLines")) setMinLinesInternal(reader.integer("minLines", -1));
            if (reader.has("minHeight")) setMinHeightInternal(reader.dimensionPixelSize("minHeight", -1));
            if (reader.has("maxEms")) setMaxEmsInternal(reader.integer("maxEms", -1));
            if (reader.has("maxWidth")) setMaxWidthInternal(reader.dimensionPixelSize("maxWidth", -1));
            if (reader.has("ems")) setEmsInternal(reader.integer("ems", -1));
            if (reader.has("width")) setWidthInternal(reader.dimensionPixelSize("width", -1));
            if (reader.has("minEms")) setMinEmsInternal(reader.integer("minEms", -1));
            if (reader.has("minWidth")) setMinWidthInternal(reader.dimensionPixelSize("minWidth", -1));

            left = reader.drawable("drawableLeft");
            top = reader.drawable("drawableTop");
            right = reader.drawable("drawableRight");
            bottom = reader.drawable("drawableBottom");
            start = reader.drawable("drawableStart");
            end = reader.drawable("drawableEnd");
            if (reader.has("drawablePadding")) mDrawablePadding = reader.dimensionPixelSize("drawablePadding", 0);

            if (reader.has("drawableTint"))
            {
                mDrawableTintList = reader.colorStateList("drawableTint");
                mHasDrawableTint = true;
            }

            if (reader.has("drawableTintMode"))
            {
                mDrawableTintMode = parseTintMode(reader.integer("drawableTintMode", -1));
                mHasDrawableTintMode = mDrawableTintMode != null;
            }

            if (reader.has("autoSizeTextType")) mAutoSizeTextType = reader.integer("autoSizeTextType", AUTO_SIZE_TEXT_TYPE_NONE);
            if (reader.has("autoSizeMinTextSize")) autoSizeMin = reader.dimension("autoSizeMinTextSize", autoSizeMin);
            if (reader.has("autoSizeMaxTextSize")) autoSizeMax = reader.dimension("autoSizeMaxTextSize", autoSizeMax);
            if (reader.has("autoSizeStepGranularity")) autoSizeStep = reader.dimension("autoSizeStepGranularity", autoSizeStep);

            int presetsId = reader.resourceId("autoSizePresetSizes", 0);
            if (presetsId != 0) setupAutoSizeUniformPresetSizes(context, presetsId);

            int cursorId = reader.resourceId("textCursorDrawable", 0);
            if (cursorId != 0) mTextCursorDrawableResource = cursorId;

            if (reader.has("enabled")) setEnabled(reader.bool("enabled", true));

            array.recycle();
        }

        applyAppearance(appearance);

        if (mTextColor == null) mTextColor = ColorStateList.valueOf(0xFF000000);
        updateTextColors();

        int variation = inputType & (InputType.TYPE_MASK_CLASS | InputType.TYPE_MASK_VARIATION);
        boolean passwordInputType = variation == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        boolean webPasswordInputType = variation == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
        boolean numberPasswordInputType = variation == (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        if (inputType != InputType.TYPE_NULL)
        {
            setInputType(inputType, true);
            singleLine = !isMultilineInputType(inputType);
        }
        else if (digits != null)
        {
            mKeyListener = DigitsKeyListener.getInstance(digits.toString());
            mInputType = inputType != InputType.TYPE_NULL ? inputType : InputType.TYPE_CLASS_TEXT;
        }
        else if (phone)
        {
            mKeyListener = DialerKeyListener.getInstance();
            mInputType = InputType.TYPE_CLASS_PHONE;
        }
        else if (numeric != 0)
        {
            mKeyListener = DigitsKeyListener.getInstance((numeric & 2) != 0, (numeric & 4) != 0);
            int type = InputType.TYPE_CLASS_NUMBER;
            if ((numeric & 2) != 0) type |= InputType.TYPE_NUMBER_FLAG_SIGNED;
            if ((numeric & 4) != 0) type |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            mInputType = type;
        }
        else if (autoText || autoCap != -1)
        {
            TextKeyListener.Capitalize capitalize;
            int type = InputType.TYPE_CLASS_TEXT;

            if (autoCap == 1)
            {
                capitalize = TextKeyListener.Capitalize.SENTENCES;
                type |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
            }
            else if (autoCap == 2)
            {
                capitalize = TextKeyListener.Capitalize.WORDS;
                type |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
            }
            else if (autoCap == 3)
            {
                capitalize = TextKeyListener.Capitalize.CHARACTERS;
                type |= InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
            }
            else
            {
                capitalize = TextKeyListener.Capitalize.NONE;
            }

            mKeyListener = TextKeyListener.getInstance(autoText, capitalize);
            mInputType = type;
        }
        else if (textIsSelectable)
        {
            mInputType = InputType.TYPE_NULL;
            mKeyListener = null;
            bufferType = BufferType.SPANNABLE;
            setFocusableInTouchMode(true);
            setFocusable(true);
            mMovement = ArrowKeyMovementMethod.getInstance();
        }
        else if (editable)
        {
            mKeyListener = TextKeyListener.getInstance();
            mInputType = InputType.TYPE_CLASS_TEXT;
        }
        else
        {
            mKeyListener = null;
            switch (bufferType)
            {
                case NORMAL:
                    bufferType = BufferType.NORMAL;
                    break;
                case SPANNABLE:
                    bufferType = BufferType.SPANNABLE;
                    break;
                default:
                    bufferType = BufferType.NORMAL;
                    break;
            }
        }

        if (mKeyListener != null) bufferType = BufferType.EDITABLE;

        if (password && (mInputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT)
        {
            mInputType = (mInputType & ~InputType.TYPE_MASK_VARIATION) | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        }

        if (selectAllOnFocus) mSelectAllOnFocus = true;
        mTextIsSelectable = textIsSelectable;

        if (left != null || top != null || right != null || bottom != null)
        {
            setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
        }
        if (start != null || end != null) setRelativeDrawablesIfNeeded(start, end);

        if (inputType == InputType.TYPE_NULL) setInputTypeSingleLine(singleLine);

        applySingleLine(singleLine, singleLine, singleLine, maxLength < 0);

        if (scrollHorizontally) setHorizontallyScrolling(true);
        if (singleLine && mKeyListener == null && ellipsize < 0) ellipsize = 3;

        if (password || passwordInputType || webPasswordInputType || numberPasswordInputType)
        {
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            if (appearance.mTypefaceIndex == DEFAULT_TYPEFACE && appearance.mFamily == null && appearance.mFont == null)
            {
                setTypeface(Typeface.MONOSPACE);
            }
        }

        if (ellipsize == 1) setEllipsize(TextUtils.TruncateAt.START);
        else if (ellipsize == 2) setEllipsize(TextUtils.TruncateAt.MIDDLE);
        else if (ellipsize == 3) setEllipsize(TextUtils.TruncateAt.END);
        else if (ellipsize == 4) setEllipsize(TextUtils.TruncateAt.MARQUEE);

        if (maxLength >= 0) setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
        else setFilters(NO_FILTERS);

        if (appearance.mAllCaps) setAllCaps(true);

        setText(text, bufferType);
        if (hint != null) setHint(hint);

        if (mAutoSizeTextType == AUTO_SIZE_TEXT_TYPE_UNIFORM && !mHasPresetAutoSizeValues)
        {
            if (autoSizeMin == UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE)
            {
                autoSizeMin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP, metrics);
            }

            if (autoSizeMax == UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE)
            {
                autoSizeMax = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP, metrics);
            }

            if (autoSizeStep == UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE) autoSizeStep = DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX;

            validateAndSetAutoSizeTextTypeUniformConfiguration(autoSizeMin, autoSizeMax, autoSizeStep);
        }
        else if (mAutoSizeTextType == AUTO_SIZE_TEXT_TYPE_UNIFORM)
        {
            setupAutoSizeText();
        }

        if (firstBaselineToTopHeight >= 0) setFirstBaselineToTopHeight(firstBaselineToTopHeight);
        if (lastBaselineToBottomHeight >= 0) setLastBaselineToBottomHeight(lastBaselineToBottomHeight);
        if (lineHeight >= 0) setLineHeight(lineHeight);

        if (mMovement != null || mKeyListener != null)
        {
            setFocusable(true);
            setClickable(true);
            setLongClickable(true);
        }
    }

    private static int attribute(Context context, String name)
    {
        if (context == null) return 0;

        return context.getResources().getIdentifier(name, "attr", "android");
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "text", "hint", "textAppearance", "textSize", "textColor",
                                         "textColorHint", "textColorLink", "textColorHighlight", "textStyle", "typeface",
                                         "fontFamily", "textFontWeight", "textAllCaps", "shadowColor", "shadowDx", "shadowDy",
                                         "shadowRadius", "elegantTextHeight", "letterSpacing", "fontFeatureSettings",
                                         "fontVariationSettings", "textLocale", "textScaleX", "editable", "inputType", "imeOptions",
                                         "imeActionLabel", "imeActionId", "privateImeOptions", "singleLine", "maxLength",
                                         "ellipsize", "password", "selectAllOnFocus", "textIsSelectable", "digits", "numeric",
                                         "phoneNumber", "autoText", "capitalize", "bufferType", "autoLink", "linksClickable",
                                         "cursorVisible", "freezesText", "marqueeRepeatLimit", "includeFontPadding",
                                         "fallbackLineSpacing", "breakStrategy", "hyphenationFrequency", "justificationMode",
                                         "lineBreakStyle", "lineBreakWordStyle", "scrollHorizontally", "gravity",
                                         "lineSpacingExtra", "lineSpacingMultiplier", "lineHeight", "firstBaselineToTopHeight",
                                         "lastBaselineToBottomHeight", "maxLines", "maxHeight", "lines", "height", "minLines",
                                         "minHeight", "maxEms", "maxWidth", "ems", "width", "minEms", "minWidth", "drawableLeft",
                                         "drawableTop", "drawableRight", "drawableBottom", "drawableStart", "drawableEnd",
                                         "drawablePadding", "drawableTint", "drawableTintMode", "autoSizeTextType",
                                         "autoSizeMinTextSize", "autoSizeMaxTextSize", "autoSizeStepGranularity",
                                         "autoSizePresetSizes", "textCursorDrawable", "enabled");
        }
        return sStyleable;
    }

    private static StyleableSet appearanceStyleable(Context context)
    {
        if (sAppearanceStyleable == null)
        {
            sAppearanceStyleable = StyleableSet.of(context.getResources(), "textSize", "textColor", "textColorHint", "textColorLink",
                                                   "textColorHighlight", "textStyle", "typeface", "fontFamily", "textFontWeight",
                                                   "textAllCaps", "shadowColor", "shadowDx", "shadowDy", "shadowRadius",
                                                   "elegantTextHeight", "letterSpacing", "fontFeatureSettings",
                                                   "fontVariationSettings", "textLocale", "textScaleX", "fallbackLineSpacing");
        }
        return sAppearanceStyleable;
    }

    private static final class Reader
    {
        private final TypedArray mArray;
        private final StyleableSet mSet;

        Reader(TypedArray array, StyleableSet set)
        {
            mArray = array;
            mSet = set;
        }

        boolean has(String name)
        {
            int index = mSet.indexOf(name);
            return index >= 0 && mArray.hasValue(index);
        }

        CharSequence text(String name)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? null : mArray.getText(index);
        }

        String string(String name)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? null : mArray.getString(index);
        }

        boolean bool(String name, boolean defaultValue)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? defaultValue : mArray.getBoolean(index, defaultValue);
        }

        int integer(String name, int defaultValue)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? defaultValue : mArray.getInt(index, defaultValue);
        }

        float floatValue(String name, float defaultValue)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? defaultValue : mArray.getFloat(index, defaultValue);
        }

        float dimension(String name, float defaultValue)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? defaultValue : mArray.getDimension(index, defaultValue);
        }

        int dimensionPixelSize(String name, int defaultValue)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? defaultValue : mArray.getDimensionPixelSize(index, defaultValue);
        }

        int color(String name, int defaultValue)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? defaultValue : mArray.getColor(index, defaultValue);
        }

        int resourceId(String name, int defaultValue)
        {
            int index = mSet.indexOf(name);
            return index < 0 ? defaultValue : mArray.getResourceId(index, defaultValue);
        }

        ColorStateList colorStateList(String name)
        {
            int index = mSet.indexOf(name);
            if (index < 0 || !mArray.hasValue(index)) return null;

            try
            {
                return mArray.getColorStateList(index);
            }
            catch (RuntimeException exception)
            {
                return ColorStateList.valueOf(mArray.getColor(index, 0));
            }
        }

        Drawable drawable(String name)
        {
            int index = mSet.indexOf(name);
            if (index < 0 || !mArray.hasValue(index)) return null;

            try
            {
                return mArray.getDrawable(index);
            }
            catch (RuntimeException exception)
            {
                return null;
            }
        }

        Typeface font(String name)
        {
            int index = mSet.indexOf(name);
            if (index < 0 || !mArray.hasValue(index)) return null;

            try
            {
                return mArray.getFont(index);
            }
            catch (RuntimeException exception)
            {
                return null;
            }
        }
    }

    private static final class Appearance
    {
        ColorStateList mTextColor;
        ColorStateList mTextColorHint;
        ColorStateList mTextColorLink;
        int mTextColorHighlight;
        boolean mHasTextColorHighlight;
        float mTextSize = -1;
        int mTextSizeUnit = -1;
        int mStyle = -1;
        int mTypefaceIndex = DEFAULT_TYPEFACE;
        String mFamily;
        Typeface mFont;
        int mFontWeight = -1;
        boolean mAllCaps;
        boolean mHasAllCaps;
        int mShadowColor;
        float mShadowDx;
        float mShadowDy;
        float mShadowRadius;
        boolean mHasElegant;
        boolean mElegant;
        boolean mHasLetterSpacing;
        float mLetterSpacing;
        String mFontFeatureSettings;
        String mFontVariationSettings;
        String mTextLocale;
        boolean mHasTextScaleX;
        float mTextScaleX = 1f;
        boolean mHasFallbackLineSpacing;
        boolean mFallbackLineSpacing;
    }

    private static void readAppearance(Context context, int appearanceId, Appearance appearance)
    {
        StyleableSet styleable = appearanceStyleable(context);
        if (styleable.isEmpty()) return;

        TypedArray array;

        try
        {
            array = context.obtainStyledAttributes(appearanceId, styleable.attributes());
        }
        catch (RuntimeException exception)
        {
            return;
        }

        if (array == null) return;

        readAppearance(new Reader(array, styleable), appearance);
        array.recycle();
    }

    private static void readAppearance(Reader reader, Appearance appearance)
    {
        if (reader.has("textColor")) appearance.mTextColor = reader.colorStateList("textColor");
        if (reader.has("textColorHint")) appearance.mTextColorHint = reader.colorStateList("textColorHint");
        if (reader.has("textColorLink")) appearance.mTextColorLink = reader.colorStateList("textColorLink");

        if (reader.has("textColorHighlight"))
        {
            appearance.mTextColorHighlight = reader.color("textColorHighlight", 0);
            appearance.mHasTextColorHighlight = true;
        }

        if (reader.has("textSize"))
        {
            appearance.mTextSize = reader.dimensionPixelSize("textSize", -1);
            appearance.mTextSizeUnit = TypedValue.COMPLEX_UNIT_PX;
        }

        if (reader.has("textStyle")) appearance.mStyle = reader.integer("textStyle", -1);
        if (reader.has("typeface")) appearance.mTypefaceIndex = reader.integer("typeface", DEFAULT_TYPEFACE);

        if (reader.has("fontFamily"))
        {
            Typeface font = reader.font("fontFamily");

            if (font != null)
            {
                appearance.mFont = font;
                appearance.mFamily = null;
            }
            else
            {
                appearance.mFamily = reader.string("fontFamily");
                appearance.mFont = null;
            }
        }

        if (reader.has("textFontWeight")) appearance.mFontWeight = reader.integer("textFontWeight", -1);

        if (reader.has("textAllCaps"))
        {
            appearance.mAllCaps = reader.bool("textAllCaps", false);
            appearance.mHasAllCaps = true;
        }

        if (reader.has("shadowColor")) appearance.mShadowColor = reader.color("shadowColor", 0);
        if (reader.has("shadowDx")) appearance.mShadowDx = reader.floatValue("shadowDx", 0f);
        if (reader.has("shadowDy")) appearance.mShadowDy = reader.floatValue("shadowDy", 0f);
        if (reader.has("shadowRadius")) appearance.mShadowRadius = reader.floatValue("shadowRadius", 0f);

        if (reader.has("elegantTextHeight"))
        {
            appearance.mHasElegant = true;
            appearance.mElegant = reader.bool("elegantTextHeight", false);
        }

        if (reader.has("letterSpacing"))
        {
            appearance.mHasLetterSpacing = true;
            appearance.mLetterSpacing = reader.floatValue("letterSpacing", 0f);
        }

        if (reader.has("fontFeatureSettings")) appearance.mFontFeatureSettings = reader.string("fontFeatureSettings");
        if (reader.has("fontVariationSettings")) appearance.mFontVariationSettings = reader.string("fontVariationSettings");
        if (reader.has("textLocale")) appearance.mTextLocale = reader.string("textLocale");

        if (reader.has("textScaleX"))
        {
            appearance.mHasTextScaleX = true;
            appearance.mTextScaleX = reader.floatValue("textScaleX", 1f);
        }

        if (reader.has("fallbackLineSpacing"))
        {
            appearance.mHasFallbackLineSpacing = true;
            appearance.mFallbackLineSpacing = reader.bool("fallbackLineSpacing", true);
        }
    }

    private void applyAppearance(Appearance appearance)
    {
        if (appearance.mTextColor != null) mTextColor = appearance.mTextColor;
        if (appearance.mTextColorHint != null) mHintTextColor = appearance.mTextColorHint;
        if (appearance.mTextColorLink != null) mLinkTextColor = appearance.mTextColorLink;
        if (appearance.mHasTextColorHighlight) mHighlightColor = appearance.mTextColorHighlight;

        if (appearance.mTextSize > 0)
        {
            mTextSizeUnit = appearance.mTextSizeUnit;
            setRawTextSize(appearance.mTextSize, true);
        }

        setTypefaceFromAttributes(appearance.mFont, appearance.mFamily, appearance.mTypefaceIndex, appearance.mStyle,
                                  appearance.mFontWeight);

        if (appearance.mShadowColor != 0)
        {
            setShadowLayer(appearance.mShadowRadius, appearance.mShadowDx, appearance.mShadowDy, appearance.mShadowColor);
        }

        if (appearance.mHasAllCaps) mAllCaps = appearance.mAllCaps;
        if (appearance.mHasElegant) mTextPaint.setElegantTextHeight(appearance.mElegant);
        if (appearance.mHasLetterSpacing) mTextPaint.setLetterSpacing(appearance.mLetterSpacing);
        if (appearance.mFontFeatureSettings != null) mTextPaint.setFontFeatureSettings(appearance.mFontFeatureSettings);
        if (appearance.mFontVariationSettings != null) mFontVariationSettings = appearance.mFontVariationSettings;
        if (appearance.mTextLocale != null) mTextPaint.setTextLocales(LocaleList.forLanguageTags(appearance.mTextLocale));
        if (appearance.mHasTextScaleX) mTextPaint.setTextScaleX(appearance.mTextScaleX);
        if (appearance.mHasFallbackLineSpacing) mUseFallbackLineSpacing = appearance.mFallbackLineSpacing;
    }

    private void setTypefaceFromAttributes(Typeface font, String family, int typefaceIndex, int style, int weight)
    {
        if (font == null && family != null)
        {
            resolveStyleAndSetTypeface(Typeface.create(family, Typeface.NORMAL), style, weight);
            return;
        }

        if (font != null)
        {
            resolveStyleAndSetTypeface(font, style, weight);
            return;
        }

        Typeface typeface = null;
        if (typefaceIndex == SANS) typeface = Typeface.SANS_SERIF;
        else if (typefaceIndex == SERIF) typeface = Typeface.SERIF;
        else if (typefaceIndex == MONOSPACE) typeface = Typeface.MONOSPACE;

        if (typeface == null && style <= 0 && weight < 0) return;

        resolveStyleAndSetTypeface(typeface, style, weight);
    }

    private void resolveStyleAndSetTypeface(Typeface typeface, int style, int weight)
    {
        if (weight >= 0)
        {
            int clamped = Math.min(1000, weight);
            boolean italic = (style & Typeface.ITALIC) != 0;
            setTypeface(Typeface.create(typeface == null ? Typeface.DEFAULT : typeface, clamped, italic));
            return;
        }

        setTypeface(typeface, Math.max(0, style));
    }

    private static PorterDuff.Mode parseTintMode(int value)
    {
        switch (value)
        {
            case 3:
                return PorterDuff.Mode.SRC_OVER;
            case 5:
                return PorterDuff.Mode.SRC_IN;
            case 9:
                return PorterDuff.Mode.SRC_ATOP;
            case 14:
                return PorterDuff.Mode.MULTIPLY;
            case 15:
                return PorterDuff.Mode.SCREEN;
            case 16:
                return PorterDuff.Mode.ADD;
            default:
                return null;
        }
    }

    private void setupAutoSizeUniformPresetSizes(Context context, int resourceId)
    {
        try
        {
            String[] sizes = context.getResources().getStringArray(resourceId);
            android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int[] values = new int[sizes.length];

            for (int index = 0; index < sizes.length; index++)
            {
                values[index] = parseDimension(sizes[index], metrics);
            }

            mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(values);
            mHasPresetAutoSizeValues = mAutoSizeTextSizesInPx.length > 0;
        }
        catch (RuntimeException exception)
        {
            mHasPresetAutoSizeValues = false;
        }
    }

    private static int parseDimension(String value, android.util.DisplayMetrics metrics)
    {
        if (value == null) return -1;

        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        int unit = TypedValue.COMPLEX_UNIT_PX;
        String number = trimmed;

        if (trimmed.endsWith("dip"))
        {
            unit = TypedValue.COMPLEX_UNIT_DIP;
            number = trimmed.substring(0, trimmed.length() - 3);
        }
        else if (trimmed.endsWith("dp"))
        {
            unit = TypedValue.COMPLEX_UNIT_DIP;
            number = trimmed.substring(0, trimmed.length() - 2);
        }
        else if (trimmed.endsWith("sp"))
        {
            unit = TypedValue.COMPLEX_UNIT_SP;
            number = trimmed.substring(0, trimmed.length() - 2);
        }
        else if (trimmed.endsWith("pt"))
        {
            unit = TypedValue.COMPLEX_UNIT_PT;
            number = trimmed.substring(0, trimmed.length() - 2);
        }
        else if (trimmed.endsWith("in"))
        {
            unit = TypedValue.COMPLEX_UNIT_IN;
            number = trimmed.substring(0, trimmed.length() - 2);
        }
        else if (trimmed.endsWith("mm"))
        {
            unit = TypedValue.COMPLEX_UNIT_MM;
            number = trimmed.substring(0, trimmed.length() - 2);
        }
        else if (trimmed.endsWith("px"))
        {
            number = trimmed.substring(0, trimmed.length() - 2);
        }

        try
        {
            return Math.round(TypedValue.applyDimension(unit, Float.parseFloat(number), metrics));
        }
        catch (NumberFormatException exception)
        {
            return -1;
        }
    }

    public void setAutoSizeTextTypeWithDefaults(int autoSizeTextType)
    {
        if (autoSizeTextType == AUTO_SIZE_TEXT_TYPE_NONE)
        {
            clearAutoSizeConfiguration();
            return;
        }

        if (autoSizeTextType != AUTO_SIZE_TEXT_TYPE_UNIFORM)
        {
            throw new IllegalArgumentException("Unknown auto-size text type: " + autoSizeTextType);
        }

        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        float minimum = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP, metrics);
        float maximum = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP, metrics);

        mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_UNIFORM;
        validateAndSetAutoSizeTextTypeUniformConfiguration(minimum, maximum, DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX);

        if (setupAutoSizeText())
        {
            autoSizeText();
            invalidate();
        }
    }

    public void setAutoSizeTextTypeUniformWithConfiguration(int autoSizeMinTextSize, int autoSizeMaxTextSize, int autoSizeStepGranularity,
                                                            int unit)
    {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        float minimum = TypedValue.applyDimension(unit, autoSizeMinTextSize, metrics);
        float maximum = TypedValue.applyDimension(unit, autoSizeMaxTextSize, metrics);
        float step = TypedValue.applyDimension(unit, autoSizeStepGranularity, metrics);

        mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_UNIFORM;
        validateAndSetAutoSizeTextTypeUniformConfiguration(minimum, maximum, step);

        if (setupAutoSizeText())
        {
            autoSizeText();
            invalidate();
        }
    }

    public void setAutoSizeTextTypeUniformWithPresetSizes(int[] presetSizes, int unit)
    {
        int length = presetSizes.length;

        if (length > 0)
        {
            int[] sizes = new int[length];

            if (unit == TypedValue.COMPLEX_UNIT_PX)
            {
                sizes = Arrays.copyOf(presetSizes, length);
            }
            else
            {
                android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                for (int index = 0; index < length; index++)
                {
                    sizes[index] = Math.round(TypedValue.applyDimension(unit, presetSizes[index], metrics));
                }
            }

            mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(sizes);
            if (mAutoSizeTextSizesInPx.length == 0)
            {
                throw new IllegalArgumentException("None of the preset sizes is valid: " + Arrays.toString(presetSizes));
            }

            mHasPresetAutoSizeValues = true;
        }
        else
        {
            mHasPresetAutoSizeValues = false;
        }

        mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_UNIFORM;

        if (setupAutoSizeText())
        {
            autoSizeText();
            invalidate();
        }
    }

    public int getAutoSizeTextType()
    {
        return mAutoSizeTextType;
    }

    public int getAutoSizeStepGranularity()
    {
        return Math.round(mAutoSizeStepGranularityInPx);
    }

    public int getAutoSizeMinTextSize()
    {
        return Math.round(mAutoSizeMinTextSizeInPx);
    }

    public int getAutoSizeMaxTextSize()
    {
        return Math.round(mAutoSizeMaxTextSizeInPx);
    }

    public int[] getAutoSizeTextAvailableSizes()
    {
        return mAutoSizeTextSizesInPx;
    }

    private void clearAutoSizeConfiguration()
    {
        mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_NONE;
        mAutoSizeMinTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        mAutoSizeMaxTextSizeInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        mAutoSizeStepGranularityInPx = UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE;
        mAutoSizeTextSizesInPx = new int[0];
        mNeedsAutoSizeText = false;
    }

    private int[] cleanupAutoSizePresetSizes(int[] presetValues)
    {
        if (presetValues.length == 0) return presetValues;

        int[] sorted = presetValues.clone();
        Arrays.sort(sorted);

        ArrayList<Integer> unique = new ArrayList<>();
        for (int value : sorted)
        {
            if (value > 0 && !unique.contains(value)) unique.add(value);
        }

        int[] result = new int[unique.size()];
        for (int index = 0; index < result.length; index++)
        {
            result[index] = unique.get(index);
        }

        return result;
    }

    private void validateAndSetAutoSizeTextTypeUniformConfiguration(float minimum, float maximum, float step)
    {
        if (minimum <= 0) throw new IllegalArgumentException("Minimum auto-size text size (" + minimum + "px) is less or equal to (0px)");
        if (maximum <= minimum)
        {
            throw new IllegalArgumentException("Maximum auto-size text size (" + maximum + "px) is less or equal to minimum auto-size "
                                               + "text size (" + minimum + "px)");
        }

        if (step <= 0) throw new IllegalArgumentException("The auto-size step granularity (" + step + "px) is less or equal to (0px)");

        mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_UNIFORM;
        mAutoSizeMinTextSizeInPx = minimum;
        mAutoSizeMaxTextSizeInPx = maximum;
        mAutoSizeStepGranularityInPx = step;
        mHasPresetAutoSizeValues = false;
    }

    private boolean setupAutoSizeText()
    {
        if (supportsAutoSizeText() && mAutoSizeTextType == AUTO_SIZE_TEXT_TYPE_UNIFORM)
        {
            if (!mHasPresetAutoSizeValues || mAutoSizeTextSizesInPx.length == 0)
            {
                int count = (int)Math.floor((mAutoSizeMaxTextSizeInPx - mAutoSizeMinTextSizeInPx) / mAutoSizeStepGranularityInPx) + 1;
                int[] sizes = new int[Math.max(0, count)];

                for (int index = 0; index < sizes.length; index++)
                {
                    sizes[index] = Math.round(mAutoSizeMinTextSizeInPx + (index * mAutoSizeStepGranularityInPx));
                }

                mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(sizes);
            }

            mNeedsAutoSizeText = true;
        }
        else
        {
            mNeedsAutoSizeText = false;
        }

        return mNeedsAutoSizeText;
    }

    private boolean supportsAutoSizeText()
    {
        return !(this instanceof EditText);
    }

    private boolean isAutoSizeEnabled()
    {
        return supportsAutoSizeText() && mAutoSizeTextType != AUTO_SIZE_TEXT_TYPE_NONE;
    }

    private void autoSizeText()
    {
        if (!isAutoSizeEnabled()) return;

        if (mNeedsAutoSizeText)
        {
            if (getMeasuredWidth() <= 0 || getMeasuredHeight() <= 0) return;

            int availableWidth = mHorizontallyScrolling ? VERY_WIDE : getMeasuredWidth() - getTotalPaddingLeft() - getTotalPaddingRight();
            int availableHeight = getMeasuredHeight() - getExtendedPaddingBottom() - getExtendedPaddingTop();

            if (availableWidth <= 0 || availableHeight <= 0) return;

            float optimal = findLargestTextSizeWhichFits(availableWidth, availableHeight);

            if (optimal != getTextSize())
            {
                setRawTextSize(optimal, false);
                makeNewLayout(availableWidth, 0, getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight(), false);
            }
        }

        mNeedsAutoSizeText = true;
    }

    private float findLargestTextSizeWhichFits(int width, int height)
    {
        int count = mAutoSizeTextSizesInPx.length;
        if (count == 0) return getTextSize();

        int bestIndex = 0;
        int low = bestIndex + 1;
        int high = count - 1;

        while (low <= high)
        {
            int middle = (low + high) / 2;

            if (suggestedSizeFitsInSpace(mAutoSizeTextSizesInPx[middle], width, height))
            {
                bestIndex = low;
                low = middle + 1;
            }
            else
            {
                high = middle - 1;
                bestIndex = high;
            }
        }

        return mAutoSizeTextSizesInPx[Math.max(0, bestIndex)];
    }

    private boolean suggestedSizeFitsInSpace(int suggestedSize, int width, int height)
    {
        CharSequence text = mTransformed == null ? getText() : mTransformed;
        int maxLines = getMaxLines();

        TextPaint paint = new TextPaint(mTextPaint);
        paint.setTextSize(suggestedSize);

        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, width).setAlignment(getLayoutAlignment())
                                  .setLineSpacing(mSpacingAdd, mSpacingMultiplier).setIncludePad(mIncludePad)
                                  .setUseLineSpacingFromFallbacks(mUseFallbackLineSpacing).setBreakStrategy(mBreakStrategy)
                                  .setHyphenationFrequency(mHyphenationFrequency).setJustificationMode(mJustificationMode)
                                  .setMaxLines(maxLines == -1 ? Integer.MAX_VALUE : maxLines).build();

        if (maxLines != -1 && (layout.getLineCount() > maxLines || layout.getLineEnd(layout.getLineCount() - 1) != text.length()))
        {
            return false;
        }

        return layout.getHeight() <= height;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        if (enabled == isEnabled()) return;

        super.setEnabled(enabled);

        if (!enabled) hideSoftInput();

        invalidate();
    }

    public void setTypeface(Typeface typeface, int style)
    {
        if (style > 0)
        {
            if (typeface == null) typeface = Typeface.defaultFromStyle(style);
            else typeface = Typeface.create(typeface, style);

            setTypeface(typeface);

            int typefaceStyle = typeface != null ? typeface.getStyle() : 0;
            int need = style & ~typefaceStyle;

            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        }
        else
        {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setTypeface(typeface);
        }
    }

    protected boolean getDefaultEditable()
    {
        return false;
    }

    protected MovementMethod getDefaultMovementMethod()
    {
        return null;
    }

    public CharSequence getText()
    {
        return mText;
    }

    public int length()
    {
        return mText.length();
    }

    public Editable getEditableText()
    {
        return mText instanceof Editable ? (Editable)mText : null;
    }

    public int getLineHeight()
    {
        return Math.round(mTextPaint.getFontMetricsInt(null) * mSpacingMultiplier + mSpacingAdd);
    }

    public final Layout getLayout()
    {
        return mLayout;
    }

    final Layout getHintLayout()
    {
        return mHintLayout;
    }

    public final KeyListener getKeyListener()
    {
        return mKeyListener;
    }

    public void setKeyListener(KeyListener input)
    {
        setKeyListenerOnly(input);
        fixFocusableAndClickableSettings();

        if (input != null)
        {
            try
            {
                mInputType = input.getInputType();
            }
            catch (IncompatibleClassChangeError error)
            {
                mInputType = InputType.TYPE_CLASS_TEXT;
            }

            setInputTypeSingleLine(mSingleLine);
        }
        else
        {
            mInputType = InputType.TYPE_NULL;
        }

        restartInput();
    }

    private void setKeyListenerOnly(KeyListener input)
    {
        if (mKeyListener == input) return;

        mKeyListener = input;

        if (input != null && !(mText instanceof Editable)) setText(mText);
        if (mText instanceof Editable) setFilters((Editable)mText, mFilters);
    }

    public final MovementMethod getMovementMethod()
    {
        return mMovement;
    }

    public final void setMovementMethod(MovementMethod movement)
    {
        if (mMovement == movement) return;

        mMovement = movement;

        if (movement != null && !(mText instanceof Spannable)) setText(mText);

        fixFocusableAndClickableSettings();
    }

    private void fixFocusableAndClickableSettings()
    {
        if (mMovement != null || mKeyListener != null)
        {
            setFocusable(true);
            setClickable(true);
            setLongClickable(true);
            return;
        }

        if (mTextIsSelectable) return;

        setFocusable(false);
        setClickable(false);
        setLongClickable(false);
    }

    public final TransformationMethod getTransformationMethod()
    {
        return mTransformation;
    }

    public final void setTransformationMethod(TransformationMethod method)
    {
        if (method == mTransformation) return;

        if (mTransformation != null && mText instanceof Spannable) ((Spannable)mText).removeSpan(mTransformation);

        mTransformation = method;
        mAllowTransformationLengthChange = false;

        setText(mText);
    }

    public int getCompoundPaddingTop()
    {
        Drawable drawable = mDrawables[1];
        if (drawable == null) return getPaddingTop();

        return getPaddingTop() + mDrawablePadding + drawable.getBounds().height();
    }

    public int getCompoundPaddingBottom()
    {
        Drawable drawable = mDrawables[3];
        if (drawable == null) return getPaddingBottom();

        return getPaddingBottom() + mDrawablePadding + drawable.getBounds().height();
    }

    public int getCompoundPaddingLeft()
    {
        Drawable drawable = mDrawables[0];
        if (drawable == null) return getPaddingLeft();

        return getPaddingLeft() + mDrawablePadding + drawable.getBounds().width();
    }

    public int getCompoundPaddingRight()
    {
        Drawable drawable = mDrawables[2];
        if (drawable == null) return getPaddingRight();

        return getPaddingRight() + mDrawablePadding + drawable.getBounds().width();
    }

    public int getCompoundPaddingStart()
    {
        return isRtl() ? getCompoundPaddingRight() : getCompoundPaddingLeft();
    }

    public int getCompoundPaddingEnd()
    {
        return isRtl() ? getCompoundPaddingLeft() : getCompoundPaddingRight();
    }

    private boolean isRtl()
    {
        return getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    public int getExtendedPaddingTop()
    {
        if (mMaxMode != LINES) return getCompoundPaddingTop();
        if (mLayout == null) assumeLayout();
        if (mLayout.getLineCount() <= mMaximum) return getCompoundPaddingTop();

        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewHeight = getHeight() - top - bottom;
        int layoutHeight = mLayout.getLineTop(mMaximum);

        if (layoutHeight >= viewHeight) return top;

        int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.TOP) return top;
        if (gravity == Gravity.BOTTOM) return top + viewHeight - layoutHeight;

        return top + (viewHeight - layoutHeight) / 2;
    }

    public int getExtendedPaddingBottom()
    {
        if (mMaxMode != LINES) return getCompoundPaddingBottom();
        if (mLayout == null) assumeLayout();
        if (mLayout.getLineCount() <= mMaximum) return getCompoundPaddingBottom();

        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewHeight = getHeight() - top - bottom;
        int layoutHeight = mLayout.getLineTop(mMaximum);

        if (layoutHeight >= viewHeight) return bottom;

        int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.TOP) return bottom + viewHeight - layoutHeight;
        if (gravity == Gravity.BOTTOM) return bottom;

        return bottom + (viewHeight - layoutHeight) / 2;
    }

    public int getTotalPaddingLeft()
    {
        return getCompoundPaddingLeft();
    }

    public int getTotalPaddingRight()
    {
        return getCompoundPaddingRight();
    }

    public int getTotalPaddingStart()
    {
        return getCompoundPaddingStart();
    }

    public int getTotalPaddingEnd()
    {
        return getCompoundPaddingEnd();
    }

    public int getTotalPaddingTop()
    {
        return getExtendedPaddingTop() + getVerticalOffset(true);
    }

    public int getTotalPaddingBottom()
    {
        return getExtendedPaddingBottom() + getBottomVerticalOffset(true);
    }

    private Drawable[] compoundDrawables()
    {
        return mDrawables == null ? new Drawable[0] : mDrawables;
    }

    private void prepareDrawable(Drawable drawable)
    {
        if (drawable == null) return;

        drawable.setCallback(this);

        if (mHasDrawableTint || mHasDrawableTintMode)
        {
            drawable.mutate();
            if (mHasDrawableTint) drawable.setTintList(mDrawableTintList);
            if (mHasDrawableTintMode) drawable.setTintMode(mDrawableTintMode);
        }

        if (drawable.isStateful()) drawable.setState(getDrawableState());
    }

    public void setCompoundDrawables(Drawable left, Drawable top, Drawable right, Drawable bottom)
    {
        for (Drawable existing : mDrawables)
        {
            if (existing != null && existing != left && existing != top && existing != right && existing != bottom)
            {
                existing.setCallback(null);
            }
        }

        mDrawables[0] = left;
        mDrawables[1] = top;
        mDrawables[2] = right;
        mDrawables[3] = bottom;
        mDrawableStart = null;
        mDrawableEnd = null;
        mDrawablesRelative = false;

        prepareDrawable(left);
        prepareDrawable(top);
        prepareDrawable(right);
        prepareDrawable(bottom);

        invalidate();
        requestLayout();
    }

    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom)
    {
        Context context = getContext();
        setCompoundDrawablesWithIntrinsicBounds(left != 0 ? context.getDrawable(left) : null, top != 0 ? context.getDrawable(top) : null,
                                                right != 0 ? context.getDrawable(right) : null,
                                                bottom != 0 ? context.getDrawable(bottom) : null);
    }

    private static void setIntrinsicBounds(Drawable drawable)
    {
        if (drawable == null) return;

        drawable.setBounds(0, 0, Math.max(0, drawable.getIntrinsicWidth()), Math.max(0, drawable.getIntrinsicHeight()));
    }

    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom)
    {
        setIntrinsicBounds(left);
        setIntrinsicBounds(right);
        setIntrinsicBounds(top);
        setIntrinsicBounds(bottom);

        setCompoundDrawables(left, top, right, bottom);
    }

    public void setCompoundDrawablesRelative(Drawable start, Drawable top, Drawable end, Drawable bottom)
    {
        boolean rtl = isRtl();
        setCompoundDrawables(rtl ? end : start, top, rtl ? start : end, bottom);

        mDrawableStart = start;
        mDrawableEnd = end;
        mDrawablesRelative = true;
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(int start, int top, int end, int bottom)
    {
        Context context = getContext();
        setCompoundDrawablesRelativeWithIntrinsicBounds(start != 0 ? context.getDrawable(start) : null,
                                                        top != 0 ? context.getDrawable(top) : null,
                                                        end != 0 ? context.getDrawable(end) : null,
                                                        bottom != 0 ? context.getDrawable(bottom) : null);
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start, Drawable top, Drawable end, Drawable bottom)
    {
        setIntrinsicBounds(start);
        setIntrinsicBounds(end);
        setIntrinsicBounds(top);
        setIntrinsicBounds(bottom);

        setCompoundDrawablesRelative(start, top, end, bottom);
    }

    private void setRelativeDrawablesIfNeeded(Drawable start, Drawable end)
    {
        setIntrinsicBounds(start);
        setIntrinsicBounds(end);

        Drawable left = mDrawables[0];
        Drawable right = mDrawables[2];
        boolean rtl = isRtl();

        if (start != null)
        {
            if (rtl) right = start;
            else left = start;
        }

        if (end != null)
        {
            if (rtl) left = end;
            else right = end;
        }

        Drawable top = mDrawables[1];
        Drawable bottom = mDrawables[3];
        setCompoundDrawables(left, top, right, bottom);

        mDrawableStart = start;
        mDrawableEnd = end;
        mDrawablesRelative = start != null || end != null;
    }

    public Drawable[] getCompoundDrawables()
    {
        return new Drawable[] {mDrawables[0], mDrawables[1], mDrawables[2], mDrawables[3]};
    }

    public Drawable[] getCompoundDrawablesRelative()
    {
        if (mDrawablesRelative) return new Drawable[] {mDrawableStart, mDrawables[1], mDrawableEnd, mDrawables[3]};

        boolean rtl = isRtl();
        return new Drawable[] {rtl ? mDrawables[2] : mDrawables[0], mDrawables[1], rtl ? mDrawables[0] : mDrawables[2], mDrawables[3]};
    }

    public void setCompoundDrawablePadding(int padding)
    {
        mDrawablePadding = padding;

        invalidate();
        requestLayout();
    }

    public int getCompoundDrawablePadding()
    {
        return mDrawablePadding;
    }

    public void setCompoundDrawableTintList(ColorStateList tint)
    {
        mDrawableTintList = tint;
        mHasDrawableTint = true;
        applyCompoundDrawableTint();
    }

    public ColorStateList getCompoundDrawableTintList()
    {
        return mDrawableTintList;
    }

    public void setCompoundDrawableTintMode(PorterDuff.Mode tintMode)
    {
        mDrawableTintMode = tintMode;
        mHasDrawableTintMode = true;
        applyCompoundDrawableTint();
    }

    public void setCompoundDrawableTintBlendMode(BlendMode blendMode)
    {
        mDrawableTintBlendMode = blendMode;

        for (Drawable drawable : mDrawables)
        {
            if (drawable == null) continue;

            drawable.mutate();
            drawable.setTintBlendMode(blendMode);
        }

        invalidate();
    }

    public PorterDuff.Mode getCompoundDrawableTintMode()
    {
        return mDrawableTintMode;
    }

    public BlendMode getCompoundDrawableTintBlendMode()
    {
        return mDrawableTintBlendMode;
    }

    private void applyCompoundDrawableTint()
    {
        for (Drawable drawable : mDrawables)
        {
            prepareDrawable(drawable);
        }

        invalidate();
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom)
    {
        if (left != getPaddingLeft() || right != getPaddingRight() || top != getPaddingTop() || bottom != getPaddingBottom()) nullLayouts();

        super.setPadding(left, top, right, bottom);
        invalidate();
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom)
    {
        if (start != getPaddingStart() || end != getPaddingEnd() || top != getPaddingTop() || bottom != getPaddingBottom()) nullLayouts();

        super.setPaddingRelative(start, top, end, bottom);
        invalidate();
    }

    public void setFirstBaselineToTopHeight(int firstBaselineToTopHeight)
    {
        if (firstBaselineToTopHeight < 0) throw new IllegalArgumentException("firstBaselineToTopHeight must be non-negative");

        mFirstBaselineToTopHeight = firstBaselineToTopHeight;

        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        int fontMetricsTop = getIncludeFontPadding() ? metrics.top : metrics.ascent;

        if (firstBaselineToTopHeight > Math.abs(fontMetricsTop))
        {
            int paddingTop = firstBaselineToTopHeight - (-fontMetricsTop);
            setPadding(getPaddingLeft(), paddingTop, getPaddingRight(), getPaddingBottom());
        }
    }

    public void setLastBaselineToBottomHeight(int lastBaselineToBottomHeight)
    {
        if (lastBaselineToBottomHeight < 0) throw new IllegalArgumentException("lastBaselineToBottomHeight must be non-negative");

        mLastBaselineToBottomHeight = lastBaselineToBottomHeight;

        Paint.FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        int fontMetricsBottom = getIncludeFontPadding() ? metrics.bottom : metrics.descent;

        if (lastBaselineToBottomHeight > Math.abs(fontMetricsBottom))
        {
            int paddingBottom = lastBaselineToBottomHeight - fontMetricsBottom;
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), paddingBottom);
        }
    }

    public int getFirstBaselineToTopHeight()
    {
        return getPaddingTop() - mTextPaint.getFontMetricsInt().top;
    }

    public int getLastBaselineToBottomHeight()
    {
        return getPaddingBottom() + mTextPaint.getFontMetricsInt().bottom;
    }

    public final int getAutoLinkMask()
    {
        return mAutoLinkMask;
    }

    public void setTextSelectHandle(Drawable textSelectHandle)
    {
        mTextSelectHandle = textSelectHandle;
    }

    public void setTextSelectHandle(int textSelectHandle)
    {
        setTextSelectHandle(getContext().getDrawable(textSelectHandle));
    }

    public Drawable getTextSelectHandle()
    {
        return mTextSelectHandle;
    }

    public void setTextSelectHandleLeft(Drawable textSelectHandleLeft)
    {
        mTextSelectHandleLeft = textSelectHandleLeft;
    }

    public void setTextSelectHandleLeft(int textSelectHandleLeft)
    {
        setTextSelectHandleLeft(getContext().getDrawable(textSelectHandleLeft));
    }

    public Drawable getTextSelectHandleLeft()
    {
        return mTextSelectHandleLeft;
    }

    public void setTextSelectHandleRight(Drawable textSelectHandleRight)
    {
        mTextSelectHandleRight = textSelectHandleRight;
    }

    public void setTextSelectHandleRight(int textSelectHandleRight)
    {
        setTextSelectHandleRight(getContext().getDrawable(textSelectHandleRight));
    }

    public Drawable getTextSelectHandleRight()
    {
        return mTextSelectHandleRight;
    }

    public void setTextCursorDrawable(Drawable textCursorDrawable)
    {
        mTextCursorDrawable = textCursorDrawable;
        mTextCursorDrawableResource = 0;
        invalidate();
    }

    public void setTextCursorDrawable(int textCursorDrawable)
    {
        setTextCursorDrawable(textCursorDrawable != 0 ? getContext().getDrawable(textCursorDrawable) : null);
        mTextCursorDrawableResource = textCursorDrawable;
    }

    public Drawable getTextCursorDrawable()
    {
        if (mTextCursorDrawable == null && mTextCursorDrawableResource != 0)
        {
            try
            {
                mTextCursorDrawable = getContext().getDrawable(mTextCursorDrawableResource);
            }
            catch (RuntimeException exception)
            {
                mTextCursorDrawableResource = 0;
            }
        }

        return mTextCursorDrawable;
    }

    public void setTextAppearance(int resourceId)
    {
        setTextAppearance(getContext(), resourceId);
    }

    public void setTextAppearance(Context context, int resourceId)
    {
        Appearance appearance = new Appearance();
        readAppearance(context, resourceId, appearance);

        if (appearance.mTextColor != null) setTextColor(appearance.mTextColor);
        if (appearance.mTextColorHint != null) setHintTextColor(appearance.mTextColorHint);
        if (appearance.mTextColorLink != null) setLinkTextColor(appearance.mTextColorLink);
        if (appearance.mHasTextColorHighlight) setHighlightColor(appearance.mTextColorHighlight);

        if (appearance.mTextSize > 0)
        {
            mTextSizeUnit = appearance.mTextSizeUnit;
            setRawTextSize(appearance.mTextSize, true);
        }

        setTypefaceFromAttributes(appearance.mFont, appearance.mFamily, appearance.mTypefaceIndex, appearance.mStyle,
                                  appearance.mFontWeight);

        if (appearance.mShadowColor != 0)
        {
            setShadowLayer(appearance.mShadowRadius, appearance.mShadowDx, appearance.mShadowDy, appearance.mShadowColor);
        }

        if (appearance.mHasAllCaps) setAllCaps(appearance.mAllCaps);
        if (appearance.mHasElegant) setElegantTextHeight(appearance.mElegant);
        if (appearance.mHasLetterSpacing) setLetterSpacing(appearance.mLetterSpacing);
        if (appearance.mFontFeatureSettings != null) setFontFeatureSettings(appearance.mFontFeatureSettings);
        if (appearance.mFontVariationSettings != null) setFontVariationSettings(appearance.mFontVariationSettings);
        if (appearance.mTextLocale != null) setTextLocales(LocaleList.forLanguageTags(appearance.mTextLocale));
    }

    public Locale getTextLocale()
    {
        return mTextPaint.getTextLocale();
    }

    public LocaleList getTextLocales()
    {
        return mTextPaint.getTextLocales();
    }

    public void setTextLocale(Locale locale)
    {
        mTextPaint.setTextLocale(locale);
        relayout();
    }

    public void setTextLocales(LocaleList locales)
    {
        mTextPaint.setTextLocales(locales);
        relayout();
    }

    @Override
    protected void onConfigurationChanged(android.content.res.Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        relayout();
    }

    public float getTextSize()
    {
        return mTextPaint.getTextSize();
    }

    public void setTextSize(float size)
    {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setTextSize(int unit, float size)
    {
        if (isAutoSizeEnabled()) return;

        mTextSizeUnit = unit;
        setRawTextSize(TypedValue.applyDimension(unit, size, getResources().getDisplayMetrics()), true);
    }

    public int getTextSizeUnit()
    {
        return mTextSizeUnit;
    }

    private void setRawTextSize(float size, boolean shouldRequestLayout)
    {
        if (size == mTextPaint.getTextSize()) return;

        mTextPaint.setTextSize(size);

        if (shouldRequestLayout && mLayout != null)
        {
            mNeedsAutoSizeText = false;
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public float getTextScaleX()
    {
        return mTextPaint.getTextScaleX();
    }

    public void setTextScaleX(float size)
    {
        if (size == mTextPaint.getTextScaleX()) return;

        mTextPaint.setTextScaleX(size);
        relayout();
    }

    public void setTypeface(Typeface typeface)
    {
        if (mTextPaint.getTypeface() == typeface) return;

        mTextPaint.setTypeface(typeface);

        if (mLayout != null)
        {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public Typeface getTypeface()
    {
        return mTextPaint.getTypeface();
    }

    public void setElegantTextHeight(boolean elegant)
    {
        if (elegant == mTextPaint.isElegantTextHeight()) return;

        mTextPaint.setElegantTextHeight(elegant);
        relayout();
    }

    public void setFallbackLineSpacing(boolean enabled)
    {
        if (mUseFallbackLineSpacing == enabled) return;

        mUseFallbackLineSpacing = enabled;
        relayout();
    }

    public boolean isFallbackLineSpacing()
    {
        return mUseFallbackLineSpacing;
    }

    public void setUseBoundsForWidth(boolean useBoundsForWidth)
    {
        if (mUseBoundsForWidth == useBoundsForWidth) return;

        mUseBoundsForWidth = useBoundsForWidth;
        relayout();
    }

    public boolean getUseBoundsForWidth()
    {
        return mUseBoundsForWidth;
    }

    public void setShiftDrawingOffsetForStartOverhang(boolean shiftDrawingOffsetForStartOverhang)
    {
        if (mShiftDrawingOffsetForStartOverhang == shiftDrawingOffsetForStartOverhang) return;

        mShiftDrawingOffsetForStartOverhang = shiftDrawingOffsetForStartOverhang;
        invalidate();
    }

    public boolean getShiftDrawingOffsetForStartOverhang()
    {
        return mShiftDrawingOffsetForStartOverhang;
    }

    public void setMinimumFontMetrics(Paint.FontMetrics minimumFontMetrics)
    {
        mMinimumFontMetrics = minimumFontMetrics;
        relayout();
    }

    public Paint.FontMetrics getMinimumFontMetrics()
    {
        return mMinimumFontMetrics;
    }

    public void setLocalePreferredLineHeightForMinimumUsed(boolean flag)
    {
        if (mLocalePreferredLineHeightForMinimumUsed == flag) return;

        mLocalePreferredLineHeightForMinimumUsed = flag;
        relayout();
    }

    public boolean isLocalePreferredLineHeightForMinimumUsed()
    {
        return mLocalePreferredLineHeightForMinimumUsed;
    }

    private boolean mUseBoundsForWidth;
    private boolean mShiftDrawingOffsetForStartOverhang;
    private Paint.FontMetrics mMinimumFontMetrics;
    private boolean mLocalePreferredLineHeightForMinimumUsed;

    public boolean isElegantTextHeight()
    {
        return mTextPaint.isElegantTextHeight();
    }

    public float getLetterSpacing()
    {
        return mTextPaint.getLetterSpacing();
    }

    public void setLetterSpacing(float letterSpacing)
    {
        if (letterSpacing == mTextPaint.getLetterSpacing()) return;

        mTextPaint.setLetterSpacing(letterSpacing);
        relayout();
    }

    public String getFontFeatureSettings()
    {
        return mTextPaint.getFontFeatureSettings();
    }

    public String getFontVariationSettings()
    {
        return mFontVariationSettings;
    }

    public void setBreakStrategy(int breakStrategy)
    {
        mBreakStrategy = breakStrategy;
        relayout();
    }

    public int getBreakStrategy()
    {
        return mBreakStrategy;
    }

    public void setHyphenationFrequency(int hyphenationFrequency)
    {
        mHyphenationFrequency = hyphenationFrequency;
        relayout();
    }

    public int getHyphenationFrequency()
    {
        return mHyphenationFrequency;
    }

    public void setLineBreakStyle(int lineBreakStyle)
    {
        if (mLineBreakStyle == lineBreakStyle) return;

        mLineBreakStyle = lineBreakStyle;
        relayout();
    }

    public void setLineBreakWordStyle(int lineBreakWordStyle)
    {
        if (mLineBreakWordStyle == lineBreakWordStyle) return;

        mLineBreakWordStyle = lineBreakWordStyle;
        relayout();
    }

    public int getLineBreakStyle()
    {
        return mLineBreakStyle;
    }

    public int getLineBreakWordStyle()
    {
        return mLineBreakWordStyle;
    }

    public PrecomputedText.Params getTextMetricsParams()
    {
        return new PrecomputedText.Params.Builder(new TextPaint(mTextPaint)).setBreakStrategy(mBreakStrategy)
            .setHyphenationFrequency(mHyphenationFrequency).setTextDirection(getTextDirectionHeuristic()).build();
    }

    public void setTextMetricsParams(PrecomputedText.Params params)
    {
        mTextPaint.set(params.getTextPaint());
        mBreakStrategy = params.getBreakStrategy();
        mHyphenationFrequency = params.getHyphenationFrequency();
        relayout();
    }

    public void setJustificationMode(int justificationMode)
    {
        mJustificationMode = justificationMode;
        relayout();
    }

    public int getJustificationMode()
    {
        return mJustificationMode;
    }

    public void setFontFeatureSettings(String fontFeatureSettings)
    {
        if (TextUtils.equals(fontFeatureSettings, mTextPaint.getFontFeatureSettings())) return;

        mTextPaint.setFontFeatureSettings(fontFeatureSettings);
        relayout();
    }

    public boolean setFontVariationSettings(String fontVariationSettings)
    {
        if (TextUtils.equals(fontVariationSettings, mFontVariationSettings)) return true;

        mFontVariationSettings = fontVariationSettings;
        relayout();
        return true;
    }

    private void relayout()
    {
        if (mLayout == null) return;

        nullLayouts();
        requestLayout();
        invalidate();
    }

    public void setTextColor(int color)
    {
        mTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public void setTextColor(ColorStateList colors)
    {
        if (colors == null) throw new NullPointerException();

        mTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getTextColors()
    {
        return mTextColor;
    }

    public final int getCurrentTextColor()
    {
        return mCurTextColor;
    }

    public void setHighlightColor(int color)
    {
        if (mHighlightColor == color) return;

        mHighlightColor = color;
        invalidate();
    }

    public int getHighlightColor()
    {
        return mHighlightColor;
    }

    public final void setShowSoftInputOnFocus(boolean show)
    {
        mShowSoftInputOnFocus = show;
    }

    public final boolean getShowSoftInputOnFocus()
    {
        return mShowSoftInputOnFocus;
    }

    public void setShadowLayer(float radius, float dx, float dy, int color)
    {
        mTextPaint.setShadowLayer(radius, dx, dy, color);

        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;
        mShadowColor = color;

        invalidate();
    }

    public float getShadowRadius()
    {
        return mShadowRadius;
    }

    public float getShadowDx()
    {
        return mShadowDx;
    }

    public float getShadowDy()
    {
        return mShadowDy;
    }

    public int getShadowColor()
    {
        return mShadowColor;
    }

    public TextPaint getPaint()
    {
        return mTextPaint;
    }

    public final void setAutoLinkMask(int mask)
    {
        mAutoLinkMask = mask;
    }

    public final void setLinksClickable(boolean whether)
    {
        mLinksClickable = whether;
    }

    public final boolean getLinksClickable()
    {
        return mLinksClickable;
    }

    public URLSpan[] getUrls()
    {
        if (mText instanceof Spanned) return ((Spanned)mText).getSpans(0, mText.length(), URLSpan.class);

        return new URLSpan[0];
    }

    public final void setHintTextColor(int color)
    {
        mHintTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public final void setHintTextColor(ColorStateList colors)
    {
        mHintTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getHintTextColors()
    {
        return mHintTextColor;
    }

    public final int getCurrentHintTextColor()
    {
        return mHintTextColor != null ? mCurHintTextColor : mCurTextColor;
    }

    public final void setLinkTextColor(int color)
    {
        mLinkTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public final void setLinkTextColor(ColorStateList colors)
    {
        mLinkTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getLinkTextColors()
    {
        return mLinkTextColor;
    }

    private void updateTextColors()
    {
        boolean changed = false;
        int[] state = getDrawableState();

        int color = mTextColor == null ? mCurTextColor : mTextColor.getColorForState(state, mTextColor.getDefaultColor());
        if (color != mCurTextColor)
        {
            mCurTextColor = color;
            changed = true;
        }

        if (mLinkTextColor != null)
        {
            color = mLinkTextColor.getColorForState(state, mLinkTextColor.getDefaultColor());
            if (color != mTextPaint.linkColor)
            {
                mTextPaint.linkColor = color;
                changed = true;
            }
        }
        else if (mTextPaint.linkColor == 0)
        {
            mTextPaint.linkColor = 0xFF0000EE;
        }

        if (mHintTextColor != null)
        {
            color = mHintTextColor.getColorForState(state, mHintTextColor.getDefaultColor());
            if (color != mCurHintTextColor)
            {
                mCurHintTextColor = color;
                if (mText.length() == 0) changed = true;
            }
        }

        if (changed) invalidate();
    }

    @Override
    protected void drawableStateChanged()
    {
        super.drawableStateChanged();

        if ((mTextColor != null && mTextColor.isStateful()) || (mHintTextColor != null && mHintTextColor.isStateful())
            || (mLinkTextColor != null && mLinkTextColor.isStateful()))
        {
            updateTextColors();
        }

        int[] state = getDrawableState();
        for (Drawable drawable : compoundDrawables())
        {
            if (drawable != null && drawable.isStateful() && drawable.setState(state)) invalidateDrawable(drawable);
        }
    }

    @Override
    public void drawableHotspotChanged(float x, float y)
    {
        super.drawableHotspotChanged(x, y);

        for (Drawable drawable : compoundDrawables())
        {
            if (drawable != null) drawable.setHotspot(x, y);
        }
    }

    public void setGravity(int gravity)
    {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) gravity |= Gravity.START;
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) gravity |= Gravity.TOP;

        boolean newLayout = (gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) != (mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK);

        if (gravity != mGravity) invalidate();

        mGravity = gravity;

        if (mLayout != null && newLayout)
        {
            int want = mLayout.getWidth();
            int hintWant = mHintLayout == null ? 0 : mHintLayout.getWidth();

            makeNewLayout(want, hintWant, getRight() - getLeft() - getCompoundPaddingLeft() - getCompoundPaddingRight(), true);
        }
    }

    public int getGravity()
    {
        return mGravity;
    }

    public int getPaintFlags()
    {
        return mTextPaint.getFlags();
    }

    public void setPaintFlags(int flags)
    {
        if (mTextPaint.getFlags() == flags) return;

        mTextPaint.setFlags(flags);
        relayout();
    }

    public void setHorizontallyScrolling(boolean whether)
    {
        if (mHorizontallyScrolling == whether) return;

        mHorizontallyScrolling = whether;
        relayout();
    }

    public final boolean isHorizontallyScrollable()
    {
        return mHorizontallyScrolling;
    }

    private void setMinLinesInternal(int minLines)
    {
        mMinimum = minLines;
        mMinMode = LINES;
    }

    public void setMinLines(int minLines)
    {
        setMinLinesInternal(minLines);
        requestLayout();
        invalidate();
    }

    public int getMinLines()
    {
        return mMinMode == LINES ? mMinimum : -1;
    }

    private void setMinHeightInternal(int minPixels)
    {
        mMinimum = minPixels;
        mMinMode = PIXELS;
    }

    public void setMinHeight(int minPixels)
    {
        setMinHeightInternal(minPixels);
        requestLayout();
        invalidate();
    }

    public int getMinHeight()
    {
        return mMinMode == PIXELS ? mMinimum : -1;
    }

    private void setMaxLinesInternal(int maxLines)
    {
        mMaximum = maxLines;
        mMaxMode = LINES;
    }

    public void setMaxLines(int maxLines)
    {
        setMaxLinesInternal(maxLines);
        nullLayouts();
        requestLayout();
        invalidate();
    }

    public int getMaxLines()
    {
        return mMaxMode == LINES ? mMaximum : -1;
    }

    private void setMaxHeightInternal(int maxPixels)
    {
        mMaximum = maxPixels;
        mMaxMode = PIXELS;
    }

    public void setMaxHeight(int maxPixels)
    {
        setMaxHeightInternal(maxPixels);
        requestLayout();
        invalidate();
    }

    public int getMaxHeight()
    {
        return mMaxMode == PIXELS ? mMaximum : -1;
    }

    private void setLinesInternal(int lines)
    {
        mMaximum = mMinimum = lines;
        mMaxMode = mMinMode = LINES;
    }

    public void setLines(int lines)
    {
        setLinesInternal(lines);
        nullLayouts();
        requestLayout();
        invalidate();
    }

    private void setHeightInternal(int pixels)
    {
        mMaximum = mMinimum = pixels;
        mMaxMode = mMinMode = PIXELS;
    }

    public void setHeight(int pixels)
    {
        setHeightInternal(pixels);
        requestLayout();
        invalidate();
    }

    private void setMinEmsInternal(int minEms)
    {
        mMinWidthValue = minEms;
        mMinWidthMode = EMS;
    }

    public void setMinEms(int minEms)
    {
        setMinEmsInternal(minEms);
        requestLayout();
        invalidate();
    }

    public int getMinEms()
    {
        return mMinWidthMode == EMS ? mMinWidthValue : -1;
    }

    private void setMinWidthInternal(int minPixels)
    {
        mMinWidthValue = minPixels;
        mMinWidthMode = PIXELS;
    }

    public void setMinWidth(int minPixels)
    {
        setMinWidthInternal(minPixels);
        requestLayout();
        invalidate();
    }

    public int getMinWidth()
    {
        return mMinWidthMode == PIXELS ? mMinWidthValue : -1;
    }

    private void setMaxEmsInternal(int maxEms)
    {
        mMaxWidthValue = maxEms;
        mMaxWidthMode = EMS;
    }

    public void setMaxEms(int maxEms)
    {
        setMaxEmsInternal(maxEms);
        requestLayout();
        invalidate();
    }

    public int getMaxEms()
    {
        return mMaxWidthMode == EMS ? mMaxWidthValue : -1;
    }

    private void setMaxWidthInternal(int maxPixels)
    {
        mMaxWidthValue = maxPixels;
        mMaxWidthMode = PIXELS;
    }

    public void setMaxWidth(int maxPixels)
    {
        setMaxWidthInternal(maxPixels);
        requestLayout();
        invalidate();
    }

    public int getMaxWidth()
    {
        return mMaxWidthMode == PIXELS ? mMaxWidthValue : -1;
    }

    private void setEmsInternal(int ems)
    {
        mMaxWidthValue = mMinWidthValue = ems;
        mMaxWidthMode = mMinWidthMode = EMS;
    }

    public void setEms(int ems)
    {
        setEmsInternal(ems);
        requestLayout();
        invalidate();
    }

    private void setWidthInternal(int pixels)
    {
        mMaxWidthValue = mMinWidthValue = pixels;
        mMaxWidthMode = mMinWidthMode = PIXELS;
    }

    public void setWidth(int pixels)
    {
        setWidthInternal(pixels);
        requestLayout();
        invalidate();
    }

    public void setLineSpacing(float add, float multiplier)
    {
        if (mSpacingAdd == add && mSpacingMultiplier == multiplier) return;

        mSpacingAdd = add;
        mSpacingMultiplier = multiplier;
        relayout();
    }

    public float getLineSpacingMultiplier()
    {
        return mSpacingMultiplier;
    }

    public float getLineSpacingExtra()
    {
        return mSpacingAdd;
    }

    public void setLineHeight(int lineHeight)
    {
        if (lineHeight < 0) throw new IllegalArgumentException("lineHeight must be non-negative");

        int fontHeight = mTextPaint.getFontMetricsInt(null);
        if (lineHeight != fontHeight) setLineSpacing(lineHeight - fontHeight, 1f);
    }

    public void setLineHeight(int unit, float lineHeight)
    {
        setLineHeight(Math.round(TypedValue.applyDimension(unit, lineHeight, getResources().getDisplayMetrics())));
    }

    public void setHighlights(android.text.Highlights highlights)
    {
        mHighlights = highlights;
        invalidate();
    }

    public android.text.Highlights getHighlights()
    {
        return mHighlights;
    }

    public void setSearchResultHighlights(int... ranges)
    {
        if (ranges != null && ranges.length % 2 == 1) throw new IllegalArgumentException("Flatten ranges must have even numbered elements");

        mSearchResultHighlights = ranges;
        invalidate();
    }

    public int[] getSearchResultHighlights()
    {
        return mSearchResultHighlights;
    }

    public void setFocusedSearchResultIndex(int index)
    {
        mFocusedSearchResultIndex = index;
        invalidate();
    }

    public int getFocusedSearchResultIndex()
    {
        return mFocusedSearchResultIndex;
    }

    public void setSearchResultHighlightColor(int color)
    {
        mSearchResultHighlightColor = color;
        invalidate();
    }

    public int getSearchResultHighlightColor()
    {
        return mSearchResultHighlightColor;
    }

    public void setFocusedSearchResultHighlightColor(int color)
    {
        mFocusedSearchResultHighlightColor = color;
        invalidate();
    }

    public int getFocusedSearchResultHighlightColor()
    {
        return mFocusedSearchResultHighlightColor;
    }

    public final void append(CharSequence text)
    {
        append(text, 0, text.length());
    }

    public void append(CharSequence text, int start, int end)
    {
        if (!(mText instanceof Editable)) setText(mText, BufferType.EDITABLE);

        ((Editable)mText).append(text, start, end);

        if (mAutoLinkMask != 0)
        {
            boolean linksWereAdded = Linkify.addLinks((Spannable)mText, mAutoLinkMask);
            if (linksWereAdded && mLinksClickable && !textCanBeSelected()) setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        Parcelable superState = super.onSaveInstanceState();

        boolean freezesText = getFreezesText();
        boolean hasSelection = false;
        int start = -1;
        int end = -1;

        if (mText != null)
        {
            start = getSelectionStart();
            end = getSelectionEnd();
            if (start >= 0 || end >= 0) hasSelection = true;
        }

        if (freezesText || hasSelection)
        {
            SavedState state = new SavedState(superState);

            if (freezesText)
            {
                if (mText instanceof Spanned)
                {
                    Spannable spannable = new SpannableStringBuilder(mText);
                    state.text = spannable;
                }
                else
                {
                    state.text = mText.toString();
                }
            }

            if (hasSelection)
            {
                state.selStart = start;
                state.selEnd = end;
            }

            state.error = getError();
            return state;
        }

        return superState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        if (!(state instanceof SavedState))
        {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState saved = (SavedState)state;
        super.onRestoreInstanceState(saved.getSuperState());

        if (saved.text != null) setText(saved.text);

        if (saved.selStart >= 0 && saved.selEnd >= 0 && mText instanceof Spannable)
        {
            int length = mText.length();

            if (saved.selStart <= length && saved.selEnd <= length) Selection.setSelection((Spannable)mText, saved.selStart, saved.selEnd);
        }

        if (saved.error != null) setError(saved.error);
    }

    public void setFreezesText(boolean freezesText)
    {
        mFreezesText = freezesText;
    }

    public boolean getFreezesText()
    {
        return mFreezesText;
    }

    public final void setEditableFactory(Editable.Factory factory)
    {
        mEditableFactory = factory;
        setText(mText);
    }

    public final void setSpannableFactory(Spannable.Factory factory)
    {
        mSpannableFactory = factory;
        setText(mText);
    }

    public final void setText(CharSequence text)
    {
        setText(text, mBufferType);
    }

    public final void setTextKeepState(CharSequence text)
    {
        setTextKeepState(text, mBufferType);
    }

    public void setText(CharSequence text, BufferType type)
    {
        setText(text, type, true, 0);
    }

    private void setText(CharSequence text, BufferType type, boolean notifyBefore, int oldLength)
    {
        if (text == null) text = "";

        if (!isSuggestionsEnabled()) text = removeSuggestionSpans(text);

        for (InputFilter filter : mFilters)
        {
            CharSequence filtered = filter.filter(text, 0, text.length(), EMPTY_SPANNED, 0, 0);
            if (filtered != null) text = filtered;
        }

        if (notifyBefore)
        {
            if (mText != null)
            {
                oldLength = mText.length();
                sendBeforeTextChanged(mText, 0, oldLength, text.length());
            }
            else
            {
                sendBeforeTextChanged("", 0, 0, text.length());
            }
        }

        boolean needEditableForNotification = !mListeners.isEmpty();
        boolean restart = false;

        if (type == BufferType.EDITABLE || mKeyListener != null || needEditableForNotification)
        {
            Editable editable = mEditableFactory.newEditable(text);
            text = editable;
            setFilters(editable, mFilters);
            restart = true;
        }
        else if (!(text instanceof PrecomputedText))
        {
            if (type == BufferType.SPANNABLE || mMovement != null) text = mSpannableFactory.newSpannable(text);
            else if (!(text instanceof CharWrapper)) text = TextUtils.stringOrSpannedString(text);
        }

        if (mAutoLinkMask != 0)
        {
            Spannable spannable;

            if (type == BufferType.EDITABLE || text instanceof Spannable) spannable = (Spannable)text;
            else spannable = mSpannableFactory.newSpannable(text);

            if (Linkify.addLinks(spannable, mAutoLinkMask))
            {
                text = spannable;
                type = type == BufferType.EDITABLE ? BufferType.EDITABLE : BufferType.SPANNABLE;
                mText = text;

                if (mLinksClickable && !textCanBeSelected()) setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        mBufferType = type;
        mText = text;

        updateTransformed();

        int textLength = text.length();

        if (text instanceof Spannable && !mAllowTransformationLengthChange)
        {
            Spannable spannable = (Spannable)text;

            ChangeWatcher[] watchers = spannable.getSpans(0, spannable.length(), ChangeWatcher.class);
            for (ChangeWatcher watcher : watchers)
            {
                spannable.removeSpan(watcher);
            }

            if (mChangeWatcher == null) mChangeWatcher = new ChangeWatcher();

            spannable.setSpan(mChangeWatcher, 0, textLength,
                              Spanned.SPAN_INCLUSIVE_INCLUSIVE | (CHANGE_WATCHER_PRIORITY << Spanned.SPAN_PRIORITY_SHIFT));

            if (mTransformation != null) spannable.setSpan(mTransformation, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            if (mMovement != null)
            {
                mMovement.initialize(this, (Spannable)text);
            }
        }

        nullLayouts();
        requestLayout();
        invalidate();

        if (restart) restartInput();

        sendOnTextChanged(text, 0, oldLength, textLength);
        onTextChanged(text, 0, oldLength, textLength);

        if (needEditableForNotification) sendAfterTextChanged((Editable)text);
    }

    private void updateTransformed()
    {
        if (mTransformation == null) mTransformed = mText;
        else mTransformed = mTransformation.getTransformation(mText, this);

        if (mTransformed == null) mTransformed = "";
    }

    private static CharSequence removeSuggestionSpans(CharSequence text)
    {
        if (!(text instanceof Spanned)) return text;

        Spannable spannable;
        if (text instanceof Spannable) spannable = (Spannable)text;
        else spannable = new SpannableString(text);

        SuggestionSpan[] spans = spannable.getSpans(0, text.length(), SuggestionSpan.class);
        if (spans.length == 0) return text;

        for (SuggestionSpan span : spans)
        {
            spannable.removeSpan(span);
        }

        return spannable;
    }

    public final void setText(char[] text, int start, int length)
    {
        int oldLength = 0;

        if (start < 0 || length < 0 || start + length > text.length)
        {
            throw new IndexOutOfBoundsException(start + ", " + length);
        }

        if (mText != null)
        {
            oldLength = mText.length();
            sendBeforeTextChanged(mText, 0, oldLength, length);
        }
        else
        {
            sendBeforeTextChanged("", 0, 0, length);
        }

        setText(new CharWrapper(text, start, length), mBufferType, false, oldLength);
    }

    public final void setTextKeepState(CharSequence text, BufferType type)
    {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        int length = text.length();

        setText(text, type);

        if ((start >= 0 || end >= 0) && mText instanceof Spannable)
        {
            Selection.setSelection((Spannable)mText, Math.max(0, Math.min(start, length)), Math.max(0, Math.min(end, length)));
        }
    }

    public final void setText(int resourceId)
    {
        setText(getContext().getResources().getText(resourceId));
    }

    public final void setText(int resourceId, BufferType type)
    {
        setText(getContext().getResources().getText(resourceId), type);
    }

    public final void setHint(CharSequence hint)
    {
        mHint = hint == null ? null : TextUtils.stringOrSpannedString(hint);

        if (mLayout != null) checkForRelayout();

        if (mText.length() == 0) invalidate();
    }

    public final void setHint(int resourceId)
    {
        setHint(getContext().getResources().getText(resourceId));
    }

    public CharSequence getHint()
    {
        return mHint;
    }

    public boolean isSingleLine()
    {
        return mSingleLine;
    }

    private static boolean isMultilineInputType(int type)
    {
        return (type & (InputType.TYPE_MASK_CLASS | InputType.TYPE_TEXT_FLAG_MULTI_LINE))
               == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    private static boolean isPasswordInputType(int inputType)
    {
        int variation = inputType & (InputType.TYPE_MASK_CLASS | InputType.TYPE_MASK_VARIATION);

        return variation == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
               || variation == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
               || variation == (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }

    private static boolean isVisiblePasswordInputType(int inputType)
    {
        int variation = inputType & (InputType.TYPE_MASK_CLASS | InputType.TYPE_MASK_VARIATION);

        return variation == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    }

    public void setInputType(int type)
    {
        boolean wasPassword = isPasswordInputType(getInputType());
        boolean wasVisiblePassword = isVisiblePasswordInputType(getInputType());

        setInputType(type, false);

        boolean isPassword = isPasswordInputType(type);
        boolean isVisiblePassword = isVisiblePasswordInputType(type);
        boolean forceUpdate = false;

        if (isPassword)
        {
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            setTypeface(Typeface.MONOSPACE);
        }
        else if (isVisiblePassword)
        {
            if (mTransformation == PasswordTransformationMethod.getInstance()) forceUpdate = true;
            setTypeface(Typeface.MONOSPACE);
        }
        else if (wasPassword || wasVisiblePassword)
        {
            setTypeface(null);
            if (mTransformation == PasswordTransformationMethod.getInstance()) forceUpdate = true;
        }

        boolean singleLine = !isMultilineInputType(type);

        if (mSingleLine != singleLine || forceUpdate) applySingleLine(singleLine, !isPassword, true, true);

        if (!isSuggestionsEnabled()) mText = removeSuggestionSpans(mText);

        restartInput();
    }

    private void setInputType(int type, boolean direct)
    {
        int typeClass = type & InputType.TYPE_MASK_CLASS;
        KeyListener input;

        if (typeClass == InputType.TYPE_CLASS_TEXT)
        {
            boolean autoText = (type & InputType.TYPE_TEXT_FLAG_AUTO_CORRECT) != 0;
            TextKeyListener.Capitalize capitalize;

            if ((type & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) capitalize = TextKeyListener.Capitalize.CHARACTERS;
            else if ((type & InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0) capitalize = TextKeyListener.Capitalize.WORDS;
            else if ((type & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) capitalize = TextKeyListener.Capitalize.SENTENCES;
            else capitalize = TextKeyListener.Capitalize.NONE;

            input = TextKeyListener.getInstance(autoText, capitalize);
        }
        else if (typeClass == InputType.TYPE_CLASS_NUMBER)
        {
            input = DigitsKeyListener.getInstance(null, (type & InputType.TYPE_NUMBER_FLAG_SIGNED) != 0,
                                                  (type & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0);
        }
        else if (typeClass == InputType.TYPE_CLASS_DATETIME)
        {
            int variation = type & InputType.TYPE_MASK_VARIATION;

            if (variation == InputType.TYPE_DATETIME_VARIATION_DATE) input = DateKeyListener.getInstance();
            else if (variation == InputType.TYPE_DATETIME_VARIATION_TIME) input = TimeKeyListener.getInstance();
            else input = DateTimeKeyListener.getInstance();
        }
        else if (typeClass == InputType.TYPE_CLASS_PHONE)
        {
            input = DialerKeyListener.getInstance();
        }
        else
        {
            input = TextKeyListener.getInstance();
        }

        setRawInputType(type);

        if (direct) mKeyListener = input;
        else setKeyListenerOnly(input);
    }

    public void setRawInputType(int type)
    {
        if (type == InputType.TYPE_NULL && mKeyListener == null) return;

        mInputType = type;
    }

    public String[] getAutofillHints()
    {
        return super.getAutofillHints();
    }

    public int getInputType()
    {
        return mInputType;
    }

    public void setImeOptions(int imeOptions)
    {
        mImeOptions = imeOptions;
    }

    public int getImeOptions()
    {
        return mImeOptions;
    }

    public void setImeActionLabel(CharSequence label, int actionId)
    {
        mImeActionLabel = label;
        mImeActionId = actionId;
    }

    public CharSequence getImeActionLabel()
    {
        return mImeActionLabel;
    }

    public int getImeActionId()
    {
        return mImeActionId;
    }

    public void setOnEditorActionListener(OnEditorActionListener listener)
    {
        mEditorActionListener = listener;
    }

    public void onEditorAction(int actionCode)
    {
        if (mEditorActionListener != null && mEditorActionListener.onEditorAction(this, actionCode, null)) return;

        if (actionCode == EditorInfo.IME_ACTION_NEXT)
        {
            View next = focusSearch(View.FOCUS_FORWARD);
            if (next != null) next.requestFocus(View.FOCUS_FORWARD);
            return;
        }

        if (actionCode == EditorInfo.IME_ACTION_PREVIOUS)
        {
            View previous = focusSearch(View.FOCUS_BACKWARD);
            if (previous != null) previous.requestFocus(View.FOCUS_BACKWARD);
            return;
        }

        if (actionCode == EditorInfo.IME_ACTION_DONE)
        {
            hideSoftInput();
            return;
        }

        long time = android.os.SystemClock.uptimeMillis();
        int flags = KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE | KeyEvent.FLAG_EDITOR_ACTION;

        dispatchKeyEvent(new KeyEvent(time, time, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0, -1, 0, flags));
        dispatchKeyEvent(new KeyEvent(time, time, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0, 0, -1, 0, flags));
    }

    public void setPrivateImeOptions(String type)
    {
        mPrivateImeOptions = type;
    }

    public String getPrivateImeOptions()
    {
        return mPrivateImeOptions;
    }

    public void setInputExtras(int xmlResId) throws java.io.IOException, org.xmlpull.v1.XmlPullParserException
    {
        if (mInputExtras == null) mInputExtras = new Bundle();
    }

    public Bundle getInputExtras(boolean create)
    {
        if (mInputExtras == null && create) mInputExtras = new Bundle();

        return mInputExtras;
    }

    public void setImeHintLocales(LocaleList hintLocales)
    {
        mImeHintLocales = hintLocales;
        restartInput();
    }

    public LocaleList getImeHintLocales()
    {
        return mImeHintLocales;
    }

    public CharSequence getError()
    {
        return mError;
    }

    public void setError(CharSequence error)
    {
        if (error == null)
        {
            setError(null, null);
            return;
        }

        setError(error, mErrorDrawable);
    }

    public void setError(CharSequence error, Drawable icon)
    {
        if (icon != null) icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());

        mError = TextUtils.stringOrSpannedString(error);
        mErrorDrawable = icon;
        invalidate();
    }

    public void setFilters(InputFilter[] filters)
    {
        if (filters == null) throw new IllegalArgumentException();

        mFilters = filters;

        if (mText instanceof Editable) setFilters((Editable)mText, filters);
    }

    private void setFilters(Editable editable, InputFilter[] filters)
    {
        if (mKeyListener instanceof InputFilter)
        {
            InputFilter[] combined = new InputFilter[filters.length + 1];
            System.arraycopy(filters, 0, combined, 0, filters.length);
            combined[filters.length] = (InputFilter)mKeyListener;
            editable.setFilters(combined);
            return;
        }

        editable.setFilters(filters);
    }

    public InputFilter[] getFilters()
    {
        return mFilters;
    }

    @Override
    public boolean onPreDraw()
    {
        if (mLayout == null) assumeLayout();

        if (mMovement != null)
        {
            int cursor = getSelectionEnd();
            if (cursor < 0 && (mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) cursor = mText.length();
            if (cursor >= 0) bringPointIntoView(cursor);
        }
        else
        {
            bringTextIntoView();
        }

        unregisterForPreDraw();
        return true;
    }

    private void registerForPreDraw()
    {
        if (mPreDrawRegistered) return;

        getViewTreeObserver().addOnPreDrawListener(this);
        mPreDrawRegistered = true;
    }

    private void unregisterForPreDraw()
    {
        if (!mPreDrawRegistered) return;

        getViewTreeObserver().removeOnPreDrawListener(this);
        mPreDrawRegistered = false;
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (isFocused() && isCursorBlinkEnabled()) startBlink();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        unregisterForPreDraw();
        stopBlink();
    }

    @Override
    public void onScreenStateChanged(int screenState)
    {
        super.onScreenStateChanged(screenState);
    }

    protected boolean setFrame(int left, int top, int right, int bottom)
    {
        boolean changed = getLeft() != left || getTop() != top || getRight() != right || getBottom() != bottom;

        if (changed && mLayout != null && (getRight() - getLeft()) != (right - left)) nullLayouts();

        return super.setFrame(left, top, right, bottom);
    }

    protected boolean isPaddingOffsetRequired()
    {
        return mShadowRadius != 0;
    }

    protected int getLeftPaddingOffset()
    {
        return getCompoundPaddingLeft() - getPaddingLeft() + (int)Math.min(0, mShadowDx - mShadowRadius);
    }

    protected int getTopPaddingOffset()
    {
        return (int)Math.min(0, mShadowDy - mShadowRadius);
    }

    protected int getBottomPaddingOffset()
    {
        return (int)Math.max(0, mShadowDy + mShadowRadius);
    }

    protected int getRightPaddingOffset()
    {
        return -(getCompoundPaddingRight() - getPaddingRight()) + (int)Math.max(0, mShadowDx + mShadowRadius);
    }

    @Override
    protected boolean verifyDrawable(Drawable who)
    {
        if (super.verifyDrawable(who)) return true;

        for (Drawable drawable : compoundDrawables())
        {
            if (who == drawable) return true;
        }

        return false;
    }

    @Override
    public void jumpDrawablesToCurrentState()
    {
        super.jumpDrawablesToCurrentState();

        for (Drawable drawable : compoundDrawables())
        {
            if (drawable != null) drawable.jumpToCurrentState();
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable)
    {
        boolean handled = false;

        for (Drawable compound : compoundDrawables())
        {
            if (compound == drawable)
            {
                handled = true;
                break;
            }
        }

        if (handled) invalidate();
        else super.invalidateDrawable(drawable);
    }

    @Override
    public boolean hasOverlappingRendering()
    {
        return getBackground() != null && getBackground().getCurrent() != null || mText instanceof Spannable || hasSelection();
    }

    public boolean isTextSelectable()
    {
        return mTextIsSelectable;
    }

    public void setTextIsSelectable(boolean selectable)
    {
        if (!selectable && mTextIsSelectable == selectable) return;

        mTextIsSelectable = selectable;
        setFocusableInTouchMode(selectable);
        setFocusable(selectable);
        setClickable(selectable);
        setLongClickable(selectable);

        setMovementMethod(selectable ? ArrowKeyMovementMethod.getInstance() : null);
        setText(mText, selectable ? BufferType.SPANNABLE : BufferType.NORMAL);
    }

    private boolean textCanBeSelected()
    {
        if (mMovement == null || !mMovement.canSelectArbitrarily()) return false;

        return isTextEditable() || (isTextSelectable() && mText instanceof Spannable && isEnabled());
    }

    private boolean isTextEditable()
    {
        return mText instanceof Editable && onCheckIsTextEditor() && isEnabled();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace)
    {
        return super.onCreateDrawableState(extraSpace);
    }

    private Path mHighlightPath;

    @Override
    protected void onDraw(Canvas canvas)
    {
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        int right = getRight();
        int left = getLeft();
        int bottom = getBottom();
        int top = getTop();

        int compoundPaddingLeft = getCompoundPaddingLeft();
        int compoundPaddingTop = getCompoundPaddingTop();
        int compoundPaddingRight = getCompoundPaddingRight();
        int compoundPaddingBottom = getCompoundPaddingBottom();

        int verticalSpace = bottom - top - compoundPaddingBottom - compoundPaddingTop;
        int horizontalSpace = right - left - compoundPaddingRight - compoundPaddingLeft;

        drawCompoundDrawables(canvas, scrollX, scrollY, verticalSpace, horizontalSpace, compoundPaddingTop, compoundPaddingLeft);

        int color = mCurTextColor;

        if (mLayout == null) assumeLayout();

        Layout layout = mLayout;

        if (mHint != null && mText.length() == 0)
        {
            if (mHintTextColor != null) color = mCurHintTextColor;
            if (mHintLayout != null) layout = mHintLayout;
        }

        mTextPaint.setColor(color);
        mTextPaint.drawableState = getDrawableState();

        if (layout == null) return;

        int saveCount = canvas.save();

        int extendedPaddingTop = getExtendedPaddingTop();
        int extendedPaddingBottom = getExtendedPaddingBottom();

        int maxScrollY = mLayout.getHeight() - verticalSpace;

        float clipLeft = compoundPaddingLeft + scrollX;
        float clipTop = scrollY == 0 ? 0 : extendedPaddingTop + scrollY;
        float clipRight = right - left - compoundPaddingRight + scrollX;
        float clipBottom = bottom - top + scrollY - (scrollY == maxScrollY ? 0 : extendedPaddingBottom);

        if (mShadowRadius != 0)
        {
            clipLeft += Math.min(0, mShadowDx - mShadowRadius);
            clipRight += Math.max(0, mShadowDx + mShadowRadius);
            clipTop += Math.min(0, mShadowDy - mShadowRadius);
            clipBottom += Math.max(0, mShadowDy + mShadowRadius);
        }

        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom);

        int verticalOffsetText = 0;
        int verticalOffsetCursor = 0;

        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP)
        {
            verticalOffsetText = getVerticalOffset(false);
            verticalOffsetCursor = getVerticalOffset(true);
        }

        canvas.translate(compoundPaddingLeft, extendedPaddingTop + verticalOffsetText);

        Path highlight = getUpdatedHighlightPath();
        drawSearchHighlights(canvas, layout);

        if (highlight != null) layout.draw(canvas, highlight, mHighlightPaint, verticalOffsetCursor - verticalOffsetText);
        else layout.draw(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawSearchHighlights(Canvas canvas, Layout layout)
    {
        if (layout != mLayout) return;

        Path path = new Path();

        if (mHighlights != null)
        {
            int length = mText.length();

            for (int index = 0; index < mHighlights.getSize(); index++)
            {
                int[] ranges = mHighlights.getRanges(index);
                Paint paint = mHighlights.getPaint(index);

                for (int range = 0; range + 1 < ranges.length; range += 2)
                {
                    int start = Math.max(0, Math.min(ranges[range], length));
                    int end = Math.max(0, Math.min(ranges[range + 1], length));
                    if (start >= end) continue;

                    layout.getSelectionPath(start, end, path);
                    canvas.drawPath(path, paint);
                }
            }
        }

        if (mSearchResultHighlights == null) return;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int length = mText.length();

        for (int index = 0; index + 1 < mSearchResultHighlights.length; index += 2)
        {
            int start = Math.max(0, Math.min(mSearchResultHighlights[index], length));
            int end = Math.max(0, Math.min(mSearchResultHighlights[index + 1], length));
            if (start >= end) continue;

            boolean focused = index / 2 == mFocusedSearchResultIndex;
            paint.setColor(focused ? mFocusedSearchResultHighlightColor : mSearchResultHighlightColor);

            layout.getSelectionPath(start, end, path);
            canvas.drawPath(path, paint);
        }
    }

    private Path getUpdatedHighlightPath()
    {
        if (mMovement == null || !(isFocused() || isPressed()) || !(mText instanceof Spannable)) return null;

        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        if (selectionStart < 0 || selectionEnd < 0) return null;

        if (mHighlightPath == null) mHighlightPath = new Path();

        if (selectionStart == selectionEnd)
        {
            if (!isTextEditable() || !mCursorVisible || !mCursorBlinkOn) return null;

            mHighlightPath.reset();

            int line = mLayout.getLineForOffset(selectionStart);
            int lineTop = mLayout.getLineTop(line);
            int lineBottom = mLayout.getLineBottom(line);
            float horizontal = mLayout.getPrimaryHorizontal(selectionStart);
            float width = Math.max(1f, 2f * mTextPaint.density) / 2f;

            mHighlightPath.addRect(horizontal - width, lineTop, horizontal + width, lineBottom, Path.Direction.CW);
            mHighlightPaint.setColor(mCurTextColor);
            mHighlightPaint.setStyle(Paint.Style.FILL);
            return mHighlightPath;
        }

        mLayout.getSelectionPath(selectionStart, selectionEnd, mHighlightPath);
        mHighlightPaint.setColor(mHighlightColor);
        mHighlightPaint.setStyle(Paint.Style.FILL);
        return mHighlightPath;
    }

    private void drawCompoundDrawables(Canvas canvas, int scrollX, int scrollY, int verticalSpace, int horizontalSpace,
                                       int compoundPaddingTop, int compoundPaddingLeft)
    {
        Drawable left = mDrawables[0];
        Drawable top = mDrawables[1];
        Drawable right = mDrawables[2];
        Drawable bottom = mDrawables[3];

        if (left != null)
        {
            int saveCount = canvas.save();
            canvas.translate(scrollX + getPaddingLeft(), scrollY + compoundPaddingTop + (verticalSpace - left.getBounds().height()) / 2);
            left.draw(canvas);
            canvas.restoreToCount(saveCount);
        }

        if (right != null)
        {
            int saveCount = canvas.save();
            canvas.translate(scrollX + getRight() - getLeft() - getPaddingRight() - right.getBounds().width(),
                             scrollY + compoundPaddingTop + (verticalSpace - right.getBounds().height()) / 2);
            right.draw(canvas);
            canvas.restoreToCount(saveCount);
        }

        if (top != null)
        {
            int saveCount = canvas.save();
            canvas.translate(scrollX + compoundPaddingLeft + (horizontalSpace - top.getBounds().width()) / 2, scrollY + getPaddingTop());
            top.draw(canvas);
            canvas.restoreToCount(saveCount);
        }

        if (bottom != null)
        {
            int saveCount = canvas.save();
            canvas.translate(scrollX + compoundPaddingLeft + (horizontalSpace - bottom.getBounds().width()) / 2,
                             scrollY + getBottom() - getTop() - getPaddingBottom() - bottom.getBounds().height());
            bottom.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    @Override
    public void getFocusedRect(Rect rect)
    {
        if (mLayout == null)
        {
            super.getFocusedRect(rect);
            return;
        }

        int selectionEnd = getSelectionEnd();
        if (selectionEnd < 0)
        {
            super.getFocusedRect(rect);
            return;
        }

        int line = mLayout.getLineForOffset(selectionEnd);
        rect.top = mLayout.getLineTop(line);
        rect.bottom = mLayout.getLineBottom(line);
        rect.left = (int)mLayout.getPrimaryHorizontal(selectionEnd) - 2;
        rect.right = rect.left + 4;

        int paddingLeft = getCompoundPaddingLeft();
        int paddingTop = getExtendedPaddingTop();
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) paddingTop += getVerticalOffset(false);

        rect.offset(paddingLeft, paddingTop);
    }

    public int getLineCount()
    {
        return mLayout != null ? mLayout.getLineCount() : 0;
    }

    public int getLineBounds(int line, Rect bounds)
    {
        if (mLayout == null)
        {
            if (bounds != null) bounds.set(0, 0, 0, 0);
            return 0;
        }

        int baseline = mLayout.getLineBounds(line, bounds);
        int verticalOffset = getExtendedPaddingTop();

        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) verticalOffset += getVerticalOffset(true);
        if (bounds != null) bounds.offset(getCompoundPaddingLeft(), verticalOffset);

        return baseline + verticalOffset;
    }

    @Override
    public int getBaseline()
    {
        if (mLayout == null) return super.getBaseline();

        return getBaselineOffset() + mLayout.getLineBaseline(0);
    }

    private int getBaselineOffset()
    {
        int verticalOffset = 0;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) verticalOffset = getVerticalOffset(true);

        return getExtendedPaddingTop() + verticalOffset;
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex)
    {
        return super.onResolvePointerIcon(event, pointerIndex);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        int which = doKeyDown(keyCode, event);
        if (which == 0) return super.onKeyDown(keyCode, event);

        return true;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event)
    {
        KeyEvent down = KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN);
        int which = doKeyDown(keyCode, down);
        if (which == 0) return super.onKeyMultiple(keyCode, repeatCount, event);
        if (which == -1) return true;

        repeatCount--;
        KeyEvent up = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);

        if (which == 1)
        {
            mKeyListener.onKeyUp(this, (Editable)mText, keyCode, up);

            while (--repeatCount > 0)
            {
                mKeyListener.onKeyDown(this, (Editable)mText, keyCode, down);
                mKeyListener.onKeyUp(this, (Editable)mText, keyCode, up);
            }
        }
        else if (which == 2)
        {
            mMovement.onKeyUp(this, (Spannable)mText, keyCode, up);

            while (--repeatCount > 0)
            {
                mMovement.onKeyDown(this, (Spannable)mText, keyCode, down);
                mMovement.onKeyUp(this, (Spannable)mText, keyCode, up);
            }
        }

        return true;
    }

    private boolean shouldAdvanceFocusOnEnter()
    {
        if (mKeyListener == null) return false;
        if (mSingleLine) return true;

        if ((mInputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT)
        {
            int variation = mInputType & InputType.TYPE_MASK_VARIATION;

            if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS || variation == InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT)
            {
                return true;
            }
        }

        return false;
    }

    private int doKeyDown(int keyCode, KeyEvent event)
    {
        if (!isEnabled()) return 0;

        switch (keyCode)
        {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (event.hasNoModifiers())
                {
                    if (mEditorActionListener != null && mEditorActionListener.onEditorAction(this, EditorInfo.IME_NULL, event))
                    {
                        return -1;
                    }

                    if ((event.getFlags() & KeyEvent.FLAG_EDITOR_ACTION) != 0 || shouldAdvanceFocusOnEnter())
                    {
                        if (hasOnClickListeners()) return 0;
                        return -1;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.hasNoModifiers() && shouldAdvanceFocusOnEnter()) return 0;
                break;

            default:
                break;
        }

        if (mKeyListener != null && mText instanceof Editable)
        {
            beginBatchEdit();
            boolean handled = mKeyListener.onKeyDown(this, (Editable)mText, keyCode, event);
            endBatchEdit();

            if (handled) return 1;
        }

        if (mMovement != null && mLayout != null && mText instanceof Spannable)
        {
            if (mMovement.onKeyDown(this, (Spannable)mText, keyCode, event)) return 2;
        }

        return 0;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (!isEnabled()) return super.onKeyUp(keyCode, event);

        switch (keyCode)
        {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.hasNoModifiers() && !hasOnClickListeners() && mMovement != null && mText instanceof Editable && mLayout != null
                    && onCheckIsTextEditor())
                {
                    showSoftInput();
                }

                return super.onKeyUp(keyCode, event);

            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (event.hasNoModifiers())
                {
                    if (mEditorActionListener != null && mEditorActionListener.onEditorAction(this, EditorInfo.IME_NULL, event))
                    {
                        return true;
                    }

                    if ((event.getFlags() & KeyEvent.FLAG_EDITOR_ACTION) != 0 || shouldAdvanceFocusOnEnter())
                    {
                        View next = focusSearch(View.FOCUS_DOWN);

                        if (next != null)
                        {
                            next.requestFocus(View.FOCUS_DOWN);
                            super.onKeyUp(keyCode, event);
                            return true;
                        }

                        if ((event.getFlags() & KeyEvent.FLAG_EDITOR_ACTION) != 0) hideSoftInput();
                    }

                    return super.onKeyUp(keyCode, event);
                }
                break;

            default:
                break;
        }

        if (mKeyListener != null && mText instanceof Editable && mKeyListener.onKeyUp(this, (Editable)mText, keyCode, event)) return true;

        if (mMovement != null && mLayout != null && mText instanceof Spannable && mMovement.onKeyUp(this, (Spannable)mText, keyCode, event))
        {
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCheckIsTextEditor()
    {
        return mKeyListener != null && mInputType != InputType.TYPE_NULL;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttributes)
    {
        if (!onCheckIsTextEditor() || !isEnabled()) return null;

        if (outAttributes != null)
        {
            outAttributes.inputType = getInputType();
            outAttributes.imeOptions = mImeOptions;
            outAttributes.privateImeOptions = mPrivateImeOptions;
            outAttributes.actionLabel = mImeActionLabel;
            outAttributes.actionId = mImeActionId;
            outAttributes.extras = mInputExtras;
            outAttributes.hintLocales = mImeHintLocales;
            outAttributes.hintText = mHint;

            if (isMultilineInputType(outAttributes.inputType))
            {
                outAttributes.imeOptions = (outAttributes.imeOptions & ~EditorInfo.IME_MASK_ACTION) | EditorInfo.IME_ACTION_NONE
                                           | EditorInfo.IME_FLAG_NO_ENTER_ACTION;
            }
            else if ((outAttributes.imeOptions & EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_UNSPECIFIED)
            {
                int action = (outAttributes.imeOptions & EditorInfo.IME_FLAG_NAVIGATE_NEXT) != 0 ? EditorInfo.IME_ACTION_NEXT
                                                                                                  : EditorInfo.IME_ACTION_DONE;
                outAttributes.imeOptions |= action;
            }

            if (getContext() != null) outAttributes.packageName = getContext().getPackageName();
            outAttributes.fieldId = getId();

            if (mText instanceof Editable)
            {
                outAttributes.initialSelStart = getSelectionStart();
                outAttributes.initialSelEnd = getSelectionEnd();
                outAttributes.initialCapsMode = TextUtils.getCapsMode(mText, Math.max(0, getSelectionStart()), getInputType());
            }
        }

        return new EditableInputConnection(this);
    }

    public boolean extractText(ExtractedTextRequest request, ExtractedText outText)
    {
        if (outText == null || mText == null) return false;

        outText.text = request != null && (request.flags & InputConnection.GET_TEXT_WITH_STYLES) != 0
                           ? new SpannableString(mText)
                           : mText.toString();
        outText.startOffset = 0;
        outText.partialStartOffset = -1;
        outText.partialEndOffset = -1;
        outText.selectionStart = getSelectionStart();
        outText.selectionEnd = getSelectionEnd();
        outText.flags = mSingleLine ? ExtractedText.FLAG_SINGLE_LINE : 0;
        return true;
    }

    public void setExtractedText(ExtractedText text)
    {
        if (text == null || text.text == null || !(mText instanceof Editable)) return;

        Editable content = (Editable)mText;

        if (text.partialStartOffset < 0)
        {
            content.replace(0, content.length(), text.text);
        }
        else
        {
            int length = content.length();
            int start = Math.max(0, Math.min(text.partialStartOffset, length));
            int end = Math.max(start, Math.min(text.partialEndOffset, length));
            content.replace(start, end, text.text);
        }

        int start = text.selectionStart;
        int end = text.selectionEnd;
        int length = content.length();
        Selection.setSelection(content, Math.max(0, Math.min(start, length)), Math.max(0, Math.min(end, length)));
    }

    public void onCommitCompletion(CompletionInfo text)
    {
    }

    public void onCommitCorrection(CorrectionInfo info)
    {
    }

    public void beginBatchEdit()
    {
        mBatchEditNesting++;
        if (mBatchEditNesting == 1) onBeginBatchEdit();
    }

    public void endBatchEdit()
    {
        if (mBatchEditNesting <= 0) return;

        mBatchEditNesting--;
        if (mBatchEditNesting == 0) onEndBatchEdit();
    }

    public void onBeginBatchEdit()
    {
    }

    public void onEndBatchEdit()
    {
    }

    public boolean onPrivateIMECommand(String action, Bundle data)
    {
        return false;
    }

    public void setIncludeFontPadding(boolean includePad)
    {
        if (mIncludePad == includePad) return;

        mIncludePad = includePad;
        relayout();
    }

    public boolean getIncludeFontPadding()
    {
        return mIncludePad;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY)
        {
            width = widthSize;
        }
        else
        {
            float limit = widthMode == MeasureSpec.AT_MOST ? widthSize : Float.MAX_VALUE;
            int desired = (int)Math.ceil(Math.min(limit, Layout.getDesiredWidth(mTransformed, mTextPaint)));
            width = desired;

            if (mDrawables[1] != null) width = Math.max(width, mDrawables[1].getBounds().width());
            if (mDrawables[3] != null) width = Math.max(width, mDrawables[3].getBounds().width());

            if (mHint != null)
            {
                int hintDesired = (int)Math.ceil(Math.min(limit, Layout.getDesiredWidth(mHint, mTextPaint)));
                if (hintDesired > width) width = hintDesired;
            }

            width += getCompoundPaddingLeft() + getCompoundPaddingRight();

            if (mMaxWidthMode == EMS) width = Math.min(width, mMaxWidthValue * getLineHeight());
            else width = Math.min(width, mMaxWidthValue);

            if (mMinWidthMode == EMS) width = Math.max(width, mMinWidthValue * getLineHeight());
            else width = Math.max(width, mMinWidthValue);

            width = Math.max(width, getSuggestedMinimumWidth());

            if (widthMode == MeasureSpec.AT_MOST) width = Math.min(widthSize, width);
        }

        int want = width - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int unpaddedWidth = want;

        if (mHorizontallyScrolling) want = VERY_WIDE;

        int hintWant = want;
        int hintWidth = mHintLayout == null ? hintWant : mHintLayout.getWidth();
        int ellipsisWidth = width - getCompoundPaddingLeft() - getCompoundPaddingRight();

        if (mLayout == null)
        {
            makeNewLayout(want, hintWant, ellipsisWidth, false);
        }
        else
        {
            boolean layoutChanged = mLayout.getWidth() != want || hintWidth != hintWant || mLayout.getEllipsizedWidth() != ellipsisWidth;
            if (layoutChanged) makeNewLayout(want, hintWant, ellipsisWidth, false);
        }

        if (heightMode == MeasureSpec.EXACTLY)
        {
            height = heightSize;
        }
        else
        {
            int desired = getDesiredHeight();
            height = desired;

            if (heightMode == MeasureSpec.AT_MOST) height = Math.min(desired, heightSize);
        }

        int unpaddedHeight = height - getCompoundPaddingTop() - getCompoundPaddingBottom();
        if (mMaxMode == LINES && mLayout.getLineCount() > mMaximum) unpaddedHeight = Math.min(unpaddedHeight, mLayout.getLineTop(mMaximum));

        if (mMovement == null && mLayout.getWidth() <= unpaddedWidth && mLayout.getHeight() <= unpaddedHeight && !mHorizontallyScrolling)
        {
            if (getScrollX() != 0 || getScrollY() != 0) scrollTo(0, 0);
        }

        setMeasuredDimension(width, height);
    }

    private int getDesiredHeight()
    {
        return Math.max(getDesiredHeight(mLayout, true), getDesiredHeight(mHintLayout, mEllipsize != null));
    }

    private int getDesiredHeight(Layout layout, boolean cap)
    {
        if (layout == null) return 0;

        int desired = layout.getHeight();
        int lineCount = layout.getLineCount();
        int padding = getCompoundPaddingTop() + getCompoundPaddingBottom();

        if (mDrawables[0] != null) desired = Math.max(desired, mDrawables[0].getBounds().height());
        if (mDrawables[2] != null) desired = Math.max(desired, mDrawables[2].getBounds().height());

        desired += padding;

        if (mMaxMode != LINES)
        {
            desired = Math.min(desired, mMaximum);
        }
        else if (cap && lineCount > mMaximum)
        {
            desired = layout.getLineTop(mMaximum);

            if (mDrawables[0] != null) desired = Math.max(desired, mDrawables[0].getBounds().height());
            if (mDrawables[2] != null) desired = Math.max(desired, mDrawables[2].getBounds().height());

            desired += padding;
            lineCount = mMaximum;
        }

        if (mMinMode == LINES)
        {
            if (lineCount < mMinimum) desired += getLineHeight() * (mMinimum - lineCount);
        }
        else
        {
            desired = Math.max(desired, mMinimum);
        }

        return Math.max(desired, getSuggestedMinimumHeight());
    }

    private Layout.Alignment getLayoutAlignment()
    {
        int textAlignment = getTextAlignment();

        switch (textAlignment)
        {
            case View.TEXT_ALIGNMENT_TEXT_START:
                return Layout.Alignment.ALIGN_NORMAL;
            case View.TEXT_ALIGNMENT_TEXT_END:
                return Layout.Alignment.ALIGN_OPPOSITE;
            case View.TEXT_ALIGNMENT_CENTER:
                return Layout.Alignment.ALIGN_CENTER;
            case View.TEXT_ALIGNMENT_VIEW_START:
                return isRtl() ? Layout.Alignment.ALIGN_RIGHT : Layout.Alignment.ALIGN_LEFT;
            case View.TEXT_ALIGNMENT_VIEW_END:
                return isRtl() ? Layout.Alignment.ALIGN_LEFT : Layout.Alignment.ALIGN_RIGHT;
            default:
                break;
        }

        int gravity = Gravity.getAbsoluteGravity(mGravity, getLayoutDirection()) & Gravity.HORIZONTAL_GRAVITY_MASK;
        int relative = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;

        if (relative == Gravity.START) return Layout.Alignment.ALIGN_NORMAL;
        if (relative == Gravity.END) return Layout.Alignment.ALIGN_OPPOSITE;
        if (gravity == Gravity.CENTER_HORIZONTAL) return Layout.Alignment.ALIGN_CENTER;
        if (gravity == Gravity.RIGHT) return Layout.Alignment.ALIGN_RIGHT;
        if (gravity == Gravity.LEFT) return Layout.Alignment.ALIGN_LEFT;

        return Layout.Alignment.ALIGN_NORMAL;
    }

    private void assumeLayout()
    {
        int width = getRight() - getLeft() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        if (width < 1) width = 0;

        int physicalWidth = width;
        if (mHorizontallyScrolling) width = VERY_WIDE;

        makeNewLayout(width, width, physicalWidth, false);
    }

    private void nullLayouts()
    {
        mLayout = null;
        mHintLayout = null;
    }

    private void makeNewLayout(int wantWidth, int hintWidth, int ellipsisWidth, boolean bringIntoView)
    {
        if (wantWidth < 0) wantWidth = 0;
        if (hintWidth < 0) hintWidth = 0;
        if (ellipsisWidth < 0) ellipsisWidth = 0;

        Layout.Alignment alignment = getLayoutAlignment();
        boolean shouldEllipsize = mEllipsize != null && mKeyListener == null;
        TextUtils.TruncateAt effectiveEllipsize = mEllipsize == TextUtils.TruncateAt.MARQUEE ? TextUtils.TruncateAt.END : mEllipsize;

        mLayout = makeSingleLayout(mTransformed, wantWidth, alignment, shouldEllipsize, effectiveEllipsize, ellipsisWidth);

        if (mHint != null)
        {
            if (shouldEllipsize) hintWidth = wantWidth;
            mHintLayout = makeSingleLayout(mHint, hintWidth, alignment, shouldEllipsize, effectiveEllipsize, ellipsisWidth);
        }
        else
        {
            mHintLayout = null;
        }

        if (bringIntoView) post(this::bringTextIntoView);
    }

    private Layout makeSingleLayout(CharSequence text, int wantWidth, Layout.Alignment alignment, boolean shouldEllipsize,
                                    TextUtils.TruncateAt effectiveEllipsize, int ellipsisWidth)
    {
        boolean editable = mText instanceof Editable;

        if (!editable && !(text instanceof Spannable))
        {
            BoringLayout.Metrics boring = BoringLayout.isBoring(text, mTextPaint);

            if (boring != null)
            {
                if (boring.width <= wantWidth && (effectiveEllipsize == null || !shouldEllipsize || boring.width <= ellipsisWidth))
                {
                    return BoringLayout.make(text, mTextPaint, wantWidth, alignment, mSpacingMultiplier, mSpacingAdd, boring, mIncludePad);
                }

                if (shouldEllipsize && boring.width <= wantWidth)
                {
                    return BoringLayout.make(text, mTextPaint, wantWidth, alignment, mSpacingMultiplier, mSpacingAdd, boring, mIncludePad,
                                             effectiveEllipsize, ellipsisWidth);
                }
            }
        }

        StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), mTextPaint, wantWidth).setAlignment(alignment)
                                           .setTextDirection(getTextDirectionHeuristic()).setLineSpacing(mSpacingAdd, mSpacingMultiplier)
                                           .setIncludePad(mIncludePad).setUseLineSpacingFromFallbacks(mUseFallbackLineSpacing)
                                           .setBreakStrategy(mBreakStrategy).setHyphenationFrequency(mHyphenationFrequency)
                                           .setJustificationMode(mJustificationMode);

        if (!editable) builder.setMaxLines(mMaxMode == LINES ? mMaximum : Integer.MAX_VALUE);
        if (shouldEllipsize) builder.setEllipsize(effectiveEllipsize).setEllipsizedWidth(ellipsisWidth);

        return builder.build();
    }

    private int getVerticalOffset(boolean forceNormal)
    {
        int voffset = 0;
        int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        Layout layout = mLayout;
        if (!forceNormal && mText.length() == 0 && mHintLayout != null) layout = mHintLayout;
        if (layout == null) return 0;

        if (gravity != Gravity.TOP)
        {
            int boxHeight = getBoxHeight(layout);
            int textHeight = layout.getHeight();

            if (textHeight < boxHeight)
            {
                if (gravity == Gravity.BOTTOM) voffset = boxHeight - textHeight;
                else voffset = (boxHeight - textHeight) >> 1;
            }
        }

        return voffset;
    }

    private int getBottomVerticalOffset(boolean forceNormal)
    {
        int voffset = 0;
        int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        Layout layout = mLayout;
        if (!forceNormal && mText.length() == 0 && mHintLayout != null) layout = mHintLayout;
        if (layout == null) return 0;

        if (gravity != Gravity.BOTTOM)
        {
            int boxHeight = getBoxHeight(layout);
            int textHeight = layout.getHeight();

            if (textHeight < boxHeight)
            {
                if (gravity == Gravity.TOP) voffset = boxHeight - textHeight;
                else voffset = (boxHeight - textHeight) >> 1;
            }
        }

        return voffset;
    }

    private int getBoxHeight(Layout layout)
    {
        int padding = layout == mHintLayout ? getCompoundPaddingTop() + getCompoundPaddingBottom()
                                            : getExtendedPaddingTop() + getExtendedPaddingBottom();

        return getMeasuredHeight() - padding;
    }

    private void checkForRelayout()
    {
        nullLayouts();
        requestLayout();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        autoSizeText();

        if (mLayout == null) assumeLayout();

        int cursor = getSelectionEnd();

        if (mMovement != null && cursor >= 0) bringPointIntoView(cursor);
        else bringTextIntoView();
    }

    private boolean bringTextIntoView()
    {
        Layout layout = isShowingHint() ? mHintLayout : mLayout;
        if (layout == null) return false;

        int line = 0;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) line = layout.getLineCount() - 1;

        Layout.Alignment alignment = layout.getParagraphAlignment(line);
        int direction = layout.getParagraphDirection(line);
        int horizontalSpace = getRight() - getLeft() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int verticalSpace = getBottom() - getTop() - getExtendedPaddingTop() - getExtendedPaddingBottom();
        int height = layout.getHeight();

        int scrollX;
        int scrollY;

        if (alignment == Layout.Alignment.ALIGN_NORMAL)
        {
            alignment = direction == Layout.DIR_LEFT_TO_RIGHT ? Layout.Alignment.ALIGN_LEFT : Layout.Alignment.ALIGN_RIGHT;
        }
        else if (alignment == Layout.Alignment.ALIGN_OPPOSITE)
        {
            alignment = direction == Layout.DIR_LEFT_TO_RIGHT ? Layout.Alignment.ALIGN_RIGHT : Layout.Alignment.ALIGN_LEFT;
        }

        if (alignment == Layout.Alignment.ALIGN_CENTER)
        {
            int left = (int)Math.floor(layout.getLineLeft(line));
            int right = (int)Math.ceil(layout.getLineRight(line));

            if (right - left < horizontalSpace) scrollX = (right + left) / 2 - horizontalSpace / 2;
            else scrollX = direction < 0 ? right - horizontalSpace : left;
        }
        else if (alignment == Layout.Alignment.ALIGN_RIGHT)
        {
            int right = (int)Math.ceil(layout.getLineRight(line));
            scrollX = right - horizontalSpace;
        }
        else
        {
            scrollX = (int)Math.floor(layout.getLineLeft(line));
        }

        if (height < verticalSpace) scrollY = 0;
        else if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) scrollY = height - verticalSpace;
        else scrollY = 0;

        if (!mHorizontallyScrolling) scrollX = 0;

        if (scrollX != getScrollX() || scrollY != getScrollY())
        {
            scrollTo(Math.max(0, scrollX), Math.max(0, scrollY));
            return true;
        }

        return false;
    }

    private boolean isShowingHint()
    {
        return TextUtils.isEmpty(mText) && !TextUtils.isEmpty(mHint);
    }

    public boolean bringPointIntoView(int offset)
    {
        return bringPointIntoView(offset, false);
    }

    public boolean bringPointIntoView(int offset, boolean requestRectWithoutFocus)
    {
        Layout layout = isShowingHint() ? mHintLayout : mLayout;
        if (layout == null) return false;

        int line = layout.getLineForOffset(offset);
        int horizontal = (int)layout.getPrimaryHorizontal(offset);
        int top = layout.getLineTop(line);
        int bottom = layout.getLineTop(line + 1);

        int left = (int)Math.floor(layout.getLineLeft(line));
        int right = (int)Math.ceil(layout.getLineRight(line));
        int height = layout.getHeight();

        int horizontalSpace = getRight() - getLeft() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int verticalSpace = getBottom() - getTop() - getExtendedPaddingTop() - getExtendedPaddingBottom();
        if (horizontalSpace <= 0 || verticalSpace <= 0) return false;

        int verticalSlack = (bottom - top) / 2;
        if (verticalSlack > verticalSpace / 4) verticalSlack = verticalSpace / 4;

        int horizontalSlack = Math.max(0, Math.min(horizontalSpace / 4, (int)(2 * mTextPaint.getTextSize())));

        int scrollX = getScrollX();
        int scrollY = getScrollY();

        if (top - scrollY < verticalSlack) scrollY = top - verticalSlack;
        if (bottom - scrollY > verticalSpace - verticalSlack) scrollY = bottom - (verticalSpace - verticalSlack);
        if (height - scrollY < verticalSpace) scrollY = height - verticalSpace;
        if (0 - scrollY > 0) scrollY = 0;

        if (right - left <= horizontalSpace || !mHorizontallyScrolling)
        {
            scrollX = 0;
        }
        else
        {
            if (horizontal < scrollX + horizontalSlack) scrollX = horizontal - horizontalSlack;
            if (horizontal > scrollX + horizontalSpace - horizontalSlack) scrollX = horizontal - (horizontalSpace - horizontalSlack);

            scrollX = Math.max(left, Math.min(scrollX, right - horizontalSpace));
        }

        if (scrollX != getScrollX() || scrollY != getScrollY())
        {
            scrollTo(scrollX, Math.max(0, scrollY));
            return true;
        }

        return false;
    }

    public boolean moveCursorToVisibleOffset()
    {
        if (!(mText instanceof Spannable) || mLayout == null) return false;

        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start != end) return false;

        int line = mLayout.getLineForOffset(start);
        int top = mLayout.getLineTop(line);
        int bottom = mLayout.getLineTop(line + 1);
        int verticalSpace = getBottom() - getTop() - getExtendedPaddingTop() - getExtendedPaddingBottom();

        int verticalSlack = (bottom - top) / 2;
        if (verticalSlack > verticalSpace / 4) verticalSlack = verticalSpace / 4;

        int verticalScroll = getScrollY();

        if (top < verticalScroll + verticalSlack) line = mLayout.getLineForVertical(verticalScroll + verticalSlack + (bottom - top));
        else if (bottom > verticalSpace + verticalScroll - verticalSlack)
        {
            line = mLayout.getLineForVertical(verticalSpace + verticalScroll - verticalSlack - (bottom - top));
        }

        int horizontalSpace = getRight() - getLeft() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int horizontalScroll = getScrollX();
        int leftChar = mLayout.getOffsetForHorizontal(line, horizontalScroll);
        int rightChar = mLayout.getOffsetForHorizontal(line, horizontalSpace + horizontalScroll);

        int lowChar = Math.min(leftChar, rightChar);
        int highChar = Math.max(leftChar, rightChar);

        int newStart = start;
        if (newStart < lowChar) newStart = lowChar;
        else if (newStart > highChar) newStart = highChar;

        if (newStart != start)
        {
            Selection.setSelection((Spannable)mText, newStart);
            return true;
        }

        return false;
    }

    @Override
    public void computeScroll()
    {
        if (mScroller != null && mScroller.computeScrollOffset())
        {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
            return;
        }

        super.computeScroll();
    }

    public void debug(int depth)
    {
    }

    public int getSelectionStart()
    {
        return Selection.getSelectionStart(getText());
    }

    public int getSelectionEnd()
    {
        return Selection.getSelectionEnd(getText());
    }

    public boolean hasSelection()
    {
        int start = getSelectionStart();
        int end = getSelectionEnd();

        return start >= 0 && end > 0 && start != end;
    }

    public void setSingleLine()
    {
        setSingleLine(true);
    }

    public void setAllCaps(boolean allCaps)
    {
        mAllCaps = allCaps;

        if (allCaps) setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        else if (mTransformation instanceof AllCapsTransformationMethod) setTransformationMethod(null);
    }

    public boolean isAllCaps()
    {
        return mAllCaps && mTransformation instanceof AllCapsTransformationMethod;
    }

    public void setSingleLine(boolean singleLine)
    {
        setInputTypeSingleLine(singleLine);
        applySingleLine(singleLine, true, true, true);
    }

    private void setInputTypeSingleLine(boolean singleLine)
    {
        if (mKeyListener != null && (mInputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT)
        {
            if (singleLine) mInputType &= ~InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            else mInputType |= InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        }
    }

    private void applySingleLine(boolean singleLine, boolean applyTransformation, boolean changeMaxLines, boolean changeMaxLength)
    {
        mSingleLine = singleLine;

        if (singleLine)
        {
            setLines(1);
            setHorizontallyScrolling(true);

            if (applyTransformation) setTransformationMethod(SingleLineTransformationMethod.getInstance());
        }
        else
        {
            if (changeMaxLines) setMaxLines(Integer.MAX_VALUE);

            setHorizontallyScrolling(false);

            if (applyTransformation && mTransformation instanceof SingleLineTransformationMethod) setTransformationMethod(null);
        }
    }

    public void setEllipsize(TextUtils.TruncateAt where)
    {
        if (mEllipsize == where) return;

        mEllipsize = where;
        relayout();
    }

    public void setMarqueeRepeatLimit(int marqueeLimit)
    {
        mMarqueeRepeatLimit = marqueeLimit;
    }

    public int getMarqueeRepeatLimit()
    {
        return mMarqueeRepeatLimit;
    }

    public TextUtils.TruncateAt getEllipsize()
    {
        return mEllipsize;
    }

    public void setSelectAllOnFocus(boolean selectAllOnFocus)
    {
        mSelectAllOnFocus = selectAllOnFocus;

        if (selectAllOnFocus && !(mText instanceof Spannable)) setText(mText, BufferType.SPANNABLE);
    }

    public void setCursorVisible(boolean visible)
    {
        if (mCursorVisible == visible) return;

        mCursorVisible = visible;
        invalidate();

        if (visible && isFocused()) startBlink();
        else if (!visible) stopBlink();
    }

    public boolean isCursorVisible()
    {
        return mCursorVisible;
    }

    private boolean isCursorBlinkEnabled()
    {
        return mCursorVisible && isTextEditable();
    }

    private void startBlink()
    {
        if (!isCursorBlinkEnabled()) return;

        if (mBlink == null)
        {
            mBlink = new Runnable()
            {
                @Override
                public void run()
                {
                    if (!isFocused() || !isCursorBlinkEnabled())
                    {
                        mCursorBlinkOn = true;
                        return;
                    }

                    mCursorBlinkOn = !mCursorBlinkOn;
                    invalidate();
                    postDelayed(this, CURSOR_BLINK_INTERVAL);
                }
            };
        }

        removeCallbacks(mBlink);
        mCursorBlinkOn = true;
        postDelayed(mBlink, CURSOR_BLINK_INTERVAL);
    }

    private void stopBlink()
    {
        if (mBlink != null) removeCallbacks(mBlink);
        mCursorBlinkOn = true;
    }

    private void resetBlink()
    {
        if (isFocused()) startBlink();
    }

    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter)
    {
    }

    protected void onSelectionChanged(int selectionStart, int selectionEnd)
    {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
    }

    public void addTextChangedListener(TextWatcher watcher)
    {
        if (watcher != null) mListeners.add(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher)
    {
        mListeners.remove(watcher);
    }

    private void sendBeforeTextChanged(CharSequence text, int start, int before, int after)
    {
        ArrayList<TextWatcher> listeners = new ArrayList<>(mListeners);

        for (TextWatcher listener : listeners)
        {
            listener.beforeTextChanged(text, start, before, after);
        }
    }

    void sendOnTextChanged(CharSequence text, int start, int before, int after)
    {
        ArrayList<TextWatcher> listeners = new ArrayList<>(mListeners);

        for (TextWatcher listener : listeners)
        {
            listener.onTextChanged(text, start, before, after);
        }
    }

    void sendAfterTextChanged(Editable text)
    {
        ArrayList<TextWatcher> listeners = new ArrayList<>(mListeners);

        for (TextWatcher listener : listeners)
        {
            listener.afterTextChanged(text);
        }
    }

    private void handleTextChanged(CharSequence buffer, int start, int before, int after)
    {
        if (mTransformation != null) updateTransformed();

        checkForRelayout();
        resetBlink();

        sendOnTextChanged(buffer, start, before, after);
        onTextChanged(buffer, start, before, after);
    }

    private void spanChange(Spanned buffer, Object what, int oldStart, int newStart, int oldEnd, int newEnd)
    {
        boolean selectionChanged = false;
        int newSelectionStart = -1;
        int newSelectionEnd = -1;

        if (what == Selection.SELECTION_END)
        {
            selectionChanged = true;
            newSelectionEnd = newStart;

            if (oldStart >= 0 || newStart >= 0)
            {
                resetBlink();
                post(this::ensureCursorVisible);
            }
        }

        if (what == Selection.SELECTION_START)
        {
            selectionChanged = true;
            newSelectionStart = newStart;
        }

        if (selectionChanged)
        {
            invalidate();

            if ((buffer.getSpanFlags(what) & Spanned.SPAN_INTERMEDIATE) == 0)
            {
                if (newSelectionStart < 0) newSelectionStart = Selection.getSelectionStart(buffer);
                if (newSelectionEnd < 0) newSelectionEnd = Selection.getSelectionEnd(buffer);

                onSelectionChanged(newSelectionStart, newSelectionEnd);
            }
        }

        if (what instanceof UpdateLayout || what instanceof ParagraphStyle)
        {
            checkForRelayout();
        }
        else if (what instanceof UpdateAppearance || what instanceof CharacterStyle)
        {
            invalidate();
        }

        if (MetaKeyKeyListener.isMetaTracker(buffer, what))
        {
            if (MetaKeyKeyListener.isSelectingMetaTracker(buffer, what)) invalidate();
        }
    }

    private void ensureCursorVisible()
    {
        if (mLayout == null || mMovement == null) return;

        int end = getSelectionEnd();
        if (end >= 0) bringPointIntoView(end);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
    {
        if (focused && mText instanceof Spannable)
        {
            Spannable spannable = (Spannable)mText;
            int selectionStart = getSelectionStart();
            int selectionEnd = getSelectionEnd();

            if (mSelectAllOnFocus) Selection.setSelection(spannable, 0, spannable.length());
            else if ((selectionStart < 0 || selectionEnd < 0) && isTextEditable()) Selection.setSelection(spannable, spannable.length());

            if (mMovement != null) mMovement.onTakeFocus(this, spannable, direction);
        }

        if (mTransformation != null) mTransformation.onFocusChanged(this, mText, focused, direction, previouslyFocusedRect);

        if (focused)
        {
            startBlink();
            if (mShowSoftInputOnFocus && isTextEditable() && isInTouchMode()) showSoftInput();
        }
        else
        {
            stopBlink();
            if (mText instanceof Spannable && !isTextEditable() && !mTextIsSelectable) Selection.removeSelection((Spannable)mText);
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        invalidate();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus)
    {
        super.onWindowFocusChanged(hasWindowFocus);

        if (hasWindowFocus && isFocused()) startBlink();
        else stopBlink();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility)
    {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility != VISIBLE) stopBlink();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible)
    {
        super.onVisibilityAggregated(isVisible);
    }

    public void clearComposingText()
    {
        if (mText instanceof Spannable) EditableInputConnection.removeComposingSpansFrom((Spannable)mText);
    }

    @Override
    public void setSelected(boolean selected)
    {
        boolean wasSelected = isSelected();

        super.setSelected(selected);

        if (selected != wasSelected && mEllipsize == TextUtils.TruncateAt.MARQUEE) invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) mTouchFocusSelected = false;

        boolean superResult = super.onTouchEvent(event);

        boolean touchIsFinished = action == MotionEvent.ACTION_UP && isFocused();

        if ((mMovement != null || onCheckIsTextEditor()) && isEnabled() && mText instanceof Spannable && mLayout != null)
        {
            boolean handled = false;

            if (mMovement != null) handled |= mMovement.onTouchEvent(this, (Spannable)mText, event);

            boolean textIsSelectable = isTextSelectable();

            if (touchIsFinished && mLinksClickable && mAutoLinkMask != 0 && textIsSelectable)
            {
                ClickableSpan[] links = ((Spannable)mText).getSpans(getSelectionStart(), getSelectionEnd(), ClickableSpan.class);

                if (links.length > 0)
                {
                    links[0].onClick(this);
                    handled = true;
                }
            }

            if (action == MotionEvent.ACTION_UP && (isTextEditable() || textIsSelectable))
            {
                if (!isFocused()) requestFocus();
                if (isTextEditable() && mShowSoftInputOnFocus) showSoftInput();

                handled = true;
            }

            if (handled) return true;
        }

        return superResult;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mMovement != null && mText instanceof Spannable && mLayout != null)
        {
            try
            {
                if (mMovement.onGenericMotionEvent(this, (Spannable)mText, event)) return true;
            }
            catch (AbstractMethodError error)
            {
                return super.onGenericMotionEvent(event);
            }
        }

        return super.onGenericMotionEvent(event);
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu)
    {
        super.onCreateContextMenu(menu);
    }

    @Override
    public boolean showContextMenu()
    {
        return super.showContextMenu();
    }

    @Override
    public boolean showContextMenu(float x, float y)
    {
        return super.showContextMenu(x, y);
    }

    public boolean didTouchFocusSelect()
    {
        return mTouchFocusSelected;
    }

    @Override
    public void cancelLongPress()
    {
        super.cancelLongPress();
    }

    public boolean onTrackballEvent(MotionEvent event)
    {
        if (mMovement != null && mText instanceof Spannable && mLayout != null)
        {
            if (mMovement.onTrackballEvent(this, (Spannable)mText, event)) return true;
        }

        return false;
    }

    public void setScroller(Scroller scroller)
    {
        mScroller = scroller;
    }

    @Override
    protected float getLeftFadingEdgeStrength()
    {
        return super.getLeftFadingEdgeStrength();
    }

    protected float getRightFadingEdgeStrength()
    {
        return 0f;
    }

    @Override
    protected int computeHorizontalScrollRange()
    {
        if (mLayout != null)
        {
            return mSingleLine && (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.LEFT ? (int)mLayout.getLineWidth(0)
                                                                                              : mLayout.getWidth();
        }

        return super.computeHorizontalScrollRange();
    }

    @Override
    protected int computeVerticalScrollRange()
    {
        if (mLayout != null) return mLayout.getHeight();

        return super.computeVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent()
    {
        return getHeight() - getCompoundPaddingTop() - getCompoundPaddingBottom();
    }

    @Override
    public void findViewsWithText(ArrayList<View> outViews, CharSequence searched, int flags)
    {
        super.findViewsWithText(outViews, searched, flags);

        if (!outViews.contains(this) && (flags & View.FIND_VIEWS_WITH_TEXT) != 0 && !TextUtils.isEmpty(searched)
            && !TextUtils.isEmpty(mText))
        {
            String searchedLowerCase = searched.toString().toLowerCase();
            String textLowerCase = mText.toString().toLowerCase();

            if (textLowerCase.contains(searchedLowerCase)) outViews.add(this);
        }
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event)
    {
        if (event.hasModifiers(KeyEvent.META_CTRL_ON))
        {
            switch (keyCode)
            {
                case KeyEvent.KEYCODE_A:
                    if (canSelectText()) return onTextContextMenuItem(ID_SELECT_ALL);
                    break;
                case KeyEvent.KEYCODE_X:
                    if (canCut()) return onTextContextMenuItem(ID_CUT);
                    break;
                case KeyEvent.KEYCODE_C:
                    if (canCopy()) return onTextContextMenuItem(ID_COPY);
                    break;
                case KeyEvent.KEYCODE_V:
                    if (canPaste()) return onTextContextMenuItem(ID_PASTE);
                    break;
                default:
                    break;
            }
        }

        return super.onKeyShortcut(keyCode, event);
    }

    private boolean canSelectText()
    {
        return mText.length() != 0 && mMovement != null && mMovement.canSelectArbitrarily();
    }

    private boolean hasPasswordTransformationMethod()
    {
        return mTransformation instanceof PasswordTransformationMethod;
    }

    private boolean canCut()
    {
        return !hasPasswordTransformationMethod() && mText.length() > 0 && hasSelection() && mText instanceof Editable
               && mKeyListener != null;
    }

    private boolean canCopy()
    {
        return !hasPasswordTransformationMethod() && mText.length() > 0 && hasSelection();
    }

    private boolean canPaste()
    {
        return mText instanceof Editable && mKeyListener != null && getSelectionStart() >= 0 && getSelectionEnd() >= 0;
    }

    public boolean isAutoHandwritingEnabled()
    {
        return false;
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return TextView.class.getName();
    }

    @Override
    public void autofill(android.view.autofill.AutofillValue value)
    {
        if (value == null || !value.isText() || !isTextEditable()) return;

        setText(value.getTextValue(), mBufferType, true, 0);

        CharSequence text = getText();
        if (text instanceof Spannable) Selection.setSelection((Spannable)text, text.length());
    }

    @Override
    public int getAutofillType()
    {
        return isTextEditable() ? View.AUTOFILL_TYPE_TEXT : View.AUTOFILL_TYPE_NONE;
    }

    @Override
    public android.view.autofill.AutofillValue getAutofillValue()
    {
        if (isTextEditable()) return android.view.autofill.AutofillValue.forText(getText());

        return null;
    }

    @Override
    public void addExtraDataToAccessibilityNodeInfo(AccessibilityNodeInfo info, String extraDataKey, Bundle arguments)
    {
        super.addExtraDataToAccessibilityNodeInfo(info, extraDataKey, arguments);
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event)
    {
        super.sendAccessibilityEventUnchecked(event);
    }

    public boolean isInputMethodTarget()
    {
        InputMethodManager manager = InputMethodManager.getInstance();
        return manager != null && manager.getServedView() == this;
    }

    public boolean onTextContextMenuItem(int id)
    {
        int min = 0;
        int max = mText.length();

        if (isFocused())
        {
            int selectionStart = getSelectionStart();
            int selectionEnd = getSelectionEnd();

            min = Math.max(0, Math.min(selectionStart, selectionEnd));
            max = Math.max(0, Math.max(selectionStart, selectionEnd));
        }

        switch (id)
        {
            case ID_SELECT_ALL:
                if (mText instanceof Spannable) Selection.setSelection((Spannable)mText, 0, mText.length());
                return true;

            case ID_PASTE:
            case ID_PASTE_AS_PLAIN_TEXT:
                paste(min, max, id == ID_PASTE_AS_PLAIN_TEXT);
                return true;

            case ID_CUT:
                if (setPrimaryClip(ClipData.newPlainText(null, mTransformed.subSequence(min, max))))
                {
                    deleteText(min, max);
                }
                return true;

            case ID_COPY:
                setPrimaryClip(ClipData.newPlainText(null, mTransformed.subSequence(min, max)));
                if (mText instanceof Spannable) Selection.setSelection((Spannable)mText, max);
                return true;

            default:
                return false;
        }
    }

    private void deleteText(int start, int end)
    {
        if (mText instanceof Editable) ((Editable)mText).delete(start, end);
    }

    private android.content.ClipboardManager clipboard()
    {
        Object service = getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        return service instanceof android.content.ClipboardManager ? (android.content.ClipboardManager)service : null;
    }

    private boolean setPrimaryClip(ClipData clip)
    {
        android.content.ClipboardManager clipboard = clipboard();
        if (clipboard == null) return false;

        try
        {
            clipboard.setPrimaryClip(clip);
        }
        catch (RuntimeException exception)
        {
            return false;
        }

        return true;
    }

    private void paste(int min, int max, boolean withFormatting)
    {
        android.content.ClipboardManager clipboard = clipboard();
        if (clipboard == null || !(mText instanceof Editable)) return;

        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null) return;

        boolean didFirst = false;

        for (int index = 0; index < clip.getItemCount(); index++)
        {
            CharSequence paste = clip.getItemAt(index).coerceToText(getContext());
            if (paste == null) continue;
            if (withFormatting) paste = paste.toString();

            if (!didFirst)
            {
                Selection.setSelection((Spannable)mText, max);
                ((Editable)mText).replace(min, max, paste);
                didFirst = true;
            }
            else
            {
                ((Editable)mText).insert(getSelectionEnd(), "\n");
                ((Editable)mText).insert(getSelectionEnd(), paste);
            }
        }
    }

    @Override
    public boolean performLongClick()
    {
        return super.performLongClick();
    }

    @Override
    protected void onScrollChanged(int horizontal, int vertical, int oldHorizontal, int oldVertical)
    {
        super.onScrollChanged(horizontal, vertical, oldHorizontal, oldVertical);
    }

    public boolean isSuggestionsEnabled()
    {
        if ((mInputType & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false;
        if ((mInputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) > 0) return false;

        int variation = mInputType & InputType.TYPE_MASK_VARIATION;

        return variation == InputType.TYPE_TEXT_VARIATION_NORMAL || variation == InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT
               || variation == InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE
               || variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
               || variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT;
    }

    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback)
    {
        mCustomSelectionActionModeCallback = actionModeCallback;
    }

    public ActionMode.Callback getCustomSelectionActionModeCallback()
    {
        return mCustomSelectionActionModeCallback;
    }

    public void setCustomInsertionActionModeCallback(ActionMode.Callback actionModeCallback)
    {
        mCustomInsertionActionModeCallback = actionModeCallback;
    }

    public ActionMode.Callback getCustomInsertionActionModeCallback()
    {
        return mCustomInsertionActionModeCallback;
    }

    public void setTextClassifier(TextClassifier textClassifier)
    {
        mTextClassifier = textClassifier;
    }

    public TextClassifier getTextClassifier()
    {
        if (mTextClassifier == null)
        {
            TextClassificationManager manager = TextClassificationManager.getInstance();
            if (manager != null) return manager.getTextClassifier();

            return TextClassifier.NO_OP;
        }

        return mTextClassifier;
    }

    public int getOffsetForPosition(float x, float y)
    {
        if (getLayout() == null) return -1;

        int line = getLineAtCoordinate(y);
        return getOffsetAtCoordinate(line, x);
    }

    private float convertToLocalHorizontalCoordinate(float x)
    {
        x -= getTotalPaddingLeft();
        x = Math.max(0.0f, x);
        x = Math.min(getWidth() - getTotalPaddingRight() - 1, x);
        x += getScrollX();
        return x;
    }

    private int getLineAtCoordinate(float y)
    {
        y -= getTotalPaddingTop();
        y = Math.max(0.0f, y);
        y = Math.min(getHeight() - getTotalPaddingBottom() - 1, y);
        y += getScrollY();
        return getLayout().getLineForVertical((int)y);
    }

    private int getOffsetAtCoordinate(int line, float x)
    {
        x = convertToLocalHorizontalCoordinate(x);
        return getLayout().getOffsetForHorizontal(line, x);
    }

    @Override
    public boolean onDragEvent(DragEvent event)
    {
        return super.onDragEvent(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection)
    {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mDrawablesRelative)
        {
            Drawable start = mDrawableStart;
            Drawable end = mDrawableEnd;
            setCompoundDrawablesRelative(start, mDrawables[1], end, mDrawables[3]);
        }

        relayout();
    }

    public TextDirectionHeuristic getTextDirectionHeuristic()
    {
        boolean defaultIsRtl = isRtl();

        switch (getTextDirection())
        {
            case View.TEXT_DIRECTION_ANY_RTL:
                return TextDirectionHeuristics.ANYRTL_LTR;
            case View.TEXT_DIRECTION_LTR:
                return TextDirectionHeuristics.LTR;
            case View.TEXT_DIRECTION_RTL:
                return TextDirectionHeuristics.RTL;
            case View.TEXT_DIRECTION_LOCALE:
                return TextDirectionHeuristics.LOCALE;
            case View.TEXT_DIRECTION_FIRST_STRONG_LTR:
                return TextDirectionHeuristics.FIRSTSTRONG_LTR;
            case View.TEXT_DIRECTION_FIRST_STRONG_RTL:
                return TextDirectionHeuristics.FIRSTSTRONG_RTL;
            default:
                return defaultIsRtl ? TextDirectionHeuristics.FIRSTSTRONG_RTL : TextDirectionHeuristics.FIRSTSTRONG_LTR;
        }
    }

    @Override
    public ContentInfo onReceiveContent(ContentInfo payload)
    {
        return super.onReceiveContent(payload);
    }

    public void onCreateViewTranslationRequest(int[] supportedFormats,
                                               java.util.function.Consumer<android.view.translation.ViewTranslationRequest> collector)
    {
    }

    private void restartInput()
    {
        InputMethodManager manager = InputMethodManager.getInstance();
        if (manager != null) manager.restartInput(this);
    }

    private void showSoftInput()
    {
        InputMethodManager manager = InputMethodManager.getInstance();
        if (manager != null) manager.showSoftInput(this, 0);
    }

    private void hideSoftInput()
    {
        InputMethodManager manager = InputMethodManager.getInstance();
        if (manager != null && manager.getServedView() == this) manager.hideSoftInputFromWindow(getWindowToken(), 0);
    }

    private final class ChangeWatcher implements TextWatcher, SpanWatcher
    {
        @Override
        public void beforeTextChanged(CharSequence buffer, int start, int before, int after)
        {
            sendBeforeTextChanged(buffer, start, before, after);
        }

        @Override
        public void onTextChanged(CharSequence buffer, int start, int before, int after)
        {
            handleTextChanged(buffer, start, before, after);
        }

        @Override
        public void afterTextChanged(Editable buffer)
        {
            sendAfterTextChanged(buffer);
        }

        @Override
        public void onSpanChanged(Spannable buffer, Object what, int oldStart, int oldEnd, int start, int end)
        {
            spanChange(buffer, what, oldStart, start, oldEnd, end);
        }

        @Override
        public void onSpanAdded(Spannable buffer, Object what, int start, int end)
        {
            spanChange(buffer, what, -1, start, -1, end);
        }

        @Override
        public void onSpanRemoved(Spannable buffer, Object what, int start, int end)
        {
            spanChange(buffer, what, start, -1, end, -1);
        }
    }

    private static final class AllCapsTransformationMethod implements TransformationMethod
    {
        private final Locale mLocale;

        AllCapsTransformationMethod(Context context)
        {
            Locale locale = null;

            if (context != null && context.getResources() != null && context.getResources().getConfiguration() != null)
            {
                locale = context.getResources().getConfiguration().locale;
            }

            mLocale = locale == null ? Locale.getDefault() : locale;
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view)
        {
            if (source == null) return null;

            String upper = source.toString().toUpperCase(mLocale);

            if (!(source instanceof Spanned) || upper.length() != source.length()) return upper;

            SpannableString spannable = new SpannableString(upper);
            TextUtils.copySpansFrom((Spanned)source, 0, source.length(), CharacterStyle.class, spannable, 0);
            return spannable;
        }

        @Override
        public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect)
        {
        }
    }

    private static final class CharWrapper implements CharSequence, GetChars
    {
        private final char[] mChars;
        private final int mStart;
        private final int mLength;

        CharWrapper(char[] chars, int start, int length)
        {
            mChars = chars;
            mStart = start;
            mLength = length;
        }

        @Override
        public int length()
        {
            return mLength;
        }

        @Override
        public char charAt(int offset)
        {
            return mChars[offset + mStart];
        }

        @Override
        public String toString()
        {
            return new String(mChars, mStart, mLength);
        }

        @Override
        public CharSequence subSequence(int start, int end)
        {
            if (start < 0 || end < 0 || start > mLength || end > mLength) throw new IndexOutOfBoundsException(start + ", " + end);

            return new String(mChars, start + mStart, end - start);
        }

        @Override
        public void getChars(int start, int end, char[] buffer, int offset)
        {
            if (start < 0 || end < 0 || start > mLength || end > mLength) throw new IndexOutOfBoundsException(start + ", " + end);

            System.arraycopy(mChars, start + mStart, buffer, offset, end - start);
        }
    }

    static final class EditableInputConnection extends BaseInputConnection
    {
        private static final Object COMPOSING = new ComposingText();

        private final TextView mTextView;

        EditableInputConnection(TextView textView)
        {
            super(textView, true);
            mTextView = textView;
        }

        private static final class ComposingText implements NoCopySpan
        {
        }

        static void removeComposingSpansFrom(Spannable text)
        {
            text.removeSpan(COMPOSING);
        }

        private static int composingStart(Spannable text)
        {
            return text.getSpanStart(COMPOSING);
        }

        private static int composingEnd(Spannable text)
        {
            return text.getSpanEnd(COMPOSING);
        }

        @Override
        public Editable getEditable()
        {
            return mTextView.getEditableText();
        }

        @Override
        public boolean beginBatchEdit()
        {
            mTextView.beginBatchEdit();
            return true;
        }

        @Override
        public boolean endBatchEdit()
        {
            mTextView.endBatchEdit();
            return true;
        }

        @Override
        public boolean clearMetaKeyStates(int states)
        {
            Editable content = getEditable();
            if (content == null) return false;

            KeyListener listener = mTextView.getKeyListener();
            if (listener != null) listener.clearMetaKeyState(mTextView, content, states);

            return true;
        }

        @Override
        public CharSequence getTextBeforeCursor(int length, int flags)
        {
            Editable content = getEditable();
            if (content == null) return null;

            int start = Selection.getSelectionStart(content);
            int end = Selection.getSelectionEnd(content);
            if (start > end) start = end;
            if (start < 0) start = content.length();

            int from = Math.max(0, start - length);
            return (flags & GET_TEXT_WITH_STYLES) != 0 ? content.subSequence(from, start) : TextUtils.substring(content, from, start);
        }

        @Override
        public CharSequence getTextAfterCursor(int length, int flags)
        {
            Editable content = getEditable();
            if (content == null) return null;

            int start = Selection.getSelectionStart(content);
            int end = Selection.getSelectionEnd(content);
            if (start > end) end = start;
            if (end < 0) end = content.length();

            int to = (int)Math.min((long)end + length, content.length());
            return (flags & GET_TEXT_WITH_STYLES) != 0 ? content.subSequence(end, to) : TextUtils.substring(content, end, to);
        }

        @Override
        public CharSequence getSelectedText(int flags)
        {
            Editable content = getEditable();
            if (content == null) return null;

            int start = Selection.getSelectionStart(content);
            int end = Selection.getSelectionEnd(content);
            if (start > end)
            {
                int swap = start;
                start = end;
                end = swap;
            }

            if (start == end || start < 0) return null;

            return (flags & GET_TEXT_WITH_STYLES) != 0 ? content.subSequence(start, end) : TextUtils.substring(content, start, end);
        }

        @Override
        public int getCursorCapsMode(int reqModes)
        {
            Editable content = getEditable();
            if (content == null) return 0;

            int start = Selection.getSelectionStart(content);
            int end = Selection.getSelectionEnd(content);
            if (start > end) start = end;

            return TextUtils.getCapsMode(content, Math.max(0, start), reqModes);
        }

        @Override
        public ExtractedText getExtractedText(ExtractedTextRequest request, int flags)
        {
            ExtractedText extracted = new ExtractedText();
            if (mTextView.extractText(request, extracted)) return extracted;

            return null;
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength)
        {
            Editable content = getEditable();
            if (content == null) return false;

            beginBatchEdit();

            int start = Selection.getSelectionStart(content);
            int end = Selection.getSelectionEnd(content);

            if (start > end)
            {
                int swap = start;
                start = end;
                end = swap;
            }

            int composingStart = composingStart(content);
            int composingEnd = composingEnd(content);

            if (composingStart < 0 && start >= 0 && start != end && beforeLength == 1 && afterLength == 0)
            {
                content.delete(start, end);
                endBatchEdit();
                return true;
            }

            if (start < 0) start = end = content.length();

            if (composingEnd < composingStart)
            {
                int swap = composingStart;
                composingStart = composingEnd;
                composingEnd = swap;
            }

            if (composingStart != -1 && composingEnd != -1)
            {
                if (composingStart < start) start = composingStart;
                if (composingEnd > end) end = composingEnd;
            }

            int deleted = 0;

            if (beforeLength > 0)
            {
                int from = Math.max(0, start - beforeLength);
                if (from > 0 && Character.isLowSurrogate(content.charAt(from)) && Character.isHighSurrogate(content.charAt(from - 1)))
                {
                    from--;
                }

                content.delete(from, start);
                deleted = start - from;
            }

            if (afterLength > 0)
            {
                end = end - deleted;
                int to = Math.min(content.length(), end + afterLength);
                content.delete(end, to);
            }

            endBatchEdit();
            return true;
        }

        @Override
        public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength)
        {
            Editable content = getEditable();
            if (content == null) return false;

            int start = Math.max(0, Math.min(Selection.getSelectionStart(content), Selection.getSelectionEnd(content)));
            int end = Math.max(0, Math.max(Selection.getSelectionStart(content), Selection.getSelectionEnd(content)));

            int before = 0;
            int position = start;
            for (int count = 0; count < beforeLength && position > 0; count++)
            {
                int next = Character.offsetByCodePoints(content, position, -1);
                before += position - next;
                position = next;
            }

            int after = 0;
            position = end;
            for (int count = 0; count < afterLength && position < content.length(); count++)
            {
                int next = Character.offsetByCodePoints(content, position, 1);
                after += next - position;
                position = next;
            }

            return deleteSurroundingText(before, after);
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition)
        {
            replaceText(text, newCursorPosition, true);
            return true;
        }

        @Override
        public boolean setComposingRegion(int start, int end)
        {
            Editable content = getEditable();
            if (content == null) return false;

            beginBatchEdit();
            removeComposingSpansFrom(content);

            int from = Math.min(start, end);
            int to = Math.max(start, end);
            int length = content.length();

            if (from < 0) from = 0;
            if (to < 0) to = 0;
            if (from > length) from = length;
            if (to > length) to = length;

            if (from != to) content.setSpan(COMPOSING, from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);

            endBatchEdit();
            return true;
        }

        @Override
        public boolean finishComposingText()
        {
            Editable content = getEditable();
            if (content == null) return false;

            beginBatchEdit();
            removeComposingSpansFrom(content);
            endBatchEdit();
            return true;
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition)
        {
            replaceText(text, newCursorPosition, false);
            return true;
        }

        @Override
        public boolean commitCompletion(CompletionInfo text)
        {
            mTextView.beginBatchEdit();
            mTextView.onCommitCompletion(text);
            mTextView.endBatchEdit();
            return true;
        }

        @Override
        public boolean commitCorrection(CorrectionInfo correctionInfo)
        {
            mTextView.beginBatchEdit();
            mTextView.onCommitCorrection(correctionInfo);
            mTextView.endBatchEdit();
            return true;
        }

        @Override
        public boolean setSelection(int start, int end)
        {
            Editable content = getEditable();
            if (content == null) return false;

            int length = content.length();
            if (start > length || end > length || start < 0 || end < 0) return true;

            Selection.setSelection(content, start, end);
            return true;
        }

        @Override
        public boolean performEditorAction(int actionCode)
        {
            mTextView.onEditorAction(actionCode);
            return true;
        }

        @Override
        public boolean performContextMenuAction(int id)
        {
            mTextView.beginBatchEdit();
            mTextView.onTextContextMenuItem(id);
            mTextView.endBatchEdit();
            return true;
        }

        @Override
        public boolean performPrivateCommand(String action, Bundle data)
        {
            mTextView.onPrivateIMECommand(action, data);
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event)
        {
            return mTextView.dispatchKeyEvent(event);
        }

        @Override
        public void closeConnection()
        {
            finishComposingText();
        }

        private void replaceText(CharSequence text, int newCursorPosition, boolean composing)
        {
            Editable content = getEditable();
            if (content == null) return;
            if (text == null) text = "";

            beginBatchEdit();

            int start = composingStart(content);
            int end = composingEnd(content);

            if (end < start)
            {
                int swap = start;
                start = end;
                end = swap;
            }

            if (start != -1 && end != -1)
            {
                removeComposingSpansFrom(content);
            }
            else
            {
                start = Selection.getSelectionStart(content);
                end = Selection.getSelectionEnd(content);

                if (start < 0) start = content.length();
                if (end < 0) end = start;

                if (end < start)
                {
                    int swap = start;
                    start = end;
                    end = swap;
                }
            }

            if (newCursorPosition > 0) newCursorPosition += end - 1;
            else newCursorPosition += start;

            if (newCursorPosition < 0) newCursorPosition = 0;
            if (newCursorPosition > content.length()) newCursorPosition = content.length();

            Selection.setSelection(content, newCursorPosition);

            int lengthBefore = content.length();
            content.replace(start, end, text);

            if (composing)
            {
                int inserted = content.length() - lengthBefore + (end - start);
                int flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING;
                if (inserted > 0) content.setSpan(COMPOSING, start, start + inserted, flags);
            }

            endBatchEdit();
        }
    }

    public static class SavedState extends BaseSavedState
    {
        int selStart = -1;
        int selEnd = -1;
        CharSequence text;
        boolean frozenWithFocus;
        CharSequence error;

        SavedState(Parcelable superState)
        {
            super(superState);
        }

        private SavedState(Parcel in)
        {
            super(in);
            selStart = in.readInt();
            selEnd = in.readInt();
            frozenWithFocus = in.readInt() != 0;
            text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            error = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            super.writeToParcel(out, flags);
            out.writeInt(selStart);
            out.writeInt(selEnd);
            out.writeInt(frozenWithFocus ? 1 : 0);
            TextUtils.writeToParcel(text, out, flags);
            TextUtils.writeToParcel(error, out, flags);
        }

        @Override
        public String toString()
        {
            String value = "TextView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " start=" + selStart + " end="
                           + selEnd;
            if (text != null) value += " text=" + text;

            return value + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>()
        {
            @Override
            public SavedState createFromParcel(Parcel in)
            {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size)
            {
                return new SavedState[size];
            }
        };
    }
}
