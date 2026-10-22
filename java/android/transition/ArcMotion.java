package android.transition;

import android.content.Context;
import android.graphics.Path;
import android.util.AttributeSet;

public class ArcMotion extends PathMotion
{
    private static final float DEFAULT_MIN_ANGLE_DEGREES = 0;
    private static final float DEFAULT_MAX_ANGLE_DEGREES = 70;
    private static final float DEFAULT_MAX_TANGENT = (float)Math.tan(Math.toRadians(DEFAULT_MAX_ANGLE_DEGREES / 2));

    private float mMinimumHorizontalAngle = 0;
    private float mMinimumVerticalAngle = 0;
    private float mMaximumAngle = DEFAULT_MAX_ANGLE_DEGREES;
    private float mMinimumHorizontalTangent = 0;
    private float mMinimumVerticalTangent = 0;
    private float mMaximumTangent = DEFAULT_MAX_TANGENT;

    public ArcMotion()
    {
    }

    public ArcMotion(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        setMinimumVerticalAngle(TransitionAttributes.getFloat(attributes, "minimumVerticalAngle", DEFAULT_MIN_ANGLE_DEGREES));
        setMinimumHorizontalAngle(TransitionAttributes.getFloat(attributes, "minimumHorizontalAngle", DEFAULT_MIN_ANGLE_DEGREES));
        setMaximumAngle(TransitionAttributes.getFloat(attributes, "maximumAngle", DEFAULT_MAX_ANGLE_DEGREES));
    }

    public void setMinimumHorizontalAngle(float angleInDegrees)
    {
        mMinimumHorizontalAngle = angleInDegrees;
        mMinimumHorizontalTangent = toTangent(angleInDegrees);
    }

    public float getMinimumHorizontalAngle()
    {
        return mMinimumHorizontalAngle;
    }

    public void setMinimumVerticalAngle(float angleInDegrees)
    {
        mMinimumVerticalAngle = angleInDegrees;
        mMinimumVerticalTangent = toTangent(angleInDegrees);
    }

    public float getMinimumVerticalAngle()
    {
        return mMinimumVerticalAngle;
    }

    public void setMaximumAngle(float angleInDegrees)
    {
        mMaximumAngle = angleInDegrees;
        mMaximumTangent = toTangent(angleInDegrees);
    }

    public float getMaximumAngle()
    {
        return mMaximumAngle;
    }

    private static float toTangent(float arcInDegrees)
    {
        if (arcInDegrees < 0 || arcInDegrees > 90) throw new IllegalArgumentException("Arc must be between 0 and 90 degrees");

        return (float)Math.tan(Math.toRadians(arcInDegrees / 2));
    }

    @Override
    public Path getPath(float startX, float startY, float endX, float endY)
    {
        Path path = new Path();
        path.moveTo(startX, startY);

        float ex;
        float ey;
        float deltaX = endX - startX;
        float deltaY = endY - startY;

        float h2 = deltaX * deltaX + deltaY * deltaY;

        float dx = (startX + endX) / 2;
        float dy = (startY + endY) / 2;

        float midDist2 = h2 * 0.25f;

        float minimumArcDist2;

        boolean isMovingUpwards = startY > endY;

        if (deltaY == 0)
        {
            ex = dx;
            ey = dy + (Math.abs(deltaX) * 0.5f * mMinimumHorizontalTangent);
            minimumArcDist2 = 0;
        }
        else if (deltaX == 0)
        {
            ex = dx + (Math.abs(deltaY) * 0.5f * mMinimumVerticalTangent);
            ey = dy;
            minimumArcDist2 = 0;
        }
        else if (Math.abs(deltaX) < Math.abs(deltaY))
        {
            float eDistY = Math.abs(h2 / (2 * deltaY));

            if (isMovingUpwards)
            {
                ey = endY + eDistY;
                ex = endX;
            }
            else
            {
                ey = startY + eDistY;
                ex = startX;
            }

            minimumArcDist2 = midDist2 * mMinimumVerticalTangent * mMinimumVerticalTangent;
        }
        else
        {
            float eDistX = h2 / (2 * deltaX);

            if (isMovingUpwards)
            {
                ex = startX + eDistX;
                ey = startY;
            }
            else
            {
                ex = endX - eDistX;
                ey = endY;
            }

            minimumArcDist2 = midDist2 * mMinimumHorizontalTangent * mMinimumHorizontalTangent;
        }

        float arcDistX = dx - ex;
        float arcDistY = dy - ey;
        float arcDist2 = arcDistX * arcDistX + arcDistY * arcDistY;

        float maximumArcDist2 = midDist2 * mMaximumTangent * mMaximumTangent;

        float newArcDistance2 = 0;
        if (arcDist2 < minimumArcDist2) newArcDistance2 = minimumArcDist2;
        else if (arcDist2 > maximumArcDist2) newArcDistance2 = maximumArcDist2;

        if (newArcDistance2 != 0 && arcDist2 != 0)
        {
            float ratio = (float)Math.sqrt(newArcDistance2 / arcDist2);
            ex = dx + (ratio * (ex - dx));
            ey = dy + (ratio * (ey - dy));
        }

        float control1X = (startX + ex) / 2;
        float control1Y = (startY + ey) / 2;
        float control2X = (ex + endX) / 2;
        float control2Y = (ey + endY) / 2;

        path.cubicTo(control1X, control1Y, control2X, control2Y, endX, endY);
        return path;
    }
}
