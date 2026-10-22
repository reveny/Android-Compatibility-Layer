package android.gesture;

import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;

public class GestureStroke
{
    private static final float TOUCH_TOLERANCE = 3;

    public final RectF boundingBox;
    public final float length;
    public final float[] points;

    private final long[] mTimestamps;
    private Path mCachedPath;

    public GestureStroke(ArrayList<GesturePoint> points)
    {
        int count = points.size();
        float[] coordinates = new float[count * 2];
        long[] times = new long[count];
        RectF box = null;
        float total = 0;

        for (int index = 0; index < count; index++)
        {
            GesturePoint point = points.get(index);
            coordinates[index * 2] = point.x;
            coordinates[index * 2 + 1] = point.y;
            times[index] = point.timestamp;

            if (box == null)
            {
                box = new RectF(point.x, point.y, point.x, point.y);
            }
            else
            {
                box.union(point.x, point.y);
                float dx = point.x - coordinates[(index - 1) * 2];
                float dy = point.y - coordinates[(index - 1) * 2 + 1];
                total += (float)Math.hypot(dx, dy);
            }
        }

        this.points = coordinates;
        mTimestamps = times;
        boundingBox = box == null ? new RectF() : box;
        length = total;
    }

    private GestureStroke(RectF box, float length, float[] points, long[] timestamps)
    {
        boundingBox = new RectF(box);
        this.length = length;
        this.points = points.clone();
        mTimestamps = timestamps.clone();
    }

    @Override
    public Object clone()
    {
        return new GestureStroke(boundingBox, length, points, mTimestamps);
    }

    long timestampAt(int index)
    {
        return mTimestamps[index];
    }

    public Path getPath()
    {
        if (mCachedPath == null) mCachedPath = buildPath();
        return mCachedPath;
    }

    private Path buildPath()
    {
        Path path = new Path();
        int count = points.length / 2;
        if (count == 0) return path;

        float lastX = points[0];
        float lastY = points[1];
        path.moveTo(lastX, lastY);

        for (int index = 1; index < count; index++)
        {
            float x = points[index * 2];
            float y = points[index * 2 + 1];
            float dx = Math.abs(x - lastX);
            float dy = Math.abs(y - lastY);

            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
            {
                path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                lastX = x;
                lastY = y;
            }
        }

        return path;
    }

    public Path toPath(float width, float height, int numSample)
    {
        Path path = new Path();
        int count = points.length / 2;
        if (count == 0 || numSample <= 0) return path;

        float[] sampled = sample(Math.max(2, numSample));
        float scale = Math.min(boundingBox.width() == 0 ? 1 : width / boundingBox.width(),
                               boundingBox.height() == 0 ? 1 : height / boundingBox.height());

        float lastX = 0;
        float lastY = 0;

        for (int index = 0; index < sampled.length; index += 2)
        {
            float x = (sampled[index] - boundingBox.left) * scale;
            float y = (sampled[index + 1] - boundingBox.top) * scale;

            if (index == 0)
            {
                path.moveTo(x, y);
                lastX = x;
                lastY = y;
                continue;
            }

            float dx = Math.abs(x - lastX);
            float dy = Math.abs(y - lastY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
            {
                path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                lastX = x;
                lastY = y;
            }
        }

        return path;
    }

    private float[] sample(int numSample)
    {
        float[] result = new float[numSample * 2];
        int count = points.length / 2;

        if (count == 1 || length == 0)
        {
            for (int index = 0; index < numSample; index++)
            {
                result[index * 2] = points[0];
                result[index * 2 + 1] = points[1];
            }
            return result;
        }

        float step = length / (numSample - 1);
        int segment = 0;
        float segmentStart = 0;

        for (int index = 0; index < numSample; index++)
        {
            float target = Math.min(length, index * step);

            while (segment < count - 2)
            {
                float segmentLength = segmentLength(segment);
                if (segmentStart + segmentLength >= target) break;
                segmentStart += segmentLength;
                segment++;
            }

            float segmentLength = segmentLength(segment);
            float fraction = segmentLength == 0 ? 0 : (target - segmentStart) / segmentLength;
            float startX = points[segment * 2];
            float startY = points[segment * 2 + 1];
            result[index * 2] = startX + (points[segment * 2 + 2] - startX) * fraction;
            result[index * 2 + 1] = startY + (points[segment * 2 + 3] - startY) * fraction;
        }

        return result;
    }

    private float segmentLength(int segment)
    {
        return (float)Math.hypot(points[segment * 2 + 2] - points[segment * 2], points[segment * 2 + 3] - points[segment * 2 + 1]);
    }

    public void clearPath()
    {
        mCachedPath = null;
    }

    public OrientedBoundingBox computeOrientedBoundingBox()
    {
        return computeOrientedBoundingBox(points);
    }

    static OrientedBoundingBox computeOrientedBoundingBox(float[] coordinates)
    {
        int count = coordinates.length / 2;
        if (count == 0) return new OrientedBoundingBox(0, 0, 0, 0, 0);

        float centreX = 0;
        float centreY = 0;
        for (int index = 0; index < count; index++)
        {
            centreX += coordinates[index * 2];
            centreY += coordinates[index * 2 + 1];
        }
        centreX /= count;
        centreY /= count;

        double xx = 0;
        double xy = 0;
        double yy = 0;
        for (int index = 0; index < count; index++)
        {
            double dx = coordinates[index * 2] - centreX;
            double dy = coordinates[index * 2 + 1] - centreY;
            xx += dx * dx;
            xy += dx * dy;
            yy += dy * dy;
        }

        double angle = xy == 0 && xx >= yy ? 0 : 0.5 * Math.atan2(2 * xy, xx - yy);
        double cosine = Math.cos(-angle);
        double sine = Math.sin(-angle);

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int index = 0; index < count; index++)
        {
            double dx = coordinates[index * 2] - centreX;
            double dy = coordinates[index * 2 + 1] - centreY;
            float x = (float)(dx * cosine - dy * sine);
            float y = (float)(dx * sine + dy * cosine);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        return new OrientedBoundingBox((float)Math.toDegrees(angle), centreX, centreY, maxX - minX, maxY - minY);
    }
}
