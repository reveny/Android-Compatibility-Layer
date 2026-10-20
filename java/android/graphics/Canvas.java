package android.graphics;

public class Canvas
{
    public static final int ALL_SAVE_FLAG = 0x1F;

    private final float[] mMatrixScratch = new float[9];

    private Path mScratchPath;
    private Paint mSharedPaint;
    private long mOwnedCanvas;
    private int[] mLayerSaveCounts = new int[8];
    private int mLayerDepth;
    private long mNativeCanvas;
    private int mWidth;
    private int mHeight;

    public Canvas()
    {
    }

    public Canvas(Bitmap bitmap)
    {
        setBitmap(bitmap);
    }

    public void setBitmap(Bitmap bitmap)
    {
        if (mOwnedCanvas != 0)
        {
            Bitmap.closeCanvas(mOwnedCanvas);
            mOwnedCanvas = 0;
        }

        if (bitmap == null)
        {
            setNativeCanvas(0, 0, 0);
            return;
        }

        mOwnedCanvas = bitmap.openCanvas();
        if (mOwnedCanvas == 0) throw new IllegalStateException("cannot draw into this bitmap");

        setNativeCanvas(mOwnedCanvas, bitmap.getWidth(), bitmap.getHeight());
    }

    @Override
    protected void finalize() throws Throwable
    {
        try
        {
            if (mOwnedCanvas != 0)
            {
                Bitmap.closeCanvas(mOwnedCanvas);
                mOwnedCanvas = 0;
            }
        }
        finally
        {
            super.finalize();
        }
    }

