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
    private int mScreenDensity;
    private DrawFilter mDrawFilter;

    public enum EdgeType
    {
        BW(0),
        AA(1);

        final int nativeInt;

        EdgeType(int nativeInt)
        {
            this.nativeInt = nativeInt;
        }
    }

    public enum VertexMode
    {
        TRIANGLES(0),
        TRIANGLE_STRIP(1),
        TRIANGLE_FAN(2);

        public final int nativeInt;

        VertexMode(int nativeInt)
        {
            this.nativeInt = nativeInt;
        }
    }

    public Canvas()
    {
    }

    public long getNativeCanvasWrapper()
    {
        return mNativeCanvas;
    }

    public void setScreenDensity(int density)
    {
        mScreenDensity = density;
    }

    public DrawFilter getDrawFilter()
    {
        return mDrawFilter;
    }

    public void setDrawFilter(DrawFilter filter)
    {
        mDrawFilter = filter;
    }

    private Paint filtered(Paint paint)
    {
        return mDrawFilter == null || paint == null ? paint : mDrawFilter.filter(paint);
    }

    public int save(int saveFlags)
    {
        return save();
    }

    public boolean quickReject(RectF rect, EdgeType type)
    {
        return quickReject(rect);
    }

    public boolean quickReject(Path path, EdgeType type)
    {
        return quickReject(path);
    }

    public boolean quickReject(float left, float top, float right, float bottom, EdgeType type)
    {
        return quickReject(left, top, right, bottom);
    }

    public boolean clipRegion(Region region, Region.Op op)
    {
        if (region == null) return false;

        Path boundary = new Path();
        region.getBoundaryPath(boundary);

        if (op == Region.Op.DIFFERENCE) return clipPath(boundary, Region.Op.DIFFERENCE);
        if (op == null || op == Region.Op.INTERSECT) return clipPath(boundary, Region.Op.INTERSECT);

        return !isClipEmpty();
    }

    public boolean clipRegion(Region region)
    {
        return clipRegion(region, Region.Op.INTERSECT);
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
        if (path == null) return true;
        if (path.isInverseFillType()) return false;

        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        if (path.isEmpty()) return true;

        return quickReject(bounds.left, bounds.top, Math.max(bounds.right, bounds.left + 1), Math.max(bounds.bottom, bounds.top + 1));
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

    public void drawColor(long color)
    {
        drawColor(Color.toArgb(color));
    }

    public void drawColor(int color, PorterDuff.Mode mode)
    {
        drawColor(color, BlendMode.fromPorterDuff(mode));
    }

    public void drawColor(long color, BlendMode mode)
    {
        drawColor(Color.toArgb(color), mode);
    }

    public void drawColor(int color, BlendMode mode)
    {
        if (mode == null || mode == BlendMode.SRC_OVER)
        {
            drawColor(color);
            return;
        }

        Paint fill = new Paint();
        fill.setColor(color);
        fill.setBlendMode(mode);
        fillVisibleArea(fill);
    }

    private void fillVisibleArea(Paint paint)
    {
        RectF area = new RectF(0, 0, getWidth(), getHeight());

        Matrix inverse = new Matrix();
        if (getMatrix().invert(inverse)) inverse.mapRect(area);

        drawRect(area, paint);
    }

    public void drawPaint(Paint paint)
    {
        if (paint == null) return;

        boolean plain = paint.getShader() == null && paint.getColorFilter() == null && paint.getBlendMode() == BlendMode.SRC_OVER;

        if (plain)
        {
            drawColor(paint.getColor());
            return;
        }

        Paint fill = new Paint(paint);
        fill.setStyle(Paint.Style.FILL);
        fill.setMaskFilter(null);
        fill.setPathEffect(null);
        fillVisibleArea(fill);
    }

    public void drawDoubleRoundRect(RectF outer, float outerRx, float outerRy, RectF inner, float innerRx, float innerRy,
                                    Paint paint)
    {
        if (outer == null || inner == null || paint == null) return;

        Path shape = new Path();
        shape.setFillType(Path.FillType.EVEN_ODD);
        shape.addRoundRect(outer, outerRx, outerRy, Path.Direction.CW);
        shape.addRoundRect(inner, innerRx, innerRy, Path.Direction.CCW);

        drawPath(shape, paint);
    }

    public void drawDoubleRoundRect(RectF outer, float[] outerRadii, RectF inner, float[] innerRadii, Paint paint)
    {
        if (outer == null || inner == null || paint == null) return;
        if (outerRadii == null || innerRadii == null || outerRadii.length != 8 || innerRadii.length != 8)
        {
            throw new IllegalArgumentException("Both inner and outer radii arrays must contain exactly 8 values");
        }

        Path shape = new Path();
        shape.setFillType(Path.FillType.EVEN_ODD);
        shape.addRoundRect(outer, outerRadii, Path.Direction.CW);
        shape.addRoundRect(inner, innerRadii, Path.Direction.CCW);

        drawPath(shape, paint);
    }

    public void drawPatch(NinePatch patch, Rect destination, Paint paint)
    {
        if (patch == null || destination == null) return;

        patch.drawInto(this, destination.left, destination.top, destination.right, destination.bottom, paint);
    }

    public void drawPatch(NinePatch patch, RectF destination, Paint paint)
    {
        if (patch == null || destination == null) return;

        patch.drawInto(this, destination.left, destination.top, destination.right, destination.bottom, paint);
    }

    public void drawBitmap(int[] colors, int offset, int stride, float x, float y, int width, int height, boolean hasAlpha,
                           Paint paint)
    {
        if (width < 0) throw new IllegalArgumentException("width must be >= 0");
        if (height < 0) throw new IllegalArgumentException("height must be >= 0");
        if (Math.abs(stride) < width) throw new IllegalArgumentException("abs(stride) must be >= width");

        int lastScanline = offset + (height - 1) * stride;
        int length = colors.length;

        if (offset < 0 || (offset + width > length) || lastScanline < 0 || (lastScanline + width > length))
        {
            throw new ArrayIndexOutOfBoundsException();
        }

        if (width == 0 || height == 0) return;

        Bitmap pixels = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, hasAlpha);
        pixels.setPixels(colors, offset, stride, 0, 0, width, height);

        drawBitmap(pixels, x, y, paint);
    }

    public void drawBitmap(int[] colors, int offset, int stride, int x, int y, int width, int height, boolean hasAlpha,
                           Paint paint)
    {
        drawBitmap(colors, offset, stride, (float) x, (float) y, width, height, hasAlpha, paint);
    }

    public void drawBitmapMesh(Bitmap bitmap, int meshWidth, int meshHeight, float[] verts, int vertOffset, int[] colors,
                               int colorOffset, Paint paint)
    {
        if ((meshWidth | meshHeight | vertOffset | colorOffset) < 0) throw new ArrayIndexOutOfBoundsException();
        if (bitmap == null || meshWidth == 0 || meshHeight == 0) return;

        int count = (meshWidth + 1) * (meshHeight + 1);
        checkArrayRange(verts.length, vertOffset, count * 2);
        if (colors != null) checkArrayRange(colors.length, colorOffset, count);

        Paint textured = paint == null ? new Paint() : new Paint(paint);
        textured.setStyle(Paint.Style.FILL);
        textured.setAntiAlias(false);
        textured.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        float cellWidth = bitmap.getWidth() / (float) meshWidth;
        float cellHeight = bitmap.getHeight() / (float) meshHeight;

        float[] source = new float[6];
        float[] destination = new float[6];

        for (int row = 0; row < meshHeight; row++)
        {
            for (int column = 0; column < meshWidth; column++)
            {
                int topLeft = vertOffset + (row * (meshWidth + 1) + column) * 2;
                int topRight = topLeft + 2;
                int bottomLeft = topLeft + (meshWidth + 1) * 2;
                int bottomRight = bottomLeft + 2;

                float left = column * cellWidth;
                float top = row * cellHeight;
                float right = left + cellWidth;
                float bottom = top + cellHeight;

                setTriangle(source, left, top, right, top, left, bottom);
                setTriangle(destination, verts[topLeft], verts[topLeft + 1], verts[topRight], verts[topRight + 1],
                            verts[bottomLeft], verts[bottomLeft + 1]);
                drawMappedTriangle(source, destination, textured);

                setTriangle(source, right, top, right, bottom, left, bottom);
                setTriangle(destination, verts[topRight], verts[topRight + 1], verts[bottomRight], verts[bottomRight + 1],
                            verts[bottomLeft], verts[bottomLeft + 1]);
                drawMappedTriangle(source, destination, textured);
            }
        }
    }

    public void drawVertices(VertexMode mode, int vertexCount, float[] verts, int vertOffset, float[] texs, int texOffset,
                             int[] colors, int colorOffset, short[] indices, int indexOffset, int indexCount, Paint paint)
    {
        checkArrayRange(verts.length, vertOffset, vertexCount);
        if (texs != null) checkArrayRange(texs.length, texOffset, vertexCount);
        if (colors != null) checkArrayRange(colors.length, colorOffset, vertexCount / 2);
        if (indices != null) checkArrayRange(indices.length, indexOffset, indexCount);
        if (mode == null || paint == null) return;

        int vertices = vertexCount / 2;
        int[] order;

        if (indices != null)
        {
            order = new int[indexCount];
            for (int index = 0; index < indexCount; index++) order[index] = indices[indexOffset + index] & 0xFFFF;
        }
        else
        {
            order = new int[vertices];
            for (int index = 0; index < vertices; index++) order[index] = index;
        }

        int triangles;
        if (mode == VertexMode.TRIANGLES) triangles = order.length / 3;
        else triangles = Math.max(0, order.length - 2);

        boolean textured = texs != null && paint.getShader() != null;

        Paint fill = new Paint(paint);
        fill.setStyle(Paint.Style.FILL);
        fill.setMaskFilter(null);
        fill.setPathEffect(null);
        if (!textured) fill.setShader(null);

        float[] source = new float[6];
        float[] destination = new float[6];
        int[] corners = new int[3];

        for (int triangle = 0; triangle < triangles; triangle++)
        {
            if (mode == VertexMode.TRIANGLES)
            {
                corners[0] = order[triangle * 3];
                corners[1] = order[triangle * 3 + 1];
                corners[2] = order[triangle * 3 + 2];
            }
            else if (mode == VertexMode.TRIANGLE_STRIP)
            {
                corners[0] = order[triangle];
                corners[1] = order[triangle + 1];
                corners[2] = order[triangle + 2];
            }
            else
            {
                corners[0] = order[0];
                corners[1] = order[triangle + 1];
                corners[2] = order[triangle + 2];
            }

            if (corners[0] >= vertices || corners[1] >= vertices || corners[2] >= vertices) continue;

            for (int corner = 0; corner < 3; corner++)
            {
                destination[corner * 2] = verts[vertOffset + corners[corner] * 2];
                destination[corner * 2 + 1] = verts[vertOffset + corners[corner] * 2 + 1];

                if (textured)
                {
                    source[corner * 2] = texs[texOffset + corners[corner] * 2];
                    source[corner * 2 + 1] = texs[texOffset + corners[corner] * 2 + 1];
                }
            }

            if (colors != null && !textured)
            {
                fill.setColor(averageColor(colors[colorOffset + corners[0]], colors[colorOffset + corners[1]],
                                           colors[colorOffset + corners[2]]));
            }

            if (textured)
            {
                drawMappedTriangle(source, destination, fill);
                continue;
            }

            Path shape = new Path();
            shape.moveTo(destination[0], destination[1]);
            shape.lineTo(destination[2], destination[3]);
            shape.lineTo(destination[4], destination[5]);
            shape.close();
            drawPath(shape, fill);
        }
    }

    public void drawMesh(Mesh mesh, BlendMode blendMode, Paint paint)
    {
        if (mesh == null || paint == null) return;

        Path shape = mesh.trianglePath();
        if (shape == null) return;

        Paint fill = new Paint(paint);
        fill.setStyle(Paint.Style.FILL);
        drawPath(shape, fill);
    }

    private static void checkArrayRange(int length, int offset, int count)
    {
        if ((offset | count) < 0 || offset + count > length) throw new ArrayIndexOutOfBoundsException();
    }

    private static void setTriangle(float[] into, float firstX, float firstY, float secondX, float secondY, float thirdX,
                                    float thirdY)
    {
        into[0] = firstX;
        into[1] = firstY;
        into[2] = secondX;
        into[3] = secondY;
        into[4] = thirdX;
        into[5] = thirdY;
    }

    private static int averageColor(int first, int second, int third)
    {
        int alpha = (Color.alpha(first) + Color.alpha(second) + Color.alpha(third)) / 3;
        int red = (Color.red(first) + Color.red(second) + Color.red(third)) / 3;
        int green = (Color.green(first) + Color.green(second) + Color.green(third)) / 3;
        int blue = (Color.blue(first) + Color.blue(second) + Color.blue(third)) / 3;

        return Color.argb(alpha, red, green, blue);
    }

    private void drawMappedTriangle(float[] source, float[] destination, Paint paint)
    {
        float sourceAx = source[2] - source[0];
        float sourceAy = source[3] - source[1];
        float sourceBx = source[4] - source[0];
        float sourceBy = source[5] - source[1];

        float determinant = sourceAx * sourceBy - sourceBx * sourceAy;
        if (determinant == 0) return;

        float destinationAx = destination[2] - destination[0];
        float destinationAy = destination[3] - destination[1];
        float destinationBx = destination[4] - destination[0];
        float destinationBy = destination[5] - destination[1];

        float inverseA = sourceBy / determinant;
        float inverseB = -sourceBx / determinant;
        float inverseC = -sourceAy / determinant;
        float inverseD = sourceAx / determinant;

        float scaleX = destinationAx * inverseA + destinationBx * inverseC;
        float skewX = destinationAx * inverseB + destinationBx * inverseD;
        float skewY = destinationAy * inverseA + destinationBy * inverseC;
        float scaleY = destinationAy * inverseB + destinationBy * inverseD;

        float[] values = new float[9];
        values[Matrix.MSCALE_X] = scaleX;
        values[Matrix.MSKEW_X] = skewX;
        values[Matrix.MTRANS_X] = destination[0] - (scaleX * source[0] + skewX * source[1]);
        values[Matrix.MSKEW_Y] = skewY;
        values[Matrix.MSCALE_Y] = scaleY;
        values[Matrix.MTRANS_Y] = destination[1] - (skewY * source[0] + scaleY * source[1]);
        values[Matrix.MPERSP_2] = 1;

        Matrix mapping = new Matrix();
        mapping.setValues(values);

        Path shape = new Path();
        shape.moveTo(source[0], source[1]);
        shape.lineTo(source[2], source[3]);
        shape.lineTo(source[4], source[5]);
        shape.close();

        int saveCount = save();
        concat(mapping);
        drawPath(shape, paint);
        restoreToCount(saveCount);
    }

    public void drawPath(Path path, Paint paint)
    {
        if (path == null || paint == null) return;

        paint = filtered(paint);
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
        paint = filtered(paint);
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

        Paint used = filtered(paint == null ? sharedPaint() : paint);

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

    public void drawPicture(Picture picture)
    {
        if (picture != null) picture.draw(this);
    }

    public void drawPicture(Picture picture, RectF destination)
    {
        if (picture == null || destination == null) return;

        int saveCount = save();
        translate(destination.left, destination.top);

        if (picture.getWidth() > 0 && picture.getHeight() > 0)
        {
            scale(destination.width() / picture.getWidth(), destination.height() / picture.getHeight());
        }

        clipRect(0, 0, picture.getWidth(), picture.getHeight());
        picture.draw(this);
        restoreToCount(saveCount);
    }

    public void drawPicture(Picture picture, Rect destination)
    {
        if (destination != null) drawPicture(picture, new RectF(destination));
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

    public void drawText(String text, int start, int end, float x, float y, Paint paint)
    {
        if (text == null) return;
        if ((start | end | (end - start) | (text.length() - end)) < 0) throw new IndexOutOfBoundsException();
        if (end == start) return;

        drawText(text.toCharArray(), start, end - start, x, y, paint);
    }

    public void drawText(char[] text, int index, int count, float x, float y, Paint paint)
    {
        if (text == null || count <= 0 || paint == null) return;
        drawDecoratedText(text, index, count, x, y, false, paint);
    }

    public void drawTextRun(char[] text, int index, int count, int contextIndex, int contextCount, float x, float y,
                            boolean rightToLeft, Paint paint)
    {
        if (text == null || count <= 0 || paint == null) return;
        drawDecoratedText(text, index, count, x, y, rightToLeft, paint);
    }

    public void drawTextRun(CharSequence text, int start, int end, int contextStart, int contextEnd, float x, float y,
                            boolean isRtl, Paint paint)
    {
        if (text == null) throw new NullPointerException("text is null");
        if (paint == null) throw new NullPointerException("paint is null");
        if ((start | end | contextStart | contextEnd | start - contextStart | end - start | contextEnd - end
             | text.length() - contextEnd) < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        if (end == start) return;

        char[] chars = new char[contextEnd - contextStart];
        for (int index = contextStart; index < contextEnd; index++) chars[index - contextStart] = text.charAt(index);

        drawTextRun(chars, start - contextStart, end - start, 0, chars.length, x, y, isRtl, paint);
    }

    public void drawTextRun(android.graphics.text.MeasuredText text, int start, int end, int contextStart, int contextEnd, float x,
                            float y, boolean isRtl, Paint paint)
    {
        if (text == null) throw new NullPointerException("text is null");

        char[] chars = text.getChars();
        if ((start | end | contextStart | contextEnd | start - contextStart | end - start | contextEnd - end
             | chars.length - contextEnd) < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        drawTextRun(chars, start, end - start, contextStart, contextEnd - contextStart, x, y, isRtl, paint);
    }

    public void drawPosText(char[] text, int index, int count, float[] pos, Paint paint)
    {
        if (index < 0 || index + count > text.length || count * 2 > pos.length) throw new IndexOutOfBoundsException();
        if (paint == null) return;

        Paint placed = paint;
        if (paint.getTextAlign() != Paint.Align.LEFT)
        {
            placed = new Paint(paint);
            placed.setTextAlign(Paint.Align.LEFT);
        }

        for (int offset = 0; offset < count; offset++)
        {
            int length = Character.isHighSurrogate(text[index + offset]) && offset + 1 < count ? 2 : 1;
            drawText(text, index + offset, length, pos[offset * 2], pos[offset * 2 + 1], placed);
            offset += length - 1;
        }
    }

    public void drawPosText(String text, float[] pos, Paint paint)
    {
        drawPosText(text.toCharArray(), 0, text.length(), pos, paint);
    }

    public void drawTextOnPath(char[] text, int index, int count, Path path, float hOffset, float vOffset, Paint paint)
    {
        if (index < 0 || index + count > text.length) throw new ArrayIndexOutOfBoundsException();
        if (path == null || paint == null || count <= 0) return;

        float[] approximation = path.approximate(0.5f);
        int points = approximation.length / 3;

        float[] distances = new float[points];
        float length = 0;

        for (int point = 1; point < points; point++)
        {
            if (approximation[point * 3] > approximation[point * 3 - 3])
            {
                float dx = approximation[point * 3 + 1] - approximation[point * 3 - 2];
                float dy = approximation[point * 3 + 2] - approximation[point * 3 - 1];
                length += (float) Math.sqrt(dx * dx + dy * dy);
            }

            distances[point] = length;
        }

        if (length <= 0) return;

        float[] widths = new float[count];
        paint.getTextWidths(text, index, count, widths);

        float total = 0;
        for (float width : widths) total += width;

        float cursor = hOffset;
        if (paint.getTextAlign() == Paint.Align.CENTER) cursor += (length - total) * 0.5f;
        else if (paint.getTextAlign() == Paint.Align.RIGHT) cursor += length - total;

        Paint placed = new Paint(paint);
        placed.setTextAlign(Paint.Align.LEFT);

        int segment = 1;

        for (int offset = 0; offset < count; offset++)
        {
            int glyphLength = Character.isHighSurrogate(text[index + offset]) && offset + 1 < count ? 2 : 1;
            float advance = widths[offset] + (glyphLength == 2 ? widths[offset + 1] : 0);
            float middle = cursor + advance * 0.5f;

            cursor += advance;

            if (middle < 0 || middle > length)
            {
                offset += glyphLength - 1;
                continue;
            }

            while (segment < points - 1 && (distances[segment] < middle || distances[segment] == distances[segment - 1])) segment++;

            int previous = segment - 1;

            float span = distances[segment] - distances[previous];
            float ratio = span <= 0 ? 0 : (middle - distances[previous]) / span;

            float startX = approximation[previous * 3 + 1];
            float startY = approximation[previous * 3 + 2];
            float endX = approximation[segment * 3 + 1];
            float endY = approximation[segment * 3 + 2];

            float pointX = startX + (endX - startX) * ratio;
            float pointY = startY + (endY - startY) * ratio;
            float degrees = (float) Math.toDegrees(Math.atan2(endY - startY, endX - startX));

            int saveCount = save();
            translate(pointX, pointY);
            rotate(degrees);
            drawText(text, index + offset, glyphLength, -advance * 0.5f, vOffset, placed);
            restoreToCount(saveCount);

            offset += glyphLength - 1;
        }
    }

    public void drawTextOnPath(String text, Path path, float hOffset, float vOffset, Paint paint)
    {
        if (text == null || text.isEmpty()) return;

        drawTextOnPath(text.toCharArray(), 0, text.length(), path, hOffset, vOffset, paint);
    }

    public void drawGlyphs(int[] glyphIds, int glyphIdOffset, float[] positions, int positionOffset, int glyphCount,
                           android.graphics.fonts.Font font, Paint paint)
    {
        if (glyphIds == null || positions == null || font == null || paint == null) throw new NullPointerException();
        if ((glyphIdOffset | positionOffset | glyphCount) < 0 || glyphIdOffset + glyphCount > glyphIds.length
            || positionOffset + glyphCount * 2 > positions.length)
        {
            throw new IndexOutOfBoundsException();
        }
    }

    private void drawDecoratedText(char[] text, int index, int count, float x, float y, boolean rightToLeft, Paint paint)
    {
        boolean underline = paint.isUnderlineText();
        boolean strikeThrough = paint.isStrikeThruText();
        Paint.Align align = paint.getTextAlign();

        float width = 0;
        if (underline || strikeThrough || (align != null && align != Paint.Align.LEFT)) width = paint.measureText(text, index, count);

        float originX = x;
        if (align == Paint.Align.CENTER) originX -= width * 0.5f;
        else if (align == Paint.Align.RIGHT) originX -= width;

        int style = paint.styleId();

        if (paint.hasShadowLayer())
        {
            int shadowColor = paint.getShadowLayerColor();
            int shadowAlpha = (Color.alpha(shadowColor) * paint.getAlpha()) / 255;
            shadowColor = (shadowColor & 0x00FFFFFF) | (shadowAlpha << 24);

            nativeDrawTextRun(mNativeCanvas, style, text, index, count, originX + paint.getShadowLayerDx(),
                              y + paint.getShadowLayerDy(), shadowColor, rightToLeft);
        }

        nativeDrawTextRun(mNativeCanvas, style, text, index, count, originX, y, paint.getColor(), rightToLeft);

        if (!underline && !strikeThrough) return;

        Paint decoration = new Paint();
        decoration.setColor(paint.getColor());
        decoration.setStyle(Paint.Style.FILL);
        decoration.setColorFilter(paint.getColorFilter());

        if (underline)
        {
            float top = y + paint.getUnderlinePosition();
            drawRect(originX, top, originX + width, top + paint.getUnderlineThickness(), decoration);
        }

        if (strikeThrough)
        {
            float thickness = paint.getStrikeThruThickness();
            float top = y + paint.getStrikeThruPosition() - thickness * 0.5f;
            drawRect(originX, top, originX + width, top + thickness, decoration);
        }
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
