package android.renderscript;

public class Element extends BaseObj
{
    private static final String PADDING_PREFIX = "#padding_";

    final DataType mType;
    final DataKind mKind;
    final boolean mNormalized;
    final int mVectorSize;
    final int mSize;

    final Element[] mElements;
    final String[] mElementNames;
    final int[] mArraySizes;
    final int[] mOffsetInBytes;

    public enum DataType
    {
        NONE(0, 0),
        FLOAT_16(1, 2),
        FLOAT_32(2, 4),
        FLOAT_64(3, 8),
        SIGNED_8(4, 1),
        SIGNED_16(5, 2),
        SIGNED_32(6, 4),
        SIGNED_64(7, 8),
        UNSIGNED_8(8, 1),
        UNSIGNED_16(9, 2),
        UNSIGNED_32(10, 4),
        UNSIGNED_64(11, 8),
        BOOLEAN(12, 1),
        UNSIGNED_5_6_5(13, 2),
        UNSIGNED_5_5_5_1(14, 2),
        UNSIGNED_4_4_4_4(15, 2),
        MATRIX_4X4(16, 64),
        MATRIX_3X3(17, 36),
        MATRIX_2X2(18, 16),
        RS_ELEMENT(1000, 32),
        RS_TYPE(1001, 32),
        RS_ALLOCATION(1002, 32),
        RS_SAMPLER(1003, 32),
        RS_SCRIPT(1004, 32),
        RS_MESH(1005, 32),
        RS_PROGRAM_FRAGMENT(1006, 32),
        RS_PROGRAM_VERTEX(1007, 32),
        RS_PROGRAM_RASTER(1008, 32),
        RS_PROGRAM_STORE(1009, 32),
        RS_FONT(1010, 32);

        final int mID;
        final int mSize;

        DataType(int id, int size)
        {
            mID = id;
            mSize = size;
        }
    }

    public enum DataKind
    {
        USER(0),
        PIXEL_L(7),
        PIXEL_A(8),
        PIXEL_LA(9),
        PIXEL_RGB(10),
        PIXEL_RGBA(11),
        PIXEL_DEPTH(12),
        PIXEL_YUV(13);

        final int mID;

        DataKind(int id)
        {
            mID = id;
        }
    }

    Element(RenderScript rs, DataType dataType, DataKind dataKind, boolean normalized, int vectorSize)
    {
        super(rs);

        if (dataType != DataType.UNSIGNED_5_6_5 && dataType != DataType.UNSIGNED_4_4_4_4 && dataType != DataType.UNSIGNED_5_5_5_1)
        {
            mSize = vectorSize == 3 ? dataType.mSize * 4 : dataType.mSize * vectorSize;
        }
        else
        {
            mSize = dataType.mSize;
        }

        mType = dataType;
        mKind = dataKind;
        mNormalized = normalized;
        mVectorSize = vectorSize;
        mElements = null;
        mElementNames = null;
        mArraySizes = null;
        mOffsetInBytes = null;
    }

    Element(RenderScript rs, Element[] elements, String[] elementNames, int[] arraySizes)
    {
        super(rs);

        mType = DataType.NONE;
        mKind = DataKind.USER;
        mNormalized = false;
        mVectorSize = 1;
        mElements = elements;
        mElementNames = elementNames;
        mArraySizes = arraySizes;
        mOffsetInBytes = new int[elements.length];

        int size = 0;
        for (int index = 0; index < elements.length; index++)
        {
            mOffsetInBytes[index] = size;
            size += elements[index].mSize * arraySizes[index];
        }
        mSize = size;
    }

    public int getBytesSize()
    {
        return mSize;
    }

    public int getVectorSize()
    {
        return mVectorSize;
    }

    public boolean isComplex()
    {
        if (mElements == null) return false;

        for (Element element : mElements)
        {
            if (element.mElements != null) return true;
        }
        return false;
    }

    public int getSubElementCount()
    {
        return mElements == null ? 0 : mElements.length;
    }

    public Element getSubElement(int index)
    {
        checkSubElementIndex(index);
        return mElements[index];
    }

    public String getSubElementName(int index)
    {
        checkSubElementIndex(index);
        return mElementNames[index];
    }

    public int getSubElementArraySize(int index)
    {
        checkSubElementIndex(index);
        return mArraySizes[index];
    }