    public void setNativeCanvas(long nativeCanvas, int width, int height)
    {
        mNativeCanvas = nativeCanvas;
        mWidth = width;
        mHeight = height;
        mLayerDepth = 0;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public boolean isHardwareAccelerated()
    {
        return false;
    }

    public void enableZ()
    {
    }

    public void disableZ()
    {
    }

    public boolean getClipBounds(Rect bounds)
    {
        if (bounds == null) return !isClipEmpty();

        int[] edges = new int[4];
        nativeClipBounds(mNativeCanvas, edges);
        bounds.set(edges[0], edges[1], edges[2], edges[3]);

        return !bounds.isEmpty();
    }

    public Rect getClipBounds()
    {
        Rect bounds = new Rect();
        return getClipBounds(bounds) ? bounds : null;
    }

    public boolean isClipEmpty()
    {
        Rect bounds = new Rect();
        getClipBounds(bounds);

        return bounds.isEmpty();
    }

    public boolean quickReject(float left, float top, float right, float bottom)
    {
        Rect bounds = new Rect();
        if (!getClipBounds(bounds)) return true;

        return right <= bounds.left || left >= bounds.right || bottom <= bounds.top || top >= bounds.bottom;
    }

    public boolean quickReject(RectF rect)
    {
        return rect == null || quickReject(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean quickReject(Path path)
    {
        return false;
    }

    public boolean isOpaque()
    {
        return false;
    }

    public int getMaximumBitmapWidth()
    {
        return 16384;
    }

    public int getMaximumBitmapHeight()
    {
        return 16384;
    }

    public void setDensity(int density)
    {
    }

    public int getDensity()
    {
        return 160;
    }

    public int save()
    {
        sDrawOperations++;

        int saveCount = nativeSave(mNativeCanvas);
        if (saveCount < 0) throw new IllegalStateException("Canvas save stack is full");
        return saveCount;
    }

    public void restore()
    {
        if (mLayerDepth > 0 && mLayerSaveCounts[mLayerDepth - 1] == nativeGetSaveCount(mNativeCanvas) - 1)
        {
            mLayerDepth--;
            nativeRestoreLayer(mNativeCanvas);
            return;
        }

        nativeRestore(mNativeCanvas);
    }

    private void rememberLayer(int saveCount)
    {
        if (mLayerDepth == mLayerSaveCounts.length)
        {
            int[] grown = new int[mLayerSaveCounts.length * 2];
            System.arraycopy(mLayerSaveCounts, 0, grown, 0, mLayerSaveCounts.length);
            mLayerSaveCounts = grown;
        }

        mLayerSaveCounts[mLayerDepth++] = saveCount;
    }

    public void restoreToCount(int saveCount)
    {
        if (saveCount < 1) throw new IllegalArgumentException("Underflow in restoreToCount: " + saveCount);

        while (nativeGetSaveCount(mNativeCanvas) > saveCount)
        {
            int before = nativeGetSaveCount(mNativeCanvas);
            restore();

            if (nativeGetSaveCount(mNativeCanvas) >= before) return;
        }
    }

    public int getSaveCount()
    {
        return nativeGetSaveCount(mNativeCanvas);
    }

    public void translate(float dx, float dy)
    {
        if (dx == 0 && dy == 0) return;

        if (dx == (int)dx && dy == (int)dy)
        {
            nativeTranslate(mNativeCanvas, (int)dx, (int)dy);
            return;
        }

        mMatrixScratch[Matrix.MSCALE_X] = 1;
        mMatrixScratch[Matrix.MSKEW_X] = 0;
        mMatrixScratch[Matrix.MTRANS_X] = dx;
        mMatrixScratch[Matrix.MSKEW_Y] = 0;
        mMatrixScratch[Matrix.MSCALE_Y] = 1;
        mMatrixScratch[Matrix.MTRANS_Y] = dy;
        mMatrixScratch[Matrix.MPERSP_0] = 0;
        mMatrixScratch[Matrix.MPERSP_1] = 0;
        mMatrixScratch[Matrix.MPERSP_2] = 1;

        nativeConcat(mNativeCanvas, mMatrixScratch);
    }

    public void scale(float sx, float sy)
    {
        Matrix step = new Matrix();
        step.setScale(sx, sy);
        concat(step);
    }

    public void scale(float sx, float sy, float pivotX, float pivotY)
    {
        Matrix step = new Matrix();
        step.setScale(sx, sy, pivotX, pivotY);
        concat(step);
    }

    public void rotate(float degrees)
    {
        Matrix step = new Matrix();
        step.setRotate(degrees);
        concat(step);
    }

    public void rotate(float degrees, float pivotX, float pivotY)
    {
        Matrix step = new Matrix();
        step.setRotate(degrees, pivotX, pivotY);
        concat(step);
    }

    public void skew(float kx, float ky)
    {
        Matrix step = new Matrix();
        step.setSkew(kx, ky);
        concat(step);
    }

    public void concat(Matrix matrix)
    {
        if (matrix == null) return;

        matrix.getValues(mMatrixScratch);
        nativeConcat(mNativeCanvas, mMatrixScratch);
    }

    public void setMatrix(Matrix matrix)
    {
        if (matrix == null) new Matrix().getValues(mMatrixScratch);
        else matrix.getValues(mMatrixScratch);

        nativeSetMatrix(mNativeCanvas, mMatrixScratch);
    }

    public void getMatrix(Matrix into)
    {
        if (into == null) return;

        float[] values = new float[9];
        values[Matrix.MPERSP_2] = 1;

        nativeGetMatrix(mNativeCanvas, values);
        into.setValues(values);
    }

    public Matrix getMatrix()
    {
        Matrix result = new Matrix();
        getMatrix(result);

        return result;
    }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint)
    {
        int alpha = paint == null ? 255 : paint.getAlpha();
        int blend = paint == null ? 0 : paint.getBlendMode().nativeId();

        int saved = nativeSaveLayer(mNativeCanvas, Math.round(left), Math.round(top), Math.round(right),
                                    Math.round(bottom), alpha, blend);

        if (saved < 0) throw new IllegalStateException("Canvas cannot open another layer");

        rememberLayer(saved);
        return saved + 1;
    }

    public int saveLayer(RectF bounds, Paint paint)
    {
        if (bounds == null) return saveLayer(0, 0, mWidth, mHeight, paint);

        return saveLayer(bounds.left, bounds.top, bounds.right, bounds.bottom, paint);
    }

    public int saveLayer(RectF bounds, Paint paint, int saveFlags)
    {
        return saveLayer(bounds, paint);
    }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint, int saveFlags)
    {
        return saveLayer(left, top, right, bottom, paint);
    }

    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha)
    {
        int saved = nativeSaveLayer(mNativeCanvas, Math.round(left), Math.round(top), Math.round(right),
                                    Math.round(bottom), alpha & 0xFF, 0);

        if (saved < 0) throw new IllegalStateException("Canvas cannot open another layer");

        rememberLayer(saved);
        return saved + 1;
    }

    public int saveLayerAlpha(RectF bounds, int alpha)
    {
        if (bounds == null) return saveLayerAlpha(0, 0, mWidth, mHeight, alpha);

        return saveLayerAlpha(bounds.left, bounds.top, bounds.right, bounds.bottom, alpha);
    }

