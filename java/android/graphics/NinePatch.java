package android.graphics;

public class NinePatch
{
    private static final int HEADER_SIZE = 32;

    private final Bitmap mBitmap;
    private final String mName;
    private final int[] mHorizontalDivs;
    private final int[] mVerticalDivs;
    private Paint mPaint;

    public NinePatch(Bitmap bitmap, byte[] chunk)
    {
        this(bitmap, chunk, null);
    }

    public NinePatch(Bitmap bitmap, byte[] chunk, String srcName)
    {
        mBitmap = bitmap;
        mName = srcName;

        int[][] divs = parse(chunk);
        mHorizontalDivs = divs == null ? new int[0] : divs[0];
        mVerticalDivs = divs == null ? new int[0] : divs[1];
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
    }

    public String getName()
    {
        return mName;
    }

    public Paint getPaint()
    {
        return mPaint;
    }

    public void setPaint(Paint paint)
    {
        mPaint = paint;
    }

    public Bitmap getBitmap()
    {
        return mBitmap;
    }

    public void draw(Canvas canvas, RectF location)
    {
        if (location == null) return;

        drawInto(canvas, location.left, location.top, location.right, location.bottom, mPaint);
    }

    public void draw(Canvas canvas, Rect location)
    {
        draw(canvas, location, mPaint);
    }

    public void draw(Canvas canvas, Rect location, Paint paint)
    {
        if (location == null) return;

        drawInto(canvas, location.left, location.top, location.right, location.bottom, paint);
    }

    public int getDensity()
    {
        return mBitmap.getDensity();
    }

    public int getWidth()
    {
        return mBitmap.getWidth();
    }

    public int getHeight()
    {
        return mBitmap.getHeight();
    }

    public final boolean hasAlpha()
    {
        return mBitmap.hasAlpha();
    }

    public final Region getTransparentRegion(Rect bounds)
    {
        return null;
    }

    public static boolean isNinePatchChunk(byte[] chunk)
    {
        return parse(chunk) != null;
    }

    void drawInto(Canvas canvas, float left, float top, float right, float bottom, Paint paint)
    {
        if (canvas == null || mBitmap == null || right <= left || bottom <= top) return;

        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();

        float[] sourceX = edges(mHorizontalDivs, width);
        float[] sourceY = edges(mVerticalDivs, height);
        float[] destinationX = stretch(sourceX, mHorizontalDivs, left, right);
        float[] destinationY = stretch(sourceY, mVerticalDivs, top, bottom);

        Rect source = new Rect();
        RectF destination = new RectF();

        for (int row = 0; row + 1 < sourceY.length; row++)
        {
            for (int column = 0; column + 1 < sourceX.length; column++)
            {
                if (sourceX[column + 1] <= sourceX[column] || sourceY[row + 1] <= sourceY[row]) continue;
                if (destinationX[column + 1] <= destinationX[column] || destinationY[row + 1] <= destinationY[row]) continue;

                source.set((int)sourceX[column], (int)sourceY[row], (int)sourceX[column + 1], (int)sourceY[row + 1]);
                destination.set(destinationX[column], destinationY[row], destinationX[column + 1], destinationY[row + 1]);
                canvas.drawBitmap(mBitmap, source, destination, paint);
            }
        }
    }

    private static float[] edges(int[] divs, int size)
    {
        float[] edges = new float[divs.length + 2];
        edges[0] = 0;

        for (int index = 0; index < divs.length; index++) edges[index + 1] = Math.max(0, Math.min(size, divs[index]));

        edges[edges.length - 1] = size;
        return edges;
    }

    private static float[] stretch(float[] source, int[] divs, float start, float end)
    {
        float fixed = 0;
        float stretchable = 0;

        for (int index = 0; index + 1 < source.length; index++)
        {
            float span = source[index + 1] - source[index];

            if ((index & 1) == 1) stretchable += span;
            else fixed += span;
        }

        float available = end - start;
        float fixedScale = fixed > available && fixed > 0 ? available / fixed : 1;
        float remaining = Math.max(0, available - fixed * fixedScale);
        float stretchScale = stretchable > 0 ? remaining / stretchable : 0;

        if (stretchable == 0 && fixed > 0) fixedScale = available / fixed;

        float[] out = new float[source.length];
        out[0] = start;

        for (int index = 0; index + 1 < source.length; index++)
        {
            float span = source[index + 1] - source[index];
            out[index + 1] = out[index] + span * ((index & 1) == 1 ? stretchScale : fixedScale);
        }

        out[out.length - 1] = end;
        return out;
    }

    private static int[][] parse(byte[] chunk)
    {
        if (chunk == null || chunk.length < HEADER_SIZE) return null;

        int horizontalCount = chunk[1] & 0xFF;
        int verticalCount = chunk[2] & 0xFF;
        int colourCount = chunk[3] & 0xFF;

        if ((horizontalCount & 1) != 0 || (verticalCount & 1) != 0) return null;
        if (chunk.length < HEADER_SIZE + (horizontalCount + verticalCount + colourCount) * 4) return null;

        boolean littleEndian = looksLittleEndian(chunk, horizontalCount + verticalCount);

        int[] horizontal = new int[horizontalCount];
        int[] vertical = new int[verticalCount];

        for (int index = 0; index < horizontalCount; index++) horizontal[index] = readInt(chunk, HEADER_SIZE + index * 4, littleEndian);

        for (int index = 0; index < verticalCount; index++)
        {
            vertical[index] = readInt(chunk, HEADER_SIZE + (horizontalCount + index) * 4, littleEndian);
        }

        if (!ascending(horizontal) || !ascending(vertical)) return null;

        return new int[][] {horizontal, vertical};
    }

    private static boolean looksLittleEndian(byte[] chunk, int count)
    {
        for (int index = 0; index < count; index++)
        {
            int little = readInt(chunk, HEADER_SIZE + index * 4, true);
            if (little < 0 || little > 0xFFFF) return false;
        }

        return true;
    }

    private static int readInt(byte[] data, int offset, boolean littleEndian)
    {
        int first = data[offset] & 0xFF;
        int second = data[offset + 1] & 0xFF;
        int third = data[offset + 2] & 0xFF;
        int fourth = data[offset + 3] & 0xFF;

        if (littleEndian) return first | (second << 8) | (third << 16) | (fourth << 24);

        return (first << 24) | (second << 16) | (third << 8) | fourth;
    }

    private static boolean ascending(int[] values)
    {
        for (int index = 0; index < values.length; index++)
        {
            if (values[index] < 0) return false;
            if (index > 0 && values[index] < values[index - 1]) return false;
        }

        return true;
    }
}
