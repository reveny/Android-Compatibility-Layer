package android.media;

import android.graphics.Bitmap;
import android.graphics.PointF;

public class FaceDetector
{
    private final int mWidth;
    private final int mHeight;
    private final int mMaxFaces;

    public FaceDetector(int width, int height, int maxFaces)
    {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid image dimensions");
        if (maxFaces <= 0) throw new IllegalArgumentException("maxFaces must be positive");

        mWidth = width;
        mHeight = height;
        mMaxFaces = maxFaces;
    }

    public int findFaces(Bitmap bitmap, FaceDetector.Face[] faces)
    {
        if (bitmap == null) throw new NullPointerException("bitmap must not be null");
        if (faces == null) throw new NullPointerException("faces must not be null");

        if (bitmap.getWidth() != mWidth || bitmap.getHeight() != mHeight)
        {
            throw new IllegalArgumentException("bitmap size doesn't match initialization");
        }

        if (faces.length < mMaxFaces) throw new IllegalArgumentException("faces[] smaller than maxFaces");

        return 0;
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public class Face
    {
        public static final float CONFIDENCE_THRESHOLD = 0.4f;
        public static final int EULER_X = 0;
        public static final int EULER_Y = 1;
        public static final int EULER_Z = 2;

        private float mConfidence;
        private float mMidPointX;
        private float mMidPointY;
        private float mEyesDistance;
        private float mPoseEulerX;
        private float mPoseEulerY;
        private float mPoseEulerZ;

        private Face()
        {
        }

        public float confidence()
        {
            return mConfidence;
        }

        public float eyesDistance()
        {
            return mEyesDistance;
        }

        public void getMidPoint(PointF point)
        {
            point.set(mMidPointX, mMidPointY);
        }

        public float pose(int euler)
        {
            if (euler == EULER_X) return mPoseEulerX;
            if (euler == EULER_Y) return mPoseEulerY;
            if (euler == EULER_Z) return mPoseEulerZ;

            throw new IllegalArgumentException();
        }
    }
}