    public int saveLayerAlpha(RectF bounds, int alpha, int saveFlags)
    {
        return saveLayerAlpha(bounds, alpha);
    }

    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha, int saveFlags)
    {
        return saveLayerAlpha(left, top, right, bottom, alpha);
    }

    public boolean clipRect(int left, int top, int right, int bottom)
    {
        return nativeClipRect(mNativeCanvas, left, top, right, bottom);
    }

    public boolean clipRect(float left, float top, float right, float bottom)
    {
        return clipRect(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
    }

    public boolean clipRect(Rect rect)
    {
        return clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipRect(RectF rect)
    {
        return rect != null && clipRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipRect(float left, float top, float right, float bottom, Region.Op op)
    {
        if (op == Region.Op.DIFFERENCE) return clipOutRect(left, top, right, bottom);

        return clipRect(left, top, right, bottom);
    }

    public boolean clipRect(Rect rect, Region.Op op)
    {
        return rect != null && clipRect(rect.left, rect.top, rect.right, rect.bottom, op);
    }

    public boolean clipRect(RectF rect, Region.Op op)
    {
        return rect != null && clipRect(rect.left, rect.top, rect.right, rect.bottom, op);
    }

    public void drawColor(int color)
    {
        nativeDrawColor(mNativeCanvas, color);
    }

    public void drawRGB(int red, int green, int blue)
    {
        drawColor(Color.rgb(red, green, blue));
    }

    public void drawARGB(int alpha, int red, int green, int blue)
    {
        drawColor(Color.argb(alpha, red, green, blue));
    }

    public void drawPaint(Paint paint)
    {
        drawColor(paint.getColor());
    }

    public void drawPath(Path path, Paint paint)
    {
        if (path == null || paint == null) return;

        sDrawOperations++;
        traceOperation("drawPath", path, paint, -1);

        nativeDrawPath(mNativeCanvas, path.nativeHandle(), path.getFillType().ordinal(), paint.snapshotInts(),
                       paint.snapshotFloats(), paint.snapshotStopColors(), paint.snapshotStopPositions(),
                       paint.blurRadius(), paint.dashIntervals(), paint.dashPhase(), paint.shaderBitmap());
    }

    private Path scratchPath()
    {
        if (mScratchPath == null) mScratchPath = new Path();

        return mScratchPath;
    }

    private void drawScratch(Paint paint)
    {
        drawScratch(paint, -1);
    }

    public static int sDrawOperations;

    static final boolean TRACE_OPS = false;

    private static void traceOperation(String what, Path path, Paint paint, int styleOverride)
    {
        if (!TRACE_OPS) return;

        RectF bounds = new RectF();
        if (path != null) path.computeBounds(bounds, true);

        int style = styleOverride >= 0 ? styleOverride : paint.getStyle().ordinal();

        android.util.Log.i("CanvasOps", what + " bounds=" + Math.round(bounds.left) + "," + Math.round(bounds.top) + "-"
                                        + Math.round(bounds.right) + "," + Math.round(bounds.bottom)
                                        + " colour=" + Integer.toHexString(paint.getColor()) + " style=" + style
                                        + " shader=" + (paint.getShader() != null)
                                        + " filter=" + (paint.getColorFilter() != null));
    }

    private void drawScratch(Paint paint, int styleOverride)
    {
        sDrawOperations++;
        traceOperation("drawShape", mScratchPath, paint, styleOverride);
        nativeDrawPath(mNativeCanvas, scratchPath().nativeHandle(), Path.FillType.WINDING.ordinal(),
                       paint.snapshotInts(styleOverride), paint.snapshotFloats(), paint.snapshotStopColors(),
                       paint.snapshotStopPositions(), paint.blurRadius(), paint.dashIntervals(), paint.dashPhase(),
                       paint.shaderBitmap());
    }

    public void drawRect(float left, float top, float right, float bottom, Paint paint)
    {
        if (paint == null) return;

        scratchPath().reset();
        mScratchPath.addRect(left, top, right, bottom, Path.Direction.CW);
        drawScratch(paint);
    }

    public void drawRect(int left, int top, int right, int bottom, Paint paint)
    {
        drawRect((float) left, (float) top, (float) right, (float) bottom, paint);
    }

    public void drawRect(Rect rect, Paint paint)
    {
        if (rect != null) drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    public void drawRect(RectF rect, Paint paint)
    {
        if (rect != null) drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    public void drawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY, Paint paint)
    {
        if (paint == null) return;

        scratchPath().reset();
        mScratchPath.addRoundRect(left, top, right, bottom, radiusX, radiusY, Path.Direction.CW);
        drawScratch(paint);
    }

    public void drawRoundRect(RectF rect, float radiusX, float radiusY, Paint paint)
    {
        if (rect != null) drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, radiusX, radiusY, paint);
    }

    public void drawOval(float left, float top, float right, float bottom, Paint paint)
    {
        if (paint == null) return;

        scratchPath().reset();
        mScratchPath.addOval(left, top, right, bottom, Path.Direction.CW);
        drawScratch(paint);
    }

    public void drawOval(RectF rect, Paint paint)
    {
        if (rect != null) drawOval(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    public void drawCircle(float centreX, float centreY, float radius, Paint paint)
    {
        drawOval(centreX - radius, centreY - radius, centreX + radius, centreY + radius, paint);
    }

    public void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                        boolean useCentre, Paint paint)
    {
        if (paint == null) return;

        scratchPath().reset();

        if (useCentre)
        {
            mScratchPath.moveTo((left + right) * 0.5f, (top + bottom) * 0.5f);
            mScratchPath.arcTo(left, top, right, bottom, startAngle, sweepAngle, false);
            mScratchPath.close();
        }
        else
        {
            mScratchPath.addArc(left, top, right, bottom, startAngle, sweepAngle);
        }

        drawScratch(paint);
    }

    public void drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCentre, Paint paint)
    {
        if (oval != null) drawArc(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, useCentre, paint);
    }

    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint)
    {
        if (paint == null) return;

        scratchPath().reset();
        mScratchPath.moveTo(startX, startY);
        mScratchPath.lineTo(stopX, stopY);

        drawScratch(paint, Paint.Style.STROKE.ordinal());
    }

    public void drawLines(float[] points, Paint paint)
    {
        if (points != null) drawLines(points, 0, points.length, paint);
    }

    public void drawLines(float[] points, int offset, int count, Paint paint)
    {
        if (points == null || paint == null) return;

        for (int index = offset; index + 3 < offset + count && index + 3 < points.length; index += 4)
        {
            drawLine(points[index], points[index + 1], points[index + 2], points[index + 3], paint);
        }
    }

    public void drawPoint(float x, float y, Paint paint)
    {
        if (paint == null) return;

        float radius = Math.max(paint.getStrokeWidth(), 1.0f) * 0.5f;

        scratchPath().reset();
        mScratchPath.addOval(x - radius, y - radius, x + radius, y + radius, Path.Direction.CW);

        drawScratch(paint, Paint.Style.FILL.ordinal());
    }

    public void drawPoints(float[] points, Paint paint)
    {
        if (points != null) drawPoints(points, 0, points.length, paint);
    }

    public void drawPoints(float[] points, int offset, int count, Paint paint)
    {
        if (points == null || paint == null) return;

        for (int index = offset; index + 1 < offset + count && index + 1 < points.length; index += 2)
        {
            drawPoint(points[index], points[index + 1], paint);
        }
    }

    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint)
    {
        if (bitmap == null) return;

        drawBitmap(bitmap, null, new RectF(left, top, left + bitmap.getWidth(), top + bitmap.getHeight()), paint);
    }

    public void drawBitmap(Bitmap bitmap, Rect source, Rect destination, Paint paint)
    {
        if (destination == null) return;

        drawBitmap(bitmap, source, new RectF(destination.left, destination.top, destination.right, destination.bottom),
                   paint);
    }

    public void drawBitmap(Bitmap bitmap, Rect source, RectF destination, Paint paint)
    {
        if (bitmap == null || destination == null) return;

        float sourceLeft = source == null ? 0 : source.left;
        float sourceTop = source == null ? 0 : source.top;
        float sourceRight = source == null ? bitmap.getWidth() : source.right;
        float sourceBottom = source == null ? bitmap.getHeight() : source.bottom;

        Paint used = paint == null ? sharedPaint() : paint;

        if (TRACE_OPS)
        {
            android.util.Log.i("CanvasOps", "drawBitmap " + bitmap.getWidth() + "x" + bitmap.getHeight() + " into "
                                            + Math.round(destination.left) + "," + Math.round(destination.top) + "-"
                                            + Math.round(destination.right) + "," + Math.round(destination.bottom));
        }

        nativeDrawBitmap(mNativeCanvas, bitmap.nativeHandle(), sourceLeft, sourceTop, sourceRight, sourceBottom,
                         destination.left, destination.top, destination.right, destination.bottom, used.snapshotInts(),
                         used.snapshotFloats());
    }

    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint)
    {
        if (bitmap == null) return;

        int saveCount = save();
        if (matrix != null) concat(matrix);

        drawBitmap(bitmap, 0, 0, paint);
        restoreToCount(saveCount);
    }

    private Paint sharedPaint()
    {
        if (mSharedPaint == null) mSharedPaint = new Paint();

        return mSharedPaint;
    }

    public void drawRenderNode(RenderNode node)
    {
        if (node != null) node.drawInto(this);
    }

    public boolean clipPath(Path path)
    {
        return clipPath(path, Region.Op.INTERSECT);
    }

    public boolean clipPath(Path path, Region.Op op)
    {
        if (path == null) return false;

        boolean subtract = op == Region.Op.DIFFERENCE;
        return nativeClipPath(mNativeCanvas, path.nativeHandle(), path.getFillType().ordinal(), subtract);
    }

    public boolean clipOutPath(Path path)
    {
        return clipPath(path, Region.Op.DIFFERENCE);
    }

    public boolean clipOutRect(float left, float top, float right, float bottom)
    {
        scratchPath().reset();
        mScratchPath.addRect(left, top, right, bottom, Path.Direction.CW);

        return nativeClipPath(mNativeCanvas, mScratchPath.nativeHandle(), Path.FillType.WINDING.ordinal(), true);
    }

    public boolean clipOutRect(Rect rect)
    {
        return rect != null && clipOutRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipOutRect(RectF rect)
    {
        return rect != null && clipOutRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public boolean clipOutRect(int left, int top, int right, int bottom)
    {
        return clipOutRect((float) left, (float) top, (float) right, (float) bottom);
    }

    public void drawText(String text, float x, float y, Paint paint)
    {
        if (text == null || text.isEmpty()) return;
        drawText(text.toCharArray(), 0, text.length(), x, y, paint);
    }

    public void drawText(CharSequence text, int start, int end, float x, float y, Paint paint)
    {
        if (text == null || end <= start) return;

        char[] chars = new char[end - start];
        for (int index = start; index < end; index++)
        {
            chars[index - start] = text.charAt(index);
        }

        drawText(chars, 0, chars.length, x, y, paint);
    }

    public void drawText(char[] text, int index, int count, float x, float y, Paint paint)
    {
        if (text == null || count <= 0 || paint == null) return;
        nativeDrawTextRun(mNativeCanvas, paint.styleId(), text, index, count, x, y, paint.getColor(), false);
    }

    public void drawTextRun(char[] text, int index, int count, int contextIndex, int contextCount, float x, float y,
                            boolean rightToLeft, Paint paint)
    {
        if (text == null || count <= 0 || paint == null) return;
        nativeDrawTextRun(mNativeCanvas, paint.styleId(), text, index, count, x, y, paint.getColor(), rightToLeft);
    }

    private static native int nativeSave(long nativeCanvas);

    private static native boolean nativeRestore(long nativeCanvas);

    private static native int nativeGetSaveCount(long nativeCanvas);

    private static native void nativeTranslate(long nativeCanvas, int dx, int dy);

    private static native boolean nativeClipRect(long nativeCanvas, int left, int top, int right, int bottom);

    private static native void nativeDrawColor(long nativeCanvas, int color);

    private static native void nativeDrawRect(long nativeCanvas, int left, int top, int right, int bottom, int color);

    private static native void nativeDrawLine(long nativeCanvas, int startX, int startY, int stopX, int stopY, int color);

    private static native void nativeDrawTextRun(long nativeCanvas, int style, char[] text, int index, int count, float x, float y,
                                                 int color, boolean rightToLeft);

    private static native void nativeDrawPath(long nativeCanvas, long path, int fillType, int[] paintInts,
                                              float[] paintFloats, int[] stopColors, float[] stopPositions,
                                              float blurRadius, float[] dashIntervals, float dashPhase,
                                              long shaderBitmap);

    private static native void nativeDrawBitmap(long nativeCanvas, long bitmap, float sourceLeft, float sourceTop,
                                                float sourceRight, float sourceBottom, float destinationLeft,
                                                float destinationTop, float destinationRight, float destinationBottom,
                                                int[] paintInts, float[] paintFloats);

    private static native void nativeConcat(long nativeCanvas, float[] matrix);

    private static native void nativeSetMatrix(long nativeCanvas, float[] matrix);

    private static native void nativeGetMatrix(long nativeCanvas, float[] into);

    private static native boolean nativeClipPath(long nativeCanvas, long path, int fillType, boolean subtract);

    private static native int nativeSaveLayer(long nativeCanvas, int left, int top, int right, int bottom, int alpha,
                                              int blend);

    private static native void nativeRestoreLayer(long nativeCanvas);

    private static native int nativeLayerCount(long nativeCanvas);

    private static native void nativeClipBounds(long nativeCanvas, int[] into);
}
