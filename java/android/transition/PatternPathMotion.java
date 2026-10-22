package android.transition;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;

public class PatternPathMotion extends PathMotion
{
    private Path mOriginalPatternPath;
    private final Path mPatternPath = new Path();
    private final Matrix mTempMatrix = new Matrix();

    public PatternPathMotion()
    {
        mPatternPath.lineTo(1, 0);
        mOriginalPatternPath = mPatternPath;
    }

    public PatternPathMotion(Context context, AttributeSet attributes)
    {
        super(context, attributes);

        String pathData = TransitionAttributes.getString(attributes, "patternPathData");
        if (pathData == null) throw new RuntimeException("pathData must be supplied for patternPathMotion");

        setPatternPath(android.util.PathParser.createPathFromPathData(pathData));
    }

    public PatternPathMotion(Path patternPath)
    {
        setPatternPath(patternPath);
    }

    public Path getPatternPath()
    {
        return mOriginalPatternPath;
    }

    public void setPatternPath(Path patternPath)
    {
        PathMeasure pathMeasure = new PathMeasure(patternPath, false);
        float length = pathMeasure.getLength();
        float[] position = new float[2];
        pathMeasure.getPosTan(length, position, null);
        float endX = position[0];
        float endY = position[1];
        pathMeasure.getPosTan(0, position, null);
        float startX = position[0];
        float startY = position[1];

        if (startX == endX && startY == endY) throw new IllegalArgumentException("pattern must not end at the starting point");

        mTempMatrix.setTranslate(-startX, -startY);
        float dx = endX - startX;
        float dy = endY - startY;
        float distance = (float)Math.hypot(dx, dy);
        float scale = 1 / distance;
        mTempMatrix.postScale(scale, scale);
        double angle = Math.atan2(dy, dx);
        mTempMatrix.postRotate((float)Math.toDegrees(-angle));
        patternPath.transform(mTempMatrix, mPatternPath);
        mOriginalPatternPath = patternPath;
    }

    @Override
    public Path getPath(float startX, float startY, float endX, float endY)
    {
        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float)Math.hypot(dx, dy);
        double angle = Math.atan2(dy, dx);

        mTempMatrix.setScale(length, length);
        mTempMatrix.postRotate((float)Math.toDegrees(angle));
        mTempMatrix.postTranslate(startX, startY);

        Path path = new Path();
        mPatternPath.transform(mTempMatrix, path);
        return path;
    }
}
