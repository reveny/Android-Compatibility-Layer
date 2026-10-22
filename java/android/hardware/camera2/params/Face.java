package android.hardware.camera2.params;

import android.graphics.Point;
import android.graphics.Rect;

public final class Face
{
    public static final int ID_UNSUPPORTED = -1;
    public static final int SCORE_MIN = 1;
    public static final int SCORE_MAX = 100;

    private Rect mBounds;
    private int mScore;
    private int mId = ID_UNSUPPORTED;
    private Point mLeftEye;
    private Point mRightEye;
    private Point mMouth;

    private Face()
    {
    }

    public Rect getBounds()
    {
        return mBounds;
    }

    public int getScore()
    {
        return mScore;
    }

    public int getId()
    {
        return mId;
    }

    public Point getLeftEyePosition()
    {
        return mLeftEye;
    }

    public Point getRightEyePosition()
    {
        return mRightEye;
    }

    public Point getMouthPosition()
    {
        return mMouth;
    }

    @Override
    public String toString()
    {
        return String.format("{ bounds: %s, score: %s, id: %d, leftEyePosition: %s, rightEyePosition: %s, mouthPosition: %s }",
                mBounds, mScore, mId, mLeftEye, mRightEye, mMouth);
    }

    public static final class Builder
    {
        private static final long FIELD_BOUNDS = 1 << 1;
        private static final long FIELD_SCORE = 1 << 2;
        private static final long FIELD_BUILT = 1 << 0;

        private long mBuilderFieldsSet;
        private Rect mBounds;
        private int mScore;
        private int mId = ID_UNSUPPORTED;
        private Point mLeftEye;
        private Point mRightEye;
        private Point mMouth;

        public Builder()
        {
        }

        public Builder(Face current)
        {
            if (current == null) throw new IllegalArgumentException("Null current face");
            mBounds = current.mBounds;
            mScore = current.mScore;
            mId = current.mId;
            mLeftEye = current.mLeftEye;
            mRightEye = current.mRightEye;
            mMouth = current.mMouth;
            mBuilderFieldsSet = FIELD_BOUNDS | FIELD_SCORE;
        }

        public Builder setBounds(Rect bounds)
        {
            checkNotUsed();
            mBuilderFieldsSet |= FIELD_BOUNDS;
            mBounds = bounds;
            return this;
        }

        public Builder setScore(int score)
        {
            checkNotUsed();
            if (score < SCORE_MIN || score > SCORE_MAX) throw new IllegalArgumentException("Confidence out of range");
            mBuilderFieldsSet |= FIELD_SCORE;
            mScore = score;
            return this;
        }

        public Builder setId(int id)
        {
            checkNotUsed();
            mId = id;
            return this;
        }

        public Builder setLeftEyePosition(Point leftEyePosition)
        {
            checkNotUsed();
            mLeftEye = leftEyePosition;
            return this;
        }

        public Builder setRightEyePosition(Point rightEyePosition)
        {
            checkNotUsed();
            mRightEye = rightEyePosition;
            return this;
        }

        public Builder setMouthPosition(Point mouthPosition)
        {
            checkNotUsed();
            mMouth = mouthPosition;
            return this;
        }

        public Face build()
        {
            checkNotUsed();
            if ((mBuilderFieldsSet & FIELD_BOUNDS) == 0) throw new IllegalStateException("Field \"bounds\" must be set");
            if ((mBuilderFieldsSet & FIELD_SCORE) == 0) throw new IllegalStateException("Field \"score\" must be set");
            if (mId == ID_UNSUPPORTED && (mLeftEye != null || mRightEye != null || mMouth != null))
            {
                throw new IllegalArgumentException("ID must be set if position is set");
            }
            if (mId != ID_UNSUPPORTED && mId < 0) throw new IllegalArgumentException("Id out of range");

            mBuilderFieldsSet |= FIELD_BUILT;

            Face face = new Face();
            face.mBounds = mBounds;
            face.mScore = mScore;
            face.mId = mId;
            face.mLeftEye = mLeftEye;
            face.mRightEye = mRightEye;
            face.mMouth = mMouth;
            return face;
        }

        private void checkNotUsed()
        {
            if ((mBuilderFieldsSet & FIELD_BUILT) != 0)
            {
                throw new IllegalStateException("This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }
}
