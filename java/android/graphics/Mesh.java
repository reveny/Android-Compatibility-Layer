package android.graphics;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

public class Mesh
{
    public static final int TRIANGLES = 0;
    public static final int TRIANGLE_STRIP = 1;

    private final MeshSpecification mSpecification;
    private final int mMode;
    private final Buffer mVertexBuffer;
    private final int mVertexCount;
    private final ShortBuffer mIndexBuffer;
    private final RectF mBounds;
    private final Map<String, float[]> mFloatUniforms = new HashMap<>();
    private final Map<String, int[]> mIntUniforms = new HashMap<>();

    public Mesh(MeshSpecification meshSpec, int mode, Buffer vertexBuffer, int vertexCount, RectF bounds)
    {
        this(meshSpec, mode, vertexBuffer, vertexCount, null, bounds);
    }

    public Mesh(MeshSpecification meshSpec, int mode, Buffer vertexBuffer, int vertexCount, ShortBuffer indexBuffer, RectF bounds)
    {
        if (meshSpec == null || vertexBuffer == null || bounds == null) throw new NullPointerException("Mesh arguments must not be null");
        if (mode != TRIANGLES && mode != TRIANGLE_STRIP) throw new IllegalArgumentException("Invalid value passed in for mode parameter");
        if (vertexCount < 0) throw new IllegalArgumentException("vertexCount must not be negative");

        mSpecification = meshSpec;
        mMode = mode;
        mVertexBuffer = vertexBuffer;
        mVertexCount = vertexCount;
        mIndexBuffer = indexBuffer;
        mBounds = new RectF(bounds);
    }

    public void setColorUniform(String uniformName, int color)
    {
        setFloatUniform(uniformName, Color.red(color) / 255.0f, Color.green(color) / 255.0f, Color.blue(color) / 255.0f,
                        Color.alpha(color) / 255.0f);
    }

    public void setColorUniform(String uniformName, long color)
    {
        setFloatUniform(uniformName, Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color));
    }

    public void setColorUniform(String uniformName, Color color)
    {
        if (color == null) throw new NullPointerException("The color parameter must not be null");

        setFloatUniform(uniformName, color.red(), color.green(), color.blue(), color.alpha());
    }

    public void setFloatUniform(String uniformName, float value)
    {
        setFloatUniform(uniformName, new float[] {value});
    }

    public void setFloatUniform(String uniformName, float value1, float value2)
    {
        setFloatUniform(uniformName, new float[] {value1, value2});
    }

    public void setFloatUniform(String uniformName, float value1, float value2, float value3)
    {
        setFloatUniform(uniformName, new float[] {value1, value2, value3});
    }

    public void setFloatUniform(String uniformName, float value1, float value2, float value3, float value4)
    {
        setFloatUniform(uniformName, new float[] {value1, value2, value3, value4});
    }

    public void setFloatUniform(String uniformName, float[] values)
    {
        if (uniformName == null) throw new NullPointerException("The uniformName parameter must not be null");
        if (values == null) throw new NullPointerException("The uniform values parameter must not be null");

        mFloatUniforms.put(uniformName, values.clone());
    }

    public void setIntUniform(String uniformName, int value)
    {
        setIntUniform(uniformName, new int[] {value});
    }

    public void setIntUniform(String uniformName, int value1, int value2)
    {
        setIntUniform(uniformName, new int[] {value1, value2});
    }

    public void setIntUniform(String uniformName, int value1, int value2, int value3)
    {
        setIntUniform(uniformName, new int[] {value1, value2, value3});
    }

    public void setIntUniform(String uniformName, int value1, int value2, int value3, int value4)
    {
        setIntUniform(uniformName, new int[] {value1, value2, value3, value4});
    }

    public void setIntUniform(String uniformName, int[] values)
    {
        if (uniformName == null) throw new NullPointerException("The uniformName parameter must not be null");
        if (values == null) throw new NullPointerException("The uniform values parameter must not be null");

        mIntUniforms.put(uniformName, values.clone());
    }

    RectF bounds()
    {
        return mBounds;
    }

    Path trianglePath()
    {
        MeshSpecification.Attribute position = mSpecification.positionAttribute();
        if (position == null || mVertexCount <= 0) return null;

        float[] vertices = readPositions(position.getOffset());
        if (vertices == null) return null;

        int[] order;
        if (mIndexBuffer != null)
        {
            ShortBuffer indices = mIndexBuffer.duplicate();
            indices.rewind();
            order = new int[indices.remaining()];
            for (int index = 0; index < order.length; index++) order[index] = indices.get() & 0xFFFF;
        }
        else
        {
            order = new int[mVertexCount];
            for (int index = 0; index < order.length; index++) order[index] = index;
        }

        Path path = new Path();
        int triangles = mMode == TRIANGLES ? order.length / 3 : Math.max(0, order.length - 2);

        for (int triangle = 0; triangle < triangles; triangle++)
        {
            int first = mMode == TRIANGLES ? order[triangle * 3] : order[triangle];
            int second = mMode == TRIANGLES ? order[triangle * 3 + 1] : order[triangle + 1];
            int third = mMode == TRIANGLES ? order[triangle * 3 + 2] : order[triangle + 2];

            if (first >= mVertexCount || second >= mVertexCount || third >= mVertexCount) continue;

            path.moveTo(vertices[first * 2], vertices[first * 2 + 1]);
            path.lineTo(vertices[second * 2], vertices[second * 2 + 1]);
            path.lineTo(vertices[third * 2], vertices[third * 2 + 1]);
            path.close();
        }

        return path;
    }

    private float[] readPositions(int offset)
    {
        int stride = mSpecification.vertexStride();
        float[] vertices = new float[mVertexCount * 2];

        if (mVertexBuffer instanceof ByteBuffer)
        {
            ByteBuffer bytes = ((ByteBuffer) mVertexBuffer).duplicate().order(ByteOrder.nativeOrder());
            int base = bytes.position();

            for (int vertex = 0; vertex < mVertexCount; vertex++)
            {
                int at = base + vertex * stride + offset;
                if (at + 8 > bytes.limit()) return null;

                vertices[vertex * 2] = bytes.getFloat(at);
                vertices[vertex * 2 + 1] = bytes.getFloat(at + 4);
            }

            return vertices;
        }

        if (mVertexBuffer instanceof FloatBuffer)
        {
            FloatBuffer floats = ((FloatBuffer) mVertexBuffer).duplicate();
            int base = floats.position();
            int floatStride = stride / 4;
            int floatOffset = offset / 4;

            for (int vertex = 0; vertex < mVertexCount; vertex++)
            {
                int at = base + vertex * floatStride + floatOffset;
                if (at + 2 > floats.limit()) return null;

                vertices[vertex * 2] = floats.get(at);
                vertices[vertex * 2 + 1] = floats.get(at + 1);
            }

            return vertices;
        }

        return null;
    }
}
