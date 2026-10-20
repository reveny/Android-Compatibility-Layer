package android.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.StyleableSet;
import android.view.View;

public class TextView extends View
{
    private static StyleableSet sStyleable;

    private android.text.Layout mLayout;
    private android.text.TextUtils.TruncateAt mEllipsize;
    private final TextPaint mPaint = new TextPaint();
    private CharSequence mText = "";
    private boolean mEditable;
    private int mTextColor = 0xFF000000;
    private android.text.method.TransformationMethod mTransformationMethod;
    private android.text.InputFilter[] mFilters = new android.text.InputFilter[0];

    public TextView(Context context)
    {
        this(context, null, 0, 0);
    }

    public TextView(Context context, AttributeSet attributes)
    {
        this(context, attributes, 0, 0);
    }

    public TextView(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public TextView(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        mPaint.setTextSize(14 * context.getResources().getDisplayMetrics().density);

        if (attributes == null) return;

        StyleableSet styleable = styleable(context);
        if (styleable.isEmpty()) return;

        TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);

        int textIndex = styleable.indexOf("text");
        if (textIndex >= 0)
        {
            String text = array.getString(textIndex);
            if (text != null) mText = text;
        }

        int sizeIndex = styleable.indexOf("textSize");
        if (sizeIndex >= 0)
        {
            float size = array.getDimension(sizeIndex, mPaint.getTextSize());
            if (size > 0) mPaint.setTextSize(size);
        }

        int colorIndex = styleable.indexOf("textColor");
        if (colorIndex >= 0 && array.hasValue(colorIndex)) mTextColor = array.getColor(colorIndex, mTextColor);

        array.recycle();
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null)
        {
            sStyleable = StyleableSet.of(context.getResources(), "text", "textSize", "textColor", "textStyle", "gravity", "maxLines",
                                         "ellipsize", "fontFamily");
        }
        return sStyleable;
    }

    public CharSequence getText()
    {
        return mText;
    }

    @Override
    public boolean onCheckIsTextEditor()
    {
        return mEditable;
    }

    protected void setEditable(boolean editable)
    {
        mEditable = editable;
        setFocusable(editable);
    }

    @Override
    public android.view.inputmethod.InputConnection onCreateInputConnection(
        android.view.inputmethod.EditorInfo outAttributes)
    {
        if (!mEditable) return null;

        if (outAttributes != null)
        {
            outAttributes.inputType = 1;
            outAttributes.initialSelStart = mText.length();
            outAttributes.initialSelEnd = mText.length();
        }

        return new android.view.inputmethod.BaseInputConnection(this, true)
        {
            @Override
            public boolean commitText(CharSequence text, int newCursorPosition)
            {
                if (text == null) return true;

                setText(mText.toString() + text);
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength)
            {
                String current = mText.toString();
                int removed = Math.min(beforeLength, current.length());

                if (removed > 0) setText(current.substring(0, current.length() - removed));
                return true;
            }
        };
    }

    public void setText(CharSequence text)
    {
        mText = text == null ? "" : text;
        requestLayout();
        invalidate();
    }

    public void setText(int resourceId)
    {
        setText(getContext().getString(resourceId));
    }

    public void setTextColor(int color)
    {
        mTextColor = color;
        invalidate();
    }

    private ColorStateList mTextColors;

    public void setTextColor(ColorStateList colors)
    {
        mTextColors = colors;

        if (colors != null) setTextColor(colors.getDefaultColor());
    }

    public ColorStateList getTextColors()
    {
        if (mTextColors == null) mTextColors = ColorStateList.valueOf(mTextColor);

        return mTextColors;
    }

    private final android.graphics.drawable.Drawable[] mCompoundDrawables =
        new android.graphics.drawable.Drawable[4];

    public void setCompoundDrawables(android.graphics.drawable.Drawable left, android.graphics.drawable.Drawable top,
                                     android.graphics.drawable.Drawable right, android.graphics.drawable.Drawable bottom)
    {
        mCompoundDrawables[0] = left;
        mCompoundDrawables[1] = top;
        mCompoundDrawables[2] = right;
        mCompoundDrawables[3] = bottom;

        invalidate();
    }

    public void setCompoundDrawablesRelative(android.graphics.drawable.Drawable start,
                                             android.graphics.drawable.Drawable top,
                                             android.graphics.drawable.Drawable end,
                                             android.graphics.drawable.Drawable bottom)
    {
        setCompoundDrawables(start, top, end, bottom);
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(android.graphics.drawable.Drawable start,
                                                                android.graphics.drawable.Drawable top,
                                                                android.graphics.drawable.Drawable end,
                                                                android.graphics.drawable.Drawable bottom)
    {
        setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
    }

    public android.graphics.drawable.Drawable[] getCompoundDrawablesRelative()
    {
        return mCompoundDrawables.clone();
    }

    private int mCompoundDrawablePadding;

    public void setCompoundDrawablePadding(int padding)
    {
        mCompoundDrawablePadding = padding;
    }

    public int getCompoundDrawablePadding()
    {
        return mCompoundDrawablePadding;
    }

    public int getCurrentTextColor()
    {
        return mTextColor;
    }

    public void setTextSize(float size)
    {
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setTextSize(int unit, float size)
    {
        android.util.DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();

        mPaint.setTextSize(android.util.TypedValue.applyDimension(unit, size, metrics));
        requestLayout();
        invalidate();
    }

    public float getTextSize()
    {
        return mPaint.getTextSize();
    }

    public TextPaint getPaint()
    {
        return mPaint;
    }

    public void setTypeface(Typeface typeface)
    {
        mPaint.setTypeface(typeface);
    }

    public android.text.method.TransformationMethod getTransformationMethod()
    {
        return mTransformationMethod;
    }

    public void setTransformationMethod(android.text.method.TransformationMethod method)
    {
        mTransformationMethod = method;
    }

    public void setAllCaps(boolean allCaps)
    {
    }

    public void setFilters(android.text.InputFilter[] filters)
    {
        mFilters = filters == null ? new android.text.InputFilter[0] : filters;
    }

    public android.text.InputFilter[] getFilters()
    {
        return mFilters;
    }

    public void setTextAppearance(int resourceId)
    {
    }

    public void setTextAppearance(Context context, int resourceId)
    {
    }

    public void setCompoundDrawablesWithIntrinsicBounds(android.graphics.drawable.Drawable left, android.graphics.drawable.Drawable top,
                                                        android.graphics.drawable.Drawable right, android.graphics.drawable.Drawable bottom)
    {
    }

    public android.graphics.drawable.Drawable[] getCompoundDrawables()
    {
        return new android.graphics.drawable.Drawable[4];
    }

    public void setGravity(int gravity)
    {
    }

    public void setSingleLine(boolean singleLine)
    {
    }

    public void setMaxLines(int maxLines)
    {
    }

    public void setEllipsize(android.text.TextUtils.TruncateAt where)
    {
        mEllipsize = where;
    }

    public android.text.TextUtils.TruncateAt getEllipsize()
    {
        return mEllipsize;
    }

    public int getLineHeight()
    {
        return mPaint.getFontMetricsInt(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int horizontalPadding = getPaddingLeft() + getPaddingRight();
        int verticalPadding = getPaddingTop() + getPaddingBottom();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int available = Math.max(0, MeasureSpec.getSize(widthMeasureSpec) - horizontalPadding);

        int desired = (int)Math.ceil(android.text.Layout.getDesiredWidth(mText, mPaint));
        int contentWidth = widthMode == MeasureSpec.EXACTLY ? available : Math.min(desired, available);
        if (widthMode == MeasureSpec.UNSPECIFIED) contentWidth = desired;

        mLayout = makeLayout(contentWidth);

        int measuredWidth = resolveSize(Math.max(contentWidth + horizontalPadding, getSuggestedMinimumWidth()), widthMeasureSpec);
        int measuredHeight = resolveSize(Math.max(mLayout.getHeight() + verticalPadding, getSuggestedMinimumHeight()), heightMeasureSpec);

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private android.text.Layout makeLayout(int width)
    {
        return android.text.StaticLayout.Builder.obtain(mText, 0, mText.length(), mPaint, Math.max(0, width)).build();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (mText.length() == 0) return;

        mPaint.setColor(mTextColor);

        if (mLayout == null) mLayout = makeLayout(getWidth() - getPaddingLeft() - getPaddingRight());

        int saveCount = canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        mLayout.draw(canvas);
        canvas.restoreToCount(saveCount);
    }

    public interface OnEditorActionListener
    {
        boolean onEditorAction(TextView view, int actionId, android.view.KeyEvent event);
    }

    private CharSequence mHint;

    public void setHint(CharSequence hint)
    {
        mHint = hint;
    }

    public void setHint(int resourceId)
    {
        mHint = getContext().getResources().getString(resourceId);
    }

    public CharSequence getHint()
    {
        return mHint;
    }

    private final java.util.ArrayList<android.text.TextWatcher> mTextWatchers = new java.util.ArrayList<>();
    private OnEditorActionListener mEditorActionListener;

    public void setOnEditorActionListener(OnEditorActionListener listener)
    {
        mEditorActionListener = listener;
    }

    public void addTextChangedListener(android.text.TextWatcher watcher)
    {
        if (watcher != null && !mTextWatchers.contains(watcher)) mTextWatchers.add(watcher);
    }

    public void removeTextChangedListener(android.text.TextWatcher watcher)
    {
        mTextWatchers.remove(watcher);
    }

    protected void notifyTextChanged(CharSequence before, CharSequence after)
    {
        for (int index = 0; index < mTextWatchers.size(); index++)
        {
            mTextWatchers.get(index).beforeTextChanged(before, 0, before == null ? 0 : before.length(),
                                                       after == null ? 0 : after.length());
        }

        for (int index = 0; index < mTextWatchers.size(); index++)
        {
            mTextWatchers.get(index).onTextChanged(after, 0, before == null ? 0 : before.length(),
                                                   after == null ? 0 : after.length());
        }
    }
}
