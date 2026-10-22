package android.gesture;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Gesture implements Parcelable
{
    private static final long GESTURE_ID_BASE = System.currentTimeMillis();
    private static final int BITMAP_RENDERING_WIDTH = 2;
    private static final boolean BITMAP_RENDERING_ANTIALIAS = true;
    private static final boolean BITMAP_RENDERING_DITHER = true;
    private static final AtomicInteger sGestureCount = new AtomicInteger(0);

    public static final Parcelable.Creator<Gesture> CREATOR = new Parcelable.Creator<Gesture>()
    {
        @Override
        public Gesture createFromParcel(Parcel source)
        {
            long identifier = source.readLong();
            int strokeCount = source.readInt();
            Gesture gesture = new Gesture();
            gesture.mGestureId = identifier;

            for (int strokeIndex = 0; strokeIndex < strokeCount; strokeIndex++)
            {
                int pointCount = source.readInt();
                ArrayList<GesturePoint> points = new ArrayList<>(pointCount);
                for (int pointIndex = 0; pointIndex < pointCount; pointIndex++)
                {
                    float x = source.readFloat();
                    float y = source.readFloat();
                    long time = source.readLong();
                    points.add(new GesturePoint(x, y, time));
                }
                gesture.addStroke(new GestureStroke(points));
            }

            return gesture;
        }

        @Override
        public Gesture[] newArray(int size)
        {
            return new Gesture[size];
        }
    };

    private final RectF mBoundingBox = new RectF();
    private final ArrayList<GestureStroke> mStrokes = new ArrayList<>();
    private long mGestureId;

    public Gesture()
    {
        mGestureId = GESTURE_ID_BASE + sGestureCount.incrementAndGet();
    }

    @Override
    public Object clone()
    {
        Gesture gesture = new Gesture();
        gesture.mBoundingBox.set(mBoundingBox);
        for (GestureStroke stroke : mStrokes) gesture.mStrokes.add((GestureStroke)stroke.clone());
        return gesture;
    }

    public ArrayList<GestureStroke> getStrokes()
    {
        return mStrokes;
    }

    public int getStrokesCount()
    {
        return mStrokes.size();
    }

    public void addStroke(GestureStroke stroke)
    {
        mStrokes.add(stroke);
        if (mStrokes.size() == 1) mBoundingBox.set(stroke.boundingBox);
        else mBoundingBox.union(stroke.boundingBox);
    }

    public float getLength()
    {
        float length = 0;
        for (GestureStroke stroke : mStrokes) length += stroke.length;
        return length;
    }

    public RectF getBoundingBox()
    {
        return mBoundingBox;
    }

    public Path toPath()
    {
        return toPath(null);
    }

    public Path toPath(Path path)
    {
        if (path == null) path = new Path();
        for (GestureStroke stroke : mStrokes) path.addPath(stroke.getPath());
        return path;
    }

    public Path toPath(int width, int height, int edge, int numSample)
    {
        return toPath(null, width, height, edge, numSample);
    }

    public Path toPath(Path path, int width, int height, int edge, int numSample)
    {
        if (path == null) path = new Path();
        for (GestureStroke stroke : mStrokes) path.addPath(stroke.toPath(width - 2 * edge, height - 2 * edge, numSample));
        return path;
    }

    public long getID()
    {
        return mGestureId;
    }

    public Bitmap toBitmap(int width, int height, int edge, int numSample, int color)
    {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(edge, edge);

        Paint paint = strokePaint(color);
        for (GestureStroke stroke : mStrokes) canvas.drawPath(stroke.toPath(width - 2 * edge, height - 2 * edge, numSample), paint);

        return bitmap;
    }

    public Bitmap toBitmap(int width, int height, int inset, int color)
    {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = strokePaint(color);

        Path path = toPath();
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        float scaleX = bounds.width() == 0 ? 1 : (width - 2 * inset) / bounds.width();
        float scaleY = bounds.height() == 0 ? 1 : (height - 2 * inset) / bounds.height();
        float scale = Math.min(scaleX, scaleY);
        paint.setStrokeWidth(2.0f / scale);

        path.offset(-bounds.left + (width - bounds.width() * scale) / 2.0f, -bounds.top + (height - bounds.height() * scale) / 2.0f);
        canvas.translate(inset, inset);
        canvas.scale(scale, scale);
        canvas.drawPath(path, paint);

        return bitmap;
    }

    private static Paint strokePaint(int color)
    {
        Paint paint = new Paint();
        paint.setAntiAlias(BITMAP_RENDERING_ANTIALIAS);
        paint.setDither(BITMAP_RENDERING_DITHER);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(BITMAP_RENDERING_WIDTH);
        return paint;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeLong(mGestureId);
        out.writeInt(mStrokes.size());

        for (GestureStroke stroke : mStrokes)
        {
            int pointCount = stroke.points.length / 2;
            out.writeInt(pointCount);
            for (int index = 0; index < pointCount; index++)
            {
                out.writeFloat(stroke.points[index * 2]);
                out.writeFloat(stroke.points[index * 2 + 1]);
                out.writeLong(stroke.timestampAt(index));
            }
        }
    }

    @Override
    public int describeContents()
    {
        return 0;
    }
}