    public int getSubElementOffsetBytes(int index)
    {
        checkSubElementIndex(index);
        return mOffsetInBytes[index];
    }

    private void checkSubElementIndex(int index)
    {
        if (mElements == null) throw new RSIllegalArgumentException("Element contains no sub-elements");
        if (index < 0 || index >= mElements.length) throw new RSIllegalArgumentException("Illegal sub-element index");
    }

    public DataType getDataType()
    {
        return mType;
    }

    public DataKind getDataKind()
    {
        return mKind;
    }

    public static Element BOOLEAN(RenderScript rs)
    {
        return user(rs, DataType.BOOLEAN);
    }

    public static Element U8(RenderScript rs)
    {
        return user(rs, DataType.UNSIGNED_8);
    }

    public static Element I8(RenderScript rs)
    {
        return user(rs, DataType.SIGNED_8);
    }

    public static Element U16(RenderScript rs)
    {
        return user(rs, DataType.UNSIGNED_16);
    }

    public static Element I16(RenderScript rs)
    {
        return user(rs, DataType.SIGNED_16);
    }

    public static Element U32(RenderScript rs)
    {
        return user(rs, DataType.UNSIGNED_32);
    }

    public static Element I32(RenderScript rs)
    {
        return user(rs, DataType.SIGNED_32);
    }

    public static Element U64(RenderScript rs)
    {
        return user(rs, DataType.UNSIGNED_64);
    }

    public static Element I64(RenderScript rs)
    {
        return user(rs, DataType.SIGNED_64);
    }

    public static Element F16(RenderScript rs)
    {
        return user(rs, DataType.FLOAT_16);
    }

    public static Element F32(RenderScript rs)
    {
        return user(rs, DataType.FLOAT_32);
    }

    public static Element F64(RenderScript rs)
    {
        return user(rs, DataType.FLOAT_64);
    }

    public static Element ELEMENT(RenderScript rs)
    {
        return user(rs, DataType.RS_ELEMENT);
    }

    public static Element TYPE(RenderScript rs)
    {
        return user(rs, DataType.RS_TYPE);
    }

    public static Element ALLOCATION(RenderScript rs)
    {
        return user(rs, DataType.RS_ALLOCATION);
    }

    public static Element SAMPLER(RenderScript rs)
    {
        return user(rs, DataType.RS_SAMPLER);
    }

    public static Element SCRIPT(RenderScript rs)
    {
        return user(rs, DataType.RS_SCRIPT);
    }

    public static Element MESH(RenderScript rs)
    {
        return user(rs, DataType.RS_MESH);
    }

    public static Element PROGRAM_FRAGMENT(RenderScript rs)
    {
        return user(rs, DataType.RS_PROGRAM_FRAGMENT);
    }

    public static Element PROGRAM_VERTEX(RenderScript rs)
    {
        return user(rs, DataType.RS_PROGRAM_VERTEX);
    }

    public static Element PROGRAM_RASTER(RenderScript rs)
    {
        return user(rs, DataType.RS_PROGRAM_RASTER);
    }

    public static Element PROGRAM_STORE(RenderScript rs)
    {
        return user(rs, DataType.RS_PROGRAM_STORE);
    }

    public static Element FONT(RenderScript rs)
    {
        return user(rs, DataType.RS_FONT);
    }

    public static Element A_8(RenderScript rs)
    {
        return pixel(rs, DataType.UNSIGNED_8, DataKind.PIXEL_A);
    }

    public static Element RGB_565(RenderScript rs)
    {
        return pixel(rs, DataType.UNSIGNED_5_6_5, DataKind.PIXEL_RGB);
    }

    public static Element RGB_888(RenderScript rs)
    {
        return pixel(rs, DataType.UNSIGNED_8, DataKind.PIXEL_RGB);
    }

    public static Element RGBA_5551(RenderScript rs)
    {
        return pixel(rs, DataType.UNSIGNED_5_5_5_1, DataKind.PIXEL_RGBA);
    }

    public static Element RGBA_4444(RenderScript rs)
    {
        return pixel(rs, DataType.UNSIGNED_4_4_4_4, DataKind.PIXEL_RGBA);
    }

    public static Element RGBA_8888(RenderScript rs)
    {
        return pixel(rs, DataType.UNSIGNED_8, DataKind.PIXEL_RGBA);
    }

