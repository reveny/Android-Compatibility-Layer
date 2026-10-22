package android.graphics;

public class RecordingCanvas extends Canvas
{
    private final DisplayList mDisplayList;

    private int mRecordedWidth;
    private int mRecordedHeight;
    private int mSaveCount = 1;

    RecordingCanvas(DisplayList displayList)
    {
        mDisplayList = displayList;
    }

    void beginRecording(int width, int height)
    {
        mDisplayList.clear();
        mRecordedWidth = width;
        mRecordedHeight = height;
        mSaveCount = 1;
    }

    @Override
    public int getWidth()
    {
        return mRecordedWidth;
    }

    @Override
    public int getHeight()
    {
        return mRecordedHeight;
    }

    public boolean isHardwareAccelerated()
    {
        return true;
    }

    public boolean isOpaque()
    {
        return false;
    }

    public void enableZ()
    {
    }

    public void disableZ()
    {
    }

    @Override
    public int save()
    {
        mDisplayList.record(target -> target.save());
        return mSaveCount++;
    }

    @Override
    public void restore()
    {
        if (mSaveCount <= 1) return;

        mSaveCount--;
        mDisplayList.record(target -> target.restore());
    }

    @Override
    public void restoreToCount(int saveCount)
    {
        while (mSaveCount > saveCount) restore();
    }

    @Override
    public int getSaveCount()
    {
        return mSaveCount;
    }

    @Override
    public int saveLayer(float left, float top, float right, float bottom, Paint paint)
    {
        Paint copy = paint == null ? null : new Paint(paint);
        mDisplayList.record(target -> target.saveLayer(left, top, right, bottom, copy));

        return mSaveCount++;
    }

    @Override
    public int saveLayerAlpha(float left, float top, float right, float bottom, int alpha)
    {
        mDisplayList.record(target -> target.saveLayerAlpha(left, top, right, bottom, alpha));

        return mSaveCount++;
    }

    @Override
    public void translate(float dx, float dy)
    {
        mDisplayList.record(target -> target.translate(dx, dy));
    }

    @Override
    public void concat(Matrix matrix)
    {
        if (matrix == null) return;

        Matrix copy = new Matrix(matrix);
        mDisplayList.record(target -> target.concat(copy));
    }

    @Override
    public void setMatrix(Matrix matrix)
    {
        Matrix copy = matrix == null ? new Matrix() : new Matrix(matrix);
        mDisplayList.record(target -> target.setMatrix(copy));
    }

    @Override
    public boolean clipRect(int left, int top, int right, int bottom)
    {
        mDisplayList.record(target -> target.clipRect(left, top, right, bottom));
        return true;
    }

    @Override
    public boolean clipPath(Path path, Region.Op op)
    {
        if (path == null) return false;

        Path copy = new Path(path);
        mDisplayList.record(target -> target.clipPath(copy, op));

        return true;
    }

    @Override
    public boolean clipOutRect(float left, float top, float right, float bottom)
    {
        mDisplayList.record(target -> target.clipOutRect(left, top, right, bottom));
        return true;
    }

    @Override
    public void drawColor(int color)
    {
        mDisplayList.record(target -> target.drawColor(color));
    }

    @Override
    public void drawColor(int color, BlendMode mode)
    {
        mDisplayList.record(target -> target.drawColor(color, mode));
    }

    @Override
    public boolean quickReject(float left, float top, float right, float bottom)
    {
        return right <= left || bottom <= top;
    }

    @Override
    public boolean quickReject(Path path)
    {
        return path == null || path.isEmpty();
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect source, RectF destination, Paint paint)
    {
        if (bitmap == null || destination == null) return;

        Rect sourceCopy = source == null ? null : new Rect(source);
        RectF destinationCopy = new RectF(destination);
        Paint copy = paint == null ? null : new Paint(paint);
        mDisplayList.record(target -> target.drawBitmap(bitmap, sourceCopy, destinationCopy, copy));
    }

    @Override
    public void drawPath(Path path, Paint paint)
    {
        if (path == null || paint == null) return;

        Path shape = new Path(path);
        Paint copy = new Paint(paint);
        mDisplayList.record(target -> target.drawPath(shape, copy));
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, Paint paint)
    {
        if (paint == null) return;

        Paint copy = new Paint(paint);
        mDisplayList.record(target -> target.drawRect(left, top, right, bottom, copy));
    }

    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY, Paint paint)
    {
        if (paint == null) return;

        Paint copy = new Paint(paint);
        mDisplayList.record(target -> target.drawRoundRect(left, top, right, bottom, radiusX, radiusY, copy));
    }

    @Override
    public void drawOval(float left, float top, float right, float bottom, Paint paint)
    {
        if (paint == null) return;

        Paint copy = new Paint(paint);
        mDisplayList.record(target -> target.drawOval(left, top, right, bottom, copy));
    }

    @Override
    public void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                        boolean useCentre, Paint paint)
    {
        if (paint == null) return;

        Paint copy = new Paint(paint);
        mDisplayList.record(target -> target.drawArc(left, top, right, bottom, startAngle, sweepAngle, useCentre, copy));
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint)
    {
        if (paint == null) return;

        Paint copy = new Paint(paint);
        mDisplayList.record(target -> target.drawLine(startX, startY, stopX, stopY, copy));
    }

    @Override
    public void drawPoint(float x, float y, Paint paint)
    {
        if (paint == null) return;

        Paint copy = new Paint(paint);
        mDisplayList.record(target -> target.drawPoint(x, y, copy));
    }

    @Override
    public void drawText(char[] text, int index, int count, float x, float y, Paint paint)
    {
        if (text == null || count <= 0 || paint == null) return;

        char[] copiedText = new char[count];
        System.arraycopy(text, index, copiedText, 0, count);

        Paint copy = new Paint(paint);
        mDisplayList.record(target -> target.drawText(copiedText, 0, copiedText.length, x, y, copy));
    }

    @Override
    public void drawTextRun(char[] text, int index, int count, int contextIndex, int contextCount, float x, float y,
                            boolean rightToLeft, Paint paint)
    {
        if (text == null || count <= 0 || paint == null) return;

        char[] copiedText = new char[count];
        System.arraycopy(text, index, copiedText, 0, count);

        Paint copy = new Paint(paint);
        mDisplayList.record(
            target -> target.drawTextRun(copiedText, 0, copiedText.length, 0, copiedText.length, x, y, rightToLeft, copy));
    }

    @Override
    public void drawRenderNode(RenderNode node)
    {
        if (node == null) return;

        mDisplayList.record(target -> target.drawRenderNode(node));
    }
}
