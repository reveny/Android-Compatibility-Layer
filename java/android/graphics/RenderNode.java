package android.graphics;

public final class RenderNode
{
    private static long sNextUniqueId = 1;

    private final String mName;
    private final long mUniqueId;
    private final DisplayList mDisplayList = new DisplayList();
    private final RecordingCanvas mRecordingCanvas = new RecordingCanvas(mDisplayList);
    private final Outline mOutline = new Outline();

    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;

    private float mTranslationX;
    private float mTranslationY;
    private float mTranslationZ;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    private float mRotationX;
    private float mRotationY;
    private float mRotationZ;
    private float mPivotX;
    private float mPivotY;
    private float mAlpha = 1.0f;
    private float mElevation;
    private float mCameraDistance;

    private boolean mPivotExplicit;
    private boolean mClipToBounds = true;
    private boolean mClipToOutline;
    private boolean mHasDisplayList;
    private boolean mRecording;
    private boolean mHasOverlappingRendering = true;
    private boolean mForceDarkAllowed = true;
    private boolean mUseCompositingLayer;

    private int mAmbientShadowColor = 0xFF000000;
    private int mSpotShadowColor = 0xFF000000;

    public RenderNode(String name)
    {
        mName = name;
        mUniqueId = sNextUniqueId++;
    }

    public long getUniqueId()
    {
        return mUniqueId;
    }

    public RecordingCanvas beginRecording()
    {
        return beginRecording(getWidth(), getHeight());
    }

    public RecordingCanvas beginRecording(int width, int height)
    {
        mRecordingCanvas.beginRecording(width, height);
        mRecording = true;

        return mRecordingCanvas;
    }

    public void endRecording()
    {
        mRecording = false;
        mHasDisplayList = true;
    }

    public boolean hasDisplayList()
    {
        return mHasDisplayList;
    }

    public void discardDisplayList()
    {
        mDisplayList.clear();
        mHasDisplayList = false;
    }

    public long computeApproximateMemoryUsage()
    {
        return mDisplayList.size() * 64L;
    }

    public boolean setPosition(int left, int top, int right, int bottom)
    {
        if (mLeft == left && mTop == top && mRight == right && mBottom == bottom) return false;

        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;

        if (!mPivotExplicit)
        {
            mPivotX = (right - left) * 0.5f;
            mPivotY = (bottom - top) * 0.5f;
        }

        return true;
    }

    public boolean setPosition(Rect position)
    {
        return position != null && setPosition(position.left, position.top, position.right, position.bottom);
    }

    public boolean offsetLeftAndRight(int offset)
    {
        mLeft += offset;
        mRight += offset;

        return offset != 0;
    }

    public boolean offsetTopAndBottom(int offset)
    {
        mTop += offset;
        mBottom += offset;

        return offset != 0;
    }

    public int getLeft()
    {
        return mLeft;
    }

    public int getTop()
    {
        return mTop;
    }

    public int getRight()
    {
        return mRight;
    }

    public int getBottom()
    {
        return mBottom;
    }

    public int getWidth()
    {
        return mRight - mLeft;
    }

    public int getHeight()
    {
        return mBottom - mTop;
    }

    public boolean setAlpha(float alpha)
    {
        if (mAlpha == alpha) return false;

        mAlpha = alpha;
        return true;
    }

    public float getAlpha()
    {
        return mAlpha;
    }

    public boolean setTranslationX(float value)
    {
        if (mTranslationX == value) return false;

        mTranslationX = value;
        return true;
    }

    public float getTranslationX()
    {
        return mTranslationX;
    }

    public boolean setTranslationY(float value)
    {
        if (mTranslationY == value) return false;

        mTranslationY = value;
        return true;
    }

    public float getTranslationY()
    {
        return mTranslationY;
    }

    public boolean setTranslationZ(float value)
    {
        if (mTranslationZ == value) return false;

        mTranslationZ = value;
        return true;
    }

    public float getTranslationZ()
    {
        return mTranslationZ;
    }

    public boolean setScaleX(float value)
    {
        if (mScaleX == value) return false;

        mScaleX = value;
        return true;
    }

    public float getScaleX()
    {
        return mScaleX;
    }

    public boolean setScaleY(float value)
    {
        if (mScaleY == value) return false;

        mScaleY = value;
        return true;
    }

    public float getScaleY()
    {
        return mScaleY;
    }

    public boolean setRotationX(float value)
    {
        if (mRotationX == value) return false;

        mRotationX = value;
        return true;
    }

    public float getRotationX()
    {
        return mRotationX;
    }

    public boolean setRotationY(float value)
    {
        if (mRotationY == value) return false;

        mRotationY = value;
        return true;
    }

    public float getRotationY()
    {
        return mRotationY;
    }

    public boolean setRotationZ(float value)
    {
        if (mRotationZ == value) return false;

        mRotationZ = value;
        return true;
    }

    public float getRotationZ()
    {
        return mRotationZ;
    }

    public boolean setPivotX(float value)
    {
        mPivotX = value;
        mPivotExplicit = true;

        return true;
    }

    public float getPivotX()
    {
        return mPivotX;
    }

    public boolean setPivotY(float value)
    {
        mPivotY = value;
        mPivotExplicit = true;

        return true;
    }