    public static Element F16_2(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_16, 2);
    }

    public static Element F16_3(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_16, 3);
    }

    public static Element F16_4(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_16, 4);
    }

    public static Element F32_2(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_32, 2);
    }

    public static Element F32_3(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_32, 3);
    }

    public static Element F32_4(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_32, 4);
    }

    public static Element F64_2(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_64, 2);
    }

    public static Element F64_3(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_64, 3);
    }

    public static Element F64_4(RenderScript rs)
    {
        return vector(rs, DataType.FLOAT_64, 4);
    }

    public static Element U8_2(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_8, 2);
    }

    public static Element U8_3(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_8, 3);
    }

    public static Element U8_4(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_8, 4);
    }

    public static Element I8_2(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_8, 2);
    }

    public static Element I8_3(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_8, 3);
    }

    public static Element I8_4(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_8, 4);
    }

    public static Element U16_2(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_16, 2);
    }

    public static Element U16_3(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_16, 3);
    }

    public static Element U16_4(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_16, 4);
    }

    public static Element I16_2(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_16, 2);
    }

    public static Element I16_3(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_16, 3);
    }

    public static Element I16_4(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_16, 4);
    }

    public static Element U32_2(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_32, 2);
    }

    public static Element U32_3(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_32, 3);
    }

    public static Element U32_4(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_32, 4);
    }

    public static Element I32_2(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_32, 2);
    }

    public static Element I32_3(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_32, 3);
    }

    public static Element I32_4(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_32, 4);
    }

    public static Element U64_2(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_64, 2);
    }

    public static Element U64_3(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_64, 3);
    }

    public static Element U64_4(RenderScript rs)
    {
        return vector(rs, DataType.UNSIGNED_64, 4);
    }

    public static Element I64_2(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_64, 2);
    }

    public static Element I64_3(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_64, 3);
    }

    public static Element I64_4(RenderScript rs)
    {
        return vector(rs, DataType.SIGNED_64, 4);
    }

    public static Element YUV(RenderScript rs)
    {
        return pixel(rs, DataType.UNSIGNED_8, DataKind.PIXEL_YUV);
    }

    public static Element MATRIX_4X4(RenderScript rs)
    {
        return user(rs, DataType.MATRIX_4X4);
    }

    @Deprecated
    public static Element MATRIX4X4(RenderScript rs)
    {
        return MATRIX_4X4(rs);
    }

    public static Element MATRIX_3X3(RenderScript rs)
    {
        return user(rs, DataType.MATRIX_3X3);
    }

    public static Element MATRIX_2X2(RenderScript rs)
    {
        return user(rs, DataType.MATRIX_2X2);
    }

    private static Element user(RenderScript rs, DataType dataType)
    {
        String key = "user:" + dataType.name();
        Element cached = rs.cachedElement(key);
        if (cached != null) return cached;

        return rs.cacheElement(key, new Element(rs, dataType, DataKind.USER, false, 1));
    }

    private static Element vector(RenderScript rs, DataType dataType, int size)
    {
        String key = "vector:" + dataType.name() + ":" + size;
        Element cached = rs.cachedElement(key);
        if (cached != null) return cached;

        return rs.cacheElement(key, createVector(rs, dataType, size));
    }

    private static Element pixel(RenderScript rs, DataType dataType, DataKind dataKind)
    {
        String key = "pixel:" + dataType.name() + ":" + dataKind.name();
        Element cached = rs.cachedElement(key);
        if (cached != null) return cached;

        return rs.cacheElement(key, createPixel(rs, dataType, dataKind));
    }

    public static Element createVector(RenderScript rs, DataType dataType, int size)
    {
        if (size < 2 || size > 4) throw new RSIllegalArgumentException("Vector size out of range 2-4.");

        switch (dataType)
        {
            case FLOAT_16:
            case FLOAT_32:
            case FLOAT_64:
            case SIGNED_8:
            case SIGNED_16:
            case SIGNED_32:
            case SIGNED_64:
            case UNSIGNED_8:
            case UNSIGNED_16:
            case UNSIGNED_32:
            case UNSIGNED_64:
            case BOOLEAN:
                return new Element(rs, dataType, DataKind.USER, false, size);
            default:
                throw new RSIllegalArgumentException("Cannot create vector of non-primitive type.");
        }
    }

    public static Element createPixel(RenderScript rs, DataType dataType, DataKind dataKind)
    {
        if (!(dataKind == DataKind.PIXEL_L || dataKind == DataKind.PIXEL_A || dataKind == DataKind.PIXEL_LA
                || dataKind == DataKind.PIXEL_RGB || dataKind == DataKind.PIXEL_RGBA || dataKind == DataKind.PIXEL_DEPTH
                || dataKind == DataKind.PIXEL_YUV))
        {
            throw new RSIllegalArgumentException("Unsupported DataKind");
        }
        if (!(dataType == DataType.UNSIGNED_8 || dataType == DataType.UNSIGNED_16 || dataType == DataType.UNSIGNED_5_6_5
                || dataType == DataType.UNSIGNED_4_4_4_4 || dataType == DataType.UNSIGNED_5_5_5_1))
        {
            throw new RSIllegalArgumentException("Unsupported DataType");
        }
        if (dataType == DataType.UNSIGNED_5_6_5 && dataKind != DataKind.PIXEL_RGB)
        {
            throw new RSIllegalArgumentException("Bad kind and type combo");
        }
        if (dataType == DataType.UNSIGNED_5_5_5_1 && dataKind != DataKind.PIXEL_RGBA)
        {
            throw new RSIllegalArgumentException("Bad kind and type combo");
        }
        if (dataType == DataType.UNSIGNED_4_4_4_4 && dataKind != DataKind.PIXEL_RGBA)
        {
            throw new RSIllegalArgumentException("Bad kind and type combo");
        }
        if (dataType == DataType.UNSIGNED_16 && dataKind != DataKind.PIXEL_DEPTH)
        {
            throw new RSIllegalArgumentException("Bad kind and type combo");
        }

        int size = 1;
        switch (dataKind)
        {
            case PIXEL_LA:
                size = 2;
                break;
            case PIXEL_RGB:
                size = 3;
                break;
            case PIXEL_RGBA:
                size = 4;
                break;
            default:
                break;
        }

        return new Element(rs, dataType, dataKind, true, size);
    }

    public boolean isCompatible(Element element)
    {
        if (equals(element)) return true;
        if (element == null) return false;

        return mSize == element.mSize && mType != DataType.NONE && mType == element.mType && mVectorSize == element.mVectorSize;
    }

    public static class Builder
    {
        private final RenderScript mRS;

        private Element[] mElements = new Element[8];
        private String[] mElementNames = new String[8];
        private int[] mArraySizes = new int[8];
        private int mCount;
        private boolean mSkipPadding;

        public Builder(RenderScript rs)
        {
            mRS = rs;
        }

        public Builder add(Element element, String name, int arraySize)
        {
            if (arraySize < 1) throw new RSIllegalArgumentException("Array size cannot be less than 1.");

            if (mSkipPadding && name.startsWith(PADDING_PREFIX))
            {
                mSkipPadding = false;
                return this;
            }
            mSkipPadding = element.mVectorSize == 3;

            if (mCount == mElements.length)
            {
                Element[] elements = new Element[mCount + 8];
                String[] names = new String[mCount + 8];
                int[] arraySizes = new int[mCount + 8];
                System.arraycopy(mElements, 0, elements, 0, mCount);
                System.arraycopy(mElementNames, 0, names, 0, mCount);
                System.arraycopy(mArraySizes, 0, arraySizes, 0, mCount);
                mElements = elements;
                mElementNames = names;
                mArraySizes = arraySizes;
            }

            mElements[mCount] = element;
            mElementNames[mCount] = name;
            mArraySizes[mCount] = arraySize;
            mCount++;
            return this;
        }

        public Builder add(Element element, String name)
        {
            return add(element, name, 1);
        }

        public Element create()
        {
            mRS.validate();

            Element[] elements = new Element[mCount];
            String[] names = new String[mCount];
            int[] arraySizes = new int[mCount];
            System.arraycopy(mElements, 0, elements, 0, mCount);
            System.arraycopy(mElementNames, 0, names, 0, mCount);
            System.arraycopy(mArraySizes, 0, arraySizes, 0, mCount);
            return new Element(mRS, elements, names, arraySizes);
        }
    }
}
