package android.graphics;

public class MeshSpecification
{
    public static final int ALPHA_TYPE_UNKNOWN = 0;
    public static final int ALPHA_TYPE_OPAQUE = 1;
    public static final int ALPHA_TYPE_PREMULTIPLIED = 2;
    public static final int ALPHA_TYPE_UNPREMULTIPLIED = 3;

    public static final int TYPE_FLOAT = 0;
    public static final int TYPE_FLOAT2 = 1;
    public static final int TYPE_FLOAT3 = 2;
    public static final int TYPE_FLOAT4 = 3;
    public static final int TYPE_UBYTE4 = 4;

    private static final int MAXIMUM_ATTRIBUTES = 8;
    private static final int MAXIMUM_VARYINGS = 6;
    private static final int MAXIMUM_STRIDE = 1024;

    private Attribute[] mAttributes;
    private int mVertexStride;
    private Varying[] mVaryings;
    private String mVertexShader;
    private String mFragmentShader;
    private ColorSpace mColorSpace;
    private int mAlphaType;

    MeshSpecification()
    {
    }

    public static class Attribute
    {
        private final int mType;
        private final int mOffset;
        private final String mName;

        public Attribute(int type, int offset, String name)
        {
            if (name == null) throw new NullPointerException("name must not be null");

            mType = type;
            mOffset = offset;
            mName = name;
        }

        public int getType()
        {
            return mType;
        }

        public int getOffset()
        {
            return mOffset;
        }

        public String getName()
        {
            return mName;
        }

        @Override
        public String toString()
        {
            return "Attribute{mType=" + mType + ", mOffset=" + mOffset + ", mName='" + mName + "'}";
        }
    }

    public static class Varying
    {
        private final int mType;
        private final String mName;

        public Varying(int type, String name)
        {
            if (name == null) throw new NullPointerException("name must not be null");

            mType = type;
            mName = name;
        }

        public int getType()
        {
            return mType;
        }

        public String getName()
        {
            return mName;
        }

        @Override
        public String toString()
        {
            return "Varying{mType=" + mType + ", mName='" + mName + "'}";
        }
    }

    public static MeshSpecification make(Attribute[] attributes, int vertexStride, Varying[] varyings, String vertexShader,
                                         String fragmentShader)
    {
        return make(attributes, vertexStride, varyings, vertexShader, fragmentShader, ColorSpace.get(ColorSpace.Named.SRGB),
                    ALPHA_TYPE_PREMULTIPLIED);
    }

    public static MeshSpecification make(Attribute[] attributes, int vertexStride, Varying[] varyings, String vertexShader,
                                         String fragmentShader, ColorSpace colorSpace)
    {
        return make(attributes, vertexStride, varyings, vertexShader, fragmentShader, colorSpace, ALPHA_TYPE_PREMULTIPLIED);
    }

    public static MeshSpecification make(Attribute[] attributes, int vertexStride, Varying[] varyings, String vertexShader,
                                         String fragmentShader, ColorSpace colorSpace, int alphaType)
    {
        if (attributes == null || varyings == null || vertexShader == null || fragmentShader == null || colorSpace == null)
        {
            throw new NullPointerException("MeshSpecification arguments must not be null");
        }

        if (attributes.length == 0 || attributes.length > MAXIMUM_ATTRIBUTES)
        {
            throw new IllegalArgumentException("A mesh specification needs between 1 and 8 attributes");
        }

        if (varyings.length > MAXIMUM_VARYINGS) throw new IllegalArgumentException("A mesh specification allows at most 6 varyings");
        if (vertexStride <= 0 || vertexStride > MAXIMUM_STRIDE || vertexStride % 4 != 0)
        {
            throw new IllegalArgumentException("vertexStride must be a positive multiple of 4 no larger than 1024");
        }

        if (alphaType < ALPHA_TYPE_UNKNOWN || alphaType > ALPHA_TYPE_UNPREMULTIPLIED)
        {
            throw new IllegalArgumentException("Unknown alpha type " + alphaType);
        }

        MeshSpecification specification = new MeshSpecification();
        specification.mAttributes = attributes.clone();
        specification.mVertexStride = vertexStride;
        specification.mVaryings = varyings.clone();
        specification.mVertexShader = vertexShader;
        specification.mFragmentShader = fragmentShader;
        specification.mColorSpace = colorSpace;
        specification.mAlphaType = alphaType;

        return specification;
    }

    int vertexStride()
    {
        return mVertexStride;
    }

    Attribute positionAttribute()
    {
        Attribute fallback = null;

        for (Attribute attribute : mAttributes)
        {
            if (attribute.getType() != TYPE_FLOAT2) continue;
            if (attribute.getName().contains("osition")) return attribute;
            if (fallback == null) fallback = attribute;
        }

        return fallback;
    }
}
