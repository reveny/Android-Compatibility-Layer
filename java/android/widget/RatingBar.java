package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.StyleableSet;

public class RatingBar extends AbsSeekBar
{
    public interface OnRatingBarChangeListener
    {
        void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser);
    }

    private static final int DEFAULT_NUM_STARS = 5;
    private static final float DEFAULT_STEP_SIZE = 0.5f;

    private static StyleableSet sStyleable;

    private int mNumStars = DEFAULT_NUM_STARS;
    private float mStepSize = DEFAULT_STEP_SIZE;
    private int mProgressOnStartTracking;
    private OnRatingBarChangeListener mOnRatingBarChangeListener;
    private final Paint mStarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mStarPath = new Path();

    public RatingBar(Context context, AttributeSet attributes, int defStyleAttr)
    {
        this(context, attributes, defStyleAttr, 0);
    }

    public RatingBar(Context context, AttributeSet attributes, int defStyleAttr, int defStyleRes)
    {
        super(context, attributes, defStyleAttr, defStyleRes);

        int numStars = DEFAULT_NUM_STARS;
        float stepSize = DEFAULT_STEP_SIZE;
        float rating = -1f;

        StyleableSet styleable = styleable(context);
        if (!styleable.isEmpty() && (attributes != null || defStyleAttr != 0 || defStyleRes != 0))
        {
            TypedArray array = context.obtainStyledAttributes(attributes, styleable.attributes(), defStyleAttr, defStyleRes);

            int index = styleable.indexOf("numStars");
            if (index >= 0) numStars = array.getInt(index, numStars);

            index = styleable.indexOf("isIndicator");
            if (index >= 0) setIsIndicator(array.getBoolean(index, false));

            index = styleable.indexOf("rating");
            if (index >= 0 && array.hasValue(index)) rating = array.getFloat(index, rating);

            index = styleable.indexOf("stepSize");
            if (index >= 0) stepSize = array.getFloat(index, stepSize);

            array.recycle();
        }

        if (numStars <= 0) numStars = DEFAULT_NUM_STARS;

        mNumStars = numStars;
        setStepSize(stepSize);

        if (rating >= 0) setRating(rating);

        mTouchProgressOffset = 0.6f;
    }

    public RatingBar(Context context, AttributeSet attributes)
    {
        this(context, attributes, attributeIdentifier(context, "ratingBarStyle"));
    }

    public RatingBar(Context context)
    {
        this(context, null);
    }

    private static StyleableSet styleable(Context context)
    {
        if (sStyleable == null) sStyleable = StyleableSet.of(context.getResources(), "numStars", "isIndicator", "rating", "stepSize");
        return sStyleable;
    }

    public void setOnRatingBarChangeListener(OnRatingBarChangeListener listener)
    {
        mOnRatingBarChangeListener = listener;
    }

    public OnRatingBarChangeListener getOnRatingBarChangeListener()
    {
        return mOnRatingBarChangeListener;
    }

    public void setIsIndicator(boolean isIndicator)
    {
        mIsUserSeekable = !isIndicator;
        setFocusable(!isIndicator);
    }

    public boolean isIndicator()
    {
        return !mIsUserSeekable;
    }

    public void setNumStars(int numStars)
    {
        if (numStars <= 0) return;

        float rating = getRating();

        mNumStars = numStars;
        setMax(Math.max(1, Math.round(numStars / mStepSize)));
        setRating(rating);
        requestLayout();
    }

    public int getNumStars()
    {
        return mNumStars;
    }

    public void setRating(float rating)
    {
        setProgress(Math.round(rating * getProgressPerStar()));
    }

    public float getRating()
    {
        return getProgress() / getProgressPerStar();
    }

    public void setStepSize(float stepSize)
    {
        if (stepSize <= 0) return;

        float newMax = mNumStars / stepSize;
        int newProgress = (int)(newMax / Math.max(1, getMax()) * getProgress());

        mStepSize = stepSize;
        setMax((int)newMax);
        setProgress(newProgress);
    }

    public float getStepSize()
    {
        return (float)getNumStars() / Math.max(1, getMax());
    }

    private float getProgressPerStar()
    {
        if (mNumStars > 0) return 1f * getMax() / mNumStars;

        return 1;
    }

    @Override
    void onProgressRefresh(float scale, boolean fromUser, int progress)
    {
        super.onProgressRefresh(scale, fromUser, progress);

        if (!fromUser) dispatchRatingChange(false);
    }

    @Override
    void onStartTrackingTouch()
    {
        mProgressOnStartTracking = getProgress();

        super.onStartTrackingTouch();
    }

    @Override
    void onStopTrackingTouch()
    {
        super.onStopTrackingTouch();

        if (getProgress() != mProgressOnStartTracking) dispatchRatingChange(true);
    }

    @Override
    void onKeyChange()
    {
        super.onKeyChange();

        dispatchRatingChange(true);
    }

    void dispatchRatingChange(boolean fromUser)
    {
        if (mOnRatingBarChangeListener != null) mOnRatingBarChangeListener.onRatingChanged(this, getRating(), fromUser);
    }

    @Override
    boolean drawsDefaultThumb()
    {
        return false;
    }

    @Override
    int intrinsicContentHeight()
    {
        return (int)(48 * density());
    }

    @Override
    void drawTrack(Canvas canvas, int left, int top, int right, int bottom)
    {
        int stars = Math.max(1, mNumStars);
        float starSize = Math.min(bottom - top, (right - left) / (float)stars);
        float rating = getRating();
        int[] state = getDrawableState();

        int filledColor = tintColor(getProgressTintList(), state, accentColor());
        int emptyColor = tintColor(getProgressBackgroundTintList(), state, 0x42000000);
        float starTop = top + ((bottom - top) - starSize) / 2f;

        boolean rightToLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        for (int index = 0; index < stars; index++)
        {
            float starLeft = rightToLeft ? right - (index + 1) * starSize : left + index * starSize;

            buildStar(starLeft, starTop, starSize);

            mStarPaint.setStyle(Paint.Style.FILL);
            mStarPaint.setColor(emptyColor);
            canvas.drawPath(mStarPath, mStarPaint);

            float fill = Math.max(0f, Math.min(1f, rating - index));
            if (fill <= 0f) continue;

            int saveCount = canvas.save();

            if (rightToLeft) canvas.clipRect(starLeft + starSize * (1f - fill), starTop, starLeft + starSize, starTop + starSize);
            else canvas.clipRect(starLeft, starTop, starLeft + starSize * fill, starTop + starSize);

            mStarPaint.setColor(filledColor);
            canvas.drawPath(mStarPath, mStarPaint);
            canvas.restoreToCount(saveCount);
        }
    }

    private void buildStar(float left, float top, float size)
    {
        float centreX = left + size / 2f;
        float centreY = top + size * 0.53f;
        float outer = size * 0.48f;
        float inner = outer * 0.4f;

        mStarPath.reset();

        for (int point = 0; point < 10; point++)
        {
            double angle = Math.PI / 5 * point - Math.PI / 2;
            float radius = (point & 1) == 0 ? outer : inner;
            float x = centreX + (float)(Math.cos(angle) * radius);
            float y = centreY + (float)(Math.sin(angle) * radius);

            if (point == 0) mStarPath.moveTo(x, y);
            else mStarPath.lineTo(x, y);
        }

        mStarPath.close();
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int starSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        int width = starSize * mNumStars + getPaddingLeft() + getPaddingRight();

        setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0), getMeasuredHeightAndState());
    }

    @Override
    public synchronized void setMax(int max)
    {
        if (max <= 0) return;

        super.setMax(max);
    }

    @Override
    public CharSequence getAccessibilityClassName()
    {
        return RatingBar.class.getName();
    }
}
