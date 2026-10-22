package android.gesture;

import android.graphics.Matrix;
import android.graphics.Path;

public class OrientedBoundingBox
{
    public final float squareness;
    public final float width;
    public final float height;
    public final float orientation;
    public final float centerX;
    public final float centerY;

    OrientedBoundingBox(float angle, float cx, float cy, float w, float h)
    {
        orientation = angle;
        width = w;
        height = h;
        centerX = cx;
        centerY = cy;

        float ratio = w / h;
        squareness = ratio > 1 ? 1 / ratio : ratio;
    }

    public Path toPath()
    {
        Path path = new Path();
        float[] point = new float[2];
        Matrix matrix = new Matrix();
        matrix.setRotate(orientation);
        matrix.postTranslate(centerX, centerY);

        float[][] corners = {{-width / 2, height / 2}, {-width / 2, -height / 2}, {width / 2, -height / 2}, {width / 2, height / 2}};

        for (int index = 0; index < corners.length; index++)
        {
            point[0] = corners[index][0];
            point[1] = corners[index][1];
            matrix.mapPoints(point);

            if (index == 0) path.moveTo(point[0], point[1]);
            else path.lineTo(point[0], point[1]);
        }

        path.close();
        return path;
    }
}