    public float getPivotY()
    {
        return mPivotY;
    }

    public boolean isPivotExplicitlySet()
    {
        return mPivotExplicit;
    }

    public boolean resetPivot()
    {
        mPivotExplicit = false;
        mPivotX = getWidth() * 0.5f;
        mPivotY = getHeight() * 0.5f;

        return true;
    }

    public boolean setClipToBounds(boolean value)
    {
        if (mClipToBounds == value) return false;

        mClipToBounds = value;
        return true;
    }

    public boolean getClipToBounds()
    {
        return mClipToBounds;
    }

    public boolean setClipToOutline(boolean value)
    {
        if (mClipToOutline == value) return false;

        mClipToOutline = value;
        return true;
    }

    public boolean getClipToOutline()
    {
        return mClipToOutline;
    }

    public boolean setOutline(Outline outline)
    {
        if (outline == null) mOutline.setEmpty();
        else mOutline.set(outline);

        return true;
    }

    public boolean setElevation(float value)
    {
        if (mElevation == value) return false;

        mElevation = value;
        return true;
    }

    public float getElevation()
    {
        return mElevation;
    }

    public boolean hasShadow()
    {
        return mElevation > 0 && !mOutline.isEmpty() && mAlpha > 0;
    }

    public boolean setCameraDistance(float value)
    {
        mCameraDistance = value;
        return true;
    }

    public float getCameraDistance()
    {
        return mCameraDistance;
    }

    public boolean setAmbientShadowColor(int color)
    {
        mAmbientShadowColor = color;
        return true;
    }

    public int getAmbientShadowColor()
    {
        return mAmbientShadowColor;
    }

    public boolean setSpotShadowColor(int color)
    {
        mSpotShadowColor = color;
        return true;
    }

    public int getSpotShadowColor()
    {
        return mSpotShadowColor;
    }

    public boolean setHasOverlappingRendering(boolean value)
    {
        mHasOverlappingRendering = value;
        return true;
    }

    public boolean hasOverlappingRendering()
    {
        return mHasOverlappingRendering;
    }

    public boolean setForceDarkAllowed(boolean value)
    {
        mForceDarkAllowed = value;
        return true;
    }

    public boolean isForceDarkAllowed()
    {
        return mForceDarkAllowed;
    }

    public boolean setUseCompositingLayer(boolean value, Paint paint)
    {
        mUseCompositingLayer = value;
        return true;
    }

    private RenderEffect mRenderEffect;

    public boolean setRenderEffect(RenderEffect renderEffect)
    {
        if (mRenderEffect == renderEffect) return false;

        mRenderEffect = renderEffect;
        return true;
    }

    public RenderEffect getRenderEffect()
    {
        return mRenderEffect;
    }

    public boolean getUseCompositingLayer()
    {
        return mUseCompositingLayer;
    }

    public boolean setProjectBackwards(boolean value)
    {
        return true;
    }

    public boolean setProjectionReceiver(boolean value)
    {
        return true;
    }

    public boolean setRenderEffect(Object effect)
    {
        return true;
    }

    public boolean setClipRect(Rect rect)
    {
        return true;
    }

    public boolean hasIdentityMatrix()
    {
        return mTranslationX == 0 && mTranslationY == 0 && mScaleX == 1 && mScaleY == 1 && mRotationZ == 0;
    }

    public void getMatrix(Matrix into)
    {
        if (into == null) return;

        into.reset();
        into.postTranslate(-mPivotX, -mPivotY);

        if (mScaleX != 1 || mScaleY != 1) into.postScale(mScaleX, mScaleY);
        if (mRotationZ != 0) into.postRotate(mRotationZ);

        into.postTranslate(mPivotX + mTranslationX, mPivotY + mTranslationY);
    }

    public void getInverseMatrix(Matrix into)
    {
        if (into == null) return;

        Matrix forward = new Matrix();
        getMatrix(forward);

        if (!forward.invert(into)) into.reset();
    }

    void drawInto(Canvas target)
    {
        if (!mHasDisplayList || mAlpha <= 0) return;

        int saveCount = target.save();
        target.translate(mLeft, mTop);

        Matrix transform = new Matrix();
        getMatrix(transform);

        if (!transform.isIdentity()) target.concat(transform);

        if (mClipToOutline && mOutline.canClip()) clipToOutline(target);
        else if (mClipToBounds) target.clipRect(0, 0, getWidth(), getHeight());

        if (mAlpha < 1.0f) target.saveLayerAlpha(0, 0, getWidth(), getHeight(), Math.round(mAlpha * 255));

        mDisplayList.replay(target);

        target.restoreToCount(saveCount);
    }

    private void clipToOutline(Canvas target)
    {
        Rect bounds = new Rect();
        if (!mOutline.getRect(bounds)) return;

        Path shape = new Path();
        float radius = mOutline.getRadius();

        if (radius > 0)
        {
            shape.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, radius, radius, Path.Direction.CW);
        }
        else
        {
            shape.addRect(bounds.left, bounds.top, bounds.right, bounds.bottom, Path.Direction.CW);
        }

        target.clipPath(shape);
    }

    @Override
    public String toString()
    {
        return "RenderNode{" + mName + "}";
    }
}
